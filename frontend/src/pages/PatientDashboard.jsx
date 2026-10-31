import React, { useState, useEffect, useCallback } from 'react';
import { Link } from 'react-router-dom';
import {
  Calendar, CalendarPlus, MapPin, User, FileText, ArrowRightLeft, Download, ChevronLeft, ChevronRight,
} from 'lucide-react';
import axios, { errorMessage } from '../api';
import { fmtDate, fmtDateTime } from '../utils/format';
import { splitAppointments, TRANSFER_BADGE, TRANSFER_LABEL, APPOINTMENT_LABEL } from '../utils/patientPortal';

/**
 * Patient home, laid out like the BeFit patient design.
 * Everything shown comes from the logged-in patient's own endpoints (/api/patients/me, /me/history, /me/transfers):
 * the server identifies the patient from the login token, so it works from any branch and after a transfer.
 * Things the design shows that BeFit has no data for yet (exercise videos, reports, video calls, recovery percentages,
 * an AI assistant) are deliberately not faked here.
 */

const SLIDES = [
  { title: 'Move better, feel stronger', text: 'All your BeFit visits, at every branch, in one place.' },
  { title: 'Stay on track', text: 'Keep an eye on your next appointment and your visit history.' },
];

const initials = (name) => (name || '?').split(/\s+/).filter(Boolean).slice(0, 2).map((w) => w[0].toUpperCase()).join('');

const csvCell = (value) => {
  let s = String(value ?? '');
  if (/^[=+\-@]/.test(s)) s = `'${s}`;            // a spreadsheet must never treat a cell as a formula
  return `"${s.replace(/"/g, '""')}"`;
};

function Ring({ value, total }) {
  const r = 42;
  const c = 2 * Math.PI * r;
  const pct = total > 0 ? Math.min(value / total, 1) : 0;
  return (
    <svg viewBox="0 0 100 100" className="w-28 h-28" role="img" aria-label={`${value} of ${total} sessions completed`}>
      <circle cx="50" cy="50" r={r} fill="none" stroke="#e2e8f0" strokeWidth="8" />
      <circle cx="50" cy="50" r={r} fill="none" stroke="#2563eb" strokeWidth="8" strokeLinecap="round"
        strokeDasharray={c} strokeDashoffset={c * (1 - pct)} transform="rotate(-90 50 50)" />
      <text x="50" y="52" textAnchor="middle" className="fill-slate-800" style={{ fontSize: 22, fontWeight: 700 }}>{value}</text>
      <text x="50" y="66" textAnchor="middle" className="fill-slate-400" style={{ fontSize: 8, fontWeight: 700 }}>DONE</text>
    </svg>
  );
}

export default function PatientDashboard() {
  const [state, setState] = useState({ status: 'loading', error: '' });
  const [profile, setProfile] = useState(null);
  const [history, setHistory] = useState(null);
  const [transfers, setTransfers] = useState([]);
  const [slide, setSlide] = useState(0);
  const [showAll, setShowAll] = useState(false);
  const [notice, setNotice] = useState('');

  const load = useCallback(async () => {
    setState({ status: 'loading', error: '' });
    try {
      const [p, h, t] = await Promise.all([
        axios.get('/api/patients/me'),
        axios.get('/api/patients/me/history'),
        axios.get('/api/patients/me/transfers'),
      ]);
      setProfile(p.data); setHistory(h.data); setTransfers(t.data);
      setState({ status: 'ready', error: '' });
    } catch (err) {
      setState({ status: 'error', error: await errorMessage(err, 'We could not load your dashboard. Please try again.') });
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  if (state.status === 'loading') {
    return <div role="status" className="p-10 text-center text-slate-500 font-bold">Loading your dashboard…</div>;
  }
  if (state.status === 'error') {
    return (
      <div role="alert" className="p-10 text-center">
        <p className="text-red-600 font-bold mb-4">{state.error}</p>
        <button onClick={load} className="bg-blue-600 hover:bg-blue-700 text-white px-5 py-2 rounded-lg text-sm font-bold">Try again</button>
      </div>
    );
  }

  const appointments = history?.appointments || [];
  const { upcoming, past, next } = splitAppointments(appointments);
  const completed = past.filter((a) => a.status === 'COMPLETED');
  const latestTransfer = transfers[0] || null;
  const firstName = (profile?.fullName || '').split(' ')[0] || 'there';
  const branchName = profile?.branchName || profile?.branchId || 'your branch';

  const therapist = next?.doctorName || past.find((a) => a.doctorName)?.doctorName || null;
  const therapistSessions = therapist ? completed.filter((a) => a.doctorName === therapist).length : 0;

  const pastShown = showAll ? past : past.slice(0, 5);
  const badge = next ? new Date(next.appointmentDate) : null;

  const goToVisits = () => document.getElementById('my-visits')?.scrollIntoView({ behavior: 'smooth' });

  const exportVisits = () => {
    const rows = [['Date', 'Branch', 'Visit', 'Doctor', 'Status'],
      ...[...upcoming, ...past].map((a) => [fmtDateTime(a.appointmentDate), a.branchName || a.branchId, a.caseType || 'Visit', a.doctorName || '', APPOINTMENT_LABEL[a.status] || a.status])];
    const url = URL.createObjectURL(new Blob([rows.map((r) => r.map(csvCell).join(',')).join('\n')], { type: 'text/csv;charset=utf-8' }));
    const a = document.createElement('a');
    a.href = url; a.download = 'my-visits.csv';
    document.body.appendChild(a); a.click(); a.remove();
    URL.revokeObjectURL(url);
  };

  const card = 'bg-white rounded-2xl border border-slate-100 shadow-sm';

  return (
    <div className="p-4 sm:p-6 lg:p-8 max-w-6xl mx-auto font-sans">
      {/* Welcome */}
      <div className="flex flex-wrap justify-between items-start gap-4 mb-6">
        <div>
          <h1 className="text-3xl font-bold text-slate-900">Welcome back, {firstName} <span aria-hidden="true">👋</span></h1>
          <p className="text-sm text-slate-500 mt-1.5">
            You are registered at <b>{branchName}</b>. Your visits at every branch are listed below.
          </p>
        </div>
        <div className="flex gap-3">
          <button type="button" onClick={goToVisits}
            className="flex items-center gap-2 bg-white border border-slate-200 hover:bg-slate-50 text-slate-700 px-4 py-2.5 rounded-xl text-sm font-bold shadow-sm">
            <Calendar size={16} /> View Calendar
          </button>
          <button type="button" onClick={() => setNotice('Online booking is not available yet. Please contact your BeFit branch to book a session.')}
            className="flex items-center gap-2 bg-blue-600 hover:bg-blue-700 text-white px-4 py-2.5 rounded-xl text-sm font-bold shadow-sm">
            <CalendarPlus size={16} /> Book Session
          </button>
        </div>
      </div>

      {notice && (
        <div role="note" className="mb-6 bg-blue-50 border border-blue-100 text-blue-800 text-sm rounded-xl p-3 flex justify-between gap-3">
          <span>{notice}</span>
          <button type="button" onClick={() => setNotice('')} aria-label="Dismiss" className="font-bold text-blue-700">×</button>
        </div>
      )}

      {/* Transfer status */}
      {profile?.pendingTransfer ? (
        <div data-testid="transfer-banner" className="mb-6 bg-amber-50 border border-amber-200 rounded-xl p-4 flex flex-wrap justify-between items-center gap-3 text-sm text-amber-900">
          <span>Your request to move to <b>{profile.pendingTransfer.toBranchName}</b> is waiting for approval (sent {fmtDate(profile.pendingTransfer.requestedAt)}).</span>
          <Link to="/dashboard/profile" className="font-bold text-amber-800 hover:underline">View or cancel</Link>
        </div>
      ) : latestTransfer && latestTransfer.status !== 'PENDING' ? (
        <div data-testid="transfer-banner" className="mb-6 bg-white border border-slate-100 rounded-xl p-4 flex flex-wrap justify-between items-center gap-3 text-sm text-slate-600 shadow-sm">
          <span>
            Last branch change request: {latestTransfer.fromBranchName} → <b>{latestTransfer.toBranchName}</b>{' '}
            <span className={`ml-1 text-[10px] font-bold px-2 py-0.5 rounded border uppercase ${TRANSFER_BADGE[latestTransfer.status]}`}>{TRANSFER_LABEL[latestTransfer.status]}</span>
            {latestTransfer.decisionNote && <span className="text-slate-500"> · “{latestTransfer.decisionNote}”</span>}
          </span>
          <Link to="/dashboard/profile" className="font-bold text-blue-600 hover:underline">Details</Link>
        </div>
      ) : null}

      {/* Hero banner */}
      <div className="relative overflow-hidden rounded-2xl h-56 sm:h-64 mb-6 shadow-sm bg-gradient-to-br from-slate-800 via-slate-700 to-blue-700 text-white">
        <div className="absolute -right-16 -top-16 w-72 h-72 rounded-full bg-white/10" aria-hidden="true" />
        <div className="absolute right-24 -bottom-24 w-72 h-72 rounded-full bg-blue-400/20" aria-hidden="true" />
        <div className="relative h-full flex flex-col justify-center px-10 sm:px-16">
          <h2 className="text-3xl sm:text-4xl font-extrabold tracking-tight">{SLIDES[slide].title}</h2>
          <p className="mt-2 text-sm sm:text-base text-white/85 max-w-md">{SLIDES[slide].text}</p>
          <button type="button" onClick={goToVisits} className="mt-5 self-start bg-blue-600 hover:bg-blue-500 text-white px-5 py-2.5 rounded-xl text-sm font-bold shadow">View my visits</button>
        </div>
        <button type="button" aria-label="Previous message" onClick={() => setSlide((slide + SLIDES.length - 1) % SLIDES.length)}
          className="absolute left-3 top-1/2 -translate-y-1/2 w-9 h-9 rounded-full bg-white text-slate-700 shadow flex items-center justify-center"><ChevronLeft size={18} /></button>
        <button type="button" aria-label="Next message" onClick={() => setSlide((slide + 1) % SLIDES.length)}
          className="absolute right-3 top-1/2 -translate-y-1/2 w-9 h-9 rounded-full bg-white text-slate-700 shadow flex items-center justify-center"><ChevronRight size={18} /></button>
        <div className="absolute bottom-3 left-1/2 -translate-x-1/2 flex gap-1.5" aria-hidden="true">
          {SLIDES.map((_, i) => <span key={i} className={`h-1.5 rounded-full transition-all ${i === slide ? 'w-5 bg-white' : 'w-1.5 bg-white/50'}`} />)}
        </div>
      </div>

      {/* Quick actions */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
        {[
          { icon: Calendar, tint: 'bg-blue-50 text-blue-600', title: 'My Visits', sub: 'See all appointments', onClick: goToVisits },
          { icon: User, tint: 'bg-green-50 text-green-600', title: 'My Profile', sub: 'Your details', to: '/dashboard/profile' },
          { icon: ArrowRightLeft, tint: 'bg-purple-50 text-purple-600', title: 'Change Branch', sub: 'Request a transfer', to: '/dashboard/profile' },
          { icon: FileText, tint: 'bg-orange-50 text-orange-600', title: 'Export Visits', sub: 'Download as CSV', onClick: exportVisits, disabled: appointments.length === 0 },
        ].map(({ icon: Icon, tint, title, sub, onClick, to, disabled }) => {
          const body = (
            <>
              <span className={`w-11 h-11 rounded-xl flex items-center justify-center ${tint}`}><Icon size={20} /></span>
              <span className="text-left">
                <span className="block text-sm font-bold text-slate-800">{title}</span>
                <span className="block text-[11px] text-slate-500">{sub}</span>
              </span>
            </>
          );
          const cls = `${card} p-4 flex items-center gap-3 hover:shadow-md transition disabled:opacity-50 disabled:hover:shadow-sm`;
          return to
            ? <Link key={title} to={to} className={cls}>{body}</Link>
            : <button key={title} type="button" onClick={onClick} disabled={disabled} className={cls}>{body}</button>;
        })}
      </div>

      {/* Next appointment / therapist / sessions */}
      <div className="grid lg:grid-cols-3 gap-5 mb-6">
        <div className={`${card} overflow-hidden`}>
          <div className="bg-blue-50/70 p-5 pb-4">
            <div className="flex justify-between items-center">
              {next
                ? <span className="text-[10px] font-bold uppercase tracking-wider bg-green-50 text-green-700 px-2 py-1 rounded">{APPOINTMENT_LABEL[next.status] || next.status}</span>
                : <span className="text-[10px] font-bold uppercase tracking-wider text-slate-400">Nothing booked</span>}
              <Calendar size={16} className="text-blue-500" />
            </div>
            <h3 className="mt-3 text-lg font-bold text-slate-900">Next Appointment</h3>
          </div>
          <div className="p-5">
            {next ? (
              <div data-testid="next-appointment">
                <div className="flex items-center gap-4">
                  <div className="w-14 h-14 rounded-xl bg-blue-600 text-white flex flex-col items-center justify-center leading-none shrink-0">
                    <span className="text-[10px] font-bold">{badge.toLocaleString('en-US', { month: 'short' }).toUpperCase()}</span>
                    <span className="text-xl font-bold mt-1">{badge.getDate()}</span>
                  </div>
                  <div>
                    <p className="font-bold text-slate-900">{badge.toLocaleString('en-US', { weekday: 'long', hour: 'numeric', minute: '2-digit' })}</p>
                    <p className="text-sm text-slate-500">{next.caseType || 'Visit'} with <b>{next.doctorName || 'your physiotherapist'}</b></p>
                  </div>
                </div>
                <p className="mt-4 text-sm text-slate-500 flex items-center gap-2 flex-wrap">
                  <MapPin size={14} className="text-blue-500" />
                  <span>{next.branchName || next.branchId}</span>
                  {next.crossBranch && <span className="text-[10px] font-bold uppercase bg-amber-50 text-amber-700 border border-amber-100 rounded px-1.5 py-0.5">Visit away from your branch</span>}
                </p>
              </div>
            ) : (
              <p data-testid="no-upcoming" className="text-sm text-slate-500">You have no upcoming appointments. Please contact your branch to book a visit.</p>
            )}
          </div>
        </div>

        <div className={`${card} p-5`}>
          <p className="text-[10px] font-bold uppercase tracking-wider text-blue-600">Primary provider</p>
          <h3 className="mt-1 text-lg font-bold text-slate-900">Your Therapist</h3>
          {therapist ? (
            <div className="mt-5 flex items-center gap-4">
              <div className="w-14 h-14 rounded-full bg-blue-100 text-blue-700 flex items-center justify-center text-lg font-bold">{initials(therapist)}</div>
              <div>
                <p className="font-bold text-slate-900">{therapist}</p>
                <p className="text-xs text-slate-500">Physiotherapist</p>
                <p className="text-xs font-semibold text-green-600 mt-1">{therapistSessions} session{therapistSessions === 1 ? '' : 's'} completed</p>
              </div>
            </div>
          ) : (
            <p className="mt-5 text-sm text-slate-500">Your therapist will appear here after your first appointment.</p>
          )}
        </div>

        <div className={`${card} p-5`}>
          <p className="text-[10px] font-bold uppercase tracking-wider text-green-600">Overview</p>
          <h3 className="mt-1 text-lg font-bold text-slate-900">My Sessions</h3>
          <div className="mt-3 flex items-center gap-5">
            <Ring value={completed.length} total={completed.length + upcoming.length} />
            <div className="flex-1 space-y-3 text-xs">
              {[['Completed', completed.length], ['Upcoming', upcoming.length]].map(([label, n]) => {
                const total = completed.length + upcoming.length;
                return (
                  <div key={label}>
                    <div className="flex justify-between font-bold text-slate-700"><span>{label}</span><span className="text-blue-600">{n}</span></div>
                    <div className="h-1.5 bg-slate-100 rounded-full mt-1"><div className="h-1.5 bg-blue-600 rounded-full" style={{ width: `${total ? (n / total) * 100 : 0}%` }} /></div>
                  </div>
                );
              })}
            </div>
          </div>
        </div>
      </div>

      {/* History + branch */}
      <div className="grid lg:grid-cols-3 gap-5 mb-8">
        <div id="my-visits" className={`${card} p-6 lg:col-span-2`}>
          <div className="flex flex-wrap justify-between items-start gap-3 mb-4">
            <div>
              <h2 className="text-lg font-bold text-slate-900">My Visits</h2>
              <p className="text-sm text-slate-500">Your appointments at every branch</p>
            </div>
            <button type="button" onClick={exportVisits} disabled={appointments.length === 0}
              className="flex items-center gap-2 border border-blue-200 text-blue-600 hover:bg-blue-50 px-4 py-2 rounded-xl text-sm font-bold disabled:opacity-40">
              <Download size={15} /> Export
            </button>
          </div>

          {appointments.length === 0 ? (
            <p data-testid="no-visits" className="text-sm text-slate-500 py-6">No visits yet. Your appointments will appear here once they are booked.</p>
          ) : (
            <div role="table" aria-label="My visits" className="relative">
              {[...upcoming, ...pastShown].map((a, i, all) => {
                const isUpcoming = upcoming.includes(a);
                return (
                  <div key={a.id} role="row" className="flex gap-4 pb-6 last:pb-0 relative">
                    {i < all.length - 1 && <span className="absolute left-[9px] top-5 bottom-0 w-px bg-slate-200" aria-hidden="true" />}
                    <span className={`mt-1 w-5 h-5 rounded-full border-2 flex items-center justify-center shrink-0 bg-white ${isUpcoming ? 'border-amber-400' : 'border-blue-600'}`} aria-hidden="true">
                      <span className={`w-2 h-2 rounded-full ${isUpcoming ? 'bg-amber-400' : 'bg-blue-600'}`} />
                    </span>
                    <div role="cell" className="flex-1 min-w-0">
                      <p className="text-[11px] font-bold text-blue-600">{fmtDateTime(a.appointmentDate)}</p>
                      <p className="font-bold text-slate-900">{a.caseType || 'Visit'}</p>
                      <p className="text-xs text-slate-500 mt-0.5 flex flex-wrap items-center gap-x-2 gap-y-1">
                        {a.doctorName && <span className="flex items-center gap-1.5"><span className="w-5 h-5 rounded-full bg-slate-100 text-[8px] font-bold text-slate-600 flex items-center justify-center">{initials(a.doctorName)}</span>{a.doctorName}</span>}
                        <span>{a.branchName || a.branchId}</span>
                        {a.crossBranch && <span className="text-[9px] font-bold uppercase bg-amber-50 text-amber-700 border border-amber-100 rounded px-1.5 py-0.5">Away from your branch</span>}
                      </p>
                    </div>
                    <div role="cell" className="text-xs font-bold text-slate-600 whitespace-nowrap">{APPOINTMENT_LABEL[a.status] || a.status}</div>
                  </div>
                );
              })}
              {past.length > 5 && (
                <button type="button" onClick={() => setShowAll(!showAll)} className="mt-4 text-sm font-bold text-blue-600 hover:underline">
                  {showAll ? 'Show fewer' : `Show all ${past.length} past visits`}
                </button>
              )}
            </div>
          )}
        </div>

        <div className={`${card} p-6`}>
          <h2 className="text-lg font-bold text-slate-900">My Branch</h2>
          <p className="text-sm text-slate-500 mt-1">Registered at {branchName}</p>
          <h3 className="mt-5 text-[11px] font-bold uppercase tracking-wider text-slate-400">Branch change requests</h3>
          {transfers.length === 0 ? (
            <p className="text-sm text-slate-500 mt-2">You have not requested a branch change.</p>
          ) : (
            <ul className="mt-2 divide-y divide-slate-100 text-sm">
              {transfers.slice(0, 3).map((t) => (
                <li key={t.id} className="py-2.5">
                  <div className="flex justify-between gap-2 items-center">
                    <span className="text-slate-700">{t.fromBranchName} → <b>{t.toBranchName}</b></span>
                    <span className={`text-[9px] font-bold px-2 py-0.5 rounded border uppercase whitespace-nowrap ${TRANSFER_BADGE[t.status]}`}>{TRANSFER_LABEL[t.status] || t.status}</span>
                  </div>
                  <p className="text-[11px] text-slate-400 mt-0.5">Requested {fmtDate(t.requestedAt)}{t.decidedAt ? ` · decided ${fmtDate(t.decidedAt)}` : ''}</p>
                </li>
              ))}
            </ul>
          )}
          <Link to="/dashboard/profile" className="mt-5 inline-block text-sm font-bold text-blue-600 hover:underline">Manage in my profile</Link>
        </div>
      </div>

      {/* Footer */}
      <div className="border-t border-slate-200 pt-6 text-center text-xs text-slate-500">
        <p className="font-bold tracking-widest text-slate-700 uppercase">BeFit Physiotherapy Clinic</p>
        <p className="mt-2 max-w-md mx-auto">Your records are visible only to you and your care team. In an emergency, contact your nearest emergency service.</p>
        <p className="mt-3 text-[11px] text-slate-400">© {new Date().getFullYear()} BeFit Care Portal. All rights reserved.</p>
      </div>
    </div>
  );
}
