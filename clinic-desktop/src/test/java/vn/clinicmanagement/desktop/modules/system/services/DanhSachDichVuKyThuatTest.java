package vn.clinicmanagement.desktop.modules.system.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import vn.clinicmanagement.desktop.modules.system.HeThongCauHinhManHinh;
import vn.clinicmanagement.domain.common.enums.LoaiDichVu;
import vn.clinicmanagement.domain.service.DanhMucDichVu;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class DanhSachDichVuKyThuatTest {
    @TempDir Path directory;
    @Test void systemOpensServiceScreenByItemNameAfterFiltering()throws Exception {
        SwingUtilities.invokeAndWait(()->{
            AtomicInteger calls=new AtomicInteger();
            var parent=new HeThongCauHinhManHinh(new HeThongCauHinhManHinh.InMemorySystemRepository(),calls::incrementAndGet);
            parent.getSearchField().setText("Dịch Vụ Kỹ Thuật");assertEquals(1,parent.getTable().getRowCount());
            parent.getTable().setRowSelectionInterval(0,0);
            parent.getTable().getActionMap().get("openManagementItem").actionPerformed(new ActionEvent(parent,0,"open"));
            assertEquals(1,calls.get());assertFalse(parent.openManagementItem("Danh Sách Phòng"));
        });
    }
    @Test void keepsReferenceProportionsAndSevenReadableHeaders()throws Exception {
        SwingUtilities.invokeAndWait(()->{
            var panel=new DanhSachDichVuKyThuatManHinh(List::of,new DichVuThuTuLuuTru(directory.resolve("order.xml")));
            var ui=panel.getCatalogUi();
            for(int[]size:new int[][]{{1586,960},{1366,720},{1440,824}}){
                panel.setSize(size[0],size[1]);layout(panel);double scale=size[0]/1586.0;
                assertEquals(312*scale,ui.split.getDividerLocation(),2);assertEquals(80*scale,ui.toolbar.getHeight(),2);
                assertEquals(7,ui.table.getColumnCount());assertEquals("Giá Viện Phí",ui.table.getColumnName(5));
                assertTrue(ui.save.getX()+ui.save.getWidth()<=ui.toolbar.getWidth());assertTrue(ui.keyword.getWidth()>700);
                for(int c=0;c<7;c++){
                    var renderer=ui.table.getTableHeader().getDefaultRenderer();
                    var label=(JLabel)renderer.getTableCellRendererComponent(ui.table,ui.table.getColumnName(c),false,false,0,c);
                    assertTrue(label.getFontMetrics(label.getFont()).stringWidth(label.getText())<=ui.table.getColumnModel().getColumn(c).getWidth()-6,"Header clipped: "+label.getText());
                }
            }
        });
    }
    @Test void searchKeepsAncestorsAndFiltersCodeEquivalentAndVietnameseNames(){
        var model=new DichVuDanhMucMoHinh();model.replace(List.of(item(1,"XN1","Hóa sinh","Định lượng Urê"),item(2,"XN2","Huyết học","Công thức máu")));
        model.filter("dinh luong ure","","");assertEquals(3,model.getRowCount());assertEquals("XN1",model.getValueAt(2,0));
        model.filter("23.1494","","");assertEquals(5,model.getRowCount());
        model.filter("","Xét nghiệm","Huyết học");assertEquals("XN2",model.getValueAt(2,0));
        model.toggle(0);assertEquals(1,model.getRowCount());model.expandAll();assertEquals(3,model.getRowCount());
    }
    @Test void mapsOnlyAvailableTariffsAndDoesNotInventUnitOrEquivalent(){
        DanhMucDichVu source=new DanhMucDichVu();source.setId(1);source.setMaDichVu("XN1");source.setTenDichVu("Xét nghiệm tổng hợp");source.setLoaiDichVu(LoaiDichVu.XET_NGHIEM);
        source.setGiaBHYT(new BigDecimal("21500"));source.setDonGia(new BigDecimal("25000"));
        var item=DichVuHang.from(source);assertEquals(new BigDecimal("21500"),item.insurancePrice());assertEquals(new BigDecimal("25000"),item.servicePrice());
        assertNull(item.hospitalPrice());assertEquals("",item.unit());assertEquals("",item.equivalent());
    }
    @Test void displayOrderPersistsWithoutChangingPrices()throws Exception {
        var model=new DichVuDanhMucMoHinh();DichVuHang first=item(1,"XN1","Hóa sinh","Urê"),second=item(2,"XN2","Hóa sinh","Creatinin");
        model.replace(List.of(first,second));assertTrue(model.move("2",-1));
        var store=new DichVuThuTuLuuTru(directory.resolve("order.xml"));store.save(model.entries());
        assertEquals(List.of(second,first),store.apply(List.of(first,second)));assertEquals(new BigDecimal("25000"),model.entries().get(0).servicePrice());
    }
    @Test void loadsActualCountsAsynchronouslyAndClearsOldRowsOnReadFailure()throws Exception {
        AtomicInteger invocation=new AtomicInteger();var panel=new DanhSachDichVuKyThuatManHinh(()->{
            if(invocation.incrementAndGet()>1)throw new IllegalStateException("synthetic failure");return List.of(item(1,"XN1","Hóa sinh","Urê"));
        },new DichVuThuTuLuuTru(directory.resolve("order.xml")));
        CountDownLatch loaded=new CountDownLatch(1),cleared=new CountDownLatch(1);
        SwingUtilities.invokeAndWait(()->{
            panel.getCatalogModel().addTableModelListener(e->{if(panel.getCatalogModel().entries().size()==1)loaded.countDown();else if(invocation.get()>1)cleared.countDown();});panel.refreshData();
        });
        assertTrue(loaded.await(4,TimeUnit.SECONDS));
        SwingUtilities.invokeAndWait(()->{assertTrue(panel.getCatalogUi().tree.getModel().getRoot().toString().contains("(1)"));panel.refreshData();});
        assertTrue(cleared.await(4,TimeUnit.SECONDS));SwingUtilities.invokeAndWait(()->assertEquals(0,panel.getCatalogModel().entries().size()));
    }
    private static DichVuHang item(int id,String code,String group,String name){return new DichVuHang(id,code,"23.1494",name,"Lần",new BigDecimal("21500"),new BigDecimal("21200"),new BigDecimal("25000"),"Xét nghiệm",group);}
    private static void layout(Container root){root.doLayout();for(Component c:root.getComponents())if(c instanceof Container child)layout(child);}
}
