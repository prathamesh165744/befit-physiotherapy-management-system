import React, { useState, useEffect, useCallback, useMemo } from 'react';
import axios, { errorMessage, saveBlob, filenameFromDisposition } from '../api';
import StaffLayout from '../layouts/StaffLayout';
import ManualBillModal from '../components/modals/ManualBillModal';
import InvoicePreviewModal from '../components/billing/InvoicePreviewModal';
import BillReportModal from '../components/billing/BillReportModal';
import EditBillModal from '../components/billing/EditBillModal';
import PaymentModal from '../components/billing/PaymentModal';
import { money, fmtDate, STATUS_BADGE, METHOD_LABEL } from '../utils/format';

const STATUSES = ['ALL', 'UNPAID', 'PARTIAL', 'PAID', 'CANCELLED'];

const Billing = () => {
  // ---- filters (all sent to the server; the list, Print All and Excel export share them) ----
  const [search, setSearch] = useState('');
  const [q, setQ] = useState('');
  const [status, setStatus] = useState('ALL');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const [sort, setSort] = useState('billDate');
  const [dir, setDir] = useState('desc');

  // ---- data ----
  const [data, setData] = useState({ items: [], totalItems: 0, totalPages: 0 });
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [exporting, setExporting] = useState(false);

  // ---- dialogs ----
  const [isManualModalOpen, setIsManualModalOpen] = useState(false);
  const [printId, setPrintId] = useState(null);
  const [editId, setEditId] = useState(null);
  const [payBill, setPayBill] = useState(null);
  const [showReport, setShowReport] = useState(false);

  // wait until the user stops typing before searching
  useEffect(() => {
    const t = setTimeout(() => { setQ(search.trim()); setPage(0); }, 350);
    return () => clearTimeout(t);
  }, [search]);

  // the filters that identify "the records on screen" - reused by Print All and Export
  const filterParams = useMemo(() => {
    const p = {};
    if (q) p.q = q;
    if (status !== 'ALL') p.status = status;
    if (from) p.from = from;
    if (to) p.to = to;
    return p;
  }, [q, status, from, to]);

  const badRange = from && to && from > to;

  const fetchBills = useCallback(async () => {
    if (badRange) return;
    setIsLoading(true); setError('');
    try {
      const res = await axios.get('/api/billing', { params: { ...filterParams, page, size, sort, dir } });
      setData(res.data);
    } catch (err) {
      setError(await errorMessage(err, 'Could not load invoices.'));
    } finally {
      setIsLoading(false);
    }
  }, [filterParams, page, size, sort, dir, badRange]);

  useEffect(() => { fetchBills(); }, [fetchBills]);

  const changeFilter = (setter) => (e) => { setter(e.target.value); setPage(0); };
  const toggleSort = (col) => {
    if (sort === col) setDir(dir === 'asc' ? 'desc' : 'asc'); else { setSort(col); setDir('desc'); }
    setPage(0);
  };
  const arrow = (col) => (sort === col ? (dir === 'asc' ? ' ▲' : ' ▼') : '');

  const exportExcel = async () => {
    setExporting(true); setNotice(''); setError('');
    try {
      const res = await axios.get('/api/billing/export', { params: filterParams, responseType: 'blob' });
      const name = filenameFromDisposition(res.headers['content-disposition'], 'befit-billing.xlsx');
      saveBlob(res.data, name);
      setNotice(`Downloaded ${name}`);
    } catch (err) {
      setError(await errorMessage(err, 'Could not export to Excel.'));
    } finally {
      setExporting(false);
    }
  };

  const afterChange = (message) => { setEditId(null); setPayBill(null); if (message) setNotice(message); fetchBills(); };
  const showBranchColumn = (localStorage.getItem('role') === 'ADMIN');

  const input = 'bg-white border border-slate-200 rounded-lg px-3 py-2 text-[13px] outline-none focus:border-[#2563eb]';
  const th = 'px-4 py-4';

  return (
    <StaffLayout>
      <div className="max-w-[1400px] mx-auto pb-10">
        <div className="flex flex-wrap justify-between items-end gap-3 mb-6">
          <div>
            <h1 className="text-2xl font-bold text-slate-800">Billing & Invoices</h1>
            <p className="text-[13px] text-slate-500 mt-1">Manage clinic revenue, print invoices and export records</p>
          </div>
          <div className="flex flex-wrap gap-2">
            <button onClick={() => setShowReport(true)} disabled={!!badRange}
              className="bg-white border border-slate-200 hover:border-[#2563eb] hover:text-[#2563eb] text-slate-700 text-sm font-semibold py-2.5 px-4 rounded-lg shadow-sm transition-colors disabled:opacity-50">
              🖨 Print All
            </button>
            <button onClick={exportExcel} disabled={exporting || !!badRange}
              className="bg-white border border-slate-200 hover:border-emerald-600 hover:text-emerald-700 text-slate-700 text-sm font-semibold py-2.5 px-4 rounded-lg shadow-sm transition-colors disabled:opacity-50">
              {exporting ? 'Exporting…' : '⬇ Export to Excel'}
            </button>
            <button onClick={() => setIsManualModalOpen(true)} className="bg-green-600 hover:bg-green-700 text-white text-sm font-semibold py-2.5 px-5 rounded-lg shadow-sm transition-colors">
              + Create Manual Bill
            </button>
          </div>
        </div>

        {/* filters */}
        <div className="bg-white rounded-2xl border border-slate-100 shadow-sm p-4 mb-4 flex flex-wrap gap-3 items-end">
          <div className="flex-1 min-w-[220px]">
            <label className="block text-[10px] font-bold text-slate-400 uppercase mb-1">Search</label>
            <input className={input + ' w-full'} placeholder="Invoice no., patient name or patient ID" value={search} onChange={(e) => setSearch(e.target.value)} />
          </div>
          <div>
            <label className="block text-[10px] font-bold text-slate-400 uppercase mb-1">Status</label>
            <select className={input} value={status} onChange={changeFilter(setStatus)}>{STATUSES.map((s) => <option key={s} value={s}>{s === 'ALL' ? 'All statuses' : s}</option>)}</select>
          </div>
          <div>
            <label className="block text-[10px] font-bold text-slate-400 uppercase mb-1">From</label>
            <input type="date" className={input} value={from} max={to || undefined} onChange={changeFilter(setFrom)} />
          </div>
          <div>
            <label className="block text-[10px] font-bold text-slate-400 uppercase mb-1">To</label>
            <input type="date" className={input} value={to} min={from || undefined} onChange={changeFilter(setTo)} />
          </div>
          {(search || status !== 'ALL' || from || to) && (
            <button onClick={() => { setSearch(''); setQ(''); setStatus('ALL'); setFrom(''); setTo(''); setPage(0); }} className="text-[12px] font-bold text-slate-500 hover:text-red-600 pb-2">Clear filters</button>
          )}
        </div>

        {badRange && <div className="mb-3 bg-amber-50 border border-amber-200 text-amber-800 text-sm rounded-lg p-3">The “From” date is after the “To” date.</div>}
        {error && <div className="mb-3 bg-red-50 border border-red-100 text-red-700 text-sm rounded-lg p-3 flex justify-between"><span>{error}</span><button onClick={fetchBills} className="font-bold underline">Retry</button></div>}
        {notice && <div className="mb-3 bg-green-50 border border-green-100 text-green-800 text-sm rounded-lg p-3 flex justify-between"><span>{notice}</span><button onClick={() => setNotice('')} className="font-bold">✕</button></div>}

        <div className="bg-white rounded-2xl border border-slate-100 shadow-sm overflow-hidden">
          <div className="overflow-x-auto min-h-[300px]">
            <table className="w-full text-left text-[13px] text-slate-600">
              <thead className="bg-[#f8f9fc] text-[10px] uppercase font-bold text-slate-400">
                <tr>
                  <th className={th}>Invoice</th>
                  <th className={th + ' cursor-pointer select-none'} onClick={() => toggleSort('billDate')}>Date{arrow('billDate')}</th>
                  <th className={th + ' cursor-pointer select-none'} onClick={() => toggleSort('patientName')}>Patient / Customer{arrow('patientName')}</th>
                  {showBranchColumn && <th className={th}>Branch</th>}
                  <th className={th}>Description</th>
                  <th className={th + ' text-right cursor-pointer select-none'} onClick={() => toggleSort('totalAmount')}>Total{arrow('totalAmount')}</th>
                  <th className={th + ' text-right'}>Paid</th>
                  <th className={th + ' text-right'}>Balance</th>
                  <th className={th}>Method</th>
                  <th className={th}>Status</th>
                  <th className={th + ' text-right'}>Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {isLoading ? (
                  <tr><td colSpan="11" className="text-center py-10 text-slate-400 font-medium">Loading invoices...</td></tr>
                ) : data.items.length === 0 ? (
                  <tr><td colSpan="11" className="text-center py-10 text-slate-400 font-medium">No billing records found.</td></tr>
                ) : (
                  data.items.map((b) => (
                    <tr key={b.id} className="hover:bg-[#f8f9fc] transition">
                      <td className="px-4 py-3 font-bold text-[#2563eb] whitespace-nowrap">{b.invoiceNumber}</td>
                      <td className="px-4 py-3 font-medium whitespace-nowrap">{fmtDate(b.billDate)}</td>
                      <td className="px-4 py-3 font-bold text-slate-800">{b.patientName}</td>
                      {showBranchColumn && <td className="px-4 py-3 text-slate-500">{b.branchName}</td>}
                      <td className="px-4 py-3 text-slate-500 max-w-[220px] truncate" title={b.description}>{b.description}</td>
                      <td className="px-4 py-3 font-bold text-slate-800 text-right whitespace-nowrap">{money(b.totalAmount)}</td>
                      <td className="px-4 py-3 text-right whitespace-nowrap">{money(b.amountPaid)}</td>
                      <td className={`px-4 py-3 text-right whitespace-nowrap font-semibold ${Number(b.balanceDue) > 0 ? 'text-red-600' : 'text-slate-400'}`}>{money(b.balanceDue)}</td>
                      <td className="px-4 py-3">{METHOD_LABEL[b.paymentMethod] || '—'}</td>
                      <td className="px-4 py-3"><span className={`text-[9px] font-bold px-2 py-1 rounded border tracking-wider uppercase ${STATUS_BADGE[b.status] || ''}`}>{b.status}</span></td>
                      <td className="px-4 py-3 text-right whitespace-nowrap space-x-3">
                        <button onClick={() => setPrintId(b.id)} className="text-slate-500 hover:text-blue-600 font-bold transition">Print</button>
                        <button onClick={() => setEditId(b.id)} className="text-slate-500 hover:text-blue-600 font-bold transition">Edit</button>
                        {b.status !== 'CANCELLED' && Number(b.balanceDue) > 0 && (
                          <button onClick={() => setPayBill(b)} className="text-green-600 hover:text-green-800 font-bold transition">Pay</button>
                        )}
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>

          {/* pagination */}
          <div className="flex flex-wrap items-center justify-between gap-3 px-4 py-3 border-t border-slate-100 bg-[#f8f9fc] text-[12px] text-slate-500">
            <div>{data.totalItems} invoice{data.totalItems === 1 ? '' : 's'}</div>
            <div className="flex items-center gap-3">
              <label className="flex items-center gap-1">Rows
                <select className={input + ' !py-1'} value={size} onChange={(e) => { setSize(Number(e.target.value)); setPage(0); }}>
                  {[10, 20, 50, 100].map((n) => <option key={n} value={n}>{n}</option>)}
                </select>
              </label>
              <button disabled={page <= 0} onClick={() => setPage(page - 1)} className="px-3 py-1 rounded border border-slate-200 bg-white disabled:opacity-40">‹ Prev</button>
              <span>Page {data.totalPages === 0 ? 0 : page + 1} of {data.totalPages}</span>
              <button disabled={page + 1 >= data.totalPages} onClick={() => setPage(page + 1)} className="px-3 py-1 rounded border border-slate-200 bg-white disabled:opacity-40">Next ›</button>
            </div>
          </div>
        </div>
      </div>

      <ManualBillModal isOpen={isManualModalOpen} onClose={() => setIsManualModalOpen(false)}
        onSuccess={() => { setIsManualModalOpen(false); fetchBills(); setNotice('Invoice created.'); }} />
      {printId && <InvoicePreviewModal billId={printId} onClose={() => setPrintId(null)} />}
      {showReport && <BillReportModal filters={filterParams} onClose={() => setShowReport(false)} />}
      {editId && <EditBillModal billId={editId} onClose={() => setEditId(null)} onSaved={() => afterChange('Invoice updated.')} />}
      {payBill && <PaymentModal bill={payBill} onClose={() => setPayBill(null)} onSaved={() => afterChange('Payment recorded.')} />}
    </StaffLayout>
  );
};
export default Billing;
