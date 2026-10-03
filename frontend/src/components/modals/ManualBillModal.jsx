import React, { useState } from 'react';
import axios from 'axios';

const ManualBillModal = ({ isOpen, onClose, onSuccess }) => {
  const [formData, setFormData] = useState({ patientName: '', amount: '', description: '' });
  const [isSubmitting, setIsSubmitting] = useState(false);

  if (!isOpen) return null;

  const handleSubmit = (e) => {
    e.preventDefault();
    setIsSubmitting(true);
    axios.post(`http://localhost:8080/api/billing/manual`, formData)
      .then(res => {
        alert("Manual Bill Generated Successfully!");
        setIsSubmitting(false);
        setFormData({ patientName: '', amount: '', description: '' });
        onSuccess(); 
      })
      .catch(err => {
        alert("Failed to generate bill.");
        setIsSubmitting(false);
      });
  };

  return (
    <div className="fixed inset-0 bg-slate-900/40 backdrop-blur-sm z-[100] flex items-center justify-center p-4">
      <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md overflow-hidden flex flex-col transform transition-all">
        <div className="flex justify-between items-center p-6 border-b border-slate-100 bg-[#f8f9fc]">
          <h2 className="text-lg font-bold text-slate-800">Create Manual Bill</h2>
          <button onClick={onClose} className="text-slate-400 hover:text-red-500">✕</button>
        </div>
        <form onSubmit={handleSubmit} className="p-6 space-y-5">
          <div>
            <label className="block text-[11px] font-bold text-slate-500 uppercase tracking-wider mb-1.5">Customer / Patient Name</label>
            <input required type="text" className="w-full bg-[#f4f7fe] border border-transparent rounded-lg px-4 py-2.5 text-[13px] outline-none focus:border-[#2563eb] focus:bg-white"
              value={formData.patientName} onChange={e => setFormData({...formData, patientName: e.target.value})} placeholder="Walk-in Customer" />
          </div>
          <div>
            <label className="block text-[11px] font-bold text-slate-500 uppercase tracking-wider mb-1.5">Item / Service Description</label>
            <input required type="text" className="w-full bg-[#f4f7fe] border border-transparent rounded-lg px-4 py-2.5 text-[13px] outline-none focus:border-[#2563eb] focus:bg-white"
              value={formData.description} onChange={e => setFormData({...formData, description: e.target.value})} placeholder="e.g. Resistance Band" />
          </div>
          <div>
            <label className="block text-[11px] font-bold text-slate-500 uppercase tracking-wider mb-1.5">Amount (₹)</label>
            <input required type="number" className="w-full bg-[#f4f7fe] border border-transparent rounded-lg px-4 py-2.5 text-[13px] outline-none focus:border-[#2563eb] focus:bg-white"
              value={formData.amount} onChange={e => setFormData({...formData, amount: e.target.value})} />
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