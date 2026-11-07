package vn.clinicmanagement.domain.common.events;

public class EmrReopenedEvent extends AbstractDomainEvent {
    private final int visitId;
    private final int approverId;
    private final String reason;

    public EmrReopenedEvent(int visitId, int approverId, String reason) {
        super();
        this.visitId = visitId;
        this.approverId = approverId;
        this.reason = reason;
    }

    public int getVisitId() { return visitId; }
    public int getApproverId() { return approverId; }
    public String getReason() { return reason; }
}
