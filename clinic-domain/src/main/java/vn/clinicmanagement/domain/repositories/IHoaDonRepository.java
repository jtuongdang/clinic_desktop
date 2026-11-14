package vn.clinicmanagement.domain.repositories;

import vn.clinicmanagement.domain.billing.CongNoBenhNhan;
import vn.clinicmanagement.domain.billing.HoaDon;
import vn.clinicmanagement.domain.billing.PhieuThu;

import java.util.List;
import java.util.Optional;

public interface IHoaDonRepository extends IRepository<HoaDon> {
    /** Hàm `getBySoHoaDon`: lấy theo so hoa don. */
    Optional<HoaDon> getBySoHoaDon(String soHoaDon);
    /** Hàm `getByLuotKham`: lấy theo luot kham. */
    Optional<HoaDon> getByLuotKham(int maLuotKham);
    /** Hàm `getByBenhNhan`: lấy theo benh nhãn. */
    List<HoaDon> getByBenhNhan(int maBenhNhan);
    /** Hàm `save`: lưu. */
    HoaDon save(HoaDon hoaDon);
    /** Hàm `savePhieuThu`: lưu phiếu thu. */
    void savePhieuThu(PhieuThu phieuThu);
    /** Hàm `getPhieuThuByHoaDon`: lấy phiếu thu theo hoa don. */
    List<PhieuThu> getPhieuThuByHoaDon(int maHoaDon);
    /** Hàm `saveCongNo`: lưu cong no. */
    void saveCongNo(CongNoBenhNhan congNo);
    /** Hàm `getCongNoByHoaDon`: lấy cong no theo hoa don. */
    Optional<CongNoBenhNhan> getCongNoByHoaDon(int maHoaDon);
    /** Hàm `getCongNoByBenhNhan`: lấy cong no theo benh nhãn. */
    List<CongNoBenhNhan> getCongNoByBenhNhan(int maBenhNhan);
}
