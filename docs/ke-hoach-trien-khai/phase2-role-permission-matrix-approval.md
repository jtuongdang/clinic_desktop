# Phê duyệt ma trận phân quyền Phase 2

**Trạng thái:** Chủ dự án duyệt ma trận role/action ngày 05/11/2026 và bổ sung các quyết định DEC-AUTHZ-01/02/04a/04b/05/06/07/08 cùng ngày: chỉ `DOCTOR` ghi sinh hiệu; mở khóa EMR cần DOCTOR thứ hai cùng khoa; `RECEPTIONIST` có thể hủy lượt chưa chỉ định/chưa thu tiền và điều chuyển liên khoa trong cùng cơ sở; `DOCTOR` chỉ chuyển trong khoa đã chọn; CASHIER thu theo bảng giá, còn chiết khấu/hoàn/hủy ngoại lệ cần CASHIER thứ hai duyệt. Mọi scope phiên giới hạn đúng một cặp cơ sở–khoa. Đây là policy đầu vào cho implementation; các grant được seed riêng sau kiểm thử.

**Tài liệu nguồn:** [Đề xuất ma trận vai trò](phase2-role-permission-matrix-proposal.md), [policy bảo mật Phase 2](phase2-security-policy-proposal.md), [implementation contract](phase2-implementation-contract.md).

## Quyết định đã duyệt

1. Duyệt các ALLOW theo role/action ghi trong bảng 5.2 của ma trận. Hành động ghi `DENY`, hoặc đã có policy nhưng chưa triển khai guard/grant, tiếp tục `DENY` tới khi implementation và test đạt.
2. Mỗi phiên có đúng một role và một cặp `(facilityId, departmentId)`. Mọi quyền đọc/ghi thông thường phải khớp **cả hai** ID đó ở Application boundary; không grant xem dữ liệu liên khoa hay toàn hệ thống. Riêng DEC-AUTHZ-04b chỉ cho phép RECEPTIONIST thực hiện use case điều chuyển lượt khám trong cùng cơ sở qua guard kiểm tra cả khoa nguồn/đích; quyền này không mở quyền đọc dữ liệu của khoa đích. DOCTOR chỉ điều chuyển trong khoa của phiên.
3. Quyết định `DEC-AUTHZ-03`: chọn phương án cô lập theo `KHOA_PHONG`. Hồ sơ hành chính, BHYT, tiền sử, lịch sử khám và hàng đợi đều chỉ trả về dữ liệu gắn với khoa của phiên. Không có thao tác mở xem liên khoa trong scope đã duyệt; điều chuyển theo DEC-AUTHZ-04b là chuyển trạng thái có guard riêng, không tiết lộ hồ sơ khoa đích.
4. Quyết định `DEC-AUTHZ-09`: bổ sung action codes `IN`, `XUAT_FILE`, `MO_KHOA_EMR`, `AUDIT_READ` vào domain contract/schema để có thể biểu đạt policy. Scope runtime chỉ cho phép `KHOA_PHONG`; các dòng legacy `TOAN_HE_THONG`, `CO_SO`, `CHI_NHANH` không tạo quyền rộng hơn scope phiên và phải bị từ chối khi đánh giá.
5. Quyết định `DEC-AUTHZ-10`: `AUDIT_READ` bị từ chối cho mọi vai trò, kể cả `ADMIN`; không role nào được sửa ma trận để tự cấp `AUDIT_READ` hoặc tự cấp quyền cho chính role đang hoạt động. Không có màn hình quản trị quyền trong task này.
6. Chỉ dùng sáu role đang có: `ADMIN`, `DOCTOR`, `RECEPTIONIST`, `PHARMACIST`, `CASHIER`, `LAB_TECH`. Không tự thêm `NURSE`, `HEAD_DOCTOR` hoặc role quản lý khác. Với DEC-AUTHZ-01, chỉ `DOCTOR` được ghi/sửa sinh hiệu trong `KHOA_PHONG`; `RECEPTIONIST` bị từ chối. Với DEC-AUTHZ-02, chỉ một `DOCTOR` khác cùng khoa được phê duyệt mở khóa EMR; người duyệt phải nhập lý do, xác thực lại và mọi thao tác phải được audit. Bác sĩ đã khóa không tự mở lại; `ADMIN` không được mở khóa. Với DEC-AUTHZ-04a, chỉ RECEPTIONIST được hủy lượt `CHO_KHAM` chưa có chỉ định/chưa thu tiền; có xác nhận, lý do và audit. Với DEC-AUTHZ-04b, RECEPTIONIST được điều chuyển giữa các khoa trong cùng cơ sở; DOCTOR chỉ trong khoa đã chọn. Các workflow này chưa có grant/use case và chỉ được triển khai sau khi SEC-01 gate đạt. Với DEC-AUTHZ-05, CASHIER chỉ thu đúng bảng giá; ngoại lệ giảm giá/hoàn tiền/hủy phiếu thu cần CASHIER khác duyệt, có tách người thực hiện và người duyệt. Thu cọc lịch hẹn chưa được bao phủ rõ, vẫn `DENY` đến khi chốt quy trình. Với CLS, `LAB_TECH` nhập và DOCTOR thuộc khoa CLS khác người nhập ký duyệt; với đơn thuốc, PHARMACIST duyệt lâm sàng rồi cấp phát; các quyết định kho do PHARMACIST khác người lập duyệt.

## Hành động tiếp tục bị từ chối

DEC-AUTHZ-01/02/04a/04b/05/06/07/08 đã được Owner chốt về actor và policy. Các workflow tương ứng vẫn runtime `DENY`, chưa seed grant cho tới khi implementation có guard, audit và test đạt. Riêng thu cọc lịch hẹn chưa có actor/flow đã duyệt và tiếp tục `DENY`.

DEC-AUTHZ-05/06/07/08 đã chốt policy nhưng chưa có grant/workflow; thu cọc lịch hẹn vẫn `DENY` vì chưa chốt chi tiết. Quyền `AUDIT_READ`, retention/purge và login lockout vẫn không được mở trong phạm vi quyết định này. Giữ audit reader default-deny, không purge và không triển khai lockout.

## Hệ quả dữ liệu và kiểm thử

- `BenhNhanCoSo` hiện chỉ liên kết bệnh nhân với cơ sở. Để thi hành scope theo khoa, truy vấn bệnh nhân phải join qua lượt khám thuộc đúng `(CoSo, Khoa)`; bệnh nhân không có liên kết hợp lệ trong scope không được trả về.
- Quy tắc CCCD duy nhất toàn cơ sở dữ liệu vẫn giữ. Nếu CCCD tồn tại ngoài khoa hiện tại, ứng dụng không được trả chi tiết hồ sơ hoặc cho tìm kiếm xuyên khoa; cần trả lỗi generic không tiết lộ thông tin định danh. Luồng xử lý chuyển hồ sơ/scope liên khoa cần quyết định nghiệp vụ riêng.
- Mọi test authorization phải có ca allow đúng cặp, deny khoa khác cùng cơ sở, deny cơ sở khác, deny role không có grant, deny `AUDIT_READ`, và deny scope legacy rộng hơn. Chỉ dùng synthetic fixtures.

