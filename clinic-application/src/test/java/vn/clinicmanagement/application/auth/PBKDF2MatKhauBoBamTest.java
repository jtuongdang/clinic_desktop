package vn.clinicmanagement.application.auth;

import org.junit.jupiter.api.Test;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PBKDF2MatKhauBoBamTest {
    private static final char[] SYNTHETIC_CREDENTIAL = "synthetic-test-only-credential-alpha".toCharArray();
    private static final byte[] SYNTHETIC_SALT_16B = "synthetic-salt16".getBytes(StandardCharsets.UTF_8);
    private static final int MIN_VALID_ITERATIONS = 100_000;

    private final PBKDF2MatKhauBoBam hasher = new PBKDF2MatKhauBoBam();

    @Test
    void verifiesDeterministicSyntheticTestVectorAndRejectsWrongOrTamperedCredentials() {
        String encodedVector = encodeSyntheticVector(SYNTHETIC_CREDENTIAL, SYNTHETIC_SALT_16B, MIN_VALID_ITERATIONS);
        String repeatedVector = encodeSyntheticVector(SYNTHETIC_CREDENTIAL, SYNTHETIC_SALT_16B, MIN_VALID_ITERATIONS);

        assertEquals(encodedVector, repeatedVector);
        assertTrue(hasher.verify(SYNTHETIC_CREDENTIAL.clone(), encodedVector));
        assertFalse(hasher.verify("synthetic-test-only-credential-beta".toCharArray(), encodedVector));
        assertFalse(hasher.verify(new char[0], encodedVector));

        byte[] originalDigest = deriveDigest(SYNTHETIC_CREDENTIAL, SYNTHETIC_SALT_16B, MIN_VALID_ITERATIONS);
        byte[] tamperedDigest = Arrays.copyOf(originalDigest, originalDigest.length);
        tamperedDigest[tamperedDigest.length - 1] ^= 0x01;
        String tamperedVector = formatEncoded("pbkdf2_sha256", MIN_VALID_ITERATIONS, SYNTHETIC_SALT_16B, tamperedDigest);

        assertFalse(hasher.verify(SYNTHETIC_CREDENTIAL.clone(), tamperedVector));
    }

    @Test
    void enforcesSaltLengthAndIterationBoundsFailClosed() {
        byte[] salt15 = syntheticBytes(15);
        byte[] salt16 = syntheticBytes(16);
        byte[] salt64 = syntheticBytes(64);
        byte[] salt65 = syntheticBytes(65);
        byte[] validDigest = deriveDigest(SYNTHETIC_CREDENTIAL, SYNTHETIC_SALT_16B, MIN_VALID_ITERATIONS);

        assertTrue(hasher.verify(
                SYNTHETIC_CREDENTIAL.clone(),
                encodeSyntheticVector(SYNTHETIC_CREDENTIAL, salt16, MIN_VALID_ITERATIONS)
        ));
        assertTrue(hasher.verify(
                SYNTHETIC_CREDENTIAL.clone(),
                encodeSyntheticVector(SYNTHETIC_CREDENTIAL, salt64, MIN_VALID_ITERATIONS)
        ));

        assertFalse(hasher.verify(
                SYNTHETIC_CREDENTIAL.clone(),
                formatEncoded("pbkdf2_sha256", MIN_VALID_ITERATIONS, salt15, validDigest)
        ));
        assertFalse(hasher.verify(
                SYNTHETIC_CREDENTIAL.clone(),
                formatEncoded("pbkdf2_sha256", MIN_VALID_ITERATIONS, salt65, validDigest)
        ));
        assertFalse(hasher.verify(
                SYNTHETIC_CREDENTIAL.clone(),
                formatEncoded("pbkdf2_sha256", 99_999, SYNTHETIC_SALT_16B, validDigest)
        ));
        assertFalse(hasher.verify(
                SYNTHETIC_CREDENTIAL.clone(),
                formatEncoded("pbkdf2_sha256", 1_000_001, SYNTHETIC_SALT_16B, validDigest)
        ));
        assertFalse(hasher.verify(
                SYNTHETIC_CREDENTIAL.clone(),
                formatEncoded("pbkdf2_sha256", 0, SYNTHETIC_SALT_16B, validDigest)
        ));
        assertFalse(hasher.verify(
                SYNTHETIC_CREDENTIAL.clone(),
                formatEncoded("pbkdf2_sha256", -100_000, SYNTHETIC_SALT_16B, validDigest)
        ));
    }

    @Test
    void rejectsNullMalformedAndUnsupportedEncodingsFailClosed() {
        String validVector = encodeSyntheticVector(SYNTHETIC_CREDENTIAL, SYNTHETIC_SALT_16B, MIN_VALID_ITERATIONS);
        String validSaltB64 = Base64.getEncoder().encodeToString(SYNTHETIC_SALT_16B);
        byte[] validDigest = deriveDigest(SYNTHETIC_CREDENTIAL, SYNTHETIC_SALT_16B, MIN_VALID_ITERATIONS);
        String validDigestB64 = Base64.getEncoder().encodeToString(validDigest);

        assertFalse(hasher.verify(null, validVector));
        assertFalse(hasher.verify(SYNTHETIC_CREDENTIAL.clone(), null));

        List<String> malformedOrUnsupported = List.of(
                "",
                "   ",
                "pbkdf2_sha512$100000$" + validSaltB64 + "$" + validDigestB64,
                "bcrypt$100000$" + validSaltB64 + "$" + validDigestB64,
                "pbkdf2_sha256$100000$" + validSaltB64,
                "pbkdf2_sha256$100000$" + validSaltB64 + "$" + validDigestB64 + "$extra",
                "pbkdf2_sha256$100000$" + validSaltB64 + "$" + validDigestB64 + "$",
                "pbkdf2_sha256$not-an-int$" + validSaltB64 + "$" + validDigestB64,
                "pbkdf2_sha256$100000$%%%not-base64%%%$" + validDigestB64,
                "pbkdf2_sha256$100000$" + validSaltB64 + "$%%%not-base64%%%",
                formatEncoded("pbkdf2_sha256", MIN_VALID_ITERATIONS, SYNTHETIC_SALT_16B, syntheticBytes(31)),
                formatEncoded("pbkdf2_sha256", MIN_VALID_ITERATIONS, SYNTHETIC_SALT_16B, syntheticBytes(33))
        );

        for (String candidate : malformedOrUnsupported) {
            assertFalse(hasher.verify(SYNTHETIC_CREDENTIAL.clone(), candidate),
                    "Expected malformed or unsupported hash to fail closed: " + candidate);
        }
    }

    private static String encodeSyntheticVector(char[] password, byte[] salt, int iterations) {
        byte[] digest = deriveDigest(password, salt, iterations);
        return formatEncoded("pbkdf2_sha256", iterations, salt, digest);
    }

    private static String formatEncoded(String algorithm, int iterations, byte[] salt, byte[] digest) {
        return algorithm + "$" + iterations + "$"
                + Base64.getEncoder().encodeToString(salt) + "$"
                + Base64.getEncoder().encodeToString(digest);
    }

    private static byte[] deriveDigest(char[] password, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, 256);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec)
                    .getEncoded();
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to derive synthetic PBKDF2 test vector", ex);
        } finally {
            spec.clearPassword();
        }
    }

    private static byte[] syntheticBytes(int length) {
        byte[] bytes = new byte[length];
        for (int i = 0; i < length; i++) {
            bytes[i] = (byte) (0x21 + (i % 64));
        }
        return bytes;
    }
}
