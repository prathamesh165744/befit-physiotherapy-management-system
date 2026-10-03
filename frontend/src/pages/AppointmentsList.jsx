import React, { useState, useEffect } from 'react';
import axios from 'axios';
import StaffLayout from '../layouts/StaffLayout';

const AppointmentsList = () => {
  const [appointments, setAppointments] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [filterDate, setFilterDate] = useState(''); // Empty means all dates

  useEffect(() => {
    setIsLoading(true);
    axios.get('http://localhost:8080/api/appointments')
      .then(res => {
        setAppointments(res.data || []);
        setIsLoading(false);
      })
      .catch(err => {
        console.error("Failed to fetch appointments", err);
        setIsLoading(false);
      });
  }, []);

  // Filter logic
  const filteredAppointments = appointments.filter(apt => {
    if (!filterDate) return true;
    return apt.appointmentDate && apt.appointmentDate.startsWith(filterDate);
  });

  return (
    <StaffLayout>
      <div className="max-w-[1400px] mx-auto pb-10">
        <div className="flex justify-between items-end mb-8">
          <div>
            <h1 className="text-2xl font-bold text-slate-800">All Appointments</h1>
            <p className="text-[13px] text-slate-500 mt-1">Master list of all scheduled clinic visits</p>
          </div>
          <div className="bg-white border border-slate-200 rounded-lg px-3 py-2 flex items-center gap-2 shadow-sm">
            <span className="text-xs font-bold text-slate-400 uppercase">Filter Date:</span>
            <input 
              type="date" 
              className="text-sm font-semibold text-slate-700 outline-none cursor-pointer"
              value={filterDate}
              onChange={(e) => setFilterDate(e.target.value)}
            />
            {filterDate && (
              <button onClick={() => setFilterDate('')} className="text-[10px] bg-red-50 text-red-600 px-2 py-1 rounded ml-2 font-bold hover:bg-red-100">CLEAR</button>
            )}
          </div>
        </div>

        <div className="bg-white rounded-2xl border border-slate-100 shadow-sm overflow-hidden">
          <div className="overflow-x-auto min-h-[300px]">
            <table className="w-full text-left text-[13px] text-slate-600">
              <thead className="bg-[#f8f9fc] text-[10px] uppercase font-bold text-slate-400">
                <tr>
                  <th className="px-6 py-4">APT ID</th>
                  <th className="px-6 py-4">DATE & TIME</th>
                  <th className="px-6 py-4">PATIENT</th>
                  <th className="px-6 py-4">DOCTOR</th>
                  <th className="px-6 py-4">CASE TYPE</th>
                  <th className="px-6 py-4">STATUS</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {isLoading ? (
                  <tr><td colSpan="6" className="text-center py-10 text-slate-400 font-medium">Loading appointments...</td></tr>
                ) : filteredAppointments.length === 0 ? (
                  <tr><td colSpan="6" className="text-center py-10 text-slate-400 font-medium">No appointments found.</td></tr>
                ) : (
                  filteredAppointments.map((apt) => (
                    <tr key={apt.id} className="hover:bg-[#f8f9fc] transition">
                      <td className="px-6 py-4 font-bold text-[#2563eb]">APT-{apt.id}</td>
                      <td className="px-6 py-4 font-medium">{new Date(apt.appointmentDate).toLocaleString()}</td>
                      <td className="px-6 py-4 font-bold text-slate-800">{apt.patient?.fullName || 'Walk-in'}</td>
                      <td className="px-6 py-4 text-slate-500">{apt.doctorName}</td>
                      <td className="px-6 py-4 font-medium">{apt.caseType}</td>
                      <td className="px-6 py-4">
                        <span className={`text-[9px] font-bold px-2 py-1 rounded uppercase tracking-wider ${apt.status === 'WAITING' ? 'bg-yellow-50 text-yellow-600' : apt.status === 'CHECKED_IN' ? 'bg-green-50 text-green-600' : 'bg-blue-50 text-blue-600'}`}>
                          {apt.status}
                        </span>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </StaffLayout>
  );
};
export default AppointmentsList;