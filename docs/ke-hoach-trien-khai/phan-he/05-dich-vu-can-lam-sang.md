# Phân hệ 05 — Danh mục dịch vụ và cận lâm sàng

**Trạng thái:** `[ ] Chưa làm end-to-end`  
**Đợt:** 1 (danh mục nền), 4 (thực thi chỉ định/kết quả).  
**Domain:** `service`, `examination`.

## Hiện có

- Models: `DanhMucDichVu`, `ChiDinhDichVu`, `ChiTietKetQuaXetNghiem`, `DinhMucVatTuDichVu` và enum trạng thái chỉ định.
- UI: `ServiceOrderPanel`, `LabOrderTabPanel`, `ParaclinicalOrderPanel`, print preview phiếu chỉ định.
- `ParaclinicalOrderPanel` đang nạp một dòng mẫu; chưa có form nhập/duyệt kết quả CLS rõ ràng trong module này.

## Backlog theo model

- [ ] **`DanhMucDichVu`:** CRUD mã/tên/loại/giá/giá BHYT/trạng thái; lịch sử giá có hiệu lực nếu cần; import/export danh mục.
- [ ] **`ChiDinhDichVu`:** tạo từ lượt khám; số lượng, người chỉ định, khoa/phòng thực hiện, trạng thái, ưu tiên và hủy có lý do.
- [ ] **`ChiTietKetQuaXetNghiem`:** nhập chỉ số, đơn vị, khoảng tham chiếu, cờ bất thường, người làm và thời gian; bảo toàn lịch sử sau duyệt.
- [ ] **Kết quả hình ảnh:** xác định model metadata cho mô tả/kết luận, file/URL, loại modality, người đọc và version. Nếu hiện chưa có entity cụ thể thì thiết kế trước khi làm UI.
- [ ] **`DinhMucVatTuDichVu`:** màn hình cấu hình vật tư tiêu hao theo dịch vụ và quy tắc trừ kho; tránh tự động trừ trước khi nghiệp vụ duyệt.

## Màn hình cần bổ sung

1. Quản lý danh mục dịch vụ và giá.
2. Hàng đợi CLS phân theo khu thực hiện, loại kỹ thuật và trạng thái.
3. Form nhập kết quả xét nghiệm theo bảng thông số có đơn vị/reference range.
4. Form kết luận CĐHA/thăm dò và đính kèm ảnh/tệp; xem preview kết quả.
5. Hàng chờ duyệt kết quả; phân biệt người nhập và người duyệt.
6. Tab kết quả trên màn hình bác sĩ; đánh dấu đã xem và hiển thị cảnh báo quan trọng.

## Nghiệm thu

- [ ] Luồng chỉ định → nhận việc → nhập kết quả → duyệt → bác sĩ xem được.
- [ ] Kết quả đã duyệt không bị ghi đè; sửa đổi có version/audit.
- [ ] Dịch vụ hủy không tính phí và không còn trong hàng đợi thực hiện.
- [ ] Dữ liệu mẫu đã được thay bằng repository/query thật.

