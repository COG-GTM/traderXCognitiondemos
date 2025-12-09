/**
 * Datatable Type Definitions
 * 
 * Type definitions for the datatable component and related trade/position data structures.
 * These types were created during the Angular to React migration to provide
 * type safety for grid data and trading entities.
 * 
 * @migration
 * - Angular Source: Trade and Position models in model/trade.model.ts
 * - TypeScript interfaces replace Angular model classes
 */

/**
 * Trade side type definition.
 * 
 * @description
 * Represents the direction of a trade - either buying or selling securities.
 * Unlike the Side type in ActionButtons, this type does not allow undefined
 * as it represents the actual side of an executed trade.
 * 
 * @migration
 * - Angular Source: Side property in Trade model
 * - Used in trade data structures for display in trade blotter
 */
export type TradeSide = 'Buy' | 'Sell';

/**
 * Data structure for trade information.
 * 
 * @description
 * Represents a trade entity returned from the Position Service API.
 * Used to populate the trade blotter grid and display trade history.
 * Contains all fields needed to display trade details including status.
 * 
 * @migration
 * - Angular Source: Trade model in model/trade.model.ts
 * - Maps to the Position Service /trades/{accountId} API response structure
 * 
 * @property {string} [id] - Unique identifier for the trade (e.g., "TRADE-123456")
 * @property {number} [accountId] - The account ID this trade belongs to
 * @property {string} security - The ticker symbol of the traded security
 * @property {TradeSide} [side] - The direction of the trade (Buy or Sell)
 * @property {string} [state] - Current state of the trade (New, Processing, Settled, Cancelled)
 * @property {number} quantity - The number of shares traded
 * @property {Date} [updated] - Timestamp of the last update to this trade
 * @property {Date} [created] - Timestamp when the trade was created
 */
export interface TradeData {
	id?: string;
	accountId?: number;
	security: string;
	side?: TradeSide;
	state?: string;
	quantity: number;
	updated?: Date;
	created?: Date;
}

/**
 * Data structure for position information.
 * 
 * @description
 * Represents a position entity returned from the Position Service API.
 * Used to populate the position blotter grid and display current holdings.
 * A position represents the aggregate quantity of a security held in an account.
 * 
 * @migration
 * - Angular Source: Position model in model/trade.model.ts
 * - Maps to the Position Service /positions/{accountId} API response structure
 * 
 * @property {number} accountId - The account ID this position belongs to
 * @property {string} security - The ticker symbol of the held security
 * @property {number} quantity - The current quantity of shares held (can be negative for short positions)
 * @property {Date} updated - Timestamp of the last update to this position
 */
export interface PositionData {
	accountId: number;
	security: string;
	quantity: number;
	updated: Date;
}
