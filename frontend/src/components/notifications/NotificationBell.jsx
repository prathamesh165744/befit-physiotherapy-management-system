import React, { useState, useEffect, useCallback, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import axios, { errorMessage } from '../../api';
import { fmtDateTime } from '../../utils/format';
import { timeAgo, NOTIFICATION_DOT } from '../../utils/notifications';

const POLL_MS = 30000;

/**
 * The bell in the top bar of the staff and patient layouts: unread badge, a list of the user's OWN notifications (the server
 * only ever returns those), read / unread state, "mark all as read", and a link to the related page - the server sends a link only
 * when this user may open it, so a notification without a link is shown as plain text.
 */
export default function NotificationBell() {
  const navigate = useNavigate();
  const [unread, setUnread] = useState(0);
  const [open, setOpen] = useState(false);
  const [items, setItems] = useState([]);
  const [status, setStatus] = useState('idle');      // idle | loading | ready | error
  const [error, setError] = useState('');
  const box = useRef(null);

  const refreshCount = useCallback(async () => {
    try {
      const res = await axios.get('/api/notifications/unread-count');
      setUnread(res.data.unread || 0);
    } catch { /* a missed poll is harmless; the next one will catch up */ }
  }, []);

  const loadList = useCallback(async () => {
    setStatus('loading'); setError('');
    try {
      const res = await axios.get('/api/notifications', { params: { limit: 20 } });
      setItems(res.data);
      setStatus('ready');
    } catch (err) {
      setError(await errorMessage(err, 'We could not load your notifications.'));
      setStatus('error');
    }
  }, []);

  useEffect(() => {
    refreshCount();
    const timer = setInterval(refreshCount, POLL_MS);
    return () => clearInterval(timer);
  }, [refreshCount]);

  useEffect(() => {
    if (!open) return undefined;
    const onKey = (e) => { if (e.key === 'Escape') setOpen(false); };
    const onClick = (e) => { if (box.current && !box.current.contains(e.target)) setOpen(false); };
    document.addEventListener('keydown', onKey);
    document.addEventListener('mousedown', onClick);
    return () => { document.removeEventListener('keydown', onKey); document.removeEventListener('mousedown', onClick); };
  }, [open]);

  const toggle = () => {
    const next = !open;
    setOpen(next);
    if (next) { loadList(); refreshCount(); }
  };

  const openItem = async (n) => {
    if (!n.read) {
      try {
        const res = await axios.post(`/api/notifications/${n.id}/read`);
        setItems((list) => list.map((x) => (x.id === n.id ? { ...x, read: true, readAt: res.data.readAt } : x)));
        setUnread((c) => Math.max(0, c - 1));
      } catch (err) {
        setError(await errorMessage(err, 'Could not update that notification.'));
        return;
      }
    }
    if (n.link) { setOpen(false); navigate(n.link); }
  };

  const markAll = async () => {
    try {
      await axios.post('/api/notifications/read-all');
      setItems((list) => list.map((x) => ({ ...x, read: true })));
      setUnread(0);
    } catch (err) {
      setError(await errorMessage(err, 'Could not mark your notifications as read.'));
    }
  };

  return (
    <div ref={box} className="relative">
      <button type="button" onClick={toggle} aria-label="Notifications" aria-haspopup="true" aria-expanded={open}
        className="relative text-slate-400 hover:text-blue-600 transition p-1">
        <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.5" d="M15 17h5l-1.4-1.4A2 2 0 0118 14.2V11a6 6 0 10-12 0v3.2a2 2 0 01-.6 1.4L4 17h5m6 0a3 3 0 11-6 0" />
        </svg>
        {unread > 0 && (
          <span data-testid="unread-badge" className="absolute -top-1 -right-1 min-w-[16px] h-4 px-1 rounded-full bg-red-500 text-white text-[9px] font-bold flex items-center justify-center border border-white">
            {unread > 99 ? '99+' : unread}
          </span>
        )}
      </button>

      {open && (
        <div role="dialog" aria-label="Notifications" className="absolute right-0 mt-3 w-80 sm:w-96 bg-white border border-slate-100 rounded-2xl shadow-xl z-50 overflow-hidden">
          <div className="flex justify-between items-center px-4 py-3 border-b border-slate-100">
            <h2 className="text-sm font-bold text-slate-800">Notifications</h2>
            {unread > 0 && <button type="button" onClick={markAll} className="text-[12px] font-bold text-blue-600 hover:underline">Mark all as read</button>}
          </div>

          {error && status !== 'error' && <p role="alert" className="px-4 py-2 text-[12px] text-red-600 bg-red-50">{error}</p>}

          <div className="max-h-96 overflow-y-auto">
            {status === 'loading' && <p role="status" className="px-4 py-6 text-center text-sm text-slate-400">Loading…</p>}
            {status === 'error' && (
              <div role="alert" className="px-4 py-6 text-center">
                <p className="text-sm text-red-600 mb-3">{error}</p>
                <button type="button" onClick={loadList} className="text-[12px] font-bold text-blue-600 hover:underline">Try again</button>
              </div>
            )}
            {status === 'ready' && items.length === 0 && <p data-testid="no-notifications" className="px-4 py-8 text-center text-sm text-slate-400">You’re all caught up.</p>}
            {status === 'ready' && items.length > 0 && (
              <ul className="divide-y divide-slate-100">
                {items.map((n) => (
                  <li key={n.id}>
                    <button type="button" onClick={() => openItem(n)} data-testid="notification-item" data-read={n.read ? 'true' : 'false'}
                      className={`w-full text-left px-4 py-3 flex gap-3 hover:bg-slate-50 ${n.read ? '' : 'bg-blue-50/40'} ${n.link ? 'cursor-pointer' : 'cursor-default'}`}>
                      <span className={`mt-1.5 w-2 h-2 rounded-full shrink-0 ${n.read ? 'bg-transparent' : (NOTIFICATION_DOT[n.type] || 'bg-blue-500')}`} aria-hidden="true" />
                      <span className="flex-1 min-w-0">
                        <span className={`block text-[13px] leading-snug ${n.read ? 'text-slate-500' : 'text-slate-800 font-semibold'}`}>{n.message}</span>
                        <span className="block text-[11px] text-slate-400 mt-1" title={fmtDateTime(n.createdAt)}>
                          {timeAgo(n.createdAt)}{n.link ? ' · Open' : ''}
                        </span>
                      </span>
                    </button>
                  </li>
                ))}
              </ul>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
