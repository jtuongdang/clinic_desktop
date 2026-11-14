package vn.clinicmanagement.domain.facility;
import vn.clinicmanagement.domain.common.CoSoThucThe;
import java.util.ArrayList;
import java.util.List;

public class Khoa extends CoSoThucThe<Integer> {
    private int maCoSo;
    private String tenKhoa = "";
    private String moTa;
    private boolean hoatDong = true;
    private CoSo coSo;
    private List<PhongKham> phongKhams = new ArrayList<>();

    public Khoa() {}
    /** Hàm `getMaCoSo`: lấy mã có so. */
    public int getMaCoSo() { return maCoSo; }
    /** Hàm `setMaCoSo`: gán mã có so. */
    public void setMaCoSo(int maCoSo) { this.maCoSo = maCoSo; }
    /** Hàm `getTenKhoa`: lấy `Ten` khoa. */
    public String getTenKhoa() { return tenKhoa; }
    /** Hàm `setTenKhoa`: gán `Ten` khoa. */
    public void setTenKhoa(String tenKhoa) { this.tenKhoa = tenKhoa; }
    /** Hàm `getMoTa`: lấy `Mo` `Ta`. */
    public String getMoTa() { return moTa; }
    /** Hàm `setMoTa`: gán `Mo` `Ta`. */
    public void setMoTa(String moTa) { this.moTa = moTa; }
    /** Hàm `isHoatDong`: là `Hoat` đóng. */
    public boolean isHoatDong() { return hoatDong; }
    /** Hàm `setHoatDong`: gán `Hoat` đóng. */
    public void setHoatDong(boolean hoatDong) { this.hoatDong = hoatDong; }
    /** Hàm `getCoSo`: lấy có so. */
    public CoSo getCoSo() { return coSo; }
    /** Hàm `setCoSo`: gán có so. */
    public void setCoSo(CoSo coSo) { this.coSo = coSo; }
    /** Hàm `getPhongKhams`: lấy phòng `Khams`. */
    public List<PhongKham> getPhongKhams() { return phongKhams; }
    /** Hàm `setPhongKhams`: gán phòng `Khams`. */
    public void setPhongKhams(List<PhongKham> phongKhams) { this.phongKhams = phongKhams; }
}
