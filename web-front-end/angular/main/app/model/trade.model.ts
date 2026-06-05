export interface Trade {
    accountid: number;
    created: Date;
    id: string;
    quantity: number;
    security: string;
    side: Side;
    state: State;
    complianceStatus: ComplianceStatus;
    updated: Date;
}

export enum ComplianceStatus {
    PendingReview = 'PENDING_REVIEW',
    Approved = 'APPROVED',
    Flagged = 'FLAGGED',
    Rejected = 'REJECTED'
}

export enum Side {
    Sell = 'Sell',
    Buy = 'Buy'
}

export enum State {
    New = 'New',
    Processing = 'Processing',
    Pending = 'Pending',
    Settled = 'Settled'
}

export interface Position {
    accountid: number;
    quantity: number;
    security: string;
    updated: Date;
}

export interface TradeTicket {
    side: 'Sell' | 'Buy';
    quantity: number;
    security: string;
    accountId: number;
    complianceStatus?: ComplianceStatus;
}
