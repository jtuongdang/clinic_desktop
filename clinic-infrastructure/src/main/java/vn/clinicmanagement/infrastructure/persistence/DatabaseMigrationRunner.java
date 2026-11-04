package vn.clinicmanagement.infrastructure.persistence;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * ============================================================================
 * TRÌNH THỰC THI MIGRATION TỰ ĐỘNG CHO SQL SERVER (DATABASE MIGRATION RUNNER)
 * ============================================================================
 * 
 * Chức năng:
 * 1. Tự động khởi tạo bảng theo dõi phiên bản: `__schema_migrations`.
 * 2. Tự động quét và thực thi tuần tự các file migration trong `migrations/`:
 *    - V1__Initial_Clinic_Schema.sql (Khởi tạo toàn bộ cấu trúc bảng, PK, FK, Index)
 *    - V2__Seed_Initial_Data.sql (Nạp dữ liệu danh mục ban đầu)
 *    - V3__Repair_Demo_Account_Passwords.sql (Sửa hash tài khoản demo)
 *    - V4__Enforce_Active_Patient_CCCD_Uniqueness.sql (Định danh CCCD duy nhất cho hồ sơ hoạt động)
 * 3. Đảm bảo tính Idempotent: Chỉ chạy những script chưa được áp dụng.
 */
public class DatabaseMigrationRunner {

    private static final String MIGRATION_TABLE = "__schema_migrations";

    private static final String[] MIGRATION_FILES = {
            "migrations/V1__Initial_Clinic_Schema.sql",
            "migrations/V2__Seed_Initial_Data.sql",
            "migrations/V3__Repair_Demo_Account_Passwords.sql",
            "migrations/V4__Enforce_Active_Patient_CCCD_Uniqueness.sql"
    };

    /**
     * Chạy toàn bộ migration đang chờ vào CSDL
     */
    public static boolean runMigrations() {
        System.out.println("=================================================================");
        System.out.println(" BẮT ĐẦU QUÁ TRÌNH MIGRATION CƠ SỞ DỮ LIỆU SQL SERVER");
        System.out.println("=================================================================");

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);

            // 1. Khởi tạo bảng tracking nếu chưa tồn tại
            initMigrationTable(conn);

            // 2. Chạy từng script migration
            for (String scriptPath : MIGRATION_FILES) {
                String scriptName = extractFileName(scriptPath);
                if (isMigrationApplied(conn, scriptName)) {
                    System.out.println(" [BỎ QUA] " + scriptName + " (Đã được áp dụng trước đó)");
                    continue;
                }

                System.out.println(" [THỰC THI] Đang chạy " + scriptName + "...");
                long startTime = System.currentTimeMillis();

                String sqlContent = loadScriptContent(scriptPath);
                if (sqlContent == null || sqlContent.trim().isEmpty()) {
                    System.err.println(" Không tìm thấy nội dung script: " + scriptPath);
                    conn.rollback();
                    return false;
                }

                executeSqlScript(conn, sqlContent);

                long duration = System.currentTimeMillis() - startTime;
                recordMigration(conn, scriptName, duration);
                conn.commit();
                System.out.println("  HOÀN THÀNH: " + scriptName + " (" + duration + " ms)");
            }

            conn.setAutoCommit(true);
            System.out.println("=================================================================");
            System.out.println(" MIGRATION THÀNH CÔNG RỰC RỠ! TẤT CẢ CÁC BẢNG ĐÃ ĐƯỢC TẠO & LIÊN KẾT.");
            System.out.println("=================================================================");
            return true;

        } catch (Exception ex) {
            System.err.println(" LỖI MIGRATION: " + ex.getMessage());
            ex.printStackTrace();
            return false;
        }
    }

    private static void initMigrationTable(Connection conn) throws SQLException {
        DatabaseMetaData meta = conn.getMetaData();
        try (ResultSet rs = meta.getTables(null, null, MIGRATION_TABLE, new String[]{"TABLE"})) {
            if (!rs.next()) {
                String createTableSql = "CREATE TABLE " + MIGRATION_TABLE + " ("
                        + "id INT IDENTITY(1,1) PRIMARY KEY, "
                        + "script_name NVARCHAR(255) NOT NULL UNIQUE, "
                        + "installed_on DATETIME2 NOT NULL DEFAULT GETDATE(), "
                        + "execution_time_ms BIGINT NOT NULL, "
                        + "success BIT NOT NULL DEFAULT 1"
                        + ");";
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute(createTableSql);
                }
                conn.commit();
                System.out.println(" [KHỞI TẠO] Đã tạo bảng quản lý migration: " + MIGRATION_TABLE);
            }
        }
    }

    private static boolean isMigrationApplied(Connection conn, String scriptName) throws SQLException {
        String sql = "SELECT COUNT(*) FROM " + MIGRATION_TABLE + " WHERE script_name = ? AND success = 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, scriptName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    private static void recordMigration(Connection conn, String scriptName, long duration) throws SQLException {
        String sql = "INSERT INTO " + MIGRATION_TABLE + " (script_name, execution_time_ms, success) VALUES (?, ?, 1)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, scriptName);
            ps.setLong(2, duration);
            ps.executeUpdate();
        }
    }

    private static String loadScriptContent(String path) {
        InputStream is = DatabaseMigrationRunner.class.getClassLoader().getResourceAsStream(path);
        if (is == null) {
            // Thử đọc từ classpath gốc
            if (path.startsWith("/")) {
                is = DatabaseMigrationRunner.class.getResourceAsStream(path);
            } else {
                is = DatabaseMigrationRunner.class.getResourceAsStream("/" + path);
            }
        }

        if (is == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
        return sb.toString();
    }

    static void executeSqlScript(Connection conn, String sqlContent) throws SQLException {
        // GO là dấu phân tách batch của sqlcmd, không phải cú pháp T-SQL gửi qua JDBC.
        // Chỉ tách khi GO đứng riêng trên một dòng; hỗ trợ cả LF và CRLF.
        List<String> batches = SqlServerBatchSplitter.split(sqlContent);
        try (Statement stmt = conn.createStatement()) {
            for (String batch : batches) {
                stmt.execute(batch);
            }
        }
    }

    private static String extractFileName(String path) {
        int idx = path.lastIndexOf('/');
        if (idx >= 0) return path.substring(idx + 1);
        return path;
    }

    public static void main(String[] args) {
        runMigrations();
    }
}
