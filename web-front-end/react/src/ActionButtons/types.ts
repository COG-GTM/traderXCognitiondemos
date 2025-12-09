/**
 * ActionButtons Type Definitions
 * 
 * Type definitions for the action button components used in the trading interface.
 * These types were created during the Angular to React migration to provide
 * type safety for component props and data structures.
 * 
 * @migration
 * - Angular Source: Various @Input() and model definitions from Angular components
 * - TypeScript interfaces replace Angular model classes
 */

/**
 * Props interface for action button components.
 * 
 * @description
 * Defines the common props passed to action button components like
 * CreateTradeButton and CreateAccountUser.
 * 
 * @migration
 * - Replaces Angular @Input() account binding from TradeTicketComponent
 * - Replaces Angular @Input() account from AssignUserToAccountComponent
 * 
 * @property {number} accountId - The ID of the account to perform actions on
 */
export interface ActionButtonsProps {
	accountId: number;
}

/**
 * Data structure for people/user information.
 * 
 * @description
 * Represents a person/user entity returned from the People Service API.
 * Used for user search and assignment functionality.
 * 
 * @migration
 * - Angular Source: User model in model/user.model.ts
 * - Maps to the People Service API response structure
 * 
 * @property {string} logonId - User's login identifier
 * @property {string} fullName - User's full display name
 * @property {string} email - User's email address
 * @property {string} employeeId - User's employee identifier
 * @property {string} department - User's department name
 * @property {string} photoUrl - URL to user's profile photo
 */
export interface PeopleData {
	logonId: "string";
  fullName: "string";
  email: "string";
  employeeId: "string";
  department: "string";
  photoUrl: "string";
}

/**
 * Trade side type definition.
 * 
 * @description
 * Represents the direction of a trade - either buying or selling securities.
 * Can be undefined when no side has been selected yet.
 * 
 * @migration
 * - Angular Source: Side property in TradeTicket model
 * - Used in trade creation forms and trade data structures
 */
export type Side = 'Buy' | 'Sell' | undefined;

/**
 * Reference data structure for securities.
 * 
 * @description
 * Represents a security/stock entity returned from the Reference Data Service.
 * Used to populate security selection dropdowns in trade forms.
 * 
 * @migration
 * - Angular Source: Stock model in model/symbol.model.ts
 * - Maps to the Reference Data Service API response structure
 * 
 * @property {string} ticker - Stock ticker symbol (e.g., "AAPL", "GOOGL")
 * @property {string} companyName - Full company name
 */
export interface RefData {
	ticker: string;
	companyName: string;
}

/**
 * Reference data company names structure.
 * 
 * @description
 * Simplified structure containing only company names from reference data.
 * Used for display purposes in autocomplete/typeahead components.
 * 
 * @property {string} companyNames - Company name string
 */
export interface RefDataCompanyNames {
	companyNames: string;
}

/**
 * Trade form data structure.
 * 
 * @description
 * Represents the data collected from a trade creation form.
 * Contains the essential fields needed to submit a new trade.
 * 
 * @migration
 * - Angular Source: TradeTicket model in model/trade.model.ts
 * - Subset of fields used in the trade creation form
 * 
 * @property {string} security - The ticker symbol of the security to trade
 * @property {number} quantity - The number of shares to trade
 */
export interface TradeFormData {
	security: string;
	quantity: number;
}
