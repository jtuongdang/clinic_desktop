package vn.clinicmanagement.application.inventory;

import vn.clinicmanagement.application.audit.AuditActorContext;
import vn.clinicmanagement.application.authorization.AuthorizationContext;
import vn.clinicmanagement.application.common.Command;
import vn.clinicmanagement.domain.common.enums.LoaiItem;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateItemCommand(
        String maItem,
        String tenItem,
        LoaiItem loaiItem,
        String hoatChat,
        String hamLuong,
        String donViCoBan,
        String quyCachDongGoi,
        BigDecimal giaNhapMacDinh,
        BigDecimal giaBanMacDinh,
        BigDecimal tonToiThieu,
        BigDecimal tonToiDa,
        String dangBaoChe,
        String nuocSanXuat,
        String hangSanXuat,
        String soDangKy,
        AuthorizationContext authorizationContext,
        AuditActorContext auditActorContext) implements Command<CreateItemReceipt> {

    public CreateItemCommand(
            String maItem,
            String tenItem,
            LoaiItem loaiItem,
            String hoatChat,
            String hamLuong,
            String donViCoBan,
            String quyCachDongGoi,
            BigDecimal giaNhapMacDinh,
            BigDecimal giaBanMacDinh,
            BigDecimal tonToiThieu,
            BigDecimal tonToiDa,
            String dangBaoChe,
            String nuocSanXuat,
            String hangSanXuat,
            String soDangKy,
            AuthorizationContext authorizationContext) {
        this(maItem, tenItem, loaiItem, hoatChat, hamLuong, donViCoBan, quyCachDongGoi,
                giaNhapMacDinh, giaBanMacDinh, tonToiThieu, tonToiDa, dangBaoChe, nuocSanXuat,
                hangSanXuat, soDangKy, authorizationContext, createAuditActor(authorizationContext));
    }

    private static AuditActorContext createAuditActor(AuthorizationContext context) {
        return context != null && context.isValid()
                ? new AuditActorContext(context.employeeId(), context.roleId(), context.roleCode(),
                        context.facilityId(), context.departmentId(), null, UUID.randomUUID())
                : null;
    }
}
