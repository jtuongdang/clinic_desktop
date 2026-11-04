package vn.clinicmanagement.domain.identity;
import vn.clinicmanagement.domain.common.BaseEntity;
import java.time.LocalDate;

public class KyChamCong extends BaseEntity<Integer> {
    private String tenKy = "";
    private LocalDate tuNgay;
    private LocalDate denNgay;
    private boolean daKhoa = false;
    public KyChamCong() {}
    public String getTenKy() { return tenKy; }
    public void setTenKy(String tenKy) { this.tenKy = tenKy; }
    public LocalDate getTuNgay() { return tuNgay; }
    public void setTuNgay(LocalDate tuNgay) { this.tuNgay = tuNgay; }
    public LocalDate getDenNgay() { return denNgay; }
    public void setDenNgay(LocalDate denNgay) { this.denNgay = denNgay; }
    public boolean isDaKhoa() { return daKhoa; }
    public void setDaKhoa(boolean daKhoa) { this.daKhoa = daKhoa; }
}
