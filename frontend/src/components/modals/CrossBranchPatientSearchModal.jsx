import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import axios, { errorMessage } from '../../api';

const MODES = [
  { key: 'phone', label: 'Phone number' },
  { key: 'regId', label: 'REG ID' },
  { key: 'name', label: 'Name + date of birth' },
];

const inputCls = 'w-full bg-[#f4f7fe] border border-transparent rounded-lg px-4 py-2.5 text-[13px] focus:outline-none focus:border-[#2563eb] focus:bg-white transition';
const labelCls = 'block text-[11px] font-bold text-slate-500 uppercase tracking-wider mb-1.5';

/**
 * Cross-branch "is this patient already registered?" lookup for receptionists / admins.
 * Shows only limited identity data returned by GET /api/patients/cross-branch-search.
 * onRegisterNew (optional): called when the staff member confirms there is no match and wants to register a new patient.
 */
const CrossBranchPatientSearchModal = ({ isOpen, onClose, onRegisterNew, initialValues }) => {
  const [mode, setMode] = useState(initialValues?.phone ? 'phone' : initialValues?.fullName && initialValues?.dateOfBirth ? 'name' : 'phone');
  const [phone, setPhone] = useState(initialValues?.phone || '');
  const [regId, setRegId] = useState('');
  const [fullName, setFullName] = useState(initialValues?.fullName || '');
  const [dateOfBirth, setDateOfBirth] = useState(initialValues?.dateOfBirth || '');
  const [results, setResults] = useState(null); // null = not searched yet
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  if (!isOpen) return null;

  const role = localStorage.getItem('role');
  if (role !== 'ADMIN' && role !== 'RECEPTIONIST') return null; // server enforces this too

  const validate = () => {
    if (mode === 'phone') {
      if (phone.replace(/\D/g, '').length < 10) return 'Enter the full phone number (at least 10 digits).';
    } else if (mode === 'regId') {
      if (!/^(REG-)?\d+$/i.test(regId.trim())) return 'Enter a valid REG ID, for example REG-1024.';
    } else {
      if (!fullName.trim()) return 'Enter the patient\'s full name.';
      if (!dateOfBirth) return 'Date of birth is required when searching by name.';
    }
    return '';
  };

  const handleSearch = async (e) => {
    e.preventDefault();
    const problem = validate();
    if (problem) { setError(problem); setResults(null); return; }
    setLoading(true); setError('');
    const params = mode === 'phone' ? { phone: phone.trim() }
      : mode === 'regId' ? { regId: regId.trim() }
      : { fullName: fullName.trim(), dateOfBirth };
    try {
      const res = await axios.get('/api/patients/cross-branch-search', { params });
      setResults(res.data);
    } catch (err) {
      setResults(null);
      setError(await errorMessage(err, 'Search failed. Please try again.'));
    } finally {
      setLoading(false);
    }
  };

  const switchMode = (m) => { setMode(m); setResults(null); setError(''); };

  return (
    <div className="fixed inset-0 bg-slate-900/40 backdrop-blur-sm z-[110] flex items-center justify-center p-4">
      <div className="bg-white rounded-2xl shadow-2xl w-full max-w-xl max-h-[90vh] overflow-hidden flex flex-col">
        <div className="flex justify-between items-center p-6 border-b border-slate-100 bg-[#f8f9fc]">
          <div>
            <h2 className="text-lg font-bold text-slate-800">Find Existing Patient</h2>
            <p className="text-[12px] text-slate-500 mt-0.5">Searches all BeFit branches. Check here before registering a new patient.</p>
          </div>
          <button onClick={onClose} className="text-slate-400 hover:text-red-500 transition" aria-label="Close">
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12"></path></svg>
          </button>
        </div>

        <div className="p-6 overflow-y-auto space-y-5">
          <div className="flex gap-2 flex-wrap">
            {MODES.map((m) => (
              <button key={m.key} type="button" onClick={() => switchMode(m.key)}
                className={`px-3 py-1.5 rounded-full text-[12px] font-bold border transition ${mode === m.key ? 'bg-[#2563eb] text-white border-[#2563eb]' : 'bg-white text-slate-600 border-slate-200 hover:bg-slate-50'}`}>
                {m.label}
              </button>
            ))}
          </div>

          <form onSubmit={handleSearch} className="space-y-4">
            {mode === 'phone' && (
              <div>
                <label className={labelCls}>Phone number</label>
                <input type="tel" className={inputCls} value={phone} onChange={(e) => setPhone(e.target.value)} placeholder="+91 98765 43210" />
              </div>
            )}
            {mode === 'regId' && (
              <div>
                <label className={labelCls}>REG ID</label>
                <input type="text" className={inputCls} value={regId} onChange={(e) => setRegId(e.target.value)} placeholder="REG-1024" />
              </div>
            )}
            {mode === 'name' && (
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className={labelCls}>Full name</label>
                  <input type="text" className={inputCls} value={fullName} onChange={(e) => setFullName(e.target.value)} placeholder="e.g. Rahul Kulkarni" />
                </div>
                <div>
                  <label className={labelCls}>Date of birth</label>
                  <input type="date" className={inputCls} value={dateOfBirth} max={new Date().toISOString().slice(0, 10)} onChange={(e) => setDateOfBirth(e.target.value)} />
                </div>
              </div>
            )}
            <button type="submit" disabled={loading}
              className="bg-[#2563eb] hover:bg-blue-700 text-white px-6 py-2.5 rounded-lg text-[13px] font-bold shadow-sm transition disabled:opacity-50">
              {loading ? 'Searching...' : 'Search all branches'}
            </button>
          </form>

          {error && <div className="bg-red-50 border border-red-100 text-red-700 text-[13px] rounded-lg p-3">{error}</div>}

          {results && results.length === 0 && (
            <div className="bg-slate-50 border border-slate-100 rounded-xl p-4 text-[13px] text-slate-600">
              <p className="font-bold text-slate-800 mb-1">No matching patient found.</p>
              <p>No patient with these details is registered at any branch.</p>
              {onRegisterNew && (
                <button type="button" onClick={() => onRegisterNew({ phone: mode === 'phone' ? phone.trim() : '', fullName: mode === 'name' ? fullName.trim() : '', dateOfBirth: mode === 'name' ? dateOfBirth : '' })}
                  className="mt-3 bg-white border border-[#2563eb] text-[#2563eb] hover:bg-blue-50 px-4 py-2 rounded-lg text-[12px] font-bold transition">
                  Continue to register new patient
                </button>
              )}
            </div>
          )}

          {results && results.length > 0 && (
            <div className="space-y-3">
              <div className="bg-amber-50 border border-amber-100 text-amber-800 text-[12px] rounded-lg p-3 font-medium">
                {results.length === 1 ? 'This patient is' : 'These patients are'} already registered. Please do not create a duplicate registration.
              </div>
              {results.map((p) => (
                <div key={p.id} className="border border-slate-100 rounded-xl p-4 flex flex-wrap items-center justify-between gap-3">
                  <div className="space-y-0.5">
                    <div className="font-bold text-slate-800">{p.fullName} <span className="text-[#2563eb] text-[12px] ml-1">{p.regId}</span></div>
                    <div className="text-[12px] text-slate-500">
                      {p.maskedPhone || 'No phone'} • {p.gender || 'N/A'} • Born {p.birthYear || 'N/A'}
                    </div>
                    <div className="text-[12px] text-slate-600">
                      Registered at <span className="font-bold">{p.branchName || p.branchId}</span>
                    </div>
                  </div>
                  {p.viewable ? (
                    <Link to={`/staff-dashboard/patients/${p.id}`} onClick={onClose} className="text-blue-600 hover:text-blue-800 font-bold text-xs">Open file ›</Link>
                  ) : (
                    <span className="text-[11px] text-slate-500 max-w-[220px] text-right">
                      Registered at another branch. Ask the patient to request a transfer to your branch (or contact an administrator).
                    </span>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default CrossBranchPatientSearchModal;