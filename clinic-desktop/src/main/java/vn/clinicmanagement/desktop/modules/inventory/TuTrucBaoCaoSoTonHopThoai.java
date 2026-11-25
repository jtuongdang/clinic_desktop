package vn.clinicmanagement.desktop.modules.inventory;

import vn.clinicmanagement.application.inventory.TuTrucCongKetNoi;
import vn.clinicmanagement.application.inventory.TuTrucCongKetNoi.*;
import vn.clinicmanagement.application.inventory.TieuHaoDichVuCongKetNoi;
import vn.clinicmanagement.desktop.core.config.UngDungPhongChu;
import vn.clinicmanagement.desktop.modules.examination.KhamBenhPhienNguCanh;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.List;

/** Period balance and movement ledger from the same persisted InventoryTransaction snapshot. */
final class TuTrucBaoCaoSoTonHopThoai extends JDialog {
    private static final ZoneId BUSINESS_ZONE=ZoneId.of("Asia/Ho_Chi_Minh");
    private final TuTrucCongKetNoi data;
    private final String session;
    private final JComboBox<Kho> warehouse=new JComboBox<>();
    private final JSpinner from=new JSpinner(new SpinnerDateModel()),until=new JSpinner(new SpinnerDateModel());
    private final JLabel status=new JLabel(" ");
    private final DefaultTableModel balanceModel=model(new String[]{"Mã","Vật tư","ĐVT","Lô","Hạn dùng","Đầu kỳ","Nhập","Xuất","Cuối kỳ"});
    private final DefaultTableModel transactionModel=model(new String[]{"Thời điểm","Mã","Vật tư","ĐVT","Lô","Loại giao dịch","Chứng từ","Thay đổi","Trước","Sau"});
    private final DefaultTableModel consumptionModel=model(new String[]{"Thời điểm","Mã chỉ định","Mã DV","Dịch vụ","Mã vật tư","Vật tư","ĐVT","Tủ","Thực dùng","Đã ghi sổ","Trạng thái"});
    private final JTable balances=new JTable(balanceModel),transactions=new JTable(transactionModel),consumption=new JTable(consumptionModel);
    private int generation;
    private boolean changingWarehouse;
    TuTrucBaoCaoSoTonHopThoai(Window owner,TuTrucCongKetNoi data){
        super(owner,"Sổ tồn và biến động kho",ModalityType.MODELESS);this.data=Objects.requireNonNull(data);var ctx=KhamBenhPhienNguCanh.current();session=ctx==null?"":ctx.sessionId().toString();
        LocalDate today=LocalDate.now(BUSINESS_ZONE);from.setValue(Date.from(today.atStartOfDay(BUSINESS_ZONE).toInstant()));until.setValue(Date.from(today.plusDays(1).atStartOfDay(BUSINESS_ZONE).toInstant()));
        JSpinner.DateEditor editor=new JSpinner.DateEditor(from,"dd/MM/yyyy HH:mm");from.setEditor(editor);until.setEditor(new JSpinner.DateEditor(until,"dd/MM/yyyy HH:mm"));
        JPanel filters=new JPanel(new FlowLayout(FlowLayout.LEFT,8,6));filters.add(new JLabel("Kho/tủ:"));warehouse.setPreferredSize(new Dimension(240,26));filters.add(warehouse);filters.add(new JLabel("Từ:"));filters.add(from);filters.add(new JLabel("Đến (chưa gồm):"));filters.add(until);
        JButton refresh=new JButton("Làm mới");refresh.addActionListener(e->load());filters.add(refresh);JButton exportBalance=new JButton("Xuất XNT CSV");exportBalance.addActionListener(e->export(balanceModel,"xnt-tu-truc.csv"));filters.add(exportBalance);JButton exportLedger=new JButton("Xuất sổ CSV");exportLedger.addActionListener(e->export(transactionModel,"so-ton-tu-truc.csv"));filters.add(exportLedger);
        JButton exportConsumption=new JButton("Xuất hao phí CSV");exportConsumption.addActionListener(e->export(consumptionModel,"hao-phi-tu-truc.csv"));filters.add(exportConsumption);
        status.setBorder(BorderFactory.createEmptyBorder(4,8,4,8));JTabbedPane tabs=new JTabbedPane();tabs.addTab("Xuất nhập tồn theo lô",new JScrollPane(balances));tabs.addTab("Sổ giao dịch",new JScrollPane(transactions));tabs.addTab("Hao phí dịch vụ",new JScrollPane(consumption));
        setLayout(new BorderLayout());add(filters,BorderLayout.NORTH);add(tabs,BorderLayout.CENTER);add(status,BorderLayout.SOUTH);setSize(1180,720);setLocationRelativeTo(owner);
        warehouse.addActionListener(e->{if(!changingWarehouse)load();});if(ctx==null){status.setText("Vui lòng đăng nhập lại.");return;}loadWarehouses();
    }
    private static DefaultTableModel model(String[] columns){return new DefaultTableModel(columns,0){@Override public boolean isCellEditable(int r,int c){return false;}};}
    private void loadWarehouses(){
        var ctx=KhamBenhPhienNguCanh.current();if(ctx==null)return;String token=ctx.sessionId().toString();int old=warehouse.getSelectedItem() instanceof Kho k?k.id():0;generation++;
        TuTrucVatTuManHinh.async(()->{List<Kho> all=new ArrayList<>(data.getSupplyWarehouses(ctx));all.addAll(data.getCabinets(ctx));return all.stream().distinct().toList();},items->{
            if(!TuTrucVatTuManHinh.current(token))return;changingWarehouse=true;warehouse.removeAllItems();items.forEach(warehouse::addItem);items.stream().filter(k->k.id()==old).findFirst().ifPresent(warehouse::setSelectedItem);changingWarehouse=false;if(!items.isEmpty())load();else status.setText("Không có kho/tủ được phép xem trong khoa/phòng hiện tại.");
        },error->{if(TuTrucVatTuManHinh.current(token))status.setText("Không tải được danh sách kho.");});
    }
    private void load(){
        var ctx=KhamBenhPhienNguCanh.current();var selected=(Kho)warehouse.getSelectedItem();if(ctx==null||selected==null)return;
        LocalDateTime start=((Date)from.getValue()).toInstant().atZone(BUSINESS_ZONE).toLocalDateTime();LocalDateTime end=((Date)until.getValue()).toInstant().atZone(BUSINESS_ZONE).toLocalDateTime();if(!start.isBefore(end)){status.setText("Thời gian bắt đầu phải trước thời gian kết thúc.");return;}
        String token=ctx.sessionId().toString();int request=++generation;status.setText("Đang đối chiếu ledger…");
        TuTrucVatTuManHinh.async(()->new ReportData(data.getStockMovement(ctx,selected.id(),start,end),data.getConsumptionReport(ctx,start,end)),report->{if(request!=generation||!TuTrucVatTuManHinh.current(token)||!Objects.equals(selected,warehouse.getSelectedItem()))return;
            balanceModel.setRowCount(0);transactionModel.setRowCount(0);consumptionModel.setRowCount(0);
            for(var row:report.stock().balances())balanceModel.addRow(new Object[]{row.code(),row.name(),row.unit(),row.batch(),row.expiry(),row.opening(),row.received(),row.issued(),row.ending()});
            for(var row:report.stock().transactions())transactionModel.addRow(new Object[]{row.occurredAt(),row.code(),row.name(),row.unit(),row.batch(),row.transactionType(),row.documentCode(),row.delta(),row.before(),row.after()});
            for(var row:report.consumption())consumptionModel.addRow(new Object[]{row.occurredAt(),row.serviceOrderId(),row.serviceCode(),row.serviceName(),row.itemCode(),row.itemName(),row.unit(),row.cabinetName(),row.actual(),row.posted(),row.status()});
            status.setText("Đã tải "+report.stock().balances().size()+" dòng tồn, "+report.stock().transactions().size()+" giao dịch và "+report.consumption().size()+" dòng hao phí. Khoảng thời gian [từ, đến).");
        },error->{if(request==generation&&TuTrucVatTuManHinh.current(token))status.setText("Không tải được ledger. Kiểm tra quyền và khoảng thời gian.");});
    }
    private record ReportData(BaoCaoTon stock,List<TieuHaoDichVuCongKetNoi.BaoCaoDong> consumption){}
    private void export(DefaultTableModel model,String defaultName){
        JFileChooser picker=new JFileChooser();picker.setSelectedFile(new File(defaultName));if(picker.showSaveDialog(this)!=JFileChooser.APPROVE_OPTION)return;File file=picker.getSelectedFile();
        try(var writer=new OutputStreamWriter(new FileOutputStream(file),StandardCharsets.UTF_8)){writer.write('\ufeff');for(int r=-1;r<model.getRowCount();r++){for(int c=0;c<model.getColumnCount();c++){if(c>0)writer.write(',');String cell=Objects.toString(r<0?model.getColumnName(c):model.getValueAt(r,c),"");if(cell.matches("^[=+@\\-].*"))cell="'"+cell;writer.write('"'+cell.replace("\"","\"\"")+'"');}writer.write("\r\n");}}
        catch(IOException ex){JOptionPane.showMessageDialog(this,"Không ghi được CSV.","Xuất báo cáo",JOptionPane.ERROR_MESSAGE);}
    }
}
