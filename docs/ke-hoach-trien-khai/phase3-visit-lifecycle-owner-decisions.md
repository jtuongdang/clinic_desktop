# Phase 3 — Quyết định Owner cho vòng đời lượt khám

**Task:** `VIS-LIFECYCLE-DECISIONS-01`
**Trạng thái:** Đề xuất tác nghiệp để Owner duyệt; chưa mở workflow.
**Gate:** Chỉ triển khai sau khi SEC-01 baseline được chấp nhận; không cấp quyền từ tài liệu này.

## Chính sách đã duyệt — không hỏi lại

- `RECEPTIONIST` chỉ hủy lượt `CHO_KHAM` trước khi có chỉ định hoặc thu phí; cần xác nhận, lý do và audit.
- `RECEPTIONIST` được điều chuyển liên khoa trong cùng cơ sở; `DOCTOR` chỉ trong khoa đã chọn. Điều chuyển không cấp quyền đọc hồ sơ/hàng đợi khoa đích.
- Chỉ `DOCTOR` được ghi/sửa sinh hiệu.
- Mỗi phiên sử dụng một role và một cặp cơ sở+khoa. Hành động nhạy cảm có guard ở Application, không dựa vào ẩn nút UI.

Các quyết định trên được ghi trong [ma trận role/action đã duyệt](phase2-role-permission-matrix-approval.md). `VIS-01` hiện chỉ có các mảnh Domain và UI prototype; chưa có handler, repository lifecycle, audit events hoặc SQL acceptance ([review vòng đời lượt khám](phase3-visit-lifecycle-review.md), [transfer review](phase3-visit-transfer-contract-review.md)).

## Quyết định cần Owner chốt để thiết kế contract

| Chủ đề | Lựa chọn | Khuyến nghị |
|---|---|---|
| Hủy lượt — phát hiện chỉ định/thu phí | A. Chỉ cho hủy khi không có chỉ định hoạt động và không có khoản thu/phiếu đã ghi nhận; kiểm tra cùng transaction. B. Cấm hủy khi tồn tại bất kỳ record chỉ định/thu nào, kể cả void/reversed. | A theo trạng thái nghiệp vụ hiện hành; tiêu chí “hoạt động/đã thu” phải tra dữ liệu nguồn. Hiện billing/đơn dịch vụ chưa có application workflow hoàn chỉnh nên giữ hủy DENY cho tới khi eligibility query có nguồn tin cậy. Lỗi dependency phải từ chối hủy. |
| Hủy lượt — audit | A. Lý do tự do bắt buộc, giới hạn độ dài và không nhập thông tin y khoa. B. Lý do chọn từ danh mục mã cố định, có ghi chú tùy chọn. | B giảm biến thiên và hạn chế PII; danh mục mã phải được nghiệp vụ thống nhất trước khi triển khai. |
| Điều chuyển — trạng thái được chuyển | A. Chỉ `CHO_KHAM` trước chỉ định/thu phí. B. Cho phép cả `DANG_KHAM`/`CHO_CLS` theo quy trình bác sĩ. | A là MVP ít rủi ro; B cần ownership lâm sàng và rule handoff riêng. Không suy ra từ quyền điều chuyển role. |
| Đích điều chuyển | A. Chọn khoa bắt buộc, phòng tùy chọn. B. Khoa và phòng đều bắt buộc. | A giữ room nullable và cho phép đội khoa tiếp nhận phân phòng; nếu phòng chỉ định thì phải active, thuộc đúng cơ sở/khoa đích. |
| Số thứ tự queue | A. Cấp số mới trong queue/ngày/khoa đích; lưu số nguồn trong lịch sử. B. Giữ số cũ và thứ tự chỉ đổi theo khoa. | A tránh đụng số và phản ánh hàng đợi đích; cần lưu transfer history và bảo đảm cấp số nguyên tử. |
| Điều chuyển và chỉ định/thu phí | A. Chặn khi có chỉ định hoặc thu phí trước khi hoàn thành workflow liên khoa. B. Cho phép theo dõi thủ công liên khoa, giữ nguyên chỉ định/phiếu ở khoa nguồn. | A cho MVP; B phải có phân bổ trách nhiệm, chi phí và read/write scope ở nguồn/đích. |
| Replay/concurrency | A. Client gửi version kỳ vọng; update CAS/row lock; request lặp cùng payload trả kết quả idempotent; request khác payload trên version cũ bị conflict. B. Cho phép “last write wins”. | A; không dùng last-write-wins cho queue/location của lượt khám. |
| Sinh hiệu — lưu đo lặp | A. Mỗi lần đo là record mới, giữ lần đo trước; sửa chỉ bằng bác sĩ theo lý do. B. Một row hiện hành trên mỗi lượt khám, cập nhật ghi đè. | A tốt hơn cho lịch sử lâm sàng; cần thêm measured-at/measured-by/measurement ID nếu chưa được bảo đảm. B đơn giản hơn nhưng mất lịch sử đo. |
| Sinh hiệu — thời điểm | A. `measuredAt` là giờ nghiệp vụ `Asia/Ho_Chi_Minh`; created/updated/audit metadata là UTC. B. Chỉ lưu thời điểm metadata UTC. | A phân biệt thời điểm đo với thời điểm nhập; không backdate ngoài chính sách được duyệt. |
| Sinh hiệu — ai đọc | A. DOCTOR trong đúng pair cơ sở+khoa. B. RECEPTIONIST cũng được đọc trong khâu tiếp đón. | A là đề xuất bảo mật; Owner đã chốt DOCTOR-only cho ghi nhưng quyền đọc sinh hiệu chưa được chốt riêng. Mặc định giữ deny cho role chưa được duyệt. |

## Hướng triển khai đề xuất

1. **Không triển khai hủy trước khi xác định được nguồn dữ liệu chỉ định/thu phí.** Trong transaction kiểm tra current status/version, active orders và posted receipts; nếu service không sẵn sàng hoặc kết quả mơ hồ thì từ chối.
2. **Tách command:** `CancelVisitCommand`, `TransferVisitCommand`, `RecordVitalsCommand` với context lấy từ session; không làm một mutation tổng quát dựa trên enum/setter.
3. **Transfer history:** ghi source/target facility-department-room, actor, role, scope, reason code, time và prior/new version; atomic với cập nhật queue/location. Không trả dữ liệu patient/queue khoa đích.
4. **Sinh hiệu:** lưu actor và measurement time riêng; đọc lại theo authorization/audit. Nếu cho phép sửa, giữ lịch sử hoặc audit diff không chứa narrative thừa.
5. **Tắt các menu prototype** cho tới khi command và guard đã nối; UI chỉ là lớp thao tác, không thay authorization.

## Acceptance gates

- Unit/application tests cho guard trạng thái, đúng role/scope, denial reason-safe, order/charge eligibility và dependency failure.
- SQL Server opt-in synthetic tests cho CAS/locking, cấp số queue đích nguyên tử, source/target history, rollback nếu audit lỗi, và đọc lại qua process mới.
- Kiểm thử transfer không gọi patient search/detail/history hoặc queue read khoa đích.
- Kiểm thử sinh hiệu DOCTOR allow, các role khác deny; measured-at và actor đúng; từ chối số đo không hợp lệ; kiểm tra history/revision theo quyết định.
- Headless Swing tests, sau đó desktop visual smoke trên dữ liệu tổng hợp; giữ mọi workflow ngoài scope ở default-deny.

## Nguồn

- [`phase3-visit-lifecycle-review.md`](phase3-visit-lifecycle-review.md)
- [`phase3-visit-transfer-contract-review.md`](phase3-visit-transfer-contract-review.md)
- [`phase2-role-permission-matrix-approval.md`](phase2-role-permission-matrix-approval.md)
- [`phase2-implementation-contract.md`](phase2-implementation-contract.md)
- `clinic-domain/src/main/java/vn/clinicmanagement/domain/examination/{LuotKham,SinhHieu}.java`
- `clinic-infrastructure/src/main/resources/migrations/V1__Initial_Clinic_Schema.sql`
