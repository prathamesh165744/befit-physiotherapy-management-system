import React from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';

import Login from './auth/Login';
import Register from './auth/Register';
import LandingPage from './pages/LandingPage';
import ProtectedRoute from './components/ProtectedRoute';

import PatientDashboard from './pages/PatientDashboard';
import StaffDashboard from './pages/StaffDashboard';
import PatientsDirectory from './pages/PatientsDirectory';
import PatientProfile from './pages/PatientProfile';          // a PATIENT's own "My Profile" page
import StaffPatientProfile from './pages/StaffPatientProfile'; // staff "View File" page
import Billing from './pages/Billing';
import AppointmentsList from './pages/AppointmentsList';
import History from './pages/History';
import TransferRequests from './pages/TransferRequests';

// The axios token / base URL / 401 handling now lives in src/api.js (imported once in main.jsx).

function App() {
  return (
    <Router>
      <Routes>
        <Route path="/" element={<LandingPage />} />
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />
        <Route
          path="/dashboard"
          element={
            <ProtectedRoute allowedRoles={['PATIENT']}>
              <PatientDashboard />
            </ProtectedRoute>
          }
        />
        <Route
          path="/dashboard/profile"
          element={
            <ProtectedRoute allowedRoles={['PATIENT']}>
              <PatientProfile />
            </ProtectedRoute>
          }
        />

        <Route
          path="/staff-dashboard/*"
          element={
            <ProtectedRoute allowedRoles={['ADMIN', 'DOCTOR', 'RECEPTIONIST']}>
              <Routes>
                <Route path="/" element={<StaffDashboard />} />
                <Route path="/patients" element={<PatientsDirectory />} />
                {/* FIX: "View File" used to render PatientProfile (the patient's own page, which ignores :id and calls /api/patients/me) */}
                <Route path="/patients/:id" element={<StaffPatientProfile />} />
                <Route path="/appointments" element={<AppointmentsList />} />
                <Route path="/billing" element={<Billing />} />
                <Route path="/transfers" element={<TransferRequests />} />
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
