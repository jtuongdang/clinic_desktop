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
import vn.clinicmanagement.application.auth.CompletedLoginSession;
import vn.clinicmanagement.application.common.Result;
import vn.clinicmanagement.desktop.modules.billing.BillingInvoicePanel;
import vn.clinicmanagement.desktop.modules.catalog.CatalogPanel;
import vn.clinicmanagement.desktop.modules.doctor.DoctorStationPanel;
import vn.clinicmanagement.desktop.modules.examination.ExaminationPanel;
import vn.clinicmanagement.desktop.modules.home.HomeWelcomePanel;
import vn.clinicmanagement.desktop.modules.paraclinical.ParaclinicalOrderPanel;
import vn.clinicmanagement.desktop.modules.pharmacy.PharmacyDispensePanel;
import vn.clinicmanagement.desktop.modules.reception.ReceptionPanel;
import vn.clinicmanagement.desktop.modules.system.SystemConfigPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.AWTEventListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Màn hình Dashboard trung tâm (EHC Hospital Shell)
 * Tự động chuyển đổi giao diện chuyên biệt theo quyền:
 * - Quyền BÁC SĨ (Doctor Station): Giao diện chuyên sâu Điều Trị / Khám Bệnh theo đúng ảnh mẫu
 * - Quyền QUẢN TRỊ VIÊN (Admin): Vỏ điều phối tổng thể hệ thống (dành cho thiết kế sau)
 */
public class MainDashboardFrame extends JFrame {

    private final WorkspaceManager workspaceManager;
    private final Map<String, RibbonButton> ribbonButtons = new HashMap<>();
    private final Set<String> visibleNavigationKeys;
    private final SessionLifecycleService sessionLifecycle;
    private final SessionLockOverlay lockOverlay;
    private final Timer sessionMonitor;
    private final AWTEventListener activityMonitor;

    public MainDashboardFrame(
            CompletedLoginSession completed,
            Set<String> grantedModules,
            SessionLifecycleService sessionLifecycle) {
        Objects.requireNonNull(completed, "completed");
        this.sessionLifecycle = Objects.requireNonNull(sessionLifecycle, "sessionLifecycle");
        this.visibleNavigationKeys = DashboardNavigationPolicy.visibleNavigationKeys(
                completed.role().id(), completed.role().code(), grantedModules);
        setTitle("PHẦN MỀM QUẢN LÝ BỆNH VIỆN");

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

        JPanel ribbonBar = buildRibbonBar(completed);
        rootPanel.add(ribbonBar, BorderLayout.NORTH);

        // Đồng bộ Ribbon khi chuyển đổi tab
        this.workspaceManager.setOnTabActivatedListener(viewKey -> {
            for (Map.Entry<String, RibbonButton> entry : ribbonButtons.entrySet()) {
                boolean selected = entry.getKey().equalsIgnoreCase(viewKey)
                        || ("paraclinical".equals(entry.getKey()) && "SERVICES".equalsIgnoreCase(viewKey));
                entry.getValue().setSelected(selected);
            }
        });

        if (visibleNavigationKeys.contains("home")) {
            this.workspaceManager.openView(new HomeWelcomePanel());
        } else if (visibleNavigationKeys.isEmpty()) {
            centerContainer.add(new JLabel("Không có phân hệ được phép truy cập.", SwingConstants.CENTER));
        }
        if (visibleNavigationKeys.contains("treatment")) {
            this.workspaceManager.openView(new DoctorStationPanel());
        }
    }

    private JPanel buildRibbonBar(CompletedLoginSession completed) {
        JPanel ribbon = new JPanel(new FlowLayout(FlowLayout.LEFT, 1, 1));
        ribbon.setBackground(UIConstants.COLOR_MENU_BG);
        ribbon.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UIConstants.COLOR_BORDER));

        addModuleButton(ribbon, "home", "Trang Chủ", "🏠", () -> workspaceManager.openView(new HomeWelcomePanel()));
        addModuleButton(ribbon, "reception", "Đón Tiếp", "📋",
                () -> workspaceManager.openView(new ReceptionPanel(sessionLifecycle)));
        addModuleButton(ribbon, "treatment", "Điều Trị", "👨‍⚕️",
                () -> workspaceManager.openView(new DoctorStationPanel()));
        addModuleButton(ribbon, "system", "Hệ Thống", "⚙️",
                () -> workspaceManager.openView(new SystemConfigPanel()));
        addModuleButton(ribbon, "catalog", "Danh Mục", "📑",
                () -> workspaceManager.openView(new CatalogPanel()));
        addModuleButton(ribbon, "examination", "Khám Bệnh", "🩺",
                () -> workspaceManager.openView(new ExaminationPanel()));
        addModuleButton(ribbon, "billing", "Viện Phí", "💳",
                () -> workspaceManager.openView(new BillingInvoicePanel()));
        addModuleButton(ribbon, "paraclinical", "CĐHA Và Thăm Dò", "🩻",
                () -> workspaceManager.openView(new ParaclinicalOrderPanel()));
        addModuleButton(ribbon, "pharmacy", "Nhà Thuốc", "💊",
                () -> workspaceManager.openView(new PharmacyDispensePanel()));

        RibbonButton userButton = new RibbonButton("Người Dùng", "👤", "user", key ->
                JOptionPane.showMessageDialog(this,
                        "Thông tin người dùng: " + completed.fullName() + "\n"
                                + "Khoa phòng: " + UserSession.getInstance().getDepartmentName() + "\n"
                                + "Vai trò: " + completed.role().name(),
                        "Tài Khoản Đang Đăng Nhập", JOptionPane.INFORMATION_MESSAGE));
        ribbon.add(userButton);
        ribbon.add(new RibbonButton("Trợ Giúp", "❓", "help", key ->
                JOptionPane.showMessageDialog(this, "Hãy chọn một phân hệ được cấp quyền để tiếp tục.",
                        "Trợ Giúp", JOptionPane.INFORMATION_MESSAGE)));
        ribbon.add(new RibbonButton("Kết Thúc", "🚪", "exit", key -> {
            int answer = JOptionPane.showConfirmDialog(
                    this, "Bạn có chắc chắn muốn thoát ứng dụng?", "Xác nhận", JOptionPane.YES_NO_OPTION);
            if (answer == JOptionPane.YES_OPTION) System.exit(0);
        }));
        return ribbon;
    }

    private void addModuleButton(JPanel ribbon, String key, String title, String icon, Runnable action) {
        if (!visibleNavigationKeys.contains(key)) return;
        RibbonButton button = new RibbonButton(title, icon, key, action == null ? null : ignored -> action.run());
        ribbonButtons.put(key, button);
        ribbon.add(button);
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
