import React, { useState, useEffect, useRef } from 'react';
import { Outlet, useNavigate } from 'react-router-dom';
import {
  Calendar,
  Clock,
  HelpCircle,
  Search,
  Bell,
  User,
  FileText,
  ChevronDown,
  Menu,
  X,
  LogOut,
} from 'lucide-react';

/* Links that live in the sidebar on desktop */
const sidebarItems = [
  { label: 'Appointments', icon: Calendar, active: true },
  { label: 'Treatment History', icon: Clock },
  { label: 'Help Assistant', icon: HelpCircle },
];

/* Links that are horizontal in the header on tablet/desktop.
   On small screens they move into the drawer and stack vertically. */
const headerItems = [
  { label: 'Appointments', icon: Calendar },
  { label: 'Treatment Plans', icon: FileText },
];

const linkClass = (active) =>
  `flex items-center gap-3 px-4 py-3 rounded-xl font-semibold transition ${
    active
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
        {sidebarItems.map(({ label, icon: Icon, active }) => (
          <a key={label} href="#" onClick={onNavigate} className={linkClass(active)}>
            <Icon size={20} />
            <span>{label}</span>
          </a>
        ))}

        {/* Header links, stacked vertically — drawer only, small screens only */}
        {onClose && (
          <div className="md:hidden pt-2">
            <div className="border-t border-slate-100 mb-2" />
            {headerItems
              .filter((i) => i.label !== 'Appointments') /* already listed above */
              .map(({ label, icon: Icon }) => (
                <a key={label} href="#" onClick={onNavigate} className={linkClass(false)}>
                  <Icon size={20} />
                  <span>{label}</span>
                </a>
              ))}
          </div>
        )}
      </nav>
    </div>

    {/* Support */}
    <div className="p-5 lg:p-6">
      <div className="bg-blue-50 rounded-2xl p-5 border border-blue-100 text-center">
        <h4 className="text-sm font-bold text-slate-800 mb-2">Need Assistance?</h4>
        <p className="text-xs text-slate-500 mb-4 leading-relaxed">
          Our support team is available 24/7 for technical queries.
        </p>
        <button className="w-full bg-white text-blue-600 border border-blue-200 py-2.5 rounded-lg text-sm font-bold shadow-sm hover:bg-blue-50 transition">
          Contact Support
        </button>
      </div>
    </div>
  </div>
);

export default function PatientLayout() {
  const navigate = useNavigate();
  const userName = localStorage.getItem('fullName') || 'Jane';

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
    // change these keys to whatever you store at login
    localStorage.removeItem('token');
    localStorage.removeItem('fullName');
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

            <div className="hidden md:flex gap-6 lg:gap-8 text-sm font-semibold text-slate-600">
              {headerItems.map(({ label, icon: Icon }) => (
                <button key={label} className="flex items-center gap-2 hover:text-blue-600">
                  <Icon size={18} />
                  {label}
                </button>
              ))}
            </div>

            <p className="md:hidden text-sm font-bold text-slate-800">Patient Dashboard</p>
          </div>

          {/* Right */}
          <div className="flex items-center gap-3 sm:gap-5">
            <div className="hidden sm:block relative">
              <Search
                className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400"
                size={18}
              />
              <input
                type="text"
                placeholder="Search records..."
                className="bg-slate-100 text-sm rounded-full pl-10 pr-4 py-2.5 w-48 lg:w-64 outline-none focus:ring-2 focus:ring-blue-500/20 focus:bg-white border border-transparent transition"
              />
            </div>

            <button className="sm:hidden text-slate-500" aria-label="Search">
              <Search size={21} />
            </button>

            <button
              className="relative text-slate-500 hover:text-slate-700 shrink-0"
              aria-label="Notifications"
            >
              <Bell size={21} />
              <span className="absolute -top-0.5 -right-0.5 w-2 h-2 bg-red-500 rounded-full border-2 border-white" />
            </button>

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
                  <p className="font-bold text-slate-800">{userName} Doe</p>
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
                    <p className="text-sm font-bold text-slate-800 truncate">{userName} Doe</p>
                    <p className="text-xs text-slate-500">Patient</p>
                  </div>

                  <button
                    role="menuitem"
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