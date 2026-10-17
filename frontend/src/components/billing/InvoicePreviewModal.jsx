import React, { useEffect, useState } from 'react';
import axios, { errorMessage } from '../../api';
import PrintPortal from '../PrintPortal';
import InvoiceDocument from './InvoiceDocument';

/** Print preview of ONE invoice. Loads the invoice by id, shows it, and prints only that invoice. */
export default function InvoicePreviewModal({ billId, onClose }) {
  const [data, setData] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    let alive = true;
    setData(null); setError('');
    axios.get(`/api/billing/${billId}`)
      .then((res) => { if (alive) setData(res.data); })
      .catch(async (err) => { if (alive) setError(await errorMessage(err, 'Could not load this invoice.')); });
    return () => { alive = false; };
  }, [billId]);

  return (
    <PrintPortal title={data ? `Invoice ${data.bill.invoiceNumber}` : 'Invoice'} onClose={onClose} ready={!!data}>
      {!data && !error && <div className="text-center text-slate-500 py-20">Loading invoice…</div>}
      {error && <div className="text-center text-red-600 py-20 font-semibold">{error}</div>}
      {data && <InvoiceDocument data={data} />}
    </PrintPortal>
  );
}
