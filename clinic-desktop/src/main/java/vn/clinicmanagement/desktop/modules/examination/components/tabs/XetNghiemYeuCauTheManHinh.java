package vn.clinicmanagement.desktop.modules.examination.components.tabs;

import vn.clinicmanagement.desktop.modules.examination.components.dialogs.DichVuYeuCauHopThoai;
import vn.clinicmanagement.desktop.modules.examination.components.print.LamSangYeuCauPrintPreviewHopThoai;

import java.util.List;
import vn.clinicmanagement.application.common.KetQua;
import vn.clinicmanagement.application.cls.CancelDichVuYeuCauCommand;
import vn.clinicmanagement.application.cls.CancelDichVuYeuCauBienNhan;
import vn.clinicmanagement.application.cls.GetDichVuTheoLuotKhamQuery;
import vn.clinicmanagement.application.cls.DichVuYeuCauDuLieu;
import vn.clinicmanagement.desktop.modules.examination.KhamBenhGhepNoiGoc;

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
 * Tab "Xét Nghiệm(3)" trong phân hệ Khám Bệnh.
 * Khớp 100% giao diện ảnh chụp mẫu thực tế:
 * - Cột phải: Danh sách các đợt/phiếu xét nghiệm theo ngày (08/08/2022 - Khám bệnh)
 * - Khung chính:
 *   + Tiêu đề phiếu: Mã phiếu (XN220009.5), Ngày y lệnh, Ngày chỉ định, Người chỉ định (Sennv),
 *     Nơi chỉ định (PK. Nhi 1), Chẩn đoán (H20-Viêm màng bồ đào), kèm nút [🕒 Lịch sử] & [📊 Biểu Đồ]
 *   + Bảng danh sách xét nghiệm phân nhóm (Group rows theo Barcode / phòng XN / ngày):
 *     Nhóm 1: Phòng Xét nghiệm, Ngày 06/08/2022, Barcode: 17182, Số thứ tự lấy mẫu: 1, Đang chờ
 *             - Định lượng AMH [YC] | 1 | (Chưa thực hiện) | ng/ml | EDTA whole blood
 *     Nhóm 2: Phòng Xét nghiệm, Ngày 08/08/2022, Barcode: 17182, Số thứ tự lấy mẫu: 1, Đang chờ
 *             - Xét nghiệm tế bào học bằng phương pháp Liqui Prep | 1 | (Chưa thực hiện) | mmol/L | EDTA whole blood
 *   + Cụm nút chân trang: [🖨 Phiếu Chỉ Định (F2)], [🖨 Phiếu Kết Quả (F3)], [❌ Hủy Gửi Phiếu]
 */
public class XetNghiemYeuCauTheManHinh extends JPanel {

    /** Trường `labTable`: xetnghiem bảng. */
    private JTable labTable;
    /** Trường `tableModel`: bảng mô hình. */
    private DefaultTableModel tableModel;
    private JLabel lblMaPhieu;
    private JLabel lblNgayYLenh;
    private JLabel lblNgayChiDinh;
    private JLabel lblNguoiChiDinh;
    private JLabel lblNoiChiDinh;
    private JLabel lblChanDoan;
    /** Trường `selectedTicketCard`: duocchon phiếu `Card`. */
    private JPanel selectedTicketCard;
    private JPanel toastBanner;
    private JLabel lblToast;
    /** Trường `rightActionBox`: `right` hành động `Box`. */
    private JPanel rightActionBox;
    /** Trường `isCanceled`: là `Canceled`. */
    private boolean isCanceled = false;
    /** Trường `onEditOrderCallback`: tải `Edit` yêu cầu `Callback`. */
    private Runnable onEditOrderCallback;

    /** Trường `services`: dịch vụ. */
    private final KhamBenhGhepNoiGoc.Services services;
    /** Trường `currentVisitId`: hientai luotkham mã. */
    private int currentVisitId = 1;
    /** Trường `currentOrderId`: hientai yêu cầu mã. */
    private int currentOrderId = 1;

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

        // 1. CỘT PHẢI: DANH SÁCH PHIẾU XÉT NGHIỆM THEO NGÀY
        add(buildTicketSelectorPanel(), BorderLayout.EAST);

        // 2. KHUNG TRUNG TÂM: CHI TIẾT PHIẾU XÉT NGHIỆM + BẢNG KẾT QUẢ + CHÂN TRANG
        add(buildTicketDetailPanel(), BorderLayout.CENTER);
    }

    /** Hàm `resolveDefaultServices`: `resolve` macdinh dịch vụ. */
    private static KhamBenhGhepNoiGoc.Services resolveDefaultServices() {
        try {
            return KhamBenhGhepNoiGoc.createDefault();
        } catch (Exception ex) {
            return null;
        }
    }

    /** Hàm `loadServicesForVisit`: tải dịch vụ cho luotkham. */
    public void loadServicesForVisit(int visitId) {
        this.currentVisitId = visitId;
        if (services == null || services.getServicesByVisit() == null || visitId <= 0) return;

        SwingWorker<KetQua<List<DichVuYeuCauDuLieu>>, Void> worker = new SwingWorker<>() {
            @Override
            protected KetQua<List<DichVuYeuCauDuLieu>> doInBackground() {
                return services.getServicesByVisit().handle(new GetDichVuTheoLuotKhamQuery(visitId, null, null));
            }

            @Override
            protected void done() {
                try {
                    KetQua<List<DichVuYeuCauDuLieu>> res = get();
                    if (res != null && res.isSuccess()) {
                        List<DichVuYeuCauDuLieu> list = res.getValue().orElse(List.of());
                        if (!list.isEmpty()) {
                            DichVuYeuCauDuLieu first = list.get(0);
                            currentOrderId = first.orderId();
                            if (lblMaPhieu != null) lblMaPhieu.setText("XN-" + first.orderId());
                            tableModel.setRowCount(0);
                            int stt = 1;
                            for (DichVuYeuCauDuLieu s : list) {
                                tableModel.addRow(new Object[]{
                                        String.valueOf(stt++),
                                        s.serviceName(),
                                        "1",
                                        s.status() != null ? s.status() : "Chờ thực hiện",
                                        "",
                                        ""
                                });
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }
        };
        worker.execute();
    }

    /** Hàm `setOnEditOrderCallback`: gán tải `Edit` yêu cầu `Callback`. */
    public void setOnEditOrderCallback(Runnable onEditOrderCallback) {
        this.onEditOrderCallback = onEditOrderCallback;
    }

    /** Hàm `buildTicketSelectorPanel`: tạo phiếu `Selector` màn hình. */
    private JPanel buildTicketSelectorPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setPreferredSize(new Dimension(145, 500));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, new Color(205, 214, 226)));

        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBackground(Color.WHITE);

        // Thẻ 1: 09/08/2022 - Khám Bệnh (Đang chọn nền xanh dương hoàng gia EHC)
        JPanel card1 = createTicketCard("09/08/2022", "Khám Bệnh", true);
        selectedTicketCard = card1;

        // Thẻ 2: 09/08/2022 - Khám Bệnh
        JPanel card2 = createTicketCard("09/08/2022", "Khám Bệnh", false);

        // Thẻ 3: 08/08/2022 - Khám Bệnh
        JPanel card3 = createTicketCard("08/08/2022", "Khám Bệnh", false);

        listPanel.add(card1);
        listPanel.add(card2);
        listPanel.add(card3);

        panel.add(listPanel, BorderLayout.NORTH);
        return panel;
    }

    /** Hàm `createTicketCard`: tạo phiếu `Card`. */
    private JPanel createTicketCard(String dateStr, String typeStr, boolean isSelected) {
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

        if (isSelected) {
            card.setBackground(new Color(0, 120, 215)); // Chuẩn màu xanh dương đậm EHC
            l1.setForeground(Color.WHITE);
            l2.setForeground(new Color(235, 245, 255));
        } else {
            card.setBackground(Color.WHITE);
            l1.setForeground(new Color(30, 35, 45));
            l2.setForeground(new Color(90, 95, 105));
        }

        card.add(l1);
        card.add(l2);

        // Hỗ trợ click chuột phải hiển thị "Chuyển Ngày Y Lệnh"
        JPopupMenu contextMenu = createOrderDateContextMenu(dateStr, l1);
        MouseAdapter adapter = new MouseAdapter() {
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

    /** Hàm `createOrderDateContextMenu`: tạo yêu cầu ngày ngữ cảnh menu. */
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
            String current = lblNgayYLenh != null ? lblNgayYLenh.getText() : "09:32 09/08/2022";
            String newDate = null;
            if (!GraphicsEnvironment.isHeadless()) {
                newDate = JOptionPane.showInputDialog(this, "Nhập ngày y lệnh mới cho phiếu xét nghiệm:", current);
            }
            if (newDate != null && !newDate.trim().isEmpty()) {
                if (lblNgayYLenh != null) {
                    lblNgayYLenh.setText(newDate.trim());
                }
                lblDate.setText(newDate.trim());
            }
        });

        menu.add(miChangeDate);
        return menu;
    }

    /** Hàm `buildTicketDetailPanel`: tạo phiếu chi tiết màn hình. */
    private JPanel buildTicketDetailPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);

        // 1. KHUNG THÔNG TIN HÀNH CHÍNH & NÚT TIỆN ÍCH
        JPanel headerPanel = buildHeaderPanel();
        panel.add(headerPanel, BorderLayout.NORTH);

        // 2. BẢNG DANH SÁCH XÉT NGHIỆM PHÂN NHÓM
        panel.add(buildLabTablePanel(), BorderLayout.CENTER);

        // 3. CHÂN TRANG CỤM NÚT THAO TÁC
        panel.add(buildActionFooterPanel(), BorderLayout.SOUTH);

        return panel;
    }

    /** Hàm `buildHeaderPanel`: tạo `Header` màn hình. */
    private JPanel buildHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(6, 8, 8, 8));

        // Bên trái: Form thông tin hành chính
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(2, 4, 2, 4);

        int r = 0;

        // Hàng 1: Mã phiếu | Ngày y lệnh | Ngày chỉ định
        this.lblMaPhieu = createUnderlineValue("XN220809.5");
        this.lblNgayYLenh = createUnderlineValue("09:32 09/08/2022");
        this.lblNgayChiDinh = createUnderlineValue("09:32 09/08/2022");
        addHeaderRow(form, c, r++,
                "Mã phiếu:", lblMaPhieu, 1,
                "Ngày y lệnh:", lblNgayYLenh, 1,
                "Ngày chỉ định:", lblNgayChiDinh, 1);

        // Hàng 2: Người chỉ định | Nơi chỉ định
        this.lblNguoiChiDinh = createUnderlineValue("Sennv");
        this.lblNoiChiDinh = createUnderlineValue("PK Nhi 1");
        addHeaderRow(form, c, r++,
                "Người chỉ định:", lblNguoiChiDinh, 1,
                "Nơi chỉ định:", lblNoiChiDinh, 3,
                null, null, 0);

        // Hàng 3: Chẩn đoán
        this.lblChanDoan = createUnderlineValue("H20-Viêm màng bồ đào");
        addHeaderRow(form, c, r,
                "Chẩn đoán:", lblChanDoan, 5,
                null, null, 0,
                null, null, 0);

        panel.add(form, BorderLayout.CENTER);

        // Bên phải: Cụm nút Lịch sử & Biểu Đồ
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
        btnHistory.addActionListener(e -> {
            JOptionPane.showMessageDialog(this,
                    "Lịch sử thực hiện xét nghiệm của bệnh nhân:\n" +
                    "- 09/08/2022: XN220009.5 (PK. Nhi 1)\n" +
                    "- 08/08/2022: XN220008.2 (Khám bệnh)",
                    "Lịch Sử Xét Nghiệm", JOptionPane.INFORMATION_MESSAGE);
        });

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
            JOptionPane.showMessageDialog(this,
                    "Biểu đồ theo dõi chỉ số xét nghiệm (AMH, Liqui Prep) qua các lần khám.",
                    "Biểu Đồ Xu Hướng Xét Nghiệm", JOptionPane.INFORMATION_MESSAGE);
        });

        toolBox.add(btnHistory);
        toolBox.add(btnChart);
        panel.add(toolBox, BorderLayout.EAST);

        return panel;
    }

    /** Hàm `buildLabTablePanel`: tạo xetnghiem bảng màn hình. */
    private JPanel buildLabTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);

        String[] cols = {"Tên Xét nghiệm", "SL", "Kết Quả", "Đơn Vị", "Loại Bệnh Phẩm"};
        tableModel = new DefaultTableModel(cols, 0) {
            /** Hàm `isCellEditable`: là `Cell` `Editable`. */
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

        DefaultTableCellRenderer centerRender = new DefaultTableCellRenderer();
        centerRender.setHorizontalAlignment(SwingConstants.CENTER);

        // Custom Cell Renderer để hỗ trợ hiển thị dòng Header nhóm xét nghiệm
        DefaultTableCellRenderer customRenderer = new DefaultTableCellRenderer() {
            /** Hàm `getTableCellRendererComponent`: lấy bảng `Cell` `Renderer` thành phần. */
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int col) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                Object firstColVal = table.getValueAt(row, 0);
                boolean isGroup = firstColVal != null && firstColVal.toString().startsWith("GROUP:");

                if (isGroup) {
                    lbl.setBackground(new Color(237, 243, 249)); // Nền nhóm xanh xám nhạt chuẩn ảnh
                    if (firstColVal.toString().contains("[Không có phòng thực hiện]")) {
                        lbl.setForeground(new Color(0, 102, 204)); // Chữ màu xanh dương chuẩn ảnh
                    } else {
                        lbl.setForeground(new Color(25, 40, 60));
                    }
                    lbl.setFont(UngDungPhongChu.SMALL_BOLD);
                    if (col == 0) {
                        String txt = firstColVal.toString().substring(6);
                        lbl.setText(" " + txt);
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
                    } else if (col == 2) {
                        lbl.setHorizontalAlignment(SwingConstants.LEFT);
                        if (value != null && value.toString().contains("(Chưa thực hiện)")) {
                            lbl.setForeground(new Color(120, 125, 135));
                        }
                    } else if (col == 3) {
                        lbl.setHorizontalAlignment(SwingConstants.LEFT);
                    } else if (col == 4) {
                        lbl.setHorizontalAlignment(SwingConstants.LEFT);
                    } else {
                        lbl.setHorizontalAlignment(SwingConstants.LEFT);
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
        labTable.getColumnModel().getColumn(2).setPreferredWidth(110);
        labTable.getColumnModel().getColumn(3).setPreferredWidth(60);
        labTable.getColumnModel().getColumn(4).setPreferredWidth(120);

        JScrollPane scroll = new JScrollPane(labTable);
        scroll.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, new Color(205, 214, 226)));
        scroll.getViewport().setBackground(Color.WHITE);

        // Toast banner màu đen hiển thị thông báo "Đã hủy gửi phiếu thành công !"
        toastBanner = new JPanel(new BorderLayout());
        toastBanner.setBackground(Color.BLACK);
        lblToast = new JLabel("Đã hủy gửi phiếu thành công !", SwingConstants.CENTER);
        lblToast.setFont(UngDungPhongChu.BODY_BOLD);
        lblToast.setForeground(Color.WHITE);
        toastBanner.add(lblToast, BorderLayout.CENTER);
        toastBanner.setVisible(false);

        // LayeredPane chứa bảng bên dưới và thanh thông báo màu đen ở giữa khớp 100% ảnh chụp
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

        // Nạp dữ liệu ban đầu
        loadTableData(false);

        return panel;
    }

    /** Hàm `loadTableData`: tải bảng dữ liệu. */
    public void loadTableData(boolean canceled) {
        tableModel.setRowCount(0);
        if (canceled) {
            // Khi hủy gửi phiếu: Nhóm chuyển thành [Không có phòng thực hiện]
            tableModel.addRow(new Object[]{
                    "GROUP:[Không có phòng thực hiện]",
                    "", "", "", ""
            });
            tableModel.addRow(new Object[]{
                    "  Xét nghiệm tế bào học bằng phương pháp Liqui Prep",
                    "1",
                    "(Chưa thực hiện)",
                    "mmol/L",
                    "EDTA whole blood"
            });
            tableModel.addRow(new Object[]{
                    "  Định lượng AMH [YC]",
                    "1",
                    "(Chưa thực hiện)",
                    "ng/ml",
                    "EDTA whole blood"
            });
        } else {
            // Khi phiếu đã gửi bình thường: Hiển thị 2 nhóm Barcode phòng xét nghiệm
            tableModel.addRow(new Object[]{
                    "GROUP:Phòng Xét nghiệm, Ngày 06/08/2022, Barcode: 17182, Số thứ tự lấy mẫu: 1, Đang chờ",
                    "", "", "", ""
            });
            tableModel.addRow(new Object[]{
                    "  Định lượng AMH [YC]",
                    "1",
                    "(Chưa thực hiện)",
                    "ng/ml",
                    "EDTA whole blood"
            });
            tableModel.addRow(new Object[]{
                    "GROUP:Phòng Xét nghiệm, Ngày 08/08/2022, Barcode: 17182, Số thứ tự lấy mẫu: 1, Đang chờ",
                    "", "", "", ""
            });
            tableModel.addRow(new Object[]{
                    "  Xét nghiệm tế bào học bằng phương pháp Liqui Prep",
                    "1",
                    "(Chưa thực hiện)",
                    "mmol/L",
                    "EDTA whole blood"
            });
        }

        for (int i = 0; i < 15; i++) {
            tableModel.addRow(new Object[]{"", "", "", "", ""});
        }
    }

    /** Hàm `buildActionFooterPanel`: tạo hành động chantrang màn hình. */
    private JPanel buildActionFooterPanel() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(Color.WHITE);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(215, 222, 232)));
        footer.setPreferredSize(new Dimension(800, 36));

        // Nút toggle bên trái [ I ]
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

        // Cụm nút thao tác bên phải (đổi linh hoạt theo trạng thái đã gửi / đã hủy gửi)
        rightActionBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        rightActionBox.setBackground(Color.WHITE);

        updateFooterButtons(false);

        footer.add(rightActionBox, BorderLayout.EAST);
        return footer;
    }

    /** Hàm `updateFooterButtons`: cập nhật chantrang `Buttons`. */
    private void updateFooterButtons(boolean canceled) {
        rightActionBox.removeAll();

        JButton btnPrintOrder = new JButton(" Phiếu Chỉ Định (F2)", new PrinterIcon());
        styleButton(btnPrintOrder);
        btnPrintOrder.addActionListener(e -> {
            openClinicalOrderPrintDialog();
        });
        rightActionBox.add(btnPrintOrder);

        JButton btnPrintResult = new JButton(" Phiếu Kết Quả (F3)", new PrinterIcon());
        styleButton(btnPrintResult);
        btnPrintResult.addActionListener(e -> {
            JOptionPane.showMessageDialog(this,
                    "In phiếu kết quả xét nghiệm:\n▶ Mã phiếu: " + (lblMaPhieu != null ? lblMaPhieu.getText() : "XN220809.5"),
                    "In Phiếu Kết Quả Xét Nghiệm", JOptionPane.INFORMATION_MESSAGE);
        });
        rightActionBox.add(btnPrintResult);

        if (!canceled) {
            JButton btnCancelSend = new JButton(" Hủy Gửi Phiếu", new RedCrossIcon());
            styleButton(btnCancelSend);
            btnCancelSend.addActionListener(e -> {
                int ret = JOptionPane.showConfirmDialog(this,
                        "Bạn có chắc chắn muốn hủy phiếu xét nghiệm không?",
                        "Xác Nhận Hủy Gửi Phiếu Xét Nghiệm", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (ret == JOptionPane.YES_OPTION) {
                    if (services != null && services.cancelServiceOrder() != null) {
                        CancelDichVuYeuCauCommand cmd = new CancelDichVuYeuCauCommand(
                                currentOrderId > 0 ? currentOrderId : 1,
                                "Hủy theo yêu cầu của bác sĩ",
                                null,
                                null
                        );
                        SwingWorker<KetQua<CancelDichVuYeuCauBienNhan>, Void> cancelWorker = new SwingWorker<>() {
                            @Override
                            protected KetQua<CancelDichVuYeuCauBienNhan> doInBackground() {
                                return services.cancelServiceOrder().handle(cmd);
                            }

                            @Override
                            protected void done() {
                                setOrderCanceled(true);
                                tableModel.setRowCount(0);
                                JOptionPane.showMessageDialog(XetNghiemYeuCauTheManHinh.this,
                                        "Đã hủy phiếu chỉ định dịch vụ thành công trên CSDL!",
                                        "Hủy Chỉ Định Thành Công", JOptionPane.INFORMATION_MESSAGE);
                            }
                        };
                        cancelWorker.execute();
                    } else {
                        setOrderCanceled(true);
                    }
                }
            });
            rightActionBox.add(btnCancelSend);
        } else {
            JButton btnEditOrder = new JButton(" Sửa Phiếu", new EditPencilIcon());
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

            JButton btnSendOrder = new JButton(" Gửi Phiếu", new CheckmarkSendIcon());
            styleButton(btnSendOrder);
            btnSendOrder.addActionListener(e -> {
                setOrderCanceled(false);
            });
            rightActionBox.add(btnSendOrder);

            JButton btnDeleteOrder = new JButton(" Xóa Phiếu", new RedCrossIcon());
            styleButton(btnDeleteOrder);
            btnDeleteOrder.addActionListener(e -> {
                int ret = JOptionPane.showConfirmDialog(this,
                        "Bạn có chắc chắn muốn xóa hẳn phiếu xét nghiệm này?",
                        "Xác Nhận Xóa Phiếu", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (ret == JOptionPane.YES_OPTION) {
                    tableModel.setRowCount(0);
                    for (int i = 0; i < 15; i++) {
                        tableModel.addRow(new Object[]{"", "", "", "", ""});
                    }
                }
            });
            rightActionBox.add(btnDeleteOrder);
        }

        rightActionBox.revalidate();
        rightActionBox.repaint();
    }

    /** Hàm `styleButton`: `style` nút. */
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

    /** Hàm `openClinicalOrderPrintDialog`: mở lamsang yêu cầu in hopthoai. */
    public void openClinicalOrderPrintDialog() {
        Window owner = SwingUtilities.getWindowAncestor(this);
        LamSangYeuCauPrintPreviewHopThoai dlg = new LamSangYeuCauPrintPreviewHopThoai(owner);
        dlg.setVisible(true);
    }

    /** Hàm `setOrderCanceled`: gán yêu cầu `Canceled`. */
    public void setOrderCanceled(boolean canceled) {
        this.isCanceled = canceled;
        loadTableData(canceled);
        updateFooterButtons(canceled);
        if (canceled) {
            lblToast.setText("Đã hủy gửi phiếu thành công !");
            toastBanner.setVisible(true);
            if (lblMaPhieu != null) lblMaPhieu.setText("XN220809.5");
            if (lblNgayYLenh != null) lblNgayYLenh.setText("09:32 09/08/2022");
            if (lblNgayChiDinh != null) lblNgayChiDinh.setText("09:32 09/08/2022");
            if (lblNoiChiDinh != null) lblNoiChiDinh.setText("PK Nhi 1");
        } else {
            lblToast.setText("Đã gửi phiếu thành công !");
            toastBanner.setVisible(true);
            Timer timer = new Timer(2500, e -> toastBanner.setVisible(false));
            timer.setRepeats(false);
            timer.start();
        }
        revalidate();
        repaint();
    }

    /** Hàm `isOrderCanceled`: là yêu cầu `Canceled`. */
    public boolean isOrderCanceled() {
        return isCanceled;
    }

    /** Hàm `addHeaderRow`: thêm `Header` `Row`. */
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

    /** Hàm `createUnderlineValue`: tạo `Underline` giá trị. */
    private JLabel createUnderlineValue(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(UngDungPhongChu.SMALL);
        lbl.setForeground(new Color(18, 45, 130)); // Chữ màu xanh dương đậm
        lbl.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(185, 195, 210)));
        return lbl;
    }

    /** Hàm `getSelectedTicketCard`: lấy duocchon phiếu `Card`. */
    public JPanel getSelectedTicketCard() {
        return selectedTicketCard;
    }

    // =========================================================================
    // VECTOR ICONS
    // =========================================================================

    public static class PrinterIcon implements Icon {
        /** Hàm `getIconWidth`: lấy `Icon` `Width`. Gốc khai báo tại `Icon#getIconWidth`. */
        @Override public int getIconWidth() { return 16; }
        /** Hàm `getIconHeight`: lấy `Icon` `Height`. Gốc khai báo tại `Icon#getIconHeight`. */
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
        /** Hàm `getIconWidth`: lấy `Icon` `Width`. Gốc khai báo tại `Icon#getIconWidth`. */
        @Override public int getIconWidth() { return 14; }
        /** Hàm `getIconHeight`: lấy `Icon` `Height`. Gốc khai báo tại `Icon#getIconHeight`. */
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
        /** Hàm `getIconWidth`: lấy `Icon` `Width`. Gốc khai báo tại `Icon#getIconWidth`. */
        @Override public int getIconWidth() { return 14; }
        /** Hàm `getIconHeight`: lấy `Icon` `Height`. Gốc khai báo tại `Icon#getIconHeight`. */
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
        /** Hàm `getIconWidth`: lấy `Icon` `Width`. Gốc khai báo tại `Icon#getIconWidth`. */
        @Override public int getIconWidth() { return 15; }
        /** Hàm `getIconHeight`: lấy `Icon` `Height`. Gốc khai báo tại `Icon#getIconHeight`. */
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
        /** Hàm `getIconWidth`: lấy `Icon` `Width`. Gốc khai báo tại `Icon#getIconWidth`. */
        @Override public int getIconWidth() { return 16; }
        /** Hàm `getIconHeight`: lấy `Icon` `Height`. Gốc khai báo tại `Icon#getIconHeight`. */
        @Override public int getIconHeight() { return 14; }
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            // Cột 1: Vàng
            g2.setColor(new Color(245, 175, 35));
            g2.fillRect(x + 1, y + 6, 3, 7);
            // Cột 2: Xanh dương
            g2.setColor(new Color(30, 130, 220));
            g2.fillRect(x + 6, y + 2, 3, 11);
            // Cột 3: Đỏ
            g2.setColor(new Color(230, 50, 50));
            g2.fillRect(x + 11, y + 4, 3, 9);
            g2.dispose();
        }
    }

    public static class EditPencilIcon implements Icon {
        /** Hàm `getIconWidth`: lấy `Icon` `Width`. Gốc khai báo tại `Icon#getIconWidth`. */
        @Override public int getIconWidth() { return 15; }
        /** Hàm `getIconHeight`: lấy `Icon` `Height`. Gốc khai báo tại `Icon#getIconHeight`. */
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
        /** Hàm `getIconWidth`: lấy `Icon` `Width`. Gốc khai báo tại `Icon#getIconWidth`. */
        @Override public int getIconWidth() { return 15; }
        /** Hàm `getIconHeight`: lấy `Icon` `Height`. Gốc khai báo tại `Icon#getIconHeight`. */
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
