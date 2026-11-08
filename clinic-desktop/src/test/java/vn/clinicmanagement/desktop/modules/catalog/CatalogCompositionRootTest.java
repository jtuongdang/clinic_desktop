package vn.clinicmanagement.desktop.modules.catalog;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.authorization.AuthorizationContext;
import vn.clinicmanagement.application.facility.CreateRoomCommand;
import vn.clinicmanagement.application.facility.CreateRoomReceipt;
import vn.clinicmanagement.application.inventory.CreateItemCommand;
import vn.clinicmanagement.application.inventory.CreateItemReceipt;
import vn.clinicmanagement.application.inventory.GetItemCatalogQuery;
import vn.clinicmanagement.application.inventory.ItemSummaryDto;
import vn.clinicmanagement.domain.common.enums.LoaiItem;
import vn.clinicmanagement.domain.facility.PhongKham;
import vn.clinicmanagement.domain.inventory.Item;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CatalogCompositionRootTest {

    @BeforeEach
    @AfterEach
    void resetRoot() {
        CatalogCompositionRoot.reset();
    }

    @Test
    @DisplayName("CreateDefault must assemble all handlers and repositories cleanly with pre-seeded data")
    void testCreateDefaultAssemblesAllHandlersAndRepositories() {
        CatalogCompositionRoot.Services services = CatalogCompositionRoot.createDefault();

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
        CatalogCompositionRoot.Services s1 = CatalogCompositionRoot.getInstance();
        CatalogCompositionRoot.Services s2 = CatalogCompositionRoot.getInstance();
        assertSame(s1, s2, "getInstance must return identical singleton instance");

        CatalogCompositionRoot.Services custom = CatalogCompositionRoot.createDefault();
        CatalogCompositionRoot.configure(custom);
        assertSame(custom, CatalogCompositionRoot.getInstance(), "configure must update instance");

        CatalogCompositionRoot.reset();
        CatalogCompositionRoot.Services renewed = CatalogCompositionRoot.getInstance();
        assertNotNull(renewed);
    }

    @Test
    @DisplayName("CreateItemHandler validates input and creates item successfully")
    void testCreateItemHandler() {
        CatalogCompositionRoot.Services services = CatalogCompositionRoot.createDefault();

        // Null command -> VALIDATION_ERROR
        var nullResult = services.createItem().handle(null);
        assertFalse(nullResult.isSuccess());
        assertEquals("VALIDATION_ERROR", nullResult.getErrorCode().orElse(null));

        // Valid command with ADMIN authorization context
        AuthorizationContext adminAuth = new AuthorizationContext(
                1, 1, "ADMIN", 1, 1, java.util.UUID.randomUUID());

        CreateItemCommand cmd = new CreateItemCommand(
                "MED-TEST-01",
                "Thuốc Thử Nghiệm",
                LoaiItem.THUOC,
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
        CreateItemReceipt receipt = result.valueOrThrow();
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
        CatalogCompositionRoot.Services services = CatalogCompositionRoot.createDefault();

        var nullResult = services.getItemCatalog().handle(null);
        assertFalse(nullResult.isSuccess());
        assertEquals("VALIDATION_ERROR", nullResult.getErrorCode().orElse(null));

        AuthorizationContext auth = new AuthorizationContext(
                2, 2, "PHARMACIST", 1, 2, java.util.UUID.randomUUID());
        GetItemCatalogQuery query = new GetItemCatalogQuery("Para", true, auth);
        var result = services.getItemCatalog().handle(query);
        assertTrue(result.isSuccess());
        List<ItemSummaryDto> list = result.valueOrThrow();
        assertFalse(list.isEmpty());
        assertTrue(list.stream().anyMatch(i -> i.tenItem().contains("Paracetamol")));
    }

    @Test
    @DisplayName("CreateRoomHandler validates input and creates room successfully")
    void testCreateRoomHandler() {
        CatalogCompositionRoot.Services services = CatalogCompositionRoot.createDefault();

        var nullResult = services.createRoom().handle(null);
        assertFalse(nullResult.isSuccess());
        assertEquals("ROOM_INVALID", nullResult.getErrorCode().orElse(null));

        CreateRoomCommand cmd = new CreateRoomCommand(
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
        CreateRoomReceipt receipt = result.valueOrThrow();
        assertEquals("PK-999", receipt.getMaPhong());
        assertTrue(services.roomRepository().existsByMaPhong("PK-999"));
    }

    @Test
    @DisplayName("InMemory repositories behave correctly")
    void testInMemoryRepositories() {
        CatalogCompositionRoot.InMemoryKhoRepository khoRepo = new CatalogCompositionRoot.InMemoryKhoRepository();
        Item item = new Item();
        item.setMaItem("ITEM-01");
        item.setTenItem("Khẩu trang y tế");
        item.setLoaiItem(LoaiItem.VAT_TU);
        item.setDonViCoBan("Hộp");
        khoRepo.saveItem(item);

        assertTrue(khoRepo.getItemByCode("ITEM-01").isPresent());
        assertEquals("Khẩu trang y tế", khoRepo.getItemByCode("ITEM-01").get().getTenItem());
        assertEquals(1, khoRepo.searchItems("khẩu", true).size());

        CatalogCompositionRoot.InMemoryPhongKhamRepository roomRepo =
                new CatalogCompositionRoot.InMemoryPhongKhamRepository();
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
