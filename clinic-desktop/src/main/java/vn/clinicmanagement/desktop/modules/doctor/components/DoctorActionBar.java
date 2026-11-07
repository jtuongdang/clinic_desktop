package vn.clinicmanagement.desktop.modules.doctor.components;

import vn.clinicmanagement.desktop.core.config.AppFonts;
import vn.clinicmanagement.desktop.modules.doctor.model.PatientExamDto;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Thanh công cụ nghiệp vụ Bác sĩ và Banner tóm tắt bệnh nhân
 */
public class DoctorActionBar extends JPanel {

    private final JLabel lblBannerLine1;
    private final JLabel lblBannerLine2;
    private final JLabel lblBannerLine3;
    private final JLabel lblBannerBhytRate;

    public DoctorActionBar() {
        setLayout(new BorderLayout());
        setBackground(new Color(245, 247, 250));
        setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(205, 212, 222)));

        // Nút chức năng bác sĩ bên trái
        JPanel doctorActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 2));
        doctorActions.setOpaque(false);

        doctorActions.add(createActionButton("Menu", "☰", false, null));
        doctorActions.add(createActionButton("In", "🖨️", true, new Color(254, 237, 208))); // Nút "In" viền vàng đặc trưng
        doctorActions.add(createActionButton("Làm mới", "🔄", false, null));
        doctorActions.add(createActionButton("Tìm Kiếm", "🔍", false, null));
        doctorActions.add(createActionButton("Bệnh Án", "📋", false, null));
        doctorActions.add(createActionButton("Dịch Vụ", "🧪", false, null));
        doctorActions.add(createActionButton("Thuốc/VT", "💊", false, null));
        doctorActions.add(createActionButton("Phác Đồ ĐT", "📝", false, null));
        doctorActions.add(createActionButton("Thanh Toán", "💳", false, null));

        add(doctorActions, BorderLayout.WEST);

        // Banner thông tin tóm tắt bệnh nhân bên phải
        JPanel bannerPanel = new JPanel(new BorderLayout(8, 0));
        bannerPanel.setOpaque(false);
        bannerPanel.setBorder(new EmptyBorder(3, 10, 3, 20));

        JPanel bannerText = new JPanel(new GridLayout(3, 1, 0, 2));
        bannerText.setOpaque(false);

        lblBannerLine1 = new JLabel("Chưa chọn bệnh nhân", SwingConstants.RIGHT);
        lblBannerLine1.setFont(AppFonts.SECTION);
        lblBannerLine1.setForeground(new Color(139, 20, 20)); // Red bold

        lblBannerLine2 = new JLabel("-", SwingConstants.RIGHT);
        lblBannerLine2.setFont(AppFonts.BODY);
        lblBannerLine2.setForeground(new Color(139, 20, 20));

        lblBannerLine3 = new JLabel("", SwingConstants.RIGHT);
        lblBannerLine3.setFont(AppFonts.BODY);
        lblBannerLine3.setForeground(new Color(139, 20, 20));

        bannerText.add(lblBannerLine1);
        bannerText.add(lblBannerLine2);
        bannerText.add(lblBannerLine3);

        // Khối BHYT to rõ ràng
        JPanel bhytBox = new JPanel(new GridLayout(2, 1));
        bhytBox.setOpaque(false);
        JLabel lblBhytTag = new JLabel("BHYT", SwingConstants.CENTER);
        lblBhytTag.setFont(AppFonts.BODY_BOLD);
        lblBhytTag.setForeground(new Color(139, 20, 20));

        lblBannerBhytRate = new JLabel("-", SwingConstants.CENTER);
        lblBannerBhytRate.setFont(AppFonts.HEADER);
        lblBannerBhytRate.setForeground(new Color(20, 40, 160)); // Navy bold

        bhytBox.add(lblBhytTag);
        bhytBox.add(lblBannerBhytRate);

        bannerPanel.add(bannerText, BorderLayout.CENTER);
        bannerPanel.add(bhytBox, BorderLayout.EAST);

        add(bannerPanel, BorderLayout.EAST);
    }

    public void updatePatient(PatientExamDto p) {
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

    private JButton createActionButton(String text, String icon, boolean highlighted, Color bg) {
        JButton btn = new JButton("<html><center>" + icon + "<br><span style='font-size:13px'>" + text + "</span></center></html>");
        btn.setFont(AppFonts.SMALL);
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
}
