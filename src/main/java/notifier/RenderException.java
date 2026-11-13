package notifier;

/** Шаблон найден, но не отрисовался (например, не хватает параметра). Превращается в HTTP 422. */
public class RenderException extends RuntimeException {
    public RenderException(String message, Throwable cause) {
        super(message, cause);
    }
}
