package vn.clinicmanagement.domain.examination;

import vn.clinicmanagement.domain.common.BaseEntity;
import vn.clinicmanagement.domain.common.IAggregateRoot;
import vn.clinicmanagement.domain.common.enums.DoiTuongKham;
import vn.clinicmanagement.domain.common.enums.HinhThucDieuTri;
import vn.clinicmanagement.domain.common.enums.HuongXuTri;
import vn.clinicmanagement.domain.common.enums.KetQuaDieuTri;
import vn.clinicmanagement.domain.common.enums.TrangThaiLuotKham;
import vn.clinicmanagement.domain.common.events.EmrLockedEvent;
import vn.clinicmanagement.domain.common.events.LuotKhamCompletedEvent;
import vn.clinicmanagement.domain.common.events.LuotKhamStartedEvent;
import vn.clinicmanagement.domain.common.exceptions.EmrLockedException;
import vn.clinicmanagement.domain.appointment.LichHen;
import vn.clinicmanagement.domain.billing.HoaDon;
import vn.clinicmanagement.domain.facility.CoSo;
import vn.clinicmanagement.domain.facility.Khoa;
import vn.clinicmanagement.domain.facility.PhongKham;
import vn.clinicmanagement.domain.identity.NhanVien;
import vn.clinicmanagement.domain.patient.BenhNhan;
import vn.clinicmanagement.domain.prescription.DonThuoc;
import vn.clinicmanagement.domain.service.ChiDinhDichVu;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class LuotKham extends BaseEntity<Integer> implements IAggregateRoot {
    private int maBenhNhan;
    private Integer maBacSi;
    private Integer maLichHen;
    private int maCoSo;
    private int maKhoa;
    private HinhThucDieuTri hinhThucDieuTri = HinhThucDieuTri.NGOAI_TRU;
    private TrangThaiLuotKham trangThai = TrangThaiLuotKham.CHO_KHAM;
    private Integer soThuTu;
    private LocalDate ngayKham = LocalDate.now();
    private LocalDateTime thoiGianBatDauKham;
    private LocalDateTime thoiGianKetThucKham;
    private String lyDoKham = "";
    private Integer maPhongKham;
    private boolean emrLocked = false;
    private String emrSignature;
    private LocalDateTime emrLockedAt;
    private Integer emrLockedBy;

    // Tiếp đón & Hàng đợi
    private String soThuTuDisplay = "";
    private LocalDateTime thoiGianTiepNhan = LocalDateTime.now();
    private DoiTuongKham doiTuongKham = DoiTuongKham.THU_PHI;
    private Integer maDichVuKham;             // Dịch vụ khám ban đầu được chọn
    private String loaiDangKyKham = "Khám bệnh"; // Khám bệnh / Tái khám / Cấp cứu
    private boolean dnVnTien = false;         // Cờ ưu tiên thanh toán / ĐN VN tiền

    // Bệnh án ngoại trú (Mẫu BA 01/BV)
    private String quaTrinhBenhLy = "";
    private String tienSuBanThan = "";
    private String tienSuGiaDinh = "";
    private String khamToanThan = "";
    private String khamBoPhan = "";
    private String tomTatBenhAn = "";
    private String huongDieuTri = "";
    private String loiDanBacSi = "";

    // Xử trí kết thúc khám
    private KetQuaDieuTri ketQuaDieuTri;
    private HuongXuTri huongXuTri = HuongXuTri.CHO_VE_TOA;
    private LocalDate ngayHenTaiKham;
    private Integer soNgayNghiHuongBHXH;
    private String lyDoChuyenTuyen = "";

    private BenhNhan benhNhan;
    private NhanVien bacSi;
    private LichHen lichHen;
    private CoSo coSo;
    private Khoa khoa;
    private PhongKham phongKham;

    private List<ChanDoan> chanDoans = new ArrayList<>();
    private List<SinhHieu> sinhHieus = new ArrayList<>();
    private List<ChiDinhDichVu> chiDinhDichVus = new ArrayList<>();
    private List<DonThuoc> donThuocs = new ArrayList<>();
    private List<HoaDon> hoaDons = new ArrayList<>();

    public LuotKham() {}

    public void startExam() {
        if (this.trangThai != TrangThaiLuotKham.CHO_KHAM) {
            throw new IllegalStateException("Lượt khám không ở trạng thái chờ khám.");
        }
        this.trangThai = TrangThaiLuotKham.DANG_KHAM;
        this.thoiGianBatDauKham = LocalDateTime.now();
        registerDomainEvent(new LuotKhamStartedEvent(getId() != null ? getId() : 0, maBenhNhan, maBacSi != null ? maBacSi : 0));
    }

    public void completeExam() {
        if (emrLocked) {
            throw new EmrLockedException(getId() != null ? getId() : 0);
        }
        if (this.trangThai != TrangThaiLuotKham.DANG_KHAM) {
            throw new IllegalStateException("Lượt khám không ở trạng thái đang khám.");
        }
        this.trangThai = TrangThaiLuotKham.HOAN_THANH;
        this.thoiGianKetThucKham = LocalDateTime.now();
        registerDomainEvent(new LuotKhamCompletedEvent(getId() != null ? getId() : 0, maBenhNhan, maBacSi != null ? maBacSi : 0));
    }

    public void lockEmr(String signature, int doctorId) {
        if (emrLocked) {
            throw new EmrLockedException(getId() != null ? getId() : 0);
        }
        if (signature == null || signature.isBlank()) {
            throw new IllegalArgumentException("Chữ ký EMR không được để trống.");
        }
        if (doctorId <= 0) {
            throw new IllegalArgumentException("Mã bác sĩ phải là số dương.");
        }
        this.emrLocked = true;
        this.emrSignature = signature;
        this.emrLockedAt = LocalDateTime.now();
        this.emrLockedBy = doctorId;
        registerDomainEvent(new EmrLockedEvent(getId() != null ? getId() : 0, doctorId, signature));
    }

    public void reopenEmr(int approverId, String reason) {
        if (!emrLocked) {
            throw new IllegalStateException("Hồ sơ bệnh án chưa bị khóa.");
        }
        if (this.trangThai != TrangThaiLuotKham.HOAN_THANH) {
            throw new IllegalStateException("Chỉ có thể mở khóa bệnh án đã hoàn thành khám.");
        }
        if (approverId <= 0) {
            throw new IllegalArgumentException("Mã người phê duyệt phải là số dương.");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Lý do mở khóa bệnh án không được để trống.");
        }
        this.emrLocked = false;
        this.emrSignature = null;
        this.emrLockedAt = null;
        this.emrLockedBy = null;
        this.trangThai = TrangThaiLuotKham.DANG_KHAM;
        registerDomainEvent(new vn.clinicmanagement.domain.common.events.EmrReopenedEvent(
                getId() != null ? getId() : 0, approverId, reason));
    }

    public int getMaBenhNhan() { return maBenhNhan; }
    public void setMaBenhNhan(int maBenhNhan) { this.maBenhNhan = maBenhNhan; }
    public Integer getMaBacSi() { return maBacSi; }
    public void setMaBacSi(Integer maBacSi) { this.maBacSi = maBacSi; }
    public Integer getMaLichHen() { return maLichHen; }
    public void setMaLichHen(Integer maLichHen) { this.maLichHen = maLichHen; }
    public int getMaCoSo() { return maCoSo; }
    public void setMaCoSo(int maCoSo) { this.maCoSo = maCoSo; }
    public int getMaKhoa() { return maKhoa; }
    public void setMaKhoa(int maKhoa) { this.maKhoa = maKhoa; }
    public HinhThucDieuTri getHinhThucDieuTri() { return hinhThucDieuTri; }
    public void setHinhThucDieuTri(HinhThucDieuTri hinhThucDieuTri) { this.hinhThucDieuTri = hinhThucDieuTri; }
    public TrangThaiLuotKham getTrangThai() { return trangThai; }
    public void setTrangThai(TrangThaiLuotKham trangThai) { this.trangThai = trangThai; }
    public Integer getSoThuTu() { return soThuTu; }
    public void setSoThuTu(Integer soThuTu) { this.soThuTu = soThuTu; }
    public LocalDate getNgayKham() { return ngayKham; }
    public void setNgayKham(LocalDate ngayKham) { this.ngayKham = ngayKham; }
    public LocalDateTime getThoiGianBatDauKham() { return thoiGianBatDauKham; }
    public void setThoiGianBatDauKham(LocalDateTime thoiGianBatDauKham) { this.thoiGianBatDauKham = thoiGianBatDauKham; }
    public LocalDateTime getThoiGianKetThucKham() { return thoiGianKetThucKham; }
    public void setThoiGianKetThucKham(LocalDateTime thoiGianKetThucKham) { this.thoiGianKetThucKham = thoiGianKetThucKham; }
    public String getLyDoKham() { return lyDoKham; }
    public void setLyDoKham(String lyDoKham) { this.lyDoKham = lyDoKham; }
    public Integer getMaPhongKham() { return maPhongKham; }
    public void setMaPhongKham(Integer maPhongKham) { this.maPhongKham = maPhongKham; }
    public boolean isEmrLocked() { return emrLocked; }
    public void setEmrLocked(boolean emrLocked) { this.emrLocked = emrLocked; }
    public String getEmrSignature() { return emrSignature; }
    public LocalDateTime getEmrLockedAt() { return emrLockedAt; }
    public Integer getEmrLockedBy() { return emrLockedBy; }

    public String getSoThuTuDisplay() { return soThuTuDisplay; }
    public void setSoThuTuDisplay(String soThuTuDisplay) { this.soThuTuDisplay = soThuTuDisplay; }
    public LocalDateTime getThoiGianTiepNhan() { return thoiGianTiepNhan; }
    public void setThoiGianTiepNhan(LocalDateTime thoiGianTiepNhan) { this.thoiGianTiepNhan = thoiGianTiepNhan; }
    public DoiTuongKham getDoiTuongKham() { return doiTuongKham; }
    public void setDoiTuongKham(DoiTuongKham doiTuongKham) { this.doiTuongKham = doiTuongKham; }

    public Integer getMaDichVuKham() { return maDichVuKham; }
    public void setMaDichVuKham(Integer maDichVuKham) { this.maDichVuKham = maDichVuKham; }
    public String getLoaiDangKyKham() { return loaiDangKyKham; }
    public void setLoaiDangKyKham(String loaiDangKyKham) { this.loaiDangKyKham = loaiDangKyKham; }
    public boolean isDnVnTien() { return dnVnTien; }
    public void setDnVnTien(boolean dnVnTien) { this.dnVnTien = dnVnTien; }

    public String getQuaTrinhBenhLy() { return quaTrinhBenhLy; }
    public void setQuaTrinhBenhLy(String quaTrinhBenhLy) { this.quaTrinhBenhLy = quaTrinhBenhLy; }
    public String getTienSuBanThan() { return tienSuBanThan; }
    public void setTienSuBanThan(String tienSuBanThan) { this.tienSuBanThan = tienSuBanThan; }
    public String getTienSuGiaDinh() { return tienSuGiaDinh; }
    public void setTienSuGiaDinh(String tienSuGiaDinh) { this.tienSuGiaDinh = tienSuGiaDinh; }
    public String getKhamToanThan() { return khamToanThan; }
    public void setKhamToanThan(String khamToanThan) { this.khamToanThan = khamToanThan; }
    public String getKhamBoPhan() { return khamBoPhan; }
    public void setKhamBoPhan(String khamBoPhan) { this.khamBoPhan = khamBoPhan; }
    public String getTomTatBenhAn() { return tomTatBenhAn; }
    public void setTomTatBenhAn(String tomTatBenhAn) { this.tomTatBenhAn = tomTatBenhAn; }
    public String getHuongDieuTri() { return huongDieuTri; }
    public void setHuongDieuTri(String huongDieuTri) { this.huongDieuTri = huongDieuTri; }
    public String getLoiDanBacSi() { return loiDanBacSi; }
    public void setLoiDanBacSi(String loiDanBacSi) { this.loiDanBacSi = loiDanBacSi; }

    public KetQuaDieuTri getKetQuaDieuTri() { return ketQuaDieuTri; }
    public void setKetQuaDieuTri(KetQuaDieuTri ketQuaDieuTri) { this.ketQuaDieuTri = ketQuaDieuTri; }
    public HuongXuTri getHuongXuTri() { return huongXuTri; }
    public void setHuongXuTri(HuongXuTri huongXuTri) { this.huongXuTri = huongXuTri; }
    public LocalDate getNgayHenTaiKham() { return ngayHenTaiKham; }
    public void setNgayHenTaiKham(LocalDate ngayHenTaiKham) { this.ngayHenTaiKham = ngayHenTaiKham; }
    public Integer getSoNgayNghiHuongBHXH() { return soNgayNghiHuongBHXH; }
    public void setSoNgayNghiHuongBHXH(Integer soNgayNghiHuongBHXH) { this.soNgayNghiHuongBHXH = soNgayNghiHuongBHXH; }
    public String getLyDoChuyenTuyen() { return lyDoChuyenTuyen; }
    public void setLyDoChuyenTuyen(String lyDoChuyenTuyen) { this.lyDoChuyenTuyen = lyDoChuyenTuyen; }

    public BenhNhan getBenhNhan() { return benhNhan; }
    public void setBenhNhan(BenhNhan benhNhan) { this.benhNhan = benhNhan; }
    public NhanVien getBacSi() { return bacSi; }
    public void setBacSi(NhanVien bacSi) { this.bacSi = bacSi; }
    public LichHen getLichHen() { return lichHen; }
    public void setLichHen(LichHen lichHen) { this.lichHen = lichHen; }
    public CoSo getCoSo() { return coSo; }
    public void setCoSo(CoSo coSo) { this.coSo = coSo; }
    public Khoa getKhoa() { return khoa; }
    public void setKhoa(Khoa khoa) { this.khoa = khoa; }
    public PhongKham getPhongKham() { return phongKham; }
    public void setPhongKham(PhongKham phongKham) { this.phongKham = phongKham; }
    public List<ChanDoan> getChanDoans() { return chanDoans; }
    public void setChanDoans(List<ChanDoan> chanDoans) { this.chanDoans = chanDoans; }
    public List<SinhHieu> getSinhHieus() { return sinhHieus; }
    public void setSinhHieus(List<SinhHieu> sinhHieus) { this.sinhHieus = sinhHieus; }
    public List<ChiDinhDichVu> getChiDinhDichVus() { return chiDinhDichVus; }
    public void setChiDinhDichVus(List<ChiDinhDichVu> chiDinhDichVus) { this.chiDinhDichVus = chiDinhDichVus; }
    public List<DonThuoc> getDonThuocs() { return donThuocs; }
    public void setDonThuocs(List<DonThuoc> donThuocs) { this.donThuocs = donThuocs; }
    public List<HoaDon> getHoaDons() { return hoaDons; }
    public void setHoaDons(List<HoaDon> hoaDons) { this.hoaDons = hoaDons; }
}
