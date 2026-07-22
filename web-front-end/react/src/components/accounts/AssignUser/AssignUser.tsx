import React from 'react';
import { Account } from '../../../models';

// SECTION 9 — Assign a user to an account.
export interface AssignUserProps {
  account?: Account;
  accounts: Account[];
  onUpdate: (account: Account) => void;
}

// Placeholder: replace with an async people typeahead (userService.getUsers,
// min 3 chars, matches on fullName), an account <Dropdown>, and an Add User
// button that calls accountService.addAccountUser and emits `onUpdate`.
export const AssignUser = (_props: AssignUserProps) => {
  return <div data-testid="assign-user-placeholder" />;
};
