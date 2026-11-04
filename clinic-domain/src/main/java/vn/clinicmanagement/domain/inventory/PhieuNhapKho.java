package vn.clinicmanagement.domain.inventory;
import vn.clinicmanagement.domain.common.BaseEntity;
import vn.clinicmanagement.domain.common.IAggregateRoot;
import vn.clinicmanagement.domain.common.enums.TrangThaiPhieuNhap;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PhieuNhapKho extends BaseEntity<Integer> implements IAggregateRoot {
    private String soPhieuNhap = "";
    private int maKho;
    private int maNhaCungCap;
    private LocalDateTime ngayNhap = LocalDateTime.now();
    private BigDecimal tongTien = BigDecimal.ZERO;
    private TrangThaiPhieuNhap trangThai = TrangThaiPhieuNhap.TAM;
    private String ghiChu = "";
    private List<ChiTietNhapKho> chiTiets = new ArrayList<>();

    public PhieuNhapKho() {}
    public String getSoPhieuNhap() { return soPhieuNhap; }
    public void setSoPhieuNhap(String soPhieuNhap) { this.soPhieuNhap = soPhieuNhap; }
    public int getMaKho() { return maKho; }
    public void setMaKho(int maKho) { this.maKho = maKho; }
    public int getMaNhaCungCap() { return maNhaCungCap; }
    public void setMaNhaCungCap(int maNhaCungCap) { this.maNhaCungCap = maNhaCungCap; }
    public LocalDateTime getNgayNhap() { return ngayNhap; }
    public void setNgayNhap(LocalDateTime ngayNhap) { this.ngayNhap = ngayNhap; }
    public BigDecimal getTongTien() { return tongTien; }
    public void setTongTien(BigDecimal tongTien) { this.tongTien = tongTien; }
    public TrangThaiPhieuNhap getTrangThai() { return trangThai; }
    public void setTrangThai(TrangThaiPhieuNhap trangThai) { this.trangThai = trangThai; }
    public String getGhiChu() { return ghiChu; }
    public void setGhiChu(String ghiChu) { this.ghiChu = ghiChu; }
    public List<ChiTietNhapKho> getChiTiets() { return chiTiets; }
    public void setChiTiets(List<ChiTietNhapKho> chiTiets) { this.chiTiets = chiTiets; }
}
