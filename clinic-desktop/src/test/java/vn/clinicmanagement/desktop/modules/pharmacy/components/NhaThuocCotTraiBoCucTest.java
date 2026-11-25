package vn.clinicmanagement.desktop.modules.pharmacy.components;
import org.junit.jupiter.api.Test;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;
class NhaThuocCotTraiBoCucTest {
 static {System.setProperty("java.awt.headless","true");}
 @Test void groupsCollapseIndependentlyAndStatusSelectionClearsTypeFilter()throws Exception{
  SwingUtilities.invokeAndWait(()->{
   var panel=new NhaThuocDanhMucCayManHinh();panel.setWarehouseTitle("Nhà thuốc test");panel.updateTicketCounts(4,2,1,1);panel.updateStatusCounts(3,1,3,1,1);
   var category=new AtomicReference<String>();var status=new AtomicReference<String>();
   panel.setOnCategorySelected(value->category.set(value.ticketType()));panel.setOnStatusSelected(status::set);
   panel.selectRow(2);assertEquals("XUAT_LE",category.get());
   panel.getGroupToggle("Loại Phiếu").doClick();assertFalse(panel.isGroupExpanded("Loại Phiếu"));assertTrue(panel.isGroupExpanded("Trạng thái"));
   panel.getGroupToggle("Loại Phiếu").doClick();assertTrue(panel.isGroupExpanded("Loại Phiếu"));
   var cancelled=findLabel(panel,"Đã hủy (1)");assertNotNull(cancelled);
   for(var listener:cancelled.getMouseListeners())listener.mousePressed(new java.awt.event.MouseEvent(cancelled,java.awt.event.MouseEvent.MOUSE_PRESSED,0,0,10,10,1,false));
   assertEquals("ALL",category.get());assertEquals("CANCELLED",status.get());
   panel.selectRow(0);panel.setSize(318,693);layout(panel);layout(panel);
   assertEquals(43,panel.getLblTitle().getHeight());assertEquals(34,panel.getRowAllTickets().getHeight());assertEquals(32,panel.getRowImport().getHeight());
   var image=new BufferedImage(318,505,BufferedImage.TYPE_INT_RGB);var graphics=image.createGraphics();panel.printAll(graphics);graphics.dispose();
   try{javax.imageio.ImageIO.write(image,"png",new java.io.File("/tmp/clinic-pharmacy-sidebar.png"));}catch(Exception ex){throw new RuntimeException(ex);}
  });
 }
 static JLabel findLabel(Container c,String value){for(var child:c.getComponents()){if(child instanceof JLabel label&&value.equals(label.getText()))return label;if(child instanceof Container box){var found=findLabel(box,value);if(found!=null)return found;}}return null;}
 static void layout(Container c){c.doLayout();for(var child:c.getComponents())if(child instanceof Container box)layout(box);}
}
