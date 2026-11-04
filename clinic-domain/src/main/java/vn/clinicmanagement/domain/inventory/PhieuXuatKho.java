package vn.clinicmanagement.domain.inventory;
import vn.clinicmanagement.domain.common.BaseEntity;
import vn.clinicmanagement.domain.common.IAggregateRoot;
import vn.clinicmanagement.domain.common.enums.LoaiXuatKho;
import vn.clinicmanagement.domain.common.enums.TrangThaiXuatNoiBo;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PhieuXuatKho extends BaseEntity<Integer> implements IAggregateRoot {
    private String soPhieuXuat = "";
    private int maKhoXuat;
    private LoaiXuatKho loaiXuat = LoaiXuatKho.BAN_THUOC;
    private Integer maDonThuoc;
    private Integer maKhoNhan;
    private LocalDateTime ngayXuat = LocalDateTime.now();
    private BigDecimal tongTien = BigDecimal.ZERO;
    private TrangThaiXuatNoiBo trangThaiNoiBo = TrangThaiXuatNoiBo.CHO_DUYET;
    private String ghiChu = "";
    private List<ChiTietXuatKho> chiTiets = new ArrayList<>();

    public PhieuXuatKho() {}
    public String getSoPhieuXuat() { return soPhieuXuat; }
    public void setSoPhieuXuat(String soPhieuXuat) { this.soPhieuXuat = soPhieuXuat; }
    public int getMaKhoXuat() { return maKhoXuat; }
    public void setMaKhoXuat(int maKhoXuat) { this.maKhoXuat = maKhoXuat; }
    public LoaiXuatKho getLoaiXuat() { return loaiXuat; }
    public void setLoaiXuat(LoaiXuatKho loaiXuat) { this.loaiXuat = loaiXuat; }
    public Integer getMaDonThuoc() { return maDonThuoc; }
    public void setMaDonThuoc(Integer maDonThuoc) { this.maDonThuoc = maDonThuoc; }
    public Integer getMaKhoNhan() { return maKhoNhan; }
    public void setMaKhoNhan(Integer maKhoNhan) { this.maKhoNhan = maKhoNhan; }
    public LocalDateTime getNgayXuat() { return ngayXuat; }
    public void setNgayXuat(LocalDateTime ngayXuat) { this.ngayXuat = ngayXuat; }
    public BigDecimal getTongTien() { return tongTien; }
    public void setTongTien(BigDecimal tongTien) { this.tongTien = tongTien; }
    public TrangThaiXuatNoiBo getTrangThaiNoiBo() { return trangThaiNoiBo; }
    public void setTrangThaiNoiBo(TrangThaiXuatNoiBo trangThaiNoiBo) { this.trangThaiNoiBo = trangThaiNoiBo; }
    public String getGhiChu() { return ghiChu; }
    public void setGhiChu(String ghiChu) { this.ghiChu = ghiChu; }
    public List<ChiTietXuatKho> getChiTiets() { return chiTiets; }
    public void setChiTiets(List<ChiTietXuatKho> chiTiets) { this.chiTiets = chiTiets; }
}
