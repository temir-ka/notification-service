package notifier;

/** Шаблон с таким именем не найден. */
public class UnknownTemplateException extends RuntimeException {
    public UnknownTemplateException(String name) {
        super("unknown template: " + name);
    }
}
