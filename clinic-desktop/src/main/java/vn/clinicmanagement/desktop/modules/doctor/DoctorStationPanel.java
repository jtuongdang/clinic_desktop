package vn.clinicmanagement.desktop.modules.doctor;

import vn.clinicmanagement.desktop.core.config.AppFonts;
import vn.clinicmanagement.desktop.core.base.BaseViewPanel;
import vn.clinicmanagement.desktop.modules.doctor.components.DoctorActionBar;
import vn.clinicmanagement.desktop.modules.doctor.components.DoctorClinicalForm;
import vn.clinicmanagement.desktop.modules.doctor.components.DoctorHistoryPanel;
import vn.clinicmanagement.desktop.modules.doctor.components.DoctorQueueSidebar;
import vn.clinicmanagement.desktop.modules.doctor.model.PatientExamDto;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Màn hình Bàn Khám Bác Sĩ & Quản Lý Điều Trị (Doctor Station)
 * Đóng gói độc lập trong package vn.clinicmanagement.desktop.modules.doctor
 * Tách biệt hoàn toàn với Admin và các phân hệ khác để dễ mở rộng & bảo trì.
 */
public class DoctorStationPanel extends BaseViewPanel {

    private final List<PatientExamDto> patientList = new ArrayList<>();
    private DoctorActionBar actionBar;
    private DoctorQueueSidebar queueSidebar;
    private DoctorClinicalForm clinicalForm;
    private DoctorHistoryPanel historyPanel;

    public DoctorStationPanel() {
        super("treatment", "Điều Trị", "👨‍⚕️");
        initPatientData();
        buildUI();
        queueSidebar.selectPatient(1); // Mặc định chọn TEST BN 033 như trong ảnh mẫu
    }

    private void initPatientData() {
        patientList.add(new PatientExamDto("TEST BN 060", "18039321 - GD4", "Nữ", "1988", "30 tuổi",
                "Phường Lê Thanh Nghị, TP Hải Dương", "GD-4-21-21-212-00060", "80%", "01/01/18 - 31/12/18",
                "08:15 11/11/2018", "BS. Nguyễn Văn Tuấn", "Khoa Khám Bệnh > Phòng Nội 1", "Viêm họng cấp", "J02"));

        patientList.add(new PatientExamDto("TEST BN 033", "18039327 - GD4", "Nam", "1995", "23 tuổi",
                "Xã Việt Hồng, H. Thanh Hà, Hải Dương", "GD-4-21-21-212-12125", "80%", "01/01/18 - 31/12/18",
                "09:40 11/11/2018", "Lê Tiến Quân", "Ngoại Đầu cổ > P.Điều Trị Ngoại DC", "D21 - Bướu lành khác của mô liên kết và mô mềm", "D21"));

        patientList.add(new PatientExamDto("TEST BN 032", "18033325 - GD4", "Nữ", "1992", "26 tuổi",
                "Huyện Tứ Kỳ, Hải Dương", "GD-4-21-21-212-12032", "80%", "01/01/18 - 31/12/18",
                "08:45 11/11/2018", "Lê Tiến Quân", "Khoa Ngoại", "Viêm xoang hàm", "J01.0"));

        patientList.add(new PatientExamDto("TEST BN 031", "18039225 - GD4", "Nam", "1978", "40 tuổi",
                "Thị xã Kinh Môn, Hải Dương", "GD-4-21-21-212-12031", "80%", "01/01/18 - 31/12/18",
                "09:00 11/11/2018", "BS. Trần Văn Hùng", "Khoa Tai Mũi Họng", "Viêm amidan hốc mủ", "J03.9"));

        patientList.add(new PatientExamDto("TEST BN B30", "18039224 - GD4", "Nữ", "2000", "18 tuổi",
                "TP Hải Dương", "GD-4-21-21-212-12030", "80%", "01/01/18 - 31/12/18",
                "09:15 11/11/2018", "Lê Tiến Quân", "Khoa Ngoại", "Polyp mũi xoang", "J33.0"));

        patientList.add(new PatientExamDto("PHẠM QUANG XX", "18039323 - DN4", "Nam", "1991", "27 tuổi",
                "Huyện Nam Sách, Hải Dương", "DN-4-21-21-212-99999", "100%", "01/01/18 - 31/12/18",
                "09:30 11/11/2018", "Lê Tiến Quân", "Khoa Khám Bệnh", "Rối loạn tiền đình", "H81.0"));

        patientList.add(new PatientExamDto("PHẠM VĂN KHÁP", "18039229 - BT2", "Nam", "1965", "53 tuổi",
                "Huyện Cẩm Giàng, Hải Dương", "BT-2-21-21-212-88888", "100%", "01/01/18 - 31/12/18",
                "09:35 11/11/2018", "BS. Nguyễn Văn Tuấn", "Khoa Nội", "Tăng huyết áp", "I10"));

        patientList.add(new PatientExamDto("TRẦN QUỐC CHÍNH", "18038221 - HT2", "Nam", "1958", "60 tuổi",
                "TP Hải Dương", "HT-2-21-21-212-77777", "95%", "01/01/18 - 31/12/18",
                "09:50 11/11/2018", "Lê Tiến Quân", "Ngoại Đầu cổ", "Viêm khớp dạng thấp", "M06.9"));

        patientList.add(new PatientExamDto("NGUYỄN THỊ NHÀ", "18038790 - MS2", "Nữ", "1972", "46 tuổi",
                "Huyện Gia Lộc, Hải Dương", "MS-2-21-21-212-66666", "80%", "01/01/18 - 31/12/18",
                "10:00 11/11/2018", "BS. Trần Văn Hùng", "Khoa Mắt", "Đục thủy tinh thể", "H25"));

        patientList.add(new PatientExamDto("NGUYỄN KIẾN SƠN", "18038797 - KC2", "Nam", "1984", "34 tuổi",
                "Huyện Bình Giang, Hải Dương", "KC-2-21-21-212-55555", "80%", "01/01/18 - 31/12/18",
                "10:15 11/11/2018", "Lê Tiến Quân", "Khoa Ngoại", "Chấn thương phần mềm", "T14.0"));

        patientList.add(new PatientExamDto("NGUYỄN VĂN THÁI", "18038799 - GD4", "Nam", "1996", "22 tuổi",
                "TP Hải Dương", "GD-4-21-21-212-44444", "80%", "01/01/18 - 31/12/18",
                "10:20 11/11/2018", "BS. Nguyễn Văn Tuấn", "Khoa Khám Bệnh", "Viêm phế quản cấp", "J20.9"));
    }

    private void buildUI() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        // 1. Top Action Bar + Banner tóm tắt bệnh nhân
        actionBar = new DoctorActionBar();
        add(actionBar, BorderLayout.NORTH);

        // 2. Main Split: Cột trái (Hàng đợi) + Cột phải (Hồ sơ khám)
        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplit.setDividerLocation(250);
        mainSplit.setDividerSize(4);
        mainSplit.setBorder(null);

        // Cột trái
        queueSidebar = new DoctorQueueSidebar(patientList);
        queueSidebar.setOnPatientSelectedListener(this::onPatientChanged);
        mainSplit.setLeftComponent(queueSidebar);

        // Cột phải
        JPanel rightWorkPanel = new JPanel(new BorderLayout());
        rightWorkPanel.setBackground(Color.WHITE);

        JTabbedPane clinicalTabs = new JTabbedPane();
        clinicalTabs.setFont(AppFonts.BODY_BOLD);

        // Tab "Bệnh Nhân"
        JPanel patientTab = new JPanel(new BorderLayout(0, 4));
        patientTab.setBackground(Color.WHITE);
        patientTab.setBorder(new EmptyBorder(8, 12, 8, 12));

        clinicalForm = new DoctorClinicalForm();
        patientTab.add(clinicalForm, BorderLayout.NORTH);

        historyPanel = new DoctorHistoryPanel();
        patientTab.add(historyPanel, BorderLayout.CENTER);

        clinicalTabs.addTab("Bệnh Nhân", patientTab);
        clinicalTabs.addTab("Công Khám(1)", createPlaceholderTab("Nội dung chỉ định dịch vụ công khám & thủ thuật"));
        clinicalTabs.addTab("Thuốc(2)", createPlaceholderTab("Danh sách thuốc & đơn thuốc ngoại trú / nội trú"));
        clinicalTabs.addTab("Xử Trí", createPlaceholderTab("Phương án điều trị, phẫu thuật, chuyển viện hoặc hẹn tái khám"));
        clinicalTabs.addTab("Phản Hồi", createPlaceholderTab("Phản hồi lâm sàng & kết quả chẩn đoán cận lâm sàng"));

        rightWorkPanel.add(clinicalTabs, BorderLayout.CENTER);
        mainSplit.setRightComponent(rightWorkPanel);

        add(mainSplit, BorderLayout.CENTER);
    }

    private void onPatientChanged(PatientExamDto p) {
        if (p == null) return;
        actionBar.updatePatient(p);
        clinicalForm.updatePatient(p);
        historyPanel.updateHistory(p);
    }

    private JPanel createPlaceholderTab(String desc) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Color.WHITE);
        JLabel lbl = new JLabel("<html><center><h3>" + desc + "</h3><p style='color:gray'>Sẵn sàng kết nối dữ liệu khám bệnh EMR & chỉ định thuốc</p></center></html>", SwingConstants.CENTER);
        p.add(lbl, BorderLayout.CENTER);
        return p;
    }

    @Override
    public void onShow() {
        refreshData();
    }

    @Override
    public void refreshData() {
        // Tải danh sách bệnh nhân từ Application / Domain Service
    }
}
