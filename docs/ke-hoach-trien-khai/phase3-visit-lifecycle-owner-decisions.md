# Phase 3 — Quyết định Owner cho vòng đời lượt khám

**Task:** `VIS-LIFECYCLE-DECISIONS-01`
**Trạng thái:** Các quyết định điều chuyển đã được Owner phê duyệt và đối soát ngày 06/11/2026 theo `VIS-TRANSFER-OWNER-DECISION-RECON-04`; workflow chưa mở.
**Gate:** Không cấp quyền hoặc thay đổi gate từ tài liệu này. Luồng điều chuyển vẫn default-deny và phụ thuộc SEC-01 cùng acceptance implementation riêng.

## Chính sách đã duyệt — không hỏi lại

- `RECEPTIONIST` chỉ hủy lượt `CHO_KHAM` trước khi có chỉ định hoặc thu phí; cần xác nhận, lý do và audit.
- Điều chuyển: `RECEPTIONIST` chỉ được chuyển lượt `CHO_KHAM`; `DOCTOR` được chuyển lượt `CHO_KHAM`, `DANG_KHAM` hoặc `CHO_CLS`. Lượt final/completed, đã hủy, soft-deleted hoặc EMR-locked bị từ chối.
- Ranh giới actor/scope: `RECEPTIONIST` chỉ chuyển giữa các khoa trong cùng cơ sở; `DOCTOR` chỉ chuyển trong khoa đã chọn. Điều chuyển không cấp quyền đọc hồ sơ, lịch sử hoặc hàng đợi khoa đích.
- Phòng đích bắt buộc, phải active và thuộc khoa đích.
- Cấp số thứ tự mới tại queue đích, giữ nguyên mức ưu tiên hiện tại và không tự nâng ưu tiên.
- Khi điều chuyển, giữ nguyên trạng thái lượt, `ngayKham` và các timestamp khám hiện có. Phân bổ queue đích không thay đổi ngày khám lâm sàng hoặc timeline khám.
- Dùng ngày điều chuyển địa phương `Asia/Ho_Chi_Minh` làm ngày queue riêng `queueDate`, tách biệt và không ghi đè `ngayKham`; metadata kỹ thuật/audit vẫn dùng UTC.
- Lý do điều chuyển bắt buộc dùng mã lý do cố định; ghi chú ngắn là tùy chọn. Không tái sử dụng trường lý do khám lâm sàng `lyDoKham`.
- Giữ nguyên liên kết của chỉ định dịch vụ, đơn thuốc, kết quả, hóa đơn và hồ sơ tài chính; không tự hủy/sửa, không đổi billing và không tạo khoản thu trùng. Pending work chỉ được theo lượt nếu chứng minh an toàn và giữ nguyên đơn vị thực hiện/trách nhiệm; nếu không thì phải hoàn tất/handoff hoặc từ chối chuyển.
- Đơn thuốc đã phát hành, kết quả đã xác nhận và giao dịch tài chính đã post là bất biến; mọi điều chỉnh thuộc workflow riêng được cấp quyền, đối soát và audit.
- Bảo đảm nhất quán transaction, optimistic concurrency, domain invariants và kiểm thử exception paths.
- Metadata kỹ thuật/audit dùng UTC; thời điểm nghiệp vụ dùng `Asia/Ho_Chi_Minh`.
- Chỉ `DOCTOR` được ghi/sửa sinh hiệu.
- Mỗi phiên sử dụng một role và một cặp cơ sở+khoa. Hành động nhạy cảm có guard ở Application, không dựa vào ẩn nút UI.

Nguồn phê duyệt điều chuyển: `VIS-TRANSFER-OWNER-DECISION-RECON-04`, 2026-11-06; xem [transfer contract review](phase3-visit-transfer-contract-review.md) và [decision-gap memo](phase3-visit-transfer-decision-gaps.md). Hợp đồng này cập nhật các quyết định trước trong `VIS-TRANSFER-OWNER-DECISION-RECON-03`; actor/scope nền tảng được ghi trong [ma trận role/action đã duyệt](phase2-role-permission-matrix-approval.md), thời gian kỹ thuật/nghiệp vụ trong [Phase 2 implementation contract](phase2-implementation-contract.md). `VIS-01` hiện chỉ có các mảnh Domain và UI prototype; chưa có handler, repository lifecycle, audit events hoặc SQL acceptance ([review vòng đời lượt khám](phase3-visit-lifecycle-review.md), [transfer review](phase3-visit-transfer-contract-review.md)).

## Quyết định điều chuyển còn mở

Chỉ các mục sau đây vẫn chưa được quyết định theo contract `VIS-TRANSFER-OWNER-DECISION-RECON-04`. Không suy diễn chi tiết triển khai chưa được duyệt:

| Chủ đề còn mở | Phạm vi chưa được quyết định |
|---|---|
| Mã lý do và ghi chú | Danh mục mã lý do; ràng buộc độ dài/chuẩn hóa của ghi chú; quy tắc privacy. |
| Chi tiết phân bổ queue | Namespace sequence, reuse/gap policy và chính xác thời điểm transaction lấy mẫu `queueDate`; hình dạng persistence chưa được quyết định. |
| Lịch sử điều chuyển | Lưu lịch sử bất biến riêng, chỉ audit, hay cả hai. |
| Replay idempotent | Semantics khi replay, bao gồm cùng key nhưng payload khác; không suy diễn kết quả. |

Status, `ngayKham`, examination timestamps, tính bắt buộc của mã lý do, `queueDate` tách biệt theo ngày địa phương và ranh giới timezone đã được quyết định; không đưa chúng trở lại danh sách mở. Các lựa chọn trước đây về hủy và sinh hiệu thuộc quyết định vòng đời riêng. Snapshot sáu lựa chọn mở theo `VIS-TRANSFER-OWNER-DECISION-RECON-03` ngày 06/11/2026 được giữ tại [transfer contract review](phase3-visit-transfer-contract-review.md) và [decision-gap memo](phase3-visit-transfer-decision-gaps.md), nay đã được RECON-04 thay thế.

## Các quyết định vòng đời khác (ngoài contract điều chuyển)

| Chủ đề | Lựa chọn | Khuyến nghị |
|---|---|---|
| Hủy lượt — phát hiện chỉ định/thu phí | A. Chỉ cho hủy khi không có chỉ định hoạt động và không có khoản thu/phiếu đã ghi nhận; kiểm tra cùng transaction. B. Cấm hủy khi tồn tại bất kỳ record chỉ định/thu nào, kể cả void/reversed. | A theo trạng thái nghiệp vụ hiện hành; tiêu chí “hoạt động/đã thu” phải tra dữ liệu nguồn. Hiện billing/đơn dịch vụ chưa có application workflow hoàn chỉnh nên giữ hủy DENY cho tới khi eligibility query có nguồn tin cậy. Lỗi dependency phải từ chối hủy. |
| Hủy lượt — audit | A. Lý do tự do bắt buộc, giới hạn độ dài và không nhập thông tin y khoa. B. Lý do chọn từ danh mục mã cố định, có ghi chú tùy chọn. | B giảm biến thiên và hạn chế PII; danh mục mã phải được nghiệp vụ thống nhất trước khi triển khai. |
| Sinh hiệu — lưu đo lặp | A. Mỗi lần đo là record mới, giữ lần đo trước; sửa chỉ bằng bác sĩ theo lý do. B. Một row hiện hành trên mỗi lượt khám, cập nhật ghi đè. | A tốt hơn cho lịch sử lâm sàng; cần thêm measured-at/measured-by/measurement ID nếu chưa được bảo đảm. B đơn giản hơn nhưng mất lịch sử đo. |
| Sinh hiệu — thời điểm | A. `measuredAt` là giờ nghiệp vụ `Asia/Ho_Chi_Minh`; created/updated/audit metadata là UTC. B. Chỉ lưu thời điểm metadata UTC. | A phân biệt thời điểm đo với thời điểm nhập; không backdate ngoài chính sách được duyệt. |
| Sinh hiệu — ai đọc | A. DOCTOR trong đúng pair cơ sở+khoa. B. RECEPTIONIST cũng được đọc trong khâu tiếp đón. | A là đề xuất bảo mật; Owner đã chốt DOCTOR-only cho ghi nhưng quyền đọc sinh hiệu chưa được chốt riêng. Mặc định giữ deny cho role chưa được duyệt. |

## Hướng triển khai đề xuất

1. **Không triển khai hủy trước khi xác định được nguồn dữ liệu chỉ định/thu phí.** Trong transaction kiểm tra current status/version, active orders và posted receipts; nếu service không sẵn sàng hoặc kết quả mơ hồ thì từ chối.
2. **Tách command:** `CancelVisitCommand`, `TransferVisitCommand`, `RecordVitalsCommand` với context lấy từ session; không làm một mutation tổng quát dựa trên enum/setter.
3. **Transfer persistence:** cập nhật location/queue theo quy tắc đã duyệt trong transaction nhất quán; lưu queue date ở `queueDate` riêng, không ghi đè `ngayKham` hoặc examination timestamps. Mã lý do cố định là bắt buộc, ghi chú ngắn tùy chọn; taxonomy, note constraints/privacy, transaction sampling và lựa chọn lịch sử riêng/audit-only/cả hai vẫn mở. Không tự chọn loại lưu vết hoặc triển khai migration.
4. **Sinh hiệu:** lưu actor và measurement time riêng; đọc lại theo authorization/audit. Nếu cho phép sửa, giữ lịch sử hoặc audit diff không chứa narrative thừa.
5. **Tắt các menu prototype** cho tới khi command và guard đã nối; UI chỉ là lớp thao tác, không thay authorization.

## Acceptance gates

- Unit/application tests cho guard trạng thái, đúng role/scope, denial reason-safe, order/charge eligibility và dependency failure.
- SQL Server opt-in synthetic tests cho optimistic concurrency, cấp số đích theo namespace/date đã được chốt, invariant liên kết work và rollback trên exception; kiểm thử history theo lựa chọn lưu trữ sẽ được Owner chốt riêng.
- Kiểm thử transfer không gọi patient search/detail/history hoặc queue read khoa đích.
- Kiểm thử sinh hiệu DOCTOR allow, các role khác deny; measured-at và actor đúng; từ chối số đo không hợp lệ; kiểm tra history/revision theo quyết định.
- Headless Swing tests, sau đó desktop visual smoke trên dữ liệu tổng hợp; test replay chỉ xác minh semantics sau khi được quyết định, không giả định kết quả. Giữ mọi workflow ngoài scope ở default-deny.

## Nguồn

- [`phase3-visit-lifecycle-review.md`](phase3-visit-lifecycle-review.md)
- [`phase3-visit-transfer-contract-review.md`](phase3-visit-transfer-contract-review.md)
- [`phase2-role-permission-matrix-approval.md`](phase2-role-permission-matrix-approval.md)
- [`phase2-implementation-contract.md`](phase2-implementation-contract.md)
- `clinic-domain/src/main/java/vn/clinicmanagement/domain/examination/{LuotKham,SinhHieu}.java`
- `clinic-infrastructure/src/main/resources/migrations/V1__Initial_Clinic_Schema.sql`
