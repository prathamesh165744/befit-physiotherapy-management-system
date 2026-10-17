import React, { useState, useEffect } from 'react';
import axios, { errorMessage } from '../../api';
import InvoicePreviewModal from '../billing/InvoicePreviewModal';
import { PAYMENT_METHODS, METHOD_LABEL, newKey, money } from '../../utils/format';

const GenerateBillModal = ({ isOpen, onClose, appointment, onSuccess }) => {
  const [amount, setAmount] = useState('');
  const [description, setDescription] = useState('');
  const [method, setMethod] = useState('CASH');
  const [payNow, setPayNow] = useState(true);
  const [step, setStep] = useState(1); // 1 = Input, 2 = Receipt
  const [billDetails, setBillDetails] = useState(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [key, setKey] = useState(newKey);
  const [showInvoice, setShowInvoice] = useState(false);

  useEffect(() => {
    if (isOpen && appointment) {
      setStep(1); setBillDetails(null); setError(''); setPayNow(true); setMethod('CASH'); setKey(newKey());
      setDescription(`${appointment.case || 'Consultation'} - Treatment`);
      setAmount(appointment.case?.includes('Rehab') ? '1200' : '600');
    }
  }, [isOpen, appointment]);

  if (!isOpen || !appointment) return null;

  const handleGenerate = async (e) => {
    e.preventDefault();
    const value = parseFloat(amount);
    if (!(value > 0)) return setError('Enter an amount greater than zero.');
    setIsSubmitting(true); setError('');
    try {
      const res = await axios.post(`/api/billing/appointment/${appointment.id}`, {
        amount: value, description, payNow, paymentMethod: payNow ? method : null, idempotencyKey: key,
      });
      setBillDetails(res.data);
      setStep(2);
    } catch (err) {
      setError(await errorMessage(err, 'Failed to generate the bill.'));
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleCloseReceipt = () => {
    onSuccess(); // Refresh the dashboard now
    onClose();   // Close the modal
  };

  const bill = billDetails?.bill;
  const input = 'w-full bg-[#f4f7fe] border border-transparent rounded-lg px-4 py-2.5 text-[13px] outline-none focus:border-[#2563eb] focus:bg-white';
  const label = 'block text-[11px] font-bold text-slate-500 uppercase tracking-wider mb-1.5';

  return (
    <div className="fixed inset-0 bg-slate-900/40 backdrop-blur-sm z-[100] flex items-center justify-center p-4">
      <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md overflow-hidden flex flex-col transform transition-all">
        {step === 1 ? (
          <>
            <div className="flex justify-between items-center p-6 border-b border-slate-100 bg-[#f8f9fc]">
              <h2 className="text-lg font-bold text-slate-800">Generate Invoice</h2>
              <button onClick={onClose} className="text-slate-400 hover:text-red-500">✕</button>
            </div>
            <form onSubmit={handleGenerate} className="p-6 space-y-5">
              {error && <div className="bg-red-50 border border-red-100 text-red-700 text-sm rounded-lg p-3">{error}</div>}
              <div className="bg-blue-50 p-4 rounded-lg border border-blue-100 mb-2">
                <p className="text-xs font-bold text-blue-800 uppercase tracking-wider mb-1">Patient</p>
                <p className="text-sm font-semibold text-slate-800">{appointment.name}</p>
              </div>
              <div>
                <label className={label}>Description</label>
                <input required maxLength={255} type="text" className={input} value={description} onChange={e => setDescription(e.target.value)} />
              </div>
              <div>
                <label className={label}>Amount (₹)</label>
                <input required type="number" min="0.01" step="0.01" className={input} value={amount} onChange={e => setAmount(e.target.value)} />
              </div>
              <div className="flex items-center gap-3">
                <label className="flex items-center gap-2 text-[13px] text-slate-700 font-medium">
                  <input type="checkbox" checked={payNow} onChange={e => setPayNow(e.target.checked)} /> Collect payment now
                </label>
                {payNow && (
                  <select className={input + ' !w-auto'} value={method} onChange={e => setMethod(e.target.value)}>
                    {PAYMENT_METHODS.map(m => <option key={m} value={m}>{METHOD_LABEL[m]}</option>)}
                  </select>
                )}
              </div>
              <div className="pt-2 flex justify-end gap-3 mt-4">
                <button type="button" onClick={onClose} className="px-5 py-2.5 rounded-lg text-[13px] font-bold text-slate-600 hover:bg-slate-100">Cancel</button>
                <button type="submit" disabled={isSubmitting} className="bg-green-600 hover:bg-green-700 text-white px-6 py-2.5 rounded-lg text-[13px] font-bold shadow-sm disabled:opacity-50">
                  {isSubmitting ? 'Generating...' : payNow ? 'Confirm & Pay' : 'Generate Invoice'}
                </button>
              </div>
            </form>
          </>
        ) : (
          <div className="p-8 text-center flex flex-col items-center">
            <div className="w-16 h-16 bg-green-100 text-green-600 rounded-full flex items-center justify-center text-3xl mb-4">✓</div>
            <h2 className="text-2xl font-bold text-slate-800 mb-1">{bill?.status === 'PAID' ? 'Payment Successful' : 'Invoice Generated'}</h2>
            <p className="text-sm text-slate-500 mb-6">Invoice {bill?.invoiceNumber} for {bill?.patientName}</p>
            <div className="w-full bg-slate-50 p-4 rounded-lg border border-slate-100 text-left mb-6">
              <div className="flex justify-between text-sm mb-2"><span className="text-slate-500">Service:</span> <span className="font-semibold text-slate-800">{bill?.description}</span></div>
              <div className="flex justify-between text-sm mb-2"><span className="text-slate-500">Date:</span> <span className="font-semibold text-slate-800">{new Date().toLocaleDateString()}</span></div>
              <div className="flex justify-between text-lg font-bold border-t border-slate-200 pt-2 mt-2">
                <span className="text-slate-800">{bill?.status === 'PAID' ? 'Total Paid:' : 'Amount Due:'}</span>
                <span className="text-green-600">{money(bill?.status === 'PAID' ? bill?.totalAmount : bill?.balanceDue)}</span>
              </div>
            </div>
            <div className="flex gap-3 w-full">
              <button onClick={() => setShowInvoice(true)} className="flex-1 bg-[#2563eb] hover:bg-blue-700 text-white py-2.5 rounded-lg text-[13px] font-bold">View / Print Invoice</button>
              <button onClick={handleCloseReceipt} className="flex-1 bg-slate-100 hover:bg-slate-200 text-slate-700 py-2.5 rounded-lg text-[13px] font-bold">Close</button>
            </div>
          </div>
        )}
      </div>
      {showInvoice && bill && <InvoicePreviewModal billId={bill.id} onClose={() => setShowInvoice(false)} />}
    </div>
  );
};

export default GenerateBillModal;
