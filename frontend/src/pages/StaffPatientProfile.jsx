import React, { useState, useEffect, useCallback } from 'react';
import { Link, useParams, useNavigate } from 'react-router-dom';
import axios, { errorMessage, classifyError } from '../api';
import StaffLayout from '../layouts/StaffLayout';
import InvoicePreviewModal from '../components/billing/InvoicePreviewModal';
import { fmtDate, fmtDateTime, money, STATUS_BADGE } from '../utils/format';

/**
 * Staff "View File" screen: /staff-dashboard/patients/:id
 *
 * This replaces the old route, which showed the PATIENT's own "My Profile" page (it ignored :id and called
 * /api/patients/me with the staff token -> 401 -> "Failed to load profile. Please log in again.").
 * Every failure kind is handled on its own: only a real 401 leads to login (done centrally in api.js).
 */
export default function StaffPatientProfile() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [profile, setProfile] = useState(null);
  const [history, setHistory] = useState(null);
  const [state, setState] = useState({ loading: true, kind: null, message: '' });
  const [printId, setPrintId] = useState(null);
  const isAdmin = localStorage.getItem('role') === 'ADMIN';

  const load = useCallback(async () => {
    setState({ loading: true, kind: null, message: '' });
    try {
      const p = await axios.get(`/api/patients/${id}`);
      setProfile(p.data);
      try {
        const h = await axios.get(`/api/patients/${id}/history`);
        setHistory(h.data);
      } catch { setHistory({ appointments: [], bills: [], branchHistory: [] }); }
      setState({ loading: false, kind: null, message: '' });
    } catch (err) {
      const kind = classifyError(err);
      const fallback = {
        forbidden: 'You do not have access to this patient’s file.',
        notfound: 'This patient could not be found.',
        server: 'The server had a problem loading this file. Please try again in a moment.',
        network: 'Cannot reach the server. Please check your connection and try again.',
        unauthorized: 'Your session has expired. Please log in again.',
      }[kind] || 'Could not load this patient’s file.';
      const message = kind === 'server' || kind === 'network' ? fallback : await errorMessage(err, fallback);
      setState({ loading: false, kind, message });
    }
  }, [id]);

  useEffect(() => { load(); }, [load]);

  const field = (label, value) => (
    <div><p className="text-[10px] font-bold text-slate-400 uppercase tracking-wider">{label}</p><p className="text-sm font-semibold text-slate-800 mt-0.5">{value || '—'}</p></div>
  );

  if (state.loading) {
    return <StaffLayout><div className="max-w-5xl mx-auto py-24 text-center text-slate-500"><div className="w-8 h-8 border-4 border-blue-200 border-t-blue-600 rounded-full animate-spin mx-auto mb-4"></div>Loading patient file…</div></StaffLayout>;
  }

  if (state.kind) {
    const icon = { forbidden: '🔒', notfound: '🔍', server: '⚠️', network: '📡' }[state.kind] || '⚠️';
    return (
      <StaffLayout>
        <div className="max-w-xl mx-auto mt-16 bg-white border border-slate-100 rounded-2xl shadow-sm p-10 text-center">
          <div className="text-4xl mb-3">{icon}</div>
          <h2 className="text-lg font-bold text-slate-800 mb-1">Can’t open this file</h2>
          <p className="text-sm text-slate-500 mb-6">{state.message}</p>
          <div className="flex justify-center gap-3">
            <button onClick={() => navigate('/staff-dashboard/patients')} className="px-5 py-2.5 rounded-lg text-[13px] font-bold text-slate-600 bg-slate-100 hover:bg-slate-200">Back to patients</button>
            {(state.kind === 'server' || state.kind === 'network') && <button onClick={load} className="px-5 py-2.5 rounded-lg text-[13px] font-bold text-white bg-[#2563eb] hover:bg-blue-700">Try again</button>}
          </div>
        </div>
      </StaffLayout>
    );
  }

  const myBranch = localStorage.getItem('branchId');
  const movedAway = !isAdmin && profile.branchId !== myBranch;

  return (
    <StaffLayout>
      <div className="max-w-5xl mx-auto pb-10">
        <Link to="/staff-dashboard/patients" className="text-[13px] font-semibold text-slate-500 hover:text-blue-600">← Back to patients</Link>

        <div className="bg-white rounded-2xl border border-slate-100 shadow-sm p-6 mt-4 flex flex-wrap items-center justify-between gap-4">
          <div className="flex items-center gap-4">
            <div className="w-14 h-14 rounded-full bg-blue-50 text-blue-600 flex items-center justify-center font-bold text-lg">{(profile.fullName || 'U').substring(0, 2).toUpperCase()}</div>
            <div>
              <h1 className="text-xl font-bold text-slate-800">{profile.fullName}</h1>
              <p className="text-sm text-slate-500">{profile.regId} • {profile.branchName || profile.branchId}</p>
            </div>
          </div>
          <div className="flex gap-2">
            {profile.pendingTransfer && <span className="text-[10px] font-bold uppercase bg-amber-50 text-amber-700 border border-amber-100 rounded px-2 py-1">Transfer pending → {profile.pendingTransfer.toBranchName}</span>}
            {movedAway && <span className="text-[10px] font-bold uppercase bg-slate-100 text-slate-600 border border-slate-200 rounded px-2 py-1">Now at {profile.branchName}</span>}
          </div>
        </div>

        {movedAway && (
          <div className="mt-4 bg-amber-50 border border-amber-200 text-amber-800 text-sm rounded-lg p-3">
            This patient has moved to another branch. You can see the visits and invoices from <b>your</b> branch only.
          </div>
        )}

        <div className="grid md:grid-cols-2 gap-6 mt-6">
          <div className="bg-white rounded-2xl border border-slate-100 shadow-sm p-6">
            <h2 className="text-sm font-bold text-slate-800 mb-4">Personal information</h2>
            <div className="grid grid-cols-2 gap-5">
              {field('Phone', profile.phone)}
              {field('Gender', profile.gender)}
              {field('Date of birth', profile.dateOfBirth && fmtDate(profile.dateOfBirth))}
              {field('Blood group', profile.bloodGroup)}
              {field('Occupation', profile.occupation)}
              {field('Registered', fmtDate(profile.registrationDate))}
              <div className="col-span-2">{field('Address', profile.address)}</div>
            </div>
          </div>
          <div className="bg-white rounded-2xl border border-slate-100 shadow-sm p-6">
            <h2 className="text-sm font-bold text-slate-800 mb-4">Emergency contact</h2>
            <div className="grid grid-cols-2 gap-5">
              {field('Name', profile.emergencyContactName)}
              {field('Phone', profile.emergencyContact)}
            </div>
            <h2 className="text-sm font-bold text-slate-800 mt-6 mb-2">Medical history</h2>
            <p className="text-sm text-slate-600 whitespace-pre-wrap">{profile.medicalHistory || 'No medical history recorded.'}</p>
          </div>
        </div>

        <div className="bg-white rounded-2xl border border-slate-100 shadow-sm p-6 mt-6">
          <h2 className="text-sm font-bold text-slate-800 mb-3">Branch history</h2>
          {history.branchHistory.length === 0 ? <p className="text-sm text-slate-400">No branch history.</p> : (
            <ul className="divide-y divide-slate-100">
              {history.branchHistory.map((b, i) => (
                <li key={i} className="py-2 flex flex-wrap justify-between gap-2 text-sm">
                  <span className="font-semibold text-slate-800">{b.branchName || b.branchId}{!b.toDate && <span className="ml-2 text-[9px] font-bold uppercase bg-green-50 text-green-700 border border-green-100 rounded px-1.5 py-0.5">current</span>}</span>
                  <span className="text-slate-500">{fmtDate(b.fromDate)} → {b.toDate ? fmtDate(b.toDate) : 'present'}{b.authorizedBy ? ` • by ${b.authorizedBy}` : ''}</span>
                </li>
              ))}
            </ul>
          )}
        </div>

        <div className="bg-white rounded-2xl border border-slate-100 shadow-sm mt-6 overflow-hidden">
          <h2 className="text-sm font-bold text-slate-800 p-6 pb-3">Appointments {isAdmin ? '(all branches)' : ''}</h2>
          <div className="overflow-x-auto">
            <table className="w-full text-left text-[13px] text-slate-600">
              <thead className="bg-[#f8f9fc] text-[10px] uppercase font-bold text-slate-400"><tr><th className="px-6 py-3">Date</th><th className="px-6 py-3">Doctor</th><th className="px-6 py-3">Case</th><th className="px-6 py-3">Branch</th><th className="px-6 py-3">Status</th></tr></thead>
              <tbody className="divide-y divide-slate-100">
                {history.appointments.length === 0 && <tr><td colSpan="5" className="px-6 py-6 text-center text-slate-400">No appointments.</td></tr>}
                {history.appointments.map((a) => (
                  <tr key={a.id}><td className="px-6 py-3">{fmtDateTime(a.appointmentDate)}</td><td className="px-6 py-3">{a.doctorName}</td><td className="px-6 py-3">{a.caseType}</td><td className="px-6 py-3">{a.branchName || a.branchId}</td><td className="px-6 py-3 font-semibold">{a.status}</td></tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>

        {(localStorage.getItem('role') !== 'DOCTOR') && (
          <div className="bg-white rounded-2xl border border-slate-100 shadow-sm mt-6 overflow-hidden">
            <h2 className="text-sm font-bold text-slate-800 p-6 pb-3">Invoices {isAdmin ? '(all branches)' : ''}</h2>
            <div className="overflow-x-auto">
              <table className="w-full text-left text-[13px] text-slate-600">
                <thead className="bg-[#f8f9fc] text-[10px] uppercase font-bold text-slate-400"><tr><th className="px-6 py-3">Invoice</th><th className="px-6 py-3">Date</th><th className="px-6 py-3">Branch</th><th className="px-6 py-3 text-right">Total</th><th className="px-6 py-3 text-right">Balance</th><th className="px-6 py-3">Status</th><th className="px-6 py-3"></th></tr></thead>
                <tbody className="divide-y divide-slate-100">
                  {history.bills.length === 0 && <tr><td colSpan="7" className="px-6 py-6 text-center text-slate-400">No invoices.</td></tr>}
                  {history.bills.map((b) => (
                    <tr key={b.id}>
                      <td className="px-6 py-3 font-bold text-[#2563eb]">{b.invoiceNumber}</td><td className="px-6 py-3">{fmtDate(b.billDate)}</td><td className="px-6 py-3">{b.branchName || b.branchId}</td>
                      <td className="px-6 py-3 text-right">{money(b.totalAmount)}</td><td className="px-6 py-3 text-right">{money(b.balanceDue)}</td>
                      <td className="px-6 py-3"><span className={`text-[9px] font-bold px-2 py-1 rounded border uppercase ${STATUS_BADGE[b.status] || ''}`}>{b.status}</span></td>
                      <td className="px-6 py-3 text-right"><button onClick={() => setPrintId(b.id)} className="text-slate-500 hover:text-blue-600 font-bold">Print</button></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}
      </div>
      {printId && <InvoicePreviewModal billId={printId} onClose={() => setPrintId(null)} />}
    </StaffLayout>
  );
}
