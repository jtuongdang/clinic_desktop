package vn.clinicmanagement.application.hr;

import vn.clinicmanagement.application.common.Command;

/** Trường `actorId`: tacnhan mã. */
/** Trường `employeeId`: nhân viên mã. */
/** Trường `active`: hoạt động. */
public record UpdateNhanVienTrangThaiCommand(int actorId, int employeeId, boolean active)
        implements Command<UpdateNhanVienTrangThaiBienNhan> {}
