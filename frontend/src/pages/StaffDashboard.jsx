import React, { useState, useEffect } from 'react';
import axios from 'axios';
import StaffLayout from '../layouts/StaffLayout';
import RegisterPatientModal from '../components/modals/RegisterPatientModal';
import NewAppointmentModal from '../components/modals/NewAppointmentModal';

const StaffDashboard = () => {
  const [dashboardData, setDashboardData] = useState({
    todayTotal: 0, checkedIn: 0, waiting: 0, newInquiries: 0, upcomingAppointments: [], currentRegistrations: []
  });
  const [isLoading, setIsLoading] = useState(true);
  
  const [isRegisterModalOpen, setIsRegisterModalOpen] = useState(false);
  const [isAppointmentModalOpen, setIsAppointmentModalOpen] = useState(false);

  const fetchDashboardData = () => {
    setIsLoading(true);
    axios.get('http://localhost:8080/api/dashboard/staff-data')
      .then(res => {
        const data = res.data || {};
        setDashboardData({
          todayTotal: data.todayTotal || 0,
          checkedIn: data.checkedIn || 0,
          waiting: data.waiting || 0,
          newInquiries: data.newInquiries || 0,
          upcomingAppointments: Array.isArray(data.upcomingAppointments) ? data.upcomingAppointments : [],
          currentRegistrations: Array.isArray(data.currentRegistrations) ? data.currentRegistrations : []
        });
        setIsLoading(false);
      })
      .catch(err => {
        console.error("Failed to fetch dashboard data", err);
        setIsLoading(false);
      });
  };

  useEffect(() => {
    fetchDashboardData();
  }, []);

  const padNum = (num) => (num < 10 && num > 0 ? `0${num}` : num === 0 ? "00" : num);

  return (
    <StaffLayout>
      <div className="max-w-[1400px] mx-auto pb-10">
        
        <div className="flex justify-between items-end mb-8">
          <div>
            <h1 className="text-2xl font-bold text-slate-800">Front Desk — Karve Road</h1>
            <p className="text-[13px] text-slate-500 mt-1 flex items-center gap-1.5">
              <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z"></path><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M15 11a3 3 0 11-6 0 3 3 0 016 0z"></path></svg>
              12th Floor, BeFit Tower, Pune • Today: {new Date().toLocaleDateString('en-US', { weekday: 'long', month: 'long', day: 'numeric' })}
            </p>
          </div>
          <button onClick={() => setIsAppointmentModalOpen(true)} className="bg-[#2563eb] hover:bg-blue-700 text-white text-sm font-semibold py-2.5 px-5 rounded-lg shadow-sm transition-colors flex items-center gap-2">
            <span className="text-lg leading-none">+</span> New Appointment
          </button>
        </div>

        <div className="grid grid-cols-4 gap-6 mb-8">
          <div className="bg-white p-6 rounded-2xl border border-slate-100 shadow-sm">
            <p className="text-[10px] font-bold text-slate-400 tracking-wider uppercase mb-2">TODAY'S TOTAL</p>
            <div className="flex justify-between items-center">
              <h3 className="text-4xl font-bold text-slate-800">{padNum(dashboardData.todayTotal)}</h3>
              <div className="w-9 h-9 rounded-full bg-blue-50 text-blue-600 flex items-center justify-center"><svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"></path></svg></div>
            </div>
            <p className="text-[13px] text-slate-500 mt-2 font-medium">Appointments scheduled</p>
          </div>
          <div className="bg-white p-6 rounded-2xl border border-slate-100 shadow-sm">
            <p className="text-[10px] font-bold text-slate-400 tracking-wider uppercase mb-2">CHECKED IN</p>
            <div className="flex justify-between items-center">
              <h3 className="text-4xl font-bold text-slate-800">{padNum(dashboardData.checkedIn)}</h3>
              <div className="w-9 h-9 rounded-full bg-blue-50 text-blue-600 flex items-center justify-center"><svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg></div>
            </div>
            <p className="text-[13px] text-slate-500 mt-2 font-medium">Patients currently in clinic</p>
          </div>
          <div className="bg-white p-6 rounded-2xl border border-slate-100 shadow-sm">
            <p className="text-[10px] font-bold text-slate-400 tracking-wider uppercase mb-2">WAITING</p>
            <div className="flex justify-between items-center">
              <h3 className="text-4xl font-bold text-slate-800">{padNum(dashboardData.waiting)}</h3>
              <div className="w-9 h-9 rounded-full bg-blue-50 text-blue-600 flex items-center justify-center"><svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg></div>
            </div>
            <p className="text-[13px] text-slate-500 mt-2 font-medium">Average wait time: 12 min</p>
          </div>
          <div className="bg-white p-6 rounded-2xl border border-slate-100 shadow-sm">
            <p className="text-[10px] font-bold text-slate-400 tracking-wider uppercase mb-2">NEW INQUIRIES</p>
            <div className="flex justify-between items-center">
              <h3 className="text-4xl font-bold text-slate-800">{padNum(dashboardData.newInquiries)}</h3>
              <div className="w-9 h-9 rounded-full bg-blue-50 text-blue-600 flex items-center justify-center"><svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M18 9v3m0 0v3m0-3h3m-3 0h-3m-2-5a4 4 0 11-8 0 4 4 0 018 0zM3 20a6 6 0 0112 0v1H3v-1z"></path></svg></div>
            </div>
            <p className="text-[13px] text-slate-500 mt-2 font-medium">Pending via portal</p>
          </div>
        </div>

        <div className="grid grid-cols-12 gap-6 mb-8">
          
          <div className="col-span-4 space-y-6">
            <div className="bg-white rounded-2xl border border-slate-100 p-6 shadow-sm">
              <h3 className="text-[17px] font-bold text-slate-800 mb-5">Quick Actions</h3>
              <div className="grid grid-cols-2 gap-4">
                <button onClick={() => setIsRegisterModalOpen(true)} className="bg-[#f8f9fc] p-5 rounded-xl border border-slate-100 hover:border-blue-200 hover:bg-blue-50 transition flex flex-col items-center justify-center gap-3 cursor-pointer">
                  <svg className="w-6 h-6 text-[#2563eb]" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M18 9v3m0 0v3m0-3h3m-3 0h-3m-2-5a4 4 0 11-8 0 4 4 0 018 0zM3 20a6 6 0 0112 0v1H3v-1z"></path></svg>
                  <span className="text-[13px] font-semibold text-[#2563eb]">Register</span>
                </button>
                <button className="bg-[#f8f9fc] p-5 rounded-xl border border-slate-100 hover:border-green-200 hover:bg-green-50 transition flex flex-col items-center justify-center gap-3">
                  <svg className="w-6 h-6 text-green-500" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
                  <span className="text-[13px] font-semibold text-green-600">Check-In</span>
                </button>
                <button className="bg-[#f8f9fc] p-5 rounded-xl border border-slate-100 hover:border-yellow-200 hover:bg-yellow-50 transition flex flex-col items-center justify-center gap-3">
                  <svg className="w-6 h-6 text-yellow-500" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"></path></svg>
                  <span className="text-[13px] font-semibold text-yellow-600">Generate Bill</span>
                </button>
                <button className="bg-[#f8f9fc] p-5 rounded-xl border border-slate-100 hover:border-slate-200 transition flex flex-col items-center justify-center gap-3">
                  <span className="text-slate-400 font-bold mb-1 text-xl leading-none">⋮</span>
                  <span className="text-[13px] font-semibold text-slate-500">Others</span>
                </button>
              </div>
            </div>

            <div className="bg-white rounded-2xl border border-slate-100 p-6 shadow-sm">
              <div className="flex justify-between items-center mb-5">
                <h3 className="text-[17px] font-bold text-slate-800">Recent Check-ins</h3>
                <span className="text-[11px] font-bold text-[#2563eb] cursor-pointer uppercase tracking-wider">View All</span>
              </div>
              <div className="space-y-4">
                <div className="flex items-center justify-between border-b border-slate-50 pb-4">
                  <div className="flex items-center gap-4">
                    <img src="https://i.pravatar.cc/150?img=32" className="w-10 h-10 rounded-full" alt="Patient" />
                    <div><p className="text-[14px] font-bold text-slate-800">Priya Sharma</p><p className="text-[11px] text-slate-400 font-medium">08:45 AM</p></div>
                  </div>
                  <span className="text-[9px] font-bold text-slate-500 bg-slate-100 px-2 py-1 rounded">BILLED</span>
                </div>
                <div className="flex items-center justify-between border-b border-slate-50 pb-4">
                  <div className="flex items-center gap-4">
                    <img src="https://i.pravatar.cc/150?img=11" className="w-10 h-10 rounded-full" alt="Patient" />
                    <div><p className="text-[14px] font-bold text-slate-800">Amit Gokhale</p><p className="text-[11px] text-slate-400 font-medium">09:00 AM</p></div>
                  </div>
                  <span className="text-[9px] font-bold text-blue-600 bg-blue-50 px-2 py-1 rounded">TREATMENT</span>
                </div>
                <div className="flex items-center justify-between pb-1">
                  <div className="flex items-center gap-4">
                    <img src="https://i.pravatar.cc/150?img=5" className="w-10 h-10 rounded-full" alt="Patient" />
                    <div><p className="text-[14px] font-bold text-slate-800">Sneha More</p><p className="text-[11px] text-slate-400 font-medium">09:10 AM</p></div>
                  </div>
                  <span className="text-[9px] font-bold text-yellow-600 bg-yellow-50 px-2 py-1 rounded">WAITING</span>
                </div>
              </div>
            </div>
          </div>

          <div className="col-span-8 bg-white rounded-2xl border border-slate-100 p-6 shadow-sm">
            <div className="flex justify-between items-center mb-6">
              <div>
                <h3 className="text-[17px] font-bold text-slate-800">Upcoming Appointments</h3>
                <p className="text-[13px] text-slate-500 mt-1">Next 2 hours schedule</p>
              </div>
              <div className="flex items-center gap-3">
                <div className="relative">
                  <svg className="w-4 h-4 absolute left-3 top-2 text-slate-400" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"></path></svg>
                  <input type="text" placeholder="Search schedule..." className="pl-9 pr-3 py-1.5 bg-[#f4f7fe] border border-transparent rounded-lg text-[13px] focus:outline-none focus:bg-white focus:border-slate-300 w-48" />
                </div>
                <button className="flex items-center gap-1.5 text-slate-600 font-semibold text-[13px] border border-slate-200 px-3 py-1.5 rounded-lg hover:bg-slate-50"><svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M3 4a1 1 0 011-1h16a1 1 0 011 1v2.586a1 1 0 01-.293.707l-6.414 6.414a1 1 0 00-.293.707V17l-4 4v-6.586a1 1 0 00-.293-.707L3.293 7.293A1 1 0 013 6.586V4z"></path></svg> Filter</button>
              </div>
            </div>

            <div className="space-y-3 min-h-[300px]">
              {isLoading ? (
                <div className="flex items-center justify-center h-full text-slate-400 font-medium">Loading appointments...</div>
              ) : dashboardData.upcomingAppointments.length === 0 ? (
                <div className="flex flex-col items-center justify-center h-full text-slate-500 bg-slate-50 rounded-xl py-10 border border-dashed border-slate-200 mt-4">
                  <span className="text-3xl mb-2">📅</span>
                  <p className="font-semibold text-sm">No upcoming appointments</p>
                  <p className="text-xs text-slate-400">Click '+ New Appointment' to schedule one.</p>
                </div>
              ) : (
                dashboardData.upcomingAppointments.map((apt, idx) => (
                  <div key={idx} className="flex items-center justify-between p-3.5 border border-[#f4f7fe] rounded-xl hover:bg-[#f8f9fc] transition cursor-pointer">
                    <div className="flex items-center gap-6">
                      <span className="text-[13px] font-bold text-[#2563eb] w-12">{apt.time}</span>
                      <div>
                        <p className="text-[14px] font-bold text-slate-800">{apt.name}</p>
                        <p className="text-[11px] text-slate-500 flex items-center gap-1 mt-0.5 font-medium"><svg className="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"></path></svg> {apt.doc}</p>
                      </div>
                    </div>
                    <div className="flex items-center gap-8">
                      <span className="text-[13px] font-semibold text-slate-700 w-32 text-right">{apt.type}</span>
                      <span className={`text-[9px] font-bold px-2 py-1.5 rounded w-16 text-center ${apt.color}`}>{apt.status}</span>
                      <span className="text-slate-300 font-bold">→</span>
                    </div>
                  </div>
                ))
              )}
            </div>
            
            <div className="mt-5 pt-5 border-t border-slate-100">
              <span className="text-[13px] font-bold text-[#2563eb] cursor-pointer">View full daily calendar →</span>
            </div>
          </div>
        </div>

        <div className="bg-white rounded-2xl border border-slate-100 shadow-sm overflow-hidden mb-6">
          <div className="p-6 flex justify-between items-center border-b border-slate-100">
            <h3 className="text-[17px] font-bold text-slate-800">Current Registrations</h3>
            <button className="flex items-center gap-1.5 text-[13px] font-semibold text-slate-600 border border-slate-200 px-3 py-1.5 rounded-lg hover:bg-slate-50">
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 17v-2m3 2v-4m3 4v-6m2 10H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"></path></svg>
              Export List
            </button>
          </div>
          
          <div className="overflow-x-auto min-h-[200px]">
            <table className="w-full text-left text-[13px] text-slate-600">
              <thead className="bg-[#f8f9fc] text-[10px] uppercase font-bold text-slate-400">
                <tr>
                  <th className="px-6 py-4">REG ID</th>
                  <th className="px-6 py-4">PATIENT NAME</th>
                  <th className="px-6 py-4">AGE/GENDER</th>
                  <th className="px-6 py-4">CONSULTING DR.</th>
                  <th className="px-6 py-4">CASE TYPE</th>
                  <th className="px-6 py-4">STATUS</th>
                  <th className="px-6 py-4 text-right">ACTIONS</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {isLoading ? (
                  <tr><td colSpan="7" className="text-center py-10 text-slate-400 font-medium">Loading records...</td></tr>
                ) : dashboardData.currentRegistrations.length === 0 ? (
                  <tr><td colSpan="7" className="text-center py-10 text-slate-400 font-medium">No registrations found for today.</td></tr>
                ) : (
                  dashboardData.currentRegistrations.map((row, idx) => (
                    <tr key={idx} className="hover:bg-[#f8f9fc] transition">
                      <td className="px-6 py-4 font-bold text-[#2563eb]">{row.id}</td>
                      <td className="px-6 py-4 flex items-center gap-3">
                        <span className="text-[10px] font-bold bg-[#f4f7fe] text-slate-500 w-7 h-7 rounded flex items-center justify-center">{row.ini}</span>
                        <span className="font-bold text-slate-800">{row.name}</span>
                      </td>
                      <td className="px-6 py-4 font-medium">{row.age}</td>
                      <td className="px-6 py-4 font-medium">{row.doc}</td>
                      <td className="px-6 py-4 font-medium">{row.case}</td>
                      <td className="px-6 py-4"><span className={`text-[10px] font-bold uppercase tracking-wider ${row.color}`}>{row.status}</span></td>
                      <td className="px-6 py-4 text-right">
                        <button className="text-slate-300 font-bold hover:text-[#2563eb] transition px-2">⋮</button>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>

      </div>

      <RegisterPatientModal 
        isOpen={isRegisterModalOpen} 
        onClose={() => setIsRegisterModalOpen(false)} 
        onSuccess={() => {
          setIsRegisterModalOpen(false);
          fetchDashboardData();
        }} 
      />
      
      <NewAppointmentModal 
        isOpen={isAppointmentModalOpen} 
        onClose={() => setIsAppointmentModalOpen(false)} 
        onSuccess={() => {
          setIsAppointmentModalOpen(false);
          fetchDashboardData();
        }} 
      />
    </StaffLayout>
  );
};
export default StaffDashboard;
