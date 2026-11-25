package vn.clinicmanagement.desktop.modules.inventory;

import vn.clinicmanagement.application.inventory.TuTrucCongKetNoi;
import vn.clinicmanagement.application.inventory.TuTrucCongKetNoi.*;
import vn.clinicmanagement.desktop.core.config.UngDungPhongChu;
import vn.clinicmanagement.desktop.modules.examination.KhamBenhPhienNguCanh;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import java.util.*;
import java.util.List;

/** Lập yêu cầu nhập bù trong tab, giữ nguyên danh sách phiếu phía trên. */
final class TuTrucNhapBuManHinh extends JPanel {
    private final TuTrucCongKetNoi data;
    private final Kho cabinet;
    private final Runnable saved;
    final JComboBox<Kho> source=new JComboBox<>();
    final JTextField note=new JTextField();
    final JLabel message=new JLabel("Đang tải kho cấp…");
    final JButton save=new JButton("Lưu yêu cầu");
    final JButton suggest=new JButton("Gợi ý cơ số");
    final JButton configurePar=new JButton("Cấu hình min / target / max");
    final DefaultTableModel model=new DefaultTableModel(new String[]{"Mã vật tư","Tên vật tư","Đơn vị","Số lô","Hạn dùng","Tồn kho cấp","Nhu cầu hao phí còn bù","Số lượng yêu cầu"},0){
        @Override public boolean isCellEditable(int row,int column){return column==7;}
    };
    final JTable table=TuTrucVatTuManHinh.grid(model);
    private List<Lo> batches=List.of();
    private Map<Integer,DinhMucTuTruc> parByItem=Map.of();
    private Map<Integer,BigDecimal> cabinetStockByItem=Map.of();
    private Map<Integer,List<NhuCauBu>> demandByItem=Map.of();
    private int generation;
    private boolean disposed, saving;
    private final UUID operationKey=UUID.randomUUID();
    private final String session;

    TuTrucNhapBuManHinh(TuTrucCongKetNoi data,Kho cabinet,Runnable saved){
        this(data,cabinet,null,saved);
    }

    TuTrucNhapBuManHinh(TuTrucCongKetNoi data,Kho cabinet,Integer initialSourceWarehouseId,Runnable saved){
        super(new BorderLayout(0,3));this.data=data;this.cabinet=cabinet;this.saved=saved;
        var context=KhamBenhPhienNguCanh.current();session=context==null?"":context.sessionId().toString();
        JPanel fields=new JPanel(new BorderLayout(8,0));fields.setBorder(BorderFactory.createEmptyBorder(4,6,4,6));
        JPanel warehouse=new JPanel(new BorderLayout(6,0));
        var lblKho=new JLabel("Kho cấp:");lblKho.setFont(UngDungPhongChu.BODY_BOLD);warehouse.add(lblKho,BorderLayout.WEST);
        source.setName("tuTrucKhoCap");source.setFont(UngDungPhongChu.BODY);source.setPreferredSize(new Dimension(220,26));warehouse.add(source);
        fields.add(warehouse,BorderLayout.WEST);
        JPanel description=new JPanel(new BorderLayout(6,0));
        var lblGhiChu=new JLabel("Ghi chú:");lblGhiChu.setFont(UngDungPhongChu.BODY_BOLD);description.add(lblGhiChu,BorderLayout.WEST);
        note.setFont(UngDungPhongChu.BODY);note.setPreferredSize(new Dimension(note.getPreferredSize().width,26));description.add(note);fields.add(description);
        add(fields,BorderLayout.NORTH);

        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        int[] widths={95,240,65,95,95,95,145,115};
        for(int i=0;i<widths.length&&i<table.getColumnCount();i++){
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
            table.getColumnModel().getColumn(i).setWidth(widths[i]);
        }
        var center=new javax.swing.table.DefaultTableCellRenderer();center.setHorizontalAlignment(SwingConstants.CENTER);
        var right=new javax.swing.table.DefaultTableCellRenderer();right.setHorizontalAlignment(SwingConstants.RIGHT);
        table.getColumnModel().getColumn(0).setCellRenderer(center);
        table.getColumnModel().getColumn(2).setCellRenderer(center);
        table.getColumnModel().getColumn(3).setCellRenderer(center);
        table.getColumnModel().getColumn(4).setCellRenderer(center);
        table.getColumnModel().getColumn(5).setCellRenderer(right);
        table.getColumnModel().getColumn(6).setCellRenderer(right);
        table.getColumnModel().getColumn(7).setCellRenderer(right);

        add(TuTrucVatTuManHinh.scrollFor(table));
        JPanel footer=new JPanel(new BorderLayout(8,0));footer.setBorder(BorderFactory.createEmptyBorder(3,6,4,6));
        message.setFont(UngDungPhongChu.SMALL);footer.add(message);
        save.setFont(UngDungPhongChu.BODY_BOLD);save.setPreferredSize(new Dimension(110,26));
        JPanel actions=new JPanel(new FlowLayout(FlowLayout.RIGHT,5,0));actions.setOpaque(false);
        suggest.setFont(UngDungPhongChu.SMALL);configurePar.setFont(UngDungPhongChu.SMALL);
        actions.add(configurePar);actions.add(suggest);actions.add(save);footer.add(actions,BorderLayout.EAST);add(footer,BorderLayout.SOUTH);
        save.setEnabled(false);suggest.setEnabled(false);configurePar.setEnabled(false);
        save.addActionListener(e->submit());suggest.addActionListener(e->applySuggestions());configurePar.addActionListener(e->editPar());source.addActionListener(e->loadBatches());
        if(context==null){message.setText("Vui lòng đăng nhập lại.");return;}
        configurePar.setEnabled(true);
        TuTrucVatTuManHinh.async(()->data.getSupplyWarehouses(context),warehouses->{if(!active())return;
            warehouses.forEach(source::addItem);
            if(initialSourceWarehouseId!=null)warehouses.stream().filter(w->w.id()==initialSourceWarehouseId).findFirst().ifPresent(source::setSelectedItem);
            if(warehouses.isEmpty())message.setText("Chưa có kho cấp đang hoạt động trong cơ sở.");
        },error->{if(active())message.setText("Không tải được danh sách kho cấp. Đóng và mở lại Nhập bù để thử lại.");});
    }
    private boolean active(){return !disposed&&TuTrucVatTuManHinh.current(session);}
    void dispose(){disposed=true;++generation;}
    private void loadBatches(){
        if(!active()||saving)return;var warehouse=(Kho)source.getSelectedItem();if(warehouse==null)return;
        int token=++generation;var context=KhamBenhPhienNguCanh.current();batches=List.of();model.setRowCount(0);save.setEnabled(false);
        message.setText("Đang tải tồn kho cấp…");
        TuTrucVatTuManHinh.async(()->data.getStock(context,warehouse.id()),stock->{if(!active()||token!=generation)return;
            batches=stock.stream().filter(b->b.expiry()!=null&&!b.expiry().isBefore(LocalDate.now())).toList();
            for(var b:batches)model.addRow(new Object[]{b.code(),b.name(),b.unit(),b.batch(),b.expiry(),b.quantity(),BigDecimal.ZERO,BigDecimal.ZERO});
            save.setEnabled(!batches.isEmpty());configurePar.setEnabled(true);message.setText(batches.isEmpty()?"Kho cấp chưa có lô còn hạn, còn tồn.":"Nhập lượng cần bù; hệ thống phân bổ lô FEFO khi xuất. Có thể áp dụng gợi ý cơ số.");
            loadParData(token,context);
        },error->{if(active()&&token==generation)message.setText("Không tải được tồn kho cấp. Chọn lại kho để thử lại.");});
    }
    private void submit(){
        if(!active()||saving)return;
        if(table.isEditing()&&!table.getCellEditor().stopCellEditing())return;
        Map<Integer,BigDecimal> requestedByItem=new LinkedHashMap<>();
        Map<Integer,BigDecimal> availableByItem=new LinkedHashMap<>();
        try{for(int r=0;r<model.getRowCount();r++){
            BigDecimal quantity=new BigDecimal(String.valueOf(model.getValueAt(r,7)).trim().replace(',','.'));
            if(quantity.signum()<0||quantity.scale()>4||quantity.compareTo(batches.get(r).quantity())>0)throw new IllegalArgumentException();
            int itemId=batches.get(r).itemId();
            if(quantity.signum()>0)requestedByItem.merge(itemId,quantity,BigDecimal::add);
            availableByItem.merge(itemId,batches.get(r).quantity(),BigDecimal::add);
        }}catch(IllegalArgumentException ex){message.setText("Số lượng phải từ 0 đến tồn kho cấp, tối đa 4 số lẻ.");return;}
        if(requestedByItem.isEmpty()){message.setText("Nhập số lượng cần bù cho ít nhất một vật tư.");return;}
        if(requestedByItem.entrySet().stream().anyMatch(e->e.getValue().compareTo(availableByItem.getOrDefault(e.getKey(),BigDecimal.ZERO))>0)){
            message.setText("Tổng số lượng một vật tư không được vượt tồn khả dụng kho cấp.");return;
        }
        List<DongYeuCau> lines=requestedByItem.entrySet().stream().map(e->{
            BigDecimal remaining=e.getValue();List<ClaimNhuCau> claims=new ArrayList<>();
            for(var demand:demandByItem.getOrDefault(e.getKey(),List.of())){if(remaining.signum()<=0)break;BigDecimal claim=remaining.min(demand.quantityAvailable());if(claim.signum()>0){claims.add(new ClaimNhuCau(demand.id(),claim));remaining=remaining.subtract(claim);}}
            return new DongYeuCau(e.getKey(),e.getValue(),claims);
        }).toList();
        if(note.getText().length()>400){message.setText("Ghi chú tối đa 400 ký tự.");return;}
        var warehouse=(Kho)source.getSelectedItem();if(warehouse==null)return;
        var request=new YeuCau(cabinet.id(),warehouse.id(),note.getText().trim(),lines,KhamBenhPhienNguCanh.current(),operationKey);
        saving=true;save.setEnabled(false);source.setEnabled(false);note.setEnabled(false);table.setEnabled(false);message.setText("Đang lưu yêu cầu…");
        TuTrucVatTuManHinh.async(()->data.requestReplenishment(request),result->{if(!active())return;
            if(result.isSuccess()){saved.run();return;}
            unlock();message.setText(result.getError().map(e->e.message()).orElse("Không lưu được yêu cầu."));
        },error->{if(active()){unlock();message.setText("Chưa xác nhận được kết quả lưu. Kiểm tra lại danh sách phiếu trước khi lập tiếp.");}});
    }
    private record ParData(List<DinhMucTuTruc> par,List<Lo> stock,List<NhuCauBu> demands){}
    private void loadParData(int token,vn.clinicmanagement.application.authorization.PhanQuyenNguCanh context){
        TuTrucVatTuManHinh.async(()->new ParData(data.getPar(context,cabinet.id()),data.getStock(context,cabinet.id()),data.getAvailableDemand(context,cabinet.id())),result->{
            if(!active()||token!=generation)return;
            Map<Integer,DinhMucTuTruc> pars=new HashMap<>();result.par().forEach(p->pars.put(p.itemId(),p));parByItem=Map.copyOf(pars);
            Map<Integer,List<NhuCauBu>> demands=new HashMap<>();result.demands().forEach(d->demands.computeIfAbsent(d.itemId(),ignored->new ArrayList<>()).add(d));demandByItem=Map.copyOf(demands);
            for(int row=0;row<batches.size();row++){BigDecimal available=demandByItem.getOrDefault(batches.get(row).itemId(),List.of()).stream().map(NhuCauBu::quantityAvailable).reduce(BigDecimal.ZERO,BigDecimal::add);model.setValueAt(available,row,6);}
            Map<Integer,BigDecimal> totals=new HashMap<>();for(var lot:result.stock())if(lot.expiry()!=null&&!lot.expiry().isBefore(LocalDate.now()))totals.merge(lot.itemId(),lot.quantity(),BigDecimal::add);
            cabinetStockByItem=Map.copyOf(totals);suggest.setEnabled(!parByItem.isEmpty()&&!batches.isEmpty());
        },error->{if(active()&&token==generation){parByItem=Map.of();cabinetStockByItem=Map.of();suggest.setEnabled(false);}});
    }
    private void applySuggestions(){
        if(!active()||batches.isEmpty())return;
        Map<Integer,BigDecimal> remainingByItem=new HashMap<>();
        for(var item:parByItem.values()){
            BigDecimal current=cabinetStockByItem.getOrDefault(item.itemId(),BigDecimal.ZERO);
            BigDecimal need=item.target().subtract(current).max(BigDecimal.ZERO).min(item.maximum().subtract(current).max(BigDecimal.ZERO));
            if(need.signum()>0)remainingByItem.put(item.itemId(),need);
        }
        for(int row=0;row<batches.size();row++){
            Lo lot=batches.get(row);BigDecimal remaining=remainingByItem.getOrDefault(lot.itemId(),BigDecimal.ZERO);
            BigDecimal suggestion=remaining.min(lot.quantity());model.setValueAt(suggestion,row,7);
            remainingByItem.put(lot.itemId(),remaining.subtract(suggestion));
        }
        message.setText("Đã điền lượng gợi ý theo target và tồn tủ khả dụng; kiểm tra lại trước khi lưu.");
    }
    private void editPar(){
        if(!active())return;var context=KhamBenhPhienNguCanh.current();if(context==null)return;
        configurePar.setEnabled(false);message.setText("Đang tải danh mục vật tư…");
        TuTrucVatTuManHinh.async(()->data.getActiveItems(context),items->{
            if(!active())return;configurePar.setEnabled(true);message.setText("Cấu hình cơ số tủ trực.");showParEditor(items,context);
        },error->{if(active()){configurePar.setEnabled(true);message.setText("Không tải được danh mục vật tư.");}});
    }
    private void showParEditor(List<Lo> items,vn.clinicmanagement.application.authorization.PhanQuyenNguCanh context){
        if(items.isEmpty()){message.setText("Chưa có vật tư hoạt động trong danh mục.");return;}
        DefaultTableModel parModel=new DefaultTableModel(new String[]{"Mã vật tư","Tên vật tư","Đơn vị","Min","Target","Max"},0){
            @Override public boolean isCellEditable(int row,int column){return column>=3;}
        };
        for(var item:items){var p=parByItem.get(item.itemId());parModel.addRow(new Object[]{item.code(),item.name(),item.unit(),p==null?BigDecimal.ZERO:p.minimum(),p==null?BigDecimal.ZERO:p.target(),p==null?BigDecimal.ZERO:p.maximum()});}
        JTable parTable=new JTable(parModel);parTable.setRowHeight(23);parTable.setAutoCreateRowSorter(false);
        JPanel panel=new JPanel(new BorderLayout(0,6));panel.add(new JLabel("Cấu hình theo vật tư trong kho cấp đã chọn; min ≤ target ≤ max."),BorderLayout.NORTH);panel.add(new JScrollPane(parTable));
        int result=JOptionPane.showConfirmDialog(this,panel,"Cơ số tủ trực",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);
        if(result!=JOptionPane.OK_OPTION)return;if(parTable.isEditing()&&!parTable.getCellEditor().stopCellEditing())return;
        List<LuuDinhMuc> commands=new ArrayList<>();
        try{for(int row=0;row<items.size();row++){
            int itemId=items.get(row).itemId();DinhMucTuTruc existing=parByItem.get(itemId);
            BigDecimal min=decimal(parModel.getValueAt(row,3)),target=decimal(parModel.getValueAt(row,4)),max=decimal(parModel.getValueAt(row,5));
            commands.add(new LuuDinhMuc(context,cabinet.id(),itemId,min,target,max,existing==null?0:existing.version()));
        }}catch(NumberFormatException ex){message.setText("Cơ số phải là số hợp lệ, tối đa 4 số lẻ.");return;}
        configurePar.setEnabled(false);suggest.setEnabled(false);message.setText("Đang lưu cơ số…");
        TuTrucVatTuManHinh.async(()->{List<vn.clinicmanagement.application.common.KetQua<DinhMucTuTruc>> saved=new ArrayList<>();for(var command:commands)saved.add(data.savePar(command));return saved;},saved->{
            if(!active())return;configurePar.setEnabled(true);boolean ok=saved.stream().allMatch(v->v!=null&&v.isSuccess());
            if(ok){message.setText("Đã lưu cơ số.");loadParData(generation,context);}else{message.setText("Một số cơ số chưa lưu được. Kiểm tra quyền và tải lại trước khi sửa tiếp.");loadParData(generation,context);}
        },error->{if(active()){configurePar.setEnabled(true);message.setText("Không xác nhận được kết quả lưu cơ số. Tải lại trước khi thử lại.");}});
    }
    private static BigDecimal decimal(Object value){BigDecimal d=new BigDecimal(Objects.toString(value,"").trim().replace(',','.'));if(d.signum()<0||d.scale()>4||d.precision()>18)throw new NumberFormatException();return d;}
    private void unlock(){saving=false;save.setEnabled(true);source.setEnabled(true);note.setEnabled(true);table.setEnabled(true);}
}
