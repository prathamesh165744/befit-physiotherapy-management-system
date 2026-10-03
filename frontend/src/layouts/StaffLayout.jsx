import React, { useState } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';

export default function StaffLayout({ children }) {
  const [isProfileMenuOpen, setIsProfileMenuOpen] = useState(false);
  const [showProfileModal, setShowProfileModal] = useState(false);
  
  const navigate = useNavigate();
  const location = useLocation();
  
  const role = localStorage.getItem('role') || 'RECEPTIONIST';
  const fullName = localStorage.getItem('fullName') || 'Sarah Jenkins';
  const email = localStorage.getItem('email') || 'reception@befit.com';
  
  const handleLogout = () => {
    localStorage.clear();
    navigate('/');
  };

  const menuItems = [
    { name: 'Dashboard', path: '/staff-dashboard', icon: <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.75" d="M4 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2V6zM14 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2V6zM4 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2v-2zM14 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2v-2z"></path></svg> },
    { name: 'Patients', path: '/staff-dashboard/patients', icon: <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.75" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z"></path></svg> },
    { name: 'Appointments', path: '/staff-dashboard/appointments', icon: <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.75" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"></path></svg> },
    { name: 'Billing', path: '/staff-dashboard/billing', icon: <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.75" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"></path></svg> },
    { name: 'History', path: '/staff-dashboard/history', icon: <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.75" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg> }
  ];

  return (
    <div className="flex h-screen bg-[#f8f9fc] font-sans text-slate-800">
      
      {/* --- Sidebar --- */}
      <aside className="w-[260px] bg-white border-r border-slate-200 flex flex-col justify-between relative z-50">
        <div>
          <div className="h-16 flex items-center px-6 border-b border-slate-100">
            <div className="text-blue-600 font-bold text-xl flex items-center space-x-2">
               <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg" className="w-7 h-7">
                  <path d="M12 2C10.8954 2 10 2.89543 10 4C10 5.10457 10.8954 6 12 6C13.1046 6 14 5.10457 14 4C14 2.89543 13.1046 2 12 2Z" fill="currentColor"/>
                  <path d="M14.8284 10.1716C13.7916 9.13474 12.5 8 10 8C7.79086 8 6 9.79086 6 12V14H8V12C8 10.8954 8.89543 10 10 10C11.5 10 12.3787 10.8787 13.4142 11.9142L14.8284 13.3284C15.8653 14.3653 17.1569 15.5 19.6569 15.5V13.5C17.866 13.5 16.9873 12.6213 15.9518 11.5858L14.8284 10.1716Z" fill="currentColor"/>
                  <path d="M8 22V16H10V22H8Z" fill="currentColor"/>
                  <path d="M14 22V16H16V22H14Z" fill="currentColor"/>
               </svg>
              <span>BeFit</span>
            </div>
          </div>
          
          <nav className="p-4 space-y-2 mt-2">
            {menuItems.map((item) => {
              const isActive = location.pathname === item.path || (location.pathname === '/staff-dashboard/' && item.path === '/staff-dashboard');
              return (
                <Link 
                  key={item.name} 
                  to={item.path} 
                  className={`flex items-center space-x-4 px-4 py-3 rounded-xl transition-all ${
                    isActive 
                      ? 'bg-[#f4f7fe] text-[#2563eb] font-semibold' 
                      : 'text-[#64748b] hover:bg-slate-50 hover:text-slate-900 font-medium'
                  }`}
                >
                  <div className={isActive ? 'text-[#2563eb]' : 'text-[#64748b]'}>
                    {item.icon}
                  </div>
                  <span className="text-[15px] tracking-wide">{item.name}</span>
                </Link>
              );
            })}
          </nav>
        </div>
        
        <div className="relative p-4 border-t border-slate-100">
          
          {isProfileMenuOpen && (
            <div className="absolute bottom-full mb-2 left-4 w-[220px] bg-white rounded-xl shadow-xl border border-slate-200 overflow-hidden z-50">
              <div className="px-4 py-3 bg-slate-50 border-b border-slate-100">
                <p className="text-sm font-bold text-slate-800 truncate">{fullName}</p>
                <p className="text-xs text-slate-500 truncate">{email}</p>
              </div>
              <button 
                onClick={() => { setShowProfileModal(true); setIsProfileMenuOpen(false); }}
                className="w-full flex items-center gap-2 px-4 py-2.5 text-sm text-slate-700 hover:bg-blue-50 hover:text-blue-700 transition"
              >
                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"></path></svg>
                My Profile Info
              </button>
              <div className="border-t border-slate-100"></div>
              <button onClick={handleLogout} className="w-full flex items-center gap-2 px-4 py-2.5 text-sm text-red-600 hover:bg-red-50 transition font-medium">
                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1"></path></svg>
                Sign out
              </button>
            </div>
          )}

          <button 
            onClick={() => setIsProfileMenuOpen(!isProfileMenuOpen)}
            className="w-full flex items-center space-x-3 hover:bg-slate-50 p-2 rounded-xl transition text-left focus:outline-none"
          >
            <img src="https://i.pravatar.cc/150?img=47" alt="Profile" className="w-10 h-10 rounded-full border border-slate-200" />
            <div>
              <p className="text-sm font-bold text-slate-800 leading-tight">{fullName}</p>
              <p className="text-[10px] text-blue-600 font-bold uppercase tracking-wider">{role.replace('_', ' ')} STAFF</p>
            </div>
          </button>
        </div>
      </aside>

      <div className="flex-1 flex flex-col overflow-hidden relative">
        
        <header className="h-16 bg-white border-b border-slate-200 flex items-center justify-between px-6 z-40">
          <div className="flex items-center space-x-8">
            <div className="font-semibold text-slate-800 flex items-center space-x-2">
              <div className="w-7 h-7 bg-blue-600 text-white rounded-full flex items-center justify-center text-xs">
                <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 24 24"><path d="M12 2L2 22h20L12 2z"/></svg>
              </div>
              <span className="text-sm font-bold">BeFit Physiotherapy</span>
            </div>
            <div className="hidden md:flex space-x-6 text-xs text-slate-500 font-medium">
              <Link to="/staff-dashboard/appointments" className="hover:text-slate-800 cursor-pointer flex items-center gap-1.5"><svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.5" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"></path></svg> Appointments</Link>
              <span className="hover:text-slate-800 cursor-pointer flex items-center gap-1.5"><svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.5" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"></path></svg> Treatment Plans</span>
            </div>
          </div>
          
          <div className="flex items-center space-x-6">
            <div className="relative hidden md:block">
              <svg className="w-4 h-4 absolute left-3 top-2 text-slate-400" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"></path></svg>
              <input type="text" placeholder="Search records..." className="pl-9 pr-4 py-1.5 bg-slate-50 border border-transparent rounded-full text-xs focus:bg-white focus:border-slate-300 outline-none w-64 transition-all" />
            </div>
            
            <button className="relative text-slate-400 hover:text-blue-600 transition p-1" onClick={() => alert("No new notifications")}>
              <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.5" d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9"></path></svg>
              <span className="absolute top-0 right-0 w-2 h-2 bg-red-500 rounded-full border border-white"></span>
            </button>
          </div>
        </header>

        <main className="flex-1 overflow-x-hidden overflow-y-auto p-8 relative z-0">
          {children}
        </main>

        {showProfileModal && (
          <div className="fixed inset-0 bg-slate-900/40 backdrop-blur-sm z-[100] flex items-center justify-center">
            <div className="bg-white rounded-2xl shadow-2xl w-full max-w-sm overflow-hidden transform transition-all">
              <div className="bg-gradient-to-r from-blue-600 to-blue-800 p-6 text-center relative">
                <button onClick={() => setShowProfileModal(false)} className="absolute top-4 right-4 text-white/70 hover:text-white transition">
                  <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12"></path></svg>
                </button>
                <img src="https://i.pravatar.cc/150?img=47" alt="Profile" className="w-24 h-24 rounded-full mx-auto border-4 border-white shadow-lg mb-3 object-cover" />
                <h2 className="text-xl font-bold text-white">{fullName}</h2>
                <p className="text-blue-100 text-xs font-bold uppercase tracking-wider mt-1">{role.replace('_', ' ')} STAFF</p>
              </div>
              <div className="p-6 space-y-5">
                <div>
                  <label className="text-[10px] font-bold text-slate-400 uppercase tracking-wider">Email Address</label>
                  <p className="text-slate-800 font-medium">{email}</p>
                </div>
                <div>
                  <label className="text-[10px] font-bold text-slate-400 uppercase tracking-wider">System Status</label>
                  <p className="text-emerald-600 font-bold flex items-center gap-1.5 text-sm mt-1">
                    <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse"></span> Active Session
                  </p>
                </div>
                <div>
                  <label className="text-[10px] font-bold text-slate-400 uppercase tracking-wider">Role Permissions</label>
                  <div className="flex flex-wrap gap-2 mt-2">
                    <span className="bg-slate-100 text-slate-600 text-[10px] uppercase font-bold px-2 py-1 rounded">Read Access</span>
                    <span className="bg-slate-100 text-slate-600 text-[10px] uppercase font-bold px-2 py-1 rounded">Manage Appointments</span>
                    {role === 'ADMIN' && <span className="bg-red-50 text-red-600 text-[10px] uppercase font-bold px-2 py-1 rounded">System Config</span>}
                  </div>
                </div>
              </div>
              <div className="bg-slate-50 p-4 border-t border-slate-100 flex justify-end">
                <button onClick={() => setShowProfileModal(false)} className="bg-slate-200 hover:bg-slate-300 text-slate-700 px-5 py-2 rounded-lg text-sm font-bold transition">Close</button>
              </div>
            </div>
          </div>
        )}

      </div>
    </div>
  );
}