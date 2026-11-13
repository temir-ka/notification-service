package notifier;

import java.util.Map;

/** Превращает имя шаблона и параметры в текст сообщения. */
@FunctionalInterface
public interface TemplateRenderer {
    /**
     * @throws UnknownTemplateException если шаблона нет
     * @throws RenderException          если шаблон не удалось отрисовать
     */
    String render(String templateName, Map<String, Object> params);
}
