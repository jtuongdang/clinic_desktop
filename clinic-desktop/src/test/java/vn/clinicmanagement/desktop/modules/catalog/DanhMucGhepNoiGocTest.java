package vn.clinicmanagement.desktop.modules.catalog;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.facility.CreatePhongCommand;
import vn.clinicmanagement.application.facility.CreatePhongBienNhan;
import vn.clinicmanagement.application.inventory.CreateMatHangCommand;
import vn.clinicmanagement.application.inventory.CreateMatHangBienNhan;
import vn.clinicmanagement.application.inventory.GetMatHangDanhMucQuery;
import vn.clinicmanagement.application.inventory.MatHangTongHopDuLieu;
import vn.clinicmanagement.domain.common.enums.LoaiMatHang;
import vn.clinicmanagement.domain.facility.PhongKham;
import vn.clinicmanagement.domain.inventory.MatHang;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DanhMucGhepNoiGocTest {

    @BeforeEach
    @AfterEach
    void resetRoot() {
        DanhMucGhepNoiGoc.reset();
    }

    @Test
    @DisplayName("CreateDefault must assemble all handlers and repositories cleanly with pre-seeded data")
    void testCreateDefaultAssemblesAllHandlersAndRepositories() {
        DanhMucGhepNoiGoc.Services services = DanhMucGhepNoiGoc.createDefault();

        assertNotNull(services, "Services bundle must not be null");
        assertNotNull(services.createItem(), "CreateItemHandler must be present");
        assertNotNull(services.getItemCatalog(), "GetItemCatalogHandler must be present");
        assertNotNull(services.createRoom(), "CreateRoomHandler must be present");
        assertNotNull(services.inventoryRepository(), "IKhoRepository must be present");
        assertNotNull(services.roomRepository(), "IPhongKhamRepository must be present");
        assertNotNull(services.serviceRepository(), "IChiDinhDichVuRepository must be present");

        // Verify pre-seeded items
        assertFalse(services.inventoryRepository().searchItems(null, true).isEmpty());
        // Verify pre-seeded rooms
        assertFalse(services.roomRepository().getAll().isEmpty());
        // Verify pre-seeded services
        assertFalse(services.serviceRepository().searchDichVu(null, null).isEmpty());
    }

    @Test
    @DisplayName("Singleton getInstance, configure, and reset work properly")
    void testSingletonLifecycle() {
        DanhMucGhepNoiGoc.Services s1 = DanhMucGhepNoiGoc.getInstance();
        DanhMucGhepNoiGoc.Services s2 = DanhMucGhepNoiGoc.getInstance();
        assertSame(s1, s2, "getInstance must return identical singleton instance");

        DanhMucGhepNoiGoc.Services custom = DanhMucGhepNoiGoc.createDefault();
        DanhMucGhepNoiGoc.configure(custom);
        assertSame(custom, DanhMucGhepNoiGoc.getInstance(), "configure must update instance");

        DanhMucGhepNoiGoc.reset();
        DanhMucGhepNoiGoc.Services renewed = DanhMucGhepNoiGoc.getInstance();
        assertNotNull(renewed);
    }

    @Test
    @DisplayName("CreateItemHandler validates input and creates item successfully")
    void testCreateItemHandler() {
        DanhMucGhepNoiGoc.Services services = DanhMucGhepNoiGoc.createDefault();

        // Null command -> VALIDATION_ERROR
        var nullResult = services.createItem().handle(null);
        assertFalse(nullResult.isSuccess());
        assertEquals("VALIDATION_ERROR", nullResult.getErrorCode().orElse(null));

        // Valid command with ADMIN authorization context
        PhanQuyenNguCanh adminAuth = new PhanQuyenNguCanh(
                1, 1, "ADMIN", 1, 1, java.util.UUID.randomUUID());

        CreateMatHangCommand cmd = new CreateMatHangCommand(
                "MED-TEST-01",
                "Thuốc Thử Nghiệm",
                LoaiMatHang.THUOC,
                "Hoạt chất X",
                "100mg",
                "Hộp",
                "Hộp 10 vỉ x 10 viên",
                new BigDecimal("50000"),
                new BigDecimal("75000"),
                new BigDecimal("10"),
                new BigDecimal("1000"),
                "Viên nén",
                "Việt Nam",
                "Dược Hậu Giang",
                "VD-12345-21",
                adminAuth
        );

        var result = services.createItem().handle(cmd);
        assertTrue(result.isSuccess(), () -> "Tạo mặt hàng phải thành công: " + result.getError());
        CreateMatHangBienNhan receipt = result.valueOrThrow();
        assertEquals("MED-TEST-01", receipt.maItem());
        assertTrue(receipt.itemId() > 0);

        // Duplicate code -> DUPLICATE_CODE
        var dupResult = services.createItem().handle(cmd);
        assertFalse(dupResult.isSuccess());
        assertEquals("DUPLICATE_CODE", dupResult.getErrorCode().orElse(null));
    }

    @Test
    @DisplayName("GetItemCatalogHandler validates query and queries catalog")
    void testGetItemCatalogHandler() {
        DanhMucGhepNoiGoc.Services services = DanhMucGhepNoiGoc.createDefault();

        var nullResult = services.getItemCatalog().handle(null);
        assertFalse(nullResult.isSuccess());
        assertEquals("VALIDATION_ERROR", nullResult.getErrorCode().orElse(null));

        PhanQuyenNguCanh auth = new PhanQuyenNguCanh(
                2, 2, "PHARMACIST", 1, 2, java.util.UUID.randomUUID());
        GetMatHangDanhMucQuery query = new GetMatHangDanhMucQuery("Para", true, auth);
        var result = services.getItemCatalog().handle(query);
        assertTrue(result.isSuccess());
        List<MatHangTongHopDuLieu> list = result.valueOrThrow();
        assertFalse(list.isEmpty());
        assertTrue(list.stream().anyMatch(i -> i.tenItem().contains("Paracetamol")));
    }

    @Test
    @DisplayName("CreateRoomHandler validates input and creates room successfully")
    void testCreateRoomHandler() {
        DanhMucGhepNoiGoc.Services services = DanhMucGhepNoiGoc.createDefault();

        var nullResult = services.createRoom().handle(null);
        assertFalse(nullResult.isSuccess());
        assertEquals("ROOM_INVALID", nullResult.getErrorCode().orElse(null));

        CreatePhongCommand cmd = new CreatePhongCommand(
                "ADMIN",
                "PK-999",
                "Phòng Khám Test 999",
                1,
                1,
                "KHAM_BENH",
                "Tầng 9"
        );
        var result = services.createRoom().handle(cmd);
        assertTrue(result.isSuccess(), () -> "Tạo phòng phải thành công: " + result.getError());
        CreatePhongBienNhan receipt = result.valueOrThrow();
        assertEquals("PK-999", receipt.getMaPhong());
        assertTrue(services.roomRepository().existsByMaPhong("PK-999"));
    }

    @Test
    @DisplayName("InMemory repositories behave correctly")
    void testInMemoryRepositories() {
        DanhMucGhepNoiGoc.InMemoryKhoRepository khoRepo = new DanhMucGhepNoiGoc.InMemoryKhoRepository();
        MatHang item = new MatHang();
        item.setMaItem("ITEM-01");
        item.setTenItem("Khẩu trang y tế");
        item.setLoaiItem(LoaiMatHang.VAT_TU);
        item.setDonViCoBan("Hộp");
        khoRepo.saveItem(item);

        assertTrue(khoRepo.getItemByCode("ITEM-01").isPresent());
        assertEquals("Khẩu trang y tế", khoRepo.getItemByCode("ITEM-01").get().getTenItem());
        assertEquals(1, khoRepo.searchItems("khẩu", true).size());

        DanhMucGhepNoiGoc.InMemoryPhongKhamRepository roomRepo =
                new DanhMucGhepNoiGoc.InMemoryPhongKhamRepository();
        PhongKham pk = new PhongKham();
        pk.setMaPhong("PK-TEST");
        pk.setTenPhong("Phòng Khám A");
        pk.setMaKhoa(5);
        roomRepo.add(pk);

        assertTrue(roomRepo.existsByMaPhong("PK-TEST"));
        assertTrue(roomRepo.getByMaPhong("PK-TEST").isPresent());
        assertEquals(1, roomRepo.getByMaKhoa(5).size());
    }
}
