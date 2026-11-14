package vn.clinicmanagement.domain.billing;

import vn.clinicmanagement.domain.common.CoSoThucThe;
import vn.clinicmanagement.domain.common.ITongHopGoc;
import vn.clinicmanagement.domain.common.enums.PhuongThucThanhToan;
import vn.clinicmanagement.domain.common.enums.TrangThaiHoaDon;
import vn.clinicmanagement.domain.common.events.HoaDonDaTaoSuKien;
import vn.clinicmanagement.domain.common.events.ThanhToanDaGhiNhanSuKien;
import vn.clinicmanagement.domain.examination.LuotKham;
import vn.clinicmanagement.domain.facility.CoSo;
import vn.clinicmanagement.domain.patient.BenhNhan;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class HoaDon extends CoSoThucThe<Integer> implements ITongHopGoc {
    private String soHoaDon = "";
    private int maLuotKham;
    private int maBenhNhan;
    private int maCoSo;
    private LocalDateTime ngayLap = LocalDateTime.now();
    private BigDecimal tongTien = BigDecimal.ZERO;
    private BigDecimal soTienBaoHiemChiTra = BigDecimal.ZERO;
    private BigDecimal soTienBenhNhanChiTra = BigDecimal.ZERO;
    private BigDecimal daThanhToan = BigDecimal.ZERO;
    private TrangThaiHoaDon trangThai = TrangThaiHoaDon.CHO_THANH_TOAN;
    private String ghiChu = "";

    // Bổ sung tạm ứng, miễn giảm & hóa đơn điện tử VAT
    private BigDecimal soTienTamUng = BigDecimal.ZERO;
    private BigDecimal soTienGiamGia = BigDecimal.ZERO;
    private BigDecimal chietKhauPhanTram = BigDecimal.ZERO;
    private String lyDoGiamGia = "";
    private BigDecimal soTienHoanTra = BigDecimal.ZERO;
    private String soHoaDonDienTu = "";
    private String maTraCuuHddt = "";

    private LuotKham luotKham;
    private BenhNhan benhNhan;
    private CoSo coSo;
    private List<ChiTietHoaDon> chiTietHoaDons = new ArrayList<>();
    private List<PhieuThu> phieuThus = new ArrayList<>();
    private CongNoBenhNhan congNoBenhNhan;

    public HoaDon() {}

    /** Hàm `addChiTiet`: thêm chi tiet. */
    public void addChiTiet(ChiTietHoaDon item) {
        if (item != null) {
            item.setHoaDon(this);
            if (this.getId() != null) {
                item.setMaHoaDon(this.getId());
            }
            this.chiTietHoaDons.add(item);
        }
    }

    /** Hàm `recordPayment`: ghi nhận thanhtoan. */
    public void recordPayment(BigDecimal amount, PhuongThucThanhToan phuongThuc, int nguoiThu, String ghiChu) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Số tiền thanh toán phải > 0.");
        }
        this.daThanhToan = this.daThanhToan.add(amount);
        PhieuThu pt = new PhieuThu();
        pt.setMaHoaDon(getId() != null ? getId() : 0);
        pt.setSoTien(amount);
        pt.setPhuongThuc(phuongThuc);
        pt.setNguoiThu(nguoiThu);
        pt.setGhiChu(ghiChu);
        this.phieuThus.add(pt);

        if (this.daThanhToan.compareTo(this.soTienBenhNhanChiTra) >= 0) {
            this.trangThai = TrangThaiHoaDon.DA_THANH_TOAN;
        } else {
            this.trangThai = TrangThaiHoaDon.THANH_TOAN_MOT_PHAN;
        }
        registerDomainEvent(new ThanhToanDaGhiNhanSuKien(getId() != null ? getId() : 0, amount, phuongThuc.name()));
    }

    /** Hàm `getSoHoaDon`: lấy so hoa don. */
    public String getSoHoaDon() { return soHoaDon; }
    /** Hàm `setSoHoaDon`: gán so hoa don. */
    public void setSoHoaDon(String soHoaDon) { this.soHoaDon = soHoaDon; }
    /** Hàm `getMaLuotKham`: lấy mã luot kham. */
    public int getMaLuotKham() { return maLuotKham; }
    /** Hàm `setMaLuotKham`: gán mã luot kham. */
    public void setMaLuotKham(int maLuotKham) { this.maLuotKham = maLuotKham; }
    /** Hàm `getMaBenhNhan`: lấy mã benh nhãn. */
    public int getMaBenhNhan() { return maBenhNhan; }
    /** Hàm `setMaBenhNhan`: gán mã benh nhãn. */
    public void setMaBenhNhan(int maBenhNhan) { this.maBenhNhan = maBenhNhan; }
    /** Hàm `getMaCoSo`: lấy mã có so. */
    public int getMaCoSo() { return maCoSo; }
    /** Hàm `setMaCoSo`: gán mã có so. */
    public void setMaCoSo(int maCoSo) { this.maCoSo = maCoSo; }
    /** Hàm `getNgayLap`: lấy `Ngay` `Lap`. */
    public LocalDateTime getNgayLap() { return ngayLap; }
    /** Hàm `setNgayLap`: gán `Ngay` `Lap`. */
    public void setNgayLap(LocalDateTime ngayLap) { this.ngayLap = ngayLap; }
    /** Hàm `getTongTien`: lấy `Tong` tien. */
    public BigDecimal getTongTien() { return tongTien; }
    /** Hàm `setTongTien`: gán `Tong` tien. */
    public void setTongTien(BigDecimal tongTien) { this.tongTien = tongTien; }
    /** Hàm `getSoTienBaoHiemChiTra`: lấy so tien bao hiem chi `Tra`. */
    public BigDecimal getSoTienBaoHiemChiTra() { return soTienBaoHiemChiTra; }
    /** Hàm `setSoTienBaoHiemChiTra`: gán so tien bao hiem chi `Tra`. */
    public void setSoTienBaoHiemChiTra(BigDecimal soTienBaoHiemChiTra) { this.soTienBaoHiemChiTra = soTienBaoHiemChiTra; }
    /** Hàm `getSoTienBenhNhanChiTra`: lấy so tien benh nhãn chi `Tra`. */
    public BigDecimal getSoTienBenhNhanChiTra() { return soTienBenhNhanChiTra; }
    /** Hàm `setSoTienBenhNhanChiTra`: gán so tien benh nhãn chi `Tra`. */
    public void setSoTienBenhNhanChiTra(BigDecimal soTienBenhNhanChiTra) { this.soTienBenhNhanChiTra = soTienBenhNhanChiTra; }
    /** Hàm `getDaThanhToan`: lấy `Da` thanh toan. */
    public BigDecimal getDaThanhToan() { return daThanhToan; }
    /** Hàm `setDaThanhToan`: gán `Da` thanh toan. */
    public void setDaThanhToan(BigDecimal daThanhToan) { this.daThanhToan = daThanhToan; }
    /** Hàm `getTrangThai`: lấy trang thai. */
    public TrangThaiHoaDon getTrangThai() { return trangThai; }
    /** Hàm `setTrangThai`: gán trang thai. */
    public void setTrangThai(TrangThaiHoaDon trangThai) { this.trangThai = trangThai; }
    /** Hàm `getGhiChu`: lấy `Ghi` chu. */
    public String getGhiChu() { return ghiChu; }
    /** Hàm `setGhiChu`: gán `Ghi` chu. */
    public void setGhiChu(String ghiChu) { this.ghiChu = ghiChu; }

    /** Hàm `getSoTienTamUng`: lấy so tien `Tam` `Ung`. */
    public BigDecimal getSoTienTamUng() { return soTienTamUng; }
    /** Hàm `setSoTienTamUng`: gán so tien `Tam` `Ung`. */
    public void setSoTienTamUng(BigDecimal soTienTamUng) { this.soTienTamUng = soTienTamUng; }
    /** Hàm `getSoTienGiamGia`: lấy so tien `Giam` `Gia`. */
    public BigDecimal getSoTienGiamGia() { return soTienGiamGia; }
    /** Hàm `setSoTienGiamGia`: gán so tien `Giam` `Gia`. */
    public void setSoTienGiamGia(BigDecimal soTienGiamGia) { this.soTienGiamGia = soTienGiamGia; }
    /** Hàm `getChietKhauPhanTram`: lấy `Chiet` `Khau` phan `Tram`. */
    public BigDecimal getChietKhauPhanTram() { return chietKhauPhanTram; }
    /** Hàm `setChietKhauPhanTram`: gán `Chiet` `Khau` phan `Tram`. */
    public void setChietKhauPhanTram(BigDecimal chietKhauPhanTram) { this.chietKhauPhanTram = chietKhauPhanTram; }
    /** Hàm `getLyDoGiamGia`: lấy `Ly` `Do` `Giam` `Gia`. */
    public String getLyDoGiamGia() { return lyDoGiamGia; }
    /** Hàm `setLyDoGiamGia`: gán `Ly` `Do` `Giam` `Gia`. */
    public void setLyDoGiamGia(String lyDoGiamGia) { this.lyDoGiamGia = lyDoGiamGia; }
    /** Hàm `getSoTienHoanTra`: lấy so tien `Hoan` `Tra`. */
    public BigDecimal getSoTienHoanTra() { return soTienHoanTra; }
    /** Hàm `setSoTienHoanTra`: gán so tien `Hoan` `Tra`. */
    public void setSoTienHoanTra(BigDecimal soTienHoanTra) { this.soTienHoanTra = soTienHoanTra; }
    /** Hàm `getSoHoaDonDienTu`: lấy so hoa don `Dien` tu. */
    public String getSoHoaDonDienTu() { return soHoaDonDienTu; }
    /** Hàm `setSoHoaDonDienTu`: gán so hoa don `Dien` tu. */
    public void setSoHoaDonDienTu(String soHoaDonDienTu) { this.soHoaDonDienTu = soHoaDonDienTu; }
    /** Hàm `getMaTraCuuHddt`: lấy mã `Tra` `Cuu` `Hddt`. */
    public String getMaTraCuuHddt() { return maTraCuuHddt; }
    /** Hàm `setMaTraCuuHddt`: gán mã `Tra` `Cuu` `Hddt`. */
    public void setMaTraCuuHddt(String maTraCuuHddt) { this.maTraCuuHddt = maTraCuuHddt; }

    /** Hàm `getLuotKham`: lấy luot kham. */
    public LuotKham getLuotKham() { return luotKham; }
    /** Hàm `setLuotKham`: gán luot kham. */
    public void setLuotKham(LuotKham luotKham) { this.luotKham = luotKham; }
    /** Hàm `getBenhNhan`: lấy benh nhãn. */
    public BenhNhan getBenhNhan() { return benhNhan; }
    /** Hàm `setBenhNhan`: gán benh nhãn. */
    public void setBenhNhan(BenhNhan benhNhan) { this.benhNhan = benhNhan; }
    /** Hàm `getCoSo`: lấy có so. */
    public CoSo getCoSo() { return coSo; }
    /** Hàm `setCoSo`: gán có so. */
    public void setCoSo(CoSo coSo) { this.coSo = coSo; }
    /** Hàm `getChiTietHoaDons`: lấy chi tiet hoa `Dons`. */
    public List<ChiTietHoaDon> getChiTietHoaDons() { return chiTietHoaDons; }
    /** Hàm `setChiTietHoaDons`: gán chi tiet hoa `Dons`. */
    public void setChiTietHoaDons(List<ChiTietHoaDon> chiTietHoaDons) { this.chiTietHoaDons = chiTietHoaDons; }
    /** Hàm `getPhieuThus`: lấy phiếu `Thus`. */
    public List<PhieuThu> getPhieuThus() { return phieuThus; }
    /** Hàm `setPhieuThu`: gán phiếu thu. */
    public void setPhieuThu(List<PhieuThu> phieuThus) { this.phieuThus = phieuThus; }
    /** Hàm `getCongNoBenhNhan`: lấy cong no benh nhãn. */
    public CongNoBenhNhan getCongNoBenhNhan() { return congNoBenhNhan; }
    /** Hàm `setCongNoBenhNhan`: gán cong no benh nhãn. */
    public void setCongNoBenhNhan(CongNoBenhNhan congNoBenhNhan) { this.congNoBenhNhan = congNoBenhNhan; }
}
