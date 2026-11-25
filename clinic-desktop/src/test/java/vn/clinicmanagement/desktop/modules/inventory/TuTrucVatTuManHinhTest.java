package vn.clinicmanagement.desktop.modules.inventory;

import org.junit.jupiter.api.*;
import vn.clinicmanagement.application.inventory.TuTrucCongKetNoi;
import vn.clinicmanagement.application.inventory.TuTrucCongKetNoi.*;
import vn.clinicmanagement.application.inventory.NhaThuocDanhSachReadCongKetNoi.Bang;
import vn.clinicmanagement.application.common.KetQua;
import vn.clinicmanagement.desktop.core.session.*;
import vn.clinicmanagement.desktop.modules.dashboard.BangDieuKhienDieuHuongChinhSach;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.BooleanSupplier;
import static org.junit.jupiter.api.Assertions.*;

class TuTrucVatTuManHinhTest {
    static {System.setProperty("java.awt.headless","true");}
    @BeforeEach void login(){PhienThuNganKiemThu.login("PHARMACIST");}
    @AfterEach void logout(){NguoiDungPhien.getInstance().clearSession();}
    static class Read implements TuTrucCongKetNoi {
        List<Kho> cabinets=List.of(new Kho(3,"Hóa chất khoa sinh hóa"));
        List<Phieu> tickets=List.of(ticket("000587","Nhập bù","DA_NHAN_HANG"));
        volatile int saved;
        public List<Kho> getCabinets(int f){assertEquals(1,f);return cabinets;}
        public List<Kho> getSupplyWarehouses(int f){assertEquals(1,f);return List.of(new Kho(1,"Kho vật tư tổng"));}
        public List<Phieu> getTickets(int f,int k,LocalDateTime from,LocalDateTime until){assertEquals(1,f);assertEquals(3,k);assertTrue(from.isBefore(until));return tickets;}
        public List<Lo> getStock(int f,int k){assertEquals(1,f);return List.of(new Lo(12,121,"VT001","Ống nghiệm","Cái","LOT01",LocalDate.now().plusYears(1),BigDecimal.TEN));}
        public Bang getDetails(int f,int k,Phieu p){return new Bang(List.of("Tên vật tư","Số lượng"),List.of(List.of("Ống nghiệm",1)));}
        public KetQua<String> requestReplenishment(YeuCau request){assertEquals(3,request.cabinetId());assertEquals(1,request.sourceWarehouseId());assertEquals(121,request.lines().get(0).itemId());assertEquals(1,request.context().facilityId());saved++;return KetQua.success("NB-TEST");}
    }
    static Phieu ticket(String code,String type,String status){return new Phieu(587,"XUAT_KHO",status,LocalDate.of(2021,9,14).atTime(9,56),LocalDate.of(2021,9,14).atStartOfDay(),code,"",type,"","Khoa Hóa Sinh","Hóa chất khoa sinh hóa");}
    static TuTrucVatTuManHinh view(Read data)throws Exception{var result=new TuTrucVatTuManHinh[1];SwingUtilities.invokeAndWait(()->{result[0]=new TuTrucVatTuManHinh(data);result[0].refreshData();});await(()->result[0].model.getRowCount()==data.tickets.size()&&result[0].cabinets.getItemCount()==data.cabinets.size());return result[0];}

    @Test void referenceLayoutRendersRealSwingAt1586AndKeepsGridBelowData()throws Exception{
        var view=view(new Read());
        SwingUtilities.invokeAndWait(()->{
            view.setSize(1586,828);layout(view);layout(view);
            assertEquals(62,view.toolbar.getHeight());assertTrue(Math.abs(view.columns.getDividerLocation()-250)<=2);
            assertTrue(view.table.getTableHeader().getHeight()>0);assertEquals(10,view.table.getColumnCount());assertEquals(24,view.table.getRowHeight());
            assertEquals("Đã nhập tủ",view.model.getValueAt(0,1));assertEquals("09:56 14/09/2021",view.model.getValueAt(0,2));
            assertEquals("Mã Phiếu TH",view.table.getColumnName(5));assertFalse(view.lower.isVisible());assertFalse(view.table.isCellEditable(0,1));
            var image=new BufferedImage(1586,828,BufferedImage.TYPE_INT_RGB);var g=image.createGraphics();view.paint(g);g.dispose();
            try{javax.imageio.ImageIO.write(image,"png",new java.io.File("/tmp/clinic-tu-truc-preview.png"));}catch(Exception ex){throw new RuntimeException(ex);}
            view.setSize(1280,720);layout(view);layout(view);
            assertTrue(view.toolbar.search.getX()>=0);assertEquals(62,view.toolbar.getHeight());
        });
    }
    @Test void quickSearchAndTypeFilterDoNotChangeCatalogCounts()throws Exception{
        Read data=new Read();data.tickets=List.of(ticket("NB-01","Nhập bù","CHO_DUYET"),ticket("XUAT-02","Xuất sử dụng","DA_XUAT"));var view=view(data);
        SwingUtilities.invokeAndWait(()->{
            view.search.setText("xuat su dung");assertEquals(1,view.model.getRowCount());assertEquals("XUAT-02",view.model.getValueAt(0,4));
            assertTrue(view.all.getText().contains("(2)"));view.search.setText("");view.categories.setSelectionRow(1);
            assertEquals(1,view.model.getRowCount());assertEquals("NB-01",view.model.getValueAt(0,4));
            view.all.doClick();assertEquals(2,view.model.getRowCount());view.toolbar.code.setText("missing");assertEquals(0,view.model.getRowCount());
        });
    }
    @Test void noCabinetsDoesNotInsertScreenshotSampleOrFallbackWarehouse()throws Exception{
        Read data=new Read();data.cabinets=List.of();data.tickets=List.of();var view=view(data);
        SwingUtilities.invokeAndWait(()->{assertEquals(0,view.cabinets.getItemCount());assertEquals(0,view.model.getRowCount());assertEquals(0,view.selectedCabinetId());});
    }
    @Test void selectedTicketDetailsOpenBelowListAndPreserveBothMainColumns()throws Exception{
        var view=view(new Read());var left=view.columns.getLeftComponent();var top=view.rows.getTopComponent();
        SwingUtilities.invokeAndWait(()->view.table.getActionMap().get("details").actionPerformed(new java.awt.event.ActionEvent(view.table,0,"details")));
        await(()->view.lower.isVisible());SwingUtilities.invokeAndWait(()->{
            assertSame(left,view.columns.getLeftComponent());assertSame(top,view.rows.getTopComponent());assertEquals(1,view.model.getRowCount());
            assertEquals(1,view.cabinets.getItemCount());assertEquals(2,view.lower.getComponentCount());
        });
    }
    @Test void lateTicketResponseCannotReplaceNewlySelectedCabinet()throws Exception{
        var gate=new CountDownLatch(1);Read data=new Read(){
            @Override public List<Phieu> getTickets(int f,int k,LocalDateTime from,LocalDateTime until){
                if(k==3)try{gate.await(3,TimeUnit.SECONDS);}catch(InterruptedException ex){Thread.currentThread().interrupt();}
                return List.of(ticket(k==3?"OLD":"NEW","Nhập bù","CHO_DUYET"));
            }
        };data.cabinets=List.of(new Kho(3,"Tủ A"),new Kho(4,"Tủ B"));
        TuTrucVatTuManHinh[] panel={null};SwingUtilities.invokeAndWait(()->{panel[0]=new TuTrucVatTuManHinh(data);panel[0].refreshData();});
        await(()->panel[0].cabinets.getItemCount()==2);
        SwingUtilities.invokeAndWait(()->panel[0].cabinets.setSelectedIndex(1));await(()->panel[0].model.getRowCount()==1);
        gate.countDown();Thread.sleep(150);SwingUtilities.invokeAndWait(()->assertEquals("NEW",panel[0].model.getValueAt(0,4)));
    }
    @Test void dateRangeRejectsReversedAndInvalidDates()throws Exception{
        SwingUtilities.invokeAndWait(()->{var toolbar=new TuTrucThanhCongCu();toolbar.from.setValue(java.sql.Timestamp.valueOf("2026-11-21 00:00:00"));toolbar.until.setValue(java.sql.Timestamp.valueOf("2026-11-20 23:59:00"));
            assertThrows(IllegalArgumentException.class,toolbar::range);
            ((JSpinner.DateEditor)toolbar.from.getEditor()).getTextField().setText("00:00 31/02/2026");assertThrows(IllegalArgumentException.class,toolbar::range);
        });
    }
    @Test void replenishmentValidatesQuantityBeforeSendingAndPassesSourceBatchAndSession()throws Exception{
        Read data=new Read();TuTrucNhapBuManHinh[] editor={null};
        SwingUtilities.invokeAndWait(()->editor[0]=new TuTrucNhapBuManHinh(data,new Kho(3,"Tủ trực"),()->{}));await(()->editor[0].model.getRowCount()==1);
        SwingUtilities.invokeAndWait(()->{var e=editor[0];e.model.setValueAt("11",0,7);e.save.doClick();assertEquals(0,data.saved);assertTrue(e.message.getText().contains("tồn kho cấp"));
            e.model.setValueAt("2",0,7);e.save.doClick();assertFalse(e.save.isEnabled());});await(()->data.saved==1);
    }
    @Test void hiddenViewIgnoresLateCabinetResponse()throws Exception{
        var gate=new CountDownLatch(1);Read data=new Read(){@Override public List<Kho> getCabinets(int f){try{gate.await(3,TimeUnit.SECONDS);}catch(InterruptedException ex){Thread.currentThread().interrupt();}return super.getCabinets(f);}};
        TuTrucVatTuManHinh[] panel={null};SwingUtilities.invokeAndWait(()->{panel[0]=new TuTrucVatTuManHinh(data);panel[0].refreshData();panel[0].onHide();});
        gate.countDown();Thread.sleep(150);SwingUtilities.invokeAndWait(()->assertEquals(0,panel[0].cabinets.getItemCount()));
    }
    @Test void cabinetRouteRequiresInventoryGrant(){assertTrue(new BangDieuKhienDieuHuongChinhSach(Set.of("INVENTORY")).canOpen("cabinet"));
        assertTrue(new BangDieuKhienDieuHuongChinhSach(Set.of("SYSTEM")).canOpen("cabinet"));assertFalse(new BangDieuKhienDieuHuongChinhSach(Set.of("PRESCRIPTION")).canOpen("cabinet"));
        assertFalse(new BangDieuKhienDieuHuongChinhSach(Set.of()).canOpen("cabinet"));}
    @Test void createMenuKeepsDistinctActionsAndOnlySupportedReplenishmentOpensItsEditor()throws Exception{
        Read data=new Read();var view=view(data);
        SwingUtilities.invokeAndWait(()->{
            var menu=view.toolbar.getCreateTicketMenu();
            for(var child:menu.getComponents())if(child instanceof JMenuItem item&&!item.getText().equals("Nhập Bù Cơ Số Tủ Trực")){
                item.doClick();assertFalse(view.lower.isVisible(),item.getText());assertEquals(0,data.saved);
            }
            assertEquals(1,view.model.getRowCount());assertEquals(1,view.cabinets.getItemCount());
            menuItem(menu,"Nhập Bù Cơ Số Tủ Trực").doClick();assertTrue(view.lower.isVisible());
            assertTrue(Arrays.stream(view.lower.getComponents()).anyMatch(c->c instanceof TuTrucNhapBuManHinh));
            assertEquals(0,data.saved); // Opening a menu/editor never writes an inventory request.
            view.onHide();
        });
    }
    @Test void createPopupHighlightsTriggerAndDisablesActionsWhenSessionIsUnavailable()throws Exception{
        var view=view(new Read());
        SwingUtilities.invokeAndWait(()->{
            view.setSize(1586,828);layout(view);layout(view);
            var menu=view.toolbar.getCreateTicketMenu();menu.setSize(menu.getPreferredSize());layout(menu);
            var event=new javax.swing.event.PopupMenuEvent(menu);
            for(var listener:menu.getPopupMenuListeners())listener.popupMenuWillBecomeVisible(event);
            assertEquals(Boolean.TRUE,view.toolbar.create.getClientProperty("tuTrucMenuMo"));
            assertTrue(menuItem(menu,"Nhập Bù Cơ Số Tủ Trực").isEnabled());
            var image=new BufferedImage(1586,828,BufferedImage.TYPE_INT_RGB);var g=image.createGraphics();view.paint(g);
            Point anchor=SwingUtilities.convertPoint(view.toolbar.create,-12,view.toolbar.create.getHeight(),view);
            g.translate(anchor.x,anchor.y);menu.paint(g);g.dispose();
            try{javax.imageio.ImageIO.write(image,"png",new java.io.File("/tmp/clinic-tu-truc-menu-preview.png"));}catch(Exception e){throw new RuntimeException(e);}
            NguoiDungPhien.getInstance().clearSession();
            for(var listener:menu.getPopupMenuListeners())listener.popupMenuWillBecomeVisible(event);
            for(var child:menu.getComponents())if(child instanceof JMenuItem item)assertFalse(item.isEnabled());
            menuItem(menu,"Nhập Bù Cơ Số Tủ Trực").doClick();assertFalse(view.lower.isVisible());
            for(var listener:menu.getPopupMenuListeners())listener.popupMenuCanceled(event);
            assertEquals(Boolean.FALSE,view.toolbar.create.getClientProperty("tuTrucMenuMo"));
        });
    }
    static JMenuItem menuItem(JPopupMenu menu,String text){return Arrays.stream(menu.getComponents()).filter(c->c instanceof JMenuItem item&&item.getText().equals(text)).map(c->(JMenuItem)c).findFirst().orElseThrow();}
    static void await(BooleanSupplier check)throws Exception{for(int i=0;i<150;i++){Thread.sleep(20);boolean[] ready={false};SwingUtilities.invokeAndWait(()->ready[0]=check.getAsBoolean());if(ready[0])return;}fail("Background load did not complete");}
    static void layout(Container c){c.doLayout();for(var child:c.getComponents())if(child instanceof Container box)layout(box);}
}
