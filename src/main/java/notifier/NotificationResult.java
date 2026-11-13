package notifier;

/** Ответ API после отправки уведомления. */
public record NotificationResult(
        String id,
        String status,
        String channel,
        String recipient,
        String message) {
}
