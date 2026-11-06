# Phase 3 — Quyết định Owner cho lịch hẹn

**Task:** `APPT-OWNER-DECISIONS-01`
**Trạng thái:** Đề xuất để Owner quyết định; workflow vẫn chưa triển khai.
**Gate:** Giữ `APPT-01` mặc định DENY cho đến khi SEC-01 đạt baseline gate và task acceptance riêng.

## Phạm vi và các quyết định đã có

Các model, bảng SQL V1, enum trạng thái và grant `APPOINTMENT` đã tồn tại; chưa có handler lịch hẹn, repository adapter, persistence acceptance hay màn hình đặt lịch được nối. Xem [readiness review](phase3-appointment-readiness-review.md) và [task board APPT-01](03-model-task-board.md).

Các chính sách đã duyệt tiếp tục có hiệu lực: mỗi phiên có một role và đúng một cặp cơ sở+khoa; technical metadata UTC, giờ nghiệp vụ `Asia/Ho_Chi_Minh`; `AUDIT_READ` bị từ chối. Thu cọc lịch hẹn vẫn `DENY`. Các grant `APPOINTMENT` hiện có không tự xác định transition hoặc actor cho từng thao tác. Không có patient self-service được duyệt.

## Đề xuất MVP để Owner xem xét

**Đề xuất:** giai đoạn đầu chỉ cho nhân viên đặt lịch thủ công trong cặp cơ sở+khoa đang chọn; không gửi thông báo, không nhận cọc/thanh toán, không cho bệnh nhân tự đặt. Lịch được đọc/ghi qua Application boundary, audit fail-closed, lưu ngày giờ nghiệp vụ ở `Asia/Ho_Chi_Minh`; mỗi booking có kiểm soát xung đột nguyên tử trong SQL Server. Đề xuất này không phải quyết định cho tới khi Owner duyệt.

| Câu hỏi Owner | Lựa chọn cần chốt | Khuyến nghị |
|---|---|---|
| Ai được tạo/sửa/hủy? | A. RECEPTIONIST quản lý lịch; DOCTOR xem lịch được phân công. B. DOCTOR và RECEPTIONIST cùng được tạo/sửa. C. Chia quyền theo thao tác khác. | A cho MVP; hiện grant chung chưa đủ để suy ra quyền từng transition. |
| Lịch thuộc resource nào? | A. Bác sĩ. B. Phòng. C. Cả bác sĩ và phòng. D. Khung giờ/dịch vụ có sức chứa. | Chọn đúng resource sẽ được nghiệp vụ dùng để tránh xếp trùng; không giả định phòng vì model V1 chưa có `maPhongKham`. |
| Thời lượng và overbooking? | Chọn thời lượng cố định hay theo dịch vụ; capacity mỗi slot; có cho phép vượt capacity và ai duyệt. | Không cho overbooking trong MVP; không đặt duration mặc định trước khi xác nhận lịch thực tế của cơ sở. |
| Trạng thái sau khi đặt? | A. Tạo thành `CHO_XAC_NHAN`, nhân viên/bác sĩ xác nhận. B. Tạo thành `DA_XAC_NHAN` ngay sau khi lưu. | A giữ đúng nghĩa của trạng thái mặc định hiện có; cần chỉ định actor và điều kiện xác nhận. |
| Hủy/đổi lịch? | Chốt actor, hạn chót, lý do bắt buộc, trạng thái nào được hủy, ai nhận chỗ trống và liệu lịch đã đến/hoàn tất có sửa được không. | RECEPTIONIST xử lý trước giờ hẹn, yêu cầu lý do và audit; sau check-in/hoàn tất thì không xóa hoặc viết lại lịch sử. |
| Check-in, trễ và no-show? | Chốt khoảng check-in trước/sau giờ, grace period, thời điểm thành no-show, khả năng phục hồi. | Không tự gán no-show theo timer cho tới khi có quy trình nhân viên xác nhận; no-show không tự sinh lượt khám. |
| Chuyển thành lượt khám? | Chốt ai check-in, có cho chuyển từ trạng thái nào, và kết quả gọi lại khi request lặp/đồng thời. | Chỉ check-in thủ công từ lịch đủ điều kiện; tạo `LuotKham` và chuyển trạng thái lịch trong một transaction; idempotent theo appointment ID. Không chuyển lịch `DA_HUY`/no-show. |
| Nhắc hẹn? | Chốt có gửi hay không, kênh, người nhận, consent, thời điểm, nội dung và retry. | Tắt gửi ngoài hệ thống cho MVP; chỉ bật sau consent, outbox/idempotency và privacy review. |
| Đặt cọc? | Có cần tiền cọc, actor, hoàn/miễn/đối soát? | Giữ DENY theo quyết định hiện tại; không dùng default `100000` trong entity hoặc `0` trong SQL làm chính sách. |

## Rủi ro và điều kiện trước code

- `ILichHenRepository.countAppointmentsInSlot` chỉ nhận bác sĩ và khoảng thời gian; thao tác count-then-insert có thể race và không mô hình hóa phòng/sức chứa. Phải chốt resource/capacity rồi chọn một transaction/constraint có thể kiểm chứng dưới concurrent booking.
- V1 `LichHen.maKhoa` nullable, không có room ID, không có no-show/cancellation actor/time, và `LuotKham.maLichHen` là FK không unique dù ERD mô tả tối đa một lượt khám cho một lịch. Chỉ thêm migration mới sau khi contract chứng minh dữ liệu nào cần lưu.
- Trường `nguonDat` là string trong entity, trong khi `NguonLich` là enum; cần chốt mapping trước khi nhận source values mới.
- `LichHen.chiPhiGiuCho` mặc định `100000` trong entity nhưng SQL mặc định `0`; đây là mismatch kỹ thuật, không phải căn cứ chọn chính sách cọc.
- Read phải giới hạn đúng scope và audit trước khi trả dữ liệu; mutation và audit cùng transaction. Không trả danh sách hẹn của khoa khác khi điều hướng.

## Acceptance gates sau Owner approval

1. Domain/Application allow/deny theo role, session và đúng pair cơ sở+khoa; không tự thêm grant.
2. Đặt/đổi/hủy và kiểm soát xung đột có optimistic version hoặc serialization; SQL Server synthetic test đồng thời chứng minh capacity không vượt mức đã duyệt.
3. Check-in/conversion atomic và idempotent; kiểm thử request lặp, concurrent conversion, rollback và đọc lại qua process khác.
4. Lưu/reload business time đúng `Asia/Ho_Chi_Minh` khi JVM dùng zone khác; metadata/audit sử dụng UTC.
5. Audit read-before-return, audit mutation trong cùng transaction, fail-closed; không ghi nội dung lâm sàng hay thông tin liên hệ vào log.
6. Swing calendar/form dùng dữ liệu đã persist, loading/I/O ngoài EDT, phản hồi lỗi rõ ràng; smoke test hiển thị thực tế trước khi tuyên bố hoàn tất.
7. Dùng dữ liệu tổng hợp; không gửi notification hoặc payment thật.

## Nguồn

- [`phase3-appointment-readiness-review.md`](phase3-appointment-readiness-review.md)
- [`phase2-implementation-contract.md`](phase2-implementation-contract.md)
- [`phase2-role-permission-matrix-approval.md`](phase2-role-permission-matrix-approval.md)
- [`phase2-session-implementation-contract.md`](phase2-session-implementation-contract.md)
- `clinic-domain/src/main/java/vn/clinicmanagement/domain/appointment/LichHen.java`
- `clinic-domain/src/main/java/vn/clinicmanagement/domain/repositories/ILichHenRepository.java`
- `clinic-infrastructure/src/main/resources/migrations/V1__Initial_Clinic_Schema.sql`
