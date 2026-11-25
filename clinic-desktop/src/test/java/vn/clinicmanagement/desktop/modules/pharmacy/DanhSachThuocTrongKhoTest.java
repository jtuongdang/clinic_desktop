package vn.clinicmanagement.desktop.modules.pharmacy;
import org.junit.jupiter.api.*;
import vn.clinicmanagement.application.inventory.NhaThuocDanhSachReadCongKetNoi;
import vn.clinicmanagement.desktop.modules.pharmacy.components.DanhSachThuocTrongKhoManHinh;
import vn.clinicmanagement.desktop.core.session.*;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
class DanhSachThuocTrongKhoTest {
 static {System.setProperty("java.awt.headless","true");}
 @AfterEach void logout(){NguoiDungPhien.getInstance().clearSession();}
 @Test void catalogSearchFiltersAndSummaryUseActualRowsAndRenderReferenceRatio()throws Exception{
  PhienThuNganKiemThu.login("PHARMACIST");
  var read=new NhaThuocDanhSachReadCongKetNoi(){
   public List<Phieu> getTickets(int f,LocalDateTime from,LocalDateTime to){return List.of();}
   public Bang getList(DanhSach d,int f,Integer w,LocalDateTime from,LocalDateTime to){return new Bang(List.of(),List.of());}
   public List<Thuoc> getDrugDirectory(int facility,Integer warehouse){assertEquals(1,facility);return List.of(
       drug(1,"TH159","Thuốc kiểm thử A","Thuốc Hô Hấp",true,"LO-1","10"),
       drug(1,"TH159","Thuốc kiểm thử A","Thuốc Hô Hấp",true,"LO-2","15"),
       drug(2,"TH160","Thuốc kiểm thử B","Thuốc Kháng Sinh",null,"LO-3","20"));}
  };
  DanhSachThuocTrongKhoManHinh[] holder=new DanhSachThuocTrongKhoManHinh[1];
  SwingUtilities.invokeAndWait(()->holder[0]=new DanhSachThuocTrongKhoManHinh(read,NhaThuocGhepNoiGoc.createDefault(),null));
  for(int i=0;i<100;i++){Thread.sleep(20);SwingUtilities.invokeAndWait(()->{});if(holder[0].getTable().getRowCount()==3)break;}
  SwingUtilities.invokeAndWait(()->{
   var panel=holder[0];assertEquals(3,panel.getTable().getRowCount());
   assertNotNull(label(panel.getSidebar(),"Tất Cả Thuốc (2)"));assertNotNull(label(panel.getSidebar(),"Thuốc Hô Hấp (1)"));
   panel.getSearchField().setText("thuoc kiem thu B");assertEquals(1,panel.getTable().getRowCount());assertEquals("TH160",panel.getTable().getValueAt(0,0));panel.getSearchField().setText("");
   panel.getSummaryCheckBox().doClick();assertEquals(2,panel.getTable().getRowCount());assertEquals(0,new BigDecimal("25").compareTo((BigDecimal)panel.getTable().getValueAt(0,11)));
   var insured=label(panel.getSidebar(),"Trong danh mục BHYT (1)");click(insured);assertEquals(1,panel.getTable().getRowCount());
   click(label(panel.getSidebar(),"Tất Cả Thuốc (2)"));panel.getSummaryCheckBox().doClick();assertEquals(3,panel.getTable().getRowCount());
   for(Dimension size:List.of(new Dimension(1366,768),new Dimension(1280,720))){
    panel.setSize(size);layout(panel);layout(panel);
    assertTrue(panel.getSidebar().getWidth()<=245);
    assertEquals(22,panel.getTable().getRowHeight());
    var viewport=(JViewport)panel.getTable().getParent();
    assertEquals(viewport.getWidth(),panel.getTable().getColumnModel().getTotalColumnWidth());
    assertFalse(((JScrollPane)viewport.getParent()).getHorizontalScrollBar().isVisible());
    assertEquals(13,panel.getTable().getColumnCount());
    var image=new BufferedImage(size.width,size.height,BufferedImage.TYPE_INT_RGB);var g=image.createGraphics();panel.printAll(g);g.dispose();
    try{javax.imageio.ImageIO.write(image,"png",new java.io.File("/tmp/clinic-drug-directory-"+size.width+".png"));}catch(Exception ex){throw new RuntimeException(ex);}
   }
  });
 }
 static NhaThuocDanhSachReadCongKetNoi.Thuoc drug(int id,String code,String name,String group,Boolean insured,String batch,String stock){return new NhaThuocDanhSachReadCongKetNoi.Thuoc(id,0,code,"",name,"Hoạt chất kiểm thử","10 mg","","","Viên",group,insured,"","Kho kiểm thử",batch,LocalDate.of(2030,1,1),new BigDecimal(stock));}
 static JLabel label(Container c,String text){for(var child:c.getComponents()){if(child instanceof JLabel label&&text.equals(label.getText()))return label;if(child instanceof Container box){var result=label(box,text);if(result!=null)return result;}}return null;}
 static void click(JLabel label){assertNotNull(label);for(var listener:label.getMouseListeners())listener.mousePressed(new java.awt.event.MouseEvent(label,java.awt.event.MouseEvent.MOUSE_PRESSED,0,0,5,5,1,false));}
 static void layout(Container c){c.doLayout();for(var child:c.getComponents())if(child instanceof Container box)layout(box);}
}
