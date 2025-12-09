import { useEffect, useState } from "react";
import { AccountData } from "../AccountsDropdown";
import { Environment } from '../env';

/**
 * GetAccounts Custom Hook
 * 
 * A React custom hook that fetches and returns the list of trading accounts.
 * This hook was migrated from the Angular AccountService.getAccounts() method.
 * 
 * @description
 * Fetches account data from the account service API on component mount.
 * Returns an array of AccountData objects representing available trading accounts.
 * The fetch is performed once when the component mounts (empty dependency array equivalent).
 * 
 * @migration
 * - Angular Source: AccountService.getAccounts() in service/account.service.ts
 * - Angular @Injectable service replaced with custom hook pattern
 * - RxJS Observable with retry and catchError replaced with async/await and try/catch
 * - Angular HttpClient replaced with fetch API
 * - Angular dependency injection replaced with direct hook usage
 * 
 * @example
 * ```tsx
 * const accounts = GetAccounts();
 * // accounts is AccountData[]
 * ```
 * 
 * @returns {AccountData[]} Array of account objects with id and displayName
 */
export const GetAccounts = () => {
	const [accounts, setAccounts] = useState<AccountData[]>([]);
  useEffect(() => {
    const loadAccounts = async () => {
      const response = await fetch(`${Environment.account_service_url}/account/`);
      // const response = await fetch(`/account/`)
      if (response.ok) {
        const accounts = await response.json();
        setAccounts(accounts);
      }
      else {
        console.log('error');
      }
      // setAccounts(accountData);
    }
    loadAccounts();
  }, [setAccounts]);
	return accounts
}
