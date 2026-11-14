package vn.clinicmanagement.domain.common.events;

public interface IMienNghiepVuSuKienHandler<T extends IMienNghiepVuSuKien> {
    void handle(T event);
}
