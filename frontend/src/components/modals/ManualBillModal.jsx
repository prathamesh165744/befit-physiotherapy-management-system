import React, { useState } from 'react';
import axios, { errorMessage } from '../../api';
import { branchForCreate } from '../../api';
import { PAYMENT_METHODS, METHOD_LABEL, newKey } from '../../utils/format';

const empty = { patientName: '', amount: '', description: '', paymentMethod: 'CASH', payNow: true };

const ManualBillModal = ({ isOpen, onClose, onSuccess }) => {
  const [formData, setFormData] = useState(empty);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [key, setKey] = useState(newKey);

  if (!isOpen) return null;

  const handleSubmit = async (e) => {
    e.preventDefault();
    const amount = parseFloat(formData.amount);
    if (!(amount > 0)) return setError('Enter an amount greater than zero.');
    setIsSubmitting(true); setError('');
    try {
      const res = await axios.post('/api/billing/manual', {
        patientName: formData.patientName.trim(),
        description: formData.description.trim(),
        amount,
        payNow: formData.payNow,
        paymentMethod: formData.payNow ? formData.paymentMethod : null,
        idempotencyKey: key,
        branchId: branchForCreate(),
      });
      setFormData(empty); setKey(newKey());
      onSuccess(res.data);
    } catch (err) {
      setError(await errorMessage(err, 'Failed to generate the bill.'));
    } finally {
      setIsSubmitting(false);
    }
  };

  const input = 'w-full bg-[#f4f7fe] border border-transparent rounded-lg px-4 py-2.5 text-[13px] outline-none focus:border-[#2563eb] focus:bg-white';
  const label = 'block text-[11px] font-bold text-slate-500 uppercase tracking-wider mb-1.5';

  return (
    <div className="fixed inset-0 bg-slate-900/40 backdrop-blur-sm z-[100] flex items-center justify-center p-4">
      <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md overflow-hidden flex flex-col transform transition-all">
        <div className="flex justify-between items-center p-6 border-b border-slate-100 bg-[#f8f9fc]">
          <h2 className="text-lg font-bold text-slate-800">Create Manual Bill</h2>
          <button onClick={onClose} className="text-slate-400 hover:text-red-500">✕</button>
        </div>
        <form onSubmit={handleSubmit} className="p-6 space-y-5">
          {error && <div className="bg-red-50 border border-red-100 text-red-700 text-sm rounded-lg p-3">{error}</div>}
          <div>
            <label className={label}>Customer / Patient Name</label>
            <input required maxLength={150} type="text" className={input} value={formData.patientName}
              onChange={e => setFormData({ ...formData, patientName: e.target.value })} placeholder="Walk-in Customer" />
          </div>
          <div>
            <label className={label}>Item / Service Description</label>
            <input required maxLength={255} type="text" className={input} value={formData.description}
              onChange={e => setFormData({ ...formData, description: e.target.value })} placeholder="e.g. Resistance Band" />
          </div>
          <div>
            <label className={label}>Amount (₹)</label>
            <input required type="number" min="0.01" step="0.01" className={input} value={formData.amount}
              onChange={e => setFormData({ ...formData, amount: e.target.value })} />
          </div>
          <div className="flex items-center gap-3">
            <label className="flex items-center gap-2 text-[13px] text-slate-700 font-medium">
              <input type="checkbox" checked={formData.payNow} onChange={e => setFormData({ ...formData, payNow: e.target.checked })} />
              Payment received now
            </label>
            {formData.payNow && (
              <select className={input + ' !w-auto'} value={formData.paymentMethod} onChange={e => setFormData({ ...formData, paymentMethod: e.target.value })}>
                {PAYMENT_METHODS.map(m => <option key={m} value={m}>{METHOD_LABEL[m]}</option>)}
              </select>
            )}
          </div>
          <div className="pt-2 flex justify-end gap-3 mt-4">
            <button type="button" onClick={onClose} className="px-5 py-2.5 rounded-lg text-[13px] font-bold text-slate-600 hover:bg-slate-100">Cancel</button>
            <button type="submit" disabled={isSubmitting} className="bg-green-600 hover:bg-green-700 text-white px-6 py-2.5 rounded-lg text-[13px] font-bold shadow-sm disabled:opacity-50">
              {isSubmitting ? 'Saving...' : 'Generate Bill'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
export default ManualBillModal;
