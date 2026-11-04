package vn.clinicmanagement.domain.repositories;

import java.util.function.Predicate;

public interface ISpecification<T> {
    boolean isSatisfiedBy(T candidate);
    Predicate<T> toPredicate();
}
