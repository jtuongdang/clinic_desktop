package vn.clinicmanagement.desktop.modules.examination.components.tabs;

import vn.clinicmanagement.application.cls.get.GetDichVuTheoLuotKhamHandler;

import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.cls.cancel.CancelDichVuYeuCauBienNhan;
import vn.clinicmanagement.application.cls.cancel.CancelDichVuYeuCauCommand;
import vn.clinicmanagement.application.cls.DichVuYeuCauDuLieu;
import vn.clinicmanagement.application.cls.get.GetDichVuTheoLuotKhamQuery;
import vn.clinicmanagement.application.cls.XetNghiemKetQuaDuLieu;
import vn.clinicmanagement.application.common.KetQua;
import vn.clinicmanagement.desktop.core.config.UngDungPhongChu;
import vn.clinicmanagement.desktop.modules.examination.KhamBenhGhepNoiGoc;
import vn.clinicmanagement.desktop.modules.examination.KhamBenhManHinh.AdminPatientItem;
import vn.clinicmanagement.desktop.modules.examination.KhamBenhPhienNguCanh;
import vn.clinicmanagement.desktop.modules.examination.components.dialogs.DichVuYeuCauHopThoai;
import vn.clinicmanagement.desktop.modules.examination.components.print.LamSangYeuCauPrintPreviewHopThoai;
import vn.clinicmanagement.desktop.modules.examination.components.print.LamSangYeuCauPrintPreviewManHinh;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.IntConsumer;

/**
 * Tab "Xét Nghiệm" trong phân hệ Khám Bệnh.
 * Đọc danh sách chỉ định xét nghiệm (`XET_NGHIEM`) và chi tiết kết quả từ DB qua `GetDichVuTheoLuotKhamHandler`.
 */
public class XetNghiemYeuCauTheManHinh extends JPanel {

    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private JTable labTable;
    private DefaultTableModel tableModel;
    private JPanel ticketListPanel;
    private JLabel lblMaPhieu;
    private JLabel lblNgayYLenh;
    private JLabel lblNgayChiDinh;
    private JLabel lblNguoiChiDinh;
    private JLabel lblNoiChiDinh;
    private JLabel lblChanDoan;
    private JPanel selectedTicketCard;
    private JPanel toastBanner;
    private JLabel lblToast;
    private JPanel rightActionBox;
    private boolean isCanceled = false;
    private Runnable onEditOrderCallback;
    private Runnable onOrdersChangedCallback;
    private IntConsumer onLabOrderCountChanged;

    private final KhamBenhGhepNoiGoc.Services services;
    private final AtomicLong loadSequence = new AtomicLong();
    private final List<DichVuYeuCauDuLieu> loadedLabOrders = new ArrayList<>();
    private final List<DichVuYeuCauDuLieu> loadedVisitClsOrders = new ArrayList<>();
    private final List<DichVuYeuCauDuLieu> currentGroupOrders = new ArrayList<>();
    private final List<Integer> tableRowOrderIds = new ArrayList<>();
    private AdminPatientItem patientContext;
    private int currentVisitId = 0;
    private int currentOrderId = 0;

    public XetNghiemYeuCauTheManHinh() {
        this(null, resolveDefaultServices());
    }

    public XetNghiemYeuCauTheManHinh(Runnable onEditOrderCallback) {
        this(onEditOrderCallback, resolveDefaultServices());
    }

    public XetNghiemYeuCauTheManHinh(Runnable onEditOrderCallback, KhamBenhGhepNoiGoc.Services services) {
        this.onEditOrderCallback = onEditOrderCallback;
        this.services = services != null ? services : resolveDefaultServices();
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        add(buildTicketSelectorPanel(), BorderLayout.EAST);
        add(buildTicketDetailPanel(), BorderLayout.CENTER);
        renderEmptyState();
    }

    private static KhamBenhGhepNoiGoc.Services resolveDefaultServices() {
        try {
            return KhamBenhGhepNoiGoc.createDefault();
        } catch (Exception ex) {
            return null;
        }
    }

    public void setOnLabOrderCountChanged(IntConsumer listener) {
        this.onLabOrderCountChanged = listener;
    }

    public void setOnOrdersChangedCallback(Runnable callback) {
        this.onOrdersChangedCallback = callback;
    }

    public void setPatientContext(AdminPatientItem patient) {
        this.patientContext = patient;
    }

    /** Tải toàn bộ chỉ định xét nghiệm từ CSDL cho lượt khám đang chọn. */
    public void loadServicesForVisit(int visitId) {
        this.currentVisitId = visitId;
        this.currentOrderId = 0;
        long seq = loadSequence.incrementAndGet();

        if (services == null || services.getServicesByVisit() == null || visitId <= 0) {
            renderEmptyState();
            notifyCountChanged(0);
            return;
        }
        PhanQuyenNguCanh auth = KhamBenhPhienNguCanh.current();
        if (auth == null) {
            renderEmptyState();
            notifyCountChanged(0);
            return;
        }
        KiemToanTacNhanNguCanh actor = createActorContext(auth);

        SwingWorker<KetQua<List<DichVuYeuCauDuLieu>>, Void> worker = new SwingWorker<>() {
            @Override
            protected KetQua<List<DichVuYeuCauDuLieu>> doInBackground() {
                return services.getServicesByVisit().handle(
                        new GetDichVuTheoLuotKhamQuery(visitId, auth, actor));
            }

            @Override
            protected void done() {
                if (seq != loadSequence.get() || visitId != currentVisitId) {
                    return;
                }
                try {
                    KetQua<List<DichVuYeuCauDuLieu>> res = get();
                    if (res != null && res.isSuccess()) {
                        List<DichVuYeuCauDuLieu> allOrders = res.getValue().orElse(List.of());
                        loadedVisitClsOrders.clear();
                        if (allOrders != null) {
                            loadedVisitClsOrders.addAll(allOrders);
                        }
                        List<DichVuYeuCauDuLieu> labOrders = filterLabOrders(allOrders);
                        applyLoadedOrders(labOrders);
                    } else {
                        renderEmptyState();
                        notifyCountChanged(0);
                    }
                } catch (Exception ex) {
                    renderEmptyState();
                    notifyCountChanged(0);
                }
            }
        };
        worker.execute();
    }

    /** Chỉ giữ lại các chỉ định thuộc nhóm xét nghiệm (`XET_NGHIEM`) và chưa bị hủy. */
    static List<DichVuYeuCauDuLieu> filterLabOrders(List<DichVuYeuCauDuLieu> allOrders) {
        if (allOrders == null || allOrders.isEmpty()) {
            return List.of();
        }
        return allOrders.stream()
                .filter(o -> o != null
                        && !"DA_HUY".equalsIgnoreCase(o.status())
                        && ("XET_NGHIEM".equalsIgnoreCase(o.serviceType()) || isLabServiceCode(o.serviceCode())))
                .toList();
    }

    private static boolean isLabServiceCode(String serviceCode) {
        return serviceCode != null && serviceCode.trim().toUpperCase(Locale.ROOT).startsWith("XN");
    }

    public static String formatOrderStatus(String status) {
        if (status == null || status.isBlank()) {
            return "Đang chờ";
        }
        return vn.clinicmanagement.domain.common.enums.TrangThaiChiDinhDichVu
                .fromCode(status)
                .getDescription();
    }

    private void applyLoadedOrders(List<DichVuYeuCauDuLieu> orders) {
        loadedLabOrders.clear();
        loadedLabOrders.addAll(orders);
        notifyCountChanged(orders.size());

        if (orders.isEmpty()) {
            renderEmptyState();
            return;
        }

        // Nhóm theo ngày chỉ định để hiển thị ở cột phiếu bên phải
        Map<LocalDate, List<DichVuYeuCauDuLieu>> groupedByDate = new LinkedHashMap<>();
        for (DichVuYeuCauDuLieu order : orders) {
            LocalDate dateKey = order.orderedAt() != null
                    ? order.orderedAt().toLocalDate()
                    : LocalDate.now();
            groupedByDate.computeIfAbsent(dateKey, k -> new ArrayList<>()).add(order);
        }

        rebuildTicketCards(groupedByDate);

        // Hiển thị đợt mới nhất
        Map.Entry<LocalDate, List<DichVuYeuCauDuLieu>> firstGroup = groupedByDate.entrySet().iterator().next();
        displayOrderGroup(firstGroup.getKey(), firstGroup.getValue());
    }

    private void rebuildTicketCards(Map<LocalDate, List<DichVuYeuCauDuLieu>> groupedByDate) {
        if (ticketListPanel == null) return;
        ticketListPanel.removeAll();
        selectedTicketCard = null;
        String visitType = patientContext != null && patientContext.registrationType != null
                && !patientContext.registrationType.isBlank()
                ? patientContext.registrationType : "Khám bệnh";

        boolean first = true;
        for (Map.Entry<LocalDate, List<DichVuYeuCauDuLieu>> entry : groupedByDate.entrySet()) {
            LocalDate date = entry.getKey();
            List<DichVuYeuCauDuLieu> dayOrders = entry.getValue();
            String dateLabel = date.format(DATE_FMT);
            JPanel card = createTicketCard(dateLabel, visitType, first, () -> displayOrderGroup(date, dayOrders));
            if (first) {
                selectedTicketCard = card;
                first = false;
            }
            ticketListPanel.add(card);
        }
        ticketListPanel.revalidate();
        ticketListPanel.repaint();
    }

    private void displayOrderGroup(LocalDate date, List<DichVuYeuCauDuLieu> groupOrders) {
        if (groupOrders == null || groupOrders.isEmpty()) {
            renderEmptyState();
            return;
        }
        currentGroupOrders.clear();
        currentGroupOrders.addAll(groupOrders);
        tableRowOrderIds.clear();

        DichVuYeuCauDuLieu head = groupOrders.get(0);
        currentOrderId = head.orderId();
        isCanceled = groupOrders.stream().allMatch(o -> "DA_HUY".equalsIgnoreCase(o.status()));

        String orderedTimeStr = formatDateTime(head.orderedAt());
        String doctorText = patientContext != null && patientContext.doctor != null
                && !patientContext.doctor.isBlank() && !"Chưa phân công".equals(patientContext.doctor)
                ? patientContext.doctor
                : (head.doctorId() > 0 ? "BS #" + head.doctorId() : "");
        String clinicText = patientContext != null && patientContext.clinicRoomName != null
                && !patientContext.clinicRoomName.isBlank()
                ? patientContext.clinicRoomName
                : (patientContext != null ? patientContext.departmentName : "");
        String diagnosisText = patientContext != null ? patientContext.diagnosis : "";

        lblMaPhieu.setText("XN-" + head.orderId());
        lblNgayYLenh.setText(orderedTimeStr);
        lblNgayChiDinh.setText(orderedTimeStr);
        lblNguoiChiDinh.setText(doctorText);
        lblNoiChiDinh.setText(clinicText);
        lblChanDoan.setText(diagnosisText);

        tableModel.setRowCount(0);
        String dateStr = date != null ? date.format(DATE_FMT) : "";
        String groupStatus = formatOrderStatus(head.status());
        tableModel.addRow(new Object[]{
                "GROUP:Phòng Xét nghiệm, Ngày " + dateStr + ", Mã phiếu: XN-" + head.orderId() + ", " + groupStatus,
                "", "", "", ""
        });
        tableRowOrderIds.add(0);

        for (DichVuYeuCauDuLieu order : groupOrders) {
            String qty = formatQuantity(order.quantity());
            List<XetNghiemKetQuaDuLieu> details = order.testResults();
            if (details != null && !details.isEmpty()) {
                for (XetNghiemKetQuaDuLieu line : details) {
                    String resultVal = line.value() != null ? line.value() : "(Chưa thực hiện)";
                    if (line.evaluation() != null && !line.evaluation().isBlank()) {
                        resultVal = resultVal + " (" + line.evaluation() + ")";
                    }
                    tableModel.addRow(new Object[]{
                            "  " + order.serviceName() + " - " + line.indicatorName(),
                            qty,
                            resultVal,
                            line.unit() != null ? line.unit() : "",
                            line.referenceRange() != null ? line.referenceRange() : ""
                    });
                    tableRowOrderIds.add(order.orderId());
                }
            } else {
                String resultCell = order.resultSummary() != null && !order.resultSummary().isBlank()
                        ? order.resultSummary()
                        : "(Chưa thực hiện - " + formatOrderStatus(order.status()) + ")";
                tableModel.addRow(new Object[]{
                        "  " + order.serviceName(),
                        qty,
                        resultCell,
                        "",
                        ""
                });
                tableRowOrderIds.add(order.orderId());
            }
        }

        padEmptyRows(15);
        updateFooterButtons(isCanceled);
        toastBanner.setVisible(false);
    }

    private void renderEmptyState() {
        loadedLabOrders.clear();
        loadedVisitClsOrders.clear();
        currentGroupOrders.clear();
        tableRowOrderIds.clear();
        currentOrderId = 0;
        isCanceled = false;

        if (ticketListPanel != null) {
            ticketListPanel.removeAll();
            selectedTicketCard = null;
            ticketListPanel.revalidate();
            ticketListPanel.repaint();
        }
        if (lblMaPhieu != null) lblMaPhieu.setText("");
        if (lblNgayYLenh != null) lblNgayYLenh.setText("");
        if (lblNgayChiDinh != null) lblNgayChiDinh.setText("");
        if (lblNguoiChiDinh != null) {
            lblNguoiChiDinh.setText(patientContext != null ? patientContext.doctor : "");
        }
        if (lblNoiChiDinh != null) {
            lblNoiChiDinh.setText(patientContext != null && !patientContext.clinicRoomName.isBlank()
                    ? patientContext.clinicRoomName : "");
        }
        if (lblChanDoan != null) {
            lblChanDoan.setText(patientContext != null ? patientContext.diagnosis : "");
        }
        if (tableModel != null) {
            tableModel.setRowCount(0);
            tableModel.addRow(new Object[]{
                    "GROUP:[Chưa có chỉ định xét nghiệm cho lượt khám này]",
                    "", "", "", ""
            });
            padEmptyRows(15);
        }
        if (rightActionBox != null) {
            updateFooterButtons(false);
        }
        if (toastBanner != null) {
            toastBanner.setVisible(false);
        }
    }

    private void padEmptyRows(int minRows) {
        int remaining = Math.max(0, minRows - tableModel.getRowCount());
        for (int i = 0; i < remaining; i++) {
            tableModel.addRow(new Object[]{"", "", "", "", ""});
            tableRowOrderIds.add(0);
        }
    }

    private void notifyCountChanged(int count) {
        if (onLabOrderCountChanged != null) {
            onLabOrderCountChanged.accept(count);
        }
    }

    private static String formatDateTime(LocalDateTime time) {
        return time != null ? time.format(DATE_TIME_FMT) : "";
    }

    private static String formatQuantity(BigDecimal qty) {
        if (qty == null) return "1";
        return qty.stripTrailingZeros().toPlainString();
    }

    private static KiemToanTacNhanNguCanh createActorContext(PhanQuyenNguCanh auth) {
        return new KiemToanTacNhanNguCanh(
                auth.employeeId(),
                auth.roleId(),
                auth.roleCode(),
                auth.facilityId(),
                auth.departmentId(),
                "DESKTOP-HIS",
                UUID.randomUUID()
        );
    }

    public void setOnEditOrderCallback(Runnable onEditOrderCallback) {
        this.onEditOrderCallback = onEditOrderCallback;
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

    private JPanel createTicketCard(String dateStr, String typeStr, boolean isSelected, Runnable onClick) {
        JPanel card = new JPanel(new GridLayout(2, 1, 0, 2));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(225, 230, 238)),
                new EmptyBorder(5, 10, 5, 8)
        ));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        card.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JLabel l1 = new JLabel(dateStr);
        l1.setFont(UngDungPhongChu.SMALL_BOLD);

        JLabel l2 = new JLabel(typeStr);
        l2.setFont(UngDungPhongChu.SMALL);

        applyCardStyle(card, l1, l2, isSelected);

        card.add(l1);
        card.add(l2);

        JPopupMenu contextMenu = createOrderDateContextMenu(dateStr, l1);
        MouseAdapter adapter = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    highlightSelectedCard(card);
                    if (onClick != null) onClick.run();
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger() || SwingUtilities.isRightMouseButton(e)) {
                    contextMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger() || SwingUtilities.isRightMouseButton(e)) {
                    contextMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        };

        card.addMouseListener(adapter);
        l1.addMouseListener(adapter);
        l2.addMouseListener(adapter);

        return card;
    }

    private void highlightSelectedCard(JPanel activeCard) {
        if (ticketListPanel == null) return;
        for (Component comp : ticketListPanel.getComponents()) {
            if (comp instanceof JPanel card && card.getComponentCount() >= 2
                    && card.getComponent(0) instanceof JLabel l1
                    && card.getComponent(1) instanceof JLabel l2) {
                applyCardStyle(card, l1, l2, card == activeCard);
            }
        }
        selectedTicketCard = activeCard;
    }

    private static void applyCardStyle(JPanel card, JLabel l1, JLabel l2, boolean isSelected) {
        if (isSelected) {
            card.setBackground(new Color(0, 120, 215));
            l1.setForeground(Color.WHITE);
            l2.setForeground(new Color(235, 245, 255));
        } else {
            card.setBackground(Color.WHITE);
            l1.setForeground(new Color(30, 35, 45));
            l2.setForeground(new Color(90, 95, 105));
        }
    }

    private JPopupMenu createOrderDateContextMenu(String currentVal, JLabel lblDate) {
        JPopupMenu menu = new JPopupMenu();
        menu.setBackground(Color.WHITE);
        menu.setBorder(BorderFactory.createLineBorder(new Color(130, 145, 165), 1));

        JMenuItem miChangeDate = new JMenuItem("Chuyển Ngày Y Lệnh", new CheckmarkMiniIcon());
        miChangeDate.setFont(UngDungPhongChu.SMALL);
        miChangeDate.setForeground(new Color(25, 30, 40));
        miChangeDate.setBackground(Color.WHITE);
        miChangeDate.setPreferredSize(new Dimension(175, 24));
        miChangeDate.setBorder(new EmptyBorder(2, 6, 2, 8));
        miChangeDate.setCursor(new Cursor(Cursor.HAND_CURSOR));

        miChangeDate.addActionListener(e -> {
            String current = lblNgayYLenh != null ? lblNgayYLenh.getText() : currentVal;
            String newDate = !GraphicsEnvironment.isHeadless()
                    ? JOptionPane.showInputDialog(this, "Nhập ngày y lệnh mới cho phiếu xét nghiệm:", current)
                    : null;
            if (newDate != null && !newDate.trim().isEmpty()) {
                if (lblNgayYLenh != null) lblNgayYLenh.setText(newDate.trim());
                lblDate.setText(newDate.trim());
            }
        });

        menu.add(miChangeDate);
        return menu;
    }

    private JPanel buildTicketDetailPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.add(buildHeaderPanel(), BorderLayout.NORTH);
        panel.add(buildLabTablePanel(), BorderLayout.CENTER);
        panel.add(buildActionFooterPanel(), BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(6, 8, 8, 8));

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(2, 4, 2, 4);

        int r = 0;

        this.lblMaPhieu = createUnderlineValue("");
        this.lblNgayYLenh = createUnderlineValue("");
        this.lblNgayChiDinh = createUnderlineValue("");
        addHeaderRow(form, c, r++,
                "Mã phiếu:", lblMaPhieu, 1,
                "Ngày y lệnh:", lblNgayYLenh, 1,
                "Ngày chỉ định:", lblNgayChiDinh, 1);

        this.lblNguoiChiDinh = createUnderlineValue("");
        this.lblNoiChiDinh = createUnderlineValue("");
        addHeaderRow(form, c, r++,
                "Người chỉ định:", lblNguoiChiDinh, 1,
                "Nơi chỉ định:", lblNoiChiDinh, 3,
                null, null, 0);

        this.lblChanDoan = createUnderlineValue("");
        addHeaderRow(form, c, r,
                "Chẩn đoán:", lblChanDoan, 5,
                null, null, 0,
                null, null, 0);

        panel.add(form, BorderLayout.CENTER);

        JPanel toolBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 2));
        toolBox.setBackground(Color.WHITE);

        JButton btnHistory = new JButton(" Lịch sử", new HistoryClockIcon());
        btnHistory.setFont(UngDungPhongChu.SMALL);
        btnHistory.setBackground(Color.WHITE);
        btnHistory.setFocusPainted(false);
        btnHistory.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnHistory.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(205, 215, 226), 1),
                new EmptyBorder(3, 8, 3, 8)
        ));
        btnHistory.addActionListener(e -> showOrderHistoryDialog());

        JButton btnChart = new JButton(" Biểu Đồ", new BarChartIcon());
        btnChart.setFont(UngDungPhongChu.SMALL);
        btnChart.setBackground(Color.WHITE);
        btnChart.setFocusPainted(false);
        btnChart.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnChart.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(205, 215, 226), 1),
                new EmptyBorder(3, 8, 3, 8)
        ));
        btnChart.addActionListener(e -> {
            String summary = loadedLabOrders.isEmpty()
                    ? "Chưa có dữ liệu xét nghiệm để vẽ biểu đồ."
                    : "Số chỉ định xét nghiệm hiện có: " + loadedLabOrders.size();
            JOptionPane.showMessageDialog(this, summary, "Biểu Đồ Xu Hướng Xét Nghiệm", JOptionPane.INFORMATION_MESSAGE);
        });

        toolBox.add(btnHistory);
        toolBox.add(btnChart);
        panel.add(toolBox, BorderLayout.EAST);

        return panel;
    }

    private void showOrderHistoryDialog() {
        if (loadedLabOrders.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Chưa có lịch sử chỉ định xét nghiệm cho lượt khám này.",
                    "Lịch Sử Xét Nghiệm", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        StringBuilder sb = new StringBuilder("Danh sách chỉ định xét nghiệm của lượt khám:\n");
        for (DichVuYeuCauDuLieu o : loadedLabOrders) {
            sb.append("- ").append(formatDateTime(o.orderedAt()))
                    .append(": XN-").append(o.orderId())
                    .append(" · ").append(o.serviceName())
                    .append(" (").append(formatOrderStatus(o.status())).append(")\n");
        }
        JOptionPane.showMessageDialog(this, sb.toString(), "Lịch Sử Xét Nghiệm", JOptionPane.INFORMATION_MESSAGE);
    }

    private JPanel buildLabTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);

        String[] cols = {"Tên Xét nghiệm", "SL", "Kết Quả", "Đơn Vị", "Loại Bệnh Phẩm"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        labTable = new JTable(tableModel);
        labTable.setFont(UngDungPhongChu.SMALL);
        labTable.setRowHeight(22);
        labTable.setShowGrid(true);
        labTable.setGridColor(new Color(230, 235, 243));
        labTable.setSelectionBackground(new Color(214, 232, 255));
        labTable.setSelectionForeground(new Color(15, 20, 25));

        JTableHeader header = labTable.getTableHeader();
        header.setFont(UngDungPhongChu.SMALL_BOLD);
        header.setBackground(new Color(242, 246, 252));
        header.setForeground(new Color(30, 35, 45));
        header.setPreferredSize(new Dimension(800, 24));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(205, 214, 226)));

        DefaultTableCellRenderer customRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int col) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                Object firstColVal = table.getValueAt(row, 0);
                boolean isGroup = firstColVal != null && firstColVal.toString().startsWith("GROUP:");

                if (isGroup) {
                    lbl.setBackground(new Color(237, 243, 249));
                    if (firstColVal.toString().contains("[Chưa có chỉ định")
                            || firstColVal.toString().contains("[Không có phòng thực hiện]")) {
                        lbl.setForeground(new Color(0, 102, 204));
                    } else {
                        lbl.setForeground(new Color(25, 40, 60));
                    }
                    lbl.setFont(UngDungPhongChu.SMALL_BOLD);
                    if (col == 0) {
                        lbl.setText(" " + firstColVal.toString().substring(6));
                        lbl.setHorizontalAlignment(SwingConstants.LEFT);
                    } else if (col == 4) {
                        lbl.setText("^ ");
                        lbl.setHorizontalAlignment(SwingConstants.RIGHT);
                    } else {
                        lbl.setText("");
                    }
                } else {
                    if (isSelected) {
                        lbl.setBackground(new Color(214, 232, 255));
                        lbl.setForeground(new Color(15, 20, 25));
                    } else {
                        lbl.setBackground(Color.WHITE);
                        lbl.setForeground(new Color(30, 35, 45));
                    }
                    lbl.setFont(UngDungPhongChu.SMALL);
                    if (col == 1) {
                        lbl.setHorizontalAlignment(SwingConstants.CENTER);
                    } else {
                        lbl.setHorizontalAlignment(SwingConstants.LEFT);
                        if (col == 2 && value != null && value.toString().contains("(Chưa thực hiện")) {
                            lbl.setForeground(new Color(120, 125, 135));
                        }
                    }
                }
                return lbl;
            }
        };

        for (int i = 0; i < labTable.getColumnCount(); i++) {
            labTable.getColumnModel().getColumn(i).setCellRenderer(customRenderer);
        }

        labTable.getColumnModel().getColumn(0).setPreferredWidth(520);
        labTable.getColumnModel().getColumn(1).setPreferredWidth(40);
        labTable.getColumnModel().getColumn(1).setMaxWidth(50);
        labTable.getColumnModel().getColumn(2).setPreferredWidth(140);
        labTable.getColumnModel().getColumn(3).setPreferredWidth(60);
        labTable.getColumnModel().getColumn(4).setPreferredWidth(120);

        JScrollPane scroll = new JScrollPane(labTable);
        scroll.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, new Color(205, 214, 226)));
        scroll.getViewport().setBackground(Color.WHITE);

        toastBanner = new JPanel(new BorderLayout());
        toastBanner.setBackground(Color.BLACK);
        lblToast = new JLabel("", SwingConstants.CENTER);
        lblToast.setFont(UngDungPhongChu.BODY_BOLD);
        lblToast.setForeground(Color.WHITE);
        toastBanner.add(lblToast, BorderLayout.CENTER);
        toastBanner.setVisible(false);

        JLayeredPane layeredPane = new JLayeredPane() {
            @Override
            public void doLayout() {
                scroll.setBounds(0, 0, getWidth(), getHeight());
                int bannerY = Math.min(220, Math.max(80, getHeight() / 2 - 14));
                toastBanner.setBounds(0, bannerY, getWidth(), 28);
            }
        };
        layeredPane.add(scroll, JLayeredPane.DEFAULT_LAYER);
        layeredPane.add(toastBanner, JLayeredPane.PALETTE_LAYER);

        panel.add(layeredPane, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildActionFooterPanel() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(Color.WHITE);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(215, 222, 232)));
        footer.setPreferredSize(new Dimension(800, 36));

        JPanel leftBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        leftBox.setBackground(Color.WHITE);
        JButton btnToggle = new JButton("I");
        btnToggle.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnToggle.setPreferredSize(new Dimension(24, 24));
        btnToggle.setMargin(new Insets(0, 0, 0, 0));
        btnToggle.setBackground(Color.WHITE);
        btnToggle.setFocusPainted(false);
        leftBox.add(btnToggle);
        footer.add(leftBox, BorderLayout.WEST);

        rightActionBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        rightActionBox.setBackground(Color.WHITE);
        updateFooterButtons(false);
        footer.add(rightActionBox, BorderLayout.EAST);
        return footer;
    }

    private void updateFooterButtons(boolean canceled) {
        rightActionBox.removeAll();

        JButton btnEditOrder = new JButton(" Chỉ Định / Sửa Phiếu", new EditPencilIcon());
        styleButton(btnEditOrder);
        btnEditOrder.addActionListener(e -> {
            if (onEditOrderCallback != null) {
                onEditOrderCallback.run();
            } else {
                Window win = SwingUtilities.getWindowAncestor(this);
                new DichVuYeuCauHopThoai(win).setVisible(true);
            }
        });
        rightActionBox.add(btnEditOrder);

        JButton btnPrintOrder = new JButton(" Phiếu Chỉ Định (F2)", new PrinterIcon());
        styleButton(btnPrintOrder);
        btnPrintOrder.addActionListener(e -> openClinicalOrderPrintDialog());
        rightActionBox.add(btnPrintOrder);

        JButton btnPrintResult = new JButton(" Phiếu Kết Quả (F3)", new PrinterIcon());
        styleButton(btnPrintResult);
        btnPrintResult.addActionListener(e -> {
            String maPhieu = lblMaPhieu != null && !lblMaPhieu.getText().isBlank()
                    ? lblMaPhieu.getText() : "(Chưa có phiếu)";
            JOptionPane.showMessageDialog(this,
                    "In phiếu kết quả xét nghiệm:\n▶ Mã phiếu: " + maPhieu,
                    "In Phiếu Kết Quả Xét Nghiệm", JOptionPane.INFORMATION_MESSAGE);
        });
        rightActionBox.add(btnPrintResult);

        if (!canceled && currentOrderId > 0) {
            JButton btnCancelSend = new JButton(" Hủy Gửi Phiếu", new RedCrossIcon());
            styleButton(btnCancelSend);
            btnCancelSend.addActionListener(e -> handleCancelCurrentOrder());
            rightActionBox.add(btnCancelSend);
        }

        rightActionBox.revalidate();
        rightActionBox.repaint();
    }

    private void handleCancelCurrentOrder() {
        if (currentOrderId <= 0 && currentGroupOrders.isEmpty()) return;

        int selectedRow = labTable != null ? labTable.getSelectedRow() : -1;
        int selectedOrderId = (selectedRow >= 0 && selectedRow < tableRowOrderIds.size())
                ? tableRowOrderIds.get(selectedRow) : 0;

        boolean multiOrderGroup = currentGroupOrders.size() > 1;
        List<String> options = new ArrayList<>();
        if (multiOrderGroup) {
            options.add("Hủy toàn bộ phiếu (" + currentGroupOrders.size() + " dịch vụ)");
            if (selectedOrderId > 0) {
                options.add("Chỉ hủy dòng đang chọn");
            }
            if (onEditOrderCallback != null) {
                options.add("Hủy & Chọn lại dịch vụ");
            }
        } else {
            options.add("Hủy phiếu");
            if (onEditOrderCallback != null) {
                options.add("Hủy & Chọn lại dịch vụ");
            }
        }
        options.add("Đóng");

        int choice = JOptionPane.showOptionDialog(
                this,
                multiOrderGroup
                        ? "Phiếu xét nghiệm XN-" + currentOrderId + " hiện có " + currentGroupOrders.size()
                        + " dịch vụ. Bác sĩ muốn hủy như thế nào?"
                        : "Bạn có chắc chắn muốn hủy phiếu xét nghiệm XN-" + currentOrderId + " không?",
                "Xác Nhận Hủy Gửi Phiếu Xét Nghiệm",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.WARNING_MESSAGE,
                null,
                options.toArray(),
                options.get(options.size() - 1));

        if (choice < 0 || choice >= options.size() - 1) {
            return;
        }
        String chosenLabel = options.get(choice);
        boolean cancelSingleRow = "Chỉ hủy dòng đang chọn".equals(chosenLabel);
        boolean reopenOrderScreen = "Hủy & Chọn lại dịch vụ".equals(chosenLabel);

        List<Integer> targetOrderIds = resolveTargetOrderIdsForCancel(!cancelSingleRow, selectedRow);
        if (targetOrderIds.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Các dịch vụ được chọn đều đã có kết quả hoặc đã hủy trước đó.",
                    "Không thể hủy phiếu", JOptionPane.WARNING_MESSAGE);
            return;
        }

        PhanQuyenNguCanh auth = KhamBenhPhienNguCanh.current();
        if (auth == null || services == null || services.cancelServiceOrder() == null) {
            JOptionPane.showMessageDialog(this,
                    "Phiên làm việc không hợp lệ hoặc dịch vụ hủy chưa sẵn sàng.",
                    "Không thể hủy phiếu", JOptionPane.ERROR_MESSAGE);
            return;
        }
        KiemToanTacNhanNguCanh actor = createActorContext(auth);
        new SwingWorker<CancelBatchOutcome, Void>() {
            @Override
            protected CancelBatchOutcome doInBackground() {
                int canceledCount = 0;
                List<String> skippedErrors = new ArrayList<>();
                for (int orderId : targetOrderIds) {
                    CancelDichVuYeuCauCommand cmd = new CancelDichVuYeuCauCommand(
                            orderId, "Hủy theo yêu cầu của bác sĩ", auth, actor);
                    KetQua<CancelDichVuYeuCauBienNhan> res = services.cancelServiceOrder().handle(cmd);
                    if (res != null && res.isSuccess()) {
                        canceledCount++;
                    } else {
                        String err = res == null ? "Không nhận được phản hồi"
                                : res.getError().map(e -> e.message()).orElse("Không thể hủy mã #" + orderId);
                        skippedErrors.add("XN-" + orderId + ": " + err);
                    }
                }
                return new CancelBatchOutcome(canceledCount, skippedErrors);
            }

            @Override
            protected void done() {
                try {
                    CancelBatchOutcome outcome = get();
                    if (outcome.canceledCount == 0) {
                        JOptionPane.showMessageDialog(XetNghiemYeuCauTheManHinh.this,
                                outcome.errors.isEmpty() ? "Không thể hủy phiếu." : String.join("\n", outcome.errors),
                                "Không thể hủy phiếu", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    lblToast.setText("Đã hủy " + outcome.canceledCount + " chỉ định xét nghiệm thành công !");
                    toastBanner.setVisible(true);
                    if (!outcome.errors.isEmpty()) {
                        JOptionPane.showMessageDialog(XetNghiemYeuCauTheManHinh.this,
                                "Đã hủy " + outcome.canceledCount + " dịch vụ.\nMột số dịch vụ không thể hủy:\n"
                                        + String.join("\n", outcome.errors),
                                "Thông báo hủy một phần", JOptionPane.WARNING_MESSAGE);
                    }
                    loadServicesForVisit(currentVisitId);
                    if (onOrdersChangedCallback != null) {
                        onOrdersChangedCallback.run();
                    }
                    if (reopenOrderScreen && onEditOrderCallback != null) {
                        onEditOrderCallback.run();
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(XetNghiemYeuCauTheManHinh.this,
                            "Hủy phiếu thất bại. Dữ liệu được giữ nguyên.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    /** Xác định danh sách mã chỉ định (`orderId`) hợp lệ để hủy (bỏ qua dòng đã có kết quả hoặc đã hủy). */
    public List<Integer> resolveTargetOrderIdsForCancel(boolean cancelWholeGroup, int selectedTableRow) {
        if (!cancelWholeGroup) {
            int rowOrderId = (selectedTableRow >= 0 && selectedTableRow < tableRowOrderIds.size())
                    ? tableRowOrderIds.get(selectedTableRow) : 0;
            if (rowOrderId > 0) {
                for (DichVuYeuCauDuLieu o : currentGroupOrders) {
                    if (o.orderId() == rowOrderId && isCancelableStatus(o.status())) {
                        return List.of(rowOrderId);
                    }
                }
            }
            return List.of();
        }
        List<Integer> ids = new ArrayList<>();
        for (DichVuYeuCauDuLieu o : currentGroupOrders) {
            if (o != null && o.orderId() > 0 && isCancelableStatus(o.status())) {
                ids.add(o.orderId());
            }
        }
        if (ids.isEmpty() && currentOrderId > 0 && currentGroupOrders.isEmpty()) {
            ids.add(currentOrderId);
        }
        return ids;
    }

    private static boolean isCancelableStatus(String status) {
        if (status == null || status.isBlank()) {
            return true;
        }
        String norm = status.trim().toUpperCase(Locale.ROOT);
        return !"DA_HUY".equals(norm) && !"DA_CO_KET_QUA".equals(norm);
    }

    private record CancelBatchOutcome(int canceledCount, List<String> errors) {}

    private void styleButton(JButton btn) {
        btn.setFont(UngDungPhongChu.SMALL_BOLD);
        btn.setBackground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(185, 195, 210), 1),
                new EmptyBorder(3, 8, 3, 8)
        ));
    }

    public LamSangYeuCauPrintPreviewManHinh.ClinicalOrderData buildClinicalOrderData() {
        List<DichVuYeuCauDuLieu> sourceOrders = !loadedVisitClsOrders.isEmpty()
                ? loadedVisitClsOrders : loadedLabOrders;
        return LamSangYeuCauPrintPreviewManHinh.fromVisitOrders(patientContext, sourceOrders, null);
    }

    public void openClinicalOrderPrintDialog() {
        Window owner = SwingUtilities.getWindowAncestor(this);
        new LamSangYeuCauPrintPreviewHopThoai(owner, buildClinicalOrderData()).setVisible(true);
    }

    public boolean isOrderCanceled() {
        return isCanceled;
    }

    private void addHeaderRow(JPanel panel, GridBagConstraints c, int row,
                              String l1, JComponent v1, int span1,
                              String l2, JComponent v2, int span2,
                              String l3, JComponent v3, int span3) {
        c.gridy = row;
        int col = 0;

        String[] labels = {l1, l2, l3};
        JComponent[] vals = {v1, v2, v3};
        int[] spans = {span1, span2, span3};

        for (int i = 0; i < 3; i++) {
            if (labels[i] == null || vals[i] == null) continue;
            c.gridx = col++;
            c.gridwidth = 1;
            c.weightx = 0;
            JLabel lbl = new JLabel(labels[i]);
            lbl.setFont(UngDungPhongChu.SMALL);
            lbl.setForeground(new Color(30, 30, 30));
            panel.add(lbl, c);

            c.gridx = col;
            c.gridwidth = spans[i];
            c.weightx = spans[i] >= 3 ? 0.6 : 0.3;
            panel.add(vals[i], c);
            col += spans[i];
        }
    }

    private JLabel createUnderlineValue(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(UngDungPhongChu.SMALL);
        lbl.setForeground(new Color(18, 45, 130));
        lbl.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(185, 195, 210)));
        return lbl;
    }

    public JPanel getSelectedTicketCard() {
        return selectedTicketCard;
    }

    public DefaultTableModel getTableModelForTest() {
        return tableModel;
    }

    public String getHeaderOrderCodeForTest() {
        return lblMaPhieu != null ? lblMaPhieu.getText() : "";
    }

    public int getTicketCardCountForTest() {
        return ticketListPanel != null ? ticketListPanel.getComponentCount() : 0;
    }

    void applyLoadedOrdersForTest(List<DichVuYeuCauDuLieu> orders) {
        applyLoadedOrders(orders);
    }

    // =========================================================================
    // VECTOR ICONS
    // =========================================================================

    public static class PrinterIcon implements Icon {
        @Override public int getIconWidth() { return 16; }
        @Override public int getIconHeight() { return 16; }
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(60, 60, 60));
            g2.fillRect(x + 3, y + 1, 10, 4);
            g2.fillRoundRect(x + 1, y + 4, 14, 7, 2, 2);
            g2.setColor(new Color(230, 230, 230));
            g2.fillRect(x + 3, y + 8, 10, 5);
            g2.setColor(new Color(100, 100, 100));
            g2.drawRect(x + 3, y + 8, 10, 5);
            g2.dispose();
        }
    }

    public static class RedCrossIcon implements Icon {
        @Override public int getIconWidth() { return 14; }
        @Override public int getIconHeight() { return 14; }
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(205, 30, 30));
            g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(x + 2, y + 2, x + 12, y + 12);
            g2.drawLine(x + 12, y + 2, x + 2, y + 12);
            g2.dispose();
        }
    }

    public static class CheckmarkMiniIcon implements Icon {
        @Override public int getIconWidth() { return 14; }
        @Override public int getIconHeight() { return 14; }
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(30, 35, 45));
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(x + 2, y + 7, x + 5, y + 10);
            g2.drawLine(x + 5, y + 10, x + 11, y + 3);
            g2.dispose();
        }
    }

    public static class HistoryClockIcon implements Icon {
        @Override public int getIconWidth() { return 15; }
        @Override public int getIconHeight() { return 15; }
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(0, 110, 200));
            g2.drawOval(x + 1, y + 1, 12, 12);
            g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(x + 7, y + 4, x + 7, y + 7);
            g2.drawLine(x + 7, y + 7, x + 10, y + 7);
            g2.dispose();
        }
    }

    public static class BarChartIcon implements Icon {
        @Override public int getIconWidth() { return 16; }
        @Override public int getIconHeight() { return 14; }
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(245, 175, 35));
            g2.fillRect(x + 1, y + 6, 3, 7);
            g2.setColor(new Color(30, 130, 220));
            g2.fillRect(x + 6, y + 2, 3, 11);
            g2.setColor(new Color(230, 50, 50));
            g2.fillRect(x + 11, y + 4, 3, 9);
            g2.dispose();
        }
    }

    public static class EditPencilIcon implements Icon {
        @Override public int getIconWidth() { return 15; }
        @Override public int getIconHeight() { return 15; }
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(0, 102, 204));
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(x + 3, y + 11, x + 10, y + 4);
            g2.drawLine(x + 4, y + 12, x + 11, y + 5);
            g2.drawLine(x + 2, y + 13, x + 4, y + 12);
            g2.drawLine(x + 2, y + 13, x + 3, y + 11);
            g2.drawLine(x + 10, y + 4, x + 12, y + 2);
            g2.drawLine(x + 11, y + 5, x + 13, y + 3);
            g2.dispose();
        }
    }

    public static class CheckmarkSendIcon implements Icon {
        @Override public int getIconWidth() { return 15; }
        @Override public int getIconHeight() { return 15; }
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(40, 150, 70));
            g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(x + 2, y + 8, x + 6, y + 12);
            g2.drawLine(x + 6, y + 12, x + 13, y + 3);
            g2.dispose();
        }
    }
}
