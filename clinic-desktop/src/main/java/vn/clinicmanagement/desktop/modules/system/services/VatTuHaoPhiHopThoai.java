package vn.clinicmanagement.desktop.modules.system.services;

import vn.clinicmanagement.application.catalog.VatTuHaoPhiCauHinh;
import vn.clinicmanagement.desktop.core.session.NguoiDungPhien;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.function.Consumer;

public final class VatTuHaoPhiHopThoai extends JDialog {
    private final VatTuHaoPhiManHinh screen;
    public VatTuHaoPhiHopThoai(Window owner,DichVuDanhMucNguon source,String definition,String legacy,Consumer<VatTuHaoPhiCauHinh> accepted){
        super(owner,"EHC - Vật Tư Hao Phí  ----- Người dùng nhập: "+NguoiDungPhien.getInstance().getFullName()+" -----",ModalityType.DOCUMENT_MODAL);
        screen=new VatTuHaoPhiManHinh(source,definition,legacy,value->{accepted.accept(value);dispose();});setContentPane(screen);setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        var config=owner==null?getGraphicsConfiguration():owner.getGraphicsConfiguration();if(config==null)config=GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
        setBounds(DichVuCauHinhHopThoai.usableBounds(config.getBounds(),Toolkit.getDefaultToolkit().getScreenInsets(config)));
        addWindowListener(new WindowAdapter(){public void windowClosing(WindowEvent e){dispose();}});
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ESCAPE"),"closeMaterials");
        getRootPane().getActionMap().put("closeMaterials",new AbstractAction(){public void actionPerformed(ActionEvent e){dispose();}});
    }
    @Override public void dispose(){if(screen!=null&&!screen.canClose())return;if(screen!=null)screen.dispose();super.dispose();}
}
