package vn.clinicmanagement.domain.inventory;
import vn.clinicmanagement.domain.common.BaseEntity;
import vn.clinicmanagement.domain.common.IAggregateRoot;
import vn.clinicmanagement.domain.common.enums.LoaiKho;
import vn.clinicmanagement.domain.common.enums.TrangThaiKho;

public class Kho extends BaseEntity<Integer> implements IAggregateRoot {
    private int maCoSo;
    private String tenKho = "";
    private LoaiKho loaiKho = LoaiKho.THUOC;
    private TrangThaiKho trangThai = TrangThaiKho.HOAT_DONG;
    private Integer maNguoiPhuTrach;

    public Kho() {}
    public int getMaCoSo() { return maCoSo; }
    public void setMaCoSo(int maCoSo) { this.maCoSo = maCoSo; }
    public String getTenKho() { return tenKho; }
    public void setTenKho(String tenKho) { this.tenKho = tenKho; }
    public LoaiKho getLoaiKho() { return loaiKho; }
    public void setLoaiKho(LoaiKho loaiKho) { this.loaiKho = loaiKho; }
    public TrangThaiKho getTrangThai() { return trangThai; }
    public void setTrangThai(TrangThaiKho trangThai) { this.trangThai = trangThai; }
    public Integer getMaNguoiPhuTrach() { return maNguoiPhuTrach; }
    public void setMaNguoiPhuTrach(Integer maNguoiPhuTrach) { this.maNguoiPhuTrach = maNguoiPhuTrach; }
}
