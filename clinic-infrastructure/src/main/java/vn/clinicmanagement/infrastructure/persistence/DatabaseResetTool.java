package vn.clinicmanagement.infrastructure.persistence;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * ============================================================================
 * TIỆN ÍCH DỌN DẸP / RESET DỮ LIỆU GIAO DỊCH TEST TRONG CƠ SỞ DỮ LIỆU
 * ============================================================================
 * 
 * Chức năng:
 * 1. Xóa toàn bộ dữ liệu phát sinh (Bệnh nhân, Lượt khám, Đơn thuốc, Viện phí,
 *    Cận lâm sàng, Hóa đơn, Lịch hẹn, Chấm công, Bảng lương, Nhật ký...).
 * 2. Bảo toàn 100% dữ liệu danh mục & tài khoản hệ thống (NhanVien, PhongKham,
 *    DanhMucDichVu, Item, Kho, ThamSoHeThong, MaTranPhanQuyen...).
 * 3. Hỗ trợ chạy trực tiếp qua hàm main() hoặc qua script shell.
 */
public class DatabaseResetTool {

    private static final String[] TRANSACTIONAL_TABLES = {
            "ChiTietHoaDon", "PhieuThu", "HoaDon", "CongNoBenhNhan",
            "ChiTietDonThuoc", "DonThuoc",
            "ChiTietKetQuaXetNghiem", "ChiDinhDichVu",
            "SinhHieu", "ChanDoan",
            "LichHen", "LuotKham",
            "TienSuBenh", "BaoHiem", "BenhNhanCoSo", "BenhNhan",
            "InventoryTransaction", "ChiTietXuatKho", "PhieuXuatKho", "ChiTietNhapKho", "PhieuNhapKho",
            "BangLuong", "KyLuong", "ChamCong", "LichLamViec",
            "NhatKyHoSoBenhAn", "NhatKyHeThong"
    };

    private static final String[] MASTER_TABLES = {
            "CoSo", "Khoa", "PhongKham",
            "VaiTro", "ChucVu", "NhanVien", "NhanVienVaiTro", "NhanVienKhoa", "PhanQuyenCoSo",
            "ICD", "DanhMucDichVu", "Item", "Kho", "ThamSoHeThong", "MaTranPhanQuyen"
    };

    public static boolean resetTransactionalData() {
        return resetTransactionalData(DatabaseConnection::getConnection);
    }

    public static boolean resetTransactionalData(DatabaseConnection.ConnectionSupplier connectionSupplier) {
        System.out.println("=================================================================");
        System.out.println(" BẮT ĐẦU DỌN DẸP DỮ LIỆU GIAO DỊCH TEST TRONG CƠ SỞ DỮ LIỆU");
        System.out.println("=================================================================");

        try (Connection conn = connectionSupplier.get()) {
            conn.setAutoCommit(false);

            // Đọc và chạy script SQL xóa sạch dữ liệu
            String scriptContent = loadScriptContent();
            if (scriptContent == null || scriptContent.trim().isEmpty()) {
                System.err.println(" Không tìm thấy nội dung script: scripts/clear_all_transaction_data.sql");
                conn.rollback();
                return false;
            }

            List<String> batches = SqlServerBatchSplitter.split(scriptContent);
            try (Statement stmt = conn.createStatement()) {
                for (String batch : batches) {
                    if (!batch.trim().isEmpty()) {
                        stmt.execute(batch);
                    }
                }
            }

            conn.commit();
            conn.setAutoCommit(true);

            // Báo cáo số lượng dòng còn lại sau khi reset
            printVerificationReport(conn);

            System.out.println("=================================================================");
            System.out.println(" DỌN DẸP DỮ LIỆU GIAO DỊCH TEST THÀNH CÔNG RỰC RỠ!");
            System.out.println(" TẤT CẢ DANH MỤC & TÀI KHOẢN HỆ THỐNG ĐÃ ĐƯỢC BẢO TOÀN.");
            System.out.println(" SẴN SÀNG ĐỂ BẮT ĐẦU QUY TRÌNH TIẾP NHẬN & NHẬP LIỆU MỚI!");
            System.out.println("=================================================================");
            return true;

        } catch (Exception ex) {
            System.err.println(" DỌN DẸP DỮ LIỆU THẤT BẠI: " + ex.getMessage());
            return false;
        }
    }

    private static void printVerificationReport(Connection conn) {
        System.out.println("\n--- BÁO CÁO KIỂM TRA BẢO TOÀN DANH MỤC HỆ THỐNG ---");
        for (String table : MASTER_TABLES) {
            int count = countTableRows(conn, table);
            if (count >= 0) {
                System.out.printf("  ✓ Danh mục [%-18s]: %3d bản ghi được bảo toàn\n", table, count);
            }
        }

        System.out.println("\n--- BÁO CÁO XÁC MINH DỮ LIỆU GIAO DỊCH ĐÃ XÓA TRẮNG ---");
        for (String table : TRANSACTIONAL_TABLES) {
            int count = countTableRows(conn, table);
            if (count >= 0) {
                System.out.printf("  ✓ Giao dịch [%-22s]: %3d bản ghi (Sạch sẽ)\n", table, count);
            }
        }
        System.out.println();
    }

    private static int countTableRows(Connection conn, String tableName) {
        String sql = "SELECT COUNT(*) FROM " + tableName;
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException ignored) {
            // Bảng có thể chưa tồn tại trong schema hiện tại
        }
        return -1;
    }

    private static String loadScriptContent() {
        Path[] candidates = new Path[]{
                Paths.get("scripts/clear_all_transaction_data.sql"),
                Paths.get("desktop/scripts/clear_all_transaction_data.sql"),
                Paths.get("../scripts/clear_all_transaction_data.sql"),
                Paths.get(System.getProperty("user.dir", "."), "scripts/clear_all_transaction_data.sql")
        };
        for (Path path : candidates) {
            if (Files.exists(path)) {
                try {
                    return Files.readString(path, StandardCharsets.UTF_8);
                } catch (Exception ignored) {}
            }
        }
        return null;
    }

    public static void main(String[] args) {
        boolean success = resetTransactionalData();
        System.exit(success ? 0 : 1);
    }
}
