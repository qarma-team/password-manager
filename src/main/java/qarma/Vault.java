package qarma;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import qarma.crypto.CryptoService;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;


public class Vault {
    private static final Gson GSON = new Gson();
    private static final Type ENTRY_LIST_TYPE = new TypeToken<List<Entry>>() { }.getType();

    private final List<Entry> entries;

    private Vault(List<Entry> entries) {
        this.entries = entries;
    }

    public static Vault empty() {
        return new Vault(new ArrayList<>());
    }

    public static boolean exists(Path file) {
        return Files.exists(file);
    }

   
    public static Vault load(Path file, char[] masterPassword) throws IOException, GeneralSecurityException {
        byte[] encrypted = Files.readAllBytes(file);
        byte[] plain = CryptoService.decrypt(encrypted, masterPassword);
        try {
            String json = new String(plain, StandardCharsets.UTF_8);
            List<Entry> list = GSON.fromJson(json, ENTRY_LIST_TYPE);
            return new Vault(list != null ? new ArrayList<>(list) : new ArrayList<>());
        } finally {
            Arrays.fill(plain, (byte) 0); 
        }
    }

    public void save(Path file, char[] masterPassword) throws IOException, GeneralSecurityException {
        byte[] plain = GSON.toJson(entries).getBytes(StandardCharsets.UTF_8);
        try {
            byte[] encrypted = CryptoService.encrypt(plain, masterPassword);

            Path dir = file.toAbsolutePath().getParent();
            Files.createDirectories(dir);
            Path tmp = Files.createTempFile(dir, ".vault-", ".tmp");
            try {
                Files.write(tmp, encrypted);
                try {
                    Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException e) {
                    Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
                }
            } finally {
                Files.deleteIfExists(tmp); 
            }
        } finally {
            Arrays.fill(plain, (byte) 0); 
        }
    }

    public void add(Entry entry) {
        delete(entry.getUrl());
        entries.add(entry);
    }

    public Optional<Entry> get(String url) {
        return entries.stream()
                .filter(e -> e.getUrl().equalsIgnoreCase(url))
                .findFirst();
    }

    public boolean contains(String url) {
        return get(url).isPresent();
    }

    public boolean delete(String url) {
        return entries.removeIf(e -> e.getUrl().equalsIgnoreCase(url));
    }

    public List<Entry> list() {
        List<Entry> copy = new ArrayList<>(entries);
        copy.sort(Comparator.comparing(e -> e.getUrl().toLowerCase()));
        return copy;
    }

    public int size() {
        return entries.size();
    }
}