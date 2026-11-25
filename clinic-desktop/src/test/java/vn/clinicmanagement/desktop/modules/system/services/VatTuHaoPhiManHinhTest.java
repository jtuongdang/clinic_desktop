package vn.clinicmanagement.desktop.modules.system.services;

import org.junit.jupiter.api.*;
import vn.clinicmanagement.application.catalog.*;
import vn.clinicmanagement.application.catalog.VatTuHaoPhiCauHinh.*;
import vn.clinicmanagement.desktop.core.session.*;
import javax.swing.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;
import static vn.clinicmanagement.desktop.modules.system.services.DichVuCauHinhManHinhTest.*;

class VatTuHaoPhiManHinhTest {
    static {System.setProperty("java.awt.headless","true");com.formdev.flatlaf.FlatLightLaf.setup();vn.clinicmanagement.desktop.core.config.UngDungPhongChu.installGlobally();}
    @BeforeEach void login(){PhienThuNganKiemThu.login("ADMIN");}
    @AfterEach void logout(){NguoiDungPhien.getInstance().clearSession();}
    static final String MATERIAL="UREA/UREA NITROGEN; 4x53ml+4x53ml/ Hộp";
    static class Materials extends Source {
        public List<VatTuHaoPhi> loadMaterials(){return List.of(new VatTuHaoPhi(8,"897.M21.60",MATERIAL,"ml","4x53ml+4x53ml/ Hộp","Vật tư hóa chất",3,"Hóa chất khoa sinh hóa",new BigDecimal("22050"),new BigDecimal("22050"),new BigDecimal("22050")));}
    }
    static VatTuHaoPhiCauHinh definition(){return new VatTuHaoPhiCauHinh(Stage.RESULT,false,List.of(new Line(8,3,"897.M21.60",MATERIAL,"ml","4x53ml+4x53ml/ Hộp","Hóa chất khoa sinh hóa",BigDecimal.ONE)));}
    static VatTuHaoPhiManHinh selector(Materials source,String definition,java.util.function.Consumer<VatTuHaoPhiCauHinh> accepted)throws Exception{
        VatTuHaoPhiManHinh[] result={null};SwingUtilities.invokeAndWait(()->result[0]=new VatTuHaoPhiManHinh(source,definition,"",accepted));await(()->result[0].save.isEnabled());return result[0];
    }
    @Test void restoresMaterialTimingAndQuantityAndRendersReference()throws Exception{
        var panel=selector(new Materials(),definition().encode(),v->{});
        SwingUtilities.invokeAndWait(()->{panel.setSize(1586,959);layout(panel);layout(panel);assertEquals(320,panel.columns.getDividerLocation(),2);
            assertEquals(8,panel.table.getColumnCount());assertEquals(1,panel.table.getRowCount());assertEquals(MATERIAL,panel.name.getText());assertEquals("ml",panel.unit.getText());
            assertEquals(1,panel.chosenModel.size());assertEquals(Stage.RESULT,panel.current().stage());assertTrue(panel.canClose());render(panel,"/tmp/clinic-material-consumption-preview.png");panel.dispose();});
    }
    @Test void invalidQuantityDoesNotAddAndReplacingSameMaterialDoesNotDuplicate()throws Exception{
        var panel=selector(new Materials(),"",v->{});
        SwingUtilities.invokeAndWait(()->{panel.quantity.setText("-1");panel.add.doClick();assertEquals(0,panel.chosenModel.size());
            panel.quantity.setText("0.5");panel.add.doClick();assertEquals(1,panel.chosenModel.size());
            panel.quantity.setText("2");panel.add.doClick();assertEquals(1,panel.chosenModel.size());assertEquals(new BigDecimal("2"),panel.current().lines().get(0).quantity());
            panel.chosen.setSelectedIndex(0);panel.chosen.getActionMap().get("remove").actionPerformed(new java.awt.event.ActionEvent(panel.chosen,0,"remove"));assertEquals(0,panel.chosenModel.size());panel.dispose();});
    }
    @Test void childSaveRequiresTimingAndOnlyReturnsConfigurationWithoutWritingCatalog()throws Exception{
        Materials source=new Materials();AtomicReference<VatTuHaoPhiCauHinh> accepted=new AtomicReference<>();var panel=selector(source,"",accepted::set);
        SwingUtilities.invokeAndWait(()->{panel.add.doClick();panel.save.doClick();assertNull(accepted.get());assertEquals(0,source.writes);
            // Restoring a configured stage exercises the same save path as selecting its radio button.
            for(var c:panel.getComponents())if(c instanceof JPanel footer)selectResult(footer);
            panel.rounding.setSelected(true);panel.save.doClick();assertNotNull(accepted.get());assertTrue(accepted.get().rounding());assertEquals(Stage.RESULT,accepted.get().stage());assertEquals(0,source.writes);panel.dispose();});
    }
    @Test void materialEllipsisOpensSelectorAndParentSaveRetainsStructuredChoice()throws Exception{
        Materials source=new Materials();AtomicReference<DichVuCauHinh> saved=new AtomicReference<>();AtomicReference<String> raw=new AtomicReference<>();DichVuCauHinhManHinh[] parent={null};
        SwingUtilities.invokeAndWait(()->parent[0]=new DichVuCauHinhManHinh(DichVuHang.from(source.original),source,saved::set,(encoded,legacy,accepted)->{raw.set(legacy);accepted.accept(definition());}));await(()->parent[0].save.isEnabled());
        SwingUtilities.invokeAndWait(()->{var panel=parent[0];panel.pickers.get("consumedMaterials").doClick();assertEquals(source.original.value("consumedMaterials"),raw.get());assertEquals(0,source.writes);
            assertFalse(((JTextField)panel.fields.get("consumedMaterials")).isEditable());assertTrue(panel.isDirty());panel.save.doClick();});await(()->saved.get()!=null);
        SwingUtilities.invokeAndWait(()->{var value=saved.get();var restored=VatTuHaoPhiCauHinh.decode(value.value("consumedMaterialsDefinition"));assertEquals(definition(),restored);
            assertEquals(source.original.value("consumedMaterials"),value.value("consumedMaterialsLegacy"));assertEquals(definition().summary(),value.value("consumedMaterials"));assertEquals(1,source.writes);parent[0].dispose();});
    }
    @Test void noMaterialsDoesNotInventScreenshotRows()throws Exception{
        Materials source=new Materials(){public List<VatTuHaoPhi> loadMaterials(){return List.of();}};var panel=selector(source,"",v->{});
        SwingUtilities.invokeAndWait(()->{assertEquals(0,panel.model.getRowCount());assertFalse(panel.add.isEnabled());assertEquals(0,panel.chosenModel.size());panel.dispose();});
    }
    @Test void codecPreservesUnicodeFractionsAndRejectsCorruptDefinition(){
        var config=definition();assertEquals(config,VatTuHaoPhiCauHinh.decode(config.encode()));assertThrows(IllegalArgumentException.class,()->VatTuHaoPhiCauHinh.decode("not xml"));
        assertThrows(IllegalArgumentException.class,()->new VatTuHaoPhiCauHinh(Stage.ORDER,false,List.of(config.lines().get(0),config.lines().get(0))));
        var fraction=new VatTuHaoPhiCauHinh(Stage.EXECUTION_TIME,true,List.of(new Line(8,3,"VT8","Bơm <5ml> & ống","ml","Hộp","Kho thử",new BigDecimal("0.125"))));
        assertEquals(fraction,VatTuHaoPhiCauHinh.decode(fraction.encode()));
    }
    @Test void openingLegacyDefinitionCannotClearItBySavingAnEmptySelection()throws Exception{
        VatTuHaoPhiManHinh[] panel={null};SwingUtilities.invokeAndWait(()->panel[0]=new VatTuHaoPhiManHinh(new Materials(),"","{\"RoomID\":289}",v->fail("Legacy configuration must not be cleared implicitly")));
        await(()->panel[0].save.isEnabled());SwingUtilities.invokeAndWait(()->{panel[0].save.doClick();assertTrue(panel[0].status.getText().contains("Cấu hình cũ được giữ nguyên"));panel[0].dispose();});
    }
    static void selectResult(java.awt.Container parent){for(var c:parent.getComponents()){if(c instanceof JRadioButton radio&&radio.getText().equals(Stage.RESULT.title()))radio.setSelected(true);else if(c instanceof java.awt.Container child)selectResult(child);}}
}
