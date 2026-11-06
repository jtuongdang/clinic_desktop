# Phase 0 — Quyết định tích hợp Value Object

- **Task:** `PLAT-VO-POLICY-01`
- **Trạng thái:** Đề xuất để chủ dự án quyết định; chưa thay đổi code/schema.
- **Phạm vi:** `DateRange`, `PhoneNumber`, `Money` và các điểm tiêu thụ thật trong project.
- **Gate:** Không thêm consumer vào workflow nhạy cảm trước `SEC-01`; mọi acceptance persistence dùng fixture tổng hợp.

## Tóm tắt

Ba Value Object hiện có unit test nhưng chưa được consumer production nào dùng. Báo cáo source audit xác nhận các lớp hiện dùng `LocalDate`, `String` và `BigDecimal` trực tiếp; do đó đổi model sang Value Object sẽ tác động Application validation, JDBC mapping, form và tìm kiếm, chứ không chỉ thay kiểu trong Domain. Cần chốt quy tắc nghiệp vụ trước khi tích hợp.

**Khuyến nghị triển khai:** quyết định và kiểm thử theo từng loại dữ liệu; không chuyển đổi hàng loạt model trong một task. Chưa chọn default tiền tệ, công thức làm tròn, định dạng điện thoại hay migration. Giữ nguyên các workflow hiện tại cho đến khi có quyết định và acceptance riêng.

## 1. `DateRange` — ngày hiệu lực bảo hiểm

**Hiện trạng:** `DateRange` bắt buộc ngày bắt đầu, cho phép ngày kết thúc null, xem hai đầu mút là bao gồm và coi hai khoảng chạm biên là overlap ([`DateRange.java`](../../clinic-domain/src/main/java/vn/clinicmanagement/domain/common/valueobjects/DateRange.java#L7-L30)). `BaoHiem` lưu ngày bắt đầu nullable, ngày hết hạn bắt buộc; handler hiện chỉ kiểm tra bắt đầu không sau ngày hết hạn khi có cả hai ([`BaoHiem.java`](../../clinic-domain/src/main/java/vn/clinicmanagement/domain/patient/BaoHiem.java#L11-L12), [`RegisterPatientHandler.java`](../../clinic-application/src/main/java/vn/clinicmanagement/application/reception/RegisterPatientHandler.java#L181-L200)). Form và JDBC đang truyền `LocalDate` trực tiếp.

| Câu hỏi cần quyết định | Phương án | Khuyến nghị để thảo luận |
|---|---|---|
| Ngày bắt đầu bảo hiểm trống có nghĩa gì? | A. Không có ngày bắt đầu đã biết; vẫn cho lưu như hiện tại. B. Bắt buộc ngày bắt đầu khi có mã thẻ. | A giữ tương thích dữ liệu hiện tại; không được hiểu null là bảo hiểm có hiệu lực vô hạn về quá khứ. |
| Ranh giới hiệu lực ngày kết thúc? | A. Có hiệu lực hết ngày ghi trên thẻ (inclusive). B. Hết hiệu lực từ đầu ngày đó (exclusive). | A phù hợp với cách người dùng đọc ngày hết hạn, nhưng cần nghiệp vụ bảo hiểm xác nhận. |
| Có kiểm tra hiệu lực trong tiếp đón không? | A. Chỉ kiểm tra khoảng ngày hợp lệ; xác minh quyền lợi ở workflow riêng. B. Từ chối tiếp đón khi thẻ ngoài hạn. | A tránh biến đăng ký thành quyết định chi trả; việc kiểm tra bảo hiểm cần quy trình riêng. |

**Điều kiện trước implementation:** xác định null-start semantics và inclusive/exclusive; sau đó mới chọn giữ `DateRange` hiện tại hay sửa constructor. Acceptance phải bao phủ form → handler → JDBC → đọc lại, gồm ngày đầu/cuối và null bắt đầu. Không suy ra quyền lợi thanh toán từ việc tạo được thẻ.

## 2. `PhoneNumber` — số liên hệ bệnh nhân

**Hiện trạng:** `PhoneNumber` chỉ nhận số di động Việt Nam theo prefix nhất định; bỏ khoảng trắng, dấu chấm và gạch nối nhưng không chuyển đổi `0...` sang `+84...` ([`PhoneNumber.java`](../../clinic-domain/src/main/java/vn/clinicmanagement/domain/common/valueobjects/PhoneNumber.java#L7-L15)). Tiếp đón hiện cho phép số chính trống và số liên hệ khẩn cấp tùy chọn; các giá trị được lưu/tìm kiếm như chuỗi.

| Câu hỏi cần quyết định | Phương án | Khuyến nghị để thảo luận |
|---|---|---|
| Số chính của bệnh nhân có bắt buộc không? | A. Không bắt buộc; nếu nhập thì validate. B. Bắt buộc số hợp lệ để đăng ký. | A giảm nguy cơ chặn tiếp đón khi người bệnh không có số hoặc không muốn cung cấp. |
| Số khẩn cấp có bắt buộc không? | A. Không; null/blank được giữ là vắng dữ liệu. B. Bắt buộc khi đăng ký. | A phù hợp với trường hiện đang tùy chọn. |
| Loại số nào được chấp nhận? | A. Di động Việt Nam theo bộ prefix hiện có. B. Thêm cố định/bàn hoặc quốc tế. | A giới hạn thay đổi đầu tiên; chỉ chọn nếu nghiệp vụ xác nhận không cần số bàn/ngoại quốc. |
| Lưu và tìm kiếm số thế nào? | A. Lưu nguyên dạng đã chuẩn hóa dấu phân cách, giữ prefix `0`/`+84`. B. Chuẩn hóa mọi số về một dạng quốc gia. | A ít thay đổi dữ liệu; tìm kiếm cần thử được cả cách nhập tương đương mà không mở rộng tìm kiếm ngoài scope. |

**Điều kiện trước implementation:** chốt optionality, định dạng, chuẩn hóa và cách xử lý số legacy không hợp lệ. Không tự động sửa dữ liệu cũ; nếu cần chuẩn hóa dữ liệu phải có migration/backfill riêng được duyệt. Test cần bao phủ validation, lưu/đọc lại và tìm kiếm tương đương.

## 3. `Money` — các trường tiền

**Hiện trạng:** `Money` mặc định VND, làm tròn mỗi lần tạo đối tượng về hai chữ số bằng `HALF_UP`, và hỗ trợ nhiều currency trong test ([`Money.java`](../../clinic-domain/src/main/java/vn/clinicmanagement/domain/common/valueobjects/Money.java#L8-L58)). Các model/schema có trường tiền `BigDecimal`/`DECIMAL(18,2)`, nhưng không lưu currency code; không có luồng hóa đơn/persistence production hoàn chỉnh.

| Câu hỏi cần quyết định | Phương án | Khuyến nghị để thảo luận |
|---|---|---|
| Phạm vi tiền tệ? | A. Chỉ VND cho phiên bản đầu. B. Hỗ trợ nhiều currency có currency code trên chứng từ. | A phù hợp phạm vi phòng khám nội địa nếu nghiệp vụ xác nhận; B đòi hỏi schema và kiểm soát tỷ giá rõ ràng. |
| Làm tròn ở đâu? | A. Làm tròn đơn giá và từng dòng. B. Giữ độ chính xác cao trong dòng, làm tròn ở tổng chứng từ. | Chưa đủ căn cứ chọn; kế toán cần xác nhận cách tính tiền dịch vụ, giảm trừ, BHYT và hoàn tiền. |
| Áp dụng cho nhóm nào? | A. Chỉ dịch vụ/viện phí. B. Bao gồm thuốc, kho, công nợ, đặt cọc và lương. | A cho phép tách hợp đồng hóa đơn; các nhóm khác cần quy tắc riêng. Khoản đặt cọc lịch hẹn hiện vẫn `DENY`. |
| Constructor có tự làm tròn không? | A. Có, scale cố định 2. B. Không; làm tròn tại boundary đã được nghiệp vụ chỉ định. | B tránh làm tròn lặp ngoài ý muốn, nhưng yêu cầu quy tắc scale/rounding được chốt trước khi có consumer. |

**Điều kiện trước implementation:** xác nhận currency, scale, rounding point và field scope. Không thêm currency default hoặc đổi schema chỉ dựa vào kiểu decimal hiện hữu. Acceptance cần tính tổng/giảm trừ và persistence read-back trên số tổng hợp; không đưa đơn giá hoặc dữ liệu thật vào log.

## Thứ tự đề xuất

1. Chốt optionality/format cho điện thoại bệnh nhân; nếu được duyệt, đây là consumer có luồng form → application → JDBC đang hoạt động và có thể test trên dữ liệu tổng hợp.
2. Chốt ngày hiệu lực cho BHYT; tích hợp `DateRange` mà không biến ngày null thành giả định nghiệp vụ.
3. Chỉ tích hợp `Money` khi có contract tính giá/chứng từ được duyệt và Phase 2 gate cho phép workflow liên quan. Không đưa deposit vào phạm vi hiện tại.

## Không thuộc quyết định của tài liệu này

- Không cấp quyền `APPOINTMENT`, `BILLING` hoặc `AUDIT_READ`; không thay đổi role/scope đã duyệt.
- Không bật đặt cọc, thanh toán, xác minh quyền lợi BHYT hay quyết định y khoa.
- Không thay schema, migration, format dữ liệu hiện hữu hoặc UI.
- Không dùng dữ liệu bệnh nhân thật; mọi integration test về sau phải dùng dữ liệu tổng hợp.

## Nguồn

- [`phase0-value-object-consumer-audit.md`](phase0-value-object-consumer-audit.md)
- [`03-model-task-board.md`](03-model-task-board.md), task `PLAT-VO-01`
- `clinic-domain/src/main/java/vn/clinicmanagement/domain/common/valueobjects/{DateRange,Money,PhoneNumber}.java`
- `clinic-domain/src/main/java/vn/clinicmanagement/domain/patient/BaoHiem.java`
- `clinic-application/src/main/java/vn/clinicmanagement/application/reception/RegisterPatientHandler.java`
- `clinic-infrastructure/src/main/resources/migrations/V1__Initial_Clinic_Schema.sql`
