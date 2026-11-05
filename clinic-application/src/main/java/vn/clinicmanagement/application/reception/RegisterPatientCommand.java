package vn.clinicmanagement.application.reception;

import vn.clinicmanagement.application.authorization.AuthorizationContext;
import vn.clinicmanagement.application.common.Command;
import vn.clinicmanagement.domain.common.enums.DoiTuongKham;
import vn.clinicmanagement.domain.patient.BaoHiem;
import vn.clinicmanagement.domain.patient.BenhNhan;

import java.util.Objects;

/** A reception registration: create/update demographics, persist insurance, and open today's visit. */
public record RegisterPatientCommand(BenhNhan patient, BaoHiem insurance, int roomId,
                                    String registrationType, String reason,
                                    DoiTuongKham payerType, boolean priority, int createdBy,
                                    AuthorizationContext authorizationContext) implements Command<RegistrationReceipt> {
    public RegisterPatientCommand {
        Objects.requireNonNull(authorizationContext, "authorizationContext");
        if (!authorizationContext.isValid()) {
            throw new IllegalArgumentException("A valid authorization context is required.");
        }
        if (createdBy != authorizationContext.employeeId()) {
            throw new IllegalArgumentException("createdBy must match the authenticated actor.");
        }
    }
}
