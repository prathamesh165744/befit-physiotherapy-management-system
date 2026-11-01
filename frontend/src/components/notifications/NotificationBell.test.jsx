import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Routes, Route, useLocation } from 'react-router-dom';

vi.mock('../../api', () => ({
  default: { get: vi.fn(), post: vi.fn() },
  errorMessage: vi.fn(async (_err, fallback) => fallback),
}));
import axios from '../../api';
import NotificationBell from './NotificationBell';

const now = () => new Date().toISOString().slice(0, 19);
const note = (over = {}) => ({
  id: 1, type: 'TRANSFER_REQUESTED', message: 'Asha Kulkarni (REG-7) requested a transfer from Branch A to Branch B.',
  createdAt: now(), read: false, readAt: null, relatedType: 'TRANSFER', relatedId: 5, link: '/staff-dashboard/transfers', ...over,
});

function mockApi({ unread = 0, list = [], listError = false } = {}) {
  axios.get.mockImplementation((url) => {
    if (url === '/api/notifications/unread-count') return Promise.resolve({ data: { unread } });
    if (url === '/api/notifications') return listError ? Promise.reject(new Error('down')) : Promise.resolve({ data: list });
    return Promise.reject(new Error('unexpected ' + url));
  });
}

function Where() { return <div data-testid="where">{useLocation().pathname}</div>; }
const renderBell = () => render(
  <MemoryRouter initialEntries={['/start']}>
    <NotificationBell />
    <Routes><Route path="*" element={<Where />} /></Routes>
  </MemoryRouter>,
);

beforeEach(() => { vi.clearAllMocks(); });

describe('NotificationBell', () => {
  it('shows the unread count from the server on the bell', async () => {
    mockApi({ unread: 3 });
    renderBell();
    expect(await screen.findByTestId('unread-badge')).toHaveTextContent('3');
    expect(axios.get).toHaveBeenCalledWith('/api/notifications/unread-count');
  });

  it('shows no badge when everything is read, and caps big numbers', async () => {
    mockApi({ unread: 0 });
    const { unmount } = renderBell();
    await waitFor(() => expect(axios.get).toHaveBeenCalled());
    expect(screen.queryByTestId('unread-badge')).not.toBeInTheDocument();
    unmount();
    mockApi({ unread: 250 });
    renderBell();
    expect(await screen.findByTestId('unread-badge')).toHaveTextContent('99+');
  });

  it('opens the list with message, time and read state, only the user\'s own (no user id is ever sent)', async () => {
    mockApi({ unread: 1, list: [note(), note({ id: 2, read: true, message: 'Earlier one', link: null })] });
    renderBell();
    await userEvent.click(screen.getByRole('button', { name: 'Notifications' }));
    const items = await screen.findAllByTestId('notification-item');
    expect(items).toHaveLength(2);
    expect(items[0]).toHaveTextContent(/requested a transfer from Branch A to Branch B/);
    expect(items[0]).toHaveTextContent(/just now/);
    expect(items[0]).toHaveAttribute('data-read', 'false');
    expect(items[1]).toHaveAttribute('data-read', 'true');
    const listCall = axios.get.mock.calls.find((c) => c[0] === '/api/notifications');
    expect(listCall[1]).toEqual({ params: { limit: 20 } });
  });

  it('marks a notification read and opens its page when the server gave a link', async () => {
    mockApi({ unread: 1, list: [note()] });
    axios.post.mockResolvedValue({ data: { id: 1, read: true, readAt: now() } });
    renderBell();
    await userEvent.click(screen.getByRole('button', { name: 'Notifications' }));
    await userEvent.click(await screen.findByTestId('notification-item'));
    expect(axios.post).toHaveBeenCalledWith('/api/notifications/1/read');
    expect(await screen.findByTestId('where')).toHaveTextContent('/staff-dashboard/transfers');
    await waitFor(() => expect(screen.queryByTestId('unread-badge')).not.toBeInTheDocument());
  });

  it('marks it read but goes nowhere when there is no link (no permission to open it)', async () => {
    mockApi({ unread: 1, list: [note({ link: null })] });
    axios.post.mockResolvedValue({ data: { id: 1, read: true, readAt: now() } });
    renderBell();
    await userEvent.click(screen.getByRole('button', { name: 'Notifications' }));
    await userEvent.click(await screen.findByTestId('notification-item'));
    expect(axios.post).toHaveBeenCalledWith('/api/notifications/1/read');
    expect(screen.getByTestId('where')).toHaveTextContent('/start');
    await waitFor(() => expect(screen.getByTestId('notification-item')).toHaveAttribute('data-read', 'true'));
  });

  it('does not call the server again for one that is already read', async () => {
    mockApi({ list: [note({ read: true })] });
    renderBell();
    await userEvent.click(screen.getByRole('button', { name: 'Notifications' }));
    await userEvent.click(await screen.findByTestId('notification-item'));
    expect(axios.post).not.toHaveBeenCalled();
    expect(await screen.findByTestId('where')).toHaveTextContent('/staff-dashboard/transfers');
  });

  it('marks everything read', async () => {
    mockApi({ unread: 2, list: [note(), note({ id: 2 })] });
    axios.post.mockResolvedValue({ data: { updated: 2 } });
    renderBell();
    await userEvent.click(screen.getByRole('button', { name: 'Notifications' }));
    await userEvent.click(await screen.findByRole('button', { name: /mark all as read/i }));
    expect(axios.post).toHaveBeenCalledWith('/api/notifications/read-all');
    await waitFor(() => expect(screen.queryByTestId('unread-badge')).not.toBeInTheDocument());
    screen.getAllByTestId('notification-item').forEach((el) => expect(el).toHaveAttribute('data-read', 'true'));
  });

  it('shows an empty state', async () => {
    mockApi({ list: [] });
    renderBell();
    await userEvent.click(screen.getByRole('button', { name: 'Notifications' }));
    expect(await screen.findByTestId('no-notifications')).toBeInTheDocument();
  });

  it('shows an error with a retry', async () => {
    mockApi({ listError: true });
    renderBell();
    await userEvent.click(screen.getByRole('button', { name: 'Notifications' }));
    expect(await screen.findByRole('alert')).toHaveTextContent(/could not load your notifications/i);
    mockApi({ list: [note()] });
    await userEvent.click(screen.getByRole('button', { name: /try again/i }));
    expect(await screen.findByTestId('notification-item')).toBeInTheDocument();
  });

  it('closes with Escape', async () => {
    mockApi({ list: [note()] });
    renderBell();
    await userEvent.click(screen.getByRole('button', { name: 'Notifications' }));
    await screen.findByRole('dialog', { name: 'Notifications' });
    await userEvent.keyboard('{Escape}');
    expect(screen.queryByRole('dialog', { name: 'Notifications' })).not.toBeInTheDocument();
  });
});
