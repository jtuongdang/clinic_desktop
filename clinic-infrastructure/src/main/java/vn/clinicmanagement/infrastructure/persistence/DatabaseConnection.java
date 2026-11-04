package vn.clinicmanagement.infrastructure.persistence;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * ============================================================================
 * TRÌNH QUẢN LÝ KẾT NỐI CƠ SỞ DỮ LIỆU (DATABASE CONNECTION MANAGER)
 * ============================================================================
 * 
 * Hướng dẫn sử dụng:
 * 1. Cấu hình qua DB_CONNECTION_STRING hoặc DB_URL/DB_USERNAME/DB_PASSWORD.
 * 2. Hoặc tạo db.properties từ file db.properties.example (không commit mật khẩu).
 * 3. Hỗ trợ SQL Server, PostgreSQL, MySQL...
 * 4. Chạy trực tiếp hàm main() trong file này để kiểm tra kết nối ngay lập tức!
 */
public class DatabaseConnection {
    private static String CUSTOM_CONNECTION_STRING = "";

    // Cấu hình mặc định đọc từ file cấu hình hoặc biến môi trường
    private static String dbUrl;
    private static String dbUser;
    private static String dbPassword;
    private static String dbDriver;

    static {
        loadConfiguration();
    }

    private DatabaseConnection() {}

    /**
     * Nạp cấu hình từ file db.properties hoặc biến môi trường hệ thống
     */
    private static void loadConfiguration() {
        try (InputStream input = DatabaseConnection.class.getClassLoader().getResourceAsStream("db.properties")) {
            Properties prop = new Properties();
            if (input != null) {
                prop.load(input);
                dbUrl = prop.getProperty("db.url");
                dbUser = prop.getProperty("db.username");
                dbPassword = prop.getProperty("db.password");
                dbDriver = prop.getProperty("db.driver");
            }
        } catch (Exception ignored) {}

        // Biến môi trường ghi đè cấu hình file; không lưu secret trong source/artifact.
        overrideFromEnvironment("DB_URL", value -> dbUrl = value);
        overrideFromEnvironment("DB_USERNAME", value -> dbUser = value);
        overrideFromEnvironment("DB_PASSWORD", value -> dbPassword = value);
        overrideFromEnvironment("DB_DRIVER", value -> dbDriver = value);

        // Chuỗi đầy đủ có ưu tiên cao nhất.
        String envUrl = System.getenv("DB_CONNECTION_STRING");
        if (envUrl != null && !envUrl.trim().isEmpty()) {
            CUSTOM_CONNECTION_STRING = envUrl;
        }
    }

    private static void overrideFromEnvironment(String key, java.util.function.Consumer<String> setter) {
        String value = System.getenv(key);
        if (value != null && !value.isBlank()) setter.accept(value.trim());
    }

    /**
     * Gán chuỗi kết nối động qua mã nguồn
     * @param connectionString Chuỗi JDBC URL đầy đủ (đã bao gồm user & password)
     */
    public static void setConnectionString(String connectionString) {
        CUSTOM_CONNECTION_STRING = connectionString != null ? connectionString.trim() : "";
    }

    /**
     * Gán thông tin kết nối tách rời
     */
    public static void configure(String url, String username, String password) {
        dbUrl = url;
        dbUser = username;
        dbPassword = password;
    }

    /**
     * Lấy đối tượng Connection đang mở tới cơ sở dữ liệu
     * @return Connection
     * @throws SQLException nếu kết nối thất bại
     */
    public static Connection getConnection() throws SQLException {
        // 1. Ưu tiên sử dụng chuỗi kết nối gán trực tiếp
        if (CUSTOM_CONNECTION_STRING != null && !CUSTOM_CONNECTION_STRING.isEmpty()) {
            autoRegisterDriver(CUSTOM_CONNECTION_STRING);
            return DriverManager.getConnection(CUSTOM_CONNECTION_STRING);
        }

        // 2. Nếu không có chuỗi trực tiếp, dùng cấu hình từ db.properties
        if (dbUrl != null && !dbUrl.isEmpty()) {
            autoRegisterDriver(dbUrl);
            if (dbUser != null && !dbUser.isEmpty()) {
                return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
            }
            return DriverManager.getConnection(dbUrl);
        }

        throw new SQLException("Chưa cấu hình CSDL. Hãy đặt DB_CONNECTION_STRING hoặc DB_URL/DB_USERNAME/DB_PASSWORD, hoặc tạo db.properties từ file mẫu.");
    }

    /**
     * Tự động đăng ký Driver JDBC tương ứng với loại cơ sở dữ liệu
     */
    private static void autoRegisterDriver(String url) {
        try {
            if (url.startsWith("jdbc:sqlserver:")) {
                Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            } else if (url.startsWith("jdbc:postgresql:")) {
                Class.forName("org.postgresql.Driver");
            } else if (url.startsWith("jdbc:mysql:")) {
                Class.forName("com.mysql.cj.jdbc.Driver");
            } else if (url.startsWith("jdbc:oracle:")) {
                Class.forName("oracle.jdbc.OracleDriver");
            }
        } catch (ClassNotFoundException ignored) {
            // Driver sẽ tự nạp qua cơ chế ServiceLoader của JDBC 4.0+
        }
    }

    /**
     * Kiểm tra trạng thái kết nối tới cơ sở dữ liệu
     * @return true nếu kết nối thành công, false nếu lỗi
     */
    public static boolean testConnection() {
        System.out.println("--------------------------------------------------");
        System.out.println("Đang kiểm tra kết nối tới cơ sở dữ liệu...");
        try (Connection conn = getConnection()) {
            if (conn != null && !conn.isClosed()) {
                System.out.println(" KẾT NỐI THÀNH CÔNG!");
                System.out.println("  • Database Product: " + conn.getMetaData().getDatabaseProductName());
                System.out.println("  • Database Version: " + conn.getMetaData().getDatabaseProductVersion());
                System.out.println("  • Connected User   : " + conn.getMetaData().getUserName());
                System.out.println("--------------------------------------------------");
                return true;
            }
        } catch (SQLException e) {
            System.err.println(" KẾT NỐI THẤT BẠI!");
            System.err.println("  • Mã lỗi : " + e.getErrorCode());
            System.err.println("  • Chi tiết: " + e.getMessage());
            System.out.println("--------------------------------------------------");
        }
        return false;
    }

    /**
     * Hàm main hỗ trợ bạn chạy kiểm tra nhanh kết nối độc lập
     */
    public static void main(String[] args) {
        testConnection();
    }
}
