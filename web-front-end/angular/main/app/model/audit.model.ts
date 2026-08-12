export enum Decision {
    Accepted = 'ACCEPTED',
    Rejected = 'REJECTED'
}

export interface OrderDecision {
    id: string;
    correlationId: string;
    orderId?: string;
    accountId?: number;
    security?: string;
    side?: string;
    quantity?: number;
    price?: number;
    priceSource?: string;
    notional?: number;
    decision: Decision;
    reasonCode: string;
    limitId?: string;
    limitType?: string;
    limitValue?: number;
    limitEffectiveFrom?: string;
    submittedBy?: string;
    decisionTimestamp: string;
}

export interface AuditPage {
    content: OrderDecision[];
    page: number;
    size: number;
    totalElements: number;
    totalPages: number;
}

export interface AuditQuery {
    accountId?: number;
    security?: string;
    decision?: Decision | '';
    from?: string;
    to?: string;
    page?: number;
    size?: number;
}
