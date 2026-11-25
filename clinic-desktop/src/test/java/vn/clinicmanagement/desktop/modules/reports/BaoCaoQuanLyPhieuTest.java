package vn.clinicmanagement.desktop.modules.reports;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.reports.PhieuBaoCaoReadCongKetNoi;
import vn.clinicmanagement.application.reports.PhieuBaoCaoReadCongKetNoi.*;
import vn.clinicmanagement.desktop.modules.reports.catalog.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.imageio.ImageIO;
import static org.junit.jupiter.api.Assertions.*;

/** Approved design: both catalog columns remain, data expands below the right-hand list. */
class BaoCaoQuanLyPhieuTest {
    @TempDir Path temp;
    static PhanQuyenNguCanh auth(){return new PhanQuyenNguCanh(1,1,"ADMIN",1,1,UUID.randomUUID());}
    static BaoCaoMau report(String name){return new BaoCaoMau(name,"Điện tiếp",name,name,"sample.doc","1001","");}
    static List<Phieu> sample(){List<Phieu> rows=new ArrayList<>();for(int i=1;i<=3;i++)rows.add(new Phieu(Loai.KHAM_BENH,5000+i,5000+i,String.valueOf(5000+i),LocalDateTime.of(2026,11,20,9,i),i==1?"HOAN_THANH":"DANG_KHAM","BN-MP-00"+i,"BỆNH NHÂN MÔ PHỎNG 0"+i,"090000000"+i,"Khoa Khám Bệnh","Bác sĩ kiểm thử",null,null,Map.of("Xử trí","CHO_VE_TOA","Đã khóa bệnh án",i==1?"Đã khóa":"Chưa khóa")));
        rows.add(new Phieu(Loai.DON_THUOC,1,5001,"DT-MP-001",LocalDateTime.of(2026,11,20,10,0),"DA_DUYET","BN-MP-001","BỆNH NHÂN MÔ PHỎNG 01","0900000001","Khoa Khám Bệnh","Bác sĩ kiểm thử",null,null,Map.of("Lời dặn","Lời dặn kiểm thử")));
        return List.copyOf(rows);}
    DanhSachBaoCaoManHinh panel(PhieuBaoCaoReadCongKetNoi source){return new DanhSachBaoCaoManHinh(new BaoCaoDanhMucLuuTru(temp.resolve("catalog.xml")),null,source,BaoCaoQuanLyPhieuTest::auth);}
    @Test void clickOpensBelowCatalogWithoutChangingEitherColumnOrCreatingWindows()throws Exception{
        int windows=Window.getWindows().length;DanhSachBaoCaoManHinh[] ref=new DanhSachBaoCaoManHinh[1];Object[] identity=new Object[2];Rectangle[] left=new Rectangle[1];int[] selected=new int[1];
        SwingUtilities.invokeAndWait(()->{com.formdev.flatlaf.FlatLightLaf.setup();var panel=panel(q->sample());ref[0]=panel;panel.setSize(1440,900);layout(panel);layout(panel);var ui=panel.getCatalogUi();identity[0]=ui.tree.getModel();identity[1]=ui.table.getModel();left[0]=ui.split.getLeftComponent().getBounds();assertFalse(ui.isDetailsVisible());int row=panel.getCatalogModel().indexOf("builtin-0");selected[0]=row;ui.table.setRowSelectionInterval(row,row);Rectangle cell=ui.table.getCellRect(row,0,true);ui.table.dispatchEvent(new MouseEvent(ui.table,MouseEvent.MOUSE_CLICKED,System.currentTimeMillis(),0,cell.x+100,cell.y+8,1,false,MouseEvent.BUTTON1));});await(ref[0]);
        SwingUtilities.invokeAndWait(()->{var panel=ref[0];var ui=panel.getCatalogUi();assertTrue(ui.isDetailsVisible());assertSame(identity[0],ui.tree.getModel());assertSame(identity[1],ui.table.getModel());assertEquals(selected[0],ui.table.getSelectedRow());assertEquals(3,ui.table.getColumnCount());assertEquals("Tên Báo Cáo",ui.table.getColumnName(0));assertEquals(3,panel.getDataPanel().getTable().getRowCount());assertEquals(8,panel.getDataPanel().getTable().getColumnCount());assertEquals("Hoàn thành",panel.getDataPanel().getTable().getValueAt(0,5));assertEquals("Đã khóa",panel.getDataPanel().getTable().getValueAt(0,7));
            for(int[] size:new int[][]{{1440,900},{1366,760},{1586,960}}){panel.setSize(size[0],size[1]);layout(panel);layout(panel);assertEquals(Math.max(220,Math.min(280,(int)Math.round(size[0]*0.1875))),ui.split.getDividerLocation(),2);assertTrue(ui.getDetailSplit().getTopComponent().getHeight()>=100);assertTrue(ui.getDetailSplit().getBottomComponent().getHeight()>=200);var image=new BufferedImage(size[0],size[1],BufferedImage.TYPE_INT_RGB);var g=image.createGraphics();panel.paint(g);g.dispose();try{ImageIO.write(image,"png",Path.of("/tmp/report-approved-inline-"+size[0]+".png").toFile());}catch(Exception ex){throw new RuntimeException(ex);}}
            panel.setSize(1440,900);layout(panel);layout(panel);assertEquals(left[0],ui.split.getLeftComponent().getBounds());assertEquals(windows,Window.getWindows().length);panel.getDataPanel().getCloseButton().doClick();layout(panel);assertFalse(ui.isDetailsVisible());assertSame(identity[0],ui.tree.getModel());assertSame(identity[1],ui.table.getModel());assertEquals(selected[0],ui.table.getSelectedRow());assertEquals(windows,Window.getWindows().length);
        });
    }
    @Test void typedSwitchAndSearchAffectOnlyDataAndExport()throws Exception{
        DanhSachBaoCaoManHinh[] ref=new DanhSachBaoCaoManHinh[1];SwingUtilities.invokeAndWait(()->{ref[0]=panel(q->sample());ref[0].openReport(report("Phiếu khám"));});await(ref[0]);SwingUtilities.invokeAndWait(()->{var data=ref[0].getDataPanel();data.getKeyword().setText("mo phong 01");assertEquals(1,data.getTable().getRowCount());try{Path csv=temp.resolve("visible.csv");data.writeVisibleCsv(csv);String text=Files.readString(csv);assertTrue(text.contains("BN-MP-001"));assertFalse(text.contains("BN-MP-002"));}catch(Exception ex){throw new RuntimeException(ex);}ref[0].openReport(report("Đơn thuốc"));});await(ref[0]);SwingUtilities.invokeAndWait(()->{assertEquals(1,ref[0].getDataPanel().getTable().getRowCount());assertEquals("Mã đơn",ref[0].getDataPanel().getTable().getColumnName(0));assertEquals("Đã duyệt",ref[0].getDataPanel().getTable().getValueAt(0,5));assertEquals(3,ref[0].getCatalogUi().table.getColumnCount());});
    }
    @Test void validatesDatesBeforeReadAndRestrictsDoctorScope(){var query=DanhSachBaoCaoManHinh.parseRange("00:00 20/11/2026","23:59 20/11/2026",auth());assertEquals(LocalDateTime.of(2026,11,21,0,0),query.toExclusive());assertNull(query.departmentId());assertThrows(IllegalArgumentException.class,()->DanhSachBaoCaoManHinh.parseRange("00:00 31/02/2026","23:59 20/11/2026",auth()));var doctor=new PhanQuyenNguCanh(1,1,"DOCTOR",2,8,UUID.randomUUID());assertEquals(8,DanhSachBaoCaoManHinh.parseRange("00:00 20/11/2026","23:59 20/11/2026",doctor).departmentId());}
    @Test void staleAndCollapsedRequestsCannotOverwriteNewReport()throws Exception{
        AtomicInteger calls=new AtomicInteger();CountDownLatch started=new CountDownLatch(1),release=new CountDownLatch(1);DanhSachBaoCaoManHinh[] ref=new DanhSachBaoCaoManHinh[1];
        try{SwingUtilities.invokeAndWait(()->{ref[0]=panel(q->{int call=calls.incrementAndGet();if(call==1){started.countDown();boolean done=false;while(!done)try{done=release.await(2,TimeUnit.SECONDS);}catch(InterruptedException ignored){}}return sample();});ref[0].openReport(report("Phiếu khám"));});assertTrue(started.await(2,TimeUnit.SECONDS));SwingUtilities.invokeAndWait(()->ref[0].openReport(report("Đơn thuốc")));await(ref[0]);release.countDown();Thread.sleep(70);SwingUtilities.invokeAndWait(()->{assertEquals("Mã đơn",ref[0].getDataPanel().getTable().getColumnName(0));assertEquals(1,ref[0].getDataPanel().getTable().getRowCount());ref[0].collapseData();assertFalse(ref[0].getCatalogUi().isDetailsVisible());});}finally{release.countDown();}
    }
    @Test void collapsingWhileQueryRunsNeverReopensDetail()throws Exception{
        CountDownLatch started=new CountDownLatch(1),release=new CountDownLatch(1);DanhSachBaoCaoManHinh[] ref=new DanhSachBaoCaoManHinh[1];
        try{SwingUtilities.invokeAndWait(()->{ref[0]=panel(q->{started.countDown();boolean done=false;while(!done)try{done=release.await(2,TimeUnit.SECONDS);}catch(InterruptedException ignored){}return sample();});ref[0].openReport(report("Phiếu khám"));});assertTrue(started.await(2,TimeUnit.SECONDS));SwingUtilities.invokeAndWait(ref[0]::collapseData);release.countDown();Thread.sleep(70);SwingUtilities.invokeAndWait(()->{assertFalse(ref[0].getCatalogUi().isDetailsVisible());assertEquals(0,ref[0].getDataPanel().getTable().getRowCount());assertFalse(ref[0].getDataPanel().getExportButton().isEnabled());});}finally{release.countDown();}
    }
    @Test void failedReloadAndUnsupportedReportClearDataAndDoNotInventRows()throws Exception{
        AtomicInteger calls=new AtomicInteger();DanhSachBaoCaoManHinh[] ref=new DanhSachBaoCaoManHinh[1];SwingUtilities.invokeAndWait(()->{ref[0]=panel(q->{if(calls.incrementAndGet()>1)throw new IllegalStateException("Offline");return sample();});ref[0].openReport(report("Phiếu khám"));});await(ref[0]);SwingUtilities.invokeAndWait(()->ref[0].getDataPanel().reload());await(ref[0]);SwingUtilities.invokeAndWait(()->{assertEquals(0,ref[0].getDataPanel().getTable().getRowCount());assertFalse(ref[0].getDataPanel().getExportButton().isEnabled());assertTrue(ref[0].getDataPanel().getStatus().getText().startsWith("Không tải"));ref[0].openReport(report("Phiếu tiêm chủng"));assertTrue(ref[0].getDataPanel().getStatus().getText().contains("chưa được kết nối"));assertEquals(2,calls.get());});
    }
    @Test void statisticsAlsoEmbedBelowCatalogWithoutOpeningAnotherWindow()throws Exception{int windows=Window.getWindows().length;SwingUtilities.invokeAndWait(()->{var panel=panel(q->sample());panel.showStatisticsInline();assertTrue(panel.getCatalogUi().isDetailsVisible());assertEquals(3,panel.getCatalogUi().table.getColumnCount());assertEquals(windows,Window.getWindows().length);panel.collapseData();assertFalse(panel.getCatalogUi().isDetailsVisible());});}
    @Test void exportsUnicodeAndEscapesCsv()throws Exception{Path file=temp.resolve("report.csv");DanhSachBaoCaoManHinh.writeCsv(file,List.of("Họ tên","Ghi chú"),List.of(List.of("Kiểm thử","  =1+1"),List.of("Tên, có dấu","Dòng \"mới\"")));String csv=Files.readString(file);assertTrue(csv.startsWith("\ufeff"));assertTrue(csv.contains("\"'  =1+1\""));assertTrue(csv.contains("\"Dòng \"\"mới\"\"\""));}
    @Test void resolvesFinancialVoucherReportsAndEnablesOpenSlipButton()throws Exception{
        assertEquals(Optional.of(Loai.PHIEU_THU),vn.clinicmanagement.desktop.modules.reports.components.BaoCaoDuLieuNoiTuyenManHinh.typeFor(report("Phiếu thu tiền")));
        assertEquals(Optional.of(Loai.PHIEU_THU),vn.clinicmanagement.desktop.modules.reports.components.BaoCaoDuLieuNoiTuyenManHinh.typeFor(report("Nhật ký thu chi")));
        assertEquals(Optional.of(Loai.PHIEU_HOAN_TRA),vn.clinicmanagement.desktop.modules.reports.components.BaoCaoDuLieuNoiTuyenManHinh.typeFor(report("Phiếu hoàn tiền")));
        assertEquals(Optional.of(Loai.HOA_DON),vn.clinicmanagement.desktop.modules.reports.components.BaoCaoDuLieuNoiTuyenManHinh.typeFor(report("Phiếu miễn giảm")));
        DanhSachBaoCaoManHinh[] ref=new DanhSachBaoCaoManHinh[1];
        List<Phieu> rows=List.of(
                new Phieu(Loai.PHIEU_THU,10,5015,"PT-10",LocalDateTime.of(2026,11,20,9,0),"TIEN_MAT","BN-1","NGUYỄN VĂN TEST","0900","Khoa Khám Bệnh","Đặng J Tường",new BigDecimal("295000"),new BigDecimal("185000"),Map.of("Số hóa đơn","DK-5015","Ghi chú","Thu đợt 1")),
                new Phieu(Loai.PHIEU_HOAN_TRA,1,5015,"HT-1",LocalDateTime.of(2026,11,20,9,30),"TIEN_MAT","BN-1","NGUYỄN VĂN TEST","0900","Khoa Khám Bệnh","Đặng J Tường",new BigDecimal("234000"),new BigDecimal("92200"),Map.of("Số hóa đơn","DK-5015","Lý do","Hoàn BHYT")));
        SwingUtilities.invokeAndWait(()->{ref[0]=panel(q->rows);ref[0].openReport(report("Phiếu thu tiền"));});await(ref[0]);
        SwingUtilities.invokeAndWait(()->{
            var data=ref[0].getDataPanel();
            assertEquals(1,data.getTable().getRowCount());
            assertEquals("Mã phiếu thu",data.getTable().getColumnName(0));
            assertEquals("PT-10",data.getTable().getValueAt(0,0));
            assertTrue(data.getOpenSlipButton().isEnabled());
            assertNotNull(data.selectedTicket());
            ref[0].openReport(report("Phiếu hoàn tiền"));
        });await(ref[0]);
        SwingUtilities.invokeAndWait(()->{
            var data=ref[0].getDataPanel();
            assertEquals(1,data.getTable().getRowCount());
            assertEquals("Mã phiếu hoàn",data.getTable().getColumnName(0));
            assertEquals("HT-1",data.getTable().getValueAt(0,0));
            assertTrue(data.getOpenSlipButton().isEnabled());
        });
    }
    static void await(DanhSachBaoCaoManHinh panel)throws Exception{for(int i=0;i<150;i++){boolean[] busy={true};SwingUtilities.invokeAndWait(()->busy[0]=panel.isLoading());if(!busy[0])return;Thread.sleep(20);}fail("Query did not finish.");}
    static void layout(Container root){root.doLayout();for(Component child:root.getComponents())if(child instanceof Container box)layout(box);}
}
