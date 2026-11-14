package vn.clinicmanagement.desktop.modules.dashboard;

import vn.clinicmanagement.desktop.core.components.ThanhBangNut;
import vn.clinicmanagement.desktop.core.components.TrangThaiThanhManHinh;
import vn.clinicmanagement.desktop.core.config.GiaoDienHangSo;
import vn.clinicmanagement.desktop.core.session.PhienVongDoiDichVu;
import vn.clinicmanagement.desktop.core.session.PhienKhoaLopPhu;
import vn.clinicmanagement.desktop.core.base.CoSoManHinhManHinh;
import vn.clinicmanagement.desktop.modules.auth.XacThucGhepNoiGoc;
import vn.clinicmanagement.desktop.modules.auth.DangNhapBieuMau;
import vn.clinicmanagement.desktop.core.diagnostics.MayTinhTacVuNenChanDoanLoi;
import vn.clinicmanagement.application.auth.AuthenticateNguoiDungCommand;
import vn.clinicmanagement.application.auth.DaHoanTatDangNhapPhien;
import java.awt.event.*;
import java.util.Set;
import java.util.Objects;
import vn.clinicmanagement.desktop.modules.billing.VienPhiHoaDonManHinh;
import vn.clinicmanagement.desktop.modules.catalog.DanhMucManHinh;
import vn.clinicmanagement.desktop.modules.doctor.BacSiTramManHinh;
import vn.clinicmanagement.desktop.modules.examination.KhamBenhManHinh;
import vn.clinicmanagement.desktop.modules.home.TrangChuChaoMungManHinh;
import vn.clinicmanagement.desktop.modules.paraclinical.CanLamSangYeuCauManHinh;
import vn.clinicmanagement.desktop.modules.pharmacy.NhaThuocDispenseManHinh;
import vn.clinicmanagement.desktop.modules.reception.TiepDonManHinh;
import vn.clinicmanagement.desktop.modules.reports.PhongKhamBaoCaoManHinh;
import vn.clinicmanagement.desktop.modules.system.HeThongCauHinhManHinh;

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
public class MainBangDieuKhienCuaSo extends JFrame {

    /** Trường `workspaceManager`: khonggianlamviec quanly. */
    private final KhongGianLamViecQuanLy workspaceManager;
    /** Trường `ribbonButtons`: thanhbang `Buttons`. */
    private final Map<String, ThanhBangNut> ribbonButtons = new HashMap<>();
    /** Trường `isDoctor`: là bacsi. */
    private final boolean isDoctor;

    /** Trường `loginSession`: dangnhap phien. */
    private final DaHoanTatDangNhapPhien loginSession;
    /** Trường `sessionLifecycle`: phien vongdoi. */
    private final PhienVongDoiDichVu sessionLifecycle;
    /** Trường `navigation`: dieuhuong. */
    private final BangDieuKhienDieuHuongChinhSach navigation;
    /** Trường `lockOverlay`: khoa lopphu. */
    private final PhienKhoaLopPhu lockOverlay;
    /** Trường `sessionMonitor`: phien `Monitor`. */
    private final Timer sessionMonitor;
    private final AWTEventListener activityMonitor;
    /** Trường `lockKeys`: khoa `Keys`. */
    private final KeyEventDispatcher lockKeys;
    /** Trường `reauthenticationWorker`: `reauthentication` tacvunen. */
    private SwingWorker<Boolean, Void> reauthenticationWorker;
    private boolean closing;
    private boolean closingPrompt;

    public MainBangDieuKhienCuaSo(DaHoanTatDangNhapPhien completed, Set<String> grants,
                              PhienVongDoiDichVu lifecycle) {
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
        this.navigation = new BangDieuKhienDieuHuongChinhSach(grants);
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

        TrangThaiThanhManHinh statusBar = new TrangThaiThanhManHinh();
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
            for (Map.Entry<String, ThanhBangNut> entry : ribbonButtons.entrySet()) {
                entry.getValue().setSelected(entry.getKey().equalsIgnoreCase(viewKey));
            }
        });

        // 1. Mặc định mở Trang Chủ
        openGuarded("home", TrangChuChaoMungManHinh::new);

        lockOverlay = new PhienKhoaLopPhu(this::reauthenticate, () -> requestClose(false));
        setGlassPane(lockOverlay);
        lockOverlay.setVisible(false);
        sessionMonitor = new Timer(1000, e -> updateSessionLock());
        activityMonitor = event -> {
            if (!(event.getSource() instanceof Component component) || !belongsToDashboard(component)) return;
            if (sessionLifecycle.recordActivity() != PhienVongDoiDichVu.State.ACTIVE) updateSessionLock();
        };
        lockKeys = event -> {
            if (closingPrompt) return false;
            if (closing || sessionLifecycle.evaluate() != PhienVongDoiDichVu.State.ACTIVE) {
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
        ribbon.setBackground(GiaoDienHangSo.COLOR_MENU_BG);
        ribbon.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, GiaoDienHangSo.COLOR_BORDER));

        ThanhBangNut btnHome = guardedButton("Trang Chủ", "🏠", "home",
                k -> openGuarded("home", TrangChuChaoMungManHinh::new));
        ribbonButtons.put("home", btnHome);

        // Nút "Đón Tiếp" có Dropdown Menu 6 mục chuẩn theo ảnh chụp
        ThanhBangNut btnReception = guardedButton("Đón Tiếp", "📋", "reception", null);
        btnReception.setPopupMenu(buildReceptionPopupMenu());
        ribbonButtons.put("reception", btnReception);

        // Nút "Điều Trị" (Mặc định chọn nổi bật viền cam cho Bác sĩ)
        ThanhBangNut btnTreatment = guardedButton("Điều Trị", "👨‍⚕️", "treatment",
                k -> openGuarded("treatment", BacSiTramManHinh::new));
        btnTreatment.setSelected(true);
        ribbonButtons.put("treatment", btnTreatment);

        ThanhBangNut btnSystem = guardedButton("Hệ Thống", "⚙️", "system",
                k -> openGuarded("system", HeThongCauHinhManHinh::new));
        ribbonButtons.put("system", btnSystem);

        ThanhBangNut btnCatalog = guardedButton("Danh Mục", "📑", "catalog",
                k -> openGuarded("catalog", DanhMucManHinh::new));
        ribbonButtons.put("catalog", btnCatalog);

        ThanhBangNut btnUser = guardedButton("Người Dùng", "👥", "user", k -> {
            JOptionPane.showMessageDialog(this,
                    formatDoctorUserInfo(loginSession),
                    "Thông Tin Bác Sĩ Điều Trị", JOptionPane.INFORMATION_MESSAGE);
        });

        ThanhBangNut btnHelp = guardedButton("Trợ Giúp", "❓", "help", k -> {
            JOptionPane.showMessageDialog(this,
                    "Hướng dẫn sử dụng bàn khám Bác sĩ EHC:\n" +
                    "- Chọn bệnh nhân từ hàng đợi bên trái để nạp bệnh án\n" +
                    "- Nhấn [In], [Dịch vụ], [Thuốc/VT] để xử trí lâm sàng\n" +
                    "- Nhấn [Phác đồ ĐT] để xem hướng dẫn điều trị chuẩn Bộ Y Tế",
                    "Trợ Giúp Khám Bệnh", JOptionPane.INFORMATION_MESSAGE);
        });

        ThanhBangNut btnExit = guardedButton("Kết Thúc", "❌", "exit", k -> {
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
            mi.setFont(vn.clinicmanagement.desktop.core.config.UngDungPhongChu.BODY);
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
                        openGuarded("reception", TiepDonManHinh::new);
                        break;
                    case "Danh Sách Bệnh Nhân":
                        openGuarded("examination", KhamBenhManHinh::new);
                        break;
                    case "Màn Hình Chờ Tổng Hợp":
                        openGuarded("reception", TiepDonManHinh::new);
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
        ribbon.setBackground(GiaoDienHangSo.COLOR_MENU_BG);
        ribbon.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, GiaoDienHangSo.COLOR_BORDER));

        ThanhBangNut btnHome = guardedButton("Trang Chủ", "🏠", "home",
                k -> openGuarded("home", TrangChuChaoMungManHinh::new));
        ribbonButtons.put("home", btnHome);

        ThanhBangNut btnAdmin = guardedButton("Quản Lý", "🏛️", "admin",
                k -> openGuarded("system", HeThongCauHinhManHinh::new));
        ribbonButtons.put("admin", btnAdmin);

        ThanhBangNut btnReception = guardedButton("Đón Tiếp", "📋", "reception", null);
        btnReception.setPopupMenu(buildReceptionPopupMenu());
        ribbonButtons.put("reception", btnReception);

        ThanhBangNut btnExam = guardedButton("Khám Bệnh", "🩺", "examination",
                k -> openGuarded("examination", KhamBenhManHinh::new));
        ribbonButtons.put("examination", btnExam);

        ThanhBangNut btnBilling = guardedButton("Viện Phí", "💳", "billing",
                k -> openGuarded("billing", VienPhiHoaDonManHinh::new));
        ribbonButtons.put("billing", btnBilling);

        ThanhBangNut btnLab = guardedButton("CĐHA Và Thăm Dò", "🩻", "paraclinical",
                k -> openGuarded("paraclinical", CanLamSangYeuCauManHinh::new));
        ribbonButtons.put("paraclinical", btnLab);

        ThanhBangNut btnPharmacy = guardedButton("Nhà Thuốc", "💊", "pharmacy",
                k -> openGuarded("pharmacy", NhaThuocDispenseManHinh::new));
        ribbonButtons.put("pharmacy", btnPharmacy);

        ThanhBangNut btnReport = guardedButton("Báo Cáo", "📊", "reports",
                k -> openGuarded("reports", PhongKhamBaoCaoManHinh::new));
        ribbonButtons.put("reports", btnReport);

        ThanhBangNut btnSystem = guardedButton("Hệ Thống", "⚙️", "system",
                k -> openGuarded("system", HeThongCauHinhManHinh::new));
        ribbonButtons.put("system", btnSystem);

        ThanhBangNut btnCatalog = guardedButton("Danh Mục", "📑", "catalog",
                k -> openGuarded("catalog", DanhMucManHinh::new));
        ribbonButtons.put("catalog", btnCatalog);

        ThanhBangNut btnUser = guardedButton("Người Dùng", "👤", "user", k -> {
            JOptionPane.showMessageDialog(this,
                    formatStaffUserInfo(loginSession),
                    "Tài Khoản Đang Đăng Nhập", JOptionPane.INFORMATION_MESSAGE);
        });

        ThanhBangNut btnExit = guardedButton("Kết Thúc", "🚪", "exit", k -> {
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
        return !closing && sessionLifecycle.evaluate() == PhienVongDoiDichVu.State.ACTIVE
                && navigation.canOpen(key);
    }

    /** Hàm `formatDoctorUserInfo`: `format` bacsi người dùng `Info`. */
    static String formatDoctorUserInfo(DaHoanTatDangNhapPhien loginSession) {
        return "Bác sĩ đang khám: " + loginSession.fullName() + "\n" +
                "Cơ sở: " + loginSession.workScope().facilityName() + "\n" +
                "Khoa phòng: " + loginSession.workScope().departmentName() + "\n" +
                "Vai trò: " + loginSession.role().name();
    }

    /** Hàm `formatStaffUserInfo`: `format` nhân viên người dùng `Info`. */
    static String formatStaffUserInfo(DaHoanTatDangNhapPhien loginSession) {
        return "Thông tin người dùng: " + loginSession.fullName() + "\n" +
                "Cơ sở: " + loginSession.workScope().facilityName() + "\n" +
                "Khoa phòng: " + loginSession.workScope().departmentName() + "\n" +
                "Vai trò: " + loginSession.role().name();
    }

    /** Hàm `syncRibbonButtonsForSessionState`: `sync` thanhbang `Buttons` cho phien `State`. */
    static void syncRibbonButtonsForSessionState(PhienVongDoiDichVu sessionLifecycle,
                                                 BangDieuKhienDieuHuongChinhSach navigation,
                                                 Map<String, ThanhBangNut> ribbonButtons) {
        boolean active = sessionLifecycle.evaluate() == PhienVongDoiDichVu.State.ACTIVE;
        for (var entry : ribbonButtons.entrySet()) {
            String key = entry.getKey();
            entry.getValue().setEnabled("exit".equals(key) || active && navigation.canOpen(key));
        }
    }

    /** Hàm `syncLockOverlayForSessionState`: `sync` khoa lopphu cho phien `State`. */
    static void syncLockOverlayForSessionState(PhienVongDoiDichVu.State state,
                                              PhienKhoaLopPhu lockOverlay) {
        if (state == PhienVongDoiDichVu.State.ACTIVE) {
            lockOverlay.setVisible(false);
            return;
        }
        lockOverlay.setAbsoluteExpiry(state == PhienVongDoiDichVu.State.LOCKED_ABSOLUTE);
        boolean newlyLocked = !lockOverlay.isVisible();
        lockOverlay.setVisible(true);
        if (newlyLocked) lockOverlay.focusCredentials();
    }

    /** Hàm `guardedButton`: `guarded` nút. */
    private ThanhBangNut guardedButton(String title, String icon, String key,
                                        java.util.function.Consumer<String> action) {
        ThanhBangNut button = new ThanhBangNut(title, icon, key, action == null ? null : selected -> {
            if ("exit".equals(key) || canNavigate(key)) action.accept(selected);
        });
        button.setEnabled("exit".equals(key) || canNavigate(key));
        ribbonButtons.putIfAbsent(key, button);
        return button;
    }

    /** Hàm `openGuarded`: mở `Guarded`. */
    private void openGuarded(String key, java.util.function.Supplier<CoSoManHinhManHinh> factory) {
        if (canNavigate(key)) workspaceManager.openView(factory.get());
    }

    /** Hàm `belongsToDashboard`: `belongs` den bangdieukhien. */
    private boolean belongsToDashboard(Component component) {
        Window window = component instanceof Window w ? w : SwingUtilities.getWindowAncestor(component);
        while (window != null) {
            if (window == this) return true;
            window = window.getOwner();
        }
        return false;
    }

    /** Hàm `updateSessionLock`: cập nhật phien khoa. */
    private void updateSessionLock() {
        if (closing) return;
        PhienVongDoiDichVu.State state = sessionLifecycle.evaluate();
        syncRibbonButtonsForSessionState(sessionLifecycle, navigation, ribbonButtons);
        if (state != PhienVongDoiDichVu.State.ACTIVE) {
            MenuSelectionManager.defaultManager().clearSelectedPath();
            // No clinical child route is enabled; dismiss any shell dialog before shielding input.
            if (!closingPrompt) for (Window owned : getOwnedWindows()) if (owned.isShowing()) owned.dispose();
        }
        syncLockOverlayForSessionState(state, lockOverlay);
    }

    private void reauthenticate(PhienKhoaLopPhu.Credentials credentials) {
        if (closing || reauthenticationWorker != null) { credentials.clear(); return; }
        lockOverlay.setBusy(true);
        char[] secret = credentials.password();
        AuthenticateNguoiDungCommand command = new AuthenticateNguoiDungCommand(credentials.username(), secret);
        java.util.Arrays.fill(secret, '\0');
        credentials.clear();
        reauthenticationWorker = new SwingWorker<>() {
            @Override protected Boolean doInBackground() {
                var result = XacThucGhepNoiGoc.createHandler().handle(command);
                return sessionLifecycle.reauthenticate(result.isSuccess() ? result.valueOrThrow() : null);
            }
            @Override protected void done() {
                if (closing) return;
                try {
                    if (!get()) lockOverlay.setMessage("Không thể tiếp tục: xác thực, phạm vi hoặc nhật ký phiên chưa hợp lệ.");
                    syncRibbonButtonsForSessionState(sessionLifecycle, navigation, ribbonButtons);
                    updateSessionLock();
                } catch (InterruptedException ex) {
                    MayTinhTacVuNenChanDoanLoi.restoreInterruptStatus();
                } catch (java.util.concurrent.ExecutionException | RuntimeException ex) {
                    MayTinhTacVuNenChanDoanLoi.logDashboardReauthenticationFailure(ex);
                    lockOverlay.setMessage("Không thể xác thực lúc này. Hãy thử lại.");
                } finally {
                    reauthenticationWorker = null;
                    lockOverlay.setBusy(false);
                }
            }
        };
        reauthenticationWorker.execute();
    }

    /** Hàm `requestClose`: yêu cầu đóng. */
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

    /** Hàm `closeWorkspace`: đóng khonggianlamviec. */
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
                else new DangNhapBieuMau().setVisible(true);
            }
        }.execute();
    }

    @Override public void dispose() {
        if (sessionMonitor == null) { super.dispose(); return; }
        closeWorkspace(false);
    }

    /** Guard direct openers and retained-tab callbacks without changing the original workspace layout. */
    static final class GuardedWorkspace extends KhongGianLamViecQuanLy {
        /** Trường `lifecycle`: vongdoi. */
        private final PhienVongDoiDichVu lifecycle;
        /** Trường `navigation`: dieuhuong. */
        private final BangDieuKhienDieuHuongChinhSach navigation;
        private final JPanel center;
        private final JPanel tabs;
        private boolean disposed;
        GuardedWorkspace(JPanel center, CardLayout cards, JPanel tabs,
                         PhienVongDoiDichVu lifecycle, BangDieuKhienDieuHuongChinhSach navigation) {
            super(center, cards, tabs);
            this.center = center;
            this.tabs = tabs;
            this.lifecycle = lifecycle;
            this.navigation = navigation;
        }
        private boolean allowed(String key) {
            return !disposed && lifecycle.evaluate() == PhienVongDoiDichVu.State.ACTIVE && navigation.canOpen(key);
        }
        private static final Set<Class<? extends CoSoManHinhManHinh>> APPROVED_VIEWS = Set.of(
                TrangChuChaoMungManHinh.class,
                TiepDonManHinh.class,
                BacSiTramManHinh.class,
                KhamBenhManHinh.class,
                VienPhiHoaDonManHinh.class,
                PhongKhamBaoCaoManHinh.class,
                HeThongCauHinhManHinh.class,
                DanhMucManHinh.class,
                NhaThuocDispenseManHinh.class,
                CanLamSangYeuCauManHinh.class
        );

        /** Hàm `openView`: mở màn hình. Gốc khai báo tại `vn.clinicmanagement.desktop.modules.dashboard.KhongGianLamViecQuanLy#openView`. */
        @Override public void openView(CoSoManHinhManHinh view) {
            if (view != null && APPROVED_VIEWS.contains(view.getClass()) && allowed(view.getViewKey())) {
                super.openView(view);
            }
        }
        /** Hàm `activateView`: `activate` màn hình. Gốc khai báo tại `vn.clinicmanagement.desktop.modules.dashboard.KhongGianLamViecQuanLy#activateView`. */
        @Override public void activateView(String key) {
            if (allowed(key)) super.activateView(key);
        }
        /** Hàm `closeView`: đóng màn hình. Gốc khai báo tại `vn.clinicmanagement.desktop.modules.dashboard.KhongGianLamViecQuanLy#closeView`. */
        @Override public void closeView(String key) {
            if (allowed(key)) super.closeView(key);
        }
        void disposeViews() {
            disposed = true;
            for (Component component : center.getComponents()) {
                if (component instanceof CoSoManHinhManHinh view) view.onHide();
            }
            center.removeAll();
            tabs.removeAll();
        }
    }

    /** Hàm `getWorkspaceManager`: lấy khonggianlamviec quanly. */
    public KhongGianLamViecQuanLy getWorkspaceManager() { return workspaceManager; }
}
