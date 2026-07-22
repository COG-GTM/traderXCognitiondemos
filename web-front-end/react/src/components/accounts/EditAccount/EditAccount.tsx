import React from 'react';
import { Account } from '../../../models';

// SECTION 8 — Add / edit account form.
export interface EditAccountProps {
  account?: Account;
  onUpdate: (account: Account) => void;
}

// Placeholder: replace with the display-name input + Add/Update + Reset buttons
// (label toggles based on whether `account` is set) that calls
// accountService.addAccount, shows a dismissible success/error alert, and emits
// `onUpdate` on success.
export const EditAccount = (_props: EditAccountProps) => {
  return <div data-testid="edit-account-placeholder" />;
};
