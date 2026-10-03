import React from 'react';
import { Navigate } from 'react-router-dom';

export default function ProtectedRoute({ children, allowedRoles }) {
  const token = localStorage.getItem('token');
  const userRole = localStorage.getItem('role');

  // 1. Check if the user is logged in at all
  if (!token) {
    return <Navigate to="/login" replace />;
  }

  // 2. Check if the user has permission to view this specific page
  if (allowedRoles && !allowedRoles.includes(userRole)) {
    // If a patient tries to access staff pages, send them back to patient dashboard
    if (userRole === 'PATIENT') return <Navigate to="/dashboard" replace />;
    // If staff tries to access patient pages, send them to staff dashboard
    return <Navigate to="/staff-dashboard" replace />;
  }

  // 3. If they have a token and the right role, render the page
  return children;
}