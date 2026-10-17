import React from 'react';
import { money, fmtDate, fmtDateTime, METHOD_LABEL } from '../../utils/format';

/** The printable A4 invoice for ONE bill. Pure display: give it the object from GET /api/billing/{id}. */
export default function InvoiceDocument({ data }) {
  const { bill, items, payments, branch, patient, appointment } = data;
  const cancelled = bill.status === 'CANCELLED';
  const statusColor = { PAID: '#15803d', PARTIAL: '#b45309', UNPAID: '#b91c1c', CANCELLED: '#64748b' }[bill.status] || '#334155';

  return (
    <div style={{ fontFamily: 'Arial, Helvetica, sans-serif', fontSize: '12px', lineHeight: 1.45, position: 'relative' }}>
      {cancelled && (
        <div style={{ position: 'absolute', top: '38%', left: 0, right: 0, textAlign: 'center', fontSize: '84px', fontWeight: 800,
          color: 'rgba(100,116,139,0.18)', transform: 'rotate(-20deg)', pointerEvents: 'none' }}>CANCELLED</div>
      )}

      <div style={{ display: 'flex', justifyContent: 'space-between', borderBottom: '2px solid #2563eb', paddingBottom: '10px' }}>
        <div>
          <div style={{ fontSize: '22px', fontWeight: 800, color: '#2563eb' }}>BeFit Physiotherapy</div>
          <div style={{ fontWeight: 700, marginTop: '2px' }}>{branch?.name || bill.branchName}</div>
          {branch?.address && <div>{branch.address}</div>}
          <div>{[branch?.phone, branch?.email].filter(Boolean).join('  •  ')}</div>
        </div>
        <div style={{ textAlign: 'right' }}>
          <div style={{ fontSize: '20px', fontWeight: 800, letterSpacing: '1px' }}>INVOICE</div>
          <div style={{ fontWeight: 700 }}>{bill.invoiceNumber}</div>
          <div>Date: {fmtDate(bill.billDate)}</div>
          <div style={{ marginTop: '4px', fontWeight: 800, color: statusColor }}>{bill.status}</div>
        </div>
      </div>

      <div style={{ display: 'flex', gap: '24px', marginTop: '14px' }}>
        <div style={{ flex: 1 }}>
          <div style={{ fontSize: '10px', fontWeight: 700, color: '#64748b', letterSpacing: '1px' }}>BILLED TO</div>
          <div style={{ fontWeight: 700, fontSize: '14px' }}>{patient?.fullName || bill.patientName}</div>
          {patient?.regId && <div>Patient ID: {patient.regId}</div>}
          {patient?.phone && <div>Phone: {patient.phone}</div>}
          {patient?.address && <div>{patient.address}</div>}
        </div>
        {appointment && (
          <div style={{ flex: 1 }}>
            <div style={{ fontSize: '10px', fontWeight: 700, color: '#64748b', letterSpacing: '1px' }}>APPOINTMENT</div>
            <div>Appointment #{appointment.id}</div>
            <div>{fmtDateTime(appointment.appointmentDate)}</div>
            {appointment.doctorName && <div>Doctor: {appointment.doctorName}</div>}
            {appointment.caseType && <div>Case: {appointment.caseType}</div>}
          </div>
        )}
      </div>

      <table style={{ width: '100%', borderCollapse: 'collapse', marginTop: '16px' }} className="print-table">
        <thead>
          <tr style={{ background: '#f1f5f9' }}>
            <th style={th('left', '6%')}>#</th>
            <th style={th('left')}>Treatment / Service</th>
            <th style={th('right', '9%')}>Qty</th>
            <th style={th('right', '17%')}>Unit price</th>
            <th style={th('right', '17%')}>Amount</th>
          </tr>
        </thead>
        <tbody>
          {items.map((it, i) => (
            <tr key={it.id ?? i}>
              <td style={td('left')}>{i + 1}</td>
              <td style={td('left')}>{it.description}</td>
              <td style={td('right')}>{it.quantity}</td>
              <td style={td('right')}>{money(it.unitPrice)}</td>
              <td style={td('right')}>{money(it.lineTotal)}</td>
            </tr>
          ))}
        </tbody>
      </table>

      <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: '12px' }}>
        <table style={{ width: '48%', borderCollapse: 'collapse' }}>
          <tbody>
            <Row label="Subtotal" value={money(bill.subtotal)} />
            {Number(bill.discountAmount) > 0 && <Row label="Discount" value={'− ' + money(bill.discountAmount)} />}
            {Number(bill.taxAmount) > 0 && <Row label={`Tax (${Number(bill.taxPercent)}%)`} value={money(bill.taxAmount)} />}
            <Row label="Total" value={money(bill.totalAmount)} bold line />
            <Row label="Amount paid" value={money(bill.amountPaid)} />
            <Row label="Balance due" value={money(bill.balanceDue)} bold color={Number(bill.balanceDue) > 0 ? '#b91c1c' : '#15803d'} />
          </tbody>
        </table>
      </div>

      <div style={{ marginTop: '16px' }}>
        <div style={{ fontSize: '10px', fontWeight: 700, color: '#64748b', letterSpacing: '1px', marginBottom: '4px' }}>PAYMENT DETAILS</div>
        <div>Payment status: <b>{bill.status}</b> &nbsp;|&nbsp; Payment method: <b>{METHOD_LABEL[bill.paymentMethod] || (payments.length ? bill.paymentMethod : '—')}</b></div>
        {payments.length > 0 && (
          <table style={{ width: '100%', borderCollapse: 'collapse', marginTop: '6px' }} className="print-table">
            <thead>
              <tr style={{ background: '#f1f5f9' }}>
                <th style={th('left')}>Date</th><th style={th('left')}>Method</th><th style={th('left')}>Reference</th><th style={th('right')}>Amount</th>
              </tr>
            </thead>
            <tbody>
              {payments.map((p) => (
                <tr key={p.id}>
                  <td style={td('left')}>{fmtDateTime(p.paymentDate)}</td>
                  <td style={td('left')}>{METHOD_LABEL[p.method] || p.method}</td>
                  <td style={td('left')}>{p.referenceNo || '—'}</td>
                  <td style={td('right')}>{money(p.amount)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      <div style={{ marginTop: '28px', borderTop: '1px solid #e2e8f0', paddingTop: '8px', color: '#64748b', fontSize: '10px', display: 'flex', justifyContent: 'space-between' }}>
        <span>Thank you for choosing BeFit Physiotherapy. This is a computer-generated invoice.</span>
        <span>Invoice {bill.invoiceNumber}</span>
      </div>
    </div>
  );
}

const th = (align, width) => ({ textAlign: align, padding: '6px 8px', borderBottom: '1px solid #cbd5e1', fontSize: '11px', width });
const td = (align) => ({ textAlign: align, padding: '6px 8px', borderBottom: '1px solid #e2e8f0', verticalAlign: 'top' });

function Row({ label, value, bold, line, color }) {
  return (
    <tr>
      <td style={{ padding: '3px 8px', borderTop: line ? '1px solid #94a3b8' : 'none', fontWeight: bold ? 700 : 400 }}>{label}</td>
      <td style={{ padding: '3px 8px', textAlign: 'right', borderTop: line ? '1px solid #94a3b8' : 'none', fontWeight: bold ? 700 : 400, color }}>{value}</td>
    </tr>
  );
}
