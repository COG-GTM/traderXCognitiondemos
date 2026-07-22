import React from 'react';
import { Account, Stock, TradeTicket as TradeTicketModel } from '../../../models';

// SECTION 4 — Trade ticket form (rendered inside the Trade page modal).
export interface TradeTicketProps {
  stocks: Stock[];
  account?: Account;
  onCreate: (ticket: TradeTicketModel) => void;
  onCancel: () => void;
}

// Placeholder: replace with the ticket form — read-only account, security
// typeahead over `stocks` (companyName), Buy/Sell toggle, quantity input, and
// Create/Close buttons. Emit `onCreate` only when a security and quantity are set.
export const TradeTicket = (_props: TradeTicketProps) => {
  return <div data-testid="trade-ticket-placeholder" />;
};
