package vn.clinicmanagement.desktop.modules.system.services;

import vn.clinicmanagement.desktop.core.session.NguoiDungPhien;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/** Owned maximized dialog; closes with its System screen rather than creating a new app frame. */
public final class DichVuDanhMucHopThoai extends JDialog {
    private final DanhSachDichVuKyThuatManHinh screen;
    public DichVuDanhMucHopThoai(Window owner){
        super(owner,"EHC - Danh Sách Dịch Vụ — Người dùng nhập: "+java.util.Objects.toString(NguoiDungPhien.getInstance().getFullName(),""),ModalityType.APPLICATION_MODAL);
        screen=new DanhSachDichVuKyThuatManHinh();setContentPane(screen);setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        GraphicsConfiguration config=owner!=null?owner.getGraphicsConfiguration():getGraphicsConfiguration();
        if(config==null)config=GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
        Rectangle area=config.getBounds();Insets insets=Toolkit.getDefaultToolkit().getScreenInsets(config);
        int availW = area.width - insets.left - insets.right;
        int availH = area.height - insets.top - insets.bottom;
        setSize(Math.min(1260, (int) (availW * 0.90)), Math.min(760, (int) (availH * 0.88)));
        setLocationRelativeTo(owner);
        addWindowListener(new WindowAdapter(){@Override public void windowClosing(WindowEvent e){dispose();}});
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ESCAPE"),"closeServiceList");
        getRootPane().getActionMap().put("closeServiceList",new AbstractAction(){@Override public void actionPerformed(ActionEvent e){dispose();}});
    }
    @Override public void setVisible(boolean visible){if(visible)screen.refreshData();super.setVisible(visible);}
    @Override public void dispose(){if(screen!=null&&!screen.canClose())return;if(screen!=null)screen.onHide();super.dispose();}
}
