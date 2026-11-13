package notifier;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/** Бизнес-логика: проверить запрос, отрисовать шаблон, передать сообщение Sender'у. */
public final class NotificationService {

    static final Set<String> CHANNELS = Set.of("whatsapp", "sms", "email");

    // Имя шаблона приходит от клиента и превращается в имя файла.
    // Разрешаем только простые имена, чтобы нельзя было передать "../../что-то".
    private static final Pattern TEMPLATE_NAME = Pattern.compile("[a-z][a-z0-9_]{0,63}");

    private final TemplateRenderer renderer;
    private final Sender sender;

    public NotificationService(TemplateRenderer renderer, Sender sender) {
        this.renderer = renderer;
        this.sender = sender;
    }

    public NotificationResult send(NotificationRequest request) {
        if (request == null) {
            throw new ValidationException("request body is empty");
        }
        String channel = requireText(request.channel(), "channel").toLowerCase(Locale.ROOT);
        String recipient = requireText(request.recipient(), "recipient");
        String template = requireText(request.template(), "template");

        if (!CHANNELS.contains(channel)) {
            throw new ValidationException("unsupported channel: " + channel + ", allowed: " + CHANNELS);
        }
        if (!TEMPLATE_NAME.matcher(template).matches()) {
            throw new ValidationException("invalid template name");
        }
        Map<String, Object> params = request.params() == null ? Map.of() : request.params();

        String text;
        try {
            text = renderer.render(template, params);
        } catch (UnknownTemplateException e) {
            throw new ValidationException(e.getMessage());
        }

        String id = UUID.randomUUID().toString();
        sender.send(new Message(id, channel, recipient, text));
        return new NotificationResult(id, "SENT", channel, recipient, text);
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new ValidationException("field '" + field + "' is required");
        }
        return value.trim();
    }
}
