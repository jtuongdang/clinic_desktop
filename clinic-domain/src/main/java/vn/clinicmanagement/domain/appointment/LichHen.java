package vn.clinicmanagement.domain.appointment;
import vn.clinicmanagement.domain.common.CoSoThucThe;
import vn.clinicmanagement.domain.common.ITongHopGoc;
import vn.clinicmanagement.domain.common.enums.TrangThaiLichHen;
import vn.clinicmanagement.domain.common.events.LichHenDaDatSuKien;
import vn.clinicmanagement.domain.facility.CoSo;
import vn.clinicmanagement.domain.facility.Khoa;
import vn.clinicmanagement.domain.identity.NhanVien;
import vn.clinicmanagement.domain.patient.BenhNhan;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class LichHen extends CoSoThucThe<Integer> implements ITongHopGoc {
    private int maBenhNhan;
    private int maBacSi;
    private int maCoSo;
    private Integer maKhoa;
    private LocalDateTime thoiGianBatDau;
    private LocalDateTime thoiGianKetThuc;
    private TrangThaiLichHen trangThai = TrangThaiLichHen.CHO_XAC_NHAN;
    private String lyDo = "";
    private String nguonDat = "LE_TAN";
    private BigDecimal chiPhiGiuCho = new BigDecimal("100000");
    private boolean daThanhToanGiuCho = false;
    private String soPhieuThu;
    private LocalDateTime ngayThanhToan;
    private Integer nguoiThuTien;
    private String ghiChuHuy;

    private BenhNhan benhNhan;
    private NhanVien bacSi;
    private CoSo coSo;
    private Khoa khoa;

    public LichHen() {}
    public LichHen(int maBenhNhan, int maBacSi, int maCoSo, Integer maKhoa, LocalDateTime thoiGianBatDau, LocalDateTime thoiGianKetThuc, String lyDo, String nguonDat, Integer createdBy) {
        this.maBenhNhan = maBenhNhan;
        this.maBacSi = maBacSi;
        this.maCoSo = maCoSo;
        this.maKhoa = maKhoa;
        this.thoiGianBatDau = thoiGianBatDau;
        this.thoiGianKetThuc = thoiGianKetThuc;
        this.lyDo = lyDo != null ? lyDo : "";
        this.nguonDat = nguonDat != null ? nguonDat : "LE_TAN";
        setCreatedBy(createdBy);
        this.trangThai = TrangThaiLichHen.CHO_XAC_NHAN;
        registerDomainEvent(new LichHenDaDatSuKien(getId() != null ? getId() : 0, maBenhNhan, maBacSi, thoiGianBatDau));
    }

    public void xacNhanThuTienMat(String soPhieuThu, int nguoiThu) {
        if (this.trangThai == TrangThaiLichHen.DA_HUY) throw new IllegalStateException("Lịch hẹn đã bị hủy.");
        this.daThanhToanGiuCho = true;
        this.soPhieuThu = soPhieuThu;
        this.ngayThanhToan = LocalDateTime.now();
        this.nguoiThuTien = nguoiThu;
        this.trangThai = TrangThaiLichHen.DA_XAC_NHAN;
    }

    /** Hàm `huyLichHen`: `huy` lich hen. */
    public void huyLichHen(String lyDoHuy) {
        if (this.trangThai == TrangThaiLichHen.DA_DEN || this.trangThai == TrangThaiLichHen.HOAN_THANH) {
            throw new IllegalStateException("Không thể hủy lịch hẹn đã tiếp nhận hoặc hoàn thành.");
        }
        this.trangThai = TrangThaiLichHen.DA_HUY;
        this.ghiChuHuy = lyDoHuy;
    }

    /** Hàm `getMaBenhNhan`: lấy mã benh nhãn. */
    public int getMaBenhNhan() { return maBenhNhan; }
    /** Hàm `setMaBenhNhan`: gán mã benh nhãn. */
    public void setMaBenhNhan(int maBenhNhan) { this.maBenhNhan = maBenhNhan; }
    /** Hàm `getMaBacSi`: lấy mã `Bac` `Si`. */
    public int getMaBacSi() { return maBacSi; }
    /** Hàm `setMaBacSi`: gán mã `Bac` `Si`. */
    public void setMaBacSi(int maBacSi) { this.maBacSi = maBacSi; }
    /** Hàm `getMaCoSo`: lấy mã có so. */
    public int getMaCoSo() { return maCoSo; }
    /** Hàm `setMaCoSo`: gán mã có so. */
    public void setMaCoSo(int maCoSo) { this.maCoSo = maCoSo; }
    /** Hàm `getMaKhoa`: lấy mã khoa. */
    public Integer getMaKhoa() { return maKhoa; }
    /** Hàm `setMaKhoa`: gán mã khoa. */
    public void setMaKhoa(Integer maKhoa) { this.maKhoa = maKhoa; }
    /** Hàm `getThoiGianBatDau`: lấy `Thoi` `Gian` `Bat` `Dau`. */
    public LocalDateTime getThoiGianBatDau() { return thoiGianBatDau; }
    /** Hàm `setThoiGianBatDau`: gán `Thoi` `Gian` `Bat` `Dau`. */
    public void setThoiGianBatDau(LocalDateTime thoiGianBatDau) { this.thoiGianBatDau = thoiGianBatDau; }
    /** Hàm `getThoiGianKetThuc`: lấy `Thoi` `Gian` ket thuc. */
    public LocalDateTime getThoiGianKetThuc() { return thoiGianKetThuc; }
    /** Hàm `setThoiGianKetThuc`: gán `Thoi` `Gian` ket thuc. */
    public void setThoiGianKetThuc(LocalDateTime thoiGianKetThuc) { this.thoiGianKetThuc = thoiGianKetThuc; }
    /** Hàm `getTrangThai`: lấy trang thai. */
    public TrangThaiLichHen getTrangThai() { return trangThai; }
    /** Hàm `setTrangThai`: gán trang thai. */
    public void setTrangThai(TrangThaiLichHen trangThai) { this.trangThai = trangThai; }
    /** Hàm `getLyDo`: lấy `Ly` `Do`. */
    public String getLyDo() { return lyDo; }
    /** Hàm `setLyDo`: gán `Ly` `Do`. */
    public void setLyDo(String lyDo) { this.lyDo = lyDo; }
    /** Hàm `getNguonDat`: lấy nguồn đặt. */
    public String getNguonDat() { return nguonDat; }
    /** Hàm `setNguonDat`: gán nguồn đặt. */
    public void setNguonDat(String nguonDat) { this.nguonDat = nguonDat; }
    /** Hàm `getChiPhiGiuCho`: lấy chi `Phi` `Giu` `Cho`. */
    public BigDecimal getChiPhiGiuCho() { return chiPhiGiuCho; }
    /** Hàm `setChiPhiGiuCho`: gán chi `Phi` `Giu` `Cho`. */
    public void setChiPhiGiuCho(BigDecimal chiPhiGiuCho) { this.chiPhiGiuCho = chiPhiGiuCho; }
    /** Hàm `isDaThanhToanGiuCho`: là `Da` thanh toan `Giu` `Cho`. */
    public boolean isDaThanhToanGiuCho() { return daThanhToanGiuCho; }
    /** Hàm `setDaThanhToanGiuCho`: gán `Da` thanh toan `Giu` `Cho`. */
    public void setDaThanhToanGiuCho(boolean daThanhToanGiuCho) { this.daThanhToanGiuCho = daThanhToanGiuCho; }
    /** Hàm `getSoPhieuThu`: lấy so phiếu thu. */
    public String getSoPhieuThu() { return soPhieuThu; }
    /** Hàm `setSoPhieuThu`: gán so phiếu thu. */
    public void setSoPhieuThu(String soPhieuThu) { this.soPhieuThu = soPhieuThu; }
    /** Hàm `getNgayThanhToan`: lấy `Ngay` thanh toan. */
    public LocalDateTime getNgayThanhToan() { return ngayThanhToan; }
    /** Hàm `setNgayThanhToan`: gán `Ngay` thanh toan. */
    public void setNgayThanhToan(LocalDateTime ngayThanhToan) { this.ngayThanhToan = ngayThanhToan; }
    /** Hàm `getNguoiThuTien`: lấy `Nguoi` thu tien. */
    public Integer getNguoiThuTien() { return nguoiThuTien; }
    /** Hàm `setNguoiThuTien`: gán `Nguoi` thu tien. */
    public void setNguoiThuTien(Integer nguoiThuTien) { this.nguoiThuTien = nguoiThuTien; }
    /** Hàm `getGhiChuHuy`: lấy `Ghi` chu `Huy`. */
    public String getGhiChuHuy() { return ghiChuHuy; }
    /** Hàm `setGhiChuHuy`: gán `Ghi` chu `Huy`. */
    public void setGhiChuHuy(String ghiChuHuy) { this.ghiChuHuy = ghiChuHuy; }
    /** Hàm `getBenhNhan`: lấy benh nhãn. */
    public BenhNhan getBenhNhan() { return benhNhan; }
    /** Hàm `setBenhNhan`: gán benh nhãn. */
    public void setBenhNhan(BenhNhan benhNhan) { this.benhNhan = benhNhan; }
    /** Hàm `getBacSi`: lấy `Bac` `Si`. */
    public NhanVien getBacSi() { return bacSi; }
    /** Hàm `setBacSi`: gán `Bac` `Si`. */
    public void setBacSi(NhanVien bacSi) { this.bacSi = bacSi; }
    /** Hàm `getCoSo`: lấy có so. */
    public CoSo getCoSo() { return coSo; }
    /** Hàm `setCoSo`: gán có so. */
    public void setCoSo(CoSo coSo) { this.coSo = coSo; }
    /** Hàm `getKhoa`: lấy khoa. */
    public Khoa getKhoa() { return khoa; }
    /** Hàm `setKhoa`: gán khoa. */
    public void setKhoa(Khoa khoa) { this.khoa = khoa; }
}
