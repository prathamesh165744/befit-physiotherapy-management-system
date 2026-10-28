import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';

import Login from './auth/Login';
import Register from './auth/Register';
import LandingPage from './pages/LandingPage';
import ProtectedRoute from './components/ProtectedRoute';

import PatientLayout from './pages/PatientLayout'; // NEW
import PatientDashboard from './pages/PatientDashboard';
import StaffDashboard from './pages/StaffDashboard';
import PatientsDirectory from './pages/PatientsDirectory';
import PatientProfile from './pages/PatientProfile';
import StaffPatientProfile from './pages/StaffPatientProfile';
import Billing from './pages/Billing';
import AppointmentsList from './pages/AppointmentsList';
import History from './pages/History';
import TransferRequests from './pages/TransferRequests';
import OutgoingTransfers from './pages/OutgoingTransfers';

// Branch managers only work with transfers; the dashboard / patient / billing data is not theirs, so send them straight there.
function StaffHome() {
  return localStorage.getItem('role') === 'BRANCH_MANAGER' ? <Navigate to="/staff-dashboard/transfers" replace /> : <StaffDashboard />;
}

function App() {
  return (
    <Router>
      <Routes>
        <Route path="/" element={<LandingPage />} />
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />

        {/* PATIENT area: one layout (sidebar + header) shared by both pages */}
        <Route
          element={
            <ProtectedRoute allowedRoles={['PATIENT']}>
              <PatientLayout />
            </ProtectedRoute>
          }
        >
          <Route path="/dashboard" element={<PatientDashboard />} />
          <Route path="/dashboard/profile" element={<PatientProfile />} />
        </Route>

        <Route
          path="/staff-dashboard/*"
          element={
            <ProtectedRoute allowedRoles={['ADMIN', 'BRANCH_MANAGER', 'DOCTOR', 'RECEPTIONIST']}>
              <Routes>
                <Route path="/" element={<StaffHome />} />
                <Route path="/patients" element={<PatientsDirectory />} />
                <Route path="/patients/:id" element={<StaffPatientProfile />} />
                <Route path="/appointments" element={<AppointmentsList />} />
                <Route path="/billing" element={<Billing />} />
                <Route path="/transfers" element={<TransferRequests />} />
                <Route path="/transfers/outgoing" element={<OutgoingTransfers />} />
                <Route path="/history" element={<History />} />
              </Routes>
            </ProtectedRoute>
          }
        />
      </Routes>
    </Router>
  );
}

export default App;