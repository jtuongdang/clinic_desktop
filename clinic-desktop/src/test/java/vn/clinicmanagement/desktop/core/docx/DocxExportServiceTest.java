package vn.clinicmanagement.desktop.core.docx;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import vn.clinicmanagement.domain.common.enums.DoiTuongKham;
import vn.clinicmanagement.domain.common.enums.GioiTinh;
import vn.clinicmanagement.domain.common.enums.HuongXuTri;
import vn.clinicmanagement.domain.common.enums.KetQuaDieuTri;
import vn.clinicmanagement.domain.examination.ChanDoan;
import vn.clinicmanagement.domain.examination.LuotKham;
import vn.clinicmanagement.domain.inventory.Item;
import vn.clinicmanagement.domain.patient.BenhNhan;
import vn.clinicmanagement.domain.prescription.ChiTietDonThuoc;
import vn.clinicmanagement.domain.prescription.DonThuoc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.*;

class DocxExportServiceTest {

    private DocxExportService exportService;

    @BeforeEach
    void setUp() {
        exportService = new DocxExportService();
    }

    private byte[] exportToBytes(DocxDocument doc) throws IOException {
        return doc.toByteArray();
    }

    private File exportToFile(DocxDocument doc, Path tempDir, String fileName) throws IOException {
        File file = tempDir.resolve(fileName).toFile();
        doc.saveToFile(file);
        return file;
    }

    private String assertValidZipAndExtractDocumentXml(byte[] zipBytes) throws IOException {
        assertNotNull(zipBytes, "Generated DOCX byte array must not be null");
        assertTrue(zipBytes.length > 0, "Generated DOCX byte array must not be empty");

        Set<String> entryNames = new HashSet<>();
        String documentXml = null;

        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                entryNames.add(entry.getName());
                if ("word/document.xml".equals(entry.getName())) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = zis.read(buffer)) != -1) {
                        baos.write(buffer, 0, len);
                    }
                    documentXml = baos.toString(StandardCharsets.UTF_8);
                }
                zis.closeEntry();
            }
        }

        assertTrue(entryNames.contains("[Content_Types].xml"), "Package must contain [Content_Types].xml");
        assertTrue(entryNames.contains("_rels/.rels"), "Package must contain _rels/.rels");
        assertTrue(entryNames.contains("word/_rels/document.xml.rels"), "Package must contain word/_rels/document.xml.rels");
        assertTrue(entryNames.contains("word/styles.xml"), "Package must contain word/styles.xml");
        assertTrue(entryNames.contains("word/document.xml"), "Package must contain word/document.xml");
        assertNotNull(documentXml, "word/document.xml must exist in the DOCX package");

        return documentXml;
    }

    private String assertValidZipAndExtractDocumentXml(File file) throws IOException {
        assertNotNull(file, "File must not be null");
        assertTrue(file.exists(), "Exported DOCX file must exist on disk");
        assertTrue(file.length() > 0, "Exported DOCX file must not be empty");

        try (InputStream in = new FileInputStream(file)) {
            byte[] bytes = in.readAllBytes();
            return assertValidZipAndExtractDocumentXml(bytes);
        }
    }

    @Nested
    @DisplayName("buildPrescriptionDocx tests: null guards, byte export, and file export")
    class PrescriptionExportTests {

        @Test
        @DisplayName("exportToBytes with null parameters produces non-empty valid ZIP with default fallback values")
        void exportToBytesWithNullGuardsProducesValidZipWithFallbacks() throws IOException {
            DocxDocument doc = exportService.buildPrescriptionDocx(null, null, null, null, null);
            assertNotNull(doc);

            byte[] bytes = exportToBytes(doc);
            String xml = assertValidZipAndExtractDocumentXml(bytes);

            assertTrue(xml.contains("PHÒNG KHÁM ĐA KHOA QUỐC TẾ EHC"));
            assertTrue(xml.contains("ĐƠN THUỐC ĐIỆN TỬ"));
            assertTrue(xml.contains("Nguyễn Văn A"));
            assertTrue(xml.contains("Nam"));
            assertTrue(xml.contains("Paracetamol 500mg"));
            assertTrue(xml.contains("Amoxicillin 500mg"));
            assertTrue(xml.contains("BS. Điều Trị"));
            assertTrue(xml.contains("Chẩn đoán xác định"));
        }

        @Test
        @DisplayName("exportToFile with null parameters writes valid DOCX file using @TempDir")
        void exportToFileWithNullGuardsCreatesFileInTempDir(@TempDir Path tempDir) throws IOException {
            DocxDocument doc = exportService.buildPrescriptionDocx(null, null, null, null, null);

            File file = exportToFile(doc, tempDir, "prescription_null_guards.docx");
            String xml = assertValidZipAndExtractDocumentXml(file);

            assertTrue(xml.contains("ĐƠN THUỐC ĐIỆN TỬ"));
            assertTrue(xml.contains("PHÒNG KHÁM ĐA KHOA QUỐC TẾ EHC"));
        }

        @Test
        @DisplayName("exportToBytes with populated patient, visit, and prescription incorporates custom clinical data")
        void exportToBytesWithPopulatedDataIncorporatesClinicalData() throws IOException {
            BenhNhan patient = new BenhNhan();
            patient.setId(101);
            patient.setHo("Trần");
            patient.setTen("Văn Bình");
            patient.setNgaySinh(LocalDate.of(1985, 5, 20));
            patient.setGioiTinh(GioiTinh.M);
            patient.setDiaChi("456 Nguyễn Huệ, Quận 1, TP.HCM");
            patient.setSoCCCD("079085001234");
            patient.setMaBenhNhanFormat("BN-2026-00101");

            LuotKham visit = new LuotKham();
            ChanDoan chanDoan = new ChanDoan();
            chanDoan.setChanDoanText("Viêm dạ dày ruột cấp tính");
            visit.getChanDoans().add(chanDoan);

            DonThuoc prescription = new DonThuoc();
            prescription.setLoiDanDonThuoc("Uống sau bữa ăn 30 phút, kiêng chất kích thích.");

            ChiTietDonThuoc ct1 = new ChiTietDonThuoc();
            Item item1 = new Item();
            item1.setTenItem("Omeprazole 20mg");
            item1.setDonViCoBan("Viên");
            ct1.setItem(item1);
            ct1.setSoLuongKe(new BigDecimal("28"));
            ct1.setDuongDung("Uống");
            ct1.setLieuDung("1 viên/lần");
            ct1.setHuongDan("Sáng 1 viên trước ăn 30 phút");
            prescription.getChiTietDonThuocs().add(ct1);

            ChiTietDonThuoc ct2 = new ChiTietDonThuoc();
            ct2.setMaItem(999);
            ct2.setItem(null);
            ct2.setSoLuongKe(new BigDecimal("10"));
            ct2.setDuongDung(null);
            ct2.setLieuDung(null);
            ct2.setHuongDan(null);
            prescription.getChiTietDonThuocs().add(ct2);

            String clinicName = "Phòng Khám Đa Khoa An Bình";
            String doctorName = "BS. CKI Lê Hoàng";

            DocxDocument doc = exportService.buildPrescriptionDocx(patient, visit, prescription, clinicName, doctorName);
            byte[] bytes = exportToBytes(doc);
            String xml = assertValidZipAndExtractDocumentXml(bytes);

            assertTrue(xml.contains("PHÒNG KHÁM ĐA KHOA AN BÌNH"));
            assertTrue(xml.contains("Trần Văn Bình"));
            assertTrue(xml.contains("BN-2026-00101"));
            assertTrue(xml.contains("079085001234"));
            assertTrue(xml.contains("456 Nguyễn Huệ"));
            assertTrue(xml.contains("Viêm dạ dày ruột cấp tính"));
            assertTrue(xml.contains("Omeprazole 20mg"));
            assertTrue(xml.contains("Thuốc #999"));
            assertTrue(xml.contains("Uống sau bữa ăn 30 phút, kiêng chất kích thích."));
            assertTrue(xml.contains("BS. CKI Lê Hoàng"));
        }

        @Test
        @DisplayName("exportToFile with populated data creates valid non-empty file on disk using @TempDir")
        void exportToFileWithPopulatedDataCreatesFileInTempDir(@TempDir Path tempDir) throws IOException {
            BenhNhan patient = new BenhNhan();
            patient.setId(202);
            patient.setHo("Lê");
            patient.setTen("Thị Lan");
            patient.setMaBenhNhanFormat("BN-2026-00202");

            LuotKham visit = new LuotKham();
            DonThuoc prescription = new DonThuoc();

            DocxDocument doc = exportService.buildPrescriptionDocx(patient, visit, prescription, "Bệnh Viện Sài Gòn", "BS. Minh");
            File file = exportToFile(doc, tempDir, "prescription_custom.docx");

            String xml = assertValidZipAndExtractDocumentXml(file);
            assertTrue(xml.contains("BỆNH VIỆN SÀI GÒN"));
            assertTrue(xml.contains("Lê Thị Lan"));
            assertTrue(xml.contains("BS. Minh"));
        }
    }

    @Nested
    @DisplayName("buildOutpatientMedicalRecordDocx tests: null guards, byte export, and file export")
    class OutpatientMedicalRecordExportTests {

        @Test
        @DisplayName("exportToBytes with null parameters produces non-empty valid ZIP with default fallback values")
        void exportToBytesWithNullGuardsProducesValidZipWithFallbacks() throws IOException {
            DocxDocument doc = exportService.buildOutpatientMedicalRecordDocx(null, null, null, null);
            assertNotNull(doc);

            byte[] bytes = exportToBytes(doc);
            String xml = assertValidZipAndExtractDocumentXml(bytes);

            assertTrue(xml.contains("PHÒNG KHÁM ĐA KHOA QUỐC TẾ EHC"));
            assertTrue(xml.contains("BỆNH ÁN NGOẠI TRÚ"));
            assertTrue(xml.contains("NGUYỄN VĂN A"));
            assertTrue(xml.contains("Viện phí"));
            assertTrue(xml.contains("BS. Điều Trị"));
            assertTrue(xml.contains("Chẩn đoán xác định"));
        }

        @Test
        @DisplayName("exportToFile with null parameters writes valid DOCX file using @TempDir")
        void exportToFileWithNullGuardsCreatesFileInTempDir(@TempDir Path tempDir) throws IOException {
            DocxDocument doc = exportService.buildOutpatientMedicalRecordDocx(null, null, null, null);

            File file = exportToFile(doc, tempDir, "emr_null_guards.docx");
            String xml = assertValidZipAndExtractDocumentXml(file);

            assertTrue(xml.contains("BỆNH ÁN NGOẠI TRÚ"));
            assertTrue(xml.contains("PHÒNG KHÁM ĐA KHOA QUỐC TẾ EHC"));
        }

        @Test
        @DisplayName("exportToBytes with populated clinical record includes all expected sections")
        void exportToBytesWithPopulatedDataIncorporatesSections() throws IOException {
            BenhNhan patient = new BenhNhan();
            patient.setHo("Nguyễn");
            patient.setTen("Thị Mai");
            patient.setNgaySinh(LocalDate.of(1992, 8, 15));
            patient.setGioiTinh(GioiTinh.F);
            patient.setDanToc("Kinh");
            patient.setNgheNghiep("Kế toán");
            patient.setDiaChi("789 CMT8, Quận 3, TP.HCM");

            LuotKham visit = new LuotKham();
            visit.setDoiTuongKham(DoiTuongKham.BHYT);
            visit.setLyDoKham("Đau tức ngực trái kéo dài");
            visit.setQuaTrinhBenhLy("Bệnh khởi phát 3 ngày trước với cơn đau nhói.");
            visit.setTienSuBanThan("Tăng huyết áp 2 năm.");
            visit.setTienSuGiaDinh("Gia đình khỏe mạnh.");
            visit.setKhamToanThan("Bệnh nhân tỉnh táo, tiếp xúc tốt.");
            visit.setKhamBoPhan("Tim đều, T1 T2 rõ.");
            visit.setTomTatBenhAn("Bệnh nhân nữ 34 tuổi, đau tức ngực.");
            visit.setHuongDieuTri("Điều trị ngoại trú theo đơn thuốc.");
            visit.setKetQuaDieuTri(KetQuaDieuTri.DO_GIAM);
            visit.setHuongXuTri(HuongXuTri.CHO_VE_TOA);

            ChanDoan cd = new ChanDoan();
            cd.setChanDoanText("Cơn đau thắt ngực ổn định");
            visit.getChanDoans().add(cd);

            String clinicName = "Bệnh Viện EHC Sài Gòn";
            String doctorName = "ThS. BS. Vũ Đức Cường";

            DocxDocument doc = exportService.buildOutpatientMedicalRecordDocx(patient, visit, clinicName, doctorName);
            byte[] bytes = exportToBytes(doc);
            String xml = assertValidZipAndExtractDocumentXml(bytes);

            assertTrue(xml.contains("BỆNH VIỆN EHC SÀI GÒN"));
            assertTrue(xml.contains("NGUYỄN THỊ MAI"));
            assertTrue(xml.contains("15/08/1992"));
            assertTrue(xml.contains("Nữ"));
            assertTrue(xml.contains("Kinh"));
            assertTrue(xml.contains("Kế toán"));
            assertTrue(xml.contains("789 CMT8"));
            assertTrue(xml.contains(DoiTuongKham.BHYT.getDescription()));
            assertTrue(xml.contains("Đau tức ngực trái kéo dài"));
            assertTrue(xml.contains("Bệnh khởi phát 3 ngày trước"));
            assertTrue(xml.contains("Tăng huyết áp 2 năm."));
            assertTrue(xml.contains("Cơn đau thắt ngực ổn định"));
            assertTrue(xml.contains("ThS. BS. Vũ Đức Cường"));
        }

        @Test
        @DisplayName("exportToFile with populated EMR creates valid file in @TempDir")
        void exportToFileWithPopulatedEmrCreatesFileInTempDir(@TempDir Path tempDir) throws IOException {
            BenhNhan patient = new BenhNhan();
            patient.setHo("Hoàng");
            patient.setTen("Văn Nam");

            LuotKham visit = new LuotKham();
            visit.setLyDoKham("Khám tổng quát");

            DocxDocument doc = exportService.buildOutpatientMedicalRecordDocx(patient, visit, "Phòng Khám X", "BS. Y");
            File file = exportToFile(doc, tempDir, "emr_record.docx");

            String xml = assertValidZipAndExtractDocumentXml(file);
            assertTrue(xml.contains("PHÒNG KHÁM X"));
            assertTrue(xml.contains("HOÀNG VĂN NAM"));
            assertTrue(xml.contains("Khám tổng quát"));
        }
    }

    @Nested
    @DisplayName("buildAppointmentSlipDocx tests: null guards, byte export, and file export")
    class AppointmentSlipExportTests {

        @Test
        @DisplayName("exportToBytes with null parameters produces non-empty valid ZIP with default fallback values")
        void exportToBytesWithNullGuardsProducesValidZipWithFallbacks() throws IOException {
            DocxDocument doc = exportService.buildAppointmentSlipDocx(null, null, null, null);
            assertNotNull(doc);

            byte[] bytes = exportToBytes(doc);
            String xml = assertValidZipAndExtractDocumentXml(bytes);

            assertTrue(xml.contains("PHÒNG KHÁM ĐA KHOA QUỐC TẾ EHC"));
            assertTrue(xml.contains("GIẤY HẸN KHÁM LẠI"));
            assertTrue(xml.contains("Sau 07 ngày"));
            assertTrue(xml.contains("BS. Chuyên Khoa"));
        }

        @Test
        @DisplayName("exportToFile with null parameters writes valid DOCX file using @TempDir")
        void exportToFileWithNullGuardsCreatesFileInTempDir(@TempDir Path tempDir) throws IOException {
            DocxDocument doc = exportService.buildAppointmentSlipDocx(null, null, null, null);

            File file = exportToFile(doc, tempDir, "appointment_null_guards.docx");
            String xml = assertValidZipAndExtractDocumentXml(file);

            assertTrue(xml.contains("GIẤY HẸN KHÁM LẠI"));
            assertTrue(xml.contains("PHÒNG KHÁM ĐA KHOA QUỐC TẾ EHC"));
        }

        @Test
        @DisplayName("exportToBytes with populated appointment slip incorporates scheduled date and doctor name")
        void exportToBytesWithPopulatedDataIncorporatesScheduledDate() throws IOException {
            BenhNhan patient = new BenhNhan();
            patient.setHo("Phạm");
            patient.setTen("Thanh Hùng");
            patient.setNgaySinh(LocalDate.of(1980, 12, 1));
            patient.setDienThoai("0901234567");
            patient.setDiaChi("12 Pasteur, Quận 1, TP.HCM");

            LuotKham visit = new LuotKham();
            visit.setNgayHenTaiKham(LocalDate.of(2026, 11, 25));

            String clinicName = "Phòng Khám Tân Định";
            String doctorName = "BS. CKII Đặng Tuấn";

            DocxDocument doc = exportService.buildAppointmentSlipDocx(patient, visit, clinicName, doctorName);
            byte[] bytes = exportToBytes(doc);
            String xml = assertValidZipAndExtractDocumentXml(bytes);

            assertTrue(xml.contains("PHÒNG KHÁM TÂN ĐỊNH"));
            assertTrue(xml.contains("Phạm Thanh Hùng"));
            assertTrue(xml.contains("01/12/1980"));
            assertTrue(xml.contains("0901234567"));
            assertTrue(xml.contains("12 Pasteur"));
            assertTrue(xml.contains("25/11/2026"));
            assertTrue(xml.contains("BS. CKII Đặng Tuấn"));
        }

        @Test
        @DisplayName("exportToFile with populated appointment slip creates valid file in @TempDir")
        void exportToFileWithPopulatedSlipCreatesFileInTempDir(@TempDir Path tempDir) throws IOException {
            BenhNhan patient = new BenhNhan();
            patient.setHo("Đỗ");
            patient.setTen("Văn Tùng");

            LuotKham visit = new LuotKham();
            visit.setNgayHenTaiKham(LocalDate.of(2026, 12, 15));

            DocxDocument doc = exportService.buildAppointmentSlipDocx(patient, visit, "Phòng Khám Á Châu", "BS. Nam");
            File file = exportToFile(doc, tempDir, "appointment_slip_populated.docx");

            String xml = assertValidZipAndExtractDocumentXml(file);
            assertTrue(xml.contains("PHÒNG KHÁM Á CHÂU"));
            assertTrue(xml.contains("Đỗ Văn Tùng"));
            assertTrue(xml.contains("15/12/2026"));
            assertTrue(xml.contains("BS. Nam"));
        }
    }
}
