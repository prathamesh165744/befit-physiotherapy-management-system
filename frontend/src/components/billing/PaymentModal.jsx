import React, { useState } from 'react';
import axios, { errorMessage } from '../../api';
import { money, PAYMENT_METHODS, METHOD_LABEL, newKey } from '../../utils/format';

/** Record a payment against an invoice. The idempotency key makes a double click / retry harmless. */
export default function PaymentModal({ bill, onClose, onSaved }) {
  const [amount, setAmount] = useState(String(bill.balanceDue));
  const [method, setMethod] = useState('CASH');
  const [reference, setReference] = useState('');
  const [key] = useState(newKey);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');

  const submit = async (e) => {
    e.preventDefault();
    const value = parseFloat(amount);
    if (!(value > 0)) return setError('Enter an amount greater than zero.');
    if (value > Number(bill.balanceDue) + 0.0001) return setError(`The amount cannot be more than the balance (${money(bill.balanceDue)}).`);
    setBusy(true); setError('');
    try {
      await axios.post(`/api/billing/${bill.id}/payments`, { amount: value, method, referenceNo: reference || null, idempotencyKey: key });
      onSaved();
    } catch (err) {
      setError(await errorMessage(err, 'Could not record the payment.'));
      setBusy(false);
    }
  };

  const input = 'w-full bg-[#f4f7fe] border border-transparent rounded-lg px-4 py-2.5 text-[13px] outline-none focus:border-[#2563eb] focus:bg-white';
  const label = 'block text-[11px] font-bold text-slate-500 uppercase tracking-wider mb-1.5';

  return (
    <div className="fixed inset-0 bg-slate-900/40 backdrop-blur-sm z-[100] flex items-center justify-center p-4">
      <form onSubmit={submit} className="bg-white rounded-2xl shadow-2xl w-full max-w-md overflow-hidden">
        <div className="flex justify-between items-center p-5 border-b border-slate-100 bg-[#f8f9fc]">
          <div>
            <h2 className="text-lg font-bold text-slate-800">Record Payment</h2>
            <p className="text-xs text-slate-500">{bill.invoiceNumber} • {bill.patientName}</p>
          </div>
          <button type="button" onClick={onClose} className="text-slate-400 hover:text-red-500">✕</button>
        </div>
        <div className="p-6 space-y-4">
          <div className="bg-blue-50 border border-blue-100 rounded-lg p-3 text-sm flex justify-between">
            <span className="text-slate-600">Outstanding balance</span><b className="text-slate-900">{money(bill.balanceDue)}</b>
          </div>
          {error && <div className="bg-red-50 border border-red-100 text-red-700 text-sm rounded-lg p-3">{error}</div>}
          <div>
            <label className={label}>Amount (₹)</label>
            <input type="number" step="0.01" min="0.01" className={input} value={amount} onChange={(e) => setAmount(e.target.value)} />
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className={label}>Method</label>
              <select className={input} value={method} onChange={(e) => setMethod(e.target.value)}>
                {PAYMENT_METHODS.map((m) => <option key={m} value={m}>{METHOD_LABEL[m]}</option>)}
              </select>
            </div>
            <div>
              <label className={label}>Reference / Txn ID</label>
              <input type="text" maxLength={100} className={input} value={reference} onChange={(e) => setReference(e.target.value)} placeholder="optional" />
            </div>
          </div>
        </div>
        <div className="p-4 bg-slate-50 border-t border-slate-100 flex justify-end gap-3">
          <button type="button" onClick={onClose} className="px-5 py-2.5 rounded-lg text-[13px] font-bold text-slate-600 hover:bg-slate-100">Cancel</button>
          <button type="submit" disabled={busy} className="bg-green-600 hover:bg-green-700 text-white px-6 py-2.5 rounded-lg text-[13px] font-bold disabled:opacity-50">
            {busy ? 'Saving…' : 'Record Payment'}
          </button>
        </div>
      </form>
    </div>
  );
}
