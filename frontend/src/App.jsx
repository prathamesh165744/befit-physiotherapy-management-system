import React from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import axios from 'axios';

import Login from './auth/Login';
import Register from './auth/Register';
import LandingPage from './pages/LandingPage';
import ProtectedRoute from './components/ProtectedRoute';

import PatientDashboard from './pages/PatientDashboard';
import StaffDashboard from './pages/StaffDashboard';
import PatientsDirectory from './pages/PatientsDirectory';
import Billing from './pages/Billing';
import AppointmentsList from './pages/AppointmentsList';
import History from './pages/History';
import PatientProfile from './pages/PatientProfile';

// --- MAGIC ROUTING INTERCEPTOR ---
axios.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  const branchId = localStorage.getItem('branchId');
  
  if (token) config.headers.Authorization = `Bearer ${token}`;
  
  // Automatically inject the staff's branchId into EVERY backend fetch request
  if (branchId && config.method === 'get') {
    config.params = { ...config.params, branchId: branchId };
  }
  
  return config;
});

function App() {
  return (
    <Router>
      <Routes>
        <Route path="/" element={<LandingPage />} />
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />
        <Route path="/dashboard" element={<ProtectedRoute allowedRoles={['PATIENT']}><PatientDashboard /></ProtectedRoute>} />
        
        <Route path="/staff-dashboard/*" element={
          <ProtectedRoute allowedRoles={['ADMIN', 'DOCTOR', 'RECEPTIONIST']}>
            <Routes>
              <Route path="/" element={<StaffDashboard />} />
              <Route path="/patients" element={<PatientsDirectory />} />
              <Route path="/appointments" element={<AppointmentsList />} />
              <Route path="/billing" element={<Billing />} />
              <Route path="/history" element={<History />} />
              <Route path="/patients/:id" element={<PatientProfile />} />
            </Routes>
          </ProtectedRoute>
        } />
      </Routes>
    </Router>
  );
}
export default App;