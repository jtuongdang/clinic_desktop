package vn.clinicmanagement.desktop.modules.examination.components.common;

import vn.clinicmanagement.desktop.core.config.UngDungPhongChu;
import vn.clinicmanagement.desktop.modules.examination.KhamBenhManHinh.AdminPatientItem;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Banner thông tin hành chính bệnh nhân góc phải trên các màn hình chỉ định (Thuốc, Vật Tư).
 */
public class PatientOrderBannerPanel extends JPanel {

    private final JLabel lblLine1;
    private final JLabel lblLine2;
    private final JLabel lblLine3;
    private final JLabel lblInsuranceTag;

    public PatientOrderBannerPanel(String initialLine1, String initialLine2, String initialLine3) {
        super(new BorderLayout(8, 0));
        setOpaque(false);
        setBorder(new EmptyBorder(2, 6, 2, 12));

        JPanel textPanel = new JPanel(new GridLayout(3, 1, 0, 0));
        textPanel.setOpaque(false);

        lblLine1 = new JLabel(initialLine1, SwingConstants.RIGHT);
        lblLine1.setFont(UngDungPhongChu.SMALL_BOLD);
        lblLine1.setForeground(new Color(125, 20, 20));

        lblLine2 = new JLabel(initialLine2, SwingConstants.RIGHT);
        lblLine2.setFont(UngDungPhongChu.SMALL);
        lblLine2.setForeground(new Color(125, 20, 20));

        lblLine3 = new JLabel(initialLine3, SwingConstants.RIGHT);
        lblLine3.setFont(UngDungPhongChu.SMALL);
        lblLine3.setForeground(new Color(125, 20, 20));

        textPanel.add(lblLine1);
        textPanel.add(lblLine2);
        textPanel.add(lblLine3);

        lblInsuranceTag = new JLabel("<html><center><font color='#800000'><b>Viện<br>phí</b></font></center></html>", SwingConstants.CENTER);
        lblInsuranceTag.setFont(UngDungPhongChu.BODY_BOLD);
        lblInsuranceTag.setForeground(new Color(145, 20, 20));
        lblInsuranceTag.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 1, 0, 0, new Color(210, 220, 232)),
                new EmptyBorder(0, 10, 0, 4)));

        add(textPanel, BorderLayout.CENTER);
        add(lblInsuranceTag, BorderLayout.EAST);
    }

    public void bindPatient(AdminPatientItem patient) {
        if (patient == null) return;
        String name = ExaminationUiFactory.safeText(patient.name, "Bệnh nhân");
        String code = ExaminationUiFactory.safeText(patient.code, "");
        String birthYear = ExaminationUiFactory.safeText(patient.birthYear, "");
        String age = ExaminationUiFactory.safeText(patient.bannerAge, ExaminationUiFactory.safeText(patient.formAge, ""));
        String gender = ExaminationUiFactory.safeText(patient.gender, "");

        lblLine1.setText(name + " | " + code + " | " + birthYear + (age.isEmpty() ? "" : " (" + age + ")")
                + (gender.isEmpty() ? "" : " | " + gender));

        String requestType = ExaminationUiFactory.safeText(patient.requestType, "Viện phí");
        String bhytCode = ExaminationUiFactory.safeText(patient.bhytNumber, "");
        lblLine2.setText("Mã VP : " + ExaminationUiFactory.safeText(patient.invoiceCode, "")
                + (bhytCode.isEmpty() ? " | " + requestType : " | " + bhytCode));

        lblLine3.setText(ExaminationUiFactory.safeText(patient.bannerAddress, ExaminationUiFactory.safeText(patient.address, "")));

        boolean isBhyt = requestType.contains("BHYT") || requestType.contains("hiểm");
        int rate = patient.bhytRate > 0 ? patient.bhytRate : 80;
        lblInsuranceTag.setText(isBhyt
                ? "<html><center><font color='#800000'><b>BHYT</b></font><br><font color='#0044CC'><b>" + rate + "%</b></font></center></html>"
                : "<html><center><font color='#800000'><b>Viện<br>phí</b></font></center></html>");
    }

    public String getLine1Text() {
        return lblLine1.getText();
    }
}
