import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { vi, type Mock } from 'vitest';
import TradePage from './TradePage';
import { accounts, stocks } from '../../test-utils/mocks.service';
import { getAccounts } from '../../services/account.service';
import { getStocks, createTicket } from '../../services/trade.service';
import { Side, type TradeTicket } from '../../models/trade.model';

vi.mock('../../services/account.service', () => ({
    getAccounts: vi.fn()
}));

vi.mock('../../services/trade.service', () => ({
    getStocks: vi.fn(),
    createTicket: vi.fn()
}));

vi.mock('../../components/trade-blotter/TradeBlotter', () => ({
    default: () => <div data-testid="trade-blotter" />
}));

vi.mock('../../components/position-blotter/PositionBlotter', () => ({
    default: () => <div data-testid="position-blotter" />
}));

vi.mock('../../components/trade-ticket/TradeTicket', () => ({
    default: ({ onCreate }: { onCreate: (ticket: TradeTicket) => void }) => (
        <button
            type="button"
            data-testid="mock-trade-ticket"
            onClick={() =>
                onCreate({ accountId: 1, quantity: 10, security: 'abc', side: Side.Buy })
            }
        >
            Submit Ticket
        </button>
    )
}));

describe('TradePage', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        (getAccounts as Mock).mockResolvedValue(accounts);
        (getStocks as Mock).mockResolvedValue(stocks);
        (createTicket as Mock).mockResolvedValue({ success: true });
    });

    it('should create', async () => {
        render(<TradePage />);
        expect(screen.getByRole('button', { name: 'Create Trade Ticket' })).toBeInTheDocument();
        await waitFor(() => expect(getAccounts).toHaveBeenCalled());
    });

    it('should get accounts and stocks on init', async () => {
        render(<TradePage />);
        await waitFor(() => expect(getAccounts).toHaveBeenCalled());
        expect(getStocks).toHaveBeenCalled();
        const options = await screen.findAllByRole('option');
        // "Select Account" placeholder + 5 accounts
        expect(options.length).toEqual(accounts.length + 1);
    });

    it('should call symbol service to create ticket and close the ticket', async () => {
        const user = userEvent.setup();
        render(<TradePage />);
        await waitFor(() => expect(getAccounts).toHaveBeenCalled());
        await user.click(screen.getByRole('button', { name: 'Create Trade Ticket' }));
        await user.click(screen.getByTestId('mock-trade-ticket'));
        expect(createTicket).toHaveBeenCalledWith({
            accountId: 1,
            quantity: 10,
            security: 'abc',
            side: Side.Buy
        });
        await waitFor(() =>
            expect(screen.queryByTestId('mock-trade-ticket')).not.toBeInTheDocument()
        );
        expect(await screen.findByRole('alert')).toHaveTextContent('"success":true');
    });

    it('should open the ticket on click', async () => {
        const user = userEvent.setup();
        render(<TradePage />);
        await waitFor(() => expect(getAccounts).toHaveBeenCalled());
        expect(screen.queryByTestId('mock-trade-ticket')).not.toBeInTheDocument();
        await user.click(screen.getByRole('button', { name: 'Create Trade Ticket' }));
        expect(screen.getByTestId('mock-trade-ticket')).toBeInTheDocument();
    });
});
