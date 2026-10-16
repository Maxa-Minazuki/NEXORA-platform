# Сценарий защиты (DEMO GUIDE)

Время: ~15 минут. Запуск: `docker compose up -d --build` → http://localhost:8088. Совет: откройте **два окна браузера** (обычное и инкогнито) — например, `employee` и `manager`, чтобы показать уведомления в реальном времени.

## 0. Подготовка (1 мин)

* Страница входа: demo-аккаунты кликабельны. Показать, что пароли в БД — BCrypt (`docs/SECURITY.md`).
* Открыть **Patterns Lab** (`/lab`) — карта всех 10 паттернов с классами.

## 1. Главный сценарий: склад → Observer → документооборот (8 мин)

| Шаг | Действие в UI | Что сказать (паттерн) |
|---|---|---|
| 1 | Войти как **employee** (окно A), **manager** (окно B) | Роли проверяются на backend (`@PreAuthorize`) |
| 2 | A: Inventory → любой товар «In stock» → кнопка движения → *Write-off* до уровня ниже минимума | Действие оформлено как **Command** `AdjustInventoryCommand`, выполняет `CommandInvoker` (транзакция + аудит) |
| 3 | Тосты: «Stock updated», «Low-stock event published» | Стратегия **Strategy** `MinimumLevelStrategy` решила, что остаток критический; `InventoryService` опубликовал `LowStockEvent` |
| 4 | Окно B (manager) получает toast в реальном времени, на колокольчике +1 | **Observer**: `NotificationEventListener` после commit; доставка: **Strategy** выбора канала → **Factory Method** (`DeliveryHandlerCreator`) → **Abstract Factory** (`InAppChannelFactory`); push через SSE |
| 5 | Overview: показатель «Low stock» вырос без перезагрузки | **Observer** `DashboardRefreshListener` → SSE `dashboard` |
| 6 | A: Inventory → вкладка *Low-stock alerts* → карточка нового предупреждения (предложенное количество) | **Observer** `LowStockAlertRecorder`; количество — **Strategy** пополнения (`TopUpToTarget`/`FixedLot`/`DemandBased`) |
| 7 | Нажать **Request & submit** | Фасад **Facade** `DocumentWorkflowFacade.replenishmentFromAlert` создаёт заявку и отправляет её |
| 8 | Открыть документ (Documents) — дорожка *Created → Validation → Manager Review → Completed* | **Chain of Responsibility** проверил поля, данные, права автора и построил маршрут; **State**: `DRAFT → PENDING_APPROVAL` |
| 9 | Documents → *Create document*: тип Purchase Request, сумма 2 400 000 — справа видно, как цепочка добавляет «Final Approval (ADMIN)»; без суммы — цепочка останавливается | Расширяемость цепочки: новый обработчик = новый `@Component` |
| 10 | B (manager): Documents → *Awaiting my decision* → документ → **Approve** | **Command** `ApproveDocumentCommand`; **Mediator** `DocumentWorkflowMediator` координирует планировщик маршрута, менеджер шагов, State, историю и события — показать «WorkflowMediator coordination log» |
| 11 | A получает toast «Approved» | **Observer** снова |
| 12 | Попробовать от имени employee утвердить свою заявку — кнопок нет; через API (Swagger) — 403 | Безопасность на backend, принцип «четырёх глаз» |
| 13 | Audit Log (manager): видны `ADJUST_INVENTORY`, `EVENT_LowStockEvent`, `CREATE_REPLENISHMENT_REQUEST`, `APPROVE_DOCUMENT` | **Command** + аудит; события — **Observer** `DomainEventAuditListener` |

## 2. Бронирование авиабилета (3 мин)

1. Flights → поиск ALA → NQZ, 2 пассажира. Переключить **Cheapest / Fastest / Earliest** — меняется порядок (**Strategy**).
2. *Select* → выбрать места на схеме → *Hold seats*. Появляется окно брони: статус PENDING, таймер 15 минут, «State object allows: confirm, cancel».
3. Раскрыть «Mediator coordination log»: `FlightAvailabilityChecker → SeatAllocator → BookingRecorder → BookingEventAnnouncer` (**Mediator**; контроллер вызвал только **Facade** `BookingFacade`).
4. *Confirm (demo, no payment)* → CONFIRMED, появился документ `CNF-…` и ссылка на документ в документообороте (системный документ прошёл ту же цепочку и State и был авто-утверждён).
5. Отменить бронь → CANCELLED. Patterns Lab → **State** → «Run booking lifecycle»: попытка подтвердить отменённую бронь отклоняется `CancelledBookingState` (HTTP 409).
6. Подчеркнуть: места блокируются на сервере (`SELECT … FOR UPDATE` + уникальный индекс места) — двое не купят последнее место.

## 3. Запись в клинику (2 мин)

1. Войти как **user** → Clinic → *Book appointment*: пациент подставлен автоматически (USER может записывать только себя).
2. Выбрать врача и дату → занятые слоты зачёркнуты → записаться. Toast «Appointment booked» (+ уведомление врачу — войти как `doctor`).
3. Попробовать занять тот же слот другим пациентом (как `employee`) → «already has an appointment» (**Facade** `PatientAppointmentFacade` блокирует календарь врача; тест с 4 параллельными запросами — `ClinicScenarioTest`).
4. Как `doctor`: «My schedule» — только свои приёмы; кнопки Confirm / Complete / No-show доступны согласно таблице переходов статуса.

## 4. Остальные паттерны за 1 минуту (Patterns Lab)

* **Singleton** — «Compare instances»: три способа получить `PlatformSettings` → один `instanceId`. Объяснить: один экземпляр на Spring `ApplicationContext`, не на JVM; неизменяемый; без состояния пользователя.
* **Factory Method / Abstract Factory** — «List creators / Inspect families»: creators и семейства каналов; e-mail честно в «demo mode» (SMTP не настроен → `SKIPPED`, а не «отправлено»).
* **Command** — «Show my commands».

## 5. Что показать в IDE

| Вопрос преподавателя | Файл |
|---|---|
| «Где State?» | `flight/state/BookingState.java`, `workflow/state/DocumentState.java` |
| «Чем Facade отличается от Mediator?» | `flight/facade/BookingFacade.java` (вход) vs `flight/mediator/FlightBookingMediator.java` (правила взаимодействия) |
| «Как добавить проверку в цепочку?» | `workflow/chain/*Handler.java`, тест `ChainOfResponsibilityTest.newHandlerIsAddedWithoutChangingOthers` |
| «Как гарантируется отсутствие двойной записи?» | `clinic/facade/PatientAppointmentFacade.book`, `DoctorRepository.lockById` |
| «Где тесты паттернов?» | `backend/src/test/java/com/nexora/patterns`, `.../integration` |

Запуск тестов: `cd backend && ./mvnw test` (59 тестов) и `cd frontend && npm test` (19 тестов).
