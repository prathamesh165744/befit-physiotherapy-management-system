import React, { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import axios from 'axios';
import StaffLayout from '../layouts/StaffLayout';

const PatientProfile = () => {
  const { id } = useParams();
  const [patient, setPatient] = useState(null);
  const [appointments, setAppointments] = useState([]);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const fetchPatientData = async () => {
      setIsLoading(true);
      try {
        const pRes = await axios.get(`http://localhost:8080/api/patients/${id}`);
        setPatient(pRes.data);
        const aRes = await axios.get(`http://localhost:8080/api/appointments/patient/${id}`);
        setAppointments(aRes.data || []);
      } catch (err) {
        console.error("Failed to fetch profile", err);
      } finally {
        setIsLoading(false);
      }
    };
    fetchPatientData();
  }, [id]);

  if (isLoading) {
    return <StaffLayout><div className="p-10 text-slate-500 font-medium">Loading patient file...</div></StaffLayout>;
  }

  if (!patient) {
    return <StaffLayout><div className="p-10 text-red-500 font-medium">Patient not found!</div></StaffLayout>;
  }

  return (
    <StaffLayout>
      <div className="max-w-[1200px] mx-auto pb-10">
        
        {/* Header & Back Button */}
        <div className="mb-6">
          <Link to="/staff-dashboard/patients" className="text-sm font-bold text-blue-600 hover:underline flex items-center gap-1 mb-4">
            &larr; Back to Directory
          </Link>
          <div className="flex justify-between items-end">
            <div className="flex items-center gap-5">
              <div className="w-16 h-16 bg-blue-100 text-blue-600 rounded-full flex items-center justify-center text-2xl font-bold">
                {patient.fullName.substring(0, 2).toUpperCase()}
              </div>
              <div>
                <h1 className="text-2xl font-bold text-slate-800">{patient.fullName}</h1>
                <p className="text-[13px] text-slate-500 mt-1 font-medium">REG ID: REG-{patient.id} • Registered: {patient.registrationDate || 'N/A'}</p>
              </div>
            </div>
            <span className="bg-emerald-100 text-emerald-700 font-bold px-3 py-1.5 rounded-lg text-xs uppercase tracking-wider">Active Patient</span>
          </div>
        </div>

        <div className="grid grid-cols-12 gap-6">
          
          {/* Left Column: Personal & Medical Details */}
          <div className="col-span-4 space-y-6">
            
            {/* Contact Info Card */}
            <div className="bg-white rounded-2xl border border-slate-100 p-6 shadow-sm">
              <h3 className="text-sm font-bold text-slate-400 uppercase tracking-wider mb-4 border-b border-slate-100 pb-2">Contact Details</h3>
              <div className="space-y-4 text-[13px]">
                <div><span className="text-slate-500 block mb-0.5">Phone Number</span><span className="font-semibold text-slate-800">{patient.phone}</span></div>
                <div><span className="text-slate-500 block mb-0.5">Gender / DOB</span><span className="font-semibold text-slate-800">{patient.gender || 'N/A'} • {patient.dateOfBirth || 'N/A'}</span></div>
                <div><span className="text-slate-500 block mb-0.5">Emergency Contact</span><span className="font-semibold text-red-600">{patient.emergencyContact || 'Not provided'}</span></div>
              </div>
            </div>

            {/* Medical History Card */}
            <div className="bg-white rounded-2xl border border-slate-100 p-6 shadow-sm">
              <h3 className="text-sm font-bold text-slate-400 uppercase tracking-wider mb-4 border-b border-slate-100 pb-2">Medical Notes</h3>
              <p className="text-[13px] text-slate-700 leading-relaxed font-medium bg-slate-50 p-4 rounded-lg border border-slate-100">
                {patient.medicalHistory || "No initial medical history recorded."}
              </p>
            </div>
            
          </div>

          {/* Right Column: Appointment Timeline */}
          <div className="col-span-8 bg-white rounded-2xl border border-slate-100 p-6 shadow-sm">
            <h3 className="text-[17px] font-bold text-slate-800 mb-6">Treatment Timeline</h3>
            
            <div className="space-y-4">
              {appointments.length === 0 ? (
                <div className="text-center py-10 bg-slate-50 rounded-xl border border-dashed border-slate-200">
                  <p className="text-slate-500 font-medium">No past appointments found for this patient.</p>
                </div>
              ) : (
                appointments.map((apt) => (
                  <div key={apt.id} className="flex gap-4 p-4 border border-slate-100 rounded-xl hover:bg-slate-50 transition">
                    <div className="flex flex-col items-center justify-center bg-blue-50 text-blue-700 rounded-lg p-3 w-20 shrink-0">
                      <span className="text-xs font-bold uppercase">{new Date(apt.appointmentDate).toLocaleString('default', { month: 'short' })}</span>
                      <span className="text-xl font-black">{new Date(apt.appointmentDate).getDate()}</span>
                      <span className="text-[10px] font-bold mt-1">{new Date(apt.appointmentDate).getFullYear()}</span>
                    </div>
                    <div className="flex-1 flex flex-col justify-center">
                      <div className="flex justify-between items-start mb-1">
                        <h4 className="text-[15px] font-bold text-slate-800">{apt.caseType || 'Consultation'}</h4>
                        <span className={`text-[10px] font-bold px-2 py-1 rounded uppercase tracking-wider ${apt.status === 'COMPLETED' ? 'bg-green-100 text-green-700' : 'bg-yellow-100 text-yellow-700'}`}>{apt.status}</span>
                      </div>
                      <p className="text-[13px] text-slate-500 font-medium mb-1">Dr. {apt.doctorName || 'Staff'}</p>
                      <p className="text-[12px] text-slate-400">Appointment ID: APT-{apt.id}</p>
                    </div>
                  </div>
                ))
              )}
            </div>
          </div>

        </div>
      </div>
    </StaffLayout>
  );
};
export default PatientProfile;