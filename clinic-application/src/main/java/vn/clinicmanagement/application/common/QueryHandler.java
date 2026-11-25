package vn.clinicmanagement.application.common;

@FunctionalInterface
public interface QueryHandler<TQuery extends Query<TResult>, TResult> {
    KetQua<TResult> handle(TQuery query);
}
