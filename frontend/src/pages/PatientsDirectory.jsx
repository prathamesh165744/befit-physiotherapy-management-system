import React, { useState, useEffect } from 'react';
import axios from 'axios';
import { Link } from 'react-router-dom';
import StaffLayout from '../layouts/StaffLayout';

const PatientsDirectory = () => {
  const [patients, setPatients] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');

  useEffect(() => {
    setIsLoading(true);
    axios.get('http://localhost:8080/api/patients')
      .then(res => {
        // Safely ensure we always set an array, even if the backend sends null
        setPatients(Array.isArray(res.data) ? res.data : []);
        setIsLoading(false);
      })
      .catch(err => {
        console.error("Failed to fetch patients", err);
        setPatients([]);
        setIsLoading(false);
      });
  }, []);

  // Bulletproof search filter that won't crash if fullName or phone is null
  const filteredPatients = patients.filter(p => {
    if (!searchTerm) return true;
    const name = p?.fullName || '';
    const phone = p?.phone || '';
    const regId = p?.id ? `REG-${p.id}` : '';
    
    return name.toLowerCase().includes(searchTerm.toLowerCase()) || 
           phone.includes(searchTerm) ||
           regId.toLowerCase().includes(searchTerm.toLowerCase());
  });

  return (
    <StaffLayout>
      <div className="max-w-[1400px] mx-auto pb-10">
        
        <div className="flex justify-between items-end mb-8">
          <div>
            <h1 className="text-2xl font-bold text-slate-800">Patients Directory</h1>
            <p className="text-[13px] text-slate-500 mt-1">Manage and view all registered clinic patients</p>
          </div>
          
          <div className="relative">
            <svg className="w-4 h-4 absolute left-3 top-2.5 text-slate-400" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"></path></svg>
            <input 
              type="text" 
              placeholder="Search by name, ID, or phone..." 
              className="pl-9 pr-4 py-2 bg-white border border-slate-200 rounded-lg text-sm focus:border-blue-500 outline-none w-72 shadow-sm transition-all"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
            />
          </div>
        </div>

        <div className="bg-white rounded-2xl border border-slate-100 shadow-sm overflow-hidden">
          <div className="overflow-x-auto min-h-[300px]">
            <table className="w-full text-left text-[13px] text-slate-600">
              <thead className="bg-[#f8f9fc] text-[10px] uppercase font-bold text-slate-400">
                <tr>
                  <th className="px-6 py-4">REG ID</th>
                  <th className="px-6 py-4">PATIENT NAME</th>
                  <th className="px-6 py-4">CONTACT</th>
                  <th className="px-6 py-4">GENDER / DOB</th>
                  <th className="px-6 py-4">REGISTERED</th>
                  <th className="px-6 py-4 text-right">ACTION</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {isLoading ? (
                  <tr><td colSpan="6" className="text-center py-10 text-slate-400 font-medium">Loading patients...</td></tr>
                ) : filteredPatients.length === 0 ? (
                  <tr><td colSpan="6" className="text-center py-10 text-slate-400 font-medium">No patients found.</td></tr>
                ) : (
                  filteredPatients.map((patient) => (
                    <tr key={patient.id} className="hover:bg-[#f8f9fc] transition">
                      <td className="px-6 py-4 font-bold text-[#2563eb]">REG-{patient.id}</td>
                      <td className="px-6 py-4 flex items-center gap-3">
                        <div className="w-8 h-8 rounded-full bg-blue-50 text-blue-600 flex items-center justify-center font-bold text-xs">
                          {(patient?.fullName || 'U').substring(0, 2).toUpperCase()}
                        </div>
                        <span className="font-bold text-slate-800">{patient?.fullName || 'Unknown Patient'}</span>
                      </td>
                      <td className="px-6 py-4 font-medium">{patient?.phone || 'N/A'}</td>
                      <td className="px-6 py-4 text-slate-500">{patient?.gender || 'N/A'} • {patient?.dateOfBirth || 'N/A'}</td>
                      <td className="px-6 py-4 text-slate-500">{patient?.registrationDate || 'N/A'}</td>
                      <td className="px-6 py-4 text-right">
                        <Link 
                          to={`/staff-dashboard/patients/${patient.id}`} 
                          className="text-blue-600 hover:text-blue-800 font-bold transition text-xs flex items-center justify-end gap-1"
                        >
                          View File <svg className="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 5l7 7-7 7"></path></svg>
                        </Link>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </StaffLayout>
  );
};

export default PatientsDirectory;