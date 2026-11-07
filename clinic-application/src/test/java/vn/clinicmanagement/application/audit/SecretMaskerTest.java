package vn.clinicmanagement.application.audit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SecretMasker Tests (AUD-01 Secret Masking)")
class SecretMaskerTest {

    @Test
    @DisplayName("Che giấu mật khẩu trong chuỗi JSON")
    void shouldMaskPasswordInJson() {
        String json = "{\"username\": \"admin\", \"password\": \"SecretPassword123!\", \"email\": \"admin@clinic.vn\"}";
        String masked = SecretMasker.mask(json);

        assertFalse(masked.contains("SecretPassword123!"));
        assertTrue(masked.contains("\"password\": \"******\""));
        assertTrue(masked.contains("\"username\": \"admin\""));
        assertTrue(masked.contains("\"email\": \"admin@clinic.vn\""));
    }

    @Test
    @DisplayName("Che giấu passwordHash và matKhau")
    void shouldMaskPasswordHashAndMatKhau() {
        String json = "{\"user\": \"tiennv\", \"passwordHash\": \"$2a$10$abcdef123456789\", \"matKhau\": \"P@ssw0rd\"}";
        String masked = SecretMasker.mask(json);

        assertFalse(masked.contains("$2a$10$abcdef123456789"));
        assertFalse(masked.contains("P@ssw0rd"));
        assertTrue(masked.contains("\"passwordHash\": \"******\""));
        assertTrue(masked.contains("\"matKhau\": \"******\""));
    }

    @Test
    @DisplayName("Che giấu token, accessToken và refreshToken")
    void shouldMaskTokens() {
        String json = "{\"token\": \"jwt.header.payload\", \"refreshToken\": \"rt_12345678\", \"accessToken\": \"at_abcdef\"}";
        String masked = SecretMasker.mask(json);

        assertFalse(masked.contains("jwt.header.payload"));
        assertFalse(masked.contains("rt_12345678"));
        assertFalse(masked.contains("at_abcdef"));
        assertTrue(masked.contains("\"token\": \"******\""));
        assertTrue(masked.contains("\"refreshToken\": \"******\""));
        assertTrue(masked.contains("\"accessToken\": \"******\""));
    }

    @Test
    @DisplayName("Che giấu apiKey, secretKey và chuoiBiMat")
    void shouldMaskApiKeysAndSecrets() {
        String text = "Cau hinh cong BHYT: apiKey=VN_BHYT_KEY_999, secretKey=sec_8888_secret, chuoiBiMat=top_secret_code";
        String masked = SecretMasker.mask(text);

        assertFalse(masked.contains("VN_BHYT_KEY_999"));
        assertFalse(masked.contains("sec_8888_secret"));
        assertFalse(masked.contains("top_secret_code"));
        assertTrue(masked.contains("apiKey=******"));
        assertTrue(masked.contains("secretKey=******"));
        assertTrue(masked.contains("chuoiBiMat=******"));
    }

    @Test
    @DisplayName("Che giấu Bearer Token trong header hoặc log message")
    void shouldMaskBearerTokens() {
        String text = "Outgoing request with header Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.e30.t-ID";
        String masked = SecretMasker.mask(text);

        assertFalse(masked.contains("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.e30.t-ID"));
        assertTrue(masked.contains("Bearer ******"));
    }

    @Test
    @DisplayName("Bảo toàn nội dung bình thường không có thông tin nhạy cảm")
    void shouldPreserveNonSensitiveContent() {
        String normalText = "User admin updated room PK-01 status to ACTIVE at 2026-11-07 10:00:00";
        String masked = SecretMasker.mask(normalText);

        assertEquals(normalText, masked);
    }

    @Test
    @DisplayName("Xử lý chuỗi rỗng hoặc null an toàn")
    void shouldHandleNullOrEmptyInputGracefully() {
        assertNull(SecretMasker.mask(null));
        assertEquals("", SecretMasker.mask(""));
        assertEquals("   ", SecretMasker.mask("   "));
    }
}
