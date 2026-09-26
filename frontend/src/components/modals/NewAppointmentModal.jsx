import React, { useState, useEffect } from 'react';
import axios from 'axios';

const NewAppointmentModal = ({ isOpen, onClose, onSuccess }) => {
  const [patients, setPatients] = useState([]);
  const [formData, setFormData] = useState({
    patientId: '', doctorId: '2', doctorName: 'Dr. Himanshu Jain', appointmentDate: '', appointmentTime: '', caseType: 'New Case'
  });
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    if (isOpen) {
      axios.get('http://localhost:8080/api/patients')
        .then(res => setPatients(res.data))
        .catch(err => console.error("Failed to fetch patients", err));
    }
  }, [isOpen]);

  if (!isOpen) return null;

  const handleSubmit = (e) => {
    e.preventDefault();
    setIsSubmitting(true);
    
    const dateTimeString = `${formData.appointmentDate}T${formData.appointmentTime}:00`;
    const payload = { ...formData, appointmentDate: dateTimeString };

    axios.post('http://localhost:8080/api/appointments', payload)
      .then(res => {
        alert("Appointment scheduled successfully!");
        setIsSubmitting(false);
        setFormData({ patientId: '', doctorId: '2', doctorName: 'Dr. Himanshu Jain', appointmentDate: '', appointmentTime: '', caseType: 'New Case' });
        onSuccess(); 
      })
      .catch(err => {
        console.error(err);
        alert("Failed to schedule appointment.");
        setIsSubmitting(false);
      });
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
              <input required type="date" className="w-full bg-[#f4f7fe] border border-transparent rounded-lg px-4 py-2.5 text-[13px] focus:outline-none focus:border-[#2563eb] focus:bg-white transition text-slate-700"
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
                value={formData.doctorName} onChange={e => setFormData({...formData, doctorName: e.target.value})}>
                <option value="Dr. Himanshu Jain">Dr. Himanshu Jain</option>
                <option value="Dr. Smita Patil">Dr. Smita Patil</option>
                <option value="Dr. Rohan Shah">Dr. Rohan Shah</option>
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
