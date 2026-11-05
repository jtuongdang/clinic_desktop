package vn.clinicmanagement.application.reception;

import vn.clinicmanagement.application.common.Command;
import vn.clinicmanagement.domain.common.enums.DoiTuongKham;
import vn.clinicmanagement.domain.patient.BaoHiem;
import vn.clinicmanagement.domain.patient.BenhNhan;

/** A reception registration: create/update demographics, persist insurance, and open today's visit. */
public record RegisterPatientCommand(BenhNhan patient, BaoHiem insurance, int roomId,
                                    String registrationType, String reason,
                                    DoiTuongKham payerType, boolean priority, int createdBy)
        implements Command<RegistrationReceipt> {}
