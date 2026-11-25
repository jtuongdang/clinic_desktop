package vn.clinicmanagement.desktop.modules.pharmacy.components;

import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.KetQua;
import vn.clinicmanagement.application.inventory.NhaThuocDanhSachReadCongKetNoi;
import vn.clinicmanagement.application.inventory.NhaThuocDanhSachReadCongKetNoi.PhieuChiTietDayDu;
import vn.clinicmanagement.application.inventory.dispense.DispenseDonThuocBienNhan;
import vn.clinicmanagement.application.inventory.dispense.DispenseDonThuocCommand;
import vn.clinicmanagement.application.prescription.approve.ApproveDonThuocCommand;
import vn.clinicmanagement.desktop.core.config.UngDungPhongChu;
import vn.clinicmanagement.desktop.modules.examination.KhamBenhPhienNguCanh;
import vn.clinicmanagement.desktop.modules.pharmacy.NhaThuocGhepNoiGoc;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.regex.Pattern;

/** Màn hình chi tiết phiếu nhà thuốc (mở khi nhấp đúp dòng phiếu hoặc đơn thuốc). */
public final class NhaThuocChiTietPhieuManHinh extends JPanel {
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
    private static final DateTimeFormatter D_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final NumberFormat MONEY_FMT = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"));
    private static final Color GRID_COLOR = new Color(220, 224, 230);
    private static final Color BLUE_VAL = new Color(20, 65, 165);
    private static final Color WINE_TITLE = new Color(155, 20, 20);

    private final NhaThuocPhieuBangManHinh.TicketRow row;
    private final NhaThuocDanhSachReadCongKetNoi read;
    private final NhaThuocGhepNoiGoc.Services services;
    private final Runnable onChanged;

    private final JLabel titleBadge = new JLabel();
    private final JLabel lblCreatedAt = valLabel(false);
    private final JLabel lblCreatedBy = valLabel(false);
    private final JLabel lblWarehouse = valLabel(false);
    private final JLabel lblApprovedAt = valLabel(false);
    private final JLabel lblApprovedBy = valLabel(false);
    private final JLabel lblInvoiceTotal = valLabel(true);
    private final JLabel lblPaidAt = valLabel(false);
    private final JLabel lblPaidBy = valLabel(false);
    private final JLabel lblPaidAmount = valLabel(true);
    private final JLabel lblIssuedAt = valLabel(false);
    private final JLabel lblIssuedBy = valLabel(false);
    private final JLabel lblDebtAmount = valLabel(true);

    private final DefaultTableModel model;
    private final JTable table;
    private final TableRowSorter<DefaultTableModel> sorter;
    private final JTextField txtSearch = new JTextField();
    private final JButton btnCancelApprove = new JButton("✕  Hủy Duyệt");
    private final JButton btnDispense = new JButton("✓  Xuất Kho");
    private final JButton btnDispensePrint = new JButton("✓  Xuất Kho + In");
    private final JLabel statusLabel = new JLabel(" ");

    private PhieuChiTietDayDu loaded;
    private boolean busy;

    public NhaThuocChiTietPhieuManHinh(
            NhaThuocPhieuBangManHinh.TicketRow row,
            NhaThuocDanhSachReadCongKetNoi read,
            NhaThuocGhepNoiGoc.Services services,
            Runnable onChanged) {
        super(new BorderLayout());
        this.row = Objects.requireNonNull(row, "row");
        this.read = read;
        this.services = services;
        this.onChanged = onChanged;
        setPreferredSize(new Dimension(1160, 660));
        setBackground(Color.WHITE);

        JPanel north = new JPanel();
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        north.setBackground(Color.WHITE);
        north.add(buildToolbar());
        north.add(buildInfoGrid());
        add(north, BorderLayout.NORTH);

        String[] cols = {"STT", "Mã Thuốc", "Tên Thuốc", "Hoạt Chất", "Đơn Vị",
                "SL Tồn", "SL YC", "SL Duyệt", "Đơn Giá", "Thành Tiền", "SĐK", "Số Lô", "Hạn Sử Dụng"};
        model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(model);
        sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);
        configureTable();
        JScrollPane scroll = new JScrollPane(table, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.setBorder(new MatteBorder(1, 0, 1, 0, new Color(185, 190, 198)));
        scroll.getViewport().setBackground(Color.WHITE);
        add(scroll, BorderLayout.CENTER);

        add(buildFooter(), BorderLayout.SOUTH);

        bindShortcuts();
        loadData();
    }

    private JPanel buildToolbar() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Color.WHITE);
        p.setPreferredSize(new Dimension(100, 54));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 54));
        p.setBorder(new CompoundBorder(new MatteBorder(0, 0, 1, 0, new Color(195, 200, 208)), new EmptyBorder(2, 8, 2, 12)));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        left.setOpaque(false);
        JButton btnMenu = topButton("☰", "Menu", 40);
        JButton btnPrint = topButton("🖨", "In Phiếu", 68);
        JButton btnRefresh = topButton("↻", "Làm Mới", 68);

        JPopupMenu popup = new JPopupMenu();
        JMenuItem miPrint = new JMenuItem("In Phiếu");
        miPrint.addActionListener(e -> printTicket());
        JMenuItem miRefresh = new JMenuItem("Làm Mới");
        miRefresh.addActionListener(e -> loadData());
        JMenuItem miClose = new JMenuItem("Đóng");
        miClose.addActionListener(e -> closeDialog());
        popup.add(miPrint);
        popup.add(miRefresh);
        popup.addSeparator();
        popup.add(miClose);

        btnMenu.addActionListener(e -> popup.show(btnMenu, 0, btnMenu.getHeight()));
        btnPrint.addActionListener(e -> printTicket());
        btnRefresh.addActionListener(e -> loadData());

        left.add(btnMenu);
        left.add(btnPrint);
        left.add(btnRefresh);
        p.add(left, BorderLayout.WEST);

        String target = row.targetPatient != null && !row.targetPatient.isBlank() ? row.targetPatient : row.ticketTypeDisplay;
        titleBadge.setText(row.code + " | " + target.toUpperCase(Locale.ROOT));
        titleBadge.setFont(UngDungPhongChu.SECTION);
        titleBadge.setForeground(WINE_TITLE);
        p.add(titleBadge, BorderLayout.EAST);
        return p;
    }

    private JButton topButton(String glyph, String text, int width) {
        JButton b = new JButton("<html><center><span style='font-size:14px;color:#0078D7'>" + glyph + "</span><br>"
                + ("Menu".equals(text) ? "" : text) + "</center></html>");
        b.setFont(UngDungPhongChu.SMALL);
        b.setPreferredSize(new Dimension(width, 46));
        b.setFocusPainted(false);
        b.setContentAreaFilled(false);
        b.setBorder(new EmptyBorder(2, 4, 2, 4));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private JPanel buildInfoGrid() {
        JPanel p = new JPanel(new GridLayout(4, 3, 18, 2));
        p.setBackground(Color.WHITE);
        p.setBorder(new EmptyBorder(6, 10, 6, 12));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));

        p.add(infoCell("Ngày tạo:", lblCreatedAt));
        p.add(infoCell("Người tạo:", lblCreatedBy));
        p.add(infoCell("Kho thuốc:", lblWarehouse));

        p.add(infoCell("Ngày duyệt:", lblApprovedAt));
        p.add(infoCell("Người duyệt:", lblApprovedBy));
        p.add(infoCell("Tiền hóa đơn:", lblInvoiceTotal));

        p.add(infoCell("Ngày thu tiền:", lblPaidAt));
        p.add(infoCell("Người thu tiền:", lblPaidBy));
        p.add(infoCell("Đã thu:", lblPaidAmount));

        p.add(infoCell("Ngày xuất:", lblIssuedAt));
        p.add(infoCell("Người xuất:", lblIssuedBy));
        p.add(infoCell("Còn nợ:", lblDebtAmount));
        return p;
    }

    private static JPanel infoCell(String label, JLabel value) {
        JPanel cell = new JPanel(new BorderLayout(8, 0));
        cell.setOpaque(false);
        JLabel k = new JLabel(label);
        k.setFont(UngDungPhongChu.BODY);
        k.setPreferredSize(new Dimension(92, 20));
        cell.add(k, BorderLayout.WEST);
        value.setBorder(new MatteBorder(0, 0, 1, 0, new Color(195, 200, 212)));
        cell.add(value, BorderLayout.CENTER);
        return cell;
    }

    private static JLabel valLabel(boolean right) {
        JLabel l = new JLabel(" ");
        l.setFont(UngDungPhongChu.BODY);
        l.setForeground(BLUE_VAL);
        if (right) l.setHorizontalAlignment(SwingConstants.RIGHT);
        return l;
    }

    private void configureTable() {
        table.setFont(UngDungPhongChu.BODY);
        table.setRowHeight(24);
        table.setFillsViewportHeight(true);
        table.setShowGrid(true);
        table.setGridColor(GRID_COLOR);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        JTableHeader header = table.getTableHeader();
        header.setFont(UngDungPhongChu.BODY_BOLD);
        header.setPreferredSize(new Dimension(100, 26));
        header.setReorderingAllowed(false);

        int[] widths = {40, 82, 210, 140, 62, 62, 62, 65, 82, 92, 78, 80, 92};
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }

        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        DefaultTableCellRenderer right = new DefaultTableCellRenderer();
        right.setHorizontalAlignment(SwingConstants.RIGHT);

        table.getColumnModel().getColumn(0).setCellRenderer(center);
        table.getColumnModel().getColumn(4).setCellRenderer(center);
        for (int c : new int[]{5, 6, 7, 8, 9}) {
            table.getColumnModel().getColumn(c).setCellRenderer(right);
        }
    }

    private JPanel buildFooter() {
        JPanel p = new JPanel(new BorderLayout(10, 0));
        p.setBackground(new Color(246, 247, 249));
        p.setPreferredSize(new Dimension(100, 42));
        p.setBorder(new CompoundBorder(new MatteBorder(1, 0, 0, 0, new Color(195, 200, 208)), new EmptyBorder(5, 8, 5, 10)));

        JPanel searchWrap = new JPanel(new BorderLayout());
        searchWrap.setPreferredSize(new Dimension(250, 28));
        searchWrap.setBackground(new Color(255, 255, 204));
        searchWrap.setBorder(new LineBorder(new Color(170, 175, 185)));
        txtSearch.setFont(UngDungPhongChu.BODY);
        txtSearch.setOpaque(false);
        txtSearch.setBorder(new EmptyBorder(0, 6, 0, 4));
        txtSearch.setToolTipText("Tìm kiếm (Ctrl+F)");
        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { applySearch(); }
            public void removeUpdate(DocumentEvent e) { applySearch(); }
            public void changedUpdate(DocumentEvent e) { applySearch(); }
        });
        JButton btnFind = new JButton(NhaThuocBieuTuong.createSearchIcon());
        btnFind.setPreferredSize(new Dimension(28, 26));
        btnFind.setFocusPainted(false);
        btnFind.setContentAreaFilled(false);
        btnFind.setBorder(new MatteBorder(0, 1, 0, 0, new Color(185, 190, 200)));
        btnFind.addActionListener(e -> applySearch());
        searchWrap.add(txtSearch, BorderLayout.CENTER);
        searchWrap.add(btnFind, BorderLayout.EAST);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        left.setOpaque(false);
        left.add(searchWrap);
        statusLabel.setFont(UngDungPhongChu.SMALL);
        statusLabel.setForeground(new Color(45, 95, 165));
        left.add(statusLabel);
        p.add(left, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 2));
        right.setOpaque(false);
        styleFooterBtn(btnCancelApprove, false);
        styleFooterBtn(btnDispense, true);
        styleFooterBtn(btnDispensePrint, false);
        btnCancelApprove.addActionListener(e -> closeDialog());
        btnDispense.addActionListener(e -> dispenseFromDetail(false));
        btnDispensePrint.addActionListener(e -> dispenseFromDetail(true));

        right.add(btnCancelApprove);
        right.add(btnDispense);
        right.add(btnDispensePrint);
        p.add(right, BorderLayout.EAST);
        return p;
    }

    private static void styleFooterBtn(JButton b, boolean highlight) {
        b.setFont(UngDungPhongChu.BODY_BOLD);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(highlight ? 118 : 138, 28));
        if (highlight) {
            b.setBackground(new Color(255, 224, 102));
            b.setOpaque(true);
            b.setBorder(new LineBorder(new Color(210, 160, 40)));
        }
    }

    private void bindShortcuts() {
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("control F"), "focusSearch");
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("meta F"), "focusSearch");
        getActionMap().put("focusSearch", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                txtSearch.requestFocusInWindow();
                txtSearch.selectAll();
            }
        });
    }

    private void applySearch() {
        String kw = txtSearch.getText().trim();
        if (kw.isEmpty()) sorter.setRowFilter(null);
        else sorter.setRowFilter(RowFilter.regexFilter("(?iu)" + Pattern.quote(kw)));
    }

    public void loadData() {
        var auth = KhamBenhPhienNguCanh.current();
        if (read == null || auth == null || !auth.isValid()) {
            statusLabel.setText("Không có kết nối CSDL hoặc phiên đăng nhập.");
            updateActionButtons();
            return;
        }
        statusLabel.setText("Đang tải chi tiết phiếu...");
        new SwingWorker<Optional<PhieuChiTietDayDu>, Void>() {
            @Override protected Optional<PhieuChiTietDayDu> doInBackground() {
                return read.getTicketFullDetail(row.type, row.id, auth.facilityId());
            }

            @Override protected void done() {
                try {
                    loaded = get().orElse(null);
                    if (loaded == null) {
                        statusLabel.setText("Không tìm thấy dữ liệu chi tiết.");
                        updateActionButtons();
                        return;
                    }
                    renderDetail(loaded);
                    boolean unpaidRx = row.isPrescription && loaded.debtAmount() != null
                            && loaded.debtAmount().signum() > 0
                            && !"DA_XUAT_HET".equals(loaded.status()) && !"HUY".equals(loaded.status());
                    if (unpaidRx) {
                        statusLabel.setForeground(WINE_TITLE);
                        statusLabel.setText("Chưa thanh toán đủ tại Thu Ngân (còn nợ "
                                + MONEY_FMT.format(loaded.debtAmount()) + "đ) — không thể xuất kho.");
                    } else {
                        statusLabel.setForeground(new Color(0, 95, 175));
                        statusLabel.setText("Số dòng: " + loaded.lines().size() + " | Trạng thái: " + formatStatus(loaded.status()));
                    }
                    updateActionButtons();
                } catch (Exception ex) {
                    statusLabel.setText("Lỗi tải chi tiết: " + ex.getMessage());
                    updateActionButtons();
                }
            }
        }.execute();
    }

    private void renderDetail(PhieuChiTietDayDu d) {
        String target = d.titleTarget() != null && !d.titleTarget().isBlank() ? d.titleTarget() : row.ticketTypeDisplay;
        titleBadge.setText(d.code() + " | " + target.toUpperCase(Locale.ROOT));
        lblCreatedAt.setText(d.createdAt() != null ? d.createdAt().format(DT_FMT) : " ");
        lblCreatedBy.setText(blankSpace(d.createdByName()));
        lblWarehouse.setText(blankSpace(d.warehouseName()));
        lblApprovedAt.setText(d.approvedAt() != null ? d.approvedAt().format(DT_FMT) : " ");
        lblApprovedBy.setText(blankSpace(d.approvedByName()));
        lblInvoiceTotal.setText(MONEY_FMT.format(d.invoiceTotal()));
        lblPaidAt.setText(d.paidAt() != null ? d.paidAt().format(DT_FMT) : " ");
        lblPaidBy.setText(blankSpace(d.paidByName()));
        lblPaidAmount.setText(MONEY_FMT.format(d.paidAmount()));
        lblIssuedAt.setText(d.issuedAt() != null ? d.issuedAt().format(DT_FMT) : " ");
        lblIssuedBy.setText(blankSpace(d.issuedByName()));
        lblDebtAmount.setText(MONEY_FMT.format(d.debtAmount()));

        model.setRowCount(0);
        List<NhaThuocDanhSachReadCongKetNoi.PhieuChiTietDongDayDu> lines = d.lines();
        for (int i = 0; i < lines.size(); i++) {
            var l = lines.get(i);
            model.addRow(new Object[]{
                    i + 1,
                    l.itemCode(),
                    l.itemName(),
                    l.activeIngredient(),
                    l.unit(),
                    fmtQty(l.stockAvailable()),
                    fmtQty(l.requestedQty()),
                    fmtQty(l.approvedQty()),
                    MONEY_FMT.format(l.unitPrice()),
                    MONEY_FMT.format(l.amount()),
                    l.registrationNo(),
                    l.batch(),
                    l.expiry() != null ? l.expiry().format(D_FMT) : ""
            });
        }
    }

    private void updateActionButtons() {
        boolean isPendingRx = row.isPrescription && row.prescriptionId != null
                && loaded != null && !"DA_XUAT_HET".equals(loaded.status()) && !"HUY".equals(loaded.status());
        btnCancelApprove.setText(isPendingRx ? "✕  Đóng" : "✕  Đóng");
        btnDispense.setEnabled(!busy && isPendingRx && services != null);
        btnDispensePrint.setEnabled(!busy && isPendingRx && services != null);
    }

    private void dispenseFromDetail(boolean printAfter) {
        if (busy || !row.isPrescription || row.prescriptionId == null || services == null) return;
        var auth = KhamBenhPhienNguCanh.current();
        if (auth == null || !auth.isValid()) return;

        if (loaded != null && loaded.debtAmount() != null && loaded.debtAmount().signum() > 0) {
            JOptionPane.showMessageDialog(this,
                    "Bệnh nhân còn nợ " + MONEY_FMT.format(loaded.debtAmount())
                            + "đ tại Thu Ngân.\nVui lòng hướng dẫn bệnh nhân quay lại quầy Thu Ngân thanh toán trước khi xuất kho.",
                    "Chưa thanh toán viện phí", JOptionPane.WARNING_MESSAGE);
            return;
        }

        boolean isDraft = loaded != null && "TAM".equals(loaded.status());
        boolean canApprove = isDraft && services.approvePrescription() != null
                && ("ADMIN".equalsIgnoreCase(auth.roleCode()) || "DOCTOR".equalsIgnoreCase(auth.roleCode()));
        if (isDraft && !canApprove) {
            JOptionPane.showMessageDialog(this,
                    "Đơn thuốc đang ở trạng thái Lưu tạm. Cần Bác sĩ hoặc Quản trị viên duyệt trước khi xuất kho.",
                    "Chưa thể xuất kho", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                (canApprove ? "Duyệt đơn và xuất kho FEFO cho phiếu " : "Xác nhận xuất kho FEFO cho phiếu ") + row.code + "?",
                "Xác nhận xuất kho", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        busy = true;
        updateActionButtons();
        statusLabel.setText("Đang xuất kho theo FEFO...");
        int whId = row.warehouseId > 0 ? row.warehouseId : 1;
        var actor = new KiemToanTacNhanNguCanh(auth.employeeId(), auth.roleId(), auth.roleCode(),
                auth.facilityId(), auth.departmentId(), "DESKTOP", auth.sessionId());
        var cmd = new DispenseDonThuocCommand(row.prescriptionId, auth.employeeId(), whId, auth, actor);

        new SwingWorker<KetQua<DispenseDonThuocBienNhan>, Void>() {
            @Override protected KetQua<DispenseDonThuocBienNhan> doInBackground() {
                if (canApprove) {
                    var app = services.approvePrescription().handle(new ApproveDonThuocCommand(row.prescriptionId, auth.employeeId(), auth));
                    if (app.isFailure()) {
                        var err = app.getError().orElseThrow();
                        return KetQua.failure(err.code(), err.message());
                    }
                }
                return services.dispensePrescription().handle(cmd);
            }

            @Override protected void done() {
                busy = false;
                try {
                    var res = get();
                    if (res.isFailure()) {
                        statusLabel.setText("Không thể xuất kho: " + res.getError().orElseThrow().message());
                        JOptionPane.showMessageDialog(NhaThuocChiTietPhieuManHinh.this,
                                res.getError().orElseThrow().message(), "Lỗi xuất kho", JOptionPane.ERROR_MESSAGE);
                        updateActionButtons();
                    } else {
                        var rc = res.valueOrThrow();
                        statusLabel.setText("Đã xuất kho: " + rc.soPhieuXuat());
                        if (onChanged != null) onChanged.run();
                        if (printAfter) printTicket();
                        JOptionPane.showMessageDialog(NhaThuocChiTietPhieuManHinh.this,
                                "Xuất kho thành công! Số phiếu xuất: " + rc.soPhieuXuat(),
                                "Xuất Kho", JOptionPane.INFORMATION_MESSAGE);
                        closeDialog();
                    }
                } catch (Exception ex) {
                    statusLabel.setText("Lỗi hệ thống: " + ex.getMessage());
                    updateActionButtons();
                }
            }
        }.execute();
    }

    private void printTicket() {
        if (GraphicsEnvironment.isHeadless()) return;
        if (loaded == null) {
            JOptionPane.showMessageDialog(this, "Dữ liệu phiếu đang tải hoặc chưa sẵn sàng để in.", "In Phiếu", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        List<PhieuNhapNhaCungCapInManHinh.LineData> printLines = new ArrayList<>();
        for (var l : loaded.lines()) {
            printLines.add(new PhieuNhapNhaCungCapInManHinh.LineData(
                    l.itemName(),
                    l.unit(),
                    "",
                    l.batch(),
                    l.expiry(),
                    l.approvedQty(),
                    l.unitPrice(),
                    l.amount()));
        }
        java.time.LocalDate docDate = loaded.createdAt() != null
                ? loaded.createdAt().toLocalDate()
                : (loaded.issuedAt() != null ? loaded.issuedAt().toLocalDate() : java.time.LocalDate.now());
        var data = new PhieuNhapNhaCungCapInManHinh.ReceiptPrintData(
                loaded.warehouseName(),
                loaded.code(),
                docDate,
                loaded.titleTarget(),
                "",
                docDate,
                loaded.createdByName(),
                printLines,
                loaded.invoiceTotal());
        if ("NHAP_KHO".equals(row.type)) {
            PhieuNhapNhaCungCapInManHinh.showPreviewDialog(this, data);
        } else {
            String docTitle = switch (Objects.toString(row.type, "")) {
                case "XUAT_LE" -> "PHIẾU XUẤT LẺ";
                case "XUAT_BENH_NHAN" -> "PHIẾU XUẤT BỆNH NHÂN";
                default -> "PHIẾU XUẤT KHO";
            };
            PhieuNhapNhaCungCapInManHinh.showPreviewDialog(
                    this, data, docTitle, "Đối tượng: ", "EHC - In Phiếu - " + loaded.code());
        }
    }

    private void closeDialog() {
        Window w = SwingUtilities.getWindowAncestor(this);
        if (w instanceof JDialog d) d.dispose();
    }

    private static String formatStatus(String s) {
        return switch (Objects.toString(s, "")) {
            case "TAM" -> "Lưu tạm";
            case "DA_DUYET" -> "Đã duyệt";
            case "DA_XUAT_HET", "DA_XUAT" -> "Đã xuất kho";
            case "DA_NHAP", "HOAN_THANH" -> "Đã nhập kho";
            case "HUY", "DA_HUY" -> "Đã hủy";
            default -> Objects.toString(s, "");
        };
    }

    private static String fmtQty(BigDecimal v) {
        return v == null ? "0" : v.stripTrailingZeros().toPlainString();
    }

    private static String blankSpace(String s) {
        return s == null || s.isBlank() ? " " : s;
    }

    public JTable getTable() { return table; }
    public JTextField getSearchField() { return txtSearch; }
}
