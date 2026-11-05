# SEC-AUTHZ-02C — Hợp đồng scope và audit cho reads Tiếp đón

**Trạng thái:** Được phân rã theo policy và quyết định Owner ngày 05/11/2026.  
**Phụ thuộc:** SEC-AUTHZ-02A evaluator; SEC-AUTHZ-02B write guard; `phase2-read-audit-decision.md`.  
**Nguồn nghiệp vụ:** `DOMAIN_ANALYSIS.md`, `ERD_DIAGRAM.md`, `phase2-role-permission-matrix-approval.md`.

## Mục tiêu

Enforce authorization và cặp scope cơ sở+khoa ở Application cho ba protected read đang được `ReceptionPanel` gọi: tìm bệnh nhân, tải hàng đợi/lượt khám và tải danh sách/tóm tắt phòng khám. Dữ liệu chỉ được trả cho caller sau khi successful-read audit được ghi và commit trên connection độc lập.

## Hợp đồng từng use case

| Use case | Quyền cần đúng matrix grant | Phạm vi dữ liệu | Successful-read action |
|---|---|---|---|
| Tìm bệnh nhân + hồ sơ BHYT mới nhất | `PATIENT/XEM` | Chỉ bệnh nhân có ít nhất một `LuotKham` chưa xóa gắn đúng `maCoSo` và `maKhoa` của phiên; kết quả tối đa 50. | `PATIENT_SEARCH` |
| Hàng đợi và số liệu phòng | `RECEPTION/XEM` | `LuotKham.maCoSo` và `LuotKham.maKhoa` phải cùng đúng scope; loại lượt xóa/hủy và bệnh nhân xóa. | `PATIENT_VIEW` |
| Danh sách phòng đang hoạt động | `CATALOG/XEM` | `PhongKham`/`Khoa` thuộc đúng `maCoSo` + `maKhoa`, active, chưa xóa; không tải danh mục của khoa khác. | `CATALOG_VIEW` |

Mọi quyền còn dùng đúng evaluator default-deny, role đã chọn và scope `KHOA_PHONG`. Không cấp grant hoặc thay đổi matrix. `AUDIT_READ` tiếp tục deny.

## Thứ tự query/audit/response

1. Validate input và immutable `AuthorizationContext`/`AuditActorContext`.
2. Evaluate active session, employee, role, assignment, permission và exact scope. Khi deny: không chạy protected query; ghi `AUTH_ACCESS_DENIED` độc lập, chỉ actor/scope/reason, không resource ID hay search term.
3. Chạy scoped query và dựng response hoàn chỉnh trong Application.
4. Ghi successful-read event bằng `AuditWritePort.appendIndependently`; dùng per-operation workstation/correlation UUID, không ghi search term, CCCD/BHYT, patient name hay narrative. Queue/search audit không gắn ID từng bệnh nhân.
5. Chỉ trả `Result.success(data)` sau khi append/commit audit thành công. Nếu truy vấn hoặc audit thất bại, trả failure chung và không đưa dữ liệu protected ra ngoài.

Truy vấn thành công nhưng không có kết quả vẫn là một lần search/queue/catalog read được audit. Lỗi validate trước query không phát sinh successful-read event. Query và audit không cần atomic với nhau; audit độc lập commit là điều kiện phát dữ liệu.

## Thiết kế implementation

- `SearchPatientsQuery` và `GetReceptionQueueQuery` mang cả authorization context và per-operation audit actor; không cung cấp constructor bỏ qua context.
- Tạo query/handler cho room catalog, không gọi `IPhongKhamRepository.getActiveRooms()` trực tiếp từ Swing.
- Thêm repository/port scoped methods; SQL bắt buộc lọc cả facility và department, không lọc theo mỗi cơ sở, room hay ngày.
- `ReceptionPanel` lấy context còn hiệu lực từ `SessionLifecycleService` cho mỗi thao tác; trên lỗi chỉ hiện thông báo cố định và giữ draft tìm/đăng ký.
- Không thay đổi flow đăng ký/writes từ SEC-AUTHZ-02B, không thêm SQL migration hoặc seed.

## Acceptance

- Unit tests chứng minh deny/inactive/stale scope không gọi repository; denial event không mang resource ID/search input.
- SQL-shape/fake JDBC tests chứng minh cả hai cột scope đều là predicate cho patient, queue, summary và room queries.
- Handler tests chứng minh query được gọi trước independent audit, dữ liệu chỉ trả sau commit audit; audit exception/commit failure trả failure không dữ liệu.
- Search 0 kết quả vẫn audit thành công; query lỗi không tạo successful-read audit.
- UI calls không còn gọi trực tiếp unscoped reads.
- Synthetic fixtures only; không truy cập SQL Server opt-in hay `clinicDb` trong task này.
- Root validation: `mvn -Djava.awt.headless=true clean verify` và `git diff --check`.
