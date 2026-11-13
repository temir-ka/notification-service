package notifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class NotificationServiceTest {

    private final List<Message> sent = new ArrayList<>();
    private final TemplateRenderer renderer = (name, params) -> "[" + name + "] " + params.get("name");
    private final NotificationService service = new NotificationService(renderer, sent::add);

    private static NotificationRequest req(String channel, String to, String template) {
        return new NotificationRequest(channel, to, template, Map.of("name", "Айдар"));
    }

    @Test
    void rendersTemplateAndPassesMessageToSender() {
        NotificationResult result = service.send(req("whatsapp", "+77001234567", "welcome"));

        assertEquals("SENT", result.status());
        assertEquals("[welcome] Айдар", result.message());
        assertEquals(1, sent.size());
        assertEquals("+77001234567", sent.get(0).recipient());
        assertEquals(result.id(), sent.get(0).id());
    }

    @Test
    void channelIsCaseInsensitive() {
        assertEquals("sms", service.send(req("SMS", "+77001234567", "welcome")).channel());
    }

    @Test
    void rejectsUnknownChannel() {
        assertThrows(ValidationException.class, () -> service.send(req("telegram", "x", "welcome")));
        assertTrue(sent.isEmpty());
    }

    @Test
    void rejectsMissingFields() {
        assertThrows(ValidationException.class, () -> service.send(null));
        assertThrows(ValidationException.class, () -> service.send(req("sms", " ", "welcome")));
        assertThrows(ValidationException.class, () -> service.send(req(null, "x", "welcome")));
        assertThrows(ValidationException.class, () -> service.send(req("sms", "x", null)));
    }

    @Test
    void rejectsTemplateNamesThatLookLikePaths() {
        assertThrows(ValidationException.class, () -> service.send(req("sms", "x", "../secret")));
        assertThrows(ValidationException.class, () -> service.send(req("sms", "x", "a/b")));
        assertThrows(ValidationException.class, () -> service.send(req("sms", "x", "Welcome")));
        assertTrue(sent.isEmpty());
    }

    @Test
    void unknownTemplateBecomesValidationError() {
        NotificationService s = new NotificationService(
                (name, params) -> { throw new UnknownTemplateException(name); }, sent::add);
        assertThrows(ValidationException.class, () -> s.send(req("sms", "x", "nope")));
        assertTrue(sent.isEmpty());
    }

    @Test
    void renderErrorIsNotSwallowedAndNothingIsSent() {
        NotificationService s = new NotificationService(
                (name, params) -> { throw new RenderException("boom", null); }, sent::add);
        assertThrows(RenderException.class, () -> s.send(req("sms", "x", "welcome")));
        assertTrue(sent.isEmpty());
    }

    @Test
    void nullParamsAreTreatedAsEmpty() {
        NotificationService s = new NotificationService((name, params) -> "n=" + params.size(), sent::add);
        assertEquals("n=0", s.send(new NotificationRequest("sms", "x", "welcome", null)).message());
    }
}
