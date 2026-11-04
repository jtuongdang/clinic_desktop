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
public class DocxManager {

    private static final DocxExportService exportService = new DocxExportService();

    private DocxManager() {}

    public static DocxExportService getExportService() {
        return exportService;
    }

    /**
     * Mở hộp thoại chọn nơi lưu và xuất file DOCX Đơn thuốc
     */
    public static boolean promptAndExportPrescription(Component parent, BenhNhan patient, LuotKham visit, DonThuoc prescription, String clinicName, String doctorName) {
        String defaultFileName = "DonThuoc_" + (patient != null ? patient.getHoTen().replace(" ", "_") : "BenhNhan") + ".docx";
        File file = chooseSaveFile(parent, defaultFileName);
        if (file == null) return false;

        try {
            DocxDocument doc = exportService.buildPrescriptionDocx(patient, visit, prescription, clinicName, doctorName);
            doc.saveToFile(file);
            JOptionPane.showMessageDialog(parent, 
                    "Đã xuất đơn thuốc thành công ra file Word (.docx):\n" + file.getAbsolutePath(), 
                    "Xuất Tài Liệu Thành Công", 
                    JOptionPane.INFORMATION_MESSAGE);
            return true;
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(parent, 
                    "Lỗi khi lưu file DOCX: " + ex.getMessage(), 
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
            DocxDocument doc = exportService.buildOutpatientMedicalRecordDocx(patient, visit, clinicName, doctorName);
            doc.saveToFile(file);
            JOptionPane.showMessageDialog(parent, 
                    "Đã xuất bệnh án ngoại trú thành công ra file Word (.docx):\n" + file.getAbsolutePath(), 
                    "Xuất Tài Liệu Thành Công", 
                    JOptionPane.INFORMATION_MESSAGE);
            return true;
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(parent, 
                    "Lỗi khi lưu file DOCX: " + ex.getMessage(), 
                    "Lỗi Xuất File", 
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

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
