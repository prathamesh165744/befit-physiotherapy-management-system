import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';

vi.mock('../api', () => ({
  default: { get: vi.fn(), post: vi.fn() },
  errorMessage: vi.fn(async (_err, fallback) => fallback),
}));
import axios from '../api';
import ActivateAccount from './ActivateAccount';

const renderAt = (path) => render(<MemoryRouter initialEntries={[path]}><ActivateAccount /></MemoryRouter>);

async function fillAndSubmit({ email = 'asha@mail.test', password = 'my-own-password', confirm = password } = {}) {
  await userEvent.type(screen.getByLabelText('Email'), email);
  await userEvent.type(screen.getByLabelText('Password'), password);
  await userEvent.type(screen.getByLabelText('Confirm password'), confirm);
  await userEvent.click(screen.getByRole('button', { name: /activate account/i }));
}

beforeEach(() => { vi.clearAllMocks(); localStorage.clear(); });

describe('ActivateAccount', () => {
  it('says the link is invalid when there is no token, without calling the server', () => {
    renderAt('/activate');
    expect(screen.getByRole('alert')).toHaveTextContent(/invalid or has expired/i);
    expect(axios.post).not.toHaveBeenCalled();
  });

  it('says the same for a token the server does not accept', async () => {
    axios.post.mockResolvedValue({ data: { valid: false } });
    renderAt('/activate#t=old');
    expect(await screen.findByRole('alert')).toHaveTextContent(/invalid or has expired/i);
    expect(screen.queryByLabelText('Password')).not.toBeInTheDocument();
  });

  it('treats a failed check as an invalid link', async () => {
    axios.post.mockRejectedValue(new Error('down'));
    renderAt('/activate#t=abc');
    expect(await screen.findByRole('alert')).toHaveTextContent(/invalid or has expired/i);
  });

  it('checks the link, then lets the patient choose their own email and password', async () => {
    axios.post.mockResolvedValueOnce({ data: { valid: true, firstName: 'Asha' } });
    renderAt('/activate#t=abc123');
    expect(screen.getByRole('status')).toHaveTextContent(/checking/i);
    expect(await screen.findByText(/Welcome, Asha/)).toBeInTheDocument();
    expect(axios.post).toHaveBeenCalledWith('/api/auth/invitations/check', { token: 'abc123' });
  });

  it('refuses mismatching passwords without calling the server', async () => {
    axios.post.mockResolvedValueOnce({ data: { valid: true, firstName: 'Asha' } });
    renderAt('/activate#t=abc123');
    await screen.findByLabelText('Email');
    await fillAndSubmit({ password: 'my-own-password', confirm: 'something-else-1' });
    expect(await screen.findByRole('alert')).toHaveTextContent(/do not match/i);
    expect(axios.post).toHaveBeenCalledTimes(1);          // only the link check
  });

  it('activates the account and sends the patient to sign in - it does not sign them in', async () => {
    axios.post.mockResolvedValueOnce({ data: { valid: true, firstName: 'Asha' } }).mockResolvedValueOnce({ data: { message: 'ok' } });
    renderAt('/activate#t=abc123');
    await screen.findByLabelText('Email');
    await fillAndSubmit();
    expect(axios.post).toHaveBeenLastCalledWith('/api/auth/activate', { token: 'abc123', email: 'asha@mail.test', password: 'my-own-password' });
    expect(await screen.findByTestId('activated')).toHaveTextContent(/account is ready/i);
    expect(screen.getByRole('link', { name: /sign in/i })).toHaveAttribute('href', '/login');
    expect(localStorage.getItem('token')).toBeNull();
  });

  it('shows the server\'s message when activation fails (e.g. the email is taken)', async () => {
    axios.post.mockResolvedValueOnce({ data: { valid: true, firstName: 'Asha' } }).mockRejectedValueOnce(new Error('409'));
    renderAt('/activate#t=abc123');
    await screen.findByLabelText('Email');
    await fillAndSubmit();
    expect(await screen.findByRole('alert')).toHaveTextContent(/could not activate/i);
    expect(screen.queryByTestId('activated')).not.toBeInTheDocument();
  });
});
