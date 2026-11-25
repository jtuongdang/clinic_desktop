package vn.clinicmanagement.desktop.modules.billing;


import vn.clinicmanagement.desktop.modules.print.billing.PhieuThanhToanThuocInManHinh;
import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.billing.get.GetHoaDonTheoLuotKhamQuery;
import vn.clinicmanagement.application.billing.get.GetBenhNhanCongNoQuery;
import vn.clinicmanagement.application.billing.HoaDonDuLieu;
import vn.clinicmanagement.application.billing.HoaDonDongDuLieu;
import vn.clinicmanagement.application.billing.BienNhanDuLieu;
import vn.clinicmanagement.application.billing.BienNhanHoanTraDuLieu;
import vn.clinicmanagement.application.billing.BenhNhanCongNoDuLieu;
import vn.clinicmanagement.application.billing.record.RecordThanhToanCommand;
import vn.clinicmanagement.application.billing.record.RecordThanhToanBienNhan;
import vn.clinicmanagement.application.billing.refund.RecordHoanTraBienNhan;
import vn.clinicmanagement.application.billing.refund.RecordHoanTraCommand;
import vn.clinicmanagement.application.common.KetQua;
import vn.clinicmanagement.desktop.core.base.CoSoManHinhManHinh;
import vn.clinicmanagement.desktop.core.components.CoKieuBang;
import vn.clinicmanagement.desktop.core.config.UngDungPhongChu;
import vn.clinicmanagement.desktop.core.config.GiaoDienHangSo;
import vn.clinicmanagement.desktop.core.diagnostics.MayTinhTacVuNenChanDoanLoi;
import vn.clinicmanagement.desktop.core.session.PhienVongDoiDichVu;
import vn.clinicmanagement.desktop.core.session.NguoiDungPhien;
import vn.clinicmanagement.application.reception.TiepDonHangDoiCongKetNoi;
import vn.clinicmanagement.application.reception.TiepDonHangDoiMatHang;
import vn.clinicmanagement.domain.common.enums.PhuongThucThanhToan;
import vn.clinicmanagement.domain.common.enums.TrangThaiHoaDon;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.DateTimeException;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Phân hệ Quản Lý Viện Phí & Hóa Đơn (Billing & Invoicing)
 * Thiết kế chuẩn EHC HIS, tối ưu cho màn hình 13-inch và đồng bộ phong cách hệ thống.
 * Đấu nối xử lý bất đồng bộ qua SwingWorker tới các use case nghiệp vụ Billing.
 */
public class VienPhiHoaDonManHinh extends CoSoManHinhManHinh {

    private final DecimalFormat currencyFormat = new DecimalFormat("#,##0 'VNĐ'");
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final VienPhiGhepNoiGoc.Services services;
    private final PhienVongDoiDichVu sessionLifecycleService;
    private final TiepDonHangDoiCongKetNoi billingQueueRepository;
    private final List<SwingWorker<?, ?>> inFlightWorkers = new CopyOnWriteArrayList<>();
    private final AtomicLong refreshSequence = new AtomicLong();
    private final AtomicLong viewSessionEpoch = new AtomicLong();

    private final List<Integer> activeVisitIds = new ArrayList<>();
    private final List<HoaDonDuLieu> currentInvoices = new ArrayList<>();
    private List<BenhNhanCongNoDuLieu> latestPatientDebts = new ArrayList<>();

    private DefaultTableModel invoiceTableModel;
    private DefaultTableModel detailTableModel;
    private CoKieuBang invoiceTable;
    private CoKieuBang detailTable;
    private JTextField txtSearch;
    private JComboBox<String> cboFilterStatus;
    private JLabel lblTotalAmount;
    private JLabel lblPaidAmount;
    private JLabel lblUnpaidAmount;
    private JSplitPane centerSplitPane;
    private JButton btnCollect;
    private boolean paymentPending;
    private RecordThanhToanCommand uncertainPayment;
    private JButton btnPrintSlip;
    private JButton btnRefresh;

    public VienPhiHoaDonManHinh() {
        this(resolveDefaultServices(), null);
    }

    public VienPhiHoaDonManHinh(VienPhiGhepNoiGoc.Services services) {
        this(services, null);
    }

    public VienPhiHoaDonManHinh(VienPhiGhepNoiGoc.Services services, PhienVongDoiDichVu sessionLifecycleService) {
        this(services, sessionLifecycleService, null);
    }

    public VienPhiHoaDonManHinh(VienPhiGhepNoiGoc.Services services,
                                PhienVongDoiDichVu sessionLifecycleService,
                                TiepDonHangDoiCongKetNoi billingQueueRepository) {
        super("billing", "Danh Sách Viện Phí", "💳");
        this.services = services;
        this.sessionLifecycleService = sessionLifecycleService;
        this.billingQueueRepository = billingQueueRepository != null ? billingQueueRepository
                : services != null && services.screenRead() != null
                ? new vn.clinicmanagement.infrastructure.persistence.reception.TiepDonHangDoiRepository(
                    new vn.clinicmanagement.infrastructure.persistence.JdbcKetNoiNhaCungCap(
                        new vn.clinicmanagement.infrastructure.persistence.JdbcGiaoDichQuanLy(new vn.clinicmanagement.infrastructure.persistence.JdbcUnitOfWork())),
                    new vn.clinicmanagement.infrastructure.persistence.JdbcUnitOfWork()) : null;
        buildUI();
    }

    private static VienPhiGhepNoiGoc.Services resolveDefaultServices() {
        try {
            if (GraphicsEnvironment.isHeadless() || System.getProperty("surefire.test.class.path") != null)
                return VienPhiGhepNoiGoc.createDefault();
            var uow = new vn.clinicmanagement.infrastructure.persistence.JdbcUnitOfWork();
            var tx = new vn.clinicmanagement.infrastructure.persistence.JdbcGiaoDichQuanLy(uow);
            var provider = new vn.clinicmanagement.infrastructure.persistence.JdbcKetNoiNhaCungCap(tx);
            var audit = new vn.clinicmanagement.infrastructure.persistence.system.JdbcKiemToanRepository(provider,uow,
                    vn.clinicmanagement.infrastructure.persistence.CoSoDuLieuKetNoi::getConnection);
            return VienPhiGhepNoiGoc.create(new vn.clinicmanagement.infrastructure.persistence.billing.HoaDonRepository(provider,uow),
                    new vn.clinicmanagement.infrastructure.persistence.examination.LuotKhamRepository(provider,uow),audit,tx)
                    .withScreenRead(new vn.clinicmanagement.infrastructure.persistence.billing.VienPhiManHinhRepository(provider));
        } catch (Exception ex) {
            return null;
        }
    }

    // Kích thước tham chiếu: phần nội dung ảnh thiết kế rộng 1672, cao 778 điểm.
    private static final Color MAU_NEN = new Color(249, 249, 249);
    private static final Color MAU_LUOI = new Color(226, 226, 226);
    private static final Font CHU_MAU = UngDungPhongChu.BODY;
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter SHORT_DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yy");
    private static final DateTimeFormatter INPUT_DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/uuuu")
            .withResolverStyle(ResolverStyle.STRICT);
    private LocalDate filterFromDate = LocalDate.now(BUSINESS_ZONE);
    private LocalDate filterToDate = LocalDate.now(BUSINESS_ZONE);
    private final List<HoaDonDuLieu> displayedInvoices = new ArrayList<>();
    private final Map<Integer, String> patientNamesByVisitId = new ConcurrentHashMap<>();
    private JLabel lblInsuranceAmount, lblAdvanceAmount, lblDiscountAmount, lblPatientAmount;
    private JLabel lblRange;
    private JTextField txtServiceSearch;
    private final Map<Integer,vn.clinicmanagement.application.billing.VienPhiManHinhReadCongKetNoi.ThongTin> visitDetails = new ConcurrentHashMap<>();
    private final Map<String,JLabel> infoValues = new java.util.LinkedHashMap<>();
    private final DefaultListModel<String> receiptListModel = new DefaultListModel<>();
    private JList<String> receiptList;
    private List<BienNhanDuLieu> selectedReceipts = List.of();
    private List<BienNhanHoanTraDuLieu> selectedRefunds = List.of();
    private JTextField paymentAmount, paymentNotes;
    private JComboBox<PhuongThucThanhToan> paymentMethod;
    private JLabel patientHeading, actualAmount, lblVoucherType, lblRefundSummary;
    private JButton btnRefund;
    private boolean fillingReceipt;
    private JPanel footerPanel;
    private JCheckBox insuredFilter, feeFilter, outpatientFilter, inpatientFilter;
    private JComboBox<String> recordFilter, dateFilter;
    private boolean fillingFilters;


    /** Dựng bàn thu ngân theo bố cục ảnh: bệnh nhân trái, dịch vụ và thông tin phải. */
    private void buildUI() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        JPanel header = new JPanel(new BorderLayout());
        header.add(buildToolbar(), BorderLayout.NORTH);
        header.add(buildSearchStrip(), BorderLayout.SOUTH);
        add(header, BorderLayout.NORTH);

        JPanel main = new JPanel(new BorderLayout()) {
            @Override public void doLayout() {
                if (footerPanel != null) footerPanel.setPreferredSize(new Dimension(500,(int)Math.round(getHeight()*.397)));
                super.doLayout();
            }
        };
        main.add(buildInvoiceDetailSection(), BorderLayout.CENTER);
        footerPanel = buildSummaryFooter();
        main.add(footerPanel, BorderLayout.SOUTH);
        JPanel right = new JPanel(new BorderLayout()) {
            @Override public void doLayout() {
                receiptList.getParent().getParent().setPreferredSize(new Dimension((int)Math.round(getWidth()*.127),100));
                super.doLayout();
            }
        };
        receiptList = new JList<>(receiptListModel);
        receiptList.setFont(CHU_MAU.deriveFont(Font.BOLD));
        receiptList.setSelectionBackground(new Color(0,145,255)); receiptList.setSelectionForeground(Color.WHITE);
        receiptList.addListSelectionListener(e -> { if (!e.getValueIsAdjusting() && !fillingReceipt) showSelectedReceipt(); });
        JScrollPane receipts = new JScrollPane(receiptList);
        receipts.setBorder(new LineBorder(Color.LIGHT_GRAY)); receipts.setPreferredSize(new Dimension(158,100));
        right.add(main,BorderLayout.CENTER); right.add(receipts,BorderLayout.EAST);
        centerSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,buildInvoiceListSection(),right);
        centerSplitPane.setBorder(BorderFactory.createEmptyBorder());centerSplitPane.setDividerSize(2);
        centerSplitPane.setContinuousLayout(true);centerSplitPane.setResizeWeight(.22);
        add(centerSplitPane,BorderLayout.CENTER);
        addComponentListener(new java.awt.event.ComponentAdapter(){
            @Override public void componentResized(java.awt.event.ComponentEvent e){centerSplitPane.setDividerLocation((int)Math.round(getWidth()*.22));}
        });
        setupKeyboardShortcuts();
        refreshData();
    }

    @Override public void doLayout(){
        super.doLayout();
        if(centerSplitPane!=null)centerSplitPane.setDividerLocation((int)Math.round(getWidth()*.22));
    }

    private void setupKeyboardShortcuts() {
        InputMap inputMap = getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        ActionMap actionMap = getActionMap();

        inputMap.put(KeyStroke.getKeyStroke("F2"), "actionCollectPayment");
        actionMap.put("actionCollectPayment", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleCollectPayment();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke("F7"), "actionOpenBillingSlip");
        actionMap.put("actionOpenBillingSlip", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleOpenBillingSlip();
            }
        });
    }

    private VienPhiMenu menuPopup;
    private String selectedReceiptBook = "duongpq test";
    private String selectedDepartmentFilter = "";
    private vn.clinicmanagement.application.billing.QuanLyDoanhThuReadPort receiptReadPort;

    public VienPhiMenu getMenuPopup() {
        return menuPopup;
    }

    public void setReceiptReadPort(vn.clinicmanagement.application.billing.QuanLyDoanhThuReadPort receiptReadPort) {
        this.receiptReadPort = receiptReadPort;
    }

    public DanhSachPhieuThuHopThoai createReceiptListPanel() {
        var port = receiptReadPort != null ? receiptReadPort
                : (GraphicsEnvironment.isHeadless() || System.getProperty("surefire.test.class.path") != null
                ? null
                : new vn.clinicmanagement.infrastructure.persistence.billing.QuanLyDoanhThuRepository(
                        new vn.clinicmanagement.infrastructure.persistence.JdbcKetNoiNhaCungCap(
                                new vn.clinicmanagement.infrastructure.persistence.JdbcGiaoDichQuanLy(
                                        new vn.clinicmanagement.infrastructure.persistence.JdbcUnitOfWork()))));
        java.time.LocalDateTime from = filterFromDate != null ? filterFromDate.atStartOfDay() : LocalDate.now(BUSINESS_ZONE).atStartOfDay();
        java.time.LocalDateTime to = filterToDate != null ? filterToDate.atTime(23, 59) : LocalDate.now(BUSINESS_ZONE).atTime(23, 59);
        return new DanhSachPhieuThuHopThoai(port, from, to);
    }

    /** Thanh công cụ hai hàng theo thiết kế gốc. */
    private JPanel buildToolbar() {
        JPanel toolbar = new JPanel(null){
            @Override public void doLayout(){super.doLayout();if(patientHeading!=null)patientHeading.setBounds(820,5,Math.max(0,getWidth()-830),62);}
        };
        toolbar.setBackground(MAU_NEN);
        // Keep room for the third filter row; y=48 plus the 25px checkbox height.
        toolbar.setPreferredSize(new Dimension(900, 76));
        toolbar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(186, 186, 186)));
        JButton menu = toolbarButton("Menu", "menu");
        menu.setBounds(4, 8, 60, 57);
        btnCollect = new JButton("Thu tiền (F2)");
        btnCollect.addActionListener(e -> handleCollectPayment());
        cboFilterStatus = new JComboBox<>(new String[]{"Tất cả hóa đơn", "Chưa thanh toán", "Đã thanh toán", "Miễn giảm / BHYT"});
        cboFilterStatus.addActionListener(e -> applyFilter());
        menuPopup = new VienPhiMenu(this::handleMenuSelection);
        menu.addActionListener(e -> {
            if (menuPopup.isVisible()) menuPopup.setVisible(false);
            else if (menu.isShowing()) menuPopup.show(menu, 0, menu.getHeight());
        });
        toolbar.add(menu);
        btnPrintSlip = toolbarButton("In", "print");
        btnPrintSlip.setBounds(60, 4, 55, 65);
        btnPrintSlip.addActionListener(e -> handleOpenBillingSlip());
        toolbar.add(btnPrintSlip);

        JLabel find = label("Tìm theo"); find.setVisible(false); find.setBounds(0,0,0,0); toolbar.add(find);
        JComboBox<String> dateType = combo("Ngày vào viện", "Ngày ra viện", "Ngày lập hóa đơn");dateFilter=dateType;
        dateFilter.addActionListener(e->applyFilter());
        dateType.setBounds(122, 4, 212, 29); toolbar.add(dateType);
        JLabel record = label("Bệnh án"); record.setVisible(false); record.setBounds(0,0,0,0); toolbar.add(record);
        JComboBox<String> records = combo("(Tất cả bệnh án)");recordFilter=records;
        recordFilter.addActionListener(e->{if(!fillingFilters)applyFilter();});
        records.setBounds(122, 37, 212, 30); toolbar.add(records);
        insuredFilter=check("BHYT");insuredFilter.setBounds(340,3,88,25);toolbar.add(insuredFilter);
        feeFilter=check("Viện phí");feeFilter.setBounds(340,29,95,25);toolbar.add(feeFilter);
        JCheckBox exam=check("Khám bệnh");exam.setBounds(435,1,112,25);exam.addActionListener(e->exam.setSelected(true));toolbar.add(exam);
        JCheckBox outpatient = check("Ngoại trú");outpatientFilter=outpatient; outpatient.setBounds(435, 25, 102, 25); toolbar.add(outpatient);
        JCheckBox inpatient = check("Nội trú");inpatientFilter=inpatient;
        for(var filter:java.util.List.of(insuredFilter,feeFilter,outpatientFilter,inpatientFilter))filter.addActionListener(e->applyFilter()); inpatient.setBounds(435, 48, 102, 25); toolbar.add(inpatient);
        btnRefresh = toolbarButton("Làm Mới", "refresh");
        btnRefresh.setBounds(540, 4, 78, 65); btnRefresh.addActionListener(e -> onShow()); toolbar.add(btnRefresh);
        JButton search = toolbarButton("Tìm Kiếm", "search");
        search.setBounds(625, 4, 85, 65);
        search.addActionListener(e -> {
            if (GraphicsEnvironment.isHeadless()) return;
            String value = JOptionPane.showInputDialog(this, "Mã lượt khám / bệnh nhân:", txtSearch.getText());
            if (value != null) { txtSearch.setText(value); onSearchTriggered(); }
        });
        toolbar.add(search);
        JButton collect = toolbarButton("Thanh Toán", "checked");
        collect.setBounds(720,4,95,65); collect.addActionListener(e -> newPayment());toolbar.add(collect);
        patientHeading = label("");patientHeading.setForeground(new Color(140,0,0));
        patientHeading.setHorizontalAlignment(SwingConstants.RIGHT);
        toolbar.add(patientHeading);
        toolbar.addComponentListener(new java.awt.event.ComponentAdapter(){
            @Override public void componentResized(java.awt.event.ComponentEvent e){patientHeading.setBounds(820,5,Math.max(0,toolbar.getWidth()-830),62);}
        });
        txtSearch = new JTextField();
        return toolbar;
    }

    /** Dải tìm kiếm vàng nhạt chạy ngang cả hai bảng. */
    private JPanel buildSearchStrip() {
        JPanel strip = new JPanel(new BorderLayout(0, 0)) {
            @Override public void doLayout() {
                if (getComponentCount() > 0) getComponent(0).setPreferredSize(
                        new Dimension((int) Math.round(getWidth() * .22), 32));
                super.doLayout();
            }
        };
        strip.setPreferredSize(new Dimension(100, 28));
        strip.setBackground(new Color(255, 255, 207));
        strip.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(186, 186, 186)));
        JPanel range = new JPanel(new BorderLayout()); range.setOpaque(false);
        JButton findPatient = smallSearchButton(); findPatient.addActionListener(e -> onSearchTriggered());
        range.add(txtSearch,BorderLayout.CENTER);
        txtSearch.setBorder(new EmptyBorder(0,4,0,0));txtSearch.setFont(CHU_MAU);txtSearch.setOpaque(false);
        txtSearch.putClientProperty("JTextField.placeholderText", "Tìm tên, mã BN, mã lượt...");
        txtSearch.getDocument().addDocumentListener(new javax.swing.event.DocumentListener(){
            public void insertUpdate(javax.swing.event.DocumentEvent e){applyFilter();}
            public void removeUpdate(javax.swing.event.DocumentEvent e){applyFilter();}
            public void changedUpdate(javax.swing.event.DocumentEvent e){applyFilter();}
        });
        range.add(findPatient, BorderLayout.EAST);
        strip.add(range, BorderLayout.WEST);
        txtServiceSearch = new JTextField(); txtServiceSearch.setFont(CHU_MAU);
        txtServiceSearch.setBorder(new EmptyBorder(0, 8, 0, 0));
        txtServiceSearch.setOpaque(false); txtServiceSearch.putClientProperty("JTextField.placeholderText", "Tìm kiếm dịch vụ");
        txtServiceSearch.setUI(new javax.swing.plaf.basic.BasicTextFieldUI() {
            @Override protected void paintSafely(Graphics g) {
                super.paintSafely(g);
                if (txtServiceSearch.getText().isEmpty() && !txtServiceSearch.isFocusOwner()) {
                    g.setColor(Color.BLACK); g.setFont(CHU_MAU);
                    g.drawString("Tìm kiếm dịch vụ", 8, (txtServiceSearch.getHeight() + g.getFontMetrics().getAscent()) / 2 - 2);
                }
            }
        });
        txtServiceSearch.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { filterServiceRows(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { filterServiceRows(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { filterServiceRows(); }
        });
        strip.add(txtServiceSearch, BorderLayout.CENTER);
        JButton findService = smallSearchButton(); findService.addActionListener(e -> filterServiceRows());
        strip.add(findService, BorderLayout.EAST);
        strip.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override public void componentResized(java.awt.event.ComponentEvent e) {
                range.setPreferredSize(new Dimension((int) (strip.getWidth() * .22) + 8, 24)); strip.revalidate();
            }
        });
        return strip;
    }

    /** Giữ mô hình hóa đơn hiện có; hai cột nhìn thấy là tên người bệnh và tuổi. */
    private JPanel buildInvoiceListSection() {
        String[] columns = {"STT", "Số Phiếu Thu", "Mã BN", "Tên Bệnh Nhân", "Ngày Lập", "Đối Tượng",
                "Tổng Tiền", "BHYT Chi Trả", "BN Cần Trả", "Đã Thu", "Còn Nợ", "Trạng Thái", "Tuổi"};
        invoiceTableModel = readOnlyModel(columns);
        invoiceTable = designTable(invoiceTableModel);
        for (int i = invoiceTable.getColumnCount() - 1; i >= 0; i--) {
            int model = invoiceTable.getColumnModel().getColumn(i).getModelIndex();
            if (model != 3 && model != 12) invoiceTable.removeColumn(invoiceTable.getColumnModel().getColumn(i));
        }
        invoiceTable.getColumnModel().getColumn(0).setPreferredWidth(268);
        invoiceTable.getColumnModel().getColumn(1).setPreferredWidth(66);
        invoiceTable.getColumnModel().getColumn(1).setMaxWidth(66);
        invoiceTable.setRowHeight(38);
        invoiceTable.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer(){
            @Override public Component getTableCellRendererComponent(JTable t,Object v,boolean selected,boolean focus,int row,int col){
                super.getTableCellRendererComponent(t,v,selected,focus,row,col);
                if(row<displayedInvoices.size()){
                    var inv=displayedInvoices.get(row);var info=visitDetails.get(inv.visitId());
                    String name=String.valueOf(v).replace("&","&amp;").replace("<","&lt;");
                    setText("<html><b>"+name+"</b><br>"+(info==null?inv.patientId():info.patientCode())+" - "+invoiceTableModel.getValueAt(row,5)+"</html>");
                }return this;
            }
        });
        invoiceTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        invoiceTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) loadSelectedInvoiceDetails();
        });
        JPanel panel = new JPanel(new BorderLayout());
        panel.setMinimumSize(new Dimension(150, 100));

        JPanel dateRangeBar = new JPanel(new BorderLayout(4, 0));
        dateRangeBar.setBackground(new Color(255, 255, 208));
        dateRangeBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(195, 205, 185)),
                new EmptyBorder(2, 6, 2, 6)));
        dateRangeBar.setPreferredSize(new Dimension(150, 24));
        lblRange = label(filterFromDate.format(SHORT_DATE_FMT) + " >> " + filterToDate.format(SHORT_DATE_FMT) + " - BH(0) VP(0)");
        lblRange.setFont(UngDungPhongChu.SMALL);
        lblRange.setForeground(new Color(20, 20, 20));
        lblRange.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lblRange.setToolTipText("Bấm để chọn khoảng ngày khám (Hôm nay, Hôm qua, 7 ngày, 30 ngày, Tùy chọn...)");
        JLabel dropHint = label("▾");
        dropHint.setFont(UngDungPhongChu.SMALL);
        dropHint.setForeground(new Color(80, 80, 80));
        dropHint.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        var openRangeMenu = new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                showDateRangeMenu(dateRangeBar);
            }
        };
        lblRange.addMouseListener(openRangeMenu);
        dropHint.addMouseListener(openRangeMenu);
        dateRangeBar.add(lblRange, BorderLayout.CENTER);
        dateRangeBar.add(dropHint, BorderLayout.EAST);

        panel.add(dateRangeBar, BorderLayout.NORTH);
        panel.add(tableScroll(invoiceTable, false), BorderLayout.CENTER);
        return panel;
    }

    private void showDateRangeMenu(Component invoker) {
        JPopupMenu menu = new JPopupMenu();
        LocalDate today = LocalDate.now(BUSINESS_ZONE);

        JMenuItem miToday = new JMenuItem("Hôm nay (" + today.format(SHORT_DATE_FMT) + ")");
        miToday.addActionListener(e -> setDateRange(today, today));
        menu.add(miToday);

        LocalDate yesterday = today.minusDays(1);
        JMenuItem miYesterday = new JMenuItem("Hôm qua (" + yesterday.format(SHORT_DATE_FMT) + ")");
        miYesterday.addActionListener(e -> setDateRange(yesterday, yesterday));
        menu.add(miYesterday);

        JMenuItem mi7Days = new JMenuItem("7 ngày gần đây");
        mi7Days.addActionListener(e -> setDateRange(today.minusDays(6), today));
        menu.add(mi7Days);

        JMenuItem mi30Days = new JMenuItem("30 ngày gần đây");
        mi30Days.addActionListener(e -> setDateRange(today.minusDays(29), today));
        menu.add(mi30Days);

        menu.addSeparator();
        JMenuItem miCustom = new JMenuItem("Chọn khoảng ngày...");
        miCustom.addActionListener(e -> promptCustomDateRange());
        menu.add(miCustom);

        menu.show(invoker, 0, invoker.getHeight());
    }

    private void promptCustomDateRange() {
        if (GraphicsEnvironment.isHeadless()) return;
        JTextField txtFrom = new JTextField(filterFromDate.format(INPUT_DATE_FMT), 12);
        JTextField txtTo = new JTextField(filterToDate.format(INPUT_DATE_FMT), 12);
        JPanel p = new JPanel(new GridLayout(2, 2, 6, 6));
        p.add(new JLabel("Từ ngày (dd/MM/yyyy):"));
        p.add(txtFrom);
        p.add(new JLabel("Đến ngày (dd/MM/yyyy):"));
        p.add(txtTo);
        int opt = JOptionPane.showConfirmDialog(this, p, "Lọc Danh Sách Viện Phí Theo Ngày",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (opt != JOptionPane.OK_OPTION) return;
        try {
            LocalDate start = LocalDate.parse(txtFrom.getText().trim(), INPUT_DATE_FMT);
            LocalDate end = LocalDate.parse(txtTo.getText().trim(), INPUT_DATE_FMT);
            setDateRange(start, end);
        } catch (DateTimeException ex) {
            showMessage("Ngày không hợp lệ. Vui lòng nhập theo định dạng dd/MM/yyyy.",
                    "Lọc Viện Phí", JOptionPane.WARNING_MESSAGE);
        }
    }

    public void setDateRange(LocalDate from, LocalDate to) {
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        LocalDate start = from != null ? from : today;
        LocalDate end = to != null ? to : start;
        if (start.isAfter(end)) {
            LocalDate tmp = start;
            start = end;
            end = tmp;
        }
        this.filterFromDate = start;
        this.filterToDate = end;
        updateSummaryCounters();
        onShow();
    }

    /** Bảng dịch vụ với độ rộng cột đo theo ảnh mẫu. */
    private JPanel buildInvoiceDetailSection() {
        detailTableModel = readOnlyModel(new String[]{"Tên Dịch Vụ", "Đơn Vị", "Đối Tượng", "Số Lượng", "Đơn Giá",
                "Tỷ Lệ TT", "Thành Tiền", "BHYT Trả", "Tỷ Lệ BHTT", "BN Trả", "Đã Thu", "YC Thu"});
        detailTable = designTable(detailTableModel);
        detailTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        int[] widths = {300,64,96,50,82,60,92,0,0,90,85};
        for (int index : new int[]{8,7}) detailTable.removeColumn(detailTable.getColumnModel().getColumn(index));
        widths = new int[]{300,64,96,50,82,60,92,90,85,85};
        for (int i = 0; i < widths.length; i++) {
            detailTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
            detailTable.getColumnModel().getColumn(i).setWidth(widths[i]);
            if (i > 2) {
                DefaultTableCellRenderer numeric = new DefaultTableCellRenderer(); numeric.setHorizontalAlignment(SwingConstants.RIGHT);
                detailTable.getColumnModel().getColumn(i).setCellRenderer(numeric);
            }
        }
        detailTable.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer(){
            @Override public Component getTableCellRendererComponent(JTable t,Object value,boolean selected,boolean focus,int row,int col){
                super.getTableCellRendererComponent(t,value,selected,focus,row,col);
                var invoice=selectedInvoice();
                int modelRow=row>=0&&row<t.getRowCount()?t.convertRowIndexToModel(row):-1;
                int prevModelRow=row>0?t.convertRowIndexToModel(row-1):-1;
                if(invoice!=null && modelRow>=0 && modelRow<invoice.lines().size()){
                    var line=invoice.lines().get(modelRow);String group=resolveDepartment(line.itemType());
                    String text=Objects.toString(value,"").replace("&","&amp;").replace("<","&lt;");
                    boolean first=row==0||prevModelRow<0||prevModelRow>=invoice.lines().size()
                            ||!Objects.equals(invoice.lines().get(prevModelRow).itemType(),line.itemType());
                    setText(first?"<html><font color='#0039ac'>"+group+"</font><br>✓ "+text+"</html>":"✓ "+text);
                }return this;
            }
        });
        String[] headings={"Tên Dịch Vụ","Đơn Vị","Đối Tượng","SL","Đơn Giá","% TT","Thành Tiền","BN Trả","Đã Thu","YC Thu"};
        for(int i=0;i<headings.length;i++)detailTable.getColumnModel().getColumn(i).setHeaderValue(headings[i]);
        detailTable.setToolTipText("Đã thu và yêu cầu thu được theo dõi theo hóa đơn; chưa phân bổ cho từng dịch vụ.");
        JPanel panel = new JPanel(new BorderLayout()); panel.add(tableScroll(detailTable, false));
        return panel;
    }

    /** Hai hàng số tiền và bốn hàng thông tin người bệnh nằm dưới bảng dịch vụ. */
    private JPanel buildSummaryFooter() {
        JPanel footer = new JPanel(new BorderLayout());footer.setBackground(MAU_NEN);
        footer.setBorder(new LineBorder(Color.GRAY));footer.setPreferredSize(new Dimension(900,210));
        JPanel totals = new JPanel(new java.awt.GridLayout(2,4,6,0));totals.setOpaque(false);totals.setPreferredSize(new Dimension(900,42));
        lblTotalAmount=money(Color.RED);lblInsuranceAmount=money(Color.RED);lblDiscountAmount=money(Color.RED);lblPatientAmount=money(Color.RED);
        lblAdvanceAmount=money(Color.BLUE);lblPaidAmount=money(Color.BLUE);lblUnpaidAmount=money(Color.RED);
        totals.add(metric("Tổng chi phí:",lblTotalAmount));totals.add(metric("BHYT:",lblInsuranceAmount));
        totals.add(metric("Miễn giảm dịch vụ:",lblDiscountAmount));totals.add(metric("Bệnh nhân:",lblPatientAmount));
        totals.add(metric("Tạm ứng:",lblAdvanceAmount));totals.add(metric("Đã thu:",lblPaidAmount));
        lblRefundSummary=money(new Color(180,0,0));
        totals.add(metric("Hoàn trả:",lblRefundSummary));totals.add(metric("Còn nợ:",lblUnpaidAmount));
        footer.add(totals,BorderLayout.NORTH);
        JPanel form = new JPanel(new java.awt.GridLayout(5,3,8,0));form.setOpaque(false);
        for(String key:new String[]{"Khoa phòng:","Đối tượng:","Trạng thái:","Ngày vào viện:","Ngày ra viện:","Số ngày điều trị:","Số phiếu:","Người thu:","Ngày thu:"}){
            JLabel value=label("");value.setForeground(Color.BLUE);infoValues.put(key,value);
            JPanel cell=new JPanel(new BorderLayout(5,0));cell.setOpaque(false);cell.add(label(key),BorderLayout.WEST);cell.add(value,BorderLayout.CENTER);
            cell.setBorder(BorderFactory.createMatteBorder(0,0,1,0,Color.LIGHT_GRAY));form.add(cell);
        }
        lblVoucherType=label("Thu tiền");lblVoucherType.setForeground(Color.BLUE);
        form.add(metric("Loại phiếu:",lblVoucherType));
        paymentAmount=new JTextField();paymentAmount.setFont(CHU_MAU);form.add(inputCell("Số tiền:",paymentAmount));
        actualAmount=money(Color.RED);
        paymentAmount.getDocument().addDocumentListener(new javax.swing.event.DocumentListener(){
            private void update(){try{actualAmount.setText(formatDesignMoney(new BigDecimal(paymentAmount.getText().trim())));}catch(NumberFormatException ex){actualAmount.setText("—");}}
            public void insertUpdate(javax.swing.event.DocumentEvent e){update();}public void removeUpdate(javax.swing.event.DocumentEvent e){update();}public void changedUpdate(javax.swing.event.DocumentEvent e){update();}
        });
        form.add(metric("Số tiền thực thu:",actualAmount));
        paymentMethod=new JComboBox<>(PhuongThucThanhToan.values());paymentMethod.setFont(CHU_MAU);paymentMethod.setRenderer(new DefaultListCellRenderer(){
            @Override public Component getListCellRendererComponent(JList<?> list,Object value,int index,boolean selected,boolean focus){return super.getListCellRendererComponent(list,value instanceof PhuongThucThanhToan method?method.getDescription():value,index,selected,focus);}
        });form.add(inputCell("Hình thức:",paymentMethod));
        paymentNotes=new JTextField("Thu tiền tại quầy thu ngân");paymentNotes.setFont(CHU_MAU);form.add(inputCell("Nội dung thu:",paymentNotes));
        form.add(label(""));footer.add(form,BorderLayout.CENTER);
        JPanel buttons=new JPanel(new FlowLayout(FlowLayout.RIGHT,18,6));buttons.setOpaque(false);buttons.setPreferredSize(new Dimension(600,44));
        btnCollect=new JButton("▣  Lưu Phiếu (F2)");btnCollect.setFont(CHU_MAU);btnCollect.addActionListener(e->handleCollectPayment());buttons.add(btnCollect);
        btnRefund=new JButton("↩  Hoàn Tiền (F4)");btnRefund.setFont(CHU_MAU);btnRefund.setEnabled(false);btnRefund.addActionListener(e->handleIssueRefund());buttons.add(btnRefund);
        JButton cancel=new JButton("✕  Hủy Bỏ (F3)");cancel.setFont(CHU_MAU);cancel.addActionListener(e->newPayment());buttons.add(cancel);footer.add(buttons,BorderLayout.SOUTH);
        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("F3"),"cancelPayment");
        getActionMap().put("cancelPayment",new AbstractAction(){public void actionPerformed(ActionEvent e){newPayment();}});
        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("F4"),"issueRefund");
        getActionMap().put("issueRefund",new AbstractAction(){public void actionPerformed(ActionEvent e){handleIssueRefund();}});
        return footer;
    }
    private static JPanel inputCell(String text,JComponent field){JPanel cell=new JPanel(new BorderLayout(6,0));cell.setOpaque(false);cell.add(label(text),BorderLayout.WEST);cell.add(field,BorderLayout.CENTER);return cell;}
    private void setInfo(String key,String value){if(infoValues.containsKey(key)){infoValues.get(key).setText(value==null?"":value);infoValues.get(key).setToolTipText(value);}}
    private HoaDonDuLieu selectedInvoice(){int row=invoiceTable.getSelectedRow();return row>=0&&row<displayedInvoices.size()?displayedInvoices.get(row):null;}
    private void newPayment(){
        fillingReceipt=true;receiptList.clearSelection();fillingReceipt=false;
        var invoice=selectedInvoice();if(paymentAmount==null)return;
        paymentAmount.setEditable(!paymentPending);paymentNotes.setEditable(!paymentPending);paymentMethod.setEnabled(!paymentPending);
        BigDecimal pendingRefund=invoice!=null&&invoice.pendingRefundAmount()!=null?invoice.pendingRefundAmount():BigDecimal.ZERO;
        BigDecimal remaining=invoice!=null&&invoice.remainingAmount()!=null?invoice.remainingAmount():BigDecimal.ZERO;
        if(pendingRefund.signum()>0&&remaining.signum()==0){
            if(lblVoucherType!=null){lblVoucherType.setText("Hoàn tiền");lblVoucherType.setForeground(new Color(180,0,0));}
            paymentAmount.setText(pendingRefund.toPlainString());
            paymentNotes.setText("Hoàn tiền chênh lệch đối tượng/dịch vụ");
            actualAmount.setText(formatDesignMoney(pendingRefund));
        }else{
            if(lblVoucherType!=null){lblVoucherType.setText("Thu tiền");lblVoucherType.setForeground(Color.BLUE);}
            paymentAmount.setText(invoice==null?"":remaining.toPlainString());
            paymentNotes.setText("Thu tiền tại quầy thu ngân");
            actualAmount.setText(invoice==null?"0đ":formatDesignMoney(remaining));
        }
        btnCollect.setEnabled(!paymentPending&&invoice!=null&&remaining.signum()>0);
        if(btnRefund!=null)btnRefund.setEnabled(!paymentPending&&invoice!=null&&pendingRefund.signum()>0);
        setInfo("Số phiếu:","Tạo mới khi lưu");setInfo("Người thu:",NguoiDungPhien.getInstance().getFullName());setInfo("Ngày thu:","");
    }
    private void showSelectedReceipt(){
        int index=receiptList.getSelectedIndex();if(index<0)return;
        var inv=selectedInvoice();var info=inv==null?null:visitDetails.get(inv.visitId());
        if(index<selectedReceipts.size()){
            var receipt=selectedReceipts.get(index);
            if(lblVoucherType!=null){lblVoucherType.setText("Thu tiền");lblVoucherType.setForeground(Color.BLUE);}
            paymentAmount.setText(receipt.amount().toPlainString());paymentAmount.setEditable(false);paymentNotes.setText(receipt.notes());paymentNotes.setEditable(false);
            paymentMethod.setSelectedItem(receipt.paymentMethod());paymentMethod.setEnabled(false);btnCollect.setEnabled(false);if(btnRefund!=null)btnRefund.setEnabled(false);
            setInfo("Số phiếu:","PT-"+receipt.id());setInfo("Người thu:",info==null?String.valueOf(receipt.cashierId()):info.collectors().getOrDefault(receipt.cashierId(),String.valueOf(receipt.cashierId())));
            setInfo("Ngày thu:",receipt.paymentTime()==null?"":receipt.paymentTime().format(dateFormatter));actualAmount.setText(formatDesignMoney(receipt.amount()));
        }else if(index-selectedReceipts.size()<selectedRefunds.size()){
            var refund=selectedRefunds.get(index-selectedReceipts.size());
            if(lblVoucherType!=null){lblVoucherType.setText("Hoàn tiền");lblVoucherType.setForeground(new Color(180,0,0));}
            paymentAmount.setText(refund.amount().toPlainString());paymentAmount.setEditable(false);paymentNotes.setText(refund.reason());paymentNotes.setEditable(false);
            paymentMethod.setSelectedItem(refund.paymentMethod());paymentMethod.setEnabled(false);btnCollect.setEnabled(false);if(btnRefund!=null)btnRefund.setEnabled(false);
            setInfo("Số phiếu:","HT-"+refund.id());setInfo("Người thu:",info==null?String.valueOf(refund.cashierId()):info.collectors().getOrDefault(refund.cashierId(),String.valueOf(refund.cashierId())));
            setInfo("Ngày thu:",refund.refundTime()==null?"":refund.refundTime().format(dateFormatter));actualAmount.setText("-"+formatDesignMoney(refund.amount()));
        }
    }

    private static JLabel label(String text) { JLabel l = new JLabel(text); l.setFont(CHU_MAU); l.setForeground(Color.BLACK); return l; }
    private static JLabel money(Color color) { JLabel l = label("0đ"); l.setForeground(color); return l; }
    private static JPanel metric(String title, JLabel value) {
        JPanel p = new JPanel(new BorderLayout()); p.setOpaque(false); p.add(label(title), BorderLayout.CENTER); p.add(value, BorderLayout.EAST); return p;
    }
    private static JComboBox<String> combo(String... choices) {
        JComboBox<String> c = new JComboBox<>(choices); c.setFont(CHU_MAU); c.setBackground(Color.WHITE);
        c.setUI(new javax.swing.plaf.basic.BasicComboBoxUI()); c.setBorder(new LineBorder(new Color(150,150,150))); return c;
    }
    private static JCheckBox check(String title) {
        JCheckBox c = new JCheckBox(title, true); c.setFont(CHU_MAU); c.setOpaque(false);
        c.setIcon(new ThuNganBieuTuong("unchecked", 16)); c.setSelectedIcon(new ThuNganBieuTuong("checked", 16)); return c;
    }
    private static DefaultTableModel readOnlyModel(String[] columns) {
        return new DefaultTableModel(columns, 0) { @Override public boolean isCellEditable(int row, int col) { return false; } };
    }
    private static CoKieuBang designTable(DefaultTableModel model) {
        CoKieuBang table = new CoKieuBang(model) {
            @Override protected void paintComponent(Graphics graphics) {
                super.paintComponent(graphics);

            }
        };
        table.setFont(CHU_MAU); table.setRowHeight(24); table.setGridColor(MAU_LUOI);
        table.setShowHorizontalLines(false);table.setShowVerticalLines(false);
        table.setBackground(Color.WHITE); table.setFillsViewportHeight(true); table.setIntercellSpacing(new Dimension(1,1));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setPreferredSize(new Dimension(100, 26));
        table.getTableHeader().setReorderingAllowed(false);
        DefaultTableCellRenderer firstColumn = new DefaultTableCellRenderer();
        firstColumn.setHorizontalAlignment(SwingConstants.LEFT);
        table.getColumnModel().getColumn(0).setCellRenderer(firstColumn);
        DefaultTableCellRenderer header = new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable owner, Object value,
                    boolean selected, boolean focused, int row, int column) {
                super.getTableCellRendererComponent(owner, value, selected, focused, row, column);
                setHorizontalAlignment(column == 0 ? SwingConstants.LEFT : SwingConstants.CENTER);
                setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 1, MAU_LUOI),
                        new EmptyBorder(0, column == 0 ? 39 : 0, 0, 0)));
                return this;
            }
        };
        header.setHorizontalAlignment(SwingConstants.CENTER); header.setFont(UngDungPhongChu.BODY_BOLD);
        header.setForeground(Color.BLACK); header.setBackground(Color.WHITE);
        header.setBorder(BorderFactory.createMatteBorder(0,0,1,1,MAU_LUOI));
        table.getTableHeader().setDefaultRenderer(header);
        return table;
    }
    private static JScrollPane tableScroll(JTable table, boolean horizontal) {
        JScrollPane scroll = new JScrollPane(table, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                horizontal ? JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS : JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setColumnHeaderView(table.getTableHeader());
        scroll.setBorder(BorderFactory.createEmptyBorder()); scroll.getViewport().setBackground(Color.WHITE);
        scroll.getHorizontalScrollBar().setPreferredSize(new Dimension(100,14));
        scroll.getHorizontalScrollBar().setUI(new javax.swing.plaf.metal.MetalScrollBarUI());
        return scroll;
    }
    private static JButton toolbarButton(String title, String icon) {
        JButton b = new JButton(title, new ThuNganBieuTuong(icon,24)); b.setFont(UngDungPhongChu.SMALL);
        b.setVerticalTextPosition(SwingConstants.BOTTOM); b.setHorizontalTextPosition(SwingConstants.CENTER);
        b.setMargin(new Insets(0,0,0,0)); b.setBorder(BorderFactory.createEmptyBorder());
        b.setFocusPainted(false); b.setContentAreaFilled(false); b.setIconTextGap(2); b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); return b;
    }
    private static JButton smallSearchButton() {
        JButton b = new JButton(new ThuNganBieuTuong("search",18)); b.setPreferredSize(new Dimension(26,26));
        b.setBackground(new Color(248,248,248)); b.setBorder(new LineBorder(new Color(180,180,180))); return b;
    }
    private void filterServiceRows() {
        if (detailTable == null || txtServiceSearch == null) return;
        var sorter = new javax.swing.table.TableRowSorter<DefaultTableModel>(detailTableModel);
        String keyword = txtServiceSearch.getText().trim();
        if (!keyword.isEmpty()) sorter.setRowFilter(RowFilter.regexFilter("(?iu)" + java.util.regex.Pattern.quote(keyword), 0));
        detailTable.setRowSorter(sorter);
        updateDetailRowHeights();
    }

    private void updateDetailRowHeights() {
        var invoice = selectedInvoice();
        if (detailTable == null || invoice == null || invoice.lines() == null) return;
        int viewCount = detailTable.getRowCount();
        for (int viewRow = 0; viewRow < viewCount; viewRow++) {
            int modelRow = detailTable.convertRowIndexToModel(viewRow);
            int prevModelRow = viewRow > 0 ? detailTable.convertRowIndexToModel(viewRow - 1) : -1;
            if (modelRow < 0 || modelRow >= invoice.lines().size()) continue;
            boolean first = viewRow == 0 || prevModelRow < 0 || prevModelRow >= invoice.lines().size()
                    || !Objects.equals(invoice.lines().get(prevModelRow).itemType(),
                            invoice.lines().get(modelRow).itemType());
            detailTable.setRowHeight(viewRow, first ? 38 : 24);
        }
    }

    /** Biểu tượng vector thu ngân, vẽ rõ ở cả màn hình Retina. */
    private record ThuNganBieuTuong(String type, int size) implements Icon {
        public int getIconWidth() { return size; }
        public int getIconHeight() { return size; }
        public void paintIcon(Component c, Graphics graphics, int x, int y) {
            Graphics2D g = (Graphics2D) graphics.create(); g.translate(x,y); g.scale(size / 36.0,size / 36.0);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(0,112,195)); g.setStroke(new BasicStroke(3));
            switch (type) {
                case "menu" -> { g.setStroke(new BasicStroke(4)); for (int yy : new int[]{8,18,28}) g.drawLine(1,yy,35,yy); }
                case "search" -> { g.setColor(new Color(184,221,242)); g.fillOval(12,1,22,22); g.setColor(new Color(85,148,188)); g.setStroke(new BasicStroke(1.5f)); g.drawOval(12,1,22,22); g.setStroke(new BasicStroke(5)); g.drawLine(14,22,2,35); }
                case "refresh" -> { g.setColor(new Color(0,145,215)); g.drawArc(6,3,25,26,35,270); g.fillPolygon(new int[]{27,34,34},new int[]{6,1,13},3); g.fillPolygon(new int[]{9,2,2},new int[]{27,33,21},3); }
                case "print" -> { g.setColor(new Color(115,125,130)); g.fillRoundRect(2,12,32,16,3,3); g.setColor(Color.WHITE); g.fillRect(10,1,16,15); g.fillRect(9,23,18,12); g.setColor(new Color(80,90,100)); g.setStroke(new BasicStroke(1.5f)); g.drawRect(10,1,16,15); g.drawRect(9,23,18,12); g.setColor(new Color(0,130,185)); g.drawLine(12,28,24,28); }
                case "checked", "unchecked" -> { g.setColor(Color.WHITE); g.fillRect(2,2,30,30); g.setColor(Color.GRAY); g.setStroke(new BasicStroke(2)); g.drawRect(2,2,30,30); if ("checked".equals(type)) { g.setColor(Color.DARK_GRAY); g.setStroke(new BasicStroke(4)); g.drawPolyline(new int[]{7,14,28},new int[]{17,25,8},3); } }
                default -> { }
            }
            g.dispose();
        }
    }

    public void handleCollectPayment() {
        if (paymentPending || receiptList != null && receiptList.getSelectedIndex()>=0) return;
        int row = invoiceTable.getSelectedRow();
        if (row < 0) {
            showMessage("Vui lòng chọn một phiếu thu từ danh sách để thực hiện thu tiền!",
                    "Chưa Chọn Hóa Đơn", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String code = (String) invoiceTableModel.getValueAt(row, 1);
        String name = (String) invoiceTableModel.getValueAt(row, 3);
        String unpaidStr = (String) invoiceTableModel.getValueAt(row, 10);

        if (uncertainPayment == null && ("0 VNĐ".equals(unpaidStr) || "0".equals(unpaidStr))) {
            showMessage("Hóa đơn " + code + " của bệnh nhân " + name + " đã được thanh toán đầy đủ!",
                    "Thông Báo", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        if (services == null || services.recordPayment() == null) {
            showMessage("Chưa kết nối nghiệp vụ lưu phiếu thu. Hệ thống chưa ghi nhận thanh toán.",
                    "Chưa kết nối thu ngân", JOptionPane.ERROR_MESSAGE);
            return;
        }

        PhanQuyenNguCanh authzContext = currentAuthorizationContext();
        KiemToanTacNhanNguCanh auditContext = currentAuditActorContext();
        if (authzContext == null) {
            showMessage("Phiên làm việc không hợp lệ hoặc đã hết hạn.", "Lỗi Xác Thực", JOptionPane.ERROR_MESSAGE);
            return;
        }

        HoaDonDuLieu currentInvoice = row < displayedInvoices.size() ? displayedInvoices.get(row) : null;
        int invoiceId = currentInvoice != null ? currentInvoice.id() : 0;
        if (currentInvoice == null || invoiceId <= 0) return;
        RecordThanhToanCommand paymentCommand;
        if (uncertainPayment != null) {
            if (uncertainPayment.invoiceId() != invoiceId
                    || uncertainPayment.authorizationContext().employeeId() != authzContext.employeeId()
                    || uncertainPayment.authorizationContext().facilityId() != authzContext.facilityId()) {
                showMessage("Có giao dịch chưa xác định kết quả. Vui lòng kiểm tra lịch sử phiếu thu của hóa đơn trước đó.",
                        "Kiểm tra giao dịch", JOptionPane.WARNING_MESSAGE);
                return;
            }
            paymentCommand = new RecordThanhToanCommand(invoiceId, uncertainPayment.amount(),
                    uncertainPayment.paymentMethod(), uncertainPayment.debtDueDate(), uncertainPayment.notes(),
                    authzContext, auditContext, uncertainPayment.requestId());
        } else {
            BigDecimal enteredAmount = requestPaymentAmount(currentInvoice.remainingAmount());
            if (enteredAmount == null) return;
            paymentCommand = new RecordThanhToanCommand(invoiceId, enteredAmount,
                    (PhuongThucThanhToan)paymentMethod.getSelectedItem(), null, paymentNotes.getText().trim(), authzContext, auditContext);
        }
        final BigDecimal amountToPay = paymentCommand.amount();
        final long paymentEpoch = viewSessionEpoch.get();
        uncertainPayment = paymentCommand;
        paymentPending = true;
        btnCollect.setEnabled(false);

        SwingWorker<KetQua<RecordThanhToanBienNhan>, Void> worker = new SwingWorker<>() {
            @Override
            protected KetQua<RecordThanhToanBienNhan> doInBackground() {
                return services.recordPayment().handle(paymentCommand);
            }

            @Override
            protected void done() {
                try {
                    if (isCancelled()) return;
                    KetQua<RecordThanhToanBienNhan> result = get();
                    if (result != null && result.isSuccess()) {
                        uncertainPayment = null;
                        if (paymentEpoch != viewSessionEpoch.get()) return;
                        RecordThanhToanBienNhan receipt = result.getValue().orElse(null);
                        applyReceiptToDisplayedInvoice(row, currentInvoice, receipt, amountToPay, paymentCommand);
                        updateSummaryCounters();
                        String selectedFilter = cboFilterStatus != null ? (String) cboFilterStatus.getSelectedItem() : null;
                        if ("Chưa thanh toán".equals(selectedFilter)) {
                            applyFilter();
                        }
                        if (!GraphicsEnvironment.isHeadless() && hasDrugLines(currentInvoice)) {
                            openDrugPaymentBill(currentInvoice);
                        } else {
                            showMessage("Thu tiền thành công cho hóa đơn: " + code, "Thành Công", JOptionPane.INFORMATION_MESSAGE);
                        }
                    } else {
                        if (result != null && !result.getErrorCode().orElse("").startsWith("TRANSACTION")) {
                            uncertainPayment = null;
                        }
                        if (paymentEpoch != viewSessionEpoch.get()) return;
                        String errorMsg = result != null && result.getError().isPresent()
                                ? result.getError().get().message()
                                : "Thu tiền thất bại.";
                        showMessage(errorMsg, "Lỗi Thu Viện Phí", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (InterruptedException ex) {
                    MayTinhTacVuNenChanDoanLoi.restoreInterruptStatus();
                } catch (CancellationException ex) {
                    // Normal worker cancellation
                } catch (ExecutionException ex) {
                    showMessage("Lỗi hệ thống khi thu tiền: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                } finally {
                    paymentPending = false;
                    newPayment();
                    refreshData();
                }
            }
        };

        // A refresh may cancel reads, but must never interrupt a payment commit.
        worker.execute();
    }

    private void applyReceiptToDisplayedInvoice(int row, HoaDonDuLieu fallbackInvoice,
                                                RecordThanhToanBienNhan receipt, BigDecimal amountPaidThisTime, RecordThanhToanCommand command) {
        row = -1;
        if (fallbackInvoice != null) {
            for (int i = 0; i < displayedInvoices.size(); i++) {
                if (displayedInvoices.get(i).id() == fallbackInvoice.id()) { row = i; break; }
            }
        }
        HoaDonDuLieu target = row >= 0 ? displayedInvoices.get(row) : fallbackInvoice;
        if (target != null) {
            BigDecimal nextPaid = receipt != null && receipt.totalPaidSoFar() != null
                    ? receipt.totalPaidSoFar()
                    : safeMoney(target.amountPaid()).add(safeMoney(amountPaidThisTime));
            BigDecimal nextRemaining = receipt != null && receipt.remainingDebt() != null
                    ? receipt.remainingDebt()
                    : safeMoney(target.patientPaidAmount()).subtract(nextPaid).max(BigDecimal.ZERO);
            TrangThaiHoaDon nextStatus = receipt != null && receipt.invoiceStatus() != null
                    ? receipt.invoiceStatus()
                    : (nextRemaining.signum() == 0 ? TrangThaiHoaDon.DA_THANH_TOAN : TrangThaiHoaDon.THANH_TOAN_MOT_PHAN);

            List<BienNhanDuLieu> nextReceipts = new ArrayList<>(target.receipts() != null ? target.receipts() : List.of());
            if (receipt != null && receipt.receiptId() > 0
                    && nextReceipts.stream().noneMatch(item -> item.id() == receipt.receiptId())) {
                nextReceipts.add(new BienNhanDuLieu(
                        receipt.receiptId(),
                        target.id(),
                        safeMoney(receipt.amountPaidThisTime()),
                        receipt.paymentTime(),
                        command.paymentMethod(), command.authorizationContext().employeeId(), command.notes()
                ));
            }
            HoaDonDuLieu updated = new HoaDonDuLieu(
                    target.id(), target.invoiceNumber(), target.visitId(), target.patientId(), target.facilityId(),
                    target.invoiceDate(), target.totalAmount(), target.insurancePaidAmount(),
                    target.advanceAmount(), target.discountAmount(), target.discountPercentage(),
                    target.discountReason(), target.patientPaidAmount(), nextPaid, nextRemaining,
                    nextStatus, target.notes(), target.lines(), List.copyOf(nextReceipts),
                    target.serviceDiscountAmount(), target.refundAmount(),
                    nextPaid.subtract(safeMoney(target.patientPaidAmount())).max(BigDecimal.ZERO),
                    target.refunds()
            );
            if (row >= 0 && row < displayedInvoices.size()) {
                displayedInvoices.set(row, updated);
            }
            for (int i = 0; i < currentInvoices.size(); i++) {
                if (currentInvoices.get(i).id() == updated.id()) {
                    currentInvoices.set(i, updated);
                    break;
                }
            }
            if (row >= 0 && row < invoiceTableModel.getRowCount()) {
                invoiceTableModel.setValueAt(currencyFormat.format(nextPaid), row, 9);
                invoiceTableModel.setValueAt(currencyFormat.format(nextRemaining), row, 10);
                invoiceTableModel.setValueAt(formatStatus(nextStatus), row, 11);
            }
        } else if (row >= 0 && row < invoiceTableModel.getRowCount()) {
            invoiceTableModel.setValueAt(invoiceTableModel.getValueAt(row, 8), row, 9);
            invoiceTableModel.setValueAt("0 VNĐ", row, 10);
            invoiceTableModel.setValueAt("Đã thanh toán", row, 11);
        }
    }

    public JButton getBtnRefund() { return btnRefund; }

    /** Chi hoàn tiền thừa cho bệnh nhân qua `RecordHoanTraHandler` và tạo chứng từ `PhieuHoanTra`. */
    public void handleIssueRefund() {
        if (paymentPending) return;
        if (services == null || services.recordRefund() == null) {
            showMessage("Chức năng chi hoàn tiền chưa được cấu hình.", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int row = invoiceTable.getSelectedRow();
        if (row < 0) {
            showMessage("Vui lòng chọn một hóa đơn để chi hoàn tiền!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        HoaDonDuLieu currentInvoice = row < displayedInvoices.size() ? displayedInvoices.get(row) : null;
        if (currentInvoice == null || currentInvoice.id() <= 0) return;
        BigDecimal pendingRefund = safeMoney(currentInvoice.pendingRefundAmount());
        if (pendingRefund.signum() <= 0) {
            showMessage("Hóa đơn này không có khoản tiền dư cần hoàn trả.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        PhanQuyenNguCanh authzContext = currentAuthorizationContext();
        KiemToanTacNhanNguCanh auditContext = currentAuditActorContext();
        if (authzContext == null) {
            showMessage("Phiên làm việc không hợp lệ hoặc đã hết hạn.", "Lỗi Xác Thực", JOptionPane.ERROR_MESSAGE);
            return;
        }

        BigDecimal amountToRefund = pendingRefund;
        if (paymentAmount != null && !paymentAmount.getText().trim().isEmpty()) {
            try {
                BigDecimal typed = new BigDecimal(paymentAmount.getText().trim());
                if (typed.signum() <= 0 || typed.compareTo(pendingRefund) > 0 || typed.stripTrailingZeros().scale() > 2) {
                    throw new NumberFormatException();
                }
                amountToRefund = typed;
            } catch (NumberFormatException ex) {
                showMessage("Số tiền hoàn không hợp lệ hoặc vượt quá số tiền cần hoàn.", "Kiểm tra số tiền", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }
        String reason = paymentNotes != null && !paymentNotes.getText().trim().isEmpty()
                ? paymentNotes.getText().trim()
                : "Hoàn tiền chênh lệch đối tượng/dịch vụ";
        PhuongThucThanhToan method = paymentMethod != null && paymentMethod.getSelectedItem() instanceof PhuongThucThanhToan m
                ? m
                : PhuongThucThanhToan.TIEN_MAT;

        RecordHoanTraCommand refundCommand = new RecordHoanTraCommand(
                currentInvoice.id(), amountToRefund, method, reason, authzContext, auditContext);
        final long refundEpoch = viewSessionEpoch.get();
        paymentPending = true;
        if (btnCollect != null) btnCollect.setEnabled(false);
        if (btnRefund != null) btnRefund.setEnabled(false);

        SwingWorker<KetQua<RecordHoanTraBienNhan>, Void> worker = new SwingWorker<>() {
            @Override
            protected KetQua<RecordHoanTraBienNhan> doInBackground() {
                return services.recordRefund().handle(refundCommand);
            }

            @Override
            protected void done() {
                try {
                    if (isCancelled()) return;
                    KetQua<RecordHoanTraBienNhan> result = get();
                    if (result != null && result.isSuccess()) {
                        if (refundEpoch != viewSessionEpoch.get()) return;
                        RecordHoanTraBienNhan receipt = result.getValue().orElse(null);
                        applyRefundToDisplayedInvoice(row, currentInvoice, receipt, refundCommand);
                        loadSelectedInvoiceDetails();
                        updateSummaryCounters();
                        showMessage("Đã chi hoàn trả " + currencyFormat.format(refundCommand.amount())
                                + " cho hóa đơn: " + currentInvoice.invoiceNumber(), "Hoàn Tiền Thành Công", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        if (refundEpoch != viewSessionEpoch.get()) return;
                        String errorMsg = result != null && result.getError().isPresent()
                                ? result.getError().get().message()
                                : "Chi hoàn tiền thất bại.";
                        showMessage(errorMsg, "Lỗi Hoàn Tiền", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (InterruptedException ex) {
                    MayTinhTacVuNenChanDoanLoi.restoreInterruptStatus();
                } catch (CancellationException ex) {
                    // Normal cancellation
                } catch (ExecutionException ex) {
                    showMessage("Lỗi hệ thống khi hoàn tiền: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                } finally {
                    paymentPending = false;
                    inFlightWorkers.remove(this);
                    newPayment();
                }
            }
        };
        inFlightWorkers.add(worker);
        worker.execute();
    }

    private void applyRefundToDisplayedInvoice(int selectedRow, HoaDonDuLieu fallbackInvoice,
                                               RecordHoanTraBienNhan receipt, RecordHoanTraCommand command) {
        int row = selectedRow;
        if (fallbackInvoice != null && (row < 0 || row >= displayedInvoices.size()
                || displayedInvoices.get(row).id() != fallbackInvoice.id())) {
            row = -1;
            for (int i = 0; i < displayedInvoices.size(); i++) {
                if (displayedInvoices.get(i).id() == fallbackInvoice.id()) { row = i; break; }
            }
        }
        HoaDonDuLieu target = row >= 0 ? displayedInvoices.get(row) : fallbackInvoice;
        if (target == null) return;

        BigDecimal nextPaid = receipt != null && receipt.netPaidAmount() != null
                ? receipt.netPaidAmount()
                : safeMoney(target.amountPaid()).subtract(safeMoney(command.amount())).max(BigDecimal.ZERO);
        BigDecimal nextRefunded = receipt != null && receipt.totalRefundedAmount() != null
                ? receipt.totalRefundedAmount()
                : safeMoney(target.refundAmount()).add(safeMoney(command.amount()));
        BigDecimal nextPending = receipt != null && receipt.pendingRefundAmount() != null
                ? receipt.pendingRefundAmount()
                : nextPaid.subtract(safeMoney(target.patientPaidAmount())).max(BigDecimal.ZERO);
        BigDecimal nextRemaining = receipt != null && receipt.remainingAmount() != null
                ? receipt.remainingAmount()
                : safeMoney(target.patientPaidAmount()).subtract(nextPaid).max(BigDecimal.ZERO);
        TrangThaiHoaDon nextStatus = receipt != null && receipt.invoiceStatus() != null
                ? receipt.invoiceStatus()
                : target.status();

        List<BienNhanHoanTraDuLieu> nextRefunds = new ArrayList<>(target.refunds() != null ? target.refunds() : List.of());
        if (receipt != null && receipt.refundId() > 0
                && nextRefunds.stream().noneMatch(item -> item.id() == receipt.refundId())) {
            nextRefunds.add(new BienNhanHoanTraDuLieu(
                    receipt.refundId(),
                    target.id(),
                    safeMoney(receipt.refundedAmount()),
                    receipt.refundTime(),
                    command.paymentMethod(),
                    command.authorizationContext().employeeId(),
                    command.reason()
            ));
        }
        HoaDonDuLieu updated = new HoaDonDuLieu(
                target.id(), target.invoiceNumber(), target.visitId(), target.patientId(), target.facilityId(),
                target.invoiceDate(), target.totalAmount(), target.insurancePaidAmount(),
                target.advanceAmount(), target.discountAmount(), target.discountPercentage(),
                target.discountReason(), target.patientPaidAmount(), nextPaid, nextRemaining,
                nextStatus, target.notes(), target.lines(), target.receipts(),
                target.serviceDiscountAmount(), nextRefunded, nextPending, List.copyOf(nextRefunds)
        );
        if (row >= 0 && row < displayedInvoices.size()) {
            displayedInvoices.set(row, updated);
        }
        for (int i = 0; i < currentInvoices.size(); i++) {
            if (currentInvoices.get(i).id() == updated.id()) {
                currentInvoices.set(i, updated);
                break;
            }
        }
        if (row >= 0 && row < invoiceTableModel.getRowCount()) {
            invoiceTableModel.setValueAt(currencyFormat.format(nextPaid), row, 9);
            invoiceTableModel.setValueAt(currencyFormat.format(nextRemaining), row, 10);
            invoiceTableModel.setValueAt(formatStatus(nextStatus), row, 11);
        }
    }

    protected BigDecimal requestPaymentAmount(BigDecimal remaining) {
        if (paymentAmount != null) {
            try {
                BigDecimal amount=new BigDecimal(paymentAmount.getText().trim());
                if(amount.signum()<=0||amount.compareTo(remaining)>0||amount.stripTrailingZeros().scale()>2)throw new NumberFormatException();
                return amount;
            }catch(NumberFormatException ex){showMessage("Số tiền không hợp lệ hoặc vượt quá số còn nợ.","Kiểm tra số tiền",JOptionPane.WARNING_MESSAGE);return null;}
        }
        if (!isModalDialogAllowed()) return remaining;
        String input = JOptionPane.showInputDialog(this,
                "Số tiền thu lần này (VNĐ), tối đa " + currencyFormat.format(remaining)
                        + "\nNhập số, không dùng dấu phân cách hàng nghìn:", remaining.toPlainString());
        if (input == null) return null;
        try {
            BigDecimal amount = new BigDecimal(input.trim());
            if (amount.signum() <= 0 || amount.compareTo(remaining) > 0
                    || amount.stripTrailingZeros().scale() > 2) throw new NumberFormatException();
            int confirmed = JOptionPane.showConfirmDialog(this,
                    "Xác nhận thu " + currencyFormat.format(amount) + "?", "Thu viện phí", JOptionPane.YES_NO_OPTION);
            return confirmed == JOptionPane.YES_OPTION ? amount : null;
        } catch (NumberFormatException ex) {
            showMessage("Số tiền không hợp lệ hoặc vượt quá số còn nợ.", "Kiểm tra số tiền", JOptionPane.WARNING_MESSAGE);
            return null;
        }
    }

    private void showReceiptHistory() {
        if (GraphicsEnvironment.isHeadless()) return;
        DanhSachPhieuThuHopThoai panel = createReceiptListPanel();
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "EHC - Danh Sách Phiếu Thu", Dialog.ModalityType.MODELESS);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setContentPane(panel);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private static BigDecimal safeMoney(BigDecimal val) {
        return val != null ? val : BigDecimal.ZERO;
    }

    private void handleOpenBillingSlip() {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }
        var invoice=selectedInvoice();if(invoice==null)return;
        if (hasDrugLines(invoice)) {
            openDrugPaymentBill(invoice);
            return;
        }
        var info=visitDetails.get(invoice.visitId());
        Window owner = SwingUtilities.getWindowAncestor(this);
        new SwingWorker<vn.clinicmanagement.desktop.modules.print.billing.YTeVienPhiPhieuManHinh.BillingSlipData, Void>() {
            @Override protected vn.clinicmanagement.desktop.modules.print.billing.YTeVienPhiPhieuManHinh.BillingSlipData doInBackground() {
                return vn.clinicmanagement.desktop.modules.print.billing.YTeVienPhiPhieuManHinh.fromInvoice(invoice, info);
            }
            @Override protected void done() {
                try {
                    var data = get();
                    if (data != null) {
                        new vn.clinicmanagement.desktop.modules.print.billing.YTeVienPhiPhieuHopThoai(owner, data).setVisible(true);
                        return;
                    }
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    private static boolean hasDrugLines(HoaDonDuLieu invoice) {
        if (invoice == null || invoice.lines() == null) return false;
        return invoice.lines().stream().anyMatch(l ->
                vn.clinicmanagement.application.billing.DonThuocVienPhiDongBo.RX_LINE.equalsIgnoreCase(l.itemType())
                        || "THUOC".equalsIgnoreCase(l.itemType()));
    }

    private void openDrugPaymentBill(HoaDonDuLieu invoice) {
        if (invoice == null || invoice.lines() == null) return;
        var info = visitDetails.get(invoice.visitId());
        BigDecimal invTotal = safeMoney(invoice.totalAmount());
        BigDecimal invIns = safeMoney(invoice.insurancePaidAmount());
        boolean hasIns = invIns.signum() > 0 && invTotal.signum() > 0;

        List<PhieuThanhToanThuocInManHinh.DrugBillLine> billLines = new ArrayList<>();
        BigDecimal sumTotal = BigDecimal.ZERO;
        BigDecimal sumIns = BigDecimal.ZERO;
        BigDecimal sumPatient = BigDecimal.ZERO;
        for (HoaDonDongDuLieu line : invoice.lines()) {
            if (!vn.clinicmanagement.application.billing.DonThuocVienPhiDongBo.RX_LINE.equalsIgnoreCase(line.itemType())
                    && !"THUOC".equalsIgnoreCase(line.itemType())) {
                continue;
            }
            String rawDesc = Objects.toString(line.description(), "").trim();
            String drugName = rawDesc;
            String unit = "Viên";
            int lp = rawDesc.lastIndexOf('(');
            int rp = rawDesc.lastIndexOf(')');
            if (lp > 0 && rp == rawDesc.length() - 1 && rp > lp + 1) {
                drugName = rawDesc.substring(0, lp).trim();
                unit = rawDesc.substring(lp + 1, rp).trim();
            }
            BigDecimal lineTotal = safeMoney(line.lineTotal());
            BigDecimal insPart = hasIns
                    ? lineTotal.multiply(invIns).divide(invTotal, 0, java.math.RoundingMode.HALF_UP).min(lineTotal)
                    : BigDecimal.ZERO;
            BigDecimal patPart = lineTotal.subtract(insPart).max(BigDecimal.ZERO);
            billLines.add(new PhieuThanhToanThuocInManHinh.DrugBillLine(
                    drugName, unit, safeMoney(line.quantity()), safeMoney(line.unitPrice()),
                    lineTotal, insPart, patPart));
            sumTotal = sumTotal.add(lineTotal);
            sumIns = sumIns.add(insPart);
            sumPatient = sumPatient.add(patPart);
        }
        if (billLines.isEmpty()) return;

        String pid = info != null && info.patientCode() != null && !info.patientCode().isBlank()
                ? info.patientCode() : String.valueOf(invoice.patientId());
        String pName = info != null && info.patientName() != null && !info.patientName().isBlank()
                ? info.patientName() : patientNamesByVisitId.getOrDefault(invoice.visitId(), "Bệnh nhân #" + invoice.patientId());
        String birthYear = info != null && info.birthDate() != null ? String.valueOf(info.birthDate().getYear()) : "";
        String gender = info == null ? "" : ("M".equalsIgnoreCase(info.gender()) ? "Nam" : "F".equalsIgnoreCase(info.gender()) ? "Nữ" : Objects.toString(info.gender(), ""));
        String address = info != null ? Objects.toString(info.address(), "") : "";
        String subject = info != null && "BHYT".equalsIgnoreCase(info.subject()) ? "Bảo hiểm" : "Viện phí";
        String bhytNo = info != null ? Objects.toString(info.bhytNumber(), "") : "";
        LocalDate bhytExp = info != null ? info.bhytExpiry() : null;
        String deptRoom = info != null
                ? (Objects.toString(info.room(), "").isBlank() ? Objects.toString(info.department(), "") : info.room())
                : "";
        String diagnosis = info != null ? Objects.toString(info.diagnosis(), "") : "";
        String facility = info != null ? Objects.toString(info.facilityName(), "") : "";

        var billData = new PhieuThanhToanThuocInManHinh.DrugBillData(
                facility, pid, pName, birthYear, gender, address, subject,
                bhytNo, bhytExp, deptRoom, diagnosis, java.time.LocalDateTime.now(),
                billLines, sumTotal, sumIns, sumPatient);
        PhieuThanhToanThuocInManHinh.showPreviewDialog(this, billData);
    }

    private void loadSelectedInvoiceDetails() {
        detailTableModel.setRowCount(0);
        updateSummaryCounters();
        int row = invoiceTable.getSelectedRow();
        if (row < 0 || row >= displayedInvoices.size()) {
            if(receiptList!=null){fillingReceipt=true;receiptListModel.clear();selectedReceipts=List.of();selectedRefunds=List.of();fillingReceipt=false;newPayment();}
            for(var label:infoValues.values())label.setText("");if(patientHeading!=null)patientHeading.setText("");
            return;
        }

        HoaDonDuLieu invoice = displayedInvoices.get(row);
        if (invoice == null) return;
        fillingReceipt=true;receiptListModel.clear();
        selectedReceipts=invoice.receipts()==null?List.of():invoice.receipts();
        selectedRefunds=invoice.refunds()==null?List.of():invoice.refunds();
        for(var receipt:selectedReceipts)receiptListModel.addElement("<html>"+(receipt.paymentTime()==null?"":receipt.paymentTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))+"<br>(+) "+currencyFormat.format(receipt.amount())+"</html>");
        for(var refund:selectedRefunds)receiptListModel.addElement("<html><font color='#c00000'>"+(refund.refundTime()==null?"":refund.refundTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))+"<br>(-) "+currencyFormat.format(refund.amount())+"</font></html>");
        fillingReceipt=false;newPayment();
        var info=visitDetails.get(invoice.visitId());
        if(info!=null){
            setInfo("Khoa phòng:",info.department()+" > "+Objects.toString(info.room(),""));setInfo("Đối tượng:","BHYT".equals(info.subject())?"BHYT":"Viện phí");setInfo("Trạng thái:",switch(Objects.toString(info.visitStatus(),"")){case "DANG_KHAM"->"Đang khám bệnh";case "CHO_KHAM"->"Chờ khám";case "CHO_CLS"->"Chờ CLS";case "HOAN_THANH"->"Hoàn thành";case "DA_HUY"->"Đã hủy";default->Objects.toString(info.visitStatus(),"");});
            setInfo("Ngày vào viện:",info.admittedAt()==null?"":info.admittedAt().format(dateFormatter));setInfo("Ngày ra viện:",info.dischargedAt()==null?"":info.dischargedAt().format(dateFormatter));
            setInfo("Số ngày điều trị:",info.admittedAt()==null?"":String.valueOf(Math.max(1,java.time.temporal.ChronoUnit.DAYS.between(info.admittedAt().toLocalDate(),(info.dischargedAt()==null?java.time.LocalDateTime.now():info.dischargedAt()).toLocalDate())+1)));
            patientHeading.setText("<html>"+info.patientName()+" | "+info.patientCode()+" | "+ageFor(invoice.visitId())+" tuổi | "+("M".equals(info.gender())?"Nam":"F".equals(info.gender())?"Nữ":"")+"<br>"+Objects.toString(info.address(),"")+" | "+Objects.toString(info.phone(),"")+"</html>");
        }else{for(String key:infoValues.keySet())if(!key.equals("Số phiếu:")&&!key.equals("Người thu:"))setInfo(key,"");patientHeading.setText("");}

        List<HoaDonDongDuLieu> lines = invoice.lines();
        if (lines != null) {
            int stt = 1;
            for (HoaDonDongDuLieu line : lines) {
                if (line == null) continue;
                String dept = resolveDepartment(line.itemType());
                String uom = resolveUom(line.itemType());
                String qtyStr = line.quantity() != null ? line.quantity().stripTrailingZeros().toPlainString() : "1";
                String priceStr = line.unitPrice() != null ? new DecimalFormat("#,##0.##").format(line.unitPrice()) : "0";
                String totalStr = line.lineTotal() != null ? new DecimalFormat("#,##0.##").format(line.lineTotal()) : "0";
                String rawPayer = line.payerType() != null ? line.payerType().trim().toUpperCase(java.util.Locale.ROOT) : "";
                String lineSubject = switch (rawPayer) {
                    case "BHYT" -> "BHYT";
                    case "BHYT_DICH_VU" -> "BHYT + DV";
                    case "MIEN_PHI" -> "Miễn phí";
                    case "THU_PHI" -> "Thu phí";
                    default -> "";
                };
                String rateStr = "MIEN_PHI".equals(rawPayer) ? "0%"
                        : (line.coverageRate() > 0 ? (100 - line.coverageRate()) + "%" : "100%");
                BigDecimal bnPay = line.patientAmount() != null ? line.patientAmount() : line.lineTotal();
                String bnPayStr = !rawPayer.isEmpty() && bnPay != null
                        ? new DecimalFormat("#,##0.##").format(bnPay)
                        : (invoice.insurancePaidAmount().signum() == 0 && invoice.discountAmount().signum() == 0 && invoice.advanceAmount().signum() == 0 ? totalStr : "—");

                detailTableModel.addRow(new Object[]{
                        line.description() != null ? line.description() : "", uom, lineSubject, qtyStr,
                        priceStr, rateStr, totalStr, "", "", bnPayStr, "—", "—"
                });
            }
        }

        updateDetailRowHeights();
        // Tải thông tin công nợ của bệnh nhân qua SwingWorker
        if (invoice.patientId() > 0) {
            loadPatientDebt(invoice.patientId());
        }
    }

    private String resolveDepartment(String itemType) {
        if (itemType == null) return "Dịch vụ";
        return switch (itemType.toUpperCase()) {
            case "DON_THUOC", "THUOC", "DUOC_PHAM" -> "Thuốc";
            case "DICH_VU_CLS", "CAN_LAM_SANG", "XET_NGHIEM" -> "Cận lâm sàng";
            default -> "Khám bệnh";
        };
    }

    private String resolveUom(String itemType) {
        if (itemType == null) return "Lượt";
        return switch (itemType.toUpperCase()) {
            case "DON_THUOC", "THUOC", "DUOC_PHAM" -> "Viên";
            case "DICH_VU_CLS", "CAN_LAM_SANG", "XET_NGHIEM" -> "Lần";
            default -> "Lượt";
        };
    }

    public void loadPatientDebt(int patientId) {
        if (services == null || services.patientDebt() == null || patientId <= 0) return;
        PhanQuyenNguCanh auth = currentAuthorizationContext();
        KiemToanTacNhanNguCanh audit = currentAuditActorContext();
        if (auth == null) return;

        SwingWorker<KetQua<List<BenhNhanCongNoDuLieu>>, Void> worker = new SwingWorker<>() {
            @Override
            protected KetQua<List<BenhNhanCongNoDuLieu>> doInBackground() {
                GetBenhNhanCongNoQuery query = new GetBenhNhanCongNoQuery(patientId, auth, audit);
                return services.patientDebt().handle(query);
            }

            @Override
            protected void done() {
                try {
                    if (isCancelled()) return;
                    KetQua<List<BenhNhanCongNoDuLieu>> result = get();
                    if (result != null && result.isSuccess()) {
                        latestPatientDebts = result.getValue().orElse(List.of());
                    }
                } catch (InterruptedException ex) {
                    MayTinhTacVuNenChanDoanLoi.restoreInterruptStatus();
                } catch (CancellationException ignored) {
                } catch (ExecutionException ignored) {
                } finally {
                    inFlightWorkers.remove(this);
                }
            }
        };

        inFlightWorkers.add(worker);
        worker.execute();
    }

    private void updateSummaryCounters() {
        int row = invoiceTable != null ? invoiceTable.getSelectedRow() : -1;
        HoaDonDuLieu invoice = row >= 0 && row < displayedInvoices.size() ? displayedInvoices.get(row) : null;
        lblTotalAmount.setText(formatDesignMoney(invoice == null ? null : invoice.totalAmount()));
        lblPaidAmount.setText(formatDesignMoney(invoice == null ? null : invoice.amountPaid()));
        lblUnpaidAmount.setText(formatDesignMoney(invoice == null ? null : invoice.remainingAmount()));
        lblInsuranceAmount.setText(formatDesignMoney(invoice == null ? null : invoice.insurancePaidAmount()));
        lblAdvanceAmount.setText(formatDesignMoney(invoice == null ? null : invoice.advanceAmount()));
        BigDecimal totalDiscount = invoice == null
                ? BigDecimal.ZERO
                : safeMoney(invoice.discountAmount()).add(safeMoney(invoice.serviceDiscountAmount()));
        lblDiscountAmount.setText(formatDesignMoney(totalDiscount));
        lblPatientAmount.setText(formatDesignMoney(invoice == null ? null : invoice.patientPaidAmount()));
        if (lblRefundSummary != null) {
            BigDecimal pendingRefund = invoice == null ? BigDecimal.ZERO : safeMoney(invoice.pendingRefundAmount());
            BigDecimal refunded = invoice == null ? BigDecimal.ZERO : safeMoney(invoice.refundAmount());
            if (pendingRefund.signum() > 0) {
                lblRefundSummary.setText("Cần hoàn: " + formatDesignMoney(pendingRefund));
            } else {
                lblRefundSummary.setText(formatDesignMoney(refunded));
            }
        }
        long insured = displayedInvoices.stream().filter(i -> i.insurancePaidAmount() != null && i.insurancePaidAmount().signum() > 0).count();
        if (lblRange != null) {
            String fromText = filterFromDate.format(SHORT_DATE_FMT);
            String toText = filterToDate.format(SHORT_DATE_FMT);
            lblRange.setText(fromText + " >> " + toText + " - BH(" + insured + ") VP(" + (displayedInvoices.size()-insured) + ")");
        }
    }

    private String formatDesignMoney(BigDecimal amount) {
        return new DecimalFormat("#,##0'đ'").format(amount == null ? BigDecimal.ZERO : amount);
    }

    private void onSearchTriggered() {
        String keyword = txtSearch != null ? txtSearch.getText().trim() : "";
        if (!keyword.isEmpty()) {
            try {
                int visitOrPatientId = Integer.parseInt(keyword);
                loadInvoiceByVisit(visitOrPatientId);
                return;
            } catch (NumberFormatException ignored) {
            }
        }
        applyFilter();
    }

    private void applyFilter() {
        String selectedFilter = cboFilterStatus != null ? (String) cboFilterStatus.getSelectedItem() : "Tất cả hóa đơn";
        boolean showInsured = insuredFilter == null || insuredFilter.isSelected();
        boolean showFee = feeFilter == null || feeFilter.isSelected();
        boolean showOutpatient = outpatientFilter == null || outpatientFilter.isSelected();
        boolean showInpatient = inpatientFilter == null || inpatientFilter.isSelected();
        String selectedRecord = (recordFilter != null && recordFilter.getSelectedIndex() > 0)
                ? (String) recordFilter.getSelectedItem() : null;

        List<HoaDonDuLieu> filtered = new ArrayList<>();
        for (HoaDonDuLieu inv : currentInvoices) {
            if (inv == null) continue;
            String statusText = formatStatus(inv.status());
            if ("Chưa thanh toán".equals(selectedFilter)
                    && !("Chưa thanh toán".equals(statusText) || "Thanh toán một phần".equals(statusText))) {
                continue;
            } else if ("Đã thanh toán".equals(selectedFilter) && !"Đã thanh toán".equals(statusText)) {
                continue;
            } else if ("Miễn giảm / BHYT".equals(selectedFilter)
                    && (inv.insurancePaidAmount() == null || inv.insurancePaidAmount().compareTo(BigDecimal.ZERO) <= 0)) {
                continue;
            }

            var info = visitDetails.get(inv.visitId());
            boolean isInsured = (inv.insurancePaidAmount() != null && inv.insurancePaidAmount().compareTo(BigDecimal.ZERO) > 0)
                    || (info != null && "BHYT".equalsIgnoreCase(info.subject()));
            if ((isInsured && !showInsured) || (!isInsured && !showFee)) {
                continue;
            }

            if (info != null && info.treatment() != null && !info.treatment().isBlank()) {
                boolean isInpatient = "NOI_TRU".equalsIgnoreCase(info.treatment().trim());
                if ((isInpatient && !showInpatient) || (!isInpatient && !showOutpatient)) {
                    continue;
                }
            }

            if (selectedRecord != null && (info == null || !selectedRecord.equals(info.registrationType()))) {
                continue;
            }
            filtered.add(inv);
        }
        renderInvoices(filtered);
    }

    private void renderInvoices(List<HoaDonDuLieu> invoices) {
        var previous = selectedInvoice();
        invoiceTable.clearSelection();
        displayedInvoices.clear();
        if (invoices != null) for (HoaDonDuLieu invoice : invoices) if (invoice != null) {
            String keyword=txtSearch==null?"":txtSearch.getText().trim().toLowerCase(java.util.Locale.ROOT);
            String name=patientNamesByVisitId.getOrDefault(invoice.visitId(),"");
            var info=visitDetails.get(invoice.visitId());
            if(info!=null&&dateFilter!=null&&dateFilter.getSelectedIndex()==1&&
                    (info.dischargedAt()==null||!info.dischargedAt().toLocalDate().equals(LocalDate.now())))continue;
            if(insuredFilter!=null){
                boolean insured=info!=null ? "BHYT".equals(info.subject()) : invoice.insurancePaidAmount().signum()>0;
                if(insured&&!insuredFilter.isSelected()||!insured&&!feeFilter.isSelected())continue;
                if(info!=null){
                    if("NOI_TRU".equals(info.treatment())&&!inpatientFilter.isSelected()||!"NOI_TRU".equals(info.treatment())&&!outpatientFilter.isSelected())continue;
                    if(recordFilter.getSelectedIndex()>0&&!Objects.equals(recordFilter.getSelectedItem(),info.registrationType()))continue;
                }
            }
            if(keyword.isEmpty()||(name+" "+invoice.patientId()+" "+invoice.visitId()+" "+(info==null?"":info.patientCode())).toLowerCase(java.util.Locale.ROOT).contains(keyword))displayedInvoices.add(invoice);
        }
        invoiceTableModel.setRowCount(0);
        detailTableModel.setRowCount(0);

        if (invoices == null || invoices.isEmpty()) {
            updateSummaryCounters();
            return;
        }

        int stt = 1;
        for (HoaDonDuLieu inv : List.copyOf(displayedInvoices)) {
            if (inv == null) continue;
            String dateStr = inv.invoiceDate() != null ? inv.invoiceDate().format(dateFormatter) : "";
            String subject = (inv.insurancePaidAmount() != null && inv.insurancePaidAmount().compareTo(BigDecimal.ZERO) > 0)
                    ? "BHYT" : "Viện Phí";
            String totalStr = inv.totalAmount() != null ? currencyFormat.format(inv.totalAmount()) : "0 VNĐ";
            String bhytStr = inv.insurancePaidAmount() != null ? currencyFormat.format(inv.insurancePaidAmount()) : "0 VNĐ";
            String copayStr = inv.patientPaidAmount() != null ? currencyFormat.format(inv.patientPaidAmount()) : "0 VNĐ";
            String paidStr = inv.amountPaid() != null ? currencyFormat.format(inv.amountPaid()) : "0 VNĐ";
            String debtStr = inv.remainingAmount() != null ? currencyFormat.format(inv.remainingAmount()) : "0 VNĐ";
            String statusStr = formatStatus(inv.status());

            String patientDisplay = patientNamesByVisitId.getOrDefault(
                    inv.visitId(), "Bệnh nhân " + inv.patientId());
            invoiceTableModel.addRow(new Object[]{
                    String.valueOf(stt++),
                    inv.invoiceNumber() != null ? inv.invoiceNumber() : "",
                    String.valueOf(inv.patientId()),
                    patientDisplay,
                    dateStr,
                    subject,
                    totalStr,
                    bhytStr,
                    copayStr,
                    paidStr,
                    debtStr,
                    statusStr,
                    ageFor(inv.visitId())
            });
        }

        if (invoiceTableModel.getRowCount() > 0) {
            int selectedRow=0;
            if(previous!=null)for(int i=0;i<displayedInvoices.size();i++)if(displayedInvoices.get(i).id()==previous.id()){selectedRow=i;break;}
            invoiceTable.setRowSelectionInterval(selectedRow, selectedRow);
            loadSelectedInvoiceDetails();
        }

        updateSummaryCounters();
    }

    private String ageFor(int visitId){var info=visitDetails.get(visitId);if(info==null||info.birthDate()==null)return "";var today=LocalDate.now();long months=java.time.temporal.ChronoUnit.MONTHS.between(info.birthDate(),today);return months<24?months+" TH":String.valueOf(java.time.Period.between(info.birthDate(),today).getYears());}

    private String formatStatus(TrangThaiHoaDon status) {
        if (status == null) return "Chưa thanh toán";
        return switch (status) {
            case DA_THANH_TOAN -> "Đã thanh toán";
            case THANH_TOAN_MOT_PHAN -> "Thanh toán một phần";
            case DA_HUY -> "Đã hủy";
            default -> "Chưa thanh toán";
        };
    }

    public void setVisitIds(List<Integer> visitIds) {
        this.activeVisitIds.clear();
        if (visitIds != null) {
            this.activeVisitIds.addAll(visitIds);
        }
        refreshData();
    }

    public void loadInvoiceByVisit(int visitId) {
        setVisitIds(List.of(visitId));
    }

    @Override
    public void onShow() {
        if (billingQueueRepository != null) {
            PhanQuyenNguCanh auth = currentAuthorizationContext();
            if (auth == null) {
                setVisitIds(List.of());
                return;
            }
            cancelInFlightWorkers();
            long epoch = viewSessionEpoch.get();
            LocalDate from = filterFromDate != null ? filterFromDate : LocalDate.now(BUSINESS_ZONE);
            LocalDate to = filterToDate != null ? filterToDate : from;
            boolean facilityWide = "CASHIER".equalsIgnoreCase(auth.roleCode())
                    || "ADMIN".equalsIgnoreCase(auth.roleCode());
            SwingWorker<List<TiepDonHangDoiMatHang>, Void> queueWorker = new SwingWorker<>() {
                @Override
                protected List<TiepDonHangDoiMatHang> doInBackground() {
                    return facilityWide
                            ? billingQueueRepository.getFacilityBillingQueue(from, to, auth.facilityId())
                            : billingQueueRepository.getQueue(from, to, auth.facilityId(), auth.departmentId());
                }

                @Override
                protected void done() {
                    try {
                        if (isCancelled() || epoch != viewSessionEpoch.get()) return;
                        List<TiepDonHangDoiMatHang> rows = get();
                        patientNamesByVisitId.clear();
                        List<Integer> visitIds = new ArrayList<>();
                        if (rows != null) {
                            for (TiepDonHangDoiMatHang item : rows) {
                                if (item == null) continue;
                                visitIds.add(item.visitId());
                                if (item.patientName() != null && !item.patientName().isBlank()) {
                                    patientNamesByVisitId.put(item.visitId(), item.patientName().trim());
                                }
                            }
                        }
                        setVisitIds(visitIds);
                    } catch (InterruptedException ex) {
                        MayTinhTacVuNenChanDoanLoi.restoreInterruptStatus();
                    } catch (CancellationException ignored) {
                    } catch (ExecutionException ex) {
                        setVisitIds(List.of());
                        showMessage("Không thể tải lượt khám trong ngày để lập danh sách viện phí.",
                                "Lỗi tải dữ liệu", JOptionPane.ERROR_MESSAGE);
                    } finally {
                        inFlightWorkers.remove(this);
                    }
                }
            };
            inFlightWorkers.add(queueWorker);
            queueWorker.execute();
            return;
        }
        refreshData();
    }

    @Override
    public void refreshData() {
        cancelInFlightWorkers();

        if (services == null || services.getInvoiceByVisit() == null || activeVisitIds.isEmpty()) {
            currentInvoices.clear();
            displayedInvoices.clear();
            invoiceTableModel.setRowCount(0);
            detailTableModel.setRowCount(0);
            updateSummaryCounters();
            return;
        }

        PhanQuyenNguCanh authzContext = currentAuthorizationContext();
        KiemToanTacNhanNguCanh auditContext = currentAuditActorContext();
        if (authzContext == null) {
            currentInvoices.clear();
            displayedInvoices.clear();
            invoiceTableModel.setRowCount(0);
            detailTableModel.setRowCount(0);
            updateSummaryCounters();
            return;
        }

        long epoch = viewSessionEpoch.get();
        long sequence = refreshSequence.incrementAndGet();
        List<Integer> targetVisits = new ArrayList<>(activeVisitIds);

        SwingWorker<List<HoaDonDuLieu>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<HoaDonDuLieu> doInBackground() {
                List<HoaDonDuLieu> loaded = new ArrayList<>();
                for (int visitId : targetVisits) {
                    if (isCancelled()) return loaded;
                    GetHoaDonTheoLuotKhamQuery query = new GetHoaDonTheoLuotKhamQuery(visitId, authzContext, auditContext);
                    KetQua<HoaDonDuLieu> result = services.getInvoiceByVisit().handle(query);
                    if (result != null && result.isSuccess()) {
                        result.getValue().ifPresent(invoice -> {
                            if (services.screenRead() != null) {
                                services.screenRead().getByVisit(visitId, authzContext.facilityId()).ifPresent(info -> {
                                    visitDetails.put(visitId, info);
                                    if (info.patientName() != null && !info.patientName().isBlank()) {
                                        patientNamesByVisitId.put(visitId, info.patientName().trim());
                                    }
                                });
                            }
                            loaded.add(invoice);
                        });
                    }
                }
                return loaded;
            }

            @Override
            protected void done() {
                try {
                    if (epoch != viewSessionEpoch.get() || sequence != refreshSequence.get() || isCancelled()) {
                        return;
                    }
                    List<HoaDonDuLieu> invoices = get();
                    currentInvoices.clear();
                    if (invoices != null) {
                        currentInvoices.addAll(invoices);
                    }
                    if(recordFilter!=null){
                        fillingFilters=true;Object selected=recordFilter.getSelectedItem();recordFilter.removeAllItems();recordFilter.addItem("(Tất cả bệnh án) - "+currentInvoices.size());
                        currentInvoices.stream().map(i->visitDetails.get(i.visitId())).filter(Objects::nonNull).map(i->i.registrationType()).filter(Objects::nonNull).distinct().forEach(recordFilter::addItem);
                        if(selected!=null)for(int i=1;i<recordFilter.getItemCount();i++)if(selected.equals(recordFilter.getItemAt(i)))recordFilter.setSelectedIndex(i);
                        fillingFilters=false;
                    }
                    applyFilter();
                } catch (InterruptedException ex) {
                    MayTinhTacVuNenChanDoanLoi.restoreInterruptStatus();
                } catch (CancellationException ex) {
                    // Normal worker cancellation
                } catch (ExecutionException ex) {
                    currentInvoices.clear();
                    displayedInvoices.clear();
                    invoiceTableModel.setRowCount(0);
                    detailTableModel.setRowCount(0);
                    loadSelectedInvoiceDetails();
                    showMessage("Không thể tải viện phí từ CSDL. Vui lòng thử Làm Mới.","Lỗi tải dữ liệu",JOptionPane.ERROR_MESSAGE);
                } finally {
                    inFlightWorkers.remove(this);
                }
            }
        };

        inFlightWorkers.add(worker);
        worker.execute();
    }

    public void cancelInFlightWorkers() {
        for (SwingWorker<?, ?> worker : inFlightWorkers) {
            worker.cancel(true);
        }
        inFlightWorkers.clear();
    }

    public int getInFlightWorkersCount() {
        return inFlightWorkers.size() + (paymentPending ? 1 : 0);
    }

    public VienPhiGhepNoiGoc.Services getServices() {
        return services;
    }

    public CoKieuBang getInvoiceTable() {
        return invoiceTable;
    }

    public CoKieuBang getDetailTable() {
        return detailTable;
    }

    public DefaultTableModel getInvoiceTableModel() {
        return invoiceTableModel;
    }

    public DefaultTableModel getDetailTableModel() {
        return detailTableModel;
    }

    public JSplitPane getCenterSplitPane() {
        return centerSplitPane;
    }

    public JButton getBtnCollect() {
        return btnCollect;
    }

    public JButton getBtnPrintSlip() {
        return btnPrintSlip;
    }

    public JButton getBtnRefresh() {
        return btnRefresh;
    }

    public JPanel getToolbar() {
        Component north = ((BorderLayout) getLayout()).getLayoutComponent(BorderLayout.NORTH);
        return north instanceof JPanel p ? p : null;
    }

    public JTextField getTxtSearch() {
        return txtSearch;
    }

    public JComboBox<String> getCboFilterStatus() {
        return cboFilterStatus;
    }

    public JLabel getLblTotalAmount() {
        return lblTotalAmount;
    }

    public JLabel getLblPaidAmount() {
        return lblPaidAmount;
    }

    public JLabel getLblUnpaidAmount() {
        return lblUnpaidAmount;
    }

    public List<HoaDonDuLieu> getCurrentInvoices() {
        return currentInvoices;
    }

    public List<HoaDonDuLieu> getDisplayedInvoices() {
        return List.copyOf(displayedInvoices);
    }

    public List<BenhNhanCongNoDuLieu> getLatestPatientDebts() {
        return latestPatientDebts;
    }

    private BigDecimal parseCurrency(String str) {
        if (str == null) return BigDecimal.ZERO;
        String digits = str.replaceAll("[^0-9]", "");
        return digits.isEmpty() ? BigDecimal.ZERO : new BigDecimal(digits);
    }

    private static boolean isModalDialogAllowed() {
        if (GraphicsEnvironment.isHeadless()) return false;
        return System.getProperty("surefire.test.class.path") == null;
    }

    private void showMessage(String message, String title, int type) {
        if (!isModalDialogAllowed()) {
            return;
        }
        JOptionPane.showMessageDialog(this, message, title, type);
    }

    private PhanQuyenNguCanh currentAuthorizationContext() {
        if (sessionLifecycleService != null) {
            return sessionLifecycleService.currentAuthorizationContext().orElse(null);
        }
        return vn.clinicmanagement.desktop.modules.examination.KhamBenhPhienNguCanh.current();
    }

    private KiemToanTacNhanNguCanh currentAuditActorContext() {
        if (sessionLifecycleService != null) {
            return sessionLifecycleService.currentAuditActorContext().orElse(null);
        }
        PhanQuyenNguCanh context = currentAuthorizationContext();
        if (context == null) return null;
        String workstation = NguoiDungPhien.getInstance().getWorkstationId();
        return new KiemToanTacNhanNguCanh(context.employeeId(), context.roleId(), context.roleCode(),
                context.facilityId(), context.departmentId(),
                workstation != null && !workstation.isBlank() ? workstation : "DESKTOP", UUID.randomUUID());
    }

    private void handleMenuSelection(VienPhiMenu.MucMenu item) {
        switch (item) {
            case QUAN_LY_VIEN_PHI -> onShow();
            case CHOT_SO_LIEU_PHIEU_THU -> showReceiptClosingSummary();
            case TUY_CHON -> showBillingOptionsDialog();
            case DANH_SACH_PHIEU_THU -> showReceiptHistory();
            case DANH_SACH_SO_THU_TIEN -> showReceiptBooksDialog(false);
            case CHON_SO_THU_TIEN -> showReceiptBooksDialog(true);
            case LOC_KHOA_PHONG -> showDepartmentFilterDialog();
            case MAN_HINH_CHO -> showWaitingScreenDialog();
            case CAU_HINH_PHAT_LOA -> showSpeakerConfigDialog();
            case HOA_DON_DIEN_TU -> showEInvoiceDialog();
        }
    }

    private void showReceiptClosingSummary() {
        if (GraphicsEnvironment.isHeadless()) return;
        BigDecimal totalAmount = BigDecimal.ZERO, totalBhyt = BigDecimal.ZERO, totalPaid = BigDecimal.ZERO, totalDebt = BigDecimal.ZERO;
        int paidCount = 0;
        for (HoaDonDuLieu inv : currentInvoices) {
            totalAmount = totalAmount.add(safeMoney(inv.totalAmount()));
            totalBhyt = totalBhyt.add(safeMoney(inv.insurancePaidAmount()));
            totalPaid = totalPaid.add(safeMoney(inv.amountPaid()));
            BigDecimal debt = safeMoney(inv.remainingAmount()).max(BigDecimal.ZERO);
            totalDebt = totalDebt.add(debt);
            if (safeMoney(inv.amountPaid()).signum() > 0) paidCount++;
        }
        String msg = String.format(
                "Sổ thu đang chọn: %s%nSố hóa đơn: %d (Đã thu: %d)%nTổng chi phí: %s%nBHYT chi trả: %s%nThực thu: %s%nCòn nợ: %s",
                selectedReceiptBook, currentInvoices.size(), paidCount,
                formatDesignMoney(totalAmount), formatDesignMoney(totalBhyt),
                formatDesignMoney(totalPaid), formatDesignMoney(totalDebt));
        JOptionPane.showMessageDialog(this, msg, "Chốt Số Liệu Phiếu Thu", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showBillingOptionsDialog() {
        if (GraphicsEnvironment.isHeadless()) return;
        JPanel p = new JPanel(new GridLayout(0, 1, 0, 6));
        p.add(new JLabel("Trạng thái lọc hóa đơn:"));
        p.add(cboFilterStatus);
        JButton btnDate = new JButton("Chọn khoảng ngày...");
        btnDate.addActionListener(e -> promptCustomDateRange());
        p.add(btnDate);
        JOptionPane.showMessageDialog(this, p, "Tùy Chọn Viện Phí", JOptionPane.PLAIN_MESSAGE);
    }

    private void showReceiptBooksDialog(boolean selectable) {
        if (GraphicsEnvironment.isHeadless()) return;
        String user = NguoiDungPhien.getInstance().getFullName();
        String[] cols = {"Mã sổ thu", "Tên sổ thu", "Thu ngân", "Trạng thái"};
        Object[][] rows = {
                {"SO_01", selectedReceiptBook, user != null && !user.isBlank() ? user : "EHC", "Đang sử dụng"},
                {"SO_02", "Sổ thu ngoại trú", "Thu ngân 01", "Hoạt động"},
                {"SO_03", "Sổ thu BHYT", "Thu ngân 02", "Hoạt động"}
        };
        JTable tbl = new JTable(rows, cols);
        tbl.setRowHeight(24);
        JScrollPane sp = new JScrollPane(tbl);
        sp.setPreferredSize(new Dimension(520, 180));
        int res = JOptionPane.showConfirmDialog(this, sp,
                selectable ? "Chọn Sổ Thu Tiền" : "Danh Sách Sổ Thu Tiền",
                selectable ? JOptionPane.OK_CANCEL_OPTION : JOptionPane.DEFAULT_OPTION);
        if (selectable && res == JOptionPane.OK_OPTION && tbl.getSelectedRow() >= 0) {
            selectedReceiptBook = Objects.toString(tbl.getValueAt(tbl.getSelectedRow(), 1), selectedReceiptBook);
        }
    }

    private void showDepartmentFilterDialog() {
        if (GraphicsEnvironment.isHeadless()) return;
        java.util.Set<String> depts = new java.util.LinkedHashSet<>();
        depts.add("");
        for (var info : visitDetails.values()) {
            if (info.department() != null && !info.department().isBlank()) depts.add(info.department());
        }
        JComboBox<String> cbo = new JComboBox<>(depts.toArray(new String[0]));
        cbo.setSelectedItem(selectedDepartmentFilter);
        int res = JOptionPane.showConfirmDialog(this, cbo, "Lọc Khoa Phòng", JOptionPane.OK_CANCEL_OPTION);
        if (res == JOptionPane.OK_OPTION) {
            selectedDepartmentFilter = Objects.toString(cbo.getSelectedItem(), "");
            txtSearch.setText(selectedDepartmentFilter);
            applyFilter();
        }
    }

    private void showWaitingScreenDialog() {
        if (GraphicsEnvironment.isHeadless()) return;
        var model = new DefaultTableModel(new String[]{"STT", "Mã HĐ", "Bệnh nhân", "Tổng tiền", "Còn nợ"}, 0);
        int stt = 1;
        for (HoaDonDuLieu inv : currentInvoices) {
            BigDecimal debt = safeMoney(inv.remainingAmount()).max(BigDecimal.ZERO);
            if (debt.signum() > 0) {
                String patient = patientNamesByVisitId.getOrDefault(inv.visitId(), "Lượt khám #" + inv.visitId());
                model.addRow(new Object[]{stt++, inv.invoiceNumber(), patient,
                        formatDesignMoney(safeMoney(inv.totalAmount())), formatDesignMoney(debt)});
            }
        }
        JTable tbl = new JTable(model);
        tbl.setRowHeight(24);
        JScrollPane sp = new JScrollPane(tbl);
        sp.setPreferredSize(new Dimension(620, 320));
        JOptionPane.showMessageDialog(this, sp, "Màn Hình Chờ Thanh Toán", JOptionPane.PLAIN_MESSAGE);
    }

    private void showSpeakerConfigDialog() {
        if (GraphicsEnvironment.isHeadless()) return;
        JPanel p = new JPanel(new GridLayout(0, 1, 0, 6));
        p.add(new JCheckBox("Bật phát loa gọi bệnh nhân thanh toán", true));
        p.add(new JLabel("Mẫu câu gọi: \"Mời bệnh nhân [Tên BN] đến quầy [Quầy thu] thanh toán\""));
        JOptionPane.showMessageDialog(this, p, "Cấu Hình Phát Loa", JOptionPane.PLAIN_MESSAGE);
    }

    private void showEInvoiceDialog() {
        if (GraphicsEnvironment.isHeadless()) return;
        var model = new DefaultTableModel(new String[]{"Số hóa đơn", "Bệnh nhân", "Tổng tiền", "Đã thu", "Trạng thái HĐĐT"}, 0);
        for (HoaDonDuLieu inv : currentInvoices) {
            String patient = patientNamesByVisitId.getOrDefault(inv.visitId(), "Lượt khám #" + inv.visitId());
            boolean paid = safeMoney(inv.amountPaid()).signum() > 0;
            model.addRow(new Object[]{inv.invoiceNumber(), patient,
                    formatDesignMoney(safeMoney(inv.totalAmount())), formatDesignMoney(safeMoney(inv.amountPaid())),
                    paid ? "Sẵn sàng phát hành" : "Chưa thanh toán"});
        }
        JTable tbl = new JTable(model);
        tbl.setRowHeight(24);
        JScrollPane sp = new JScrollPane(tbl);
        sp.setPreferredSize(new Dimension(680, 340));
        JOptionPane.showMessageDialog(this, sp, "Hóa Đơn Điện Tử", JOptionPane.PLAIN_MESSAGE);
    }
}
