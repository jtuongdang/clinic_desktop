package vn.clinicmanagement.domain.identity;
import vn.clinicmanagement.domain.common.CoSoThucThe;
import vn.clinicmanagement.domain.common.enums.LoaiNghiPhep;
import vn.clinicmanagement.domain.common.enums.TrangThaiNghiPhep;
import java.time.LocalDate;

public class NghiPhep extends CoSoThucThe<Integer> {
    private int maNhanVien;
    private LoaiNghiPhep loaiNghiPhep = LoaiNghiPhep.PHEP_NAM;
    private LocalDate tuNgay;
    private LocalDate denNgay;
    private String lyDo = "";
    private TrangThaiNghiPhep trangThai = TrangThaiNghiPhep.CHO_DUYET;
    public NghiPhep() {}
    /** Hàm `getMaNhanVien`: lấy mã nhãn vien. */
    public int getMaNhanVien() { return maNhanVien; }
    /** Hàm `setMaNhanVien`: gán mã nhãn vien. */
    public void setMaNhanVien(int maNhanVien) { this.maNhanVien = maNhanVien; }
    /** Hàm `getLoaiNghiPhep`: lấy loai nghi phep. */
    public LoaiNghiPhep getLoaiNghiPhep() { return loaiNghiPhep; }
    /** Hàm `setLoaiNghiPhep`: gán loai nghi phep. */
    public void setLoaiNghiPhep(LoaiNghiPhep loaiNghiPhep) { this.loaiNghiPhep = loaiNghiPhep; }
    /** Hàm `getTuNgay`: lấy tu `Ngay`. */
    public LocalDate getTuNgay() { return tuNgay; }
    /** Hàm `setTuNgay`: gán tu `Ngay`. */
    public void setTuNgay(LocalDate tuNgay) { this.tuNgay = tuNgay; }
    /** Hàm `getDenNgay`: lấy `Den` `Ngay`. */
    public LocalDate getDenNgay() { return denNgay; }
    /** Hàm `setDenNgay`: gán `Den` `Ngay`. */
    public void setDenNgay(LocalDate denNgay) { this.denNgay = denNgay; }
    /** Hàm `getLyDo`: lấy `Ly` `Do`. */
    public String getLyDo() { return lyDo; }
    /** Hàm `setLyDo`: gán `Ly` `Do`. */
    public void setLyDo(String lyDo) { this.lyDo = lyDo; }
    /** Hàm `getTrangThai`: lấy trang thai. */
    public TrangThaiNghiPhep getTrangThai() { return trangThai; }
    /** Hàm `setTrangThai`: gán trang thai. */
    public void setTrangThai(TrangThaiNghiPhep trangThai) { this.trangThai = trangThai; }
}
