package vn.clinicmanagement.desktop.modules.reports.components;

import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.reports.PhieuBaoCaoReadCongKetNoi;
import vn.clinicmanagement.application.reports.PhieuBaoCaoReadCongKetNoi.*;
import vn.clinicmanagement.desktop.core.config.UngDungPhongChu;
import vn.clinicmanagement.desktop.modules.reports.DanhSachBaoCaoManHinh;
import vn.clinicmanagement.desktop.modules.reports.catalog.BaoCaoMau;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.*;
import java.util.function.Supplier;
import static vn.clinicmanagement.desktop.modules.reports.components.PhieuBaoCaoMoHinh.*;

/** Inline data preview with direct link to print slip dialogs on double-click or button click. */
public final class BaoCaoDuLieuNoiTuyenManHinh extends JPanel {
    private static final DateTimeFormatter DAY=DateTimeFormatter.ofPattern("dd/MM/uuuu"),TIME=DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
    private final PhieuBaoCaoReadCongKetNoi source;
    private final Supplier<PhanQuyenNguCanh> authorization;
    private final JLabel title=new JLabel(),status=new JLabel(),count=new JLabel("0 dòng");
    private final JTextField from=new JTextField(),to=new JTextField(),keyword=new JTextField();
    private final JButton reload=new JButton("Xem dữ liệu"),openSlip=new JButton("Xem Phiếu In"),export=new JButton("Xuất CSV"),print=new JButton("In dữ liệu"),close=new JButton("Thu gọn dữ liệu");
    private final DataModel model=new DataModel();
    private final JTable table;
    private final TableRowSorter<DataModel> sorter=new TableRowSorter<>(model);
    private BaoCaoMau report;
    private SwingWorker<List<Phieu>,Void> worker;
    private long generation;
    private boolean loaded;

    public BaoCaoDuLieuNoiTuyenManHinh(PhieuBaoCaoReadCongKetNoi source,Supplier<PhanQuyenNguCanh> authorization,Runnable collapse){
        super(new BorderLayout());this.source=Objects.requireNonNull(source);this.authorization=Objects.requireNonNull(authorization);setName("reports.inline-data");setBackground(Color.WHITE);
        LocalDate today=LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));from.setText(today.minusDays(30).format(DAY));to.setText(today.format(DAY));
        JPanel titleBar=new JPanel(new BorderLayout(8,0));titleBar.setBackground(new Color(246,248,252));titleBar.setBorder(new EmptyBorder(4,8,4,8));title.setFont(UngDungPhongChu.BODY_BOLD);title.setForeground(new Color(35,70,110));titleBar.add(title);close.setFont(UngDungPhongChu.BODY);close.addActionListener(e->collapse.run());titleBar.add(close,BorderLayout.EAST);
        JPanel controls=new JPanel(new FlowLayout(FlowLayout.LEFT,6,4));controls.setBackground(new Color(246,248,252));
        for(JTextField field:List.of(from,to)){field.setFont(UngDungPhongChu.BODY);field.setPreferredSize(new Dimension(104,24));field.setBorder(new CompoundBorder(new LineBorder(new Color(165,180,196)),new EmptyBorder(1,4,1,4)));field.addActionListener(e->reload());field.setToolTipText("Ngày theo định dạng dd/MM/yyyy.");}
        JLabel fromLabel=new JLabel("Từ ngày:"),toLabel=new JLabel("Đến ngày:");fromLabel.setFont(UngDungPhongChu.BODY);toLabel.setFont(UngDungPhongChu.BODY);controls.add(fromLabel);controls.add(from);controls.add(toLabel);controls.add(to);
        openSlip.setForeground(new Color(0,95,185));
        openSlip.setToolTipText("Mở mẫu phiếu in xem trước của dòng đang chọn (hoặc nhấp đúp dòng).");
        for(JButton button:List.of(reload,openSlip,export,print)){button.setFont(UngDungPhongChu.BODY_BOLD);button.setMargin(new Insets(2,8,2,8));controls.add(button);}
        reload.addActionListener(e->reload());openSlip.addActionListener(e->openSelectedSlip());export.addActionListener(e->export());print.addActionListener(e->print());
        keyword.setFont(UngDungPhongChu.BODY);keyword.setBackground(new Color(255,255,204));keyword.setBorder(new EmptyBorder(0,9,0,5));keyword.setPreferredSize(new Dimension(0,26));keyword.putClientProperty("JTextField.placeholderText","Tìm trong dữ liệu (Ctrl+F)");keyword.getDocument().addDocumentListener(new DocumentListener(){public void insertUpdate(DocumentEvent e){filter();}public void removeUpdate(DocumentEvent e){filter();}public void changedUpdate(DocumentEvent e){filter();}});
        JPanel top=new JPanel(new BorderLayout());top.add(titleBar,BorderLayout.NORTH);top.add(controls);top.add(keyword,BorderLayout.SOUTH);add(top,BorderLayout.NORTH);
        table=new JTable(model){@Override protected void paintComponent(Graphics graphics){super.paintComponent(graphics);graphics.setColor(new Color(221,227,234));int start=getRowCount()*getRowHeight();for(int y=start;y<getHeight();y+=getRowHeight())graphics.drawLine(0,y,getWidth(),y);int x=0;for(int c=0;c<getColumnCount();c++){x+=getColumnModel().getColumn(c).getWidth();graphics.drawLine(x,start,x,getHeight());}}};
        table.setRowSorter(sorter);table.setFont(UngDungPhongChu.BODY);table.setRowHeight(24);table.setSelectionBackground(new Color(0,143,250));table.setSelectionForeground(Color.WHITE);table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);table.setGridColor(new Color(221,227,234));table.setShowGrid(true);table.setFillsViewportHeight(true);table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);table.getTableHeader().setReorderingAllowed(false);table.getTableHeader().setPreferredSize(new Dimension(0,26));
        table.setDefaultRenderer(Object.class,new DefaultTableCellRenderer(){@Override public Component getTableCellRendererComponent(JTable t,Object value,boolean selected,boolean focus,int r,int c){super.getTableCellRendererComponent(t,value,selected,false,r,c);putClientProperty("html.disable",true);setFont(UngDungPhongChu.BODY);setBorder(new EmptyBorder(0,5,0,5));setBackground(selected?t.getSelectionBackground():Color.WHITE);setForeground(selected?Color.WHITE:Color.BLACK);setToolTipText(Objects.toString(value,""));return this;}});
        table.getTableHeader().setDefaultRenderer(new DefaultTableCellRenderer(){@Override public Component getTableCellRendererComponent(JTable t,Object value,boolean s,boolean f,int r,int c){super.getTableCellRendererComponent(t,value,false,false,r,c);setFont(UngDungPhongChu.BODY_BOLD);setHorizontalAlignment(SwingConstants.CENTER);setBackground(new Color(234,239,245));setBorder(new MatteBorder(0,0,1,1,new Color(221,227,234)));return this;}});
        table.getSelectionModel().addListSelectionListener(e->{if(!e.getValueIsAdjusting())updateOpenSlipState();});
        table.addMouseListener(new MouseAdapter(){@Override public void mouseClicked(MouseEvent e){if(SwingUtilities.isLeftMouseButton(e)&&e.getClickCount()==2&&table.getSelectedRow()>=0)openSelectedSlip();}});
        JScrollPane grid=new JScrollPane(table);grid.setColumnHeaderView(table.getTableHeader());grid.setBorder(null);add(grid);
        JPanel bottom=new JPanel(new BorderLayout(8,0));bottom.setBackground(new Color(246,248,251));bottom.setBorder(new EmptyBorder(2,8,2,8));bottom.setPreferredSize(new Dimension(0,25));status.setFont(UngDungPhongChu.SMALL);count.setFont(UngDungPhongChu.SMALL);bottom.add(status);bottom.add(count,BorderLayout.EAST);add(bottom,BorderLayout.SOUTH);
        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("control F"),"findData");getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("meta F"),"findData");getActionMap().put("findData",new AbstractAction(){public void actionPerformed(ActionEvent e){keyword.requestFocusInWindow();}});setOutputEnabled(false);
    }
    public static Optional<Loai> typeFor(BaoCaoMau report){String text=normalize(report.name()+" "+report.group());
        if(text.contains("lich hen")||text.contains("hen kham")||text.contains("hen tai kham"))return Optional.of(Loai.LICH_HEN);
        if(text.contains("don thuoc"))return Optional.of(Loai.DON_THUOC);
        if(text.contains("chi dinh")||text.contains("xet nghiem")||text.contains("labo"))return Optional.of(Loai.CHI_DINH);
        if(text.contains("hoan tien")||text.contains("hoan ung"))return Optional.of(Loai.PHIEU_HOAN_TRA);
        if(text.contains("phieu thu tien")||text.contains("tam ung")||text.contains("nhat ky thu chi"))return Optional.of(Loai.PHIEU_THU);
        if(text.contains("thanh toan vien phi")||text.contains("hoa don")||text.contains("mien giam"))return Optional.of(Loai.HOA_DON);
        if(text.contains("phieu kham")||text.contains("so kham")||text.contains("benh an kham")||text.contains("giu the")||text.contains("dang ky kham"))return Optional.of(Loai.KHAM_BENH);
        return Optional.empty();
    }
    public void openReport(BaoCaoMau report){this.report=Objects.requireNonNull(report);title.setText(report.name()+(report.reportId().isBlank()?"":" (ID: "+report.reportId()+")"));title.setToolTipText(title.getText());keyword.setText("");reload();}
    public void reload(){
        cancelLoading();loaded=false;setOutputEnabled(false);model.clear();if(report==null)return;
        Optional<Loai> resolved=typeFor(report);if(resolved.isEmpty()){message("Mẫu này chưa được kết nối nguồn dữ liệu.",false);return;}
        model.setType(resolved.get());setColumnWidths();
        TruyVan query;try{query=DanhSachBaoCaoManHinh.parseRange("00:00 "+from.getText(),"23:59 "+to.getText(),authorization.get());}catch(IllegalArgumentException ex){message(ex.getMessage(),true);return;}
        long request=++generation;reload.setEnabled(false);message("Đang tải dữ liệu...",false);Loai type=resolved.get();
        worker=new SwingWorker<>(){
            protected List<Phieu> doInBackground(){return source.load(query,type);}
            protected void done(){if(request!=generation||isCancelled())return;worker=null;reload.setEnabled(true);try{model.replace(get());loaded=true;filter();if(table.getRowCount()>0)table.setRowSelectionInterval(0,0);message(model.getRowCount()==0?"Không có dữ liệu trong khoảng ngày đã chọn.":"Đã tải "+model.getRowCount()+" dòng. Nhấp đúp dòng hoặc bấm Xem Phiếu In để mở mẫu phiếu.",false);}catch(Exception ex){model.clear();filter();setOutputEnabled(false);message("Không tải được dữ liệu. Kiểm tra kết nối và bấm Xem dữ liệu để thử lại.",true);}}
        };worker.execute();
    }
    public void cancelLoading(){generation++;if(worker!=null)worker.cancel(true);worker=null;reload.setEnabled(true);}
    private void filter(){String query=normalize(keyword.getText());sorter.setRowFilter(new RowFilter<>(){@Override public boolean include(Entry<? extends DataModel,? extends Integer> entry){for(int c=0;c<entry.getValueCount();c++)if(normalize(entry.getStringValue(c)).contains(query))return true;return query.isEmpty();}});count.setText(table.getRowCount()+" dòng");setOutputEnabled(loaded&&worker==null);}
    private void setOutputEnabled(boolean enabled){export.setEnabled(enabled&&table!=null&&table.getRowCount()>0);print.setEnabled(enabled&&table!=null&&table.getRowCount()>0);updateOpenSlipState();}
    private void updateOpenSlipState(){openSlip.setEnabled(loaded&&worker==null&&selectedTicket()!=null);}
    public Phieu selectedTicket(){int viewRow=table==null?-1:table.getSelectedRow();if(viewRow<0||viewRow>=table.getRowCount())return null;int modelRow=table.convertRowIndexToModel(viewRow);return model.ticketAt(modelRow);}
    public void openSelectedSlip(){Phieu ticket=selectedTicket();if(ticket==null)return;BaoCaoPhieuInLienKet.openForTicket(SwingUtilities.getWindowAncestor(this),ticket);}
    private void message(String text,boolean error){status.setText(text);status.setToolTipText(text);status.setForeground(error?new Color(175,30,20):new Color(35,70,110));count.setText(table.getRowCount()+" dòng");}
    private void setColumnWidths(){for(int i=0;i<table.getColumnCount();i++){String name=table.getColumnName(i);int width=name.equals("Họ tên")?180:name.equals("Mã BN")?160:name.contains("ngày")||name.contains("Ngày")||name.equals("Tiếp nhận")?145:name.equals("SĐT")?120:name.equals("Dịch vụ")||name.equals("Ghi chú")||name.equals("Lời dặn")||name.equals("Kết luận")||name.equals("Lý do")?230:130;table.getColumnModel().getColumn(i).setPreferredWidth(width);}}
    public void writeVisibleCsv(Path path)throws java.io.IOException{List<String> columns=new ArrayList<>();for(int c=0;c<table.getColumnCount();c++)columns.add(table.getColumnName(c));List<List<String>> rows=new ArrayList<>();for(int r=0;r<table.getRowCount();r++){List<String> row=new ArrayList<>();for(int c=0;c<table.getColumnCount();c++)row.add(Objects.toString(table.getValueAt(r,c),""));rows.add(row);}DanhSachBaoCaoManHinh.writeCsv(path,columns,rows);}
    private void export(){if(!loaded||table.getRowCount()==0||GraphicsEnvironment.isHeadless())return;JFileChooser chooser=new JFileChooser();chooser.setDialogTitle("Xuất dữ liệu báo cáo ra CSV");chooser.setSelectedFile(new java.io.File("bao-cao-"+safe(report.reportId()).replaceAll("[^A-Za-z0-9_-]","_")+".csv"));if(chooser.showSaveDialog(this)!=JFileChooser.APPROVE_OPTION)return;Path chosen=chooser.getSelectedFile().toPath();Path path=chosen.toString().toLowerCase(Locale.ROOT).endsWith(".csv")?chosen:Path.of(chosen+".csv");if(Files.exists(path)&&JOptionPane.showConfirmDialog(this,"Thay thế tệp CSV đã tồn tại?","Xuất CSV",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)return;try{writeVisibleCsv(path);message("Đã xuất CSV: "+path,false);}catch(Exception ex){message("Không lưu được CSV. Kiểm tra đường dẫn và thử lại.",true);}}
    private void print(){if(!loaded||table.getRowCount()==0||GraphicsEnvironment.isHeadless())return;Phieu selected=selectedTicket();if(selected!=null){openSelectedSlip();return;}try{table.print(JTable.PrintMode.FIT_WIDTH,new java.text.MessageFormat(report.name()),new java.text.MessageFormat("Trang {0}"));}catch(Exception ex){message("Không in được dữ liệu báo cáo.",true);}}
    public JTable getTable(){return table;}public JTextField getFromDate(){return from;}public JTextField getToDate(){return to;}public JTextField getKeyword(){return keyword;}public JButton getReloadButton(){return reload;}public JButton getOpenSlipButton(){return openSlip;}public JButton getExportButton(){return export;}public JButton getCloseButton(){return close;}public JLabel getStatus(){return status;}public boolean isLoading(){return worker!=null;}public BaoCaoMau getSelectedReport(){return report;}
    private static final class DataModel extends AbstractTableModel {
        private Loai type=Loai.KHAM_BENH;private List<Phieu> rows=List.of();
        void clear(){rows=List.of();fireTableDataChanged();}void replace(List<Phieu> value){rows=List.copyOf(value);fireTableDataChanged();}void setType(Loai value){type=value;rows=List.of();fireTableStructureChanged();}
        Phieu ticketAt(int row){return row>=0&&row<rows.size()?rows.get(row):null;}
        String[] columns(){return switch(type){
            case KHAM_BENH->new String[]{"Mã lượt","Mã BN","Họ tên","SĐT","Tiếp nhận","Trạng thái","Xử trí","Khóa bệnh án"};
            case LICH_HEN->new String[]{"Mã hẹn","Mã BN","Họ tên","SĐT","Ngày hẹn","Trạng thái","Lý do hẹn"};
            case HOA_DON->new String[]{"Mã hóa đơn","Mã BN","Họ tên","Ngày lập","Tổng tiền","Đã thu","Trạng thái","Ghi chú"};
            case DON_THUOC->new String[]{"Mã đơn","Mã BN","Họ tên","Ngày kê","Bác sĩ","Trạng thái","Ghi chú","Lời dặn"};
            case CHI_DINH->new String[]{"Mã chỉ định","Mã BN","Họ tên","Dịch vụ","Ngày chỉ định","Trạng thái","Kết luận"};
            case PHIEU_THU->new String[]{"Mã phiếu thu","Số hóa đơn","Mã BN","Họ tên","Ngày thu","Số tiền thu","Hình thức","Người thu","Ghi chú"};
            case PHIEU_HOAN_TRA->new String[]{"Mã phiếu hoàn","Số hóa đơn","Mã BN","Họ tên","Ngày hoàn","Số tiền hoàn","Hình thức","Người hoàn","Lý do"};};}
        @Override public int getRowCount(){return rows.size();}@Override public int getColumnCount(){return columns().length;}@Override public String getColumnName(int col){return columns()[col];}
        @Override public Object getValueAt(int row,int col){Phieu p=rows.get(row);String date=p.date()==null?"":p.date().format(TIME);String state=status(p.status());Map<String,String> d=p.details();Object[] values=switch(type){
            case KHAM_BENH->new Object[]{p.code(),p.patientCode(),p.patientName(),p.phone(),date,state,safe(d.get("Xử trí")).isBlank()?"":status(d.get("Xử trí")),d.getOrDefault("Đã khóa bệnh án","")};
            case LICH_HEN->new Object[]{p.code(),p.patientCode(),p.patientName(),p.phone(),date,state,d.getOrDefault("Lý do hẹn","")};
            case HOA_DON->new Object[]{p.code(),p.patientCode(),p.patientName(),date,money(p.amount()),money(p.paid()),state,d.getOrDefault("Ghi chú","")};
            case DON_THUOC->new Object[]{p.code(),p.patientCode(),p.patientName(),date,p.author(),state,d.getOrDefault("Ghi chú",""),d.getOrDefault("Lời dặn","")};
            case CHI_DINH->new Object[]{p.code(),p.patientCode(),p.patientName(),d.getOrDefault("Dịch vụ",""),date,state,d.getOrDefault("Kết luận","")};
            case PHIEU_THU->new Object[]{p.code(),d.getOrDefault("Số hóa đơn",""),p.patientCode(),p.patientName(),date,money(p.paid()),state,p.author(),d.getOrDefault("Ghi chú","")};
            case PHIEU_HOAN_TRA->new Object[]{p.code(),d.getOrDefault("Số hóa đơn",""),p.patientCode(),p.patientName(),date,money(p.paid()),state,p.author(),d.getOrDefault("Lý do","")};};return values[col]==null?"":values[col];}
    }
}
