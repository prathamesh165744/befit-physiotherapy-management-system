import React, { useEffect, useState } from 'react';
import axios, { errorMessage } from '../../api';
import PrintPortal from '../PrintPortal';
import { money, fmtDate, fmtDateTime, METHOD_LABEL } from '../../utils/format';

/**
 * "Print All": a summary REPORT of every invoice matching the current filters - the data comes from
 * GET /api/billing/report (all pages at once), not from the rows currently on screen.
 * Deliberately looks different from a single invoice: it is titled "Billing Report", has a filter summary, and totals.
 */
export default function BillReportModal({ filters, onClose }) {
  const [report, setReport] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    let alive = true;
    axios.get('/api/billing/report', { params: filters })
      .then((res) => { if (alive) setReport(res.data); })
      .catch(async (err) => { if (alive) setError(await errorMessage(err, 'Could not build the report.')); });
    return () => { alive = false; };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const th = { textAlign: 'left', padding: '5px 6px', borderBottom: '1px solid #cbd5e1', fontSize: '10px' };
  const thr = { ...th, textAlign: 'right' };
  const td = { padding: '4px 6px', borderBottom: '1px solid #e2e8f0', fontSize: '10.5px', verticalAlign: 'top' };
  const tdr = { ...td, textAlign: 'right' };

  return (
    <PrintPortal title="BeFit Billing Report" onClose={onClose} ready={!!report}>
      {!report && !error && <div className="text-center text-slate-500 py-20">Preparing report…</div>}
      {error && <div className="text-center text-red-600 py-20 font-semibold">{error}</div>}
      {report && (
        <div style={{ fontFamily: 'Arial, Helvetica, sans-serif' }}>
          <div style={{ borderBottom: '2px solid #0f172a', paddingBottom: '8px' }}>
            <div style={{ fontSize: '11px', fontWeight: 700, color: '#2563eb', letterSpacing: '1px' }}>BEFIT PHYSIOTHERAPY</div>
            <div style={{ fontSize: '20px', fontWeight: 800 }}>Billing Report</div>
            <div style={{ fontSize: '11px', marginTop: '4px' }}>Branch: <b>{report.branchLabel}</b></div>
            <div style={{ fontSize: '11px' }}>Filters: {report.filterSummary}</div>
            <div style={{ fontSize: '11px', color: '#64748b' }}>Generated {fmtDateTime(report.generatedAt)} by {report.generatedBy} • {report.totals.count} invoice(s)</div>
          </div>

          {report.truncated && (
            <div style={{ margin: '8px 0', padding: '6px 8px', background: '#fef3c7', border: '1px solid #f59e0b', fontSize: '11px' }}>
              Only the first {report.maxRows} invoices are shown. Narrow the date range to see the rest.
            </div>
          )}

          <table style={{ width: '100%', borderCollapse: 'collapse', marginTop: '10px' }} className="print-table">
            <thead>
              <tr style={{ background: '#f1f5f9' }}>
                <th style={th}>Invoice</th><th style={th}>Date</th><th style={th}>Patient</th><th style={th}>Branch</th>
                <th style={thr}>Total</th><th style={thr}>Paid</th><th style={thr}>Balance</th><th style={th}>Method</th><th style={th}>Status</th>
              </tr>
            </thead>
            <tbody>
              {report.rows.map((r) => (
                <tr key={r.id}>
                  <td style={td}>{r.invoiceNumber}</td>
                  <td style={td}>{fmtDate(r.billDate)}</td>
                  <td style={td}>{r.patientName}{r.patientId ? ` (REG-${r.patientId})` : ''}</td>
                  <td style={td}>{r.branchName}</td>
                  <td style={tdr}>{money(r.totalAmount)}</td>
                  <td style={tdr}>{money(r.amountPaid)}</td>
                  <td style={tdr}>{money(r.balanceDue)}</td>
                  <td style={td}>{METHOD_LABEL[r.paymentMethod] || '—'}</td>
                  <td style={td}>{r.status}</td>
                </tr>
              ))}
              {report.rows.length === 0 && (
                <tr><td colSpan={9} style={{ ...td, textAlign: 'center', padding: '24px' }}>No invoices match these filters.</td></tr>
              )}
            </tbody>
          </table>

          <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: '14px' }}>
            <table style={{ borderCollapse: 'collapse', fontSize: '11px', minWidth: '280px' }}>
              <tbody>
                {[['Subtotal', report.totals.subtotal], ['Discounts', report.totals.discount], ['Tax', report.totals.tax],
                  ['Total billed', report.totals.total], ['Collected', report.totals.paid], ['Outstanding', report.totals.outstanding]].map(([k, v], i) => (
                  <tr key={k}>
                    <td style={{ padding: '2px 8px', fontWeight: i >= 3 ? 700 : 400 }}>{k}</td>
                    <td style={{ padding: '2px 8px', textAlign: 'right', fontWeight: i >= 3 ? 700 : 400 }}>{money(v)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <div style={{ fontSize: '9px', color: '#64748b', marginTop: '6px', textAlign: 'right' }}>Cancelled invoices are listed but not included in the totals.</div>
        </div>
      )}
    </PrintPortal>
  );
}
