/** "5 min ago", "2 h ago", "3 days ago", or the date for anything older than a week. Pure, so it can be tested. */
export function timeAgo(iso, now = new Date()) {
  if (!iso) return '';
  const then = new Date(iso);
  if (isNaN(then)) return '';
  const seconds = Math.floor((now.getTime() - then.getTime()) / 1000);
  if (seconds < 60) return 'just now';
  const minutes = Math.floor(seconds / 60);
  if (minutes < 60) return `${minutes} min ago`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours} h ago`;
  const days = Math.floor(hours / 24);
  if (days < 7) return `${days} day${days === 1 ? '' : 's'} ago`;
  return then.toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' });
}

export const NOTIFICATION_DOT = {
  TRANSFER_REQUESTED: 'bg-amber-400',
  TRANSFER_APPROVED: 'bg-green-500',
  TRANSFER_REJECTED: 'bg-red-500',
  TRANSFER_CANCELLED: 'bg-slate-400',
};
