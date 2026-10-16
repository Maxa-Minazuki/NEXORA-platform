/**
 * Central Kazakh localization of the NEXORA UI.
 *
 * - `t(key)` - navigation and application-level texts;
 * - `label(code)` - human-readable Kazakh name of any internal code (statuses, roles, severities,
 *   channels, document types, audit actions...). Codes coming from the API stay unchanged; only their
 *   display form is translated here, in one place.
 * - `errorText(code, fallback)` - Kazakh text for API error codes.
 *
 * Kazakh is the only UI language of this version; the structure allows adding other catalogues later.
 */
export const LOCALE = 'kk-KZ'

const messages = {
  'nav.overview': 'Басқару панелі',
  'nav.inventory': 'Қойма',
  'nav.clinic': 'Емхана',
  'nav.flights': 'Әуе рейстері',
  'nav.documents': 'Құжат айналымы',
  'nav.notifications': 'Хабарландырулар',
  'nav.audit': 'Әрекеттер журналы',
  'nav.lab': 'Паттерндер зертханасы',
  'nav.settings': 'Баптаулар',
  'app.subtitle': 'Біртұтас басқару платформасы',
  'app.tagline': 'Біртұтас операциялар, ақылды жұмыс үдерістері, нақты уақыттағы хабарландырулар',
} as const

export type MessageKey = keyof typeof messages

export function t(key: MessageKey): string {
  return messages[key]
}

/** Frequently used words, so every page says "Сақтау" the same way. */
export const common = {
  save: 'Сақтау',
  cancel: 'Болдырмау',
  close: 'Жабу',
  edit: 'Өңдеу',
  delete: 'Жою',
  search: 'Іздеу',
  loading: 'Жүктелуде',
  retry: 'Қайталау',
  back: 'Артқа',
  all: 'Барлығы',
  noData: 'Деректер жоқ',
  somethingWrong: 'Қате орын алды',
  open: 'Ашу',
  yes: 'Иә',
  no: 'Жоқ',
}

const labels: Record<string, string> = {
  // roles
  ADMIN: 'Әкімші', MANAGER: 'Менеджер', DOCTOR: 'Дәрігер', EMPLOYEE: 'Қызметкер', USER: 'Пайдаланушы',
  // inventory
  IN_STOCK: 'Қоймада бар', LOW_STOCK: 'Қор аз', OUT_OF_STOCK: 'Таусылған', CRITICAL_STOCK: 'Аз немесе таусылған',
  OPEN: 'Ашық', REQUEST_CREATED: 'Өтінім жасалды', RESOLVED: 'Шешілді',
  RECEIPT: 'Кіріс', WRITE_OFF: 'Есептен шығару', ADJUSTMENT: 'Түзету', INITIAL: 'Бастапқы қалдық',
  TOP_UP_TO_TARGET: 'Ең төменгі қордың 3 есесіне дейін', FIXED_LOT: '50 бірліктік партиялар', DEMAND_BASED: 'Сұранысқа негізделген',
  'minimum-level': 'Ең төменгі деңгей', 'safety-buffer': 'Қауіпсіздік қоры (+25%)',
  // clinic
  SCHEDULED: 'Жоспарланған', CONFIRMED: 'Расталған', COMPLETED: 'Аяқталған', CANCELLED: 'Болдырылмаған', NO_SHOW: 'Келмеді',
  FEMALE: 'Әйел', MALE: 'Ер', OTHER: 'Басқа',
  // flights
  PENDING: 'Күтуде', EXPIRED: 'Мерзімі өткен',
  // workflow
  DRAFT: 'Қаралама', PENDING_APPROVAL: 'Бекітуді күтуде', IN_REVIEW: 'Қаралуда', APPROVED: 'Бекітілген', REJECTED: 'Қабылданбаған',
  WAITING: 'Кезекте', ACTIVE: 'Белсенді', RETURNED: 'Қайтарылды',
  PURCHASE_REQUEST: 'Сатып алуға өтінім', INVENTORY_REPLENISHMENT: 'Қорды толықтыру',
  MEDICAL_APPOINTMENT_REPORT: 'Медициналық қабылдау есебі', FLIGHT_BOOKING_CONFIRMATION: 'Брондауды растау құжаты',
  GENERAL_ADMINISTRATIVE: 'Жалпы әкімшілік құжат',
  PRODUCT: 'Тауар', APPOINTMENT: 'Қабылдау', FLIGHT_BOOKING: 'Брондау',
  // notifications
  INFO: 'Ақпарат', SUCCESS: 'Сәтті', WARNING: 'Ескерту', CRITICAL: 'Сыни',
  DELIVERED: 'Жеткізілді', FAILED: 'Сәтсіз', SKIPPED: 'Жіберілмеді', FAILURE: 'Сәтсіз',
  IN_APP: 'Қосымша ішінде', EMAIL: 'Электрондық пошта',
  INVENTORY: 'Қойма', CLINIC: 'Емхана', FLIGHT: 'Әуе рейстері', WORKFLOW: 'Құжат айналымы', SYSTEM: 'Жүйе',
  // audit actions
  LOGIN: 'Жүйеге кіру', LOGOUT: 'Жүйеден шығу', SEED_DEMO_DATA: 'Демо деректер орнатылды',
  CREATE_PRODUCT: 'Тауар жасау', UPDATE_PRODUCT: 'Тауарды өңдеу', ARCHIVE_PRODUCT: 'Тауарды мұрағаттау',
  RESTORE_PRODUCT: 'Тауарды қалпына келтіру', ADJUST_INVENTORY: 'Қор қозғалысы',
  CREATE_PATIENT: 'Пациент қосу', UPDATE_PATIENT: 'Пациентті өңдеу', BOOK_APPOINTMENT: 'Қабылдауға жазу',
  RESCHEDULE_APPOINTMENT: 'Қабылдауды ауыстыру', CANCEL_APPOINTMENT: 'Қабылдауды болдырмау',
  SET_APPOINTMENT_STATUS: 'Қабылдау күйін өзгерту',
  CREATE_FLIGHT_BOOKING: 'Брондау жасау', CONFIRM_FLIGHT_BOOKING: 'Брондауды растау',
  CANCEL_FLIGHT_BOOKING: 'Брондауды болдырмау', EXPIRE_FLIGHT_BOOKING: 'Брондау мерзімі өтті',
  CREATE_DOCUMENT: 'Құжат жасау', EDIT_DOCUMENT: 'Құжатты өңдеу', SUBMIT_DOCUMENT: 'Бекітуге жіберу',
  APPROVE_DOCUMENT: 'Құжатты бекіту', REJECT_DOCUMENT: 'Құжатты қабылдамау', RETURN_DOCUMENT: 'Пысықтауға қайтару',
  COMMENT_DOCUMENT: 'Пікір қосу', CREATE_REPLENISHMENT_REQUEST: 'Толықтыру өтінімін жасау',
  UPDATE_NOTIFICATION_PREFERENCES: 'Хабарландыру баптаулары', RETRY_DELIVERY: 'Жеткізуді қайталау',
  SEND_TEST_NOTIFICATION: 'Сынақ хабарландыру', UPDATE_USER: 'Пайдаланушыны өзгерту',
  // document history actions
  CREATE: 'Жасалды', EDIT: 'Өңделді', SUBMIT: 'Жіберілді', APPROVE: 'Бекітілді', REJECT: 'Қабылданбады', RETURN: 'Қайтарылды',
  // domain events (audit "EVENT_*" entries)
  LowStockEvent: 'Қор аз', StockAdjustedEvent: 'Қор өзгерді', AppointmentBookedEvent: 'Қабылдауға жазылу',
  AppointmentRescheduledEvent: 'Қабылдау ауыстырылды', AppointmentCancelledEvent: 'Қабылдау болдырылмады',
  FlightBookingCreatedEvent: 'Брондау жасалды', FlightBookingConfirmedEvent: 'Брондау расталды',
  FlightBookingCancelledEvent: 'Брондау жабылды', DocumentSubmittedEvent: 'Құжат жіберілді',
  DocumentApprovedEvent: 'Құжат бекітілді', DocumentRejectedEvent: 'Құжат қабылданбады', DocumentReturnedEvent: 'Құжат қайтарылды',
  // entity types in the audit log
  Product: 'Тауар', Appointment: 'Қабылдау', Patient: 'Пациент', FlightBooking: 'Брондау', Document: 'Құжат',
  Notification: 'Хабарландыру', DeliveryAttempt: 'Жеткізу әрекеті', User: 'Пайдаланушы', Platform: 'Платформа', Number: 'Сан',
  // platform settings (PlatformSettings singleton)
  platformName: 'Платформа атауы', businessTimezone: 'Бизнес уақыт белдеуі', bookingHoldMinutes: 'Брондауды ұстау (мин)',
  appointmentSlotMinutes: 'Қабылдау ұзақтығы (мин)', managerApprovalThreshold: 'Менеджер бекітетін шек (₸)',
  additionalApprovalThreshold: 'Қосымша бекіту шегі (₸)', lowStockPolicy: 'Төмен қор саясаты',
  flightScheduleHorizonDays: 'Рейстер кестесі (күн)', emailEnabled: 'E-mail қосылған', singletonInstanceId: 'Singleton экземпляр ID',
  singletonCreatedAt: 'Singleton жасалған уақыты',
  // step outcomes (Chain of Responsibility trace)
  PASSED: 'Өтті', STEP_ADDED: 'Қадам қосылды', DECIDED: 'Шешім',
}

/** Kazakh display name of an internal code; unknown codes are shown as-is. */
export function label(code: string | null | undefined): string {
  if (!code) return '-'
  if (labels[code]) return labels[code]
  if (code.startsWith('EVENT_')) return labels[code.slice(6)] ?? code.slice(6)
  return code
}

export function hasLabel(code: string): boolean {
  return code in labels
}

const errors: Record<string, string> = {
  NETWORK: 'NEXORA серверіне қосылу мүмкін болмады. Сервер іске қосылғанын тексеріңіз.',
  BAD_CREDENTIALS: 'Пайдаланушы аты немесе құпиясөз қате',
  TOO_MANY_ATTEMPTS: 'Сәтсіз әрекеттер тым көп. Бірнеше минуттан кейін қайталап көріңіз.',
  UNAUTHENTICATED: 'Жүйеге кіру қажет',
  FORBIDDEN: 'Бұл әрекетті орындауға рұқсатыңыз жоқ',
  NOT_FOUND: 'Сұралған жазба табылмады',
  INTERNAL_ERROR: 'Күтпеген қате орын алды',
  CONCURRENT_MODIFICATION: 'Жазбаны басқа сұраныс өзгертті. Бетті жаңартып, қайталап көріңіз.',
}

/** Field names that may appear in server-side validation errors. */
const fields: Record<string, string> = {
  sku: 'SKU', name: 'Атауы', title: 'Тақырып', content: 'Мазмұны', amount: 'Сома', reason: 'Себебі', quantity: 'Мөлшері',
  minStock: 'Ең аз қор', purchasePrice: 'Сатып алу бағасы', retailPrice: 'Бөлшек баға', unit: 'Өлшем бірлігі',
  categoryId: 'Санат', firstName: 'Аты', lastName: 'Тегі', dateOfBirth: 'Туған күні', phone: 'Телефон', email: 'E-mail',
  contactEmail: 'Байланыс e-mail', passengers: 'Жолаушылар', flightId: 'Рейс', startTime: 'Уақыты', username: 'Пайдаланушы аты',
  password: 'Құпиясөз', comment: 'Пікір', body: 'Мәтін', status: 'Күйі',
}

/** Server messages are already Kazakh; the table covers client-side errors and empty messages. */
export function errorText(code: string, serverMessage?: string | null): string {
  if (serverMessage && serverMessage.trim()) return serverMessage
  return errors[code] ?? 'Қате орын алды'
}

export function fieldLabel(field: string): string {
  const base = field.replace(/\[\d+\]/g, '').split('.').pop() ?? field
  return fields[base] ?? base
}
