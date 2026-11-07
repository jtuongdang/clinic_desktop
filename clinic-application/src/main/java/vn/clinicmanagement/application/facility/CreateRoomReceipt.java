package vn.clinicmanagement.application.facility;

import java.time.LocalDateTime;

/**
 * Biên nhận kết quả tạo mới phòng khám / buồng kỹ thuật (FAC-01).
 */
public class CreateRoomReceipt {
    private final int roomId;
    private final String maPhong;
    private final String tenPhong;
    private final int maKhoa;
    private final int maCoSo;
    private final String loaiPhong;
    private final String viTri;
    private final boolean hoatDong;
    private final LocalDateTime createdAt;
    private final String message;

    public CreateRoomReceipt(int roomId, String maPhong, String tenPhong, int maKhoa, int maCoSo,
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

    public CreateRoomReceipt(int roomId, String maPhong, String tenPhong, int maKhoa, int maCoSo,
                             String loaiPhong, String viTri, boolean hoatDong) {
        this(roomId, maPhong, tenPhong, maKhoa, maCoSo, loaiPhong, viTri, hoatDong, LocalDateTime.now(), "Tạo phòng khám thành công.");
    }

    public CreateRoomReceipt(int roomId, String maPhong, String tenPhong, int maKhoa, int maCoSo,
                             String loaiPhong) {
        this(roomId, maPhong, tenPhong, maKhoa, maCoSo, loaiPhong, null, true, LocalDateTime.now(), "Tạo phòng khám thành công.");
    }

    public int getRoomId() { return roomId; }
    public int roomId() { return roomId; }
    public int getId() { return roomId; }
    public int id() { return roomId; }

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

    public boolean isHoatDong() { return hoatDong; }
    public boolean hoatDong() { return hoatDong; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime createdAt() { return createdAt; }

    public String getMessage() { return message; }
    public String message() { return message; }
}
