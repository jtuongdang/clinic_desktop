package vn.clinicmanagement.domain.billing;
import vn.clinicmanagement.domain.common.CoSoThucThe;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

public class ChiTietHoaDon extends CoSoThucThe<Integer> {
    private int maHoaDon;
    private String loaiDoiTuong = "";
    private int maDoiTuong;
    private String moTa = "";
    private BigDecimal soLuong = BigDecimal.ONE;
    private BigDecimal donGia = BigDecimal.ZERO;
    private String doiTuongThanhToan = "THU_PHI";
    private BigDecimal giaBHYT = BigDecimal.ZERO;
    private int tyLeHuong = 0;
    private BigDecimal soTienBHYT = BigDecimal.ZERO;
    private BigDecimal soTienMienGiam = BigDecimal.ZERO;
    private BigDecimal soTienBenhNhan = BigDecimal.ZERO;
    private HoaDon hoaDon;

    public ChiTietHoaDon() {}
    public int getMaHoaDon() { return maHoaDon; }
    public void setMaHoaDon(int maHoaDon) { this.maHoaDon = maHoaDon; }
    public String getLoaiDoiTuong() { return loaiDoiTuong; }
    public void setLoaiDoiTuong(String loaiDoiTuong) { this.loaiDoiTuong = loaiDoiTuong; }
    public int getMaDoiTuong() { return maDoiTuong; }
    public void setMaDoiTuong(int maDoiTuong) { this.maDoiTuong = maDoiTuong; }
    public String getMoTa() { return moTa; }
    public void setMoTa(String moTa) { this.moTa = moTa; }
    public BigDecimal getSoLuong() { return soLuong; }
    public void setSoLuong(BigDecimal soLuong) { this.soLuong = soLuong; }
    public BigDecimal getDonGia() { return donGia; }
    public void setDonGia(BigDecimal donGia) { this.donGia = donGia; }

    public String getDoiTuongThanhToan() { return doiTuongThanhToan; }
    public void setDoiTuongThanhToan(String doiTuongThanhToan) {
        this.doiTuongThanhToan = (doiTuongThanhToan == null || doiTuongThanhToan.isBlank())
                ? "THU_PHI"
                : doiTuongThanhToan.trim().toUpperCase(Locale.ROOT);
    }

    public BigDecimal getGiaBHYT() { return giaBHYT != null ? giaBHYT : BigDecimal.ZERO; }
    public void setGiaBHYT(BigDecimal giaBHYT) { this.giaBHYT = giaBHYT != null ? giaBHYT : BigDecimal.ZERO; }

    public int getTyLeHuong() { return tyLeHuong; }
    public void setTyLeHuong(int tyLeHuong) { this.tyLeHuong = Math.max(0, Math.min(100, tyLeHuong)); }

    public BigDecimal getSoTienBHYT() { return soTienBHYT != null ? soTienBHYT : BigDecimal.ZERO; }
    public void setSoTienBHYT(BigDecimal soTienBHYT) { this.soTienBHYT = soTienBHYT != null ? soTienBHYT : BigDecimal.ZERO; }

    public BigDecimal getSoTienMienGiam() { return soTienMienGiam != null ? soTienMienGiam : BigDecimal.ZERO; }
    public void setSoTienMienGiam(BigDecimal soTienMienGiam) { this.soTienMienGiam = soTienMienGiam != null ? soTienMienGiam : BigDecimal.ZERO; }

    public BigDecimal getSoTienBenhNhan() {
        BigDecimal lineTotal = getThanhTien();
        if ("MIEN_PHI".equalsIgnoreCase(doiTuongThanhToan)) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal computed = lineTotal.subtract(getSoTienBHYT()).subtract(getSoTienMienGiam()).max(BigDecimal.ZERO);
        if (soTienBenhNhan == null || (soTienBenhNhan.signum() == 0 && computed.signum() > 0)) {
            return computed;
        }
        return soTienBenhNhan;
    }
    public void setSoTienBenhNhan(BigDecimal soTienBenhNhan) {
        this.soTienBenhNhan = soTienBenhNhan != null ? soTienBenhNhan : BigDecimal.ZERO;
    }

    public BigDecimal getThanhTien() {
        BigDecimal qty = soLuong != null ? soLuong : BigDecimal.ZERO;
        BigDecimal price = donGia != null ? donGia : BigDecimal.ZERO;
        return qty.multiply(price).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Áp quy tắc chuyển đối tượng thanh toán cho dòng dịch vụ/thuốc:
     * - BHYT: áp đơn giá = giaBHYT, quỹ BHYT trả theo tyLeHuong, bệnh nhân đồng chi trả phần còn lại.
     * - BHYT_DICH_VU: giữ đơn giá dịch vụ/thu phí, quỹ BHYT trả trên nền giaBHYT * tyLeHuong, bệnh nhân trả đồng chi trả + chênh lệch.
     * - THU_PHI: áp đơn giá thu phí gốc, BHYT = 0, bệnh nhân trả 100%.
     * - MIEN_PHI: áp miễn giảm 100% thành tiền dòng, bệnh nhân trả 0.
     */
    public void applyPayerRule(String targetPayer, BigDecimal standardServicePrice, BigDecimal catalogBhytPrice, int coveragePercent) {
        String normalized = (targetPayer == null || targetPayer.isBlank())
                ? "THU_PHI"
                : targetPayer.trim().toUpperCase(Locale.ROOT);
        BigDecimal qty = soLuong != null ? soLuong : BigDecimal.ONE;
        BigDecimal servicePrice = (standardServicePrice != null && standardServicePrice.signum() > 0)
                ? standardServicePrice
                : (donGia != null ? donGia : BigDecimal.ZERO);
        BigDecimal bhytPrice = (catalogBhytPrice != null && catalogBhytPrice.signum() > 0)
                ? catalogBhytPrice
                : BigDecimal.ZERO;
        int rate = Math.max(0, Math.min(100, coveragePercent));

        switch (normalized) {
            case "BHYT" -> {
                this.doiTuongThanhToan = "BHYT";
                this.donGia = bhytPrice;
                this.giaBHYT = bhytPrice;
                this.tyLeHuong = rate;
                BigDecimal total = qty.multiply(this.donGia).setScale(2, RoundingMode.HALF_UP);
                this.soTienBHYT = total.multiply(BigDecimal.valueOf(rate))
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                this.soTienMienGiam = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
                this.soTienBenhNhan = total.subtract(this.soTienBHYT).max(BigDecimal.ZERO);
            }
            case "BHYT_DICH_VU" -> {
                this.doiTuongThanhToan = "BHYT_DICH_VU";
                this.donGia = servicePrice.compareTo(bhytPrice) >= 0 ? servicePrice : bhytPrice;
                this.giaBHYT = bhytPrice;
                this.tyLeHuong = rate;
                BigDecimal total = qty.multiply(this.donGia).setScale(2, RoundingMode.HALF_UP);
                BigDecimal bhytBase = qty.multiply(bhytPrice).setScale(2, RoundingMode.HALF_UP);
                this.soTienBHYT = bhytBase.multiply(BigDecimal.valueOf(rate))
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                        .min(total);
                this.soTienMienGiam = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
                this.soTienBenhNhan = total.subtract(this.soTienBHYT).max(BigDecimal.ZERO);
            }
            case "MIEN_PHI" -> {
                this.doiTuongThanhToan = "MIEN_PHI";
                this.donGia = servicePrice;
                this.giaBHYT = bhytPrice;
                this.tyLeHuong = 0;
                BigDecimal total = qty.multiply(this.donGia).setScale(2, RoundingMode.HALF_UP);
                this.soTienBHYT = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
                this.soTienMienGiam = total;
                this.soTienBenhNhan = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            }
            default -> {
                this.doiTuongThanhToan = "THU_PHI";
                this.donGia = servicePrice;
                this.giaBHYT = bhytPrice;
                this.tyLeHuong = 0;
                BigDecimal total = qty.multiply(this.donGia).setScale(2, RoundingMode.HALF_UP);
                this.soTienBHYT = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
                this.soTienMienGiam = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
                this.soTienBenhNhan = total;
            }
        }
    }

    public HoaDon getHoaDon() { return hoaDon; }
    public void setHoaDon(HoaDon hoaDon) { this.hoaDon = hoaDon; }
}
