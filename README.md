# NEXORA — Біртұтас басқару платформасы

**Біртұтас операциялар, ақылды жұмыс үдерістері, нақты уақыттағы хабарландырулар.**

NEXORA — «Жобалау паттерндері» (Design Patterns) пәні бойынша жасалған оқу full-stack жобасы. Бұл бір-бірімен байланысқан бес модульден тұратын біртұтас платформа. Интерфейс толығымен қазақ тілінде.

> Барлық деректер демонстрациялық: пациенттер, әуе компаниялары, рейстер мен бағалар ойдан шығарылған. Нақты брондау мен төлем жасалмайды.

## Жобаның мақсаты

Он GoF паттернін (Singleton, Factory Method, Abstract Factory, Observer, State, Strategy, Facade, Mediator, Chain of Responsibility, Command) жасанды мысалдарда емес, нақты бизнес-логикада көрсету және оларды оқытушыға ыңғайлы түрде қорғау.

## Бес бизнес-модуль

| Модуль | Сипаттамасы |
|---|---|
| **Қойма** | Тауарлар, санаттар, жеткізушілер, қалдықтар, кіріс/шығыс қозғалыстары, төмен қор ескертулері, толықтыру өтінімі бір батырмамен |
| **Емхана** | Пациенттер, дәрігерлер, мамандықтар, апталық кесте, қабылдауға жазылу/ауыстыру/болдырмау; бір дәрігерге бір уақытқа екі жазылу мүмкін емес |
| **Әуе рейстері** | Ойдан шығарылған рейстерді іздеу, орын картасы, орындарды 15 минутқа ұстау, растау (төлемсіз), болдырмау, растау құжаты |
| **Құжат айналымы** | Құжаттар, тексерулер тізбегі, кезекті бекіту, қабылдамау себебімен, пысықтауға қайтару, тарих, нұсқалар, пікірлер |
| **Хабарландырулар** | Хабарландыру орталығы, нақты уақыттағы жаңарту (SSE), «қосымша ішінде» және e-mail арналары, жеткізу журналы |

Модульдер оқиғалар арқылы байланысқан: қор азайса — ескерту, хабарландыру және толықтыру өтінімі пайда болады; брондау расталса — құжат айналымында растау құжаты жасалады; құжат бойынша шешім қабылданса — автор мен келесі бекітуші хабардар болады.

## Он Design Pattern

| Паттерн | Негізгі кластар (backend/src/main/java/com/nexora) |
|---|---|
| Singleton | `shared/config/PlatformSettings` |
| Factory Method | `notification/delivery/DeliveryHandlerCreator` → `InAppDeliveryHandlerCreator`, `EmailDeliveryHandlerCreator` |
| Abstract Factory | `notification/channel/NotificationChannelFactory` → `InAppChannelFactory`, `EmailChannelFactory` |
| Observer | `shared/event/*Event` + `LowStockAlertRecorder`, `NotificationEventListener`, `DomainEventAuditListener`, `DashboardRefreshListener` |
| State | `flight/state/*BookingState`, `workflow/state/*State` |
| Strategy | `flight/strategy/FlightSortStrategy`, `inventory/strategy/*`, `notification/strategy/ChannelSelectionStrategy` |
| Facade | `BookingFacade`, `PatientAppointmentFacade`, `DocumentWorkflowFacade` |
| Mediator | `FlightBookingMediator`, `DocumentWorkflowMediator` және олардың әріптес кластары |
| Chain of Responsibility | `workflow/chain/DocumentCheckHandler` және 6 өңдеуші |
| Command | `shared/command/Command`, `CommandInvoker`, `AdjustInventoryCommand`, `ApproveDocumentCommand`, `CancelFlightBookingCommand` т.б. |

Сайттағы **«Паттерндер зертханасы»** бетінде әр паттерн сервердегі нақты кластарды іске қосатын батырмамен көрсетілген. Толық түсіндірме: [docs/DESIGN_PATTERNS.md](docs/DESIGN_PATTERNS.md).

## Қажетті бағдарламалар

* **Windows 10/11** және **Docker Desktop** (WSL 2 қосылған). Жүктеу: https://www.docker.com/products/docker-desktop/
* Басқа ештеңе қажет емес: Java, Node.js және MySQL Docker ішінде іске қосылады.

## Іске қосу — ең оңай жолы: `Старт.bat`

1. Жоба бумасындағы **`Старт.bat`** файлын екі рет басыңыз.
2. Файл өзі:
   * Docker бар-жоғын тексереді, қажет болса Docker Desktop-ты іске қосып, дайын болуын күтеді;
   * `.env` файлы жоқ болса, оны `.env.example` негізінде жасайды және кездейсоқ құпиясөздер қояды (бар `.env` файлы ешқашан қайта жазылмайды);
   * конфигурацияны тексеріп (`docker compose config`), контейнерлерді жинап, іске қосады (`docker compose up -d --build`);
   * сайттың жауап беруін күтіп, **http://localhost:8088** мекенжайын браузерде ашады.
3. Алғашқы іске қосу бірнеше минут алуы мүмкін (бейнелер жиналады). Кейінгі іске қосулар жылдамырақ.

## Тоқтату: `Стоп.bat`

**`Стоп.bat`** файлын екі рет басыңыз. Ол тек осы жобаның контейнерлерін `docker compose stop` арқылы тоқтатады. Дерекқор мен Docker томдары **сақталады** (`down -v` қолданылмайды). Қайта іске қосу үшін — `Старт.bat` немесе `Қайта іске қосу.bat`.

## Мекенжайлар

| Не | Мекенжайы |
|---|---|
| Сайт | http://localhost:8088 |
| Swagger UI (API құжаттамасы) | http://localhost:8088/swagger-ui.html |
| Денсаулық тексерісі | http://localhost:8088/actuator/health |

## Демо-аккаунттар

Құпиясөздер дерекқорда тек BCrypt хеші түрінде сақталады. Кіру бетіндегі демо-аккаунт жолын басқанда өрістер ғана толтырылады — «Жүйеге кіру» батырмасын өзіңіз басасыз.

| Рөл | Логин | Құпиясөз | Мүмкіндіктері |
|---|---|---|---|
| Әкімші | `admin` | `Admin#2026` | Барлығы: пайдаланушылар, баптаулар, әрекеттер журналы, соңғы бекіту |
| Менеджер | `manager` | `Manager#2026` | Құжаттарды бекіту, қойма, барлық брондаулар, журнал |
| Дәрігер | `doctor` | `Doctor#2026` | Өз кестесі мен қабылдаулары, медициналық есептер |
| Қызметкер | `employee` | `Employee#2026` | Қойма, тіркеу бөлімі (пациенттер, жазылулар), құжаттар |
| Пайдаланушы | `user` | `User#2026` | Өз брондаулары, өз жазылулары, хабарландырулар |

Әр іске қосылғанда backend осы бес аккаунтты тексереді: аккаунт бұғатталса, рөлі немесе құпиясөзі өзгертілсе, оны демо мәндеріне қайтарады (басқа деректерге тиіспейді, қайталанатын пайдаланушы жасамайды). Өшіру: `.env` ішінде `DEMO_ACCOUNTS_SYNC=false`.

## Қолмен іске қосу (міндетті емес)

```bash
cp .env.example .env              # DB_PASSWORD және MYSQL_ROOT_PASSWORD толтырыңыз
docker compose up -d --build      # іске қосу
docker compose stop               # тоқтату (деректер сақталады)
```

Әзірлеу режимі (backend :8080, frontend :5173):

```bash
cd backend  && DB_PASSWORD=<құпиясөз> ./mvnw spring-boot:run      # MySQL 3307 портында болуы керек
cd frontend && npm install && npm run dev
```

## Тесттерді орындау

```bash
cd backend  && ./mvnw test            # JUnit 5, Mockito, MockMvc, H2 (MySQL режимі), нақты Flyway миграциялары
cd frontend && npm test               # Vitest + Testing Library
cd frontend && npm run typecheck      # TypeScript тексерісі
cd frontend && npm run build          # production жинағы
```

## Технологиялар

Java 25, Spring Boot 3.5 (Web, Data JPA, Validation, Security, Actuator, Mail), Hibernate, Flyway, MySQL 8.4, springdoc-openapi · React 19, TypeScript, Vite, Tailwind CSS 4, React Router, TanStack Query, React Hook Form + Zod, Recharts, Lucide · Docker Compose, Nginx.

## Жиі кездесетін қателер және шешімдері

| Белгісі | Себебі және шешімі |
|---|---|
| `Старт.bat`: «Docker табылмады» | Docker Desktop орнатылмаған. Орнатып, компьютерді қайта іске қосыңыз. |
| «Docker Engine 4 минут ішінде іске қосылмады» | Docker Desktop-ты ашып, қатені қараңыз (көбінесе WSL 2 немесе BIOS-та виртуализация өшірулі). |
| «.env файлы жоқ, бірақ дерекқор бар» | Бұрынғы `.env` файлын қайтарыңыз — жаңа құпиясөздер ескі дерекқорға сәйкес келмейді. |
| `8088` порты бос емес | `.env` ішінде `FRONTEND_PORT=8090` сияқты басқа порт жазыңыз. |
| Сайт ашылмайды | `docker compose ps` және `docker compose logs --tail 100 backend` командаларымен логтарды қараңыз. |
| Жүйеге кіре алмаймын | Логин мен құпиясөзді кестеден тексеріңіз; 5 рет қате енгізсеңіз, 5 минут күтіңіз. Қайта іске қосқанда демо-аккаунттар қалпына келеді. |
| E-mail келмейді | Демо режимде SMTP бапталмаған: хаттар жіберілмейді және жеткізу журналында «Жіберілмеді» деп көрсетіледі. SMTP үшін `.env` ішіндегі `EMAIL_ENABLED` және `SMTP_*` мәндерін толтырыңыз. |

## Құжаттама

* [docs/DESIGN_PATTERNS.md](docs/DESIGN_PATTERNS.md) — он паттерн: не үшін, қай жерде, қалай көрсету
* [docs/architecture.md](docs/architecture.md) — архитектура және Mermaid диаграммалары
* [docs/API.md](docs/API.md) — REST API
* [docs/DATABASE.md](docs/DATABASE.md) — дерекқор сұлбасы және миграциялар
* [docs/SECURITY.md](docs/SECURITY.md) — қауіпсіздік
* [docs/DEMO_GUIDE.md](docs/DEMO_GUIDE.md) — қорғауға арналған демо-сценарий

(`docs/` бумасындағы толық техникалық құжаттама орыс тілінде жазылған.)

## Белгілі шектеулер

* SMTP бапталмаса, e-mail хаттары жіберілмейді және бұл журналда шынайы көрсетіледі.
* Spring оқиғалары жадта өңделеді: commit пен өңдеуші арасында процесс тоқтаса, хабарландыру жасалмай қалуы мүмкін (transactional outbox жоқ).
* Кіру әрекеттерін шектеу және SSE қосылымдары бір сервер жадында сақталады (жоба бір түйінге арналған).
