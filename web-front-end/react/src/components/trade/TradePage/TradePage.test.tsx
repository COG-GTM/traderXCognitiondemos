import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { TradePage } from './TradePage';
import { accountService } from '../../../services/accountService';
import { symbolService } from '../../../services/symbolService';
import { Account, Stock } from '../../../models';

jest.mock('../../../services/accountService');
jest.mock('../../../services/symbolService');

// Render the real child components as lightweight stubs so this test focuses on
// the container behaviour (the real ones are owned by other sections).
jest.mock('../../Dropdown', () => ({
  Dropdown: (props: { selectedItem?: { displayName: string }; placeholder?: string }) => (
    <div data-testid="dropdown">{props.selectedItem?.displayName ?? props.placeholder}</div>
  ),
}));
jest.mock('../TradeTicket', () => ({
  TradeTicket: (props: { account?: { displayName: string } }) => (
    <div data-testid="trade-ticket">ticket for {props.account?.displayName}</div>
  ),
}));
jest.mock('../TradeBlotter', () => ({
  TradeBlotter: () => <div data-testid="trade-blotter" />,
}));
jest.mock('../PositionBlotter', () => ({
  PositionBlotter: () => <div data-testid="position-blotter" />,
}));

const mockedAccountService = accountService as jest.Mocked<typeof accountService>;
const mockedSymbolService = symbolService as jest.Mocked<typeof symbolService>;

const accounts: Account[] = Array.from({ length: 8 }, (_, i) => ({
  id: i,
  displayName: `Account ${i}`,
}));
const stocks: Stock[] = [{ ticker: 'AAPL', companyName: 'Apple Inc.' }];

beforeEach(() => {
  jest.clearAllMocks();
  mockedAccountService.getAccounts.mockResolvedValue(accounts);
  mockedSymbolService.getStocks.mockResolvedValue(stocks);
});

test('loads accounts and defaults selection to accounts[5]', async () => {
  render(<TradePage />);

  await waitFor(() => expect(mockedAccountService.getAccounts).toHaveBeenCalled());
  expect(mockedSymbolService.getStocks).toHaveBeenCalled();
  await waitFor(() => expect(screen.getByTestId('dropdown')).toHaveTextContent('Account 5'));
  expect(screen.getByTestId('trade-blotter')).toBeInTheDocument();
  expect(screen.getByTestId('position-blotter')).toBeInTheDocument();
});

test('opens the trade ticket modal when the button is clicked', async () => {
  render(<TradePage />);
  await waitFor(() => expect(screen.getByTestId('dropdown')).toHaveTextContent('Account 5'));

  expect(screen.queryByTestId('trade-ticket')).not.toBeInTheDocument();

  await userEvent.click(screen.getByRole('button', { name: /create trade ticket/i }));

  await waitFor(() =>
    expect(screen.getByTestId('trade-ticket')).toHaveTextContent('ticket for Account 5'),
  );
});
