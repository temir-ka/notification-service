package notifier;

/**
 * Канал доставки. Здесь можно подключить реальный WhatsApp / SMS / email.
 * В проекте есть только заглушка {@link LogSender}.
 */
@FunctionalInterface
public interface Sender {
    void send(Message message);
}
