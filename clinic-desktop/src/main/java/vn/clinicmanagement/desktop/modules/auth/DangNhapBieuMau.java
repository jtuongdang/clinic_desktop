package vn.clinicmanagement.desktop.modules.auth;

import vn.clinicmanagement.desktop.core.config.UngDungPhongChu;
import vn.clinicmanagement.desktop.core.config.GiaoDienHangSo;
import vn.clinicmanagement.desktop.core.diagnostics.MayTinhTacVuNenChanDoanLoi;
import vn.clinicmanagement.desktop.core.session.NguoiDungPhien;
import vn.clinicmanagement.desktop.core.session.PhienVongDoiDichVu;
import vn.clinicmanagement.desktop.modules.dashboard.MainBangDieuKhienCuaSo;
import vn.clinicmanagement.application.auth.AuthenticateNguoiDungCommand;
import vn.clinicmanagement.application.auth.AuthenticateNguoiDungHandler;
import vn.clinicmanagement.application.auth.DaXacThucNguoiDung;
import vn.clinicmanagement.application.auth.DaXacThucNguoiDung.RoleChoice;
import vn.clinicmanagement.application.auth.DaXacThucNguoiDung.WorkScopeChoice;
import vn.clinicmanagement.application.auth.CompleteDangNhapPhienCommand;
import vn.clinicmanagement.application.auth.CompleteDangNhapPhienHandler;
import vn.clinicmanagement.application.auth.DaHoanTatDangNhapPhien;
import vn.clinicmanagement.application.authorization.DieuHuongQuyenHandler;
import vn.clinicmanagement.application.common.KetQua;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Set;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

/**
 * Màn hình Đăng Nhập Hệ Thống Phòng Khám / Bệnh Viện EHC
 */
public class DangNhapBieuMau extends JFrame {

    private JTextField txtUsername;
    /** Trường `txtPassword`: `txt` matkhau. */
    private JPasswordField txtPassword;
    /** Trường `btnLogin`: `btn` dangnhap. */
    private JButton btnLogin;
    /** Trường `attemptGeneration`: thu `Generation`. */
    private long attemptGeneration;
    /** Trường `loginPending`: dangnhap `Pending`. */
    private boolean loginPending;
    /** Trường `authenticateUserHandler`: xác thực người dùng xử lý. */
    private final AuthenticateNguoiDungHandler authenticateUserHandler;
    /** Trường `completeLoginSessionHandler`: hoàn tất dangnhap phien xử lý. */
    private final CompleteDangNhapPhienHandler completeLoginSessionHandler;

    public DangNhapBieuMau() {
        this(
                XacThucGhepNoiGoc.createHandler(),
                XacThucGhepNoiGoc.createCompleteLoginSessionHandler());
    }

    DangNhapBieuMau(
            AuthenticateNguoiDungHandler authenticateUserHandler,
            CompleteDangNhapPhienHandler completeLoginSessionHandler) {
        this.authenticateUserHandler = Objects.requireNonNull(authenticateUserHandler, "authenticateUserHandler");
        this.completeLoginSessionHandler = Objects.requireNonNull(
                completeLoginSessionHandler, "completeLoginSessionHandler");
        NguoiDungPhien.getInstance().clearSession();
        initUI();
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosing(java.awt.event.WindowEvent event) {
                attemptGeneration++;
                NguoiDungPhien.getInstance().clearSession();
            }
        });
    }

    @Override public void dispose() {
        attemptGeneration++;
        super.dispose();
    }

    /** Hàm `initUI`: `init` giao diện. */
    private void initUI() {
        setTitle("Đăng nhập - Phần Mềm Quản Lý Bệnh Viện EHC");
        setSize(440, 390);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(25, 30, 25, 30));

        // Header
        JPanel headerPanel = new JPanel(new GridLayout(2, 1, 4, 4));
        headerPanel.setOpaque(false);

        JLabel lblTitle = new JLabel("HỆ THỐNG Y TẾ THÔNG MINH EHC", SwingConstants.CENTER);
        lblTitle.setFont(UngDungPhongChu.SECTION);
        lblTitle.setForeground(GiaoDienHangSo.COLOR_PRIMARY);

        JLabel lblSub = new JLabel("Đăng nhập tài khoản & chọn phân hệ làm việc", SwingConstants.CENTER);
        lblSub.setFont(GiaoDienHangSo.FONT_SMALL);
        lblSub.setForeground(Color.GRAY);

        headerPanel.add(lblTitle);
        headerPanel.add(lblSub);
        panel.add(headerPanel, BorderLayout.NORTH);

        // Form fields
        JPanel formPanel = new JPanel(new GridLayout(4, 1, 6, 6));
        formPanel.setOpaque(false);
        formPanel.setBorder(new EmptyBorder(12, 0, 12, 0));

        JLabel lblUser = new JLabel("Tên đăng nhập:");
        lblUser.setFont(GiaoDienHangSo.FONT_BOLD);
        txtUsername = new JTextField();
        txtUsername.setFont(GiaoDienHangSo.FONT_REGULAR);

        JLabel lblPass = new JLabel("Mật khẩu:");
        lblPass.setFont(GiaoDienHangSo.FONT_BOLD);
        txtPassword = new JPasswordField();
        txtPassword.setFont(GiaoDienHangSo.FONT_REGULAR);

        formPanel.add(lblUser);
        formPanel.add(txtUsername);
        formPanel.add(lblPass);
        formPanel.add(txtPassword);

        panel.add(formPanel, BorderLayout.CENTER);

        // Footer buttons
        JPanel footerPanel = new JPanel(new BorderLayout(8, 8));
        footerPanel.setOpaque(false);

        btnLogin = new JButton("ĐĂNG NHẬP HỆ THỐNG");
        btnLogin.setFont(UngDungPhongChu.SECTION);
        btnLogin.setBackground(GiaoDienHangSo.COLOR_PRIMARY);
        btnLogin.setForeground(Color.WHITE);
        btnLogin.setFocusPainted(false);
        btnLogin.setOpaque(true);
        btnLogin.setPreferredSize(new Dimension(100, 42));

        btnLogin.addActionListener(e -> onLogin());

        footerPanel.add(btnLogin, BorderLayout.CENTER);
        panel.add(footerPanel, BorderLayout.SOUTH);

        setContentPane(panel);
    }

    /** Hàm `onLogin`: tải dangnhap. */
    private void onLogin() {
        if (loginPending || !isDisplayable()) return;
        final long generation = ++attemptGeneration;
        NguoiDungPhien.getInstance().clearSession();
        String username = txtUsername.getText().trim();
        char[] password = txtPassword.getPassword();

        if (!validateAndScrubIfInvalid(username, password)) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập tên đăng nhập và mật khẩu.", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        loginPending = true;
        setLoginControlsEnabled(false);
        txtPassword.setText("");
        AuthenticateNguoiDungCommand command = new AuthenticateNguoiDungCommand(username, password);
        java.util.Arrays.fill(password, '\0');

        new SwingWorker<KetQua<DaXacThucNguoiDung>, Void>() {
            @Override
            protected KetQua<DaXacThucNguoiDung> doInBackground() {
                return authenticateUserHandler.handle(command);
            }

            @Override
            protected void done() {
                if (!isDisplayable() || generation != attemptGeneration) return;
                boolean finalizing = false;
                try {
                    KetQua<DaXacThucNguoiDung> result = get();
                    if (result.isFailure()) {
                        String message = toLoginFailureMessage(result, "Không thể đăng nhập.");
                        JOptionPane.showMessageDialog(DangNhapBieuMau.this, message, "Đăng nhập thất bại", JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    DaXacThucNguoiDung user = result.valueOrThrow();
                    RoleChoice role = chooseRole(user.roles());
                    if (role == null) return;
                    WorkScopeChoice scope = chooseScope(user.workScopes());
                    if (scope == null || !isDisplayable() || generation != attemptGeneration) return;

                    finalizing = true;
                    finalizeLoginSession(user, role, scope, generation);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    JOptionPane.showMessageDialog(DangNhapBieuMau.this, "Quá trình đăng nhập bị gián đoạn.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                } catch (ExecutionException ex) {
                    MayTinhTacVuNenChanDoanLoi.logLoginAuthenticationFailure(ex);
                    JOptionPane.showMessageDialog(DangNhapBieuMau.this, "Không thể kết nối hoặc xác thực tài khoản.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                } catch (RuntimeException ex) {
                    MayTinhTacVuNenChanDoanLoi.logLoginAuthenticationFailure(ex);
                    NguoiDungPhien.getInstance().clearSession();
                } finally {
                    if (!finalizing) {
                        loginPending = false;
                        setLoginControlsEnabled(true);
                        txtUsername.requestFocusInWindow();
                    }
                }
            }
        }.execute();
    }

    /** Hàm `finalizeLoginSession`: `finalize` dangnhap phien. */
    private void finalizeLoginSession(DaXacThucNguoiDung user, RoleChoice role, WorkScopeChoice scope, long generation) {
        CompleteDangNhapPhienCommand finalizeCommand = new CompleteDangNhapPhienCommand(user, role, scope);
        new SwingWorker<FinalizedLoginAttempt, Void>() {
            @Override
            protected FinalizedLoginAttempt doInBackground() {
                FinalizedLoginAttempt attempt = prepareSession(finalizeCommand,
                        completeLoginSessionHandler::handle,
                        completed -> XacThucGhepNoiGoc.createNavigationPermissionsHandler().handle(
                                completed.role().id(), completed.role().code()),
                        () -> XacThucGhepNoiGoc.createSessionLifecycleService(NguoiDungPhien.getInstance()));
                if (attempt.lifecycle() != null) {
                    // Decode the welcome resource off the EDT before constructing its Swing view.
                    try { Class.forName("vn.clinicmanagement.desktop.modules.home.HomeWelcomePanel"); }
                    catch (ClassNotFoundException ex) { throw new IllegalStateException("Welcome view unavailable."); }
                }
                return attempt;
            }

            @Override
            protected void done() {
                if (!isDisplayable() || generation != attemptGeneration) return;
                PhienVongDoiDichVu activated = null;
                try {
                    FinalizedLoginAttempt attempt = get();
                    KetQua<DaHoanTatDangNhapPhien> result = attempt.result();
                    if (result.isFailure()) {
                        NguoiDungPhien.getInstance().clearSession();
                        String message = toLoginFailureMessage(result, "Không thể hoàn tất đăng nhập.");
                        JOptionPane.showMessageDialog(
                                DangNhapBieuMau.this, message, "Đăng nhập thất bại", JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    KetQua<Set<String>> navigationResult = attempt.navigationResult();
                    if (navigationResult == null || navigationResult.isFailure()) {
                        NguoiDungPhien.getInstance().clearSession();
                        JOptionPane.showMessageDialog(
                                DangNhapBieuMau.this,
                                "Không thể tải quyền điều hướng. Đăng nhập bị từ chối.",
                                "Không thể mở phân hệ",
                                JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    DaHoanTatDangNhapPhien completed = result.valueOrThrow();
                    PhienVongDoiDichVu lifecycle = attempt.lifecycle();
                    if (!attempt.activateIfCurrent(() -> isDisplayable() && generation == attemptGeneration)) return;
                    activated = lifecycle;

                    MainBangDieuKhienCuaSo dashboard = new MainBangDieuKhienCuaSo(
                            completed, navigationResult.valueOrThrow(), lifecycle);
                    dashboard.setVisible(true);
                    dispose();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    NguoiDungPhien.getInstance().clearSession();
                    JOptionPane.showMessageDialog(
                            DangNhapBieuMau.this, "Quá trình đăng nhập bị gián đoạn.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                } catch (ExecutionException ex) {
                    MayTinhTacVuNenChanDoanLoi.logLoginSessionCompletionFailure(ex);
                    NguoiDungPhien.getInstance().clearSession();
                    JOptionPane.showMessageDialog(
                            DangNhapBieuMau.this, "Không thể kết nối hoặc xác thực tài khoản.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                } catch (RuntimeException ex) {
                    if (activated != null) activated.clear();
                    else NguoiDungPhien.getInstance().clearSession();
                    MayTinhTacVuNenChanDoanLoi.logLoginSessionCompletionFailure(ex);
                    JOptionPane.showMessageDialog(DangNhapBieuMau.this, "Không thể hoàn tất đăng nhập.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                } finally {
                    loginPending = false;
                    if (isDisplayable()) {
                        setLoginControlsEnabled(true);
                        txtUsername.requestFocusInWindow();
                    }
                }
            }
        }.execute();
    }

    /** Hàm `setLoginControlsEnabled`: gán dangnhap `Controls` bat. */
    private void setLoginControlsEnabled(boolean enabled) {
        btnLogin.setEnabled(enabled);
        txtUsername.setEnabled(enabled);
        txtPassword.setEnabled(enabled);
    }

    /** Hàm `validateAndScrubIfInvalid`: kiểm tra hợp lệ va `Scrub` `If` khonghople. */
    static boolean validateAndScrubIfInvalid(String username, char[] password) {
        if (username == null || username.trim().isEmpty() || password == null || password.length == 0) {
            if (password != null) java.util.Arrays.fill(password, '\0');
            return false;
        }
        return true;
    }

    /** Hàm `toLoginFailureMessage`: den dangnhap thất bại thông báo. */
    static String toLoginFailureMessage(KetQua<?> result, String fallback) {
        return result == null ? fallback : result.getError().map(KetQua.ApplicationError::message).orElse(fallback);
    }

    /** Hàm `prepareSession`: `prepare` phien. */
    static FinalizedLoginAttempt prepareSession(
            CompleteDangNhapPhienCommand command,
            java.util.function.Function<CompleteDangNhapPhienCommand, KetQua<DaHoanTatDangNhapPhien>> complete,
            java.util.function.Function<DaHoanTatDangNhapPhien, KetQua<Set<String>>> loadGrants,
            java.util.function.Supplier<PhienVongDoiDichVu> createLifecycle) {
        if (SwingUtilities.isEventDispatchThread()) throw new IllegalStateException("Login I/O requires a worker.");
        KetQua<DaHoanTatDangNhapPhien> result = complete.apply(command);
        if (result.isFailure()) return new FinalizedLoginAttempt(result, null, null);
        KetQua<Set<String>> navigation = loadGrants.apply(result.valueOrThrow());
        if (navigation.isFailure()) return new FinalizedLoginAttempt(result, navigation, null);
        return new FinalizedLoginAttempt(result, navigation, Objects.requireNonNull(createLifecycle.get()));
    }

    /** Hàm `chooseAssigned`: `choose` daphancong. */
    static <T> T chooseAssigned(java.util.List<T> choices, java.util.function.Function<Object[], Object> prompt) {
        if (choices.size() == 1) return choices.get(0);
        if (choices.isEmpty()) return null;
        Object selected = prompt.apply(choices.toArray());
        return choices.stream().filter(choice -> choice == selected).findFirst().orElse(null);
    }

    record FinalizedLoginAttempt(
            /** Trường `result`: kết quả. */
            KetQua<DaHoanTatDangNhapPhien> result,
            /** Trường `navigationResult`: dieuhuong kết quả. */
            KetQua<Set<String>> navigationResult,
            /** Trường `lifecycle`: vongdoi. */
            PhienVongDoiDichVu lifecycle) {
        /** Hàm `activateIfCurrent`: `activate` `If` hientai. */
        boolean activateIfCurrent(java.util.function.BooleanSupplier stillCurrent) {
            if (!SwingUtilities.isEventDispatchThread()) throw new IllegalStateException("Session publication requires EDT.");
            if (!stillCurrent.getAsBoolean() || result.isFailure() || navigationResult == null
                    || navigationResult.isFailure() || lifecycle == null) return false;
            lifecycle.startSession(result.valueOrThrow());
            return true;
        }
    }

    /** Hàm `chooseRole`: `choose` vai trò. */
    private RoleChoice chooseRole(java.util.List<RoleChoice> roles) {
        return chooseAssigned(roles, choices -> JOptionPane.showInputDialog(this,
                "Chọn vai trò làm việc:", "Chọn vai trò", JOptionPane.QUESTION_MESSAGE, null, choices, null));
    }

    /** Hàm `chooseScope`: `choose` phamvi. */
    private WorkScopeChoice chooseScope(java.util.List<WorkScopeChoice> scopes) {
        return chooseAssigned(scopes, choices -> JOptionPane.showInputDialog(this,
                "Chọn cơ sở và khoa làm việc:", "Chọn phạm vi làm việc", JOptionPane.QUESTION_MESSAGE, null, choices, null));
    }
}
