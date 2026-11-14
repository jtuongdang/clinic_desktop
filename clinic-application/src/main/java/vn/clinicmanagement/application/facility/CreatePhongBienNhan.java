package vn.clinicmanagement.application.facility;

import java.time.LocalDateTime;

/**
 * Biên nhận kết quả tạo mới phòng khám / buồng kỹ thuật (FAC-01).
 */
public class CreatePhongBienNhan {
    /** Trường `roomId`: phòng mã. */
    private final int roomId;
    private final String maPhong;
    private final String tenPhong;
    private final int maKhoa;
    private final int maCoSo;
    private final String loaiPhong;
    private final String viTri;
    private final boolean hoatDong;
    /** Trường `createdAt`: datao luc. */
    private final LocalDateTime createdAt;
    /** Trường `message`: thông báo. */
    private final String message;

    public CreatePhongBienNhan(int roomId, String maPhong, String tenPhong, int maKhoa, int maCoSo,
                             String loaiPhong, String viTri, boolean hoatDong, LocalDateTime createdAt,
                             String message) {
        this.roomId = roomId;
        this.maPhong = maPhong;
        this.tenPhong = tenPhong;
        this.maKhoa = maKhoa;
        this.maCoSo = maCoSo;
        this.loaiPhong = loaiPhong;
        this.viTri = viTri;
        this.hoatDong = hoatDong;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.message = message != null ? message : "Tạo phòng khám thành công.";
    }

    public CreatePhongBienNhan(int roomId, String maPhong, String tenPhong, int maKhoa, int maCoSo,
                             String loaiPhong, String viTri, boolean hoatDong) {
        this(roomId, maPhong, tenPhong, maKhoa, maCoSo, loaiPhong, viTri, hoatDong, LocalDateTime.now(), "Tạo phòng khám thành công.");
    }

    public CreatePhongBienNhan(int roomId, String maPhong, String tenPhong, int maKhoa, int maCoSo,
                             String loaiPhong) {
        this(roomId, maPhong, tenPhong, maKhoa, maCoSo, loaiPhong, null, true, LocalDateTime.now(), "Tạo phòng khám thành công.");
    }

    /** Hàm `getRoomId`: lấy phòng mã. */
    public int getRoomId() { return roomId; }
    /** Hàm `roomId`: phòng mã. */
    public int roomId() { return roomId; }
    /** Hàm `getId`: lấy mã. */
    public int getId() { return roomId; }
    /** Hàm `id`: mã. */
    public int id() { return roomId; }

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

    /** Hàm `isHoatDong`: là `Hoat` đóng. */
    public boolean isHoatDong() { return hoatDong; }
    public boolean hoatDong() { return hoatDong; }

    /** Hàm `getCreatedAt`: lấy datao luc. */
    public LocalDateTime getCreatedAt() { return createdAt; }
    /** Hàm `createdAt`: datao luc. */
    public LocalDateTime createdAt() { return createdAt; }

    /** Hàm `getMessage`: lấy thông báo. */
    public String getMessage() { return message; }
    /** Hàm `message`: thông báo. */
    public String message() { return message; }
}
