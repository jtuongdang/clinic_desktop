package vn.clinicmanagement.application.inventory;

/** Trường `itemId`: mathang mã. */
/** Trường `maItem`: `ma` mathang. */
/** Trường `tenItem`: `ten` mathang. */
public record CreateMatHangBienNhan(int itemId, String maItem, String tenItem) {
}
