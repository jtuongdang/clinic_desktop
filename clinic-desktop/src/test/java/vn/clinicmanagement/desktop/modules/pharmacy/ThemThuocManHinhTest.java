package vn.clinicmanagement.desktop.modules.pharmacy;
import org.junit.jupiter.api.Test;
import vn.clinicmanagement.desktop.modules.pharmacy.components.ThemThuocManHinh;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import static org.junit.jupiter.api.Assertions.*;
class ThemThuocManHinhTest {
 static {System.setProperty("java.awt.headless","true");}
 @Test void allFieldsAndSaveAreVisibleOnLaptopAndCollectValues()throws Exception{
  SwingUtilities.invokeAndWait(()->{
   var panel=new ThemThuocManHinh(null,null);
   ((JTextField)panel.getField("maThuoc")).setText("THU-TEST");((JTextField)panel.getField("tenThuoc")).setText("Thuốc kiểm thử");
   var unit=(JComboBox<?>)panel.getField("donViTinh");unit.getEditor().setItem("Viên");
   ((JTextField)panel.getField("giaBan")).setText("25000.00");((JCheckBox)panel.getField("keDon")).setSelected(true);
   assertEquals("Viên",panel.collectValues().get("donViTinh"));assertEquals("true",panel.collectValues().get("keDon"));
   assertTrue(panel.collectValues().size()>=65);
   for(var size:java.util.List.of(new Dimension(1280,680),new Dimension(1366,700))){
    panel.setSize(size);layout(panel);layout(panel);
    for(String key:panel.collectValues().keySet()){
     var input=panel.getField(key);var rectangle=SwingUtilities.convertRectangle(input.getParent(),input.getBounds(),panel);
     assertTrue(rectangle.x>=0&&rectangle.y>=0&&rectangle.x+rectangle.width<=size.width&&rectangle.y+rectangle.height<=size.height,"Trường bị khuất: "+key+" "+rectangle);
     assertTrue(input.getWidth()>35,"Trường quá hẹp: "+key);
    }
    assertFalse(find(panel,JScrollPane.class).getVerticalScrollBar().isVisible());
    assertTrue(panel.getSaveButton().isVisible());assertEquals(43,panel.getSaveButton().getParent().getHeight());
    BufferedImage image=new BufferedImage(size.width,size.height,BufferedImage.TYPE_INT_RGB);var graphics=image.createGraphics();panel.printAll(graphics);graphics.dispose();
    try{javax.imageio.ImageIO.write(image,"png",new java.io.File("/tmp/clinic-them-thuoc-"+size.width+".png"));}catch(Exception ex){throw new RuntimeException(ex);}
   }
  });
 }
 static void layout(Container c){c.doLayout();for(var child:c.getComponents())if(child instanceof Container box)layout(box);}
 static <T>T find(Container c,Class<T> type){for(var child:c.getComponents()){if(type.isInstance(child))return type.cast(child);if(child instanceof Container box){var found=find(box,type);if(found!=null)return found;}}return null;}
}
