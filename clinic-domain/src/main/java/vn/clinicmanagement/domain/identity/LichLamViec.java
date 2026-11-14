package vn.clinicmanagement.domain.identity;
import vn.clinicmanagement.domain.common.CoSoThucThe;
import vn.clinicmanagement.domain.common.enums.NguonLich;
import vn.clinicmanagement.domain.common.enums.TrangThaiLichLamViec;
import java.time.LocalDate;

public class LichLamViec extends CoSoThucThe<Integer> {
    private int maNhanVien;
    private int maCaTruc;
    private int maCoSo;
    private Integer maKhoa;
    private Integer maPhongKham;
    private LocalDate ngayLamViec;
    private TrangThaiLichLamViec trangThai = TrangThaiLichLamViec.LAM_VIEC;
    private NguonLich nguonLich = NguonLich.TU_DONG_THEO_LICH_MAU;
    private String ghiChu;

    public LichLamViec() {}
    /** Hàm `getMaNhanVien`: lấy mã nhãn vien. */
    public int getMaNhanVien() { return maNhanVien; }
    /** Hàm `setMaNhanVien`: gán mã nhãn vien. */
    public void setMaNhanVien(int maNhanVien) { this.maNhanVien = maNhanVien; }
    /** Hàm `getMaCaTruc`: lấy mã ca truc. */
    public int getMaCaTruc() { return maCaTruc; }
    /** Hàm `setMaCaTruc`: gán mã ca truc. */
    public void setMaCaTruc(int maCaTruc) { this.maCaTruc = maCaTruc; }
    /** Hàm `getMaCoSo`: lấy mã có so. */
    public int getMaCoSo() { return maCoSo; }
    /** Hàm `setMaCoSo`: gán mã có so. */
    public void setMaCoSo(int maCoSo) { this.maCoSo = maCoSo; }
    /** Hàm `getMaKhoa`: lấy mã khoa. */
    public Integer getMaKhoa() { return maKhoa; }
    /** Hàm `setMaKhoa`: gán mã khoa. */
    public void setMaKhoa(Integer maKhoa) { this.maKhoa = maKhoa; }
    /** Hàm `getMaPhongKham`: lấy mã phòng kham. */
    public Integer getMaPhongKham() { return maPhongKham; }
    /** Hàm `setMaPhongKham`: gán mã phòng kham. */
    public void setMaPhongKham(Integer maPhongKham) { this.maPhongKham = maPhongKham; }
    /** Hàm `getNgayLamViec`: lấy `Ngay` lam viec. */
    public LocalDate getNgayLamViec() { return ngayLamViec; }
    /** Hàm `setNgayLamViec`: gán `Ngay` lam viec. */
    public void setNgayLamViec(LocalDate ngayLamViec) { this.ngayLamViec = ngayLamViec; }
    /** Hàm `getTrangThai`: lấy trang thai. */
    public TrangThaiLichLamViec getTrangThai() { return trangThai; }
    /** Hàm `setTrangThai`: gán trang thai. */
    public void setTrangThai(TrangThaiLichLamViec trangThai) { this.trangThai = trangThai; }
    /** Hàm `getNguonLich`: lấy nguồn lich. */
    public NguonLich getNguonLich() { return nguonLich; }
    /** Hàm `setNguonLich`: gán nguồn lich. */
    public void setNguonLich(NguonLich nguonLich) { this.nguonLich = nguonLich; }
    /** Hàm `getGhiChu`: lấy `Ghi` chu. */
    public String getGhiChu() { return ghiChu; }
    /** Hàm `setGhiChu`: gán `Ghi` chu. */
    public void setGhiChu(String ghiChu) { this.ghiChu = ghiChu; }
}
