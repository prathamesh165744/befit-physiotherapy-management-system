// Small pure helpers for the patient portal (kept out of the components so they can be unit-tested).

/** Visits that are still ahead of the patient: open status and not in the past, soonest first. */
export const OPEN_STATUSES = ['SCHEDULED', 'WAITING', 'CHECKED_IN', 'IN_PROGRESS'];

/**
 * Splits the patient's appointments (all branches) into what is coming up and what is behind them.
 * upcoming: open visits dated today or later, soonest first.  past: everything else, newest first.
 */
export function splitAppointments(appointments, now = new Date()) {
  const startOfToday = new Date(now.getFullYear(), now.getMonth(), now.getDate());
  const list = Array.isArray(appointments) ? appointments : [];
  const time = (a) => new Date(a.appointmentDate).getTime();
  const upcoming = list
    .filter((a) => OPEN_STATUSES.includes(a.status) && time(a) >= startOfToday.getTime())
    .sort((a, b) => time(a) - time(b));
  const past = list.filter((a) => !upcoming.includes(a)).sort((a, b) => time(b) - time(a));
  return { upcoming, past, next: upcoming[0] || null };
}

export const TRANSFER_LABEL = {
  PENDING: 'Waiting for approval',
  APPROVED: 'Approved',
  REJECTED: 'Not approved',
  CANCELLED: 'Cancelled',
};

export const TRANSFER_BADGE = {
  PENDING: 'bg-amber-50 text-amber-700 border-amber-100',
  APPROVED: 'bg-green-50 text-green-700 border-green-100',
  REJECTED: 'bg-red-50 text-red-700 border-red-100',
  CANCELLED: 'bg-slate-100 text-slate-500 border-slate-200',
};

export const APPOINTMENT_LABEL = {
  SCHEDULED: 'Scheduled', WAITING: 'Booked', CHECKED_IN: 'Checked in', IN_PROGRESS: 'In progress',
  COMPLETED: 'Completed', CANCELLED: 'Cancelled', NO_SHOW: 'Missed',
};
