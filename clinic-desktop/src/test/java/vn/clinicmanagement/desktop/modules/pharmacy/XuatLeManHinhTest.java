package vn.clinicmanagement.desktop.modules.pharmacy;

import org.junit.jupiter.api.*;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.KetQua;
import vn.clinicmanagement.application.inventory.retail.XuatLe;
import vn.clinicmanagement.desktop.core.session.*;
import vn.clinicmanagement.desktop.modules.pharmacy.components.XuatLeManHinh;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class XuatLeManHinhTest {
    @AfterEach void logout(){NguoiDungPhien.getInstance().clearSession();}
    @Test void selectLotAddLineAndSaveOnceAtLaptopSize() throws Exception {
        PhienThuNganKiemThu.login("PHARMACIST");
        var calls=new AtomicInteger();var refreshed=new CountDownLatch(1);
        var service=new XuatLe(){
            public List<Warehouse> warehouses(PhanQuyenNguCanh a){return List.of(new Warehouse(7,"Nhà thuốc kiểm thử"));}
            public List<Lot> lots(int warehouse,PhanQuyenNguCanh a){return List.of(new Lot(3,1,"TH01","Thuốc kiểm thử","Viên","10 mg","","","","",
                    "L01",LocalDate.now().plusYears(1),BigDecimal.TEN,BigDecimal.ONE,BigDecimal.valueOf(100),BigDecimal.TEN,"12345",""));}
            public KetQua<String> save(Ticket t,PhanQuyenNguCanh a){
                assertEquals(7,t.warehouseId());assertEquals(0,t.total().compareTo(BigDecimal.valueOf(220)));
                calls.incrementAndGet();return KetQua.success(t.number());
            }
        };
        XuatLeManHinh[] form={null};
        SwingUtilities.invokeAndWait(()->form[0]=new XuatLeManHinh(service,7,refreshed::countDown));
        for(int attempt=0;attempt<100;attempt++){
            boolean[] ready={false};
            SwingUtilities.invokeAndWait(()->ready[0]=component(form[0],JButton.class,"…").isEnabled()
                    && component(form[0],JLabel.class,"Đang tải kho thuốc…")==null
                    && component(form[0],JLabel.class,"Đang tải lô thuốc…")==null);
            if(ready[0])break;
            Thread.sleep(20);
        }
        SwingUtilities.invokeAndWait(()->{
            var p=form[0];p.setSize(1280,680);ThemThuocManHinhTest.layout(p);ThemThuocManHinhTest.layout(p);
            var scan=component(p,JTextField.class,"Mã vạch");scan.setText("12345");scan.postActionEvent();
            component(p,JTextField.class,"Số lượng").setText("2");
            component(p,JButton.class,"Cập Nhật (Ctrl+A)").doClick();
            var table=ThemThuocManHinhTest.find(p,JTable.class);
            assertEquals(12,table.getColumnCount());assertEquals(1,table.getRowCount());assertEquals("220",table.getValueAt(0,7));
            assertEquals(((JViewport)table.getParent()).getWidth(),table.getColumnModel().getTotalColumnWidth());
            assertTrue(table.getHeight()>300);assertNotNull(component(p,JLabel.class,"220đ"));
            var image=new BufferedImage(1280,680,BufferedImage.TYPE_INT_RGB);
            var g=image.createGraphics();p.paint(g);g.dispose();
            try{javax.imageio.ImageIO.write(image,"png",new java.io.File("/tmp/clinic-xuat-le-preview.png"));}catch(Exception ex){throw new RuntimeException(ex);}
            var button=component(p,JButton.class,"Lưu (F8)");button.doClick();button.doClick();
        });
        assertTrue(refreshed.await(5,TimeUnit.SECONDS));assertEquals(1,calls.get());
        SwingUtilities.invokeAndWait(()->assertTrue(component(form[0],JButton.class,"In lại (F9)").isEnabled()));
    }
    private static <T> T component(Container c,Class<T> type,String name){
        for(var child:c.getComponents()){
            String text=child instanceof AbstractButton b?b.getText():child instanceof JLabel l?l.getText():child.getName();
            if(type.isInstance(child) && name.equals(text))return type.cast(child);
            if(child instanceof Container nested){T result=component(nested,type,name);if(result!=null)return result;}
        }
        return null;
    }
}
