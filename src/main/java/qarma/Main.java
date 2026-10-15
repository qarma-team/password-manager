package qarma;

import qarma.security.PasswordGenerator;

import javax.crypto.AEADBadTagException;
import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class Main {
    private static final Path VAULT_PATH = System.getenv("QARMA_VAULT") != null
            ? Path.of(System.getenv("QARMA_VAULT"))
            : Path.of(System.getProperty("user.home"), ".qarma-vault");

    private static final int MIN_MASTER_LENGTH = 12;
    private static final int DEFAULT_GEN_LENGTH = 20;

    private static final Console CONSOLE = System.console();
    private static final BufferedReader IN = new BufferedReader(new InputStreamReader(System.in));

    private static final PasswordGenerator GENERATOR = new PasswordGenerator();

    public static void main(String[] args) {
        if (args.length == 0) {
            printHelp();
            return;
        }

        try {
            switch (args[0].toLowerCase()) {
                case "init" -> init();
                case "add" -> add(requireArg(args, 1, "url"));
                case "get" -> get(requireArg(args, 1, "url"));
                case "list" -> list();
                case "delete" -> delete(requireArg(args, 1, "url"));
                case "gen" -> gen(args);
                case "help", "-h", "--help" -> printHelp();
                default -> fail("Unknown command: " + args[0] + "\nRun 'qarma help' to see available commands.");
            }
        } catch (AEADBadTagException e) {
            fail("Wrong master password, or the vault file is corrupted.");
        } catch (IllegalArgumentException e) {
            fail(e.getMessage());
        } catch (Exception e) {
            fail("Error: " + e.getMessage());
        }
    }


    private static void init() throws Exception {
        if (Vault.exists(VAULT_PATH)) {
            fail("A vault already exists at " + VAULT_PATH + "\nDelete it manually if you really want to start over.");
        }

        char[] master = readPassword("Create master password: ");
        char[] confirm = readPassword("Repeat master password: ");
        try {
            if (!Arrays.equals(master, confirm)) {
                fail("Passwords do not match.");
            }
            if (master.length < MIN_MASTER_LENGTH) {
                fail("Master password must be at least " + MIN_MASTER_LENGTH + " characters.");
            }
            Vault.empty().save(VAULT_PATH, master);
            System.out.println("Vault created at " + VAULT_PATH);
            System.out.println("Remember your master password. It cannot be recovered.");
        } finally {
            wipe(master);
            wipe(confirm);
        }
    }

    private static void add(String url) throws Exception {
        char[] master = unlockPrompt();
        try {
            Vault vault = Vault.load(VAULT_PATH, master);

            if (vault.contains(url) && !confirm("An entry for '" + url + "' already exists. Overwrite?")) {
                System.out.println("Cancelled.");
                return;
            }

            String username = readLine("Username: ");
            char[] pwChars = readPassword("Password (leave empty to generate one): ");
            String password;
            if (pwChars.length == 0) {
                password = GENERATOR.generate(DEFAULT_GEN_LENGTH);
                System.out.println("Generated a " + DEFAULT_GEN_LENGTH + "-character password.");
            } else {
                password = new String(pwChars);
                wipe(pwChars);
            }
            String notes = readLine("Notes (optional): ");

            vault.add(new Entry(username, password, url, notes));
            vault.save(VAULT_PATH, master);
            System.out.println("Saved entry for '" + url + "'.");
        } finally {
            wipe(master);
        }
    }

    private static void get(String url) throws Exception {
        char[] master = unlockPrompt();
        try {
            Vault vault = Vault.load(VAULT_PATH, master);
            Optional<Entry> found = vault.get(url);
            if (found.isEmpty()) {
                fail("No entry found for '" + url + "'.");
            }
            Entry e = found.get();
            System.out.println("URL:      " + e.getUrl());
            System.out.println("Username: " + e.getUsername());
            System.out.println("Password: " + e.getPassword());
            if (e.getNotes() != null && !e.getNotes().isBlank()) {
                System.out.println("Notes:    " + e.getNotes());
            }
        } finally {
            wipe(master);
        }
    }

    private static void list() throws Exception {
        char[] master = unlockPrompt();
        try {
            List<Entry> entries = Vault.load(VAULT_PATH, master).list();
            if (entries.isEmpty()) {
                System.out.println("The vault is empty. Add an entry with: qarma add <url>");
                return;
            }
            System.out.println(entries.size() + " entr" + (entries.size() == 1 ? "y" : "ies") + ":");
            for (Entry e : entries) {
                System.out.println("  " + e.getUrl() + "  (" + e.getUsername() + ")");
            }
        } finally {
            wipe(master);
        }
    }

    private static void delete(String url) throws Exception {
        char[] master = unlockPrompt();
        try {
            Vault vault = Vault.load(VAULT_PATH, master);
            if (!vault.contains(url)) {
                fail("No entry found for '" + url + "'.");
            }
            if (!confirm("Delete the entry for '" + url + "'?")) {
                System.out.println("Cancelled.");
                return;
            }
            vault.delete(url);
            vault.save(VAULT_PATH, master);
            System.out.println("Deleted entry for '" + url + "'.");
        } finally {
            wipe(master);
        }
    }

    private static void gen(String[] args) {
        int length = DEFAULT_GEN_LENGTH;
        if (args.length > 1) {
            try {
                length = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                fail("Length must be a number, e.g. 'qarma gen 24'.");
            }
        }
        System.out.println(GENERATOR.generate(length));
    }



    private static char[] unlockPrompt() throws IOException {
        if (!Vault.exists(VAULT_PATH)) {
            fail("No vault found. Create one first with: qarma init");
        }
        return readPassword("Master password: ");
    }

    private static char[] readPassword(String prompt) throws IOException {
        if (CONSOLE != null) {
            char[] pw = CONSOLE.readPassword(prompt);
            return pw != null ? pw : new char[0];
        }
        System.out.print(prompt);
        System.out.flush();
        String line = IN.readLine();
        return line != null ? line.toCharArray() : new char[0];
    }

    private static String readLine(String prompt) throws IOException {
        if (CONSOLE != null) {
            String line = CONSOLE.readLine(prompt);
            return line != null ? line.trim() : "";
        }
        System.out.print(prompt);
        System.out.flush();
        String line = IN.readLine();
        return line != null ? line.trim() : "";
    }

    private static boolean confirm(String question) throws IOException {
        String answer = readLine(question + " [y/N]: ");
        return answer.equalsIgnoreCase("y") || answer.equalsIgnoreCase("yes");
    }

    private static String requireArg(String[] args, int index, String name) {
        if (args.length <= index) {
            fail("Missing argument: <" + name + ">\nUsage: qarma " + args[0] + " <" + name + ">");
        }
        return args[index];
    }

    private static void wipe(char[] secret) {
        if (secret != null) {
            Arrays.fill(secret, '\0');
        }
    }

    private static void fail(String message) {
        System.err.println(message);
        System.exit(1);
    }

    private static void printHelp() {
        System.out.println("""
                qarma - a simple encrypted password manager

                Usage: qarma <command> [arguments]

                Commands:
                  init            Create a new vault
                  add <url>       Add or update an entry
                  get <url>       Show an entry (including its password)
                  list            List all entries (no passwords)
                  delete <url>    Delete an entry
                  gen [length]    Generate a random password (default 20)
                  help            Show this help

                Vault file: """ + VAULT_PATH);
    }
}