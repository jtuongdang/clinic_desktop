package vn.clinicmanagement.domain.common.events;

public class EMRDaMoLaiSuKien extends TruuTuongMienNghiepVuSuKien {
    /** Trường `visitId`: luotkham mã. */
    private final int visitId;
    /** Trường `approverId`: `approver` mã. */
    private final int approverId;
    /** Trường `reason`: lý do. */
    private final String reason;

    public EMRDaMoLaiSuKien(int visitId, int approverId, String reason) {
        super();
        this.visitId = visitId;
        this.approverId = approverId;
        this.reason = reason;
    }

    /** Hàm `getVisitId`: lấy luotkham mã. */
    public int getVisitId() { return visitId; }
    /** Hàm `getApproverId`: lấy `Approver` mã. */
    public int getApproverId() { return approverId; }
    /** Hàm `getReason`: lấy lý do. */
    public String getReason() { return reason; }
}
