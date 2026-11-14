package vn.clinicmanagement.desktop.modules.pharmacy.components;

import vn.clinicmanagement.desktop.core.config.UngDungPhongChu;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.function.Consumer;

/**
 * Thanh công cụ phụ (Sub-Toolbar) trên đầu màn hình Quản lý Kho / Tủ Trực Vật Tư.
 * Khớp 100% tỷ lệ và màu sắc ảnh mẫu EHC.
 */
public class NhaThuocPhuThanhCongCu extends JPanel {

    private final JButton btnMenu;
    /** Trường `btnReport`: `btn` baocao. */
    private final JButton btnReport;
    private final JButton btnList;
    /** Trường `btnCreate`: `btn` tạo. */
    private final JButton btnCreate;
    /** Trường `btnSearch`: `btn` tìm kiếm. */
    private final JButton btnSearch;
    /** Trường `txtTicketCode`: `txt` phiếu mã. */
    private final JTextField txtTicketCode;
    /** Trường `txtFromDate`: `txt` tu ngày. */
    private final JTextField txtFromDate;
    /** Trường `txtToDate`: `txt` den ngày. */
    private final JTextField txtToDate;

    /** Trường `searchListener`: tìm kiếm bolangnghe. */
    private Consumer<String> searchListener;

    public NhaThuocPhuThanhCongCu() {
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(1000, 36));
        setBackground(new Color(235, 241, 247));
        setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(189, 202, 215)));

        // --- CỤM BÊN TRÁI: Menu, Báo Cáo, Danh Sách, Tạo Phiếu, Mã Phiếu ---
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 4));
        leftPanel.setOpaque(false);

        btnMenu = createToolbarButton("Menu", NhaThuocBieuTuong.createMenuIcon(), false);
        btnReport = createToolbarButton("Báo Cáo", NhaThuocBieuTuong.createReportIcon(), false);
        btnList = createToolbarButton("Danh Sách", NhaThuocBieuTuong.createListIcon(), true); // Đang chọn (highlight)
        btnCreate = createToolbarButton("Tạo Phiếu", NhaThuocBieuTuong.createPlusIcon(), false);

        JLabel lblTicketCode = new JLabel("Mã phiếu:");
        lblTicketCode.setFont(UngDungPhongChu.BODY_BOLD);
        lblTicketCode.setForeground(new Color(20, 20, 20));
        lblTicketCode.setBorder(new EmptyBorder(0, 8, 0, 2));

        txtTicketCode = new JTextField();
        txtTicketCode.setFont(UngDungPhongChu.BODY);
        txtTicketCode.setPreferredSize(new Dimension(115, 24));
        txtTicketCode.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(165, 180, 196), 1),
                new EmptyBorder(1, 4, 1, 4)
        ));
        txtTicketCode.addActionListener(e -> triggerSearch());

        leftPanel.add(btnMenu);
        leftPanel.add(btnReport);
        leftPanel.add(btnList);
        leftPanel.add(btnCreate);
        leftPanel.add(lblTicketCode);
        leftPanel.add(txtTicketCode);

        // --- CỤM BÊN PHẢI: Từ ngày, Đến ngày, Nút Tìm Kiếm ▾ ---
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 4));
        rightPanel.setOpaque(false);

        JLabel lblFrom = new JLabel("Từ ngày");
        lblFrom.setFont(UngDungPhongChu.BODY);
        lblFrom.setForeground(new Color(30, 30, 30));

        txtFromDate = createDateTimeBox("00:00 14/09/2021");

        JLabel lblTo = new JLabel("Đến ngày");
        lblTo.setFont(UngDungPhongChu.BODY);
        lblTo.setForeground(new Color(30, 30, 30));

        txtToDate = createDateTimeBox("23:59 14/09/2021");

        btnSearch = createSearchButton();
        btnSearch.addActionListener(e -> triggerSearch());

        rightPanel.add(lblFrom);
        rightPanel.add(txtFromDate);
        rightPanel.add(lblTo);
        rightPanel.add(txtToDate);
        rightPanel.add(btnSearch);

        add(leftPanel, BorderLayout.WEST);
        add(rightPanel, BorderLayout.EAST);
    }

    /** Hàm `createToolbarButton`: tạo thanhcongcu nút. */
    private JButton createToolbarButton(String text, Icon icon, boolean isSelected) {
        JButton btn = new JButton(text, icon) {
            /** Hàm `paintComponent`: `paint` thành phần. */
            @Override
            protected void paintComponent(Graphics g) {
                if (isSelected) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(new Color(220, 233, 246));
                    g2.fill(new RoundRectangle2D.Float(1, 1, getWidth() - 2, getHeight() - 2, 4, 4));
                    g2.setColor(new Color(75, 145, 215));
                    g2.draw(new RoundRectangle2D.Float(1, 1, getWidth() - 3, getHeight() - 3, 4, 4));
                    g2.dispose();
                }
                super.paintComponent(g);
            }
        };

        btn.setFont(UngDungPhongChu.BODY);
        btn.setForeground(new Color(25, 35, 45));
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(!isSelected);
        btn.setOpaque(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setIconTextGap(4);
        btn.setMargin(new Insets(2, 6, 2, 6));

        if (!isSelected) {
            btn.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
            btn.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    btn.setBorder(BorderFactory.createCompoundBorder(
                            new LineBorder(new Color(185, 205, 225), 1),
                            new EmptyBorder(1, 5, 1, 5)
                    ));
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    btn.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
                }
            });
        }

        return btn;
    }

    /** Hàm `createDateTimeBox`: tạo ngày thời gian `Box`. */
    private JTextField createDateTimeBox(String text) {
        JTextField tf = new JTextField(text);
        tf.setFont(UngDungPhongChu.BODY);
        tf.setEditable(false);
        tf.setPreferredSize(new Dimension(135, 24));
        tf.setBackground(Color.WHITE);
        tf.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Icon mũi tên trỏ xuống bên phải
        tf.setLayout(new BorderLayout());
        JLabel arrow = new JLabel(NhaThuocBieuTuong.createSmallArrowDownIcon());
        arrow.setBorder(new EmptyBorder(0, 0, 0, 4));
        tf.add(arrow, BorderLayout.EAST);

        tf.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(165, 180, 196), 1),
                new EmptyBorder(1, 6, 1, 18)
        ));
        return tf;
    }

    /** Hàm `createSearchButton`: tạo tìm kiếm nút. */
    private JButton createSearchButton() {
        JButton btn = new JButton("Tìm Kiếm", NhaThuocBieuTuong.createSearchIcon()) {
            /** Hàm `paintComponent`: `paint` thành phần. */
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();

                GradientPaint gp = new GradientPaint(0, 0, new Color(248, 250, 252), 0, h, new Color(225, 234, 244));
                g2.setPaint(gp);
                g2.fill(new RoundRectangle2D.Float(1, 1, w - 2, h - 2, 4, 4));

                g2.setColor(new Color(155, 175, 198));
                g2.draw(new RoundRectangle2D.Float(1, 1, w - 3, h - 3, 4, 4));

                g2.dispose();
                super.paintComponent(g);
            }
        };

        btn.setFont(UngDungPhongChu.BODY_BOLD);
        btn.setForeground(new Color(25, 45, 75));
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setIconTextGap(4);
        btn.setMargin(new Insets(2, 8, 2, 4));

        // Thêm mũi tên dropdown nhỏ ▾ bên phải chữ
        btn.setLayout(new BorderLayout());
        JLabel arrow = new JLabel(NhaThuocBieuTuong.createSmallArrowDownIcon());
        arrow.setBorder(new EmptyBorder(0, 0, 0, 6));
        btn.add(arrow, BorderLayout.EAST);
        btn.setPreferredSize(new Dimension(115, 26));

        return btn;
    }

    /** Hàm `setOnSearchListener`: gán tải tìm kiếm bolangnghe. */
    public void setOnSearchListener(Consumer<String> listener) {
        this.searchListener = listener;
    }

    /** Hàm `triggerSearch`: `trigger` tìm kiếm. */
    private void triggerSearch() {
        if (searchListener != null) {
            searchListener.accept(getTicketCode());
        }
    }

    /** Hàm `getTicketCode`: lấy phiếu mã. */
    public String getTicketCode() {
        return txtTicketCode.getText();
    }

    /** Hàm `getBtnMenu`: lấy `Btn` menu. */
    public JButton getBtnMenu() {
        return btnMenu;
    }

    /** Hàm `getBtnReport`: lấy `Btn` baocao. */
    public JButton getBtnReport() {
        return btnReport;
    }

    /** Hàm `getBtnList`: lấy `Btn` `List`. */
    public JButton getBtnList() {
        return btnList;
    }

    /** Hàm `getBtnCreate`: lấy `Btn` tạo. */
    public JButton getBtnCreate() {
        return btnCreate;
    }

    /** Hàm `getBtnSearch`: lấy `Btn` tìm kiếm. */
    public JButton getBtnSearch() {
        return btnSearch;
    }

    /** Hàm `getTxtTicketCode`: lấy `Txt` phiếu mã. */
    public JTextField getTxtTicketCode() {
        return txtTicketCode;
    }

    /** Hàm `getTxtFromDate`: lấy `Txt` tu ngày. */
    public JTextField getTxtFromDate() {
        return txtFromDate;
    }

    /** Hàm `getTxtToDate`: lấy `Txt` den ngày. */
    public JTextField getTxtToDate() {
        return txtToDate;
    }
}
