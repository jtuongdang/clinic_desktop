package vn.clinicmanagement.desktop.core.docx;

import vn.clinicmanagement.domain.examination.LuotKham;
import vn.clinicmanagement.domain.patient.BenhNhan;
import vn.clinicmanagement.domain.prescription.DonThuoc;

import java.awt.Component;
import javax.swing.*;
import java.io.File;
import java.io.IOException;

/**
 * Quản lý tập trung toàn bộ tác vụ xuất, lưu và tương tác với tài liệu Word DOCX trên Desktop UI.
 */
public class DOCXQuanLy {

    /** Trường `exportService`: xuất dịch vụ. */
    private static final DOCXExportDichVu exportService = new DOCXExportDichVu();

    private DOCXQuanLy() {}

    /** Hàm `getExportService`: lấy xuất dịch vụ. */
    public static DOCXExportDichVu getExportService() {
        return exportService;
    }

    /** Hàm `toUserFacingSaveFailureMessage`: den người dùng `Facing` lưu thất bại thông báo. */
    static String toUserFacingSaveFailureMessage(IOException ignored) {
        return "Không thể lưu tài liệu DOCX. Vui lòng chọn thư mục khác hoặc kiểm tra quyền truy cập và dung lượng lưu trữ, rồi thử lại.";
    }

    /**
     * Mở hộp thoại chọn nơi lưu và xuất file DOCX Đơn thuốc
     */
    public static boolean promptAndExportPrescription(Component parent, BenhNhan patient, LuotKham visit, DonThuoc prescription, String clinicName, String doctorName) {
        String defaultFileName = "DonThuoc_" + (patient != null ? patient.getHoTen().replace(" ", "_") : "BenhNhan") + ".docx";
        File file = chooseSaveFile(parent, defaultFileName);
        if (file == null) return false;

        try {
            DOCXTaiLieu doc = exportService.buildPrescriptionDocx(patient, visit, prescription, clinicName, doctorName);
            doc.saveToFile(file);
            JOptionPane.showMessageDialog(parent, 
                    "Đã xuất đơn thuốc thành công ra file Word (.docx):\n" + file.getAbsolutePath(), 
                    "Xuất Tài Liệu Thành Công", 
                    JOptionPane.INFORMATION_MESSAGE);
            return true;
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(parent, 
                    toUserFacingSaveFailureMessage(ex), 
                    "Lỗi Xuất File", 
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    /**
     * Mở hộp thoại chọn nơi lưu và xuất file DOCX Bệnh án ngoại trú
     */
    public static boolean promptAndExportMedicalRecord(Component parent, BenhNhan patient, LuotKham visit, String clinicName, String doctorName) {
        String defaultFileName = "BenhAnNgoaiTru_" + (patient != null ? patient.getHoTen().replace(" ", "_") : "BenhNhan") + ".docx";
        File file = chooseSaveFile(parent, defaultFileName);
        if (file == null) return false;

        try {
            DOCXTaiLieu doc = exportService.buildOutpatientMedicalRecordDocx(patient, visit, clinicName, doctorName);
            doc.saveToFile(file);
            JOptionPane.showMessageDialog(parent, 
                    "Đã xuất bệnh án ngoại trú thành công ra file Word (.docx):\n" + file.getAbsolutePath(), 
                    "Xuất Tài Liệu Thành Công", 
                    JOptionPane.INFORMATION_MESSAGE);
            return true;
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(parent, 
                    toUserFacingSaveFailureMessage(ex), 
                    "Lỗi Xuất File", 
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    /** Hàm `chooseSaveFile`: `choose` lưu `File`. */
    private static File chooseSaveFile(Component parent, String defaultFileName) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Chọn nơi lưu tài liệu DOCX");
        fileChooser.setSelectedFile(new File(defaultFileName));
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Microsoft Word Document (*.docx)", "docx"));

        int result = fileChooser.showSaveDialog(parent);
        if (result == JFileChooser.APPROVE_OPTION) {
            File f = fileChooser.getSelectedFile();
            if (!f.getName().toLowerCase().endsWith(".docx")) {
                f = new File(f.getParentFile(), f.getName() + ".docx");
            }
            return f;
        }
        return null;
    }
}
