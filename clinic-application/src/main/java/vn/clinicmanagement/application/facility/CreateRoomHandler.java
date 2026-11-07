package vn.clinicmanagement.application.facility;

import vn.clinicmanagement.application.common.CommandHandler;
import vn.clinicmanagement.application.common.Result;
import vn.clinicmanagement.application.common.TransactionBoundary;
import vn.clinicmanagement.domain.facility.PhongKham;
import vn.clinicmanagement.domain.repositories.IPhongKhamRepository;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Xử lý lệnh tạo mới phòng khám / buồng kỹ thuật (FAC-01).
 * Xác thực thẩm quyền ADMIN, tính duy nhất của mã phòng và tính hợp lệ của tên/khoa/cơ sở.
 */
public final class CreateRoomHandler implements CommandHandler<CreateRoomCommand, CreateRoomReceipt> {
    private final IPhongKhamRepository phongKhamRepository;
    private final TransactionBoundary transactionBoundary;

    public CreateRoomHandler(IPhongKhamRepository phongKhamRepository) {
        this(phongKhamRepository, null);
    }

    public CreateRoomHandler(IPhongKhamRepository phongKhamRepository, TransactionBoundary transactionBoundary) {
        this.phongKhamRepository = Objects.requireNonNull(phongKhamRepository, "phongKhamRepository");
        this.transactionBoundary = transactionBoundary;
    }

    @Override
    public Result<CreateRoomReceipt> handle(CreateRoomCommand command) {
        if (command == null) {
            return Result.failure("ROOM_INVALID", "Lệnh tạo phòng khám không được để trống.");
        }

        // 1. Kiểm tra thẩm quyền (Chỉ ADMIN)
        String role = command.getCurrentUserRole();
        if (role == null || role.isBlank() || !role.trim().equalsIgnoreCase("ADMIN")) {
            return Result.failure("AUTHZ_DENIED", "Chỉ Quản trị viên (ADMIN) mới có quyền tạo phòng khám.");
        }

        // 2. Kiểm tra mã phòng không rỗng
        if (command.getMaPhong() == null || command.getMaPhong().trim().isEmpty()) {
            return Result.failure("ROOM_CODE_REQUIRED", "Mã phòng không được để trống.");
        }
        String maPhong = command.getMaPhong().trim();

        // 3. Kiểm tra tên phòng không rỗng
        if (command.getTenPhong() == null || command.getTenPhong().trim().isEmpty()) {
            return Result.failure("ROOM_NAME_REQUIRED", "Tên phòng không được để trống.");
        }
        String tenPhong = command.getTenPhong().trim();

        // 4. Kiểm tra phân cấp Khoa & Cơ sở
        if (command.getMaKhoa() <= 0) {
            return Result.failure("ROOM_DEPARTMENT_INVALID", "Khoa trực thuộc không hợp lệ.");
        }
        if (command.getMaCoSo() <= 0) {
            return Result.failure("ROOM_FACILITY_INVALID", "Cơ sở trực thuộc không hợp lệ.");
        }

        // 5. Kiểm tra mã phòng duy nhất
        if (phongKhamRepository.existsByMaPhong(maPhong)) {
            return Result.failure("ROOM_CODE_DUPLICATE", "Mã phòng khám đã tồn tại trong hệ thống: " + maPhong);
        }

        // 6. Khởi tạo đối tượng PhongKham miền
        PhongKham room = new PhongKham();
        room.setMaPhong(maPhong);
        room.setMaKhoa(command.getMaKhoa());
        room.setTenPhong(tenPhong);
        room.setViTri(command.getViTri() != null && !command.getViTri().isBlank()
                ? command.getViTri().trim()
                : command.getLoaiPhong());
        room.setHoatDong(true);

        if (transactionBoundary != null) {
            return transactionBoundary.inTransaction(() -> persistAndBuildReceipt(room, command, maPhong, tenPhong));
        }

        return persistAndBuildReceipt(room, command, maPhong, tenPhong);
    }

    private Result<CreateRoomReceipt> persistAndBuildReceipt(PhongKham room, CreateRoomCommand command,
                                                            String maPhong, String tenPhong) {
        phongKhamRepository.add(room);
        int generatedId = room.getId() != null && room.getId() > 0 ? room.getId() : 1;

        CreateRoomReceipt receipt = new CreateRoomReceipt(
                generatedId,
                maPhong,
                tenPhong,
                command.getMaKhoa(),
                command.getMaCoSo(),
                command.getLoaiPhong(),
                room.getViTri(),
                room.isHoatDong(),
                LocalDateTime.now(),
                "Tạo phòng khám thành công."
        );
        return Result.success(receipt);
    }
}
