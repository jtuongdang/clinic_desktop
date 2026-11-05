# Phê duyệt ma trận phân quyền Phase 2

**Trạng thái:** Chủ dự án duyệt ma trận role/action đề xuất ngày 05/11/2026, đồng thời xác nhận scope mỗi phiên phải giới hạn đúng một cặp cơ sở–khoa. Đây là policy được duyệt để lập implementation contract; chưa có grant nào được seed cho tới khi implementation và kiểm thử đạt.

**Tài liệu nguồn:** [Đề xuất ma trận vai trò](phase2-role-permission-matrix-proposal.md), [policy bảo mật Phase 2](phase2-security-policy-proposal.md), [implementation contract](phase2-implementation-contract.md).

## Quyết định đã duyệt

1. Duyệt các ALLOW theo role/action ghi trong bảng 5.2 của ma trận. Hành động ghi `DENY`, hoặc đang chờ `DEC-AUTHZ-*`, tiếp tục `DENY` cho tới khi có quyết định riêng.
2. Mỗi phiên có đúng một role và một cặp `(facilityId, departmentId)`. Mọi allow phải khớp **cả hai** ID đó ở Application boundary; không grant xem liên khoa trong cùng cơ sở và không grant toàn hệ thống.
3. Quyết định `DEC-AUTHZ-03`: chọn phương án cô lập theo `KHOA_PHONG`. Hồ sơ hành chính, BHYT, tiền sử, lịch sử khám và hàng đợi đều chỉ trả về dữ liệu gắn với khoa của phiên. Không có thao tác mở xem liên khoa trong scope đã duyệt.
4. Quyết định `DEC-AUTHZ-09`: bổ sung action codes `IN`, `XUAT_FILE`, `MO_KHOA_EMR`, `AUDIT_READ` vào domain contract/schema để có thể biểu đạt policy. Scope runtime chỉ cho phép `KHOA_PHONG`; các dòng legacy `TOAN_HE_THONG`, `CO_SO`, `CHI_NHANH` không tạo quyền rộng hơn scope phiên và phải bị từ chối khi đánh giá.
5. Quyết định `DEC-AUTHZ-10`: `AUDIT_READ` bị từ chối cho mọi vai trò, kể cả `ADMIN`; không role nào được sửa ma trận để tự cấp `AUDIT_READ` hoặc tự cấp quyền cho chính role đang hoạt động. Không có màn hình quản trị quyền trong task này.
6. Chỉ dùng sáu role đang có: `ADMIN`, `DOCTOR`, `RECEPTIONIST`, `PHARMACIST`, `CASHIER`, `LAB_TECH`. Không tự thêm `NURSE`, `HEAD_DOCTOR` hoặc role quản lý khác.

## Hành động tiếp tục bị từ chối

Các quyết định sau chưa được chọn trong proposal; vì vậy những hành động tương ứng giữ `DENY` và không được seed trong đợt này:

- `DEC-AUTHZ-01`: ai ghi sinh hiệu ở khâu tiếp đón/điều dưỡng.
- `DEC-AUTHZ-02`: ai được mở khóa bệnh án.
- `DEC-AUTHZ-04`: quyền hủy lượt khám và điều chuyển liên khoa.
- `DEC-AUTHZ-05`: thu cọc, miễn giảm, hoàn tiền và hủy phiếu thu.
- `DEC-AUTHZ-06`: phân tách người nhập và người duyệt kết quả CLS.
- `DEC-AUTHZ-07`: phân tách kê, duyệt và cấp phát thuốc.
- `DEC-AUTHZ-08`: duyệt kiểm kê, xuất hủy và điều chuyển kho.

Quyền `AUDIT_READ`, retention/purge và login lockout vẫn không được mở trong phạm vi quyết định này. Giữ audit reader default-deny, không purge và không triển khai lockout.

## Hệ quả dữ liệu và kiểm thử

- `BenhNhanCoSo` hiện chỉ liên kết bệnh nhân với cơ sở. Để thi hành scope theo khoa, truy vấn bệnh nhân phải join qua lượt khám thuộc đúng `(CoSo, Khoa)`; bệnh nhân không có liên kết hợp lệ trong scope không được trả về.
- Quy tắc CCCD duy nhất toàn cơ sở dữ liệu vẫn giữ. Nếu CCCD tồn tại ngoài khoa hiện tại, ứng dụng không được trả chi tiết hồ sơ hoặc cho tìm kiếm xuyên khoa; cần trả lỗi generic không tiết lộ thông tin định danh. Luồng xử lý chuyển hồ sơ/scope liên khoa cần quyết định nghiệp vụ riêng.
- Mọi test authorization phải có ca allow đúng cặp, deny khoa khác cùng cơ sở, deny cơ sở khác, deny role không có grant, deny `AUDIT_READ`, và deny scope legacy rộng hơn. Chỉ dùng synthetic fixtures.

