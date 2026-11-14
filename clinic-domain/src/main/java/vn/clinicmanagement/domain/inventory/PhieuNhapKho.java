package vn.clinicmanagement.domain.inventory;
import vn.clinicmanagement.domain.common.CoSoThucThe;
import vn.clinicmanagement.domain.common.ITongHopGoc;
import vn.clinicmanagement.domain.common.enums.TrangThaiPhieuNhap;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PhieuNhapKho extends CoSoThucThe<Integer> implements ITongHopGoc {
    private String soPhieuNhap = "";
    private int maKho;
    private int maNhaCungCap;
    private LocalDateTime ngayNhap = LocalDateTime.now();
    private BigDecimal tongTien = BigDecimal.ZERO;
    private TrangThaiPhieuNhap trangThai = TrangThaiPhieuNhap.TAM;
    private String ghiChu = "";
    private List<ChiTietNhapKho> chiTiets = new ArrayList<>();

    public PhieuNhapKho() {}
    /** Hàm `getSoPhieuNhap`: lấy so phiếu nhap. */
    public String getSoPhieuNhap() { return soPhieuNhap; }
    /** Hàm `setSoPhieuNhap`: gán so phiếu nhap. */
    public void setSoPhieuNhap(String soPhieuNhap) { this.soPhieuNhap = soPhieuNhap; }
    /** Hàm `getMaKho`: lấy mã kho. */
    public int getMaKho() { return maKho; }
    /** Hàm `setMaKho`: gán mã kho. */
    public void setMaKho(int maKho) { this.maKho = maKho; }
    /** Hàm `getMaNhaCungCap`: lấy mã nha cung cap. */
    public int getMaNhaCungCap() { return maNhaCungCap; }
    /** Hàm `setMaNhaCungCap`: gán mã nha cung cap. */
    public void setMaNhaCungCap(int maNhaCungCap) { this.maNhaCungCap = maNhaCungCap; }
    /** Hàm `getNgayNhap`: lấy `Ngay` nhap. */
    public LocalDateTime getNgayNhap() { return ngayNhap; }
    /** Hàm `setNgayNhap`: gán `Ngay` nhap. */
    public void setNgayNhap(LocalDateTime ngayNhap) { this.ngayNhap = ngayNhap; }
    /** Hàm `getTongTien`: lấy `Tong` tien. */
    public BigDecimal getTongTien() { return tongTien; }
    /** Hàm `setTongTien`: gán `Tong` tien. */
    public void setTongTien(BigDecimal tongTien) { this.tongTien = tongTien; }
    /** Hàm `getTrangThai`: lấy trang thai. */
    public TrangThaiPhieuNhap getTrangThai() { return trangThai; }
    /** Hàm `setTrangThai`: gán trang thai. */
    public void setTrangThai(TrangThaiPhieuNhap trangThai) { this.trangThai = trangThai; }
    /** Hàm `getGhiChu`: lấy `Ghi` chu. */
    public String getGhiChu() { return ghiChu; }
    /** Hàm `setGhiChu`: gán `Ghi` chu. */
    public void setGhiChu(String ghiChu) { this.ghiChu = ghiChu; }
    /** Hàm `getChiTiets`: lấy chi `Tiets`. */
    public List<ChiTietNhapKho> getChiTiets() { return chiTiets; }
    /** Hàm `setChiTiets`: gán chi `Tiets`. */
    public void setChiTiets(List<ChiTietNhapKho> chiTiets) { this.chiTiets = chiTiets; }
}
