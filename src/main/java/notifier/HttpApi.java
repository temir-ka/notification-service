package notifier;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Map;

/** HTTP-слой на встроенном com.sun.net.httpserver: GET /health и POST /notifications. */
public final class HttpApi {

    private static final int MAX_BODY_BYTES = 64 * 1024;

    private final NotificationService service;
    private final ObjectMapper mapper = new ObjectMapper();

    public HttpApi(NotificationService service) {
        this.service = service;
    }

    /** port = 0 выбирает свободный порт (удобно в тестах). */
    public HttpServer start(int port) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/health", this::health);
        server.createContext("/notifications", this::notifications);
        server.start();
        return server;
    }

    private void health(HttpExchange ex) throws IOException {
        if (!ex.getRequestURI().getPath().equals("/health")) {
            respond(ex, 404, Map.of("error", "not found"));
        } else if (!ex.getRequestMethod().equals("GET")) {
            methodNotAllowed(ex, "GET");
        } else {
            respond(ex, 200, Map.of("status", "ok"));
        }
    }

    private void notifications(HttpExchange ex) throws IOException {
        try {
            if (!ex.getRequestURI().getPath().equals("/notifications")) {
                respond(ex, 404, Map.of("error", "not found"));
                return;
            }
            if (!ex.getRequestMethod().equals("POST")) {
                methodNotAllowed(ex, "POST");
                return;
            }
            byte[] body = ex.getRequestBody().readNBytes(MAX_BODY_BYTES + 1);
            if (body.length > MAX_BODY_BYTES) {
                respond(ex, 413, Map.of("error", "request body too large"));
                return;
            }

            NotificationRequest request;
            try {
                request = mapper.readValue(body, NotificationRequest.class);
            } catch (JsonProcessingException e) {
                respond(ex, 400, Map.of("error", "invalid JSON"));
                return;
            }

            respond(ex, 200, service.send(request));
        } catch (ValidationException e) {
            respond(ex, 400, Map.of("error", e.getMessage()));
        } catch (RenderException e) {
            respond(ex, 422, Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            // Детали наружу не отдаём, только в лог.
            e.printStackTrace();
            respond(ex, 500, Map.of("error", "internal error"));
        }
    }

    private void methodNotAllowed(HttpExchange ex, String allowed) throws IOException {
        ex.getResponseHeaders().set("Allow", allowed);
        respond(ex, 405, Map.of("error", "method not allowed"));
    }

    private void respond(HttpExchange ex, int status, Object payload) throws IOException {
        byte[] bytes = mapper.writeValueAsBytes(payload);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = ex.getResponseBody()) {
            out.write(bytes);
        }
    }
}
