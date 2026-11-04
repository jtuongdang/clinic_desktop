package vn.clinicmanagement.domain.appointment;
import vn.clinicmanagement.domain.common.BaseEntity;
import vn.clinicmanagement.domain.common.IAggregateRoot;
import vn.clinicmanagement.domain.common.enums.TrangThaiLichHen;
import vn.clinicmanagement.domain.common.events.LichHenBookedEvent;
import vn.clinicmanagement.domain.facility.CoSo;
import vn.clinicmanagement.domain.facility.Khoa;
import vn.clinicmanagement.domain.identity.NhanVien;
import vn.clinicmanagement.domain.patient.BenhNhan;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class LichHen extends BaseEntity<Integer> implements IAggregateRoot {
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
        setCreatedBy(createdBy != null ? createdBy : 0);
        this.trangThai = TrangThaiLichHen.CHO_XAC_NHAN;
        registerDomainEvent(new LichHenBookedEvent(getId() != null ? getId() : 0, maBenhNhan, maBacSi, thoiGianBatDau));
    }

    public void xacNhanThuTienMat(String soPhieuThu, int nguoiThu) {
        if (this.trangThai == TrangThaiLichHen.DA_HUY) throw new IllegalStateException("Lịch hẹn đã bị hủy.");
        this.daThanhToanGiuCho = true;
        this.soPhieuThu = soPhieuThu;
        this.ngayThanhToan = LocalDateTime.now();
        this.nguoiThuTien = nguoiThu;
        this.trangThai = TrangThaiLichHen.DA_XAC_NHAN;
    }

    public void huyLichHen(String lyDoHuy) {
        if (this.trangThai == TrangThaiLichHen.DA_DEN || this.trangThai == TrangThaiLichHen.HOAN_THANH) {
            throw new IllegalStateException("Không thể hủy lịch hẹn đã tiếp nhận hoặc hoàn thành.");
        }
        this.trangThai = TrangThaiLichHen.DA_HUY;
        this.ghiChuHuy = lyDoHuy;
    }

    public int getMaBenhNhan() { return maBenhNhan; }
    public void setMaBenhNhan(int maBenhNhan) { this.maBenhNhan = maBenhNhan; }
    public int getMaBacSi() { return maBacSi; }
    public void setMaBacSi(int maBacSi) { this.maBacSi = maBacSi; }
    public int getMaCoSo() { return maCoSo; }
    public void setMaCoSo(int maCoSo) { this.maCoSo = maCoSo; }
    public Integer getMaKhoa() { return maKhoa; }
    public void setMaKhoa(Integer maKhoa) { this.maKhoa = maKhoa; }
    public LocalDateTime getThoiGianBatDau() { return thoiGianBatDau; }
    public void setThoiGianBatDau(LocalDateTime thoiGianBatDau) { this.thoiGianBatDau = thoiGianBatDau; }
    public LocalDateTime getThoiGianKetThuc() { return thoiGianKetThuc; }
    public void setThoiGianKetThuc(LocalDateTime thoiGianKetThuc) { this.thoiGianKetThuc = thoiGianKetThuc; }
    public TrangThaiLichHen getTrangThai() { return trangThai; }
    public void setTrangThai(TrangThaiLichHen trangThai) { this.trangThai = trangThai; }
    public String getLyDo() { return lyDo; }
    public void setLyDo(String lyDo) { this.lyDo = lyDo; }
    public String getNguonDat() { return nguonDat; }
    public void setNguonDat(String nguonDat) { this.nguonDat = nguonDat; }
    public BigDecimal getChiPhiGiuCho() { return chiPhiGiuCho; }
    public void setChiPhiGiuCho(BigDecimal chiPhiGiuCho) { this.chiPhiGiuCho = chiPhiGiuCho; }
    public boolean isDaThanhToanGiuCho() { return daThanhToanGiuCho; }
    public void setDaThanhToanGiuCho(boolean daThanhToanGiuCho) { this.daThanhToanGiuCho = daThanhToanGiuCho; }
    public String getSoPhieuThu() { return soPhieuThu; }
    public void setSoPhieuThu(String soPhieuThu) { this.soPhieuThu = soPhieuThu; }
    public LocalDateTime getNgayThanhToan() { return ngayThanhToan; }
    public void setNgayThanhToan(LocalDateTime ngayThanhToan) { this.ngayThanhToan = ngayThanhToan; }
    public Integer getNguoiThuTien() { return nguoiThuTien; }
    public void setNguoiThuTien(Integer nguoiThuTien) { this.nguoiThuTien = nguoiThuTien; }
    public String getGhiChuHuy() { return ghiChuHuy; }
    public void setGhiChuHuy(String ghiChuHuy) { this.ghiChuHuy = ghiChuHuy; }
    public BenhNhan getBenhNhan() { return benhNhan; }
    public void setBenhNhan(BenhNhan benhNhan) { this.benhNhan = benhNhan; }
    public NhanVien getBacSi() { return bacSi; }
    public void setBacSi(NhanVien bacSi) { this.bacSi = bacSi; }
    public CoSo getCoSo() { return coSo; }
    public void setCoSo(CoSo coSo) { this.coSo = coSo; }
    public Khoa getKhoa() { return khoa; }
    public void setKhoa(Khoa khoa) { this.khoa = khoa; }
}
