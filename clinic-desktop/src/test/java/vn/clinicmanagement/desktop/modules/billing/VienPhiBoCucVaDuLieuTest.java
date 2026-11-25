package vn.clinicmanagement.desktop.modules.billing;
import org.junit.jupiter.api.*;
import vn.clinicmanagement.desktop.core.session.*;
import vn.clinicmanagement.application.billing.*;
import vn.clinicmanagement.domain.billing.*;
import vn.clinicmanagement.domain.common.enums.*;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.time.*;
import java.util.Map;
import java.util.Optional;
import javax.imageio.ImageIO;
import static org.junit.jupiter.api.Assertions.*;
class VienPhiBoCucVaDuLieuTest {
 @AfterEach void logout(){NguoiDungPhien.getInstance().clearSession();}
 @Test void ratioAndMetadataAndReceiptsMatchSelectedInvoice() throws Exception {
  PhienThuNganKiemThu.login("CASHIER");
  var invoices=new VienPhiGhepNoiGoc.InMemoryInvoiceRepository();
  var visits=new VienPhiGhepNoiGoc.InMemoryVisitRepository();
  // Explicit test repositories; no fixture can enter production composition.
  var fallback=VienPhiGhepNoiGoc.createDefault();
  var invoice=new HoaDon();invoice.setId(1);invoice.setSoHoaDon("TEST-PREVIEW");invoice.setMaLuotKham(1);invoice.setMaBenhNhan(2);invoice.setMaCoSo(1);
  for(String type:new String[]{"DICH_VU_KHAM","DICH_VU_CLS","DON_THUOC"}){var line=new ChiTietHoaDon();line.setLoaiDoiTuong(type);line.setMaDoiTuong(type.equals("DICH_VU_KHAM")?1:2);line.setMoTa(switch(type){case "DICH_VU_KHAM"->"Khám chuyên khoa";case "DON_THUOC"->"Vitamin C 500mg";default->"Siêu âm tổng quát";});line.setSoLuong(BigDecimal.ONE);line.setDonGia(new BigDecimal("120000"));invoice.addChiTiet(line);}
  invoice.recalculateCharges();invoices.save(invoice);invoice.recordPayment(new BigDecimal("120000"),PhuongThucThanhToan.TIEN_MAT,1,"Thu lần 1");invoices.savePhieuThu(invoice.getPhieuThus().get(0));
  var services=new VienPhiGhepNoiGoc.Services(fallback.createInvoice(),
      new vn.clinicmanagement.application.billing.get.GetHoaDonTheoLuotKhamHandler(invoices,new Audit()),
      new vn.clinicmanagement.application.billing.record.RecordThanhToanHandler(invoices,new Audit()),fallback.patientDebt())
      .withScreenRead((visit,facility)->Optional.of(new VienPhiManHinhReadCongKetNoi.ThongTin("TEST-002","BỆNH NHÂN KIỂM THỬ",LocalDate.of(1998,1,1),"M","Địa chỉ thử nghiệm","0900000000","Khoa Khám Bệnh","Phòng 1","THU_PHI","DANG_KHAM",LocalDateTime.now(),null,Map.of(1,"Thu ngân kiểm thử"))));
  VienPhiHoaDonManHinh[] holder=new VienPhiHoaDonManHinh[1];
  SwingUtilities.invokeAndWait(()->{holder[0]=new VienPhiHoaDonManHinh(services);holder[0].setVisitIds(java.util.List.of(1));});
  for(int i=0;i<100;i++){Thread.sleep(20);SwingUtilities.invokeAndWait(()->{});if(holder[0].getInFlightWorkersCount()==0)break;}
  SwingUtilities.invokeAndWait(()->{
   var panel=holder[0];panel.setSize(1586,832);layout(panel);layout(panel);
   assertEquals("BỆNH NHÂN KIỂM THỬ",panel.getInvoiceTableModel().getValueAt(0,3));
   assertFalse(panel.getInvoiceTableModel().getValueAt(0,12).toString().isBlank());
   assertEquals(10,panel.getDetailTable().getColumnCount());
   assertEquals("Viên",panel.getDetailTable().getValueAt(2,1));
   var col0Text=((JLabel)panel.getDetailTable().getCellRenderer(2,0).getTableCellRendererComponent(panel.getDetailTable(),panel.getDetailTable().getValueAt(2,0),false,false,2,0)).getText();
   assertTrue(col0Text.contains("Thuốc"),()->"Expected Thuốc group heading, got: "+col0Text);
   var split=find(panel,JSplitPane.class);assertEquals(349,split.getDividerLocation(),3);
   var receipts=find(panel,JList.class);assertEquals(1,receipts.getModel().getSize());assertEquals(157,receipts.getParent().getParent().getWidth(),5);
   receipts.setSelectedIndex(0);assertFalse(panel.getBtnCollect().isEnabled());
   panel.getActionMap().get("cancelPayment").actionPerformed(null);assertTrue(panel.getBtnCollect().isEnabled());
   var image=new BufferedImage(1586,832,BufferedImage.TYPE_INT_RGB);var g=image.createGraphics();panel.printAll(g);g.dispose();
   try{ImageIO.write(image,"png",new java.io.File("/tmp/clinic-billing-ui-preview.png"));}catch(Exception ex){throw new RuntimeException(ex);}
  });
 }
 static void layout(Container c){c.doLayout();for(var child:c.getComponents())if(child instanceof Container box)layout(box);}
 static <T> T find(Container c,Class<T> type){for(var child:c.getComponents()){if(type.isInstance(child))return type.cast(child);if(child instanceof Container box){T found=find(box,type);if(found!=null)return found;}}return null;}
 static class Audit implements vn.clinicmanagement.application.audit.KiemToanWriteCongKetNoi {
  public void appendInCurrentTransaction(vn.clinicmanagement.application.audit.HeThongKiemToanNhapLieu e){}
  public void appendIndependently(vn.clinicmanagement.application.audit.HeThongKiemToanNhapLieu e){}
  public void appendMedicalRecordInCurrentTransaction(vn.clinicmanagement.application.audit.YTeRecordKiemToanNhapLieu e){}
  public void appendMedicalRecordIndependently(vn.clinicmanagement.application.audit.YTeRecordKiemToanNhapLieu e){}
 }
}
