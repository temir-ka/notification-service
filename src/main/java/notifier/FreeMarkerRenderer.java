package notifier;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.TemplateNotFoundException;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

/** Рендерит шаблоны *.ftl из classpath: src/main/resources/templates. */
public final class FreeMarkerRenderer implements TemplateRenderer {

    private final Configuration cfg;

    public FreeMarkerRenderer() {
        cfg = new Configuration(Configuration.VERSION_2_3_33);
        cfg.setClassForTemplateLoading(FreeMarkerRenderer.class, "/templates");
        cfg.setDefaultEncoding("UTF-8");
        // Ошибка в шаблоне или пропущенный параметр = исключение, а не "тихая" пустая строка.
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        cfg.setLogTemplateExceptions(false);
    }

    @Override
    public String render(String templateName, Map<String, Object> params) {
        try {
            Template template = cfg.getTemplate(templateName + ".ftl");
            StringWriter out = new StringWriter();
            template.process(params, out);
            return out.toString();
        } catch (TemplateNotFoundException e) {
            throw new UnknownTemplateException(templateName);
        } catch (IOException | TemplateException e) {
            throw new RenderException("cannot render template '" + templateName + "'", e);
        }
    }
}
