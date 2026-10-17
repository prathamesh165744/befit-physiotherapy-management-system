import React, { useEffect, useState } from 'react';
import axios, { errorMessage } from '../../api';
import { money, fmtDateTime, STATUS_BADGE } from '../../utils/format';

const round2 = (x) => Math.round((x + Number.EPSILON) * 100) / 100;

/** Same formula as the server (BillCalculator). Only for the live preview - the server recomputes and is the authority. */
function calc(items, discount, taxPercent) {
  const subtotal = round2(items.reduce((s, i) => s + (parseInt(i.quantity, 10) || 0) * (parseFloat(i.unitPrice) || 0), 0));
  const disc = round2(parseFloat(discount) || 0);
  const pct = parseFloat(taxPercent) || 0;
  const tax = round2((subtotal - disc) * pct / 100);
  return { subtotal, disc, pct, tax, total: round2(subtotal - disc + tax) };
}

function validate(form, paid) {
  if (!form.items.length) return 'Add at least one line item.';
  for (let i = 0; i < form.items.length; i++) {
    const it = form.items[i];
    const n = `Line ${i + 1}: `;
    if (!it.description.trim()) return n + 'description is required.';
    if (!Number.isInteger(Number(it.quantity)) || Number(it.quantity) < 1) return n + 'quantity must be a whole number, 1 or more.';
    if (it.unitPrice === '' || Number(it.unitPrice) < 0) return n + 'price cannot be negative.';
  }
  const c = calc(form.items, form.discount, form.taxPercent);
  if (c.disc < 0) return 'Discount cannot be negative.';
  if (c.disc > c.subtotal) return 'Discount cannot be more than the subtotal.';
  if (c.pct < 0 || c.pct > 100) return 'Tax must be between 0% and 100%.';
  if (c.total <= 0) return 'The invoice total must be greater than zero.';
  if (c.total < paid) return `The new total (${money(c.total)}) is lower than the amount already paid (${money(paid)}).`;
  if (!form.reason.trim()) return 'Please give a reason for the change.';
  return '';
}

/** Edit an invoice (items, discount, tax) - with a required reason, and the full change history underneath. */
export default function EditBillModal({ billId, onClose, onSaved }) {
  const [detail, setDetail] = useState(null);
  const [form, setForm] = useState(null);
  const [audit, setAudit] = useState([]);
  const [showAudit, setShowAudit] = useState(false);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const [cancelReason, setCancelReason] = useState('');
  const [showCancel, setShowCancel] = useState(false);

  const load = async () => {
    try {
      const res = await axios.get(`/api/billing/${billId}`);
      const d = res.data;
      setDetail(d);
      setForm({
        description: d.bill.description || '',
        items: d.items.map((i) => ({ description: i.description, quantity: String(i.quantity), unitPrice: String(i.unitPrice) })),
        discount: String(d.bill.discountAmount ?? 0),
        taxPercent: String(d.bill.taxPercent ?? 0),
        reason: '',
      });
      const a = await axios.get(`/api/billing/${billId}/audit`);
      setAudit(a.data);
    } catch (err) {
      setError(await errorMessage(err, 'Could not load this invoice.'));
    }
  };

  // eslint-disable-next-line react-hooks/exhaustive-deps
  useEffect(() => { load(); }, [billId]);

  const setItem = (idx, patch) => setForm((f) => ({ ...f, items: f.items.map((it, i) => (i === idx ? { ...it, ...patch } : it)) }));
  const addItem = () => setForm((f) => ({ ...f, items: [...f.items, { description: '', quantity: '1', unitPrice: '' }] }));
  const removeItem = (idx) => setForm((f) => ({ ...f, items: f.items.filter((_, i) => i !== idx) }));

  const save = async () => {
    const paid = Number(detail.bill.amountPaid);
    const problem = validate(form, paid);
    if (problem) return setError(problem);
    setBusy(true); setError('');
    try {
      await axios.put(`/api/billing/${billId}`, {
        items: form.items.map((i) => ({ description: i.description.trim(), quantity: parseInt(i.quantity, 10), unitPrice: parseFloat(i.unitPrice) })),
        discountAmount: parseFloat(form.discount) || 0,
        taxPercent: parseFloat(form.taxPercent) || 0,
        description: form.description,
        reason: form.reason.trim(),
        expectedRevision: detail.bill.revision,
      });
      onSaved();
    } catch (err) {
      setError(await errorMessage(err, 'Could not save the changes.'));
      setBusy(false);
    }
  };

  const cancelInvoice = async () => {
    if (!cancelReason.trim()) return setError('Please give a reason for cancelling.');
    if (!window.confirm('Cancel this invoice? It stays in the records, marked CANCELLED, and cannot be edited again.')) return;
    setBusy(true); setError('');
    try {
      await axios.post(`/api/billing/${billId}/cancel`, { reason: cancelReason.trim() });
      onSaved();
    } catch (err) {
      setError(await errorMessage(err, 'Could not cancel the invoice.'));
      setBusy(false);
    }
  };

  const input = 'w-full bg-[#f4f7fe] border border-transparent rounded-lg px-3 py-2 text-[13px] outline-none focus:border-[#2563eb] focus:bg-white disabled:opacity-60';
  const label = 'block text-[11px] font-bold text-slate-500 uppercase tracking-wider mb-1';

  const shell = (body) => (
    <div className="fixed inset-0 bg-slate-900/40 backdrop-blur-sm z-[100] flex items-center justify-center p-4">
      <div className="bg-white rounded-2xl shadow-2xl w-full max-w-3xl max-h-[92vh] overflow-hidden flex flex-col">{body}</div>
    </div>
  );

  if (!detail || !form) {
    return shell(
      <div className="p-10 text-center">
        {error ? <p className="text-red-600 font-semibold mb-4">{error}</p> : <p className="text-slate-500">Loading invoice…</p>}
        <button onClick={onClose} className="px-5 py-2 rounded-lg text-[13px] font-bold text-slate-600 bg-slate-100 hover:bg-slate-200">Close</button>
      </div>
    );
  }

  const { bill } = detail;
  const locked = !detail.canEdit;
  const c = calc(form.items, form.discount, form.taxPercent);
  const correction = bill.status === 'PAID' && detail.canEdit;

  return shell(
    <>
      <div className="flex justify-between items-center p-5 border-b border-slate-100 bg-[#f8f9fc]">
        <div>
          <h2 className="text-lg font-bold text-slate-800">Edit Invoice {bill.invoiceNumber}</h2>
          <p className="text-xs text-slate-500">{bill.patientName} • {bill.branchName} • revision {bill.revision}</p>
        </div>
        <div className="flex items-center gap-3">
          <span className={`px-2.5 py-1 rounded-full text-[10px] font-bold uppercase border ${STATUS_BADGE[bill.status]}`}>{bill.status}</span>
          <button onClick={onClose} className="text-slate-400 hover:text-red-500">✕</button>
        </div>
      </div>

      <div className="p-6 overflow-y-auto space-y-5">
        {error && <div className="bg-red-50 border border-red-100 text-red-700 text-sm rounded-lg p-3">{error}</div>}
        {locked && <div className="bg-amber-50 border border-amber-200 text-amber-800 text-sm rounded-lg p-3">🔒 {detail.lockReason}</div>}
        {correction && (
          <div className="bg-amber-50 border border-amber-200 text-amber-800 text-sm rounded-lg p-3">
            This invoice is fully paid. As an administrator you can apply a <b>controlled correction</b>: the change is recorded in the history with your name and reason. Payments are never removed.
          </div>
        )}

        <div>
          <label className={label}>Description</label>
          <input className={input} disabled={locked} value={form.description} maxLength={255} onChange={(e) => setForm({ ...form, description: e.target.value })} />
        </div>

        <div>
          <div className="flex justify-between items-center mb-1">
            <label className={label + ' mb-0'}>Line items</label>
            {!locked && <button type="button" onClick={addItem} className="text-[12px] font-bold text-[#2563eb] hover:underline">+ Add line</button>}
          </div>
          <div className="space-y-2">
            {form.items.map((it, idx) => (
              <div key={idx} className="grid grid-cols-12 gap-2 items-center">
                <input className={input + ' col-span-6'} disabled={locked} placeholder="Service / item" value={it.description} maxLength={255} onChange={(e) => setItem(idx, { description: e.target.value })} />
                <input className={input + ' col-span-2'} disabled={locked} type="number" min="1" step="1" placeholder="Qty" value={it.quantity} onChange={(e) => setItem(idx, { quantity: e.target.value })} />
                <input className={input + ' col-span-3'} disabled={locked} type="number" min="0" step="0.01" placeholder="Price" value={it.unitPrice} onChange={(e) => setItem(idx, { unitPrice: e.target.value })} />
                {!locked && form.items.length > 1
                  ? <button type="button" onClick={() => removeItem(idx)} className="col-span-1 text-slate-400 hover:text-red-500">🗑</button>
                  : <span className="col-span-1" />}
              </div>
            ))}
          </div>
        </div>

        <div className="grid grid-cols-2 gap-4">
          <div><label className={label}>Discount (₹)</label><input className={input} disabled={locked} type="number" min="0" step="0.01" value={form.discount} onChange={(e) => setForm({ ...form, discount: e.target.value })} /></div>
          <div><label className={label}>Tax (%)</label><input className={input} disabled={locked} type="number" min="0" max="100" step="0.01" value={form.taxPercent} onChange={(e) => setForm({ ...form, taxPercent: e.target.value })} /></div>
        </div>

        <div className="bg-slate-50 border border-slate-100 rounded-xl p-4 text-sm space-y-1.5">
          <div className="flex justify-between"><span className="text-slate-500">Subtotal</span><span>{money(c.subtotal)}</span></div>
          <div className="flex justify-between"><span className="text-slate-500">Discount</span><span>− {money(c.disc)}</span></div>
          <div className="flex justify-between"><span className="text-slate-500">Tax ({c.pct}%)</span><span>{money(c.tax)}</span></div>
          <div className="flex justify-between font-bold text-base border-t border-slate-200 pt-2"><span>New total</span><span>{money(c.total)}</span></div>
          <div className="flex justify-between"><span className="text-slate-500">Already paid</span><span>{money(bill.amountPaid)}</span></div>
          <div className="flex justify-between font-semibold"><span className="text-slate-500">New balance due</span><span className={c.total - Number(bill.amountPaid) > 0 ? 'text-red-600' : 'text-green-600'}>{money(Math.max(c.total - Number(bill.amountPaid), 0))}</span></div>
        </div>

        {!locked && (
          <div>
            <label className={label}>Reason for this change *</label>
            <input className={input} value={form.reason} maxLength={500} placeholder="e.g. Corrected session count" onChange={(e) => setForm({ ...form, reason: e.target.value })} />
          </div>
        )}

        <div className="border-t border-slate-100 pt-3">
          <button type="button" onClick={() => setShowAudit(!showAudit)} className="text-[12px] font-bold text-slate-600 hover:text-[#2563eb]">
            {showAudit ? '▼' : '▶'} Change history ({audit.length})
          </button>
          {showAudit && (
            <ul className="mt-2 space-y-2">
              {audit.map((a) => (
                <li key={a.id} className="text-xs bg-slate-50 border border-slate-100 rounded-lg p-2.5">
                  <div className="flex justify-between"><b>{a.action.replace('_', ' ')}</b><span className="text-slate-500">{fmtDateTime(a.changedAt)}</span></div>
                  <div className="text-slate-600">by {a.changedBy}{a.reason ? ` — “${a.reason}”` : ''}</div>
                  {a.previousValues && (
                    <details className="mt-1"><summary className="cursor-pointer text-slate-500">before / after</summary>
                      <pre className="whitespace-pre-wrap break-all text-[10px] text-slate-600 mt-1">before: {a.previousValues}{'\n'}after:  {a.newValues}</pre>
                    </details>
                  )}
                </li>
              ))}
            </ul>
          )}
        </div>

        {detail.canCancel && (
          <div className="border-t border-slate-100 pt-3">
            <button type="button" onClick={() => setShowCancel(!showCancel)} className="text-[12px] font-bold text-red-600 hover:underline">Cancel this invoice…</button>
            {showCancel && (
              <div className="mt-2 flex gap-2">
                <input className={input} placeholder="Reason for cancelling (required)" value={cancelReason} maxLength={500} onChange={(e) => setCancelReason(e.target.value)} />
                <button type="button" disabled={busy} onClick={cancelInvoice} className="bg-red-600 hover:bg-red-700 text-white px-4 rounded-lg text-[13px] font-bold disabled:opacity-50 whitespace-nowrap">Cancel invoice</button>
              </div>
            )}
          </div>
        )}
      </div>

      <div className="p-4 bg-slate-50 border-t border-slate-100 flex justify-end gap-3">
        <button onClick={onClose} className="px-5 py-2.5 rounded-lg text-[13px] font-bold text-slate-600 hover:bg-slate-100">{locked ? 'Close' : 'Cancel'}</button>
        {!locked && (
          <button onClick={save} disabled={busy} className="bg-[#2563eb] hover:bg-blue-700 text-white px-6 py-2.5 rounded-lg text-[13px] font-bold shadow-sm disabled:opacity-50">
            {busy ? 'Saving…' : 'Save changes'}
          </button>
        )}
      </div>
    </>
  );
}
