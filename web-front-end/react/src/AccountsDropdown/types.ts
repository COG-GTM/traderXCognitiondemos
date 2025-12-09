import { SelectChangeEvent } from "@mui/material";
import { ReactNode } from "react";

/**
 * AccountsDropdown Type Definitions
 * 
 * Type definitions for the accounts dropdown component and related data structures.
 * These types were created during the Angular to React migration to provide
 * type safety for component props and account data.
 * 
 * @migration
 * - Angular Source: Account model and DropdownComponent @Input/@Output definitions
 * - TypeScript interfaces replace Angular model classes and component bindings
 */

/**
 * Data structure for account information.
 * 
 * @description
 * Represents a trading account entity returned from the Account Service API.
 * Used to populate account selection dropdowns and identify accounts for operations.
 * 
 * @migration
 * - Angular Source: Account model in model/account.model.ts
 * - Maps to the Account Service API response structure
 * 
 * @property {number} id - Unique identifier for the account
 * @property {string} displayName - Human-readable name for the account
 */
export interface AccountData {
	id: number;
	displayName: string
}

/**
 * Props interface for the AccountsDropdown component.
 * 
 * @description
 * Defines the props passed to the AccountsDropdown component for controlling
 * selection state and handling selection changes.
 * 
 * @migration
 * - Angular Source: DropdownComponent @Input/@Output bindings
 * - handleChange replaces Angular @Output() selectedItemChange EventEmitter
 * - currentAccount replaces Angular @Input() selectedItem
 * 
 * @property {function} handleChange - Callback function triggered when account selection changes
 * @property {string} currentAccount - The currently selected account ID as a string
 */
export interface AccountsDropdownProps {
  handleChange: (
    event: SelectChangeEvent<string>, 
    child: ReactNode
    ) => void;
  currentAccount: string;
}

/**
 * Type for matching people search results.
 * 
 * @description
 * Simplified structure for people search results containing only the full name.
 * Used in typeahead/autocomplete components for user search functionality.
 * 
 * @migration
 * - Angular Source: User model subset used in AssignUserToAccountComponent
 * - Simplified from full User model for search result display
 * 
 * @property {string} fullName - The full name of the matched person
 */
export type MatchingPeople = {
	fullName: string;
}
