# REST API NEXORA

Полная интерактивная спецификация — Swagger UI: `http://localhost:8088/swagger-ui.html` (Docker) или `http://localhost:8080/swagger-ui.html` (локально).

## Общие правила

* **Аутентификация:** cookie-сессия `NEXORA_SESSION` (HttpOnly, SameSite=Lax).
* **CSRF:** перед изменяющими запросами получите токен `GET /api/auth/csrf` (ставит cookie `XSRF-TOKEN`) и передавайте его в заголовке `X-XSRF-TOKEN`.
* **Формат:** JSON, даты — ISO-8601 в UTC (`2026-10-16T09:30:00Z`), деньги — число с 2 знаками (тенге, KZT).
* **Пагинация:** параметры `page` (с 0), `size` (≤ 100), `sort` (`поле,asc|desc`, только разрешённые поля). Ответ:
  `{ "content": [...], "page": 0, "size": 10, "totalElements": 42, "totalPages": 5 }`
* **Единый формат ошибки:**

```json
{
  "timestamp": "2026-10-16T09:30:00Z",
  "status": 409,
  "error": "Conflict",
  "code": "INVALID_STATE_TRANSITION",
  "message": "Cannot confirm a booking in state CANCELLED",
  "path": "/api/bookings/7/confirm",
  "fieldErrors": []
}
```

| HTTP | code (примеры) | Когда |
|---|---|---|
| 400 | `VALIDATION_FAILED`, `BAD_REQUEST` | ошибки валидации (`fieldErrors`), неверный JSON/параметр |
| 401 | `UNAUTHENTICATED`, `BAD_CREDENTIALS` | нет сессии / неверный пароль |
| 403 | `FORBIDDEN` | нет роли, чужой объект, неверный CSRF-токен |
| 404 | `NOT_FOUND` | объект не найден или не виден пользователю |
| 409 | `INVALID_STATE_TRANSITION`, `SLOT_TAKEN`, `SEAT_TAKEN`, `DUPLICATE_SKU`, `CONCURRENT_MODIFICATION` | конфликт состояния/данных |
| 422 | `INSUFFICIENT_STOCK`, `OUTSIDE_WORKING_HOURS`, `VALIDATION_CHAIN_FAILED`, `HOLD_EXPIRED` | нарушено бизнес-правило |
| 429 | `TOO_MANY_ATTEMPTS` | 5 неудачных входов за 5 минут |

## Auth — `/api/auth`, `/api/users`

| Метод | Путь | Роли | Описание |
|---|---|---|---|
| GET | `/api/auth/csrf` | все | выдать CSRF-токен |
| GET | `/api/auth/demo-accounts` | все | список demo-аккаунтов для страницы входа |
| POST | `/api/auth/login` | все | `{username, password}` → профиль; меняет id сессии и CSRF-токен |
| POST | `/api/auth/logout` | auth | завершить сессию |
| GET | `/api/auth/me` | auth | текущий пользователь |
| GET | `/api/users` | ADMIN | пользователи |
| PUT | `/api/users/{id}` | ADMIN | `{role, active}` (себя изменить нельзя) |

## Dashboard — `/api/dashboard`

| GET | `/api/dashboard` | auth | KPI, ряды графиков, внимание, события и действия (набор зависит от роли) |
|---|---|---|---|
| GET | `/api/dashboard/settings` | auth | конфигурация из Singleton `PlatformSettings` |

## Inventory — `/api/inventory` (ADMIN, MANAGER, EMPLOYEE)

| Метод | Путь | Описание |
|---|---|---|
| GET | `/products?q&categoryId&availability=IN_STOCK|LOW_STOCK|OUT_OF_STOCK|CRITICAL&minValue&maxValue&includeInactive&page&size&sort` | список товаров |
| GET | `/products/{id}` | товар |
| POST | `/products` | создать (`CreateProductCommand`); начальный остаток — движение INITIAL |
| PUT | `/products/{id}` | изменить (`UpdateProductCommand`), количество не меняется |
| PATCH | `/products/{id}/active?value=false` | архивировать/восстановить (ADMIN, MANAGER) |
| POST | `/products/{id}/adjustments` | `{type: RECEIPT|WRITE_OFF|ADJUSTMENT, quantity, reason}` (`AdjustInventoryCommand`) → `{product, transaction, lowStockTriggered, detectionStrategy}` |
| GET | `/transactions?productId&type&page&size` | журнал движений |
| GET | `/alerts?openOnly=true` | предупреждения о низком остатке |
| GET | `/stats` | агрегаты склада |
| GET | `/strategies` | доступные стратегии (Strategy) |
| GET/POST | `/categories`, `/suppliers` | справочники (создание — ADMIN, MANAGER) |

## Clinic — `/api/clinic`

| Метод | Путь | Роли | Описание |
|---|---|---|---|
| GET | `/patients?q&page&size&sort` | ADMIN, MANAGER, EMPLOYEE, DOCTOR | пациенты |
| GET | `/patients/me` | auth | карта пациента текущего пользователя |
| GET/POST/PUT | `/patients/{id}` | чтение — персонал и врачи; изменение — ADMIN, MANAGER, EMPLOYEE | |
| GET | `/specialties`, `/doctors?specialtyId`, `/doctors/{id}` | auth | врачи и расписание |
| GET | `/doctors/{id}/slots?date=2026-10-20` | auth | слоты дня (в поясе Asia/Almaty) |
| GET | `/appointments?doctorId&patientId&specialtyId&status&from&to&q&page&size&sort` | auth | USER видит только свои, DOCTOR — только свой календарь |
| POST | `/appointments` | ADMIN, MANAGER, EMPLOYEE, USER (только для себя) | `{patientId, doctorId, startTime, reason}` |
| POST | `/appointments/{id}/reschedule` | владелец/врач/персонал | `{startTime}` |
| POST | `/appointments/{id}/cancel` | владелец/врач/персонал | `{reason}` |
| POST | `/appointments/{id}/status` | врач приёма/персонал | `{status: CONFIRMED|COMPLETED|NO_SHOW, notes}` |

## Flights — `/api/flights`, `/api/bookings` (все аутентифицированные)

| Метод | Путь | Описание |
|---|---|---|
| GET | `/api/flights/airports` | аэропорты |
| GET | `/api/flights/search?from=ALA&to=NQZ&date=2026-10-18&passengers=2&sort=price|duration|departure` | поиск (Strategy) |
| GET | `/api/flights/{id}`, `/api/flights/{id}/seats` | рейс, карта мест |
| GET | `/api/bookings?status&q&all&page&size` | история (`all=true` — для ADMIN/MANAGER) |
| GET | `/api/bookings/{id}` | бронь (только своя, кроме ADMIN/MANAGER) |
| POST | `/api/bookings` | `{flightId, contactEmail, passengers:[{fullName, seatNumber?}]}` → PENDING + `coordinationLog` |
| POST | `/api/bookings/{id}/confirm` | подтвердить (без оплаты) → документ-подтверждение |
| POST | `/api/bookings/{id}/cancel` | `{reason}` (`CancelFlightBookingCommand`) |

## Documents & workflow

| Метод | Путь | Роли | Описание |
|---|---|---|---|
| GET | `/api/documents/types` | auth | типы и правила |
| GET | `/api/documents?q&type&status&from&to&mine&authorId&page&size&sort` | auth | ADMIN/MANAGER — все, остальные — свои |
| GET | `/api/documents/{id}` | auth | детали: track, шаги, история, комментарии, ревизии, разрешённые действия |
| POST | `/api/documents` | ADMIN, MANAGER, EMPLOYEE, DOCTOR | `{type, title, content, amount?, relatedEntityType?, relatedEntityId?}` |
| PUT | `/api/documents/{id}` | автор, только DRAFT | новая ревизия |
| POST | `/api/documents/{id}/submit` | автор | цепочка проверок + маршрут → `{document, coordinationLog}` |
| POST | `/api/documents/{id}/comments` | кто видит документ | `{body}` |
| GET | `/api/workflows/inbox` | ADMIN, MANAGER | ожидают моего решения |
| POST | `/api/workflows/{id}/approve` | роль шага (ADMIN — любой), не автор | `{comment?}` |
| POST | `/api/workflows/{id}/reject` | то же | `{reason}` (обязательно, 5–500) |
| POST | `/api/workflows/{id}/return` | то же | `{comment}` → DRAFT |
| POST | `/api/workflows/validate` | авторы | пробный прогон Chain of Responsibility |
| POST | `/api/workflows/replenishment-from-alert/{alertId}` | ADMIN, MANAGER, EMPLOYEE | `{submit: true|false}` |

## Notifications — `/api/notifications` (auth)

| Метод | Путь | Описание |
|---|---|---|
| GET | `?unreadOnly&severity&category&page&size` | входящие (с попытками доставки) |
| GET | `/unread-count`, `/latest` | счётчик, 5 последних |
| POST | `/{id}/read`, `/read-all` | отметить прочитанным |
| GET | `/stream` | Server-Sent Events: `connected`, `notification`, `dashboard`, `ping` |
| GET/PUT | `/preferences` | `{preferences:[{channel, enabled, minSeverity}]}` |
| GET | `/channels` | семейства Abstract Factory, creators Factory Method, доступность |
| GET | `/deliveries?page&size` | журнал доставки (ADMIN — все) |
| POST | `/deliveries/{id}/retry` | повтор FAILED/SKIPPED e-mail |
| POST | `/test` | `{severity, title, message}` → отчёт о выбранной стратегии и каналах |

## Audit — `/api/audit` (ADMIN, MANAGER)

`GET /api/audit?action&entityType&outcome&actor&page&size`, `GET /api/audit/actions`

## Patterns Lab — `/api/lab` (auth)

| Метод | Путь | Описание |
|---|---|---|
| GET | `/singleton` | сравнение экземпляров `PlatformSettings` |
| GET | `/flight-strategies?from&to&date` | один поиск тремя стратегиями |
| POST | `/low-stock` | списание лабораторного товара до критического уровня (ADMIN, MANAGER, EMPLOYEE) |
| POST | `/booking-lifecycle` | создать → отменить → попытаться подтвердить |
| POST | `/workflow-submit?amount` | создать и отправить документ, вернуть журнал Mediator |
| GET | `/my-commands` | мои команды из аудита |

## Пример сеанса (curl)

```bash
B=http://localhost:8088
curl -c j -b j $B/api/auth/csrf
T=$(grep XSRF-TOKEN j | awk '{print $7}')
curl -c j -b j -H "X-XSRF-TOKEN: $T" -H 'Content-Type: application/json' \
     -d '{"username":"employee","password":"Employee#2026"}' $B/api/auth/login
curl -c j -b j $B/api/auth/csrf; T=$(grep XSRF-TOKEN j | awk '{print $7}')     # токен меняется после входа
curl -b j -H "X-XSRF-TOKEN: $T" -H 'Content-Type: application/json' \
     -d '{"type":"WRITE_OFF","quantity":5,"reason":"demo"}' $B/api/inventory/products/5/adjustments
```
