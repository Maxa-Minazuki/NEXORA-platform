import { Boxes, Cpu, Eye, Factory, GitBranch, Layers, Link2, Network, Repeat, TerminalSquare } from 'lucide-react'
import type { ReactNode } from 'react'
import type { Accent } from '../../components/ui/accents'

export type PatternGroup = 'Creational' | 'Structural' | 'Behavioral'

/** Kazakh names of the GoF groups (the codes stay in English). */
export const GROUP_LABEL: Record<PatternGroup | 'All', string> = {
  All: 'Барлығы',
  Creational: 'Туындатушы',
  Structural: 'Құрылымдық',
  Behavioral: 'Мінез-құлықтық',
}

export interface PatternInfo {
  id: string
  name: string
  group: PatternGroup
  icon: ReactNode
  accent: Accent
  summary: string
  scenario: string
  classes: string[]
  demo: string
  demoLabel: string
  roles?: string
  link?: { to: string; label: string }
}

export const PATTERNS: PatternInfo[] = [
  {
    id: 'singleton', name: 'Singleton', group: 'Creational', icon: <Boxes className="size-5" />, accent: 'violet',
    summary: 'Барлық бизнес-үдерістерге ортақ, өзгермейтін бір ғана баптаулар объектісі (Spring ApplicationContext ішінде бір экземпляр).',
    scenario: 'Брондауды ұстау уақыты, бекіту шектері, төмен қор саясаты және уақыт белдеуі барлық модульде бір PlatformSettings экземплярынан оқылады.',
    classes: ['PlatformSettings', 'NexoraProperties', 'InventoryStrategyResolver', 'ManagerApprovalHandler'],
    demo: 'singleton', demoLabel: 'Экземплярларды салыстыру',
  },
  {
    id: 'factory-method', name: 'Factory Method', group: 'Creational', icon: <Cpu className="size-5" />, accent: 'sky',
    summary: 'Негізгі класс жеткізу алгоритмін анықтайды, ал қандай өңдеуші объектіні жасау керегін ішкі кластар шешеді.',
    scenario: 'DeliveryHandlerCreator.deliver() тұрақты; InAppDeliveryHandlerCreator мен EmailDeliveryHandlerCreator createHandler() әдісін қайта анықтайды.',
    classes: ['DeliveryHandlerCreator', 'InAppDeliveryHandlerCreator', 'EmailDeliveryHandlerCreator', 'DeliveryHandler', 'InAppDeliveryHandler', 'EmailDeliveryHandler'],
    demo: 'channels', demoLabel: 'Creator-ларды көрсету', link: { to: '/notifications?tab=channels', label: 'Хабарландыру арналары' },
  },
  {
    id: 'abstract-factory', name: 'Abstract Factory', group: 'Creational', icon: <Factory className="size-5" />, accent: 'teal',
    summary: 'Нақты кластарды атамай-ақ, бір-біріне үйлесетін объектілер отбасын жасайды.',
    scenario: 'Әр арна фабрикасы сәйкес форматтаушы, жіберуші және қолжетімділік тексерушісін жасайды; демо режимде e-mail «SMTP бапталмаған» деп шынайы хабарлайды.',
    classes: ['NotificationChannelFactory', 'InAppChannelFactory', 'EmailChannelFactory', 'MessageFormatter', 'MessageSender', 'ChannelAvailabilityChecker'],
    demo: 'channels', demoLabel: 'Отбасыларды қарау',
  },
  {
    id: 'observer', name: 'Observer', group: 'Behavioral', icon: <Eye className="size-5" />, accent: 'rose',
    summary: 'Жариялаушы оқиға туралы хабарлайды, ал тәуелсіз бақылаушылар оған өздері әрекет етеді — жариялаушы оларды білмейді.',
    scenario: 'Қорды ең аз деңгейден төмен шығару LowStockEvent жариялайды: ескерту жазылады, commit-тен кейін хабарландырулар жіберіледі, журнал мен басқару панелі жаңарады.',
    classes: ['DomainEvent', 'LowStockEvent', 'LowStockAlertRecorder', 'NotificationEventListener', 'DomainEventAuditListener', 'DashboardRefreshListener'],
    demo: 'low-stock', demoLabel: 'Төмен қорды іске қосу', roles: 'Әкімші, Менеджер немесе Қызметкер', link: { to: '/inventory?tab=alerts', label: 'Қор ескертулері' },
  },
  {
    id: 'state', name: 'State', group: 'Behavioral', icon: <Repeat className="size-5" />, accent: 'mint',
    summary: 'Ағымдағы күй объектісі қандай әрекеттерге рұқсат барын және келесі күйді өзі анықтайды.',
    scenario: 'Брондау жасалады (Күтуде), болдырылмайды (Болдырылмаған), содан кейін растау әрекеті CancelledBookingState арқылы қабылданбайды. Құжаттар DraftState ... RejectedState күйлерін қолданады.',
    classes: ['BookingState', 'PendingBookingState', 'ConfirmedBookingState', 'CancelledBookingState', 'ExpiredBookingState', 'DocumentState', 'DraftState', 'InReviewState'],
    demo: 'booking-lifecycle', demoLabel: 'Брондау циклін іске қосу',
  },
  {
    id: 'strategy', name: 'Strategy', group: 'Behavioral', icon: <GitBranch className="size-5" />, accent: 'sky',
    summary: 'Бір-бірін алмастыра алатын алгоритмдер жұмыс уақытында кілт бойынша, if/else тізбегінсіз таңдалады.',
    scenario: 'Бір рейс іздеуі үш стратегиямен сұрыпталады. Қойма анықтау мен толықтыру стратегияларын, хабарландырулар маңыздылыққа қарай арна стратегиясын қолданады.',
    classes: ['FlightSortStrategy', 'CheapestFirstStrategy', 'ShortestDurationStrategy', 'EarliestDepartureStrategy', 'ReplenishmentStrategy', 'ChannelSelectionStrategy'],
    demo: 'flight-strategies', demoLabel: 'Сұрыптауды салыстыру', link: { to: '/flights', label: 'Рейстерді іздеу' },
  },
  {
    id: 'facade', name: 'Facade', group: 'Structural', icon: <Layers className="size-5" />, accent: 'peach',
    summary: 'Күрделі ішкі жүйенің алдындағы бір қарапайым кіру нүктесі.',
    scenario: 'Контроллерлер BookingFacade.create/confirm/cancel, PatientAppointmentFacade.book немесе DocumentWorkflowFacade.submit әдісін шақырады — көп қатысушыны басқарудың орнына бір шақыру.',
    classes: ['BookingFacade', 'PatientAppointmentFacade', 'DocumentWorkflowFacade'],
    demo: 'booking-lifecycle', demoLabel: 'BookingFacade шақыру',
  },
  {
    id: 'mediator', name: 'Mediator', group: 'Behavioral', icon: <Network className="size-5" />, accent: 'violet',
    summary: 'Әріптес объектілер бір-бірімен тек делдал арқылы байланысады; өзара әрекеттесу ережелері делдалда сақталады.',
    scenario: 'DocumentWorkflowMediator сигналдарды маршрут жоспарлаушы, қадамдар менеджері, күй сақтаушы, тарих жазушы және оқиға таратушы арасында бағыттайды. FlightBookingMediator орындар мен құжаттар үшін дәл солай жұмыс істейді.',
    classes: ['WorkflowMediator', 'DocumentWorkflowMediator', 'ApprovalRoutePlanner', 'ApprovalStepManager', 'BookingMediator', 'FlightBookingMediator', 'SeatAllocator'],
    demo: 'workflow-submit', demoLabel: 'Құжат жіберу', roles: 'Қызметкер, Менеджер, Дәрігер немесе Әкімші',
  },
  {
    id: 'chain', name: 'Chain of Responsibility', group: 'Behavioral', icon: <Link2 className="size-5" />, accent: 'peach',
    summary: 'Сұраныс өңдеушілер тізбегімен өтеді; әрқайсысы бір нәрсені тексеріп, әрі қарай жібереді немесе тоқтатады.',
    scenario: 'Міндетті өрістер → деректердің дұрыстығы → автордың құқығы → менеджер бекітуі → қосымша бекіту → маршрут шешімі. Жаңа ереже қосу = бір жаңа @Component қосу.',
    classes: ['DocumentCheckHandler', 'RequiredFieldsHandler', 'DataIntegrityHandler', 'AuthorPermissionHandler', 'ManagerApprovalHandler', 'AdditionalApprovalHandler', 'RoutingDecisionHandler'],
    demo: 'chain', demoLabel: 'Екі құжатты тексеру', roles: 'Қызметкер, Менеджер, Дәрігер немесе Әкімші',
  },
  {
    id: 'command', name: 'Command', group: 'Behavioral', icon: <TerminalSquare className="size-5" />, accent: 'teal',
    summary: 'Пайдаланушының әр әрекеті — транзакция мен журнал жазбасын қосатын invoker орындайтын объект.',
    scenario: 'AdjustInventoryCommand, SubmitDocumentCommand, ApproveDocumentCommand, CancelFlightBookingCommand... барлығы CommandInvoker арқылы орындалады; сәтсіз әрекеттер де журналға жазылады.',
    classes: ['Command', 'CommandInvoker', 'AdjustInventoryCommand', 'CreateDocumentCommand', 'ApproveDocumentCommand', 'RejectDocumentCommand', 'CancelFlightBookingCommand'],
    demo: 'my-commands', demoLabel: 'Менің командаларым', link: { to: '/audit', label: 'Әрекеттер журналы' },
  },
]
