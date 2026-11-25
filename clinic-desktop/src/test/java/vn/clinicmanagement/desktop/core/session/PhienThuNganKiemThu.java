package vn.clinicmanagement.desktop.core.session;

import java.time.Instant;
import java.util.UUID;
import vn.clinicmanagement.application.auth.DaXacThucNguoiDung.RoleChoice;
import vn.clinicmanagement.application.auth.DaXacThucNguoiDung.WorkScopeChoice;

public final class PhienThuNganKiemThu {
    private PhienThuNganKiemThu() {}
    public static void login(String role) {
        var session=NguoiDungPhien.getInstance();
        session.setSession(1,"test","Test",new RoleChoice(1,role,role),new WorkScopeChoice(1,"Test",1,"Test"));
        var now=Instant.now();
        session.setLifecycleMetadata(UUID.randomUUID().toString(),now,now,now.plusSeconds(3600));
    }
}
