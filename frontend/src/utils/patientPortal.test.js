import { describe, it, expect } from 'vitest';
import { splitAppointments, TRANSFER_LABEL, APPOINTMENT_LABEL } from './patientPortal';

const NOW = new Date(2026, 9, 28, 12, 0, 0);               // 28 Oct 2026, noon
const at = (days, hour = 10) => new Date(2026, 9, 28 + days, hour, 0, 0).toISOString().slice(0, 19);
const appt = (id, days, status, branchId = 'A') => ({ id, appointmentDate: at(days), status, branchId });

describe('splitAppointments', () => {
  it('puts open visits from today on into upcoming, soonest first, and picks the next one', () => {
    const { upcoming, next } = splitAppointments([appt(1, 5, 'WAITING'), appt(2, 1, 'SCHEDULED'), appt(3, 0, 'CHECKED_IN')], NOW);
    expect(upcoming.map((a) => a.id)).toEqual([3, 2, 1]);
    expect(next.id).toBe(3);
  });

  it('keeps finished, cancelled, missed and past visits in the past list, newest first', () => {
    const { past, upcoming } = splitAppointments(
      [appt(1, -10, 'COMPLETED'), appt(2, -2, 'COMPLETED', 'B'), appt(3, 3, 'CANCELLED'), appt(4, -1, 'NO_SHOW'), appt(5, -3, 'WAITING')], NOW);
    expect(upcoming).toEqual([]);
    expect(past.map((a) => a.id)).toEqual([3, 4, 2, 5, 1]);
  });

  it('works for visits at any branch (the old and the new one after a transfer)', () => {
    const { upcoming, past } = splitAppointments([appt(1, 2, 'WAITING', 'B'), appt(2, -20, 'COMPLETED', 'A')], NOW);
    expect(upcoming.map((a) => a.branchId)).toEqual(['B']);
    expect(past.map((a) => a.branchId)).toEqual(['A']);
  });

  it('copes with nothing, null or undefined', () => {
    for (const input of [[], null, undefined]) {
      expect(splitAppointments(input, NOW)).toEqual({ upcoming: [], past: [], next: null });
    }
  });
});

describe('labels', () => {
  it('covers every transfer and appointment status a patient can see', () => {
    for (const s of ['PENDING', 'APPROVED', 'REJECTED', 'CANCELLED']) expect(TRANSFER_LABEL[s]).toBeTruthy();
    for (const s of ['SCHEDULED', 'WAITING', 'CHECKED_IN', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED', 'NO_SHOW']) expect(APPOINTMENT_LABEL[s]).toBeTruthy();
  });
});
