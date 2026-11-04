package vn.clinicmanagement.domain.identity;
import vn.clinicmanagement.domain.common.BaseEntity;
import vn.clinicmanagement.domain.common.enums.LoaiNghiPhep;
import vn.clinicmanagement.domain.common.enums.TrangThaiNghiPhep;
import java.time.LocalDate;

public class NghiPhep extends BaseEntity<Integer> {
    private int maNhanVien;
    private LoaiNghiPhep loaiNghiPhep = LoaiNghiPhep.PHEP_NAM;
    private LocalDate tuNgay;
    private LocalDate denNgay;
    private String lyDo = "";
    private TrangThaiNghiPhep trangThai = TrangThaiNghiPhep.CHO_DUYET;
    public NghiPhep() {}
    public int getMaNhanVien() { return maNhanVien; }
    public void setMaNhanVien(int maNhanVien) { this.maNhanVien = maNhanVien; }
    public LoaiNghiPhep getLoaiNghiPhep() { return loaiNghiPhep; }
    public void setLoaiNghiPhep(LoaiNghiPhep loaiNghiPhep) { this.loaiNghiPhep = loaiNghiPhep; }
    public LocalDate getTuNgay() { return tuNgay; }
    public void setTuNgay(LocalDate tuNgay) { this.tuNgay = tuNgay; }
    public LocalDate getDenNgay() { return denNgay; }
    public void setDenNgay(LocalDate denNgay) { this.denNgay = denNgay; }
    public String getLyDo() { return lyDo; }
    public void setLyDo(String lyDo) { this.lyDo = lyDo; }
    public TrangThaiNghiPhep getTrangThai() { return trangThai; }
    public void setTrangThai(TrangThaiNghiPhep trangThai) { this.trangThai = trangThai; }
}
