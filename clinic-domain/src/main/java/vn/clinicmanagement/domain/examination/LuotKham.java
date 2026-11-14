package vn.clinicmanagement.domain.examination;

import vn.clinicmanagement.domain.common.CoSoThucThe;
import vn.clinicmanagement.domain.common.ITongHopGoc;
import vn.clinicmanagement.domain.common.enums.DoiTuongKham;
import vn.clinicmanagement.domain.common.enums.HinhThucDieuTri;
import vn.clinicmanagement.domain.common.enums.HuongXuTri;
import vn.clinicmanagement.domain.common.enums.KetQuaDieuTri;
import vn.clinicmanagement.domain.common.enums.TrangThaiLuotKham;
import vn.clinicmanagement.domain.common.events.EMRDaKhoaSuKien;
import vn.clinicmanagement.domain.common.events.LuotKhamDaHoanTatSuKien;
import vn.clinicmanagement.domain.common.events.LuotKhamDaBatDauSuKien;
import vn.clinicmanagement.domain.common.exceptions.EMRDaKhoaNgoaiLe;
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

public class LuotKham extends CoSoThucThe<Integer> implements ITongHopGoc {
    private int maBenhNhan;
    private Integer maBacSi;
    /** Trường `maLichHen`: `ma` lich hen. */
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
    /** Trường `emrLocked`: emr dakhoa. */
    private boolean emrLocked = false;
    /** Trường `emrSignature`: emr `Signature`. */
    private String emrSignature;
    /** Trường `emrLockedAt`: emr dakhoa luc. */
    private LocalDateTime emrLockedAt;
    /** Trường `emrLockedBy`: emr dakhoa theo. */
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
    /** Trường `ngayHenTaiKham`: `ngay` hen `Tai` kham. */
    private LocalDate ngayHenTaiKham;
    private Integer soNgayNghiHuongBHXH;
    private String lyDoChuyenTuyen = "";

    private BenhNhan benhNhan;
    private NhanVien bacSi;
    /** Trường `lichHen`: `lich` hen. */
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

    /** Hàm `startExam`: bắt đầu kham. */
    public void startExam() {
        if (this.trangThai != TrangThaiLuotKham.CHO_KHAM) {
            throw new IllegalStateException("Lượt khám không ở trạng thái chờ khám.");
        }
        this.trangThai = TrangThaiLuotKham.DANG_KHAM;
        this.thoiGianBatDauKham = LocalDateTime.now();
        registerDomainEvent(new LuotKhamDaBatDauSuKien(getId() != null ? getId() : 0, maBenhNhan, maBacSi != null ? maBacSi : 0));
    }

    /** Hàm `completeExam`: hoàn tất kham. */
    public void completeExam() {
        if (emrLocked) {
            throw new EMRDaKhoaNgoaiLe(getId() != null ? getId() : 0);
        }
        if (this.trangThai != TrangThaiLuotKham.DANG_KHAM) {
            throw new IllegalStateException("Lượt khám không ở trạng thái đang khám.");
        }
        this.trangThai = TrangThaiLuotKham.HOAN_THANH;
        this.thoiGianKetThucKham = LocalDateTime.now();
        registerDomainEvent(new LuotKhamDaHoanTatSuKien(getId() != null ? getId() : 0, maBenhNhan, maBacSi != null ? maBacSi : 0));
    }

    /** Hàm `lockEmr`: khoa emr. */
    public void lockEmr(String signature, int doctorId) {
        if (emrLocked) {
            throw new EMRDaKhoaNgoaiLe(getId() != null ? getId() : 0);
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
        registerDomainEvent(new EMRDaKhoaSuKien(getId() != null ? getId() : 0, doctorId, signature));
    }

    /** Hàm `reopenEmr`: mở lại emr. */
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
        registerDomainEvent(new vn.clinicmanagement.domain.common.events.EMRDaMoLaiSuKien(
                getId() != null ? getId() : 0, approverId, reason));
    }

    /** Hàm `getMaBenhNhan`: lấy mã benh nhãn. */
    public int getMaBenhNhan() { return maBenhNhan; }
    /** Hàm `setMaBenhNhan`: gán mã benh nhãn. */
    public void setMaBenhNhan(int maBenhNhan) { this.maBenhNhan = maBenhNhan; }
    /** Hàm `getMaBacSi`: lấy mã `Bac` `Si`. */
    public Integer getMaBacSi() { return maBacSi; }
    /** Hàm `setMaBacSi`: gán mã `Bac` `Si`. */
    public void setMaBacSi(Integer maBacSi) { this.maBacSi = maBacSi; }
    /** Hàm `getMaLichHen`: lấy mã lich hen. */
    public Integer getMaLichHen() { return maLichHen; }
    /** Hàm `setMaLichHen`: gán mã lich hen. */
    public void setMaLichHen(Integer maLichHen) { this.maLichHen = maLichHen; }
    /** Hàm `getMaCoSo`: lấy mã có so. */
    public int getMaCoSo() { return maCoSo; }
    /** Hàm `setMaCoSo`: gán mã có so. */
    public void setMaCoSo(int maCoSo) { this.maCoSo = maCoSo; }
    /** Hàm `getMaKhoa`: lấy mã khoa. */
    public int getMaKhoa() { return maKhoa; }
    /** Hàm `setMaKhoa`: gán mã khoa. */
    public void setMaKhoa(int maKhoa) { this.maKhoa = maKhoa; }
    /** Hàm `getHinhThucDieuTri`: lấy hinh thuc dieu tri. */
    public HinhThucDieuTri getHinhThucDieuTri() { return hinhThucDieuTri; }
    /** Hàm `setHinhThucDieuTri`: gán hinh thuc dieu tri. */
    public void setHinhThucDieuTri(HinhThucDieuTri hinhThucDieuTri) { this.hinhThucDieuTri = hinhThucDieuTri; }
    /** Hàm `getTrangThai`: lấy trang thai. */
    public TrangThaiLuotKham getTrangThai() { return trangThai; }
    /** Hàm `setTrangThai`: gán trang thai. */
    public void setTrangThai(TrangThaiLuotKham trangThai) { this.trangThai = trangThai; }
    /** Hàm `getSoThuTu`: lấy so thu tu. */
    public Integer getSoThuTu() { return soThuTu; }
    /** Hàm `setSoThuTu`: gán so thu tu. */
    public void setSoThuTu(Integer soThuTu) { this.soThuTu = soThuTu; }
    /** Hàm `getNgayKham`: lấy `Ngay` kham. */
    public LocalDate getNgayKham() { return ngayKham; }
    /** Hàm `setNgayKham`: gán `Ngay` kham. */
    public void setNgayKham(LocalDate ngayKham) { this.ngayKham = ngayKham; }
    /** Hàm `getThoiGianBatDauKham`: lấy `Thoi` `Gian` `Bat` `Dau` kham. */
    public LocalDateTime getThoiGianBatDauKham() { return thoiGianBatDauKham; }
    /** Hàm `setThoiGianBatDauKham`: gán `Thoi` `Gian` `Bat` `Dau` kham. */
    public void setThoiGianBatDauKham(LocalDateTime thoiGianBatDauKham) { this.thoiGianBatDauKham = thoiGianBatDauKham; }
    /** Hàm `getThoiGianKetThucKham`: lấy `Thoi` `Gian` ket thuc kham. */
    public LocalDateTime getThoiGianKetThucKham() { return thoiGianKetThucKham; }
    /** Hàm `setThoiGianKetThucKham`: gán `Thoi` `Gian` ket thuc kham. */
    public void setThoiGianKetThucKham(LocalDateTime thoiGianKetThucKham) { this.thoiGianKetThucKham = thoiGianKetThucKham; }
    /** Hàm `getLyDoKham`: lấy `Ly` `Do` kham. */
    public String getLyDoKham() { return lyDoKham; }
    /** Hàm `setLyDoKham`: gán `Ly` `Do` kham. */
    public void setLyDoKham(String lyDoKham) { this.lyDoKham = lyDoKham; }
    /** Hàm `getMaPhongKham`: lấy mã phòng kham. */
    public Integer getMaPhongKham() { return maPhongKham; }
    /** Hàm `setMaPhongKham`: gán mã phòng kham. */
    public void setMaPhongKham(Integer maPhongKham) { this.maPhongKham = maPhongKham; }
    /** Hàm `isEmrLocked`: là emr dakhoa. */
    public boolean isEmrLocked() { return emrLocked; }
    /** Hàm `setEmrLocked`: gán emr dakhoa. */
    public void setEmrLocked(boolean emrLocked) { this.emrLocked = emrLocked; }
    /** Hàm `getEmrSignature`: lấy emr `Signature`. */
    public String getEmrSignature() { return emrSignature; }
    /** Hàm `getEmrLockedAt`: lấy emr dakhoa luc. */
    public LocalDateTime getEmrLockedAt() { return emrLockedAt; }
    /** Hàm `getEmrLockedBy`: lấy emr dakhoa theo. */
    public Integer getEmrLockedBy() { return emrLockedBy; }

    /** Hàm `getSoThuTuDisplay`: lấy so thu tu `Display`. */
    public String getSoThuTuDisplay() { return soThuTuDisplay; }
    /** Hàm `setSoThuTuDisplay`: gán so thu tu `Display`. */
    public void setSoThuTuDisplay(String soThuTuDisplay) { this.soThuTuDisplay = soThuTuDisplay; }
    /** Hàm `getThoiGianTiepNhan`: lấy `Thoi` `Gian` `Tiep` nhãn. */
    public LocalDateTime getThoiGianTiepNhan() { return thoiGianTiepNhan; }
    /** Hàm `setThoiGianTiepNhan`: gán `Thoi` `Gian` `Tiep` nhãn. */
    public void setThoiGianTiepNhan(LocalDateTime thoiGianTiepNhan) { this.thoiGianTiepNhan = thoiGianTiepNhan; }
    /** Hàm `getDoiTuongKham`: lấy doi tuong kham. */
    public DoiTuongKham getDoiTuongKham() { return doiTuongKham; }
    /** Hàm `setDoiTuongKham`: gán doi tuong kham. */
    public void setDoiTuongKham(DoiTuongKham doiTuongKham) { this.doiTuongKham = doiTuongKham; }

    /** Hàm `getMaDichVuKham`: lấy mã dich vu kham. */
    public Integer getMaDichVuKham() { return maDichVuKham; }
    /** Hàm `setMaDichVuKham`: gán mã dich vu kham. */
    public void setMaDichVuKham(Integer maDichVuKham) { this.maDichVuKham = maDichVuKham; }
    /** Hàm `getLoaiDangKyKham`: lấy loai `Dang` ky kham. */
    public String getLoaiDangKyKham() { return loaiDangKyKham; }
    /** Hàm `setLoaiDangKyKham`: gán loai `Dang` ky kham. */
    public void setLoaiDangKyKham(String loaiDangKyKham) { this.loaiDangKyKham = loaiDangKyKham; }
    /** Hàm `isDnVnTien`: là `Dn` `Vn` tien. */
    public boolean isDnVnTien() { return dnVnTien; }
    /** Hàm `setDnVnTien`: gán `Dn` `Vn` tien. */
    public void setDnVnTien(boolean dnVnTien) { this.dnVnTien = dnVnTien; }

    /** Hàm `getQuaTrinhBenhLy`: lấy qua `Trinh` benh `Ly`. */
    public String getQuaTrinhBenhLy() { return quaTrinhBenhLy; }
    /** Hàm `setQuaTrinhBenhLy`: gán qua `Trinh` benh `Ly`. */
    public void setQuaTrinhBenhLy(String quaTrinhBenhLy) { this.quaTrinhBenhLy = quaTrinhBenhLy; }
    /** Hàm `getTienSuBanThan`: lấy tien su `Ban` `Than`. */
    public String getTienSuBanThan() { return tienSuBanThan; }
    /** Hàm `setTienSuBanThan`: gán tien su `Ban` `Than`. */
    public void setTienSuBanThan(String tienSuBanThan) { this.tienSuBanThan = tienSuBanThan; }
    /** Hàm `getTienSuGiaDinh`: lấy tien su `Gia` dinh. */
    public String getTienSuGiaDinh() { return tienSuGiaDinh; }
    /** Hàm `setTienSuGiaDinh`: gán tien su `Gia` dinh. */
    public void setTienSuGiaDinh(String tienSuGiaDinh) { this.tienSuGiaDinh = tienSuGiaDinh; }
    /** Hàm `getKhamToanThan`: lấy kham toan `Than`. */
    public String getKhamToanThan() { return khamToanThan; }
    /** Hàm `setKhamToanThan`: gán kham toan `Than`. */
    public void setKhamToanThan(String khamToanThan) { this.khamToanThan = khamToanThan; }
    /** Hàm `getKhamBoPhan`: lấy kham bo phan. */
    public String getKhamBoPhan() { return khamBoPhan; }
    /** Hàm `setKhamBoPhan`: gán kham bo phan. */
    public void setKhamBoPhan(String khamBoPhan) { this.khamBoPhan = khamBoPhan; }
    /** Hàm `getTomTatBenhAn`: lấy `Tom` `Tat` benh an. */
    public String getTomTatBenhAn() { return tomTatBenhAn; }
    /** Hàm `setTomTatBenhAn`: gán `Tom` `Tat` benh an. */
    public void setTomTatBenhAn(String tomTatBenhAn) { this.tomTatBenhAn = tomTatBenhAn; }
    /** Hàm `getHuongDieuTri`: lấy huong dieu tri. */
    public String getHuongDieuTri() { return huongDieuTri; }
    /** Hàm `setHuongDieuTri`: gán huong dieu tri. */
    public void setHuongDieuTri(String huongDieuTri) { this.huongDieuTri = huongDieuTri; }
    /** Hàm `getLoiDanBacSi`: lấy `Loi` `Dan` `Bac` `Si`. */
    public String getLoiDanBacSi() { return loiDanBacSi; }
    /** Hàm `setLoiDanBacSi`: gán `Loi` `Dan` `Bac` `Si`. */
    public void setLoiDanBacSi(String loiDanBacSi) { this.loiDanBacSi = loiDanBacSi; }

    /** Hàm `getKetQuaDieuTri`: lấy ket qua dieu tri. */
    public KetQuaDieuTri getKetQuaDieuTri() { return ketQuaDieuTri; }
    /** Hàm `setKetQuaDieuTri`: gán ket qua dieu tri. */
    public void setKetQuaDieuTri(KetQuaDieuTri ketQuaDieuTri) { this.ketQuaDieuTri = ketQuaDieuTri; }
    /** Hàm `getHuongXuTri`: lấy huong xu tri. */
    public HuongXuTri getHuongXuTri() { return huongXuTri; }
    /** Hàm `setHuongXuTri`: gán huong xu tri. */
    public void setHuongXuTri(HuongXuTri huongXuTri) { this.huongXuTri = huongXuTri; }
    /** Hàm `getNgayHenTaiKham`: lấy `Ngay` hen `Tai` kham. */
    public LocalDate getNgayHenTaiKham() { return ngayHenTaiKham; }
    /** Hàm `setNgayHenTaiKham`: gán `Ngay` hen `Tai` kham. */
    public void setNgayHenTaiKham(LocalDate ngayHenTaiKham) { this.ngayHenTaiKham = ngayHenTaiKham; }
    /** Hàm `getSoNgayNghiHuongBHXH`: lấy so `Ngay` nghi huong `BHXH`. */
    public Integer getSoNgayNghiHuongBHXH() { return soNgayNghiHuongBHXH; }
    /** Hàm `setSoNgayNghiHuongBHXH`: gán so `Ngay` nghi huong `BHXH`. */
    public void setSoNgayNghiHuongBHXH(Integer soNgayNghiHuongBHXH) { this.soNgayNghiHuongBHXH = soNgayNghiHuongBHXH; }
    /** Hàm `getLyDoChuyenTuyen`: lấy `Ly` `Do` `Chuyen` `Tuyen`. */
    public String getLyDoChuyenTuyen() { return lyDoChuyenTuyen; }
    /** Hàm `setLyDoChuyenTuyen`: gán `Ly` `Do` `Chuyen` `Tuyen`. */
    public void setLyDoChuyenTuyen(String lyDoChuyenTuyen) { this.lyDoChuyenTuyen = lyDoChuyenTuyen; }

    /** Hàm `getBenhNhan`: lấy benh nhãn. */
    public BenhNhan getBenhNhan() { return benhNhan; }
    /** Hàm `setBenhNhan`: gán benh nhãn. */
    public void setBenhNhan(BenhNhan benhNhan) { this.benhNhan = benhNhan; }
    /** Hàm `getBacSi`: lấy `Bac` `Si`. */
    public NhanVien getBacSi() { return bacSi; }
    /** Hàm `setBacSi`: gán `Bac` `Si`. */
    public void setBacSi(NhanVien bacSi) { this.bacSi = bacSi; }
    /** Hàm `getLichHen`: lấy lich hen. */
    public LichHen getLichHen() { return lichHen; }
    /** Hàm `setLichHen`: gán lich hen. */
    public void setLichHen(LichHen lichHen) { this.lichHen = lichHen; }
    /** Hàm `getCoSo`: lấy có so. */
    public CoSo getCoSo() { return coSo; }
    /** Hàm `setCoSo`: gán có so. */
    public void setCoSo(CoSo coSo) { this.coSo = coSo; }
    /** Hàm `getKhoa`: lấy khoa. */
    public Khoa getKhoa() { return khoa; }
    /** Hàm `setKhoa`: gán khoa. */
    public void setKhoa(Khoa khoa) { this.khoa = khoa; }
    /** Hàm `getPhongKham`: lấy phòng kham. */
    public PhongKham getPhongKham() { return phongKham; }
    /** Hàm `setPhongKham`: gán phòng kham. */
    public void setPhongKham(PhongKham phongKham) { this.phongKham = phongKham; }
    /** Hàm `getChanDoans`: lấy chan `Doans`. */
    public List<ChanDoan> getChanDoans() { return chanDoans; }
    /** Hàm `setChanDoans`: gán chan `Doans`. */
    public void setChanDoans(List<ChanDoan> chanDoans) { this.chanDoans = chanDoans; }
    /** Hàm `getSinhHieus`: lấy sinh `Hieus`. */
    public List<SinhHieu> getSinhHieus() { return sinhHieus; }
    /** Hàm `setSinhHieus`: gán sinh `Hieus`. */
    public void setSinhHieus(List<SinhHieu> sinhHieus) { this.sinhHieus = sinhHieus; }
    /** Hàm `getChiDinhDichVus`: lấy chi dinh dich `Vus`. */
    public List<ChiDinhDichVu> getChiDinhDichVus() { return chiDinhDichVus; }
    /** Hàm `setChiDinhDichVus`: gán chi dinh dich `Vus`. */
    public void setChiDinhDichVus(List<ChiDinhDichVu> chiDinhDichVus) { this.chiDinhDichVus = chiDinhDichVus; }
    /** Hàm `getDonThuocs`: lấy don `Thuocs`. */
    public List<DonThuoc> getDonThuocs() { return donThuocs; }
    /** Hàm `setDonThuocs`: gán don `Thuocs`. */
    public void setDonThuocs(List<DonThuoc> donThuocs) { this.donThuocs = donThuocs; }
    /** Hàm `getHoaDons`: lấy hoa `Dons`. */
    public List<HoaDon> getHoaDons() { return hoaDons; }
    /** Hàm `setHoaDons`: gán hoa `Dons`. */
    public void setHoaDons(List<HoaDon> hoaDons) { this.hoaDons = hoaDons; }
}
