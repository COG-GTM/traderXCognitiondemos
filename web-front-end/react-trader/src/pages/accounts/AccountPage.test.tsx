import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { vi } from 'vitest';
import AccountPage from './AccountPage';
import type { Account } from '../../models/account.model';
import type { AccountUser } from '../../models/user.model';

const accounts: Account[] = Array.from({ length: 5 }, (_, i) => ({
  id: i + 1,
  displayName: `Account ${i + 1}`
}));

const accountUsers: AccountUser[] = accounts.map((ac) => ({
  accountId: ac.id,
  username: `user${ac.id}`
}));

vi.mock('../../services/account.service', () => ({
  getAccounts: vi.fn(),
  getAccountUsers: vi.fn(),
  addAccount: vi.fn(),
  addAccountUser: vi.fn()
}));

import * as accountService from '../../services/account.service';

describe('Account tests', () => {
  beforeEach(() => {
    vi.mocked(accountService.getAccounts).mockResolvedValue(accounts);
    vi.mocked(accountService.getAccountUsers).mockResolvedValue(accountUsers);
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  it('should render the grid with the account rows', async () => {
    const { container } = render(<AccountPage />);
    await waitFor(() => {
      expect(container.querySelector('#accountgrid .ag-root-wrapper')).toBeTruthy();
    });
    await waitFor(() => {
      expect(
        container.querySelectorAll('#accountgrid .ag-center-cols-container .ag-row').length
      ).toBe(5);
    });
  });

  it('should fetch accounts list on init', async () => {
    expect(accountService.getAccounts).not.toHaveBeenCalled();
    render(<AccountPage />);
    await waitFor(() => {
      expect(accountService.getAccounts).toHaveBeenCalled();
    });
  });

  it('should select the account from account list', async () => {
    const { container } = render(<AccountPage />);
    await waitFor(() => {
      expect(
        container.querySelectorAll('#accountgrid .ag-center-cols-container .ag-row').length
      ).toBe(5);
    });
    expect(screen.getByText(/^Users List\s*$/)).toBeInTheDocument();

    const firstCell = container.querySelector(
      '#accountgrid .ag-center-cols-container .ag-row .ag-cell'
    ) as HTMLElement;
    await userEvent.click(firstCell);

    await waitFor(() => {
      expect(screen.getByText(/Users List \(Account 1\)/)).toBeInTheDocument();
    });
  });

  it('should fetch account users for selected account', async () => {
    const { container } = render(<AccountPage />);
    await waitFor(() => {
      expect(
        container.querySelectorAll('#accountgrid .ag-center-cols-container .ag-row').length
      ).toBe(5);
    });
    await waitFor(() => {
      expect(accountService.getAccountUsers).toHaveBeenCalled();
    });
    expect(container.querySelectorAll('#usergrid .ag-center-cols-container .ag-row').length).toBe(0);

    const firstCell = container.querySelector(
      '#accountgrid .ag-center-cols-container .ag-row .ag-cell'
    ) as HTMLElement;
    await userEvent.click(firstCell);

    await waitFor(() => {
      expect(container.querySelectorAll('#usergrid .ag-center-cols-container .ag-row').length).toBe(1);
    });
  });
});
