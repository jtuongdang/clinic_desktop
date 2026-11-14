package vn.clinicmanagement.infrastructure.persistence.billing;

import vn.clinicmanagement.domain.billing.ChiTietHoaDon;
import vn.clinicmanagement.domain.billing.CongNoBenhNhan;
import vn.clinicmanagement.domain.billing.HoaDon;
import vn.clinicmanagement.domain.billing.PhieuThu;
import vn.clinicmanagement.domain.common.IUnitOfWork;
import vn.clinicmanagement.domain.common.enums.PhuongThucThanhToan;
import vn.clinicmanagement.domain.common.enums.TrangThaiCongNo;
import vn.clinicmanagement.domain.common.enums.TrangThaiHoaDon;
import vn.clinicmanagement.domain.repositories.IHoaDonRepository;
import vn.clinicmanagement.domain.repositories.ITieuChuan;
import vn.clinicmanagement.domain.repositories.PhanTrangKetQua;
import vn.clinicmanagement.infrastructure.persistence.JdbcKetNoiNhaCungCap;
import vn.clinicmanagement.infrastructure.persistence.JdbcUnitOfWork;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/** SQL Server persistence for invoices and their billing records. */
public final class HoaDonRepository implements IHoaDonRepository {
    private static final String INVOICE_COLUMNS = "SELECT id, soHoaDon, maLuotKham, maBenhNhan, maCoSo, ngayLap, "
            + "tongTien, soTienBaoHiemChiTra, soTienBenhNhanChiTra, daThanhToan, soTienTamUng, soTienGiamGia, "
            + "chietKhauPhanTram, lyDoGiamGia, soTienHoanTra, soHoaDonDienTu, maTraCuuHddt, trangThai, ghiChu, "
            + "version, createdAt, createdBy, updatedAt, updatedBy, isDeleted, deletedAt, deletedBy FROM HoaDon";
    private static final String LINE_COLUMNS = "SELECT id, maHoaDon, loaiDoiTuong, maDoiTuong, moTa, soLuong, donGia, "
            + "version, createdAt, createdBy, updatedAt, updatedBy, isDeleted, deletedAt, deletedBy "
            + "FROM ChiTietHoaDon";
    private static final String RECEIPT_COLUMNS = "SELECT id, maHoaDon, soTien, ngayThu, phuongThuc, nguoiThu, ghiChu, "
            + "version, createdAt, createdBy, updatedAt, updatedBy, isDeleted, deletedAt, deletedBy FROM PhieuThu";
    private static final String DEBT_COLUMNS = "SELECT id, maHoaDon, maBenhNhan, soTienNo, soTienDaTra, hanThanhToan, "
            + "trangThai, version, createdAt, createdBy, updatedAt, updatedBy, isDeleted, deletedAt, deletedBy "
            + "FROM CongNoBenhNhan";

    private final JdbcKetNoiNhaCungCap connections;
    /** Trường `unitOfWork`: đơn vị của làm việc. */
    private final JdbcUnitOfWork unitOfWork;

    public HoaDonRepository(JdbcKetNoiNhaCungCap connections, JdbcUnitOfWork unitOfWork) {
        this.connections = Objects.requireNonNull(connections, "connections");
        this.unitOfWork = Objects.requireNonNull(unitOfWork, "unitOfWork");
    }

    /** Hàm `getById`: lấy theo mã. Gốc khai báo tại `IHoaDonRepository#getById`. */
    @Override
    public Optional<HoaDon> getById(int id) {
        return queryOne(INVOICE_COLUMNS + " WHERE id=? AND isDeleted=0", ps -> ps.setInt(1, id));
    }

    /** Hàm `getBySoHoaDon`: lấy theo so hoa don. Gốc khai báo tại `vn.clinicmanagement.domain.repositories.IHoaDonRepository#getBySoHoaDon`. */
    @Override
    public Optional<HoaDon> getBySoHoaDon(String soHoaDon) {
        if (soHoaDon == null || soHoaDon.isBlank()) return Optional.empty();
        return queryOne(INVOICE_COLUMNS + " WHERE soHoaDon=? AND isDeleted=0",
                ps -> ps.setString(1, soHoaDon.trim()));
    }

    /** Hàm `getByLuotKham`: lấy theo luot kham. Gốc khai báo tại `vn.clinicmanagement.domain.repositories.IHoaDonRepository#getByLuotKham`. */
    @Override
    public Optional<HoaDon> getByLuotKham(int maLuotKham) {
        if (maLuotKham <= 0) return Optional.empty();
        return queryOne(INVOICE_COLUMNS + " WHERE maLuotKham=? AND isDeleted=0 ORDER BY id DESC",
                ps -> ps.setInt(1, maLuotKham));
    }

    /** Hàm `getByBenhNhan`: lấy theo benh nhãn. Gốc khai báo tại `vn.clinicmanagement.domain.repositories.IHoaDonRepository#getByBenhNhan`. */
    @Override
    public List<HoaDon> getByBenhNhan(int maBenhNhan) {
        if (maBenhNhan <= 0) return List.of();
        return queryMany(INVOICE_COLUMNS + " WHERE maBenhNhan=? AND isDeleted=0 ORDER BY ngayLap DESC, id DESC",
                ps -> ps.setInt(1, maBenhNhan));
    }

    /** Hàm `save`: lưu. Gốc khai báo tại `vn.clinicmanagement.domain.repositories.IHoaDonRepository#save`. */
    @Override
    public HoaDon save(HoaDon invoice) {
        Objects.requireNonNull(invoice, "invoice");
        if (invoice.getSoHoaDon() == null || invoice.getSoHoaDon().isBlank()
                || invoice.getMaLuotKham() <= 0 || invoice.getMaBenhNhan() <= 0 || invoice.getMaCoSo() <= 0
                || invoice.getNgayLap() == null || invoice.getTongTien() == null
                || invoice.getSoTienBaoHiemChiTra() == null || invoice.getSoTienBenhNhanChiTra() == null
                || invoice.getDaThanhToan() == null || invoice.getSoTienTamUng() == null
                || invoice.getSoTienGiamGia() == null || invoice.getChietKhauPhanTram() == null
                || invoice.getSoTienHoanTra() == null || invoice.getTrangThai() == null
                || invoice.getChiTietHoaDons() == null) {
            throw new IllegalArgumentException("Invoice number, visit, patient, and facility are required.");
        }
        for (ChiTietHoaDon line : invoice.getChiTietHoaDons()) {
            if (line == null || line.getLoaiDoiTuong() == null || line.getMoTa() == null
                    || line.getSoLuong() == null || line.getDonGia() == null) {
                throw new IllegalArgumentException("Invoice lines require type, description, quantity, and price.");
            }
        }
        withConnection(connection -> {
            if (invoice.getId() == null) insertInvoice(connection, invoice);
            else updateInvoice(connection, invoice);
            replaceLines(connection, invoice);
            recordChange(invoice.getChiTietHoaDons().size() + 1);
            return null;
        });
        return invoice;
    }

    /** Hàm `savePhieuThu`: lưu phiếu thu. Gốc khai báo tại `vn.clinicmanagement.domain.repositories.IHoaDonRepository#savePhieuThu`. */
    @Override
    public void savePhieuThu(PhieuThu receipt) {
        Objects.requireNonNull(receipt, "receipt");
        if (receipt.getMaHoaDon() <= 0 || receipt.getNguoiThu() <= 0 || receipt.getSoTien() == null
                || receipt.getSoTien().signum() <= 0 || receipt.getPhuongThuc() == null) {
            throw new IllegalArgumentException("Receipt invoice, cashier, amount, and method are required.");
        }
        withConnection(connection -> {
            if (receipt.getId() == null) insertReceipt(connection, receipt);
            else updateReceipt(connection, receipt);
            recordChange(1);
            return null;
        });
    }

    /** Hàm `getPhieuThuByHoaDon`: lấy phiếu thu theo hoa don. Gốc khai báo tại `vn.clinicmanagement.domain.repositories.IHoaDonRepository#getPhieuThuByHoaDon`. */
    @Override
    public List<PhieuThu> getPhieuThuByHoaDon(int maHoaDon) {
        if (maHoaDon <= 0) return List.of();
        return queryReceipts(RECEIPT_COLUMNS + " WHERE maHoaDon=? AND isDeleted=0 ORDER BY ngayThu, id",
                ps -> ps.setInt(1, maHoaDon));
    }

    /** Hàm `saveCongNo`: lưu cong no. Gốc khai báo tại `vn.clinicmanagement.domain.repositories.IHoaDonRepository#saveCongNo`. */
    @Override
    public void saveCongNo(CongNoBenhNhan debt) {
        Objects.requireNonNull(debt, "debt");
        if (debt.getMaHoaDon() <= 0 || debt.getMaBenhNhan() <= 0 || debt.getSoTienNo() == null
                || debt.getSoTienDaTra() == null || debt.getHanThanhToan() == null || debt.getTrangThai() == null) {
            throw new IllegalArgumentException("Debt invoice, patient, amounts, due date, and status are required.");
        }
        withConnection(connection -> {
            if (debt.getId() == null) insertDebt(connection, debt);
            else updateDebt(connection, debt);
            recordChange(1);
            return null;
        });
    }

    /** Hàm `getCongNoByHoaDon`: lấy cong no theo hoa don. Gốc khai báo tại `vn.clinicmanagement.domain.repositories.IHoaDonRepository#getCongNoByHoaDon`. */
    @Override
    public Optional<CongNoBenhNhan> getCongNoByHoaDon(int maHoaDon) {
        if (maHoaDon <= 0) return Optional.empty();
        return queryDebts(DEBT_COLUMNS + " WHERE maHoaDon=? AND isDeleted=0 ORDER BY id DESC",
                ps -> ps.setInt(1, maHoaDon)).stream().findFirst();
    }

    /** Hàm `getCongNoByBenhNhan`: lấy cong no theo benh nhãn. Gốc khai báo tại `vn.clinicmanagement.domain.repositories.IHoaDonRepository#getCongNoByBenhNhan`. */
    @Override
    public List<CongNoBenhNhan> getCongNoByBenhNhan(int maBenhNhan) {
        if (maBenhNhan <= 0) return List.of();
        return queryDebts(DEBT_COLUMNS + " WHERE maBenhNhan=? AND isDeleted=0 ORDER BY hanThanhToan, id",
                ps -> ps.setInt(1, maBenhNhan));
    }

    /** Hàm `getAll`: lấy `All`. Gốc khai báo tại `IHoaDonRepository#getAll`. */
    @Override
    public List<HoaDon> getAll() {
        return queryMany(INVOICE_COLUMNS + " WHERE isDeleted=0 ORDER BY id DESC", ps -> {});
    }

    /** Hàm `getPaged`: lấy phân trang. Gốc khai báo tại `IHoaDonRepository#getPaged`. */
    @Override
    public PhanTrangKetQua<HoaDon> getPaged(int pageIndex, int pageSize, ITieuChuan<HoaDon> specification) {
        int page = Math.max(1, pageIndex);
        int size = Math.max(1, Math.min(500, pageSize));
        List<HoaDon> invoices = find(specification);
        int from = (int) Math.min((long) (page - 1) * size, invoices.size());
        int to = Math.min(from + size, invoices.size());
        return new PhanTrangKetQua<>(invoices.subList(from, to), invoices.size(), page, size);
    }

    /** Hàm `find`: tìm. Gốc khai báo tại `IHoaDonRepository#find`. */
    @Override
    public List<HoaDon> find(ITieuChuan<HoaDon> specification) {
        List<HoaDon> invoices = getAll();
        return specification == null ? invoices : invoices.stream().filter(specification.toPredicate()).toList();
    }

    @Override
    public boolean exists(ITieuChuan<HoaDon> specification) {
        return !find(specification).isEmpty();
    }

    /** Hàm `count`: dem. Gốc khai báo tại `IHoaDonRepository#count`. */
    @Override
    public int count(ITieuChuan<HoaDon> specification) {
        return find(specification).size();
    }

    /** Hàm `add`: thêm. Gốc khai báo tại `IHoaDonRepository#add`. */
    @Override
    public void add(HoaDon invoice) {
        if (invoice.getId() != null) throw new IllegalArgumentException("Cannot add an invoice that already has an ID.");
        save(invoice);
    }

    /** Hàm `update`: cập nhật. Gốc khai báo tại `IHoaDonRepository#update`. */
    @Override
    public void update(HoaDon invoice) {
        if (invoice.getId() == null) throw new IllegalArgumentException("Cannot update an invoice without an ID.");
        save(invoice);
    }

    /** Hàm `delete`: xóa. Gốc khai báo tại `IHoaDonRepository#delete`. */
    @Override
    public void delete(HoaDon invoice) {
        if (invoice == null || invoice.getId() == null) return;
        withConnection(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "UPDATE HoaDon SET isDeleted=1, deletedAt=SYSUTCDATETIME(), deletedBy=?, version=version+1 "
                            + "WHERE id=? AND version=? AND isDeleted=0")) {
                ps.setObject(1, invoice.getDeletedBy(), Types.INTEGER);
                ps.setInt(2, invoice.getId());
                ps.setLong(3, version(invoice));
                if (ps.executeUpdate() != 1) throw new IllegalStateException("Invoice changed or no longer exists.");
            }
            invoice.setDeleted(true);
            invoice.setDeletedAt(LocalDateTime.now());
            invoice.setVersion(version(invoice) + 1);
            recordChange(1);
            return null;
        });
    }

    /** Hàm `getUnitOfWork`: lấy đơn vị của làm việc. Gốc khai báo tại `IHoaDonRepository#getUnitOfWork`. */
    @Override
    public IUnitOfWork getUnitOfWork() {
        return unitOfWork;
    }

    /** Hàm `queryOne`: truy vấn `One`. */
    private Optional<HoaDon> queryOne(String sql, SqlBinder binder) {
        return queryMany(sql, binder).stream().findFirst();
    }

    /** Hàm `queryMany`: truy vấn `Many`. */
    private List<HoaDon> queryMany(String sql, SqlBinder binder) {
        List<HoaDon> invoices = query(sql, binder, HoaDonRepository::mapInvoice);
        for (HoaDon invoice : invoices) {
            List<ChiTietHoaDon> lines = queryLines(LINE_COLUMNS + " WHERE maHoaDon=? AND isDeleted=0 ORDER BY id",
                    ps -> ps.setInt(1, invoice.getId()));
            invoice.setChiTietHoaDons(lines);
            lines.forEach(line -> line.setHoaDon(invoice));
        }
        return invoices;
    }

    /** Hàm `queryLines`: truy vấn `Lines`. */
    private List<ChiTietHoaDon> queryLines(String sql, SqlBinder binder) {
        return query(sql, binder, HoaDonRepository::mapLine);
    }

    /** Hàm `queryReceipts`: truy vấn `Receipts`. */
    private List<PhieuThu> queryReceipts(String sql, SqlBinder binder) {
        return query(sql, binder, HoaDonRepository::mapReceipt);
    }

    /** Hàm `queryDebts`: truy vấn `Debts`. */
    private List<CongNoBenhNhan> queryDebts(String sql, SqlBinder binder) {
        return query(sql, binder, HoaDonRepository::mapDebt);
    }

    /** Hàm `query`: truy vấn. */
    private <T> List<T> query(String sql, SqlBinder binder, RowMapper<T> mapper) {
        try (JdbcKetNoiNhaCungCap.Lease lease = connections.acquire();
             PreparedStatement ps = lease.connection().prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                List<T> rows = new ArrayList<>();
                while (rs.next()) rows.add(mapper.map(rs));
                return rows;
            }
        } catch (SQLException exception) {
            throw persistenceFailure(exception);
        }
    }

    /** Hàm `insertInvoice`: chèn hóa đơn. */
    private void insertInvoice(Connection connection, HoaDon invoice) throws SQLException {
        String sql = "INSERT INTO HoaDon (soHoaDon, maLuotKham, maBenhNhan, maCoSo, ngayLap, tongTien, "
                + "soTienBaoHiemChiTra, soTienBenhNhanChiTra, daThanhToan, soTienTamUng, soTienGiamGia, "
                + "chietKhauPhanTram, lyDoGiamGia, soTienHoanTra, soHoaDonDienTu, maTraCuuHddt, trangThai, ghiChu, "
                + "createdBy) OUTPUT INSERTED.id VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            bindInvoice(ps, invoice);
            ps.setObject(19, invoice.getCreatedBy(), Types.INTEGER);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("Invoice insert returned no identity.");
                invoice.setId(rs.getInt(1));
                invoice.setVersion(0L);
            }
        }
    }

    /** Hàm `updateInvoice`: cập nhật hóa đơn. */
    private void updateInvoice(Connection connection, HoaDon invoice) throws SQLException {
        String sql = "UPDATE HoaDon SET soHoaDon=?, maLuotKham=?, maBenhNhan=?, maCoSo=?, ngayLap=?, tongTien=?, "
                + "soTienBaoHiemChiTra=?, soTienBenhNhanChiTra=?, daThanhToan=?, soTienTamUng=?, soTienGiamGia=?, "
                + "chietKhauPhanTram=?, lyDoGiamGia=?, soTienHoanTra=?, soHoaDonDienTu=?, maTraCuuHddt=?, "
                + "trangThai=?, ghiChu=?, updatedBy=?, updatedAt=SYSUTCDATETIME(), version=version+1 "
                + "WHERE id=? AND version=? AND isDeleted=0";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            bindInvoice(ps, invoice);
            ps.setObject(19, invoice.getUpdatedBy(), Types.INTEGER);
            ps.setInt(20, invoice.getId());
            ps.setLong(21, version(invoice));
            if (ps.executeUpdate() != 1) throw new IllegalStateException("Invoice changed or no longer exists.");
            invoice.setVersion(version(invoice) + 1);
            invoice.setUpdatedAt(LocalDateTime.now());
        }
    }

    /** Hàm `bindInvoice`: gắn kết hóa đơn. */
    private static void bindInvoice(PreparedStatement ps, HoaDon invoice) throws SQLException {
        ps.setString(1, invoice.getSoHoaDon().trim());
        ps.setInt(2, invoice.getMaLuotKham());
        ps.setInt(3, invoice.getMaBenhNhan());
        ps.setInt(4, invoice.getMaCoSo());
        ps.setTimestamp(5, timestamp(invoice.getNgayLap()));
        ps.setBigDecimal(6, invoice.getTongTien());
        ps.setBigDecimal(7, invoice.getSoTienBaoHiemChiTra());
        ps.setBigDecimal(8, invoice.getSoTienBenhNhanChiTra());
        ps.setBigDecimal(9, invoice.getDaThanhToan());
        ps.setBigDecimal(10, invoice.getSoTienTamUng());
        ps.setBigDecimal(11, invoice.getSoTienGiamGia());
        ps.setBigDecimal(12, invoice.getChietKhauPhanTram());
        ps.setString(13, invoice.getLyDoGiamGia());
        ps.setBigDecimal(14, invoice.getSoTienHoanTra());
        ps.setString(15, invoice.getSoHoaDonDienTu());
        ps.setString(16, invoice.getMaTraCuuHddt());
        ps.setString(17, invoice.getTrangThai().name());
        ps.setString(18, invoice.getGhiChu());
    }

    private void replaceLines(Connection connection, HoaDon invoice) throws SQLException {
        if (invoice.getId() == null) throw new IllegalStateException("Invoice identity was not assigned.");
        if (invoice.getChiTietHoaDons() == null) throw new IllegalArgumentException("Invoice lines must not be null.");
        if (invoice.getId() != null && version(invoice) > 0) {
            try (PreparedStatement ps = connection.prepareStatement(
                    "DELETE FROM ChiTietHoaDon WHERE maHoaDon=?")) {
                ps.setInt(1, invoice.getId());
                ps.executeUpdate();
            }
        }
        String sql = "INSERT INTO ChiTietHoaDon (maHoaDon, loaiDoiTuong, maDoiTuong, moTa, soLuong, donGia, createdBy) "
                + "OUTPUT INSERTED.id VALUES (?, ?, ?, ?, ?, ?, ?)";
        for (ChiTietHoaDon line : invoice.getChiTietHoaDons()) {
            Objects.requireNonNull(line, "invoice line");
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setInt(1, invoice.getId());
                ps.setString(2, line.getLoaiDoiTuong());
                ps.setInt(3, line.getMaDoiTuong());
                ps.setString(4, line.getMoTa());
                ps.setBigDecimal(5, line.getSoLuong());
                ps.setBigDecimal(6, line.getDonGia());
                ps.setObject(7, line.getCreatedBy(), Types.INTEGER);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) throw new SQLException("Invoice line insert returned no identity.");
                    line.setId(rs.getInt(1));
                    line.setMaHoaDon(invoice.getId());
                    line.setHoaDon(invoice);
                }
            }
        }
    }

    /** Hàm `insertReceipt`: chèn biên nhận. */
    private static void insertReceipt(Connection connection, PhieuThu receipt) throws SQLException {
        String sql = "INSERT INTO PhieuThu (maHoaDon, soTien, ngayThu, phuongThuc, nguoiThu, ghiChu, createdBy) "
                + "OUTPUT INSERTED.id VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, receipt.getMaHoaDon());
            ps.setBigDecimal(2, receipt.getSoTien());
            ps.setTimestamp(3, timestamp(receipt.getNgayThu()));
            ps.setString(4, receipt.getPhuongThuc().name());
            ps.setInt(5, receipt.getNguoiThu());
            ps.setString(6, receipt.getGhiChu());
            ps.setObject(7, receipt.getCreatedBy(), Types.INTEGER);
            setGeneratedId(ps, receipt::setId, "Receipt");
        }
    }

    /** Hàm `updateReceipt`: cập nhật biên nhận. */
    private static void updateReceipt(Connection connection, PhieuThu receipt) throws SQLException {
        String sql = "UPDATE PhieuThu SET soTien=?, ngayThu=?, phuongThuc=?, nguoiThu=?, ghiChu=?, updatedBy=?, "
                + "updatedAt=SYSUTCDATETIME(), version=version+1 WHERE id=? AND version=? AND isDeleted=0";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setBigDecimal(1, receipt.getSoTien());
            ps.setTimestamp(2, timestamp(receipt.getNgayThu()));
            ps.setString(3, receipt.getPhuongThuc().name());
            ps.setInt(4, receipt.getNguoiThu());
            ps.setString(5, receipt.getGhiChu());
            ps.setObject(6, receipt.getUpdatedBy(), Types.INTEGER);
            ps.setInt(7, receipt.getId());
            ps.setLong(8, version(receipt));
            if (ps.executeUpdate() != 1) throw new IllegalStateException("Receipt changed or no longer exists.");
            receipt.setVersion(version(receipt) + 1);
        }
    }

    /** Hàm `insertDebt`: chèn congno. */
    private static void insertDebt(Connection connection, CongNoBenhNhan debt) throws SQLException {
        String sql = "INSERT INTO CongNoBenhNhan (maHoaDon, maBenhNhan, soTienNo, soTienDaTra, hanThanhToan, "
                + "trangThai, createdBy) OUTPUT INSERTED.id VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, debt.getMaHoaDon());
            ps.setInt(2, debt.getMaBenhNhan());
            ps.setBigDecimal(3, debt.getSoTienNo());
            ps.setBigDecimal(4, debt.getSoTienDaTra());
            ps.setObject(5, debt.getHanThanhToan());
            ps.setString(6, debt.getTrangThai().name());
            ps.setObject(7, debt.getCreatedBy(), Types.INTEGER);
            setGeneratedId(ps, debt::setId, "Debt");
        }
    }

    /** Hàm `updateDebt`: cập nhật congno. */
    private static void updateDebt(Connection connection, CongNoBenhNhan debt) throws SQLException {
        String sql = "UPDATE CongNoBenhNhan SET maBenhNhan=?, soTienNo=?, soTienDaTra=?, hanThanhToan=?, "
                + "trangThai=?, updatedBy=?, updatedAt=SYSUTCDATETIME(), version=version+1 "
                + "WHERE id=? AND version=? AND isDeleted=0";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, debt.getMaBenhNhan());
            ps.setBigDecimal(2, debt.getSoTienNo());
            ps.setBigDecimal(3, debt.getSoTienDaTra());
            ps.setObject(4, debt.getHanThanhToan());
            ps.setString(5, debt.getTrangThai().name());
            ps.setObject(6, debt.getUpdatedBy(), Types.INTEGER);
            ps.setInt(7, debt.getId());
            ps.setLong(8, version(debt));
            if (ps.executeUpdate() != 1) throw new IllegalStateException("Debt changed or no longer exists.");
            debt.setVersion(version(debt) + 1);
        }
    }

    /** Hàm `setGeneratedId`: gán `Generated` mã. */
    private static void setGeneratedId(PreparedStatement ps, IdSetter setter, String recordName) throws SQLException {
        try (ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) throw new SQLException(recordName + " insert returned no identity.");
            setter.set(rs.getInt(1));
        }
    }

    /** Hàm `recordChange`: ghi nhận `Change`. */
    private void recordChange(int rows) {
        if (connections.isTransactionActive()) unitOfWork.recordChanges(rows);
    }

    /** Hàm `withConnection`: voi kết nối. */
    private <T> T withConnection(SqlWork<T> work) {
        try (JdbcKetNoiNhaCungCap.Lease lease = connections.acquire()) {
            return work.execute(lease.connection());
        } catch (SQLException exception) {
            throw persistenceFailure(exception);
        }
    }

    /** Hàm `mapInvoice`: ánh xạ hóa đơn. */
    private static HoaDon mapInvoice(ResultSet rs) throws SQLException {
        HoaDon invoice = new HoaDon();
        invoice.setId(rs.getInt("id"));
        invoice.setSoHoaDon(rs.getString("soHoaDon"));
        invoice.setMaLuotKham(rs.getInt("maLuotKham"));
        invoice.setMaBenhNhan(rs.getInt("maBenhNhan"));
        invoice.setMaCoSo(rs.getInt("maCoSo"));
        invoice.setNgayLap(localDateTime(rs, "ngayLap"));
        invoice.setTongTien(rs.getBigDecimal("tongTien"));
        invoice.setSoTienBaoHiemChiTra(rs.getBigDecimal("soTienBaoHiemChiTra"));
        invoice.setSoTienBenhNhanChiTra(rs.getBigDecimal("soTienBenhNhanChiTra"));
        invoice.setDaThanhToan(rs.getBigDecimal("daThanhToan"));
        invoice.setSoTienTamUng(rs.getBigDecimal("soTienTamUng"));
        invoice.setSoTienGiamGia(rs.getBigDecimal("soTienGiamGia"));
        invoice.setChietKhauPhanTram(rs.getBigDecimal("chietKhauPhanTram"));
        invoice.setLyDoGiamGia(rs.getString("lyDoGiamGia"));
        invoice.setSoTienHoanTra(rs.getBigDecimal("soTienHoanTra"));
        invoice.setSoHoaDonDienTu(rs.getString("soHoaDonDienTu"));
        invoice.setMaTraCuuHddt(rs.getString("maTraCuuHddt"));
        invoice.setTrangThai(enumValue(TrangThaiHoaDon.class, rs.getString("trangThai")));
        invoice.setGhiChu(rs.getString("ghiChu"));
        mapAudit(rs, invoice);
        return invoice;
    }

    /** Hàm `mapLine`: ánh xạ đóng. */
    private static ChiTietHoaDon mapLine(ResultSet rs) throws SQLException {
        ChiTietHoaDon line = new ChiTietHoaDon();
        line.setId(rs.getInt("id"));
        line.setMaHoaDon(rs.getInt("maHoaDon"));
        line.setLoaiDoiTuong(rs.getString("loaiDoiTuong"));
        line.setMaDoiTuong(rs.getInt("maDoiTuong"));
        line.setMoTa(rs.getString("moTa"));
        line.setSoLuong(rs.getBigDecimal("soLuong"));
        line.setDonGia(rs.getBigDecimal("donGia"));
        mapAudit(rs, line);
        return line;
    }

    /** Hàm `mapReceipt`: ánh xạ biên nhận. */
    private static PhieuThu mapReceipt(ResultSet rs) throws SQLException {
        PhieuThu receipt = new PhieuThu();
        receipt.setId(rs.getInt("id"));
        receipt.setMaHoaDon(rs.getInt("maHoaDon"));
        receipt.setSoTien(rs.getBigDecimal("soTien"));
        receipt.setNgayThu(localDateTime(rs, "ngayThu"));
        receipt.setPhuongThuc(enumValue(PhuongThucThanhToan.class, rs.getString("phuongThuc")));
        receipt.setNguoiThu(rs.getInt("nguoiThu"));
        receipt.setGhiChu(rs.getString("ghiChu"));
        mapAudit(rs, receipt);
        return receipt;
    }

    /** Hàm `mapDebt`: ánh xạ congno. */
    private static CongNoBenhNhan mapDebt(ResultSet rs) throws SQLException {
        CongNoBenhNhan debt = new CongNoBenhNhan();
        debt.setId(rs.getInt("id"));
        debt.setMaHoaDon(rs.getInt("maHoaDon"));
        debt.setMaBenhNhan(rs.getInt("maBenhNhan"));
        debt.setSoTienNo(rs.getBigDecimal("soTienNo"));
        debt.setSoTienDaTra(rs.getBigDecimal("soTienDaTra"));
        java.sql.Date dueDate = rs.getDate("hanThanhToan");
        debt.setHanThanhToan(dueDate == null ? null : dueDate.toLocalDate());
        debt.setTrangThai(enumValue(TrangThaiCongNo.class, rs.getString("trangThai")));
        mapAudit(rs, debt);
        return debt;
    }

    /** Hàm `mapAudit`: ánh xạ kiểm toán. */
    private static void mapAudit(ResultSet rs, vn.clinicmanagement.domain.common.CoSoThucThe<Integer> entity)
            throws SQLException {
        entity.setVersion(rs.getLong("version"));
        entity.setCreatedAt(localDateTime(rs, "createdAt"));
        entity.setCreatedBy((Integer) rs.getObject("createdBy"));
        entity.setUpdatedAt(localDateTime(rs, "updatedAt"));
        entity.setUpdatedBy((Integer) rs.getObject("updatedBy"));
        entity.setDeleted(rs.getBoolean("isDeleted"));
        entity.setDeletedAt(localDateTime(rs, "deletedAt"));
        entity.setDeletedBy((Integer) rs.getObject("deletedBy"));
    }

    /** Hàm `enumValue`: `enum` giá trị. */
    private static <E extends Enum<E>> E enumValue(Class<E> type, String value) {
        if (value == null) return null;
        return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT));
    }

    /** Hàm `localDateTime`: `local` ngày thời gian. */
    private static LocalDateTime localDateTime(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toLocalDateTime();
    }

    private static Timestamp timestamp(LocalDateTime value) {
        return value == null ? null : Timestamp.valueOf(value);
    }

    private static long version(vn.clinicmanagement.domain.common.CoSoThucThe<Integer> entity) {
        return entity.getVersion() == null ? 0L : entity.getVersion();
    }

    /** Hàm `persistenceFailure`: luutru thất bại. */
    private static RuntimeException persistenceFailure(SQLException exception) {
        return new IllegalStateException("Không thể lưu hoặc truy vấn dữ liệu hóa đơn.", exception);
    }

    @FunctionalInterface private interface SqlBinder {
        /** Hàm `bind`: gắn kết. */
        void bind(PreparedStatement statement) throws SQLException;
    }
    @FunctionalInterface private interface RowMapper<T> {
        /** Hàm `map`: ánh xạ. */
        T map(ResultSet resultSet) throws SQLException;
    }
    @FunctionalInterface private interface SqlWork<T> {
        T execute(Connection connection) throws SQLException;
    }
    @FunctionalInterface private interface IdSetter {
        /** Hàm `set`: gán. */
        void set(Integer id);
    }
}
