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
    /** Hàm `save`: lưu. */
    void save(ChiDinhDichVu chiDinh);
    /** Hàm `getByLuotKham`: lấy theo luot kham. */
    List<ChiDinhDichVu> getByLuotKham(int maLuotKham);
    /** Hàm `getKetQuaByChiDinh`: lấy ket qua theo chi dinh. */
    List<ChiTietKetQuaXetNghiem> getKetQuaByChiDinh(int maChiDinh);
    /** Hàm `saveKetQua`: lưu ket qua. */
    void saveKetQua(ChiTietKetQuaXetNghiem ketQua);
    /** Hàm `getDichVuById`: lấy dich vu theo mã. */
    Optional<DanhMucDichVu> getDichVuById(int maDichVu);
    /** Hàm `searchDichVu`: tìm kiếm dich vu. */
    List<DanhMucDichVu> searchDichVu(String keyword, LoaiDichVu loaiDichVu);
}
