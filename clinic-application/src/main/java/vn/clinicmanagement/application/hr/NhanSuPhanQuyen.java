package vn.clinicmanagement.application.hr;

/** Resolves administrative permission from the authenticated application context. */
@FunctionalInterface
public interface NhanSuPhanQuyen {
    /** Hàm `canManageEmployees`: có thể `Manage` nhân viên. */
    boolean canManageEmployees(int actorId);
}
