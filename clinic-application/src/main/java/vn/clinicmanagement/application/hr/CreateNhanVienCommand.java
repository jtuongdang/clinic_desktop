package vn.clinicmanagement.application.hr;

import vn.clinicmanagement.application.common.Command;

import java.util.Arrays;

public final class CreateNhanVienCommand implements Command<CreateNhanVienBienNhan>, AutoCloseable {
    /** Trường `actorId`: tacnhan mã. */
    private final int actorId;
    private final String maNhanVien;
    private final String ho;
    private final String ten;
    private final String soDienThoai;
    private final String email;
    /** Trường `maChucVu`: `ma` chuc vu. */
    private final Integer maChucVu;
    private final String username;
    /** Trường `password`: matkhau. */
    private final char[] password;

    public CreateNhanVienCommand(int actorId, String maNhanVien, String ho, String ten,
                                 String soDienThoai, String email, Integer maChucVu,
                                 String username, char[] password) {
        this.actorId = actorId;
        this.maNhanVien = trim(maNhanVien);
        this.ho = trim(ho);
        this.ten = trim(ten);
        this.soDienThoai = trim(soDienThoai);
        this.email = trim(email);
        this.maChucVu = maChucVu;
        this.username = trim(username);
        this.password = password == null ? new char[0] : Arrays.copyOf(password, password.length);
    }

    private static String trim(String value) { return value == null ? "" : value.trim(); }

    /** Hàm `actorId`: tacnhan mã. */
    public int actorId() { return actorId; }
    public String maNhanVien() { return maNhanVien; }
    public String ho() { return ho; }
    public String ten() { return ten; }
    public String soDienThoai() { return soDienThoai; }
    public String email() { return email; }
    /** Hàm `maChucVu`: `ma` chuc vu. */
    public Integer maChucVu() { return maChucVu; }
    public String username() { return username; }
    /** Hàm `passwordCopy`: matkhau `Copy`. */
    public char[] passwordCopy() { return Arrays.copyOf(password, password.length); }
    /** Hàm `close`: đóng. Gốc khai báo tại `Command#close`. */
    @Override public void close() { Arrays.fill(password, '\0'); }
}
