package vn.clinicmanagement.application.reception;

import vn.clinicmanagement.application.common.CommandHandler;
import vn.clinicmanagement.application.common.Result;
import vn.clinicmanagement.application.common.TransactionBoundary;
import vn.clinicmanagement.domain.common.enums.DoiTuongKham;
import vn.clinicmanagement.domain.facility.PhongKham;
import vn.clinicmanagement.domain.patient.BaoHiem;
import vn.clinicmanagement.domain.patient.BenhNhan;
import vn.clinicmanagement.domain.patient.PatientIdentityConflictException;
import vn.clinicmanagement.domain.repositories.IBaoHiemRepository;
import vn.clinicmanagement.domain.repositories.IBenhNhanRepository;
import vn.clinicmanagement.domain.repositories.IPhongKhamRepository;

import java.time.LocalDate;

public final class RegisterPatientHandler implements CommandHandler<RegisterPatientCommand, RegistrationReceipt> {
    private final IBenhNhanRepository patients;
    private final IBaoHiemRepository insuranceCards;
    private final IPhongKhamRepository rooms;
    private final ReceptionQueuePort receptionQueue;
    private final TransactionBoundary transactions;

    public RegisterPatientHandler(IBenhNhanRepository patients, IBaoHiemRepository insuranceCards,
                                  IPhongKhamRepository rooms, ReceptionQueuePort receptionQueue,
                                  TransactionBoundary transactions) {
        this.patients = patients;
        this.insuranceCards = insuranceCards;
        this.rooms = rooms;
        this.receptionQueue = receptionQueue;
        this.transactions = transactions;
    }

    @Override
    public Result<RegistrationReceipt> handle(RegisterPatientCommand command) {
        String validationError = validate(command);
        if (validationError != null) return Result.failure("PATIENT_VALIDATION", validationError);

        return transactions.inTransaction(() -> {
            try {
                BenhNhan patient = command.patient();
                boolean existing = patient.getId() != null && patient.getId() > 0;
                if (!existing && patient.getSoCCCD() != null && !patient.getSoCCCD().isBlank()
                        && patients.getByCCCD(patient.getSoCCCD()).isPresent()) {
                    return Result.failure("PATIENT_DUPLICATE_CCCD", "CCCD đã có hồ sơ. Hãy tìm người bệnh và chọn hồ sơ hiện có.");
                }

                PhongKham room = rooms.getById(command.roomId()).orElse(null);
                if (room == null || !room.isHoatDong() || room.getKhoa() == null || room.getKhoa().getId() == null
                        || room.getKhoa().getCoSo() == null || room.getKhoa().getCoSo().getId() == null) {
                    return Result.failure("ROOM_UNAVAILABLE", "Phòng khám không hoạt động hoặc chưa gắn với cơ sở/khoa.");
                }

                if (existing) patients.update(patient);
                else patients.add(patient);

                if (hasInsuranceNumber(command.insurance())) {
                    BaoHiem card = command.insurance();
                    card.setMaBenhNhan(patient.getId());
                    var currentCard = insuranceCards.getLatestByPatientId(patient.getId());
                    if (currentCard.isPresent()) {
                        card.setId(currentCard.get().getId());
                        card.setVersion(currentCard.get().getVersion());
                        insuranceCards.update(card);
                    } else {
                        insuranceCards.add(card);
                    }
                }

                int facilityId = room.getKhoa().getCoSo().getId();
                int departmentId = room.getMaKhoa();
                var visit = receptionQueue.registerVisit(patient.getId(), facilityId, departmentId,
                        command.roomId(), command.registrationType(), command.reason(), command.payerType(),
                        command.priority(), command.createdBy());

                return Result.success(new RegistrationReceipt(patient.getId(), patient.getMaBenhNhanFormat(),
                        visit.getId(), visit.getSoThuTu() == null ? 0 : visit.getSoThuTu(), visit.getSoThuTuDisplay()));
            } catch (PatientIdentityConflictException ex) {
                return Result.failure("PATIENT_DUPLICATE_CCCD",
                        "CCCD đã có hồ sơ. Hãy tìm người bệnh và chọn hồ sơ hiện có.");
            } catch (RuntimeException ex) {
                return Result.failure("PATIENT_REGISTRATION_FAILED", "Không thể lưu đăng ký. Dữ liệu chưa được ghi; hãy thử lại.");
            }
        });
    }

    private static String validate(RegisterPatientCommand command) {
        if (command == null || command.patient() == null) return "Thiếu dữ liệu người bệnh.";
        BenhNhan patient = command.patient();
        if (patient.getHo() == null || patient.getHo().isBlank() || patient.getTen() == null || patient.getTen().isBlank())
            return "Vui lòng nhập họ và tên người bệnh.";
        if (patient.getNgaySinh() == null || patient.getNgaySinh().isAfter(LocalDate.now()))
            return "Ngày sinh không hợp lệ.";
        if (patient.getGioiTinh() == null) return "Vui lòng chọn giới tính.";
        if (command.roomId() <= 0) return "Vui lòng chọn phòng khám.";
        if (command.registrationType() == null || command.registrationType().isBlank()) return "Vui lòng chọn loại đăng ký khám.";
        if (command.payerType() == null) return "Vui lòng chọn đối tượng khám.";
        if (hasInsuranceNumber(command.insurance())) {
            BaoHiem card = command.insurance();
            if (card.getNgayHetHan() == null) return "Vui lòng nhập hạn thẻ BHYT.";
            if (card.getMucHuong() < 0 || card.getMucHuong() > 100) return "Mức hưởng BHYT phải từ 0 đến 100%.";
            if (card.getNgayBatDau() != null && card.getNgayBatDau().isAfter(card.getNgayHetHan()))
                return "Ngày bắt đầu thẻ BHYT phải trước hoặc bằng ngày hết hạn.";
        } else if (command.payerType() == DoiTuongKham.BHYT) {
            return "Đối tượng BHYT cần có số thẻ và hạn thẻ.";
        }
        return null;
    }

    private static boolean hasInsuranceNumber(BaoHiem insurance) {
        return insurance != null && insurance.getSoBaoHiem() != null && !insurance.getSoBaoHiem().isBlank();
    }
}
