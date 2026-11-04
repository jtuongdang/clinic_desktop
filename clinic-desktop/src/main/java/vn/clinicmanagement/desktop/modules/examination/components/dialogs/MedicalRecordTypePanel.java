package vn.clinicmanagement.desktop.modules.examination.components.dialogs;

import vn.clinicmanagement.desktop.core.config.AppFonts;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.function.Consumer;

/**
 * Khung chuyển loại bệnh án (EHC Medical Record Type Panel).
 * Khớp 100% giao diện trong ảnh chụp thực tế:
 * - Tiêu đề cửa sổ: ĐHC - Bệnh Án --- Người đăng nhập : sennv --- --- Version: 2022.8.17.1 ---
 * - Tiêu đề lớn màu đỏ sẫm: BỆNH ÁN
 * - Lưới 4 cột x 5 hàng chứa 17 loại bệnh án:
 *   + Hàng 1: [Khám Bệnh] (nổi bật màu vàng hổ phách #F6C15D), [Ngoại Trú], [Tai - Mũi - Họng], [Răng - Hàm - Mặt]
 *   + Hàng 2: [Đông Y], [Đái Tháo Đường], [Tăng Huyết Áp], [Bàn Chân]
 *   + Hàng 3: [Ung Bướu], [Bỏng], [Mắt], [Viêm Gan B]
 *   + Hàng 4: [Hen], [Phổi / Tắc Nghẽn Mãn T...], [Tử Từng Trước Khi Vào Viện / Cấp Cứu], [Phá Thai]
 *   + Hàng 5: [Thận Nhân Tạo], (3 ô trống)
 */
public class MedicalRecordTypePanel extends JPanel {

    public static final String[] RECORD_TYPES = {
            "Khám Bệnh",
            "Ngoại Trú",
            "Tai - Mũi - Họng",
            "Răng - Hàm - Mặt",
            "Đông Y",
            "Đái Tháo Đường",
            "Tăng Huyết Áp",
            "Bàn Chân",
            "Ung Bướu",
            "Bỏng",
            "Mắt",
            "Viêm Gan B",
            "Hen",
            "Phổi\nTắc Nghẽn Mãn T...",
            "Tử Từng Trước Khi Vào Viện\nCấp Cứu",
            "Phá Thai",
            "Thận Nhân Tạo"
    };

    private String selectedType = "Khám Bệnh";
    private final Consumer<String> onSelectCallback;
    private final Runnable onCloseCallback;
    private Point mouseClickPoint;

    public MedicalRecordTypePanel(Consumer<String> onSelectCallback, Runnable onCloseCallback) {
        this.onSelectCallback = onSelectCallback;
        this.onCloseCallback = onCloseCallback;

        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createLineBorder(new Color(130, 145, 165), 1));
        setPreferredSize(new Dimension(640, 440));

        // 1. Thanh tiêu đề cửa sổ phẳng chuẩn EHC
        add(buildTitleBar(), BorderLayout.NORTH);

        // 2. Nội dung thân: Tiêu đề BỆNH ÁN + Lưới 17 nút chọn loại bệnh án
        add(buildBodyContent(), BorderLayout.CENTER);
    }

    private JPanel buildTitleBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setPreferredSize(new Dimension(640, 26));
        bar.setBackground(new Color(242, 245, 250));
        bar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(210, 218, 228)));

        // Kéo thả di chuyển cửa sổ (nếu nằm trong Window)
        bar.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                mouseClickPoint = e.getPoint();
            }
        });
        bar.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                Window win = SwingUtilities.getWindowAncestor(MedicalRecordTypePanel.this);
                if (win != null && mouseClickPoint != null) {
                    Point curr = win.getLocation();
                    win.setLocation(curr.x + e.getX() - mouseClickPoint.x,
                                   curr.y + e.getY() - mouseClickPoint.y);
                }
            }
        });

        // Tiêu đề trái kèm biểu tượng chữ thập đỏ
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        left.setOpaque(false);

        JLabel lblIcon = new JLabel(new RedCrossEmblemIcon());
        JLabel lblTitle = new JLabel("ĐHC - Bệnh Án  --- Người đăng nhập : sennv ---  --- Version: 2022.8.17.1 ---");
        lblTitle.setFont(AppFonts.SMALL);
        lblTitle.setForeground(new Color(40, 45, 55));

        left.add(lblIcon);
        left.add(lblTitle);
        bar.add(left, BorderLayout.WEST);

        // Nút đóng ✕ bên phải
        JButton btnClose = new JButton("✕") {
            private boolean isHover = false;
            {
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) { isHover = true; repaint(); }
                    @Override public void mouseExited(MouseEvent e) { isHover = false; repaint(); }
                });
            }
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (isHover) {
                    g2.setColor(new Color(232, 17, 35));
                    g2.fillRect(0, 0, getWidth(), getHeight());
                    g2.setColor(Color.WHITE);
                } else {
                    g2.setColor(new Color(100, 110, 125));
                }
                FontMetrics fm = g2.getFontMetrics();
                int tx = (getWidth() - fm.stringWidth("✕")) / 2;
                int ty = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString("✕", tx, ty);
                g2.dispose();
            }
        };
        btnClose.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnClose.setPreferredSize(new Dimension(32, 25));
        btnClose.setFocusPainted(false);
        btnClose.setContentAreaFilled(false);
        btnClose.setBorderPainted(false);
        btnClose.setOpaque(false);
        btnClose.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnClose.addActionListener(e -> {
            if (onCloseCallback != null) {
                onCloseCallback.run();
            }
        });

        bar.add(btnClose, BorderLayout.EAST);
        return bar;
    }

    private JPanel buildBodyContent() {
        JPanel body = new JPanel(new BorderLayout(0, 10));
        body.setBackground(Color.WHITE);
        body.setBorder(new EmptyBorder(12, 16, 16, 16));

        // Tiêu đề lớn màu đỏ sẫm: BỆNH ÁN
        JLabel lblHeading = new JLabel("BỆNH ÁN");
        lblHeading.setFont(new Font(AppFonts.FAMILY, Font.BOLD, 16));
        lblHeading.setForeground(new Color(145, 20, 20)); // Màu đỏ sẫm chuẩn EHC
        body.add(lblHeading, BorderLayout.NORTH);

        // Lưới 5 hàng x 4 cột nút chọn loại bệnh án
        JPanel grid = new JPanel(new GridLayout(5, 4, 10, 10));
        grid.setBackground(Color.WHITE);

        for (int i = 0; i < 20; i++) {
            if (i < RECORD_TYPES.length) {
                String typeName = RECORD_TYPES[i];
                boolean isSelected = "Khám Bệnh".equalsIgnoreCase(typeName);
                JButton btnTile = createTypeTile(typeName, isSelected);
                grid.add(btnTile);
            } else {
                // Ô trống giữ khung lưới 4 cột hoàn hảo
                JPanel emptyCell = new JPanel();
                emptyCell.setOpaque(false);
                grid.add(emptyCell);
            }
        }

        body.add(grid, BorderLayout.CENTER);
        return body;
    }

    private JButton createTypeTile(String text, boolean isHighlighted) {
        String htmlText = "<html><center>" + text.replace("\n", "<br>") + "</center></html>";
        JButton btn = new JButton(htmlText) {
            private boolean isHover = false;
            {
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) { isHover = true; repaint(); }
                    @Override public void mouseExited(MouseEvent e) { isHover = false; repaint(); }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();

                if (isHighlighted) {
                    // Nổi bật màu vàng hổ phách rực rỡ chuẩn ảnh chụp EHC (#F7BA4C)
                    g2.setColor(isHover ? new Color(250, 195, 85) : new Color(247, 186, 76));
                    g2.fillRect(0, 0, w, h);

                    // Viền cam đậm nổi bật
                    g2.setColor(new Color(205, 120, 10));
                    g2.setStroke(new BasicStroke(1.8f));
                    g2.drawRect(0, 0, w - 1, h - 1);
                } else {
                    // Ô xám nhạt tiêu chuẩn (#EDEDED)
                    g2.setColor(isHover ? new Color(245, 248, 252) : new Color(236, 239, 244));
                    g2.fillRect(0, 0, w, h);

                    // Viền mảnh
                    g2.setColor(isHover ? new Color(165, 185, 215) : new Color(215, 222, 230));
                    g2.setStroke(new BasicStroke(1.0f));
                    g2.drawRect(0, 0, w - 1, h - 1);
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };

        btn.setFont(AppFonts.SMALL_BOLD);
        btn.setForeground(new Color(25, 30, 40));
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setOpaque(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(140, 58));

        btn.addActionListener(e -> {
            selectedType = text.replace("\n", " ");
            if (onSelectCallback != null) {
                onSelectCallback.accept(selectedType);
            }
            if (onCloseCallback != null) {
                onCloseCallback.run();
            }
        });

        return btn;
    }

    public String getSelectedType() {
        return selectedType;
    }

    // =========================================================================
    // VECTOR ICON: BIỂU TƯỢNG VUÔNG ĐỎ CHỮ THẬP TRẮNG Y TẾ
    // =========================================================================

    private static class RedCrossEmblemIcon implements Icon {
        @Override public int getIconWidth() { return 14; }
        @Override public int getIconHeight() { return 14; }
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(new Color(205, 35, 35));
            g2.fillRoundRect(x + 1, y + 1, 12, 12, 2, 2);

            g2.setColor(Color.WHITE);
            // Thanh dọc chữ thập
            g2.fillRect(x + 5, y + 3, 4, 8);
            // Thanh ngang chữ thập
            g2.fillRect(x + 3, y + 5, 8, 4);

            g2.dispose();
        }
    }
}
