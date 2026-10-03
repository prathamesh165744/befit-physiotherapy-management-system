import React, { useState, useEffect } from 'react';
import axios from 'axios';
import StaffLayout from '../layouts/StaffLayout';

const History = () => {
  const [history, setHistory] = useState([]);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    setIsLoading(true);
    axios.get('http://localhost:8080/api/appointments')
      .then(res => {
        // Filter out only COMPLETED appointments for the History log
        const completedApts = (res.data || []).filter(apt => apt.status === 'COMPLETED');
        // Sort by most recently completed
        completedApts.sort((a, b) => new Date(b.appointmentDate) - new Date(a.appointmentDate));
        setHistory(completedApts);
        setIsLoading(false);
      })
      .catch(err => {
        console.error("Failed to fetch history", err);
        setIsLoading(false);
      });
  }, []);

  return (
    <StaffLayout>
      <div className="max-w-[1400px] mx-auto pb-10">
        <div className="flex justify-between items-end mb-8">
          <div>
            <h1 className="text-2xl font-bold text-slate-800">Patient History Log</h1>
            <p className="text-[13px] text-slate-500 mt-1">Archive of all completed treatments and visits</p>
          </div>
        </div>

        <div className="bg-white rounded-2xl border border-slate-100 shadow-sm overflow-hidden">
          <div className="overflow-x-auto min-h-[300px]">
            <table className="w-full text-left text-[13px] text-slate-600">
              <thead className="bg-[#f8f9fc] text-[10px] uppercase font-bold text-slate-400">
                <tr>
                  <th className="px-6 py-4">APT ID</th>
                  <th className="px-6 py-4">DATE COMPLETED</th>
                  <th className="px-6 py-4">PATIENT</th>
                  <th className="px-6 py-4">TREATED BY</th>
                  <th className="px-6 py-4">TREATMENT TYPE</th>
                  <th className="px-6 py-4 text-right">STATUS</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {isLoading ? (
                  <tr><td colSpan="6" className="text-center py-10 text-slate-400 font-medium">Loading history...</td></tr>
                ) : history.length === 0 ? (
                  <tr><td colSpan="6" className="text-center py-10 text-slate-400 font-medium">No completed history found yet.</td></tr>
                ) : (
                  history.map((apt) => (
                    <tr key={apt.id} className="hover:bg-[#f8f9fc] transition">
                      <td className="px-6 py-4 font-bold text-slate-400">APT-{apt.id}</td>
                      <td className="px-6 py-4 font-medium text-slate-500">{new Date(apt.appointmentDate).toLocaleDateString()}</td>
                      <td className="px-6 py-4 font-bold text-slate-700">{apt.patient?.fullName || 'Walk-in'}</td>
                      <td className="px-6 py-4 font-medium">{apt.doctorName}</td>
                      <td className="px-6 py-4 text-slate-500">{apt.caseType}</td>
                      <td className="px-6 py-4 text-right">
                        <span className="text-[9px] font-bold px-2 py-1 rounded uppercase tracking-wider bg-slate-100 text-slate-500">
                          ARCHIVED
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
export default History;