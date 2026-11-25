package vn.clinicmanagement.desktop.modules.pharmacy;
import org.junit.jupiter.api.*;
import vn.clinicmanagement.desktop.modules.pharmacy.components.*;
import vn.clinicmanagement.desktop.core.session.*;
import vn.clinicmanagement.application.inventory.NhaThuocDanhSachReadCongKetNoi;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;
class NhaThuocDanhSachMenuTest {
 static {System.setProperty("java.awt.headless","true");}
 @AfterEach void logout(){NguoiDungPhien.getInstance().clearSession();}
 @Test void menuMatchesReferenceAndDispatchesAllThreeLists(){
  var selected=new AtomicReference<NhaThuocDanhSachReadCongKetNoi.DanhSach>();var menu=new NhaThuocDanhSachMenu(selected::set);
  assertEquals(3,menu.getComponentCount());
  String[] titles={"Danh Sách Thuốc Trong Kho","Danh Sách Phiếu Xuất Lẻ","Dự Trù Thuốc"};
  for(int i=0;i<3;i++){var item=(JMenuItem)menu.getComponent(i);assertEquals(titles[i],item.getText());item.doClick();assertEquals(NhaThuocDanhSachReadCongKetNoi.DanhSach.values()[i],selected.get());}
 }
 @Test void liveRowsDriveCountsStatusAndListViewsWithoutSampleFallback()throws Exception{
  PhienThuNganKiemThu.login("PHARMACIST");
  var now=LocalDateTime.now();
  var read=new NhaThuocDanhSachReadCongKetNoi(){
   public List<Phieu> getTickets(int facility,LocalDateTime from,LocalDateTime to){assertEquals(1,facility);return List.of(
    new Phieu(11,"NHAP_KHO","TEST-NHAP","HOAN_THANH",now,now,null,"",1,"Kho kiểm thử","","Nhập nhà cung cấp",new BigDecimal("100000")),
    new Phieu(12,"XUAT_KHO","TEST-HUY","DA_XUAT",now,now,now,"Người hủy thử nghiệm",1,"Kho kiểm thử","","BAN_THUOC",new BigDecimal("50000")));}
   public List<Thuoc> getDrugDirectory(int facility,Integer warehouse){return List.of(new Thuoc(1,0,"TEST-THUOC","","Thuốc kiểm thử","","","","","Viên","",null,"","Kho kiểm thử","",null,BigDecimal.TEN));}
   public Bang getList(DanhSach type,int facility,Integer warehouse,LocalDateTime from,LocalDateTime to){return new Bang(List.of("Tên thuốc","Tồn kho"),List.of(List.of("Thuốc kiểm thử",BigDecimal.TEN)));}
  };
  NhaThuocDispenseManHinh[] panel=new NhaThuocDispenseManHinh[1];
  SwingUtilities.invokeAndWait(()->panel[0]=new NhaThuocDispenseManHinh(NhaThuocGhepNoiGoc.createDefault(),read));
  await(()->panel[0].getTablePanel().getAllTickets().size()==2);
  SwingUtilities.invokeAndWait(()->{assertEquals(2,panel[0].getTreePanel().getAllCount());panel[0].getTablePanel().filterStatus("CANCELLED");assertEquals(1,panel[0].getTablePanel().getDisplayedRows().size());assertEquals("TEST-HUY",panel[0].getTablePanel().getDisplayedRows().get(0).code);panel[0].getTreePanel().selectRow(0);});
  SwingUtilities.invokeAndWait(()->{
   assertEquals("Xuất lẻ (1)",panel[0].getTreePanel().getLblExport().getText());
   assertEquals("Xuất bệnh nhân (0)",panel[0].getTreePanel().getLblPrescription().getText());
   panel[0].getTreePanel().selectRow(2);assertEquals(1,panel[0].getTablePanel().getDisplayedRows().size());
   panel[0].getTreePanel().selectRow(3);assertEquals(0,panel[0].getTablePanel().getDisplayedRows().size());
   panel[0].getTreePanel().selectRow(0);
  });
  JPanel[] stock=new JPanel[1];SwingUtilities.invokeAndWait(()->stock[0]=panel[0].createListPanel(NhaThuocDanhSachReadCongKetNoi.DanhSach.THUOC_TRONG_KHO));
  await(()->find(stock[0],JTable.class).getRowCount()==1);
  SwingUtilities.invokeAndWait(()->{
   assertEquals("TEST-THUOC",find(stock[0],JTable.class).getValueAt(0,0));
   var view=panel[0];view.setSize(1586,765);layout(view);layout(view);
   var menu=view.getSubToolbar().getListMenu();menu.setSize(menu.getPreferredSize());layout(menu);
   view.getSubToolbar().getBtnList().putClientProperty("menuTaoPhieuMo",true);
   var image=new BufferedImage(1586,765,BufferedImage.TYPE_INT_RGB);var g=image.createGraphics();view.printAll(g);
   var anchor=SwingUtilities.convertPoint(view.getSubToolbar().getBtnList(),0,view.getSubToolbar().getBtnList().getHeight(),view);g.translate(anchor.x,anchor.y);menu.printAll(g);g.dispose();
   try{javax.imageio.ImageIO.write(image,"png",new java.io.File("/tmp/clinic-pharmacy-menu-preview.png"));}catch(Exception e){throw new RuntimeException(e);}
  });
 }
 interface Check{boolean get();}
 static void await(Check check)throws Exception{for(int i=0;i<100;i++){Thread.sleep(20);boolean[] ready={false};SwingUtilities.invokeAndWait(()->ready[0]=check.get());if(ready[0])return;}fail("Background load did not complete");}
 static void layout(Container c){c.doLayout();for(var child:c.getComponents())if(child instanceof Container box)layout(box);}
 static <T> T find(Container c,Class<T> type){for(var child:c.getComponents()){if(type.isInstance(child))return type.cast(child);if(child instanceof Container box){T result=find(box,type);if(result!=null)return result;}}return null;}
}
