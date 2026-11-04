package vn.clinicmanagement.desktop.modules.home;

import vn.clinicmanagement.desktop.core.base.BaseViewPanel;
import vn.clinicmanagement.desktop.core.config.AppFonts;
import vn.clinicmanagement.desktop.core.config.UIConstants;
import vn.clinicmanagement.desktop.modules.pharmacy.components.PharmacyIcons;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;

/**
 * Màn hình Trang Chủ / Chào Mừng giao diện chuẩn bệnh viện thông minh EHC.
 * Khớp 100% ảnh chụp mẫu:
 * - Banner xanh đậm: "PHẦN MỀM QUẢN LÝ BỆNH VIỆN THÔNG MINH EHC"
 * - Ảnh chụp đội ngũ y bác sĩ, logo Bệnh Viện Ung Bướu Hà Nội và logo EHC
 * - Hai phím cuộn nhanh nổi bên phải (Up / Down)
 */
public class HomeWelcomePanel extends BaseViewPanel {

    private static BufferedImage heroImage = null;

    static {
        loadHeroImage();
    }

    private static void loadHeroImage() {
        try {
            // 1. Thử đọc từ classpath resource
            InputStream is = HomeWelcomePanel.class.getResourceAsStream("/images/ehc_home_hero.jpg");
            if (is != null) {
                heroImage = ImageIO.read(is);
                is.close();
                return;
            }

            // 2. Thử đọc từ thư mục file hệ thống dự án
            File file = new File("desktop/clinic-desktop/src/main/resources/images/ehc_home_hero.jpg");
            if (file.exists()) {
                heroImage = ImageIO.read(file);
                return;
            }

            File fileAlt = new File("src/main/resources/images/ehc_home_hero.jpg");
            if (fileAlt.exists()) {
                heroImage = ImageIO.read(fileAlt);
            }
        } catch (Exception ignored) {
            // Fallback sang vẽ vector nếu không tìm thấy file ảnh
        }
    }

    public HomeWelcomePanel() {
        super("HOME", "Chào Mừng", "🏠");
        initUI();
    }

    private void initUI() {
        setBackground(Color.WHITE);
        setLayout(new BorderLayout());

        // 1. BANNER TIÊU ĐỀ XANH ĐẬM TRÊN CÙNG
        JPanel bannerHeader = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();

                // Gradient xanh thẫm chuẩn EHC
                GradientPaint gp = new GradientPaint(
                        0, 0, new Color(22, 94, 140),
                        w, 0, new Color(30, 115, 160)
                );
                g2.setPaint(gp);
                g2.fillRect(0, 0, w, h);

                g2.setColor(new Color(255, 255, 255, 45));
                g2.drawLine(0, 1, w, 1);
                g2.setColor(new Color(0, 0, 0, 45));
                g2.drawLine(0, h - 1, w, h - 1);
                g2.dispose();
            }
        };
        bannerHeader.setPreferredSize(new Dimension(100, 70));
        bannerHeader.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.CENTER;

        JLabel lblTitle = new JLabel("PHẦN MỀM QUẢN LÝ BỆNH VIỆN THÔNG MINH EHC");
        lblTitle.setFont(UIConstants.FONT_TITLE);
        lblTitle.setForeground(Color.WHITE);
        bannerHeader.add(lblTitle, gbc);

        gbc.gridy = 1;
        gbc.insets = new Insets(3, 0, 0, 0);
        JLabel lblSubtitle = new JLabel("Quản lý tổng thể bệnh viện, tự động hóa thiết bị y tế, tối ưu quy trình, tăng công suất, chất lượng KCB");
        lblSubtitle.setFont(UIConstants.FONT_SUBTITLE);
        lblSubtitle.setForeground(new Color(225, 248, 240));
        bannerHeader.add(lblSubtitle, gbc);

        add(bannerHeader, BorderLayout.NORTH);

        // 2. KHU VỰC HERO CHÍNH CÓ NÚT CUỘN NỔI
        add(new MedicalHeroPanel(), BorderLayout.CENTER);
    }

    @Override
    public void refreshData() {}

    private static class MedicalHeroPanel extends JPanel {
        private final JButton btnScrollUp;
        private final JButton btnScrollDown;

        public MedicalHeroPanel() {
            setBackground(Color.WHITE);
            setLayout(null); // Tọa độ tự do cho cụm phím cuộn nổi bên phải

            btnScrollUp = createFloatingButton(PharmacyIcons.createChevronUpIcon());
            btnScrollDown = createFloatingButton(PharmacyIcons.createChevronDownIcon());

            add(btnScrollUp);
            add(btnScrollDown);
        }

        private JButton createFloatingButton(Icon icon) {
            JButton btn = new JButton(icon) {
                private boolean isHovered = false;

                {
                    addMouseListener(new MouseAdapter() {
                        @Override public void mouseEntered(MouseEvent e) { isHovered = true; repaint(); }
                        @Override public void mouseExited(MouseEvent e) { isHovered = false; repaint(); }
                    });
                }

                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    int w = getWidth();
                    int h = getHeight();

                    // Màu xám bo góc mờ đặc trưng chuẩn ảnh chụp
                    g2.setColor(isHovered ? new Color(90, 100, 110, 235) : new Color(125, 135, 145, 210));
                    g2.fill(new RoundRectangle2D.Float(1, 1, w - 2, h - 2, 8, 8));
                    g2.dispose();

                    super.paintComponent(g);
                }
            };

            btn.setFocusPainted(false);
            btn.setContentAreaFilled(false);
            btn.setBorderPainted(false);
            btn.setOpaque(false);
            btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            return btn;
        }

        @Override
        public void doLayout() {
            super.doLayout();
            int w = getWidth();
            int h = getHeight();

            // Đặt cụm nút cuộn nổi bên phải giống hệt ảnh chụp
            int btnW = 46;
            int btnH = 42;
            int btnX = Math.max(10, w - btnW - 14);
            int btnY = Math.max(10, (h / 2) - btnH - 10);

            btnScrollUp.setBounds(btnX, btnY, btnW, btnH);
            btnScrollDown.setBounds(btnX, btnY + btnH + 6, btnW, btnH);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            int w = getWidth();
            int h = getHeight();

            if (heroImage != null) {
                // Vẽ ảnh chụp thật chất lượng cao kéo căng phủ kín khung nhìn
                g2.drawImage(heroImage, 0, 0, w, h, null);
            } else {
                // Fallback nếu không có file ảnh: vẽ vector
                drawVectorFallback(g2, w, h);
            }

            g2.dispose();
        }

        private void drawVectorFallback(Graphics2D g2, int w, int h) {
            GradientPaint bgGradient = new GradientPaint(
                    0, 0, new Color(242, 248, 252),
                    w, h, new Color(225, 238, 247)
            );
            g2.setPaint(bgGradient);
            g2.fillRect(0, 0, w, h);

            drawMedicalCross(g2, 120, 80, 50, new Color(210, 230, 245, 120));
            drawMedicalCross(g2, w - 200, 100, 80, new Color(210, 230, 245, 90));
            drawMedicalCross(g2, w / 2 - 60, h / 2 - 40, 110, new Color(215, 235, 248, 70));

            drawDoctorTeam(g2, w, h);
            drawHanoiHospitalLogo(g2, 40, h - 85);
            drawEhcLogo(g2, w - 240, h - 90);
        }

        private void drawMedicalCross(Graphics2D g2, int cx, int cy, int size, Color color) {
            int arm = size / 3;
            g2.setColor(color);
            g2.fillRect(cx - arm / 2, cy - size / 2, arm, size);
            g2.fillRect(cx - size / 2, cy - arm / 2, size, arm);
        }

        private void drawHanoiHospitalLogo(Graphics2D g2, int x, int y) {
            // Huy hiệu tròn Bệnh Viện Ung Bướu Hà Nội
            g2.setColor(new Color(25, 65, 125));
            g2.fillOval(x, y, 46, 46);
            g2.setColor(Color.WHITE);
            g2.fillOval(x + 4, y + 4, 38, 38);

            g2.setColor(new Color(220, 45, 45));
            g2.fillRect(x + 20, y + 12, 6, 22);
            g2.fillRect(x + 12, y + 20, 22, 6);

            g2.setColor(new Color(20, 55, 110));
            g2.setFont(new Font(AppFonts.FAMILY, Font.BOLD, 15));
            g2.drawString("HANOI ONCOLOGY HOSPITAL", x + 56, y + 22);

            g2.setColor(new Color(15, 95, 75));
            g2.setFont(new Font(AppFonts.FAMILY, Font.BOLD, 16));
            g2.drawString("BỆNH VIỆN UNG BƯỚU HÀ NỘI", x + 56, y + 42);
        }

        private void drawDoctorTeam(Graphics2D g2, int w, int h) {
            int baseY = h - 20;
            drawDoctorFigure(g2, (int) (w * 0.20), (int) (baseY * 0.95), 150, new Color(245, 210, 190), new Color(45, 30, 20), new Color(255, 255, 255), true);
            drawDoctorFigure(g2, (int) (w * 0.35), (int) (baseY * 0.90), 140, new Color(215, 175, 145), new Color(30, 20, 15), new Color(60, 120, 175), false);
            drawDoctorFigure(g2, (int) (w * 0.48), (int) (baseY * 0.92), 155, new Color(240, 205, 175), new Color(25, 20, 20), new Color(65, 130, 190), false);
            drawDoctorFigure(g2, (int) (w * 0.60), (int) (baseY * 0.93), 150, new Color(250, 220, 200), new Color(175, 120, 60), new Color(70, 140, 200), true);
            drawMainSeniorDoctor(g2, (int) (w * 0.74), baseY, 220);
        }

        private void drawDoctorFigure(Graphics2D g2, int x, int y, int height, Color skin, Color hair, Color scrubColor, boolean isFemale) {
            int headR = height / 5;
            int shoulderW = (int) (headR * 2.8);

            g2.setColor(scrubColor);
            g2.fillRoundRect(x - shoulderW / 2, y - (int) (height * 0.65), shoulderW, (int) (height * 0.75), 25, 25);

            g2.setColor(Color.WHITE);
            Polygon coat = new Polygon();
            coat.addPoint(x - headR, y - (int) (height * 0.65));
            coat.addPoint(x + headR, y - (int) (height * 0.65));
            coat.addPoint(x, y - (int) (height * 0.40));
            g2.fillPolygon(coat);

            g2.setColor(skin);
            g2.fillOval(x - headR, y - height, headR * 2, (int) (headR * 2.2));

            g2.setColor(hair);
            if (isFemale) {
                g2.fillArc(x - headR - 3, y - height - 4, headR * 2 + 6, headR * 2, 0, 180);
                g2.fillRect(x - headR - 4, y - height + headR / 2, 8, headR);
                g2.fillRect(x + headR - 4, y - height + headR / 2, 8, headR);
            } else {
                g2.fillArc(x - headR - 2, y - height - 5, headR * 2 + 4, (int) (headR * 1.6), 0, 180);
            }
        }

        private void drawMainSeniorDoctor(Graphics2D g2, int x, int y, int height) {
            int headR = height / 5;
            int shoulderW = (int) (headR * 3.4);

            g2.setColor(new Color(250, 252, 255));
            g2.fillRoundRect(x - shoulderW / 2, y - (int) (height * 0.68), shoulderW, (int) (height * 0.75), 30, 30);
            g2.setColor(new Color(215, 222, 230));
            g2.drawRoundRect(x - shoulderW / 2, y - (int) (height * 0.68), shoulderW, (int) (height * 0.75), 30, 30);

            g2.setColor(new Color(230, 240, 252));
            Polygon shirt = new Polygon();
            shirt.addPoint(x - (int) (headR * 1.1), y - (int) (height * 0.68));
            shirt.addPoint(x + (int) (headR * 1.1), y - (int) (height * 0.68));
            shirt.addPoint(x, y - (int) (height * 0.35));
            g2.fillPolygon(shirt);

            g2.setColor(new Color(24, 100, 185));
            Polygon tie = new Polygon();
            tie.addPoint(x - 8, y - (int) (height * 0.62));
            tie.addPoint(x + 8, y - (int) (height * 0.62));
            tie.addPoint(x + 12, y - (int) (height * 0.25));
            tie.addPoint(x, y - (int) (height * 0.20));
            tie.addPoint(x - 12, y - (int) (height * 0.25));
            g2.fillPolygon(tie);

            g2.setColor(new Color(55, 65, 75));
            g2.setStroke(new BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawArc(x - headR - 10, y - (int) (height * 0.68), (headR + 10) * 2, headR * 2, 190, 160);

            g2.setColor(new Color(250, 218, 198));
            g2.fillOval(x - headR, y - height, headR * 2, (int) (headR * 2.2));

            g2.setColor(new Color(165, 125, 75));
            g2.fillArc(x - headR - 3, y - height - 6, headR * 2 + 6, (int) (headR * 1.7), 0, 180);
        }

        private void drawEhcLogo(Graphics2D g2, int x, int y) {
            g2.setColor(new Color(125, 195, 65));
            Path2D leaf1 = new Path2D.Double();
            leaf1.moveTo(x, y + 25);
            leaf1.curveTo(x + 20, y - 5, x + 50, y + 10, x + 35, y + 45);
            leaf1.curveTo(x + 15, y + 50, x - 5, y + 45, x, y + 25);
            g2.fill(leaf1);

            g2.setColor(new Color(20, 105, 75));
            Path2D leaf2 = new Path2D.Double();
            leaf2.moveTo(x + 20, y + 30);
            leaf2.curveTo(x + 40, y + 5, x + 70, y + 20, x + 55, y + 50);
            leaf2.curveTo(x + 35, y + 55, x + 15, y + 48, x + 20, y + 30);
            g2.fill(leaf2);

            g2.setColor(new Color(245, 135, 20));
            Path2D flame = new Path2D.Double();
            flame.moveTo(x + 30, y + 15);
            flame.curveTo(x + 32, y - 5, x + 44, y - 5, x + 46, y + 15);
            flame.curveTo(x + 44, y + 24, x + 32, y + 24, x + 30, y + 15);
            g2.fill(flame);

            g2.setColor(new Color(130, 185, 60));
            g2.setFont(new Font(AppFonts.FAMILY, Font.BOLD, 36));
            g2.drawString("EHC", x + 75, y + 38);

            g2.setColor(new Color(105, 160, 50));
            g2.setFont(new Font(AppFonts.FAMILY, Font.ITALIC, 14));
            g2.drawString("Ehealthcare Vietnam", x + 30, y + 66);
        }
    }
}
