package vn.clinicmanagement.application.examination;

import vn.clinicmanagement.application.audit.KiemToanHanhDongMa;
import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.audit.KiemToanKetQua;
import vn.clinicmanagement.application.audit.KiemToanLyDoMa;
import vn.clinicmanagement.application.audit.KiemToanTaiNguyenLoai;
import vn.clinicmanagement.application.audit.KiemToanWriteCongKetNoi;
import vn.clinicmanagement.application.audit.HeThongKiemToanNhapLieu;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.CommandHandler;
import vn.clinicmanagement.application.common.KetQua;
import vn.clinicmanagement.application.common.GiaoDichRanhGioi;
import vn.clinicmanagement.domain.common.enums.TrangThaiLuotKham;
import vn.clinicmanagement.domain.examination.LuotKham;
import vn.clinicmanagement.domain.examination.TransferLyDoMa;
import vn.clinicmanagement.domain.facility.PhongKham;
import vn.clinicmanagement.domain.repositories.ILuotKhamRepository;
import vn.clinicmanagement.domain.repositories.IPhongKhamRepository;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class TransferLuotKhamHandler implements CommandHandler<TransferLuotKhamCommand, TransferLuotKhamBienNhan> {
    /** Trường `visitRepository`: luotkham kho dữ liệu. */
    private final ILuotKhamRepository visitRepository;
    /** Trường `roomRepository`: phòng kho dữ liệu. */
    private final IPhongKhamRepository roomRepository;
    private final GiaoDichRanhGioi transactions;
    /** Trường `audit`: kiểm toán. */
    private final KiemToanWriteCongKetNoi audit;

    public TransferLuotKhamHandler(ILuotKhamRepository visitRepository,
                                IPhongKhamRepository roomRepository,
                                GiaoDichRanhGioi transactions,
                                KiemToanWriteCongKetNoi audit) {
        this.visitRepository = Objects.requireNonNull(visitRepository, "visitRepository");
        this.roomRepository = Objects.requireNonNull(roomRepository, "roomRepository");
        this.transactions = Objects.requireNonNull(transactions, "transactions");
        this.audit = Objects.requireNonNull(audit, "audit");
    }

    @Override
    public KetQua<TransferLuotKhamBienNhan> handle(TransferLuotKhamCommand command) {
        if (command == null) {
            return KetQua.failure("COMMAND_INVALID", "Yêu cầu chuyển phòng không hợp lệ.");
        }

        PhanQuyenNguCanh context = command.context() != null ? command.context() : command.authorizationContext();
        TransferLyDoMa reasonCode = command.reasonCode();

        if (context == null || !context.isValid() || reasonCode == null || !reasonCode.isEligibleRole(context.roleCode())) {
            return deny(context, command.auditActorContext(), KiemToanLyDoMa.AUTH_FORBIDDEN);
        }

        Optional<LuotKham> visitOpt = visitRepository.getById(command.visitId());
        if (visitOpt.isEmpty()) {
            return KetQua.failure("NOT_FOUND", "Không tìm thấy lượt khám.");
        }
        LuotKham visit = visitOpt.get();

        if (visit.getTrangThai() != TrangThaiLuotKham.CHO_KHAM && visit.getTrangThai() != TrangThaiLuotKham.DANG_KHAM) {
            return KetQua.failure("INVALID_STATE", "Lượt khám không ở trạng thái có thể chuyển phòng.");
        }

        Optional<PhongKham> roomOpt = roomRepository.getById(command.destinationRoomId());
        if (roomOpt.isEmpty()) {
            return KetQua.failure("ROOM_UNAVAILABLE", "Phòng khám đích không hợp lệ hoặc không hoạt động.");
        }
        PhongKham destinationRoom = roomOpt.get();
        if (!destinationRoom.isHoatDong()
                || destinationRoom.getKhoa() == null
                || destinationRoom.getKhoa().getCoSo() == null
                || destinationRoom.getKhoa().getCoSo().getId() == null
                || destinationRoom.getKhoa().getCoSo().getId() != context.facilityId()) {
            return KetQua.failure("ROOM_UNAVAILABLE", "Phòng khám đích không hợp lệ hoặc không hoạt động.");
        }

        if (visit.getMaPhongKham() != null && visit.getMaPhongKham().intValue() == command.destinationRoomId()) {
            return KetQua.failure("SAME_ROOM", "Phòng khám đích trùng với phòng khám hiện tại.");
        }

        int previousRoomId = visit.getMaPhongKham() != null ? visit.getMaPhongKham() : 0;

        return transactions.inTransaction(() -> {
            try {
                visit.setMaPhongKham(command.destinationRoomId());
                applyTransferReason(visit, reasonCode, command.transferNote());
                visitRepository.update(visit);

                int departmentId = (destinationRoom.getKhoa() != null
                        && destinationRoom.getKhoa().getId() != null
                        && destinationRoom.getKhoa().getId() > 0)
                        ? destinationRoom.getKhoa().getId()
                        : context.departmentId();

                audit.appendInCurrentTransaction(new HeThongKiemToanNhapLieu(
                        context.employeeId(),
                        context.roleId(),
                        context.roleCode(),
                        context.facilityId(),
                        departmentId,
                        KiemToanHanhDongMa.VISIT_TRANSFERRED,
                        KiemToanTaiNguyenLoai.VISIT,
                        command.destinationRoomId(),
                        KiemToanKetQua.SUCCESS,
                        null,
                        Instant.now(),
                        command.auditActorContext() != null ? command.auditActorContext().workstationId() : null,
                        command.auditActorContext() != null ? command.auditActorContext().correlationId() : UUID.randomUUID()));

                return KetQua.success(new TransferLuotKhamBienNhan(
                        visit.getId() != null ? visit.getId() : command.visitId(),
                        previousRoomId,
                        command.destinationRoomId(),
                        reasonCode,
                        command.transferNote()));
            } catch (RuntimeException ex) {
                return KetQua.failure("TRANSFER_FAILED", "Không thể hoàn tất chuyển phòng khám: " + ex.getMessage());
            }
        });
    }

    /** Hàm `applyTransferReason`: `apply` chuyển lý do. */
    private static void applyTransferReason(LuotKham visit, TransferLyDoMa reasonCode, String transferNote) {
        String formattedReason = reasonCode.name();
        if (transferNote != null && !transferNote.isBlank()) {
            formattedReason += ": " + transferNote.trim();
        }
        String currentReason = visit.getLyDoChuyenTuyen();
        if (currentReason == null || currentReason.isBlank()) {
            visit.setLyDoChuyenTuyen(formattedReason);
        } else {
            visit.setLyDoChuyenTuyen(currentReason + "; " + formattedReason);
        }
    }

    private KetQua<TransferLuotKhamBienNhan> deny(PhanQuyenNguCanh context,
                                              KiemToanTacNhanNguCanh auditActor,
                                              KiemToanLyDoMa reasonCode) {
        if (context != null && context.isValid()) {
            try {
                audit.appendIndependently(new HeThongKiemToanNhapLieu(
                        context.employeeId(),
                        context.roleId(),
                        context.roleCode(),
                        context.facilityId(),
                        context.departmentId(),
                        KiemToanHanhDongMa.AUTH_ACCESS_DENIED,
                        null,
                        null,
                        KiemToanKetQua.DENIED,
                        reasonCode,
                        Instant.now(),
                        auditActor != null ? auditActor.workstationId() : null,
                        auditActor != null ? auditActor.correlationId() : UUID.randomUUID()));
            } catch (RuntimeException ignored) {
                // Denial stands even if audit write throws
            }
        }
        return KetQua.failure("AUTHORIZATION_DENIED", "Không được phép thực hiện chuyển phòng khám.");
    }
}
