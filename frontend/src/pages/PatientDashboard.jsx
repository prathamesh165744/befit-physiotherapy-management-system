import React from 'react';
import { Link, useNavigate } from 'react-router-dom';

const PatientDashboard = () => {
  const navigate = useNavigate();
  const name = localStorage.getItem('fullName') || 'Patient';
  const logout = () => { localStorage.clear(); navigate('/login'); };

  return (
    <div className="min-h-screen bg-gray-50 flex flex-col items-center justify-center gap-4">
      <h1 className="text-3xl font-bold text-gray-800">Patient Dashboard</h1>
      <p className="text-slate-500">Welcome, {name}</p>
      <div className="flex gap-3">
        <Link to="/dashboard/profile" className="bg-blue-600 hover:bg-blue-700 text-white px-6 py-3 rounded-lg text-sm font-bold">My Profile & Branch</Link>
        <button onClick={logout} className="bg-white border border-slate-200 text-slate-600 px-6 py-3 rounded-lg text-sm font-bold hover:bg-slate-100">Log out</button>
      </div>
    </div>
  );
};
export default PatientDashboard;
