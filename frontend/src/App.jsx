import React from 'react';
import { BrowserRouter as Router, Routes, Route, useNavigate } from 'react-router-dom';
import axios from 'axios';

import Login from './auth/Login';
import Register from './auth/Register';
import LandingPage from './pages/LandingPage';
import ProtectedRoute from './components/ProtectedRoute';

// GLOBAL AXIOS INTERCEPTOR: Automatically attach JWT token to every request
axios.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Simple Logout Component for our Placeholders
const LogoutButton = () => {
  const navigate = useNavigate();
  const handleLogout = () => {
    localStorage.clear(); // Clear token, role, and name
    navigate('/'); // Go back to landing page
  };
  return (
    <button onClick={handleLogout} className="mt-6 bg-red-600 text-white px-6 py-2 rounded-lg hover:bg-red-700 transition">
      Logout
    </button>
  );
};

function App() {
  return (
    <Router>
      <Routes>
        {/* The public website is now the default root */}
        <Route path="/" element={<LandingPage />} />
        
        {/* Authentication Routes */}
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />
        
        {/* Protected PATIENT Routes */}
        <Route path="/dashboard" element={
          <ProtectedRoute allowedRoles={['PATIENT']}>
            <div className="min-h-screen bg-gray-50 flex flex-col items-center justify-center">
              <h1 className="text-3xl font-bold text-gray-800">Welcome to the Patient Dashboard!</h1>
              <p className="text-gray-500 mt-2">Your token is successfully securing this page.</p>
              <LogoutButton />
            </div>
          </ProtectedRoute>
        } />
        
        {/* Protected STAFF Routes */}
        <Route path="/staff-dashboard" element={
          <ProtectedRoute allowedRoles={['ADMIN', 'DOCTOR', 'RECEPTIONIST']}>
            <div className="min-h-screen bg-slate-900 flex flex-col items-center justify-center">
              <h1 className="text-3xl font-bold text-white">Welcome to the Staff Portal!</h1>
              <p className="text-gray-400 mt-2">Secure access granted based on staff role.</p>
              <LogoutButton />
            </div>
          </ProtectedRoute>
        } />
      </Routes>
    </Router>
  );
}

export default App;