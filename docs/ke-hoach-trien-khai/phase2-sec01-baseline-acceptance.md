# SEC-01 — Baseline acceptance theo Definition A

**Ngày cập nhật:** 06/11/2026
**Trạng thái:** `[~]` — còn thiếu manual desktop visual smoke.
**Gate được Owner duyệt:** Definition A; workflow chưa triển khai tiếp tục default-deny và có acceptance riêng. Chúng không phải điều kiện đóng baseline SEC-01.

## Phạm vi baseline

Baseline này xác nhận identity/session, default-deny, grant đã duyệt, các đường đọc/ghi tiếp đón hiện có, audit và giao diện bảo mật trong phạm vi đã triển khai. Không xác nhận transfer, billing, EMR, CLS, pharmacy, inventory, appointment deposit, audit reader hoặc quản trị tài khoản. Những workflow đó phải tiếp tục bị từ chối/không có đường gọi cho tới khi acceptance riêng đạt.

## Checklist và bằng chứng

| Hạng mục | Trạng thái | Bằng chứng / giới hạn |
|---|---|---|
| Danh tính, chọn một role và một scope cơ sở+khoa, session idle 15 phút / tối đa 12 giờ | PASS | Implementation và test được theo dõi ở `SEC-DATA-01`, `SEC-LOGIN-01`, `SEC-SESSION-01`; task board ghi bằng chứng theo từng task. |
| Default-deny, allowlist sáu role và grant `KHOA_PHONG` đã duyệt | PASS | `ApprovedRoleCodes` dùng chung evaluator/navigation; unit tests cho sáu role hợp lệ và role không hỗ trợ; `AUDIT_READ` luôn deny. Không cấp grant cho workflow tương lai. |
| Đường ghi tiếp đón có authorization đúng scope và audit mutation cùng transaction | PASS | SQL Server opt-in synthetic acceptance ghi ở `SEC-AUTHZ-02B`/`SEC-AUDIT-01D`; audit failure rollback dữ liệu liên quan. |
| Patient search, queue và room read scope đúng cặp; successful-read audit trước khi trả dữ liệu | PASS | SQL Server synthetic acceptance ghi ở `SEC-AUTHZ-02C`/`SEC-AUDIT-01D`; lỗi audit fail-closed. Queue kiểm tra ngày nghiệp vụ hiện tại, scope cơ sở+khoa, `CHO_KHAM`/`DANG_KHAM`/`CHO_CLS`, loại cancelled/completed và soft-deleted rows. |
| Login throttle theo policy B được duyệt | PASS (local) | Sau 5 sai trong fixed window 15 phút, lần kế tiếp phải chờ trước password verification; mức đầu 30 giây, tăng gấp đôi tới 15 phút. Trong khi delay còn hiệu lực chỉ audit, không tăng delay. Mật khẩu đúng của tài khoản hoạt động reset ngay sau xác minh; account inactive/deleted không reset. SQL Server UTC là nguồn thời gian chuẩn. JDK 17 `mvn -Djava.awt.headless=true clean verify`: 321 tests, 0 failures/errors, 6 opt-in skips; SQL Server synthetic V1–V11 migration 1/1 và throttle concurrency/audit rollback 1/1. Chi tiết tại `phase2-login-lockout-policy-proposal.md`. |
| Root Maven baseline | PASS | `mvn -Djava.awt.headless=true clean verify` — **BUILD SUCCESS**, 336 tests, 0 failures, 0 errors, 6 SQL Server opt-in skips, 70 Surefire XML reports. Opt-in SQL Server runs được báo riêng, không cộng vào số này. |
| Desktop visual smoke: login, role/scope selection, dashboard/menu theo grant, lock/re-auth/logout và lỗi deny an toàn | OPEN | Ứng dụng đã được mở bằng synthetic DB nhưng CUA không attach được cửa sổ Java; macOS từ chối `osascript` keyboard input. Chưa quan sát dashboard hoặc hoàn tất acceptance trực quan. Không đánh dấu PASS bằng headless test. Cần chạy/quan sát trực tiếp trên desktop có thể tương tác và ghi revision, ngày, thao tác, kết quả. |

## Quyết định giữ nguyên

- `AUDIT_READ` tiếp tục deny; chưa có quyền tra cứu audit.
- Retention duration chưa chốt; không purge/xóa audit.
- Workflow ngoài reception chưa được mở quyền bởi policy đã duyệt. Mỗi workflow cần guard, scope, audit, persistence, test và UI acceptance phù hợp trước khi enable.
- Không push, mở PR hoặc merge được thực hiện trong lần cập nhật này.

## Điều kiện đóng SEC-01

Thực hiện và ghi nhận visual smoke ở trên. Sau khi đạt, cập nhật checklist cùng revision/build và đổi SEC-01 sang complete nếu không có regression mới. Không yêu cầu triển khai transfer, billing, CLS, pharmacy, inventory hoặc các workflow tương lai để đóng Definition A.
