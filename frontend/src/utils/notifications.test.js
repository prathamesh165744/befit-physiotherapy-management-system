import { describe, it, expect } from 'vitest';
import { timeAgo, NOTIFICATION_DOT } from './notifications';

const NOW = new Date('2026-11-02T12:00:00');
const ago = (ms) => new Date(NOW.getTime() - ms).toISOString().slice(0, 19);

describe('timeAgo', () => {
  it('describes recent times in words', () => {
    expect(timeAgo(ago(20 * 1000), NOW)).toBe('just now');
    expect(timeAgo(ago(5 * 60 * 1000), NOW)).toBe('5 min ago');
    expect(timeAgo(ago(3 * 3600 * 1000), NOW)).toBe('3 h ago');
    expect(timeAgo(ago(86400 * 1000), NOW)).toBe('1 day ago');
    expect(timeAgo(ago(3 * 86400 * 1000), NOW)).toBe('3 days ago');
  });
  it('shows a date after a week and copes with bad input', () => {
    expect(timeAgo(ago(20 * 86400 * 1000), NOW)).toMatch(/2026/);
    expect(timeAgo(null, NOW)).toBe('');
    expect(timeAgo('not a date', NOW)).toBe('');
  });
  it('has a colour for every notification type', () => {
    ['TRANSFER_REQUESTED', 'TRANSFER_APPROVED', 'TRANSFER_REJECTED', 'TRANSFER_CANCELLED'].forEach((t) => expect(NOTIFICATION_DOT[t]).toBeTruthy());
  });
});
