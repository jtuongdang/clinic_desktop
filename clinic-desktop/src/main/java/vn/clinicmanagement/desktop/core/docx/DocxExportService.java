package vn.clinicmanagement.desktop.core.docx;

import vn.clinicmanagement.domain.examination.LuotKham;
import vn.clinicmanagement.domain.patient.BenhNhan;
import vn.clinicmanagement.domain.prescription.ChiTietDonThuoc;
import vn.clinicmanagement.domain.prescription.DonThuoc;
import vn.clinicmanagement.domain.service.ChiDinhDichVu;
import vn.clinicmanagement.domain.billing.HoaDon;

import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Dịch vụ tạo và xuất khẩu các biểu mẫu y tế chính thức sang định dạng Word (.docx).
 * Tuân thủ biểu mẫu của Bộ Y Tế Việt Nam.
 */
public class DocxExportService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    /**
     * Xuất Đơn thuốc điện tử chuẩn Bộ Y Tế ra file DOCX
     */
    public DocxDocument buildPrescriptionDocx(BenhNhan patient, LuotKham visit, DonThuoc prescription, String clinicName, String doctorName) {
        DocxDocument doc = new DocxDocument();

        // 1. Header cơ sở khám chữa bệnh
        doc.addParagraph(clinicName != null ? clinicName.toUpperCase() : "PHÒNG KHÁM ĐA KHOA QUỐC TẾ EHC", true, false, "left", 22);
        doc.addParagraph("Khoa Khám Bệnh - Bàn Khám Bác Sĩ", false, true, "left", 20);
        doc.addParagraph("--------------------------------------------------------------------------------", false, false, "center", 18);

        // 2. Tiêu đề đơn thuốc
        doc.addTitle("ĐƠN THUỐC ĐIỆN TỬ");
        doc.addParagraph("(Ban hành kèm theo Thông tư số 04/2022/TT-BYT của Bộ Y Tế)", false, true, "center", 20);
        doc.addParagraph("", false, false, "left", 14);

        // 3. Thông tin hành chính bệnh nhân
        String fullName = (patient != null) ? patient.getHoTen() : "Nguyễn Văn A";
        String ageStr = (patient != null) ? String.valueOf(patient.getTuoi()) : "30";
        String genderStr = (patient != null && patient.getGioiTinh() != null) ? patient.getGioiTinh().getDescription() : "Nam";
        String address = (patient != null && patient.getDiaChi() != null) ? patient.getDiaChi() : "---";
        String cccd = (patient != null && patient.getSoCCCD() != null) ? patient.getSoCCCD() : "---";
        String bnCode = (patient != null && patient.getMaBenhNhanFormat() != null && !patient.getMaBenhNhanFormat().isBlank()) 
                ? patient.getMaBenhNhanFormat() : ("BN-" + (patient != null ? patient.getId() : "001"));

        doc.addKeyValueParagraph("Họ và tên người bệnh", fullName + "    |    Mã BN: " + bnCode);
        doc.addKeyValueParagraph("Tuổi", ageStr + " tuổi    |    Giới tính: " + genderStr);
        doc.addKeyValueParagraph("Số CCCD/Định danh", cccd);
        doc.addKeyValueParagraph("Địa chỉ", address);

        String diagnosis = (visit != null && !visit.getChanDoans().isEmpty()) 
                ? visit.getChanDoans().get(0).getChanDoanText() : "Chẩn đoán xác định";
        doc.addKeyValueParagraph("Chẩn đoán", diagnosis);
        doc.addParagraph("", false, false, "left", 14);

        // 4. Lưới danh mục thuốc chỉ định
        doc.addHeading1("DANH MỤC THUỐC ĐIỀU TRỊ:");
        DocxTable table = new DocxTable(true);
        table.setColumnWidthsPercent(8, 42, 12, 18, 20);
        table.addRow("STT", "Tên Thuốc / Hoạt Chất / Hàm Lượng", "ĐVT", "Số Lượng", "Cách Dùng");

        if (prescription != null && !prescription.getChiTietDonThuocs().isEmpty()) {
            List<ChiTietDonThuoc> list = prescription.getChiTietDonThuocs();
            for (int i = 0; i < list.size(); i++) {
                ChiTietDonThuoc ct = list.get(i);
                String itemName = (ct.getItem() != null) ? ct.getItem().getTenItem() : ("Thuốc #" + ct.getMaItem());
                String donVi = (ct.getItem() != null) ? ct.getItem().getDonViCoBan() : "Viên";
                String qty = ct.getSoLuongKe() != null ? ct.getSoLuongKe().stripTrailingZeros().toPlainString() : "0";
                
                String cachDung = ct.getDuongDung() != null ? ct.getDuongDung() : "Uống";
                if (ct.getLieuDung() != null && !ct.getLieuDung().isBlank()) {
                    cachDung += " (" + ct.getLieuDung() + ")";
                }
                if (ct.getHuongDan() != null && !ct.getHuongDan().isBlank()) {
                    cachDung += " - " + ct.getHuongDan();
                }

                table.addRow(String.valueOf(i + 1), itemName, donVi, qty, cachDung);
            }
        } else {
            table.addRow("1", "Paracetamol 500mg", "Viên", "10", "Uống khi sốt > 38.5 độ");
            table.addRow("2", "Amoxicillin 500mg", "Viên", "14", "Uống ngày 2 lần, mỗi lần 1 viên sau ăn");
        }
        doc.addTable(table);
        doc.addParagraph("", false, false, "left", 14);

        // 5. Lời dặn của Bác sĩ
        String advice = (prescription != null && prescription.getLoiDanDonThuoc() != null && !prescription.getLoiDanDonThuoc().isBlank())
                ? prescription.getLoiDanDonThuoc() : "Uống thuốc đúng giờ, tái khám khi có dấu hiệu bất thường.";
        doc.addKeyValueParagraph("Lời dặn của Bác sĩ", advice);
        doc.addParagraph("", false, false, "left", 14);

        // 6. Chữ ký xác thực
        doc.addParagraph("Ngày ..... tháng ..... năm 20...", false, true, "right", 22);
        doc.addParagraph("BÁC SĨ KHÁM BỆNH", true, false, "right", 24);
        doc.addParagraph("(Ký, ghi rõ họ tên)", false, true, "right", 20);
        doc.addParagraph("\n\n", false, false, "right", 20);
        doc.addParagraph(doctorName != null ? doctorName : "BS. Điều Trị", true, false, "right", 24);

        doc.addParagraph("Khám lại xin mang theo đơn này.", false, true, "center", 18);
        return doc;
    }

    /**
     * Xuất Bệnh án Ngoại trú Mẫu 01/BV
     */
    public DocxDocument buildOutpatientMedicalRecordDocx(BenhNhan patient, LuotKham visit, String clinicName, String doctorName) {
        DocxDocument doc = new DocxDocument();

        doc.addParagraph(clinicName != null ? clinicName.toUpperCase() : "PHÒNG KHÁM ĐA KHOA QUỐC TẾ EHC", true, false, "left", 22);
        doc.addTitle("BỆNH ÁN NGOẠI TRÚ");
        doc.addParagraph("(Mẫu số 01/BV - Ban hành theo quy định Bộ Y Tế)", false, true, "center", 20);
        doc.addParagraph("", false, false, "left", 14);

        // I. Phần Hành Chính
        doc.addHeading1("I. HÀNH CHÍNH");
        doc.addKeyValueParagraph("1. Họ và tên người bệnh", patient != null ? patient.getHoTen().toUpperCase() : "NGUYỄN VĂN A");
        doc.addKeyValueParagraph("2. Ngày sinh", (patient != null && patient.getNgaySinh() != null) ? patient.getNgaySinh().format(DATE_FMT) : "---");
        doc.addKeyValueParagraph("3. Giới tính", (patient != null && patient.getGioiTinh() != null) ? patient.getGioiTinh().getDescription() : "---");
        doc.addKeyValueParagraph("4. Dân tộc", patient != null ? patient.getDanToc() : "Kinh");
        doc.addKeyValueParagraph("5. Nghề nghiệp", patient != null ? patient.getNgheNghiep() : "---");
        doc.addKeyValueParagraph("6. Địa chỉ", patient != null ? patient.getDiaChi() : "---");
        doc.addKeyValueParagraph("7. Đối tượng", visit != null ? visit.getDoiTuongKham().getDescription() : "Viện phí");

        // II. Lý Do Đến Khám & Bệnh Sử
        doc.addHeading1("II. LÝ DO ĐẾN KHÁM VÀ BỆNH SỬ");
        doc.addKeyValueParagraph("1. Lý do vào viện", visit != null ? visit.getLyDoKham() : "");
        doc.addKeyValueParagraph("2. Quá trình bệnh lý", (visit != null && !visit.getQuaTrinhBenhLy().isBlank()) ? visit.getQuaTrinhBenhLy() : "Khởi phát bệnh 2 ngày trước, triệu chứng tăng dần.");
        doc.addKeyValueParagraph("3. Tiền sử bản thân", (visit != null && !visit.getTienSuBanThan().isBlank()) ? visit.getTienSuBanThan() : "Chưa phát hiện bệnh mạn tính.");
        doc.addKeyValueParagraph("4. Tiền sử gia đình", (visit != null && !visit.getTienSuGiaDinh().isBlank()) ? visit.getTienSuGiaDinh() : "Khỏe mạnh.");

        // III. Khám Bệnh
        doc.addHeading1("III. KHÁM BỆNH");
        doc.addKeyValueParagraph("1. Khám toàn thân", (visit != null && !visit.getKhamToanThan().isBlank()) ? visit.getKhamToanThan() : "Bệnh nhân tỉnh táo, tiếp xúc tốt, da niêm mạc hồng hào.");
        doc.addKeyValueParagraph("2. Khám các bộ phận", (visit != null && !visit.getKhamBoPhan().isBlank()) ? visit.getKhamBoPhan() : "Tim đều, phổi trong không rale, bụng mềm.");
        doc.addKeyValueParagraph("3. Tóm tắt bệnh án", (visit != null && !visit.getTomTatBenhAn().isBlank()) ? visit.getTomTatBenhAn() : "Bệnh nhân nam vào viện vì triệu chứng điển hình.");

        // IV. Chẩn Đoán & Điều Trị
        doc.addHeading1("IV. CHẨN ĐOÁN VÀ ĐIỀU TRỊ");
        String diagnosis = (visit != null && !visit.getChanDoans().isEmpty()) ? visit.getChanDoans().get(0).getChanDoanText() : "Chẩn đoán xác định";
        doc.addKeyValueParagraph("1. Chẩn đoán xác định", diagnosis);
        doc.addKeyValueParagraph("2. Hướng điều trị", (visit != null && !visit.getHuongDieuTri().isBlank()) ? visit.getHuongDieuTri() : "Điều trị ngoại trú theo đơn thuốc.");
        doc.addKeyValueParagraph("3. Kết quả điều trị", (visit != null && visit.getKetQuaDieuTri() != null) ? visit.getKetQuaDieuTri().getDescription() : "Đỡ / Giảm");
        doc.addKeyValueParagraph("4. Hướng xử trí", (visit != null && visit.getHuongXuTri() != null) ? visit.getHuongXuTri().getDescription() : "Cho về toa");

        // Chữ ký
        doc.addParagraph("\n", false, false, "left", 14);
        doc.addParagraph("BÁC SĨ KHÁM BỆNH", true, false, "right", 24);
        doc.addParagraph(doctorName != null ? doctorName : "BS. Điều Trị", true, false, "right", 24);

        return doc;
    }

    /**
     * Xuất Giấy Hẹn Tái Khám
     */
    public DocxDocument buildAppointmentSlipDocx(BenhNhan patient, LuotKham visit, String clinicName, String doctorName) {
        DocxDocument doc = new DocxDocument();
        doc.addParagraph(clinicName != null ? clinicName.toUpperCase() : "PHÒNG KHÁM ĐA KHOA QUỐC TẾ EHC", true, false, "left", 22);
        doc.addTitle("GIẤY HẸN KHÁM LẠI");
        doc.addParagraph("", false, false, "left", 14);

        doc.addKeyValueParagraph("Họ và tên người bệnh", patient != null ? patient.getHoTen() : "");
        doc.addKeyValueParagraph("Ngày sinh", (patient != null && patient.getNgaySinh() != null) ? patient.getNgaySinh().format(DATE_FMT) : "");
        doc.addKeyValueParagraph("Số điện thoại", patient != null ? patient.getDienThoai() : "");
        doc.addKeyValueParagraph("Địa chỉ", patient != null ? patient.getDiaChi() : "");

        String dateHen = (visit != null && visit.getNgayHenTaiKham() != null) 
                ? visit.getNgayHenTaiKham().format(DATE_FMT) : "Sau 07 ngày";
        doc.addKeyValueParagraph("Hẹn khám lại vào ngày", dateHen);
        doc.addKeyValueParagraph("Lý do hẹn tái khám", "Đánh giá lại đáp ứng điều trị và điều chỉnh phác đồ.");
        doc.addKeyValueParagraph("Bác sĩ hẹn khám", doctorName != null ? doctorName : "BS. Chuyên Khoa");

        doc.addParagraph("\n\nLưu ý: Bệnh nhân vui lòng mang theo Giấy hẹn này và Đơn thuốc khi đi tái khám.", false, true, "center", 20);
        return doc;
    }
}
