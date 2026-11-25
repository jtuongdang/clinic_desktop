package vn.clinicmanagement.application.cls.record;

import vn.clinicmanagement.application.cls.XetNghiemKetQuaMatHangDauVao;

import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.Command;

import java.util.List;
import java.util.UUID;

public record RecordDichVuKetQuaCommand(
        int orderId,
        String resultSummary,
        String conclusion,
        String imageAttachmentUrls,
        List<XetNghiemKetQuaMatHangDauVao> resultItems,
        PhanQuyenNguCanh sessionContext,
        KiemToanTacNhanNguCanh auditActorContext,
        List<vn.clinicmanagement.application.inventory.TieuHaoDichVuCongKetNoi.SoLuongThucTe> actualConsumption,
        UUID consumptionOperationKey,
        boolean approveFinal
) implements Command<RecordDichVuKetQuaBienNhan> {
    public RecordDichVuKetQuaCommand(int orderId,String resultSummary,String conclusion,String imageAttachmentUrls,
                                     List<XetNghiemKetQuaMatHangDauVao> resultItems,PhanQuyenNguCanh sessionContext,
                                     KiemToanTacNhanNguCanh auditActorContext){
        this(orderId,resultSummary,conclusion,imageAttachmentUrls,resultItems,sessionContext,auditActorContext,List.of(),UUID.randomUUID(),true);
    }
    public RecordDichVuKetQuaCommand(int orderId,String resultSummary,String conclusion,String imageAttachmentUrls,
                                     List<XetNghiemKetQuaMatHangDauVao> resultItems,PhanQuyenNguCanh sessionContext,
                                     KiemToanTacNhanNguCanh auditActorContext,
                                     List<vn.clinicmanagement.application.inventory.TieuHaoDichVuCongKetNoi.SoLuongThucTe> actualConsumption,
                                     UUID consumptionOperationKey){
        this(orderId,resultSummary,conclusion,imageAttachmentUrls,resultItems,sessionContext,auditActorContext,actualConsumption,consumptionOperationKey,true);
    }
    public RecordDichVuKetQuaCommand { actualConsumption=actualConsumption==null?List.of():List.copyOf(actualConsumption);consumptionOperationKey=consumptionOperationKey==null?UUID.randomUUID():consumptionOperationKey; }
}
