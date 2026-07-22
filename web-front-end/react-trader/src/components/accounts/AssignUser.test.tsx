import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { vi } from 'vitest';
import { AssignUser } from './AssignUser';
import type { Account } from '../../models/account.model';
import type { User } from '../../models/user.model';

vi.mock('../../services/account.service', () => ({
  addAccountUser: vi.fn()
}));

vi.mock('../../services/user.service', () => ({
  getUsers: vi.fn()
}));

import * as accountService from '../../services/account.service';
import * as userService from '../../services/user.service';

const mockUsers = [
  { logonId: 'jmac', fullName: 'Jhon mac' },
  { logonId: 'tsan', fullName: 'Tom san' },
  { logonId: 'msan', fullName: 'Merry san' }
] as User[];

const accounts: Account[] = [
  { id: 1, displayName: 'Account 1' },
  { id: 2, displayName: 'Account 2' }
];

describe('Assign user to account tests', () => {
  beforeEach(() => {
    vi.mocked(accountService.addAccountUser).mockImplementation(async (au) => au);
    vi.mocked(userService.getUsers).mockImplementation(async (searchText: string) =>
      mockUsers.filter((u) => u.fullName.indexOf(searchText) !== -1)
    );
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  it('should allow to search user', async () => {
    render(<AssignUser accounts={accounts} onUpdate={vi.fn()} />);

    await userEvent.type(screen.getByPlaceholderText('Add User to Account'), 'san');

    await waitFor(() => {
      expect(userService.getUsers).toHaveBeenCalledWith('san');
    });
    await waitFor(() => {
      expect(screen.getByText('Tom san')).toBeInTheDocument();
      expect(screen.getByText('Merry san')).toBeInTheDocument();
    });
  });

  it('should assign user to account', async () => {
    const onUpdate = vi.fn();
    render(<AssignUser accounts={accounts} account={accounts[0]} onUpdate={onUpdate} />);

    await userEvent.type(screen.getByPlaceholderText('Add User to Account'), 'san');
    await waitFor(() => {
      expect(screen.getByText('Tom san')).toBeInTheDocument();
    });
    await userEvent.click(screen.getByText('Tom san'));
    await userEvent.click(screen.getByRole('button', { name: 'Add User' }));

    await waitFor(() => {
      expect(accountService.addAccountUser).toHaveBeenCalledWith({
        username: 'tsan',
        accountId: 1
      });
    });
    expect(onUpdate).toHaveBeenCalled();
    expect(screen.getByText('User added successfully!')).toBeInTheDocument();
    // search field is reset after add
    expect((screen.getByPlaceholderText('Add User to Account') as HTMLInputElement).value).toBe('');
  });

  it('should not call account service if user or account is undefined', async () => {
    render(<AssignUser accounts={accounts} onUpdate={vi.fn()} />);

    await userEvent.click(screen.getByRole('button', { name: 'Add User' }));
    expect(accountService.addAccountUser).not.toHaveBeenCalled();

    // select account only, still no user chosen
    await userEvent.selectOptions(screen.getByLabelText('Select an account'), '1');
    await userEvent.click(screen.getByRole('button', { name: 'Add User' }));
    expect(accountService.addAccountUser).not.toHaveBeenCalled();

    // choose a user as well -> now it should call
    await userEvent.type(screen.getByPlaceholderText('Add User to Account'), 'san');
    await waitFor(() => {
      expect(screen.getByText('Tom san')).toBeInTheDocument();
    });
    await userEvent.click(screen.getByText('Tom san'));
    await userEvent.click(screen.getByRole('button', { name: 'Add User' }));
    await waitFor(() => {
      expect(accountService.addAccountUser).toHaveBeenCalled();
    });
  });

  it('should close alert after 2 sec', async () => {
    const onUpdate = vi.fn();
    render(<AssignUser accounts={accounts} account={accounts[0]} onUpdate={onUpdate} />);

    await userEvent.type(screen.getByPlaceholderText('Add User to Account'), 'san');
    await waitFor(() => {
      expect(screen.getByText('Tom san')).toBeInTheDocument();
    });
    await userEvent.click(screen.getByText('Tom san'));
    await userEvent.click(screen.getByRole('button', { name: 'Add User' }));

    await waitFor(() => {
      expect(screen.getByText('User added successfully!')).toBeInTheDocument();
    });
    await waitFor(
      () => {
        expect(screen.queryByText('User added successfully!')).not.toBeInTheDocument();
      },
      { timeout: 3000 }
    );
  });
});
