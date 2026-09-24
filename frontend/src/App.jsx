import React, { useState, useEffect } from 'react';
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

// GLOBAL AXIOS RESPONSE INTERCEPTOR: Catch 401 (Unauthorized) and 403 (Forbidden)
axios.interceptors.response.use(
  (response) => {
    return response;
  },
  (error) => {
    if (error.response) {
      if (error.response.status === 401) {
        // Token expired or invalid
        alert("Session expired. Please log in again.");
        localStorage.clear();
        window.location.href = '/login';
      } else if (error.response.status === 403) {
        // Logged in, but wrong role (Access Denied)
        alert("Access Denied: You do not have permission to view this data.");
      }
    }
    return Promise.reject(error);
  }
);

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

// --- Secure Patient Dashboard Component ---
const PatientDashboard = () => {
  const [data, setData] = useState('');
  
  useEffect(() => {
    // Attempt to fetch Patient Data from the new Controller
    axios.get('http://localhost:8080/api/dashboard/patient-data')
      .then(res => setData(res.data))
      .catch(err => console.error("Failed to fetch data"));
  }, []);

  return (
    <div className="min-h-screen bg-gray-50 flex flex-col items-center justify-center">
      <h1 className="text-3xl font-bold text-gray-800">Welcome to the Patient Dashboard!</h1>
      <p className="text-gray-500 mt-2">Your token is successfully securing this page.</p>
      
      {/* This will only show if the backend accepts the JWT and Role */}
      {data && <p className="text-green-600 mt-4 font-bold text-xl">Backend says: "{data}"</p>}
      
      <LogoutButton />
    </div>
  );
};

// --- Secure Staff Dashboard Component ---
const StaffDashboard = () => {
  const [data, setData] = useState('');
  
  useEffect(() => {
    // Attempt to fetch Staff Data from the new Controller
    axios.get('http://localhost:8080/api/dashboard/staff-data')
      .then(res => setData(res.data))
      .catch(err => console.error("Failed to fetch data"));
  }, []);

  return (
    <div className="min-h-screen bg-slate-900 flex flex-col items-center justify-center">
      <h1 className="text-3xl font-bold text-white">Welcome to the Staff Portal!</h1>
      <p className="text-gray-400 mt-2">Secure access granted based on staff role.</p>
      
      {/* This will only show if the backend accepts the JWT and Role */}
      {data && <p className="text-blue-400 mt-4 font-bold text-xl">Backend says: "{data}"</p>}
      
      <LogoutButton />
    </div>
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
            <PatientDashboard />
          </ProtectedRoute>
        } />
        
        {/* Protected STAFF Routes */}
        <Route path="/staff-dashboard" element={
          <ProtectedRoute allowedRoles={['ADMIN', 'DOCTOR', 'RECEPTIONIST']}>
            <StaffDashboard />
          </ProtectedRoute>
        } />
      </Routes>
    </Router>
  );
}

export default App;