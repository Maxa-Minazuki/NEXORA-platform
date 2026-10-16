import { lazy, Suspense } from 'react'
import { Navigate, Route, Routes } from 'react-router'
import { Spinner } from './components/ui/States'
import { AppLayout } from './components/layout/AppLayout'
import { RequireAuth } from './components/layout/RequireAuth'
import { LoginPage } from './pages/LoginPage'
import { NotFoundPage } from './pages/NotFoundPage'

const DashboardPage = lazy(() => import('./pages/dashboard/DashboardPage').then((m) => ({ default: m.DashboardPage })))
const InventoryPage = lazy(() => import('./pages/inventory/InventoryPage').then((m) => ({ default: m.InventoryPage })))
const ClinicPage = lazy(() => import('./pages/clinic/ClinicPage').then((m) => ({ default: m.ClinicPage })))
const FlightsPage = lazy(() => import('./pages/flights/FlightsPage').then((m) => ({ default: m.FlightsPage })))
const DocumentsPage = lazy(() => import('./pages/documents/DocumentsPage').then((m) => ({ default: m.DocumentsPage })))
const DocumentDetailPage = lazy(() => import('./pages/documents/DocumentDetailPage').then((m) => ({ default: m.DocumentDetailPage })))
const NotificationsPage = lazy(() => import('./pages/notifications/NotificationsPage').then((m) => ({ default: m.NotificationsPage })))
const AuditPage = lazy(() => import('./pages/AuditPage').then((m) => ({ default: m.AuditPage })))
const SettingsPage = lazy(() => import('./pages/SettingsPage').then((m) => ({ default: m.SettingsPage })))
const PatternsLabPage = lazy(() => import('./pages/lab/PatternsLabPage').then((m) => ({ default: m.PatternsLabPage })))

export default function App() {
  return (
    <Suspense fallback={<Spinner />}>
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route element={<RequireAuth><AppLayout /></RequireAuth>}>
        <Route index element={<DashboardPage />} />
        <Route path="inventory" element={<RequireAuth roles={['ADMIN', 'MANAGER', 'EMPLOYEE']}><InventoryPage /></RequireAuth>} />
        <Route path="clinic" element={<ClinicPage />} />
        <Route path="flights" element={<FlightsPage />} />
        <Route path="documents" element={<DocumentsPage />} />
        <Route path="documents/:id" element={<DocumentDetailPage />} />
        <Route path="notifications" element={<NotificationsPage />} />
        <Route path="audit" element={<RequireAuth roles={['ADMIN', 'MANAGER']}><AuditPage /></RequireAuth>} />
        <Route path="lab" element={<PatternsLabPage />} />
        <Route path="settings" element={<SettingsPage />} />
        <Route path="home" element={<Navigate to="/" replace />} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
    </Suspense>
  )
}
