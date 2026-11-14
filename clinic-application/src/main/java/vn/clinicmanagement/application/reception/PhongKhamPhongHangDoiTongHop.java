package vn.clinicmanagement.application.reception;

/** Trường `roomId`: phòng mã. */
/** Trường `roomName`: phòng tên. */
/** Trường `departmentName`: khoa tên. */
public record PhongKhamPhongHangDoiTongHop(int roomId, String roomName, String departmentName,
                                     /** Trường `registeredCount`: dadangky dem. */
                                     /** Trường `waitingCount`: `waiting` dem. */
                                     /** Trường `examiningCount`: `examining` dem. */
                                     int registeredCount, int waitingCount, int examiningCount) {}
