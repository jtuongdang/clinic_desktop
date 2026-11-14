package vn.clinicmanagement.domain.patient;

import vn.clinicmanagement.domain.common.CoSoThucThe;
import vn.clinicmanagement.domain.common.enums.LoaiBaoHiem;
import java.time.LocalDate;

public class BaoHiem extends CoSoThucThe<Integer> {
    private int maBenhNhan;
    private LoaiBaoHiem loaiBaoHiem = LoaiBaoHiem.BHYT;
    private String soBaoHiem = "";
    private LocalDate ngayBatDau;
    private LocalDate ngayHetHan;
    private int mucHuong;
    private String noiDangKyKCB = "";

    // Bổ sung chuẩn tiếp đón & Cổng giám định BHYT QĐ 130/BYT
    private String maNoiSong = "";         // K1, K2, K3
    private String maNoiDangKyKCB = "";    // Ví dụ: 01-025
    private String diaChiThe = "";         // Địa chỉ in trên phôi thẻ BHYT
    private boolean dungTuyen = true;      // Đúng tuyến / Trái tuyến
    private boolean coGiayKhaiSinh = false;// Trẻ em có giấy khai sinh
    private boolean khongGiuThe = false;   // Không giữ thẻ BHYT bản cứng
    private boolean mienCungChiTra = false;// Giấy chứng nhận không cùng chi trả trong năm
    private LocalDate ngayMienCungChiTra;  // Ngày bắt đầu được miễn cùng chi trả
    private LocalDate ngayDu5NamLienTuc;   // Ngày tham gia đủ 5 năm liên tục

    private BenhNhan benhNhan;

    public BaoHiem() {}

    /** Hàm `getMaBenhNhan`: lấy mã benh nhãn. */
    public int getMaBenhNhan() { return maBenhNhan; }
    /** Hàm `setMaBenhNhan`: gán mã benh nhãn. */
    public void setMaBenhNhan(int maBenhNhan) { this.maBenhNhan = maBenhNhan; }
    /** Hàm `getLoaiBaoHiem`: lấy loai bao hiem. */
    public LoaiBaoHiem getLoaiBaoHiem() { return loaiBaoHiem; }
    /** Hàm `setLoaiBaoHiem`: gán loai bao hiem. */
    public void setLoaiBaoHiem(LoaiBaoHiem loaiBaoHiem) { this.loaiBaoHiem = loaiBaoHiem; }
    /** Hàm `getSoBaoHiem`: lấy so bao hiem. */
    public String getSoBaoHiem() { return soBaoHiem; }
    /** Hàm `setSoBaoHiem`: gán so bao hiem. */
    public void setSoBaoHiem(String soBaoHiem) { this.soBaoHiem = soBaoHiem; }
    /** Hàm `getNgayBatDau`: lấy `Ngay` `Bat` `Dau`. */
    public LocalDate getNgayBatDau() { return ngayBatDau; }
    /** Hàm `setNgayBatDau`: gán `Ngay` `Bat` `Dau`. */
    public void setNgayBatDau(LocalDate ngayBatDau) { this.ngayBatDau = ngayBatDau; }
    /** Hàm `getNgayHetHan`: lấy `Ngay` `Het` han. */
    public LocalDate getNgayHetHan() { return ngayHetHan; }
    /** Hàm `setNgayHetHan`: gán `Ngay` `Het` han. */
    public void setNgayHetHan(LocalDate ngayHetHan) { this.ngayHetHan = ngayHetHan; }
    /** Hàm `getMucHuong`: lấy muc huong. */
    public int getMucHuong() { return mucHuong; }
    /** Hàm `setMucHuong`: gán muc huong. */
    public void setMucHuong(int mucHuong) { this.mucHuong = mucHuong; }
    /** Hàm `getNoiDangKyKCB`: lấy noi `Dang` ky `KCB`. */
    public String getNoiDangKyKCB() { return noiDangKyKCB; }
    /** Hàm `setNoiDangKyKCB`: gán noi `Dang` ky `KCB`. */
    public void setNoiDangKyKCB(String noiDangKyKCB) { this.noiDangKyKCB = noiDangKyKCB; }

    /** Hàm `getMaNoiSong`: lấy mã noi `Song`. */
    public String getMaNoiSong() { return maNoiSong; }
    /** Hàm `setMaNoiSong`: gán mã noi `Song`. */
    public void setMaNoiSong(String maNoiSong) { this.maNoiSong = maNoiSong; }
    /** Hàm `getMaNoiDangKyKCB`: lấy mã noi `Dang` ky `KCB`. */
    public String getMaNoiDangKyKCB() { return maNoiDangKyKCB; }
    /** Hàm `setMaNoiDangKyKCB`: gán mã noi `Dang` ky `KCB`. */
    public void setMaNoiDangKyKCB(String maNoiDangKyKCB) { this.maNoiDangKyKCB = maNoiDangKyKCB; }
    /** Hàm `getDiaChiThe`: lấy `Dia` chi `The`. */
    public String getDiaChiThe() { return diaChiThe; }
    /** Hàm `setDiaChiThe`: gán `Dia` chi `The`. */
    public void setDiaChiThe(String diaChiThe) { this.diaChiThe = diaChiThe; }
    /** Hàm `isDungTuyen`: là `Dung` `Tuyen`. */
    public boolean isDungTuyen() { return dungTuyen; }
    /** Hàm `setDungTuyen`: gán `Dung` `Tuyen`. */
    public void setDungTuyen(boolean dungTuyen) { this.dungTuyen = dungTuyen; }
    /** Hàm `isCoGiayKhaiSinh`: là có `Giay` `Khai` sinh. */
    public boolean isCoGiayKhaiSinh() { return coGiayKhaiSinh; }
    /** Hàm `setCoGiayKhaiSinh`: gán có `Giay` `Khai` sinh. */
    public void setCoGiayKhaiSinh(boolean coGiayKhaiSinh) { this.coGiayKhaiSinh = coGiayKhaiSinh; }
    /** Hàm `isKhongGiuThe`: là `Khong` `Giu` `The`. */
    public boolean isKhongGiuThe() { return khongGiuThe; }
    /** Hàm `setKhongGiuThe`: gán `Khong` `Giu` `The`. */
    public void setKhongGiuThe(boolean khongGiuThe) { this.khongGiuThe = khongGiuThe; }
    /** Hàm `isMienCungChiTra`: là `Mien` cung chi `Tra`. */
    public boolean isMienCungChiTra() { return mienCungChiTra; }
    /** Hàm `setMienCungChiTra`: gán `Mien` cung chi `Tra`. */
    public void setMienCungChiTra(boolean mienCungChiTra) { this.mienCungChiTra = mienCungChiTra; }
    /** Hàm `getNgayMienCungChiTra`: lấy `Ngay` `Mien` cung chi `Tra`. */
    public LocalDate getNgayMienCungChiTra() { return ngayMienCungChiTra; }
    /** Hàm `setNgayMienCungChiTra`: gán `Ngay` `Mien` cung chi `Tra`. */
    public void setNgayMienCungChiTra(LocalDate ngayMienCungChiTra) { this.ngayMienCungChiTra = ngayMienCungChiTra; }
    /** Hàm `getNgayDu5NamLienTuc`: lấy `Ngay` du `5` nam `Lien` `Tuc`. */
    public LocalDate getNgayDu5NamLienTuc() { return ngayDu5NamLienTuc; }
    /** Hàm `setNgayDu5NamLienTuc`: gán `Ngay` du `5` nam `Lien` `Tuc`. */
    public void setNgayDu5NamLienTuc(LocalDate ngayDu5NamLienTuc) { this.ngayDu5NamLienTuc = ngayDu5NamLienTuc; }

    /** Hàm `getBenhNhan`: lấy benh nhãn. */
    public BenhNhan getBenhNhan() { return benhNhan; }
    /** Hàm `setBenhNhan`: gán benh nhãn. */
    public void setBenhNhan(BenhNhan benhNhan) { this.benhNhan = benhNhan; }
}
