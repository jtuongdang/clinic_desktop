package vn.clinicmanagement.application.reception;

public record PhongKhamPhongHangDoiTongHop(int roomId, String roomName, String departmentName,
                                     int registeredCount, int waitingCount, int examiningCount) {}
