package vn.clinicmanagement.desktop.modules.auth;

import vn.clinicmanagement.desktop.core.config.AppFonts;
import vn.clinicmanagement.desktop.core.config.UIConstants;
import vn.clinicmanagement.desktop.core.session.UserSession;
import vn.clinicmanagement.desktop.modules.dashboard.MainDashboardFrame;
import vn.clinicmanagement.application.auth.AuthenticateUserCommand;
import vn.clinicmanagement.application.auth.AuthenticateUserHandler;
import vn.clinicmanagement.application.auth.AuthenticatedUser;
import vn.clinicmanagement.application.common.Result;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.concurrent.ExecutionException;

/**
 * Màn hình Đăng Nhập Hệ Thống Phòng Khám / Bệnh Viện EHC
 */
public class LoginForm extends JFrame {

    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JButton btnLogin;
    private final AuthenticateUserHandler authenticateUserHandler = AuthenticationCompositionRoot.createHandler();

    public LoginForm() {
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
        String username = txtUsername.getText().trim();
        char[] password = txtPassword.getPassword();

        if (username.isEmpty() || password.length == 0) {
            java.util.Arrays.fill(password, '\0');
            JOptionPane.showMessageDialog(this, "Vui lòng nhập tên đăng nhập và mật khẩu.", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        btnLogin.setEnabled(false);
        txtUsername.setEnabled(false);
        txtPassword.setEnabled(false);
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
                try {
                    Result<AuthenticatedUser> result = get();
                    if (result.isFailure()) {
                        String message = result.getError().map(Result.ApplicationError::message)
                                .orElse("Không thể đăng nhập.");
                        JOptionPane.showMessageDialog(LoginForm.this, message, "Đăng nhập thất bại", JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    AuthenticatedUser user = result.valueOrThrow();
                    UserSession session = UserSession.getInstance();
                    session.setSession(user.userId(), user.username(), user.fullName(), user.roleName());
                    session.setDepartmentName("Chưa phân công");

                    dispose();
                    MainDashboardFrame dashboard = new MainDashboardFrame(user.username(), user.roleName());
                    dashboard.setVisible(true);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    JOptionPane.showMessageDialog(LoginForm.this, "Quá trình đăng nhập bị gián đoạn.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                } catch (ExecutionException ex) {
                    JOptionPane.showMessageDialog(LoginForm.this, "Không thể kết nối hoặc xác thực tài khoản.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                } finally {
                    btnLogin.setEnabled(true);
                    txtUsername.setEnabled(true);
                    txtPassword.setEnabled(true);
                    txtUsername.requestFocusInWindow();
                }
            }
        }.execute();
    }
}
