import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

vi.mock('../../api', () => ({
  default: { get: vi.fn(), post: vi.fn(), delete: vi.fn() },
  errorMessage: vi.fn(async (_err, fallback) => fallback),
}));
import axios from '../../api';
import PortalAccessPanel from './PortalAccessPanel';

beforeEach(() => { vi.clearAllMocks(); });

describe('PortalAccessPanel', () => {
  it('tells staff whether the patient has portal access', () => {
    const { rerender } = render(<PortalAccessPanel patientId={7} status="NONE" canManage />);
    expect(screen.getByTestId('portal-status')).toHaveTextContent(/no portal access/i);
    rerender(<PortalAccessPanel patientId={7} status="INVITED" inviteExpiresAt="2026-11-02T10:00:00" canManage />);
    expect(screen.getByTestId('portal-status')).toHaveTextContent(/invitation sent/i);
    expect(screen.getByText(/link valid until/i)).toBeInTheDocument();
    rerender(<PortalAccessPanel patientId={7} status="ACTIVE" canManage />);
    expect(screen.getByTestId('portal-status')).toHaveTextContent(/active/i);
  });

  it('offers no link creation when the patient is already active or the user may not manage access', () => {
    const { rerender } = render(<PortalAccessPanel patientId={7} status="ACTIVE" canManage />);
    expect(screen.queryByRole('button', { name: /activation link/i })).not.toBeInTheDocument();
    rerender(<PortalAccessPanel patientId={7} status="NONE" canManage={false} />);
    expect(screen.queryByRole('button', { name: /activation link/i })).not.toBeInTheDocument();
  });

  it('creates a one-time link, shows it once with a warning, and never shows a password', async () => {
    axios.post.mockResolvedValue({ data: { token: 'tok123', activationPath: '/activate#t=tok123', expiresAt: '2026-11-02T10:00:00' } });
    render(<PortalAccessPanel patientId={7} status="NONE" canManage />);
    await userEvent.click(screen.getByRole('button', { name: /create activation link/i }));

    expect(axios.post).toHaveBeenCalledWith('/api/patients/7/portal-invitation');
    const box = await screen.findByTestId('activation-link');
    expect(screen.getByLabelText('Activation link')).toHaveValue(`${window.location.origin}/activate#t=tok123`);
    expect(box).toHaveTextContent(/only once/i);
    expect(box).toHaveTextContent(/choose their own email and password/i);
    expect(screen.getByTestId('portal-status')).toHaveTextContent(/invitation sent/i);
    expect(screen.queryByText(/password:/i)).not.toBeInTheDocument();
  });

  it('does not keep the link once the panel is gone (it is not stored anywhere)', async () => {
    axios.post.mockResolvedValue({ data: { token: 't', activationPath: '/activate#t=t', expiresAt: '2026-11-02T10:00:00' } });
    const { unmount } = render(<PortalAccessPanel patientId={7} status="NONE" canManage />);
    await userEvent.click(screen.getByRole('button', { name: /create activation link/i }));
    await screen.findByTestId('activation-link');
    unmount();
    render(<PortalAccessPanel patientId={7} status="INVITED" inviteExpiresAt="2026-11-02T10:00:00" canManage />);
    expect(screen.queryByTestId('activation-link')).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: /create a new link/i })).toBeInTheDocument();
  });

  it('shows the server\'s refusal', async () => {
    axios.post.mockRejectedValue(new Error('409'));
    render(<PortalAccessPanel patientId={7} status="NONE" canManage />);
    await userEvent.click(screen.getByRole('button', { name: /create activation link/i }));
    expect(await screen.findByRole('alert')).toHaveTextContent(/could not create the activation link/i);
    expect(screen.queryByTestId('activation-link')).not.toBeInTheDocument();
  });

  it('cancels an open link', async () => {
    axios.delete.mockResolvedValue({});
    const onChanged = vi.fn();
    render(<PortalAccessPanel patientId={7} status="INVITED" inviteExpiresAt="2026-11-02T10:00:00" canManage onChanged={onChanged} />);
    await userEvent.click(screen.getByRole('button', { name: /cancel link/i }));
    expect(axios.delete).toHaveBeenCalledWith('/api/patients/7/portal-invitation');
    expect(onChanged).toHaveBeenCalled();
  });
});
