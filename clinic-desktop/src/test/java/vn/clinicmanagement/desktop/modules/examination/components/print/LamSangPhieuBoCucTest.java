package vn.clinicmanagement.desktop.modules.examination.components.print;


import vn.clinicmanagement.desktop.modules.print.examination.ClsMaVach;
import vn.clinicmanagement.desktop.modules.print.examination.LamSangPhieuTrang;
import vn.clinicmanagement.desktop.modules.print.examination.LamSangYeuCauPrintPreviewManHinh;
import org.junit.jupiter.api.Test;
import vn.clinicmanagement.desktop.modules.print.examination.LamSangYeuCauPrintPreviewManHinh.*;
import vn.clinicmanagement.desktop.modules.print.reception.PhieuDangKyKhamPdf;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.print.Printable;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import static org.junit.jupiter.api.Assertions.*;

class LamSangPhieuBoCucTest {
    static ClinicalOrderData sample(List<OrderItem> items) {
        return new ClinicalOrderData("22000285","BỆNH NHÂN KIỂM THỬ","2000","26","Nam","","",
                "Phường Đáp Cầu, TP. Bắc Ninh, Bắc Ninh","PK Nhi I","H81.0 - Bệnh Ménière","22/000010","22000285","214302","Viện phí","14:57, ngày 19 tháng 11 năm 2026",items);
    }
    static List<OrderItem> items(){return List.of(
        new OrderItem("Đông máu","Định lượng Fibrinogen (Tên khác: Định lượng yếu tố I), phương pháp Clauss - phương pháp trực tiếp, bằng máy tự động (Thu phí)","1","Lần","","PHÒNG LẤY MẪU BỆNH PHẨM","Đông máu","2","17176","Tầng 2 - Khoa xét nghiệm",""),
        new OrderItem("Điện tim","Điện tim thường (Thu phí)","1","Lần","","PHÒNG ĐIỆN TIM","Thăm dò chức năng","3","","Tầng 2 - B225",""));}
    @Test void matchesReferenceGeometryAndScales() throws Exception {
        SwingUtilities.invokeAndWait(()->{
            com.formdev.flatlaf.FlatLightLaf.setup();
            var panel=new LamSangYeuCauPrintPreviewManHinh(sample(items()));
            for(int[] size:new int[][]{{1586,964},{1366,736},{1440,824}}){panel.setSize(size[0],size[1]);layout(panel);layout(panel);
                double ratio=size[0]/1586.0;
                assertEquals(198*ratio,find(panel,"cls.sidebar").getWidth(),2);
                assertEquals(68*ratio,find(panel,"cls.toolbar").getHeight(),2);
                assertEquals(858*ratio,find(panel,"cls.paper").getWidth(),2);
                assertEquals(1,panel.getDocument().pageCount());
                var image=new BufferedImage(size[0],size[1],BufferedImage.TYPE_INT_RGB);var g=image.createGraphics();panel.printAll(g);g.dispose();
                try{ImageIO.write(image,"png",Path.of("/tmp/cls-preview-"+size[0]+".png").toFile());}catch(Exception e){throw new RuntimeException(e);}
            }
            var paper=panel.getDocument();paper.setDisplayScale(1);paper.setSize(paper.getPreferredSize());
            var image=new BufferedImage(858,1214,BufferedImage.TYPE_INT_RGB);var g=image.createGraphics();paper.printAll(g);g.dispose();
            assertEquals(Color.WHITE.getRGB(),image.getRGB(100,100));
        });
    }
    @Test void paginatesLongServicesAndExportsEveryPage() throws Exception {
        List<OrderItem> list=new ArrayList<>();for(int i=0;i<70;i++)list.add(new OrderItem("Xét nghiệm","Dịch vụ "+i+" "+"Tên dịch vụ dài cần được xuống dòng đầy đủ ".repeat(5),"1","Lần","Ghi chú đầy đủ"));
        var doc=new LamSangPhieuTrang(sample(list));assertTrue(doc.pageCount()>3);
        var image=new BufferedImage(858,1214,BufferedImage.TYPE_INT_RGB);var g=image.createGraphics();
        for(int i=0;i<doc.pageCount();i++)assertEquals(Printable.PAGE_EXISTS,doc.print(g,doc.pageFormat(),i));
        assertEquals(Printable.NO_SUCH_PAGE,doc.print(g,doc.pageFormat(),doc.pageCount()));g.dispose();
        Path pdf=Files.createTempFile("cls-test-",".pdf");
        try {PhieuDangKyKhamPdf.write(doc,doc.pageCount(),pdf,new int[]{0,doc.pageCount()-1},doc.pageFormat());String raw=Files.readString(pdf,java.nio.charset.StandardCharsets.ISO_8859_1);assertTrue(raw.startsWith("%PDF-1.4"));assertTrue(raw.contains("/Count 2"));}finally{Files.deleteIfExists(pdf);}
    }
    @Test void code39UsesActualPidAndRejectsUnsupportedInput(){assertFalse(java.util.Arrays.equals(ClsMaVach.encode("22000285"),ClsMaVach.encode("22000286")));assertEquals(0,ClsMaVach.encode("mã bệnh nhân").length);assertEquals(0,ClsMaVach.encode("").length);}
    static void layout(Container box){box.doLayout();for(Component c:box.getComponents())if(c instanceof Container ct)layout(ct);}
    static Component find(Container box,String name){for(Component c:box.getComponents()){if(name.equals(c.getName()))return c;if(c instanceof Container ct){Component result=find(ct,name);if(result!=null)return result;}}return null;}
}
