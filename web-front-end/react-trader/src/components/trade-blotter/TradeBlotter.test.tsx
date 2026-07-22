import { render, screen, waitFor } from '@testing-library/react';
import { vi, type Mock } from 'vitest';
import TradeBlotter, { getRowId, columnDefs } from './TradeBlotter';
import { accounts, trades } from '../../test-utils/mocks.service';
import { getTrades } from '../../services/position.service';
import { subscribe } from '../../services/trade-feed.service';
import type { Trade } from '../../models/trade.model';
import type { GetRowIdParams } from 'ag-grid-community';

vi.mock('../../services/position.service', () => ({
    getTrades: vi.fn()
}));

vi.mock('../../services/trade-feed.service', () => ({
    subscribe: vi.fn()
}));

describe('TradeBlotter', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        (getTrades as Mock).mockResolvedValue(trades);
        (subscribe as Mock).mockReturnValue(() => {});
    });

    it('should create', () => {
        render(<TradeBlotter />);
        expect(screen.getByText('Trades')).toBeInTheDocument();
    });

    it('should show given trades columns in the grid with no rows when no account is set', async () => {
        const { container } = render(<TradeBlotter />);
        await waitFor(() => {
            expect(container.querySelectorAll('.ag-header-cell').length).toEqual(4);
        });
        expect(container.querySelectorAll('.ag-center-cols-container .ag-row').length).toEqual(0);
        expect(getTrades).not.toHaveBeenCalled();
    });

    it('should show trades rows for the given account', async () => {
        const { container } = render(<TradeBlotter account={accounts[0]} />);
        await waitFor(() => {
            expect(container.querySelectorAll('.ag-center-cols-container .ag-row').length).toEqual(2);
        });
        expect(screen.getByText(trades[0].security)).toBeInTheDocument();
    });

    it('should call getTrades and subscribe to trade feed service for given account', async () => {
        render(<TradeBlotter account={accounts[0]} />);
        await waitFor(() => expect(getTrades).toHaveBeenCalledWith(accounts[0].id));
        expect(subscribe).toHaveBeenCalledWith(
            `/accounts/${accounts[0].id}/trades`,
            expect.any(Function)
        );
    });

    it('getRowId should return id from trade data', () => {
        expect(getRowId({ data: trades[0] } as GetRowIdParams<Trade>)).toEqual(trades[0].id);
    });

    it('should define the expected columns', () => {
        expect(columnDefs.map((col) => col.headerName)).toEqual([
            'SECURITY',
            'QUANTITY',
            'SIDE',
            'STATE'
        ]);
    });
});
