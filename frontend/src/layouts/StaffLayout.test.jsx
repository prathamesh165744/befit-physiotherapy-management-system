import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

vi.mock('../api', () => ({
  default: { get: vi.fn(), post: vi.fn() },
  errorMessage: vi.fn(async (_err, fallback) => fallback),
}));
import axios from '../api';
import StaffLayout from './StaffLayout';

beforeEach(() => {
  vi.clearAllMocks();
  localStorage.clear();
  localStorage.setItem('role', 'BRANCH_MANAGER');
  localStorage.setItem('branchId', 'B');
  axios.get.mockImplementation((url) => {
    if (url === '/api/notifications/unread-count') return Promise.resolve({ data: { unread: 2 } });
    return Promise.resolve({ data: [] });                       // branches etc.
  });
});

describe('StaffLayout notifications', () => {
  it('shows the real notification bell with the unread count, not the old placeholder', async () => {
    const alertSpy = vi.spyOn(window, 'alert').mockImplementation(() => {});
    render(<MemoryRouter><StaffLayout><div>page</div></StaffLayout></MemoryRouter>);
    expect(await screen.findByTestId('unread-badge')).toHaveTextContent('2');
    expect(screen.getByRole('button', { name: 'Notifications' })).toBeInTheDocument();
    expect(alertSpy).not.toHaveBeenCalled();
  });
});
