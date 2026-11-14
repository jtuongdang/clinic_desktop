package vn.clinicmanagement.infrastructure.persistence;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
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
public class CoSoDuLieuKetNoi {
    private static String CUSTOM_CONNECTION_STRING = "";
    private static final CoSoDuLieuCauHinh CONFIGURATION =
            new CoSoDuLieuCauHinh(CoSoDuLieuKetNoi::loadProperties, CoSoDuLieuKetNoi::loadEnvironment);

    private CoSoDuLieuKetNoi() {}

    /** Hàm `loadProperties`: tải `Properties`. */
    private static Properties loadProperties() {
        Properties properties = new Properties();
        try (InputStream input = CoSoDuLieuKetNoi.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (input != null) {
                try {
                    properties.load(input);
                } catch (Exception ignored) {
                    properties.clear();
                }
            }
        } catch (Exception ignored) {}
        if (properties.isEmpty()) {
            java.nio.file.Path[] candidates = new java.nio.file.Path[]{
                    java.nio.file.Paths.get("db.properties"),
                    java.nio.file.Paths.get(System.getProperty("user.dir", "."), "db.properties"),
                    java.nio.file.Paths.get("clinic-infrastructure/src/main/resources/db.properties")
            };
            for (java.nio.file.Path path : candidates) {
                if (java.nio.file.Files.exists(path)) {
                    try (InputStream fileInput = java.nio.file.Files.newInputStream(path)) {
                        properties.load(fileInput);
                        if (!properties.isEmpty()) {
                            break;
                        }
                    } catch (Exception ignored) {
                        properties.clear();
                    }
                }
            }
        }
        return properties;
    }

    /** Hàm `loadEnvironment`: tải `Environment`. */
    private static Map<String, String> loadEnvironment() {
        Map<String, String> environment = new HashMap<>();
        for (String key : new String[]{
                "DB_URL", "DB_USERNAME", "DB_PASSWORD", "DB_DRIVER", "DB_CONNECTION_STRING"}) {
            String value = System.getenv(key);
            if (value != null) {
                environment.put(key, value);
            }
        }
        return environment;
    }

    /**
     * Gán chuỗi kết nối động qua mã nguồn
     * @param connectionString Chuỗi JDBC URL đầy đủ (đã bao gồm user & password)
     */
    public static void setConnectionString(String connectionString) {
        CUSTOM_CONNECTION_STRING = connectionString != null ? connectionString.trim() : "";
        CONFIGURATION.setConnectionString(CUSTOM_CONNECTION_STRING);
    }

    /**
     * Gán thông tin kết nối tách rời
     */
    public static void configure(String url, String username, String password) {
        CONFIGURATION.configure(url, username, password);
    }

    /**
     * Lấy đối tượng Connection đang mở tới cơ sở dữ liệu
     * @return Connection
     * @throws SQLException nếu kết nối thất bại
     */
    public static Connection getConnection() throws SQLException {
        CONFIGURATION.setConnectionString(CUSTOM_CONNECTION_STRING);
        CoSoDuLieuCauHinh.ConnectionTarget target =
                CONFIGURATION.getConfiguration().connectionTarget();
        if (target.mode() == CoSoDuLieuCauHinh.ConnectionMode.UNCONFIGURED) {
            throw new SQLException("Chưa cấu hình CSDL. Hãy đặt DB_CONNECTION_STRING hoặc DB_URL/DB_USERNAME/DB_PASSWORD, hoặc tạo db.properties từ file mẫu.");
        }

        autoRegisterDriver(target.value());
        return openConnection(target, new DriverManagerConnectionFactory());
    }

    /** Hàm `openConnection`: mở kết nối. */
    static Connection openConnection(
            CoSoDuLieuCauHinh.ConnectionTarget target,
            ConnectionFactory connectionFactory) throws SQLException {
        return switch (target.mode()) {
            case FULL_CONNECTION_STRING, URL_ONLY -> connectionFactory.getConnection(target.value());
            case URL_WITH_CREDENTIALS ->
                    connectionFactory.getConnection(target.value(), target.username(), target.password());
            case UNCONFIGURED -> throw new SQLException(
                    "Chưa cấu hình CSDL. Hãy đặt DB_CONNECTION_STRING hoặc DB_URL/DB_USERNAME/DB_PASSWORD, hoặc tạo db.properties từ file mẫu.");
        };
    }

    private static final class DriverManagerConnectionFactory implements ConnectionFactory {
        /** Hàm `getConnection`: lấy kết nối. Gốc khai báo tại `ConnectionFactory#getConnection`. */
        @Override
        public Connection getConnection(String connectionString) throws SQLException {
            return DriverManager.getConnection(connectionString);
        }

        /** Hàm `getConnection`: lấy kết nối. Gốc khai báo tại `ConnectionFactory#getConnection`. */
        @Override
        public Connection getConnection(String url, String username, String password) throws SQLException {
            return DriverManager.getConnection(url, username, password);
        }
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
        return testConnection(CoSoDuLieuKetNoi::getConnection);
    }

    /** Hàm `testConnection`: test kết nối. */
    static boolean testConnection(ConnectionSupplier connectionSupplier) {
        System.out.println("--------------------------------------------------");
        System.out.println("Đang kiểm tra kết nối tới cơ sở dữ liệu...");
        try (Connection conn = connectionSupplier.get()) {
            if (conn != null && !conn.isClosed()) {
                System.out.println(" KẾT NỐI THÀNH CÔNG!");
                System.out.println("--------------------------------------------------");
                return true;
            }
        } catch (SQLException exception) {
            printConnectionFailure();
            return false;
        }
        printConnectionFailure();
        return false;
    }

    /** Hàm `printConnectionFailure`: in kết nối thất bại. */
    private static void printConnectionFailure() {
        System.err.println(" KẾT NỐI THẤT BẠI!");
        System.out.println("--------------------------------------------------");
    }

    @FunctionalInterface
    interface ConnectionSupplier {
        /** Hàm `get`: lấy. */
        Connection get() throws SQLException;
    }

    interface ConnectionFactory {
        /** Hàm `getConnection`: lấy kết nối. */
        Connection getConnection(String connectionString) throws SQLException;

        /** Hàm `getConnection`: lấy kết nối. */
        Connection getConnection(String url, String username, String password) throws SQLException;
    }

    /**
     * Hàm main hỗ trợ bạn chạy kiểm tra nhanh kết nối độc lập
     */
    public static void main(String[] args) {
        testConnection();
    }
}
