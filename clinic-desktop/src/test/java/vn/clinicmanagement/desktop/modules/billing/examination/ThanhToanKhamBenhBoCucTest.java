package vn.clinicmanagement.desktop.modules.billing.examination;

import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.billing.*;
import vn.clinicmanagement.domain.common.enums.TrangThaiHoaDon;
import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.List;
import javax.imageio.ImageIO;
import static org.junit.jupiter.api.Assertions.*;
import static vn.clinicmanagement.desktop.modules.billing.examination.ThanhToanKhamBenhDuLieu.*;

class ThanhToanKhamBenhBoCucTest {
    static BigDecimal n(String s){return new BigDecimal(s);}
    static ThanhToanKhamBenhDuLieu sample(){
        var at=LocalDateTime.of(2026,11,19,15,49);
        var rows=List.of(new Dong(1,at,"23.00367.1515","Định lượng Folate (Máu)","","Thu phí","",n("86200"),"Lần",BigDecimal.ONE,n("86200"),n("86200"),"Xét nghiệm","Khoa khám bệnh","Bác sĩ kiểm thử",false),
                new Dong(2,at,"","Siêu âm 2D [YC - Siêu âm thai nhi trong 3 tháng giữa]","","Thu phí","",BigDecimal.ZERO,"Lần",BigDecimal.ONE,n("153000"),n("153000"),"Chẩn đoán hình ảnh","Khoa khám bệnh","Bác sĩ kiểm thử",false),
                new Dong(3,at,"18.0071.0028","Chụp Xquang Hertz [số hóa 1 phim]","","BHYT","",n("65400"),"Lần",BigDecimal.ONE,n("65400"),n("65400"),"Chẩn đoán hình ảnh","Khoa khám bệnh","Bác sĩ kiểm thử",true),
                new Dong(4,at,"","Kiếm chuyên khoa tim mạch","","Hao phí khác","",BigDecimal.ZERO,"Lần",BigDecimal.ONE,BigDecimal.ZERO,BigDecimal.ZERO,"Thăm khám","Khoa khám bệnh","Bác sĩ kiểm thử",true),
                new Dong(5,at.minusMinutes(12),"18.1896","Khám ngoại","Khám lần 1","BHYT","",n("38700"),"Lần",BigDecimal.ONE,n("38700"),n("38700"),"Thăm khám","Khoa khám bệnh","Bác sĩ kiểm thử",true));
        return new ThanhToanKhamBenhDuLieu("BỆNH NHÂN KIỂM THỬ","22000308","1999","27","Nữ","Thị trấn Phú Mẫn, H. Quế Võ, Bắc Ninh","DN4-01-0337378888",80,"Còn 875 ngày","214332",invoice(),rows,Map.of(),"");
    }
    static HoaDonDuLieu invoice(){return new HoaDonDuLieu(1,"214332",1,2,1,LocalDateTime.now(),n("343300"),n("104100"),n("200000"),BigDecimal.ZERO,BigDecimal.ZERO,"",n("239200"),BigDecimal.ZERO,n("39200"),TrangThaiHoaDon.CHO_THANH_TOAN,"",List.of(),List.of());}
    @Test void geometrySearchGroupsAndContextMenu() throws Exception {
        SwingUtilities.invokeAndWait(()->{
            com.formdev.flatlaf.FlatLightLaf.setup();var panel=new ThanhToanKhamBenhManHinh(sample());
            for(int[] size:new int[][]{{1586,962},{1366,736},{1440,824}}){panel.setSize(size[0],size[1]);layout(panel);layout(panel);
                assertEquals(284*size[0]/1586.0,find(panel,"payment.sidebar").getWidth(),2);assertEquals(64*size[0]/1586.0,find(panel,"payment.toolbar").getHeight(),2);
                assertEquals(5,panel.getServiceTable().getRowCount());assertEquals(9,panel.getServiceTable().getColumnCount());assertTrue(panel.getServiceTable().getTableHeader().getHeight()>20);
                panel.getServiceTable().setRowSelectionInterval(4,4);var image=new BufferedImage(size[0],size[1],BufferedImage.TYPE_INT_RGB);var g=image.createGraphics();panel.paint(g);
                if(size[0]==1586){JPopupMenu popup=panel.getServicePopup();popup.setSize(popup.getPreferredSize());layout(popup);Graphics2D menu=(Graphics2D)g.create();menu.translate(700,244);popup.printAll(menu);menu.dispose();}
                g.dispose();try{ImageIO.write(image,"png",Path.of("/tmp/payment-preview-"+size[0]+".png").toFile());}catch(Exception ex){throw new RuntimeException(ex);}
            }
            panel.getSearchField().setText("sieu am");assertEquals(1,panel.getServiceTable().getRowCount());panel.getSearchField().setText("");
            JTree tree=panel.getCostTree();var root=(DefaultMutableTreeNode)tree.getModel().getRoot();var category=(DefaultMutableTreeNode)root.getChildAt(1);tree.setSelectionPath(new TreePath(((DefaultMutableTreeNode)category.getChildAt(1)).getPath()));assertEquals(2,panel.getServiceTable().getRowCount());
            assertFalse(panel.getServiceTable().isCellEditable(0,0));assertTrue(panel.getServicePopup().getComponentCount()>19);
            panel.bindData(null);assertEquals(0,panel.getServiceTable().getRowCount());
        });
    }
    @Test void mappingDoesNotAllocateAggregateInsuranceToIndividualLines(){
        var source=invoice();var invoice=new HoaDonDuLieu(source.id(),source.invoiceNumber(),source.visitId(),source.patientId(),source.facilityId(),source.invoiceDate(),source.totalAmount(),source.insurancePaidAmount(),source.advanceAmount(),source.discountAmount(),source.discountPercentage(),source.discountReason(),source.patientPaidAmount(),source.amountPaid(),source.remainingAmount(),source.status(),source.notes(),List.of(new HoaDonDongDuLieu(3,"DICH_VU_CLS",10,"Xét nghiệm",BigDecimal.ONE,n("10000"),n("10000"))),List.of());
        var mapped=mapRows(invoice,Map.of(),"BHYT");assertEquals("Chưa phân bổ",mapped.get(0).subject());assertNull(mapped.get(0).insurancePrice());assertEquals("",mapped.get(0).unit());
        assertEquals("Thu phí",mapRows(invoice,Map.of(),"THU_PHI").get(0).subject());assertEquals(n("10000"),mapped.get(0).amount());
    }
    @Test void missingDoctorAppearsInTreeUnderFallbackNode() throws Exception {
        SwingUtilities.invokeAndWait(()->{
            var at=LocalDateTime.of(2026,11,19,21,52);
            var rows=List.of(
                    new Dong(1,at,"DV_KHAM_NOI","Khám Nội tổng quát","Khám bệnh","Thu phí","Bệnh nhân tự trả",n("39000"),"Lần",BigDecimal.ONE,n("100000"),n("100000"),"Thăm khám","Khoa Khám Bệnh","",true),
                    new Dong(2,at,"XN_CTM","Xét nghiệm máu","Xét nghiệm","Thu phí","Bệnh nhân tự trả",n("42000"),"Lần",BigDecimal.ONE,n("85000"),n("85000"),"Xét nghiệm","Khoa Khám Bệnh","Đặng J Tường",true));
            var inv=new HoaDonDuLieu(1,"VP-2",1,2,1,at,n("185000"),BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO,"",n("185000"),n("185000"),BigDecimal.ZERO,TrangThaiHoaDon.DA_THANH_TOAN,"",List.of(),List.of());
            var data=new ThanhToanKhamBenhDuLieu("BN TEST","BN-1","2000","26","Nữ","","",0,"","VP-2",inv,rows,Map.of(),"");
            var panel=new ThanhToanKhamBenhManHinh(data);
            JTree tree=panel.getCostTree();var root=(DefaultMutableTreeNode)tree.getModel().getRoot();
            var doctorNode=(DefaultMutableTreeNode)root.getChildAt(4);
            assertEquals(2,doctorNode.getChildCount());
            assertEquals("Chưa gán bác sĩ (100,000)",doctorNode.getChildAt(0).toString());
            assertEquals("Đặng J Tường (85,000)",doctorNode.getChildAt(1).toString());
            tree.setSelectionPath(new TreePath(((DefaultMutableTreeNode)doctorNode.getChildAt(0)).getPath()));
            assertEquals(1,panel.getServiceTable().getRowCount());
            assertEquals("Khám Nội tổng quát",panel.getServiceTable().getValueAt(0,3));
        });
    }
    @Test void contextMenuSwitchesLinePayerBetweenThuPhiBhytAndMienPhi() throws Exception {
        SwingUtilities.invokeAndWait(()->{
            var panel=new ThanhToanKhamBenhManHinh(sample());
            panel.getServiceTable().setRowSelectionInterval(0,0);
            assertEquals("Thu phí",panel.getServiceTable().getValueAt(0,5));
            panel.changeSelectedLinePayer("BHYT","Chuyển đối tượng BHYT");
            assertEquals("BHYT",panel.getServiceTable().getValueAt(0,5));
            panel.getServiceTable().setRowSelectionInterval(0,0);
            panel.changeSelectedLinePayer("MIEN_PHI","Miễn phí dịch vụ");
            assertEquals("Miễn phí",panel.getServiceTable().getValueAt(0,5));
            panel.getServiceTable().setRowSelectionInterval(0,0);
            panel.changeSelectedLinePayer("THU_PHI","Thu phí dịch vụ");
            assertEquals("Thu phí",panel.getServiceTable().getValueAt(0,5));
        });
    }
    static void layout(Container c){c.doLayout();for(Component child:c.getComponents())if(child instanceof Container box)layout(box);}
    static Component find(Container box,String name){for(Component child:box.getComponents()){if(name.equals(child.getName()))return child;if(child instanceof Container ct){Component found=find(ct,name);if(found!=null)return found;}}return null;}
}
