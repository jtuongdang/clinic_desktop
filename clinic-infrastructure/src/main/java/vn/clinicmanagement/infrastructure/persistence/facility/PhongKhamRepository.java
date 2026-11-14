package vn.clinicmanagement.infrastructure.persistence.facility;

import vn.clinicmanagement.domain.facility.CoSo;
import vn.clinicmanagement.domain.facility.Khoa;
import vn.clinicmanagement.domain.facility.PhongKham;
import vn.clinicmanagement.domain.repositories.IPhongKhamRepository;
import vn.clinicmanagement.infrastructure.persistence.JdbcKetNoiNhaCungCap;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Reads active clinic rooms together with their department and facility for reception registration. */
public final class PhongKhamRepository implements IPhongKhamRepository {
    private static final String ROOM_SELECT = "SELECT p.id roomId, p.maKhoa, p.tenPhong roomName, p.viTri, p.hoatDong roomActive, "
            + "p.version roomVersion, p.createdAt roomCreatedAt, p.createdBy roomCreatedBy, p.updatedAt roomUpdatedAt, "
            + "p.updatedBy roomUpdatedBy, p.isDeleted roomDeleted, p.deletedAt roomDeletedAt, p.deletedBy roomDeletedBy, "
            + "k.id khoaId, k.maCoSo, k.tenKhoa, k.moTa khoaMoTa, k.hoatDong khoaActive, k.version khoaVersion, "
            + "k.isDeleted khoaDeleted, c.id coSoId, c.tenCoSo, c.diaChi coSoDiaChi, c.hoatDong coSoActive "
            + "FROM PhongKham p INNER JOIN Khoa k ON k.id=p.maKhoa "
            + "INNER JOIN CoSo c ON c.id=k.maCoSo";

    private final JdbcKetNoiNhaCungCap connections;

    public PhongKhamRepository(JdbcKetNoiNhaCungCap connections) { this.connections = connections; }

    /** Hàm `getById`: lấy theo mã. Gốc khai báo tại `vn.clinicmanagement.domain.repositories.IPhongKhamRepository#getById`. */
    @Override
    public Optional<PhongKham> getById(int id) {
        return query(ROOM_SELECT + " WHERE p.id=? AND p.hoatDong=1 AND p.isDeleted=0 AND k.hoatDong=1 AND k.isDeleted=0 "
                + "AND c.hoatDong=1 AND c.isDeleted=0", ps -> ps.setInt(1, id)).stream().findFirst();
    }

    /** Hàm `getActiveRooms`: lấy hoạt động phòng. Gốc khai báo tại `vn.clinicmanagement.domain.repositories.IPhongKhamRepository#getActiveRooms`. */
    @Override
    public List<PhongKham> getActiveRooms() {
        return query(ROOM_SELECT + " WHERE p.hoatDong=1 AND p.isDeleted=0 AND k.hoatDong=1 AND k.isDeleted=0 "
                + "AND c.hoatDong=1 AND c.isDeleted=0 ORDER BY c.id, k.id, p.tenPhong", ps -> {});
    }

    /** Hàm `getActiveRoomsInScope`: lấy hoạt động phòng trong phamvi. Gốc khai báo tại `vn.clinicmanagement.domain.repositories.IPhongKhamRepository#getActiveRoomsInScope`. */
    @Override
    public List<PhongKham> getActiveRoomsInScope(int facilityId, int departmentId) {
        if (facilityId <= 0 || departmentId <= 0) return List.of();
        return query(scopedActiveRoomsSql(), ps -> {
            ps.setInt(1, facilityId);
            ps.setInt(2, departmentId);
        });
    }

    /** Hàm `scopedActiveRoomsSql`: `scoped` hoạt động phòng sql. */
    static String scopedActiveRoomsSql() {
        return ROOM_SELECT + " WHERE p.hoatDong=1 AND p.isDeleted=0 AND k.hoatDong=1 AND k.isDeleted=0 "
                + "AND c.hoatDong=1 AND c.isDeleted=0 AND k.maCoSo=? AND k.id=? "
                + "ORDER BY p.tenPhong";
    }

    /** Hàm `query`: truy vấn. */
    private List<PhongKham> query(String sql, Binder binder) {
        try (JdbcKetNoiNhaCungCap.Lease lease = connections.acquire();
             PreparedStatement ps = lease.connection().prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                List<PhongKham> rooms = new ArrayList<>();
                while (rs.next()) rooms.add(mapRoom(rs));
                return rooms;
            }
        } catch (SQLException ex) { throw new IllegalStateException("Lỗi truy cập danh mục phòng khám.", ex); }
    }

    /** Hàm `mapRoom`: ánh xạ phòng. */
    private static PhongKham mapRoom(ResultSet rs) throws SQLException {
        PhongKham room = new PhongKham();
        room.setId(rs.getInt("roomId"));
        room.setMaKhoa(rs.getInt("maKhoa"));
        room.setTenPhong(rs.getString("roomName"));
        room.setViTri(rs.getString("viTri"));
        room.setHoatDong(rs.getBoolean("roomActive"));
        room.setVersion(rs.getLong("roomVersion"));
        room.setCreatedAt(localDateTime(rs, "roomCreatedAt"));
        room.setCreatedBy((Integer) rs.getObject("roomCreatedBy"));
        room.setUpdatedAt(localDateTime(rs, "roomUpdatedAt"));
        room.setUpdatedBy((Integer) rs.getObject("roomUpdatedBy"));
        room.setDeleted(rs.getBoolean("roomDeleted"));
        room.setDeletedAt(localDateTime(rs, "roomDeletedAt"));
        room.setDeletedBy((Integer) rs.getObject("roomDeletedBy"));

        Khoa department = new Khoa();
        department.setId(rs.getInt("khoaId"));
        department.setMaCoSo(rs.getInt("maCoSo"));
        department.setTenKhoa(rs.getString("tenKhoa"));
        department.setMoTa(rs.getString("khoaMoTa"));
        department.setHoatDong(rs.getBoolean("khoaActive"));
        department.setVersion(rs.getLong("khoaVersion"));
        department.setDeleted(rs.getBoolean("khoaDeleted"));

        CoSo facility = new CoSo();
        facility.setId(rs.getInt("coSoId"));
        facility.setTenCoSo(rs.getString("tenCoSo"));
        facility.setDiaChi(rs.getString("coSoDiaChi"));
        facility.setHoatDong(rs.getBoolean("coSoActive"));
        department.setCoSo(facility);
        room.setKhoa(department);
        return room;
    }

    /** Hàm `localDateTime`: `local` ngày thời gian. */
    private static LocalDateTime localDateTime(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column); return value == null ? null : value.toLocalDateTime();
    }

    /** Hàm `bind`: gắn kết. */
    @FunctionalInterface private interface Binder { void bind(PreparedStatement statement) throws SQLException; }
}
