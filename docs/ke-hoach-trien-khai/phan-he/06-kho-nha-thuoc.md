# Phân hệ 06 — Kho, mua hàng và nhà thuốc

**Trạng thái:** `[ ] Chưa làm end-to-end`  
**Đợt:** 5.  
**Domain:** `inventory`, `prescription`.

## Hiện có

- Models: `Item`, `Kho`, `TonKho`, `NhaCungCap`, `DonDatHang`, `ChiTietDonDatHang`, `PhieuNhapKho`, `ChiTietNhapKho`, `PhieuXuatKho`, `ChiTietXuatKho`, `InventoryTransaction`, `Stocktake`, `StocktakeCount`, `DonViTinhQuyDoi`, `IdempotencyRecord`, `FefoAllocationService`.
- UI: `PharmacyDispensePanel`, `PharmacyTicketTablePanel`, `PharmacyCategoryTreePanel`, `PharmacySubToolbar`.
- Chưa thấy màn hình riêng cho mua/nhận hàng, kiểm kê, điều chuyển, quản lý nhà cung cấp hoặc tra lịch sử tồn theo lô.

## Backlog theo model

- [ ] **`Item` + `DonViTinhQuyDoi`:** danh mục thuốc/vật tư, hoạt chất/hàm lượng/đơn vị, ngừng sử dụng, quy đổi đơn vị có kiểm tra vòng lặp/tỷ lệ.
- [ ] **`NhaCungCap`:** hồ sơ, thông tin liên hệ, trạng thái, điều khoản và lịch sử giao dịch.
- [ ] **`DonDatHang` + chi tiết:** lập, gửi, nhận một phần/đủ, hủy; số lượng đặt/nhận và giá.
- [ ] **`PhieuNhapKho` + chi tiết:** nhận lô, hạn dùng, số đăng ký, giá nhập, NCC; kiểm tra hàng quá hạn và trùng lô.
- [ ] **`TonKho`:** tra tồn theo kho/item/lô/hạn; cảnh báo dưới định mức và cận hạn.
- [ ] **`DonThuoc` + `PhieuXuatKho`:** cấp phát sau duyệt, allocation FEFO, xác nhận dược sĩ và transaction atomic.
- [ ] **`PhieuXuatKho`:** điều chuyển, xuất hủy và hoàn trả; cần phân biệt loại xuất và người nhận.
- [ ] **`Stocktake` + `StocktakeCount`:** tạo phiên kiểm kê, khóa snapshot, nhập số đếm, giải trình chênh lệch, duyệt điều chỉnh.
- [ ] **`InventoryTransaction` + `IdempotencyRecord`:** chỉ hiển thị read-only; truy vết mã lệnh và ngăn ghi trùng.

## Màn hình cần bổ sung

1. Danh mục item và đơn vị quy đổi.
2. Nhà cung cấp và đơn đặt hàng.
3. Nhận hàng/nhập kho theo lô.
4. Tra tồn và thẻ kho theo item/kho/lô/hạn dùng.
5. Cấp phát đơn thuốc; chi tiết đề xuất FEFO và xác nhận số lượng thay thế khi thiếu lô.
6. Điều chuyển/xuất hủy/trả thuốc có duyệt theo quyền.
7. Kiểm kê kho theo phiên; chênh lệch trước khi commit.
8. Báo cáo tồn tối thiểu/cận hạn/hết hạn.

## Nghiệm thu

- [ ] Mọi thay đổi tồn có chứng từ và dòng transaction truy vết được.
- [ ] Đồng thời cấp phát không thể làm tồn âm hoặc trừ trùng.
- [ ] FEFO bỏ qua lô hết hạn/không đủ tồn và chọn đúng lô hợp lệ.
- [ ] Kiểm kê chỉ ghi điều chỉnh sau khi duyệt; rollback không để tồn và chứng từ lệch nhau.

