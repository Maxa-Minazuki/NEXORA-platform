# Design Patterns в NEXORA

Все 10 паттернов используются в настоящей бизнес-логике, имеют тесты и могут быть запущены со страницы **Patterns Lab** (`/lab`). Пути к классам указаны относительно `backend/src/main/java/com/nexora/`.

**Как быстро найти реализацию в IntelliJ IDEA:** `Ctrl+N` (Go to Class) → имя класса; `Ctrl+Alt+B` на интерфейсе — показывает все реализации; `Alt+F7` — где используется. В Javadoc каждого ключевого класса есть заголовок `Design pattern: ...` — можно искать `Ctrl+Shift+F` по строке «Design pattern».

| # | Паттерн | Группа | Главный класс | Тест |
|---|---|---|---|---|
| 1 | Singleton | порождающий | `PlatformSettings` | `PlatformIntegrationTest.platformSettingsIsASingleSharedInstance` |
| 2 | Factory Method | порождающий | `DeliveryHandlerCreator` | `NotificationFactoriesTest` |
| 3 | Abstract Factory | порождающий | `NotificationChannelFactory` | `NotificationFactoriesTest` |
| 4 | Observer | поведенческий | `DomainEvent` + listeners | `ObserverUnitTest`, `InventoryScenarioTest` |
| 5 | State | поведенческий | `BookingState`, `DocumentState` | `BookingStatePatternTest`, `DocumentStatePatternTest` |
| 6 | Strategy | поведенческий | `FlightSortStrategy` и др. | `StrategyPatternTest`, `FlightScenarioTest` |
| 7 | Facade | структурный | `BookingFacade`, `PatientAppointmentFacade`, `DocumentWorkflowFacade` | `FlightScenarioTest`, `ClinicScenarioTest` |
| 8 | Mediator | поведенческий | `FlightBookingMediator`, `DocumentWorkflowMediator` | `FlightBookingMediatorTest`, `WorkflowScenarioTest` |
| 9 | Chain of Responsibility | поведенческий | `DocumentCheckHandler` | `ChainOfResponsibilityTest` |
| 10 | Command | поведенческий | `Command`, `CommandInvoker` | `CommandInvokerTest` |

---

## 1. Singleton

**Простыми словами.** У класса есть ровно один общий экземпляр, и все пользуются именно им.

**Проблема.** Правила бизнес-процессов (порог согласования, время удержания брони, политика низкого остатка, часовой пояс, доступность e-mail) нужны в разных модулях. Если бы каждый модуль хранил свою копию, правила могли бы разойтись.

**Классы.** `shared/config/PlatformSettings` (singleton-бин), `shared/config/NexoraProperties` (привязка `nexora.*` из `application.yml`).

**Связи.** `PlatformSettings` внедряется через конструктор в `InventoryStrategyResolver` (активная стратегия остатка), `ManagerApprovalHandler`/`AdditionalApprovalHandler` (пороги), `BookingRecorder` (время удержания), `ScheduleService` (часовой пояс, длина слота), `EmailChannelFactory` (включён ли e-mail), `DashboardController` (страница настроек).

**Сценарий.** Lab → «Compare instances»: `LabService.singleton()` получает бин дважды через `ApplicationContext` и сравнивает с внедрённым — `sameInstance: true`, одинаковые `instanceId` и `identityHashCode`.

**Почему так.** В Spring singleton — область видимости бина по умолчанию: контейнер создаёт один экземпляр и отдаёт его всем. Класс неизменяемый (все поля `final`), поэтому безопасен для многопоточного доступа. Пользовательское и транзакционное состояние в нём **не хранится** — для этого есть объекты-контексты (`BookingContext`, `WorkflowContext`), создаваемые на каждый запрос.

**Важно для защиты.** Spring singleton = **один экземпляр на `ApplicationContext`**, а не один на всю JVM (классический GoF-Singleton с private-конструктором и static-полем — это один на ClassLoader). Если в одной JVM поднять два контекста, будет два экземпляра. Зато Spring-вариант тестируем (в тестах создаём `new PlatformSettings(...)` с нужными значениями) и не прячет зависимости.

---

## 2. Factory Method

**Простыми словами.** Базовый класс задаёт алгоритм, а *какой объект создать* решают подклассы через переопределяемый «фабричный метод».

**Проблема.** Доставка уведомления по любому каналу — это одно и то же: создать обработчик, выполнить, сохранить попытку `DeliveryAttempt` с номером. Отличается только сам обработчик (и можно ли повторять доставку).

**Классы.** `notification/delivery/DeliveryHandlerCreator` (creator, метод `deliver()` — шаблонный, `createHandler()` — фабричный), `InAppDeliveryHandlerCreator`, `EmailDeliveryHandlerCreator` (concrete creators), `DeliveryHandler` (product), `InAppDeliveryHandler`, `EmailDeliveryHandler` (concrete products).

**Связи.** `NotificationDispatcher` держит карту `channel → DeliveryHandlerCreator` и вызывает `creator.deliver(notification, user)`; повтор доставки (`retry`) идёт тем же путём.

**Сценарий.** Предупреждение уровня WARNING: стратегия выбирает IN_APP и EMAIL → каждый creator создаёт свой handler → в журнале доставки появляются `IN_APP DELIVERED` и `EMAIL SKIPPED (SMTP not configured)`. Кнопка Retry доступна только для EMAIL, потому что `EmailDeliveryHandler.retryable() == true`.

**Отличие от Abstract Factory.** Factory Method создаёт **один продукт** через наследование (подкласс переопределяет метод). Abstract Factory создаёт **семейство** совместимых продуктов через композицию (объект-фабрика). Добавить SMS = новый creator + новый handler.

---

## 3. Abstract Factory

**Простыми словами.** Фабрика, которая выпускает целый согласованный набор объектов одного «семейства».

**Проблема.** Для канала нужны три согласованных компонента: форматтер (короткий текст для тоста или письмо с приветствием), отправитель (SSE-push или SMTP) и проверка доступности. Нельзя смешать e-mail-форматтер с in-app-отправителем.

**Классы.** `notification/channel/NotificationChannelFactory` (абстрактная фабрика: `createFormatter()`, `createSender()`, `createAvailabilityChecker()`), конкретные фабрики `inapp/InAppChannelFactory`, `email/EmailChannelFactory`; абстрактные продукты `MessageFormatter`, `MessageSender`, `ChannelAvailabilityChecker`; конкретные продукты `InAppMessageFormatter/Sender/AvailabilityChecker`, `EmailMessageFormatter/Sender/AvailabilityChecker`.

**Связи.** Клиент — `AbstractDeliveryHandler`: получает фабрику в конструкторе и работает только через интерфейсы (проверить доступность → отформатировать → отправить). Какая фабрика — решает Factory Method из п. 2.

**Сценарий.** Notifications → «Channels & test» показывает, какие классы создаёт каждая фабрика, и доступен ли канал. `EmailMessageSender` никогда не сообщает об успехе, если SMTP не принял письмо.

**Почему так.** Добавление канала (SMS/Telegram) — новая фабрика с тремя продуктами, без изменения клиентского кода.

---

## 4. Observer

**Простыми словами.** Издатель сообщает «произошло событие», а подписчики сами решают, как реагировать. Издатель их не знает.

**Проблема.** После списания товара нужно: создать предупреждение, уведомить людей, записать событие в журнал, обновить dashboard. Если `InventoryService` будет вызывать всё это напрямую, склад станет зависеть от уведомлений, аудита и UI.

**Классы.**
* События (субъект): `shared/event/DomainEvent`, `LowStockEvent`, `StockAdjustedEvent`, `AppointmentBookedEvent/RescheduledEvent/CancelledEvent`, `FlightBookingCreated/Confirmed/CancelledEvent`, `DocumentSubmitted/Approved/Rejected/ReturnedEvent`. Публикация — `ApplicationEventPublisher`.
* Наблюдатели:
  * `inventory/service/LowStockAlertRecorder` — `@EventListener` (в той же транзакции: предупреждение сохраняется атомарно со списанием);
  * `notification/listener/NotificationEventListener` — `@TransactionalEventListener(AFTER_COMMIT)` + новая транзакция;
  * `audit/listener/DomainEventAuditListener` — пишет `EVENT_*` в журнал аудита;
  * `dashboard/listener/DashboardRefreshListener` — рассылает SSE-событие `dashboard`, открытые dashboard перечитывают данные.

**Сценарий.** Lab → «Trigger low stock» (или списание в Inventory): `InventoryService.adjustStock` → `LowStockEvent` → предупреждение (с количеством от стратегии пополнения) → после commit уведомления ADMIN/MANAGER/EMPLOYEE, запись в аудит, обновление dashboard. У пользователя всплывает toast в реальном времени.

**Надёжность (честно).** `AFTER_COMMIT` гарантирует, что никто не получит уведомление об откатившейся операции. Но Spring-события — в памяти: если процесс упадёт между commit и обработчиком, уведомление потеряется. Для гарантированной доставки понадобился бы transactional outbox — в проекте он не реализован и это задокументировано.

**Циклические зависимости.** События лежат в `shared`, поэтому inventory/clinic/flight/workflow не зависят от notification/audit/dashboard.

---

## 5. State

**Простыми словами.** Поведение объекта зависит от его состояния; каждое состояние — отдельный класс, который знает, какие операции разрешены и куда переходить.

**Проблема.** Без паттерна в контроллерах появляются `if (status == ...)` и опасное «присвоить статус, который прислал клиент».

**Классы.**
* Бронирование: `flight/state/BookingState` + `PendingBookingState`, `ConfirmedBookingState`, `CancelledBookingState`, `ExpiredBookingState`, реестр `BookingStateRegistry`. Контекст — `FlightBooking` (статус хранится в БД).
* Документ: `workflow/state/DocumentState` + `DraftState`, `PendingApprovalState`, `InReviewState`, `ApprovedState`, `RejectedState`, реестр `DocumentStateRegistry`. Контекст — `Document`.

```
Booking:  PENDING --confirm--> CONFIRMED --cancel--> CANCELLED
          PENDING --cancel--> CANCELLED ; PENDING --expire (job)--> EXPIRED
Document: DRAFT --submit--> PENDING_APPROVAL --approve(не последний)--> IN_REVIEW --approve(последний)--> APPROVED
          PENDING_APPROVAL/IN_REVIEW --reject--> REJECTED ; --return--> DRAFT
          DRAFT --submit (пустой маршрут, системный документ)--> APPROVED
```

**Правила.** По умолчанию интерфейс бросает `InvalidStateTransitionException` (HTTP 409). Отменённую бронь нельзя подтвердить; подтверждённую нельзя отменить позже чем за 2 часа до вылета; удержание нельзя подтвердить после истечения; утверждённый документ нельзя вернуть в черновик или отредактировать. API не принимает статус от клиента — только операции (`/confirm`, `/cancel`, `/approve`…).

**Восстановление после перезапуска.** В БД хранится enum-имя состояния; реестр по нему возвращает объект-состояние. `BookingExpiryJob` раз в минуту переводит просроченные PENDING в EXPIRED через тот же State.

**Сценарий.** Lab → «Run booking lifecycle»: создать → отменить → попытаться подтвердить → `INVALID_STATE_TRANSITION: Cannot confirm a booking in state CANCELLED`.

---

## 6. Strategy

**Простыми словами.** Семейство взаимозаменяемых алгоритмов; нужный выбирается во время выполнения.

**Классы и сценарии.**
1. **Сортировка рейсов** — `flight/strategy/FlightSortStrategy`: `CheapestFirstStrategy` (`price`), `ShortestDurationStrategy` (`duration`), `EarliestDepartureStrategy` (`departure`); выбор по ключу в `FlightSortStrategyResolver` (карта, без switch). Переключатель Cheapest/Fastest/Earliest на странице Flights меняет порядок результатов API.
2. **Критический остаток** — `inventory/strategy/LowStockDetectionStrategy`: `MinimumLevelStrategy` (≤ минимума), `SafetyBufferStrategy` (≤ минимум × 1.25). Активная выбирается настройкой `LOW_STOCK_POLICY` из `PlatformSettings`. Стратегия даёт и правило, и JPA-`Specification`, поэтому фильтр «Low stock» совпадает с логикой предупреждений.
3. **Количество пополнения** — `ReplenishmentStrategy`: `TopUpToTargetStrategy` (до 3× минимума), `FixedLotStrategy` (лоты по 50), `DemandBasedStrategy` (расход за 30 дней × 21 день + страховой запас). Выбирается полем товара `replenishment_strategy`.
4. **Выбор каналов уведомления** — `notification/strategy/ChannelSelectionStrategy`: `PreferenceBasedChannelStrategy` (INFO/SUCCESS/WARNING — по настройкам пользователя), `CriticalEscalationChannelStrategy` (CRITICAL — всегда in-app + все включённые каналы). `ChannelSelector` строит карту severity → strategy.

**Отличие от State.** Strategy выбирает *клиент/конфигурация*, стратегии не знают друг о друге. В State объект-состояние сам определяет следующее состояние.

---

## 7. Facade

**Простыми словами.** Один простой вход в сложную подсистему.

**Классы.**
* `flight/facade/BookingFacade` — search, seatMap, create, confirm, cancel, expire, history; за ним стратегии, реестр состояний, медиатор, репозитории, порт документов.
* `clinic/facade/PatientAppointmentFacade` — book, reschedule, cancel, changeStatus; за ним политика доступа, блокировка врача, проверка часов работы, проверка пересечений, публикация событий.
* `workflow/facade/DocumentWorkflowFacade` — create, updateDraft, submit, approve, reject, return, comment, replenishmentFromAlert, issueSystemDocument.

**Почему так.** Контроллер делает один вызов и не знает порядок шагов (например, «сначала `SELECT … FOR UPDATE` строки врача, потом проверка пересечений» — этот порядок зашит в одном месте). Фасады небольшие, по одному на подсистему, а не один «божественный».

**Отличие от Mediator.** Фасад — упрощённый интерфейс *для внешнего клиента*; подсистема о фасаде не знает. Медиатор — центр взаимодействия *внутри* подсистемы; коллеги знают медиатор и сообщают ему о событиях, а он решает, кто действует дальше. В NEXORA `BookingFacade` вызывает `BookingMediator`: фасад — «вход», медиатор — «диспетчер».

---

## 8. Mediator

**Простыми словами.** Объекты-коллеги общаются не напрямую, а через посредника, который хранит правила взаимодействия.

**Классы (бронирование).** Контракт `flight/mediator/BookingMediator`, реализация `FlightBookingMediator`; базовый класс коллег `BookingColleague`; коллеги `FlightAvailabilityChecker`, `SeatAllocator`, `BookingRecorder`, `BookingStateKeeper` (State), `ConfirmationIssuer` (документ), `BookingEventAnnouncer` (Observer). Сигналы — `BookingSignal`, данные запроса — `BookingContext`.

```
create:  CREATE_REQUESTED → checker.verify → FLIGHT_VERIFIED → seats.hold → SEATS_HELD → recorder.record → BOOKING_RECORDED → announcer
confirm: CONFIRM_REQUESTED → stateKeeper.confirm → STATE_CONFIRMED → seats.book → SEATS_BOOKED → issuer.issue → DOCUMENT_ISSUED → announcer
cancel:  CANCEL_REQUESTED → stateKeeper.cancel → STATE_CLOSED → seats.release → SEATS_RELEASED → announcer
```

**Классы (документооборот).** `workflow/mediator/WorkflowMediator`, `DocumentWorkflowMediator`; коллеги `ApprovalRoutePlanner` (запускает Chain of Responsibility), `ApprovalStepManager` (шаги, проверка прав согласующего), `DocumentLifecycleKeeper` (State), `WorkflowHistoryRecorder`, `WorkflowEventRelay` (Observer).

**Сценарий.** Lab → «Submit a document» или ответ любого approve/confirm содержит `coordinationLog`: видно, кто сообщил медиатору какой сигнал и кого он вызвал следующим. Изменение сценария (например, проверка на мошенничество между FLIGHT_VERIFIED и SEATS_HELD) затрагивает только медиатор.

**Отсутствие циклов в Spring.** Медиатор получает коллег в конструкторе и сам вызывает у них `attach(this)` — коллеги не внедряют медиатор, поэтому цикла бинов нет.

---

## 9. Chain of Responsibility

**Простыми словами.** Запрос идёт по цепочке обработчиков; каждый делает одну проверку и либо передаёт дальше, либо останавливает цепочку.

**Классы.** Абстрактный обработчик `workflow/chain/DocumentCheckHandler` (`handle()` → `check()` → next), объект запроса `DocumentReview` (факты документа + накопленные trace и маршрут), сборщик `DocumentProcessingChain` (связывает все бины-обработчики в порядке `@Order`). Обработчики:

1. `RequiredFieldsHandler` (10) — обязательные поля для типа (сумма, связанный объект);
2. `DataIntegrityHandler` (20) — диапазон суммы, знаки после запятой, длина, тип связанного объекта;
3. `AuthorPermissionHandler` (30) — может ли роль автора подавать этот тип; системные документы — только платформа;
4. `ManagerApprovalHandler` (40) — шаг «Manager Review» (всегда для заявок/пополнений/отчётов, иначе при сумме > 250 000);
5. `AdditionalApprovalHandler` (50) — шаг «Final Approval» ADMIN при сумме > 1 500 000 и для медицинских отчётов;
6. `RoutingDecisionHandler` (100) — итог: маршрут или авто-утверждение; правило «четырёх глаз» для пользовательских документов.

**Сценарий.** В окне создания документа справа работает живой предпросмотр (`POST /api/workflows/validate`): видно, какие обработчики прошли, кто добавил шаг и где цепочка остановилась. Lab → «Validate two documents».

**Расширяемость.** Новый обработчик = новый `@Component` с `@Order`; остальной код не меняется (см. тест `newHandlerIsAddedWithoutChangingOthers`).

---

## 10. Command

**Простыми словами.** Действие пользователя — это объект с методом `execute()`; его можно передать исполнителю, который добавит общее поведение.

**Классы.** Интерфейс `shared/command/Command<R>` (`name`, `entityType`, `execute`, `entityId`, `details`), исполнитель `CommandInvoker` (транзакция + запись в аудит успеха **и** неудачи), порт `AuditTrail` (реализован `audit/service/AuditService` с `REQUIRES_NEW`). Команды: `AdjustInventoryCommand`, `CreateProductCommand`, `UpdateProductCommand`, `CreateDocumentCommand`, `SubmitDocumentCommand`, `ApproveDocumentCommand`, `RejectDocumentCommand`, `ReturnDocumentCommand`, `CreateFlightBookingCommand`, `ConfirmFlightBookingCommand`, `CancelFlightBookingCommand`, плюс `SimpleCommand` для небольших действий (запись к врачу, комментарии, настройки).

**Связи.** Контроллер создаёт команду с получателем (receiver — сервис/фасад) и параметрами и отдаёт `invoker.run(command)`. Исполнитель — единственная граница транзакции для действий пользователя.

**Сценарий.** Любое действие → запись в Audit Log (`APPROVE_DOCUMENT`, `ADJUST_INVENTORY`…); неуспешное (например, попытка подтвердить отменённую бронь) записывается с `FAILURE` и причиной. Lab → «Show my commands».

**Undo.** Не реализован намеренно: утверждение, отправка уведомления или освобождение места нельзя безопасно «отменить» по бизнес-правилам — фиктивный undo только вводил бы в заблуждение.
