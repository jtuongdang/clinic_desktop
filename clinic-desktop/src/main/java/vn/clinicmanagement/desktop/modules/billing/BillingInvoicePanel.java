package vn.clinicmanagement.desktop.modules.billing;

import vn.clinicmanagement.application.audit.AuditActorContext;
import vn.clinicmanagement.application.authorization.AuthorizationContext;
import vn.clinicmanagement.application.billing.GetInvoiceByVisitQuery;
import vn.clinicmanagement.application.billing.GetPatientDebtQuery;
import vn.clinicmanagement.application.billing.InvoiceDto;
import vn.clinicmanagement.application.billing.InvoiceLineDto;
import vn.clinicmanagement.application.billing.PatientDebtDto;
import vn.clinicmanagement.application.billing.RecordPaymentCommand;
import vn.clinicmanagement.application.billing.RecordPaymentReceipt;
import vn.clinicmanagement.application.common.Result;
import vn.clinicmanagement.desktop.core.base.BaseViewPanel;
import vn.clinicmanagement.desktop.core.components.StyledTable;
import vn.clinicmanagement.desktop.core.config.AppFonts;
import vn.clinicmanagement.desktop.core.config.UIConstants;
import vn.clinicmanagement.desktop.core.diagnostics.DesktopWorkerDiagnostics;
import vn.clinicmanagement.desktop.core.session.SessionLifecycleService;
import vn.clinicmanagement.desktop.core.session.UserSession;
import vn.clinicmanagement.desktop.modules.examination.components.print.MedicalBillingSlipDialog;
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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Phân hệ Quản Lý Viện Phí & Hóa Đơn (Billing & Invoicing)
 * Thiết kế chuẩn EHC HIS, tối ưu cho màn hình 13-inch và đồng bộ phong cách hệ thống.
 * Đấu nối xử lý bất đồng bộ qua SwingWorker tới các use case nghiệp vụ Billing.
 */
public class BillingInvoicePanel extends BaseViewPanel {

    private final DecimalFormat currencyFormat = new DecimalFormat("#,##0 'VNĐ'");
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final BillingCompositionRoot.Services services;
    private final SessionLifecycleService sessionLifecycleService;
    private final List<SwingWorker<?, ?>> inFlightWorkers = new CopyOnWriteArrayList<>();
    private final AtomicLong refreshSequence = new AtomicLong();
    private final AtomicLong viewSessionEpoch = new AtomicLong();

    private final List<Integer> activeVisitIds = new ArrayList<>();
    private final List<InvoiceDto> currentInvoices = new ArrayList<>();
    private List<PatientDebtDto> latestPatientDebts = new ArrayList<>();

    private DefaultTableModel invoiceTableModel;
    private DefaultTableModel detailTableModel;
    private StyledTable invoiceTable;
    private StyledTable detailTable;
    private JTextField txtSearch;
    private JComboBox<String> cboFilterStatus;
    private JLabel lblTotalAmount;
    private JLabel lblPaidAmount;
    private JLabel lblUnpaidAmount;
    private JSplitPane centerSplitPane;
    private JButton btnCollect;
    private JButton btnPrintSlip;
    private JButton btnRefresh;

    public BillingInvoicePanel() {
        this(resolveDefaultServices(), null);
    }

    public BillingInvoicePanel(BillingCompositionRoot.Services services) {
        this(services, null);
    }

    public BillingInvoicePanel(BillingCompositionRoot.Services services, SessionLifecycleService sessionLifecycleService) {
        super("billing", "Quản Lý Viện Phí", "💳");
        this.services = services;
        this.sessionLifecycleService = sessionLifecycleService;
        buildUI();
    }

    private static BillingCompositionRoot.Services resolveDefaultServices() {
        try {
            return BillingCompositionRoot.createDefault();
        } catch (Exception ex) {
            return null;
        }
    }

    private void buildUI() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        // 1. THANH CÔNG CỤ ĐỈNH (Top Action Toolbar)
        add(buildToolbar(), BorderLayout.NORTH);

        // 2. KHU VỰC TRUNG TÂM (SplitPane: Danh sách hóa đơn phía trên/trái, Chi tiết phía dưới/phải)
        centerSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        centerSplitPane.setDividerLocation(360);
        centerSplitPane.setResizeWeight(0.55);
        centerSplitPane.setDividerSize(5);
        centerSplitPane.setBorder(BorderFactory.createEmptyBorder());

        centerSplitPane.setTopComponent(buildInvoiceListSection());
        centerSplitPane.setBottomComponent(buildInvoiceDetailSection());

        add(centerSplitPane, BorderLayout.CENTER);

        // 3. THANH TỔNG KẾT & TRẠNG THÁI (Summary Footer)
        add(buildSummaryFooter(), BorderLayout.SOUTH);

        // 4. ĐĂNG KÝ PHÍM TẮT F2 (Thu Tiền) VÀ F7 (In Bảng Kê)
        setupKeyboardShortcuts();

        refreshData();
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

    private JPanel buildToolbar() {
        JPanel toolbar = new JPanel(new BorderLayout(8, 0));
        toolbar.setBackground(new Color(245, 248, 252));
        toolbar.setBorder(new CompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(210, 220, 230)),
                new EmptyBorder(6, 12, 6, 12)
        ));

        // Nhóm tìm kiếm & Bộ lọc
        JPanel leftGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        leftGroup.setOpaque(false);

        JLabel lblTitle = new JLabel("QUẢN LÝ VIỆN PHÍ & THU NGÂN");
        lblTitle.setFont(AppFonts.SECTION);
        lblTitle.setForeground(new Color(24, 85, 130));
        leftGroup.add(lblTitle);

        JSeparator sep1 = new JSeparator(SwingConstants.VERTICAL);
        sep1.setPreferredSize(new Dimension(2, 22));
        leftGroup.add(sep1);

        JLabel lblSearch = new JLabel("Tìm kiếm (Mã BN/Tên):");
        lblSearch.setFont(AppFonts.BODY);
        leftGroup.add(lblSearch);

        txtSearch = new JTextField(12);
        txtSearch.setFont(AppFonts.BODY);
        txtSearch.setBackground(new Color(255, 255, 220));
        txtSearch.addActionListener(e -> onSearchTriggered());
        leftGroup.add(txtSearch);

        JLabel lblStatus = new JLabel("Trạng thái:");
        lblStatus.setFont(AppFonts.BODY);
        leftGroup.add(lblStatus);

        cboFilterStatus = new JComboBox<>(new String[]{"Tất cả hóa đơn", "Chưa thanh toán", "Đã thanh toán", "Miễn giảm / BHYT"});
        cboFilterStatus.setFont(AppFonts.BODY);
        cboFilterStatus.addActionListener(e -> applyFilter());
        leftGroup.add(cboFilterStatus);

        // Nhóm nút chức năng bên phải
        JPanel rightGroup = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        rightGroup.setOpaque(false);

        btnCollect = createButton(" Thu Tiền (F2)", new Color(25, 135, 84), Color.WHITE);
        btnCollect.addActionListener(e -> handleCollectPayment());
        rightGroup.add(btnCollect);

        btnPrintSlip = createButton(" In Bảng Kê (F7)", new Color(13, 110, 253), Color.WHITE);
        btnPrintSlip.addActionListener(e -> handleOpenBillingSlip());
        rightGroup.add(btnPrintSlip);

        btnRefresh = createButton("🔄 Làm Mới", new Color(108, 117, 125), Color.WHITE);
        btnRefresh.addActionListener(e -> refreshData());
        rightGroup.add(btnRefresh);

        toolbar.add(leftGroup, BorderLayout.WEST);
        toolbar.add(rightGroup, BorderLayout.EAST);
        return toolbar;
    }

    private JPanel buildInvoiceListSection() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(new CompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UIConstants.COLOR_BORDER),
                new EmptyBorder(4, 8, 4, 8)
        ));

        JLabel title = new JLabel("DANH SÁCH HÓA ĐƠN & PHIẾU THU KHÁM BỆNH");
        title.setFont(AppFonts.BODY_BOLD);
        title.setForeground(new Color(40, 70, 100));
        title.setBorder(new EmptyBorder(4, 4, 6, 4));
        panel.add(title, BorderLayout.NORTH);

        String[] cols = {
                "STT", "Số Phiếu Thu", "Mã BN", "Họ Và Tên Bệnh Nhân", "Ngày Lập",
                "Đối Tượng", "Tổng Tiền", "BHYT Chi Trả", "BN Cần Trả", "Đã Thu", "Còn Nợ", "Trạng Thái"
        };
        invoiceTableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        invoiceTable = new StyledTable(invoiceTableModel);
        invoiceTable.setFont(AppFonts.BODY);
        invoiceTable.setRowHeight(26);

        // Căn chỉnh cột số tiền sang phải
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        for (int c : new int[]{6, 7, 8, 9, 10}) {
            invoiceTable.getColumnModel().getColumn(c).setCellRenderer(rightRenderer);
        }

        invoiceTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                loadSelectedInvoiceDetails();
            }
        });

        JScrollPane scrollPane = new JScrollPane(invoiceTable);
        scrollPane.setBorder(new LineBorder(UIConstants.COLOR_BORDER, 1));
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel buildInvoiceDetailSection() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(4, 8, 4, 8));

        JLabel title = new JLabel("CHI TIẾT DỊCH VỤ / THUỐC TRONG PHIẾU THU ĐANG CHỌN");
        title.setFont(AppFonts.BODY_BOLD);
        title.setForeground(new Color(40, 70, 100));
        title.setBorder(new EmptyBorder(4, 4, 6, 4));
        panel.add(title, BorderLayout.NORTH);

        String[] cols = {
                "STT", "Khoa/Phòng Thực Hiện", "Tên Dịch Vụ / Thuốc / Vật Tư", "ĐVT",
                "Số Lượng", "Đơn Giá", "Thành Tiền", "Tỷ Lệ BHYT", "BHYT Trả", "Bệnh Nhân Trả"
        };
        detailTableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        detailTable = new StyledTable(detailTableModel);
        detailTable.setFont(AppFonts.BODY);
        detailTable.setRowHeight(25);

        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        for (int c : new int[]{4, 5, 6, 7, 8, 9}) {
            detailTable.getColumnModel().getColumn(c).setCellRenderer(rightRenderer);
        }

        JScrollPane scrollPane = new JScrollPane(detailTable);
        scrollPane.setBorder(new LineBorder(UIConstants.COLOR_BORDER, 1));
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel buildSummaryFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(new Color(240, 244, 250));
        footer.setBorder(new CompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(195, 205, 220)),
                new EmptyBorder(8, 14, 8, 14)
        ));

        JPanel statPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 18, 0));
        statPanel.setOpaque(false);

        lblTotalAmount = new JLabel("Tổng doanh thu: 0 VNĐ");
        lblTotalAmount.setFont(AppFonts.BODY_BOLD);
        lblTotalAmount.setForeground(new Color(20, 60, 110));

        lblPaidAmount = new JLabel("Đã thanh toán: 0 VNĐ");
        lblPaidAmount.setFont(AppFonts.BODY_BOLD);
        lblPaidAmount.setForeground(new Color(25, 135, 84));

        lblUnpaidAmount = new JLabel("Còn công nợ: 0 VNĐ");
        lblUnpaidAmount.setFont(AppFonts.BODY_BOLD);
        lblUnpaidAmount.setForeground(new Color(220, 53, 69));

        statPanel.add(lblTotalAmount);
        statPanel.add(new JSeparator(SwingConstants.VERTICAL));
        statPanel.add(lblPaidAmount);
        statPanel.add(new JSeparator(SwingConstants.VERTICAL));
        statPanel.add(lblUnpaidAmount);

        JLabel lblGuide = new JLabel("💡 Mẹo: Nhấn 'In Bảng Kê' để xem trước Mẫu 01/KCCB chuẩn Bộ Y Tế.");
        lblGuide.setFont(AppFonts.SMALL);
        lblGuide.setForeground(new Color(100, 110, 120));

        footer.add(lblGuide, BorderLayout.WEST);
        footer.add(statPanel, BorderLayout.EAST);
        return footer;
    }

    private JButton createButton(String text, Color bg, Color fg) {
        JButton btn = new JButton(text);
        btn.setFont(AppFonts.BODY_BOLD);
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(new CompoundBorder(
                new LineBorder(bg.darker(), 1),
                new EmptyBorder(4, 10, 4, 10)
        ));
        return btn;
    }

    public void handleCollectPayment() {
        int row = invoiceTable.getSelectedRow();
        if (row < 0) {
            showMessage("Vui lòng chọn một phiếu thu từ danh sách để thực hiện thu tiền!",
                    "Chưa Chọn Hóa Đơn", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String code = (String) invoiceTableModel.getValueAt(row, 1);
        String name = (String) invoiceTableModel.getValueAt(row, 3);
        String unpaidStr = (String) invoiceTableModel.getValueAt(row, 10);

        if ("0 VNĐ".equals(unpaidStr) || "0".equals(unpaidStr)) {
            showMessage("Hóa đơn " + code + " của bệnh nhân " + name + " đã được thanh toán đầy đủ!",
                    "Thông Báo", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int confirm = JOptionPane.YES_OPTION;
        if (!GraphicsEnvironment.isHeadless()) {
            confirm = JOptionPane.showConfirmDialog(this,
                    "Xác nhận thu tiền phiếu [" + code + "] - " + name + "\nSố tiền cần thu: " + unpaidStr + "\n\nTiến hành xác nhận thanh toán?",
                    "Xác Nhận Thu Viện Phí", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        }

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        if (services == null || services.recordPayment() == null) {
            // Local fallback if no services
            invoiceTableModel.setValueAt(invoiceTableModel.getValueAt(row, 8), row, 9);
            invoiceTableModel.setValueAt("0 VNĐ", row, 10);
            invoiceTableModel.setValueAt("Đã thanh toán", row, 11);
            showMessage("Thu tiền thành công cho hóa đơn: " + code, "Thành Công", JOptionPane.INFORMATION_MESSAGE);
            updateSummaryCounters();
            return;
        }

        AuthorizationContext authzContext = currentAuthorizationContext();
        AuditActorContext auditContext = currentAuditActorContext();
        if (authzContext == null) {
            showMessage("Phiên làm việc không hợp lệ hoặc đã hết hạn.", "Lỗi Xác Thực", JOptionPane.ERROR_MESSAGE);
            return;
        }

        InvoiceDto currentInvoice = row < currentInvoices.size() ? currentInvoices.get(row) : null;
        int invoiceId = currentInvoice != null ? currentInvoice.id() : 0;
        BigDecimal amountToPay = currentInvoice != null ? currentInvoice.remainingAmount() : parseCurrency(unpaidStr);

        SwingWorker<Result<RecordPaymentReceipt>, Void> worker = new SwingWorker<>() {
            @Override
            protected Result<RecordPaymentReceipt> doInBackground() {
                RecordPaymentCommand command = new RecordPaymentCommand(
                        invoiceId,
                        amountToPay,
                        PhuongThucThanhToan.TIEN_MAT,
                        null,
                        "Thu tiền tại quầy thu ngân",
                        authzContext,
                        auditContext
                );
                return services.recordPayment().handle(command);
            }

            @Override
            protected void done() {
                try {
                    if (isCancelled()) return;
                    Result<RecordPaymentReceipt> result = get();
                    if (result != null && result.isSuccess()) {
                        RecordPaymentReceipt receipt = result.getValue().orElse(null);
                        invoiceTableModel.setValueAt(invoiceTableModel.getValueAt(row, 8), row, 9);
                        invoiceTableModel.setValueAt("0 VNĐ", row, 10);
                        invoiceTableModel.setValueAt("Đã thanh toán", row, 11);
                        updateSummaryCounters();
                        showMessage("Thu tiền thành công cho hóa đơn: " + code, "Thành Công", JOptionPane.INFORMATION_MESSAGE);
                        // Tải lại chi tiết công nợ nếu có
                        if (currentInvoice != null) {
                            loadPatientDebt(currentInvoice.patientId());
                        }
                    } else {
                        String errorMsg = result != null && result.getError().isPresent()
                                ? result.getError().get().message()
                                : "Thu tiền thất bại.";
                        showMessage(errorMsg, "Lỗi Thu Viện Phí", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (InterruptedException ex) {
                    DesktopWorkerDiagnostics.restoreInterruptStatus();
                } catch (CancellationException ex) {
                    // Normal worker cancellation
                } catch (ExecutionException ex) {
                    showMessage("Lỗi hệ thống khi thu tiền: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                } finally {
                    inFlightWorkers.remove(this);
                }
            }
        };

        inFlightWorkers.add(worker);
        worker.execute();
    }

    private void handleOpenBillingSlip() {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }
        Window owner = SwingUtilities.getWindowAncestor(this);
        MedicalBillingSlipDialog dialog = new MedicalBillingSlipDialog(owner);
        dialog.setVisible(true);
    }

    private void loadSelectedInvoiceDetails() {
        detailTableModel.setRowCount(0);
        int row = invoiceTable.getSelectedRow();
        if (row < 0 || row >= currentInvoices.size()) return;

        InvoiceDto invoice = currentInvoices.get(row);
        if (invoice == null) return;

        List<InvoiceLineDto> lines = invoice.lines();
        if (lines != null) {
            int stt = 1;
            for (InvoiceLineDto line : lines) {
                if (line == null) continue;
                String dept = resolveDepartment(line.itemType());
                String uom = resolveUom(line.itemType());
                String qtyStr = line.quantity() != null ? line.quantity().stripTrailingZeros().toPlainString() : "1";
                String priceStr = line.unitPrice() != null ? currencyFormat.format(line.unitPrice()) : "0 VNĐ";
                String totalStr = line.lineTotal() != null ? currencyFormat.format(line.lineTotal()) : "0 VNĐ";

                detailTableModel.addRow(new Object[]{
                        String.valueOf(stt++),
                        dept,
                        line.description() != null ? line.description() : "",
                        uom,
                        qtyStr,
                        priceStr,
                        totalStr,
                        "0%",
                        "0 VNĐ",
                        totalStr
                });
            }
        }

        // Tải thông tin công nợ của bệnh nhân qua SwingWorker
        if (invoice.patientId() > 0) {
            loadPatientDebt(invoice.patientId());
        }
    }

    private String resolveDepartment(String itemType) {
        if (itemType == null) return "Phòng Khám Đa Khoa";
        return switch (itemType.toUpperCase()) {
            case "THUOC", "DUOC_PHAM" -> "Nhà Thuốc Bệnh Viện";
            case "DICH_VU_CLS", "CAN_LAM_SANG", "XET_NGHIEM" -> "Khoa Xét Nghiệm / CĐHA";
            default -> "Phòng Khám Nội";
        };
    }

    private String resolveUom(String itemType) {
        if (itemType == null) return "Lượt";
        return switch (itemType.toUpperCase()) {
            case "THUOC", "DUOC_PHAM" -> "Viên";
            case "DICH_VU_CLS", "CAN_LAM_SANG" -> "Lần";
            default -> "Lượt";
        };
    }

    public void loadPatientDebt(int patientId) {
        if (services == null || services.patientDebt() == null || patientId <= 0) return;
        AuthorizationContext auth = currentAuthorizationContext();
        AuditActorContext audit = currentAuditActorContext();
        if (auth == null) return;

        SwingWorker<Result<List<PatientDebtDto>>, Void> worker = new SwingWorker<>() {
            @Override
            protected Result<List<PatientDebtDto>> doInBackground() {
                GetPatientDebtQuery query = new GetPatientDebtQuery(patientId, auth, audit);
                return services.patientDebt().handle(query);
            }

            @Override
            protected void done() {
                try {
                    if (isCancelled()) return;
                    Result<List<PatientDebtDto>> result = get();
                    if (result != null && result.isSuccess()) {
                        latestPatientDebts = result.getValue().orElse(List.of());
                    }
                } catch (InterruptedException ex) {
                    DesktopWorkerDiagnostics.restoreInterruptStatus();
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
        long total = 0;
        long paid = 0;
        long unpaid = 0;

        for (int r = 0; r < invoiceTableModel.getRowCount(); r++) {
            String strTotal = String.valueOf(invoiceTableModel.getValueAt(r, 6)).replaceAll("[^0-9]", "");
            String strPaid = String.valueOf(invoiceTableModel.getValueAt(r, 9)).replaceAll("[^0-9]", "");
            String strUnpaid = String.valueOf(invoiceTableModel.getValueAt(r, 10)).replaceAll("[^0-9]", "");

            if (!strTotal.isEmpty()) total += Long.parseLong(strTotal);
            if (!strPaid.isEmpty()) paid += Long.parseLong(strPaid);
            if (!strUnpaid.isEmpty()) unpaid += Long.parseLong(strUnpaid);
        }

        lblTotalAmount.setText("Tổng doanh thu: " + currencyFormat.format(total));
        lblPaidAmount.setText("Đã thanh toán: " + currencyFormat.format(paid));
        lblUnpaidAmount.setText("Còn công nợ: " + currencyFormat.format(unpaid));
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
        if (cboFilterStatus == null) return;
        String selectedFilter = (String) cboFilterStatus.getSelectedItem();
        if (selectedFilter == null || "Tất cả hóa đơn".equals(selectedFilter)) {
            renderInvoices(currentInvoices);
            return;
        }

        List<InvoiceDto> filtered = new ArrayList<>();
        for (InvoiceDto inv : currentInvoices) {
            if (inv == null) continue;
            String statusText = formatStatus(inv.status());
            if ("Chưa thanh toán".equals(selectedFilter) && ("Chưa thanh toán".equals(statusText) || "Thanh toán một phần".equals(statusText))) {
                filtered.add(inv);
            } else if ("Đã thanh toán".equals(selectedFilter) && "Đã thanh toán".equals(statusText)) {
                filtered.add(inv);
            } else if ("Miễn giảm / BHYT".equals(selectedFilter) && inv.insurancePaidAmount() != null && inv.insurancePaidAmount().compareTo(BigDecimal.ZERO) > 0) {
                filtered.add(inv);
            }
        }
        renderInvoices(filtered);
    }

    private void renderInvoices(List<InvoiceDto> invoices) {
        invoiceTableModel.setRowCount(0);
        detailTableModel.setRowCount(0);

        if (invoices == null || invoices.isEmpty()) {
            updateSummaryCounters();
            return;
        }

        int stt = 1;
        for (InvoiceDto inv : invoices) {
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

            invoiceTableModel.addRow(new Object[]{
                    String.valueOf(stt++),
                    inv.invoiceNumber() != null ? inv.invoiceNumber() : "",
                    String.valueOf(inv.patientId()),
                    "Bệnh nhân " + inv.patientId(),
                    dateStr,
                    subject,
                    totalStr,
                    bhytStr,
                    copayStr,
                    paidStr,
                    debtStr,
                    statusStr
            });
        }

        if (invoiceTableModel.getRowCount() > 0) {
            invoiceTable.setRowSelectionInterval(0, 0);
            loadSelectedInvoiceDetails();
        }

        updateSummaryCounters();
    }

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
        refreshData();
    }

    @Override
    public void refreshData() {
        cancelInFlightWorkers();

        if (services == null || services.getInvoiceByVisit() == null || activeVisitIds.isEmpty()) {
            currentInvoices.clear();
            invoiceTableModel.setRowCount(0);
            detailTableModel.setRowCount(0);
            updateSummaryCounters();
            return;
        }

        AuthorizationContext authzContext = currentAuthorizationContext();
        AuditActorContext auditContext = currentAuditActorContext();
        if (authzContext == null) {
            currentInvoices.clear();
            invoiceTableModel.setRowCount(0);
            detailTableModel.setRowCount(0);
            updateSummaryCounters();
            return;
        }

        long epoch = viewSessionEpoch.get();
        long sequence = refreshSequence.incrementAndGet();
        List<Integer> targetVisits = new ArrayList<>(activeVisitIds);

        SwingWorker<List<InvoiceDto>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<InvoiceDto> doInBackground() {
                List<InvoiceDto> loaded = new ArrayList<>();
                for (int visitId : targetVisits) {
                    if (isCancelled()) return loaded;
                    GetInvoiceByVisitQuery query = new GetInvoiceByVisitQuery(visitId, authzContext, auditContext);
                    Result<InvoiceDto> result = services.getInvoiceByVisit().handle(query);
                    if (result != null && result.isSuccess()) {
                        result.getValue().ifPresent(loaded::add);
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
                    List<InvoiceDto> invoices = get();
                    currentInvoices.clear();
                    if (invoices != null) {
                        currentInvoices.addAll(invoices);
                    }
                    renderInvoices(currentInvoices);
                } catch (InterruptedException ex) {
                    DesktopWorkerDiagnostics.restoreInterruptStatus();
                } catch (CancellationException ex) {
                    // Normal worker cancellation
                } catch (ExecutionException ex) {
                    currentInvoices.clear();
                    invoiceTableModel.setRowCount(0);
                    detailTableModel.setRowCount(0);
                    updateSummaryCounters();
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
        return inFlightWorkers.size();
    }

    public BillingCompositionRoot.Services getServices() {
        return services;
    }

    public StyledTable getInvoiceTable() {
        return invoiceTable;
    }

    public StyledTable getDetailTable() {
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

    public List<InvoiceDto> getCurrentInvoices() {
        return currentInvoices;
    }

    public List<PatientDebtDto> getLatestPatientDebts() {
        return latestPatientDebts;
    }

    private BigDecimal parseCurrency(String str) {
        if (str == null) return BigDecimal.ZERO;
        String digits = str.replaceAll("[^0-9]", "");
        return digits.isEmpty() ? BigDecimal.ZERO : new BigDecimal(digits);
    }

    private void showMessage(String message, String title, int type) {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }
        JOptionPane.showMessageDialog(this, message, title, type);
    }

    private AuthorizationContext currentAuthorizationContext() {
        if (sessionLifecycleService != null) {
            return sessionLifecycleService.currentAuthorizationContext().orElse(null);
        }
        UserSession session = UserSession.getInstance();
        int employeeId = session.getUserId() > 0 ? session.getUserId() : 1;
        int roleId = session.getRoleId() > 0 ? session.getRoleId() : 1;
        String roleCode = session.getRoleCode() != null && !session.getRoleCode().isBlank()
                ? session.getRoleCode() : "CASHIER";
        int facilityId = session.getFacilityId() > 0 ? session.getFacilityId() : 1;
        int departmentId = session.getDepartmentId() > 0 ? session.getDepartmentId() : 1;
        UUID sessionId;
        try {
            sessionId = session.getSessionId() != null ? UUID.fromString(session.getSessionId()) : UUID.randomUUID();
        } catch (Exception ex) {
            sessionId = UUID.randomUUID();
        }
        return new AuthorizationContext(employeeId, roleId, roleCode, facilityId, departmentId, sessionId);
    }

    private AuditActorContext currentAuditActorContext() {
        if (sessionLifecycleService != null) {
            return sessionLifecycleService.currentAuditActorContext().orElse(null);
        }
        UserSession session = UserSession.getInstance();
        int employeeId = session.getUserId() > 0 ? session.getUserId() : 1;
        int roleId = session.getRoleId() > 0 ? session.getRoleId() : 1;
        String roleCode = session.getRoleCode() != null && !session.getRoleCode().isBlank()
                ? session.getRoleCode() : "CASHIER";
        int facilityId = session.getFacilityId() > 0 ? session.getFacilityId() : 1;
        int departmentId = session.getDepartmentId() > 0 ? session.getDepartmentId() : 1;
        String workstationId = session.getWorkstationId();
        if (workstationId == null || workstationId.isBlank()) {
            workstationId = "CASHIER-01";
        }
        return new AuditActorContext(employeeId, roleId, roleCode, facilityId, departmentId, workstationId, UUID.randomUUID());
    }
}
