package vn.clinicmanagement.desktop.modules.examination.components.tabs;

import vn.clinicmanagement.desktop.modules.examination.components.print.DonThuocPrintPreviewHopThoai;
import vn.clinicmanagement.desktop.modules.examination.components.print.DonThuocPrintPreviewManHinh;
import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.KetQua;
import vn.clinicmanagement.application.prescription.cancel.CancelDonThuocCommand;
import vn.clinicmanagement.application.prescription.cancel.CancelDonThuocBienNhan;
import vn.clinicmanagement.application.prescription.get.GetDonThuocTheoLuotKhamQuery;
import vn.clinicmanagement.application.prescription.DonThuocDuLieu;
import vn.clinicmanagement.application.prescription.DonThuocMatHangDuLieu;
import vn.clinicmanagement.desktop.modules.examination.KhamBenhGhepNoiGoc;
import vn.clinicmanagement.desktop.modules.examination.KhamBenhManHinh.AdminPatientItem;
import vn.clinicmanagement.desktop.modules.examination.KhamBenhPhienNguCanh;
import vn.clinicmanagement.domain.common.enums.TrangThaiDonThuoc;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.IntConsumer;

import vn.clinicmanagement.desktop.core.config.UngDungPhongChu;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Tab "Thuốc(1)" trong phân hệ Khám Bệnh.
 * Hiển thị chi tiết đơn thuốc chỉ định sau khi nhấn "Lưu + Đóng" từ màn hình kê đơn.
 * Khớp 100% giao diện trong ảnh chụp mẫu thực tế:
 * - Cột phải: Khung tìm kiếm màu vàng nhạt + Thẻ phiếu TH220809.1 (màu xanh dương đậm)
 * - Khung chính:
 *   + Tiêu đề phiếu: Loại phiếu, Mã phiếu, Người chỉ định, Khoa, Ngày y lệnh, Ngày chỉ định, Nơi chỉ định, Trạng thái (Chờ xuất/nhập), Chẩn đoán (H20-Viêm màng bồ đào)
 *   + Bảng thuốc chi tiết: STT, Tên Thuốc, Mã Thuốc, Đơn Vị, Kê, Liều, Đường Dùng, Hướng Dẫn Sử Dụng (dòng 1: Attapulgite... | X191.DK36 | Gói | Liều: 10 | Uống | uống ngày 1 lần)
 *   + Cụm nút chân trang: [🖨 Phiếu Truyền HC (F2)], [🖨 Đơn Thuốc (F3)] và [❌ Hủy Gửi Đơn Thuốc]
 */
public class DonThuocYeuCauTheManHinh extends JPanel {

    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final KhamBenhGhepNoiGoc.Services services;
    private final AtomicLong loadSequence = new AtomicLong();
    private final List<DonThuocDuLieu> loadedPrescriptions = new ArrayList<>();
    private AdminPatientItem patientContext;
    private DonThuocDuLieu selectedPrescription;
    private IntConsumer onPrescriptionCountChanged;
    private Runnable onPrescriptionsChangedCallback;

    private int currentVisitId = 0;
    private int currentDoctorId = 0;
    private int currentPrescriptionId = 0;

    private JTable prescriptionTable;
    private DefaultTableModel tableModel;
    private JPanel ticketListPanel;
    private JLabel lblNgayYLenh;
    private JLabel lblNgayChiDinh;
    private JLabel lblMaPhieu;
    private JLabel lblTrangThai;
    private JLabel lblKhoa;
    private JLabel lblNguoiChiDinh;
    private JLabel lblNoiChiDinh;
    private JLabel lblChanDoan;
    private JPanel selectedTicketCard;

    public DonThuocYeuCauTheManHinh() {
        this(resolveDefaultServices());
    }

    public DonThuocYeuCauTheManHinh(KhamBenhGhepNoiGoc.Services services) {
        this.services = services != null ? services : resolveDefaultServices();
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        // 1. CỘT PHẢI: KHUNG CHỌN PHIẾU THUỐC (TÌM KIẾM + DANH SÁCH THẺ PHIẾU THUỐC)
        add(buildTicketSelectorPanel(), BorderLayout.EAST);

        // 2. KHUNG TRUNG TÂM: CHI TIẾT ĐƠN THUỐC (THÔNG TIN HÀNH CHÍNH + BẢNG THUỐC + CỤM NÚT IN)
        add(buildTicketDetailPanel(), BorderLayout.CENTER);
    }

    private static KhamBenhGhepNoiGoc.Services resolveDefaultServices() {
        try {
            return KhamBenhGhepNoiGoc.createDefault();
        } catch (Exception ex) {
            return null;
        }
    }

    public void setOnPrescriptionCountChanged(IntConsumer listener) {
        this.onPrescriptionCountChanged = listener;
    }

    public void setOnPrescriptionsChangedCallback(Runnable callback) {
        this.onPrescriptionsChangedCallback = callback;
    }

    public void setPatientContext(AdminPatientItem patient) {
        this.patientContext = patient;
        if (lblNguoiChiDinh != null) {
            lblNguoiChiDinh.setText(patient != null && patient.doctor != null ? patient.doctor : "");
        }
        if (lblNoiChiDinh != null) {
            lblNoiChiDinh.setText(patient != null && patient.clinicRoomName != null && !patient.clinicRoomName.isBlank()
                    ? patient.clinicRoomName : (patient != null ? patient.departmentName : ""));
        }
        if (lblKhoa != null) {
            lblKhoa.setText(patient != null && patient.departmentName != null ? patient.departmentName : "");
        }
        if (lblChanDoan != null) {
            lblChanDoan.setText(patient != null && patient.diagnosis != null ? patient.diagnosis : "");
        }
    }

    public void setVisitContext(int visitId, int doctorId) {
        this.currentVisitId = visitId;
        this.currentDoctorId = doctorId;
        loadPrescriptionsForVisit(visitId);
    }

    public void loadPrescriptionsForVisit(int visitId) {
        long seq = loadSequence.incrementAndGet();
        if (services == null || services.getPrescriptionByVisit() == null || visitId <= 0) {
            applyLoadedPrescriptions(List.of());
            return;
        }
        PhanQuyenNguCanh auth = KhamBenhPhienNguCanh.current();
        if (auth == null) {
            applyLoadedPrescriptions(List.of());
            return;
        }
        SwingWorker<KetQua<List<DonThuocDuLieu>>, Void> worker = new SwingWorker<>() {
            @Override
            protected KetQua<List<DonThuocDuLieu>> doInBackground() {
                return services.getPrescriptionByVisit().handle(new GetDonThuocTheoLuotKhamQuery(visitId, auth));
            }

            @Override
            protected void done() {
                if (seq != loadSequence.get() || visitId != currentVisitId) {
                    return;
                }
                try {
                    KetQua<List<DonThuocDuLieu>> res = get();
                    if (res != null && res.isSuccess()) {
                        applyLoadedPrescriptions(res.getValue().orElse(List.of()));
                    } else {
                        applyLoadedPrescriptions(List.of());
                    }
                } catch (Exception ignored) {
                    applyLoadedPrescriptions(List.of());
                }
            }
        };
        worker.execute();
    }

    /** Đổ danh sách đơn thuốc thật lên bảng và cột thẻ bên phải (hỗ trợ cả unit test). */
    public void applyLoadedPrescriptions(List<DonThuocDuLieu> rawList) {
        loadedPrescriptions.clear();
        if (rawList != null) {
            for (DonThuocDuLieu rx : rawList) {
                if (rx != null && rx.trangThai() != TrangThaiDonThuoc.HUY && !rx.isSupplyOrder()) {
                    loadedPrescriptions.add(rx);
                }
            }
        }
        int itemTotal = 0;
        for (DonThuocDuLieu rx : loadedPrescriptions) {
            if (rx.items() != null) {
                itemTotal += rx.items().size();
            }
        }
        if (onPrescriptionCountChanged != null) {
            onPrescriptionCountChanged.accept(itemTotal);
        }

        if (ticketListPanel != null) {
            ticketListPanel.removeAll();
            selectedTicketCard = null;
        }
        if (loadedPrescriptions.isEmpty()) {
            renderEmptyPrescriptionState();
            return;
        }

        String regType = patientContext != null && patientContext.registrationType != null && !patientContext.registrationType.isBlank()
                ? patientContext.registrationType : "Khám bệnh";
        boolean first = true;
        for (DonThuocDuLieu rx : loadedPrescriptions) {
            JPanel card = createTicketCard(rx, regType, first);
            if (first) {
                selectedTicketCard = card;
                first = false;
            }
            if (ticketListPanel != null) {
                ticketListPanel.add(card);
            }
        }
        if (ticketListPanel != null) {
            ticketListPanel.revalidate();
            ticketListPanel.repaint();
        }
        displayPrescriptionDetail(loadedPrescriptions.get(0));
    }

    private void renderEmptyPrescriptionState() {
        selectedPrescription = null;
        currentPrescriptionId = 0;
        if (lblMaPhieu != null) lblMaPhieu.setText("");
        if (lblNgayYLenh != null) lblNgayYLenh.setText("");
        if (lblNgayChiDinh != null) lblNgayChiDinh.setText("");
        if (lblTrangThai != null) lblTrangThai.setText("Chưa có đơn thuốc");
        if (ticketListPanel != null) {
            ticketListPanel.revalidate();
            ticketListPanel.repaint();
        }
        if (tableModel != null) {
            tableModel.setRowCount(0);
            tableModel.addRow(new Object[]{"", "Chưa có đơn thuốc được kê cho lượt khám này", "", "", "", "", "", ""});
            padEmptyRows(15);
        }
    }

    private void displayPrescriptionDetail(DonThuocDuLieu rx) {
        if (rx == null) {
            renderEmptyPrescriptionState();
            return;
        }
        this.selectedPrescription = rx;
        this.currentPrescriptionId = rx.id();

        String timeStr = rx.ngayKe() != null ? rx.ngayKe().format(DATE_TIME_FMT) : "";
        if (lblMaPhieu != null) lblMaPhieu.setText("DT-" + rx.id());
        if (lblNgayYLenh != null) lblNgayYLenh.setText(timeStr);
        if (lblNgayChiDinh != null) lblNgayChiDinh.setText(timeStr);
        if (lblTrangThai != null) lblTrangThai.setText(formatPrescriptionStatus(rx.trangThai()));

        if (tableModel != null) {
            tableModel.setRowCount(0);
            int stt = 1;
            if (rx.items() != null) {
                for (DonThuocMatHangDuLieu item : rx.items()) {
                    tableModel.addRow(new Object[]{
                            String.valueOf(stt++),
                            item.itemName(),
                            item.itemCode(),
                            item.donViTinh(),
                            item.soLuongKe() != null ? item.soLuongKe().stripTrailingZeros().toPlainString() : "1",
                            item.lieuDung(),
                            item.duongDung(),
                            item.huongDan()
                    });
                }
            }
            padEmptyRows(15);
        }
    }

    private void padEmptyRows(int minRows) {
        int remaining = Math.max(0, minRows - tableModel.getRowCount());
        for (int i = 0; i < remaining; i++) {
            tableModel.addRow(new Object[]{"", "", "", "", "", "", "", ""});
        }
    }

    private static String formatPrescriptionStatus(TrangThaiDonThuoc status) {
        if (status == null) return "Lưu tạm";
        return switch (status) {
            case TAM -> "Chờ duyệt / xuất thuốc";
            case DA_DUYET -> "Đã duyệt - Chờ xuất kho";
            case DA_XUAT_HET -> "Đã cấp phát";
            case HUY -> "Đã hủy";
        };
    }

    private JPanel createTicketCard(DonThuocDuLieu rx, String regType, boolean active) {
        String dateText = rx.ngayKe() != null ? rx.ngayKe().format(DATE_FMT) : "";
        String timeText = rx.ngayKe() != null ? rx.ngayKe().format(DATE_TIME_FMT) : "";

        JPanel card = new JPanel(new GridLayout(2, 1, 0, 2));
        card.setBackground(active ? new Color(0, 120, 215) : new Color(242, 246, 252));
        card.setBorder(new EmptyBorder(4, 8, 4, 6));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        card.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JLabel l1 = new JLabel("DT-" + rx.id() + (dateText.isEmpty() ? "" : " - " + dateText));
        l1.setFont(UngDungPhongChu.SMALL_BOLD);
        l1.setForeground(active ? Color.WHITE : new Color(25, 35, 50));

        JLabel l2 = new JLabel("[" + regType + "]" + (timeText.isEmpty() ? "" : " lúc " + timeText));
        l2.setFont(UngDungPhongChu.SMALL);
        l2.setForeground(active ? new Color(235, 245, 255) : new Color(80, 90, 105));

        card.add(l1);
        card.add(l2);

        MouseAdapter clickHandler = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    selectedTicketCard = card;
                    displayPrescriptionDetail(rx);
                }
            }
        };
        card.addMouseListener(clickHandler);
        l1.addMouseListener(clickHandler);
        l2.addMouseListener(clickHandler);
        return card;
    }

    private JPanel buildTicketSelectorPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setPreferredSize(new Dimension(210, 500));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, new Color(205, 214, 226)));

        // Hàng tìm kiếm màu vàng nhạt trên cùng
        JPanel searchBar = new JPanel(new BorderLayout(4, 0));
        searchBar.setBackground(new Color(255, 255, 208));
        searchBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(205, 212, 195)),
                new EmptyBorder(2, 6, 2, 6)
        ));
        searchBar.setPreferredSize(new Dimension(210, 24));

        JLabel lblSearch = new JLabel("Tìm kiếm");
        lblSearch.setFont(UngDungPhongChu.SMALL);
        lblSearch.setForeground(new Color(40, 40, 40));

        JLabel lblSearchIcon = new JLabel("🔍");
        lblSearchIcon.setFont(new Font("Segoe UI", Font.PLAIN, 11));

        searchBar.add(lblSearch, BorderLayout.WEST);
        searchBar.add(lblSearchIcon, BorderLayout.EAST);
        panel.add(searchBar, BorderLayout.NORTH);

        // Danh sách thẻ phiếu
        ticketListPanel = new JPanel();
        ticketListPanel.setLayout(new BoxLayout(ticketListPanel, BoxLayout.Y_AXIS));
        ticketListPanel.setBackground(Color.WHITE);

        panel.add(ticketListPanel, BorderLayout.CENTER);

        return panel;
    }

    private JPopupMenu createOrderDateContextMenu(String ticketCode, JLabel lblSubtitle) {
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

        miChangeDate.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                miChangeDate.setBackground(new Color(225, 238, 252));
            }
            @Override
            public void mouseExited(MouseEvent e) {
                miChangeDate.setBackground(Color.WHITE);
            }
        });

        miChangeDate.addActionListener(e -> {
            String current = lblNgayYLenh != null ? lblNgayYLenh.getText() : "10:23 09/08/2022";
            String newDate = null;
            if (!GraphicsEnvironment.isHeadless()) {
                newDate = JOptionPane.showInputDialog(this,
                        "Nhập ngày y lệnh mới cho phiếu " + ticketCode + ":",
                        current);
            }
            if (newDate != null && !newDate.trim().isEmpty()) {
                if (lblNgayYLenh != null) {
                    lblNgayYLenh.setText(newDate.trim());
                }
                lblSubtitle.setText("[Khám Bệnh] lúc " + newDate.trim());
            }
        });

        menu.add(miChangeDate);
        return menu;
    }

    public JPanel getSelectedTicketCard() {
        return selectedTicketCard;
    }

    private JPanel buildTicketDetailPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);

        // 1. Khung thông tin hành chính của phiếu
        JPanel headerForm = buildTicketHeaderForm();
        panel.add(headerForm, BorderLayout.NORTH);

        // 2. Bảng danh sách thuốc chi tiết (8 cột)
        String[] cols = {
                "STT",
                "Tên Thuốc",
                "Mã Thuốc",
                "Đơn Vị",
                "Kê",
                "Liều",
                "Đường Dùng",
                "Hướng Dẫn Sử Dụng"
        };

        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        padEmptyRows(15);

        prescriptionTable = new JTable(tableModel);
        prescriptionTable.setFont(UngDungPhongChu.SMALL);
        prescriptionTable.setRowHeight(24);
        prescriptionTable.setGridColor(new Color(230, 235, 243));
        prescriptionTable.setShowGrid(true);
        prescriptionTable.setSelectionBackground(new Color(214, 232, 255));
        prescriptionTable.setSelectionForeground(new Color(15, 20, 25));

        JTableHeader header = prescriptionTable.getTableHeader();
        header.setFont(UngDungPhongChu.SMALL_BOLD);
        header.setBackground(new Color(242, 246, 252));
        header.setForeground(new Color(30, 35, 45));
        header.setPreferredSize(new Dimension(100, 24));

        DefaultTableCellRenderer centerRender = new DefaultTableCellRenderer();
        centerRender.setHorizontalAlignment(SwingConstants.CENTER);

        prescriptionTable.getColumnModel().getColumn(0).setPreferredWidth(40);
        prescriptionTable.getColumnModel().getColumn(0).setCellRenderer(centerRender);

        prescriptionTable.getColumnModel().getColumn(1).setPreferredWidth(270);

        prescriptionTable.getColumnModel().getColumn(2).setPreferredWidth(75);
        prescriptionTable.getColumnModel().getColumn(2).setCellRenderer(centerRender);

        prescriptionTable.getColumnModel().getColumn(3).setPreferredWidth(55);
        prescriptionTable.getColumnModel().getColumn(3).setCellRenderer(centerRender);

        prescriptionTable.getColumnModel().getColumn(4).setPreferredWidth(45);
        prescriptionTable.getColumnModel().getColumn(4).setCellRenderer(centerRender);

        prescriptionTable.getColumnModel().getColumn(5).setPreferredWidth(45);
        prescriptionTable.getColumnModel().getColumn(5).setCellRenderer(centerRender);

        prescriptionTable.getColumnModel().getColumn(6).setPreferredWidth(75);
        prescriptionTable.getColumnModel().getColumn(6).setCellRenderer(centerRender);

        prescriptionTable.getColumnModel().getColumn(7).setPreferredWidth(200);

        JScrollPane scroll = new JScrollPane(prescriptionTable);
        scroll.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, new Color(205, 214, 226)));
        scroll.getViewport().setBackground(Color.WHITE);
        panel.add(scroll, BorderLayout.CENTER);

        // 3. Cụm nút thao tác chân trang: [🖨 Phiếu Truyền HC (F2)] | [🖨 Đơn Thuốc (F3)] | [❌ Hủy Gửi Đơn Thuốc]
        JPanel actionFooter = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        actionFooter.setBackground(Color.WHITE);
        actionFooter.setPreferredSize(new Dimension(500, 36));

        JButton btnPrintInfusion = new JButton(" Phiếu Truyền HC (F2)", new PrinterIcon());
        btnPrintInfusion.setFont(UngDungPhongChu.SMALL_BOLD);
        btnPrintInfusion.setBackground(Color.WHITE);
        btnPrintInfusion.setFocusPainted(false);
        btnPrintInfusion.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnPrintInfusion.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(185, 195, 210), 1),
                new EmptyBorder(3, 8, 3, 8)
        ));

        JButton btnPrintPrescription = new JButton(" Đơn Thuốc (F3)", new PrinterIcon());
        btnPrintPrescription.setFont(UngDungPhongChu.SMALL_BOLD);
        btnPrintPrescription.setBackground(Color.WHITE);
        btnPrintPrescription.setFocusPainted(false);
        btnPrintPrescription.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnPrintPrescription.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(185, 195, 210), 1),
                new EmptyBorder(3, 8, 3, 8)
        ));

        JButton btnCancel = new JButton(" Hủy Gửi Đơn Thuốc", new RedCrossIcon());
        btnCancel.setFont(UngDungPhongChu.SMALL_BOLD);
        btnCancel.setBackground(Color.WHITE);
        btnCancel.setFocusPainted(false);
        btnCancel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnCancel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(185, 195, 210), 1),
                new EmptyBorder(3, 8, 3, 8)
        ));

        btnPrintInfusion.addActionListener(e -> {
            if (selectedPrescription == null) {
                JOptionPane.showMessageDialog(this,
                        "Chưa có đơn thuốc nào để in phiếu truyền.",
                        "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            String ticketCode = lblMaPhieu != null ? lblMaPhieu.getText() : ("DT-" + selectedPrescription.id());
            String patientLabel = patientContext != null
                    ? (safeText(patientContext.name) + " (" + safeText(patientContext.code) + ")")
                    : "Chưa chọn bệnh nhân";
            JOptionPane.showMessageDialog(this,
                    "In phiếu truyền HC:\n▶ Phiếu: " + ticketCode + "\n▶ Bệnh nhân: " + patientLabel,
                    "In Phiếu Truyền HC", JOptionPane.INFORMATION_MESSAGE);
        });

        btnPrintPrescription.addActionListener(e -> {
            openPrescriptionPrintDialog();
        });

        btnCancel.addActionListener(e -> {
            if (currentPrescriptionId <= 0) {
                JOptionPane.showMessageDialog(this,
                        "Chưa có đơn thuốc hợp lệ để hủy.",
                        "Không thể hủy đơn thuốc", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int ret = JOptionPane.showConfirmDialog(this,
                    "Bạn có chắc chắn muốn hủy gửi đơn thuốc DT-" + currentPrescriptionId + " không?",
                    "Xác Nhận Hủy Gửi Đơn Thuốc", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (ret == JOptionPane.YES_OPTION) {
                if (services != null && services.cancelPrescription() != null) {
                    PhanQuyenNguCanh auth = KhamBenhPhienNguCanh.current();
                    if (auth == null || !auth.isValid()) {
                        JOptionPane.showMessageDialog(this,
                                "Phiên đăng nhập chưa có quyền bác sĩ để hủy đơn thuốc.",
                                "Không thể hủy đơn thuốc", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    int cancelDoctorId = auth.employeeId() > 0 ? auth.employeeId() : currentDoctorId;
                    int prescriptionIdToCancel = currentPrescriptionId;
                    CancelDonThuocCommand cmd = new CancelDonThuocCommand(
                            prescriptionIdToCancel,
                            cancelDoctorId,
                            "Hủy theo yêu cầu của bác sĩ khám",
                            auth
                    );
                    SwingWorker<KetQua<CancelDonThuocBienNhan>, Void> cancelWorker = new SwingWorker<>() {
                        @Override
                        protected KetQua<CancelDonThuocBienNhan> doInBackground() {
                            return services.cancelPrescription().handle(cmd);
                        }

                        @Override
                        protected void done() {
                            try {
                                KetQua<CancelDonThuocBienNhan> res = get();
                                if (res != null && res.isSuccess()) {
                                    loadPrescriptionsForVisit(currentVisitId);
                                    if (onPrescriptionsChangedCallback != null) {
                                        onPrescriptionsChangedCallback.run();
                                    }
                                    JOptionPane.showMessageDialog(DonThuocYeuCauTheManHinh.this,
                                            "Đã hủy đơn thuốc DT-" + prescriptionIdToCancel + " thành công trên CSDL!",
                                            "Hủy Đơn Thuốc", JOptionPane.INFORMATION_MESSAGE);
                                } else {
                                    JOptionPane.showMessageDialog(DonThuocYeuCauTheManHinh.this,
                                            res == null ? "Không nhận được kết quả hủy."
                                                    : res.getError().map(error -> error.message()).orElse("Không thể hủy đơn thuốc."),
                                            "Không thể hủy đơn thuốc", JOptionPane.ERROR_MESSAGE);
                                }
                            } catch (Exception ex) {
                                JOptionPane.showMessageDialog(DonThuocYeuCauTheManHinh.this,
                                        "Hủy đơn thuốc thất bại. Dữ liệu được giữ nguyên.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                            }
                        }
                    };
                    cancelWorker.execute();
                } else {
                    tableModel.setRowCount(0);
                    padEmptyRows(15);
                }
            }
        });

        actionFooter.add(btnPrintInfusion);
        actionFooter.add(btnPrintPrescription);
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

        // Hàng 1: Loại phiếu | Ngày y lệnh | Ngày chỉ định
        JLabel lblLoaiPhieu = createUnderlineValue("Đơn thuốc");
        this.lblNgayYLenh = createUnderlineValue("");
        this.lblNgayChiDinh = createUnderlineValue("");
        addHeaderRow(form, c, r++,
                "Loại phiếu:", lblLoaiPhieu, 1,
                "Ngày y lệnh:", lblNgayYLenh, 1,
                "Ngày chỉ định:", lblNgayChiDinh, 1);

        // Hàng 2: Mã phiếu | Nơi chỉ định
        this.lblMaPhieu = createUnderlineValue("");
        this.lblNoiChiDinh = createUnderlineValue("");
        addHeaderRow(form, c, r++,
                "Mã phiếu:", lblMaPhieu, 1,
                "Nơi chỉ định:", lblNoiChiDinh, 3,
                null, null, 0);

        // Hàng 3: Người chỉ định | Trạng thái
        this.lblNguoiChiDinh = createUnderlineValue("");
        this.lblTrangThai = createUnderlineValue("Chưa có đơn thuốc");
        addHeaderRow(form, c, r++,
                "Người chỉ định :", lblNguoiChiDinh, 1,
                "Trạng thái:", lblTrangThai, 3,
                null, null, 0);

        // Hàng 4: Khoa | Chẩn đoán
        this.lblKhoa = createUnderlineValue("");
        this.lblChanDoan = createUnderlineValue("");
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
        lbl.setForeground(new Color(18, 45, 130)); // Chữ màu xanh dương đậm
        lbl.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(185, 195, 210)));
        return lbl;
    }

    // =========================================================================
    // VECTOR ICONS
    // =========================================================================

    public DonThuocPrintPreviewManHinh.PrescriptionData buildPrescriptionPrintData() {
        String patientName = patientContext != null ? safeText(patientContext.name) : "";
        String patientCode = patientContext != null ? safeText(patientContext.code) : "";
        String birthYear = patientContext != null ? safeText(patientContext.birthYear) : "";
        String gender = patientContext != null ? safeText(patientContext.gender) : "";
        String bhyt = patientContext != null ? safeText(patientContext.bhytNumber) : "";
        String diagnosis = patientContext != null ? safeText(patientContext.diagnosis) : "";
        String doctorName = selectedPrescription != null && selectedPrescription.doctorName() != null
                && !selectedPrescription.doctorName().isBlank()
                ? selectedPrescription.doctorName()
                : (patientContext != null ? safeText(patientContext.doctor) : "");

        List<DonThuocPrintPreviewManHinh.PrescriptionItem> items = new ArrayList<>();
        if (selectedPrescription != null && selectedPrescription.items() != null) {
            for (DonThuocMatHangDuLieu item : selectedPrescription.items()) {
                String qty = item.soLuongKe() == null ? "0" : item.soLuongKe().stripTrailingZeros().toPlainString();
                StringBuilder instr = new StringBuilder();
                if (item.duongDung() != null && !item.duongDung().isBlank()) {
                    instr.append(item.duongDung());
                }
                if (item.lieuDung() != null && !item.lieuDung().isBlank()) {
                    if (!instr.isEmpty()) instr.append(" - ");
                    instr.append(item.lieuDung());
                }
                if (item.huongDan() != null && !item.huongDan().isBlank()) {
                    if (!instr.isEmpty()) instr.append(" | ");
                    instr.append(item.huongDan());
                }
                items.add(new DonThuocPrintPreviewManHinh.PrescriptionItem(
                        safeText(item.itemName()),
                        qty,
                        safeText(item.donViTinh()),
                        instr.toString()));
            }
        }
        return new DonThuocPrintPreviewManHinh.PrescriptionData(
                patientName,
                patientCode,
                birthYear,
                gender,
                bhyt,
                diagnosis,
                items,
                doctorName);
    }

    public void openPrescriptionPrintDialog() {
        if (selectedPrescription == null) {
            JOptionPane.showMessageDialog(this,
                    "Chưa có đơn thuốc nào để xem / in.",
                    "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        Window owner = SwingUtilities.getWindowAncestor(this);
        DonThuocPrintPreviewHopThoai dlg = new DonThuocPrintPreviewHopThoai(owner, buildPrescriptionPrintData());
        dlg.setVisible(true);
    }

    private static String safeText(String text) {
        return text == null ? "" : text;
    }

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
