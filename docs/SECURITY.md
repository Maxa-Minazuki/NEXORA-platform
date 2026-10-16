# Безопасность NEXORA

## Аутентификация

* **Одна согласованная схема:** серверная HTTP-сессия Spring Security (cookie `NEXORA_SESSION`, `HttpOnly`, `SameSite=Lax`, `Secure` включается переменной `COOKIE_SECURE=true` при HTTPS). JWT не используется — схемы не смешиваются.
* Вход: `POST /api/auth/login` (JSON). `AuthController` проверяет пароль через `AuthenticationManager` + `DaoAuthenticationProvider`, затем:
  * меняет идентификатор сессии (`ChangeSessionIdAuthenticationStrategy`) — защита от session fixation;
  * перевыпускает CSRF-токен (`CsrfAuthenticationStrategy`);
  * сохраняет `SecurityContext` в сессии (`HttpSessionSecurityContextRepository`).
* В сессии хранится `AuthenticatedUser` **без** хеша пароля.
* Пароли — только **BCrypt** (`BCryptPasswordEncoder`); demo-аккаунты создаются миграцией с хешами.
* **Ограничение перебора:** `LoginAttemptService` — не более 5 неудачных попыток на пару «логин + IP» за 5 минут (HTTP 429). Неудачные входы пишутся в аудит.
* Выход: `POST /api/auth/logout` инвалидирует сессию.
* Публичной регистрации нет.

## CSRF

* Включён для всех изменяющих запросов. Шаблон double-submit cookie: `CookieCsrfTokenRepository` (cookie `XSRF-TOKEN`, читается SPA) + заголовок `X-XSRF-TOKEN`.
* `CsrfCookieFilter` гарантирует выдачу cookie; SPA при «протухшем» токене один раз обновляет его (`GET /api/auth/csrf`) и повторяет запрос.
* Тест `SecurityIntegrationTest.stateChangingRequestWithoutCsrfTokenIsRejected`.

## Авторизация (проверяется на backend)

1. **По ролям:** `@PreAuthorize` на контроллерах (`@EnableMethodSecurity`). Пример: склад — ADMIN/MANAGER/EMPLOYEE; согласование — ADMIN/MANAGER; аудит — ADMIN/MANAGER; пользователи — ADMIN.
2. **По объектам (защита от доступа к чужим данным):**
   * `ClinicAccessPolicy` — USER видит и бронирует только свою карту пациента, DOCTOR — только свой календарь; клинические статусы ставит только врач приёма или персонал.
   * `BookingFacade` — бронь видит/меняет только владелец (или ADMIN/MANAGER); чужая бронь отвечает **404**, чтобы не раскрывать существование.
   * `DocumentAccessPolicy` + `ApprovalStepManager` — документ видят автор и ADMIN/MANAGER; редактирует и отправляет только автор; решение принимает только роль шага (ADMIN — любой шаг) и **никогда не автор** (принцип «четырёх глаз»).
   * `NotificationService` — свои уведомления и попытки доставки.
   * `UserAdminService` — админ не может деактивировать себя или сменить свою роль.
3. Скрытие кнопок во frontend — только удобство; проверки дублируются на сервере и покрыты тестами (`SecurityIntegrationTest`, `FlightScenarioTest.otherUsersCannotSeeOrCancelMyBooking`, `WorkflowScenarioTest`).

## Валидация и ошибки

* Bean Validation на всех DTO (`@NotBlank`, `@Size`, `@Email`, `@Pattern`, `@DecimalMin/Max`, `@Digits`, `@Future`, `@Past`), длины совпадают с колонками БД.
* Бизнес-правила — в сервисах/State/Chain (например, нельзя списать больше, чем есть; нельзя записаться вне рабочего времени).
* `GlobalExceptionHandler` возвращает единый JSON без stack trace и внутренних сообщений; неожиданные ошибки логируются на сервере, клиенту — общее сообщение. `server.error.include-stacktrace: never`.
* Сортировка — только по белому списку полей, размер страницы ≤ 100.
* SQL-инъекции исключены: только JPA/JPQL с параметрами и `PreparedStatement` в миграции.

## Надёжность данных

* Транзакции на каждую команду (`CommandInvoker`), пессимистические блокировки строк для склада, врачей, рейсов и документов, `@Version` для оптимистической блокировки, уникальные и CHECK-ограничения в БД.
* Аудит критических изменений: каждая команда (успех и отказ), входы/выходы, все бизнес-события.

## HTTP-заголовки

Backend: `Content-Security-Policy`, `X-Frame-Options: DENY`, `X-Content-Type-Options: nosniff`, `Referrer-Policy: strict-origin-when-cross-origin`, `Cache-Control: no-store` для API. Nginx добавляет те же заголовки для статики и скрывает версию (`server_tokens off`).

## Секреты и конфигурация

* Все пароли/хосты — через переменные окружения (`.env`, не в Git; шаблон `.env.example` без реальных значений).
* `actuator`: публично только `health` и `info`; остальное — ADMIN. Детали health — только авторизованным.
* В репозитории нет реальных секретов, реальных данных пациентов или платёжных данных; платёжный шлюз не подключается вообще.

## Что сознательно не входит в демо

* HTTPS настраивается на уровне обратного прокси (в Compose — HTTP на localhost; для публикации включить TLS и `COOKIE_SECURE=true`).
* Ограничитель попыток входа и SSE-реестр хранятся в памяти одного экземпляра.
* Сброс пароля и MFA не реализованы (нет публичной регистрации, только demo-аккаунты).
