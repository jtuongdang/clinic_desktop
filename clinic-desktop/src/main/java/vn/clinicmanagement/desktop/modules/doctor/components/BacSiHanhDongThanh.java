package vn.clinicmanagement.desktop.modules.doctor.components;

import vn.clinicmanagement.desktop.core.config.UngDungPhongChu;
import vn.clinicmanagement.desktop.modules.doctor.model.BenhNhanKhamDuLieu;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Thanh công cụ nghiệp vụ Bác sĩ và Banner tóm tắt bệnh nhân
 */
public class BacSiHanhDongThanh extends JPanel {

    /** Trường `lblBannerLine1`: `lbl` `Banner` đóng `1`. */
    private final JLabel lblBannerLine1;
    /** Trường `lblBannerLine2`: `lbl` `Banner` đóng `2`. */
    private final JLabel lblBannerLine2;
    /** Trường `lblBannerLine3`: `lbl` `Banner` đóng `3`. */
    private final JLabel lblBannerLine3;
    private final JLabel lblBannerBhytRate;

    private final JButton btnMenu;
    /** Trường `btnPrint`: `btn` in. */
    private final JButton btnPrint;
    /** Trường `btnRefresh`: `btn` làm mới. */
    private final JButton btnRefresh;
    /** Trường `btnSearch`: `btn` tìm kiếm. */
    private final JButton btnSearch;
    /** Trường `btnMedicalRecord`: `btn` yte ghi nhận. */
    private final JButton btnMedicalRecord;
    /** Trường `btnService`: `btn` dịch vụ. */
    private final JButton btnService;
    /** Trường `btnMedicine`: `btn` thuốc. */
    private final JButton btnMedicine;
    private final JButton btnRegimen;
    /** Trường `btnPayment`: `btn` thanhtoan. */
    private final JButton btnPayment;
    /** Trường `onActionClickListener`: tải hành động `Click` bolangnghe. */
    private java.util.function.Consumer<String> onActionClickListener;

    public BacSiHanhDongThanh() {
        setLayout(new BorderLayout());
        setBackground(new Color(245, 247, 250));
        setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(205, 212, 222)));

        // Nút chức năng bác sĩ bên trái
        JPanel doctorActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 2));
        doctorActions.setOpaque(false);

        btnMenu = createActionButton("Menu", "☰", false, null);
        btnPrint = createActionButton("In", "🖨️", true, new Color(254, 237, 208)); // Nút "In" viền vàng đặc trưng
        btnRefresh = createActionButton("Làm mới", "🔄", false, null);
        btnSearch = createActionButton("Tìm Kiếm", "🔍", false, null);
        btnMedicalRecord = createActionButton("Bệnh Án", "📋", false, null);
        btnService = createActionButton("Dịch Vụ", "🧪", false, null);
        btnMedicine = createActionButton("Thuốc/VT", "💊", false, null);
        btnRegimen = createActionButton("Phác Đồ ĐT", "📝", false, null);
        btnPayment = createActionButton("Thanh Toán", "💳", false, null);

        btnMenu.addActionListener(e -> triggerAction("Menu"));
        btnPrint.addActionListener(e -> triggerAction("In"));
        btnRefresh.addActionListener(e -> triggerAction("Làm mới"));
        btnSearch.addActionListener(e -> triggerAction("Tìm Kiếm"));
        btnMedicalRecord.addActionListener(e -> triggerAction("Bệnh Án"));
        btnService.addActionListener(e -> triggerAction("Dịch Vụ"));
        btnMedicine.addActionListener(e -> triggerAction("Thuốc/VT"));
        btnRegimen.addActionListener(e -> triggerAction("Phác Đồ ĐT"));
        btnPayment.addActionListener(e -> triggerAction("Thanh Toán"));

        doctorActions.add(btnMenu);
        doctorActions.add(btnPrint);
        doctorActions.add(btnRefresh);
        doctorActions.add(btnSearch);
        doctorActions.add(btnMedicalRecord);
        doctorActions.add(btnService);
        doctorActions.add(btnMedicine);
        doctorActions.add(btnRegimen);
        doctorActions.add(btnPayment);

        add(doctorActions, BorderLayout.WEST);

        // Banner thông tin tóm tắt bệnh nhân bên phải
        JPanel bannerPanel = new JPanel(new BorderLayout(8, 0));
        bannerPanel.setOpaque(false);
        bannerPanel.setBorder(new EmptyBorder(3, 10, 3, 20));

        JPanel bannerText = new JPanel(new GridLayout(3, 1, 0, 2));
        bannerText.setOpaque(false);

        lblBannerLine1 = new JLabel("Chưa chọn bệnh nhân", SwingConstants.RIGHT);
        lblBannerLine1.setFont(UngDungPhongChu.SECTION);
        lblBannerLine1.setForeground(new Color(139, 20, 20)); // Red bold

        lblBannerLine2 = new JLabel("-", SwingConstants.RIGHT);
        lblBannerLine2.setFont(UngDungPhongChu.BODY);
        lblBannerLine2.setForeground(new Color(139, 20, 20));

        lblBannerLine3 = new JLabel("", SwingConstants.RIGHT);
        lblBannerLine3.setFont(UngDungPhongChu.BODY);
        lblBannerLine3.setForeground(new Color(139, 20, 20));

        bannerText.add(lblBannerLine1);
        bannerText.add(lblBannerLine2);
        bannerText.add(lblBannerLine3);

        // Khối BHYT to rõ ràng
        JPanel bhytBox = new JPanel(new GridLayout(2, 1));
        bhytBox.setOpaque(false);
        JLabel lblBhytTag = new JLabel("BHYT", SwingConstants.CENTER);
        lblBhytTag.setFont(UngDungPhongChu.BODY_BOLD);
        lblBhytTag.setForeground(new Color(139, 20, 20));

        lblBannerBhytRate = new JLabel("-", SwingConstants.CENTER);
        lblBannerBhytRate.setFont(UngDungPhongChu.HEADER);
        lblBannerBhytRate.setForeground(new Color(20, 40, 160)); // Navy bold

        bhytBox.add(lblBhytTag);
        bhytBox.add(lblBannerBhytRate);

        bannerPanel.add(bannerText, BorderLayout.CENTER);
        bannerPanel.add(bhytBox, BorderLayout.EAST);

        add(bannerPanel, BorderLayout.EAST);
    }

    /** Hàm `updatePatient`: cập nhật bệnh nhân. */
    public void updatePatient(BenhNhanKhamDuLieu p) {
        if (p == null) {
            lblBannerLine1.setText("Chưa chọn bệnh nhân");
            lblBannerLine2.setText("-");
            lblBannerLine3.setText("");
            lblBannerBhytRate.setText("-");
            return;
        }
        String name = p.getName() != null && !p.getName().isBlank() ? p.getName() : "-";
        String code = p.getShortCode() != null && !p.getShortCode().isBlank() ? p.getShortCode() : "-";
        String age = p.getAge() != null && !p.getAge().isBlank() ? " (" + p.getAge() + ")" : "";
        String birthYear = p.getBirthYear() != null && !p.getBirthYear().isBlank() ? p.getBirthYear() + age : "-";
        String gender = p.getGender() != null && !p.getGender().isBlank() ? p.getGender() : "-";
        lblBannerLine1.setText(name + " | " + code + " | " + birthYear + " | " + gender);
        lblBannerLine2.setText(p.getBhytCode() != null && !p.getBhytCode().isBlank() ? p.getBhytCode() : "-");
        lblBannerLine3.setText(p.getAddress() != null ? p.getAddress() : "");
        lblBannerBhytRate.setText(p.getBhytRate() != null && !p.getBhytRate().isBlank() ? p.getBhytRate() : "-");
    }

    /** Hàm `createActionButton`: tạo hành động nút. */
    private JButton createActionButton(String text, String icon, boolean highlighted, Color bg) {
        JButton btn = new JButton("<html><center>" + icon + "<br><span style='font-size:13px'>" + text + "</span></center></html>");
        btn.setFont(UngDungPhongChu.SMALL);
        btn.setFocusPainted(false);
        btn.setMargin(new Insets(2, 6, 2, 6));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        if (highlighted && bg != null) {
            btn.setBackground(bg);
            btn.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(225, 175, 90), 1),
                    new EmptyBorder(2, 4, 2, 4)
            ));
            btn.setOpaque(true);
        } else {
            btn.setBackground(new Color(245, 247, 250));
            btn.setBorder(BorderFactory.createEmptyBorder(3, 6, 3, 6));
            btn.setOpaque(false);
        }

        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (!highlighted) {
                    btn.setBackground(new Color(228, 235, 245));
                    btn.setOpaque(true);
                }
            }
            @Override
            public void mouseExited(MouseEvent e) {
                if (!highlighted) {
                    btn.setOpaque(false);
                }
            }
        });

        return btn;
    }

    /** Hàm `triggerAction`: `trigger` hành động. */
    private void triggerAction(String action) {
        if (onActionClickListener != null) {
            onActionClickListener.accept(action);
        }
    }

    /** Hàm `setOnActionClickListener`: gán tải hành động `Click` bolangnghe. */
    public void setOnActionClickListener(java.util.function.Consumer<String> listener) {
        this.onActionClickListener = listener;
    }

    /** Hàm `getBtnMenu`: lấy `Btn` menu. */
    public JButton getBtnMenu() { return btnMenu; }
    /** Hàm `getBtnPrint`: lấy `Btn` in. */
    public JButton getBtnPrint() { return btnPrint; }
    /** Hàm `getBtnRefresh`: lấy `Btn` làm mới. */
    public JButton getBtnRefresh() { return btnRefresh; }
    /** Hàm `getBtnSearch`: lấy `Btn` tìm kiếm. */
    public JButton getBtnSearch() { return btnSearch; }
    /** Hàm `getBtnMedicalRecord`: lấy `Btn` yte ghi nhận. */
    public JButton getBtnMedicalRecord() { return btnMedicalRecord; }
    /** Hàm `getBtnService`: lấy `Btn` dịch vụ. */
    public JButton getBtnService() { return btnService; }
    /** Hàm `getBtnMedicine`: lấy `Btn` thuốc. */
    public JButton getBtnMedicine() { return btnMedicine; }
    /** Hàm `getBtnRegimen`: lấy `Btn` `Regimen`. */
    public JButton getBtnRegimen() { return btnRegimen; }
    /** Hàm `getBtnPayment`: lấy `Btn` thanhtoan. */
    public JButton getBtnPayment() { return btnPayment; }
}
