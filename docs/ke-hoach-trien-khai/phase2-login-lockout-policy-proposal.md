# Đề xuất chính sách giới hạn đăng nhập sai

**Task:** `SEC-LOCKOUT-POLICY-01`  
**Trạng thái:** Owner duyệt phương án B ngày 06/11/2026; triển khai theo task riêng, chưa đổi code/schema.
**Ngày:** 06/11/2026

## Hiện trạng đã kiểm tra

- `AuthenticateUserHandler` trả cùng thông báo khi username không tồn tại, nhân viên bị vô hiệu hóa/xóa mềm hoặc mật khẩu sai; mỗi lần thất bại ghi `AUTH_LOGIN_FAILED` bằng audit độc lập. Lỗi ghi audit làm đăng nhập thất bại an toàn.
- Bảng `NhanVien` hiện không có bộ đếm thất bại, thời điểm thất bại gần nhất hoặc thời điểm cho phép đăng nhập lại. Bộ đếm trong bộ nhớ của một ứng dụng desktop không đủ vì nhiều máy có thể cùng dùng một SQL Server.
- Session metadata dùng UTC theo quyết định `DEC-TIME-01`; `AuthenticateUserHandler` nhận `Clock`, thuận lợi cho kiểm thử thời gian.
- `SEC-01` vẫn chưa hoàn tất. Chính sách này chỉ chốt rate limiting đăng nhập; không tự cấp quyền, đổi thông báo phân quyền, hay triển khai quản trị tài khoản.

## Cơ sở tham khảo

- NIST SP 800-63B-4 yêu cầu cơ chế giới hạn tốc độ thử xác thực; nêu trần tối đa 100 lần thất bại liên tiếp cho một authenticator trong trường hợp mặc định, và gợi ý tăng thời gian chờ để giảm nguy cơ người khác khóa người dùng hợp lệ. Đây là hướng dẫn cho verifier, không phải ngưỡng mặc định nên bê nguyên vào ứng dụng này. [NIST SP 800-63B, §3.2.2](https://pages.nist.gov/800-63-4/sp800-63b.html)
- OWASP khuyến nghị gắn bộ đếm vào tài khoản, cân nhắc threshold, observation window và lockout duration, đồng thời lưu ý việc khóa cứng có thể bị lạm dụng để gây từ chối dịch vụ. [OWASP Authentication Cheat Sheet — Login Throttling](https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html#login-throttling)

## Phương án

| Phương án | Chính sách | Ưu điểm | Rủi ro / chi phí |
|---|---|---|---|
| **A — Khóa tạm (đề xuất mặc định đơn giản)** | 5 lần sai liên tiếp trong 15 phút thì khóa tài khoản 15 phút; hết hạn tự mở; đăng nhập thành công reset bộ đếm; không tự khóa vĩnh viễn. Không có mở khóa thủ công cho tới khi có quy trình HR/Admin an toàn. | Dễ hiểu, dễ triển khai và kiểm thử; giới hạn thử mật khẩu rõ ràng. | Người khác có thể cố tình làm tài khoản bị khóa; trong 15 phút nhân viên đó không thể đăng nhập. |
| **B — Trì hoãn tăng dần (khuyến nghị về giảm DoS)** | Sau 5 lần sai trong cửa sổ 15 phút, lần thử kế tiếp phải chờ 30 giây; thời gian chờ tăng gấp đôi theo các lần sai kế tiếp, tối đa 15 phút. Không chuyển tài khoản sang trạng thái khóa cứng; thành công reset chuỗi sai; chuỗi sai hết hiệu lực sau 24 giờ không có lần sai mới. | Giảm tốc độ brute force nhưng khó bị lợi dụng để khóa cứng tài khoản nhân viên. | Luật nhiều trạng thái hơn; người dùng có thể phải chờ; cần chốt cách hiển thị thời gian chờ mà không lộ sự tồn tại của username. |
| **C — Chưa giới hạn đăng nhập sai** | Giữ hành vi hiện tại. | Không có khóa nhầm do chính sách. | Không có lớp giới hạn tốc độ ở tầng tài khoản; không phù hợp với mục tiêu bảo vệ thông tin xác thực. |

**Khuyến nghị:** chọn **B** vì ứng dụng desktop có nguy cơ bị người khác cố ý nhập sai để làm gián đoạn tài khoản nhân viên. Nếu ưu tiên quy tắc đơn giản và chấp nhận khóa tạm do người khác gây ra, chọn A. Không khuyến nghị C.

## Quyết định của Owner — 06/11/2026

Owner chọn **B — trì hoãn tăng dần**. Policy được duyệt: sau 5 lần sai trong cửa sổ 15 phút, lần thử tiếp theo phải chờ 30 giây; thời gian chờ tăng gấp đôi theo các lần sai kế tiếp, tối đa 15 phút. Không khóa cứng tài khoản; đăng nhập thành công reset chuỗi sai; chuỗi sai hết hiệu lực sau 24 giờ không có lần sai mới. Tiếp tục dùng thông báo xác thực chung, lưu trạng thái nguyên tử theo tài khoản trong SQL Server và audit failure/throttle fail-closed như các điều khoản đề xuất. Implementation phải giữ nguyên tài khoản bị HR/Admin vô hiệu hóa; không thêm flow mở khóa thủ công. Chỉ áp dụng trên dữ liệu tổng hợp trong acceptance.

**Chi tiết triển khai Owner duyệt:** Cửa sổ 15 phút là fixed window bắt đầu tại lần sai đầu; tại đúng mốc 15 phút, cửa sổ trước hết hạn và bộ đếm trước ngưỡng bắt đầu mới. Sau 5 lỗi trong cửa sổ, yêu cầu kế tiếp bị delay trước khi kiểm tra mật khẩu. Yêu cầu đến trong lúc delay còn hiệu lực chỉ ghi audit throttle và không tăng cấp delay. Sau khi delay hết, nếu mật khẩu sai thì áp dụng mức delay tiếp theo. Mật khẩu đúng của tài khoản còn hoạt động reset throttle ngay sau xác minh, trước kiểm tra role/scope; tài khoản inactive/deleted không được reset trạng thái hay đăng nhập. SQL Server `SYSUTCDATETIME()` là nguồn thời gian chuẩn cho mọi so sánh/thời điểm lưu giữa nhiều máy. Sequence/delay hết sau 24 giờ kể từ lỗi gần nhất; success reset ngay.

**Triển khai:** Contract Orchestrator đầu `a2c4ea7f-db14-4933-b8fc-a658972af395` được thay bằng contract chi tiết đã Owner duyệt `c67f6998-c9d4-402a-9fd0-008f2616c9eb`. Các lượt task trước bị dừng/thất bại trước khi tích hợp để mở rộng đúng file scope/lưu vết audit và làm rõ semantics. Task hiện tại `30d87478-4db8-4d0a-8a5d-a4677f0d22a3` đang chạy bằng Copilot CLI; xem task board để theo dõi validation. Chưa có code/migration được tích hợp; runtime acceptance còn chờ review.

## Quy tắc chung đề nghị áp dụng

1. Đếm theo tài khoản trong SQL Server, không theo tiến trình desktop; phép tăng bộ đếm và tính thời điểm thử tiếp theo phải nguyên tử khi nhiều máy đăng nhập đồng thời.
2. Không phân biệt username không tồn tại, mật khẩu sai, tài khoản đang bị trì hoãn hoặc tài khoản không hoạt động qua thông báo đăng nhập. Giữ phản hồi xác thực chung; không trả thời điểm khóa hay trạng thái tài khoản cho client.
3. Ghi audit `AUTH_LOGIN_FAILED` cho lỗi thông tin xác thực và một reason code riêng khi bị throttle. Audit lỗi phải fail closed như chính sách login hiện tại; không ghi password, hash hoặc raw exception.
4. Timestamp dùng UTC. Đăng nhập thành công xóa trạng thái thất bại; hết hạn chính sách không tự kích hoạt tài khoản đã bị quản trị viên vô hiệu hóa.
5. Migration phải thêm cột nullable/default-safe cho database hiện có và không sửa dữ liệu nhân viên. Kiểm thử chỉ dùng SQL Server synthetic và clock giả lập.
6. Chỉ bắt đầu implementation sau khi chủ dự án chọn threshold, window và thời hạn/kiểu khóa. Không suy diễn từ prototype `AUTO_LOGOUT_MINUTES`.

## Các quyết định cần chủ dự án chọn

- Chọn **A**, **B** hoặc **C**.
- Nếu chọn A: xác nhận ngưỡng `5 lần / 15 phút`, khóa `15 phút`, tự mở khóa khi hết hạn, không có mở khóa thủ công trong task này.
- Nếu chọn B: xác nhận bắt đầu throttle sau `5 lần / 15 phút`, chờ đầu `30 giây`, tăng gấp đôi tới tối đa `15 phút`, reset khi đăng nhập thành công, hết chuỗi sau `24 giờ` không có lần sai.

Khi được duyệt, task triển khai sẽ bao gồm migration, domain/repository, transaction chống race, audit reason/action, generic UI message, unit test với `Clock` giả và SQL Server opt-in test trên database synthetic; không dùng dữ liệu bệnh nhân hay database thật.
