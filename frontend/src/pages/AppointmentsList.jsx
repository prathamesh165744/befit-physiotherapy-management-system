import React, { useState, useEffect, useCallback } from 'react';
import axios, { errorMessage } from '../api';
import StaffLayout from '../layouts/StaffLayout';
import { fmtDateTime } from '../utils/format';

const BADGE = {
  WAITING: 'bg-yellow-50 text-yellow-600', CHECKED_IN: 'bg-green-50 text-green-600', IN_PROGRESS: 'bg-indigo-50 text-indigo-600',
  COMPLETED: 'bg-blue-50 text-blue-600', CANCELLED: 'bg-slate-100 text-slate-500', NO_SHOW: 'bg-red-50 text-red-600', SCHEDULED: 'bg-sky-50 text-sky-600',
};

/** historyMode = the "History" page: only finished visits (completed / cancelled / no-show). */
const AppointmentsList = ({ historyMode = false }) => {
  const [data, setData] = useState({ items: [], totalItems: 0, totalPages: 0 });
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');
  const [filterDate, setFilterDate] = useState(''); // Empty means all dates
  const [page, setPage] = useState(0);
  const size = 15;
  const isAdmin = localStorage.getItem('role') === 'ADMIN';

  const load = useCallback(async () => {
    setIsLoading(true); setError('');
    try {
      const res = await axios.get('/api/appointments', {
        params: { date: filterDate || undefined, statuses: historyMode ? 'COMPLETED,CANCELLED,NO_SHOW' : undefined, page, size, dir: historyMode ? 'desc' : 'asc' },
      });
      setData(res.data);
    } catch (err) {
      setData({ items: [], totalItems: 0, totalPages: 0 });
      setError(await errorMessage(err, 'Could not load appointments.'));
    } finally {
      setIsLoading(false);
    }
  }, [filterDate, page, historyMode]);

  useEffect(() => { load(); }, [load]);

  return (
    <StaffLayout>
      <div className="max-w-[1400px] mx-auto pb-10">
        <div className="flex justify-between items-end mb-8">
          <div>
            <h1 className="text-2xl font-bold text-slate-800">{historyMode ? 'Visit History' : 'All Appointments'}</h1>
            <p className="text-[13px] text-slate-500 mt-1">{historyMode ? 'Completed, cancelled and missed visits' : 'Master list of all scheduled clinic visits'}</p>
          </div>
          <div className="bg-white border border-slate-200 rounded-lg px-3 py-2 flex items-center gap-2 shadow-sm">
            <span className="text-xs font-bold text-slate-400 uppercase">Filter Date:</span>
            <input type="date" className="text-sm font-semibold text-slate-700 outline-none cursor-pointer" value={filterDate}
              onChange={(e) => { setFilterDate(e.target.value); setPage(0); }} />
            {filterDate && (
              <button onClick={() => { setFilterDate(''); setPage(0); }} className="text-[10px] bg-red-50 text-red-600 px-2 py-1 rounded ml-2 font-bold hover:bg-red-100">CLEAR</button>
            )}
          </div>
        </div>

        {error && <div className="mb-3 bg-red-50 border border-red-100 text-red-700 text-sm rounded-lg p-3 flex justify-between"><span>{error}</span><button onClick={load} className="font-bold underline">Retry</button></div>}

        <div className="bg-white rounded-2xl border border-slate-100 shadow-sm overflow-hidden">
          <div className="overflow-x-auto min-h-[300px]">
            <table className="w-full text-left text-[13px] text-slate-600">
              <thead className="bg-[#f8f9fc] text-[10px] uppercase font-bold text-slate-400">
                <tr>
                  <th className="px-6 py-4">APT ID</th>
                  <th className="px-6 py-4">DATE & TIME</th>
                  <th className="px-6 py-4">PATIENT</th>
                  <th className="px-6 py-4">DOCTOR</th>
                  <th className="px-6 py-4">CASE TYPE</th>
                  {isAdmin && <th className="px-6 py-4">BRANCH</th>}
                  <th className="px-6 py-4">STATUS</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {isLoading ? (
                  <tr><td colSpan="7" className="text-center py-10 text-slate-400 font-medium">Loading appointments...</td></tr>
                ) : data.items.length === 0 ? (
                  <tr><td colSpan="7" className="text-center py-10 text-slate-400 font-medium">No appointments found.</td></tr>
                ) : (
                  data.items.map((apt) => (
                    <tr key={apt.id} className="hover:bg-[#f8f9fc] transition">
                      <td className="px-6 py-4 font-bold text-[#2563eb]">APT-{apt.id}</td>
                      <td className="px-6 py-4 font-medium">{fmtDateTime(apt.appointmentDate)}</td>
                      <td className="px-6 py-4 font-bold text-slate-800">{apt.patient?.fullName || 'Walk-in'}</td>
                      <td className="px-6 py-4 text-slate-500">{apt.doctorName}</td>
                      <td className="px-6 py-4 font-medium">{apt.caseType}</td>
                      {isAdmin && <td className="px-6 py-4 text-slate-500">{apt.branchName || apt.branchId}</td>}
                      <td className="px-6 py-4">
                        <span className={`text-[9px] font-bold px-2 py-1 rounded uppercase tracking-wider ${BADGE[apt.status] || 'bg-blue-50 text-blue-600'}`}>
                          {apt.status?.replace('_', ' ')}
                        </span>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
          <div className="flex flex-wrap items-center justify-between gap-3 px-4 py-3 border-t border-slate-100 bg-[#f8f9fc] text-[12px] text-slate-500">
            <div>{data.totalItems} appointment{data.totalItems === 1 ? '' : 's'}</div>
            <div className="flex items-center gap-3">
              <button disabled={page <= 0} onClick={() => setPage(page - 1)} className="px-3 py-1 rounded border border-slate-200 bg-white disabled:opacity-40">‹ Prev</button>
              <span>Page {data.totalPages === 0 ? 0 : page + 1} of {data.totalPages}</span>
              <button disabled={page + 1 >= data.totalPages} onClick={() => setPage(page + 1)} className="px-3 py-1 rounded border border-slate-200 bg-white disabled:opacity-40">Next ›</button>
            </div>
          </div>
        </div>
      </div>
    </StaffLayout>
  );
};

export default AppointmentsList;
