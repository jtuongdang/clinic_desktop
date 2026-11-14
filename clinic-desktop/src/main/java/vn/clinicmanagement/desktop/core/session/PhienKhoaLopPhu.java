package vn.clinicmanagement.desktop.core.session;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Arrays;
import java.util.function.Consumer;

/** Input-blocking glass-pane form shown over the retained dashboard workspace. */
public final class PhienKhoaLopPhu extends JPanel {
    /** Trường `password`: matkhau. */
    public record Credentials(String username, char[] password) {
        public Credentials { password = password.clone(); }
        /** Hàm `password`: matkhau. */
        @Override public char[] password() { return password.clone(); }
        /** Hàm `clear`: xóa. */
        public void clear() { Arrays.fill(password, '\0'); }
    }

    private final JLabel explanation = new JLabel("Phiên làm việc đã bị khóa. Đăng nhập lại để tiếp tục.", SwingConstants.CENTER);
    /** Trường `message`: thông báo. */
    private final JLabel message = new JLabel(" ", SwingConstants.CENTER);
    private final JTextField username = new JTextField(22);
    /** Trường `password`: matkhau. */
    private final JPasswordField password = new JPasswordField(22);
    private final JButton unlock = new JButton("Xác thực lại");
    private final JButton logout = new JButton("Đăng xuất");

    public PhienKhoaLopPhu(Consumer<Credentials> onAuthenticate, Runnable onLogout) {
        super(new GridBagLayout());
        setOpaque(true);
        setFocusCycleRoot(true);
        setFocusTraversalPolicy(new LayoutFocusTraversalPolicy());
        addMouseListener(new java.awt.event.MouseAdapter() {});
        addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {});
        addMouseWheelListener(event -> event.consume());
        setBackground(new Color(20, 30, 45));
        setBorder(new EmptyBorder(24, 24, 24, 24));
        JPanel card = new JPanel(new GridLayout(0, 1, 8, 8));
        card.setBorder(new EmptyBorder(24, 32, 24, 32));
        explanation.setForeground(new Color(25, 45, 70));
        card.add(explanation);
        card.add(new JLabel("Tên đăng nhập"));
        card.add(username);
        card.add(new JLabel("Mật khẩu"));
        card.add(password);
        JPanel buttons = new JPanel(new FlowLayout());
        buttons.add(unlock);
        buttons.add(logout);
        card.add(buttons);
        message.setForeground(new Color(150, 0, 0));
        card.add(message);
        add(card);
        unlock.addActionListener(e -> {
            char[] secret = password.getPassword();
            password.setText("");
            Credentials credentials = new Credentials(username.getText().trim(), secret);
            Arrays.fill(secret, '\0');
            onAuthenticate.accept(credentials);
        });
        password.addActionListener(e -> unlock.doClick());
        logout.addActionListener(e -> onLogout.run());
    }

    public void focusCredentials() { username.requestFocusInWindow(); }

    /** Hàm `setAbsoluteExpiry`: gán `Absolute` `Expiry`. */
    public void setAbsoluteExpiry(boolean expired) {
        explanation.setText(expired
                ? "Phiên làm việc đã hết hạn. Xác thực lại để bắt đầu phiên mới."
                : "Phiên làm việc đã bị khóa do không hoạt động. Xác thực lại để tiếp tục.");
    }

    /** Hàm `setMessage`: gán thông báo. */
    public void setMessage(String text) { message.setText(text == null || text.isBlank() ? " " : text); }
    /** Hàm `setBusy`: gán `Busy`. */
    public void setBusy(boolean busy) { unlock.setEnabled(!busy); username.setEnabled(!busy); password.setEnabled(!busy); }
    /** Hàm `isInputBlocked`: là dauvao `Blocked`. */
    public boolean isInputBlocked() { return isVisible() && isOpaque(); }
    /** Hàm `getUnlockButton`: lấy `Unlock` nút. */
    public JButton getUnlockButton() { return unlock; }
    /** Hàm `getLogoutButton`: lấy `Logout` nút. */
    public JButton getLogoutButton() { return logout; }
}
