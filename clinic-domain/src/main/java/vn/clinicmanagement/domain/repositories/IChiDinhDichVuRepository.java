package vn.clinicmanagement.domain.repositories;

import vn.clinicmanagement.domain.common.enums.LoaiDichVu;
import vn.clinicmanagement.domain.service.ChiDinhDichVu;
import vn.clinicmanagement.domain.service.ChiTietKetQuaXetNghiem;
import vn.clinicmanagement.domain.service.DanhMucDichVu;

import java.util.List;
import java.util.Optional;

/**
 * Cổng giao tiếp kho dữ liệu cho dịch vụ kỹ thuật và cận lâm sàng (CLS)
 */
public interface IChiDinhDichVuRepository extends IRepository<ChiDinhDichVu> {
    void save(ChiDinhDichVu chiDinh);
    List<ChiDinhDichVu> getByLuotKham(int maLuotKham);
    List<ChiTietKetQuaXetNghiem> getKetQuaByChiDinh(int maChiDinh);
    void saveKetQua(ChiTietKetQuaXetNghiem ketQua);
    Optional<DanhMucDichVu> getDichVuById(int maDichVu);
    List<DanhMucDichVu> searchDichVu(String keyword, LoaiDichVu loaiDichVu);
}
