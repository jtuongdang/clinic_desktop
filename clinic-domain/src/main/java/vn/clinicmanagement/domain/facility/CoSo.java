package vn.clinicmanagement.domain.facility;
import vn.clinicmanagement.domain.common.BaseEntity;
import vn.clinicmanagement.domain.common.IAggregateRoot;
import vn.clinicmanagement.domain.common.enums.LoaiCoSo;
import java.util.ArrayList;
import java.util.List;

public class CoSo extends BaseEntity<Integer> implements IAggregateRoot {
    private String tenCoSo = "";
    private String diaChi = "";
    private String dienThoai = "";
    private String email;
    private LoaiCoSo loaiCoSo = LoaiCoSo.PHONG_KHAM_DA_KHOA;
    private boolean hoatDong = true;
    private List<Khoa> khoas = new ArrayList<>();

    public CoSo() {}
    public String getTenCoSo() { return tenCoSo; }
    public void setTenCoSo(String tenCoSo) { this.tenCoSo = tenCoSo; }
    public String getDiaChi() { return diaChi; }
    public void setDiaChi(String diaChi) { this.diaChi = diaChi; }
    public String getDienThoai() { return dienThoai; }
    public void setDienThoai(String dienThoai) { this.dienThoai = dienThoai; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public LoaiCoSo getLoaiCoSo() { return loaiCoSo; }
    public void setLoaiCoSo(LoaiCoSo loaiCoSo) { this.loaiCoSo = loaiCoSo; }
    public boolean isHoatDong() { return hoatDong; }
    public void setHoatDong(boolean hoatDong) { this.hoatDong = hoatDong; }
    public List<Khoa> getKhoas() { return khoas; }
    public void setKhoas(List<Khoa> khoas) { this.khoas = khoas; }
}
