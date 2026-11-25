package vn.clinicmanagement.desktop.modules.system.services;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import vn.clinicmanagement.application.catalog.DichVuCauHinh;
import vn.clinicmanagement.application.common.KetQua;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.desktop.core.session.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.function.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

class DichVuCauHinhManHinhTest {
    static {System.setProperty("java.awt.headless","true");com.formdev.flatlaf.FlatLightLaf.setup();vn.clinicmanagement.desktop.core.config.UngDungPhongChu.installGlobally();}
    @TempDir Path directory;
    @BeforeEach void login(){PhienThuNganKiemThu.login("ADMIN");}
    @AfterEach void logout(){NguoiDungPhien.getInstance().clearSession();}
    static DichVuCauHinh reference(){
        Map<String,String> v=new LinkedHashMap<>();
        v.put("code","XN0046");v.put("name","Định lượng Urê [Máu]");v.put("insuranceName","Định lượng Urê [Máu]");
        v.put("category","Xét nghiệm");v.put("group","Hóa sinh");v.put("unit","Lần");v.put("insuranceCode","23.0166.1494");
        v.put("insuranceNumber","37.1E03.1494");v.put("sortNumber","16");v.put("insuranceCostGroup","Xét nghiệm");v.put("accountGroup","Hóa sinh");v.put("priceBasis","Ngày nhập viện");
        v.put("insurancePrice","21500");v.put("hospitalPrice","21200");v.put("servicePrice","35000");v.put("foreignPrice","0");
        for(String key:List.of("inNetworkPercent","outNetworkPercent"))v.put(key,"100.00");
        for(String key:List.of("insuranceLimit","materialLimit","drugLimit"))v.put(key,"-1.00");
        v.put("defaultQuantity","0.00");v.put("filmQuantity","0");v.put("reuseDays","0.00");v.put("duration","5.00");v.put("minimumDuration","5.00");
        v.put("executionRooms","XÉT NGHIỆM HÓA SINH");v.put("receptionRooms","Lấy mẫu sinh hóa");v.put("executionArea","Hóa Sinh");
        v.put("consumedMaterials","{\"KeKhiTraKetQua\":1,\"RoomID\":289}");v.put("profitGroup","(Không chia)");v.put("referralGroup","(Không chia)");v.put("resultType","Mặc định");
        v.put("skipTimeOverlapCheck","true");v.put("unseenSetting","preserve me");return new DichVuCauHinh(1,7,v);
    }
    static class Source implements DichVuDanhMucNguon {
        final DichVuCauHinh original=reference();volatile int writes;DichVuCauHinh last;boolean conflict,failLoad;
        public List<DichVuHang> load(){return List.of(DichVuHang.from(original));}
        public DichVuCauHinh loadDetail(DichVuHang item){if(failLoad)throw new IllegalStateException("read failure");return original;}
        public boolean canSaveDetail(){return true;}
        public KetQua<DichVuCauHinh> saveDetail(DichVuCauHinh value,PhanQuyenNguCanh actor){assertEquals("ADMIN",actor.roleCode());writes++;last=value;
            return conflict?KetQua.failure("CONFLICT","Dịch vụ đã được người khác sửa."):KetQua.success(new DichVuCauHinh(value.id(),value.version()+1,value.values()));}
    }
    static DichVuCauHinhManHinh panel(Source source,Consumer<DichVuCauHinh> saved)throws Exception{
        var box=new DichVuCauHinhManHinh[1];SwingUtilities.invokeAndWait(()->box[0]=new DichVuCauHinhManHinh(DichVuHang.from(source.original),source,saved));
        await(()->source.failLoad?box[0].status.getText().startsWith("Không tải"):box[0].save.isEnabled());return box[0];
    }
    @Test void renderReferenceAndKeepEveryFieldAccessibleAtSmallerScreen()throws Exception{
        var panel=panel(new Source(),v->{});
        SwingUtilities.invokeAndWait(()->{
            panel.setSize(1586,959);layout(panel);layout(panel);
            assertEquals(6,panel.tabs.getTabCount());assertEquals("Thông Tin Dịch Vụ",panel.tabs.getTitleAt(0));
            assertEquals("21,500",((JTextField)panel.fields.get("insurancePrice")).getText());
            assertEquals(151,panel.fields.get("code").getWidth(),1);assertEquals(618,panel.fields.get("name").getWidth(),1);
            assertTrue(((JCheckBox)panel.fields.get("skipTimeOverlapCheck")).isSelected());assertFalse(panel.isDirty());
            assertEquals(22,panel.fields.values().stream().filter(c->c instanceof JCheckBox).count());
            render(panel,"/tmp/clinic-service-config-preview.png");
            for(int[] size:new int[][]{{1440,820},{1200,700}}){panel.setSize(size[0],size[1]);layout(panel);layout(panel);
                for(var c:panel.form.positions.keySet())assertTrue(c.getX()+c.getWidth()<=panel.form.getWidth()+1,c.getName());
                assertTrue(panel.save.getWidth()>0);assertTrue(panel.form.getPreferredSize().height>0);
            }
            panel.dispose();
        });
    }
    @Test void savePreservesUnknownSettingsAndCorrectServiceVersion()throws Exception{
        Source source=new Source();AtomicReference<DichVuCauHinh> saved=new AtomicReference<>();var panel=panel(source,saved::set);
        SwingUtilities.invokeAndWait(()->{((JTextField)panel.fields.get("name")).setText("Urê đã cập nhật");((JTextField)panel.fields.get("servicePrice")).setText("39,000");
            ((JCheckBox)panel.fields.get("allowDiscount")).setSelected(true);assertTrue(panel.isDirty());panel.save.doClick();assertFalse(panel.save.isEnabled());});
        await(()->saved.get()!=null);
        SwingUtilities.invokeAndWait(()->{assertEquals(1,source.last.id());assertEquals(7,source.last.version());assertEquals("preserve me",source.last.value("unseenSetting"));
            assertEquals("true",source.last.value("allowDiscount"));assertEquals(8,saved.get().version());assertFalse(panel.isDirty());assertEquals(1,source.writes);panel.dispose();});
    }
    @Test void invalidPriceDoesNotWriteAndConflictKeepsDraft()throws Exception{
        Source source=new Source();source.conflict=true;var panel=panel(source,v->fail("Conflict must not notify success"));
        SwingUtilities.invokeAndWait(()->{((JTextField)panel.fields.get("servicePrice")).setText("-10");panel.save();assertEquals(0,source.writes);
            ((JTextField)panel.fields.get("servicePrice")).setText("40,000");panel.save();});
        await(()->panel.save.isEnabled());SwingUtilities.invokeAndWait(()->{assertEquals(1,source.writes);assertEquals("40,000",((JTextField)panel.fields.get("servicePrice")).getText());assertTrue(panel.isDirty());assertTrue(panel.status.getText().contains("người khác"));panel.dispose();});
    }
    @Test void failedReadDisablesSavingRatherThanOpeningBlankWritableRecord()throws Exception{
        Source source=new Source();source.failLoad=true;var panel=panel(source,v->fail());SwingUtilities.invokeAndWait(()->{panel.save();assertEquals(0,source.writes);assertFalse(panel.save.isEnabled());panel.dispose();});
    }
    @Test void doubleClickUsesClickedServiceAndSaveUpdatesThatCatalogRow()throws Exception{
        Source source=new Source();AtomicReference<DichVuHang> opened=new AtomicReference<>();AtomicReference<Consumer<DichVuCauHinh>> callback=new AtomicReference<>();
        var list=new DanhSachDichVuKyThuatManHinh(source,new DichVuThuTuLuuTru(directory.resolve("order.xml")),(item,saved)->{opened.set(item);callback.set(saved);});
        SwingUtilities.invokeAndWait(()->list.refreshData());await(()->list.getCatalogModel().entries().size()==1);
        SwingUtilities.invokeAndWait(()->{list.setSize(1586,960);layout(list);var table=list.getCatalogUi().table;int row=list.getCatalogModel().indexOf("1");Rectangle bounds=table.getCellRect(row,2,true);
            var event=new MouseEvent(table,MouseEvent.MOUSE_CLICKED,System.currentTimeMillis(),0,bounds.x+8,bounds.y+4,2,false,MouseEvent.BUTTON1);
            for(var listener:table.getMouseListeners())listener.mouseClicked(event);assertEquals(1,opened.get().id());
            var values=new LinkedHashMap<>(reference().values());values.put("name","Tên mới");values.put("servicePrice","42000");callback.get().accept(new DichVuCauHinh(1,8,values));
            assertEquals("Tên mới",list.getCatalogModel().entries().get(0).name());assertEquals(new BigDecimal("42000"),list.getCatalogModel().entries().get(0).servicePrice());
        });
    }
    @Test void usableAreaAccountsForMenuBarDockAndSecondMonitor(){
        assertEquals(new Rectangle(-1920,24,1920,1006),DichVuCauHinhHopThoai.usableBounds(new Rectangle(-1920,0,1920,1080),new Insets(24,0,50,0)));
    }
    static void render(JComponent panel,String path){var image=new BufferedImage(panel.getWidth(),panel.getHeight(),BufferedImage.TYPE_INT_RGB);var g=image.createGraphics();
        g.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING,java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(java.awt.RenderingHints.KEY_FRACTIONALMETRICS,java.awt.RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
        var manager=RepaintManager.currentManager(panel);boolean buffering=manager.isDoubleBufferingEnabled();manager.setDoubleBufferingEnabled(false);
        panel.printAll(g);manager.setDoubleBufferingEnabled(buffering);g.dispose();try{javax.imageio.ImageIO.write(image,"png",new java.io.File(path));}catch(Exception ex){throw new RuntimeException(ex);}}
    static void await(BooleanSupplier condition)throws Exception{for(int i=0;i<150;i++){Thread.sleep(20);boolean[] done={false};SwingUtilities.invokeAndWait(()->done[0]=condition.getAsBoolean());if(done[0])return;}fail("Background work did not finish");}
    static void layout(Container box){box.doLayout();for(var c:box.getComponents())if(c instanceof Container child)layout(child);}
}
