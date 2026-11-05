package vn.clinicmanagement.desktop.modules.auth;

import vn.clinicmanagement.desktop.core.config.AppFonts;
import vn.clinicmanagement.desktop.core.config.UIConstants;
import vn.clinicmanagement.desktop.core.diagnostics.DesktopWorkerDiagnostics;
import vn.clinicmanagement.desktop.core.session.UserSession;
import vn.clinicmanagement.desktop.core.session.SessionLifecycleService;
import vn.clinicmanagement.desktop.modules.dashboard.MainDashboardFrame;
import vn.clinicmanagement.application.auth.AuthenticateUserCommand;
import vn.clinicmanagement.application.auth.AuthenticateUserHandler;
import vn.clinicmanagement.application.auth.AuthenticatedUser;
import vn.clinicmanagement.application.auth.AuthenticatedUser.RoleChoice;
import vn.clinicmanagement.application.auth.AuthenticatedUser.WorkScopeChoice;
import vn.clinicmanagement.application.auth.CompleteLoginSessionCommand;
import vn.clinicmanagement.application.auth.CompleteLoginSessionHandler;
import vn.clinicmanagement.application.auth.CompletedLoginSession;
import vn.clinicmanagement.application.authorization.NavigationPermissionsHandler;
import vn.clinicmanagement.application.common.Result;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Set;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

/**
 * Màn hình Đăng Nhập Hệ Thống Phòng Khám / Bệnh Viện EHC
 */
public class LoginForm extends JFrame {

    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JButton btnLogin;
    private final AuthenticateUserHandler authenticateUserHandler;
    private final CompleteLoginSessionHandler completeLoginSessionHandler;

    public LoginForm() {
        this(
                AuthenticationCompositionRoot.createHandler(),
                AuthenticationCompositionRoot.createCompleteLoginSessionHandler());
    }

    LoginForm(
            AuthenticateUserHandler authenticateUserHandler,
            CompleteLoginSessionHandler completeLoginSessionHandler) {
        this.authenticateUserHandler = Objects.requireNonNull(authenticateUserHandler, "authenticateUserHandler");
        this.completeLoginSessionHandler = Objects.requireNonNull(
                completeLoginSessionHandler, "completeLoginSessionHandler");
        initUI();
    }

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
        lblTitle.setFont(AppFonts.SECTION);
        lblTitle.setForeground(UIConstants.COLOR_PRIMARY);

        JLabel lblSub = new JLabel("Đăng nhập tài khoản & chọn phân hệ làm việc", SwingConstants.CENTER);
        lblSub.setFont(UIConstants.FONT_SMALL);
        lblSub.setForeground(Color.GRAY);

        headerPanel.add(lblTitle);
        headerPanel.add(lblSub);
        panel.add(headerPanel, BorderLayout.NORTH);

        // Form fields
        JPanel formPanel = new JPanel(new GridLayout(4, 1, 6, 6));
        formPanel.setOpaque(false);
        formPanel.setBorder(new EmptyBorder(12, 0, 12, 0));

        JLabel lblUser = new JLabel("Tên đăng nhập:");
        lblUser.setFont(UIConstants.FONT_BOLD);
        txtUsername = new JTextField();
        txtUsername.setFont(UIConstants.FONT_REGULAR);

        JLabel lblPass = new JLabel("Mật khẩu:");
        lblPass.setFont(UIConstants.FONT_BOLD);
        txtPassword = new JPasswordField();
        txtPassword.setFont(UIConstants.FONT_REGULAR);

        formPanel.add(lblUser);
        formPanel.add(txtUsername);
        formPanel.add(lblPass);
        formPanel.add(txtPassword);

        panel.add(formPanel, BorderLayout.CENTER);

        // Footer buttons
        JPanel footerPanel = new JPanel(new BorderLayout(8, 8));
        footerPanel.setOpaque(false);

        btnLogin = new JButton("ĐĂNG NHẬP HỆ THỐNG");
        btnLogin.setFont(AppFonts.SECTION);
        btnLogin.setBackground(UIConstants.COLOR_PRIMARY);
        btnLogin.setForeground(Color.WHITE);
        btnLogin.setFocusPainted(false);
        btnLogin.setOpaque(true);
        btnLogin.setPreferredSize(new Dimension(100, 42));

        btnLogin.addActionListener(e -> onLogin());

        footerPanel.add(btnLogin, BorderLayout.CENTER);
        panel.add(footerPanel, BorderLayout.SOUTH);

        setContentPane(panel);
    }

    private void onLogin() {
        UserSession.getInstance().clearSession();
        String username = txtUsername.getText().trim();
        char[] password = txtPassword.getPassword();

        if (username.isEmpty() || password.length == 0) {
            java.util.Arrays.fill(password, '\0');
            JOptionPane.showMessageDialog(this, "Vui lòng nhập tên đăng nhập và mật khẩu.", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        setLoginControlsEnabled(false);
        txtPassword.setText("");
        AuthenticateUserCommand command = new AuthenticateUserCommand(username, password);
        java.util.Arrays.fill(password, '\0');

        new SwingWorker<Result<AuthenticatedUser>, Void>() {
            @Override
            protected Result<AuthenticatedUser> doInBackground() {
                return authenticateUserHandler.handle(command);
            }

            @Override
            protected void done() {
                boolean finalizing = false;
                try {
                    Result<AuthenticatedUser> result = get();
                    if (result.isFailure()) {
                        String message = result.getError().map(Result.ApplicationError::message)
                                .orElse("Không thể đăng nhập.");
                        JOptionPane.showMessageDialog(LoginForm.this, message, "Đăng nhập thất bại", JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    AuthenticatedUser user = result.valueOrThrow();
                    RoleChoice role = chooseRole(user.roles());
                    if (role == null) return;
                    WorkScopeChoice scope = chooseScope(user.workScopes());
                    if (scope == null) return;

                    finalizing = true;
                    finalizeLoginSession(user, role, scope);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    JOptionPane.showMessageDialog(LoginForm.this, "Quá trình đăng nhập bị gián đoạn.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                } catch (ExecutionException ex) {
                    DesktopWorkerDiagnostics.logLoginAuthenticationFailure(ex);
                    JOptionPane.showMessageDialog(LoginForm.this, "Không thể kết nối hoặc xác thực tài khoản.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                } finally {
                    if (!finalizing) {
                        setLoginControlsEnabled(true);
                        txtUsername.requestFocusInWindow();
                    }
                }
            }
        }.execute();
    }

    private void finalizeLoginSession(AuthenticatedUser user, RoleChoice role, WorkScopeChoice scope) {
        CompleteLoginSessionCommand finalizeCommand = new CompleteLoginSessionCommand(user, role, scope);
        new SwingWorker<FinalizedLoginAttempt, Void>() {
            @Override
            protected FinalizedLoginAttempt doInBackground() {
                Result<CompletedLoginSession> result = completeLoginSessionHandler.handle(finalizeCommand);
                if (result.isFailure()) {
                    return new FinalizedLoginAttempt(result, null, null);
                }
                CompletedLoginSession completed = result.valueOrThrow();
                NavigationPermissionsHandler navigationPermissionsHandler =
                        AuthenticationCompositionRoot.createNavigationPermissionsHandler();
                Result<Set<String>> navigationResult = navigationPermissionsHandler.handle(
                        completed.role().id(), completed.role().code());
                if (navigationResult.isFailure()) {
                    return new FinalizedLoginAttempt(result, navigationResult, null);
                }
                SessionLifecycleService lifecycle = AuthenticationCompositionRoot
                        .createSessionLifecycleService(UserSession.getInstance());
                return new FinalizedLoginAttempt(result, navigationResult, lifecycle);
            }

            @Override
            protected void done() {
                try {
                    FinalizedLoginAttempt attempt = get();
                    Result<CompletedLoginSession> result = attempt.result();
                    if (result.isFailure()) {
                        UserSession.getInstance().clearSession();
                        String message = result.getError().map(Result.ApplicationError::message)
                                .orElse("Không thể hoàn tất đăng nhập.");
                        JOptionPane.showMessageDialog(
                                LoginForm.this, message, "Đăng nhập thất bại", JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    Result<Set<String>> navigationResult = attempt.navigationResult();
                    if (navigationResult == null || navigationResult.isFailure()) {
                        UserSession.getInstance().clearSession();
                        JOptionPane.showMessageDialog(
                                LoginForm.this,
                                "Không thể tải quyền điều hướng. Đăng nhập bị từ chối.",
                                "Không thể mở phân hệ",
                                JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    CompletedLoginSession completed = result.valueOrThrow();
                    SessionLifecycleService lifecycle = attempt.lifecycle();
                    lifecycle.startSession(completed);

                    dispose();
                    MainDashboardFrame dashboard = new MainDashboardFrame(
                            completed, navigationResult.valueOrThrow(), lifecycle);
                    dashboard.setVisible(true);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    UserSession.getInstance().clearSession();
                    JOptionPane.showMessageDialog(
                            LoginForm.this, "Quá trình đăng nhập bị gián đoạn.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                } catch (ExecutionException ex) {
                    DesktopWorkerDiagnostics.logLoginSessionCompletionFailure(ex);
                    UserSession.getInstance().clearSession();
                    JOptionPane.showMessageDialog(
                            LoginForm.this, "Không thể kết nối hoặc xác thực tài khoản.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                } finally {
                    if (isDisplayable()) {
                        setLoginControlsEnabled(true);
                        txtUsername.requestFocusInWindow();
                    }
                }
            }
        }.execute();
    }

    private void setLoginControlsEnabled(boolean enabled) {
        btnLogin.setEnabled(enabled);
        txtUsername.setEnabled(enabled);
        txtPassword.setEnabled(enabled);
    }

    private record FinalizedLoginAttempt(
            Result<CompletedLoginSession> result,
            Result<Set<String>> navigationResult,
            SessionLifecycleService lifecycle) {}

    private RoleChoice chooseRole(java.util.List<RoleChoice> roles) {
        if (roles.size() == 1) return roles.get(0);
        Object selected = JOptionPane.showInputDialog(this, "Chọn vai trò làm việc:", "Chọn vai trò",
                JOptionPane.QUESTION_MESSAGE, null, roles.toArray(), null);
        return selected instanceof RoleChoice role ? role : null;
    }

    private WorkScopeChoice chooseScope(java.util.List<WorkScopeChoice> scopes) {
        if (scopes.size() == 1) return scopes.get(0);
        Object selected = JOptionPane.showInputDialog(this, "Chọn cơ sở và khoa làm việc:", "Chọn phạm vi làm việc",
                JOptionPane.QUESTION_MESSAGE, null, scopes.toArray(), null);
        return selected instanceof WorkScopeChoice scope ? scope : null;
    }
}
