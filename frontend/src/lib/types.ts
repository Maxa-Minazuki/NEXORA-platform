export type Role = 'ADMIN' | 'MANAGER' | 'DOCTOR' | 'EMPLOYEE' | 'USER'

export interface Page<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface UserProfile {
  id: number
  username: string
  fullName: string
  email: string
  role: Role
  title: string | null
}

export interface DemoAccount {
  username: string
  role: Role
  fullName: string
  description: string
}

// ---------------------------------------------------------------- inventory
export type StockStatus = 'IN_STOCK' | 'LOW_STOCK' | 'OUT_OF_STOCK'
export type ReplenishmentPolicy = 'TOP_UP_TO_TARGET' | 'FIXED_LOT' | 'DEMAND_BASED'
export type MovementType = 'RECEIPT' | 'WRITE_OFF' | 'ADJUSTMENT' | 'INITIAL'

export interface Product {
  id: number
  sku: string
  name: string
  description: string | null
  categoryId: number
  categoryName: string
  supplierId: number | null
  supplierName: string | null
  purchasePrice: number
  retailPrice: number
  quantity: number
  minStock: number
  criticalThreshold: number
  unit: string
  replenishmentStrategy: ReplenishmentPolicy
  stockStatus: StockStatus
  stockValue: number
  active: boolean
  updatedAt: string
}

export interface InventoryTransaction {
  id: number
  productId: number
  productSku: string
  productName: string
  type: MovementType
  quantityChange: number
  quantityBefore: number
  quantityAfter: number
  reason: string | null
  performedBy: number | null
  createdAt: string
}

export interface AdjustmentResult {
  product: Product
  transaction: InventoryTransaction
  lowStockTriggered: boolean
  detectionStrategy: string
}

export interface StockAlert {
  id: number
  productId: number
  productSku: string
  productName: string
  quantityAtAlert: number
  threshold: number
  currentQuantity: number
  suggestedQuantity: number
  strategy: string
  status: 'OPEN' | 'REQUEST_CREATED' | 'RESOLVED'
  documentId: number | null
  createdAt: string
  resolvedAt: string | null
}

export interface InventoryStats {
  totalProducts: number
  totalUnits: number
  stockValue: number
  retailValue: number
  lowStockCount: number
  outOfStockCount: number
  openAlerts: number
  detectionStrategy: string
  byCategory: { category: string; products: number; value: number }[]
}

export interface Category { id: number; name: string; description: string | null }
export interface Supplier { id: number; name: string; contactEmail: string | null; phone: string | null }
export interface StrategyInfo { key: string; description: string; active: boolean }

// ---------------------------------------------------------------- clinic
export type AppointmentStatus = 'SCHEDULED' | 'CONFIRMED' | 'COMPLETED' | 'CANCELLED' | 'NO_SHOW'

export interface Specialty { id: number; name: string; description: string | null }
export interface ScheduleEntry { dayOfWeek: string; startTime: string; endTime: string }
export interface Doctor {
  id: number
  fullName: string
  specialtyId: number
  specialty: string
  email: string | null
  phone: string | null
  room: string | null
  bio: string | null
  schedule: ScheduleEntry[]
}
export interface Patient {
  id: number
  firstName: string
  lastName: string
  fullName: string
  dateOfBirth: string
  gender: 'FEMALE' | 'MALE' | 'OTHER'
  phone: string | null
  email: string | null
  notes: string | null
  hasAccount: boolean
  createdAt: string
}
export interface Appointment {
  id: number
  patientId: number
  patientName: string
  doctorId: number
  doctorName: string
  specialty: string
  room: string | null
  startTime: string
  endTime: string
  status: AppointmentStatus
  reason: string
  notes: string | null
  cancelReason: string | null
  allowedTransitions: AppointmentStatus[]
  createdAt: string
}
export interface Slot { start: string; end: string; available: boolean }
export interface DaySlots {
  doctorId: number
  date: string
  timezone: string
  slotMinutes: number
  workingDay: boolean
  slots: Slot[]
}

// ---------------------------------------------------------------- flights
export type BookingStatus = 'PENDING' | 'CONFIRMED' | 'CANCELLED' | 'EXPIRED'
export interface Airport { id: number; code: string; name: string; city: string; country: string; timezone: string }
export interface Flight {
  id: number
  flightNumber: string
  airline: string
  origin: Airport
  destination: Airport
  departureTime: string
  arrivalTime: string
  durationMinutes: number
  price: number
  availableSeats: number
  totalSeats: number
  status: string
}
export interface SearchResult {
  sort: string
  sortLabel: string
  availableSorts: string[]
  flights: Flight[]
  disclaimer: string
}
export interface Seat { seatNumber: string; status: 'AVAILABLE' | 'HELD' | 'BOOKED' }
export interface Booking {
  id: number
  reference: string
  flight: Flight
  userId: number
  passengerCount: number
  totalPrice: number
  contactEmail: string
  status: BookingStatus
  allowedActions: string[]
  expiresAt: string
  confirmedAt: string | null
  cancelledAt: string | null
  cancelReason: string | null
  passengers: { fullName: string; seatNumber: string }[]
  confirmation: { documentNumber: string; workflowDocumentId: number | null; content: string; issuedAt: string } | null
  createdAt: string
  coordinationLog: string[]
}

// ---------------------------------------------------------------- workflow
export type DocumentStatus = 'DRAFT' | 'PENDING_APPROVAL' | 'IN_REVIEW' | 'APPROVED' | 'REJECTED'
export type DocumentType =
  | 'PURCHASE_REQUEST'
  | 'INVENTORY_REPLENISHMENT'
  | 'MEDICAL_APPOINTMENT_REPORT'
  | 'FLIGHT_BOOKING_CONFIRMATION'
  | 'GENERAL_ADMINISTRATIVE'

export interface DocumentSummary {
  id: number
  number: string
  title: string
  type: DocumentType
  typeLabel: string
  status: DocumentStatus
  authorId: number
  authorName: string
  amount: number | null
  currentStep: string | null
  currentApproverRole: Role | null
  createdAt: string
  updatedAt: string
  submittedAt: string | null
}
export interface TrackItem {
  key: string
  label: string
  state: 'DONE' | 'CURRENT' | 'PENDING' | 'FAILED' | 'RETURNED' | 'SKIPPED'
  actor: string | null
  at: string | null
  note: string | null
}
export interface PlannedStep { name: string; requiredRole: Role; reason: string }
export interface TraceEntry { handler: string; outcome: 'PASSED' | 'STEP_ADDED' | 'SKIPPED' | 'FAILED' | 'DECIDED'; message: string }
export interface DocumentDetail {
  summary: DocumentSummary
  content: string
  relatedEntityType: string | null
  relatedEntityId: number | null
  revision: number
  approvalRound: number
  rejectionReason: string | null
  allowedActions: string[]
  canDecide: boolean
  track: TrackItem[]
  steps: {
    id: number; round: number; order: number; name: string; requiredRole: Role; status: string
    decidedBy: string | null; decidedAt: string | null; comment: string | null
  }[]
  plannedRoute: PlannedStep[]
  history: { action: string; actor: string | null; fromStatus: DocumentStatus | null; toStatus: DocumentStatus | null; note: string | null; at: string }[]
  comments: { id: number; author: string; body: string; createdAt: string }[]
  revisions: { revision: number; title: string; amount: number | null; changedBy: string | null; createdAt: string }[]
}
export interface WorkflowOutcome { document: DocumentDetail; coordinationLog: string[] }
export interface ChainResult {
  passed: boolean
  failure: string | null
  decision: string | null
  trace: TraceEntry[]
  route: PlannedStep[]
  handlers: string[]
}
export interface DocumentTypeInfo {
  type: DocumentType
  label: string
  amountRequired: boolean
  relatedEntityType: string | null
  systemIssued: boolean
  authorRoles: Role[]
}

// ---------------------------------------------------------------- notifications
export type Severity = 'INFO' | 'SUCCESS' | 'WARNING' | 'CRITICAL'
export type Channel = 'IN_APP' | 'EMAIL'
export type NotificationCategory = 'INVENTORY' | 'CLINIC' | 'FLIGHT' | 'WORKFLOW' | 'SYSTEM'
export interface DeliveryAttempt {
  id: number
  notificationId: number
  channel: Channel
  status: 'DELIVERED' | 'FAILED' | 'SKIPPED'
  attemptNo: number
  detail: string | null
  createdAt: string
  retryable: boolean
}
export interface AppNotification {
  id: number
  category: NotificationCategory
  severity: Severity
  title: string
  message: string
  link: string | null
  sourceModule: string
  read: boolean
  readAt: string | null
  createdAt: string
  deliveries: DeliveryAttempt[]
}
export interface Preference { channel: Channel; enabled: boolean; minSeverity: Severity }
export interface ChannelInfo {
  channel: Channel
  factory: string
  family: string[]
  available: boolean
  availabilityReason: string
  creator: string
  retryable: boolean
}
export interface DispatchReport {
  notificationId: number
  severity: Severity
  strategy: string
  selectedChannels: Channel[]
  attempts: DeliveryAttempt[]
}

// ---------------------------------------------------------------- dashboard & audit
export interface AuditEntry {
  id: number
  actor: string
  action: string
  entityType: string
  entityId: string | null
  outcome: 'SUCCESS' | 'FAILURE'
  details: string | null
  createdAt: string
}
export interface Activity {
  id: number
  actor: string
  action: string
  entityType: string
  entityId: string | null
  outcome: string
  details: string | null
  at: string
}
export interface DashboardView {
  greetingName: string
  role: Role
  generatedAt: string
  timezone: string
  modules: string[]
  kpis: {
    products: number | null
    stockValue: number | null
    lowStock: number | null
    outOfStock: number | null
    doctors: number
    appointmentsToday: number
    upcomingAppointments: number
    bookableFlights: number
    bookings: number
    documentsInApproval: number
    awaitingMyAction: number
    unreadNotifications: number
  }
  inventoryMovement: { date: string; inbound: number; outbound: number }[]
  appointmentTrend: { date: string; count: number }[]
  bookingDistribution: { name: string; value: number }[]
  documentStatus: { name: string; value: number }[]
  notificationActivity: { date: string; info: number; success: number; warning: number; critical: number }[]
  upcomingAppointments: { id: number; patient: string; doctor: string; specialty: string; start: string; status: string }[]
  recentEvents: Activity[]
  recentActions: Activity[]
  attention: { kind: string; title: string; detail: string; link: string; severity: string }[]
}
export interface AdminUser {
  id: number
  username: string
  fullName: string
  email: string
  role: Role
  title: string | null
  active: boolean
  createdAt: string
}
