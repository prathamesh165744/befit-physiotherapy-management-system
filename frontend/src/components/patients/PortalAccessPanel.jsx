import React, { useState } from 'react';
import axios, { errorMessage } from '../../api';
import { fmtDateTime } from '../../utils/format';

export const PORTAL_BADGE = {
  ACTIVE: 'bg-green-50 text-green-700 border-green-100',
  INVITED: 'bg-amber-50 text-amber-700 border-amber-100',
  NONE: 'bg-slate-100 text-slate-500 border-slate-200',
};
export const PORTAL_LABEL = { ACTIVE: 'Portal access: active', INVITED: 'Invitation sent', NONE: 'No portal access' };

/**
 * Portal access of one patient, for staff: shows the state and, for ADMIN / RECEPTIONIST of the patient's own branch, lets them
 * create a one-time activation link. The link is shown ONCE (the server keeps only a hash), so it has to be handed to the
 * patient now; it expires, and creating a new one cancels the old one. The patient then picks their own e-mail and password.
 * No password is ever created or shown here.
 */
export default function PortalAccessPanel({ patientId, status = 'NONE', inviteExpiresAt = null, canManage = false, onChanged }) {
  const [link, setLink] = useState(null);          // { url, expiresAt } - only in memory, never stored
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [copied, setCopied] = useState(false);

  const create = async () => {
    setBusy(true); setError(''); setCopied(false);
    try {
      const res = await axios.post(`/api/patients/${patientId}/portal-invitation`);
      // no parent reload here: it could unmount this panel and lose the one-time link. The state shown comes from `link` until reopened.
      setLink({ url: `${window.location.origin}${res.data.activationPath}`, expiresAt: res.data.expiresAt });
    } catch (err) {
      setError(await errorMessage(err, 'Could not create the activation link.'));
    } finally { setBusy(false); }
  };

  const revoke = async () => {
    setBusy(true); setError('');
    try {
      await axios.delete(`/api/patients/${patientId}/portal-invitation`);
      setLink(null);
      onChanged && onChanged();
    } catch (err) {
      setError(await errorMessage(err, 'Could not cancel the activation link.'));
    } finally { setBusy(false); }
  };

  const copy = async () => {
    try { await navigator.clipboard.writeText(link.url); setCopied(true); } catch { setCopied(false); }
  };

  const effective = link ? 'INVITED' : status;

  return (
    <div data-testid="portal-panel">
      <div className="flex flex-wrap items-center gap-3">
        <span data-testid="portal-status" className={`text-[10px] font-bold px-2 py-1 rounded border uppercase ${PORTAL_BADGE[effective] || PORTAL_BADGE.NONE}`}>
          {PORTAL_LABEL[effective] || PORTAL_LABEL.NONE}
        </span>
        {effective === 'INVITED' && (link?.expiresAt || inviteExpiresAt) && (
          <span className="text-[12px] text-slate-500">Link valid until {fmtDateTime(link?.expiresAt || inviteExpiresAt)}</span>
        )}
        {canManage && status !== 'ACTIVE' && (
          <>
            <button type="button" disabled={busy} onClick={create} className="text-[12px] font-bold text-blue-600 hover:underline disabled:opacity-50">
              {effective === 'INVITED' ? 'Create a new link' : 'Create activation link'}
            </button>
            {effective === 'INVITED' && (
              <button type="button" disabled={busy} onClick={revoke} className="text-[12px] font-bold text-red-600 hover:underline disabled:opacity-50">Cancel link</button>
            )}
          </>
        )}
      </div>
      {status === 'ACTIVE' && <p className="text-[12px] text-slate-500 mt-2">This patient can sign in to the patient portal.</p>}
      {status === 'NONE' && !link && <p className="text-[12px] text-slate-500 mt-2">Registered at the clinic only. The patient can use the portal after they activate an account{canManage ? ' with a link from you.' : '.'}</p>}
      {error && <p role="alert" className="text-[12px] text-red-600 mt-2">{error}</p>}
      {link && (
        <div data-testid="activation-link" className="mt-3 bg-amber-50 border border-amber-200 rounded-lg p-3">
          <p className="text-[12px] font-bold text-amber-900 mb-2">Give this link to the patient now. It is shown only once and can be used once.</p>
          <div className="flex gap-2">
            <input readOnly value={link.url} onFocus={(e) => e.target.select()} aria-label="Activation link" className="flex-1 bg-white border border-amber-200 rounded px-3 py-2 text-[12px]" />
            <button type="button" onClick={copy} className="px-3 rounded bg-amber-600 text-white text-[12px] font-bold">{copied ? 'Copied' : 'Copy'}</button>
          </div>
          <p className="text-[11px] text-amber-800 mt-2">The patient chooses their own email and password on that page. Never share the link by anything other than handing it to the patient.</p>
        </div>
      )}
    </div>
  );
}
