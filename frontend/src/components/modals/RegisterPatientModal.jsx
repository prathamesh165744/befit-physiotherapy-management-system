import React, { useState, useEffect } from 'react';
import axios, { errorMessage, branchForCreate } from '../../api';
import CrossBranchPatientSearchModal from './CrossBranchPatientSearchModal';

const EMPTY = { fullName: '', phone: '', gender: 'M', dateOfBirth: '', emergencyContact: '' };

const RegisterPatientModal = ({ isOpen, onClose, onSuccess, initialValues }) => {
  const [formData, setFormData] = useState({ ...EMPTY, ...(initialValues || {}) });
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [duplicate, setDuplicate] = useState(false);
  const [searchOpen, setSearchOpen] = useState(false);
  const canSearch = ['ADMIN', 'RECEPTIONIST'].includes(localStorage.getItem('role'));

  // Pre-fill when opened from the search modal ("no match - register new patient")
  useEffect(() => {
    if (isOpen) { setFormData({ ...EMPTY, ...(initialValues || {}) }); setError(''); setDuplicate(false); }
  }, [isOpen, initialValues]);

  if (!isOpen) return null;

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(''); setDuplicate(false);
    if (formData.phone.replace(/\D/g, '').length < 7) { setError('Enter a valid phone number.'); return; }
    if (!formData.fullName.trim()) { setError('Full name is required.'); return; }
    setIsSubmitting(true);

    const payload = { ...formData, branchId: branchForCreate() };
    if (payload.dateOfBirth === '') payload.dateOfBirth = null;
    if (payload.emergencyContact === '') payload.emergencyContact = null;

    try {
      // The patient is registered at the receptionist's own branch - decided by the server, not by this form.
      await axios.post('/api/patients', payload);
      alert("Patient registered successfully!");
      setFormData({ ...EMPTY });
      onSuccess();
    } catch (err) {
      // 409 DUPLICATE_PATIENT: the server found this person already registered (possibly at another branch)
      if (err.response?.status === 409 && err.response?.data?.code === 'DUPLICATE_PATIENT') setDuplicate(true);
      setError(await errorMessage(err, 'Failed to register patient.'));
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-slate-900/40 backdrop-blur-sm z-[100] flex items-center justify-center p-4">
      <div className="bg-white rounded-2xl shadow-2xl w-full max-w-lg overflow-hidden flex flex-col transform transition-all">
        <div className="flex justify-between items-center p-6 border-b border-slate-100 bg-[#f8f9fc]">
          <h2 className="text-lg font-bold text-slate-800">Register New Patient</h2>
          <button onClick={onClose} className="text-slate-400 hover:text-red-500 transition">
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12"></path></svg>
          </button>
        </div>
        
        <form onSubmit={handleSubmit} className="p-6 space-y-5">
          {canSearch && (
            <div className="flex items-center justify-between gap-3 bg-blue-50 border border-blue-100 rounded-lg px-4 py-2.5 text-[12px] text-blue-800">
              <span>Is this patient already registered at another branch?</span>
              <button type="button" onClick={() => setSearchOpen(true)} className="font-bold underline whitespace-nowrap">Search all branches</button>
            </div>
          )}
          {error && (
            <div className={`rounded-lg p-3 text-[13px] border ${duplicate ? 'bg-amber-50 border-amber-100 text-amber-800' : 'bg-red-50 border-red-100 text-red-700'}`}>
              <p>{error}</p>
              {duplicate && canSearch && (
                <button type="button" onClick={() => setSearchOpen(true)} className="mt-2 font-bold underline">Find the existing patient</button>
              )}
            </div>
          )}
          <div>
            <label className="block text-[11px] font-bold text-slate-500 uppercase tracking-wider mb-1.5">Full Name *</label>
            <input required type="text" className="w-full bg-[#f4f7fe] border border-transparent rounded-lg px-4 py-2.5 text-[13px] focus:outline-none focus:border-[#2563eb] focus:bg-white transition"
              value={formData.fullName} onChange={e => setFormData({...formData, fullName: e.target.value})} placeholder="e.g. Rahul Kulkarni" />
          </div>
          
          <div className="grid grid-cols-2 gap-5">
            <div>
              <label className="block text-[11px] font-bold text-slate-500 uppercase tracking-wider mb-1.5">Phone Number *</label>
              <input required type="tel" className="w-full bg-[#f4f7fe] border border-transparent rounded-lg px-4 py-2.5 text-[13px] focus:outline-none focus:border-[#2563eb] focus:bg-white transition"
                value={formData.phone} onChange={e => setFormData({...formData, phone: e.target.value})} placeholder="+91 98765 43210" />
            </div>
            <div>
              <label className="block text-[11px] font-bold text-slate-500 uppercase tracking-wider mb-1.5">Date of Birth</label>
              <input type="date" className="w-full bg-[#f4f7fe] border border-transparent rounded-lg px-4 py-2.5 text-[13px] focus:outline-none focus:border-[#2563eb] focus:bg-white transition text-slate-700"
                value={formData.dateOfBirth} onChange={e => setFormData({...formData, dateOfBirth: e.target.value})} />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-5">
            <div>
              <label className="block text-[11px] font-bold text-slate-500 uppercase tracking-wider mb-1.5">Gender</label>
              <select className="w-full bg-[#f4f7fe] border border-transparent rounded-lg px-4 py-2.5 text-[13px] focus:outline-none focus:border-[#2563eb] focus:bg-white transition"
                value={formData.gender} onChange={e => setFormData({...formData, gender: e.target.value})}>
                <option value="M">Male</option>
                <option value="F">Female</option>
                <option value="O">Other</option>
              </select>
            </div>
            <div>
              <label className="block text-[11px] font-bold text-slate-500 uppercase tracking-wider mb-1.5">Emergency Contact</label>
              <input type="tel" className="w-full bg-[#f4f7fe] border border-transparent rounded-lg px-4 py-2.5 text-[13px] focus:outline-none focus:border-[#2563eb] focus:bg-white transition"
                value={formData.emergencyContact} onChange={e => setFormData({...formData, emergencyContact: e.target.value})} placeholder="Optional" />
            </div>
          </div>
          
          <div className="pt-2 flex justify-end gap-3 mt-4">
            <button type="button" onClick={onClose} className="px-5 py-2.5 rounded-lg text-[13px] font-bold text-slate-600 hover:bg-slate-100 transition">Cancel</button>
            <button type="submit" disabled={isSubmitting} className="bg-[#2563eb] hover:bg-blue-700 text-white px-6 py-2.5 rounded-lg text-[13px] font-bold shadow-sm transition disabled:opacity-50">
              {isSubmitting ? 'Saving...' : 'Register Patient'}
            </button>
          </div>
        </form>
      </div>
      {canSearch && searchOpen && (
        <CrossBranchPatientSearchModal isOpen={searchOpen} onClose={() => setSearchOpen(false)}
          initialValues={{ phone: formData.phone, fullName: formData.fullName, dateOfBirth: formData.dateOfBirth }} />
      )}
    </div>
  );
};
export default RegisterPatientModal;