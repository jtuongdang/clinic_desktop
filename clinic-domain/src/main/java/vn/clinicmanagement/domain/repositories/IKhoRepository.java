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
    Optional<MatHang> getItemById(int id);
    Optional<MatHang> getItemByCode(String code);
    MatHang saveItem(MatHang item);
    List<MatHang> searchItems(String keyword, Boolean hoatDong);

    Optional<Kho> getKhoById(int id);
    List<TonKho> getAvailableBatches(int maKho, int maItem);
    List<TonKho> getBatchesByKho(int maKho);
    Optional<TonKho> getBatchById(int id);
    TonKho saveTonKho(TonKho tonKho);

    PhieuNhapKho savePhieuNhapKho(PhieuNhapKho phieuNhapKho);
    Optional<PhieuNhapKho> getPhieuNhapKhoById(int id);

    PhieuXuatKho savePhieuXuatKho(PhieuXuatKho phieuXuatKho);
    Optional<PhieuXuatKho> getPhieuXuatKhoById(int id);

    DonDatHang savePurchaseOrder(DonDatHang donDatHang);
    Optional<DonDatHang> getPurchaseOrderById(int id);

    KiemKe saveStocktake(KiemKe stocktake);
    Optional<KiemKe> getStocktakeById(int id);
}
