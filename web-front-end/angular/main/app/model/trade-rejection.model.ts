export interface TradeRejection {
    decision: string;
    reason: string;
    limit?: number;
    attempted?: number;
}

export const REJECTION_STATUS = 422;

const REASON_LABELS: { [reason: string]: string } = {
    NOTIONAL_LIMIT_BREACH: 'notional limit breach',
    UNPRICEABLE_SECURITY: 'no price available for this security'
};

export function parseTradeRejection(status: number, body: unknown): TradeRejection | undefined {
    if (status !== REJECTION_STATUS || !body || typeof body !== 'object') {
        return undefined;
    }
    const candidate = body as Partial<TradeRejection>;
    if (typeof candidate.decision !== 'string' || typeof candidate.reason !== 'string') {
        return undefined;
    }
    return {
        decision: candidate.decision,
        reason: candidate.reason,
        limit: typeof candidate.limit === 'number' ? candidate.limit : undefined,
        attempted: typeof candidate.attempted === 'number' ? candidate.attempted : undefined
    };
}

export function formatNotional(value: number): string {
    return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(value);
}

const REASON_CODE_PATTERN = /^[A-Z0-9_]{1,64}$/;

export function describeReason(reason: string): string {
    if (REASON_LABELS[reason]) {
        return REASON_LABELS[reason];
    }
    if (!REASON_CODE_PATTERN.test(reason)) {
        return 'a pre-trade risk check';
    }
    return reason.toLowerCase().replace(/_/g, ' ');
}

export function formatRejectionMessage(rejection: TradeRejection): string {
    const headline = `Order rejected: ${describeReason(rejection.reason)}.`;
    if (rejection.limit === undefined || rejection.attempted === undefined) {
        return `${headline} Amend the order and resubmit.`;
    }
    const excess = rejection.attempted - rejection.limit;
    const detail = `${headline} Account limit ${formatNotional(rejection.limit)}, this order ${formatNotional(rejection.attempted)}`;
    if (excess <= 0) {
        return `${detail}. Amend the order and resubmit.`;
    }
    return `${detail} — over by ${formatNotional(excess)}. Amend the order and resubmit.`;
}
