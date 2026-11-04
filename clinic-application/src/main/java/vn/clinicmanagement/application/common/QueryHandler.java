package vn.clinicmanagement.application.common;

@FunctionalInterface
public interface QueryHandler<TQuery extends Query<TResult>, TResult> {
    Result<TResult> handle(TQuery query);
}
