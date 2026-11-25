package vn.clinicmanagement.desktop.modules.paraclinical;

import org.junit.jupiter.api.Test;
import vn.clinicmanagement.desktop.modules.paraclinical.components.XetNghiemKetQuaBoCuc;
import vn.clinicmanagement.desktop.modules.paraclinical.model.PhieuXetNghiem;
import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class XetNghiemKetQuaBoCucTest {
    @Test void preservesReferenceRegionsAtLaptopAndReferenceSizes() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            var view = new XetNghiemKetQuaManHinh();
            var ui = (XetNghiemKetQuaBoCuc) read(view, "ui");
            for (int[] size : new int[][]{{1586,888},{1366,680},{1440,760}}) {
                view.setSize(size[0],size[1]); layout(view);
                int expectedDivider = Math.max(265, Math.min(310, (int) Math.round(size[0] * 0.215)));
                assertEquals(expectedDivider, ui.split.getDividerLocation(), 2);
                assertEquals(58, ui.toolbar.getHeight(), 2);
                assertEquals(114, ui.metadata.getHeight(), 2);
                assertEquals(8, ui.results.getColumnCount());
                assertTrue(ui.execute.getWidth() >= 70);
                assertTrue(ui.execute.getX()+ui.execute.getWidth() <= ui.controls.getWidth());
                assertTrue(ui.metadata.getY()+ui.metadata.getHeight() < ui.controls.getY());
                assertEquals(JTable.AUTO_RESIZE_OFF,ui.results.getAutoResizeMode());
            }
            assertEquals("lab",view.getViewKey());
            assertEquals("specimen",new XetNghiemManHinh().getViewKey());
            view.onHide();
        });
    }
    @Test void searchClearAndCancelledSelectionKeepActionsBoundToCurrentOrder() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            var view = new XetNghiemKetQuaManHinh();
            var ui = (XetNghiemKetQuaBoCuc)read(view,"ui");
            @SuppressWarnings("unchecked") List<PhieuXetNghiem> orders = (List<PhieuXetNghiem>)read(view,"orders");
            orders.add(order(1,"BỆNH NHÂN TỔNG HỢP A","DA_LAY_MAU"));
            orders.add(order(2,"BỆNH NHÂN TỔNG HỢP B","DA_HUY"));
            orders.add(order(3,"BỆNH NHÂN TỔNG HỢP C","DANG_THUC_HIEN"));
            orders.add(order(4,"BỆNH NHÂN TỔNG HỢP D","DA_DUYET_KET_QUA"));
            ui.keyword.setText("TỔNG HỢP A");
            assertEquals(1,ui.queueModel.getRowCount()); assertEquals("XN1",ui.orderCode.getText());
            assertEquals(2,ui.resultModel.getRowCount()); assertTrue(ui.execute.isEnabled()); assertFalse(ui.save.isEnabled());
            ui.keyword.setText("TỔNG HỢP C");
            assertEquals("XN3",ui.orderCode.getText()); assertFalse(ui.execute.isEnabled());
            assertFalse(ui.save.isEnabled(), "Wait for the selected order's results and settings before saving");
            ui.keyword.setText("TỔNG HỢP D");
            assertEquals("XN4",ui.orderCode.getText()); assertFalse(ui.execute.isEnabled()); assertFalse(ui.save.isEnabled()); assertTrue(ui.releaseResult.isEnabled());
            ui.keyword.setText("KHÔNG KHỚP");
            assertEquals(0,ui.queueModel.getRowCount()); assertFalse(ui.execute.isEnabled()); assertFalse(ui.save.isEnabled());
            ui.clear.doClick(); assertEquals(4,ui.queueModel.getRowCount());
            ui.keyword.setText("TỔNG HỢP B");
            assertEquals("XN2",ui.orderCode.getText()); assertFalse(ui.execute.isEnabled()); assertFalse(ui.save.isEnabled());
            view.onHide();
        });
    }
    private static PhieuXetNghiem order(int id,String name,String status) {
        return new PhieuXetNghiem(id,1,"XN"+id,"H"+id,id,"SYNTHETIC"+id,name,LocalDate.of(2000,1,1),"M","","DICH_VU","",0,null,
                "NGOAI_TRU","Khoa tổng hợp","Phòng tổng hợp","","Xét nghiệm tổng hợp",BigDecimal.ONE,"Nhóm tổng hợp","",status,
                LocalDateTime.of(2026,11,19,8,0),null,null,"","","","");
    }
    private static Object read(Object object,String name) {
        try { var field = object.getClass().getDeclaredField(name); field.setAccessible(true); return field.get(object); }
        catch (ReflectiveOperationException ex) { throw new AssertionError(ex); }
    }
    private static void layout(Container container) {
        container.doLayout(); for (Component child:container.getComponents()) if (child instanceof Container nested) layout(nested);
    }
}
