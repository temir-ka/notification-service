package notifier;

import java.io.IOException;

public final class Main {
    public static void main(String[] args) throws IOException {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        NotificationService service = new NotificationService(new FreeMarkerRenderer(), new LogSender());
        new HttpApi(service).start(port);
        System.out.println("notification-service listening on http://localhost:" + port);
    }
}
