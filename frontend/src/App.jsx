import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import Login from './auth/Login';
import Register from './auth/Register';

function App() {
  return (
    <Router>
      <Routes>
        {/* Automatically redirect the home page to the login screen */}
        <Route path="/" element={<Navigate to="/login" replace />} />
        
        {/* Authentication Routes */}
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />
        
        {/* Placeholder for the next phase */}
        <Route path="/dashboard" element={
          <div className="min-h-screen bg-gray-50 flex items-center justify-center">
            <h1 className="text-3xl font-bold text-gray-800">Welcome to the Patient Dashboard!</h1>
          </div>
        } />
      </Routes>
    </Router>
  );
}

export default App;