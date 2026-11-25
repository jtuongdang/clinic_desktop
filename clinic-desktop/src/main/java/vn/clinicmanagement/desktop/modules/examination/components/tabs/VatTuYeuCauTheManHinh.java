package vn.clinicmanagement.desktop.modules.examination.components.tabs;

import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.KetQua;
import vn.clinicmanagement.application.prescription.cancel.CancelDonThuocBienNhan;
import vn.clinicmanagement.application.prescription.cancel.CancelDonThuocCommand;
import vn.clinicmanagement.application.prescription.DonThuocDuLieu;
import vn.clinicmanagement.application.prescription.DonThuocMatHangDuLieu;
import vn.clinicmanagement.application.prescription.get.GetDonThuocTheoLuotKhamQuery;
import vn.clinicmanagement.desktop.core.config.UngDungPhongChu;
import vn.clinicmanagement.desktop.modules.examination.KhamBenhGhepNoiGoc;
import vn.clinicmanagement.desktop.modules.examination.KhamBenhManHinh.AdminPatientItem;
import vn.clinicmanagement.desktop.modules.examination.KhamBenhPhienNguCanh;
import vn.clinicmanagement.domain.common.enums.TrangThaiDonThuoc;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.IntConsumer;

/**
 * Tab "Vật Tư(N)" trong phân hệ Khám Bệnh:
 * nạp danh sách phiếu chỉ định vật tư y tế (`VAT_TU`) thật của lượt khám từ CSDL `clinicDb`.
 */
public class VatTuYeuCauTheManHinh extends JPanel {

    private static final DateTimeFormatter TICKET_DT_FMT = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
    private static final Color SELECTED_CARD_BG = new Color(18, 55, 125);

    private final KhamBenhGhepNoiGoc.Services services;
    private final List<DonThuocDuLieu> loadedSupplyOrders = new ArrayList<>();
    private final AtomicLong loadGeneration = new AtomicLong(0);

    private JTable suppliesTable;
    private DefaultTableModel tableModel;
    private JPanel ticketListPanel;
    private JPanel selectedTicketCard;

    private JLabel lblNgayYLenh;
    private JLabel lblNgayChiDinh;
    private JLabel lblMaPhieu;
    private JLabel lblTrangThai;
    private JLabel lblKhoa;
    private JLabel lblLoaiPhieu;
    private JLabel lblNguoiChiDinh;
    private JLabel lblNoiChiDinh;
    private JLabel lblChanDoan;

    private JButton btnPrint;
    private JButton btnCancel;

    private AdminPatientItem patient;
    private DonThuocDuLieu selectedSupplyOrder;
    private IntConsumer onSupplyCountChanged;
    private Runnable onSuppliesChangedCallback;

    private int currentVisitId = 0;
    private int currentDoctorId = 0;
    private int currentSupplyOrderId = 0;
    private String patientCode = "";
    private String patientName = "";
    private String currentTicketCode = "";
    private boolean isCancelled = false;
    private boolean suppressDialogs = false;

    public VatTuYeuCauTheManHinh() {
        this((AdminPatientItem) null, null);
    }

    public VatTuYeuCauTheManHinh(AdminPatientItem patient) {
        this(patient, null);
    }

    public VatTuYeuCauTheManHinh(KhamBenhGhepNoiGoc.Services services) {
        this(null, services);
    }

    public VatTuYeuCauTheManHinh(AdminPatientItem patient, KhamBenhGhepNoiGoc.Services services) {
        this.services = services;
        this.patient = patient;
        if (patient != null) {
            this.patientName = safeText(patient.name);
            this.patientCode = safeText(patient.code);
        }
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        add(buildTicketSelectorPanel(), BorderLayout.EAST);
        add(buildTicketDetailPanel(), BorderLayout.CENTER);

        if (patient != null) {
            applyPatientData(patient);
        } else {
            renderEmptySupplyState();
        }
    }

    public void setOnSupplyCountChanged(IntConsumer listener) {
        this.onSupplyCountChanged = listener;
    }

    public void setOnSuppliesChangedCallback(Runnable callback) {
        this.onSuppliesChangedCallback = callback;
    }

    public void setPatient(AdminPatientItem patient) {
        this.patient = patient;
        if (patient != null) {
            this.patientName = safeText(patient.name);
            this.patientCode = safeText(patient.code);
            applyPatientData(patient);
        }
    }

    public void setVisitContext(int visitId, int doctorId) {
        this.currentVisitId = Math.max(visitId, 0);
        this.currentDoctorId = Math.max(doctorId, 0);
        loadSuppliesForVisit(this.currentVisitId);
    }

    public void loadSuppliesForVisit(int visitId) {
        final long generation = loadGeneration.incrementAndGet();
        if (services == null || services.getPrescriptionByVisit() == null || visitId <= 0) {
            applyLoadedSupplyOrders(List.of());
            return;
        }
        PhanQuyenNguCanh auth = KhamBenhPhienNguCanh.current();
        if (auth == null || !auth.isValid()) {
            applyLoadedSupplyOrders(List.of());
            return;
        }

        SwingWorker<KetQua<List<DonThuocDuLieu>>, Void> worker = new SwingWorker<>() {
            @Override
            protected KetQua<List<DonThuocDuLieu>> doInBackground() {
                return services.getPrescriptionByVisit().handle(new GetDonThuocTheoLuotKhamQuery(visitId, auth));
            }

            @Override
            protected void done() {
                if (generation != loadGeneration.get()) return;
                try {
                    KetQua<List<DonThuocDuLieu>> res = get();
                    if (res != null && res.isSuccess()) {
                        applyLoadedSupplyOrders(res.getValue().orElse(List.of()));
                    } else {
                        applyLoadedSupplyOrders(List.of());
                    }
                } catch (Exception ex) {
                    applyLoadedSupplyOrders(List.of());
                }
            }
        };
        worker.execute();
    }

    public void applyLoadedSupplyOrders(List<DonThuocDuLieu> rawList) {
        loadedSupplyOrders.clear();
        if (rawList != null) {
            for (DonThuocDuLieu rx : rawList) {
                if (rx != null && rx.trangThai() != TrangThaiDonThuoc.HUY && rx.isSupplyOrder()) {
                    loadedSupplyOrders.add(rx);
                }
            }
        }
        int itemTotal = 0;
        for (DonThuocDuLieu rx : loadedSupplyOrders) {
            if (rx.items() != null) {
                itemTotal += rx.items().size();
            }
        }
        if (onSupplyCountChanged != null) {
            onSupplyCountChanged.accept(itemTotal);
        }

        if (ticketListPanel != null) {
            ticketListPanel.removeAll();
            selectedTicketCard = null;
        }
        if (loadedSupplyOrders.isEmpty()) {
            renderEmptySupplyState();
            if (ticketListPanel != null) {
                JLabel emptyLbl = new JLabel("Chưa có phiếu vật tư");
                emptyLbl.setFont(UngDungPhongChu.SMALL);
                emptyLbl.setForeground(Color.GRAY);
                emptyLbl.setBorder(new EmptyBorder(12, 10, 8, 10));
                ticketListPanel.add(emptyLbl);
                ticketListPanel.revalidate();
                ticketListPanel.repaint();
            }
            return;
        }

        for (int i = 0; i < loadedSupplyOrders.size(); i++) {
            DonThuocDuLieu rx = loadedSupplyOrders.get(i);
            String ticketCode = "VT-" + rx.id();
            String timeText = rx.ngayKe() != null ? rx.ngayKe().format(TICKET_DT_FMT) : LocalDateTime.now().format(TICKET_DT_FMT);
            String subtitle = "(Khám bệnh) Lúc " + timeText;
            JPanel card = createTicketCard(rx, ticketCode, subtitle, i == 0);
            if (i == 0) {
                selectedTicketCard = card;
            }
            if (ticketListPanel != null) {
                ticketListPanel.add(card);
            }
        }
        if (ticketListPanel != null) {
            ticketListPanel.revalidate();
            ticketListPanel.repaint();
        }
        displaySupplyOrderDetail(loadedSupplyOrders.get(0));
    }

    private void renderEmptySupplyState() {
        this.selectedSupplyOrder = null;
        this.currentSupplyOrderId = 0;
        this.currentTicketCode = "";
        this.isCancelled = false;
        if (lblMaPhieu != null) lblMaPhieu.setText("");
        if (lblNgayYLenh != null) lblNgayYLenh.setText("");
        if (lblNgayChiDinh != null) lblNgayChiDinh.setText("");
        if (lblTrangThai != null) lblTrangThai.setText("Chưa có phiếu vật tư");
        if (tableModel != null) {
            tableModel.setRowCount(0);
            padEmptyRows(18);
        }
        applyPatientData(patient);
    }

    private void displaySupplyOrderDetail(DonThuocDuLieu rx) {
        if (rx == null) {
            renderEmptySupplyState();
            return;
        }
        this.selectedSupplyOrder = rx;
        this.currentSupplyOrderId = rx.id();
        this.currentTicketCode = "VT-" + rx.id();
        this.isCancelled = rx.trangThai() == TrangThaiDonThuoc.HUY;

        String timeText = rx.ngayKe() != null ? rx.ngayKe().format(TICKET_DT_FMT) : "";
        if (lblLoaiPhieu != null) lblLoaiPhieu.setText("Phiếu chỉ định vật tư");
        if (lblMaPhieu != null) lblMaPhieu.setText(currentTicketCode);
        if (lblNgayYLenh != null) lblNgayYLenh.setText(timeText);
        if (lblNgayChiDinh != null) lblNgayChiDinh.setText(timeText);
        if (lblTrangThai != null) lblTrangThai.setText(formatStatus(rx.trangThai()));
        if (lblNguoiChiDinh != null && rx.doctorName() != null && !rx.doctorName().isBlank()) {
            lblNguoiChiDinh.setText(rx.doctorName());
        } else {
            applyPatientData(patient);
        }

        if (tableModel != null) {
            tableModel.setRowCount(0);
            int stt = 1;
            if (rx.items() != null) {
                for (DonThuocMatHangDuLieu item : rx.items()) {
                    String qty = item.soLuongKe() == null ? "0" : item.soLuongKe().stripTrailingZeros().toPlainString();
                    tableModel.addRow(new Object[]{
                            String.valueOf(stt++),
                            safeText(item.itemCode()),
                            safeText(item.itemName()),
                            safeText(item.donViTinh()),
                            qty
                    });
                }
            }
            padEmptyRows(18);
        }
    }

    private void applyPatientData(AdminPatientItem p) {
        if (p == null) {
            if (lblChanDoan != null) lblChanDoan.setText("");
            if (lblNoiChiDinh != null) lblNoiChiDinh.setText("");
            if (lblNguoiChiDinh != null) lblNguoiChiDinh.setText("");
            if (lblKhoa != null) lblKhoa.setText("Khoa Khám Bệnh");
            return;
        }
        if (lblChanDoan != null) lblChanDoan.setText(safeText(p.diagnosis));
        if (lblNoiChiDinh != null) {
            String room = safeText(p.clinicRoomName).isBlank() ? safeText(p.department) : safeText(p.clinicRoomName);
            lblNoiChiDinh.setText(room);
        }
        if (lblNguoiChiDinh != null && (selectedSupplyOrder == null || selectedSupplyOrder.doctorName() == null || selectedSupplyOrder.doctorName().isBlank())) {
            lblNguoiChiDinh.setText(safeText(p.doctor));
        }
        if (lblKhoa != null) {
            String dept = safeText(p.departmentName).isBlank() ? safeText(p.department) : safeText(p.departmentName);
            lblKhoa.setText(dept.isBlank() ? "Khoa Khám Bệnh" : dept);
        }
    }

    public void setSupplies(List<Object[]> suppliesList) {
        if (tableModel == null) return;
        tableModel.setRowCount(0);
        int count = 0;
        if (suppliesList != null && !suppliesList.isEmpty()) {
            int stt = 1;
            for (Object[] item : suppliesList) {
                if (item.length >= 5) {
                    tableModel.addRow(new Object[]{String.valueOf(stt++), item[1], item[2], item[3], item[4]});
                    count++;
                } else if (item.length == 4) {
                    tableModel.addRow(new Object[]{String.valueOf(stt++), item[0], item[1], item[2], item[3]});
                    count++;
                } else if (item.length > 0) {
                    tableModel.addRow(item);
                    count++;
                }
            }
        }
        padEmptyRows(18);
        this.isCancelled = false;
        if (lblTrangThai != null) {
            lblTrangThai.setText(count > 0 ? "Lưu tạm" : "Chưa có phiếu vật tư");
        }
        if (onSupplyCountChanged != null) {
            onSupplyCountChanged.accept(count);
        }
    }

    private void padEmptyRows(int minRows) {
        if (tableModel == null) return;
        for (int i = tableModel.getRowCount(); i < minRows; i++) {
            tableModel.addRow(new Object[]{"", "", "", "", ""});
        }
    }

    private static String formatStatus(TrangThaiDonThuoc status) {
        if (status == null) return "Lưu tạm";
        return switch (status) {
            case TAM -> "Lưu tạm";
            case DA_DUYET -> "Đã duyệt";
            case DA_XUAT_HET -> "Đã xuất hết";
            case HUY -> "Đã hủy";
        };
    }

    public void setSuppressDialogs(boolean suppressDialogs) {
        this.suppressDialogs = suppressDialogs;
    }

    public boolean isSuppressDialogs() {
        return suppressDialogs;
    }

    private boolean isModalDialogAllowed() {
        if (suppressDialogs || GraphicsEnvironment.isHeadless()) return false;
        String surefire = System.getProperty("surefire.test.class.path");
        return surefire == null || surefire.isBlank();
    }

    public void executePrint() {
        StringBuilder itemsSummary = new StringBuilder();
        int count = 0;
        if (tableModel != null) {
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                Object nameObj = tableModel.getValueAt(i, 2);
                Object qtyObj = tableModel.getValueAt(i, 4);
                if (nameObj != null && !nameObj.toString().trim().isEmpty()) {
                    if (count > 0) itemsSummary.append(", ");
                    itemsSummary.append(nameObj.toString().trim());
                    if (qtyObj != null && !qtyObj.toString().trim().isEmpty()) {
                        itemsSummary.append(" (SL: ").append(qtyObj.toString().trim()).append(")");
                    }
                    count++;
                }
            }
        }
        if (count == 0) {
            if (isModalDialogAllowed()) {
                JOptionPane.showMessageDialog(this, "Chưa có vật tư nào để in phiếu.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            }
            return;
        }

        String ticketLabel = currentTicketCode.isBlank() ? "Phiếu vật tư" : currentTicketCode;
        String msg = "Đang xuất lệnh in phiếu chỉ định vật tư:\n" +
                "▶ Mã phiếu: " + ticketLabel + "\n" +
                "▶ Bệnh nhân: " + patientName + " | " + patientCode + "\n" +
                "▶ Vật tư: " + itemsSummary;

        if (isModalDialogAllowed()) {
            JOptionPane.showMessageDialog(this, msg, "In Phiếu Chỉ Định Vật Tư", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    public void executeCancel() {
        if (services != null && services.cancelPrescription() != null) {
            if (currentSupplyOrderId <= 0) {
                if (isModalDialogAllowed()) {
                    JOptionPane.showMessageDialog(this,
                            "Chưa có phiếu vật tư hợp lệ để hủy.",
                            "Không thể hủy phiếu", JOptionPane.WARNING_MESSAGE);
                }
                return;
            }
            if (isModalDialogAllowed()) {
                int confirm = JOptionPane.showConfirmDialog(this,
                        "Bạn có chắc chắn muốn hủy phiếu vật tư " + currentTicketCode + "?",
                        "Xác Nhận Hủy Gửi Phiếu", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (confirm != JOptionPane.YES_OPTION) return;
            }
            PhanQuyenNguCanh auth = KhamBenhPhienNguCanh.current();
            if (auth == null || !auth.isValid()) {
                if (isModalDialogAllowed()) {
                    JOptionPane.showMessageDialog(this,
                            "Phiên đăng nhập chưa có quyền bác sĩ để hủy phiếu vật tư.",
                            "Không thể hủy phiếu", JOptionPane.ERROR_MESSAGE);
                }
                return;
            }
            int cancelDoctorId = auth.employeeId() > 0 ? auth.employeeId() : currentDoctorId;
            int orderIdToCancel = currentSupplyOrderId;
            CancelDonThuocCommand cmd = new CancelDonThuocCommand(
                    orderIdToCancel,
                    cancelDoctorId,
                    "Hủy chỉ định vật tư theo yêu cầu của bác sĩ khám",
                    auth);
            SwingWorker<KetQua<CancelDonThuocBienNhan>, Void> worker = new SwingWorker<>() {
                @Override
                protected KetQua<CancelDonThuocBienNhan> doInBackground() {
                    return services.cancelPrescription().handle(cmd);
                }

                @Override
                protected void done() {
                    try {
                        KetQua<CancelDonThuocBienNhan> res = get();
                        if (res != null && res.isSuccess()) {
                            isCancelled = true;
                            loadSuppliesForVisit(currentVisitId);
                            if (onSuppliesChangedCallback != null) {
                                onSuppliesChangedCallback.run();
                            }
                            if (isModalDialogAllowed()) {
                                JOptionPane.showMessageDialog(VatTuYeuCauTheManHinh.this,
                                        "Đã hủy phiếu vật tư VT-" + orderIdToCancel + " thành công trên CSDL!",
                                        "Thông Báo", JOptionPane.INFORMATION_MESSAGE);
                            }
                        } else if (isModalDialogAllowed()) {
                            String err = res == null ? "Không nhận được kết quả hủy."
                                    : res.getError().map(e -> e.message()).orElse("Không thể hủy phiếu vật tư.");
                            JOptionPane.showMessageDialog(VatTuYeuCauTheManHinh.this, err,
                                    "Không thể hủy phiếu", JOptionPane.ERROR_MESSAGE);
                        }
                    } catch (Exception ex) {
                        if (isModalDialogAllowed()) {
                            JOptionPane.showMessageDialog(VatTuYeuCauTheManHinh.this,
                                    "Hủy phiếu vật tư thất bại: " + ex.getMessage(),
                                    "Lỗi", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }
            };
            worker.execute();
            return;
        }

        if (isModalDialogAllowed()) {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Bạn có chắc chắn muốn hủy phiếu chỉ định vật tư " + currentTicketCode + "?",
                    "Xác Nhận Hủy Gửi Phiếu", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm != JOptionPane.YES_OPTION) return;
        }

        this.isCancelled = true;
        if (lblTrangThai != null) {
            lblTrangThai.setText("ĐÃ HỦY PHIẾU [" + currentTicketCode + "]");
        }
        if (tableModel != null) {
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                Object nameObj = tableModel.getValueAt(i, 2);
                if (nameObj != null && !nameObj.toString().trim().isEmpty()) {
                    String str = nameObj.toString().trim();
                    if (!str.contains("[Đã hủy]")) {
                        tableModel.setValueAt(str + " [Đã hủy]", i, 2);
                    }
                }
            }
        }
        if (isModalDialogAllowed()) {
            JOptionPane.showMessageDialog(this, "Đã hủy phiếu vật tư thành công!", "Thông Báo", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private JPanel buildTicketSelectorPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setPreferredSize(new Dimension(215, 500));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, new Color(205, 214, 226)));

        JPanel searchBar = new JPanel(new BorderLayout(4, 0));
        searchBar.setBackground(new Color(255, 255, 208));
        searchBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(205, 212, 195)),
                new EmptyBorder(2, 6, 2, 6)
        ));
        searchBar.setPreferredSize(new Dimension(215, 24));

        JLabel lblSearch = new JLabel("Danh sách phiếu vật tư");
        lblSearch.setFont(UngDungPhongChu.SMALL);
        lblSearch.setForeground(new Color(40, 40, 40));
        searchBar.add(lblSearch, BorderLayout.WEST);
        panel.add(searchBar, BorderLayout.NORTH);

        ticketListPanel = new JPanel();
        ticketListPanel.setLayout(new BoxLayout(ticketListPanel, BoxLayout.Y_AXIS));
        ticketListPanel.setBackground(Color.WHITE);
        panel.add(new JScrollPane(ticketListPanel), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createTicketCard(DonThuocDuLieu rx, String ticketCode, String subtitle, boolean isSelected) {
        JPanel card = new JPanel(new BorderLayout(4, 2));
        card.setMaximumSize(new Dimension(215, 46));
        card.setPreferredSize(new Dimension(215, 46));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(215, 222, 232)),
                new EmptyBorder(4, 8, 4, 8)
        ));
        card.setCursor(new Cursor(Cursor.HAND_CURSOR));
        styleTicketCard(card, isSelected);

        JLabel l1 = new JLabel(ticketCode);
        l1.setFont(UngDungPhongChu.SMALL_BOLD);
        l1.setForeground(isSelected ? Color.WHITE : new Color(30, 30, 30));

        JLabel l2 = new JLabel(subtitle);
        l2.setFont(UngDungPhongChu.TINY);
        l2.setForeground(isSelected ? new Color(220, 235, 255) : new Color(110, 110, 110));

        card.add(l1, BorderLayout.NORTH);
        card.add(l2, BorderLayout.SOUTH);

        MouseAdapter clickAdapter = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    if (selectedTicketCard != null) {
                        styleTicketCard(selectedTicketCard, false);
                    }
                    selectedTicketCard = card;
                    styleTicketCard(card, true);
                    displaySupplyOrderDetail(rx);
                }
            }
        };
        card.addMouseListener(clickAdapter);
        l1.addMouseListener(clickAdapter);
        l2.addMouseListener(clickAdapter);
        return card;
    }

    private void styleTicketCard(JPanel card, boolean selected) {
        card.setBackground(selected ? SELECTED_CARD_BG : Color.WHITE);
        for (Component comp : card.getComponents()) {
            if (comp instanceof JLabel lbl) {
                if (lbl.getFont().isBold()) {
                    lbl.setForeground(selected ? Color.WHITE : new Color(30, 30, 30));
                } else {
                    lbl.setForeground(selected ? new Color(220, 235, 255) : new Color(110, 110, 110));
                }
            }
        }
    }

    public JPanel getSelectedTicketCard() {
        return selectedTicketCard;
    }

    private JPanel buildTicketDetailPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);

        JPanel headerForm = buildTicketHeaderForm();
        panel.add(headerForm, BorderLayout.NORTH);

        String[] cols = {"STT", "Mã Vật Tư", "Tên Vật Tư", "Đơn Vị", "Số Lượng"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        padEmptyRows(18);

        suppliesTable = new JTable(tableModel);
        suppliesTable.setRowHeight(21);
        suppliesTable.setFont(UngDungPhongChu.BODY);
        suppliesTable.setShowGrid(true);
        suppliesTable.setGridColor(new Color(230, 235, 242));
        suppliesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        suppliesTable.setBackground(Color.WHITE);

        JTableHeader th = suppliesTable.getTableHeader();
        th.setFont(UngDungPhongChu.SMALL_BOLD);
        th.setPreferredSize(new Dimension(800, 24));
        th.setBackground(new Color(242, 246, 252));
        th.setForeground(new Color(25, 30, 40));
        th.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(205, 214, 226)));

        DefaultTableCellRenderer centerRender = new DefaultTableCellRenderer();
        centerRender.setHorizontalAlignment(SwingConstants.CENTER);

        suppliesTable.getColumnModel().getColumn(0).setPreferredWidth(45);
        suppliesTable.getColumnModel().getColumn(0).setMaxWidth(50);
        suppliesTable.getColumnModel().getColumn(0).setCellRenderer(centerRender);

        suppliesTable.getColumnModel().getColumn(1).setPreferredWidth(95);
        suppliesTable.getColumnModel().getColumn(1).setMaxWidth(110);

        suppliesTable.getColumnModel().getColumn(2).setPreferredWidth(340);

        suppliesTable.getColumnModel().getColumn(3).setPreferredWidth(65);
        suppliesTable.getColumnModel().getColumn(3).setMaxWidth(75);
        suppliesTable.getColumnModel().getColumn(3).setCellRenderer(centerRender);

        suppliesTable.getColumnModel().getColumn(4).setPreferredWidth(65);
        suppliesTable.getColumnModel().getColumn(4).setMaxWidth(75);
        suppliesTable.getColumnModel().getColumn(4).setCellRenderer(centerRender);

        JScrollPane scrollPane = new JScrollPane(suppliesTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(Color.WHITE);
        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel actionFooter = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 6));
        actionFooter.setBackground(new Color(248, 250, 253));
        actionFooter.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(210, 218, 228)));
        actionFooter.setPreferredSize(new Dimension(800, 38));

        btnPrint = new JButton(" In Phiếu (F3)", new PrinterIcon());
        btnPrint.setFont(UngDungPhongChu.BODY_BOLD);
        btnPrint.setForeground(new Color(25, 25, 25));
        btnPrint.setBackground(Color.WHITE);
        btnPrint.setFocusPainted(false);
        btnPrint.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnPrint.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(185, 195, 208), 1),
                new EmptyBorder(3, 10, 3, 10)
        ));
        btnPrint.addActionListener(e -> executePrint());

        btnCancel = new JButton(" Hủy Gửi Phiếu", new RedCrossIcon());
        btnCancel.setFont(UngDungPhongChu.BODY_BOLD);
        btnCancel.setForeground(new Color(185, 20, 20));
        btnCancel.setBackground(Color.WHITE);
        btnCancel.setFocusPainted(false);
        btnCancel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnCancel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(185, 195, 208), 1),
                new EmptyBorder(3, 10, 3, 10)
        ));
        btnCancel.addActionListener(e -> executeCancel());

        actionFooter.add(btnPrint);
        actionFooter.add(btnCancel);

        panel.add(actionFooter, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildTicketHeaderForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(new EmptyBorder(6, 8, 8, 8));

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(2, 4, 2, 4);

        int r = 0;

        lblLoaiPhieu = createUnderlineValue("Phiếu chỉ định vật tư");
        lblNgayYLenh = createUnderlineValue("");
        lblNgayChiDinh = createUnderlineValue("");
        addHeaderRow(form, c, r++,
                "Loại phiếu:", lblLoaiPhieu, 1,
                "Ngày y lệnh:", lblNgayYLenh, 1,
                "Ngày chỉ định:", lblNgayChiDinh, 1);

        lblMaPhieu = createUnderlineValue("");
        lblNoiChiDinh = createUnderlineValue("");
        addHeaderRow(form, c, r++,
                "Mã phiếu:", lblMaPhieu, 1,
                "Nơi chỉ định:", lblNoiChiDinh, 3,
                null, null, 0);

        lblNguoiChiDinh = createUnderlineValue("");
        lblTrangThai = createUnderlineValue("Chưa có phiếu vật tư");
        addHeaderRow(form, c, r++,
                "Người chỉ định :", lblNguoiChiDinh, 1,
                "Trạng thái:", lblTrangThai, 3,
                null, null, 0);

        lblKhoa = createUnderlineValue("");
        lblChanDoan = createUnderlineValue("");
        addHeaderRow(form, c, r,
                "Khoa:", lblKhoa, 1,
                "Chẩn đoán:", lblChanDoan, 3,
                null, null, 0);

        return form;
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

    private static String safeText(String val) {
        return val == null ? "" : val;
    }

    public JButton getBtnPrint() { return btnPrint; }
    public JButton getBtnCancel() { return btnCancel; }
    public JTable getSuppliesTable() { return suppliesTable; }
    public DefaultTableModel getTableModel() { return tableModel; }
    public JLabel getLblTrangThai() { return lblTrangThai; }
    public JLabel getLblMaPhieu() { return lblMaPhieu; }
    public JLabel getLblChanDoan() { return lblChanDoan; }
    public AdminPatientItem getPatient() { return patient; }
    public boolean isCancelled() { return isCancelled; }
    public String getCurrentTicketCode() { return currentTicketCode; }

    private static class PrinterIcon implements Icon {
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

    private static class RedCrossIcon implements Icon {
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
}
