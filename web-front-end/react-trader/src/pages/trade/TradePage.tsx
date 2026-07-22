import { useEffect, useRef, useState } from 'react';
import type { Account } from '../../models/account.model';
import type { Stock } from '../../models/symbol.model';
import type { TradeTicket as TradeTicketModel } from '../../models/trade.model';
import { getAccounts } from '../../services/account.service';
import { getStocks, createTicket } from '../../services/trade.service';
import TradeTicket from '../../components/trade-ticket/TradeTicket';
import TradeBlotter from '../../components/trade-blotter/TradeBlotter';
import PositionBlotter from '../../components/position-blotter/PositionBlotter';
import './TradePage.css';

interface CreateTicketResponse {
    success?: boolean;
    [key: string]: unknown;
}

function TradePage() {
    const [accounts, setAccounts] = useState<Account[]>([]);
    const [accountModel, setAccountModel] = useState<Account | undefined>(undefined);
    const [stocks, setStocks] = useState<Stock[]>([]);
    const [showTicket, setShowTicket] = useState(false);
    const [createTicketResponse, setCreateTicketResponse] = useState<CreateTicketResponse | undefined>(undefined);
    const alertTimeoutRef = useRef<ReturnType<typeof setTimeout>>();

    useEffect(() => {
        let cancelled = false;
        getAccounts()
            .then((result) => {
                if (cancelled) {
                    return;
                }
                setAccounts(result);
                setAccountModel(result[5] ?? result[0]);
            })
            .catch((error) => console.error(error));
        getStocks()
            .then((result) => {
                if (!cancelled) {
                    setStocks(result);
                }
            })
            .catch((error) => console.error(error));
        return () => {
            cancelled = true;
            if (alertTimeoutRef.current) {
                clearTimeout(alertTimeoutRef.current);
            }
        };
    }, []);

    const onAccountChange = (accountId: number) => {
        const account = accounts.find((item) => item.id === accountId);
        if (account) {
            setAccountModel(account);
        }
    };

    const createTradeTicket = (ticket: TradeTicketModel) => {
        createTicket(ticket)
            .then((response) => {
                setCreateTicketResponse(response as CreateTicketResponse);
                if (alertTimeoutRef.current) {
                    clearTimeout(alertTimeoutRef.current);
                }
                alertTimeoutRef.current = setTimeout(() => setCreateTicketResponse(undefined), 2000);
            })
            .catch((error) => console.error(error));
        setShowTicket(false);
    };

    return (
        <div className="trade-container p-5">
            <div className="trade-ops mb-4">
                <div className="trade-filter me-3">
                    <select
                        name="account"
                        className="form-select"
                        aria-label="Select Account"
                        value={accountModel?.id ?? ''}
                        onChange={(event) => onAccountChange(Number(event.target.value))}
                    >
                        <option value="" disabled>
                            Select Account
                        </option>
                        {accounts.map((account) => (
                            <option key={account.id} value={account.id}>
                                {account.displayName}
                            </option>
                        ))}
                    </select>
                </div>
                <div className="trade-ticket mb-4">
                    <button
                        type="button"
                        id="createTicketBtn"
                        className="btn btn-sm btn-primary mb-2"
                        onClick={() => setShowTicket(true)}
                    >
                        Create Trade Ticket
                    </button>
                    {createTicketResponse && (
                        <div
                            className={`alert alert-dismissible alert-${createTicketResponse.success ? 'success' : 'danger'}`}
                            role="alert"
                        >
                            {JSON.stringify(createTicketResponse)}
                            <button
                                type="button"
                                className="btn-close"
                                aria-label="Close"
                                onClick={() => setCreateTicketResponse(undefined)}
                            />
                        </div>
                    )}
                </div>
            </div>

            {showTicket && (
                <div className="modal d-block" tabIndex={-1} role="dialog">
                    <div className="modal-dialog" role="document">
                        <div className="modal-content">
                            <TradeTicket
                                stocks={stocks}
                                account={accountModel}
                                onCreate={createTradeTicket}
                                onCancel={() => setShowTicket(false)}
                            />
                        </div>
                    </div>
                </div>
            )}

            <div className="trade-blotter">
                <div className="trade-blotter-items me-2">
                    <TradeBlotter account={accountModel} />
                </div>
                <div className="position-blotter-items">
                    <PositionBlotter account={accountModel} />
                </div>
            </div>
        </div>
    );
}

export default TradePage;
