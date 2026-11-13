package notifier;

/** Готовое к отправке сообщение: то, что получает Sender. */
public record Message(String id, String channel, String recipient, String text) {
}
