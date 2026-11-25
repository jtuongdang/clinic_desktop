package vn.clinicmanagement.infrastructure.persistence.examination;

import java.sql.*;
import java.time.*;
import java.util.*;
import vn.clinicmanagement.domain.examination.*;
import vn.clinicmanagement.domain.common.IUnitOfWork;
import vn.clinicmanagement.domain.common.enums.*;
import vn.clinicmanagement.domain.repositories.*;
import vn.clinicmanagement.infrastructure.persistence.*;

/** Đọc và lưu lượt khám trong giao dịch JDBC; kiểm tra phiên bản để tránh ghi đè hồ sơ. */
public final class LuotKhamRepository implements ILuotKhamRepository {
    private final JdbcKetNoiNhaCungCap connections;
    private final JdbcUnitOfWork unitOfWork;
    public LuotKhamRepository(JdbcKetNoiNhaCungCap connections, JdbcUnitOfWork unitOfWork) {
        this.connections = Objects.requireNonNull(connections);
        this.unitOfWork = Objects.requireNonNull(unitOfWork);
    }
    @Override public Optional<LuotKham> getById(int id) {
        return query("id=?", id).stream().findFirst();
    }
    @Override public List<LuotKham> getAll() { return query("1=1"); }
    @Override public List<LuotKham> getVisitsByDoctor(int doctor, LocalDate date) {
        return query("maBacSi=? AND ngayKham=?", doctor, date);
    }
    @Override public List<LuotKham> find(ITieuChuan<LuotKham> spec) {
        return getAll().stream().filter(v -> spec == null || spec.isSatisfiedBy(v)).toList();
    }
    @Override public boolean exists(ITieuChuan<LuotKham> spec) { return !find(spec).isEmpty(); }
    @Override public int count(ITieuChuan<LuotKham> spec) { return find(spec).size(); }
    @Override public PhanTrangKetQua<LuotKham> getPaged(int page, int size, ITieuChuan<LuotKham> spec) {
        if (page < 1 || size < 1) throw new IllegalArgumentException("Trang và số dòng phải lớn hơn 0.");
        List<LuotKham> rows = find(spec);
        return new PhanTrangKetQua<>(rows.stream().skip((long)(page-1)*size).limit(size).toList(), rows.size(), page, size);
    }
    @Override public IUnitOfWork getUnitOfWork() { return unitOfWork; }
    @Override public void add(LuotKham visit) {
        throw new UnsupportedOperationException("Tạo lượt khám qua nghiệp vụ đăng ký tiếp đón.");
    }
    @Override public void delete(LuotKham visit) {
        throw new UnsupportedOperationException("Hủy lượt khám qua nghiệp vụ hủy tiếp đón.");
    }
    @Override public int getNextQueueNumber(int facility, int department, LocalDate date) {
        throw new UnsupportedOperationException("Cấp số thứ tự qua nghiệp vụ đăng ký tiếp đón.");
    }
    private List<LuotKham> query(String where, Object... params) {
        try (var lease = connections.acquire(); var ps = lease.connection().prepareStatement(
                "SELECT * FROM LuotKham WHERE isDeleted=0 AND " + where)) {
            for (int i=0;i<params.length;i++) ps.setObject(i+1,params[i]);
            List<LuotKham> rows = new ArrayList<>();
            try(var rs=ps.executeQuery()) { while(rs.next()) rows.add(map(rs)); }
            for(var row:rows) {
                try(var q=lease.connection().prepareStatement("SELECT * FROM ChanDoan WHERE maLuotKham=? AND isDeleted=0")) {
                    q.setInt(1,row.getId());
                    try(var rs=q.executeQuery()) { while(rs.next()) {
                        ChanDoan d=new ChanDoan(); d.setId(rs.getInt("id")); d.setMaLuotKham(row.getId());
                        d.setMaICD((Integer)rs.getObject("maICD")); d.setChanDoanText(rs.getString("chanDoanText"));
                        d.setLaChinh(rs.getBoolean("laChinh")); d.setLoaiChanDoan(rs.getString("loaiChanDoan"));
                        row.getChanDoans().add(d);
                    }}
                }
            }
            return rows;
        } catch(SQLException ex) { throw new IllegalStateException("Không thể đọc lượt khám.",ex); }
    }
    private LuotKham map(ResultSet rs) throws SQLException {
        LuotKham v=new LuotKham(); v.setId(rs.getInt("id")); v.setVersion(rs.getLong("version"));
        v.setEmrLocked(rs.getBoolean("emrLocked"));
        v.setMaBenhNhan(rs.getInt("maBenhNhan"));
        v.setMaBacSi((Integer)rs.getObject("maBacSi"));
        v.setMaCoSo(rs.getInt("maCoSo"));
        v.setMaKhoa(rs.getInt("maKhoa"));
        v.setMaPhongKham((Integer)rs.getObject("maPhongKham"));
        v.setHinhThucDieuTri(rs.getString("hinhThucDieuTri") == null ? HinhThucDieuTri.NGOAI_TRU : HinhThucDieuTri.valueOf(rs.getString("hinhThucDieuTri")));
        v.setTrangThai(rs.getString("trangThai") == null ? null : TrangThaiLuotKham.valueOf(rs.getString("trangThai")));
        v.setNgayKham(rs.getDate("ngayKham") == null ? null : rs.getDate("ngayKham").toLocalDate());
        v.setThoiGianBatDauKham(rs.getTimestamp("thoiGianBatDauKham") == null ? null : rs.getTimestamp("thoiGianBatDauKham").toLocalDateTime());
        v.setThoiGianKetThucKham(rs.getTimestamp("thoiGianKetThucKham") == null ? null : rs.getTimestamp("thoiGianKetThucKham").toLocalDateTime());
        v.setThoiGianTiepNhan(rs.getTimestamp("thoiGianTiepNhan") == null ? null : rs.getTimestamp("thoiGianTiepNhan").toLocalDateTime());
        v.setSoThuTuDisplay(rs.getString("soThuTuDisplay"));
        v.setLyDoKham(rs.getString("lyDoKham"));
        v.setLyDoChuyenTuyen(rs.getString("lyDoChuyenTuyen"));
        v.setQuaTrinhBenhLy(rs.getString("quaTrinhBenhLy"));
        v.setTienSuBanThan(rs.getString("tienSuBanThan"));
        v.setTienSuGiaDinh(rs.getString("tienSuGiaDinh"));
        v.setKhamToanThan(rs.getString("khamToanThan"));
        v.setKhamBoPhan(rs.getString("khamBoPhan"));
        v.setTomTatBenhAn(rs.getString("tomTatBenhAn"));
        v.setHuongDieuTri(rs.getString("huongDieuTri"));
        v.setLoiDanBacSi(rs.getString("loiDanBacSi"));
        v.setKetQuaDieuTri(rs.getString("ketQuaDieuTri") == null ? null : KetQuaDieuTri.valueOf(rs.getString("ketQuaDieuTri")));
        v.setHuongXuTri(rs.getString("huongXuTri") == null ? null : HuongXuTri.valueOf(rs.getString("huongXuTri")));
        v.setNgayHenTaiKham(rs.getDate("ngayHenTaiKham") == null ? null : rs.getDate("ngayHenTaiKham").toLocalDate());
        v.setSoNgayNghiHuongBHXH((Integer)rs.getObject("soNgayNghiHuongBHXH"));
        return v;
    }
    @Override public void update(LuotKham v) {
        if (!connections.isTransactionActive()) throw new IllegalStateException("Lưu lượt khám phải nằm trong giao dịch.");
        String sql="UPDATE LuotKham SET maBacSi=?,maKhoa=?,maPhongKham=?,hinhThucDieuTri=?,trangThai=?,thoiGianBatDauKham=?,thoiGianKetThucKham=?,quaTrinhBenhLy=?,tienSuBanThan=?,tienSuGiaDinh=?,khamToanThan=?,khamBoPhan=?,tomTatBenhAn=?,huongDieuTri=?,loiDanBacSi=?,ketQuaDieuTri=?,huongXuTri=?,ngayHenTaiKham=?,soNgayNghiHuongBHXH=?,lyDoChuyenTuyen=?,version=version+1,updatedAt=GETDATE() WHERE id=? AND version=? AND isDeleted=0";
        try(var lease=connections.acquire(); var ps=lease.connection().prepareStatement(sql)) {
            int i=1;
            ps.setObject(i++, v.getMaBacSi());
            ps.setInt(i++, v.getMaKhoa());
            ps.setObject(i++, v.getMaPhongKham());
            ps.setObject(i++, v.getHinhThucDieuTri() == null ? null : v.getHinhThucDieuTri().name());
            ps.setObject(i++, v.getTrangThai() == null ? null : v.getTrangThai().name());
            ps.setObject(i++, v.getThoiGianBatDauKham());
            ps.setObject(i++, v.getThoiGianKetThucKham());
            ps.setObject(i++, v.getQuaTrinhBenhLy());
            ps.setObject(i++, v.getTienSuBanThan());
            ps.setObject(i++, v.getTienSuGiaDinh());
            ps.setObject(i++, v.getKhamToanThan());
            ps.setObject(i++, v.getKhamBoPhan());
            ps.setObject(i++, v.getTomTatBenhAn());
            ps.setObject(i++, v.getHuongDieuTri());
            ps.setObject(i++, v.getLoiDanBacSi());
            ps.setObject(i++, v.getKetQuaDieuTri() == null ? null : v.getKetQuaDieuTri().name());
            ps.setObject(i++, v.getHuongXuTri() == null ? null : v.getHuongXuTri().name());
            ps.setObject(i++, v.getNgayHenTaiKham());
            ps.setObject(i++, v.getSoNgayNghiHuongBHXH());
            ps.setString(i++, v.getLyDoChuyenTuyen());
            ps.setInt(i++,v.getId()); ps.setLong(i,v.getVersion());
            int changed=ps.executeUpdate();
            if(changed!=1) throw new IllegalStateException("Hồ sơ đã được thay đổi bởi người khác. Hãy tải lại.");
            unitOfWork.recordChanges(changed);
            for(ChanDoan d:v.getChanDoans()) {
                boolean insert=d.getId()==null;
                String diagnosisSql=insert
                    ? "INSERT INTO ChanDoan(maLuotKham,maICD,chanDoanText,laChinh,loaiChanDoan) VALUES(?,?,?,?,?)"
                    : "UPDATE ChanDoan SET maICD=?,chanDoanText=?,laChinh=?,loaiChanDoan=?,version=version+1 WHERE id=? AND maLuotKham=? AND isDeleted=0";
                try(var q=lease.connection().prepareStatement(diagnosisSql)) {
                    int j=1; if(insert)q.setInt(j++,v.getId());
                    q.setObject(j++,d.getMaICD()); q.setString(j++,d.getChanDoanText());
                    q.setBoolean(j++,d.isLaChinh()); q.setString(j++,d.getLoaiChanDoan());
                    if(!insert){q.setInt(j++,d.getId());q.setInt(j,v.getId());}
                    if(q.executeUpdate()!=1)throw new IllegalStateException("Không thể lưu chẩn đoán.");
                    unitOfWork.recordChanges(1);
                }
            }
        } catch(SQLException ex){throw new IllegalStateException("Không thể lưu lượt khám.",ex);}
    }
}
