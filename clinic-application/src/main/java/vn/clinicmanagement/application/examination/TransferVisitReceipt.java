package vn.clinicmanagement.application.examination;

import vn.clinicmanagement.domain.examination.TransferReasonCode;

public record TransferVisitReceipt(
        int visitId,
        int previousRoomId,
        int destinationRoomId,
        TransferReasonCode reasonCode,
        String transferNote) {

    public TransferVisitReceipt(int visitId, int previousRoomId, int destinationRoomId,
                                TransferReasonCode reasonCode) {
        this(visitId, previousRoomId, destinationRoomId, reasonCode, null);
    }

    public TransferVisitReceipt(int visitId, int destinationRoomId, TransferReasonCode reasonCode) {
        this(visitId, 0, destinationRoomId, reasonCode, null);
    }

    public int sourceRoomId() {
        return previousRoomId;
    }

    public int targetRoomId() {
        return destinationRoomId;
    }
}
