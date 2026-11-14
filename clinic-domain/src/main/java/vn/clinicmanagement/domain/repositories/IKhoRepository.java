package vn.clinicmanagement.domain.repositories;

import vn.clinicmanagement.domain.common.IUnitOfWork;
import vn.clinicmanagement.domain.inventory.DonDatHang;
import vn.clinicmanagement.domain.inventory.MatHang;
import vn.clinicmanagement.domain.inventory.Kho;
import vn.clinicmanagement.domain.inventory.PhieuNhapKho;
import vn.clinicmanagement.domain.inventory.PhieuXuatKho;
import vn.clinicmanagement.domain.inventory.KiemKe;
import vn.clinicmanagement.domain.inventory.TonKho;

import java.util.List;
import java.util.Optional;

public interface IKhoRepository extends IUnitOfWork {
    /** Hàm `getItemById`: lấy mathang theo mã. */
    Optional<MatHang> getItemById(int id);
    /** Hàm `getItemByCode`: lấy mathang theo mã. */
    Optional<MatHang> getItemByCode(String code);
    /** Hàm `saveItem`: lưu mathang. */
    MatHang saveItem(MatHang item);
    /** Hàm `searchItems`: tìm kiếm `Items`. */
    List<MatHang> searchItems(String keyword, Boolean hoatDong);

    /** Hàm `getKhoById`: lấy kho theo mã. */
    Optional<Kho> getKhoById(int id);
    /** Hàm `getAvailableBatches`: lấy khadung `Batches`. */
    List<TonKho> getAvailableBatches(int maKho, int maItem);
    /** Hàm `getBatchesByKho`: lấy `Batches` theo kho. */
    List<TonKho> getBatchesByKho(int maKho);
    /** Hàm `getBatchById`: lấy lo theo mã. */
    Optional<TonKho> getBatchById(int id);
    /** Hàm `saveTonKho`: lưu ton kho. */
    TonKho saveTonKho(TonKho tonKho);

    /** Hàm `savePhieuNhapKho`: lưu phiếu nhap kho. */
    PhieuNhapKho savePhieuNhapKho(PhieuNhapKho phieuNhapKho);
    /** Hàm `getPhieuNhapKhoById`: lấy phiếu nhap kho theo mã. */
    Optional<PhieuNhapKho> getPhieuNhapKhoById(int id);

    /** Hàm `savePhieuXuatKho`: lưu phiếu xuất kho. */
    PhieuXuatKho savePhieuXuatKho(PhieuXuatKho phieuXuatKho);
    /** Hàm `getPhieuXuatKhoById`: lấy phiếu xuất kho theo mã. */
    Optional<PhieuXuatKho> getPhieuXuatKhoById(int id);

    /** Hàm `savePurchaseOrder`: lưu muahang yêu cầu. */
    DonDatHang savePurchaseOrder(DonDatHang donDatHang);
    /** Hàm `getPurchaseOrderById`: lấy muahang yêu cầu theo mã. */
    Optional<DonDatHang> getPurchaseOrderById(int id);

    /** Hàm `saveStocktake`: lưu kiemke. */
    KiemKe saveStocktake(KiemKe stocktake);
    /** Hàm `getStocktakeById`: lấy kiemke theo mã. */
    Optional<KiemKe> getStocktakeById(int id);
}
