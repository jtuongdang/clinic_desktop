package vn.clinicmanagement.desktop.modules.examination.components.print;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.function.Supplier;

/** Treatment viewer opens across the usable area of its owner's monitor. */
public class ToDieuTriPrintHopThoai extends JDialog {
    private ToDieuTriPrintManHinh previewPanel;
    private SwingWorker<ToDieuTriPrintManHinh.TreatmentSheetData,Void> loader;

    public ToDieuTriPrintHopThoai(Window owner, ToDieuTriPrintManHinh.TreatmentSheetData data) {
        super(owner,"EHC - Bệnh Án Điện Tử — Tờ Điều Trị",ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);setResizable(true);
        if(data!=null)setPreview(data);else showMessage("Đang tải tờ điều trị...");
        fitToMonitor();
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ESCAPE"),"closeTreatment");
        getRootPane().getActionMap().put("closeTreatment",new AbstractAction(){@Override public void actionPerformed(ActionEvent e){dispose();}});
    }
    private void setPreview(ToDieuTriPrintManHinh.TreatmentSheetData data) {
        previewPanel=new ToDieuTriPrintManHinh(data);setContentPane(previewPanel);revalidate();repaint();
    }
    private void showMessage(String text) {
        JLabel message=new JLabel(text,SwingConstants.CENTER);
        message.setFont(vn.clinicmanagement.desktop.core.config.UngDungPhongChu.BODY);
        JPanel panel=new JPanel(new BorderLayout());panel.setBackground(Color.WHITE);panel.add(message);setContentPane(panel);revalidate();repaint();
    }
    private void fitToMonitor() {
        GraphicsConfiguration config=getOwner()!=null?getOwner().getGraphicsConfiguration():getGraphicsConfiguration();
        if(config==null)config=GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
        setBounds(usableBounds(config.getBounds(),Toolkit.getDefaultToolkit().getScreenInsets(config)));
    }
    static Rectangle usableBounds(Rectangle monitor,Insets insets) {
        return new Rectangle(monitor.x+insets.left,monitor.y+insets.top,
                Math.max(1,monitor.width-insets.left-insets.right),Math.max(1,monitor.height-insets.top-insets.bottom));
    }
    @Override public void setVisible(boolean visible) {if(visible)fitToMonitor();super.setVisible(visible);}
    public static void showDocument(Window owner,Supplier<ToDieuTriPrintManHinh.TreatmentSheetData> source) {
        ToDieuTriPrintHopThoai dialog=new ToDieuTriPrintHopThoai(owner,null);
        dialog.loader=new SwingWorker<>() {
            @Override protected ToDieuTriPrintManHinh.TreatmentSheetData doInBackground(){return source.get();}
            @Override protected void done(){
                if(isCancelled())return;
                try {
                    var data=get();
                    if(data==null){dialog.showMessage("Không tìm thấy dữ liệu lượt khám để lập tờ điều trị.");return;}
                    dialog.setPreview(data);
                }catch(Exception ex){dialog.showMessage("Không tải được đầy đủ tờ điều trị. Đóng cửa sổ và thử lại.");}
            }
        };
        dialog.loader.execute();dialog.setVisible(true);
    }
    @Override public void dispose(){if(loader!=null)loader.cancel(true);super.dispose();}
    public ToDieuTriPrintManHinh getPreviewPanel(){return previewPanel;}
}
