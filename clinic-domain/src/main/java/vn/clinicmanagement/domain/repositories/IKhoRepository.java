package vn.clinicmanagement.domain.repositories;
import vn.clinicmanagement.domain.common.IUnitOfWork;
import vn.clinicmanagement.domain.inventory.Item;
import vn.clinicmanagement.domain.inventory.TonKho;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface IKhoRepository extends IUnitOfWork {
    Optional<Item> getItemById(int id);
    List<TonKho> getAvailableBatches(int maKho, int maItem);
}
