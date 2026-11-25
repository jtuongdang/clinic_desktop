package vn.clinicmanagement.desktop.modules.system.services;

import vn.clinicmanagement.application.catalog.DichVuCauHinh;
import vn.clinicmanagement.desktop.core.session.NguoiDungPhien;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.function.Consumer;

/** Owned editor fills the usable monitor area, including when launched from the service-list dialog. */
public final class DichVuCauHinhHopThoai extends JDialog {
    private final DichVuCauHinhManHinh screen;
    public DichVuCauHinhHopThoai(Window owner,DichVuHang item,DichVuDanhMucNguon source,Consumer<DichVuCauHinh> saved){
        super(owner,"EHC - Cập Nhật Dịch Vụ  ----- Người dùng nhập: "+NguoiDungPhien.getInstance().getFullName()+" -----",ModalityType.DOCUMENT_MODAL);
        screen=new DichVuCauHinhManHinh(item,source,saved);setContentPane(screen);setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        var config=owner==null?getGraphicsConfiguration():owner.getGraphicsConfiguration();
        if(config==null)config=GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
        setBounds(usableBounds(config.getBounds(),Toolkit.getDefaultToolkit().getScreenInsets(config)));
        addWindowListener(new WindowAdapter(){@Override public void windowClosing(WindowEvent e){dispose();}});
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ESCAPE"),"closeEditor");
        getRootPane().getActionMap().put("closeEditor",new AbstractAction(){public void actionPerformed(ActionEvent e){dispose();}});
    }
    static Rectangle usableBounds(Rectangle monitor,Insets insets){return new Rectangle(monitor.x+insets.left,monitor.y+insets.top,
            monitor.width-insets.left-insets.right,monitor.height-insets.top-insets.bottom);}
    @Override public void dispose(){if(screen!=null&&!screen.canClose())return;if(screen!=null)screen.dispose();super.dispose();}
}
