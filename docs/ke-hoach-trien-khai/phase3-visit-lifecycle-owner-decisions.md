# Phase 3 — Quyết định Owner cho vòng đời lượt khám

**Task:** `VIS-LIFECYCLE-DECISIONS-01`
**Trạng thái:** Các quyết định điều chuyển đã được Owner phê duyệt và đối soát ngày 06/11/2026 theo `VIS-TRANSFER-OWNER-DECISION-RECON-03`; workflow chưa mở.
**Gate:** Không cấp quyền hoặc thay đổi gate từ tài liệu này. Luồng điều chuyển vẫn default-deny và phụ thuộc SEC-01 cùng acceptance implementation riêng.

## Chính sách đã duyệt — không hỏi lại

- `RECEPTIONIST` chỉ hủy lượt `CHO_KHAM` trước khi có chỉ định hoặc thu phí; cần xác nhận, lý do và audit.
- Điều chuyển: `RECEPTIONIST` chỉ được chuyển lượt `CHO_KHAM`; `DOCTOR` được chuyển lượt `CHO_KHAM`, `DANG_KHAM` hoặc `CHO_CLS`. Lượt final/completed, đã hủy, soft-deleted hoặc EMR-locked bị từ chối.
- Ranh giới actor/scope: `RECEPTIONIST` chỉ chuyển giữa các khoa trong cùng cơ sở; `DOCTOR` chỉ chuyển trong khoa đã chọn. Điều chuyển không cấp quyền đọc hồ sơ, lịch sử hoặc hàng đợi khoa đích.
- Phòng đích bắt buộc, phải active và thuộc khoa đích.
- Cấp số thứ tự mới tại queue đích, giữ nguyên mức ưu tiên hiện tại và không tự nâng ưu tiên.
- Giữ nguyên liên kết của chỉ định dịch vụ, đơn thuốc, kết quả, hóa đơn và hồ sơ tài chính; không tự hủy/sửa, không đổi billing và không tạo khoản thu trùng. Pending work chỉ được theo lượt nếu chứng minh an toàn và giữ nguyên đơn vị thực hiện/trách nhiệm; nếu không thì phải hoàn tất/handoff hoặc từ chối chuyển.
- Đơn thuốc đã phát hành, kết quả đã xác nhận và giao dịch tài chính đã post là bất biến; mọi điều chỉnh thuộc workflow riêng được cấp quyền, đối soát và audit.
- Bảo đảm nhất quán transaction, optimistic concurrency, domain invariants và kiểm thử exception paths.
- Metadata kỹ thuật/audit dùng UTC; thời điểm nghiệp vụ dùng `Asia/Ho_Chi_Minh`.
- Chỉ `DOCTOR` được ghi/sửa sinh hiệu.
- Mỗi phiên sử dụng một role và một cặp cơ sở+khoa. Hành động nhạy cảm có guard ở Application, không dựa vào ẩn nút UI.

Nguồn phê duyệt điều chuyển: `VIS-TRANSFER-OWNER-DECISION-RECON-03`, 06/11/2026; xem [transfer contract review](phase3-visit-transfer-contract-review.md) và [decision-gap memo](phase3-visit-transfer-decision-gaps.md). Actor/scope nền tảng được ghi trong [ma trận role/action đã duyệt](phase2-role-permission-matrix-approval.md); thời gian trong [Phase 2 implementation contract](phase2-implementation-contract.md). `VIS-01` hiện chỉ có các mảnh Domain và UI prototype; chưa có handler, repository lifecycle, audit events hoặc SQL acceptance ([review vòng đời lượt khám](phase3-visit-lifecycle-review.md), [transfer review](phase3-visit-transfer-contract-review.md)).

## Quyết định điều chuyển còn mở

Chỉ các mục sau đây vẫn chưa được quyết định theo contract `VIS-TRANSFER-OWNER-DECISION-RECON-03`. Không suy diễn status đích hoặc kết quả replay:

| Chủ đề còn mở | Phạm vi chưa được quyết định |
|---|---|
| Lý do điều chuyển | Có bắt buộc không; mã lý do/free text và giới hạn nội dung nào được dùng. |
| Ngày hiệu lực | Ranh giới ngày hiệu lực và business date dùng cho queue. |
| Namespace số queue | Phạm vi sequence và bảo đảm reuse/collision của số cấp mới. |
| Status/thời điểm của lượt | Giữ hay chuyển trạng thái và timestamps; status nguồn đủ điều kiện không quyết định status kết quả. |
| Lịch sử điều chuyển | Lưu lịch sử bất biến riêng, chỉ audit, hay cả hai. |
| Replay idempotent | Kết quả khi replay hoặc khi cùng key nhưng payload khác. Không giả định replay trả thành công hay phát sinh lại side effect. |

Các lựa chọn trước đây bên dưới về hủy và sinh hiệu thuộc quyết định vòng đời riêng, không phải khoảng trống của contract điều chuyển này; chúng không thay đổi quy tắc điều chuyển đã duyệt.

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
3. **Transfer persistence:** cập nhật location/queue theo quy tắc đã duyệt và trong transaction nhất quán; lựa chọn lịch sử riêng/audit-only/cả hai cùng payload lý do vẫn mở. Không tự chọn loại lưu vết hoặc triển khai migration.
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
