import React, { useState, useEffect } from 'react';
import axios from 'axios';

const GenerateBillModal = ({ isOpen, onClose, appointment, onSuccess }) => {
  const [amount, setAmount] = useState('');
  const [description, setDescription] = useState('');
  const [step, setStep] = useState(1); // 1 = Input, 2 = Receipt
  const [billDetails, setBillDetails] = useState(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    if (isOpen && appointment) {
      setStep(1); setBillDetails(null);
      setDescription(`${appointment.case || 'Consultation'} - Treatment`);
      setAmount(appointment.case?.includes('Rehab') ? '1200' : '600');
    }
  }, [isOpen, appointment]);

  if (!isOpen || !appointment) return null;

  const handleGenerate = (e) => {
    e.preventDefault();
    setIsSubmitting(true);
    axios.post(`http://localhost:8080/api/billing/appointment/${appointment.id}`, { amount: parseFloat(amount), description })
      .then(res => {
        setBillDetails(res.data);
        setStep(2); // Move to receipt view safely
        setIsSubmitting(false);
      })
      .catch(err => {
        alert("Failed to generate bill.");
        setIsSubmitting(false);
      });
  };

  const handleCloseReceipt = () => {
    onSuccess(); // Refresh the dashboard now
    onClose();   // Close the modal
  };

  return (
    <div className="fixed inset-0 bg-slate-900/40 backdrop-blur-sm z-[100] flex items-center justify-center p-4 print:bg-white print:backdrop-blur-none">
      <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md overflow-hidden flex flex-col transform transition-all print:shadow-none print:max-w-none print:w-full">
        {step === 1 ? (
          <>
            <div className="flex justify-between items-center p-6 border-b border-slate-100 bg-[#f8f9fc]">
              <h2 className="text-lg font-bold text-slate-800">Generate Invoice</h2>
              <button onClick={onClose} className="text-slate-400 hover:text-red-500">✕</button>
            </div>
            <form onSubmit={handleGenerate} className="p-6 space-y-5">
              <div className="bg-blue-50 p-4 rounded-lg border border-blue-100 mb-2">
                <p className="text-xs font-bold text-blue-800 uppercase tracking-wider mb-1">Patient</p>
                <p className="text-sm font-semibold text-slate-800">{appointment.name}</p>
              </div>
              <div>
                <label className="block text-[11px] font-bold text-slate-500 uppercase tracking-wider mb-1.5">Description</label>
                <input required type="text" className="w-full bg-[#f4f7fe] border border-transparent rounded-lg px-4 py-2.5 text-[13px] outline-none focus:border-[#2563eb] focus:bg-white"
                  value={description} onChange={e => setDescription(e.target.value)} />
              </div>
              <div>
                <label className="block text-[11px] font-bold text-slate-500 uppercase tracking-wider mb-1.5">Amount (₹)</label>
                <input required type="number" className="w-full bg-[#f4f7fe] border border-transparent rounded-lg px-4 py-2.5 text-[13px] outline-none focus:border-[#2563eb] focus:bg-white"
                  value={amount} onChange={e => setAmount(e.target.value)} />
              </div>
              <div className="pt-2 flex justify-end gap-3 mt-4">
                <button type="button" onClick={onClose} className="px-5 py-2.5 rounded-lg text-[13px] font-bold text-slate-600 hover:bg-slate-100">Cancel</button>
                <button type="submit" disabled={isSubmitting} className="bg-green-600 hover:bg-green-700 text-white px-6 py-2.5 rounded-lg text-[13px] font-bold shadow-sm disabled:opacity-50">
                  {isSubmitting ? 'Generating...' : 'Confirm & Pay'}
                </button>
              </div>
            </form>
          </>
        ) : (
          <div className="p-8 text-center flex flex-col items-center">
            <div className="w-16 h-16 bg-green-100 text-green-600 rounded-full flex items-center justify-center text-3xl mb-4">✓</div>
            <h2 className="text-2xl font-bold text-slate-800 mb-1">Payment Successful</h2>
            <p className="text-sm text-slate-500 mb-6">Invoice #{billDetails?.id} generated for {billDetails?.patientName}</p>
            <div className="w-full bg-slate-50 p-4 rounded-lg border border-slate-100 text-left mb-6">
              <div className="flex justify-between text-sm mb-2"><span className="text-slate-500">Service:</span> <span className="font-semibold text-slate-800">{billDetails?.description}</span></div>
              <div className="flex justify-between text-sm mb-2"><span className="text-slate-500">Date:</span> <span className="font-semibold text-slate-800">{new Date().toLocaleDateString()}</span></div>
              <div className="flex justify-between text-lg font-bold border-t border-slate-200 pt-2 mt-2"><span className="text-slate-800">Total Paid:</span> <span className="text-green-600">₹{billDetails?.amount}</span></div>
            </div>
            <div className="flex gap-3 w-full print:hidden">
              <button onClick={() => window.print()} className="flex-1 bg-[#2563eb] hover:bg-blue-700 text-white py-2.5 rounded-lg text-[13px] font-bold">Print Receipt</button>
              <button onClick={handleCloseReceipt} className="flex-1 bg-slate-100 hover:bg-slate-200 text-slate-700 py-2.5 rounded-lg text-[13px] font-bold">Close</button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default GenerateBillModal;