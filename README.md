# qarma: a command-line password manager in Java

a small, local password manager that runs in the terminal; stores logins in a single encrypted file protected by one master password, and can generate strong random passwords.

built it as a learning project to practise applied cryptography in Java: key derivation, authenticated encryption, and handling secrets safely in memory.

> ⚠️ **This is an educational project.** It has not been audited. Don't use it for your real passwords; use an established manager such as Bitwarden or KeePassXC.

## Features

- **Encrypted vault.** All entries are stored in one file encrypted with AES-256-GCM.
- **Master password, never stored.** The encryption key is derived from it each time with PBKDF2, and neither the password nor the key is ever written to disk.
- **Tamper detection.** A wrong master password, or any change to the vault file, is detected and rejected.
- **Password generator.** Random passwords built with `SecureRandom`, guaranteed to contain a letter, a digit and a symbol.
- **Hidden input.** The master password and entry passwords are not shown on screen when you type them in a terminal.
- **Crash-safe saving.** The vault is written to a temporary file first and then moved into place, so a crash can't leave it half-written.

## Requirements

- Java 17 or newer
- Maven

## Build

```bash
mvn clean package
```

This creates `target/pm.jar`, with the Gson dependency bundled inside.

## Usage

```bash
java -jar target/pm.jar <command> [arguments]
```

| Command | What it does |
|---|---|
| `init` | Create a new vault and set the master password (at least 12 characters) |
| `add <url>` | Add an entry. Leave the password empty to generate one. Asks before overwriting an existing entry. |
| `get <url>` | Show an entry, including its password |
| `list` | List all entries (URLs and usernames only, never passwords) |
| `delete <url>` | Delete an entry after confirmation |
| `gen [length]` | Print a random password (default 20, allowed 8–128) |
| `help` | Show usage |

### Example

```text
$ java -jar target/pm.jar init
Create master password:
Repeat master password:
Vault created at /Users/you/.qarma-vault
Remember your master password. It cannot be recovered.

$ java -jar target/pm.jar add github.com
Master password:
Username: alice
Password (leave empty to generate one):
Generated a 20-character password.
Notes (optional):
Saved entry for 'github.com'.

$ java -jar target/pm.jar list
Master password:
1 entry:
  github.com  (alice)

$ java -jar target/pm.jar gen 24
q7#Rk.Vm2!xT9bLw$e4ZpN&h
```

The vault is stored at `~/.qarma-vault`. To use a different location (for example, a test vault), set the `QARMA_VAULT` environment variable:

```bash
QARMA_VAULT=./test-vault java -jar target/pm.jar init
```

## How it works

### Architecture

```
Main  ──►  Vault  ──►  CryptoService
 │          │
 │          └── List<Entry>
 └──►  PasswordGenerator
```

| Class | Responsibility |
|---|---|
| `Main` | Command-line interface: parses commands, reads input, prints results and friendly error messages |
| `Entry` | One saved login: URL, username, password, notes |
| `Vault` | Holds the entries in memory; converts them to JSON and loads/saves the encrypted file |
| `CryptoService` | All cryptography: key derivation, encryption and decryption. Works only with raw bytes. |
| `PasswordGenerator` | Generates random passwords |

Each class has a single job. `CryptoService` knows nothing about entries or files, and `Main` never touches encryption directly.

### Saving the vault

1. Entries are serialised to JSON.
2. A new random 16-byte **salt** and 12-byte **IV** are generated.
3. The master password and salt go through **PBKDF2-HMAC-SHA256** (600,000 iterations) to produce a 256-bit key.
4. The JSON is encrypted with **AES-256-GCM**, which also produces a 16-byte authentication tag.
5. `salt + IV + ciphertext + tag` is written to a temp file, which then replaces the vault file.

### Opening the vault

1. The salt and IV are read from the start of the file.
2. The key is derived again from the typed master password and that salt.
3. AES-GCM decrypts and checks the tag. With the right password, the original JSON comes back. With the wrong password, or a modified file, the tag check fails and the program reports it instead of returning data.

The master password is never compared against anything stored. The vault either decrypts correctly or it doesn't.

### File format

```
[ salt: 16 bytes ][ IV: 12 bytes ][ ciphertext ... ][ GCM tag: 16 bytes ]
```

## Security design

| Decision | Why |
|---|---|
| **AES-256-GCM** | Authenticated encryption: it provides confidentiality and detects any tampering or a wrong key. |
| **PBKDF2-HMAC-SHA256, 600,000 iterations** | Turns a human password into a key and makes each guess deliberately slow (about a second). The iteration count follows OWASP's current recommendation. |
| **Random salt per save** | The same master password produces a different key in every vault, which defeats precomputed (rainbow table) attacks. |
| **Fresh random IV on every save** | Reusing an IV with the same key breaks GCM, so a new one is generated each time. |
| **`SecureRandom` everywhere** | `java.util.Random` is predictable and unsuitable for keys, IVs or passwords. |
| **Master password not stored** | Nothing on disk can be stolen and cracked offline except the encrypted vault itself. |
| **Passwords handled as `char[]`** | Unlike `String`, a `char[]` can be overwritten with zeros after use, which shortens how long the secret stays in memory. |
| **No passwords as command-line arguments** | Arguments are visible in shell history and process lists, so all secrets are typed at a prompt. |
| **One error message for wrong password and corrupted file** | It doesn't tell an attacker which one occurred. |

## Threat model

**Protects against:**
- Someone who copies or steals the vault file. Without the master password they see only encrypted bytes, and brute-forcing a strong master password is very slow.
- Modification of the vault file. Any change is detected when it is opened.
- Shoulder-surfing while typing passwords in a terminal.

**Does not protect against:**
- Malware or a keylogger on your computer, which can capture the master password as you type it.
- A weak master password. The vault is only as strong as the password protecting it (minimum length is enforced, but not strength).
- Someone with access to your unlocked terminal while a password is displayed by `get`.
- Memory inspection. Java can't fully guarantee that secrets are wiped from memory (for example, decrypted entries exist as `String` objects).
- Loss of the master password. There is no recovery, by design.

## Known limitations

- Running inside an IDE, passwords are visible while typing, because `System.console()` is not available there. Use a real terminal.
- The master password is asked for on every command; there is no session or auto-lock mode.
- The whole vault is decrypted into memory for every command, which is fine for personal use but not for very large vaults.

## Possible improvements

- Password strength checker for the master password and stored entries
- Breach check against the Have I Been Pwned API using k-anonymity (only the first 5 characters of the SHA-1 hash are sent)
- Copy to clipboard with automatic clearing after 30 seconds
- `audit` command to find weak and reused passwords
- Change master password command
- Argon2id instead of PBKDF2
- Unit tests for the security properties (wrong password fails, tampering is detected, every save produces a different ciphertext)

## Project structure

```
src/main/java/qarma/
├── Main.java
├── Entry.java
├── Vault.java
├── CryptoService.java
└── security/
    └── PasswordGenerator.java
```
