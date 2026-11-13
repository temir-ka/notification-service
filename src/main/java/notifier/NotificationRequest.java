package notifier;

import java.util.Map;

/** Тело запроса POST /notifications. Неизвестные поля в JSON отклоняются (400). */
public record NotificationRequest(
        String channel,
        String recipient,
        String template,
        Map<String, Object> params) {
}
