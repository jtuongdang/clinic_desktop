package vn.clinicmanagement.domain.common;

import vn.clinicmanagement.domain.common.events.IMienNghiepVuSuKien;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Enterprise BaseEntity hỗ trợ Generic Id, Audit Trail, Soft Delete,
 * Optimistic Concurrency Locking (version) và Domain Event Hub.
 */
public abstract class CoSoThucThe<TId> {
    /** Trường `id`: mã. */
    private TId id;
    private Long version = 0L;

    /** Trường `createdAt`: datao luc. */
    private LocalDateTime createdAt;
    /** Trường `createdBy`: datao theo. */
    private Integer createdBy;
    /** Trường `updatedAt`: `updated` luc. */
    private LocalDateTime updatedAt;
    /** Trường `updatedBy`: `updated` theo. */
    private Integer updatedBy;

    /** Trường `isDeleted`: là `Deleted`. */
    private boolean isDeleted;
    /** Trường `deletedAt`: `deleted` luc. */
    private LocalDateTime deletedAt;
    /** Trường `deletedBy`: `deleted` theo. */
    private Integer deletedBy;

    /** Trường `domainEvents`: miennghiepvu sukien. */
    private transient List<IMienNghiepVuSuKien> domainEvents = new ArrayList<>();

    protected CoSoThucThe() {
        this.createdAt = LocalDateTime.now();
        this.isDeleted = false;
        this.version = 0L;
    }

    /** Hàm `getId`: lấy mã. */
    public TId getId() { return id; }
    /** Hàm `setId`: gán mã. */
    public void setId(TId id) { this.id = id; }

    /** Hàm `getVersion`: lấy `Version`. */
    public Long getVersion() { return version; }
    /** Hàm `setVersion`: gán `Version`. */
    public void setVersion(Long version) { this.version = version; }

    /** Hàm `getCreatedAt`: lấy datao luc. */
    public LocalDateTime getCreatedAt() { return createdAt; }
    /** Hàm `setCreatedAt`: gán datao luc. */
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    /** Hàm `getCreatedBy`: lấy datao theo. */
    public Integer getCreatedBy() { return createdBy; }
    /** Hàm `setCreatedBy`: gán datao theo. */
    public void setCreatedBy(Integer createdBy) {
        if (createdBy != null && createdBy <= 0) {
            throw new IllegalArgumentException("Người tạo bản ghi phải là người dùng hợp lệ.");
        }
        this.createdBy = createdBy;
    }

    /** Hàm `getUpdatedAt`: lấy `Updated` luc. */
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    /** Hàm `setUpdatedAt`: gán `Updated` luc. */
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    /** Hàm `getUpdatedBy`: lấy `Updated` theo. */
    public Integer getUpdatedBy() { return updatedBy; }
    /** Hàm `setUpdatedBy`: gán `Updated` theo. */
    public void setUpdatedBy(Integer updatedBy) {
        validateNullableActor(updatedBy, "Người cập nhật bản ghi phải là người dùng hợp lệ.");
        this.updatedBy = updatedBy;
    }

    /** Hàm `isDeleted`: là `Deleted`. */
    public boolean isDeleted() { return isDeleted; }
    /** Hàm `setDeleted`: gán `Deleted`. */
    public void setDeleted(boolean deleted) { isDeleted = deleted; }

    /** Hàm `getDeletedAt`: lấy `Deleted` luc. */
    public LocalDateTime getDeletedAt() { return deletedAt; }
    /** Hàm `setDeletedAt`: gán `Deleted` luc. */
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }

    /** Hàm `getDeletedBy`: lấy `Deleted` theo. */
    public Integer getDeletedBy() { return deletedBy; }
    /** Hàm `setDeletedBy`: gán `Deleted` theo. */
    public void setDeletedBy(Integer deletedBy) {
        validateNullableActor(deletedBy, "Người xóa bản ghi phải là người dùng hợp lệ.");
        this.deletedBy = deletedBy;
    }

    public void markAsUpdated(int updatedBy) {
        validateActor(updatedBy, "Người cập nhật bản ghi phải là người dùng hợp lệ.");
        this.updatedAt = LocalDateTime.now();
        this.updatedBy = updatedBy;
    }

    /** Hàm `softDelete`: `soft` xóa. */
    public void softDelete(int deletedBy) {
        validateActor(deletedBy, "Người xóa bản ghi phải là người dùng hợp lệ.");
        if (this.isDeleted) return;
        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();
        this.deletedBy = deletedBy;
    }

    public void restore() {
        if (!this.isDeleted) return;
        this.isDeleted = false;
        this.deletedAt = null;
        this.deletedBy = null;
    }

    /** Hàm `validateNullableActor`: kiểm tra hợp lệ `Nullable` tacnhan. */
    private static void validateNullableActor(Integer actorId, String message) {
        if (actorId != null) validateActor(actorId, message);
    }

    /** Hàm `validateActor`: kiểm tra hợp lệ tacnhan. */
    private static void validateActor(int actorId, String message) {
        if (actorId <= 0) throw new IllegalArgumentException(message);
    }

    /** Hàm `registerDomainEvent`: đăng ký miennghiepvu sukien. */
    public void registerDomainEvent(IMienNghiepVuSuKien event) {
        Objects.requireNonNull(event, "event must not be null");
        if (this.domainEvents == null) this.domainEvents = new ArrayList<>();
        this.domainEvents.add(event);
    }

    /** Hàm `getDomainEvents`: lấy miennghiepvu sukien. */
    public List<IMienNghiepVuSuKien> getDomainEvents() {
        if (this.domainEvents == null) return Collections.emptyList();
        return Collections.unmodifiableList(new ArrayList<>(this.domainEvents));
    }

    /** Hàm `pullDomainEvents`: `pull` miennghiepvu sukien. */
    public List<IMienNghiepVuSuKien> pullDomainEvents() {
        if (this.domainEvents == null || this.domainEvents.isEmpty()) {
            return Collections.emptyList();
        }
        List<IMienNghiepVuSuKien> events = new ArrayList<>(this.domainEvents);
        this.domainEvents.clear();
        return Collections.unmodifiableList(events);
    }

    /** Hàm `clearDomainEvents`: xóa miennghiepvu sukien. */
    public void clearDomainEvents() {
        if (this.domainEvents != null) this.domainEvents.clear();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CoSoThucThe<?> that = (CoSoThucThe<?>) o;
        return id != null && Objects.equals(id, that.id);
    }

    /** Hàm `hashCode`: `hash` mã. Gốc khai báo tại `Object#hashCode`. */
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
