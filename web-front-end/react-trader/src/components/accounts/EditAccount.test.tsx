import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { vi } from 'vitest';
import { EditAccount } from './EditAccount';
import type { Account } from '../../models/account.model';

vi.mock('../../services/account.service', () => ({
  addAccount: vi.fn()
}));

import * as accountService from '../../services/account.service';

const testAccount: Account = { id: 42, displayName: 'Test Account' };

describe('Account add/update tests', () => {
  beforeEach(() => {
    vi.mocked(accountService.addAccount).mockImplementation(async (account) => account);
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  it('should add new account and show success message', async () => {
    const onUpdate = vi.fn();
    render(<EditAccount onUpdate={onUpdate} />);

    await userEvent.type(screen.getByPlaceholderText('Account name'), testAccount.displayName);
    await userEvent.click(screen.getByRole('button', { name: 'Add Account' }));

    await waitFor(() => {
      expect(screen.getByText('Account added successfully!')).toBeInTheDocument();
    });
    expect(onUpdate).toHaveBeenCalled();
  });

  it('should update account and show success message', async () => {
    const onUpdate = vi.fn();
    render(<EditAccount onUpdate={onUpdate} account={testAccount} />);

    await userEvent.click(screen.getByRole('button', { name: 'Update Account' }));

    await waitFor(() => {
      expect(screen.getByText('Account updated successfully!')).toBeInTheDocument();
    });
    expect(onUpdate).toHaveBeenCalled();
  });

  it('should return if displayName is undefined or empty', async () => {
    const onUpdate = vi.fn();
    render(<EditAccount onUpdate={onUpdate} />);

    await userEvent.click(screen.getByRole('button', { name: 'Add Account' }));

    expect(accountService.addAccount).not.toHaveBeenCalled();
    expect(onUpdate).not.toHaveBeenCalled();
  });

  it('should reset the form', async () => {
    render(<EditAccount onUpdate={vi.fn()} account={testAccount} />);

    const input = screen.getByPlaceholderText('Account name') as HTMLInputElement;
    expect(input.value).toBe(testAccount.displayName);
    expect(screen.getByRole('button', { name: 'Update Account' })).toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: 'Reset' }));

    expect(input.value).toBe('');
    expect(screen.getByRole('button', { name: 'Add Account' })).toBeInTheDocument();
  });

  it('should show add button when no existing account available', () => {
    render(<EditAccount onUpdate={vi.fn()} />);
    expect(screen.getByRole('button', { name: 'Add Account' })).toBeInTheDocument();
  });

  it('should show update button when an existing account is provided', () => {
    render(<EditAccount onUpdate={vi.fn()} account={testAccount} />);
    expect(screen.getByRole('button', { name: 'Update Account' })).toBeInTheDocument();
  });
});
