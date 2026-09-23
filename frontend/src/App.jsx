import React from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import Login from './auth/Login';
import Register from './auth/Register';
import LandingPage from './pages/LandingPage';

function App() {
  return (
    <Router>
      <Routes>
        {/* The public website is now the default root */}
        <Route path="/" element={<LandingPage />} />
        
        {/* Authentication Routes */}
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />
        
        {/* Placeholder for the patient dashboard */}
        <Route path="/dashboard" element={
          <div className="min-h-screen bg-gray-50 flex items-center justify-center">
            <h1 className="text-3xl font-bold text-gray-800">Welcome to the Patient Dashboard!</h1>
          </div>
        } />
        
        {/* Staff Portal Placeholder */}
        <Route path="/staff-dashboard" element={
          <div className="min-h-screen bg-slate-900 flex items-center justify-center">
            <h1 className="text-3xl font-bold text-white">Welcome to the Staff Portal!</h1>
          </div>
        } />
      </Routes>
    </Router>
  );
}

export default App;