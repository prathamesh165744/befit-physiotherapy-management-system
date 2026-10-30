import React, { useState, useEffect } from 'react';
import axios, { errorMessage } from '../api';
import { fmtDate, fmtDateTime, money } from '../utils/format';
import { useNavigate } from 'react-router-dom';
import { TRANSFER_BADGE, TRANSFER_LABEL, APPOINTMENT_LABEL } from '../utils/patientPortal';

const PatientProfile = () => {
  const [profile, setProfile] = useState(null);
  const [isEditing, setIsEditing] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');
  const [successMsg, setSuccessMsg] = useState('');
  const navigate = useNavigate();

  // Form state for allowed update fields
  const [formData, setFormData] = useState({
    fullName: '',
    phone: '',
    emergencyContact: '',
    gender: ''
  });

  // Branch change + history (a patient may ask to move to another branch; old visits/invoices stay at the old branch)
  const [branches, setBranches] = useState([]);
  const [transfers, setTransfers] = useState([]);
  const [history, setHistory] = useState(null);
  const [targetBranch, setTargetBranch] = useState('');
  const [reason, setReason] = useState('');
  const [branchMsg, setBranchMsg] = useState({ type: '', text: '' });
  const [branchBusy, setBranchBusy] = useState(false);
  const [extras, setExtras] = useState({ loading: true, error: '' });   // branches, transfers and visit history load after the profile

  useEffect(() => {
    fetchMyProfile();
  }, []);

  const loadExtras = async () => {
    setExtras({ loading: true, error: '' });
    try {
      const [b, t, h] = await Promise.all([axios.get('/api/branches'), axios.get('/api/patients/me/transfers'), axios.get('/api/patients/me/history')]);
      setBranches(b.data); setTransfers(t.data); setHistory(h.data);
      setExtras({ loading: false, error: '' });
    } catch (err) {
      // the profile itself still works; say what failed and let the patient retry
      setExtras({ loading: false, error: await errorMessage(err, 'We could not load your visits and branch requests.') });
    }
  };

  const fetchMyProfile = async () => {
    setIsLoading(true);
    try {
      const res = await axios.get('/api/patients/me');
      setProfile(res.data);
      setFormData({
        fullName: res.data.fullName || '',
        phone: res.data.phone || '',
        emergencyContact: res.data.emergencyContact || '',
        gender: res.data.gender || ''
      });
      setIsLoading(false);
      loadExtras();
    } catch (err) {
      // A real 401 is redirected to login centrally (api.js). Anything else is shown honestly, without blaming the session.
      setError(await errorMessage(err, 'We could not load your profile. Please try again.'));
      setIsLoading(false);
    }
  };

  const handleUpdate = async (e) => {
    e.preventDefault();
    setError('');
    setSuccessMsg('');
    
    try {
      const res = await axios.put('/api/patients/me', formData);
      setProfile(res.data); // Update UI with confirmed database data
      setIsEditing(false);
      setSuccessMsg('Profile updated successfully!');
      
      // Clear success message after 3 seconds
      setTimeout(() => setSuccessMsg(''), 3000);
    } catch (err) {
      setError(await errorMessage(err, 'Failed to update profile. Please try again.'));
    }
  };

  const requestTransfer = async (e) => {
    e.preventDefault();
    setBranchBusy(true); setBranchMsg({ type: '', text: '' });
    try {
      const res = await axios.put('/api/patients/me/branch', { branchId: targetBranch, reason: reason || null });
      setBranchMsg({ type: 'ok', text: res.data.status === 'APPROVED' ? 'Your branch has been changed.' : 'Request sent. The new branch will review it.' });
      setTargetBranch(''); setReason('');
      fetchMyProfile();
    } catch (err) {
      setBranchMsg({ type: 'error', text: await errorMessage(err, 'Could not request the branch change.') });
    } finally { setBranchBusy(false); }
  };

  const cancelTransfer = async () => {
    setBranchBusy(true); setBranchMsg({ type: '', text: '' });
    try {
      await axios.delete('/api/patients/me/branch-request');
      setBranchMsg({ type: 'ok', text: 'Request cancelled.' });
      fetchMyProfile();
    } catch (err) {
      setBranchMsg({ type: 'error', text: await errorMessage(err, 'Could not cancel the request.') });
    } finally { setBranchBusy(false); }
  };

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  if (isLoading) return <div role="status" className="flex justify-center items-center h-64 text-slate-500 font-bold">Loading your profile…</div>;
  if (error && !profile) return (
    <div className="p-8 text-center">
      <p className="text-red-500 font-bold mb-4">{error}</p>
      <button onClick={fetchMyProfile} className="bg-blue-600 text-white px-5 py-2 rounded-lg text-sm font-bold">Try again</button>
    </div>
  );

  return (
    <div className="bg-[#f8f9fc] p-4 sm:p-8 font-sans">
      <div className="max-w-3xl mx-auto">
        
        {/* Header */}
        <div className="flex flex-wrap justify-between items-end gap-3 mb-8">
          <div>
            <h1 className="text-3xl font-bold text-slate-800">My Medical Profile</h1>
            <p className="text-sm text-slate-500 mt-1">Manage your personal information and contacts</p>
          </div>
          <button onClick={() => navigate('/dashboard')} className="text-blue-600 hover:text-blue-800 text-sm font-bold">
            &larr; Back to Dashboard
          </button>
        </div>

        {/* Success Alert */}
        {successMsg && (
          <div className="bg-green-50 border border-green-200 text-green-700 px-4 py-3 rounded-lg mb-6 font-semibold shadow-sm">
            ✓ {successMsg}
          </div>
        )}
        {error && isEditing && (
          <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg mb-6 font-semibold shadow-sm">
            {error}
          </div>
        )}

        {/* Profile Card */}
        <div className="bg-white rounded-2xl shadow-sm border border-slate-100 overflow-hidden">
          
          <div className="bg-blue-600 px-8 py-6 flex justify-between items-center">
            <div className="flex items-center gap-4">
              <div className="w-16 h-16 bg-white text-blue-600 rounded-full flex items-center justify-center text-2xl font-bold uppercase shadow-md">
                {profile?.fullName ? profile.fullName.charAt(0) : 'P'}
              </div>
              <div>
                <h2 className="text-xl font-bold text-white">{profile?.fullName}</h2>
                <p className="text-blue-100 text-sm font-medium">Patient ID: {profile?.id}</p>
              </div>
            </div>
            {!isEditing && (
              <button onClick={() => setIsEditing(true)} className="bg-white/20 hover:bg-white/30 text-white px-5 py-2 rounded-lg text-sm font-bold transition">
                Edit Profile
              </button>
            )}
          </div>

          <div className="p-8">
            {isEditing ? (
              <form onSubmit={handleUpdate} className="space-y-6">
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
                  <div>
                    <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider mb-2">Full Name</label>
                    <input type="text" name="fullName" value={formData.fullName} onChange={handleChange} required className="w-full bg-[#f4f7fe] border border-transparent rounded-lg px-4 py-3 text-sm outline-none focus:border-blue-500 focus:bg-white transition" />
                  </div>
                  <div>
                    <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider mb-2">Phone Number</label>
                    <input type="tel" name="phone" value={formData.phone} onChange={handleChange} required className="w-full bg-[#f4f7fe] border border-transparent rounded-lg px-4 py-3 text-sm outline-none focus:border-blue-500 focus:bg-white transition" />
                  </div>
                  <div>
                    <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider mb-2">Emergency Contact</label>
                    <input type="tel" name="emergencyContact" value={formData.emergencyContact} onChange={handleChange} required className="w-full bg-[#f4f7fe] border border-transparent rounded-lg px-4 py-3 text-sm outline-none focus:border-blue-500 focus:bg-white transition" />
                  </div>
                  <div>
                    <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider mb-2">Gender</label>
                    <select name="gender" value={formData.gender} onChange={handleChange} className="w-full bg-[#f4f7fe] border border-transparent rounded-lg px-4 py-3 text-sm outline-none focus:border-blue-500 focus:bg-white transition">
                      <option value="">Select...</option>
                      <option value="Male">Male</option>
                      <option value="Female">Female</option>
                      <option value="Other">Other</option>
                    </select>
                  </div>
                </div>
                
                <div className="pt-4 border-t border-slate-100 flex justify-end gap-3">
                  <button type="button" onClick={() => { setIsEditing(false); setError(''); }} className="px-6 py-2.5 rounded-lg text-sm font-bold text-slate-600 hover:bg-slate-100 transition">Cancel</button>
                  <button type="submit" className="bg-blue-600 hover:bg-blue-700 text-white px-6 py-2.5 rounded-lg text-sm font-bold shadow-sm transition">Save Changes</button>
                </div>
              </form>
            ) : (
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-y-8 gap-x-6">
                <div>
                  <p className="text-xs font-bold text-slate-400 uppercase tracking-wider mb-1">Full Name</p>
                  <p className="text-slate-800 font-semibold text-lg">{profile?.fullName}</p>
                </div>
                <div>
                  <p className="text-xs font-bold text-slate-400 uppercase tracking-wider mb-1">Phone Number</p>
                  <p className="text-slate-800 font-semibold text-lg">{profile?.phone || 'Not provided'}</p>
                </div>
                <div>
                  <p className="text-xs font-bold text-slate-400 uppercase tracking-wider mb-1">Emergency Contact</p>
                  <p className="text-slate-800 font-semibold text-lg">{profile?.emergencyContact || 'Not provided'}</p>
                </div>
                <div>
                  <p className="text-xs font-bold text-slate-400 uppercase tracking-wider mb-1">Gender</p>
                  <p className="text-slate-800 font-semibold text-lg">{profile?.gender || 'Not specified'}</p>
                </div>
                
                {/* Read-Only System Fields */}
                <div className="sm:col-span-2 pt-6 border-t border-slate-100">
                  <h3 className="text-sm font-bold text-slate-800 mb-4">Clinic Information (Read-Only)</h3>
                  <div className="grid grid-cols-1 sm:grid-cols-3 gap-6">
                    <div className="bg-slate-50 p-4 rounded-xl">
                      <p className="text-xs font-bold text-slate-400 uppercase tracking-wider mb-1">Branch</p>
                      <p className="text-slate-700 font-semibold">{profile?.branchName || profile?.branchId || 'N/A'}</p>
                    </div>
                    <div className="bg-slate-50 p-4 rounded-xl">
                      <p className="text-xs font-bold text-slate-400 uppercase tracking-wider mb-1">Registered On</p>
                      <p className="text-slate-700 font-semibold">{profile?.registrationDate ? new Date(profile.registrationDate).toLocaleDateString() : 'N/A'}</p>
                    </div>
                    <div className="bg-slate-50 p-4 rounded-xl">
                      <p className="text-xs font-bold text-slate-400 uppercase tracking-wider mb-1">Blood Group</p>
                      <p className="text-slate-700 font-semibold text-red-600">{profile?.bloodGroup || 'Unknown'}</p>
                    </div>
                  </div>
                </div>
              </div>
            )}
          </div>
        </div>


        {/* Branch */}
        <div className="bg-white rounded-2xl shadow-sm border border-slate-100 p-8 mt-8">
          <h3 className="text-sm font-bold text-slate-800 mb-1">My Branch</h3>
          <p className="text-sm text-slate-500 mb-4">You are registered at <b>{profile?.branchName || profile?.branchId}</b>. Your earlier visits and invoices always stay with the branch where they happened.</p>
          {branchMsg.text && (
            <div className={`${branchMsg.type === 'error' ? 'bg-red-50 border-red-200 text-red-700' : 'bg-green-50 border-green-200 text-green-700'} border px-4 py-3 rounded-lg mb-4 text-sm font-semibold`}>{branchMsg.text}</div>
          )}
          {profile?.pendingTransfer ? (
            <div className="bg-amber-50 border border-amber-200 rounded-xl p-4 flex flex-wrap justify-between items-center gap-3">
              <div className="text-sm text-amber-900">Waiting for approval: move to <b>{profile.pendingTransfer.toBranchName}</b> (requested {fmtDate(profile.pendingTransfer.requestedAt)})</div>
              <button disabled={branchBusy} onClick={cancelTransfer} className="text-sm font-bold text-red-600 hover:underline disabled:opacity-50">Cancel request</button>
            </div>
          ) : (
            <form onSubmit={requestTransfer} className="flex flex-wrap gap-3 items-end">
              <div>
                <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider mb-2">Move to</label>
                <select value={targetBranch} onChange={(e) => setTargetBranch(e.target.value)} required className="bg-[#f4f7fe] rounded-lg px-4 py-3 text-sm outline-none min-w-[220px]">
                  <option value="">Select a branch…</option>
                  {branches.filter((b) => b.id !== profile?.branchId).map((b) => <option key={b.id} value={b.id}>{b.name}</option>)}
                </select>
              </div>
              <div className="flex-1 min-w-[200px]">
                <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider mb-2">Reason (optional)</label>
                <input value={reason} maxLength={500} onChange={(e) => setReason(e.target.value)} className="w-full bg-[#f4f7fe] rounded-lg px-4 py-3 text-sm outline-none" />
              </div>
              <button type="submit" disabled={branchBusy || !targetBranch} className="bg-blue-600 hover:bg-blue-700 text-white px-6 py-3 rounded-lg text-sm font-bold disabled:opacity-50">Request branch change</button>
            </form>
          )}
          <h4 className="text-[11px] font-bold text-slate-400 uppercase tracking-wider mt-6 mb-1">My branch change requests</h4>
          {extras.loading ? (
            <p role="status" className="text-sm text-slate-400 py-2">Loading…</p>
          ) : transfers.length === 0 ? (
            <p data-testid="no-transfers" className="text-sm text-slate-400 py-2">You have not requested a branch change yet.</p>
          ) : (
            <ul className="divide-y divide-slate-100 text-sm">
              {transfers.map((t) => (
                <li key={t.id} className="py-3 flex flex-wrap justify-between gap-2">
                  <span>{t.fromBranchName} → <b>{t.toBranchName}</b>
                    <span className={`ml-2 text-[10px] font-bold px-2 py-0.5 rounded border uppercase ${TRANSFER_BADGE[t.status]}`}>{TRANSFER_LABEL[t.status] || t.status}</span>
                  </span>
                  <span className="text-slate-500">
                    Requested {fmtDate(t.requestedAt)}
                    {t.decidedAt ? ` · decided ${fmtDate(t.decidedAt)}` : ''}
                    {t.decisionNote ? ` · “${t.decisionNote}”` : ''}
                  </span>
                </li>
              ))}
            </ul>
          )}
        </div>

        {/* History across branches */}
        {extras.error && (
          <div role="alert" className="bg-red-50 border border-red-200 text-red-700 rounded-2xl p-6 mt-8 text-sm font-semibold flex flex-wrap justify-between items-center gap-3">
            <span>{extras.error}</span>
            <button onClick={loadExtras} className="bg-white border border-red-200 px-4 py-2 rounded-lg text-red-700 font-bold">Try again</button>
          </div>
        )}
        {history && (
          <div className="bg-white rounded-2xl shadow-sm border border-slate-100 p-8 mt-8">
            <h3 className="text-sm font-bold text-slate-800 mb-4">My Visits & Invoices (all branches)</h3>
            <div className="overflow-x-auto">
              <table className="w-full text-left text-[13px] text-slate-600">
                <thead className="text-[10px] uppercase font-bold text-slate-400"><tr><th className="py-2 pr-4">Date</th><th className="py-2 pr-4">Branch</th><th className="py-2 pr-4">Visit</th><th className="py-2">Status</th></tr></thead>
                <tbody className="divide-y divide-slate-100">
                  {history.appointments.length === 0 && <tr><td colSpan="4" className="py-4 text-slate-400">No visits yet.</td></tr>}
                  {history.appointments.map((a) => <tr key={a.id}><td className="py-2 pr-4">{fmtDateTime(a.appointmentDate)}</td><td className="py-2 pr-4">{a.branchName || a.branchId}</td><td className="py-2 pr-4">{a.caseType} • {a.doctorName}</td><td className="py-2 font-semibold">{APPOINTMENT_LABEL[a.status] || a.status}</td></tr>)}
                </tbody>
              </table>
            </div>
            <div className="overflow-x-auto mt-6">
              <table className="w-full text-left text-[13px] text-slate-600">
                <thead className="text-[10px] uppercase font-bold text-slate-400"><tr><th className="py-2 pr-4">Invoice</th><th className="py-2 pr-4">Branch</th><th className="py-2 pr-4 text-right">Total</th><th className="py-2 pr-4 text-right">Balance</th><th className="py-2">Status</th></tr></thead>
                <tbody className="divide-y divide-slate-100">
                  {history.bills.length === 0 && <tr><td colSpan="5" className="py-4 text-slate-400">No invoices yet.</td></tr>}
                  {history.bills.map((b) => <tr key={b.id}><td className="py-2 pr-4 font-bold text-blue-600">{b.invoiceNumber}</td><td className="py-2 pr-4">{b.branchName || b.branchId}</td><td className="py-2 pr-4 text-right">{money(b.totalAmount)}</td><td className="py-2 pr-4 text-right">{money(b.balanceDue)}</td><td className="py-2 font-semibold">{b.status}</td></tr>)}
                </tbody>
              </table>
            </div>
          </div>
        )}

      </div>
    </div>
  );
};

export default PatientProfile;