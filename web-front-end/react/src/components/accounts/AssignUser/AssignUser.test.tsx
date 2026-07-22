import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { AssignUser } from './AssignUser';
import { userService } from '../../../services/userService';
import { accountService } from '../../../services/accountService';
import { Account, User } from '../../../models';

jest.mock('../../../services/userService');
jest.mock('../../../services/accountService');

const mockedUserService = userService as jest.Mocked<typeof userService>;
const mockedAccountService = accountService as jest.Mocked<typeof accountService>;

const accounts: Account[] = [
  { id: 1, displayName: 'Account One' },
  { id: 2, displayName: 'Account Two' },
];

const user: User = {
  logonId: 'jdoe',
  fullName: 'John Doe',
  email: 'jdoe@example.com',
  employeeId: 'E1',
  department: 'Trading',
  photoUrl: '',
};

beforeEach(() => {
  jest.clearAllMocks();
});

test('searches, selects a user, adds them to an account and emits onUpdate', async () => {
  mockedUserService.getUsers.mockResolvedValue([user]);
  mockedAccountService.addAccountUser.mockResolvedValue({ username: 'jdoe', accountId: 1 });
  const onUpdate = jest.fn();

  render(<AssignUser account={accounts[0]} accounts={accounts} onUpdate={onUpdate} />);

  await userEvent.type(screen.getByPlaceholderText('Add User to Account'), 'Joh');

  await waitFor(() => expect(mockedUserService.getUsers).toHaveBeenCalledWith('Joh'));

  const option = await screen.findByRole('option', { name: 'John Doe' });
  await userEvent.click(option);

  await userEvent.click(screen.getByRole('button', { name: 'Add User' }));

  await waitFor(() =>
    expect(mockedAccountService.addAccountUser).toHaveBeenCalledWith({
      username: 'jdoe',
      accountId: 1,
    })
  );
  expect(onUpdate).toHaveBeenCalledWith(accounts[0]);
  expect(await screen.findByText('User added successfully!')).toBeInTheDocument();
});

test('does not query the service for queries of 2 characters or fewer', async () => {
  mockedUserService.getUsers.mockResolvedValue([]);
  render(<AssignUser accounts={accounts} onUpdate={jest.fn()} />);

  await userEvent.type(screen.getByPlaceholderText('Add User to Account'), 'Jo');

  await new Promise((resolve) => setTimeout(resolve, 400));
  expect(mockedUserService.getUsers).not.toHaveBeenCalled();
});

test('does not call addAccountUser until both a user and account are chosen', async () => {
  mockedUserService.getUsers.mockResolvedValue([user]);
  render(<AssignUser accounts={accounts} onUpdate={jest.fn()} />);

  await userEvent.type(screen.getByPlaceholderText('Add User to Account'), 'Joh');
  const option = await screen.findByRole('option', { name: 'John Doe' });
  await userEvent.click(option);

  await userEvent.click(screen.getByRole('button', { name: 'Add User' }));

  expect(mockedAccountService.addAccountUser).not.toHaveBeenCalled();
});
