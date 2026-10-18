import React, { useState, useEffect, useCallback } from 'react';
import axios, { errorMessage } from '../api';
import { Link } from 'react-router-dom';
import StaffLayout from '../layouts/StaffLayout';
import { fmtDate } from '../utils/format';
import CrossBranchPatientSearchModal from '../components/modals/CrossBranchPatientSearchModal';
import RegisterPatientModal from '../components/modals/RegisterPatientModal';

const PatientsDirectory = () => {
  const [data, setData] = useState({ items: [], totalItems: 0, totalPages: 0 });
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');
  const [search, setSearch] = useState('');
  const [q, setQ] = useState('');
  const [scope, setScope] = useState('current'); // current = patients now at this branch | all = also patients who moved away
  const [page, setPage] = useState(0);
  const size = 15;
  const myBranch = localStorage.getItem('branchId');
  const canCrossSearch = ['ADMIN', 'RECEPTIONIST'].includes(localStorage.getItem('role'));
  const [searchOpen, setSearchOpen] = useState(false);
  const [registerOpen, setRegisterOpen] = useState(false);
  const [prefill, setPrefill] = useState(null);

  useEffect(() => {
    const t = setTimeout(() => { setQ(search.trim()); setPage(0); }, 350);
    return () => clearTimeout(t);
  }, [search]);

  const load = useCallback(async () => {
    setIsLoading(true); setError('');
    try {
      const res = await axios.get('/api/patients', { params: { q: q || undefined, scope, page, size, sort: 'id', dir: 'desc' } });
      setData(res.data);
    } catch (err) {
      setData({ items: [], totalItems: 0, totalPages: 0 });
      setError(await errorMessage(err, 'Could not load patients.'));
    } finally {
      setIsLoading(false);
    }
  }, [q, scope, page]);

  useEffect(() => { load(); }, [load]);

  return (
    <StaffLayout>
      <div className="max-w-[1400px] mx-auto pb-10">
        <div className="flex flex-wrap justify-between items-end gap-3 mb-8">
          <div>
            <h1 className="text-2xl font-bold text-slate-800">Patients Directory</h1>
            <p className="text-[13px] text-slate-500 mt-1">Manage and view registered clinic patients</p>
          </div>
          <div className="flex flex-wrap items-center gap-3">
            {canCrossSearch && (
              <button onClick={() => setSearchOpen(true)}
                className="bg-white border border-[#2563eb] text-[#2563eb] hover:bg-blue-50 px-4 py-2 rounded-lg text-sm font-bold shadow-sm transition">
                Find patient in other branches
              </button>
            )}
            <select value={scope} onChange={(e) => { setScope(e.target.value); setPage(0); }}
              className="bg-white border border-slate-200 rounded-lg px-3 py-2 text-sm shadow-sm outline-none focus:border-blue-500">
              <option value="current">Patients at this branch</option>
              <option value="all">Including patients who moved away</option>
            </select>
            <div className="relative">
              <svg className="w-4 h-4 absolute left-3 top-2.5 text-slate-400" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"></path></svg>
              <input type="text" placeholder="Search by name, ID, or phone..."
                className="pl-9 pr-4 py-2 bg-white border border-slate-200 rounded-lg text-sm focus:border-blue-500 outline-none w-72 shadow-sm transition-all"
                value={search} onChange={(e) => setSearch(e.target.value)} />
            </div>
          </div>
        </div>

        {error && <div className="mb-3 bg-red-50 border border-red-100 text-red-700 text-sm rounded-lg p-3 flex justify-between"><span>{error}</span><button onClick={load} className="font-bold underline">Retry</button></div>}

        <div className="bg-white rounded-2xl border border-slate-100 shadow-sm overflow-hidden">
          <div className="overflow-x-auto min-h-[300px]">
            <table className="w-full text-left text-[13px] text-slate-600">
              <thead className="bg-[#f8f9fc] text-[10px] uppercase font-bold text-slate-400">
                <tr>
                  <th className="px-6 py-4">REG ID</th>
                  <th className="px-6 py-4">PATIENT NAME</th>
                  <th className="px-6 py-4">CONTACT</th>
                  <th className="px-6 py-4">GENDER / DOB</th>
                  <th className="px-6 py-4">BRANCH</th>
                  <th className="px-6 py-4">REGISTERED</th>
                  <th className="px-6 py-4 text-right">ACTION</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {isLoading ? (
                  <tr><td colSpan="7" className="text-center py-10 text-slate-400 font-medium">Loading patients...</td></tr>
                ) : data.items.length === 0 ? (
                  <tr><td colSpan="7" className="text-center py-10 text-slate-400 font-medium">No patients found.</td></tr>
                ) : (
                  data.items.map((patient) => {
                    const movedAway = scope === 'all' && myBranch && localStorage.getItem('role') !== 'ADMIN' && patient.branchId !== myBranch;
                    return (
                      <tr key={patient.id} className="hover:bg-[#f8f9fc] transition">
                        <td className="px-6 py-4 font-bold text-[#2563eb]">{patient.regId}</td>
                        <td className="px-6 py-4 flex items-center gap-3">
                          <div className="w-8 h-8 rounded-full bg-blue-50 text-blue-600 flex items-center justify-center font-bold text-xs">
                            {(patient?.fullName || 'U').substring(0, 2).toUpperCase()}
                          </div>
                          <span className="font-bold text-slate-800">{patient?.fullName || 'Unknown Patient'}</span>
                        </td>
                        <td className="px-6 py-4 font-medium">{patient?.phone || 'N/A'}</td>
                        <td className="px-6 py-4 text-slate-500">{patient?.gender || 'N/A'} • {patient?.dateOfBirth || 'N/A'}</td>
                        <td className="px-6 py-4 text-slate-500">
                          {patient.branchName || patient.branchId}
                          {movedAway && <span className="ml-2 text-[9px] font-bold uppercase bg-amber-50 text-amber-700 border border-amber-100 rounded px-1.5 py-0.5">moved</span>}
                        </td>
                        <td className="px-6 py-4 text-slate-500">{fmtDate(patient?.registrationDate)}</td>
                        <td className="px-6 py-4 text-right">
                          <Link to={`/staff-dashboard/patients/${patient.id}`}
                            className="text-blue-600 hover:text-blue-800 font-bold transition text-xs flex items-center justify-end gap-1">
                            View File <svg className="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 5l7 7-7 7"></path></svg>
                          </Link>
                        </td>
                      </tr>
                    );
                  })
                )}
              </tbody>
            </table>
          </div>
          <div className="flex flex-wrap items-center justify-between gap-3 px-4 py-3 border-t border-slate-100 bg-[#f8f9fc] text-[12px] text-slate-500">
            <div>{data.totalItems} patient{data.totalItems === 1 ? '' : 's'}</div>
            <div className="flex items-center gap-3">
              <button disabled={page <= 0} onClick={() => setPage(page - 1)} className="px-3 py-1 rounded border border-slate-200 bg-white disabled:opacity-40">‹ Prev</button>
              <span>Page {data.totalPages === 0 ? 0 : page + 1} of {data.totalPages}</span>
              <button disabled={page + 1 >= data.totalPages} onClick={() => setPage(page + 1)} className="px-3 py-1 rounded border border-slate-200 bg-white disabled:opacity-40">Next ›</button>
            </div>
          </div>
        </div>
      </div>
      {canCrossSearch && (
        <>
          {/* The directory above stays branch-restricted; this modal only returns limited identity data. */}
          <CrossBranchPatientSearchModal isOpen={searchOpen} onClose={() => setSearchOpen(false)}
            onRegisterNew={(values) => { setPrefill(values); setSearchOpen(false); setRegisterOpen(true); }} />
          <RegisterPatientModal isOpen={registerOpen} initialValues={prefill} onClose={() => setRegisterOpen(false)}
            onSuccess={() => { setRegisterOpen(false); load(); }} />
        </>
      )}
    </StaffLayout>
  );
};

export default PatientsDirectory;