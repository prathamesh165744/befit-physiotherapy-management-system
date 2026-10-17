import React, { useState, useEffect } from 'react';
import axios, { errorMessage } from '../../api';

const NewAppointmentModal = ({ isOpen, onClose, onSuccess }) => {
  const [patients, setPatients] = useState([]);
  const [doctors, setDoctors] = useState([]);
  const [error, setError] = useState('');
  const blank = { patientId: '', doctorId: '', doctorName: '', appointmentDate: '', appointmentTime: '', caseType: 'New Case' };
  const [formData, setFormData] = useState(blank);
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    if (isOpen) {
      setError('');
      // Only patients of the receptionist's own branch, and only that branch's doctors (the server decides the branch).
      axios.get('/api/patients/options')
        .then(res => setPatients(res.data))
        .catch(async err => setError(await errorMessage(err, 'Could not load patients.')));
      axios.get('/api/staff/doctors')
        .then(res => {
          setDoctors(res.data);
          if (res.data.length) setFormData(f => ({ ...f, doctorId: String(res.data[0].id), doctorName: res.data[0].fullName }));
        })
        .catch(async err => setError(await errorMessage(err, 'Could not load doctors.')));
    }
  }, [isOpen]);

  if (!isOpen) return null;

  const handleSubmit = async (e) => {
    e.preventDefault();
    setIsSubmitting(true); setError('');

    const dateTimeString = `${formData.appointmentDate}T${formData.appointmentTime}:00`;
    const payload = {
      patientId: Number(formData.patientId),
      doctorId: Number(formData.doctorId),
      doctorName: formData.doctorName,
      appointmentDate: dateTimeString,
      caseType: formData.caseType,
    };
    try {
      await axios.post('/api/appointments', payload);
      alert("Appointment scheduled successfully!");
      setFormData({ ...blank, doctorId: formData.doctorId, doctorName: formData.doctorName });
      onSuccess();
    } catch (err) {
      setError(await errorMessage(err, 'Failed to schedule the appointment.'));
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-slate-900/40 backdrop-blur-sm z-[100] flex items-center justify-center p-4">
      <div className="bg-white rounded-2xl shadow-2xl w-full max-w-lg overflow-hidden flex flex-col transform transition-all">
        <div className="flex justify-between items-center p-6 border-b border-slate-100 bg-[#f8f9fc]">
          <h2 className="text-lg font-bold text-slate-800">Schedule Appointment</h2>
          <button onClick={onClose} className="text-slate-400 hover:text-red-500 transition">
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12"></path></svg>
          </button>
        </div>
        
        <form onSubmit={handleSubmit} className="p-6 space-y-5">
          {error && <div className="bg-red-50 border border-red-100 text-red-700 text-sm rounded-lg p-3">{error}</div>}
          <div>
            <label className="block text-[11px] font-bold text-slate-500 uppercase tracking-wider mb-1.5">Select Patient *</label>
            <select required className="w-full bg-[#f4f7fe] border border-transparent rounded-lg px-4 py-2.5 text-[13px] focus:outline-none focus:border-[#2563eb] focus:bg-white transition"
              value={formData.patientId} onChange={e => setFormData({...formData, patientId: e.target.value})}>
              <option value="">-- Choose a Registered Patient --</option>
              {patients.map(p => (
                <option key={p.id} value={p.id}>{p.fullName} ({p.phone})</option>
              ))}
            </select>
          </div>
          
          <div className="grid grid-cols-2 gap-5">
            <div>
              <label className="block text-[11px] font-bold text-slate-500 uppercase tracking-wider mb-1.5">Date *</label>
              <input required type="date" min={new Date().toISOString().slice(0,10)} className="w-full bg-[#f4f7fe] border border-transparent rounded-lg px-4 py-2.5 text-[13px] focus:outline-none focus:border-[#2563eb] focus:bg-white transition text-slate-700"
                value={formData.appointmentDate} onChange={e => setFormData({...formData, appointmentDate: e.target.value})} />
            </div>
            <div>
              <label className="block text-[11px] font-bold text-slate-500 uppercase tracking-wider mb-1.5">Time *</label>
              <input required type="time" className="w-full bg-[#f4f7fe] border border-transparent rounded-lg px-4 py-2.5 text-[13px] focus:outline-none focus:border-[#2563eb] focus:bg-white transition text-slate-700"
                value={formData.appointmentTime} onChange={e => setFormData({...formData, appointmentTime: e.target.value})} />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-5">
            <div>
              <label className="block text-[11px] font-bold text-slate-500 uppercase tracking-wider mb-1.5">Doctor *</label>
              <select required className="w-full bg-[#f4f7fe] border border-transparent rounded-lg px-4 py-2.5 text-[13px] focus:outline-none focus:border-[#2563eb] focus:bg-white transition"
                value={formData.doctorId} onChange={e => {
                  const d = doctors.find(x => String(x.id) === e.target.value);
                  setFormData({...formData, doctorId: e.target.value, doctorName: d ? d.fullName : ''});
                }}>
                {doctors.length === 0 && <option value="">No doctors at this branch</option>}
                {doctors.map(d => <option key={d.id} value={d.id}>{d.fullName}</option>)}
              </select>
            </div>
            <div>
              <label className="block text-[11px] font-bold text-slate-500 uppercase tracking-wider mb-1.5">Case Type *</label>
              <select required className="w-full bg-[#f4f7fe] border border-transparent rounded-lg px-4 py-2.5 text-[13px] focus:outline-none focus:border-[#2563eb] focus:bg-white transition"
                value={formData.caseType} onChange={e => setFormData({...formData, caseType: e.target.value})}>
                <option value="New Case">New Case</option>
                <option value="Follow-up">Follow-up</option>
                <option value="Physiotherapy">Physiotherapy</option>
                <option value="Post-Op Rehab">Post-Op Rehab</option>
              </select>
            </div>
          </div>
          
          <div className="pt-2 flex justify-end gap-3 mt-4">
            <button type="button" onClick={onClose} className="px-5 py-2.5 rounded-lg text-[13px] font-bold text-slate-600 hover:bg-slate-100 transition">Cancel</button>
            <button type="submit" disabled={isSubmitting} className="bg-[#2563eb] hover:bg-blue-700 text-white px-6 py-2.5 rounded-lg text-[13px] font-bold shadow-sm transition disabled:opacity-50">
              {isSubmitting ? 'Scheduling...' : 'Schedule Appointment'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
export default NewAppointmentModal;
