package vn.clinicmanagement.application.cls;

import vn.clinicmanagement.application.audit.AuditActorContext;
import vn.clinicmanagement.application.authorization.AuthorizationContext;
import vn.clinicmanagement.application.common.Command;

import java.util.List;

public record RecordServiceResultCommand(
        int orderId,
        String resultSummary,
        String conclusion,
        String imageAttachmentUrls,
        List<LabResultItemInput> resultItems,
        AuthorizationContext sessionContext,
        AuditActorContext auditActorContext
) implements Command<RecordServiceResultReceipt> {}
