package vn.clinicmanagement.desktop.modules.dashboard;

import vn.clinicmanagement.desktop.core.components.RibbonButton;
import vn.clinicmanagement.desktop.core.components.StatusBarPanel;
import vn.clinicmanagement.desktop.core.config.UIConstants;
import vn.clinicmanagement.desktop.core.session.SessionLifecycleService;
import vn.clinicmanagement.desktop.core.session.SessionLockOverlay;
import vn.clinicmanagement.desktop.core.base.BaseViewPanel;
import vn.clinicmanagement.desktop.modules.auth.AuthenticationCompositionRoot;
import vn.clinicmanagement.desktop.modules.auth.LoginForm;
import vn.clinicmanagement.desktop.core.diagnostics.DesktopWorkerDiagnostics;
import vn.clinicmanagement.application.auth.AuthenticateUserCommand;
import vn.clinicmanagement.application.auth.CompletedLoginSession;
import java.awt.event.*;
import java.util.Set;
import java.util.Objects;
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

    private final CompletedLoginSession loginSession;
    private final SessionLifecycleService sessionLifecycle;
    private final DashboardNavigationPolicy navigation;
    private final SessionLockOverlay lockOverlay;
    private final Timer sessionMonitor;
    private final AWTEventListener activityMonitor;
    private final KeyEventDispatcher lockKeys;
    private SwingWorker<Boolean, Void> reauthenticationWorker;
    private boolean closing;
    private boolean closingPrompt;

    public MainDashboardFrame(CompletedLoginSession completed, Set<String> grants,
                              SessionLifecycleService lifecycle) {
        this.loginSession = Objects.requireNonNull(completed, "completed");
        this.sessionLifecycle = Objects.requireNonNull(lifecycle, "lifecycle");
        var context = lifecycle.currentAuthorizationContext().orElseThrow(
                () -> new IllegalStateException("An active completed session is required."));
        if (context.employeeId() != completed.userId() || context.roleId() != completed.role().id()
                || !context.roleCode().equals(completed.role().code())
                || context.facilityId() != completed.workScope().facilityId()
                || context.departmentId() != completed.workScope().departmentId()
                || !context.sessionId().equals(completed.correlationId())) {
            throw new IllegalArgumentException("Dashboard context must match session completion.");
        }
        this.navigation = new DashboardNavigationPolicy(grants);
        this.isDoctor = "DOCTOR".equals(completed.role().code());

        if (isDoctor) {
            setTitle("PHẦN MỀM QUẢN LÝ BỆNH VIỆN - 2018.10.5.1 - BUILD: 10/2018");
        } else {
            setTitle("PHẦN MỀM QUẢN LÝ BỆNH VIỆN - 2021.1.15.1 - 2021 - PC : DESKTOP-E64DCHT");
        }

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
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
        this.workspaceManager = new GuardedWorkspace(centerContainer, cardLayout, tabStripPanel,
                sessionLifecycle, navigation);

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
        openGuarded("home", HomeWelcomePanel::new);

        lockOverlay = new SessionLockOverlay(this::reauthenticate, () -> requestClose(false));
        setGlassPane(lockOverlay);
        lockOverlay.setVisible(false);
        sessionMonitor = new Timer(1000, e -> updateSessionLock());
        activityMonitor = event -> {
            if (!(event.getSource() instanceof Component component) || !belongsToDashboard(component)) return;
            if (sessionLifecycle.recordActivity() != SessionLifecycleService.State.ACTIVE) updateSessionLock();
        };
        lockKeys = event -> {
            if (closingPrompt) return false;
            if (closing || sessionLifecycle.evaluate() != SessionLifecycleService.State.ACTIVE) {
                updateSessionLock();
                return !SwingUtilities.isDescendingFrom(event.getComponent(), lockOverlay);
            }
            return false;
        };
        Toolkit.getDefaultToolkit().addAWTEventListener(activityMonitor,
                AWTEvent.KEY_EVENT_MASK | AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK);
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(lockKeys);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { requestClose(true); }
        });
        sessionMonitor.start();
        updateSessionLock();
    }

    /**
     * Ribbon Menu dành riêng cho vai trò BÁC SĨ (Khớp 100% ảnh chụp phân hệ Bác sĩ)
     */
    private JPanel buildDoctorRibbonBar() {
        JPanel ribbon = new JPanel(new FlowLayout(FlowLayout.LEFT, 1, 1));
        ribbon.setBackground(UIConstants.COLOR_MENU_BG);
        ribbon.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UIConstants.COLOR_BORDER));

        RibbonButton btnHome = guardedButton("Trang Chủ", "🏠", "home",
                k -> openGuarded("home", HomeWelcomePanel::new));
        ribbonButtons.put("home", btnHome);

        // Nút "Đón Tiếp" có Dropdown Menu 6 mục chuẩn theo ảnh chụp
        RibbonButton btnReception = guardedButton("Đón Tiếp", "📋", "reception", null);
        btnReception.setPopupMenu(buildReceptionPopupMenu());
        ribbonButtons.put("reception", btnReception);

        // Nút "Điều Trị" (Mặc định chọn nổi bật viền cam cho Bác sĩ)
        RibbonButton btnTreatment = guardedButton("Điều Trị", "👨‍⚕️", "treatment",
                k -> openGuarded("treatment", DoctorStationPanel::new));
        btnTreatment.setSelected(true);
        ribbonButtons.put("treatment", btnTreatment);

        RibbonButton btnSystem = guardedButton("Hệ Thống", "⚙️", "system",
                k -> openGuarded("system", SystemConfigPanel::new));
        ribbonButtons.put("system", btnSystem);

        RibbonButton btnCatalog = guardedButton("Danh Mục", "📑", "catalog",
                k -> openGuarded("catalog", CatalogPanel::new));
        ribbonButtons.put("catalog", btnCatalog);

        RibbonButton btnUser = guardedButton("Người Dùng", "👥", "user", k -> {
            JOptionPane.showMessageDialog(this,
                    formatDoctorUserInfo(loginSession),
                    "Thông Tin Bác Sĩ Điều Trị", JOptionPane.INFORMATION_MESSAGE);
        });

        RibbonButton btnHelp = guardedButton("Trợ Giúp", "❓", "help", k -> {
            JOptionPane.showMessageDialog(this,
                    "Hướng dẫn sử dụng bàn khám Bác sĩ EHC:\n" +
                    "- Chọn bệnh nhân từ hàng đợi bên trái để nạp bệnh án\n" +
                    "- Nhấn [In], [Dịch vụ], [Thuốc/VT] để xử trí lâm sàng\n" +
                    "- Nhấn [Phác đồ ĐT] để xem hướng dẫn điều trị chuẩn Bộ Y Tế",
                    "Trợ Giúp Khám Bệnh", JOptionPane.INFORMATION_MESSAGE);
        });

        RibbonButton btnExit = guardedButton("Kết Thúc", "❌", "exit", k -> {
            requestClose(true);
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
        JPopupMenu menu = new JPopupMenu() {
            @Override public void show(Component invoker, int x, int y) {
                if (canNavigate("reception")) super.show(invoker, x, y);
            }
        };
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

            mi.setEnabled(canNavigate("reception"));
            mi.addActionListener(e -> {
                if (!canNavigate("reception")) return;
                switch (title) {
                    case "Đăng Ký Khám Bệnh":
                    case "Danh Sách Đón Tiếp":
                        openGuarded("reception", ReceptionPanel::new);
                        break;
                    case "Danh Sách Bệnh Nhân":
                        openGuarded("examination", ExaminationPanel::new);
                        break;
                    case "Màn Hình Chờ Tổng Hợp":
                        openGuarded("reception", ReceptionPanel::new);
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

        RibbonButton btnHome = guardedButton("Trang Chủ", "🏠", "home",
                k -> openGuarded("home", HomeWelcomePanel::new));
        ribbonButtons.put("home", btnHome);

        RibbonButton btnAdmin = guardedButton("Quản Lý", "🏛️", "admin",
                k -> openGuarded("system", SystemConfigPanel::new));
        ribbonButtons.put("admin", btnAdmin);

        RibbonButton btnReception = guardedButton("Đón Tiếp", "📋", "reception", null);
        btnReception.setPopupMenu(buildReceptionPopupMenu());
        ribbonButtons.put("reception", btnReception);

        RibbonButton btnExam = guardedButton("Khám Bệnh", "🩺", "examination",
                k -> openGuarded("examination", ExaminationPanel::new));
        ribbonButtons.put("examination", btnExam);

        RibbonButton btnBilling = guardedButton("Viện Phí", "💳", "billing",
                k -> openGuarded("billing", BillingInvoicePanel::new));
        ribbonButtons.put("billing", btnBilling);

        RibbonButton btnLab = guardedButton("CĐHA Và Thăm Dò", "🩻", "paraclinical",
                k -> openGuarded("paraclinical", ParaclinicalOrderPanel::new));
        ribbonButtons.put("paraclinical", btnLab);

        RibbonButton btnPharmacy = guardedButton("Nhà Thuốc", "💊", "pharmacy",
                k -> openGuarded("pharmacy", PharmacyDispensePanel::new));
        ribbonButtons.put("pharmacy", btnPharmacy);

        RibbonButton btnReport = guardedButton("Báo Cáo", "📊", "reports",
                k -> openGuarded("reports", ClinicReportPanel::new));
        ribbonButtons.put("reports", btnReport);

        RibbonButton btnSystem = guardedButton("Hệ Thống", "⚙️", "system",
                k -> openGuarded("system", SystemConfigPanel::new));
        ribbonButtons.put("system", btnSystem);

        RibbonButton btnCatalog = guardedButton("Danh Mục", "📑", "catalog",
                k -> openGuarded("catalog", CatalogPanel::new));
        ribbonButtons.put("catalog", btnCatalog);

        RibbonButton btnUser = guardedButton("Người Dùng", "👤", "user", k -> {
            JOptionPane.showMessageDialog(this,
                    formatStaffUserInfo(loginSession),
                    "Tài Khoản Đang Đăng Nhập", JOptionPane.INFORMATION_MESSAGE);
        });

        RibbonButton btnExit = guardedButton("Kết Thúc", "🚪", "exit", k -> {
            requestClose(true);
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

    private boolean canNavigate(String key) {
        return !closing && sessionLifecycle.evaluate() == SessionLifecycleService.State.ACTIVE
                && navigation.canOpen(key);
    }

    static String formatDoctorUserInfo(CompletedLoginSession loginSession) {
        return "Bác sĩ đang khám: " + loginSession.fullName() + "\n" +
                "Cơ sở: " + loginSession.workScope().facilityName() + "\n" +
                "Khoa phòng: " + loginSession.workScope().departmentName() + "\n" +
                "Vai trò: " + loginSession.role().name();
    }

    static String formatStaffUserInfo(CompletedLoginSession loginSession) {
        return "Thông tin người dùng: " + loginSession.fullName() + "\n" +
                "Cơ sở: " + loginSession.workScope().facilityName() + "\n" +
                "Khoa phòng: " + loginSession.workScope().departmentName() + "\n" +
                "Vai trò: " + loginSession.role().name();
    }

    static void syncRibbonButtonsForSessionState(SessionLifecycleService sessionLifecycle,
                                                 DashboardNavigationPolicy navigation,
                                                 Map<String, RibbonButton> ribbonButtons) {
        boolean active = sessionLifecycle.evaluate() == SessionLifecycleService.State.ACTIVE;
        for (var entry : ribbonButtons.entrySet()) {
            String key = entry.getKey();
            entry.getValue().setEnabled("exit".equals(key) || active && navigation.canOpen(key));
        }
    }

    static void syncLockOverlayForSessionState(SessionLifecycleService.State state,
                                              SessionLockOverlay lockOverlay) {
        if (state == SessionLifecycleService.State.ACTIVE) {
            lockOverlay.setVisible(false);
            return;
        }
        lockOverlay.setAbsoluteExpiry(state == SessionLifecycleService.State.LOCKED_ABSOLUTE);
        boolean newlyLocked = !lockOverlay.isVisible();
        lockOverlay.setVisible(true);
        if (newlyLocked) lockOverlay.focusCredentials();
    }

    private RibbonButton guardedButton(String title, String icon, String key,
                                        java.util.function.Consumer<String> action) {
        RibbonButton button = new RibbonButton(title, icon, key, action == null ? null : selected -> {
            if ("exit".equals(key) || canNavigate(key)) action.accept(selected);
        });
        button.setEnabled("exit".equals(key) || canNavigate(key));
        ribbonButtons.putIfAbsent(key, button);
        return button;
    }

    private void openGuarded(String key, java.util.function.Supplier<BaseViewPanel> factory) {
        if (canNavigate(key)) workspaceManager.openView(factory.get());
    }

    private boolean belongsToDashboard(Component component) {
        Window window = component instanceof Window w ? w : SwingUtilities.getWindowAncestor(component);
        while (window != null) {
            if (window == this) return true;
            window = window.getOwner();
        }
        return false;
    }

    private void updateSessionLock() {
        if (closing) return;
        SessionLifecycleService.State state = sessionLifecycle.evaluate();
        syncRibbonButtonsForSessionState(sessionLifecycle, navigation, ribbonButtons);
        if (state != SessionLifecycleService.State.ACTIVE) {
            MenuSelectionManager.defaultManager().clearSelectedPath();
            // No clinical child route is enabled; dismiss any shell dialog before shielding input.
            if (!closingPrompt) for (Window owned : getOwnedWindows()) if (owned.isShowing()) owned.dispose();
        }
        syncLockOverlayForSessionState(state, lockOverlay);
    }

    private void reauthenticate(SessionLockOverlay.Credentials credentials) {
        if (closing || reauthenticationWorker != null) { credentials.clear(); return; }
        lockOverlay.setBusy(true);
        char[] secret = credentials.password();
        AuthenticateUserCommand command = new AuthenticateUserCommand(credentials.username(), secret);
        java.util.Arrays.fill(secret, '\0');
        credentials.clear();
        reauthenticationWorker = new SwingWorker<>() {
            @Override protected Boolean doInBackground() {
                var result = AuthenticationCompositionRoot.createHandler().handle(command);
                return sessionLifecycle.reauthenticate(result.isSuccess() ? result.valueOrThrow() : null);
            }
            @Override protected void done() {
                if (closing) return;
                try {
                    if (!get()) lockOverlay.setMessage("Không thể tiếp tục: xác thực, phạm vi hoặc nhật ký phiên chưa hợp lệ.");
                    syncRibbonButtonsForSessionState(sessionLifecycle, navigation, ribbonButtons);
                    updateSessionLock();
                } catch (InterruptedException ex) {
                    DesktopWorkerDiagnostics.restoreInterruptStatus();
                } catch (java.util.concurrent.ExecutionException | RuntimeException ex) {
                    DesktopWorkerDiagnostics.logDashboardReauthenticationFailure(ex);
                    lockOverlay.setMessage("Không thể xác thực lúc này. Hãy thử lại.");
                } finally {
                    reauthenticationWorker = null;
                    lockOverlay.setBusy(false);
                }
            }
        };
        reauthenticationWorker.execute();
    }

    private void requestClose(boolean exit) {
        if (closing) return;
        closingPrompt = true;
        int answer;
        try { answer = JOptionPane.showConfirmDialog(this,
                "Đóng không gian làm việc sẽ bỏ các bản nháp chưa lưu. Tiếp tục?",
                "Xác nhận", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        } finally { closingPrompt = false; }
        if (answer == JOptionPane.YES_OPTION) closeWorkspace(exit);
    }

    private void closeWorkspace(boolean exit) {
        if (closing) return;
        closing = true;
        sessionMonitor.stop();
        Toolkit.getDefaultToolkit().removeAWTEventListener(activityMonitor);
        KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(lockKeys);
        sessionLifecycle.clear();
        if (reauthenticationWorker != null) reauthenticationWorker.cancel(true);
        ((GuardedWorkspace) workspaceManager).disposeViews();
        for (Window owned : getOwnedWindows()) owned.dispose();
        getContentPane().removeAll();
        setContentPane(new JPanel());
        super.dispose();
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() {
                sessionLifecycle.awaitPendingAudits(java.time.Duration.ofSeconds(3));
                return null;
            }
            @Override protected void done() {
                if (exit) System.exit(0);
                else new LoginForm().setVisible(true);
            }
        }.execute();
    }

    @Override public void dispose() {
        if (sessionMonitor == null) { super.dispose(); return; }
        closeWorkspace(false);
    }

    /** Guard direct openers and retained-tab callbacks without changing the original workspace layout. */
    static final class GuardedWorkspace extends WorkspaceManager {
        private final SessionLifecycleService lifecycle;
        private final DashboardNavigationPolicy navigation;
        private final JPanel center;
        private final JPanel tabs;
        private boolean disposed;
        GuardedWorkspace(JPanel center, CardLayout cards, JPanel tabs,
                         SessionLifecycleService lifecycle, DashboardNavigationPolicy navigation) {
            super(center, cards, tabs);
            this.center = center;
            this.tabs = tabs;
            this.lifecycle = lifecycle;
            this.navigation = navigation;
        }
        private boolean allowed(String key) {
            return !disposed && lifecycle.evaluate() == SessionLifecycleService.State.ACTIVE && navigation.canOpen(key);
        }
        private static final Set<Class<? extends BaseViewPanel>> APPROVED_VIEWS = Set.of(
                HomeWelcomePanel.class,
                ReceptionPanel.class,
                DoctorStationPanel.class,
                ExaminationPanel.class,
                BillingInvoicePanel.class,
                ClinicReportPanel.class,
                SystemConfigPanel.class,
                CatalogPanel.class,
                PharmacyDispensePanel.class,
                ParaclinicalOrderPanel.class
        );

        @Override public void openView(BaseViewPanel view) {
            if (view != null && APPROVED_VIEWS.contains(view.getClass()) && allowed(view.getViewKey())) {
                super.openView(view);
            }
        }
        @Override public void activateView(String key) {
            if (allowed(key)) super.activateView(key);
        }
        @Override public void closeView(String key) {
            if (allowed(key)) super.closeView(key);
        }
        void disposeViews() {
            disposed = true;
            for (Component component : center.getComponents()) {
                if (component instanceof BaseViewPanel view) view.onHide();
            }
            center.removeAll();
            tabs.removeAll();
        }
    }

    public WorkspaceManager getWorkspaceManager() { return workspaceManager; }
}
