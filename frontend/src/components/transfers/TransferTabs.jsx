import React from 'react';
import { Link } from 'react-router-dom';

/** Small switch shown at the top of both transfer screens. */
export default function TransferTabs({ active }) {
  const tab = (key, to, label) => (
    <Link to={to} className={`px-4 py-2 rounded-lg text-[13px] font-bold ${active === key ? 'bg-blue-50 text-blue-700' : 'text-slate-500 hover:bg-slate-100'}`}>{label}</Link>
  );
  return (
    <div className="flex gap-2 mb-5">
      {tab('incoming', '/staff-dashboard/transfers', 'Incoming (to this branch)')}
      {tab('outgoing', '/staff-dashboard/transfers/outgoing', 'Outgoing (from this branch)')}
    </div>
  );
}

export const STATUS_BADGE = {
  PENDING: 'bg-amber-50 text-amber-700 border-amber-100',
  APPROVED: 'bg-green-50 text-green-700 border-green-100',
  REJECTED: 'bg-red-50 text-red-700 border-red-100',
  CANCELLED: 'bg-slate-100 text-slate-500 border-slate-200',
};
