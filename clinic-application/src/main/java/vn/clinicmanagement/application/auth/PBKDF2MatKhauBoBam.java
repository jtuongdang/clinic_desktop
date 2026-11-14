package vn.clinicmanagement.application.auth;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.util.Base64;

/** PBKDF2-HMAC-SHA256 verifier; format: pbkdf2_sha256$iterations$salt$hash. */
public final class PBKDF2MatKhauBoBam implements MatKhauBoBam {
    private static final String PREFIX = "pbkdf2_sha256$";
    private static final int KEY_BITS = 256;
    private static final int MIN_ITERATIONS = 100_000;
    private static final int MAX_ITERATIONS = 1_000_000;

    @Override
    public boolean verify(char[] password, String encodedHash) {
        if (password == null || encodedHash == null || !encodedHash.startsWith(PREFIX)) return false;
        String[] parts = encodedHash.split("\\$", -1);
        if (parts.length != 4 || !"pbkdf2_sha256".equals(parts[0])) return false;
        try {
            int iterations = Integer.parseInt(parts[1]);
            if (iterations < MIN_ITERATIONS || iterations > MAX_ITERATIONS) return false;
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            if (salt.length < 16 || salt.length > 64 || expected.length != KEY_BITS / 8) return false;

            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, KEY_BITS);
            try {
                byte[] actual = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                        .generateSecret(spec).getEncoded();
                return MessageDigest.isEqual(expected, actual);
            } finally {
                spec.clearPassword();
            }
        } catch (Exception ignored) {
            return false;
        }
    }
}
