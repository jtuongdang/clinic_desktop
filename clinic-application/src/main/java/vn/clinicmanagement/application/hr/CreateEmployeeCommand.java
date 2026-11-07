package vn.clinicmanagement.application.hr;

import vn.clinicmanagement.application.common.Command;

import java.util.Arrays;

public final class CreateEmployeeCommand implements Command<CreateEmployeeReceipt>, AutoCloseable {
    private final int actorId;
    private final String maNhanVien;
    private final String ho;
    private final String ten;
    private final String soDienThoai;
    private final String email;
    private final Integer maChucVu;
    private final String username;
    private final char[] password;

    public CreateEmployeeCommand(int actorId, String maNhanVien, String ho, String ten,
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

    public int actorId() { return actorId; }
    public String maNhanVien() { return maNhanVien; }
    public String ho() { return ho; }
    public String ten() { return ten; }
    public String soDienThoai() { return soDienThoai; }
    public String email() { return email; }
    public Integer maChucVu() { return maChucVu; }
    public String username() { return username; }
    public char[] passwordCopy() { return Arrays.copyOf(password, password.length); }
    @Override public void close() { Arrays.fill(password, '\0'); }
}
