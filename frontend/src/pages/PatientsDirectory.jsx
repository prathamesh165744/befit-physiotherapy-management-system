import React, { useState, useEffect } from 'react';
import axios from 'axios';
import StaffLayout from '../layouts/StaffLayout';

const PatientsDirectory = () => {
  const [patients, setPatients] = useState([]);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    axios.get('http://localhost:8080/api/patients')
      .then(res => {
        setPatients(res.data || []);
        setIsLoading(false);
      })
      .catch(err => {
        console.error("Failed to fetch patients list", err);
        setIsLoading(false);
      });
  }, []);

  return (
    <StaffLayout>
      <div className="max-w-[1400px] mx-auto pb-10">
        <div className="flex justify-between items-end mb-8">
          <div>
            <h1 className="text-2xl font-bold text-slate-800">Patients Directory</h1>
            <p className="text-[13px] text-slate-500 mt-1">Manage all registered patients across branches</p>
          </div>
        </div>

        <div className="bg-white rounded-2xl border border-slate-100 shadow-sm overflow-hidden">
          <div className="overflow-x-auto min-h-[300px]">
            <table className="w-full text-left text-[13px] text-slate-600">
              <thead className="bg-[#f8f9fc] text-[10px] uppercase font-bold text-slate-400">
                <tr>
                  <th className="px-6 py-4">PATIENT ID</th>
                  <th className="px-6 py-4">FULL NAME</th>
                  <th className="px-6 py-4">PHONE</th>
                  <th className="px-6 py-4">GENDER</th>
                  <th className="px-6 py-4">EMERGENCY CONTACT</th>
                  <th className="px-6 py-4 text-right">ACTIONS</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {isLoading ? (
                  <tr><td colSpan="6" className="text-center py-10 text-slate-400 font-medium">Loading patients...</td></tr>
                ) : patients.length === 0 ? (
                  <tr><td colSpan="6" className="text-center py-10 text-slate-400 font-medium">No patients found. Click 'Register' on the dashboard to add one.</td></tr>
                ) : (
                  patients.map((p) => (
                    <tr key={p.id} className="hover:bg-[#f8f9fc] transition">
                      <td className="px-6 py-4 font-bold text-[#2563eb]">PAT-{p.id}</td>
                      <td className="px-6 py-4 font-bold text-slate-800">{p.fullName}</td>
                      <td className="px-6 py-4 font-medium">{p.phone || 'N/A'}</td>
                      <td className="px-6 py-4 font-medium">{p.gender === 'M' ? 'Male' : p.gender === 'F' ? 'Female' : 'Other'}</td>
                      <td className="px-6 py-4 font-medium">{p.emergencyContact || 'N/A'}</td>
                      <td className="px-6 py-4 text-right">
                        <button className="text-[#2563eb] font-bold hover:underline">View File</button>
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
