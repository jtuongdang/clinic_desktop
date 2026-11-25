package vn.clinicmanagement.application.cls.record;

import java.time.LocalDateTime;

public record RecordDichVuKetQuaBienNhan(
        int orderId,
        int visitId,
        String status,
        int performedBy,
        int resultItemCount,
        LocalDateTime performedAt,
        LocalDateTime returnedAt,
        String consumptionStatus
) {
    public RecordDichVuKetQuaBienNhan(int orderId,int visitId,String status,int performedBy,int resultItemCount,
                                      LocalDateTime performedAt,LocalDateTime returnedAt){
        this(orderId,visitId,status,performedBy,resultItemCount,performedAt,returnedAt,"NOT_CONFIGURED");
    }
}
