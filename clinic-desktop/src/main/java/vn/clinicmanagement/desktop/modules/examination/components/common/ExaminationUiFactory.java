package vn.clinicmanagement.desktop.modules.examination.components.common;

import vn.clinicmanagement.desktop.core.config.UngDungPhongChu;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Tiện ích dựng UI và định dạng số liệu dùng chung cho các màn hình chỉ định.
 */
public final class ExaminationUiFactory {

    private static final DecimalFormat MONEY_FORMAT;

    static {
        DecimalFormatSymbols sym = new DecimalFormatSymbols(Locale.US);
        sym.setGroupingSeparator(',');
        MONEY_FORMAT = new DecimalFormat("#,##0", sym);
    }

    private ExaminationUiFactory() {}

    public static JTabbedPane createStyledOrderTabbedPane(boolean roundedStyle) {
        JTabbedPane tp = new JTabbedPane();
        tp.setFont(UngDungPhongChu.BODY_BOLD);
        tp.setBackground(roundedStyle ? new Color(214, 228, 245) : new Color(235, 240, 248));
        tp.setUI(new BasicTabbedPaneUI() {
            @Override
            protected void installDefaults() {
                super.installDefaults();
                if (roundedStyle) {
                    tabAreaInsets = new Insets(2, 4, 0, 4);
                    contentBorderInsets = new Insets(1, 0, 0, 0);
                    tabInsets = new Insets(4, 12, 4, 12);
                }
            }

            @Override
            protected void paintTabArea(Graphics g, int tabPlacement, int selectedIndex) {
                if (roundedStyle) {
                    g.setColor(new Color(214, 228, 245));
                    g.fillRect(0, 0, tabPane.getWidth(), calculateTabAreaHeight(tabPlacement, runCount, maxTabHeight));
                }
                super.paintTabArea(g, tabPlacement, selectedIndex);
            }

            @Override
            protected void paintTabBackground(Graphics g, int tabPlacement, int tabIndex, int x, int y, int w, int h, boolean isSelected) {
                Graphics2D g2 = (Graphics2D) g.create();
                if (roundedStyle) {
                    g2.setColor(isSelected ? new Color(245, 248, 253) : new Color(204, 220, 240));
                    g2.fillRoundRect(x + 1, y + 1, w - 2, h + 2, 4, 4);
                } else {
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(isSelected ? Color.WHITE : new Color(212, 226, 243));
                    g2.fillRect(x, y, w, h);
                }
                g2.dispose();
            }

            @Override
            protected void paintTabBorder(Graphics g, int tabPlacement, int tabIndex, int x, int y, int w, int h, boolean isSelected) {
                if (roundedStyle) {
                    g.setColor(isSelected ? new Color(150, 175, 205) : new Color(175, 195, 220));
                    g.drawRoundRect(x + 1, y + 1, w - 3, h + 2, 4, 4);
                } else {
                    g.setColor(new Color(175, 195, 220));
                    g.drawRect(x, y, w, h);
                }
            }

            @Override
            protected void paintContentBorder(Graphics g, int tabPlacement, int selectedIndex) {
                if (roundedStyle) {
                    int th = calculateTabAreaHeight(tabPlacement, runCount, maxTabHeight);
                    g.setColor(new Color(165, 185, 212));
                    g.drawLine(0, th, tabPane.getWidth(), th);
                } else {
                    super.paintContentBorder(g, tabPlacement, selectedIndex);
                }
            }

            @Override
            protected void paintFocusIndicator(Graphics g, int tabPlacement, Rectangle[] rects, int tabIndex, Rectangle iconRect, Rectangle textRect, boolean isSelected) {}
        });
        return tp;
    }

    public static JButton createToolbarButton(String text, String iconType, boolean hasDropdownArrow, boolean verticalText) {
        String labelText = hasDropdownArrow ? text + " ▾" : text;
        JButton btn = new JButton(labelText, new ExaminationOrderIcons.SuppliesToolbarIcon(iconType));
        btn.setFont(verticalText ? UngDungPhongChu.SMALL : UngDungPhongChu.BODY);
        btn.setForeground(new Color(20, 25, 35));
        if (verticalText) {
            btn.setVerticalTextPosition(SwingConstants.BOTTOM);
            btn.setHorizontalTextPosition(SwingConstants.CENTER);
            btn.setIconTextGap(2);
            btn.setBorder(new EmptyBorder(2, 6, 2, 6));
        } else {
            btn.setBorder(new EmptyBorder(4, 10, 4, 10));
        }
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        Color hoverBg = verticalText ? new Color(228, 236, 248) : new Color(230, 238, 250);
        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(hoverBg);
                btn.setOpaque(true);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                btn.setOpaque(false);
            }
        });
        return btn;
    }

    public static JButton createFooterButton(String text, Icon icon, int width, Color textColor) {
        JButton btn = new JButton(text, icon);
        btn.setFont(UngDungPhongChu.BODY_BOLD);
        if (textColor != null) btn.setForeground(textColor);
        btn.setBackground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(width, 28));
        btn.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(185, 195, 208), 1),
                new EmptyBorder(2, 4, 2, 4)));
        return btn;
    }

    public static JButton createMiniBrowseButton(int width, int height) {
        JButton btn = new JButton("...");
        btn.setFont(UngDungPhongChu.SMALL_BOLD);
        btn.setPreferredSize(new Dimension(width, height));
        btn.setMargin(new Insets(0, 0, 0, 0));
        btn.setBackground(new Color(242, 244, 248));
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(new LineBorder(new Color(185, 195, 208), 1));
        return btn;
    }

    public static JPanel createMetricBox(String title, JLabel valLabel) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        p.setOpaque(false);
        JLabel lTitle = new JLabel(title);
        lTitle.setFont(UngDungPhongChu.SMALL);
        lTitle.setForeground(new Color(45, 50, 60));
        p.add(lTitle);
        p.add(valLabel);
        return p;
    }

    public static JPanel createMetricLabel(String title, String val, Color valColor) {
        JLabel lblVal = new JLabel(val);
        lblVal.setFont(UngDungPhongChu.BODY_BOLD);
        lblVal.setForeground(valColor);
        return createMetricBox(title, lblVal);
    }

    public static void expandAllNodes(JTree tree, int startingIndex, int rowCount) {
        for (int i = startingIndex; i < rowCount; ++i) {
            tree.expandRow(i);
        }
        if (tree.getRowCount() != rowCount) {
            expandAllNodes(tree, rowCount, tree.getRowCount());
        }
    }

    public static String formatMoney(BigDecimal value) {
        return MONEY_FORMAT.format(value != null ? value : BigDecimal.ZERO);
    }

    public static BigDecimal parseDecimalSafe(String raw) {
        if (raw == null || raw.isBlank()) return BigDecimal.ZERO;
        try {
            return new BigDecimal(raw.replace(",", "").trim());
        } catch (NumberFormatException ex) {
            return BigDecimal.ZERO;
        }
    }

    public static String safeText(String val, String fallback) {
        return val != null && !val.isBlank() ? val.trim() : fallback;
    }
}
