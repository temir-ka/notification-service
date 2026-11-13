# notification-service

Мини-сервис уведомлений на Java 17. Принимает HTTP-запрос с JSON, подставляет данные в шаблон FreeMarker и передаёт готовое сообщение каналу доставки. Канал доставки здесь заглушка (печатает в консоль), реальный WhatsApp / SMS / email подключается одним классом.

Стек: Java 17, Jackson (JSON), FreeMarker (шаблоны), встроенный `com.sun.net.httpserver`, JUnit 5, Maven.

## Запуск

```bash
mvn test          # 17 тестов
mvn compile exec:java
```

Сервис слушает порт `8080` (меняется переменной `PORT`).

## API

`POST /notifications`

```bash
curl -X POST http://localhost:8080/notifications \
  -H "Content-Type: application/json" \
  -d '{"channel":"whatsapp","recipient":"+77001234567","template":"welcome",
       "params":{"name":"Айдар","service":"Beeline"}}'
```

Ответ `200`:

```json
{"id":"b1c0...","status":"SENT","channel":"whatsapp","recipient":"+77001234567",
 "message":"Здравствуйте, Айдар! Добро пожаловать в Beeline.\n"}
```

| Код | Когда |
|---|---|
| 200 | сообщение отрисовано и передано Sender'у |
| 400 | битый JSON, неизвестное поле, нет `channel` / `recipient` / `template`, неподдерживаемый канал, неизвестный шаблон |
| 405 | метод не `POST` (в заголовке `Allow` указан нужный) |
| 413 | тело запроса больше 64 КБ |
| 422 | шаблон найден, но не отрисовался (например, не передан параметр) |
| 500 | внутренняя ошибка, детали наружу не отдаются |

`GET /health` возвращает `{"status":"ok"}`.

Каналы: `whatsapp`, `sms`, `email`. Шаблоны лежат в `src/main/resources/templates/*.ftl`:

- `welcome`: параметры `name`, `service`
- `order_status`: параметры `orderId` (строка), `status` (`shipped`, `delivered` или любой текст)

Готовая коллекция запросов для Postman: `postman/notification-service.postman_collection.json`.

## Устройство

```
HttpApi  ->  NotificationService  ->  TemplateRenderer (FreeMarkerRenderer)
                                  ->  Sender (LogSender)
```

- `HttpApi` отвечает только за HTTP: разбор JSON, коды ответов.
- `NotificationService` содержит логику: проверки, шаблон, отправка.
- `TemplateRenderer` и `Sender` это интерфейсы. Поэтому в тестах сервис проверяется без FreeMarker и без сети, подставляются простые лямбды.

Что сделано намеренно:

- Имя шаблона приходит от клиента и становится именем файла, поэтому допускаются только имена вида `[a-z][a-z0-9_]*`. Запрос с `../secret` отклоняется до обращения к файловой системе.
- Пропущенный параметр в шаблоне это ошибка (422), а не пустая строка в сообщении клиенту.
- Тело запроса ограничено 64 КБ, неизвестные поля в JSON отклоняются.

## Что можно добавить

- `Sender` для реального канала, например WhatsApp через Evolution API.
- Очередь и повторные попытки при сбое отправки.
- Хранение истории отправок в БД.
- Шаблоны на нескольких языках (казахский / русский).
