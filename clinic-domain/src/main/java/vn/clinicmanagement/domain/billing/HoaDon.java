package vn.clinicmanagement.domain.billing;

import vn.clinicmanagement.domain.common.BaseEntity;
import vn.clinicmanagement.domain.common.IAggregateRoot;
import vn.clinicmanagement.domain.common.enums.PhuongThucThanhToan;
import vn.clinicmanagement.domain.common.enums.TrangThaiHoaDon;
import vn.clinicmanagement.domain.common.events.HoaDonCreatedEvent;
import vn.clinicmanagement.domain.common.events.PaymentRecordedEvent;
import vn.clinicmanagement.domain.examination.LuotKham;
import vn.clinicmanagement.domain.facility.CoSo;
import vn.clinicmanagement.domain.patient.BenhNhan;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class HoaDon extends BaseEntity<Integer> implements IAggregateRoot {
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
        registerDomainEvent(new PaymentRecordedEvent(getId() != null ? getId() : 0, amount, phuongThuc.name()));
    }

    public String getSoHoaDon() { return soHoaDon; }
    public void setSoHoaDon(String soHoaDon) { this.soHoaDon = soHoaDon; }
    public int getMaLuotKham() { return maLuotKham; }
    public void setMaLuotKham(int maLuotKham) { this.maLuotKham = maLuotKham; }
    public int getMaBenhNhan() { return maBenhNhan; }
    public void setMaBenhNhan(int maBenhNhan) { this.maBenhNhan = maBenhNhan; }
    public int getMaCoSo() { return maCoSo; }
    public void setMaCoSo(int maCoSo) { this.maCoSo = maCoSo; }
    public LocalDateTime getNgayLap() { return ngayLap; }
    public void setNgayLap(LocalDateTime ngayLap) { this.ngayLap = ngayLap; }
    public BigDecimal getTongTien() { return tongTien; }
    public void setTongTien(BigDecimal tongTien) { this.tongTien = tongTien; }
    public BigDecimal getSoTienBaoHiemChiTra() { return soTienBaoHiemChiTra; }
    public void setSoTienBaoHiemChiTra(BigDecimal soTienBaoHiemChiTra) { this.soTienBaoHiemChiTra = soTienBaoHiemChiTra; }
    public BigDecimal getSoTienBenhNhanChiTra() { return soTienBenhNhanChiTra; }
    public void setSoTienBenhNhanChiTra(BigDecimal soTienBenhNhanChiTra) { this.soTienBenhNhanChiTra = soTienBenhNhanChiTra; }
    public BigDecimal getDaThanhToan() { return daThanhToan; }
    public void setDaThanhToan(BigDecimal daThanhToan) { this.daThanhToan = daThanhToan; }
    public TrangThaiHoaDon getTrangThai() { return trangThai; }
    public void setTrangThai(TrangThaiHoaDon trangThai) { this.trangThai = trangThai; }
    public String getGhiChu() { return ghiChu; }
    public void setGhiChu(String ghiChu) { this.ghiChu = ghiChu; }

    public BigDecimal getSoTienTamUng() { return soTienTamUng; }
    public void setSoTienTamUng(BigDecimal soTienTamUng) { this.soTienTamUng = soTienTamUng; }
    public BigDecimal getSoTienGiamGia() { return soTienGiamGia; }
    public void setSoTienGiamGia(BigDecimal soTienGiamGia) { this.soTienGiamGia = soTienGiamGia; }
    public BigDecimal getChietKhauPhanTram() { return chietKhauPhanTram; }
    public void setChietKhauPhanTram(BigDecimal chietKhauPhanTram) { this.chietKhauPhanTram = chietKhauPhanTram; }
    public String getLyDoGiamGia() { return lyDoGiamGia; }
    public void setLyDoGiamGia(String lyDoGiamGia) { this.lyDoGiamGia = lyDoGiamGia; }
    public BigDecimal getSoTienHoanTra() { return soTienHoanTra; }
    public void setSoTienHoanTra(BigDecimal soTienHoanTra) { this.soTienHoanTra = soTienHoanTra; }
    public String getSoHoaDonDienTu() { return soHoaDonDienTu; }
    public void setSoHoaDonDienTu(String soHoaDonDienTu) { this.soHoaDonDienTu = soHoaDonDienTu; }
    public String getMaTraCuuHddt() { return maTraCuuHddt; }
    public void setMaTraCuuHddt(String maTraCuuHddt) { this.maTraCuuHddt = maTraCuuHddt; }

    public LuotKham getLuotKham() { return luotKham; }
    public void setLuotKham(LuotKham luotKham) { this.luotKham = luotKham; }
    public BenhNhan getBenhNhan() { return benhNhan; }
    public void setBenhNhan(BenhNhan benhNhan) { this.benhNhan = benhNhan; }
    public CoSo getCoSo() { return coSo; }
    public void setCoSo(CoSo coSo) { this.coSo = coSo; }
    public List<ChiTietHoaDon> getChiTietHoaDons() { return chiTietHoaDons; }
    public void setChiTietHoaDons(List<ChiTietHoaDon> chiTietHoaDons) { this.chiTietHoaDons = chiTietHoaDons; }
    public List<PhieuThu> getPhieuThus() { return phieuThus; }
    public void setPhieuThu(List<PhieuThu> phieuThus) { this.phieuThus = phieuThus; }
    public CongNoBenhNhan getCongNoBenhNhan() { return congNoBenhNhan; }
    public void setCongNoBenhNhan(CongNoBenhNhan congNoBenhNhan) { this.congNoBenhNhan = congNoBenhNhan; }
}
