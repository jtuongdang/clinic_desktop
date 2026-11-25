package vn.clinicmanagement.desktop.modules.paraclinical;

import vn.clinicmanagement.application.inventory.TieuHaoDichVuCongKetNoi;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.Component;
import java.awt.BorderLayout;
import java.math.BigDecimal;
import java.util.*;

/** Explicit actual-quantity confirmation before a CLS result can post cabinet consumption. */
final class TieuHaoDichVuNhapLieu {
    private TieuHaoDichVuNhapLieu(){}
    static List<TieuHaoDichVuCongKetNoi.SoLuongThucTe> ask(Component parent,List<TieuHaoDichVuCongKetNoi.CauHinh> definitions){
        if(definitions==null||definitions.isEmpty())return List.of();
        DefaultTableModel model=new DefaultTableModel(new String[]{"Mã","Vật tư","ĐVT","Định mức (ước tính)","Thực dùng"},0){
            @Override public boolean isCellEditable(int row,int column){return column==4;}
        };
        definitions.forEach(d->model.addRow(new Object[]{d.code(),d.name(),d.unit(),d.standardQuantity(),d.suggestedQuantity().toPlainString()}));
        JTable table=new JTable(model);table.setRowHeight(24);table.getColumnModel().getColumn(0).setPreferredWidth(85);table.getColumnModel().getColumn(1).setPreferredWidth(250);
        table.getColumnModel().getColumn(2).setPreferredWidth(70);table.getColumnModel().getColumn(3).setPreferredWidth(145);table.getColumnModel().getColumn(4).setPreferredWidth(120);
        JPanel panel=new JPanel(new BorderLayout(0,8));panel.add(new JLabel("Lượng thực dùng được gợi ý từ định mức (làm tròn lên nếu đã cấu hình). Kiểm tra và chỉnh sửa; nhập 0 nếu không dùng."),BorderLayout.NORTH);panel.add(new JScrollPane(table));
        while(true){
            int result=JOptionPane.showConfirmDialog(parent,panel,"Xác nhận vật tư tiêu hao",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);
            if(result!=JOptionPane.OK_OPTION)return null;
            if(table.isEditing()&&!table.getCellEditor().stopCellEditing())continue;
            List<TieuHaoDichVuCongKetNoi.SoLuongThucTe> actual=new ArrayList<>();String error=null;
            for(int row=0;row<definitions.size();row++)try{
                String input=Objects.toString(model.getValueAt(row,4),"").trim().replace(',','.');if(input.isEmpty())throw new NumberFormatException();
                BigDecimal quantity=new BigDecimal(input);if(quantity.signum()<0||quantity.scale()>4||quantity.precision()>18)throw new NumberFormatException();
                var definition=definitions.get(row);actual.add(new TieuHaoDichVuCongKetNoi.SoLuongThucTe(definition.itemId(),definition.cabinetId(),quantity));
            }catch(NumberFormatException ex){error="Nhập số lượng hợp lệ (0 trở lên, tối đa 4 số lẻ) cho tất cả vật tư.";break;}
            if(error==null)return List.copyOf(actual);
            JOptionPane.showMessageDialog(parent,error,"Kiểm tra số lượng",JOptionPane.WARNING_MESSAGE);
        }
    }
}
