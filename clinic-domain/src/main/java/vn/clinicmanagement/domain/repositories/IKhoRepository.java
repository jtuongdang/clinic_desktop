package vn.clinicmanagement.domain.repositories;

import vn.clinicmanagement.domain.common.IUnitOfWork;
import vn.clinicmanagement.domain.inventory.DonDatHang;
import vn.clinicmanagement.domain.inventory.Item;
import vn.clinicmanagement.domain.inventory.Kho;
import vn.clinicmanagement.domain.inventory.PhieuNhapKho;
import vn.clinicmanagement.domain.inventory.PhieuXuatKho;
import vn.clinicmanagement.domain.inventory.Stocktake;
import vn.clinicmanagement.domain.inventory.TonKho;

import java.util.List;
import java.util.Optional;

public interface IKhoRepository extends IUnitOfWork {
    Optional<Item> getItemById(int id);
    Optional<Item> getItemByCode(String code);
    Item saveItem(Item item);
    List<Item> searchItems(String keyword, Boolean hoatDong);

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

    Stocktake saveStocktake(Stocktake stocktake);
    Optional<Stocktake> getStocktakeById(int id);
}
