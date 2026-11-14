package vn.clinicmanagement.domain.inventory;
import vn.clinicmanagement.domain.common.CoSoThucThe;
import vn.clinicmanagement.domain.common.ITongHopGoc;
import vn.clinicmanagement.domain.common.enums.LoaiKho;
import vn.clinicmanagement.domain.common.enums.TrangThaiKho;

public class Kho extends CoSoThucThe<Integer> implements ITongHopGoc {
    private int maCoSo;
    private String tenKho = "";
    private LoaiKho loaiKho = LoaiKho.THUOC;
    private TrangThaiKho trangThai = TrangThaiKho.HOAT_DONG;
    private Integer maNguoiPhuTrach;

    public Kho() {}
    /** Hàm `getMaCoSo`: lấy mã có so. */
    public int getMaCoSo() { return maCoSo; }
    /** Hàm `setMaCoSo`: gán mã có so. */
    public void setMaCoSo(int maCoSo) { this.maCoSo = maCoSo; }
    /** Hàm `getTenKho`: lấy `Ten` kho. */
    public String getTenKho() { return tenKho; }
    /** Hàm `setTenKho`: gán `Ten` kho. */
    public void setTenKho(String tenKho) { this.tenKho = tenKho; }
    /** Hàm `getLoaiKho`: lấy loai kho. */
    public LoaiKho getLoaiKho() { return loaiKho; }
    /** Hàm `setLoaiKho`: gán loai kho. */
    public void setLoaiKho(LoaiKho loaiKho) { this.loaiKho = loaiKho; }
    /** Hàm `getTrangThai`: lấy trang thai. */
    public TrangThaiKho getTrangThai() { return trangThai; }
    /** Hàm `setTrangThai`: gán trang thai. */
    public void setTrangThai(TrangThaiKho trangThai) { this.trangThai = trangThai; }
    /** Hàm `getMaNguoiPhuTrach`: lấy mã `Nguoi` `Phu` `Trach`. */
    public Integer getMaNguoiPhuTrach() { return maNguoiPhuTrach; }
    /** Hàm `setMaNguoiPhuTrach`: gán mã `Nguoi` `Phu` `Trach`. */
    public void setMaNguoiPhuTrach(Integer maNguoiPhuTrach) { this.maNguoiPhuTrach = maNguoiPhuTrach; }
}
