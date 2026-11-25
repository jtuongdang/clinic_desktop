package vn.clinicmanagement.application.common;

@FunctionalInterface
public interface CommandHandler<TCommand extends Command<TResult>, TResult> {
    KetQua<TResult> handle(TCommand command);
}
