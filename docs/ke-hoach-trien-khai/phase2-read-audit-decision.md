# Phase 2 — Quyết định Audit cho thao tác đọc

**Trạng thái:** Đã được Chủ dự án duyệt ngày 05/11/2026  
**Phạm vi:** Patient search, queue, room catalogs và các protected read tương tự trong Phase 2.

## Quyết định

1. Application thực hiện truy vấn dữ liệu trước.
2. Sau khi truy vấn thành công, Application ghi successful-read event bằng transaction audit độc lập.
3. Chỉ trả dữ liệu truy vấn cho caller sau khi transaction audit commit thành công.
4. Nếu audit storage/write/commit lỗi, use case fail closed: không trả dữ liệu protected; trả lỗi tổng quát, không chứa thông tin hồ sơ hay thông điệp DB.
5. Nếu truy vấn lỗi hoặc không được phép, không ghi successful-read event. Denial audit tiếp tục ghi độc lập theo contract hiện hành.

## Ràng buộc triển khai

- Query phải giới hạn đúng cặp cơ sở+khoa được chọn trong phiên; không suy rộng sang mọi cơ sở/khoa được gán.
- Audit event chỉ mang actor, role, scope, action, resource metadata được policy cho phép, workstation và correlation UUID. Không ghi CCCD/BHYT, tên bệnh nhân, lý do khám hoặc narrative.
- Audit failure không được fallback sang trả dữ liệu, cache chưa kiểm toán hoặc thông báo có chứa dữ liệu.
- `AUDIT_READ` là quyền tra cứu nhật ký kiểm toán riêng và vẫn bị từ chối mặc định; quyết định này không cấp quyền đọc audit log cho người dùng.
- Integration tests dùng dữ liệu tổng hợp; không dùng database `clinicDb` hay dữ liệu bệnh nhân thật.

## Kiểm chứng bắt buộc cho SEC-AUTHZ-02C

- Xác nhận query được thực hiện trước audit append và kết quả chỉ phát ra sau successful commit.
- Khi audit append/commit thất bại, kết quả trả về là failure và không chứa patient/queue data.
- Khi audit thành công, trả đúng các hàng thuộc facility+department đã chọn; không có hàng ngoài scope.
- Khi session, permission hoặc scope không hợp lệ, không chạy query và không trả dữ liệu.
