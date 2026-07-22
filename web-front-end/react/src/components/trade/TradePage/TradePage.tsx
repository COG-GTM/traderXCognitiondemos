import React, { useEffect, useRef, useState } from 'react';
import { Modal } from 'react-bootstrap';
import { Account, Stock, TradeTicket as TradeTicketModel } from '../../../models';
import { accountService } from '../../../services/accountService';
import { symbolService } from '../../../services/symbolService';
import { Dropdown } from '../../Dropdown';
import { TradeTicket } from '../TradeTicket';
import { TradeBlotter } from '../TradeBlotter';
import { PositionBlotter } from '../PositionBlotter';
import './TradePage.css';

const DISMISS_TIMEOUT_MS = 2000;

const isSuccessResponse = (response: unknown): boolean =>
  typeof response === 'object' &&
  response !== null &&
  (response as { success?: boolean }).success === true;

export const TradePage = () => {
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [selected, setSelected] = useState<Account | undefined>(undefined);
  const [stocks, setStocks] = useState<Stock[]>([]);
  const [showModal, setShowModal] = useState(false);
  const [createTicketResponse, setCreateTicketResponse] = useState<unknown>(undefined);
  const dismissTimer = useRef<ReturnType<typeof setTimeout> | undefined>(undefined);

  useEffect(() => {
    accountService.getAccounts().then((loaded) => {
      setAccounts(loaded);
      if (loaded.length > 5) {
        setSelected(loaded[5]);
      }
    });
    symbolService.getStocks().then((loaded) => setStocks(loaded));
  }, []);

  useEffect(() => {
    if (createTicketResponse === undefined) {
      return;
    }
    dismissTimer.current = setTimeout(() => setCreateTicketResponse(undefined), DISMISS_TIMEOUT_MS);
    return () => {
      if (dismissTimer.current) {
        clearTimeout(dismissTimer.current);
      }
    };
  }, [createTicketResponse]);

  const onAccountChange = (account: Account) => {
    if (account) {
      setSelected(account);
    }
  };

  const openTicket = () => setShowModal(true);
  const closeTicket = () => setShowModal(false);

  const createTradeTicket = (ticket: TradeTicketModel) => {
    symbolService.createTicket(ticket).then((response) => {
      setCreateTicketResponse(response);
    });
    closeTicket();
  };

  const onCloseAlert = () => setCreateTicketResponse(undefined);

  return (
    <div className="trade-container p-5">
      <div className="trade-ops mb-4">
        <div className="trade-filter me-3">
          <Dropdown<Account>
            items={accounts}
            itemKey="displayName"
            selectedItem={selected}
            placeholder="Select Account"
            onSelect={onAccountChange}
          />
        </div>
        <div className="trade-ticket mb-4">
          <button
            type="button"
            id="createTicketBtn"
            className="btn btn-sm btn-primary mb-2"
            onClick={openTicket}
          >
            Create Trade Ticket
          </button>

          <Modal show={showModal} onHide={closeTicket}>
            <TradeTicket
              stocks={stocks}
              account={selected}
              onCreate={createTradeTicket}
              onCancel={closeTicket}
            />
          </Modal>

          {createTicketResponse !== undefined && (
            <div
              className={`alert ${
                isSuccessResponse(createTicketResponse) ? 'alert-success' : 'alert-danger'
              } alert-dismissible fade show`}
              role="alert"
            >
              {JSON.stringify(createTicketResponse)}
              <button
                type="button"
                className="btn-close"
                aria-label="Close"
                onClick={onCloseAlert}
              />
            </div>
          )}
        </div>
      </div>

      <div className="trade-blotter">
        <div className="trade-blotter-items me-2">
          <TradeBlotter account={selected} />
        </div>
        <div className="position-blotter-items">
          <PositionBlotter account={selected} />
        </div>
      </div>
    </div>
  );
};
