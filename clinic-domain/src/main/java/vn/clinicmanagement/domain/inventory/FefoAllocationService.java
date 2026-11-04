package vn.clinicmanagement.domain.inventory;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class FefoAllocationService {
    public record AllocationLine(TonKho batch, BigDecimal quantityToTake, BigDecimal quantityBefore) {}

    public List<AllocationLine> allocateFefo(List<TonKho> batches, BigDecimal requiredQty) {
        if (requiredQty == null || requiredQty.compareTo(BigDecimal.ZERO) <= 0) return List.of();
        List<TonKho> sorted = batches.stream()
                .filter(b -> b.getSoLuongTon().compareTo(BigDecimal.ZERO) > 0)
                .sorted(Comparator.comparing(TonKho::getHanSuDung))
                .toList();
        List<AllocationLine> res = new ArrayList<>();
        BigDecimal rem = requiredQty;
        for (TonKho b : sorted) {
            if (rem.compareTo(BigDecimal.ZERO) <= 0) break;
            BigDecimal take = b.getSoLuongTon().min(rem);
            res.add(new AllocationLine(b, take, b.getSoLuongTon()));
            rem = rem.subtract(take);
        }
        if (rem.compareTo(BigDecimal.ZERO) > 0) throw new IllegalStateException("Không đủ tồn kho.");
        return res;
    }
}
