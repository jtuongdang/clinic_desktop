package vn.clinicmanagement.desktop.modules.print.examination;


import vn.clinicmanagement.desktop.modules.print.billing.*;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/** Phiếu chỉ định CLS mở trên toàn bộ vùng màn hình khả dụng. */
public class LamSangYeuCauPrintPreviewHopThoai extends JDialog {

    private final LamSangYeuCauPrintPreviewManHinh printPreviewPanel;

    public LamSangYeuCauPrintPreviewHopThoai(Window owner) {
        this(owner, null);
    }

    public LamSangYeuCauPrintPreviewHopThoai(Window owner, LamSangYeuCauPrintPreviewManHinh.ClinicalOrderData data) {
        super(owner, buildWindowTitle(data), ModalityType.MODELESS);

        setIconImage(createCrossIconImage());
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        fitToMonitor();
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ESCAPE"),"closeCls");
        getRootPane().getActionMap().put("closeCls",new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e){dispose();}
        });

        this.printPreviewPanel = new LamSangYeuCauPrintPreviewManHinh(data);
        setContentPane(printPreviewPanel);
    }

    private void fitToMonitor() {
        GraphicsConfiguration config=getOwner()!=null?getOwner().getGraphicsConfiguration():getGraphicsConfiguration();
        if(config==null)config=GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
        setBounds(ToDieuTriPrintHopThoai.usableBounds(config.getBounds(),Toolkit.getDefaultToolkit().getScreenInsets(config)));
    }
    @Override public void setVisible(boolean visible){if(visible)fitToMonitor();super.setVisible(visible);}

    private static String buildWindowTitle(LamSangYeuCauPrintPreviewManHinh.ClinicalOrderData data) {
        String patientSuffix = data != null && data.patientName() != null && !data.patientName().isBlank()
                ? " - " + data.patientName() + " (" + (data.patientCode() != null ? data.patientCode() : "") + ")"
                : "";
        return "EHC - Bệnh Án Điện Tử -- Phiếu Chỉ Định Cận Lâm Sàng" + patientSuffix;
    }

    public LamSangYeuCauPrintPreviewManHinh getPrintPreviewPanel() {
        return printPreviewPanel;
    }

    private static Image createCrossIconImage() {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(new Color(215, 45, 45));
        g2.fillRect(6, 2, 4, 12);
        g2.fillRect(2, 6, 12, 4);
        g2.dispose();
        return img;
    }
}
