package vn.clinicmanagement.desktop.modules.reports;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import vn.clinicmanagement.desktop.modules.reports.catalog.*;
import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class DanhSachBaoCaoBoCucTest {
    @TempDir Path directory;
    @Test void matchesReferenceRegionsAndColumnsAtThreeWindowSizes() throws Exception {
        SwingUtilities.invokeAndWait(()->{
            var panel=new DanhMucMauBaoCaoManHinh(new BaoCaoDanhMucLuuTru(directory.resolve("catalog.xml")),null);
            var ui=panel.getCatalogUi();
            for(int[] size:new int[][]{{1586,960},{1366,680},{1440,760}}){
                panel.setSize(size[0],size[1]);layout(panel);
                int expectedDivider=Math.max(220,Math.min(280,(int)Math.round(size[0]*0.1875)));
                assertEquals(expectedDivider,ui.split.getDividerLocation(),2);
                assertEquals(58,ui.toolbar.getHeight(),2);assertEquals(24,ui.footer.getHeight(),2);
                assertEquals(3,ui.table.getColumnCount());assertEquals("Tên Báo Cáo",ui.table.getColumnName(0));
                assertEquals("Tên Tập Tin",ui.table.getColumnName(1));assertEquals("ID",ui.table.getColumnName(2));
                assertTrue(ui.save.getX()+ui.save.getWidth()<=ui.toolbar.getWidth());
                Rectangle text=new Rectangle(),icon=new Rectangle(),bounds=new Rectangle(0,0,ui.save.getWidth(),ui.save.getHeight());
                String caption=SwingUtilities.layoutCompoundLabel(ui.save,ui.save.getFontMetrics(ui.save.getFont()),ui.save.getText(),ui.save.getIcon(),
                        ui.save.getVerticalAlignment(),ui.save.getHorizontalAlignment(),ui.save.getVerticalTextPosition(),ui.save.getHorizontalTextPosition(),bounds,icon,text,ui.save.getIconTextGap());
                assertEquals(ui.save.getText(),caption);
                assertTrue(ui.keyword.getWidth()>500);assertTrue(ui.tree.getRowCount()>20);
            }
            assertEquals("report-templates",panel.getViewKey());
            assertTrue(ui.tree.getPathForRow(0).toString().contains("("+panel.getCatalogModel().entries().size()+")"));
        });
    }
    @Test void searchRetainsAncestorsAndMatchesVietnameseWithoutAccents() {
        var model=new BaoCaoDanhMucMoHinh();model.replace(List.of(entry("one","Nhóm A","Phiếu khám"),entry("two","Nhóm B","Thu viện phí")));
        model.filter("phieu kham","","");assertEquals(3,model.getRowCount());
        assertEquals(BaoCaoDanhMucMoHinh.Kind.CATEGORY,model.row(0).kind());assertEquals("one",model.row(2).key());
        model.filter("không khớp","","");assertEquals(0,model.getRowCount());
        model.filter("","","Nhóm B");assertEquals("two",model.row(2).key());
    }
    @Test void collapseAndExpandDoNotLoseReports() {
        var model=new BaoCaoDanhMucMoHinh();model.replace(List.of(entry("one","Nhóm A","Phiếu khám")));
        model.toggle(0);assertEquals(1,model.getRowCount());assertEquals(1,model.entries().size());
        model.expandAll();assertEquals(3,model.getRowCount());
    }
    @Test void reorderAndSaveRoundTripKeepsUnicodeAndDistinctKeysForSharedIds() throws Exception {
        var store=new BaoCaoDanhMucLuuTru(directory.resolve("catalog.xml"));var model=new BaoCaoDanhMucMoHinh();
        model.replace(List.of(entry("one","Nhóm A","Phiếu đầu"),entry("two","Nhóm A","Phiếu thứ hai"),entry("three","Nhóm B","Phiếu khác")));
        assertTrue(model.move("two",-1));assertEquals("two",model.entries().get(0).key());
        store.save(model.entries());assertEquals(model.entries(),store.load());
        model.replace(store.load());assertTrue(model.move("g:Danh mục/Nhóm B",-1));assertEquals("three",model.entries().get(0).key());
        assertFalse(model.move("three",-1));
    }
    @Test void addingEntryUpdatesActualCountAndRejectsIncompleteMetadata() {
        var model=new BaoCaoDanhMucMoHinh();model.replace(List.of());model.add(entry("one","Nhóm A","Phiếu khám"));
        assertEquals(1,model.entries().size());assertEquals(3,model.getRowCount());
        assertThrows(IllegalArgumentException.class,()->new BaoCaoMau("","","","","","",""));
    }
    private static BaoCaoMau entry(String key,String group,String name){return new BaoCaoMau(key,"Danh mục",group,name,"Mẫu tổng hợp.doc","1001","");}
    private static void layout(Container root){root.doLayout();for(Component c:root.getComponents())if(c instanceof Container child)layout(child);}
}
