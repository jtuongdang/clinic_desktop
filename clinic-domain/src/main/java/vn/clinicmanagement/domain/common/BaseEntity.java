package vn.clinicmanagement.domain.common;

import vn.clinicmanagement.domain.common.events.IDomainEvent;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Enterprise BaseEntity hỗ trợ Generic Id, Audit Trail, Soft Delete,
 * Optimistic Concurrency Locking (version) và Domain Event Hub.
 */
public abstract class BaseEntity<TId> {
    private TId id;
    private Long version = 0L;

    private LocalDateTime createdAt;
    private Integer createdBy;
    private LocalDateTime updatedAt;
    private Integer updatedBy;

    private boolean isDeleted;
    private LocalDateTime deletedAt;
    private Integer deletedBy;

    private transient List<IDomainEvent> domainEvents = new ArrayList<>();

    protected BaseEntity() {
        this.createdAt = LocalDateTime.now();
        this.isDeleted = false;
        this.version = 0L;
    }

    public TId getId() { return id; }
    public void setId(TId id) { this.id = id; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Integer getCreatedBy() { return createdBy; }
    public void setCreatedBy(Integer createdBy) {
        if (createdBy != null && createdBy <= 0) {
            throw new IllegalArgumentException("Người tạo bản ghi phải là người dùng hợp lệ.");
        }
        this.createdBy = createdBy;
    }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Integer getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(Integer updatedBy) { this.updatedBy = updatedBy; }

    public boolean isDeleted() { return isDeleted; }
    public void setDeleted(boolean deleted) { isDeleted = deleted; }

    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }

    public Integer getDeletedBy() { return deletedBy; }
    public void setDeletedBy(Integer deletedBy) { this.deletedBy = deletedBy; }

    public void markAsUpdated(int updatedBy) {
        this.updatedAt = LocalDateTime.now();
        this.updatedBy = updatedBy;
        if (this.version != null) this.version++;
    }

    public void softDelete(int deletedBy) {
        if (this.isDeleted) return;
        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();
        this.deletedBy = deletedBy;
        if (this.version != null) this.version++;
    }

    public void restore() {
        if (!this.isDeleted) return;
        this.isDeleted = false;
        this.deletedAt = null;
        this.deletedBy = null;
        if (this.version != null) this.version++;
    }

    public void registerDomainEvent(IDomainEvent event) {
        if (this.domainEvents == null) this.domainEvents = new ArrayList<>();
        this.domainEvents.add(event);
    }

    public List<IDomainEvent> getDomainEvents() {
        if (this.domainEvents == null) return Collections.emptyList();
        return Collections.unmodifiableList(this.domainEvents);
    }

    public void clearDomainEvents() {
        if (this.domainEvents != null) this.domainEvents.clear();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BaseEntity<?> that = (BaseEntity<?>) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
