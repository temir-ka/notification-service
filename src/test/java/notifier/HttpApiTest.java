package notifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpServer;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HttpApiTest {

    private final List<Message> sent = new ArrayList<>();
    private final HttpClient http = HttpClient.newHttpClient();
    private HttpServer server;
    private String base;

    @BeforeEach
    void start() throws Exception {
        TemplateRenderer renderer = (name, params) -> {
            if (name.equals("missing")) throw new UnknownTemplateException(name);
            if (name.equals("broken")) throw new RenderException("broken template", null);
            return "hello " + params.get("name");
        };
        server = new HttpApi(new NotificationService(renderer, sent::add)).start(0);
        base = "http://localhost:" + server.getAddress().getPort();
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    private HttpResponse<String> post(String json) throws Exception {
        HttpRequest r = HttpRequest.newBuilder(URI.create(base + "/notifications"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();
        return http.send(r, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    @Test
    void healthReturnsOk() throws Exception {
        HttpResponse<String> r = http.send(HttpRequest.newBuilder(URI.create(base + "/health")).build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, r.statusCode());
        assertTrue(r.body().contains("ok"));
    }

    @Test
    void validRequestIsSent() throws Exception {
        HttpResponse<String> r = post("{\"channel\":\"whatsapp\",\"recipient\":\"+77001234567\","
                + "\"template\":\"welcome\",\"params\":{\"name\":\"Айдар\"}}");
        assertEquals(200, r.statusCode());
        assertTrue(r.body().contains("hello Айдар"));
        assertEquals(1, sent.size());
    }

    @Test
    void brokenJsonIs400() throws Exception {
        assertEquals(400, post("{not json").statusCode());
        assertTrue(sent.isEmpty());
    }

    @Test
    void unknownFieldIs400() throws Exception {
        assertEquals(400, post("{\"channel\":\"sms\",\"recipient\":\"x\",\"template\":\"welcome\",\"evil\":1}").statusCode());
    }

    @Test
    void validationErrorIs400() throws Exception {
        assertEquals(400, post("{\"channel\":\"fax\",\"recipient\":\"x\",\"template\":\"welcome\"}").statusCode());
        assertEquals(400, post("{\"channel\":\"sms\",\"recipient\":\"x\",\"template\":\"missing\"}").statusCode());
    }

    @Test
    void renderFailureIs422() throws Exception {
        assertEquals(422, post("{\"channel\":\"sms\",\"recipient\":\"x\",\"template\":\"broken\"}").statusCode());
        assertTrue(sent.isEmpty());
    }

    @Test
    void getOnNotificationsIs405() throws Exception {
        HttpResponse<String> r = http.send(HttpRequest.newBuilder(URI.create(base + "/notifications")).build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(405, r.statusCode());
        assertEquals("POST", r.headers().firstValue("Allow").orElse(""));
    }

    @Test
    void oversizedBodyIs413() throws Exception {
        String big = "{\"channel\":\"sms\",\"recipient\":\"" + "a".repeat(70_000) + "\",\"template\":\"welcome\"}";
        assertEquals(413, post(big).statusCode());
    }

    @Test
    void unknownPathIs404() throws Exception {
        HttpResponse<String> r = http.send(HttpRequest.newBuilder(URI.create(base + "/notificationsX")).build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(404, r.statusCode());
    }
}
