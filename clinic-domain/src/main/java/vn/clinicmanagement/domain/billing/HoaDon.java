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
    private List<PhieuHoanTra> phieuHoanTras = new ArrayList<>();
    private CongNoBenhNhan congNoBenhNhan;

    public HoaDon() {}

    /** Tính lại nghĩa vụ thanh toán từ các khoản phí còn hiệu lực. */
    public void recalculateCharges() {
        if (trangThai == TrangThaiHoaDon.DA_HUY) {
            throw new IllegalStateException("Hóa đơn đã hủy không được cập nhật chi phí.");
        }
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal lineBhyt = BigDecimal.ZERO;
        BigDecimal lineDiscount = BigDecimal.ZERO;
        boolean hasLinePayerSplit = false;
        for (ChiTietHoaDon line : chiTietHoaDons) {
            if (line == null || line.isDeleted()) continue;
            BigDecimal lt = line.getThanhTien();
            total = total.add(lt);
            lineBhyt = lineBhyt.add(line.getSoTienBHYT());
            lineDiscount = lineDiscount.add(line.getSoTienMienGiam());
            if (line.getSoTienBHYT().signum() > 0 || line.getSoTienMienGiam().signum() > 0
                    || "BHYT".equalsIgnoreCase(line.getDoiTuongThanhToan())
                    || "BHYT_DICH_VU".equalsIgnoreCase(line.getDoiTuongThanhToan())
                    || "MIEN_PHI".equalsIgnoreCase(line.getDoiTuongThanhToan())) {
                hasLinePayerSplit = true;
            }
            line.setSoTienBenhNhan(lt.subtract(line.getSoTienBHYT()).subtract(line.getSoTienMienGiam()).max(BigDecimal.ZERO));
        }
        if (hasLinePayerSplit) {
            soTienBaoHiemChiTra = lineBhyt;
        }
        BigDecimal copay = total.subtract(soTienBaoHiemChiTra)
                .subtract(lineDiscount)
                .subtract(soTienTamUng)
                .subtract(soTienGiamGia)
                .max(BigDecimal.ZERO);
        if (copay.compareTo(daThanhToan) < 0) {
            throw new IllegalStateException("Chi phí mới thấp hơn số đã thu. Vui lòng liên hệ thu ngân để hoàn tiền.");
        }
        tongTien = total;
        soTienBenhNhanChiTra = copay;
        trangThai = copay.compareTo(daThanhToan) <= 0 ? TrangThaiHoaDon.DA_THANH_TOAN
                : daThanhToan.signum() == 0 ? TrangThaiHoaDon.CHO_THANH_TOAN
                : TrangThaiHoaDon.THANH_TOAN_MOT_PHAN;
    }

    /**
     * Tính lại hóa đơn sau khi chuyển đối tượng thanh toán từng dòng.
     * Cho phép nghĩa vụ bệnh nhân mới thấp hơn số tiền đã thu ròng (`daThanhToan`),
     * phần dư được theo dõi qua `getPendingRefundAmount()` để Thu ngân chi hoàn tiền mặt qua `PhieuHoanTra`.
     */
    public void recalculateAfterPayerChange() {
        if (trangThai == TrangThaiHoaDon.DA_HUY) {
            throw new IllegalStateException("Hóa đơn đã hủy không được cập nhật chi phí.");
        }
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal lineBhyt = BigDecimal.ZERO;
        BigDecimal lineDiscount = BigDecimal.ZERO;
        for (ChiTietHoaDon line : chiTietHoaDons) {
            if (line == null || line.isDeleted()) continue;
            BigDecimal lt = line.getThanhTien();
            total = total.add(lt);
            lineBhyt = lineBhyt.add(line.getSoTienBHYT());
            lineDiscount = lineDiscount.add(line.getSoTienMienGiam());
            line.setSoTienBenhNhan(lt.subtract(line.getSoTienBHYT()).subtract(line.getSoTienMienGiam()).max(BigDecimal.ZERO));
        }
        tongTien = total;
        soTienBaoHiemChiTra = lineBhyt;
        BigDecimal copay = total.subtract(soTienBaoHiemChiTra)
                .subtract(lineDiscount)
                .subtract(soTienTamUng)
                .subtract(soTienGiamGia)
                .max(BigDecimal.ZERO);
        soTienBenhNhanChiTra = copay;
        trangThai = copay.compareTo(daThanhToan) <= 0 ? TrangThaiHoaDon.DA_THANH_TOAN
                : daThanhToan.signum() == 0 ? TrangThaiHoaDon.CHO_THANH_TOAN
                : TrangThaiHoaDon.THANH_TOAN_MOT_PHAN;
    }

    public BigDecimal getTotalServiceDiscountAmount() {
        if (chiTietHoaDons == null) return BigDecimal.ZERO;
        return chiTietHoaDons.stream()
                .filter(line -> line != null && !line.isDeleted())
                .map(ChiTietHoaDon::getSoTienMienGiam)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Số tiền đang thừa cần Thu ngân chi hoàn trả bệnh nhân = max(0, daThanhToan - soTienBenhNhanChiTra). */
    public BigDecimal getPendingRefundAmount() {
        BigDecimal paid = daThanhToan != null ? daThanhToan : BigDecimal.ZERO;
        BigDecimal copay = soTienBenhNhanChiTra != null ? soTienBenhNhanChiTra : BigDecimal.ZERO;
        return paid.subtract(copay).max(BigDecimal.ZERO);
    }

    /** Số tiền bệnh nhân còn nợ = max(0, soTienBenhNhanChiTra - daThanhToan). */
    public BigDecimal getRemainingAmount() {
        BigDecimal paid = daThanhToan != null ? daThanhToan : BigDecimal.ZERO;
        BigDecimal copay = soTienBenhNhanChiTra != null ? soTienBenhNhanChiTra : BigDecimal.ZERO;
        return copay.subtract(paid).max(BigDecimal.ZERO);
    }

    public BigDecimal patientAmountFor(BigDecimal total) {
        return total.subtract(soTienBaoHiemChiTra)
                .subtract(getTotalServiceDiscountAmount())
                .subtract(soTienTamUng)
                .subtract(soTienGiamGia).max(BigDecimal.ZERO);
    }

    /** Ghi nhận chi hoàn tiền mặt/chuyển khoản cho bệnh nhân thông qua chứng từ `PhieuHoanTra` độc lập. */
    public PhieuHoanTra recordRefund(BigDecimal amount, PhuongThucThanhToan phuongThuc, int nguoiHoan, String lyDo) {
        if (trangThai == TrangThaiHoaDon.DA_HUY) {
            throw new IllegalStateException("Hóa đơn đã bị hủy, không thể hoàn tiền.");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Số tiền hoàn trả phải lớn hơn 0.");
        }
        BigDecimal pending = getPendingRefundAmount();
        if (amount.compareTo(pending) > 0) {
            throw new IllegalArgumentException(
                    String.format("Số tiền hoàn trả (%s) vượt quá số tiền cần hoàn (%s).",
                            amount.toPlainString(), pending.toPlainString()));
        }
        this.daThanhToan = this.daThanhToan.subtract(amount);
        this.soTienHoanTra = (this.soTienHoanTra != null ? this.soTienHoanTra : BigDecimal.ZERO).add(amount);

        PhieuHoanTra pht = new PhieuHoanTra();
        pht.setMaHoaDon(getId() != null ? getId() : 0);
        pht.setSoTien(amount);
        pht.setPhuongThuc(phuongThuc != null ? phuongThuc : PhuongThucThanhToan.TIEN_MAT);
        pht.setNguoiHoan(nguoiHoan);
        pht.setLyDo(lyDo != null ? lyDo : "");
        this.phieuHoanTras.add(pht);

        if (this.daThanhToan.compareTo(this.soTienBenhNhanChiTra) >= 0) {
            this.trangThai = TrangThaiHoaDon.DA_THANH_TOAN;
        } else if (this.daThanhToan.signum() > 0) {
            this.trangThai = TrangThaiHoaDon.THANH_TOAN_MOT_PHAN;
        } else {
            this.trangThai = TrangThaiHoaDon.CHO_THANH_TOAN;
        }
        return pht;
    }

    public void addChiTiet(ChiTietHoaDon item) {
        if (item != null) {
            item.setHoaDon(this);
            if (this.getId() != null) {
                item.setMaHoaDon(this.getId());
            }
            this.chiTietHoaDons.add(item);
        }
    }

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
    public List<PhieuHoanTra> getPhieuHoanTras() { return phieuHoanTras; }
    public void setPhieuHoanTras(List<PhieuHoanTra> phieuHoanTras) {
        this.phieuHoanTras = phieuHoanTras != null ? phieuHoanTras : new ArrayList<>();
    }
    public CongNoBenhNhan getCongNoBenhNhan() { return congNoBenhNhan; }
    public void setCongNoBenhNhan(CongNoBenhNhan congNoBenhNhan) { this.congNoBenhNhan = congNoBenhNhan; }
}
