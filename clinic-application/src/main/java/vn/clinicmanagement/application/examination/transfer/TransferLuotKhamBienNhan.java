package vn.clinicmanagement.application.examination.transfer;

import vn.clinicmanagement.domain.examination.TransferLyDoMa;

public record TransferLuotKhamBienNhan(
        int visitId,
        int previousRoomId,
        int destinationRoomId,
        TransferLyDoMa reasonCode,
        String transferNote) {

    public TransferLuotKhamBienNhan(int visitId, int previousRoomId, int destinationRoomId,
                                TransferLyDoMa reasonCode) {
        this(visitId, previousRoomId, destinationRoomId, reasonCode, null);
    }

    public TransferLuotKhamBienNhan(int visitId, int destinationRoomId, TransferLyDoMa reasonCode) {
        this(visitId, 0, destinationRoomId, reasonCode, null);
    }

    public int sourceRoomId() {
        return previousRoomId;
    }

    public int targetRoomId() {
        return destinationRoomId;
    }
}
