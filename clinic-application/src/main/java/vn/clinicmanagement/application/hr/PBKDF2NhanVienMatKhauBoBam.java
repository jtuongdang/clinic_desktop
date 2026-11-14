package vn.clinicmanagement.application.hr;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

/** Produces hashes compatible with the application's PBKDF2 password verifier. */
public final class PBKDF2NhanVienMatKhauBoBam implements NhanVienMatKhauBoBam {
    private static final int ITERATIONS = 210_000;
    private static final int KEY_BITS = 256;
    private static final int SALT_BYTES = 16;
    private final SecureRandom random;

    public PBKDF2NhanVienMatKhauBoBam() {
        this(new SecureRandom());
    }

    PBKDF2NhanVienMatKhauBoBam(SecureRandom random) {
        this.random = random;
    }

    @Override
    public String hash(char[] password) {
        if (password == null || password.length == 0) {
            throw new IllegalArgumentException("Mật khẩu không được để trống.");
        }

        byte[] salt = new byte[SALT_BYTES];
        random.nextBytes(salt);
        PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, KEY_BITS);
        try {
            byte[] derived = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec).getEncoded();
            return "pbkdf2_sha256$" + ITERATIONS + "$" + Base64.getEncoder().encodeToString(salt)
                    + "$" + Base64.getEncoder().encodeToString(derived);
        } catch (Exception ex) {
            throw new IllegalStateException("Không thể tạo mã băm mật khẩu.", ex);
        } finally {
            spec.clearPassword();
        }
    }
}
