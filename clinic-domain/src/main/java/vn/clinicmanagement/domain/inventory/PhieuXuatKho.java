package vn.clinicmanagement.domain.inventory;
import vn.clinicmanagement.domain.common.CoSoThucThe;
import vn.clinicmanagement.domain.common.ITongHopGoc;
import vn.clinicmanagement.domain.common.enums.LoaiXuatKho;
import vn.clinicmanagement.domain.common.enums.TrangThaiXuatNoiBo;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PhieuXuatKho extends CoSoThucThe<Integer> implements ITongHopGoc {
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
    /** Hàm `getSoPhieuXuat`: lấy so phiếu xuất. */
    public String getSoPhieuXuat() { return soPhieuXuat; }
    /** Hàm `setSoPhieuXuat`: gán so phiếu xuất. */
    public void setSoPhieuXuat(String soPhieuXuat) { this.soPhieuXuat = soPhieuXuat; }
    /** Hàm `getMaKhoXuat`: lấy mã kho xuất. */
    public int getMaKhoXuat() { return maKhoXuat; }
    /** Hàm `setMaKhoXuat`: gán mã kho xuất. */
    public void setMaKhoXuat(int maKhoXuat) { this.maKhoXuat = maKhoXuat; }
    /** Hàm `getLoaiXuat`: lấy loai xuất. */
    public LoaiXuatKho getLoaiXuat() { return loaiXuat; }
    /** Hàm `setLoaiXuat`: gán loai xuất. */
    public void setLoaiXuat(LoaiXuatKho loaiXuat) { this.loaiXuat = loaiXuat; }
    /** Hàm `getMaDonThuoc`: lấy mã don thuốc. */
    public Integer getMaDonThuoc() { return maDonThuoc; }
    /** Hàm `setMaDonThuoc`: gán mã don thuốc. */
    public void setMaDonThuoc(Integer maDonThuoc) { this.maDonThuoc = maDonThuoc; }
    /** Hàm `getMaKhoNhan`: lấy mã kho nhãn. */
    public Integer getMaKhoNhan() { return maKhoNhan; }
    /** Hàm `setMaKhoNhan`: gán mã kho nhãn. */
    public void setMaKhoNhan(Integer maKhoNhan) { this.maKhoNhan = maKhoNhan; }
    /** Hàm `getNgayXuat`: lấy `Ngay` xuất. */
    public LocalDateTime getNgayXuat() { return ngayXuat; }
    /** Hàm `setNgayXuat`: gán `Ngay` xuất. */
    public void setNgayXuat(LocalDateTime ngayXuat) { this.ngayXuat = ngayXuat; }
    /** Hàm `getTongTien`: lấy `Tong` tien. */
    public BigDecimal getTongTien() { return tongTien; }
    /** Hàm `setTongTien`: gán `Tong` tien. */
    public void setTongTien(BigDecimal tongTien) { this.tongTien = tongTien; }
    /** Hàm `getTrangThaiNoiBo`: lấy trang thai noi bo. */
    public TrangThaiXuatNoiBo getTrangThaiNoiBo() { return trangThaiNoiBo; }
    /** Hàm `setTrangThaiNoiBo`: gán trang thai noi bo. */
    public void setTrangThaiNoiBo(TrangThaiXuatNoiBo trangThaiNoiBo) { this.trangThaiNoiBo = trangThaiNoiBo; }
    /** Hàm `getGhiChu`: lấy `Ghi` chu. */
    public String getGhiChu() { return ghiChu; }
    /** Hàm `setGhiChu`: gán `Ghi` chu. */
    public void setGhiChu(String ghiChu) { this.ghiChu = ghiChu; }
    /** Hàm `getChiTiets`: lấy chi `Tiets`. */
    public List<ChiTietXuatKho> getChiTiets() { return chiTiets; }
    /** Hàm `setChiTiets`: gán chi `Tiets`. */
    public void setChiTiets(List<ChiTietXuatKho> chiTiets) { this.chiTiets = chiTiets; }
}
