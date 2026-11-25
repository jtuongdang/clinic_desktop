package vn.clinicmanagement.desktop.modules.reports;

import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.reports.get.*;
import vn.clinicmanagement.desktop.core.config.UngDungPhongChu;
import vn.clinicmanagement.domain.examination.LuotKham;
import vn.clinicmanagement.domain.common.enums.TrangThaiLuotKham;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.time.*;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import javax.imageio.ImageIO;
import static org.junit.jupiter.api.Assertions.*;

class BaoCaoThongKeBoCucTest {
    static PhanQuyenNguCanh auth(String role){return new PhanQuyenNguCanh(1,1,role,1,1,UUID.randomUUID());}
    static BaoCaoGhepNoiGoc.Services services(AtomicInteger calls){
        var defaults=BaoCaoGhepNoiGoc.createDefault();var visits=new GetLuotKhamThongKeHandler((from,to,facility,department)->{calls.incrementAndGet();assertEquals(1,facility);assertEquals(1,department);LuotKham visit=new LuotKham();visit.setId(1);visit.setMaBenhNhan(1);visit.setMaCoSo(1);visit.setMaKhoa(1);visit.setNgayKham(LocalDate.of(2026,11,20));visit.setThoiGianTiepNhan(LocalDateTime.of(2026,11,20,9,0));visit.setTrangThai(TrangThaiLuotKham.HOAN_THANH);return List.of(visit);});return BaoCaoGhepNoiGoc.create(visits,defaults.getRevenueReportHandler(),defaults.getInventoryReportHandler());
    }
    @Test void usesSameRegionsAndDensityAsTicketRegister()throws Exception{
        PhongKhamBaoCaoManHinh[] ref=new PhongKhamBaoCaoManHinh[1];SwingUtilities.invokeAndWait(()->{com.formdev.flatlaf.FlatLightLaf.setup();ref[0]=new PhongKhamBaoCaoManHinh(services(new AtomicInteger()),()->auth("ADMIN"));ref[0].setRegisterOpener(()->{});ref[0].getTxtFromDate().setText("01/11/2026");ref[0].getTxtToDate().setText("20/11/2026");ref[0].executeReportQuery();});await(ref[0]);
        SwingUtilities.invokeAndWait(()->{var panel=ref[0];var ui=panel.getReportUi();assertTrue(panel.getTableModel().getRowCount()>0);for(int[] size:new int[][]{{1440,760},{1366,680},{1586,900}}){panel.setSize(size[0],size[1]);layout(panel);layout(panel);assertEquals(58,ui.toolbar.getHeight());assertEquals(size[0]*.1875,ui.split.getDividerLocation(),2);assertEquals(24,ui.table.getRowHeight());assertEquals(UngDungPhongChu.BODY,ui.table.getFont());assertEquals(7,ui.table.getColumnCount());
                Insets in=ui.view.getInsets();Rectangle bounds=new Rectangle(in.left,in.top,ui.view.getWidth()-in.left-in.right,ui.view.getHeight()-in.top-in.bottom);
                String caption=SwingUtilities.layoutCompoundLabel(ui.view,ui.view.getFontMetrics(ui.view.getFont()),ui.view.getText(),ui.view.getIcon(),ui.view.getVerticalAlignment(),ui.view.getHorizontalAlignment(),ui.view.getVerticalTextPosition(),ui.view.getHorizontalTextPosition(),bounds,new Rectangle(),new Rectangle(),ui.view.getIconTextGap());assertEquals(ui.view.getText(),caption);assertTrue(ui.view.getParent().getX()+ui.view.getParent().getWidth()<=ui.toolbar.getWidth());var image=new BufferedImage(size[0],size[1],BufferedImage.TYPE_INT_RGB);var g=image.createGraphics();panel.paint(g);g.dispose();try{ImageIO.write(image,"png",Path.of("/tmp/statistics-report-"+size[0]+".png").toFile());}catch(Exception ex){throw new RuntimeException(ex);}}
            ui.keyword.setText("khong co chi tieu nay");assertEquals(0,ui.table.getRowCount());assertFalse(ui.export.isEnabled());ui.keyword.setText("");assertTrue(ui.table.getRowCount()>0);assertTrue(ui.export.isEnabled());assertFalse(ui.branch(3).isEnabled());AtomicInteger opened=new AtomicInteger();panel.setRegisterOpener(opened::incrementAndGet);ui.list.doClick();assertEquals(1,opened.get());
        });
    }
    @Test void invalidDatesAndUnsupportedGroupsDoNotQuery()throws Exception{AtomicInteger calls=new AtomicInteger();SwingUtilities.invokeAndWait(()->{var panel=new PhongKhamBaoCaoManHinh(services(calls),()->auth("ADMIN"));panel.getTxtFromDate().setText("31/02/2026");panel.executeReportQuery();assertEquals(0,calls.get());assertFalse(panel.getBtnExport().isEnabled());panel.getCboReportType().setSelectedIndex(3);assertEquals(0,calls.get());assertEquals(0,panel.getTableModel().getRowCount());assertTrue(panel.getReportUi().status.getText().contains("Chưa kết nối"));});}
    @Test void defaultSourceIsNotPresentedAsRealZeroStatistics()throws Exception{SwingUtilities.invokeAndWait(()->{var panel=new PhongKhamBaoCaoManHinh();panel.executeReportQuery();assertEquals(0,panel.getTableModel().getRowCount());assertTrue(panel.getReportUi().status.getText().contains("Chưa kết nối nguồn thống kê"));assertFalse(panel.getBtnPrint().isEnabled());});}
    @Test void missingSessionDoesNotQuery()throws Exception{AtomicInteger calls=new AtomicInteger();SwingUtilities.invokeAndWait(()->{var panel=new PhongKhamBaoCaoManHinh(services(calls),()->null);panel.executeReportQuery();assertEquals(0,calls.get());assertTrue(panel.getReportUi().status.getText().contains("Phiên đăng nhập"));});}
    static void await(PhongKhamBaoCaoManHinh panel)throws Exception{for(int i=0;i<150;i++){boolean[] busy={true};SwingUtilities.invokeAndWait(()->busy[0]=panel.isLoading());if(!busy[0])return;Thread.sleep(20);}fail("Query did not finish.");}
    static void layout(Container root){root.doLayout();for(Component child:root.getComponents())if(child instanceof Container box)layout(box);}
}
