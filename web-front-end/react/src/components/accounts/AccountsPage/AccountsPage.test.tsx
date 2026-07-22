import React from 'react';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { AccountsPage } from './AccountsPage';
import { accountService } from '../../../services/accountService';
import { Account, AccountUser } from '../../../models';

jest.mock('../../../services/accountService', () => ({
  accountService: {
    getAccounts: jest.fn(),
    getAccountUsers: jest.fn(),
  },
}));

// EditAccount / AssignUser are separate sections; stub them so this test only
// exercises the container. `onUpdate` is surfaced as a button we can click.
jest.mock('../EditAccount', () => ({
  EditAccount: ({ account, onUpdate }: any) => (
    <div data-testid="edit-account">
      <span data-testid="edit-account-target">{account ? account.displayName : 'none'}</span>
      <button onClick={() => onUpdate({ id: 2, displayName: 'Beta' })}>emit-edit-update</button>
    </div>
  ),
}));

jest.mock('../AssignUser', () => ({
  AssignUser: ({ account }: any) => (
    <div data-testid="assign-user">{account ? account.displayName : 'none'}</div>
  ),
}));

// ButtonCellRenderer is Section 10 (placeholder on base); stub it with a real
// button so the container's `cellRendererParams.clicked` wiring is exercised.
jest.mock('../../shared', () => ({
  ButtonCellRenderer: (props: any) => (
    <button onClick={() => props.clicked(props.data)}>Update</button>
  ),
}));

const mockedService = accountService as jest.Mocked<typeof accountService>;

const accounts: Account[] = [
  { id: 1, displayName: 'Alpha' },
  { id: 2, displayName: 'Beta' },
];

const accountUsers: AccountUser[] = [
  { accountId: 1, username: 'alice' },
  { accountId: 2, username: 'bob' },
];

beforeEach(() => {
  mockedService.getAccounts.mockResolvedValue(accounts);
  mockedService.getAccountUsers.mockResolvedValue(accountUsers);
});

afterEach(() => {
  jest.clearAllMocks();
});

it('hydrates the account list from the service on mount', async () => {
  render(<AccountsPage />);

  expect(mockedService.getAccounts).toHaveBeenCalled();
  expect(mockedService.getAccountUsers).toHaveBeenCalled();

  const grid = document.getElementById('accountgrid') as HTMLElement;
  await waitFor(() => {
    expect(within(grid).getByText('Alpha')).toBeInTheDocument();
    expect(within(grid).getByText('Beta')).toBeInTheDocument();
  });
});

it('sets accountToEdit when a row Update button is clicked', async () => {
  render(<AccountsPage />);

  const grid = document.getElementById('accountgrid') as HTMLElement;
  await waitFor(() => expect(within(grid).getByText('Alpha')).toBeInTheDocument());

  expect(screen.getByTestId('edit-account-target')).toHaveTextContent('none');

  const updateButtons = within(grid).getAllByRole('button', { name: 'Update' });
  await userEvent.click(updateButtons[0]);

  expect(screen.getByTestId('edit-account-target')).toHaveTextContent('Alpha');
});

it('re-fetches lists and updates selection on onUpdate', async () => {
  render(<AccountsPage />);

  const grid = document.getElementById('accountgrid') as HTMLElement;
  await waitFor(() => expect(within(grid).getByText('Alpha')).toBeInTheDocument());

  expect(mockedService.getAccounts).toHaveBeenCalledTimes(1);
  expect(screen.getByTestId('assign-user')).toHaveTextContent('none');

  await userEvent.click(screen.getByRole('button', { name: 'emit-edit-update' }));

  await waitFor(() => expect(mockedService.getAccounts).toHaveBeenCalledTimes(2));
  expect(screen.getByTestId('assign-user')).toHaveTextContent('Beta');
});
