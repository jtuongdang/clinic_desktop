package vn.clinicmanagement.application.facility;

import vn.clinicmanagement.application.common.Command;

/**
 * Lệnh tạo mới phòng khám / buồng kỹ thuật thuộc Khoa và Cơ sở (FAC-01).
 * Yêu cầu thẩm quyền ADMIN, mã phòng duy nhất, tên không rỗng.
 */
public class CreateRoomCommand implements Command<CreateRoomReceipt> {
    private final String currentUserRole;
    private final String maPhong;
    private final String tenPhong;
    private final int maKhoa;
    private final int maCoSo;
    private final String loaiPhong;
    private final String viTri;

    public CreateRoomCommand(String currentUserRole, String maPhong, String tenPhong,
                             int maKhoa, int maCoSo, String loaiPhong, String viTri) {
        this.currentUserRole = currentUserRole;
        this.maPhong = maPhong;
        this.tenPhong = tenPhong;
        this.maKhoa = maKhoa;
        this.maCoSo = maCoSo;
        this.loaiPhong = loaiPhong;
        this.viTri = viTri;
    }

    public CreateRoomCommand(String currentUserRole, String maPhong, String tenPhong,
                             int maKhoa, int maCoSo, String loaiPhong) {
        this(currentUserRole, maPhong, tenPhong, maKhoa, maCoSo, loaiPhong, null);
    }

    public CreateRoomCommand(String maPhong, String tenPhong, int maKhoa, int maCoSo, String loaiPhong) {
        this("ADMIN", maPhong, tenPhong, maKhoa, maCoSo, loaiPhong, null);
    }

    public CreateRoomCommand(String maPhong, String tenPhong, int maKhoa, int maCoSo, String loaiPhong, String viTri) {
        this("ADMIN", maPhong, tenPhong, maKhoa, maCoSo, loaiPhong, viTri);
    }

    public String getCurrentUserRole() { return currentUserRole; }
    public String currentUserRole() { return currentUserRole; }
    public String getActorRole() { return currentUserRole; }
    public String actorRole() { return currentUserRole; }
    public String getRole() { return currentUserRole; }
    public String role() { return currentUserRole; }

    public String getMaPhong() { return maPhong; }
    public String maPhong() { return maPhong; }

    public String getTenPhong() { return tenPhong; }
    public String tenPhong() { return tenPhong; }

    public int getMaKhoa() { return maKhoa; }
    public int maKhoa() { return maKhoa; }

    public int getMaCoSo() { return maCoSo; }
    public int maCoSo() { return maCoSo; }

    public String getLoaiPhong() { return loaiPhong; }
    public String loaiPhong() { return loaiPhong; }

    public String getViTri() { return viTri; }
    public String viTri() { return viTri; }
}
