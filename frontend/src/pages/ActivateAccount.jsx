import React, { useState, useEffect } from 'react';
import { Link, useLocation } from 'react-router-dom';
import axios, { errorMessage } from '../api';

/**
 * Public page opened from the activation link a clinic gave the patient: /activate#t=<token>.
 * The token is read from the URL fragment (never sent to a server by the browser), checked, and then the patient chooses
 * their OWN email and password. Nothing here signs the patient in - they continue to the normal login page.
 */
export default function ActivateAccount() {
  const { hash } = useLocation();
  const token = new URLSearchParams(hash.replace(/^#/, '')).get('t') || '';
  const [state, setState] = useState(token ? 'checking' : 'invalid');    // checking | invalid | ready | done
  const [firstName, setFirstName] = useState('');
  const [form, setForm] = useState({ email: '', password: '', confirm: '' });
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (!token) return undefined;
    let cancelled = false;
    axios.post('/api/auth/invitations/check', { token })
      .then((res) => {
        if (cancelled) return;
        if (res.data.valid) { setFirstName(res.data.firstName || ''); setState('ready'); } else { setState('invalid'); }
      })
      .catch(() => { if (!cancelled) setState('invalid'); });
    return () => { cancelled = true; };
  }, [token]);

  const submit = async (e) => {
    e.preventDefault();
    setError('');
    if (form.password.length < 8) { setError('Password must be at least 8 characters.'); return; }
    if (form.password !== form.confirm) { setError('The two passwords do not match.'); return; }
    setBusy(true);
    try {
      await axios.post('/api/auth/activate', { token, email: form.email.trim(), password: form.password });
      setState('done');
    } catch (err) {
      setError(await errorMessage(err, 'We could not activate your account. Please try again.'));
    } finally { setBusy(false); }
  };

  const field = 'w-full bg-[#f4f7fe] border border-transparent rounded-lg px-4 py-3 text-sm outline-none focus:border-blue-500 focus:bg-white transition';

  return (
    <div className="min-h-screen bg-[#f8f9fc] flex items-center justify-center p-4 font-sans">
      <div className="w-full max-w-md bg-white rounded-2xl shadow-sm border border-slate-100 p-8">
        <h1 className="text-2xl font-bold text-slate-800 mb-1">Activate your BeFit account</h1>

        {state === 'checking' && <p role="status" className="text-sm text-slate-500 mt-4">Checking your link…</p>}

        {state === 'invalid' && (
          <div role="alert" className="mt-4 text-sm text-slate-600">
            <p className="text-red-600 font-bold mb-2">This activation link is invalid or has expired.</p>
            <p>Please ask your BeFit branch for a new link. Already activated? <Link to="/login" className="text-blue-600 font-bold">Sign in</Link>.</p>
          </div>
        )}

        {state === 'done' && (
          <div data-testid="activated" className="mt-4 text-sm text-slate-600">
            <p className="text-green-700 font-bold mb-3">Your account is ready.</p>
            <Link to="/login" className="inline-block bg-blue-600 hover:bg-blue-700 text-white px-6 py-2.5 rounded-lg font-bold">Sign in</Link>
          </div>
        )}

        {state === 'ready' && (
          <form onSubmit={submit} className="mt-4 space-y-4">
            <p className="text-sm text-slate-500">{firstName ? `Welcome, ${firstName}. ` : ''}Choose the email and password you will use to sign in.</p>
            {error && <p role="alert" className="bg-red-50 border border-red-100 text-red-700 text-sm rounded-lg p-3">{error}</p>}
            <div>
              <label htmlFor="act-email" className="block text-xs font-bold text-slate-500 uppercase tracking-wider mb-1.5">Email</label>
              <input id="act-email" type="email" required autoComplete="email" className={field} value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} />
            </div>
            <div>
              <label htmlFor="act-pw" className="block text-xs font-bold text-slate-500 uppercase tracking-wider mb-1.5">Password</label>
              <input id="act-pw" type="password" required minLength={8} autoComplete="new-password" className={field} value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} />
            </div>
            <div>
              <label htmlFor="act-pw2" className="block text-xs font-bold text-slate-500 uppercase tracking-wider mb-1.5">Confirm password</label>
              <input id="act-pw2" type="password" required autoComplete="new-password" className={field} value={form.confirm} onChange={(e) => setForm({ ...form, confirm: e.target.value })} />
            </div>
            <button type="submit" disabled={busy} className="w-full bg-blue-600 hover:bg-blue-700 text-white py-3 rounded-lg font-bold disabled:opacity-50">{busy ? 'Activating…' : 'Activate account'}</button>
          </form>
        )}
      </div>
    </div>
  );
}
