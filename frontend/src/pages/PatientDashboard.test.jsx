import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';

vi.mock('../api', () => ({
  default: { get: vi.fn(), put: vi.fn(), delete: vi.fn() },
  errorMessage: vi.fn(async (_err, fallback) => fallback),
}));
import axios from '../api';
import PatientDashboard from './PatientDashboard';

const future = (days) => new Date(Date.now() + days * 86400000).toISOString().slice(0, 19);
const past = (days) => new Date(Date.now() - days * 86400000).toISOString().slice(0, 19);

const profile = (over = {}) => ({ id: 7, fullName: 'Asha Kulkarni', branchId: 'A', branchName: 'Branch A', pendingTransfer: null, ...over });
const history = (appointments = []) => ({ appointments, bills: [], branchHistory: [] });

function mockApi({ me = profile(), hist = history(), transfers = [] } = {}) {
  axios.get.mockImplementation((url) => {
    if (url === '/api/patients/me') return Promise.resolve({ data: me });
    if (url === '/api/patients/me/history') return Promise.resolve({ data: hist });
    if (url === '/api/patients/me/transfers') return Promise.resolve({ data: transfers });
    return Promise.reject(new Error('unexpected request ' + url));
  });
}

const renderPage = () => render(<MemoryRouter><PatientDashboard /></MemoryRouter>);

beforeEach(() => { vi.clearAllMocks(); });

describe('PatientDashboard', () => {
  it('shows a loading state, then the patient\'s own dashboard', async () => {
    mockApi();
    renderPage();
    expect(screen.getByRole('status')).toHaveTextContent(/loading/i);
    expect(await screen.findByText(/Welcome, Asha/)).toBeInTheDocument();
    expect(screen.getByText('Branch A')).toBeInTheDocument();
  });

  it('only uses the patient self-service endpoints - no patient id, no branch filtering', async () => {
    mockApi();
    renderPage();
    await screen.findByText(/Welcome, Asha/);
    const urls = axios.get.mock.calls.map((c) => c[0]).sort();
    expect(urls).toEqual(['/api/patients/me', '/api/patients/me/history', '/api/patients/me/transfers']);
    axios.get.mock.calls.forEach((c) => expect(c[1]?.params).toBeUndefined());
  });

  it('shows the next appointment with its branch, and marks a visit away from the patient\'s branch', async () => {
    mockApi({ hist: history([
      { id: 1, appointmentDate: future(3), status: 'WAITING', caseType: 'Follow-up', doctorName: 'Dr Rao', branchId: 'B', branchName: 'Branch B', crossBranch: true },
      { id: 2, appointmentDate: past(10), status: 'COMPLETED', caseType: 'Consultation', doctorName: 'Dr Rao', branchId: 'A', branchName: 'Branch A' },
    ]) });
    renderPage();
    const next = await screen.findByTestId('next-appointment');
    expect(next).toHaveTextContent('Dr Rao');
    expect(next).toHaveTextContent('Branch B');
    expect(next).toHaveTextContent(/away from your branch/i);
    expect(screen.queryByTestId('no-upcoming')).not.toBeInTheDocument();
  });

  it('lists visits from every branch, including the old branch after a transfer', async () => {
    mockApi({
      me: profile({ branchId: 'B', branchName: 'Branch B' }),
      hist: history([
        { id: 1, appointmentDate: past(2), status: 'COMPLETED', caseType: 'Session', doctorName: 'Dr N', branchId: 'B', branchName: 'Branch B' },
        { id: 2, appointmentDate: past(40), status: 'COMPLETED', caseType: 'Consultation', doctorName: 'Dr Rao', branchId: 'A', branchName: 'Branch A' },
      ]),
      transfers: [{ id: 5, fromBranchName: 'Branch A', toBranchName: 'Branch B', status: 'APPROVED', decisionNote: 'Welcome' }],
    });
    renderPage();
    await screen.findByText(/Welcome, Asha/);
    const rows = screen.getAllByRole('row');
    expect(rows.some((r) => r.textContent.includes('Branch A') && r.textContent.includes('Dr Rao'))).toBe(true);
    expect(rows.some((r) => r.textContent.includes('Branch B') && r.textContent.includes('Dr N'))).toBe(true);
    expect(screen.getByTestId('transfer-banner')).toHaveTextContent(/approved/i);
  });

  it('shows empty states when there are no visits', async () => {
    mockApi();
    renderPage();
    expect(await screen.findByTestId('no-visits')).toBeInTheDocument();
    expect(screen.getByTestId('no-upcoming')).toBeInTheDocument();
  });

  it('shows a pending branch-change request', async () => {
    mockApi({ me: profile({ pendingTransfer: { id: 9, toBranchName: 'Branch B', requestedAt: past(1) } }),
              transfers: [{ id: 9, fromBranchName: 'Branch A', toBranchName: 'Branch B', status: 'PENDING' }] });
    renderPage();
    expect(await screen.findByTestId('transfer-banner')).toHaveTextContent(/waiting for approval/i);
  });

  it('shows an error with a retry button, and recovers when the retry works', async () => {
    axios.get.mockRejectedValue(new Error('boom'));
    renderPage();
    expect(await screen.findByRole('alert')).toHaveTextContent(/could not load your dashboard/i);

    mockApi();
    await userEvent.click(screen.getByRole('button', { name: /try again/i }));
    expect(await screen.findByText(/Welcome, Asha/)).toBeInTheDocument();
    await waitFor(() => expect(screen.queryByRole('alert')).not.toBeInTheDocument());
  });
});
