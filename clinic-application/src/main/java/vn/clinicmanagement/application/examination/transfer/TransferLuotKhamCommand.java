package vn.clinicmanagement.application.examination.transfer;

import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.Command;
import vn.clinicmanagement.domain.examination.TransferLyDoMa;

import java.util.UUID;

public record TransferLuotKhamCommand(
        int visitId,
        int destinationRoomId,
        TransferLyDoMa reasonCode,
        String transferNote,
        PhanQuyenNguCanh authorizationContext,
        KiemToanTacNhanNguCanh auditActorContext) implements Command<TransferLuotKhamBienNhan> {

    public TransferLuotKhamCommand(int visitId, int destinationRoomId, TransferLyDoMa reasonCode,
                                String transferNote, PhanQuyenNguCanh authorizationContext) {
        this(visitId, destinationRoomId, reasonCode, transferNote, authorizationContext,
                authorizationContext != null && authorizationContext.isValid()
                        ? new KiemToanTacNhanNguCanh(
                                authorizationContext.employeeId(),
                                authorizationContext.roleId(),
                                authorizationContext.roleCode(),
                                authorizationContext.facilityId(),
                                authorizationContext.departmentId(),
                                null,
                                UUID.randomUUID())
                        : null);
    }

    public TransferLuotKhamCommand(int visitId, int destinationRoomId, TransferLyDoMa reasonCode,
                                PhanQuyenNguCanh authorizationContext) {
        this(visitId, destinationRoomId, reasonCode, null, authorizationContext);
    }

    public PhanQuyenNguCanh context() {
        return authorizationContext;
    }

    public String note() {
        return transferNote;
    }
}
