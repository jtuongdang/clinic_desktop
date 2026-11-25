package vn.clinicmanagement.desktop.modules.pharmacy.components;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.util.function.Consumer;
import vn.clinicmanagement.application.inventory.NhaThuocDanhSachReadCongKetNoi.DanhSach;
/** Danh sách nhà thuốc theo thứ tự và kích thước tham chiếu trong ảnh. */
public final class NhaThuocDanhSachMenu extends JPopupMenu {
 public NhaThuocDanhSachMenu(Consumer<DanhSach> selected){
  setBorder(new LineBorder(new Color(145,145,135)));setBackground(Color.WHITE);
  String[] names={"Danh Sách Thuốc Trong Kho","Danh Sách Phiếu Xuất Lẻ","Dự Trù Thuốc"};
  for(int i=0;i<names.length;i++){
   var type=DanhSach.values()[i];var item=new JMenuItem(names[i]);
   item.setUI(new javax.swing.plaf.basic.BasicMenuItemUI());item.setFont(new Font("Arial",Font.PLAIN,16));
   item.setBackground(Color.WHITE);item.setForeground(Color.BLACK);item.setBorder(new EmptyBorder(2,36,2,8));
   item.setPreferredSize(new Dimension(264,28));item.addActionListener(e->{if(selected!=null)selected.accept(type);});add(item);
  }
  getAccessibleContext().setAccessibleName("Danh sách nhà thuốc");
 }
}
