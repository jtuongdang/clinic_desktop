package vn.clinicmanagement.application.facility;

import vn.clinicmanagement.application.common.Command;

/**
 * Lệnh tạo mới phòng khám / buồng kỹ thuật thuộc Khoa và Cơ sở (FAC-01).
 * Yêu cầu thẩm quyền ADMIN, mã phòng duy nhất, tên không rỗng.
 */
public class CreatePhongCommand implements Command<CreatePhongBienNhan> {
    /** Trường `currentUserRole`: hientai người dùng vai trò. */
    private final String currentUserRole;
    private final String maPhong;
    private final String tenPhong;
    private final int maKhoa;
    private final int maCoSo;
    private final String loaiPhong;
    private final String viTri;

    public CreatePhongCommand(String currentUserRole, String maPhong, String tenPhong,
                             int maKhoa, int maCoSo, String loaiPhong, String viTri) {
        this.currentUserRole = currentUserRole;
        this.maPhong = maPhong;
        this.tenPhong = tenPhong;
        this.maKhoa = maKhoa;
        this.maCoSo = maCoSo;
        this.loaiPhong = loaiPhong;
        this.viTri = viTri;
    }

    public CreatePhongCommand(String currentUserRole, String maPhong, String tenPhong,
                             int maKhoa, int maCoSo, String loaiPhong) {
        this(currentUserRole, maPhong, tenPhong, maKhoa, maCoSo, loaiPhong, null);
    }

    public CreatePhongCommand(String maPhong, String tenPhong, int maKhoa, int maCoSo, String loaiPhong) {
        this("ADMIN", maPhong, tenPhong, maKhoa, maCoSo, loaiPhong, null);
    }

    public CreatePhongCommand(String maPhong, String tenPhong, int maKhoa, int maCoSo, String loaiPhong, String viTri) {
        this("ADMIN", maPhong, tenPhong, maKhoa, maCoSo, loaiPhong, viTri);
    }

    /** Hàm `getCurrentUserRole`: lấy hientai người dùng vai trò. */
    public String getCurrentUserRole() { return currentUserRole; }
    /** Hàm `currentUserRole`: hientai người dùng vai trò. */
    public String currentUserRole() { return currentUserRole; }
    /** Hàm `getActorRole`: lấy tacnhan vai trò. */
    public String getActorRole() { return currentUserRole; }
    /** Hàm `actorRole`: tacnhan vai trò. */
    public String actorRole() { return currentUserRole; }
    /** Hàm `getRole`: lấy vai trò. */
    public String getRole() { return currentUserRole; }
    /** Hàm `role`: vai trò. */
    public String role() { return currentUserRole; }

    /** Hàm `getMaPhong`: lấy mã phòng. */
    public String getMaPhong() { return maPhong; }
    public String maPhong() { return maPhong; }

    /** Hàm `getTenPhong`: lấy `Ten` phòng. */
    public String getTenPhong() { return tenPhong; }
    public String tenPhong() { return tenPhong; }

    /** Hàm `getMaKhoa`: lấy mã khoa. */
    public int getMaKhoa() { return maKhoa; }
    public int maKhoa() { return maKhoa; }

    /** Hàm `getMaCoSo`: lấy mã có so. */
    public int getMaCoSo() { return maCoSo; }
    public int maCoSo() { return maCoSo; }

    /** Hàm `getLoaiPhong`: lấy loai phòng. */
    public String getLoaiPhong() { return loaiPhong; }
    public String loaiPhong() { return loaiPhong; }

    /** Hàm `getViTri`: lấy vi tri. */
    public String getViTri() { return viTri; }
    public String viTri() { return viTri; }
}
