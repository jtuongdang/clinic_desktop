package vn.clinicmanagement.domain.service;

import java.util.Objects;

/** Cấu hình danh mục được dùng khi thao tác trên một chỉ định. */
public record ThietLapThucHienDichVu(boolean chanHuyChiDinh, boolean khongInPhieuRieng,
        String donViXetNghiem, String thamChieuXetNghiem, String huongDanTraKetQua,
        String mauNoiDungKetQua, String mauKetLuan) {
    public static final ThietLapThucHienDichVu MAC_DINH =
            new ThietLapThucHienDichVu(false, false, "", "", "", "", "");

    public ThietLapThucHienDichVu {
        donViXetNghiem = Objects.toString(donViXetNghiem, "").strip();
        thamChieuXetNghiem = Objects.toString(thamChieuXetNghiem, "").strip();
        huongDanTraKetQua = Objects.toString(huongDanTraKetQua, "").strip();
        mauNoiDungKetQua = Objects.toString(mauNoiDungKetQua, "").strip();
        mauKetLuan = Objects.toString(mauKetLuan, "").strip();
    }
}
