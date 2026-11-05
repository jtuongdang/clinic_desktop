package vn.clinicmanagement.desktop.modules.dashboard;

import vn.clinicmanagement.desktop.core.components.RibbonButton;
import vn.clinicmanagement.desktop.core.components.StatusBarPanel;
import vn.clinicmanagement.desktop.core.config.UIConstants;
import vn.clinicmanagement.desktop.core.session.UserSession;
import vn.clinicmanagement.desktop.core.session.SessionLifecycleService;
import vn.clinicmanagement.desktop.core.session.SessionLockOverlay;
import vn.clinicmanagement.desktop.modules.auth.AuthenticationCompositionRoot;
import vn.clinicmanagement.desktop.modules.auth.LoginForm;
import vn.clinicmanagement.application.auth.AuthenticateUserCommand;
import vn.clinicmanagement.application.auth.AuthenticatedUser;
import vn.clinicmanagement.application.common.Result;
import vn.clinicmanagement.desktop.modules.billing.BillingInvoicePanel;
import vn.clinicmanagement.desktop.modules.catalog.CatalogPanel;
import vn.clinicmanagement.desktop.modules.doctor.DoctorStationPanel;
import vn.clinicmanagement.desktop.modules.examination.ExaminationPanel;
import vn.clinicmanagement.desktop.modules.home.HomeWelcomePanel;
import vn.clinicmanagement.desktop.modules.paraclinical.ParaclinicalOrderPanel;
import vn.clinicmanagement.desktop.modules.pharmacy.PharmacyDispensePanel;
import vn.clinicmanagement.desktop.modules.reception.ReceptionPanel;
import vn.clinicmanagement.desktop.modules.reports.ClinicReportPanel;
import vn.clinicmanagement.desktop.modules.system.SystemConfigPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.AWTEventListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.Map;

/**
 * Màn hình Dashboard trung tâm (EHC Hospital Shell)
 * Tự động chuyển đổi giao diện chuyên biệt theo quyền:
 * - Quyền BÁC SĨ (Doctor Station): Giao diện chuyên sâu Điều Trị / Khám Bệnh theo đúng ảnh mẫu
 * - Quyền QUẢN TRỊ VIÊN (Admin): Vỏ điều phối tổng thể hệ thống (dành cho thiết kế sau)
 */
public class MainDashboardFrame extends JFrame {

    private final WorkspaceManager workspaceManager;
    private final Map<String, RibbonButton> ribbonButtons = new HashMap<>();
    private final boolean isDoctor;
    private final SessionLifecycleService sessionLifecycle;
    private final SessionLockOverlay lockOverlay;
    private final Timer sessionMonitor;
    private final AWTEventListener activityMonitor;

    public MainDashboardFrame() {
        this("admin", "Bác sĩ điều trị");
    }

    public MainDashboardFrame(String username, String role) {
        this(username, role, AuthenticationCompositionRoot.createSessionLifecycleService(UserSession.getInstance()));
    }

    public MainDashboardFrame(String username, String role, SessionLifecycleService sessionLifecycle) {
        this.sessionLifecycle = sessionLifecycle;
        this.isDoctor = role != null && (role.contains("Bác sĩ") || role.contains("Doctor"));

        if (isDoctor) {
            setTitle("PHẦN MỀM QUẢN LÝ BỆNH VIỆN - 2018.10.5.1 - BUILD: 10/2018");
        } else {
            setTitle("PHẦN MỀM QUẢN LÝ BỆNH VIỆN - 2021.1.15.1 - 2021 - PC : DESKTOP-E64DCHT");
        }

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setExtendedState(Frame.MAXIMIZED_BOTH);
        setMinimumSize(new Dimension(1200, 750));
        setLocationRelativeTo(null);

        // Content pane layout
        JPanel rootPanel = new JPanel(new BorderLayout());
        setContentPane(rootPanel);

        // Center card container
        CardLayout cardLayout = new CardLayout();
        JPanel centerContainer = new JPanel(cardLayout);
        centerContainer.setBackground(Color.WHITE);
        rootPanel.add(centerContainer, BorderLayout.CENTER);

        // Bottom area (Tabs + Status Bar)
        JPanel bottomContainer = new JPanel(new BorderLayout());

        JPanel tabStripPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        tabStripPanel.setBackground(new Color(216, 228, 242)); // Nền xanh xám nhạt của dải tab dưới chuẩn EHC
        tabStripPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(185, 198, 214)));
        tabStripPanel.setPreferredSize(new Dimension(Integer.MAX_VALUE, 26));
        bottomContainer.add(tabStripPanel, BorderLayout.NORTH);

        StatusBarPanel statusBar = new StatusBarPanel();
        bottomContainer.add(statusBar, BorderLayout.SOUTH);

        rootPanel.add(bottomContainer, BorderLayout.SOUTH);

        // Khởi tạo WorkspaceManager
        this.workspaceManager = new WorkspaceManager(centerContainer, cardLayout, tabStripPanel);

        this.lockOverlay = new SessionLockOverlay(this::reauthenticate, this::logout);
        setGlassPane(lockOverlay);
        this.activityMonitor = event -> {
            if (event.getSource() instanceof Component component
                    && (component == this || SwingUtilities.isDescendingFrom(component, this))) {
                // Re-authentication controls remain interactive while the retained workspace is locked.
                if (component == lockOverlay || SwingUtilities.isDescendingFrom(component, lockOverlay)) return;
                SessionLifecycleService.State state = sessionLifecycle.recordActivity();
                if (state == SessionLifecycleService.State.LOCKED_IDLE
                        || state == SessionLifecycleService.State.LOCKED_ABSOLUTE) {
                    if (event instanceof java.awt.event.InputEvent inputEvent) inputEvent.consume();
                    showSessionLock(state);
                }
            }
        };
        Toolkit.getDefaultToolkit().addAWTEventListener(activityMonitor,
                AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK
                        | AWTEvent.MOUSE_WHEEL_EVENT_MASK | AWTEvent.KEY_EVENT_MASK);
        this.sessionMonitor = new Timer(1000, e -> {
            SessionLifecycleService.State state = sessionLifecycle.evaluate();
            if (state == SessionLifecycleService.State.LOCKED_IDLE
                    || state == SessionLifecycleService.State.LOCKED_ABSOLUTE) showSessionLock(state);
        });
        this.sessionMonitor.start();
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                sessionMonitor.stop();
                Toolkit.getDefaultToolkit().removeAWTEventListener(activityMonitor);
                sessionLifecycle.clear();
            }

            @Override public void windowClosed(WindowEvent e) {
                sessionMonitor.stop();
                Toolkit.getDefaultToolkit().removeAWTEventListener(activityMonitor);
            }
        });

        // Top Ribbon: Xây dựng menu tương ứng theo vai trò Bác sĩ hoặc Admin
        JPanel ribbonBar = isDoctor ? buildDoctorRibbonBar() : buildAdminRibbonBar();
        rootPanel.add(ribbonBar, BorderLayout.NORTH);

        // Đồng bộ Ribbon khi chuyển đổi tab
        this.workspaceManager.setOnTabActivatedListener(viewKey -> {
            for (Map.Entry<String, RibbonButton> entry : ribbonButtons.entrySet()) {
                entry.getValue().setSelected(entry.getKey().equalsIgnoreCase(viewKey));
            }
        });

        // 1. Mặc định mở Trang Chủ
        this.workspaceManager.openView(new HomeWelcomePanel());

        // 2. KHI ĐĂNG NHẬP VỚI QUYỀN BÁC SĨ: Tự động kích hoạt ngay màn hình Điều Trị / Bàn Khám Bác Sĩ
        if (isDoctor) {
            this.workspaceManager.openView(new DoctorStationPanel());
        }
    }

    /**
     * Ribbon Menu dành riêng cho vai trò BÁC SĨ (Khớp 100% ảnh chụp phân hệ Bác sĩ)
     */
    private JPanel buildDoctorRibbonBar() {
        JPanel ribbon = new JPanel(new FlowLayout(FlowLayout.LEFT, 1, 1));
        ribbon.setBackground(UIConstants.COLOR_MENU_BG);
        ribbon.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UIConstants.COLOR_BORDER));

        RibbonButton btnHome = new RibbonButton("Trang Chủ", "🏠", "home",
                k -> workspaceManager.openView(new HomeWelcomePanel()));
        ribbonButtons.put("home", btnHome);

        // Nút "Đón Tiếp" có Dropdown Menu 6 mục chuẩn theo ảnh chụp
        RibbonButton btnReception = new RibbonButton("Đón Tiếp", "📋", "reception", null);
        btnReception.setPopupMenu(buildReceptionPopupMenu());
        ribbonButtons.put("reception", btnReception);

        // Nút "Điều Trị" (Mặc định chọn nổi bật viền cam cho Bác sĩ)
        RibbonButton btnTreatment = new RibbonButton("Điều Trị", "👨‍⚕️", "treatment",
                k -> workspaceManager.openView(new DoctorStationPanel()));
        btnTreatment.setSelected(true);
        ribbonButtons.put("treatment", btnTreatment);

        RibbonButton btnSystem = new RibbonButton("Hệ Thống", "⚙️", "system",
                k -> workspaceManager.openView(new SystemConfigPanel()));
        ribbonButtons.put("system", btnSystem);

        RibbonButton btnCatalog = new RibbonButton("Danh Mục", "📑", "catalog",
                k -> workspaceManager.openView(new CatalogPanel()));
        ribbonButtons.put("catalog", btnCatalog);

        RibbonButton btnUser = new RibbonButton("Người Dùng", "👥", "user", k -> {
            JOptionPane.showMessageDialog(this,
                    "Bác sĩ đang khám: " + UserSession.getInstance().getFullName() + "\n" +
                    "Khoa phòng: " + UserSession.getInstance().getDepartmentName() + "\n" +
                    "Mã trạm: " + UserSession.getInstance().getStationId(),
                    "Thông Tin Bác Sĩ Điều Trị", JOptionPane.INFORMATION_MESSAGE);
        });

        RibbonButton btnHelp = new RibbonButton("Trợ Giúp", "❓", "help", k -> {
            JOptionPane.showMessageDialog(this,
                    "Hướng dẫn sử dụng bàn khám Bác sĩ EHC:\n" +
                    "- Chọn bệnh nhân từ hàng đợi bên trái để nạp bệnh án\n" +
                    "- Nhấn [In], [Dịch vụ], [Thuốc/VT] để xử trí lâm sàng\n" +
                    "- Nhấn [Phác đồ ĐT] để xem hướng dẫn điều trị chuẩn Bộ Y Tế",
                    "Trợ Giúp Khám Bệnh", JOptionPane.INFORMATION_MESSAGE);
        });

        RibbonButton btnExit = new RibbonButton("Kết Thúc", "❌", "exit", k -> {
            int ret = JOptionPane.showConfirmDialog(this, "Bạn có chắc chắn muốn thoát ứng dụng?", "Xác nhận", JOptionPane.YES_NO_OPTION);
            if (ret == JOptionPane.YES_OPTION) {
                System.exit(0);
            }
        });

        ribbon.add(btnHome);
        ribbon.add(btnReception);
        ribbon.add(btnTreatment);
        ribbon.add(btnSystem);
        ribbon.add(btnCatalog);
        ribbon.add(btnUser);
        ribbon.add(btnHelp);
        ribbon.add(btnExit);

        return ribbon;
    }

    /**
     * Menu Dropdown 6 mục chuẩn dưới nút "Đón Tiếp" (Khớp 100% ảnh chụp mẫu):
     * 1. Đăng Ký Khám Bệnh
     * 2. Danh Sách Đón Tiếp
     * 3. Danh Sách Bệnh Nhân
     * 4. Màn Hình Chờ Tổng Hợp
     * 5. Kiosk Đăng Ký Khám
     * 6. Kiosk Đánh Giá Chất Lượng
     */
    private JPopupMenu buildReceptionPopupMenu() {
        JPopupMenu menu = new JPopupMenu();
        menu.setBackground(Color.WHITE);
        menu.setBorder(BorderFactory.createLineBorder(new Color(145, 155, 170), 1));

        String[] items = {
                "Đăng Ký Khám Bệnh",
                "Danh Sách Đón Tiếp",
                "Danh Sách Bệnh Nhân",
                "Màn Hình Chờ Tổng Hợp",
                "Kiosk Đăng Ký Khám",
                "Kiosk Đánh Giá Chất Lượng"
        };

        for (String title : items) {
            JMenuItem mi = new JMenuItem(title);
            mi.setFont(vn.clinicmanagement.desktop.core.config.AppFonts.BODY);
            mi.setBackground(Color.WHITE);
            mi.setForeground(new Color(25, 30, 40));
            mi.setPreferredSize(new Dimension(195, 25));
            mi.setBorder(BorderFactory.createEmptyBorder(2, 14, 2, 8));
            mi.setCursor(new Cursor(Cursor.HAND_CURSOR));

            mi.addActionListener(e -> {
                switch (title) {
                    case "Đăng Ký Khám Bệnh":
                    case "Danh Sách Đón Tiếp":
                        workspaceManager.openView(new ReceptionPanel(sessionLifecycle));
                        break;
                    case "Danh Sách Bệnh Nhân":
                        workspaceManager.openView(new ExaminationPanel());
                        break;
                    case "Màn Hình Chờ Tổng Hợp":
                        workspaceManager.openView(new ReceptionPanel(sessionLifecycle));
                        break;
                    case "Kiosk Đăng Ký Khám":
                        JOptionPane.showMessageDialog(this,
                                "Mô phỏng Giao Diện Kiosk Tự Động Tiếp Đón & Lấy Số Thứ Tự (EHC Smart Kiosk)\n" +
                                "- Người bệnh quét thẻ BHYT hoặc CCCD gắn chip\n" +
                                "- Hệ thống tự động phân luồng phòng khám và in phiếu STT.",
                                "Kiosk Đăng Ký Khám Bệnh", JOptionPane.INFORMATION_MESSAGE);
                        break;
                    case "Kiosk Đánh Giá Chất Lượng":
                        JOptionPane.showMessageDialog(this,
                                "Mô phỏng Giao Diện Kiosk Đánh Giá Hài Lòng Của Người Bệnh (Chuẩn Bộ Y Tế)\n" +
                                "- 5 mức đánh giá (Rất hài lòng, Hài lòng, Bình thường, Không hài lòng, Rất bức xúc)\n" +
                                "- Dữ liệu kết nối trực tiếp cổng báo cáo giám đốc bệnh viện.",
                                "Kiosk Đánh Giá Chất Lượng", JOptionPane.INFORMATION_MESSAGE);
                        break;
                }
            });

            menu.add(mi);
        }

        return menu;
    }

    /**
     * Ribbon Menu dành cho Admin / Quản trị hệ thống tổng thể (Dành để thiết kế sau)
     */
    private JPanel buildAdminRibbonBar() {
        JPanel ribbon = new JPanel(new FlowLayout(FlowLayout.LEFT, 1, 1));
        ribbon.setBackground(UIConstants.COLOR_MENU_BG);
        ribbon.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UIConstants.COLOR_BORDER));

        RibbonButton btnHome = new RibbonButton("Trang Chủ", "🏠", "home",
                k -> workspaceManager.openView(new HomeWelcomePanel()));
        ribbonButtons.put("home", btnHome);

        RibbonButton btnAdmin = new RibbonButton("Quản Lý", "🏛️", "admin",
                k -> workspaceManager.openView(new SystemConfigPanel()));
        ribbonButtons.put("admin", btnAdmin);

        RibbonButton btnReception = new RibbonButton("Đón Tiếp", "📋", "reception", null);
        btnReception.setPopupMenu(buildReceptionPopupMenu());
        ribbonButtons.put("reception", btnReception);

        RibbonButton btnExam = new RibbonButton("Khám Bệnh", "🩺", "examination",
                k -> workspaceManager.openView(new ExaminationPanel()));
        ribbonButtons.put("examination", btnExam);

        RibbonButton btnBilling = new RibbonButton("Viện Phí", "💳", "billing",
                k -> workspaceManager.openView(new BillingInvoicePanel()));
        ribbonButtons.put("billing", btnBilling);

        RibbonButton btnLab = new RibbonButton("CĐHA Và Thăm Dò", "🩻", "paraclinical",
                k -> workspaceManager.openView(new ParaclinicalOrderPanel()));
        ribbonButtons.put("paraclinical", btnLab);

        RibbonButton btnPharmacy = new RibbonButton("Nhà Thuốc", "💊", "pharmacy",
                k -> workspaceManager.openView(new PharmacyDispensePanel()));
        ribbonButtons.put("pharmacy", btnPharmacy);

        RibbonButton btnReport = new RibbonButton("Báo Cáo", "📊", "reports",
                k -> workspaceManager.openView(new ClinicReportPanel()));
        ribbonButtons.put("reports", btnReport);

        RibbonButton btnSystem = new RibbonButton("Hệ Thống", "⚙️", "system",
                k -> workspaceManager.openView(new SystemConfigPanel()));
        ribbonButtons.put("system", btnSystem);

        RibbonButton btnCatalog = new RibbonButton("Danh Mục", "📑", "catalog",
                k -> workspaceManager.openView(new CatalogPanel()));
        ribbonButtons.put("catalog", btnCatalog);

        RibbonButton btnUser = new RibbonButton("Người Dùng", "👤", "user", k -> {
            JOptionPane.showMessageDialog(this,
                    "Thông tin người dùng: " + UserSession.getInstance().getFullName() + "\n" +
                    "Khoa phòng: " + UserSession.getInstance().getDepartmentName() + "\n" +
                    "Vai trò: " + UserSession.getInstance().getRole(),
                    "Tài Khoản Đang Đăng Nhập", JOptionPane.INFORMATION_MESSAGE);
        });

        RibbonButton btnExit = new RibbonButton("Kết Thúc", "🚪", "exit", k -> {
            int ret = JOptionPane.showConfirmDialog(this, "Bạn có chắc chắn muốn thoát ứng dụng?", "Xác nhận", JOptionPane.YES_NO_OPTION);
            if (ret == JOptionPane.YES_OPTION) {
                System.exit(0);
            }
        });

        ribbon.add(btnHome);
        ribbon.add(btnAdmin);
        ribbon.add(btnReception);
        ribbon.add(btnExam);
        ribbon.add(btnBilling);
        ribbon.add(btnLab);
        ribbon.add(btnPharmacy);
        ribbon.add(btnReport);
        ribbon.add(btnSystem);
        ribbon.add(btnCatalog);
        ribbon.add(btnUser);
        ribbon.add(btnExit);

        return ribbon;
    }

    public WorkspaceManager getWorkspaceManager() {
        return workspaceManager;
    }

    private void showSessionLock(SessionLifecycleService.State state) {
        lockOverlay.setAbsoluteExpiry(state == SessionLifecycleService.State.LOCKED_ABSOLUTE);
        lockOverlay.setVisible(true);
        lockOverlay.requestFocusInWindow();
    }

    private void reauthenticate(SessionLockOverlay.Credentials credentials) {
        lockOverlay.setBusy(true);
        lockOverlay.setMessage("Đang xác thực...");
        char[] secret = credentials.password();
        AuthenticateUserCommand command = new AuthenticateUserCommand(credentials.username(), secret);
        java.util.Arrays.fill(secret, '\0');
        credentials.clear();
        new SwingWorker<Boolean, Void>() {
            @Override protected Boolean doInBackground() {
                Result<AuthenticatedUser> result = AuthenticationCompositionRoot.createHandler().handle(command);
                AuthenticatedUser authenticated = result.isSuccess() ? result.valueOrThrow() : null;
                return sessionLifecycle.reauthenticate(authenticated);
            }
            @Override protected void done() {
                try {
                    if (!get()) {
                        lockOverlay.setMessage("Không thể tiếp tục: thông tin xác thực, phạm vi hoặc nhật ký phiên chưa hợp lệ.");
                        return;
                    }
                    lockOverlay.setMessage("");
                    lockOverlay.setVisible(false);
                } catch (Exception ex) {
                    lockOverlay.setMessage("Không thể xác thực lúc này. Hãy thử lại.");
                } finally {
                    lockOverlay.setBusy(false);
                }
            }
        }.execute();
    }

    private void logout() {
        int answer = JOptionPane.showConfirmDialog(this,
                "Đăng xuất sẽ đóng không gian làm việc hiện tại. Bạn có chắc chắn muốn tiếp tục?",
                "Xác nhận đăng xuất", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (answer != JOptionPane.YES_OPTION) return;
        sessionMonitor.stop();
        Toolkit.getDefaultToolkit().removeAWTEventListener(activityMonitor);
        sessionLifecycle.clear();
        dispose();
        SwingUtilities.invokeLater(() -> new LoginForm().setVisible(true));
    }
}
