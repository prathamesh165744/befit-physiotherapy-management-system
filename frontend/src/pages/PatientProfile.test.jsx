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
import PatientProfile from './PatientProfile';

const profile = (over = {}) => ({ id: 7, fullName: 'Asha Kulkarni', phone: '9820000000', gender: 'Female', branchId: 'A', branchName: 'Branch A', pendingTransfer: null, ...over });
const branches = [{ id: 'A', name: 'Branch A' }, { id: 'B', name: 'Branch B' }];
const emptyHistory = { appointments: [], bills: [], branchHistory: [] };

function mockApi({ me = profile(), transfers = [], hist = emptyHistory, failExtras = false } = {}) {
  axios.get.mockImplementation((url) => {
    if (url === '/api/patients/me') return Promise.resolve({ data: me });
    if (failExtras) return Promise.reject(new Error('extras down'));
    if (url === '/api/branches') return Promise.resolve({ data: branches });
    if (url === '/api/patients/me/transfers') return Promise.resolve({ data: transfers });
    if (url === '/api/patients/me/history') return Promise.resolve({ data: hist });
    return Promise.reject(new Error('unexpected request ' + url));
  });
}

const renderPage = () => render(<MemoryRouter><PatientProfile /></MemoryRouter>);

beforeEach(() => { vi.clearAllMocks(); });

describe('PatientProfile', () => {
  it('loads the patient\'s own profile and shows loading first', async () => {
    mockApi();
    renderPage();
    expect(screen.getByRole('status')).toBeInTheDocument();
    expect(await screen.findByRole('heading', { name: 'Asha Kulkarni' })).toBeInTheDocument();
    expect(screen.getByText('9820000000')).toBeInTheDocument();
    expect(axios.get).toHaveBeenCalledWith('/api/patients/me');
  });

  it('shows an error with a retry when the profile cannot be loaded', async () => {
    axios.get.mockRejectedValue(new Error('down'));
    renderPage();
    expect(await screen.findByText(/could not load your profile/i)).toBeInTheDocument();
    mockApi();
    await userEvent.click(screen.getByRole('button', { name: /try again/i }));
    expect(await screen.findByRole('heading', { name: 'Asha Kulkarni' })).toBeInTheDocument();
  });

  it('keeps the profile visible and offers a retry when the visits and requests fail to load', async () => {
    mockApi({ failExtras: true });
    renderPage();
    expect(await screen.findByRole('heading', { name: 'Asha Kulkarni' })).toBeInTheDocument();
    expect(await screen.findByRole('alert')).toHaveTextContent(/could not load your visits/i);
    mockApi();
    await userEvent.click(screen.getByRole('button', { name: /try again/i }));
    await waitFor(() => expect(screen.queryByRole('alert')).not.toBeInTheDocument());
  });

  it('shows an empty state when there are no branch change requests', async () => {
    mockApi();
    renderPage();
    expect(await screen.findByTestId('no-transfers')).toBeInTheDocument();
  });

  it('tracks branch change requests with their status and decision', async () => {
    mockApi({ transfers: [
      { id: 1, fromBranchName: 'Branch A', toBranchName: 'Branch B', status: 'REJECTED', requestedAt: '2026-10-01T10:00:00', decidedAt: '2026-10-02T10:00:00', decisionNote: 'No capacity' },
      { id: 2, fromBranchName: 'Branch A', toBranchName: 'Branch B', status: 'APPROVED', requestedAt: '2026-10-10T10:00:00', decidedAt: '2026-10-11T10:00:00' },
    ] });
    renderPage();
    expect(await screen.findByText('Not approved')).toBeInTheDocument();
    expect(screen.getByText('Approved')).toBeInTheDocument();
    expect(screen.getByText(/No capacity/)).toBeInTheDocument();
  });

  it('requests a branch change for the logged-in patient and confirms it', async () => {
    mockApi();
    axios.put.mockResolvedValue({ data: { status: 'PENDING' } });
    renderPage();
    await screen.findByRole('heading', { name: 'Asha Kulkarni' });
    await userEvent.selectOptions(screen.getByRole('combobox'), 'B');
    await userEvent.click(screen.getByRole('button', { name: /request branch change/i }));
    expect(axios.put).toHaveBeenCalledWith('/api/patients/me/branch', { branchId: 'B', reason: null });
    expect(await screen.findByText(/request sent/i)).toBeInTheDocument();
  });

  it('shows the server\'s reason when a request is refused', async () => {
    mockApi();
    axios.put.mockRejectedValue(new Error('conflict'));
    renderPage();
    await screen.findByRole('heading', { name: 'Asha Kulkarni' });
    await userEvent.selectOptions(screen.getByRole('combobox'), 'B');
    await userEvent.click(screen.getByRole('button', { name: /request branch change/i }));
    expect(await screen.findByText(/could not request the branch change/i)).toBeInTheDocument();
  });

  it('lets the patient cancel their own pending request', async () => {
    mockApi({ me: profile({ pendingTransfer: { id: 9, toBranchName: 'Branch B', requestedAt: '2026-10-20T10:00:00' } }) });
    axios.delete.mockResolvedValue({ data: {} });
    renderPage();
    await userEvent.click(await screen.findByRole('button', { name: /cancel request/i }));
    expect(axios.delete).toHaveBeenCalledWith('/api/patients/me/branch-request');
    expect(await screen.findByText(/request cancelled/i)).toBeInTheDocument();
  });

  it('shows visits and invoices from every branch', async () => {
    mockApi({ hist: { appointments: [
      { id: 1, appointmentDate: '2026-09-01T10:00:00', branchId: 'A', branchName: 'Branch A', caseType: 'Consultation', doctorName: 'Dr Rao', status: 'COMPLETED' },
      { id: 2, appointmentDate: '2026-10-15T10:00:00', branchId: 'B', branchName: 'Branch B', caseType: 'Session', doctorName: 'Dr N', status: 'COMPLETED' },
    ], bills: [], branchHistory: [] } });
    renderPage();
    expect(await screen.findByText(/Dr Rao/)).toBeInTheDocument();
    expect(screen.getByText(/Dr N/)).toBeInTheDocument();
    expect(screen.getByText('No invoices yet.')).toBeInTheDocument();
  });
});
