# SEC-AUTHZ-02B — Guard lệnh ghi đăng ký bệnh nhân/tiếp đón

**Policy nguồn:** [ma trận đã duyệt](phase2-role-permission-matrix-approval.md), [evaluator](phase2-authorization-evaluator-contract.md), [scope inventory](phase2-authz-enforcement-review.md).

## Mục tiêu

Nối evaluator default-deny vào vertical slice ghi tiếp đón: tạo/cập nhật hồ sơ bệnh nhân, ghi BHYT và tạo lượt khám. Đưa trusted session scope từ `SessionLifecycleService` tới Application, và xác nhận mọi thay đổi thuộc đúng facility+department.

## Luật quyết định

- Dùng `AuthorizationService` trong `clinic-application`; không lấy role/scope từ trường do form tự nhập. `AuthorizationContext` từ desktop chỉ được dựng từ `UserSession` và session UUID; `ActiveSessionPort` phải đọc `SessionLifecycleService` để xác nhận active/unlocked/unexpired và khớp user/role/pair.
- Kiểm tra `RECEPTION / TAO` cho lượt tiếp đón, `PATIENT / TAO` cho hồ sơ mới và `PATIENT / SUA` cho hồ sơ hiện có. Chỉ cho phép khi từng permission có một row active duy nhất, ALLOW, scope `KHOA_PHONG`, và pair đích khớp session.
- Trước khi cập nhật bệnh nhân hiện có, repository chỉ được trả target nếu có lượt khám liên kết với đúng pair `(maCoSo, maKhoa)`; thêm phương thức tìm CCCD có scope bằng SQL `EXISTS` trên `LuotKham`. Không dùng kết quả tìm kiếm toàn cục để điền thông tin hồ sơ.
- CCCD toàn DB vẫn unique. Nếu CCCD bị ràng buộc bởi hồ sơ ngoài scope, trả một lỗi generic không tiết lộ hồ sơ/giá trị/đơn vị; không update hồ sơ đó, không lộ mã hoặc thông tin trùng.
- Audit thành công đã được ghi trong cùng transaction với patient/insurance/visit. Khi authorization bị từ chối, ghi `AUTH_ACCESS_DENIED` độc lập với actor/scope, không gắn patient ID hoặc dữ liệu form; lỗi ghi denial audit không được làm thao tác được phép hoặc tiết lộ exception.
- Không seed permissions. Khi chưa có grant thì use case ghi phải bị từ chối mặc định.

## Ngoài phạm vi

Không triển khai patient search, queue/room reads, EMR, audit read, quyền admin, permission UI, role grant seed hoặc sửa schema scope của `BaoHiem`. Successful-read audit nằm ngoài contract này; Owner đã duyệt thứ tự query/audit và fail-closed behavior sau đó trong [read-audit decision](phase2-read-audit-decision.md). `AUDIT_READ` vẫn bị deny cho mọi role theo DEC-AUTHZ-10.

## Acceptance

- Tests cho allow/deny `PATIENT/TAO`, `PATIENT/SUA`, `RECEPTION/TAO`; deny thiếu, false, duplicate grant; deny role/scope/revoked session và target phòng khác khoa/cơ sở.
- Synthetic integration/fake-JDBC tests xác nhận patient, BHYT, visit và audit cùng rollback khi ghi thất bại; không cập nhật bệnh nhân ngoài scope.
- Global CCCD collision không trả chi tiết cross-scope; mọi denial audit không chứa CCCD, BHYT, reason text hoặc patient ID.
- UI command lấy context từ current lifecycle session; thiếu/khóa/hết hạn context phải deny.
- Không đổi luồng query dữ liệu cá nhân trong task này. JDK 17 `mvn -Djava.awt.headless=true clean verify` pass, `git diff --check` pass, SQL Server chỉ opt-in riêng do root.
