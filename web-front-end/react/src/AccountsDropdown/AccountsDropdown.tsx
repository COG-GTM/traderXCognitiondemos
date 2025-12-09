import { MouseEvent, ReactNode, useEffect, useState } from 'react';
import InputLabel from '@mui/material/InputLabel';
import MenuItem from '@mui/material/MenuItem';
import FormControl from '@mui/material/FormControl';
import Select from '@mui/material/Select';
import { SelectChangeEvent } from '@mui/material';
import React from 'react';
import { GetAccounts } from '../hooks';
import { AccountData, AccountsDropdownProps } from './types';

/**
 * AccountsDropdown Component
 * 
 * A React functional component that renders a dropdown selector for trading accounts.
 * This component was migrated from the Angular DropdownComponent.
 * 
 * @description
 * Renders a Material-UI Select component populated with available trading accounts.
 * Uses the GetAccounts custom hook to fetch account data from the account service.
 * When an account is selected, it triggers the handleChange callback passed via props.
 * Includes a "None" option to allow deselection.
 * 
 * @migration
 * - Angular Source: DropdownComponent in dropdown/dropdown.component.ts
 * - Angular @Input() items replaced with GetAccounts hook for data fetching
 * - Angular @Input() selectedItem replaced with currentAccount prop
 * - Angular @Output() selectedItemChange EventEmitter replaced with handleChange callback prop
 * - Angular ngOnInit comparator setup removed (MUI Select handles comparison internally)
 * - Custom dropdown template replaced with MUI Select component
 * 
 * @param {AccountsDropdownProps} props - Component props
 * @param {function} props.handleChange - Callback function triggered when selection changes
 * @param {string} props.currentAccount - The currently selected account ID
 * 
 * @example
 * ```tsx
 * <AccountsDropdown 
 *   currentAccount={selectedAccountId} 
 *   handleChange={(event) => setSelectedAccountId(event.target.value)} 
 * />
 * ```
 * 
 * @returns {JSX.Element} A dropdown selector for trading accounts
 */
export const AccountsDropdown = ({handleChange, currentAccount}:AccountsDropdownProps) => {
  const accounts = GetAccounts()
  const accountUsers = accounts.map((account:AccountData) => {
    return (
      <MenuItem
        value={account.id}
        key={account.id}
      >
        {account.displayName}
      </MenuItem>
    )
  })

  return (
      <FormControl sx={{ m: 1, minWidth: 120 }} size="small">
        <InputLabel>Accounts</InputLabel>
        <Select
          value={currentAccount}
          label="Accounts"
          onChange={handleChange}
        >
          <MenuItem value="">
            <em>None</em>
          </MenuItem>
          {accountUsers}
        </Select>
      </FormControl>
  );
}
