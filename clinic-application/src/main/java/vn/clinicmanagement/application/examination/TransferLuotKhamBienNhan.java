package vn.clinicmanagement.application.examination;

import vn.clinicmanagement.domain.examination.TransferLyDoMa;

public record TransferLuotKhamBienNhan(
        /** Trường `visitId`: luotkham mã. */
        int visitId,
        /** Trường `previousRoomId`: `previous` phòng mã. */
        int previousRoomId,
        /** Trường `destinationRoomId`: `destination` phòng mã. */
        int destinationRoomId,
        /** Trường `reasonCode`: lý do mã. */
        TransferLyDoMa reasonCode,
        /** Trường `transferNote`: chuyển `Note`. */
        String transferNote) {

    public TransferLuotKhamBienNhan(int visitId, int previousRoomId, int destinationRoomId,
                                TransferLyDoMa reasonCode) {
        this(visitId, previousRoomId, destinationRoomId, reasonCode, null);
    }

    public TransferLuotKhamBienNhan(int visitId, int destinationRoomId, TransferLyDoMa reasonCode) {
        this(visitId, 0, destinationRoomId, reasonCode, null);
    }

    /** Hàm `sourceRoomId`: nguồn phòng mã. */
    public int sourceRoomId() {
        return previousRoomId;
    }

    /** Hàm `targetRoomId`: `target` phòng mã. */
    public int targetRoomId() {
        return destinationRoomId;
    }
}
