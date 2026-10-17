import React, { useState, useEffect, useCallback } from 'react';
import axios, { errorMessage } from '../api';
import StaffLayout from '../layouts/StaffLayout';
import { fmtDateTime } from '../utils/format';

/** Incoming branch-transfer requests for THIS branch. Approving moves the patient; their old visits/invoices stay where they were. */
export default function TransferRequests() {
  const [status, setStatus] = useState('PENDING');
  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [busyId, setBusyId] = useState(null);

  const load = useCallback(async () => {
    setLoading(true); setError('');
    try {
      const res = await axios.get('/api/transfers', { params: { status } });
      setRows(res.data);
    } catch (err) {
      setError(await errorMessage(err, 'Could not load transfer requests.'));
    } finally { setLoading(false); }
  }, [status]);

  useEffect(() => { load(); }, [load]);

  const decide = async (t, action) => {
    const note = window.prompt(action === 'approve' ? 'Optional note for the patient:' : 'Reason for rejecting (optional):', '');
    if (note === null) return;
    setBusyId(t.id); setError(''); setNotice('');
    try {
      await axios.post(`/api/transfers/${t.id}/${action}`, { note: note || null });
      setNotice(action === 'approve' ? `${t.patientName} is now registered at ${t.toBranchName}.` : 'Request rejected.');
      load();
    } catch (err) {
      setError(await errorMessage(err, 'Could not update the request.'));
    } finally { setBusyId(null); }
  };

  const badge = { PENDING: 'bg-amber-50 text-amber-700 border-amber-100', APPROVED: 'bg-green-50 text-green-700 border-green-100', REJECTED: 'bg-red-50 text-red-700 border-red-100', CANCELLED: 'bg-slate-100 text-slate-500 border-slate-200' };

  return (
    <StaffLayout>
      <div className="max-w-[1200px] mx-auto pb-10">
        <div className="flex justify-between items-end mb-6">
          <div>
            <h1 className="text-2xl font-bold text-slate-800">Branch Transfers</h1>
            <p className="text-[13px] text-slate-500 mt-1">Patients asking to join this branch</p>
          </div>
          <select value={status} onChange={(e) => setStatus(e.target.value)} className="bg-white border border-slate-200 rounded-lg px-3 py-2 text-sm shadow-sm">
            {['PENDING', 'APPROVED', 'REJECTED', 'CANCELLED', 'ALL'].map((s) => <option key={s} value={s}>{s === 'ALL' ? 'All' : s.charAt(0) + s.slice(1).toLowerCase()}</option>)}
          </select>
        </div>
        {error && <div className="mb-3 bg-red-50 border border-red-100 text-red-700 text-sm rounded-lg p-3">{error}</div>}
        {notice && <div className="mb-3 bg-green-50 border border-green-100 text-green-800 text-sm rounded-lg p-3">{notice}</div>}
        <div className="bg-white rounded-2xl border border-slate-100 shadow-sm overflow-hidden">
          <table className="w-full text-left text-[13px] text-slate-600">
            <thead className="bg-[#f8f9fc] text-[10px] uppercase font-bold text-slate-400">
              <tr><th className="px-6 py-4">Patient</th><th className="px-6 py-4">From → To</th><th className="px-6 py-4">Reason</th><th className="px-6 py-4">Requested</th><th className="px-6 py-4">Status</th><th className="px-6 py-4 text-right">Action</th></tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {loading ? <tr><td colSpan="6" className="text-center py-10 text-slate-400">Loading…</td></tr>
                : rows.length === 0 ? <tr><td colSpan="6" className="text-center py-10 text-slate-400">No transfer requests.</td></tr>
                : rows.map((t) => (
                  <tr key={t.id}>
                    <td className="px-6 py-4 font-bold text-slate-800">{t.patientName}<div className="text-[11px] font-normal text-slate-400">REG-{t.patientId}</div></td>
                    <td className="px-6 py-4">{t.fromBranchName} → <b>{t.toBranchName}</b></td>
                    <td className="px-6 py-4 max-w-[240px]">{t.reason || '—'}{t.decisionNote && <div className="text-[11px] text-slate-400">Note: {t.decisionNote}</div>}</td>
                    <td className="px-6 py-4 whitespace-nowrap">{fmtDateTime(t.requestedAt)}</td>
                    <td className="px-6 py-4"><span className={`text-[9px] font-bold px-2 py-1 rounded border uppercase ${badge[t.status]}`}>{t.status}</span></td>
                    <td className="px-6 py-4 text-right whitespace-nowrap space-x-3">
                      {t.status === 'PENDING' && (<>
                        <button disabled={busyId === t.id} onClick={() => decide(t, 'approve')} className="text-green-600 hover:text-green-800 font-bold disabled:opacity-50">Approve</button>
                        <button disabled={busyId === t.id} onClick={() => decide(t, 'reject')} className="text-red-600 hover:text-red-800 font-bold disabled:opacity-50">Reject</button>
                      </>)}
                    </td>
                  </tr>
                ))}
            </tbody>
          </table>
        </div>
      </div>
    </StaffLayout>
  );
}
