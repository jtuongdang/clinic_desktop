package vn.clinicmanagement.desktop.modules.examination.components.print;

import org.junit.jupiter.api.Test;
import vn.clinicmanagement.domain.examination.LuotKham;
import vn.clinicmanagement.domain.inventory.MatHang;
import vn.clinicmanagement.domain.prescription.*;
import vn.clinicmanagement.domain.common.enums.*;
import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ToDieuTriBoCucTest {
    @Test void fillsOwnerMonitorWithoutCoveringSystemBars() {
        assertEquals(new Rectangle(0,25,1440,830),ToDieuTriPrintHopThoai.usableBounds(new Rectangle(0,0,1440,900),new Insets(25,0,45,0)));
        assertEquals(new Rectangle(-1920,0,1920,1040),ToDieuTriPrintHopThoai.usableBounds(new Rectangle(-1920,0,1920,1080),new Insets(0,0,40,0)));
    }
    @Test void keepsPaperCenteredAndReadableAtReferenceAndLaptopSizes() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            ToDieuTriPrintManHinh viewer=new ToDieuTriPrintManHinh(data("Diễn biến minh họa"));
            for(int[] size:new int[][]{{1586,959},{1366,736},{1440,824}}) {
                viewer.setSize(size[0],size[1]);layout(viewer);
                JPanel paper=viewer.getPaperPanel();
                assertEquals(844*size[0]/1586.0,paper.getWidth(),2);
                assertTrue(paper.getX()>=0);assertTrue(paper.getWidth()<=paper.getParent().getWidth());
                assertEquals((paper.getParent().getWidth()-paper.getWidth())/2,paper.getX(),1);
                assertEquals(0,paper.getY());
            }
        });
    }
    @Test void longColumnsWrapAndExtendPageRatherThanOverlapSignatures() {
        ToDieuTriTrangGiay shortPage=new ToDieuTriTrangGiay(data("Diễn biến minh họa"));
        ToDieuTriTrangGiay longPage=new ToDieuTriTrangGiay(data("Nội dung dài cần xuống dòng trong cột. ".repeat(140)));
        assertEquals(1194,shortPage.referenceHeight());
        assertTrue(longPage.referenceHeight()>1194);
        assertTrue(longPage.leftSignatureBounds().getY()>shortPage.leftSignatureBounds().getY());
        assertTrue(longPage.leftSignatureBounds().getMaxY()+30<longPage.referenceHeight());
        assertTrue(longPage.leftSignatureBounds().getMaxX()<=longPage.rightSignatureBounds().getX());
    }
    @Test void mapsSuppliesSeparatelyAndExcludesCancelledOrders() {
        LuotKham visit=new LuotKham();visit.setId(77);
        DonThuoc active=new DonThuoc();active.setChiTietDonThuocs(List.of(line(LoaiMatHang.THUOC,"Thuốc tổng hợp"),line(LoaiMatHang.VAT_TU,"Vật tư tổng hợp")));
        DonThuoc cancelled=new DonThuoc();cancelled.setTrangThai(TrangThaiDonThuoc.HUY);cancelled.setChiTietDonThuocs(List.of(line(LoaiMatHang.THUOC,"Đơn đã hủy")));
        visit.setDonThuocs(List.of(active,cancelled));
        var data=ToDieuTriPrintManHinh.fromVisit(visit,null);
        assertEquals(1,data.medicationLines().size());assertEquals(1,data.supplyLines().size());
        assertTrue(data.supplyLines().get(0).contains("Vật tư tổng hợp"));
        assertFalse(data.medicationLines().get(0).contains("Đơn đã hủy"));
        assertEquals("",data.sheetNumber());assertEquals("77",data.visitId());
    }
    private static ChiTietDonThuoc line(LoaiMatHang type,String name) {
        MatHang item=new MatHang();item.setLoaiItem(type);item.setTenItem(name);
        ChiTietDonThuoc line=new ChiTietDonThuoc();line.setItem(item);line.setSoLuongKe(BigDecimal.ONE);return line;
    }
    private static ToDieuTriPrintManHinh.TreatmentSheetData data(String progress) {
        return new ToDieuTriPrintManHinh.TreatmentSheetData("","77","SYNTHETIC","BỆNH NHÂN TỔNG HỢP","25","Nữ","Viện phí","Địa chỉ tổng hợp",
                "Khoa tổng hợp","","","Chẩn đoán tổng hợp","19/11/2026","Lý do tổng hợp",progress,"","","",
                List.of("Thuốc tổng hợp"),List.of("Vật tư tổng hợp"),List.of("Dịch vụ tổng hợp"),"Bác sĩ tổng hợp");
    }
    private static void layout(Container container) {container.doLayout();for(Component c:container.getComponents())if(c instanceof Container nested)layout(nested);}
}
