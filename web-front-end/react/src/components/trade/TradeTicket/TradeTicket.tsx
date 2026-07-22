import React, { useMemo, useState } from 'react';
import { Account, Stock, TradeTicket as TradeTicketModel } from '../../../models';
import './TradeTicket.css';

// SECTION 4 — Trade ticket form (rendered inside the Trade page modal).
export interface TradeTicketProps {
  stocks: Stock[];
  account?: Account;
  onCreate: (ticket: TradeTicketModel) => void;
  onCancel: () => void;
}

export const TradeTicket = ({ stocks, account, onCreate, onCancel }: TradeTicketProps) => {
  const [ticket, setTicket] = useState<TradeTicketModel>({
    quantity: 0,
    accountId: account?.id ?? 0,
    side: 'Buy',
    security: '',
  });
  const [selectedCompany, setSelectedCompany] = useState('');

  const filteredStocks = useMemo(() => {
    const query = selectedCompany.trim().toLowerCase();
    if (!query) return stocks;
    return stocks.filter((s) => s.companyName.toLowerCase().includes(query));
  }, [stocks, selectedCompany]);

  const onCompanyChange = (value: string) => {
    setSelectedCompany(value);
    const match = stocks.find((s) => s.companyName === value);
    setTicket((t) => ({ ...t, security: match ? match.ticker : '' }));
  };

  const onCreateClick = () => {
    if (!ticket.security || !ticket.quantity) {
      return;
    }
    onCreate(ticket);
  };

  return (
    <div className="p-4">
      <div className="mb-3 row">
        <label className="col-sm-2 col-form-label me-3">Account</label>
        <div className="col-sm-8">
          <input
            readOnly
            disabled
            className="form-control"
            id="accountLabel"
            value={account?.displayName ?? ''}
          />
        </div>
      </div>
      <div className="mb-3 row">
        <label className="col-sm-2 col-form-label me-3">Security</label>
        <div className="col-sm-8">
          <input
            id="stock-input"
            className="form-control"
            placeholder="Search security"
            list="stock-options"
            value={selectedCompany}
            onChange={(e) => onCompanyChange(e.target.value)}
          />
          <datalist id="stock-options">
            {filteredStocks.map((stock) => (
              <option key={stock.ticker} value={stock.companyName} />
            ))}
          </datalist>
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
              checked={ticket.side === 'Buy'}
              onChange={() => setTicket((t) => ({ ...t, side: 'Buy' }))}
            />
            <label
              className={ticket.side === 'Buy' ? 'btn btn-info btn-sm' : 'btn btn-secondary btn-sm'}
              htmlFor="buyButton"
            >
              Buy
            </label>

            <input
              type="radio"
              className="btn-check"
              name="sideButton"
              value="Sell"
              id="sellButton"
              checked={ticket.side === 'Sell'}
              onChange={() => setTicket((t) => ({ ...t, side: 'Sell' }))}
            />
            <label
              className={ticket.side === 'Sell' ? 'btn btn-warning btn-sm' : 'btn btn-secondary btn-sm'}
              htmlFor="sellButton"
            >
              Sell
            </label>
          </div>
        </div>
      </div>
      <div className="mb-3 row">
        <label className="col-sm-2 col-form-label me-3">Quantity</label>
        <div className="col-sm-8">
          <input
            className="form-control d-inline-block"
            id="quantityField"
            type="number"
            value={ticket.quantity}
            onChange={(e) => setTicket((t) => ({ ...t, quantity: Number(e.target.value) }))}
          />
        </div>
      </div>
      <div className="mb-3 row">
        <button className="btn btn-sm btn-primary col-sm-3 me-2" id="createButton" onClick={onCreateClick}>
          Create
        </button>
        <button className="btn btn-sm btn-secondary col-sm-3" id="cancelButton" onClick={onCancel}>
          Close
        </button>
      </div>
    </div>
  );
};
