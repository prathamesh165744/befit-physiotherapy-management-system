import React, { useState, useEffect, useRef } from 'react';
import { Outlet, useNavigate, NavLink } from 'react-router-dom';
import {
  LayoutDashboard,
  User,
  ChevronDown,
  Menu,
  X,
  LogOut,
} from 'lucide-react';

/* The patient portal has two real pages. */
const navItems = [
  { label: 'Dashboard', icon: LayoutDashboard, to: '/dashboard', end: true },
  { label: 'My Profile', icon: User, to: '/dashboard/profile', end: false },
];

const linkClass = ({ isActive }) =>
  `flex items-center gap-3 px-4 py-3 rounded-xl font-semibold transition ${
    isActive
      ? 'bg-blue-50 text-blue-700'
      : 'text-slate-500 hover:bg-slate-50 hover:text-slate-700'
  }`;

/* One sidebar, reused for desktop and the mobile drawer */
const SidebarContent = ({ onNavigate, onClose }) => (
  <div className="flex flex-col justify-between h-full overflow-y-auto">
    <div>
      {/* Logo */}
      <div className="h-20 flex items-center justify-between gap-3 px-5 lg:px-6 border-b border-slate-100">
        <div className="flex items-center gap-3 min-w-0">
          <div className="w-8 h-8 bg-blue-600 rounded-lg flex items-center justify-center text-white font-bold text-xl shrink-0">
            B
          </div>
          <span className="text-lg lg:text-xl font-bold text-slate-800 tracking-tight whitespace-nowrap">
            BeFit Physiotherapy
          </span>
        </div>

        {onClose && (
          <button
            onClick={onClose}
            aria-label="Close menu"
            className="w-9 h-9 shrink-0 rounded-lg flex items-center justify-center text-slate-500 hover:bg-slate-100"
          >
            <X size={22} />
          </button>
        )}
      </div>

      {/* Sidebar links */}
      <nav className="p-4 space-y-2 mt-4">
        {navItems.map(({ label, icon: Icon, to, end }) => (
          <NavLink key={label} to={to} end={end} onClick={onNavigate} className={linkClass}>
            <Icon size={20} />
            <span>{label}</span>
          </NavLink>
        ))}
      </nav>
    </div>

    {/* Support */}
    <div className="p-5 lg:p-6">
      <div className="bg-blue-50 rounded-2xl p-5 border border-blue-100 text-center">
        <h4 className="text-sm font-bold text-slate-800 mb-2">Need assistance?</h4>
        <p className="text-xs text-slate-500 leading-relaxed">
          Please contact your BeFit branch for bookings and any changes to your appointments.
        </p>
      </div>
    </div>
  </div>
);

export default function PatientLayout() {
  const navigate = useNavigate();
  const userName = localStorage.getItem('fullName') || 'Patient';

  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const [profileOpen, setProfileOpen] = useState(false);
  const profileRef = useRef(null);

  // Close profile dropdown on outside click
  useEffect(() => {
    const onDown = (e) => {
      if (profileRef.current && !profileRef.current.contains(e.target)) {
        setProfileOpen(false);
      }
    };
    document.addEventListener('mousedown', onDown);
    return () => document.removeEventListener('mousedown', onDown);
  }, []);

  // Drawer: lock page scroll, Escape closes, closes when screen grows to desktop
  useEffect(() => {
    if (!mobileMenuOpen) return;

    const prev = document.body.style.overflow;
    document.body.style.overflow = 'hidden';

    const onKey = (e) => e.key === 'Escape' && setMobileMenuOpen(false);
    const onResize = () => window.innerWidth >= 1024 && setMobileMenuOpen(false);

    window.addEventListener('keydown', onKey);
    window.addEventListener('resize', onResize);
    return () => {
      document.body.style.overflow = prev;
      window.removeEventListener('keydown', onKey);
      window.removeEventListener('resize', onResize);
    };
  }, [mobileMenuOpen]);

  const handleLogout = () => {
    localStorage.clear();   // token, role, name, branch - nothing of this patient stays in the browser
    navigate('/login', { replace: true });
  };

  return (
    <div className="flex min-h-screen bg-[#f8f9fc] font-sans overflow-x-hidden">
      {/* DESKTOP SIDEBAR — only from 1024px up */}
      <aside className="hidden lg:block w-[260px] shrink-0 bg-white border-r border-slate-200 sticky top-0 h-screen">
        <SidebarContent />
      </aside>

      {/* MOBILE DRAWER — below 1024px */}
      <div
        className={`fixed inset-0 z-50 lg:hidden ${mobileMenuOpen ? '' : 'pointer-events-none'}`}
        aria-hidden={!mobileMenuOpen}
      >
        <div
          onClick={() => setMobileMenuOpen(false)}
          className={`absolute inset-0 bg-black/40 transition-opacity duration-300 ${
            mobileMenuOpen ? 'opacity-100' : 'opacity-0'
          }`}
        />
        <aside
          className={`absolute left-0 top-0 bottom-0 w-[300px] max-w-[85vw] bg-white shadow-2xl transition-transform duration-300 ${
            mobileMenuOpen ? 'translate-x-0' : '-translate-x-full'
          }`}
        >
          <SidebarContent
            onNavigate={() => setMobileMenuOpen(false)}
            onClose={() => setMobileMenuOpen(false)}
          />
        </aside>
      </div>

      {/* MAIN */}
      <main className="flex-1 min-w-0 flex flex-col min-h-screen">
        <header className="min-h-20 bg-white border-b border-slate-200 flex items-center justify-between px-4 sm:px-6 lg:px-8 py-3 sticky top-0 z-40">
          {/* Left: hamburger + horizontal links */}
          <div className="flex items-center gap-3">
            <button
              onClick={() => setMobileMenuOpen(true)}
              aria-label="Open menu"
              className="lg:hidden w-10 h-10 rounded-xl bg-slate-100 flex items-center justify-center text-slate-600 hover:bg-slate-200 transition"
            >
              <Menu size={22} />
            </button>

            <p className="text-sm font-bold text-slate-800">BeFit Patient Portal</p>
          </div>

          {/* Right */}
          <div className="flex items-center gap-3 sm:gap-5">
            {/* Profile dropdown with Logout */}
            <div ref={profileRef} className="relative sm:border-l sm:border-slate-200 sm:pl-5">
              <button
                onClick={() => setProfileOpen((o) => !o)}
                aria-haspopup="menu"
                aria-expanded={profileOpen}
                className="flex items-center gap-2 sm:gap-3"
              >
                <div className="w-9 h-9 bg-slate-100 rounded-full flex items-center justify-center shrink-0">
                  <User size={20} className="text-slate-600" />
                </div>

                <div className="hidden sm:block text-sm text-left">
                  <p className="font-bold text-slate-800">{userName}</p>
                  <p className="text-xs text-slate-500">Patient</p>
                </div>

                <ChevronDown
                  size={16}
                  className={`hidden sm:block text-slate-400 transition-transform ${
                    profileOpen ? 'rotate-180' : ''
                  }`}
                />
              </button>

              {profileOpen && (
                <div
                  role="menu"
                  className="absolute right-0 top-full mt-3 w-56 bg-white rounded-2xl border border-slate-100 shadow-xl py-2 z-50"
                >
                  <div className="px-4 py-3 border-b border-slate-100">
                    <p className="text-sm font-bold text-slate-800 truncate">{userName}</p>
                    <p className="text-xs text-slate-500">Patient</p>
                  </div>

                  <button
                    role="menuitem"
                    onClick={() => { setProfileOpen(false); navigate('/dashboard/profile'); }}
                    className="w-full flex items-center gap-3 px-4 py-2.5 text-sm font-semibold text-slate-600 hover:bg-slate-50 transition"
                  >
                    <User size={18} />
                    My Profile
                  </button>

                  <button
                    role="menuitem"
                    onClick={handleLogout}
                    className="w-full flex items-center gap-3 px-4 py-2.5 text-sm font-semibold text-red-600 hover:bg-red-50 transition"
                  >
                    <LogOut size={18} />
                    Logout
                  </button>
                </div>
              )}
            </div>
          </div>
        </header>

        {/* Page content is rendered here */}
        <div className="flex-1">
          <Outlet />
        </div>
      </main>
    </div>
  );
}