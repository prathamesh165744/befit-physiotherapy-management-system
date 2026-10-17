export const money = (n) =>
  '₹' + Number(n || 0).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });

export const fmtDate = (v) => {
  if (!v) return '—';
  const d = new Date(v);
  return isNaN(d) ? '—' : d.toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' });
};

export const fmtDateTime = (v) => {
  if (!v) return '—';
  const d = new Date(v);
  return isNaN(d)
    ? '—'
    : d.toLocaleString('en-IN', { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' });
};

export const STATUS_BADGE = {
  PAID: 'bg-green-50 text-green-700 border-green-100',
  PARTIAL: 'bg-amber-50 text-amber-700 border-amber-100',
  UNPAID: 'bg-red-50 text-red-700 border-red-100',
  CANCELLED: 'bg-slate-100 text-slate-500 border-slate-200',
};

export const METHOD_LABEL = {
  CASH: 'Cash', UPI: 'UPI', CARD: 'Card', BANK_TRANSFER: 'Bank transfer', CHEQUE: 'Cheque', OTHER: 'Other', MIXED: 'Mixed',
};

export const PAYMENT_METHODS = ['CASH', 'UPI', 'CARD', 'BANK_TRANSFER', 'CHEQUE', 'OTHER'];

/** A new random key per payment form, so a double click / retry cannot record the payment twice. */
export const newKey = () =>
  (typeof crypto !== 'undefined' && crypto.randomUUID) ? crypto.randomUUID() : `k-${Date.now()}-${Math.random().toString(16).slice(2)}`;

/** Local yyyy-MM-dd (not UTC), for <input type="date"> values. */
export const toISODate = (d) => {
  const p = (x) => String(x).padStart(2, '0');
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`;
};
