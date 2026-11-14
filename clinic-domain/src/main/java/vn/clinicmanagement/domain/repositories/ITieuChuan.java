package vn.clinicmanagement.domain.repositories;

import java.util.function.Predicate;

public interface ITieuChuan<T> {
    /** Hàm `isSatisfiedBy`: là `Satisfied` theo. */
    boolean isSatisfiedBy(T candidate);
    /** Hàm `toPredicate`: den `Predicate`. */
    Predicate<T> toPredicate();
}
