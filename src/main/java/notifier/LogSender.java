package notifier;

/** Заглушка: вместо отправки печатает сообщение в консоль. */
public final class LogSender implements Sender {
    @Override
    public void send(Message m) {
        System.out.printf("[SEND] id=%s channel=%s to=%s%n%s%n", m.id(), m.channel(), m.recipient(), m.text());
    }
}
