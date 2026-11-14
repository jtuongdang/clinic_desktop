package vn.clinicmanagement.domain.identity;
import vn.clinicmanagement.domain.common.CoSoThucThe;
import java.time.LocalTime;

public class CaTruc extends CoSoThucThe<Integer> {
    private int maLoaiCa;
    private String tenCa = "";
    private LocalTime gioBatDau;
    private LocalTime gioKetThuc;
    private boolean hoatDong = true;
    public CaTruc() {}
    /** Hàm `getMaLoaiCa`: lấy mã loai ca. */
    public int getMaLoaiCa() { return maLoaiCa; }
    /** Hàm `setMaLoaiCa`: gán mã loai ca. */
    public void setMaLoaiCa(int maLoaiCa) { this.maLoaiCa = maLoaiCa; }
    /** Hàm `getTenCa`: lấy `Ten` ca. */
    public String getTenCa() { return tenCa; }
    /** Hàm `setTenCa`: gán `Ten` ca. */
    public void setTenCa(String tenCa) { this.tenCa = tenCa; }
    /** Hàm `getGioBatDau`: lấy `Gio` `Bat` `Dau`. */
    public LocalTime getGioBatDau() { return gioBatDau; }
    /** Hàm `setGioBatDau`: gán `Gio` `Bat` `Dau`. */
    public void setGioBatDau(LocalTime gioBatDau) { this.gioBatDau = gioBatDau; }
    /** Hàm `getGioKetThuc`: lấy `Gio` ket thuc. */
    public LocalTime getGioKetThuc() { return gioKetThuc; }
    /** Hàm `setGioKetThuc`: gán `Gio` ket thuc. */
    public void setGioKetThuc(LocalTime gioKetThuc) { this.gioKetThuc = gioKetThuc; }
    /** Hàm `isHoatDong`: là `Hoat` đóng. */
    public boolean isHoatDong() { return hoatDong; }
    /** Hàm `setHoatDong`: gán `Hoat` đóng. */
    public void setHoatDong(boolean hoatDong) { this.hoatDong = hoatDong; }
}
