import React, { useState, useEffect, useCallback } from 'react';
import axios, { errorMessage } from '../api';
import StaffLayout from '../layouts/StaffLayout';
import TransferTabs, { STATUS_BADGE } from '../components/transfers/TransferTabs';
import { fmtDateTime } from '../utils/format';

/**
 * Outgoing transfer tracking: requests LEAVING this branch, their status, and who decided what.
 * The receiving branch decides; this page only follows the request (it never shows the receiving branch's queue or its
 * internal review). Counts, rows and actions all come from /api/transfers, which the server limits to this branch.
 * Receptionists can also raise a new request here for one of this branch's patients and withdraw a pending one.
 */
export default function OutgoingTransfers() {
  const [status, setStatus] = useState('ALL');
  const [counts, setCounts] = useState({ PENDING: 0, APPROVED: 0, REJECTED: 0, CANCELLED: 0 });
  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [busyId, setBusyId] = useState(null);

  const [patients, setPatients] = useState([]);
  const [branches, setBranches] = useState([]);
  const [form, setForm] = useState({ patientId: '', toBranchId: '', reason: '' });
  const [submitting, setSubmitting] = useState(false);
  const isManager = localStorage.getItem('role') === 'BRANCH_MANAGER';   // managers decide requests; receptionists raise them

  const load = useCallback(async () => {
    setLoading(true); setError('');
    try {
      const res = await axios.get('/api/transfers', { params: { status, direction: 'OUTGOING' } });
      setRows(res.data);
    } catch (err) {
      setError(await errorMessage(err, 'Could not load transfer requests.'));
    } finally { setLoading(false); }
  }, [status]);

  const loadCounts = useCallback(async () => {
    try {
      const res = await axios.get('/api/transfers/summary', { params: { direction: 'OUTGOING' } });
      setCounts(res.data);
    } catch { /* the table below still works; counts are only a convenience */ }
  }, []);

  useEffect(() => { load(); }, [load]);
  useEffect(() => { loadCounts(); }, [loadCounts]);

  useEffect(() => {
    if (!isManager) axios.get('/api/patients/options').then((r) => setPatients(r.data)).catch(() => {});
    axios.get('/api/branches').then((r) => setBranches(r.data)).catch(() => {});
  }, []);

  const selected = patients.find((p) => String(p.id) === String(form.patientId));
  const destinations = branches.filter((b) => !selected || b.id !== selected.branchId);

  const submit = async (e) => {
    e.preventDefault();
    setSubmitting(true); setError(''); setNotice('');
    try {
      await axios.post('/api/transfers', {
        patientId: Number(form.patientId),
        fromBranchId: selected?.branchId || null,
        toBranchId: form.toBranchId,
        reason: form.reason.trim(),
      });
      setNotice('Transfer request sent. The patient stays at this branch until the receiving branch approves it.');
      setForm({ patientId: '', toBranchId: '', reason: '' });
      load(); loadCounts();
    } catch (err) {
      setError(await errorMessage(err, 'Could not create the transfer request.'));
    } finally { setSubmitting(false); }
  };

  const cancel = async (t) => {
    const reason = window.prompt('Why are you withdrawing this request? (optional)', '');
    if (reason === null) return;
    setBusyId(t.id); setError(''); setNotice('');
    try {
      await axios.post(`/api/transfers/${t.id}/cancel`, { note: reason || null });
      setNotice('Request withdrawn.');
      load(); loadCounts();
    } catch (err) {
      setError(await errorMessage(err, 'Could not withdraw the request.'));
    } finally { setBusyId(null); }
  };

  const field = 'w-full bg-[#f4f7fe] border border-transparent rounded-lg px-4 py-2.5 text-[13px] focus:outline-none focus:border-[#2563eb] focus:bg-white transition';

  return (
    <StaffLayout>
      <div className="max-w-[1200px] mx-auto pb-10">
        <div className="flex justify-between items-end mb-4">
          <div>
            <h1 className="text-2xl font-bold text-slate-800">Branch Transfers</h1>
            <p className="text-[13px] text-slate-500 mt-1">Follow the transfer requests you sent to other branches. The receiving branch makes the decision.</p>
          </div>
          <select value={status} onChange={(e) => setStatus(e.target.value)} className="bg-white border border-slate-200 rounded-lg px-3 py-2 text-sm shadow-sm">
            {['PENDING', 'APPROVED', 'REJECTED', 'CANCELLED', 'ALL'].map((s) => <option key={s} value={s}>{s === 'ALL' ? 'All' : s.charAt(0) + s.slice(1).toLowerCase()}</option>)}
          </select>
        </div>
        <TransferTabs active="outgoing" />
        <div className="grid grid-cols-2 md:grid-cols-5 gap-3 mb-5">
          {[['ALL', 'All requests', Object.values(counts).reduce((a, b) => a + b, 0)], ['PENDING', 'Pending', counts.PENDING], ['APPROVED', 'Approved', counts.APPROVED], ['REJECTED', 'Rejected', counts.REJECTED], ['CANCELLED', 'Cancelled', counts.CANCELLED]].map(([key, label, n]) => (
            <button key={key} type="button" onClick={() => setStatus(key)}
              className={`text-left rounded-xl border p-4 transition ${status === key ? 'bg-blue-50 border-blue-200' : 'bg-white border-slate-100 hover:bg-slate-50'}`}>
              <div className="text-2xl font-bold text-slate-800">{n ?? 0}</div>
              <div className="text-[11px] font-bold uppercase tracking-wider text-slate-500">{label}</div>
            </button>
          ))}
        </div>
        {error && <div className="mb-3 bg-red-50 border border-red-100 text-red-700 text-sm rounded-lg p-3">{error}</div>}
        {notice && <div className="mb-3 bg-green-50 border border-green-100 text-green-800 text-sm rounded-lg p-3">{notice}</div>}

        {!isManager && <form onSubmit={submit} className="bg-white rounded-2xl border border-slate-100 shadow-sm p-6 mb-6">
          <h2 className="text-sm font-bold text-slate-800 mb-4">New transfer request</h2>
          <div className="grid md:grid-cols-3 gap-5">
            <div>
              <label className="block text-[11px] font-bold text-slate-500 uppercase tracking-wider mb-1.5">Patient *</label>
              <select required className={field} value={form.patientId} onChange={(e) => setForm({ ...form, patientId: e.target.value, toBranchId: '' })}>
                <option value="">-- Choose a patient --</option>
                {patients.map((p) => <option key={p.id} value={p.id}>{p.fullName} ({p.phone})</option>)}
              </select>
            </div>
            <div>
              <label className="block text-[11px] font-bold text-slate-500 uppercase tracking-wider mb-1.5">Move to branch *</label>
              <select required className={field} value={form.toBranchId} onChange={(e) => setForm({ ...form, toBranchId: e.target.value })}>
                <option value="">-- Destination --</option>
                {destinations.map((b) => <option key={b.id} value={b.id}>{b.name}</option>)}
              </select>
            </div>
            <div>
              <label className="block text-[11px] font-bold text-slate-500 uppercase tracking-wider mb-1.5">Reason *</label>
              <input required maxLength={500} className={field} value={form.reason} onChange={(e) => setForm({ ...form, reason: e.target.value })} placeholder="e.g. Moved house, closer to work" />
            </div>
          </div>
          <div className="mt-4 flex justify-end">
            <button type="submit" disabled={submitting} className="bg-[#2563eb] hover:bg-blue-700 text-white px-6 py-2.5 rounded-lg text-[13px] font-bold shadow-sm disabled:opacity-50">{submitting ? 'Sending…' : 'Send request'}</button>
          </div>
        </form>}

        <div className="bg-white rounded-2xl border border-slate-100 shadow-sm overflow-hidden">
          <table className="w-full text-left text-[13px] text-slate-600">
            <thead className="bg-[#f8f9fc] text-[10px] uppercase font-bold text-slate-400">
              <tr><th className="px-6 py-4">Patient</th><th className="px-6 py-4">From → To</th><th className="px-6 py-4">Reason</th><th className="px-6 py-4">Requested</th><th className="px-6 py-4">Decision</th><th className="px-6 py-4">Status</th><th className="px-6 py-4 text-right">Action</th></tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {loading ? <tr><td colSpan="7" className="text-center py-10 text-slate-400">Loading…</td></tr>
                : rows.length === 0 ? <tr><td colSpan="7" className="text-center py-10 text-slate-400">No transfer requests.</td></tr>
                : rows.map((t) => (
                  <tr key={t.id}>
                    <td className="px-6 py-4 font-bold text-slate-800">{t.patientName}<div className="text-[11px] font-normal text-slate-400">REG-{t.patientId}</div></td>
                    <td className="px-6 py-4">{t.fromBranchName} → <b>{t.toBranchName}</b></td>
                    <td className="px-6 py-4 max-w-[240px]">{t.reason || '—'}</td>
                    <td className="px-6 py-4 whitespace-nowrap">{fmtDateTime(t.requestedAt)}<div className="text-[11px] text-slate-400">by {t.requestedBy}</div></td>
                    <td className="px-6 py-4 max-w-[260px]">
                      {t.decidedBy ? (<>
                        <div className="text-[11px] font-bold uppercase text-slate-400">{t.status === 'APPROVED' ? 'Approved by' : t.status === 'REJECTED' ? 'Rejected by' : 'Withdrawn by'}</div>
                        <div className="text-[12px]">{t.decidedBy}</div>
                        <div className="text-[11px] text-slate-400">{fmtDateTime(t.decidedAt)}</div>
                      </>) : <span className="text-slate-400">Waiting for {t.toBranchName}</span>}
                      {t.decisionNote && <div className="text-[11px] text-slate-500 mt-1">Note: {t.decisionNote}</div>}
                    </td>
                    <td className="px-6 py-4"><span className={`text-[9px] font-bold px-2 py-1 rounded border uppercase ${STATUS_BADGE[t.status]}`}>{t.status}</span></td>
                    <td className="px-6 py-4 text-right whitespace-nowrap">
                      {t.status === 'PENDING' && <button disabled={busyId === t.id} onClick={() => cancel(t)} className="text-red-600 hover:text-red-800 font-bold disabled:opacity-50">Withdraw</button>}
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
