package com.nexora.lab;

import com.nexora.flight.command.CancelFlightBookingCommand;
import com.nexora.flight.command.ConfirmFlightBookingCommand;
import com.nexora.flight.command.CreateFlightBookingCommand;
import com.nexora.flight.dto.FlightDtos.BookingDto;
import com.nexora.flight.dto.FlightDtos.CreateBookingRequest;
import com.nexora.flight.dto.FlightDtos.FlightDto;
import com.nexora.flight.dto.FlightDtos.PassengerRequest;
import com.nexora.flight.facade.BookingFacade;
import com.nexora.inventory.command.AdjustInventoryCommand;
import com.nexora.inventory.dto.InventoryDtos.AdjustStockRequest;
import com.nexora.inventory.dto.InventoryDtos.AdjustmentResult;
import com.nexora.inventory.dto.InventoryDtos.StockAlertDto;
import com.nexora.inventory.entity.MovementType;
import com.nexora.inventory.entity.Product;
import com.nexora.inventory.repository.ProductRepository;
import com.nexora.inventory.service.InventoryService;
import com.nexora.notification.repository.NotificationRepository;
import com.nexora.shared.command.CommandInvoker;
import com.nexora.shared.config.PlatformSettings;
import com.nexora.shared.exception.BusinessRuleException;
import com.nexora.shared.exception.ForbiddenOperationException;
import com.nexora.shared.exception.NexoraException;
import com.nexora.shared.i18n.Kk;
import com.nexora.shared.security.AuthenticatedUser;
import com.nexora.shared.security.Role;
import com.nexora.workflow.command.CreateDocumentCommand;
import com.nexora.workflow.command.SubmitDocumentCommand;
import com.nexora.workflow.dto.WorkflowDtos.CreateDocumentRequest;
import com.nexora.workflow.dto.WorkflowDtos.DocumentDetail;
import com.nexora.workflow.dto.WorkflowDtos.WorkflowOutcome;
import com.nexora.workflow.entity.DocumentType;
import com.nexora.workflow.facade.DocumentWorkflowFacade;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Runs real scenarios against the real services for the Design Patterns Lab page. Every scenario
 * goes through the same Commands, Facades and Mediators as the normal UI; nothing is simulated.
 */
@Service
public class LabService {

    public static final String LAB_SKU = "LAB-SENSOR-01";

    private final ApplicationContext context;
    private final PlatformSettings settings;
    private final ProductRepository products;
    private final InventoryService inventory;
    private final BookingFacade bookingFacade;
    private final DocumentWorkflowFacade workflowFacade;
    private final NotificationRepository notifications;
    private final CommandInvoker invoker;
    private final Clock clock;

    public LabService(ApplicationContext context, PlatformSettings settings, ProductRepository products,
                      InventoryService inventory, BookingFacade bookingFacade, DocumentWorkflowFacade workflowFacade,
                      NotificationRepository notifications, CommandInvoker invoker, Clock clock) {
        this.context = context;
        this.settings = settings;
        this.products = products;
        this.inventory = inventory;
        this.bookingFacade = bookingFacade;
        this.workflowFacade = workflowFacade;
        this.notifications = notifications;
        this.invoker = invoker;
        this.clock = clock;
    }

    public Map<String, Object> singleton() {
        PlatformSettings a = context.getBean(PlatformSettings.class);
        PlatformSettings b = context.getBean(PlatformSettings.class);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("injectedInstanceId", settings.instanceId());
        m.put("lookupAInstanceId", a.instanceId());
        m.put("lookupBInstanceId", b.instanceId());
        m.put("identityHashCodes", List.of(System.identityHashCode(settings), System.identityHashCode(a), System.identityHashCode(b)));
        m.put("sameInstance", a == b && b == settings);
        m.put("scope", context.isSingleton("platformSettings") ? "singleton" : "other");
        m.put("createdAt", settings.createdAt());
        m.put("lowStockPolicy", settings.lowStockPolicy());
        m.put("bookingHoldMinutes", settings.bookingHold().toMinutes());
        return m;
    }

    public Map<String, Object> flightStrategies(String from, String to, LocalDate date, AuthenticatedUser user) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (String sort : List.of("price", "duration", "departure")) {
            var result = bookingFacade.search(from, to, date, 1, sort);
            m.put(sort, Map.of("label", result.sortLabel(), "order", result.flights().stream()
                    .map(f -> Map.of("flight", f.flightNumber() + " " + f.airline(), "price", f.price(),
                            "durationMinutes", f.durationMinutes(), "departure", f.departureTime()))
                    .toList()));
        }
        m.put("route", from + " -> " + to + " on " + date);
        return m;
    }

    public Map<String, Object> lowStock(AuthenticatedUser user) {
        if (!user.hasRole(Role.ADMIN, Role.MANAGER, Role.EMPLOYEE)) {
            throw new ForbiddenOperationException("Қойма сценарийлері үшін Әкімші, Менеджер немесе Қызметкер рөлі қажет");
        }
        Product lab = products.findBySku(LAB_SKU)
                .orElseThrow(() -> new BusinessRuleException("LAB_PRODUCT_MISSING", "Зертхана тауары " + LAB_SKU + " табылмады"));
        List<String> steps = new ArrayList<>();
        int target = lab.getMinStock() * 3;
        if (lab.getQuantity() < target) {
            AdjustmentResult restock = invoker.run(new AdjustInventoryCommand(inventory, lab.getId(),
                    new AdjustStockRequest(MovementType.RECEIPT, target - lab.getQuantity(), "Зертхана: демо алдында толықтыру"), user.id()));
            steps.add("AdjustInventoryCommand КІРІС +" + restock.transaction().quantityChange()
                    + " -> қалдық " + restock.product().quantity() + " (ашық ескертулер шешілді, қор жеткілікті)");
        }
        long before = notifications.countByRecipientIdAndReadAtIsNull(user.id());
        Product fresh = products.findById(lab.getId()).orElseThrow();
        int writeOff = fresh.getQuantity() - Math.max(fresh.getMinStock() - 2, 0);
        AdjustmentResult result = invoker.run(new AdjustInventoryCommand(inventory, lab.getId(),
                new AdjustStockRequest(MovementType.WRITE_OFF, writeOff, "Зертхана: қорды ең төменгі деңгейден төмен жұмсау"), user.id()));
        steps.add("AdjustInventoryCommand ЕСЕПТЕН ШЫҒАРУ -" + writeOff + " -> қалдық " + result.product().quantity());
        steps.add("Strategy " + result.detectionStrategy() + ": сыни деңгей = " + (result.lowStockTriggered() ? "иә" : "жоқ"));
        long after = notifications.countByRecipientIdAndReadAtIsNull(user.id());
        StockAlertDto alert = inventory.alerts(true).stream().filter(a -> a.productSku().equals(LAB_SKU)).findFirst().orElse(null);
        if (result.lowStockTriggered()) {
            steps.add("Observer LowStockAlertRecorder: #" + (alert == null ? "?" : alert.id()) + " ескерту, ұсыныс "
                    + (alert == null ? "?" : alert.suggestedQuantity()));
            steps.add("Observer NotificationEventListener (commit кейін): сізге +" + (after - before) + " хабарландыру");
            steps.add("Observer DomainEventAuditListener + DashboardRefreshListener: журналға жазба және басқару панелін жаңарту");
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("steps", steps);
        m.put("product", result.product());
        m.put("alert", alert);
        m.put("newNotifications", after - before);
        return m;
    }

    public Map<String, Object> bookingLifecycle(AuthenticatedUser user) {
        LocalDate date = LocalDate.ofInstant(clock.instant(), settings.businessZone()).plusDays(2);
        List<FlightDto> options = bookingFacade.search("ALA", "NQZ", date, 1, "departure").flights();
        if (options.isEmpty()) {
            throw new BusinessRuleException("NO_LAB_FLIGHT", date + " күні бос орны бар ALA -> NQZ рейсі жоқ");
        }
        FlightDto flight = options.getFirst();
        List<String> steps = new ArrayList<>();
        BookingDto created = invoker.run(new CreateFlightBookingCommand(bookingFacade, new CreateBookingRequest(flight.id(),
                List.of(new PassengerRequest("Зертхана жолаушысы", "")), "lab@nexora.local"), user));
        steps.add("CreateFlightBookingCommand -> " + created.reference() + " " + Kk.status(created.status().name())
                + ", рұқсат етілген әрекеттер: " + Kk.actions(created.allowedActions()));
        BookingDto cancelled = invoker.run(new CancelFlightBookingCommand(bookingFacade, created.id(),
                "Паттерндер зертханасының демосы", user));
        steps.add("CancelFlightBookingCommand -> " + Kk.status(cancelled.status().name()) + ", рұқсат етілген әрекеттер: " + Kk.actions(cancelled.allowedActions()));
        String forbidden;
        try {
            invoker.run(new ConfirmFlightBookingCommand(bookingFacade, created.id(), user));
            forbidden = "күтпеген нәтиже: растау орындалды";
        } catch (NexoraException ex) {
            forbidden = ex.code() + ": " + ex.getMessage();
        }
        steps.add("ConfirmFlightBookingCommand (Болдырылмаған күйде) -> CancelledBookingState қабылдамады: " + forbidden);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("flight", flight.flightNumber() + " " + flight.origin().code() + " -> " + flight.destination().code());
        m.put("steps", steps);
        m.put("createLog", created.coordinationLog());
        m.put("cancelLog", cancelled.coordinationLog());
        m.put("forbiddenTransition", forbidden);
        return m;
    }

    public Map<String, Object> workflowSubmit(AuthenticatedUser user, BigDecimal amount) {
        if (!DocumentType.GENERAL_ADMINISTRATIVE.authorRoles().contains(user.role())) {
            throw new ForbiddenOperationException("Сіздің рөліңіз құжат жасай алмайды; қызметкер, менеджер немесе дәрігер ретінде кіріңіз");
        }
        DocumentDetail draft = invoker.run(new CreateDocumentCommand(workflowFacade, new CreateDocumentRequest(
                DocumentType.GENERAL_ADMINISTRATIVE, "Зертхана: жабдық бюджеті туралы жазба",
                "Паттерндер зертханасының сценарийі: оқу сыныбына арналған жабдық бюджетін бекіту туралы өтініш.",
                amount, null, null), user));
        WorkflowOutcome submitted = invoker.run(new SubmitDocumentCommand(workflowFacade, draft.summary().id(), user));
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("documentId", draft.summary().id());
        m.put("number", submitted.document().summary().number());
        m.put("status", submitted.document().summary().status());
        m.put("track", submitted.document().track());
        m.put("coordinationLog", submitted.coordinationLog());
        return m;
    }
}
