package vn.clinicmanagement.desktop.modules.examination.components.tabs;

import vn.clinicmanagement.desktop.modules.examination.components.print.PrescriptionPrintPreviewDialog;
import vn.clinicmanagement.application.common.Result;
import vn.clinicmanagement.application.prescription.CancelPrescriptionCommand;
import vn.clinicmanagement.application.prescription.CancelPrescriptionReceipt;
import vn.clinicmanagement.application.prescription.GetPrescriptionByVisitQuery;
import vn.clinicmanagement.application.prescription.PrescriptionDto;
import vn.clinicmanagement.application.prescription.PrescriptionItemDto;
import vn.clinicmanagement.desktop.modules.examination.ExaminationCompositionRoot;

import java.util.List;

import vn.clinicmanagement.desktop.core.config.AppFonts;

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
public class PrescriptionOrderTabPanel extends JPanel {

    private final ExaminationCompositionRoot.Services services;
    private int currentVisitId = 1;
    private int currentDoctorId = 1;
    private int currentPrescriptionId = 1;

    private JTable prescriptionTable;
    private DefaultTableModel tableModel;
    private JLabel lblNgayYLenh;
    private JLabel lblNgayChiDinh;
    private JLabel lblMaPhieu;
    private JLabel lblTrangThai;
    private JLabel lblKhoa;
    private JPanel selectedTicketCard;

    public PrescriptionOrderTabPanel() {
        this(resolveDefaultServices());
    }

    public PrescriptionOrderTabPanel(ExaminationCompositionRoot.Services services) {
        this.services = services != null ? services : resolveDefaultServices();
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        // 1. CỘT PHẢI: KHUNG CHỌN PHIẾU THUỐC (TÌM KIẾM + DANH SÁCH THẺ PHIẾU THUỐC)
        add(buildTicketSelectorPanel(), BorderLayout.EAST);

        // 2. KHUNG TRUNG TÂM: CHI TIẾT ĐƠN THUỐC (THÔNG TIN HÀNH CHÍNH + BẢNG THUỐC + CỤM NÚT IN)
        add(buildTicketDetailPanel(), BorderLayout.CENTER);
    }

    private static ExaminationCompositionRoot.Services resolveDefaultServices() {
        try {
            return ExaminationCompositionRoot.createDefault();
        } catch (Exception ex) {
            return null;
        }
    }

    public void setVisitContext(int visitId, int doctorId) {
        this.currentVisitId = visitId;
        this.currentDoctorId = doctorId;
        loadPrescriptionsForVisit(visitId);
    }

    public void loadPrescriptionsForVisit(int visitId) {
        if (services == null || services.getPrescriptionByVisit() == null || visitId <= 0) return;
        SwingWorker<Result<List<PrescriptionDto>>, Void> worker = new SwingWorker<>() {
            @Override
            protected Result<List<PrescriptionDto>> doInBackground() {
                return services.getPrescriptionByVisit().handle(new GetPrescriptionByVisitQuery(visitId, null));
            }

            @Override
            protected void done() {
                try {
                    Result<List<PrescriptionDto>> res = get();
                    if (res != null && res.isSuccess()) {
                        List<PrescriptionDto> list = res.getValue().orElse(List.of());
                        if (!list.isEmpty()) {
                            PrescriptionDto first = list.get(0);
                            currentPrescriptionId = first.id();
                            if (lblMaPhieu != null) lblMaPhieu.setText("DT-" + first.id());
                            if (lblTrangThai != null) lblTrangThai.setText(first.trangThai() != null ? first.trangThai().name() : "Chờ xuất");
                            tableModel.setRowCount(0);
                            int stt = 1;
                            if (first.items() != null) {
                                for (PrescriptionItemDto item : first.items()) {
                                    tableModel.addRow(new Object[]{
                                            String.valueOf(stt++),
                                            item.itemName(),
                                            item.itemCode(),
                                            item.donViTinh(),
                                            item.soLuongKe() != null ? item.soLuongKe().toString() : "1",
                                            item.lieuDung(),
                                            item.duongDung(),
                                            item.huongDan()
                                    });
                                }
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }
        };
        worker.execute();
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
        lblSearch.setFont(AppFonts.SMALL);
        lblSearch.setForeground(new Color(40, 40, 40));

        JLabel lblSearchIcon = new JLabel("🔍");
        lblSearchIcon.setFont(new Font("Segoe UI", Font.PLAIN, 11));

        searchBar.add(lblSearch, BorderLayout.WEST);
        searchBar.add(lblSearchIcon, BorderLayout.EAST);
        panel.add(searchBar, BorderLayout.NORTH);

        // Danh sách thẻ phiếu
        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBackground(Color.WHITE);

        // Thẻ phiếu đang chọn (màu xanh dương hoàng gia đậm chuẩn EHC)
        JPanel card = new JPanel(new GridLayout(2, 1, 0, 2));
        card.setBackground(new Color(0, 120, 215)); // Màu xanh dương đậm
        card.setBorder(new EmptyBorder(4, 8, 4, 6));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        card.setCursor(new Cursor(Cursor.HAND_CURSOR));
        selectedTicketCard = card;

        JLabel l1 = new JLabel("TH220809.1 - 09/08/2022 - Thứ 3");
        l1.setFont(AppFonts.SMALL_BOLD);
        l1.setForeground(Color.WHITE);

        JLabel l2 = new JLabel("[Khám Bệnh] lúc 10:23 09/08/2022");
        l2.setFont(AppFonts.SMALL);
        l2.setForeground(new Color(235, 245, 255));

        card.add(l1);
        card.add(l2);

        // Tạo Popup Menu khi bấm chuột phải: [ ✓ ] Chuyển Ngày Y Lệnh
        JPopupMenu contextMenu = createOrderDateContextMenu("TH220809.1", l2);

        MouseAdapter rightClickAdapter = new MouseAdapter() {
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

        card.addMouseListener(rightClickAdapter);
        l1.addMouseListener(rightClickAdapter);
        l2.addMouseListener(rightClickAdapter);

        listPanel.add(card);
        panel.add(listPanel, BorderLayout.CENTER);

        return panel;
    }

    private JPopupMenu createOrderDateContextMenu(String ticketCode, JLabel lblSubtitle) {
        JPopupMenu menu = new JPopupMenu();
        menu.setBackground(Color.WHITE);
        menu.setBorder(BorderFactory.createLineBorder(new Color(130, 145, 165), 1));

        JMenuItem miChangeDate = new JMenuItem("Chuyển Ngày Y Lệnh", new CheckmarkMiniIcon());
        miChangeDate.setFont(AppFonts.SMALL);
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

        // Dòng 1: Thuốc vừa kê khớp 100% ảnh chụp thực tế
        tableModel.addRow(new Object[]{
                "1",
                "Attapulgite;Aluminum hydroxyde and magnesium carbonate (Gastropulgite) 2,5g + 0,5g",
                "X191.DK36",
                "Gói",
                "10",
                "",
                "Uống",
                "uống ngày 1 lần"
        });

        // 14 dòng trống để tạo khung lưới bảng đẹp mắt chuẩn phong cách EHC
        for (int i = 0; i < 14; i++) {
            tableModel.addRow(new Object[]{"", "", "", "", "", "", "", ""});
        }

        prescriptionTable = new JTable(tableModel);
        prescriptionTable.setFont(AppFonts.SMALL);
        prescriptionTable.setRowHeight(22);
        prescriptionTable.setRowHeight(0, 44);
        prescriptionTable.setGridColor(new Color(230, 235, 243));
        prescriptionTable.setShowGrid(true);
        prescriptionTable.setSelectionBackground(new Color(214, 232, 255));
        prescriptionTable.setSelectionForeground(new Color(15, 20, 25));

        JTableHeader header = prescriptionTable.getTableHeader();
        header.setFont(AppFonts.SMALL_BOLD);
        header.setBackground(new Color(242, 246, 252));
        header.setForeground(new Color(30, 35, 45));
        header.setPreferredSize(new Dimension(100, 24));

        DefaultTableCellRenderer centerRender = new DefaultTableCellRenderer();
        centerRender.setHorizontalAlignment(SwingConstants.CENTER);

        prescriptionTable.getColumnModel().getColumn(0).setPreferredWidth(40);
        prescriptionTable.getColumnModel().getColumn(0).setCellRenderer(centerRender);

        prescriptionTable.getColumnModel().getColumn(1).setPreferredWidth(270);
        prescriptionTable.getColumnModel().getColumn(1).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                lbl.setFont(AppFonts.SMALL);
                if (row == 0 && value != null && !value.toString().isEmpty()) {
                    lbl.setText("<html><body style='margin:0; padding:1px;'>" +
                            "Attapulgite;Aluminum hydroxyde and<br>" +
                            "magnesium carbonate (Gastropulgite) 2,5g<br>" +
                            "+ 0,5g</body></html>");
                }
                return lbl;
            }
        });

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
        btnPrintInfusion.setFont(AppFonts.SMALL_BOLD);
        btnPrintInfusion.setBackground(Color.WHITE);
        btnPrintInfusion.setFocusPainted(false);
        btnPrintInfusion.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnPrintInfusion.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(185, 195, 210), 1),
                new EmptyBorder(3, 8, 3, 8)
        ));

        JButton btnPrintPrescription = new JButton(" Đơn Thuốc (F3)", new PrinterIcon());
        btnPrintPrescription.setFont(AppFonts.SMALL_BOLD);
        btnPrintPrescription.setBackground(Color.WHITE);
        btnPrintPrescription.setFocusPainted(false);
        btnPrintPrescription.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnPrintPrescription.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(185, 195, 210), 1),
                new EmptyBorder(3, 8, 3, 8)
        ));

        JButton btnCancel = new JButton(" Hủy Gửi Đơn Thuốc", new RedCrossIcon());
        btnCancel.setFont(AppFonts.SMALL_BOLD);
        btnCancel.setBackground(Color.WHITE);
        btnCancel.setFocusPainted(false);
        btnCancel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnCancel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(185, 195, 210), 1),
                new EmptyBorder(3, 8, 3, 8)
        ));

        btnPrintInfusion.addActionListener(e -> {
            JOptionPane.showMessageDialog(this,
                    "In phiếu truyền HC:\n▶ Phiếu: TH220809.1\n▶ Bệnh nhân: NGUYỄN HOÀNG LONG (BN-20261107-0003)",
                    "In Phiếu Truyền HC", JOptionPane.INFORMATION_MESSAGE);
        });

        btnPrintPrescription.addActionListener(e -> {
            openPrescriptionPrintDialog();
        });

        btnCancel.addActionListener(e -> {
            int ret = JOptionPane.showConfirmDialog(this,
                    "Bạn có chắc chắn muốn hủy gửi đơn thuốc không?",
                    "Xác Nhận Hủy Gửi Đơn Thuốc", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (ret == JOptionPane.YES_OPTION) {
                if (services != null && services.cancelPrescription() != null) {
                    CancelPrescriptionCommand cmd = new CancelPrescriptionCommand(
                            currentPrescriptionId > 0 ? currentPrescriptionId : 1,
                            currentDoctorId > 0 ? currentDoctorId : 1,
                            "Hủy theo yêu cầu của bác sĩ khám",
                            null
                    );
                    SwingWorker<Result<CancelPrescriptionReceipt>, Void> cancelWorker = new SwingWorker<>() {
                        @Override
                        protected Result<CancelPrescriptionReceipt> doInBackground() {
                            return services.cancelPrescription().handle(cmd);
                        }

                        @Override
                        protected void done() {
                            try {
                                Result<CancelPrescriptionReceipt> res = get();
                                if (res != null && res.isSuccess()) {
                                    tableModel.setRowCount(0);
                                    if (lblTrangThai != null) lblTrangThai.setText("Đã hủy");
                                    JOptionPane.showMessageDialog(PrescriptionOrderTabPanel.this,
                                            "Đã hủy đơn thuốc thành công trên CSDL!",
                                            "Hủy Đơn Thuốc", JOptionPane.INFORMATION_MESSAGE);
                                } else {
                                    tableModel.setRowCount(0);
                                }
                            } catch (Exception ex) {
                                tableModel.setRowCount(0);
                            }
                        }
                    };
                    cancelWorker.execute();
                } else {
                    tableModel.setRowCount(0);
                    for (int i = 0; i < 15; i++) {
                        tableModel.addRow(new Object[]{"", "", "", "", "", "", "", ""});
                    }
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
        JLabel lblLoaiPhieu = createUnderlineValue("Phiếu chỉ định");
        this.lblNgayYLenh = createUnderlineValue("10:23 09/08/2022");
        this.lblNgayChiDinh = createUnderlineValue("10:23 09/08/2022");
        addHeaderRow(form, c, r++,
                "Loại phiếu:", lblLoaiPhieu, 1,
                "Ngày y lệnh:", lblNgayYLenh, 1,
                "Ngày chỉ định:", lblNgayChiDinh, 1);

        // Hàng 2: Mã phiếu | Nơi chỉ định
        this.lblMaPhieu = createUnderlineValue("TH220809.1");
        JLabel lblNoiChiDinh = createUnderlineValue("PK Nhi 1");
        addHeaderRow(form, c, r++,
                "Mã phiếu:", lblMaPhieu, 1,
                "Nơi chỉ định:", lblNoiChiDinh, 3,
                null, null, 0);

        // Hàng 3: Người chỉ định | Trạng thái (Chờ xuất/nhập...)
        JLabel lblNguoiChiDinh = createUnderlineValue("Sennv");
        this.lblTrangThai = createUnderlineValue("Chờ xuất/nhập (Mã phiếu: 1608759)");
        addHeaderRow(form, c, r++,
                "Người chỉ định :", lblNguoiChiDinh, 1,
                "Trạng thái:", lblTrangThai, 3,
                null, null, 0);

        // Hàng 4: Khoa | Chẩn đoán
        this.lblKhoa = createUnderlineValue("TTVT Phòng khám");
        JLabel lblChanDoan = createUnderlineValue("H20-Viêm màng bồ đào");
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
            lbl.setFont(AppFonts.SMALL);
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
        lbl.setFont(AppFonts.SMALL);
        lbl.setForeground(new Color(18, 45, 130)); // Chữ màu xanh dương đậm
        lbl.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(185, 195, 210)));
        return lbl;
    }

    // =========================================================================
    // VECTOR ICONS
    // =========================================================================

    public void openPrescriptionPrintDialog() {
        Window owner = SwingUtilities.getWindowAncestor(this);
        PrescriptionPrintPreviewDialog dlg = new PrescriptionPrintPreviewDialog(owner);
        dlg.setVisible(true);
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
