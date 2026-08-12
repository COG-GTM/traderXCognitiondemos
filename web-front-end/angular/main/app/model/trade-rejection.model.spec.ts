import { formatRejectionMessage, parseTradeRejection } from './trade-rejection.model';

describe('trade rejection', () => {

    const rejectionBody = {
        decision: 'REJECTED',
        reason: 'NOTIONAL_LIMIT_BREACH',
        limit: 1000000,
        attempted: 1250000
    };

    it('should parse a structured 422 body', () => {
        expect(parseTradeRejection(422, rejectionBody)).toEqual(rejectionBody);
    });

    it('should ignore a body on a status other than 422', () => {
        expect(parseTradeRejection(404, rejectionBody)).toBeUndefined();
    });

    it('should ignore an unstructured 422 body', () => {
        expect(parseTradeRejection(422, 'Trade not found')).toBeUndefined();
        expect(parseTradeRejection(422, { message: 'nope' })).toBeUndefined();
    });

    it('should name the limit, the attempt and the excess as currency', () => {
        expect(formatRejectionMessage(rejectionBody)).toEqual(
            'Order rejected: notional limit breach. Account limit $1,000,000.00, this order $1,250,000.00 — '
            + 'over by $250,000.00. Amend the order and resubmit.'
        );
    });

    it('should humanise an unknown reason code and cope with missing amounts', () => {
        expect(formatRejectionMessage({ decision: 'REJECTED', reason: 'RESTRICTED_SECURITY' })).toEqual(
            'Order rejected: restricted security. Amend the order and resubmit.'
        );
    });

});
