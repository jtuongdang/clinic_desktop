package vn.clinicmanagement.application.reception;

import vn.clinicmanagement.application.audit.AuditActorContext;
import vn.clinicmanagement.application.common.Command;
import vn.clinicmanagement.domain.common.enums.DoiTuongKham;
import vn.clinicmanagement.domain.patient.BaoHiem;
import vn.clinicmanagement.domain.patient.BenhNhan;

import java.util.Objects;

/** A reception registration: create/update demographics, persist insurance, and open today's visit. */
public record RegisterPatientCommand(BenhNhan patient, BaoHiem insurance, int roomId,
                                    String registrationType, String reason,
                                    DoiTuongKham payerType, boolean priority, int createdBy,
                                    AuditActorContext actorContext) implements Command<RegistrationReceipt> {
    public RegisterPatientCommand {
        Objects.requireNonNull(actorContext, "actorContext");
        if (createdBy != actorContext.employeeId()) {
            throw new IllegalArgumentException("createdBy must match the authenticated audit actor.");
        }
    }
}
