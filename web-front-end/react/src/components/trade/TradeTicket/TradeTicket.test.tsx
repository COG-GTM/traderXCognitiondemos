import React from 'react';
import { render, fireEvent } from '@testing-library/react';
import { TradeTicket } from './TradeTicket';
import { Account, Stock, TradeTicket as TradeTicketModel } from '../../../models';

const stocks: Stock[] = [
  { ticker: 'AAPL', companyName: 'Apple Inc.' },
  { ticker: 'MSFT', companyName: 'Microsoft Corporation' },
];

const account: Account = { id: 42, displayName: 'Test Account' };

describe('TradeTicket', () => {
  it('shows the account display name in a disabled input', () => {
    render(<TradeTicket stocks={stocks} account={account} onCreate={jest.fn()} onCancel={jest.fn()} />);
    const accountInput = document.getElementById('accountLabel') as HTMLInputElement;
    expect(accountInput).toBeDisabled();
    expect(accountInput.value).toBe('Test Account');
  });

  it('emits onCreate with the ticket when security and quantity are set', () => {
    const onCreate = jest.fn();
    render(<TradeTicket stocks={stocks} account={account} onCreate={onCreate} onCancel={jest.fn()} />);

    fireEvent.change(document.getElementById('stock-input') as HTMLInputElement, {
      target: { value: 'Apple Inc.' },
    });
    fireEvent.change(document.getElementById('quantityField') as HTMLInputElement, {
      target: { value: '10' },
    });
    fireEvent.click(document.getElementById('createButton') as HTMLButtonElement);

    expect(onCreate).toHaveBeenCalledTimes(1);
    const payload = onCreate.mock.calls[0][0] as TradeTicketModel;
    expect(payload).toEqual({ quantity: 10, accountId: 42, side: 'Buy', security: 'AAPL' });
  });

  it('is a no-op when security or quantity is missing', () => {
    const onCreate = jest.fn();
    render(<TradeTicket stocks={stocks} account={account} onCreate={onCreate} onCancel={jest.fn()} />);

    // no security, no quantity
    fireEvent.click(document.getElementById('createButton') as HTMLButtonElement);
    expect(onCreate).not.toHaveBeenCalled();

    // quantity set but no security selected
    fireEvent.change(document.getElementById('quantityField') as HTMLInputElement, {
      target: { value: '5' },
    });
    fireEvent.click(document.getElementById('createButton') as HTMLButtonElement);
    expect(onCreate).not.toHaveBeenCalled();

    // security typed but does not match any company -> no ticker resolved
    fireEvent.change(document.getElementById('stock-input') as HTMLInputElement, {
      target: { value: 'Unknown Co' },
    });
    fireEvent.click(document.getElementById('createButton') as HTMLButtonElement);
    expect(onCreate).not.toHaveBeenCalled();
  });

  it('switches side to Sell and calls onCancel', () => {
    const onCancel = jest.fn();
    render(<TradeTicket stocks={stocks} account={account} onCreate={jest.fn()} onCancel={onCancel} />);

    fireEvent.click(document.getElementById('sellButton') as HTMLInputElement);
    expect((document.getElementById('sellButton') as HTMLInputElement).checked).toBe(true);

    fireEvent.click(document.getElementById('cancelButton') as HTMLButtonElement);
    expect(onCancel).toHaveBeenCalledTimes(1);
  });
});
