package vn.clinicmanagement.application.auth;

import vn.clinicmanagement.application.common.Command;

import java.util.Arrays;

/** Giữ bản sao mật khẩu dạng char[] và xóa sau khi xử lý. */
public final class AuthenticateNguoiDungCommand implements Command<DaXacThucNguoiDung>, AutoCloseable {
    private final String username;
    /** Trường `password`: matkhau. */
    private final char[] password;

    public AuthenticateNguoiDungCommand(String username, char[] password) {
        this.username = username == null ? "" : username.trim();
        this.password = password == null ? new char[0] : Arrays.copyOf(password, password.length);
    }

    public String username() { return username; }
    /** Hàm `passwordCopy`: matkhau `Copy`. */
    public char[] passwordCopy() { return Arrays.copyOf(password, password.length); }

    /** Hàm `close`: đóng. Gốc khai báo tại `Command#close`. */
    @Override
    public void close() { Arrays.fill(password, '\0'); }
}
