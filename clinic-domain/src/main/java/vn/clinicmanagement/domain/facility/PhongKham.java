package vn.clinicmanagement.domain.facility;
import vn.clinicmanagement.domain.common.CoSoThucThe;

public class PhongKham extends CoSoThucThe<Integer> {
    private String maPhong;
    private int maKhoa;
    private String tenPhong = "";
    private String viTri;
    private boolean hoatDong = true;
    private Khoa khoa;

    public PhongKham() {}
    /** Hàm `getMaPhong`: lấy mã phòng. */
    public String getMaPhong() { return maPhong != null ? maPhong : tenPhong; }
    /** Hàm `setMaPhong`: gán mã phòng. */
    public void setMaPhong(String maPhong) { this.maPhong = maPhong; }
    /** Hàm `getMaKhoa`: lấy mã khoa. */
    public int getMaKhoa() { return maKhoa; }
    /** Hàm `setMaKhoa`: gán mã khoa. */
    public void setMaKhoa(int maKhoa) { this.maKhoa = maKhoa; }
    /** Hàm `getTenPhong`: lấy `Ten` phòng. */
    public String getTenPhong() { return tenPhong; }
    /** Hàm `setTenPhong`: gán `Ten` phòng. */
    public void setTenPhong(String tenPhong) { this.tenPhong = tenPhong; }
    /** Hàm `getViTri`: lấy vi tri. */
    public String getViTri() { return viTri; }
    /** Hàm `setViTri`: gán vi tri. */
    public void setViTri(String viTri) { this.viTri = viTri; }
    /** Hàm `isHoatDong`: là `Hoat` đóng. */
    public boolean isHoatDong() { return hoatDong; }
    /** Hàm `setHoatDong`: gán `Hoat` đóng. */
    public void setHoatDong(boolean hoatDong) { this.hoatDong = hoatDong; }
    /** Hàm `getKhoa`: lấy khoa. */
    public Khoa getKhoa() { return khoa; }
    /** Hàm `setKhoa`: gán khoa. */
    public void setKhoa(Khoa khoa) { this.khoa = khoa; }
}
