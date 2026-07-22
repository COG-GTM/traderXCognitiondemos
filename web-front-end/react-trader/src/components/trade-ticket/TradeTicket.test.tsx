import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { TradeTicket } from './TradeTicket';
import type { Account } from '../../models/account.model';
import type { Stock } from '../../models/symbol.model';

const dummyAccounts: Account[] = [
    { id: 1, displayName: 'Test Account 1' },
    { id: 2, displayName: 'Test Account 2' }
];

const dummyStocks: Stock[] = [
    { ticker: 'AAPL', companyName: 'Apple Inc' },
    { ticker: 'MSFT', companyName: 'Microsoft Corp' },
    { ticker: 'GOOG', companyName: 'Alphabet Inc' },
    { ticker: 'AMZN', companyName: 'Amazon.com Inc' },
    { ticker: 'TSLA', companyName: 'Tesla Inc' }
];

function renderTicket(overrides: Partial<Parameters<typeof TradeTicket>[0]> = {}) {
    const onCreate = vi.fn();
    const onCancel = vi.fn();
    render(
        <TradeTicket
            stocks={dummyStocks}
            account={dummyAccounts[0]}
            onCreate={onCreate}
            onCancel={onCancel}
            {...overrides}
        />
    );
    return { onCreate, onCancel };
}

describe('TradeTicket', () => {
    it('should create', () => {
        renderTicket();
        expect(screen.getByRole('button', { name: 'Create' })).toBeInTheDocument();
    });

    it('should show ticket with initial values', () => {
        renderTicket();
        expect(screen.getByLabelText('Quantity')).toHaveValue(0);
        expect(screen.getByRole('radio', { name: 'Buy' })).toBeChecked();
        expect(screen.getByLabelText('Account')).toHaveValue(dummyAccounts[0].displayName);
    });

    it('should update ticket object with given values on create click and emit create event', async () => {
        const user = userEvent.setup();
        const { onCreate } = renderTicket();

        const quantityField = screen.getByLabelText('Quantity');
        await user.clear(quantityField);
        await user.type(quantityField, '10');

        await user.click(screen.getByRole('radio', { name: 'Sell' }));

        const stockInput = screen.getByLabelText('Security');
        await user.type(stockInput, 'Apple');
        await user.click(screen.getByRole('option', { name: dummyStocks[0].companyName }));

        await user.click(screen.getByRole('button', { name: 'Create' }));

        expect(onCreate).toHaveBeenCalledWith({
            quantity: 10,
            accountId: dummyAccounts[0].id,
            side: 'Sell',
            security: dummyStocks[0].ticker
        });
    });

    it('should not emit create when security or quantity is missing', async () => {
        const user = userEvent.setup();
        const { onCreate } = renderTicket();
        await user.click(screen.getByRole('button', { name: 'Create' }));
        expect(onCreate).not.toHaveBeenCalled();
    });

    it('should emit cancel on cancel click', async () => {
        const user = userEvent.setup();
        const { onCancel } = renderTicket();
        await user.click(screen.getByRole('button', { name: 'Close' }));
        expect(onCancel).toHaveBeenCalled();
    });

    it('should filter stock options based on given query', async () => {
        const user = userEvent.setup();
        renderTicket();

        const stockInput = screen.getByLabelText('Security');
        await user.click(stockInput);
        expect(screen.getAllByRole('option')).toHaveLength(5);

        await user.type(stockInput, 'Apple');
        expect(screen.getAllByRole('option')).toHaveLength(1);
        expect(screen.getByRole('option', { name: 'Apple Inc' })).toBeInTheDocument();
    });
});
