package vn.clinicmanagement.desktop.modules.pharmacy;

import vn.clinicmanagement.desktop.modules.pharmacy.components.NhaThuocPhieuBangManHinh;

import vn.clinicmanagement.desktop.modules.pharmacy.components.NhaThuocPhuThanhCongCu;

import vn.clinicmanagement.desktop.modules.pharmacy.components.NhaThuocDanhMucCayManHinh;
import vn.clinicmanagement.desktop.modules.pharmacy.components.NhapNhaCungCapManHinh;
import vn.clinicmanagement.desktop.modules.pharmacy.components.XuatLeManHinh;
import vn.clinicmanagement.desktop.core.session.NguoiDungPhien;

import vn.clinicmanagement.desktop.core.base.CoSoManHinhManHinh;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

/**
 * Phân hệ Quản lý Nhà Thuốc / Tủ Trực Vật Tư (Khớp 100% ảnh thiết kế EHC HIS).
 * Kế thừa BaseViewPanel để tích hợp vòng đời MDI Tab chuẩn hệ thống.
 * Đấu nối Clean Architecture qua PharmacyCompositionRoot.Services.
 */
public class NhaThuocDispenseManHinh extends CoSoManHinhManHinh {

    private final NhaThuocGhepNoiGoc.Services services;
    private final NhaThuocPhuThanhCongCu subToolbar;
    private final NhaThuocDanhMucCayManHinh treePanel;
    private final NhaThuocPhieuBangManHinh tablePanel;
    private final vn.clinicmanagement.application.inventory.NhaThuocDanhSachReadCongKetNoi listRead;
    private final vn.clinicmanagement.application.inventory.NhaThuocBaoCaoReadPort reportRead;
    private JSplitPane splitPane;

    public NhaThuocDispenseManHinh() {
        this(GraphicsEnvironment.isHeadless()||System.getProperty("surefire.test.class.path")!=null?NhaThuocGhepNoiGoc.createDefault():NhaThuocGhepNoiGoc.createProduction());
    }

    public NhaThuocDispenseManHinh(NhaThuocGhepNoiGoc.Services services) {
        this(services,
                GraphicsEnvironment.isHeadless()||System.getProperty("surefire.test.class.path")!=null?null:
                    new vn.clinicmanagement.infrastructure.persistence.inventory.NhaThuocDanhSachRepository(new vn.clinicmanagement.infrastructure.persistence.JdbcKetNoiNhaCungCap(new vn.clinicmanagement.infrastructure.persistence.JdbcGiaoDichQuanLy(new vn.clinicmanagement.infrastructure.persistence.JdbcUnitOfWork()))),
                GraphicsEnvironment.isHeadless()||System.getProperty("surefire.test.class.path")!=null?null:
                    new vn.clinicmanagement.infrastructure.persistence.inventory.NhaThuocBaoCaoRepository(new vn.clinicmanagement.infrastructure.persistence.JdbcKetNoiNhaCungCap(new vn.clinicmanagement.infrastructure.persistence.JdbcGiaoDichQuanLy(new vn.clinicmanagement.infrastructure.persistence.JdbcUnitOfWork()))));
    }

    public NhaThuocDispenseManHinh(NhaThuocGhepNoiGoc.Services services, vn.clinicmanagement.application.inventory.NhaThuocDanhSachReadCongKetNoi read) {
        this(services, read, null);
    }

    public NhaThuocDispenseManHinh(NhaThuocGhepNoiGoc.Services services,
                                   vn.clinicmanagement.application.inventory.NhaThuocDanhSachReadCongKetNoi read,
                                   vn.clinicmanagement.application.inventory.NhaThuocBaoCaoReadPort reportRead) {
        super("pharmacy", "Kho Nhà Thuốc", "💊");
        listRead=read;
        this.reportRead = reportRead;
        this.services = Objects.requireNonNull(services, "services");
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        // 1. Thanh công cụ phụ (Sub-Toolbar) trên đầu
        subToolbar = new NhaThuocPhuThanhCongCu();
        add(subToolbar, BorderLayout.NORTH);

        // 2. Khu vực chính: Chia 2 cột (Cây phân loại bên trái + Bảng dữ liệu phiếu bên phải)
        treePanel = new NhaThuocDanhMucCayManHinh();
        tablePanel = new NhaThuocPhieuBangManHinh(services,listRead);
        tablePanel.setCategoryTreePanel(treePanel);
        if(listRead!=null){tablePanel.setLiveRead(listRead);treePanel.setWarehouseTitle("Kho Nhà Thuốc");}

        // Đấu nối callback bộ lọc từ cây danh mục sang bảng phiếu
        treePanel.setOnCategorySelected(tablePanel::applyFilter);
        treePanel.setOnStatusSelected(tablePanel::filterStatus);

        // Đấu nối thanh công cụ phụ sang bảng dữ liệu
        subToolbar.setOnSearchListener(code->{
            try{var range=dateRange();
                Object sel=subToolbar.getCboSearchMode().getSelectedItem();
                var mode="Ngày tạo".equals(sel)?vn.clinicmanagement.application.inventory.NhaThuocDanhSachReadCongKetNoi.NgayLoc.NGAY_TAO
                        :"Ngày nhập xuất".equals(sel)?vn.clinicmanagement.application.inventory.NhaThuocDanhSachReadCongKetNoi.NgayLoc.NGAY_NHAP_XUAT
                        :vn.clinicmanagement.application.inventory.NhaThuocDanhSachReadCongKetNoi.NgayLoc.TONG_HOP;
                tablePanel.setDateRange(range[0],range[1]);tablePanel.setDateFilterMode(mode);tablePanel.filterByTicketCode(code);tablePanel.loadData();}
            catch(IllegalArgumentException ex){if(isShowing())JOptionPane.showMessageDialog(this,ex.getMessage(),"Kiểm tra ngày",JOptionPane.WARNING_MESSAGE);}
        });
        subToolbar.setOnListSelected(this::openList);
        subToolbar.setOnCreateTicketListener(type -> {
            if (type == null) return;
            switch (type) {
                case "Nhập Thuốc Từ Nhà Cung Cấp",
                     "Nhập Thuốc/Vật Tư Từ Kho Tổng",
                     "Nhập Từ Kho Khác",
                     "Nhập Trả Thuốc Từ Khách Hàng",
                     "Nhập Bù" -> openSupplierReceipt(type);
                case "Xuất Thuốc Cho Khách Hàng",
                     "Xuất Trả Thuốc Cho Nhà Cung Cấp",
                     "Xuất Trả Thuốc/Vật Tư Cho Kho Tổng",
                     "Xuất Trả Thuốc đến Kho Khác",
                     "Xuất Thanh Lý",
                     "Xuất Kiểm Nghiệm",
                     "Xuất Hủy (Mất/Hỏng/Vỡ)",
                     "Xuất Khác" -> openRetailIssue(type);
            }
        });
        subToolbar.getBtnList().addActionListener(e -> {
            tablePanel.filterByTicketCode("");
            tablePanel.loadData();
        });
        subToolbar.setOnReportSelected(this::openReport);
        subToolbar.getBtnExport().addActionListener(e -> exportTickets());

        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treePanel, tablePanel);
        splitPane.setDividerLocation(215);
        splitPane.setDividerSize(2);
        splitPane.setContinuousLayout(true);
        splitPane.setBorder(BorderFactory.createEmptyBorder());

        add(splitPane, BorderLayout.CENTER);
    }

    @Override public void doLayout(){super.doLayout();if(splitPane!=null)splitPane.setDividerLocation(Math.max(220,Math.min(270,(int)Math.round(getWidth()*.21))));}
    private java.time.LocalDateTime[] dateRange(){
        try{var format=java.time.format.DateTimeFormatter.ofPattern("HH:mm dd/MM/uuuu").withResolverStyle(java.time.format.ResolverStyle.STRICT);
            var from=java.time.LocalDateTime.parse(subToolbar.getTxtFromDate().getText().trim(),format);
            var to=java.time.LocalDateTime.parse(subToolbar.getTxtToDate().getText().trim(),format).plusMinutes(1);
            if(!from.isBefore(to))throw new IllegalArgumentException();return new java.time.LocalDateTime[]{from,to};
        }catch(java.time.DateTimeException|IllegalArgumentException ex){throw new IllegalArgumentException("Ngày phải có dạng HH:mm dd/MM/yyyy và từ ngày không được sau đến ngày.");}
    }
    public JPanel createListPanel(vn.clinicmanagement.application.inventory.NhaThuocDanhSachReadCongKetNoi.DanhSach type){
        if(type==vn.clinicmanagement.application.inventory.NhaThuocDanhSachReadCongKetNoi.DanhSach.THUOC_TRONG_KHO)
            return new vn.clinicmanagement.desktop.modules.pharmacy.components.DanhSachThuocTrongKhoManHinh(listRead,services,treePanel.getWarehouseId());
        JPanel panel=new JPanel(new BorderLayout(0,6));panel.setPreferredSize(new Dimension(1100,600));
        JLabel message=new JLabel(type==vn.clinicmanagement.application.inventory.NhaThuocDanhSachReadCongKetNoi.DanhSach.DU_TRU_THUOC?"Đề nghị bổ sung theo tồn tối thiểu; tồn khả dụng không gồm lô hết hạn. Chưa tạo đơn đặt hàng.":"Đang tải dữ liệu...");panel.add(message,BorderLayout.NORTH);
        var model=new javax.swing.table.DefaultTableModel(){public boolean isCellEditable(int row,int column){return false;}};
        JTable table=new JTable(model);table.setAutoCreateRowSorter(true);panel.add(new JScrollPane(table),BorderLayout.CENTER);
        var auth=vn.clinicmanagement.desktop.modules.examination.KhamBenhPhienNguCanh.current();
        if(listRead==null||auth==null||!java.util.Set.of("ADMIN","PHARMACIST","WAREHOUSE_MANAGER").contains(auth.roleCode())){message.setText("Cần phiên đăng nhập có quyền kho để tải dữ liệu.");return panel;}
        var range=dateRange();Integer warehouse=treePanel.getWarehouseId();
        new SwingWorker<vn.clinicmanagement.application.inventory.NhaThuocDanhSachReadCongKetNoi.Bang,Void>(){
            protected vn.clinicmanagement.application.inventory.NhaThuocDanhSachReadCongKetNoi.Bang doInBackground(){return listRead.getList(type,auth.facilityId(),warehouse,range[0],range[1]);}
            protected void done(){
                var current=vn.clinicmanagement.desktop.modules.examination.KhamBenhPhienNguCanh.current();if(current==null||!current.sessionId().equals(auth.sessionId())){message.setText("Phiên làm việc đã thay đổi.");return;}
                try{var data=get();model.setColumnIdentifiers(data.columns().toArray());for(var row:data.rows())model.addRow(row.toArray());if(type!=vn.clinicmanagement.application.inventory.NhaThuocDanhSachReadCongKetNoi.DanhSach.DU_TRU_THUOC)message.setText("Số dòng: "+data.rows().size());}
                catch(Exception ex){message.setText("Không thể tải dữ liệu từ CSDL. Vui lòng mở lại danh sách.");}
            }
        }.execute();return panel;
    }
    private void openList(vn.clinicmanagement.application.inventory.NhaThuocDanhSachReadCongKetNoi.DanhSach type){
        JPanel content;try{content=createListPanel(type);}catch(IllegalArgumentException ex){if(isShowing())JOptionPane.showMessageDialog(this,ex.getMessage());return;}
        if(GraphicsEnvironment.isHeadless())return;
        String title=switch(type){case THUOC_TRONG_KHO->"Danh Sách Thuốc Trong Kho";case PHIEU_XUAT_LE->"Danh Sách Phiếu Xuất Lẻ";case DU_TRU_THUOC->"Dự Trù Thuốc";};
        JDialog dialog=new JDialog(SwingUtilities.getWindowAncestor(this),type==vn.clinicmanagement.application.inventory.NhaThuocDanhSachReadCongKetNoi.DanhSach.THUOC_TRONG_KHO?"EHC - Danh Sách Thuốc":title,Dialog.ModalityType.MODELESS);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);dialog.setContentPane(content);dialog.pack();
        Rectangle area=getGraphicsConfiguration()==null?GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds():getGraphicsConfiguration().getBounds();
        Insets insets=getGraphicsConfiguration()==null?new Insets(0,0,0,0):Toolkit.getDefaultToolkit().getScreenInsets(getGraphicsConfiguration());
        int maxW=area.width-insets.left-insets.right,maxH=area.height-insets.top-insets.bottom;
        if(type==vn.clinicmanagement.application.inventory.NhaThuocDanhSachReadCongKetNoi.DanhSach.THUOC_TRONG_KHO){
            dialog.setBounds(area.x+insets.left,area.y+insets.top,maxW,maxH);
        }else{
            dialog.setSize(Math.min(dialog.getWidth(),maxW),Math.min(dialog.getHeight(),maxH));
            dialog.setLocationRelativeTo(this);
        }
        dialog.setVisible(true);
    }

    public vn.clinicmanagement.desktop.modules.pharmacy.components.NhaThuocBaoCaoHopThoai createReportPanel(
            vn.clinicmanagement.application.inventory.NhaThuocBaoCaoReadPort.LoaiBaoCao type) {
        var range = dateRange();
        return new vn.clinicmanagement.desktop.modules.pharmacy.components.NhaThuocBaoCaoHopThoai(
                reportRead, type, treePanel.getWarehouseId(), range[0], range[1]);
    }

    public vn.clinicmanagement.desktop.modules.pharmacy.components.TuyChonBaoCaoXuatNhapTonHopThoai createXuatNhapTonOptionsPanel(
            vn.clinicmanagement.desktop.modules.pharmacy.components.TuyChonBaoCaoXuatNhapTonHopThoai.ConfirmListener onConfirm) {
        var range = dateRange();
        return new vn.clinicmanagement.desktop.modules.pharmacy.components.TuyChonBaoCaoXuatNhapTonHopThoai(
                reportRead, range[0], range[1].minusMinutes(1), onConfirm);
    }

    private void openReport(vn.clinicmanagement.application.inventory.NhaThuocBaoCaoReadPort.LoaiBaoCao type) {
        if (type == vn.clinicmanagement.application.inventory.NhaThuocBaoCaoReadPort.LoaiBaoCao.XUAT_NHAP_TON) {
            openXuatNhapTonOptions();
            return;
        }
        JPanel content;
        try {
            content = createReportPanel(type);
        } catch (IllegalArgumentException ex) {
            if (isShowing()) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Kiểm tra ngày", JOptionPane.WARNING_MESSAGE);
            }
            return;
        }
        showReportDialog("EHC - " + type.title(), content);
    }

    private void openXuatNhapTonOptions() {
        vn.clinicmanagement.desktop.modules.pharmacy.components.TuyChonBaoCaoXuatNhapTonHopThoai optionsPanel;
        try {
            optionsPanel = createXuatNhapTonOptionsPanel(this::loadAndShowXuatNhapTonPrint);
        } catch (IllegalArgumentException ex) {
            if (isShowing()) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Kiểm tra ngày", JOptionPane.WARNING_MESSAGE);
            }
            return;
        }
        if (GraphicsEnvironment.isHeadless()) return;
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "EHC - Tùy Chọn Báo Cáo", Dialog.ModalityType.MODELESS);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        optionsPanel.setCloseAction(dialog::dispose);
        dialog.setContentPane(optionsPanel);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void loadAndShowXuatNhapTonPrint(
            java.time.LocalDateTime from,
            java.time.LocalDateTime to,
            vn.clinicmanagement.application.inventory.NhaThuocBaoCaoReadPort.XuatNhapTonOptions options) {
        var auth = vn.clinicmanagement.desktop.modules.examination.KhamBenhPhienNguCanh.current();
        if (reportRead == null || auth == null) {
            if (isShowing()) {
                JOptionPane.showMessageDialog(this, "Cần phiên đăng nhập và kết nối CSDL để in báo cáo.", "Thông báo", JOptionPane.WARNING_MESSAGE);
            }
            return;
        }
        Integer whId = treePanel.getWarehouseId();
        new SwingWorker<vn.clinicmanagement.application.inventory.NhaThuocBaoCaoReadPort.XuatNhapTonPrintData, Void>() {
            @Override
            protected vn.clinicmanagement.application.inventory.NhaThuocBaoCaoReadPort.XuatNhapTonPrintData doInBackground() {
                return reportRead.getXuatNhapTonPrintData(auth.facilityId(), whId, from, to, options);
            }

            @Override
            protected void done() {
                try {
                    var data = get();
                    if (!GraphicsEnvironment.isHeadless()) {
                        var printDialog = new vn.clinicmanagement.desktop.modules.pharmacy.components.BaoCaoXuatNhapTonInHopThoai(
                                SwingUtilities.getWindowAncestor(NhaThuocDispenseManHinh.this), data, from, to);
                        printDialog.setVisible(true);
                    }
                } catch (Exception ex) {
                    if (isShowing()) {
                        JOptionPane.showMessageDialog(NhaThuocDispenseManHinh.this,
                                "Không thể tải dữ liệu báo cáo từ CSDL: " + ex.getMessage(),
                                "Lỗi CSDL", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        }.execute();
    }

    private void showReportDialog(String title, JPanel content) {
        if (GraphicsEnvironment.isHeadless()) return;
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), title, Dialog.ModalityType.MODELESS);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setContentPane(content);
        dialog.pack();
        Rectangle area = getGraphicsConfiguration() == null
                ? GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds()
                : getGraphicsConfiguration().getBounds();
        Insets insets = getGraphicsConfiguration() == null
                ? new Insets(0, 0, 0, 0)
                : Toolkit.getDefaultToolkit().getScreenInsets(getGraphicsConfiguration());
        dialog.setSize(
                Math.min(dialog.getWidth(), area.width - insets.left - insets.right),
                Math.min(dialog.getHeight(), area.height - insets.top - insets.bottom));
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void exportTickets(){
        if(GraphicsEnvironment.isHeadless())return;
        JFileChooser chooser=new JFileChooser();chooser.setSelectedFile(new java.io.File("phieu-kho.csv"));
        if(chooser.showSaveDialog(this)!=JFileChooser.APPROVE_OPTION)return;
        try(var writer=java.nio.file.Files.newBufferedWriter(chooser.getSelectedFile().toPath(),java.nio.charset.StandardCharsets.UTF_8)){
            writer.write('\ufeff');var table=tablePanel.getTable();
            for(int row=-1;row<table.getRowCount();row++){for(int col=0;col<table.getColumnCount();col++){if(col>0)writer.write(',');String value=Objects.toString(row<0?table.getColumnModel().getColumn(col).getHeaderValue():table.getValueAt(row,col),"");if(value.startsWith("=")||value.startsWith("+")||value.startsWith("-")||value.startsWith("@"))value="'"+value;writer.write("\""+value.replace("\"","\"\"")+"\"");}writer.newLine();}
        }catch(java.io.IOException ex){JOptionPane.showMessageDialog(this,"Không thể xuất dữ liệu phiếu kho.");}
    }

    private void openRetailIssue(String ticketType) {
        if (GraphicsEnvironment.isHeadless()) return;
        Window owner = SwingUtilities.getWindowAncestor(this);
        String title = "Xuất Thuốc Cho Khách Hàng".equals(ticketType)
                ? "EHC - Xuất Thuốc Cho Khách Mua Lẻ   —   " + NguoiDungPhien.getInstance().getFullName()
                : "EHC - " + ticketType + "   —   " + NguoiDungPhien.getInstance().getFullName();
        String heading = "Xuất Thuốc Cho Khách Hàng".equals(ticketType) ? "XUẤT THUỐC CHO KHÁCH MUA LẺ" : ticketType;
        var form = new XuatLeManHinh(services.retail(), treePanel.getWarehouseId(), this::refreshData, heading);
        var dialog = new JDialog(owner, title, Dialog.ModalityType.MODELESS);
        dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        dialog.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosing(java.awt.event.WindowEvent event) {
                if (form.canClose()) dialog.dispose();
            }
        });
        dialog.setContentPane(form);
        dialog.pack();
        var config = dialog.getGraphicsConfiguration();
        var area = config.getBounds();
        var edges = Toolkit.getDefaultToolkit().getScreenInsets(config);
        dialog.setSize(Math.min(dialog.getWidth(), area.width-edges.left-edges.right),
                Math.min(dialog.getHeight(), area.height-edges.top-edges.bottom));
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }

    /** Mở cửa sổ nhập kho từ menu Tạo Phiếu. */
    private void openSupplierReceipt(String ticketType) {
        if (GraphicsEnvironment.isHeadless()) return;
        Window owner = SwingUtilities.getWindowAncestor(this);
        String dialogTitle = "Nhập Thuốc Từ Nhà Cung Cấp".equals(ticketType) ? "Nhập Từ Nhà Cung Cấp" : ticketType;
        String heading = "Nhập Thuốc Từ Nhà Cung Cấp".equals(ticketType) ? "NHẬP TỪ NHÀ CUNG CẤP" : ticketType;
        NhapNhaCungCapManHinh form = new NhapNhaCungCapManHinh(
                services.supplierReceipt(), treePanel.getWarehouseId(), this::refreshData, heading);
        form.setOnSupplierMenuListener(action -> {
            if ("Danh Sách Thuốc".equals(action)) {
                openList(vn.clinicmanagement.application.inventory.NhaThuocDanhSachReadCongKetNoi.DanhSach.THUOC_TRONG_KHO);
            } else if ("Danh Sách Nhà Cung Cấp".equals(action)) {
                form.openSupplierDirectoryDialog();
            }
        });
        JDialog dialog = new JDialog(owner, dialogTitle, Dialog.ModalityType.MODELESS);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowActivated(java.awt.event.WindowEvent e) {
                form.refreshCatalogsPreservingSelection();
            }
        });
        dialog.setContentPane(form);
        dialog.pack();
        GraphicsConfiguration config = getGraphicsConfiguration();
        Rectangle area = config == null
                ? GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds()
                : config.getBounds();
        Insets margins = config == null
                ? new Insets(0, 0, 0, 0)
                : Toolkit.getDefaultToolkit().getScreenInsets(config);
        dialog.setMinimumSize(new Dimension(960, 600));
        dialog.setBounds(
                area.x + margins.left,
                area.y + margins.top,
                area.width - margins.left - margins.right,
                area.height - margins.top - margins.bottom);
        dialog.setVisible(true);
    }

    public NhaThuocGhepNoiGoc.Services getServices() {
        return services;
    }

    public NhaThuocPhuThanhCongCu getSubToolbar() {
        return subToolbar;
    }

    public NhaThuocDanhMucCayManHinh getTreePanel() {
        return treePanel;
    }

    public NhaThuocPhieuBangManHinh getTablePanel() {
        return tablePanel;
    }

    @Override
    public void onShow() {
        refreshData();
    }

    @Override
    public void refreshData() {
        if (tablePanel != null) {
            tablePanel.loadData();
        }
    }
}
