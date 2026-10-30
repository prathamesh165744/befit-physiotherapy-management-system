import React, { useState, useEffect, useCallback } from 'react';
import { Link } from 'react-router-dom';
import axios, { errorMessage } from '../api';
import { fmtDate, fmtDateTime } from '../utils/format';
import { splitAppointments, TRANSFER_BADGE, TRANSFER_LABEL, APPOINTMENT_LABEL } from '../utils/patientPortal';

/**
 * Patient home. Everything comes from the logged-in patient's own endpoints (/api/patients/me, /me/history, /me/transfers):
 * the server identifies the patient from the login token, so this works from any branch and keeps working after a transfer.
 * States: loading, error (with retry), empty (no visits yet) and the filled dashboard.
 */
export default function PatientDashboard() {
  const [state, setState] = useState({ status: 'loading', error: '' });
  const [profile, setProfile] = useState(null);
  const [history, setHistory] = useState(null);
  const [transfers, setTransfers] = useState([]);

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

  const { upcoming, past, next } = splitAppointments(history?.appointments);
  const latestTransfer = transfers[0] || null;
  const firstName = (profile?.fullName || '').split(' ')[0] || 'there';
  const hasVisits = (history?.appointments || []).length > 0;

  return (
    <div className="p-4 sm:p-6 lg:p-8 max-w-5xl mx-auto font-sans">
      <div className="mb-6">
        <h1 className="text-2xl sm:text-3xl font-bold text-slate-800">Welcome, {firstName}</h1>
        <p className="text-sm text-slate-500 mt-1">
          You are registered at <b>{profile?.branchName || profile?.branchId || 'your branch'}</b>. Your visits at every branch are listed below.
        </p>
      </div>

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

      {/* Next appointment + counts */}
      <div className="grid md:grid-cols-3 gap-5 mb-6">
        <div className="md:col-span-2 bg-white rounded-2xl border border-slate-100 shadow-sm p-6">
          <p className="text-[11px] font-bold text-slate-400 uppercase tracking-wider mb-2">Next appointment</p>
          {next ? (
            <div data-testid="next-appointment">
              <p className="text-xl font-bold text-slate-800">{fmtDateTime(next.appointmentDate)}</p>
              <p className="text-slate-600 mt-1">{next.caseType || 'Visit'} with <b>{next.doctorName || 'your physiotherapist'}</b></p>
              <p className="text-sm text-slate-500 mt-1">
                {next.branchName || next.branchId}
                {next.crossBranch && <span className="ml-2 text-[10px] font-bold uppercase bg-amber-50 text-amber-700 border border-amber-100 rounded px-1.5 py-0.5">Visit away from your branch</span>}
              </p>
            </div>
          ) : (
            <p data-testid="no-upcoming" className="text-slate-500">You have no upcoming appointments. Please contact your branch to book a visit.</p>
          )}
        </div>
        <div className="bg-white rounded-2xl border border-slate-100 shadow-sm p-6 grid grid-cols-2 gap-4 text-center">
          <div><p className="text-2xl font-bold text-slate-800">{upcoming.length}</p><p className="text-[11px] font-bold text-slate-400 uppercase">Upcoming</p></div>
          <div><p className="text-2xl font-bold text-slate-800">{past.filter((a) => a.status === 'COMPLETED').length}</p><p className="text-[11px] font-bold text-slate-400 uppercase">Completed</p></div>
        </div>
      </div>

      {/* Visit history */}
      <div className="bg-white rounded-2xl border border-slate-100 shadow-sm overflow-hidden mb-6">
        <div className="flex justify-between items-center p-6 pb-3">
          <h2 className="text-sm font-bold text-slate-800">My visits (all branches)</h2>
          <Link to="/dashboard/profile" className="text-sm font-bold text-blue-600 hover:underline">My profile</Link>
        </div>
        {!hasVisits ? (
          <p data-testid="no-visits" className="px-6 pb-6 text-sm text-slate-500">No visits yet. Your appointments will appear here once they are booked.</p>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-[13px] text-slate-600">
              <thead className="bg-[#f8f9fc] text-[10px] uppercase font-bold text-slate-400">
                <tr><th className="px-6 py-3">Date</th><th className="px-6 py-3">Branch</th><th className="px-6 py-3">Visit</th><th className="px-6 py-3">Status</th></tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {[...upcoming, ...past].map((a) => (
                  <tr key={a.id}>
                    <td className="px-6 py-3 whitespace-nowrap">{fmtDateTime(a.appointmentDate)}</td>
                    <td className="px-6 py-3">{a.branchName || a.branchId}</td>
                    <td className="px-6 py-3">{a.caseType || 'Visit'} · {a.doctorName || '—'}</td>
                    <td className="px-6 py-3 font-semibold">{APPOINTMENT_LABEL[a.status] || a.status}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
