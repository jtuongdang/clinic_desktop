package vn.clinicmanagement.application.reception;

import vn.clinicmanagement.application.common.QueryHandler;
import vn.clinicmanagement.application.common.Result;

import java.time.LocalDate;

public final class GetReceptionQueueHandler implements QueryHandler<GetReceptionQueueQuery, ReceptionQueueSnapshot> {
    private final ReceptionQueuePort receptionQueue;

    public GetReceptionQueueHandler(ReceptionQueuePort receptionQueue) { this.receptionQueue = receptionQueue; }

    @Override
    public Result<ReceptionQueueSnapshot> handle(GetReceptionQueueQuery query) {
        try {
            LocalDate date = query == null || query.date() == null ? LocalDate.now() : query.date();
            return Result.success(new ReceptionQueueSnapshot(receptionQueue.getQueue(date), receptionQueue.getRoomSummaries(date)));
        } catch (RuntimeException ex) {
            return Result.failure("RECEPTION_QUEUE_FAILED", "Không thể tải hàng đợi tiếp đón.");
        }
    }
}
