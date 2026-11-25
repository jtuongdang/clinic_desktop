package vn.clinicmanagement.desktop.modules.reports.catalog;

import vn.clinicmanagement.infrastructure.persistence.CoSoDuLieuKetNoi;

import java.awt.GraphicsEnvironment;
import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;

/**
 * Lưu trữ danh mục mẫu báo cáo:
 * - Trong phiên ứng dụng thật: đồng bộ trực tiếp bảng `dbo.DanhMucBaoCao` trong SQL Server.
 * - Khi truyền `Path` cụ thể (hoặc chạy test headless): lưu/đọc file XML cục bộ.
 */
public final class BaoCaoDanhMucLuuTru {
    private final Path path;
    private final boolean databaseBacked;

    public BaoCaoDanhMucLuuTru() {
        this.path = Path.of(System.getProperty("user.home"), ".clinic-management", "report-catalog.xml");
        this.databaseBacked = !GraphicsEnvironment.isHeadless()
                && System.getProperty("surefire.test.class.path") == null;
    }

    public BaoCaoDanhMucLuuTru(Path path) {
        this.path = path;
        this.databaseBacked = false;
    }

    public List<BaoCaoMau> load() throws IOException {
        if (databaseBacked) {
            try {
                return loadFromDatabase();
            } catch (SQLException ex) {
                // Fallback sang XML nếu mất kết nối DB tạm thời
            }
        }
        return loadFromXml();
    }

    public void save(List<BaoCaoMau> entries) throws IOException {
        if (databaseBacked) {
            try {
                saveToDatabase(entries);
                return;
            } catch (SQLException ex) {
                throw new IOException("Không thể lưu danh mục báo cáo xuống CSDL.", ex);
            }
        }
        saveToXml(entries);
    }

    private List<BaoCaoMau> loadFromDatabase() throws SQLException, IOException {
        try (Connection c = CoSoDuLieuKetNoi.getConnection()) {
            ensureTableAndSeed(c);
            List<BaoCaoMau> rows = new ArrayList<>();
            String sql = "SELECT reportKey, danhMuc, nhomBaoCao, tenBaoCao, tenTepTin, maBaoCao, duongDanMau "
                    + "FROM dbo.DanhMucBaoCao WHERE isDeleted = 0 ORDER BY thuTu ASC, id ASC";
            try (PreparedStatement ps = c.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new BaoCaoMau(
                            rs.getString("reportKey"),
                            rs.getString("danhMuc"),
                            rs.getString("nhomBaoCao"),
                            rs.getString("tenBaoCao"),
                            Objects.toString(rs.getString("tenTepTin"), ""),
                            Objects.toString(rs.getString("maBaoCao"), ""),
                            Objects.toString(rs.getString("duongDanMau"), "")
                    ));
                }
            }
            return rows;
        }
    }

    private void saveToDatabase(List<BaoCaoMau> entries) throws SQLException, IOException {
        try (Connection c = CoSoDuLieuKetNoi.getConnection()) {
            ensureTableAndSeed(c);
            c.setAutoCommit(false);
            try {
                String upsertSql = "IF EXISTS (SELECT 1 FROM dbo.DanhMucBaoCao WHERE reportKey = ?) "
                        + "UPDATE dbo.DanhMucBaoCao SET danhMuc = ?, nhomBaoCao = ?, tenBaoCao = ?, "
                        + "tenTepTin = ?, maBaoCao = ?, duongDanMau = ?, thuTu = ?, isDeleted = 0, updatedAt = SYSDATETIME() "
                        + "WHERE reportKey = ? "
                        + "ELSE "
                        + "INSERT INTO dbo.DanhMucBaoCao (reportKey, danhMuc, nhomBaoCao, tenBaoCao, tenTepTin, maBaoCao, duongDanMau, thuTu) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement ps = c.prepareStatement(upsertSql)) {
                    for (int i = 0; i < entries.size(); i++) {
                        BaoCaoMau e = entries.get(i);
                        ps.setString(1, e.key());
                        ps.setString(2, e.category());
                        ps.setString(3, e.group());
                        ps.setString(4, e.name());
                        ps.setString(5, e.fileName());
                        ps.setString(6, e.reportId());
                        ps.setString(7, e.localPath());
                        ps.setInt(8, i);
                        ps.setString(9, e.key());

                        ps.setString(10, e.key());
                        ps.setString(11, e.category());
                        ps.setString(12, e.group());
                        ps.setString(13, e.name());
                        ps.setString(14, e.fileName());
                        ps.setString(15, e.reportId());
                        ps.setString(16, e.localPath());
                        ps.setInt(17, i);
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
                c.commit();
            } catch (SQLException ex) {
                c.rollback();
                throw ex;
            }
        }
    }

    private static void ensureTableAndSeed(Connection c) throws SQLException, IOException {
        try (Statement st = c.createStatement()) {
            st.execute("IF OBJECT_ID('dbo.DanhMucBaoCao', 'U') IS NULL "
                    + "CREATE TABLE dbo.DanhMucBaoCao ("
                    + "id INT IDENTITY(1,1) PRIMARY KEY, "
                    + "reportKey NVARCHAR(120) NOT NULL UNIQUE, "
                    + "danhMuc NVARCHAR(200) NOT NULL, "
                    + "nhomBaoCao NVARCHAR(200) NOT NULL, "
                    + "tenBaoCao NVARCHAR(250) NOT NULL, "
                    + "tenTepTin NVARCHAR(250) NULL, "
                    + "maBaoCao NVARCHAR(80) NULL, "
                    + "duongDanMau NVARCHAR(500) NULL, "
                    + "thuTu INT NOT NULL DEFAULT 0, "
                    + "isDeleted BIT NOT NULL DEFAULT 0, "
                    + "createdAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(), "
                    + "updatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME()"
                    + ")");
        }
        int count = 0;
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM dbo.DanhMucBaoCao WHERE isDeleted = 0")) {
            if (rs.next()) count = rs.getInt(1);
        }
        if (count == 0) {
            List<BaoCaoMau> seed = defaults();
            String insertSql = "INSERT INTO dbo.DanhMucBaoCao (reportKey, danhMuc, nhomBaoCao, tenBaoCao, tenTepTin, maBaoCao, duongDanMau, thuTu) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps = c.prepareStatement(insertSql)) {
                for (int i = 0; i < seed.size(); i++) {
                    BaoCaoMau e = seed.get(i);
                    ps.setString(1, e.key());
                    ps.setString(2, e.category());
                    ps.setString(3, e.group());
                    ps.setString(4, e.name());
                    ps.setString(5, e.fileName());
                    ps.setString(6, e.reportId());
                    ps.setString(7, e.localPath());
                    ps.setInt(8, i);
                    ps.addBatch();
                }
                ps.executeBatch();
            }
        }
    }

    private List<BaoCaoMau> loadFromXml() throws IOException {
        if (!Files.exists(path)) return defaults();
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(path)) {
            p.loadFromXML(in);
        }
        List<BaoCaoMau> result = new ArrayList<>();
        int count;
        try {
            count = Integer.parseInt(p.getProperty("count"));
        } catch (RuntimeException ex) {
            throw new IOException("Danh mục không hợp lệ.", ex);
        }
        if (count < 0 || count > 10000) throw new IOException("Số mục không hợp lệ.");
        for (int i = 0; i < count; i++) {
            String k = i + ".";
            try {
                result.add(new BaoCaoMau(
                        p.getProperty(k + "key"),
                        p.getProperty(k + "category"),
                        p.getProperty(k + "group"),
                        p.getProperty(k + "name"),
                        p.getProperty(k + "file"),
                        p.getProperty(k + "id"),
                        p.getProperty(k + "path")
                ));
            } catch (IllegalArgumentException ex) {
                throw new IOException("Mục báo cáo không hợp lệ.", ex);
            }
        }
        return result;
    }

    private void saveToXml(List<BaoCaoMau> entries) throws IOException {
        Properties p = new Properties();
        p.setProperty("count", String.valueOf(entries.size()));
        for (int i = 0; i < entries.size(); i++) {
            var entry = entries.get(i);
            String k = i + ".";
            p.setProperty(k + "key", entry.key());
            p.setProperty(k + "category", entry.category());
            p.setProperty(k + "group", entry.group());
            p.setProperty(k + "name", entry.name());
            p.setProperty(k + "file", entry.fileName());
            p.setProperty(k + "id", entry.reportId());
            p.setProperty(k + "path", entry.localPath());
        }
        Path parent = path.toAbsolutePath().getParent();
        Files.createDirectories(parent);
        Path temp = Files.createTempFile(parent, "report-catalog-", ".xml");
        try {
            try (OutputStream out = Files.newOutputStream(temp)) {
                p.storeToXML(out, "Danh mục mẫu báo cáo trên máy này", "UTF-8");
            }
            try {
                Files.move(temp, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    public static List<BaoCaoMau> defaults() throws IOException {
        var stream = BaoCaoDanhMucLuuTru.class.getResourceAsStream("/reports/catalog.tsv");
        if (stream == null) throw new IOException("Không tìm thấy cấu hình danh mục báo cáo.");
        List<BaoCaoMau> entries = new ArrayList<>();
        try (var reader = new BufferedReader(new InputStreamReader(stream, java.nio.charset.StandardCharsets.UTF_8))) {
            String line;
            int index = 0;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank() || line.startsWith("#")) continue;
                String[] c = line.split("\t", -1);
                if (c.length != 5) throw new IOException("Cấu hình danh mục báo cáo không hợp lệ.");
                entries.add(new BaoCaoMau(
                        "builtin-" + (index++),
                        c[0],
                        c[1],
                        c[2].equals("SO_001_SoKhamBenh.xls") ? "Sổ khám bệnh1" : c[1],
                        c[2],
                        c[3],
                        c[4]
                ));
            }
        }
        return entries;
    }
}
