import React, { useState, useEffect } from 'react';
import axios from 'axios';
import StaffLayout from '../layouts/StaffLayout';
import ManualBillModal from '../components/modals/ManualBillModal';

const Billing = () => {
  const [bills, setBills] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isManualModalOpen, setIsManualModalOpen] = useState(false);

  const fetchBills = () => {
    setIsLoading(true);
    axios.get('http://localhost:8080/api/billing')
      .then(res => { setBills(res.data || []); setIsLoading(false); })
      .catch(err => { console.error("Failed to fetch bills", err); setIsLoading(false); });
  };

  useEffect(() => { fetchBills(); }, []);

  return (
    <StaffLayout>
      <div className="max-w-[1400px] mx-auto pb-10">
        <div className="flex justify-between items-end mb-8">
          <div>
            <h1 className="text-2xl font-bold text-slate-800">Billing & Invoices</h1>
            <p className="text-[13px] text-slate-500 mt-1">Manage all clinic revenue and manual bills</p>
          </div>
          <button onClick={() => setIsManualModalOpen(true)} className="bg-green-600 hover:bg-green-700 text-white text-sm font-semibold py-2.5 px-5 rounded-lg shadow-sm transition-colors">
            + Create Manual Bill
          </button>
        </div>

        <div className="bg-white rounded-2xl border border-slate-100 shadow-sm overflow-hidden">
          <div className="overflow-x-auto min-h-[300px]">
            <table className="w-full text-left text-[13px] text-slate-600">
              <thead className="bg-[#f8f9fc] text-[10px] uppercase font-bold text-slate-400">
                <tr>
                  <th className="px-6 py-4">INV ID</th>
                  <th className="px-6 py-4">DATE</th>
                  <th className="px-6 py-4">PATIENT / CUSTOMER</th>
                  <th className="px-6 py-4">DESCRIPTION</th>
                  <th className="px-6 py-4">AMOUNT</th>
                  <th className="px-6 py-4">STATUS</th>
                  <th className="px-6 py-4 text-right">ACTION</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {isLoading ? (
                  <tr><td colSpan="7" className="text-center py-10 text-slate-400 font-medium">Loading invoices...</td></tr>
                ) : bills.length === 0 ? (
                  <tr><td colSpan="7" className="text-center py-10 text-slate-400 font-medium">No billing records found.</td></tr>
                ) : (
                  bills.map((b) => (
                    <tr key={b.id} className="hover:bg-[#f8f9fc] transition">
                      <td className="px-6 py-4 font-bold text-[#2563eb]">INV-{b.id}</td>
                      <td className="px-6 py-4 font-medium">{new Date(b.billDate).toLocaleDateString()}</td>
                      <td className="px-6 py-4 font-bold text-slate-800">{b.patientName}</td>
                      <td className="px-6 py-4 text-slate-500">{b.description}</td>
                      <td className="px-6 py-4 font-bold text-slate-800">₹{b.amount}</td>
                      <td className="px-6 py-4"><span className="text-[9px] font-bold text-green-600 bg-green-50 px-2 py-1 rounded tracking-wider uppercase">{b.status}</span></td>
                      <td className="px-6 py-4 text-right">
                        <button onClick={() => window.print()} className="text-slate-400 hover:text-blue-600 font-bold transition">Print</button>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>
      </div>
      <ManualBillModal isOpen={isManualModalOpen} onClose={() => setIsManualModalOpen(false)} onSuccess={() => { setIsManualModalOpen(false); fetchBills(); }} />
    </StaffLayout>
  );
};
export default Billing;