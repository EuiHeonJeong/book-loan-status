import { Navigate, Route, Routes } from 'react-router-dom';
import { LoginPage } from './pages/LoginPage';
import { DashboardPage } from './pages/DashboardPage';
import { FamilyMembersPage } from './pages/FamilyMembersPage';
import { NotificationSettingsPage } from './pages/NotificationSettingsPage';

function App() {
  return (
    <div className="app-shell">
      <Routes>
        <Route path="/" element={<Navigate to="/login" replace />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/dashboard" element={<DashboardPage />} />
        <Route path="/family" element={<FamilyMembersPage />} />
        <Route path="/notifications" element={<NotificationSettingsPage />} />
      </Routes>
    </div>
  );
}

export default App;
