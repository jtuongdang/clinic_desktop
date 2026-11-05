# Phase 2 — contract triển khai vòng đời phiên

**Trạng thái:** Implementation contract dựa trên policy đã được chủ dự án duyệt. Thời lượng idle 15 phút, tuyệt đối 12 giờ; tự khóa giữ nguyên bản nháp; xác thực lại cùng tài khoản và vai trò. Không bổ sung login lockout.

## 1. Cấu hình thời hạn

- Lưu tại `ThamSoHeThong` bằng hai key `SESSION_IDLE_TIMEOUT_MINUTES=15` và `SESSION_ABSOLUTE_TIMEOUT_HOURS=12`.
- Không hardcode timeout trong timer/UI. Composition root đọc cấu hình qua `ISystemRepository`; thiếu/sai giá trị phải giới hạn về policy mặc định đã duyệt, ghi chẩn đoán an toàn và không được biến phiên thành vô hạn.
- Migration seed có điều kiện, chạy được trên DB hiện hữu, không cập nhật/ghi đè giá trị đã cấu hình.

## 2. Trạng thái phiên

- Mỗi lần đăng nhập tạo `sessionId`, `issuedAt`, `lastActivityAt`, `absoluteExpiresAt` và trạng thái khóa.
- `UserSession` giữ role ID/code/name cùng một facility/department đã chọn. Station/workstation độc lập và không thay thế department scope.
- Tính thời gian bằng `Clock`/`Instant` để kiểm thử xác định; hoạt động sau khi bị khóa không làm mới thời gian idle.
- Logout, từ chối xác thực lại hoặc đóng workspace xóa toàn bộ session metadata.

## 3. Khóa UI và xác thực lại

- Theo dõi thao tác bàn phím/chuột của ứng dụng desktop; sau 15 phút hiển thị lớp khóa che toàn bộ workspace và chặn thao tác phía sau.
- Giữ dashboard/forms/bản nháp còn trong bộ nhớ; không dispose/recreate workspace khi idle-lock.
- Xác thực mở khóa phải khớp user ID, role ID và facility/department IDs đang giữ; đồng thời xác nhận các assignment hiện vẫn hoạt động. Không cho đổi tài khoản/vai trò/phạm vi ngay trên lớp khóa.
- Khi hết 12 giờ, bắt buộc xác thực lại để mở phiên mới; chỉ khôi phục workspace nếu cùng user/role/scope. Không hiển thị draft của phiên cũ cho identity khác.
- Xác thực sai/hủy giữ lớp khóa; không ghi đè session context. Logout chủ động vẫn qua cảnh báo bản nháp như contract hiện hành.

## 4. Phạm vi và kiểm thử

- Chia implementation theo file scope: (A) repository cấu hình và migration; (B) policy/state machine; (C) desktop activity monitor, lock overlay và re-auth. Mỗi giao diện chung phải được merge/contract chốt trước task phụ thuộc.
- Unit test ranh giới thời gian 14:59/15:00 phút và 11:59:59/12:00 giờ bằng `Clock` giả; test activity reset, lock persistence, absolute expiry, same-context re-auth, revoked assignment, cancellation và logout clear.
- Headless test cho state machine/session; desktop smoke test xác nhận overlay che toàn workspace và draft còn nguyên. Chỉ dùng dữ liệu tổng hợp; không nối SQL Server thật.

## 5. Không thuộc contract này

- Số lần đăng nhập sai, khóa tài khoản và mở khóa thủ công vẫn chờ quyết định riêng.
- Authorization role/action matrix và audit implementation là task riêng; không gán mặc định quyền `AUDIT_READ`.
