package vn.clinicmanagement.application.inventory;

import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.KetQua;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Contract for recording actual cabinet material usage against a completed service execution. */
public interface TieuHaoDichVuCongKetNoi {
    record CauHinh(int itemId,int cabinetId,String code,String name,String unit,BigDecimal standardQuantity,boolean rounding) {
        public CauHinh(int itemId,int cabinetId,String code,String name,String unit,BigDecimal standardQuantity) {
            this(itemId,cabinetId,code,name,unit,standardQuantity,false);
        }
        /** Lượng gợi ý; lượng thực dùng do người thực hiện xác nhận. */
        public BigDecimal suggestedQuantity() {
            return rounding ? standardQuantity.setScale(0, java.math.RoundingMode.CEILING) : standardQuantity;
        }
    }
    record SoLuongThucTe(int itemId,int cabinetId,BigDecimal quantity) {}
    record BienNhan(long consumptionId,String status,int postedLineCount) {}
    record ChoXuLy(long id,int serviceOrderId,int serviceId,String status,String reason) {
        @Override public String toString(){return "Tiêu hao #"+id+" — chỉ định "+serviceOrderId+" — "+status+(reason==null||reason.isBlank()?"":" — "+reason);}
    }
    record BaoCaoDong(java.time.LocalDateTime occurredAt,int serviceOrderId,String serviceCode,String serviceName,
                     int itemId,String itemCode,String itemName,String unit,String cabinetName,
                     BigDecimal actual,BigDecimal posted,String status) {}

    List<CauHinh> getDefinition(PhanQuyenNguCanh context,int serviceOrderId);
    KetQua<BienNhan> record(PhanQuyenNguCanh context,int serviceOrderId,UUID operationKey,
                            List<SoLuongThucTe> actualQuantities,String reason);
    List<ChoXuLy> getPending(PhanQuyenNguCanh context);
    KetQua<BienNhan> retryPending(PhanQuyenNguCanh context,long consumptionId);
    List<BaoCaoDong> getReport(PhanQuyenNguCanh context,java.time.LocalDateTime from,java.time.LocalDateTime until);
}
