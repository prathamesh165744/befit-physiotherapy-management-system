import React from 'react';
import {
  Calendar,
  ChevronLeft,
  ChevronRight,
  CalendarPlus,
  PlayCircle,
  FileText,
  Video,
  MessageSquare,
  ArrowUpRight,
  Send,
  MapPin,
  CheckCircle2,
} from 'lucide-react';

/* Page content only. Sidebar, header, hamburger and logout live in PatientLayout. */
const PatientDashboard = () => {
  const userName = localStorage.getItem('fullName') || 'Jane';

  const quickActions = [
    { title: 'Book Session', desc: 'Schedule next visit', icon: CalendarPlus, color: 'text-blue-600', bg: 'bg-blue-50' },
    { title: 'View Exercises', desc: 'Watch videos', icon: PlayCircle, color: 'text-green-600', bg: 'bg-green-50' },
    { title: 'My Reports', desc: 'Check progress', icon: FileText, color: 'text-purple-600', bg: 'bg-purple-50' },
    { title: 'Video Call', desc: 'Telehealth consult', icon: Video, color: 'text-orange-600', bg: 'bg-orange-50' },
  ];

  const treatmentHistory = [
    { date: 'Oct 12, 2023', title: 'Lower Back Lumbar Therapy', doc: 'Dr. Sarah Mitchell' },
    { date: 'Oct 05, 2023', title: 'Sciatica Consultation', doc: 'Dr. Sarah Mitchell' },
    { date: 'Sep 28, 2023', title: 'Post-Surgical Assessment', doc: 'Dr. Sarah Mitchell' },
    { date: 'Sep 21, 2023', title: 'Initial Evaluation', doc: 'Dr. Marcus Chen' },
  ];

  return (
    <div className="p-4 sm:p-6 lg:p-8 pb-10">
      <div className="max-w-[1200px] mx-auto space-y-5 sm:space-y-6">
            {/* =================================================
                WELCOME
            ================================================== */}
            <div className="flex flex-col lg:flex-row lg:items-end lg:justify-between gap-5">
              <div className="min-w-0">
                <h1 className="text-2xl sm:text-3xl font-extrabold text-slate-800 tracking-tight mb-2">
                  Welcome back, {userName} 👋
                </h1>

                <p className="text-slate-500 text-sm leading-relaxed max-w-2xl">
                  You're doing great! You've completed{' '}
                  <span className="font-bold text-blue-600">85%</span> of your recovery
                  milestones this month.
                </p>
              </div>

              {/* Buttons */}
              <div className="flex flex-col sm:flex-row gap-2 sm:gap-3 w-full lg:w-auto">
                <button className="flex-1 lg:flex-none justify-center flex items-center gap-2 bg-white border border-slate-200 text-slate-700 px-4 sm:px-5 py-2.5 rounded-xl text-sm font-bold shadow-sm hover:bg-slate-50 transition">
                  <Calendar size={18} />
                  View Calendar
                </button>

                <button className="flex-1 lg:flex-none justify-center flex items-center gap-2 bg-blue-600 text-white px-4 sm:px-5 py-2.5 rounded-xl text-sm font-bold shadow-sm hover:bg-blue-700 transition">
                  <CalendarPlus size={18} />
                  Book Session
                </button>
              </div>
            </div>

            {/* =================================================
                HERO
            ================================================== */}
            <div className="relative w-full min-h-[260px] sm:h-[240px] rounded-2xl sm:rounded-3xl overflow-hidden bg-slate-800 shadow-sm">
              <img
                src="https://images.unsplash.com/photo-1576091160399-112ba8d25d1d?auto=format&fit=crop&q=80&w=2000"
                alt="Physiotherapy session"
                className="absolute inset-0 w-full h-full object-cover opacity-60"
              />

              <div className="absolute inset-0 bg-gradient-to-r from-slate-900/90 via-slate-900/60 to-transparent" />

              <div className="relative h-full min-h-[260px] sm:min-h-0 p-6 sm:p-8 lg:p-10 flex flex-col justify-center">
                <h2 className="text-2xl sm:text-3xl lg:text-4xl font-extrabold text-white mb-3 max-w-xl">
                  Move better, feel stronger
                </h2>

                <p className="text-slate-200 text-sm mb-5 sm:mb-6 max-w-md leading-relaxed">
                  Personalized recovery plans designed for your lifestyle.
                </p>

                <button className="bg-blue-600 text-white w-max px-5 sm:px-6 py-2.5 rounded-xl text-sm font-bold shadow-lg hover:bg-blue-500 transition">
                  View Program
                </button>
              </div>

              {/* Slider Controls */}
              <button className="absolute left-3 sm:left-4 top-1/2 -translate-y-1/2 w-9 h-9 sm:w-10 sm:h-10 bg-white/20 backdrop-blur-md rounded-full flex items-center justify-center text-white hover:bg-white/30 transition">
                <ChevronLeft size={21} />
              </button>

              <button className="absolute right-3 sm:right-4 top-1/2 -translate-y-1/2 w-9 h-9 sm:w-10 sm:h-10 bg-white/20 backdrop-blur-md rounded-full flex items-center justify-center text-white hover:bg-white/30 transition">
                <ChevronRight size={21} />
              </button>

              <div className="absolute bottom-5 left-1/2 -translate-x-1/2 flex gap-2">
                <div className="w-6 h-1.5 bg-white rounded-full" />
                <div className="w-1.5 h-1.5 bg-white/50 rounded-full" />
                <div className="w-1.5 h-1.5 bg-white/50 rounded-full" />
              </div>
            </div>

            {/* =================================================
                QUICK ACTIONS
            ================================================== */}
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3 sm:gap-4">
              {quickActions.map((item, idx) => (
                <div
                  key={idx}
                  className="bg-white p-4 sm:p-5 rounded-2xl border border-slate-100 shadow-sm flex items-center gap-4 cursor-pointer hover:border-blue-200 transition group"
                >
                  <div
                    className={`w-11 h-11 sm:w-12 sm:h-12 rounded-xl flex items-center justify-center ${item.bg} ${item.color} group-hover:scale-110 transition-transform shrink-0`}
                  >
                    <item.icon size={23} />
                  </div>

                  <div className="min-w-0">
                    <h3 className="text-sm font-bold text-slate-800 truncate">{item.title}</h3>
                    <p className="text-xs text-slate-500 truncate">{item.desc}</p>
                  </div>
                </div>
              ))}
            </div>

            {/* =================================================
                MIDDLE WIDGETS
            ================================================== */}
            <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4 sm:gap-6">
              {/* NEXT APPOINTMENT */}
              <div className="bg-white p-5 sm:p-6 rounded-2xl sm:rounded-3xl border border-slate-100 shadow-sm flex flex-col relative overflow-hidden">
                <div className="flex justify-between items-start mb-5 sm:mb-6">
                  <span className="bg-green-100 text-green-700 text-xs font-bold px-3 py-1 rounded-full">
                    Confirmed
                  </span>

                  <button className="text-slate-400 hover:text-slate-600">
                    <Calendar size={20} />
                  </button>
                </div>

                <h3 className="text-lg font-bold text-slate-800 mb-5 sm:mb-6">
                  Next Appointment
                </h3>

                <div className="flex gap-3 sm:gap-4 mb-6 sm:mb-8">
                  <div className="bg-blue-600 text-white rounded-xl w-14 h-14 flex flex-col items-center justify-center shrink-0 shadow-md">
                    <span className="text-[10px] font-bold uppercase tracking-wider opacity-80">
                      Oct
                    </span>
                    <span className="text-xl font-black">18</span>
                  </div>

                  <div className="min-w-0">
                    <h4 className="text-sm sm:text-base font-bold text-slate-800">
                      Wednesday, 10:30 AM
                    </h4>
                    <p className="text-sm text-slate-500 mt-0.5 leading-relaxed">
                      Spinal Rehabilitation Session
                    </p>
                  </div>
                </div>

                <div className="flex items-start gap-2 text-slate-500 text-sm font-medium mb-6">
                  <MapPin size={16} className="shrink-0 mt-0.5" />
                  <span>Karve Road Branch • Room 302</span>
                </div>

                <div className="mt-auto pt-4 border-t border-slate-100">
                  <button className="text-blue-600 font-bold text-sm flex items-center gap-1 hover:text-blue-800 transition">
                    Add to calendar
                    <ChevronRight size={16} />
                  </button>
                </div>
              </div>

              {/* THERAPIST */}
              <div className="bg-white p-5 sm:p-6 rounded-2xl sm:rounded-3xl border border-slate-100 shadow-sm relative">
                <div className="flex justify-between items-start mb-5 sm:mb-6">
                  <span className="text-[10px] font-bold text-blue-600 uppercase tracking-wider">
                    Primary Provider
                  </span>

                  <button className="text-slate-400 hover:text-slate-600">
                    <MessageSquare size={18} />
                  </button>
                </div>

                <h3 className="text-lg font-bold text-slate-800 mb-5 sm:mb-6">Your Therapist</h3>

                <div className="flex items-center gap-3 sm:gap-4 mb-6 sm:mb-8">
                  <div className="relative shrink-0">
                    <img
                      src="https://i.pravatar.cc/150?img=32"
                      alt="Doctor"
                      className="w-14 h-14 sm:w-16 sm:h-16 rounded-full object-cover border-2 border-white shadow-sm"
                    />
                    <div className="absolute bottom-0 right-0 w-4 h-4 bg-green-500 border-2 border-white rounded-full" />
                  </div>

                  <div className="min-w-0">
                    <h4 className="text-sm sm:text-base font-bold text-slate-800 truncate">
                      Dr. Sarah Mitchell
                    </h4>
                    <p className="text-sm text-slate-500 truncate">Senior Physiotherapist</p>

                    <div className="flex items-center gap-1 mt-1 text-green-600 text-xs font-semibold">
                      <CheckCircle2 size={12} />
                      12 Sessions Completed
                    </div>
                  </div>
                </div>

                <div className="flex gap-3 mt-auto">
                  <button className="flex-1 bg-blue-600 text-white py-2.5 rounded-xl text-sm font-bold hover:bg-blue-700 transition shadow-sm">
                    Message
                  </button>

                  <button className="flex-1 bg-white border border-slate-200 text-slate-700 py-2.5 rounded-xl text-sm font-bold hover:bg-slate-50 transition shadow-sm">
                    Profile
                  </button>
                </div>
              </div>

              {/* PROGRESS */}
              <div className="bg-white p-5 sm:p-6 rounded-2xl sm:rounded-3xl border border-slate-100 shadow-sm relative md:col-span-2 xl:col-span-1">
                <div className="flex justify-between items-start mb-2">
                  <span className="text-[10px] font-bold text-green-600 uppercase tracking-wider">
                    Overall Goal
                  </span>

                  <button className="text-green-500 bg-green-50 p-1.5 rounded-lg">
                    <ArrowUpRight size={16} />
                  </button>
                </div>

                <h3 className="text-lg font-bold text-slate-800 mb-5 sm:mb-6">
                  Recovery Progress
                </h3>

                <div className="flex justify-center mb-6 sm:mb-8">
                  <div className="relative w-28 h-28 sm:w-32 sm:h-32">
                    <svg className="w-full h-full transform -rotate-90" viewBox="0 0 36 36">
                      <path
                        d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"
                        fill="none"
                        stroke="#f1f5f9"
                        strokeWidth="4"
                      />
                      <path
                        d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"
                        fill="none"
                        stroke="#2563eb"
                        strokeWidth="4"
                        strokeDasharray="70, 100"
                      />
                    </svg>

                    <div className="absolute inset-0 flex flex-col items-center justify-center">
                      <span className="text-2xl font-black text-slate-800">70%</span>
                      <span className="text-[9px] font-bold text-slate-400 uppercase tracking-wider">
                        Done
                      </span>
                    </div>
                  </div>
                </div>

                <div className="space-y-4">
                  <div>
                    <div className="flex justify-between text-xs font-bold text-slate-700 mb-1.5">
                      <span>Mobility</span>
                      <span className="text-blue-600">72%</span>
                    </div>
                    <div className="w-full bg-slate-100 rounded-full h-1.5">
                      <div className="bg-blue-600 h-1.5 rounded-full w-[72%]" />
                    </div>
                  </div>

                  <div>
                    <div className="flex justify-between text-xs font-bold text-slate-700 mb-1.5">
                      <span>Strength</span>
                      <span className="text-blue-600">48%</span>
                    </div>
                    <div className="w-full bg-slate-100 rounded-full h-1.5">
                      <div className="bg-blue-600 h-1.5 rounded-full w-[48%]" />
                    </div>
                  </div>
                </div>
              </div>
            </div>

            {/* =================================================
                BOTTOM WIDGETS
            ================================================== */}
            <div className="grid grid-cols-1 xl:grid-cols-3 gap-4 sm:gap-6">
              {/* TREATMENT HISTORY */}
              <div className="xl:col-span-2 bg-white p-5 sm:p-6 rounded-2xl sm:rounded-3xl border border-slate-100 shadow-sm">
                <div className="flex flex-col sm:flex-row sm:justify-between sm:items-center gap-4 mb-6">
                  <div>
                    <h3 className="text-lg font-bold text-slate-800">My Treatment History</h3>
                    <p className="text-sm text-slate-500 mt-0.5">
                      Timeline of your past clinic sessions
                    </p>
                  </div>

                  <button className="w-full sm:w-auto justify-center flex items-center gap-1.5 text-sm font-bold text-blue-600 bg-blue-50 px-4 py-2 rounded-xl hover:bg-blue-100 transition">
                    Export Report
                    <ArrowUpRight size={16} />
                  </button>
                </div>

                <div className="space-y-5 sm:space-y-6 relative before:absolute before:inset-0 before:ml-2 before:-translate-x-px before:h-full before:w-0.5 before:bg-gradient-to-b before:from-transparent before:via-slate-200 before:to-transparent pt-2">
                  {treatmentHistory.map((history, i) => (
                    <div
                      key={i}
                      className="relative flex items-start gap-4 pl-8 pb-5 border-b border-slate-50 last:border-0 last:pb-0"
                    >
                      <div className="absolute left-0 top-1 w-4 h-4 rounded-full border-[3px] border-blue-600 bg-white shadow-sm z-10" />

                      <div className="flex-1 min-w-0">
                        <p className="text-[11px] font-bold text-blue-600 uppercase tracking-wider mb-1">
                          {history.date}
                        </p>

                        <h4 className="text-sm font-bold text-slate-800 leading-relaxed">
                          {history.title}
                        </h4>

                        <p className="text-xs text-slate-500 mt-1 flex items-center gap-1 flex-wrap">
                          <span className="font-bold text-slate-400 text-[10px] bg-slate-100 px-1.5 py-0.5 rounded">
                            DPT
                          </span>
                          {history.doc}
                        </p>
                      </div>

                      <button className="hidden sm:block shrink-0 bg-white border border-slate-200 text-slate-600 px-3 sm:px-4 py-1.5 rounded-lg text-xs font-bold hover:bg-slate-50 transition shadow-sm">
                        View Summary
                      </button>
                    </div>
                  ))}
                </div>
              </div>

              {/* AI ASSISTANT */}
              <div className="xl:col-span-1 bg-white p-5 sm:p-6 rounded-2xl sm:rounded-3xl border border-slate-100 shadow-sm flex flex-col min-h-[400px]">
                <div className="flex items-center gap-3 mb-5 sm:mb-6">
                  <div className="w-10 h-10 bg-blue-50 text-blue-600 rounded-xl flex items-center justify-center shrink-0">
                    <MessageSquare size={20} />
                  </div>

                  <div className="min-w-0">
                    <h3 className="text-sm font-bold text-slate-800">BeFit AI Assistant</h3>
                    <p className="text-[11px] text-slate-500 truncate">
                      Ask me anything about your recovery
                    </p>
                  </div>
                </div>

                <div className="flex-1 bg-slate-50 rounded-2xl p-3 sm:p-4 flex flex-col min-h-[300px]">
                  {/* Chat */}
                  <div className="flex gap-3 mb-6">
                    <div className="w-6 h-6 bg-blue-100 text-blue-600 rounded-full flex items-center justify-center shrink-0 mt-1">
                      <div className="w-3 h-3 bg-blue-600 rounded-sm" />
                    </div>

                    <div className="bg-white border border-slate-200 p-3 rounded-2xl rounded-tl-none shadow-sm text-sm text-slate-700 leading-relaxed">
                      Hello {userName}! I'm your recovery assistant. How can I help you today?
                    </div>
                  </div>

                  {/* Suggestions */}
                  <div className="mt-auto mb-4 flex flex-wrap gap-2">
                    <span className="bg-white border border-slate-200 text-slate-600 text-[11px] font-semibold px-3 py-1.5 rounded-full cursor-pointer hover:bg-slate-100">
                      Next session prep?
                    </span>

                    <span className="bg-white border border-slate-200 text-slate-600 text-[11px] font-semibold px-3 py-1.5 rounded-full cursor-pointer hover:bg-slate-100">
                      Pain management tips
                    </span>

                    <span className="bg-white border border-slate-200 text-slate-600 text-[11px] font-semibold px-3 py-1.5 rounded-full cursor-pointer hover:bg-slate-100">
                      View my exercises
                    </span>
                  </div>

                  {/* Input */}
                  <div className="relative">
                    <input
                      type="text"
                      placeholder="Type your question..."
                      className="w-full bg-white border border-slate-200 rounded-xl pl-4 pr-12 py-3 text-sm outline-none focus:border-blue-500 shadow-sm"
                    />

                    <button className="absolute right-2 top-1/2 -translate-y-1/2 w-8 h-8 bg-blue-600 text-white rounded-lg flex items-center justify-center hover:bg-blue-700 transition shadow-sm">
                      <Send size={14} />
                    </button>
                  </div>
                </div>
              </div>
            </div>

            {/* =================================================
                FOOTER
            ================================================== */}
            <div className="text-center pt-6 sm:pt-8 pb-4">
              <div className="flex items-center justify-center gap-2 mb-3">
                <div className="w-4 h-4 bg-blue-200 rounded text-blue-700 flex items-center justify-center text-[10px] font-bold">
                  B
                </div>

                <span className="text-xs font-bold text-slate-400 tracking-widest uppercase">
                  BeFit Physiotherapy Clinic
                </span>
              </div>

              <p className="text-[10px] text-slate-400 max-w-lg mx-auto leading-relaxed mb-4 px-4">
                Your medical data is encrypted and managed according to HIPAA guidelines. For
                emergencies, please call 911 or visit your nearest emergency department.
              </p>

              <div className="flex flex-wrap justify-center gap-x-5 gap-y-2 text-[11px] font-bold text-slate-400 px-4">
                <a href="#" className="hover:text-slate-600">
                  Privacy Policy
                </a>

                <a href="#" className="hover:text-slate-600">
                  Terms of Service
                </a>

                <a href="#" className="hover:text-slate-600">
                  Support Center
                </a>
              </div>

              <p className="text-[10px] text-slate-300 mt-4">
                © 2026 BeFit Care Portal. All rights reserved.
              </p>
            </div>
          </div>
    </div>
  );
};

export default PatientDashboard;
