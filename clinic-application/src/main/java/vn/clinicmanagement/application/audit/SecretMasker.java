package vn.clinicmanagement.application.audit;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tiện ích che giấu dữ liệu nhạy cảm (Secret Masking) trong nhật ký kiểm toán.
 * Che giấu tuyệt đối mật khẩu, token, chuỗi bí mật, api key, credential.
 */
public final class SecretMasker {

    public static final String MASKED_PLACEHOLDER = "******";

    // Pattern nhận diện các trường khóa nhạy cảm
    private static final String SENSITIVE_KEYS =
            "password|matKhau|token|secret|apiKey|secretKey|accessToken|refreshToken|bearerToken|passwordHash|credential|privateKey|chuoiBiMat";

    // 1. JSON string pattern: "password": "value" hoặc 'password': 'value'
    private static final Pattern JSON_STRING_PATTERN = Pattern.compile(
            "(?i)(\"|')([^\"]*?(?:" + SENSITIVE_KEYS + ")[^\"]*?)(\"|')\\s*:\\s*(\"|')(.*?)(\"|')",
            Pattern.DOTALL
    );

    // 2. JSON non-string / unquoted pattern: "password": 123456 hoặc "token": null
    private static final Pattern JSON_UNQUOTED_PATTERN = Pattern.compile(
            "(?i)(\"|')([^\"]*?(?:" + SENSITIVE_KEYS + ")[^\"]*?)(\"|')\\s*:\\s*([^,\"\\}\\]\\s]+)"
    );

    // 3. Key-Value query string / form / text pattern: password=secret123
    private static final Pattern KV_EQUALS_PATTERN = Pattern.compile(
            "(?i)\\b([a-zA-Z0-9_.-]*(?:" + SENSITIVE_KEYS + ")[a-zA-Z0-9_.-]*)\\s*=\\s*([^\\s,;&\"']+)"
    );

    // 4. Plain text colon pattern: password: secret123
    private static final Pattern KV_COLON_PATTERN = Pattern.compile(
            "(?i)\\b([a-zA-Z0-9_.-]*(?:" + SENSITIVE_KEYS + ")[a-zA-Z0-9_.-]*)\\s*:\\s+([^\\s,;&\"']+)"
    );

    // 5. Bearer token pattern: Bearer eyJhbGciOi...
    private static final Pattern BEARER_PATTERN = Pattern.compile(
            "(?i)\\bBearer\\s+([A-Za-z0-9\\-_.]+)"
    );

    private SecretMasker() {}

    /**
     * Che giấu các giá trị nhạy cảm trong chuỗi văn bản hoặc JSON.
     *
     * @param input Chuỗi văn bản/JSON thô cần làm sạch
     * @return Chuỗi đã được che giấu bí mật
     */
    public static String mask(String input) {
        if (input == null || input.isBlank()) {
            return input;
        }

        String result = input;

        // Mask JSON quoted values: "key": "value" -> "key": "******"
        Matcher jsonStringMatcher = JSON_STRING_PATTERN.matcher(result);
        if (jsonStringMatcher.find()) {
            result = jsonStringMatcher.replaceAll("$1$2$3: $4" + MASKED_PLACEHOLDER + "$6");
        }

        // Mask JSON unquoted values: "key": 12345 -> "key": "******"
        Matcher jsonUnquotedMatcher = JSON_UNQUOTED_PATTERN.matcher(result);
        if (jsonUnquotedMatcher.find()) {
            result = jsonUnquotedMatcher.replaceAll("$1$2$3: \"" + MASKED_PLACEHOLDER + "\"");
        }

        // Mask Bearer tokens: Bearer abc -> Bearer ******
        Matcher bearerMatcher = BEARER_PATTERN.matcher(result);
        if (bearerMatcher.find()) {
            result = bearerMatcher.replaceAll("Bearer " + MASKED_PLACEHOLDER);
        }

        // Mask Key=Value: password=secret -> password=******
        Matcher kvEqualsMatcher = KV_EQUALS_PATTERN.matcher(result);
        if (kvEqualsMatcher.find()) {
            result = kvEqualsMatcher.replaceAll("$1=" + MASKED_PLACEHOLDER);
        }

        // Mask Plain text colon: password: secret -> password: ******
        Matcher kvColonMatcher = KV_COLON_PATTERN.matcher(result);
        if (kvColonMatcher.find()) {
            result = kvColonMatcher.replaceAll("$1: " + MASKED_PLACEHOLDER);
        }

        return result;
    }
}
