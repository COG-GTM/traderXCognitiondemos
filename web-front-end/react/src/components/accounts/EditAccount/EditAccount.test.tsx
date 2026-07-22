import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { EditAccount } from './EditAccount';
import { accountService } from '../../../services/accountService';

jest.mock('../../../services/accountService', () => ({
  accountService: {
    addAccount: jest.fn(),
  },
}));

const mockedAddAccount = accountService.addAccount as jest.Mock;

describe('EditAccount', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('adds an account, emits onUpdate, and shows a success alert', async () => {
    mockedAddAccount.mockResolvedValueOnce({ displayName: 'New Fund' });
    const onUpdate = jest.fn();
    render(<EditAccount onUpdate={onUpdate} />);

    const input = screen.getByPlaceholderText('Account name');
    fireEvent.change(input, { target: { value: 'New Fund' } });

    fireEvent.click(screen.getByRole('button', { name: 'Add Account' }));

    await waitFor(() => {
      expect(mockedAddAccount).toHaveBeenCalledWith({ displayName: 'New Fund' });
    });
    expect(onUpdate).toHaveBeenCalledWith({ displayName: 'New Fund' });
    expect(await screen.findByText('Account added successfully!')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('Account name')).toHaveValue('');
  });

  it('prefills the name and shows an Update label / updated message when editing', async () => {
    mockedAddAccount.mockResolvedValueOnce({ id: 5, displayName: 'Renamed' });
    const onUpdate = jest.fn();
    render(<EditAccount account={{ id: 5, displayName: 'Existing' }} onUpdate={onUpdate} />);

    const input = screen.getByPlaceholderText('Account name');
    expect(input).toHaveValue('Existing');

    const updateBtn = screen.getByRole('button', { name: 'Update Account' });
    fireEvent.change(input, { target: { value: 'Renamed' } });
    fireEvent.click(updateBtn);

    await waitFor(() => {
      expect(mockedAddAccount).toHaveBeenCalledWith({ id: 5, displayName: 'Renamed' });
    });
    expect(onUpdate).toHaveBeenCalledWith({ id: 5, displayName: 'Renamed' });
    expect(await screen.findByText('Account updated successfully!')).toBeInTheDocument();
  });

  it('does nothing when the name is empty', () => {
    const onUpdate = jest.fn();
    render(<EditAccount onUpdate={onUpdate} />);

    fireEvent.click(screen.getByRole('button', { name: 'Add Account' }));

    expect(mockedAddAccount).not.toHaveBeenCalled();
    expect(onUpdate).not.toHaveBeenCalled();
  });

  it('shows a danger alert on service error', async () => {
    mockedAddAccount.mockRejectedValueOnce(new Error('boom'));
    const consoleError = jest.spyOn(console, 'error').mockImplementation(() => {});
    render(<EditAccount onUpdate={jest.fn()} />);

    fireEvent.change(screen.getByPlaceholderText('Account name'), {
      target: { value: 'Bad Fund' },
    });
    fireEvent.click(screen.getByRole('button', { name: 'Add Account' }));

    expect(await screen.findByText('There is some error!')).toBeInTheDocument();
    consoleError.mockRestore();
  });

  it('reset clears the display name', () => {
    render(<EditAccount account={{ id: 1, displayName: 'Keep' }} onUpdate={jest.fn()} />);

    const input = screen.getByPlaceholderText('Account name');
    expect(input).toHaveValue('Keep');

    fireEvent.click(screen.getByRole('button', { name: 'Reset' }));
    expect(input).toHaveValue('');
    expect(screen.getByRole('button', { name: 'Add Account' })).toBeInTheDocument();
  });
});
