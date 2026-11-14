package vn.clinicmanagement.desktop.core.config;

import java.awt.*;

public final class GiaoDienHangSo {
    private GiaoDienHangSo() {}

    // Primary Colors
    public static final Color COLOR_PRIMARY = new Color(24, 119, 242);
    public static final Color COLOR_TEAL_DARK = new Color(30, 107, 137);
    public static final Color COLOR_TEAL_LIGHT = new Color(42, 135, 168);
    public static final Color COLOR_TAB_ACTIVE = new Color(21, 101, 192);

    // Background Colors
    public static final Color BG_ROOT = new Color(238, 240, 243);
    public static final Color BG_TOOLBAR = new Color(243, 244, 246);
    public static final Color BG_STATUS = new Color(241, 243, 246);
    public static final Color BG_PANEL = new Color(248, 249, 251);
    public static final Color COLOR_BG_GRAY = BG_PANEL;
    public static final Color COLOR_MENU_BG = BG_TOOLBAR;
    public static final Color COLOR_TAB_BAR_BG = new Color(225, 232, 240);

    // Borders
    public static final Color BORDER_SEPARATOR = new Color(195, 202, 211);
    public static final Color BORDER_GRID = new Color(225, 230, 235);
    public static final Color COLOR_BORDER = BORDER_SEPARATOR;

    // Fonts: dùng AppFonts làm nguồn duy nhất. FONT_TITLE/FONT_SUBTITLE là chữ banner dạng serif (logo) nên giữ riêng.
    public static final Font FONT_TITLE = new Font("Times New Roman", Font.BOLD, 22);
    public static final Font FONT_SUBTITLE = new Font("Times New Roman", Font.ITALIC, 14);
    public static final Font FONT_SECTION = UngDungPhongChu.SECTION;
    public static final Font FONT_REGULAR = UngDungPhongChu.BODY;
    public static final Font FONT_BOLD = UngDungPhongChu.BODY_BOLD;
    public static final Font FONT_SMALL = UngDungPhongChu.SMALL;
}
