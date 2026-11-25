package vn.clinicmanagement.desktop.modules.billing.examination;

import vn.clinicmanagement.desktop.modules.examination.KhamBenhManHinh.AdminPatientItem;
import vn.clinicmanagement.desktop.modules.examination.KhamBenhPhienNguCanh;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

/** Full usable monitor window; loading never blocks Swing's event thread. */
public final class ThanhToanKhamBenhHopThoai extends JDialog {
    private final ThanhToanKhamBenhManHinh panel=new ThanhToanKhamBenhManHinh(null);
    private final Supplier<ThanhToanKhamBenhDuLieu> source;
    private SwingWorker<ThanhToanKhamBenhDuLieu,Void> worker;
    public ThanhToanKhamBenhHopThoai(Window owner,Supplier<ThanhToanKhamBenhDuLieu> source){
        super(owner,"EHC - Thanh Toán Viện Phí",ModalityType.MODELESS);this.source=source;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);setContentPane(panel);panel.setRefreshAction(this::reload);fitToMonitor();
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ESCAPE"),"closePayment");getRootPane().getActionMap().put("closePayment",new AbstractAction(){public void actionPerformed(ActionEvent e){dispose();}});
    }
    private void fitToMonitor(){GraphicsConfiguration config=getOwner()!=null?getOwner().getGraphicsConfiguration():getGraphicsConfiguration();if(config==null)config=GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();Rectangle bounds=config.getBounds();Insets insets=Toolkit.getDefaultToolkit().getScreenInsets(config);setBounds(bounds.x+insets.left,bounds.y+insets.top,Math.max(1,bounds.width-insets.left-insets.right),Math.max(1,bounds.height-insets.top-insets.bottom));}
    @Override public void setVisible(boolean visible){if(visible){fitToMonitor();if(worker==null)reload();}super.setVisible(visible);}
    private void reload(){
        if(worker!=null&&!worker.isDone())return;panel.setLoading(true);
        worker=new SwingWorker<>(){
            protected ThanhToanKhamBenhDuLieu doInBackground(){return source.get();}
            protected void done(){if(isCancelled())return;try{panel.bindData(get());}catch(InterruptedException ex){Thread.currentThread().interrupt();panel.showError("Đã dừng tải thanh toán.");}catch(ExecutionException ex){Throwable cause=ex.getCause();panel.showError(cause instanceof IllegalArgumentException||cause instanceof IllegalStateException?cause.getMessage():"Không thể tải dữ liệu thanh toán. Bấm Làm Mới để thử lại.");}}
        };worker.execute();
    }
    @Override public void dispose(){if(worker!=null)worker.cancel(true);super.dispose();}
    public static void showForVisit(Component parent,AdminPatientItem patient){
        if(patient==null||patient.visitId<=0){JOptionPane.showMessageDialog(parent,"Chọn bệnh nhân và lượt khám để xem thanh toán.");return;}
        var auth=KhamBenhPhienNguCanh.current();if(auth==null){JOptionPane.showMessageDialog(parent,"Phiên đăng nhập không hợp lệ. Vui lòng đăng nhập lại.");return;}
        var dialog=new ThanhToanKhamBenhHopThoai(SwingUtilities.getWindowAncestor(parent),()->ThanhToanKhamBenhNguonDuLieu.load(patient,auth));
        dialog.getPaymentPanel().setPayerChangeHandler((invoiceId,lineId,targetPayer,svcPrice,insPrice,rate)->
                ThanhToanKhamBenhNguonDuLieu.changePayer(invoiceId,lineId,targetPayer,svcPrice,insPrice,rate,auth));
        dialog.setVisible(true);
    }
    public ThanhToanKhamBenhManHinh getPaymentPanel(){return panel;}
}
