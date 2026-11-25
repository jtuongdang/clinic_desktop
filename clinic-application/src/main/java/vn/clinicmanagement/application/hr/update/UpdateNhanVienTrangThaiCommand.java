package vn.clinicmanagement.application.hr.update;

import vn.clinicmanagement.application.common.Command;

public record UpdateNhanVienTrangThaiCommand(int actorId, int employeeId, boolean active)
        implements Command<UpdateNhanVienTrangThaiBienNhan> {}
