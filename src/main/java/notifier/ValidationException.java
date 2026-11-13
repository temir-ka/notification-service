package notifier;

/** Ошибка во входных данных клиента. Превращается в HTTP 400. */
public class ValidationException extends RuntimeException {
    public ValidationException(String message) {
        super(message);
    }
}
