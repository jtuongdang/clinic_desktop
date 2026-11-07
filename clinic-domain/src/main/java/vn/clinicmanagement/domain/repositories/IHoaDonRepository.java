package vn.clinicmanagement.domain.repositories;

import vn.clinicmanagement.domain.billing.CongNoBenhNhan;
import vn.clinicmanagement.domain.billing.HoaDon;
import vn.clinicmanagement.domain.billing.PhieuThu;

import java.util.List;
import java.util.Optional;

public interface IHoaDonRepository extends IRepository<HoaDon> {
    Optional<HoaDon> getBySoHoaDon(String soHoaDon);
    Optional<HoaDon> getByLuotKham(int maLuotKham);
    List<HoaDon> getByBenhNhan(int maBenhNhan);
    HoaDon save(HoaDon hoaDon);
    void savePhieuThu(PhieuThu phieuThu);
    List<PhieuThu> getPhieuThuByHoaDon(int maHoaDon);
    void saveCongNo(CongNoBenhNhan congNo);
    Optional<CongNoBenhNhan> getCongNoByHoaDon(int maHoaDon);
    List<CongNoBenhNhan> getCongNoByBenhNhan(int maBenhNhan);
}
