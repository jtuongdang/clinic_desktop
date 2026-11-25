package vn.clinicmanagement.desktop.modules.reports;

import vn.clinicmanagement.application.common.KetQua;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.reports.*;
import vn.clinicmanagement.application.reports.get.*;
import vn.clinicmanagement.desktop.core.base.CoSoManHinhManHinh;
import vn.clinicmanagement.desktop.core.session.PhienVongDoiDichVu;
import vn.clinicmanagement.desktop.modules.examination.KhamBenhPhienNguCanh;
import vn.clinicmanagement.desktop.modules.reports.components.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.time.*;
import java.time.format.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/** Full-tab statistics viewer, consistent with the pharmacy and report ticket registers. */
public class PhongKhamBaoCaoManHinh extends CoSoManHinhManHinh {
    public static final String[] COLUMNS={"STT","Chỉ Tiêu / Hạng Mục","Đơn Vị Tính","Kỳ Trước","Kỳ Này","Tăng/Giảm (%)","Ghi Chú"};
    private static final DateTimeFormatter DATE_FORMATTER=DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);
    private final BaoCaoGhepNoiGoc.Services services;
    private final Supplier<PhanQuyenNguCanh> authorization;
    private final boolean sourceConnected;
    private final BaoCaoThongKeBoCuc ui;
    private final DefaultTableModel tableModel=new DefaultTableModel(COLUMNS,0){@Override public boolean isCellEditable(int row,int col){return false;}};
    private final TableRowSorter<DefaultTableModel> sorter=new TableRowSorter<>(tableModel);
    private SwingWorker<List<Object[]>,Void> worker;
    private long generation;
    private Runnable registerOpener;
    private boolean updating;
    public PhongKhamBaoCaoManHinh(){this(BaoCaoGhepNoiGoc.createDefault(),KhamBenhPhienNguCanh::current,false);}
    public PhongKhamBaoCaoManHinh(BaoCaoGhepNoiGoc.Services services){this(services,KhamBenhPhienNguCanh::current,true);}
    public PhongKhamBaoCaoManHinh(BaoCaoGhepNoiGoc.Services services,Supplier<PhanQuyenNguCanh> authorization){this(services,authorization,true);}
    private PhongKhamBaoCaoManHinh(BaoCaoGhepNoiGoc.Services services,Supplier<PhanQuyenNguCanh> authorization,boolean connected){
        super("report-mgmt","Báo Cáo Thống Kê","📊");this.services=Objects.requireNonNull(services);this.authorization=Objects.requireNonNull(authorization);this.sourceConnected=connected;ui=new BaoCaoThongKeBoCuc(this,tableModel);ui.table.setRowSorter(sorter);bindActions();if(!connected)showMessage("Chưa kết nối nguồn thống kê tổng hợp. Chọn Danh Sách để xem phiếu.",false);
    }
    public PhongKhamBaoCaoManHinh(PhienVongDoiDichVu session){this(BaoCaoGhepNoiGoc.create(session),()->session.currentAuthorizationContext().orElse(null),false);}
    @Override public void doLayout(){if(ui!=null)ui.resize(this);super.doLayout();}
    private void bindActions(){
        ui.type.addActionListener(e->{ui.select(ui.type.getSelectedIndex());executeReportQuery();});ui.setOnSelected(index->{if(ui.type.getSelectedIndex()==index)executeReportQuery();else ui.type.setSelectedIndex(index);});
        ui.view.addActionListener(e->executeReportQuery());ui.refresh.addActionListener(e->executeReportQuery());ui.export.addActionListener(e->exportReport());ui.print.addActionListener(e->printReport());ui.list.setEnabled(false);ui.list.addActionListener(e->{if(registerOpener!=null)registerOpener.run();});
        for(JTextField field:List.of(ui.from,ui.to))field.addActionListener(e->executeReportQuery());
        ui.period.addActionListener(e->{if(updating||ui.period.getSelectedIndex()==3)return;LocalDate today=LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));LocalDate from=switch(ui.period.getSelectedIndex()){case 0->today.withDayOfMonth(1);case 1->today.minusDays(6);default->today.minusDays(29);};updating=true;ui.from.setText(from.format(DATE_FORMATTER));ui.to.setText(today.format(DATE_FORMATTER));updating=false;executeReportQuery();});
        DocumentListener dates=new DocumentListener(){public void insertUpdate(DocumentEvent e){changed();}public void removeUpdate(DocumentEvent e){changed();}public void changedUpdate(DocumentEvent e){changed();}private void changed(){if(!updating)ui.period.setSelectedIndex(3);}};
        ui.from.getDocument().addDocumentListener(dates);ui.to.getDocument().addDocumentListener(dates);
        ui.keyword.getDocument().addDocumentListener(new DocumentListener(){public void insertUpdate(DocumentEvent e){filter();}public void removeUpdate(DocumentEvent e){filter();}public void changedUpdate(DocumentEvent e){filter();}});
        JPopupMenu menu=new JPopupMenu();JMenuItem tickets=new JMenuItem("Quản lý phiếu báo cáo");tickets.addActionListener(e->{if(registerOpener!=null)registerOpener.run();});menu.add(tickets);JMenuItem reload=new JMenuItem("Làm mới số liệu");reload.addActionListener(e->executeReportQuery());menu.add(reload);ui.menu.addActionListener(e->{tickets.setEnabled(registerOpener!=null);menu.show(ui.menu,0,ui.menu.getHeight());});
        JPopupMenu reports=new JPopupMenu();for(int i=0;i<BaoCaoThongKeBoCuc.REPORTS.length;i++){int index=i;JMenuItem item=new JMenuItem(BaoCaoThongKeBoCuc.REPORTS[i]);item.setEnabled(i<3);if(i>=3)item.setToolTipText("Chưa kết nối nguồn dữ liệu cho loại báo cáo này.");item.addActionListener(e->ui.type.setSelectedIndex(index));reports.add(item);}ui.report.addActionListener(e->reports.show(ui.report,0,ui.report.getHeight()));
        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("control F"),"searchReport");getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("meta F"),"searchReport");getActionMap().put("searchReport",new AbstractAction(){public void actionPerformed(ActionEvent e){ui.keyword.requestFocusInWindow();}});getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("F5"),"reloadReport");getActionMap().put("reloadReport",new AbstractAction(){public void actionPerformed(ActionEvent e){executeReportQuery();}});setOutputEnabled(false);
    }
    private void filter(){String text=PhieuBaoCaoMoHinh.normalize(ui.keyword.getText());sorter.setRowFilter(new RowFilter<>(){@Override public boolean include(Entry<? extends DefaultTableModel,? extends Integer> entry){for(int col=0;col<entry.getValueCount();col++)if(PhieuBaoCaoMoHinh.normalize(entry.getStringValue(col)).contains(text))return true;return text.isEmpty();}});ui.updateSummary();setOutputEnabled(worker==null&&tableModel.getRowCount()>0);}
    public void setRegisterOpener(Runnable action){registerOpener=action;ui.list.setEnabled(action!=null);}
    public void executeReportQuery(){
        cancelInFlightWorkers();tableModel.setRowCount(0);setOutputEnabled(false);ui.updateSummary();int index=ui.type.getSelectedIndex();
        if(index>=3){showMessage("Chưa kết nối nguồn dữ liệu cho báo cáo này. Chọn nhóm khác bên trái.",false);return;}
        if(!sourceConnected){showMessage("Chưa kết nối nguồn thống kê tổng hợp. Chọn Danh Sách để xem phiếu.",false);return;}
        LocalDate from,to;try{from=LocalDate.parse(ui.from.getText().strip(),DATE_FORMATTER);to=LocalDate.parse(ui.to.getText().strip(),DATE_FORMATTER);if(to.isBefore(from))throw new IllegalArgumentException();}catch(Exception ex){showMessage("Nhập ngày dd/MM/yyyy; đến ngày không được trước từ ngày.",true);return;}
        PhanQuyenNguCanh auth=authorization.get();if(auth==null||!auth.isValid()){showMessage("Phiên đăng nhập không hợp lệ. Vui lòng đăng nhập lại.",true);return;}
        long request=++generation;ui.view.setEnabled(false);ui.refresh.setEnabled(false);showMessage("Đang tải báo cáo...",false);
        worker=new SwingWorker<>(){
            protected List<Object[]> doInBackground(){List<Object[]> rows=new ArrayList<>();switch(index){
                case 0->{var result=services.getVisitStatisticsHandler().handle(new GetLuotKhamThongKeQuery(auth,from,to));var dto=require(result);int n=1;if(dto!=null)for(var item:dto.getItems())rows.add(item.toTableRow(n++));}
                case 1->{var result=services.getRevenueReportHandler().handle(new GetDoanhThuBaoCaoQuery(auth,from,to));var dto=require(result);int n=1;if(dto!=null)for(var item:dto.getItems())rows.add(item.toTableRow(n++));}
                case 2->{var query=new GetKhoBaoCaoQuery(auth.roleCode(),from,to,null,GetKhoBaoCaoQuery.DEFAULT_EXPIRY_ALERT_DAYS,auth,null);var dto=require(services.getInventoryReportHandler().handle(query));int n=1;if(dto!=null)for(var item:dto.getItems())rows.add(item.toTableRow(n++));}
                default->throw new IllegalArgumentException("Loại báo cáo không hợp lệ.");}return rows;}
            protected void done(){if(isCancelled()||request!=generation)return;worker=null;ui.view.setEnabled(true);ui.refresh.setEnabled(true);try{List<Object[]> rows=get();for(Object[] row:rows)tableModel.addRow(row);filter();showMessage(rows.isEmpty()?"Không có số liệu trong kỳ đã chọn.":"Đã tải "+rows.size()+" chỉ tiêu. Có thể lọc, in hoặc xuất dữ liệu.",false);}catch(Exception ex){tableModel.setRowCount(0);ui.updateSummary();Throwable cause=ex instanceof java.util.concurrent.ExecutionException?ex.getCause():ex;showMessage(cause instanceof IllegalStateException?cause.getMessage():"Không tải được báo cáo. Bấm Làm Mới để thử lại.",true);setOutputEnabled(false);}}
        };worker.execute();
    }
    private static <T>T require(KetQua<T> result){if(result==null)throw new IllegalStateException("Không nhận được dữ liệu báo cáo.");if(result.isFailure())throw new IllegalStateException(result.getError().map(e->e.message()).orElse("Không tải được báo cáo."));return result.getValue().orElse(null);}
    private void showMessage(String text,boolean error){ui.status.setText(text);ui.status.setForeground(error?new Color(175,30,20):new Color(35,70,110));}
    private void setOutputEnabled(boolean value){ui.export.setEnabled(value&&ui.table.getRowCount()>0);ui.print.setEnabled(value&&ui.table.getRowCount()>0);}
    public void exportReport(){
        if(!isShowing()||GraphicsEnvironment.isHeadless()||ui.table.getRowCount()==0)return;JFileChooser chooser=new JFileChooser();chooser.setDialogTitle("Xuất báo cáo ra CSV");chooser.setSelectedFile(new java.io.File("BaoCao_"+LocalDate.now()+".csv"));if(chooser.showSaveDialog(this)!=JFileChooser.APPROVE_OPTION)return;java.io.File chosen=chooser.getSelectedFile();java.io.File destination=chosen.getName().toLowerCase(java.util.Locale.ROOT).endsWith(".csv")?chosen:new java.io.File(chosen+".csv");if(destination.exists()&&JOptionPane.showConfirmDialog(this,"Thay thế tệp CSV đã tồn tại?","Xuất dữ liệu",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)return;
        try{List<String> columns=List.of(COLUMNS);List<List<String>> rows=new ArrayList<>();for(int r=0;r<ui.table.getRowCount();r++){List<String> row=new ArrayList<>();for(int c=0;c<ui.table.getColumnCount();c++)row.add(Objects.toString(ui.table.getValueAt(r,c),""));rows.add(row);}DanhSachBaoCaoManHinh.writeCsv(destination.toPath(),columns,rows);showMessage("Đã xuất "+rows.size()+" chỉ tiêu: "+destination,false);}catch(Exception ex){showMessage("Không lưu được CSV. Kiểm tra đường dẫn và thử lại.",true);}
    }
    public void exportReportToCsv(java.io.File destination) throws java.io.IOException {
        if (destination == null) {
            throw new IllegalArgumentException("Destination file cannot be null");
        }
        try (java.io.BufferedWriter writer = new java.io.BufferedWriter(
                new java.io.OutputStreamWriter(new java.io.FileOutputStream(destination), java.nio.charset.StandardCharsets.UTF_8))) {
            // Ghi BOM UTF-8 để tương thích với Microsoft Excel.
            writer.write('\ufeff');

            // Đầu trang. columns
            for (int i = 0; i < tableModel.getColumnCount(); i++) {
                if (i > 0) writer.write(",");
                writer.write(escapeCsv(tableModel.getColumnName(i)));
            }
            writer.newLine();

            // Các dòng dữ liệu.
            for (int row = 0; row < tableModel.getRowCount(); row++) {
                for (int col = 0; col < tableModel.getColumnCount(); col++) {
                    if (col > 0) writer.write(",");
                    Object val = tableModel.getValueAt(row, col);
                    writer.write(escapeCsv(val != null ? val.toString() : ""));
                }
                writer.newLine();
            }
        }
    }

    private String escapeCsv(String val) {
        if (val == null) return "";
        if (val.contains(",") || val.contains("\"") || val.contains("\n") || val.contains("\r")) {
            return "\"" + val.replace("\"", "\"\"") + "\"";
        }
        return val;
    }

    /** Reuses the report controls and table inside the catalog; no second sidebar or app tab. */
    public JPanel createInlinePreview(Runnable close){
        JPanel inline=new JPanel(new BorderLayout());inline.setBackground(Color.WHITE);
        JPanel top=new JPanel(new BorderLayout());top.setBackground(new Color(246,248,252));
        JPanel fields=new JPanel(new FlowLayout(FlowLayout.LEFT,6,4));fields.setOpaque(false);
        for(JComponent c:new JComponent[]{ui.type,ui.from,ui.to,ui.view,ui.export,ui.print}){
            if(c.getParent()!=null)c.getParent().remove(c);
            if(c instanceof JButton b){b.setHorizontalTextPosition(SwingConstants.RIGHT);b.setVerticalTextPosition(SwingConstants.CENTER);b.setIcon(null);b.setPreferredSize(new Dimension(b==ui.view?116:104,26));}
        }
        ui.type.setPreferredSize(new Dimension(185,26));ui.from.setPreferredSize(new Dimension(96,26));ui.to.setPreferredSize(new Dimension(96,26));
        fields.add(ui.type);fields.add(new JLabel("Từ:"));fields.add(ui.from);fields.add(new JLabel("Đến:"));fields.add(ui.to);fields.add(ui.view);fields.add(ui.export);fields.add(ui.print);
        JButton collapse=new JButton("Thu gọn dữ liệu");collapse.setFont(vn.clinicmanagement.desktop.core.config.UngDungPhongChu.BODY);collapse.addActionListener(e->close.run());
        JPanel title=new JPanel(new BorderLayout());title.setOpaque(false);JLabel label=new JLabel("Báo cáo thống kê");label.setFont(vn.clinicmanagement.desktop.core.config.UngDungPhongChu.BODY_BOLD);title.add(label);title.add(collapse,BorderLayout.EAST);top.add(title,BorderLayout.NORTH);top.add(fields);inline.add(top,BorderLayout.NORTH);inline.add(ui.content);
        JPanel footer=new JPanel(new BorderLayout());footer.add(ui.status);footer.add(ui.count,BorderLayout.EAST);footer.setPreferredSize(new Dimension(0,24));inline.add(footer,BorderLayout.SOUTH);return inline;
    }
    public void printReport(){if(!isShowing()||GraphicsEnvironment.isHeadless()||ui.table.getRowCount()==0)return;try{ui.table.print(JTable.PrintMode.FIT_WIDTH,new java.text.MessageFormat(Objects.toString(ui.type.getSelectedItem(),"Báo cáo thống kê")),new java.text.MessageFormat("Trang {0}"));}catch(Exception ex){showMessage("Không in được báo cáo.",true);}}
    private void cancelInFlightWorkers(){generation++;if(worker!=null){worker.cancel(true);worker=null;}if(ui!=null){ui.view.setEnabled(true);ui.refresh.setEnabled(true);}}
    public JComboBox<String> getCboReportType(){return ui.type;}public JTextField getTxtFromDate(){return ui.from;}public JTextField getTxtToDate(){return ui.to;}public JButton getBtnView(){return ui.view;}public JButton getBtnExport(){return ui.export;}public JButton getBtnPrint(){return ui.print;}public JTable getReportTable(){return ui.table;}public DefaultTableModel getTableModel(){return tableModel;}public BaoCaoGhepNoiGoc.Services getServices(){return services;}public BaoCaoThongKeBoCuc getReportUi(){return ui;}public boolean isLoading(){return worker!=null;}
    @Override public void onShow(){refreshData();}@Override public void onHide(){cancelInFlightWorkers();}public void onClose(){cancelInFlightWorkers();}@Override public void refreshData(){executeReportQuery();}
}
