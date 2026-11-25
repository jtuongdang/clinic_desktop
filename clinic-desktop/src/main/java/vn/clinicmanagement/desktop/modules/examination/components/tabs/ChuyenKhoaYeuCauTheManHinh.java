package vn.clinicmanagement.desktop.modules.examination.components.tabs;

import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.cls.DichVuYeuCauDuLieu;
import vn.clinicmanagement.application.cls.cancel.CancelDichVuYeuCauCommand;
import vn.clinicmanagement.application.cls.get.GetDichVuTheoLuotKhamQuery;
import vn.clinicmanagement.application.common.KetQua;
import vn.clinicmanagement.desktop.core.config.UngDungPhongChu;
import vn.clinicmanagement.desktop.modules.examination.KhamBenhGhepNoiGoc;
import vn.clinicmanagement.desktop.modules.examination.KhamBenhManHinh.AdminPatientItem;
import vn.clinicmanagement.desktop.modules.examination.KhamBenhPhienNguCanh;
import vn.clinicmanagement.desktop.modules.print.examination.LamSangYeuCauPrintPreviewHopThoai;
import vn.clinicmanagement.desktop.modules.print.examination.LamSangYeuCauPrintPreviewManHinh;
import vn.clinicmanagement.domain.common.enums.TrangThaiChiDinhDichVu;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.IntConsumer;

/**
 * Tab "Chuyên Khoa" trong phân hệ Khám Bệnh.
 * Hiển thị danh sách dịch vụ kỹ thuật / thủ thuật / phẫu thuật chuyên khoa (`DICH_VU_KY_THUAT`, `CHUYEN_KHOA`, ...),
 * thông tin phiếu chỉ định (`CK-...`), trạng thái thực hiện, in phiếu chỉ định (F2) và hủy chỉ phiếu.
 */
public class ChuyenKhoaYeuCauTheManHinh extends JPanel {

    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final KhamBenhGhepNoiGoc.Services services;
    private int currentVisitId = 0;
    private AdminPatientItem currentPatient;
    private IntConsumer onOrderCountChanged;
    private Runnable onOrdersChangedCallback;

    private final List<DichVuYeuCauDuLieu> loadedOrders = new ArrayList<>();
    private List<DichVuYeuCauDuLieu> displayedOrders = List.of();
    private final List<Integer> tableRowOrderIds = new ArrayList<>();

    private JLabel lblMaPhieu;
    private JLabel lblNgayYLenh;
    private JLabel lblNgayChiDinh;
    private JLabel lblNguoiChiDinh;
    private JLabel lblNoiChiDinh;
    private JLabel lblChanDoan;

    private JTable table;
    private DefaultTableModel tableModel;
    private JPanel ticketListPanel;
    private JButton btnPrintOrder;
    private JButton btnCancelOrder;

    public ChuyenKhoaYeuCauTheManHinh(KhamBenhGhepNoiGoc.Services services) {
        this.services = services;
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        JPanel centerWorkspace = new JPanel(new BorderLayout(0, 0));
        centerWorkspace.setBackground(Color.WHITE);
        centerWorkspace.add(buildHeaderInfoPanel(), BorderLayout.NORTH);
        centerWorkspace.add(buildTableSection(), BorderLayout.CENTER);
        centerWorkspace.add(buildFooterBar(), BorderLayout.SOUTH);

        add(centerWorkspace, BorderLayout.CENTER);
        add(buildTicketSelectorPanel(), BorderLayout.EAST);

        int mask = GraphicsEnvironment.isHeadless()
                ? java.awt.event.InputEvent.CTRL_DOWN_MASK
                : Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0), "printSpecialtyOrder");
        getActionMap().put("printSpecialtyOrder", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { handlePrintOrder(); }
        });
        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_P, mask), "printSpecialtyOrderCtrl");
        getActionMap().put("printSpecialtyOrderCtrl", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { handlePrintOrder(); }
        });

        renderEmptyState();
    }

    private JPanel buildHeaderInfoPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(6, 10, 6, 10));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 4, 2, 12);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        lblMaPhieu = createBlueLabel("");
        lblNgayYLenh = createBlueLabel("");
        lblNgayChiDinh = createBlueLabel("");
        lblNguoiChiDinh = createBlueLabel("");
        lblNoiChiDinh = createBlueLabel("");
        lblChanDoan = createBlueLabel("");

        // Row 0
        gbc.gridy = 0;
        gbc.gridx = 0; gbc.weightx = 0; panel.add(createFieldLabel("Mã phiếu:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.28; panel.add(wrapUnderlined(lblMaPhieu), gbc);
        gbc.gridx = 2; gbc.weightx = 0; panel.add(createFieldLabel("Ngày y lệnh:"), gbc);
        gbc.gridx = 3; gbc.weightx = 0.32; panel.add(wrapUnderlined(lblNgayYLenh), gbc);
        gbc.gridx = 4; gbc.weightx = 0; panel.add(createFieldLabel("Ngày chỉ định:"), gbc);
        gbc.gridx = 5; gbc.weightx = 0.40; panel.add(wrapUnderlined(lblNgayChiDinh), gbc);

        // Row 1
        gbc.gridy = 1;
        gbc.gridx = 0; gbc.weightx = 0; panel.add(createFieldLabel("Người chỉ định:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.28; panel.add(wrapUnderlined(lblNguoiChiDinh), gbc);
        gbc.gridx = 2; gbc.weightx = 0; panel.add(createFieldLabel("Nơi chỉ định:"), gbc);
        gbc.gridx = 3; gbc.weightx = 0.72; gbc.gridwidth = 3; panel.add(wrapUnderlined(lblNoiChiDinh), gbc);
        gbc.gridwidth = 1;

        // Row 2
        gbc.gridy = 2;
        gbc.gridx = 0; gbc.weightx = 0; panel.add(createFieldLabel("Chẩn đoán:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0; gbc.gridwidth = 5; panel.add(wrapUnderlined(lblChanDoan), gbc);

        return panel;
    }

    private JScrollPane buildTableSection() {
        String[] cols = {"Tên Dịch Vụ", "SL", "PTTT", "Loại PTTT", "Ghi Chú (Chỉ Định)", "Bắt Đầu", "Kết Thúc"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        table.setRowHeight(24);
        table.setFont(UngDungPhongChu.BODY);
        table.setGridColor(new Color(228, 233, 240));
        table.setSelectionBackground(new Color(210, 228, 248));
        table.setSelectionForeground(new Color(20, 25, 35));
        table.getTableHeader().setFont(UngDungPhongChu.BODY_BOLD);
        table.getTableHeader().setBackground(new Color(242, 245, 250));
        table.getTableHeader().setReorderingAllowed(false);

        int[] widths = {270, 38, 85, 110, 140, 115, 115};
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }

        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable tbl, Object value,
                                                                   boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, col);
                String firstCell = String.valueOf(tbl.getValueAt(row, 0));
                if (firstCell.startsWith("GROUP:")) {
                    lbl.setFont(UngDungPhongChu.SMALL_BOLD);
                    lbl.setForeground(new Color(25, 85, 175));
                    lbl.setBackground(new Color(244, 247, 252));
                    lbl.setIcon(null);
                    lbl.setText(col == 0 ? firstCell.substring(6) : "");
                    lbl.setHorizontalAlignment(SwingConstants.LEFT);
                    return lbl;
                }
                lbl.setFont(UngDungPhongChu.BODY);
                lbl.setForeground(new Color(25, 30, 40));
                lbl.setBackground(isSelected ? tbl.getSelectionBackground() : Color.WHITE);
                lbl.setHorizontalAlignment(col == 1 ? SwingConstants.CENTER : SwingConstants.LEFT);
                if (col == 0 && value != null && !value.toString().isBlank()) {
                    int orderId = row < tableRowOrderIds.size() ? tableRowOrderIds.get(row) : 0;
                    String status = findStatusByOrderId(orderId);
                    lbl.setIcon(new StatusDotIcon(status));
                    lbl.setIconTextGap(6);
                } else {
                    lbl.setIcon(null);
                }
                return lbl;
            }
        });

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, new Color(205, 214, 226)));
        sp.getViewport().setBackground(Color.WHITE);
        return sp;
    }

    private JPanel buildFooterBar() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 6));
        footer.setBackground(new Color(248, 249, 252));

        btnPrintOrder = new JButton("Phiếu Chỉ Định (F2)");
        btnPrintOrder.setFont(UngDungPhongChu.SMALL_BOLD);
        btnPrintOrder.setBackground(Color.WHITE);
        btnPrintOrder.setFocusPainted(false);
        btnPrintOrder.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnPrintOrder.setBorder(new CompoundBorder(new LineBorder(new Color(185, 195, 210), 1), new EmptyBorder(4, 10, 4, 10)));
        btnPrintOrder.addActionListener(e -> handlePrintOrder());

        btnCancelOrder = new JButton("Hủy Chỉ Phiếu");
        btnCancelOrder.setFont(UngDungPhongChu.SMALL_BOLD);
        btnCancelOrder.setForeground(new Color(180, 40, 35));
        btnCancelOrder.setBackground(Color.WHITE);
        btnCancelOrder.setFocusPainted(false);
        btnCancelOrder.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnCancelOrder.setBorder(new CompoundBorder(new LineBorder(new Color(210, 175, 175), 1), new EmptyBorder(4, 10, 4, 10)));
        btnCancelOrder.addActionListener(e -> handleCancelOrder());

        footer.add(btnPrintOrder);
        footer.add(btnCancelOrder);
        return footer;
    }

    private JPanel buildTicketSelectorPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setPreferredSize(new Dimension(145, 500));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, new Color(205, 214, 226)));

        ticketListPanel = new JPanel();
        ticketListPanel.setLayout(new BoxLayout(ticketListPanel, BoxLayout.Y_AXIS));
        ticketListPanel.setBackground(Color.WHITE);
        panel.add(ticketListPanel, BorderLayout.NORTH);
        return panel;
    }

    public void setPatientContext(AdminPatientItem patient) {
        this.currentPatient = patient;
    }

    public void setOnOrderCountChanged(IntConsumer callback) {
        this.onOrderCountChanged = callback;
    }

    public void setOnOrdersChangedCallback(Runnable callback) {
        this.onOrdersChangedCallback = callback;
    }

    public void loadServicesForVisit(int visitId) {
        this.currentVisitId = visitId;
        if (visitId <= 0 || services == null || services.getServicesByVisit() == null
                || KhamBenhPhienNguCanh.current() == null) {
            loadedOrders.clear();
            notifyCountChanged(0);
            renderEmptyState();
            return;
        }
        PhanQuyenNguCanh auth = KhamBenhPhienNguCanh.current();
        KiemToanTacNhanNguCanh actor = new KiemToanTacNhanNguCanh(
                auth.employeeId(), auth.roleId(), auth.roleCode(),
                auth.facilityId(), auth.departmentId(), "DESKTOP-HIS", UUID.randomUUID());

        new SwingWorker<List<DichVuYeuCauDuLieu>, Void>() {
            @Override protected List<DichVuYeuCauDuLieu> doInBackground() {
                KetQua<List<DichVuYeuCauDuLieu>> res = services.getServicesByVisit().handle(
                        new GetDichVuTheoLuotKhamQuery(visitId, auth, actor));
                if (res != null && res.isSuccess()) {
                    return filterSpecialtyOrders(res.getValue().orElse(List.of()));
                }
                return List.of();
            }

            @Override protected void done() {
                if (visitId != currentVisitId) return;
                try {
                    applyLoadedOrders(get());
                } catch (Exception ex) {
                    renderEmptyState();
                }
            }
        }.execute();
    }

    /** Giữ lại các chỉ định chuyên khoa / dịch vụ kỹ thuật / PTTT (khác KHAM, XET_NGHIEM, CDHA). */
    static List<DichVuYeuCauDuLieu> filterSpecialtyOrders(List<DichVuYeuCauDuLieu> allOrders) {
        if (allOrders == null || allOrders.isEmpty()) return List.of();
        return allOrders.stream()
                .filter(o -> o != null && !"DA_HUY".equalsIgnoreCase(o.status()) && isSpecialtyType(o.serviceType()))
                .toList();
    }

    private static boolean isSpecialtyType(String serviceType) {
        if (serviceType == null || serviceType.isBlank()) return false;
        String t = serviceType.trim().toUpperCase(Locale.ROOT);
        return "DICH_VU_KY_THUAT".equals(t) || "CHUYEN_KHOA".equals(t) || "THU_THUAT".equals(t) || "PHAU_THUAT".equals(t);
    }

    private void applyLoadedOrders(List<DichVuYeuCauDuLieu> orders) {
        loadedOrders.clear();
        loadedOrders.addAll(orders);
        notifyCountChanged(orders.size());

        if (orders.isEmpty()) {
            renderEmptyState();
            return;
        }

        Map<LocalDate, List<DichVuYeuCauDuLieu>> grouped = new LinkedHashMap<>();
        for (DichVuYeuCauDuLieu o : orders) {
            LocalDate day = o.orderedAt() != null ? o.orderedAt().toLocalDate() : LocalDate.now();
            grouped.computeIfAbsent(day, k -> new ArrayList<>()).add(o);
        }

        ticketListPanel.removeAll();
        boolean first = true;
        for (var entry : grouped.entrySet()) {
            LocalDate day = entry.getKey();
            List<DichVuYeuCauDuLieu> dayOrders = entry.getValue();
            String code = "CK-" + day.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            JPanel card = createTicketCard(code, "Chuyên khoa (" + dayOrders.size() + ")", first,
                    () -> displayOrderGroup(day, dayOrders));
            ticketListPanel.add(card);
            first = false;
        }
        ticketListPanel.revalidate();
        ticketListPanel.repaint();

        var firstEntry = grouped.entrySet().iterator().next();
        displayOrderGroup(firstEntry.getKey(), firstEntry.getValue());
    }

    private void displayOrderGroup(LocalDate dateKey, List<DichVuYeuCauDuLieu> groupOrders) {
        displayedOrders = groupOrders;
        DichVuYeuCauDuLieu first = groupOrders.get(0);
        String dayTag = dateKey != null ? dateKey.format(DateTimeFormatter.ofPattern("yyMMdd")) : "";
        lblMaPhieu.setText("CK" + dayTag + "." + first.orderId());
        lblNgayYLenh.setText(first.orderedAt() != null ? first.orderedAt().format(DATE_TIME_FMT) : "");
        lblNgayChiDinh.setText(lblNgayYLenh.getText());
        lblNguoiChiDinh.setText(currentPatient != null && currentPatient.doctor != null && !currentPatient.doctor.isBlank()
                ? currentPatient.doctor : "Bác sĩ khám");
        lblNoiChiDinh.setText(currentPatient != null && currentPatient.clinicRoomName != null && !currentPatient.clinicRoomName.isBlank()
                ? currentPatient.clinicRoomName : "Khoa Khám Bệnh");
        lblChanDoan.setText(currentPatient != null && currentPatient.diagnosis != null ? currentPatient.diagnosis : "");

        tableModel.setRowCount(0);
        tableRowOrderIds.clear();

        String statusDesc = TrangThaiChiDinhDichVu.fromCode(first.status()).getDescription();
        String endInfo = first.returnedAt() != null ? ", Thời gian kết thúc: " + first.returnedAt().format(DATE_TIME_FMT) : "";
        tableModel.addRow(new Object[]{
                "GROUP:Dịch vụ Chuyên Khoa / PTTT, Ngày " + (dateKey != null ? dateKey.format(DATE_FMT) : "") + ", " + statusDesc + endInfo,
                "", "", "", "", "", ""
        });
        tableRowOrderIds.add(0);

        for (DichVuYeuCauDuLieu o : groupOrders) {
            tableModel.addRow(new Object[]{
                    o.serviceName(),
                    o.quantity() != null ? o.quantity().stripTrailingZeros().toPlainString() : "1",
                    "Thực hiện chính",
                    o.serviceType(),
                    o.conclusion() != null && !o.conclusion().isBlank() ? o.conclusion()
                            : (o.resultSummary() != null ? o.resultSummary() : ""),
                    o.performedAt() != null ? o.performedAt().format(DATE_TIME_FMT)
                            : (o.orderedAt() != null ? o.orderedAt().format(DATE_TIME_FMT) : ""),
                    o.returnedAt() != null ? o.returnedAt().format(DATE_TIME_FMT) : ""
            });
            tableRowOrderIds.add(o.orderId());
        }
        btnPrintOrder.setEnabled(true);
        btnCancelOrder.setEnabled(true);
    }

    private void renderEmptyState() {
        displayedOrders = List.of();
        lblMaPhieu.setText("");
        lblNgayYLenh.setText("");
        lblNgayChiDinh.setText("");
        lblNguoiChiDinh.setText("");
        lblNoiChiDinh.setText("");
        lblChanDoan.setText("");
        if (ticketListPanel != null) {
            ticketListPanel.removeAll();
            ticketListPanel.revalidate();
            ticketListPanel.repaint();
        }
        tableModel.setRowCount(0);
        tableRowOrderIds.clear();
        tableModel.addRow(new Object[]{"GROUP:[Chưa có chỉ định chuyên khoa / PTTT cho lượt khám này]", "", "", "", "", "", ""});
        tableRowOrderIds.add(0);
        btnPrintOrder.setEnabled(false);
        btnCancelOrder.setEnabled(false);
    }

    private void handlePrintOrder() {
        if (displayedOrders == null || displayedOrders.isEmpty()) return;
        Window owner = SwingUtilities.getWindowAncestor(this);
        LamSangYeuCauPrintPreviewManHinh.ClinicalOrderData data =
                LamSangYeuCauPrintPreviewManHinh.fromVisitOrders(currentPatient, displayedOrders, Map.of());
        new LamSangYeuCauPrintPreviewHopThoai(owner, data).setVisible(true);
    }

    private void handleCancelOrder() {
        if (services == null || services.cancelServiceOrder() == null) return;
        PhanQuyenNguCanh auth = KhamBenhPhienNguCanh.current();
        if (auth == null) return;

        int row = table.getSelectedRow();
        int orderId = (row >= 0 && row < tableRowOrderIds.size()) ? tableRowOrderIds.get(row) : 0;
        if (orderId <= 0 && !displayedOrders.isEmpty()) {
            orderId = displayedOrders.get(0).orderId();
        }
        if (orderId <= 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn dịch vụ chuyên khoa cần hủy.",
                    "Hủy Chỉ Phiếu", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String reason = JOptionPane.showInputDialog(this, "Nhập lý do hủy chỉ định:", "Hủy Chỉ Phiếu", JOptionPane.QUESTION_MESSAGE);
        if (reason == null) return;
        if (reason.isBlank()) reason = "Bác sĩ hủy chỉ định chuyên khoa";

        final int targetOrderId = orderId;
        final String finalReason = reason.trim();
        KiemToanTacNhanNguCanh actor = new KiemToanTacNhanNguCanh(
                auth.employeeId(), auth.roleId(), auth.roleCode(),
                auth.facilityId(), auth.departmentId(), "DESKTOP-HIS", UUID.randomUUID());

        new SwingWorker<KetQua<?>, Void>() {
            @Override protected KetQua<?> doInBackground() {
                return services.cancelServiceOrder().handle(new CancelDichVuYeuCauCommand(targetOrderId, finalReason, auth, actor));
            }
            @Override protected void done() {
                try {
                    KetQua<?> res = get();
                    if (res != null && res.isSuccess()) {
                        loadServicesForVisit(currentVisitId);
                        if (onOrdersChangedCallback != null) onOrdersChangedCallback.run();
                    } else {
                        String msg = res == null ? "Không thể hủy chỉ định."
                                : res.getError().map(err -> err.message()).orElse("Không thể hủy chỉ định.");
                        JOptionPane.showMessageDialog(ChuyenKhoaYeuCauTheManHinh.this, msg,
                                "Không thể hủy chỉ phiếu", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    private String findStatusByOrderId(int orderId) {
        for (DichVuYeuCauDuLieu o : displayedOrders) {
            if (o.orderId() == orderId) return o.status();
        }
        return "";
    }

    private void notifyCountChanged(int count) {
        if (onOrderCountChanged != null) onOrderCountChanged.accept(count);
    }

    private static JLabel createFieldLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(UngDungPhongChu.BODY);
        lbl.setForeground(new Color(45, 50, 60));
        return lbl;
    }

    private static JLabel createBlueLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(UngDungPhongChu.BODY);
        lbl.setForeground(new Color(25, 85, 175));
        return lbl;
    }

    private static JPanel wrapUnderlined(JLabel lbl) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(210, 218, 228)));
        p.add(lbl, BorderLayout.CENTER);
        return p;
    }

    private static JPanel createTicketCard(String title, String subtitle, boolean selected, Runnable onClick) {
        JPanel card = new JPanel(new GridLayout(2, 1, 0, 2));
        card.setBorder(new CompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(225, 230, 238)),
                new EmptyBorder(6, 8, 6, 8)));
        card.setBackground(selected ? new Color(30, 120, 215) : Color.WHITE);
        card.setCursor(new Cursor(Cursor.HAND_CURSOR));
        JLabel l1 = new JLabel(title);
        l1.setFont(UngDungPhongChu.SMALL_BOLD);
        l1.setForeground(selected ? Color.WHITE : new Color(25, 30, 40));
        JLabel l2 = new JLabel(subtitle);
        l2.setFont(UngDungPhongChu.SMALL);
        l2.setForeground(selected ? new Color(230, 240, 255) : new Color(95, 105, 120));
        card.add(l1);
        card.add(l2);
        card.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) { onClick.run(); }
        });
        return card;
    }

    private static final class StatusDotIcon implements Icon {
        private final String status;
        StatusDotIcon(String status) { this.status = status; }
        @Override public int getIconWidth() { return 12; }
        @Override public int getIconHeight() { return 12; }
        @Override public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            boolean done = "DA_CO_KET_QUA".equalsIgnoreCase(status) || "HOAN_THANH".equalsIgnoreCase(status);
            g2.setColor(done ? new Color(34, 150, 85) : new Color(235, 155, 35));
            g2.fillOval(x, y, 11, 11);
            if (done) {
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(1.6f));
                g2.drawLine(x + 3, y + 6, x + 5, y + 8);
                g2.drawLine(x + 5, y + 8, x + 9, y + 3);
            }
            g2.dispose();
        }
    }
}
