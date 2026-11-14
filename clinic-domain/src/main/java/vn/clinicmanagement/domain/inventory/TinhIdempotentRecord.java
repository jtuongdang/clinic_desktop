package vn.clinicmanagement.domain.inventory;
import vn.clinicmanagement.domain.common.CoSoThucThe;
import java.time.LocalDateTime;

public class TinhIdempotentRecord extends CoSoThucThe<Integer> {
    /** Trường `idempotencyKey`: tinhidempotent `Key`. */
    private String idempotencyKey = "";
    /** Trường `operationName`: `operation` tên. */
    private String operationName = "";
    private String responsePayload;
    /** Trường `statusCode`: trạng thái mã. */
    private int statusCode = 200;
    /** Trường `expiryTime`: `expiry` thời gian. */
    private LocalDateTime expiryTime = LocalDateTime.now().plusHours(24);

    public TinhIdempotentRecord() {}
    /** Hàm `getIdempotencyKey`: lấy tinhidempotent `Key`. */
    public String getIdempotencyKey() { return idempotencyKey; }
    /** Hàm `setIdempotencyKey`: gán tinhidempotent `Key`. */
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    /** Hàm `getOperationName`: lấy `Operation` tên. */
    public String getOperationName() { return operationName; }
    /** Hàm `setOperationName`: gán `Operation` tên. */
    public void setOperationName(String operationName) { this.operationName = operationName; }
    /** Hàm `getResponsePayload`: lấy `Response` `Payload`. */
    public String getResponsePayload() { return responsePayload; }
    /** Hàm `setResponsePayload`: gán `Response` `Payload`. */
    public void setResponsePayload(String responsePayload) { this.responsePayload = responsePayload; }
    /** Hàm `getStatusCode`: lấy trạng thái mã. */
    public int getStatusCode() { return statusCode; }
    /** Hàm `setStatusCode`: gán trạng thái mã. */
    public void setStatusCode(int statusCode) { this.statusCode = statusCode; }
    /** Hàm `getExpiryTime`: lấy `Expiry` thời gian. */
    public LocalDateTime getExpiryTime() { return expiryTime; }
    /** Hàm `setExpiryTime`: gán `Expiry` thời gian. */
    public void setExpiryTime(LocalDateTime expiryTime) { this.expiryTime = expiryTime; }
}
