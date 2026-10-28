import React, { useState, useEffect, useCallback } from 'react';
import axios, { errorMessage } from '../api';
import StaffLayout from '../layouts/StaffLayout';
import TransferTabs, { STATUS_BADGE } from '../components/transfers/TransferTabs';
import { fmtDate, fmtDateTime, money } from '../utils/format';

/**
 * Incoming branch-transfer requests for THIS branch.
 * Approving opens a review: the server's rule check (blockers / warnings) is shown first. Blockers disable approval;
 * warnings need a tick + a note. Approving moves the patient; their old visits/invoices stay at the branch that made them.
 * The server enforces every rule again - this screen only makes them visible.
 */
export default function TransferRequests() {
  const [status, setStatus] = useState('PENDING');
  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [busyId, setBusyId] = useState(null);
  // What this user may do comes from the server (role + clinic rule). The server re-checks every action anyway.
  const [perm, setPerm] = useState({ canDecide: false, managerApprovalRequired: true });

  const [review, setReview] = useState(null);       // { transfer, validation }
  const [reviewError, setReviewError] = useState('');
  const [note, setNote] = useState('');
  const [ack, setAck] = useState(false);

  const load = useCallback(async () => {
    setLoading(true); setError('');
    try {
      const res = await axios.get('/api/transfers', { params: { status, direction: 'INCOMING' } });
      setRows(res.data);
    } catch (err) {
      setError(await errorMessage(err, 'Could not load transfer requests.'));
    } finally { setLoading(false); }
  }, [status]);

  useEffect(() => { load(); }, [load]);
  useEffect(() => { axios.get('/api/transfers/permissions').then((r) => setPerm(r.data)).catch(() => setPerm({ canDecide: false, managerApprovalRequired: true })); }, []);

  const openReview = async (t) => {
    setBusyId(t.id); setError(''); setNotice('');
    try {
      const res = await axios.get(`/api/transfers/${t.id}/validation`);
      setReview({ transfer: t, validation: res.data });
      setNote(''); setAck(false); setReviewError('');
    } catch (err) {
      setError(await errorMessage(err, 'Could not check this request.'));
    } finally { setBusyId(null); }
  };

  const approve = async () => {
    const { transfer, validation } = review;
    setBusyId(transfer.id); setReviewError('');
    try {
      await axios.post(`/api/transfers/${transfer.id}/approve`, { note: note.trim() || null, acknowledgeWarnings: validation.requiresAcknowledgement ? ack : false });
      setNotice(`${transfer.patientName} is now registered at ${transfer.toBranchName}.`);
      setReview(null);
      load();
    } catch (err) {
      setReviewError(await errorMessage(err, 'Could not approve the request.'));
    } finally { setBusyId(null); }
  };

  const reject = async (t) => {
    const reason = window.prompt('Reason for rejecting (optional):', '');
    if (reason === null) return;
    setBusyId(t.id); setError(''); setNotice('');
    try {
      await axios.post(`/api/transfers/${t.id}/reject`, { note: reason || null });
      setNotice('Request rejected. The patient stays at their current branch.');
      load();
    } catch (err) {
      setError(await errorMessage(err, 'Could not reject the request.'));
    } finally { setBusyId(null); }
  };

  const v = review?.validation;
  const approveDisabled = !v || !v.canApprove || busyId != null || (v.requiresAcknowledgement && (!ack || !note.trim()));

  return (
    <StaffLayout>
      <div className="max-w-[1200px] mx-auto pb-10">
        <div className="flex justify-between items-end mb-4">
          <div>
            <h1 className="text-2xl font-bold text-slate-800">Branch Transfers</h1>
            <p className="text-[13px] text-slate-500 mt-1">Patients asking to join this branch</p>
          </div>
          <select value={status} onChange={(e) => setStatus(e.target.value)} className="bg-white border border-slate-200 rounded-lg px-3 py-2 text-sm shadow-sm">
            {['PENDING', 'APPROVED', 'REJECTED', 'CANCELLED', 'ALL'].map((s) => <option key={s} value={s}>{s === 'ALL' ? 'All' : s.charAt(0) + s.slice(1).toLowerCase()}</option>)}
          </select>
        </div>
        <TransferTabs active="incoming" />
        {!perm.canDecide && <div className="mb-3 bg-blue-50 border border-blue-100 text-blue-800 text-sm rounded-lg p-3">{perm.managerApprovalRequired ? 'Transfers into this branch are approved or rejected by the branch manager or an administrator.' : 'You can view transfer requests here.'}</div>}
        {error && <div className="mb-3 bg-red-50 border border-red-100 text-red-700 text-sm rounded-lg p-3">{error}</div>}
        {notice && <div className="mb-3 bg-green-50 border border-green-100 text-green-800 text-sm rounded-lg p-3">{notice}</div>}
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
                    <td className="px-6 py-4 whitespace-nowrap">{fmtDateTime(t.requestedAt)}<div className="text-[11px] text-slate-400">by {t.requestedBy}{t.requestedByRole ? ` (${t.requestedByRole.toLowerCase()})` : ''}</div></td>
                    <td className="px-6 py-4 max-w-[260px]">
                      {t.decidedBy ? <><div className="text-[12px]">{t.decidedBy}</div><div className="text-[11px] text-slate-400">{fmtDateTime(t.decidedAt)}</div></> : '—'}
                      {t.decisionNote && <div className="text-[11px] text-slate-500">Note: {t.decisionNote}</div>}
                      {t.decisionWarnings && <div className="text-[11px] text-amber-700">Acknowledged: {t.decisionWarnings}</div>}
                      {t.outstandingInvoices > 0 && <div className="text-[11px] text-amber-700">Due at approval: {money(t.outstandingAmount)} on {t.outstandingInvoices} invoice(s){t.outstandingInvoiceRefs ? ` (${t.outstandingInvoiceRefs})` : ''}</div>}
                    </td>
                    <td className="px-6 py-4"><span className={`text-[9px] font-bold px-2 py-1 rounded border uppercase ${STATUS_BADGE[t.status]}`}>{t.status}</span></td>
                    <td className="px-6 py-4 text-right whitespace-nowrap space-x-3">
                      {t.status === 'PENDING' && !perm.canDecide && <span className="text-[11px] text-slate-400">Awaiting manager</span>}
                      {t.status === 'PENDING' && perm.canDecide && (<>
                        <button disabled={busyId === t.id} onClick={() => openReview(t)} className="text-green-600 hover:text-green-800 font-bold disabled:opacity-50">Review &amp; approve</button>
                        <button disabled={busyId === t.id} onClick={() => reject(t)} className="text-red-600 hover:text-red-800 font-bold disabled:opacity-50">Reject</button>
                      </>)}
                    </td>
                  </tr>
                ))}
            </tbody>
          </table>
        </div>
      </div>

      {review && (
        <div className="fixed inset-0 bg-slate-900/40 backdrop-blur-sm z-[100] flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl shadow-2xl w-full max-w-lg overflow-hidden">
            <div className="p-6 border-b border-slate-100 bg-[#f8f9fc]">
              <h2 className="text-lg font-bold text-slate-800">Review transfer</h2>
              <p className="text-[13px] text-slate-500 mt-1"><b>{review.transfer.patientName}</b>: {review.transfer.fromBranchName} → <b>{review.transfer.toBranchName}</b></p>
            </div>
            <div className="p-6 space-y-4 max-h-[70vh] overflow-y-auto">
              {reviewError && <div className="bg-red-50 border border-red-100 text-red-700 text-sm rounded-lg p-3">{reviewError}</div>}
              <div className="grid grid-cols-3 gap-3 text-center text-[12px]">
                <div className="bg-slate-50 rounded-lg p-3"><div className="text-lg font-bold text-slate-800">{v.openAppointments}</div>Open / overdue visits</div>
                <div className="bg-slate-50 rounded-lg p-3"><div className="text-lg font-bold text-slate-800">{v.futureAppointments}</div>Upcoming visits</div>
                <div className="bg-slate-50 rounded-lg p-3"><div className="text-lg font-bold text-slate-800">{v.outstandingInvoices}</div>Invoices due{v.outstandingInvoices > 0 && <div className="text-[11px] font-normal">{money(v.outstandingAmount)}</div>}</div>
              </div>
              {v.blockers.length > 0 && (
                <div className="bg-red-50 border border-red-200 rounded-lg p-3 text-sm text-red-800">
                  <div className="font-bold mb-1">Cannot be approved yet</div>
                  <ul className="list-disc pl-5 space-y-1">{v.blockers.map((b) => <li key={b.code}>{b.message}</li>)}</ul>
                </div>
              )}
              {v.warnings.length > 0 && (
                <div className="bg-amber-50 border border-amber-200 rounded-lg p-3 text-sm text-amber-800">
                  <div className="font-bold mb-1">Warnings</div>
                  <ul className="list-disc pl-5 space-y-1">{v.warnings.map((w) => <li key={w.code}>{w.message}</li>)}</ul>
                </div>
              )}
              {v.dueInvoices && v.dueInvoices.length > 0 && (
                <div>
                  <div className="text-[11px] font-bold text-slate-500 uppercase tracking-wider mb-1.5">Unpaid &amp; partly paid invoices · {v.outstandingRule === 'BLOCK' ? 'must be settled before a transfer' : 'stay with the issuing branch'}</div>
                  <div className="border border-slate-100 rounded-lg overflow-x-auto">
                    <table className="w-full text-left text-[12px] text-slate-600">
                      <thead className="bg-[#f8f9fc] text-[10px] uppercase font-bold text-slate-400">
                        <tr><th className="px-3 py-2">Invoice</th><th className="px-3 py-2">Branch</th><th className="px-3 py-2 text-right">Total</th><th className="px-3 py-2 text-right">Paid</th><th className="px-3 py-2 text-right">Due</th></tr>
                      </thead>
                      <tbody className="divide-y divide-slate-100">
                        {v.dueInvoices.map((d) => (
                          <tr key={d.id}>
                            <td className="px-3 py-2">{d.invoiceNumber}<div className="text-[10px] text-slate-400">{fmtDate(d.billDate)} · {d.status}</div></td>
                            <td className="px-3 py-2">{d.branchName || d.branchId}</td>
                            <td className="px-3 py-2 text-right">{money(d.totalAmount)}</td>
                            <td className="px-3 py-2 text-right">{money(d.amountPaid)}</td>
                            <td className="px-3 py-2 text-right font-bold text-slate-800">{money(d.balanceDue)}</td>
                          </tr>
                        ))}
                      </tbody>
                      <tfoot>
                        <tr className="bg-slate-50 font-bold text-slate-800"><td className="px-3 py-2" colSpan="4">Total due{v.dueInvoicesNotShown > 0 ? ` (incl. ${v.dueInvoicesNotShown} more not listed)` : ''}</td><td className="px-3 py-2 text-right">{money(v.outstandingAmount)}</td></tr>
                      </tfoot>
                    </table>
                  </div>
                </div>
              )}
              {v.blockers.length === 0 && v.warnings.length === 0 && <div className="bg-green-50 border border-green-100 rounded-lg p-3 text-sm text-green-800">No open visits or unpaid invoices. Ready to approve.</div>}
              <p className="text-[12px] text-slate-500">Past visits, invoices and payments stay with the branch that created them, and that branch remains responsible for collecting what is due. Only the patient’s current branch changes.</p>
              {v.requiresAcknowledgement && v.canApprove && (
                <label className="flex items-start gap-2 text-sm text-slate-700">
                  <input type="checkbox" checked={ack} onChange={(e) => setAck(e.target.checked)} className="mt-1" />
                  <span>I have reviewed these warnings and accept them.</span>
                </label>
              )}
              <div>
                <label className="block text-[11px] font-bold text-slate-500 uppercase tracking-wider mb-1.5">Decision note {v.requiresAcknowledgement ? '*' : '(optional)'}</label>
                <textarea rows={2} maxLength={500} value={note} onChange={(e) => setNote(e.target.value)} className="w-full bg-[#f4f7fe] border border-transparent rounded-lg px-4 py-2.5 text-[13px] focus:outline-none focus:border-[#2563eb] focus:bg-white" />
              </div>
            </div>
            <div className="p-4 border-t border-slate-100 flex justify-end gap-3">
              <button onClick={() => setReview(null)} className="px-5 py-2.5 rounded-lg text-[13px] font-bold text-slate-600 hover:bg-slate-100">Close</button>
              <button onClick={approve} disabled={approveDisabled} className="bg-green-600 hover:bg-green-700 text-white px-6 py-2.5 rounded-lg text-[13px] font-bold disabled:opacity-40">{busyId != null ? 'Approving…' : 'Approve transfer'}</button>
            </div>
          </div>
        </div>
      )}
    </StaffLayout>
  );
}
