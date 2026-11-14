package vn.clinicmanagement.application.examination;

import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.Command;
import vn.clinicmanagement.domain.examination.TransferLyDoMa;

import java.util.UUID;

public record TransferLuotKhamCommand(
        /** Trường `visitId`: luotkham mã. */
        int visitId,
        /** Trường `destinationRoomId`: `destination` phòng mã. */
        int destinationRoomId,
        /** Trường `reasonCode`: lý do mã. */
        TransferLyDoMa reasonCode,
        /** Trường `transferNote`: chuyển `Note`. */
        String transferNote,
        /** Trường `authorizationContext`: phanquyen ngữ cảnh. */
        PhanQuyenNguCanh authorizationContext,
        /** Trường `auditActorContext`: kiểm toán tacnhan ngữ cảnh. */
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

    /** Hàm `context`: ngữ cảnh. */
    public PhanQuyenNguCanh context() {
        return authorizationContext;
    }

    public String note() {
        return transferNote;
    }
}
