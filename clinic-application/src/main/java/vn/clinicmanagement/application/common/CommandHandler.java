package vn.clinicmanagement.application.common;

@FunctionalInterface
public interface CommandHandler<TCommand extends Command<TResult>, TResult> {
    Result<TResult> handle(TCommand command);
}
