package qarma;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;

public class CryptoService {

    private static final int SALT_LEN = 16;
    private static final int IV_LEN = 12;
    private static final int TAG_BITS = 128;
    private static final int ITERATIONS = 600_000;
    private static final int KEY_BITS = 256;
    private static final SecureRandom RNG = new SecureRandom();



    public static byte[] encrypt(byte[] plain, char[] password) throws GeneralSecurityException {
        byte[] salt = new byte[SALT_LEN];
        byte[] iv = new byte[IV_LEN];
        RNG.nextBytes(salt);
        RNG.nextBytes(iv);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, deriveKey(password, salt), new GCMParameterSpec(TAG_BITS, iv));
        byte[] ct = cipher.doFinal(plain);

        return ByteBuffer.allocate(SALT_LEN + IV_LEN + ct.length)
                .put(salt).put(iv).put(ct).array();
    }

    public static byte[] decrypt(byte[] data, char[] password) throws GeneralSecurityException {
        if (data.length < SALT_LEN + IV_LEN + TAG_BITS / 8) {
            throw new GeneralSecurityException("Vault file is too short or corrupted");
        }
        ByteBuffer bb = ByteBuffer.wrap(data);
        byte[] salt = new byte[SALT_LEN];
        byte[] iv = new byte[IV_LEN];
        byte[] ct = new byte[data.length - SALT_LEN - IV_LEN];
        bb.get(salt).get(iv).get(ct);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, deriveKey(password, salt), new GCMParameterSpec(TAG_BITS, iv));
        return cipher.doFinal(ct);
    }

    private static SecretKey deriveKey(char[] password, byte[] salt) throws GeneralSecurityException {
        PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, KEY_BITS);
        try {
            byte[] keyBytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec).getEncoded();
            SecretKey key = new SecretKeySpec(keyBytes, "AES");
            Arrays.fill(keyBytes, (byte) 0);
            return key;
        } finally {
            spec.clearPassword();
        }
    }

}
