# Phase 0 — Quyết định tích hợp Value Object

- **Task:** `PLAT-VO-POLICY-01`
- **Trạng thái:** Owner xác nhận cả ba quyết định ngày 06/11/2026. Các acceptance hẹp được ghi theo task liên quan; `PLAT-VO-01` vẫn `[~]`. Điều này không đánh dấu Phase 0/1 hay visual acceptance hoàn tất.
- **Phạm vi:** `DateRange`, `PhoneNumber`, `Money` và các điểm tiêu thụ thật trong project.
- **Gate:** Không thêm consumer vào workflow nhạy cảm trước `SEC-01`; mọi acceptance persistence dùng fixture tổng hợp.

## Tóm tắt

Source audit ban đầu ghi nhận ba Value Object chưa có consumer production. Hiện `DateRange` được dùng để kiểm tra interval BHYT ở handler tiếp đón; `PhoneNumber` validate và lưu giá trị đã làm sạch, với read-back SQL synthetic được theo dõi riêng; `Money` chưa có consumer invoice/billing. Các acceptance hẹp không hoàn tất `PLAT-VO-01` hay visual acceptance. Chi tiết source snapshot và validation evidence được tách tại [`phase0-value-object-domain-review.md`](phase0-value-object-domain-review.md).

**Nguyên tắc triển khai:** quyết định và kiểm thử theo từng loại dữ liệu; không chuyển đổi hàng loạt model trong một task. Owner đã chốt các quy tắc bên dưới. Không thêm consumer ngoài các acceptance hẹp đã ghi, thay schema hoặc mở workflow tài chính; mọi phần mở rộng cần task và acceptance riêng.

## 1. `DateRange` — ngày hiệu lực bảo hiểm

**Hiện trạng:** `DateRange` chấp nhận ngày bắt đầu chưa biết; `contains` không xác nhận ngày thuộc khoảng khi start chưa biết, hai đầu mút đã biết là bao gồm, và `overlaps` xử lý thận trọng khi endpoint chưa biết ([`DateRange.java`](../../clinic-domain/src/main/java/vn/clinicmanagement/domain/common/valueobjects/DateRange.java#L7-L35)). `BaoHiem` lưu ngày bắt đầu nullable, ngày hết hạn bắt buộc; handler dùng `DateRange` để từ chối khoảng đã biết bị đảo, không so sánh ngày thẻ với ngày hiện tại ([`BaoHiem.java`](../../clinic-domain/src/main/java/vn/clinicmanagement/domain/patient/BaoHiem.java#L11-L12), [`RegisterPatientHandler.java`](../../clinic-application/src/main/java/vn/clinicmanagement/application/reception/RegisterPatientHandler.java#L197-L210)). Form và JDBC truyền `LocalDate` trực tiếp.

| Lựa chọn đã thảo luận trước quyết định Owner | Phương án | Ghi chú khi thảo luận |
|---|---|---|
| Ngày bắt đầu bảo hiểm trống có nghĩa gì? | A. Không có ngày bắt đầu đã biết; vẫn cho lưu như hiện tại. B. Bắt buộc ngày bắt đầu khi có mã thẻ. | A giữ tương thích dữ liệu hiện tại; không được hiểu null là bảo hiểm có hiệu lực vô hạn về quá khứ. |
| Ranh giới hiệu lực ngày kết thúc? | A. Có hiệu lực hết ngày ghi trên thẻ (inclusive). B. Hết hiệu lực từ đầu ngày đó (exclusive). | A phù hợp với cách người dùng đọc ngày hết hạn, nhưng cần nghiệp vụ bảo hiểm xác nhận. |
| Có kiểm tra hiệu lực trong tiếp đón không? | A. Chỉ kiểm tra khoảng ngày hợp lệ; xác minh quyền lợi ở workflow riêng. B. Từ chối tiếp đón khi thẻ ngoài hạn. | A tránh biến đăng ký thành quyết định chi trả; việc kiểm tra bảo hiểm cần quy trình riêng. |

**Quyết định Owner — 06/11/2026:** ngày bắt đầu có thể trống và có nghĩa là chưa biết, không được suy thành bắt đầu từ vô hạn quá khứ hay giả định đủ điều kiện; ngày hết hạn có hiệu lực hết toàn bộ ngày lịch địa phương ghi trên thẻ (inclusive). Tiếp đón không bị chặn bởi thẻ hết hạn, chưa đến ngày bắt đầu hoặc chưa biết ngày bắt đầu. Ghi nhận/kiểm tra tính hợp lệ của khoảng ngày không phải là xác minh hay adjudication quyền lợi thanh toán.

**Ranh giới và bằng chứng hiện tại:** Handler BHYT đã dùng `DateRange` để xác thực khoảng; test handler bao phủ start chưa biết, ngày hết hạn, ngày tương lai, cùng ngày và khoảng đảo. SQL Server read-back riêng cho các cạnh ngày này chưa được xác nhận. Không dùng `contains` hoặc trạng thái ngày để adjudicate hay chặn tiếp đón; xem [`phase0-value-object-domain-review.md`](phase0-value-object-domain-review.md) và task board để biết phạm vi acceptance.

## 2. `PhoneNumber` — số liên hệ bệnh nhân

**Hiện trạng:** `PhoneNumber` chỉ nhận số di động Việt Nam theo prefix hiện có; bỏ khoảng trắng, dấu chấm và gạch nối nhưng không chuyển đổi `0...` sang `+84...` ([`PhoneNumber.java`](../../clinic-domain/src/main/java/vn/clinicmanagement/domain/common/valueobjects/PhoneNumber.java#L7-L15)). Tiếp đón cho phép số chính và số liên hệ khẩn cấp vắng mặt; số đã nhập được chuẩn hóa trước khi lưu.

| Lựa chọn đã thảo luận trước quyết định Owner | Phương án | Ghi chú khi thảo luận |
|---|---|---|
| Số chính của bệnh nhân có bắt buộc không? | A. Không bắt buộc; nếu nhập thì validate. B. Bắt buộc số hợp lệ để đăng ký. | A giảm nguy cơ chặn tiếp đón khi người bệnh không có số hoặc không muốn cung cấp. |
| Số khẩn cấp có bắt buộc không? | A. Không; null/blank được giữ là vắng dữ liệu. B. Bắt buộc khi đăng ký. | A phù hợp với trường hiện đang tùy chọn. |
| Loại số nào được chấp nhận? | A. Di động Việt Nam theo bộ prefix hiện có. B. Thêm cố định/bàn hoặc quốc tế. | A giới hạn thay đổi đầu tiên; chỉ chọn nếu nghiệp vụ xác nhận không cần số bàn/ngoại quốc. |
| Lưu và tìm kiếm số thế nào? | A. Lưu nguyên dạng đã chuẩn hóa dấu phân cách, giữ prefix `0`/`+84`. B. Chuẩn hóa mọi số về một dạng quốc gia. | A ít thay đổi dữ liệu; tìm kiếm cần thử được cả cách nhập tương đương mà không mở rộng tìm kiếm ngoài scope. |

**Quyết định Owner — 06/11/2026:** số chính và số khẩn cấp đều tùy chọn; chỉ validate khi có nhập theo bộ quy tắc mobile Việt Nam hiện có; bỏ các dấu phân cách được hỗ trợ (khoảng trắng, dấu chấm, gạch nối) khi lưu; giữ nguyên prefix `0...` hoặc `+84...` như đã nhập, không chuyển đổi giữa hai dạng. Không backfill dữ liệu cũ.

**Ranh giới và bằng chứng hiện tại:** test ứng dụng bao phủ số tùy chọn, validation và giá trị canonical; SQL Server opt-in đã xác nhận form-originated insert/update và đọc lại giá trị canonical qua tìm bệnh nhân theo CCCD. Tìm kiếm bằng số với cách nhập tương đương và visual/interaction acceptance toàn `ReceptionPanel` chưa được nghiệm thu. Không tự động sửa số legacy không hợp lệ.

## 3. `Money` — các trường tiền

**Hiện trạng:** `Money` hiện chỉ chấp nhận VND và vẫn làm tròn về hai chữ số bằng `HALF_UP` khi tạo đối tượng ([`Money.java`](../../clinic-domain/src/main/java/vn/clinicmanagement/domain/common/valueobjects/Money.java#L8-L25)). Đây là hành vi code hiện tại, không phải phê duyệt áp dụng cách làm tròn này tại bất kỳ ranh giới hóa đơn nào. Các model/schema có trường tiền `BigDecimal`/`DECIMAL(18,2)`, nhưng không lưu currency code; chưa có consumer production cho hóa đơn.

| Lựa chọn đã thảo luận trước quyết định Owner | Phương án | Ghi chú khi thảo luận |
|---|---|---|
| Phạm vi tiền tệ? | A. Chỉ VND cho phiên bản đầu. B. Hỗ trợ nhiều currency có currency code trên chứng từ. | A phù hợp phạm vi phòng khám nội địa nếu nghiệp vụ xác nhận; B đòi hỏi schema và kiểm soát tỷ giá rõ ràng. |
| Làm tròn ở đâu? | A. Làm tròn đơn giá và từng dòng. B. Giữ độ chính xác cao trong dòng, làm tròn ở tổng chứng từ. | Chưa đủ căn cứ chọn; kế toán cần xác nhận cách tính tiền dịch vụ, giảm trừ, BHYT và hoàn tiền. |
| Áp dụng cho nhóm nào? | A. Chỉ dịch vụ/viện phí. B. Bao gồm thuốc, kho, công nợ, đặt cọc và lương. | A cho phép tách hợp đồng hóa đơn; các nhóm khác cần quy tắc riêng. Khoản đặt cọc lịch hẹn hiện vẫn `DENY`. |
| Constructor có tự làm tròn không? | A. Có, scale cố định 2. B. Không; làm tròn tại boundary đã được nghiệp vụ chỉ định. | B tránh làm tròn lặp ngoài ý muốn, nhưng yêu cầu quy tắc scale/rounding được chốt trước khi có consumer. |

**Quyết định Owner — 06/11/2026:** chỉ dùng VND. Chưa phê duyệt consumer hóa đơn/viện phí hoặc cách làm tròn tại bất kỳ ranh giới hóa đơn nào. Trước khi mở consumer như vậy, Owner phải có một quyết định riêng chốt cả điểm làm tròn và phạm vi field áp dụng. Hành vi hiện tại của `Money` (hai chữ số, `HALF_UP`) không phải phê duyệt làm tròn hóa đơn. Quyết định này cũng không cho phép mở thanh toán, đặt cọc, giảm trừ hay hoàn tiền.

**Gate consumer:** không thêm consumer invoice/billing cho tới khi Owner quyết định riêng điểm làm tròn và field scope. Khi được quyết định, acceptance tương ứng phải chứng minh đúng các field và boundary được phê duyệt với dữ liệu tổng hợp; quyết định này không mở rộng sang nghiệp vụ tiền khác. Không ghi đơn giá hoặc dữ liệu thật vào log.

## Trạng thái triển khai hẹp

1. `PhoneNumber`: acceptance canonical persistence/read-back hẹp đã có; visual/interaction acceptance và tìm kiếm bằng định dạng số tương đương vẫn mở.
2. `DateRange`: đã hỗ trợ start chưa biết và được nối vào validation khoảng BHYT; không biến null thành giả định quyền lợi hay quyết định tiếp đón.
3. `Money`: chỉ VND; không tích hợp consumer hóa đơn trước quyết định riêng về rounding point và field scope.

## Không thuộc quyết định của tài liệu này

- Không cấp quyền `APPOINTMENT`, `BILLING` hoặc `AUDIT_READ`; không thay đổi role/scope đã duyệt.
- Không bật đặt cọc, thanh toán, xác minh quyền lợi BHYT hay quyết định y khoa.
- Chưa cho phép thay schema, migration, format dữ liệu hiện hữu hoặc UI ngoài task consumer có acceptance riêng.
- Không dùng dữ liệu bệnh nhân thật; mọi integration test về sau phải dùng dữ liệu tổng hợp.

## Nguồn

- [`phase0-value-object-consumer-audit.md`](phase0-value-object-consumer-audit.md)
- [`03-model-task-board.md`](03-model-task-board.md), task `PLAT-VO-01`
- `clinic-domain/src/main/java/vn/clinicmanagement/domain/common/valueobjects/{DateRange,Money,PhoneNumber}.java`
- `clinic-domain/src/main/java/vn/clinicmanagement/domain/patient/BaoHiem.java`
- `clinic-application/src/main/java/vn/clinicmanagement/application/reception/RegisterPatientHandler.java`
- `clinic-infrastructure/src/main/resources/migrations/V1__Initial_Clinic_Schema.sql`
