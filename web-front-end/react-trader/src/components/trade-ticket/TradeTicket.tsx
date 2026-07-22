import { useMemo, useState } from 'react';
import type { Account } from '../../models/account.model';
import type { Stock } from '../../models/symbol.model';
import type { TradeTicket as TradeTicketModel } from '../../models/trade.model';

export interface TradeTicketProps {
    stocks: Stock[];
    account?: Account;
    onCreate: (ticket: TradeTicketModel) => void;
    onCancel: () => void;
}

export function TradeTicket({ stocks, account, onCreate, onCancel }: TradeTicketProps) {
    const [selectedCompany, setSelectedCompany] = useState('');
    const [security, setSecurity] = useState('');
    const [side, setSide] = useState<'Buy' | 'Sell'>('Buy');
    const [quantity, setQuantity] = useState(0);
    const [showOptions, setShowOptions] = useState(false);

    const filteredStocks = useMemo(() => {
        const query = selectedCompany.trim().toLowerCase();
        if (!query) return stocks;
        return stocks.filter((stock) => stock.companyName.toLowerCase().includes(query));
    }, [stocks, selectedCompany]);

    const onSelect = (stock: Stock) => {
        setSelectedCompany(stock.companyName);
        setSecurity(stock.ticker);
        setShowOptions(false);
    };

    const onBlur = () => {
        setShowOptions(false);
        if (selectedCompany) return;
        setSecurity('');
    };

    const handleCreate = () => {
        if (!security || !quantity) {
            console.warn('Either security is not selected or quanity is not set!');
            return;
        }
        onCreate({ quantity, accountId: account?.id || 0, side, security });
    };

    return (
        <div className="p-4">
            <div className="mb-3 row">
                <label className="col-sm-2 col-form-label me-3" htmlFor="accountLabel">Account</label>
                <div className="col-sm-8">
                    <input readOnly disabled className="form-control" id="accountLabel" value={account?.displayName ?? ''} />
                </div>
            </div>
            <div className="mb-3 row">
                <label className="col-sm-2 col-form-label me-3" htmlFor="stock-input">Security</label>
                <div className="col-sm-8">
                    <input
                        id="stock-input"
                        minLength={1}
                        value={selectedCompany}
                        onChange={(e) => {
                            setSelectedCompany(e.target.value);
                            setShowOptions(true);
                        }}
                        onFocus={() => setShowOptions(true)}
                        onBlur={onBlur}
                        placeholder="Search security"
                        className="form-control"
                        autoComplete="off"
                        role="combobox"
                        aria-expanded={showOptions}
                        aria-controls="stock-options"
                    />
                    {showOptions && filteredStocks.length > 0 && (
                        <ul id="stock-options" className="list-group position-absolute" role="listbox" style={{ zIndex: 1000 }}>
                            {filteredStocks.map((stock) => (
                                <li
                                    key={stock.ticker}
                                    role="option"
                                    aria-selected={stock.ticker === security}
                                    className="list-group-item list-group-item-action"
                                    onMouseDown={() => onSelect(stock)}
                                >
                                    {stock.companyName}
                                </li>
                            ))}
                        </ul>
                    )}
                </div>
            </div>
            <div className="mb-3 row">
                <label className="col-sm-2 col-form-label me-3">Side</label>
                <div className="col-sm-8">
                    <div className="btn-group">
                        <input
                            type="radio"
                            className="btn-check"
                            name="sideButton"
                            value="Buy"
                            id="buyButton"
                            checked={side === 'Buy'}
                            onChange={() => setSide('Buy')}
                        />
                        <label className={side === 'Buy' ? 'btn btn-info btn-sm' : 'btn btn-secondary btn-sm'} htmlFor="buyButton">Buy</label>
                        <input
                            type="radio"
                            className="btn-check"
                            name="sideButton"
                            value="Sell"
                            id="sellButton"
                            checked={side === 'Sell'}
                            onChange={() => setSide('Sell')}
                        />
                        <label className={side === 'Sell' ? 'btn btn-warning btn-sm' : 'btn btn-secondary btn-sm'} htmlFor="sellButton">Sell</label>
                    </div>
                </div>
            </div>
            <div className="mb-3 row">
                <label className="col-sm-2 col-form-label me-3" htmlFor="quantityField">Quantity</label>
                <div className="col-sm-8">
                    <input
                        className="form-control d-inline-block"
                        id="quantityField"
                        type="number"
                        value={quantity}
                        onChange={(e) => setQuantity(Number(e.target.value))}
                    />
                </div>
            </div>
            <div className="mb-3 row">
                <button className="btn btn-sm btn-primary col-sm-3 me-2" id="createButton" onClick={handleCreate}>Create</button>
                <button className="btn btn-sm btn-secondary col-sm-3" id="cancelButton" onClick={onCancel}>Close</button>
            </div>
        </div>
    );
}

export default TradeTicket;
