package vn.clinicmanagement.domain.common.events;

public class EMRDaMoLaiSuKien extends TruuTuongMienNghiepVuSuKien {
    private final int visitId;
    private final int approverId;
    private final String reason;

    public EMRDaMoLaiSuKien(int visitId, int approverId, String reason) {
        super();
        this.visitId = visitId;
        this.approverId = approverId;
        this.reason = reason;
    }

    public int getVisitId() { return visitId; }
    public int getApproverId() { return approverId; }
    public String getReason() { return reason; }
}
