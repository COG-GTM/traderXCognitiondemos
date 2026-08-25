import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { SymbolService } from './symbols.service';
import { environment } from 'main/environments/environment';
import { TradeTicket } from '../model/trade.model';

describe('SymbolService HTTP edge cases', () => {
    let service: SymbolService;
    let httpMock: HttpTestingController;

    const ticket: TradeTicket = { accountId: 1, quantity: 10, security: 'AAPL', side: 'Buy' };

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [HttpClientTestingModule],
            providers: [SymbolService]
        });
        service = TestBed.inject(SymbolService);
        httpMock = TestBed.inject(HttpTestingController);
    });

    afterEach(() => httpMock.verify());

    // UI-16k
    it('should retry getStocks twice before giving up on a 500', (done) => {
        service.getStocks().subscribe({
            next: () => done.fail('expected the 500 to propagate'),
            error: (error) => {
                expect(error.status).toEqual(500);
                done();
            }
        });

        for (let attempt = 0; attempt < 3; attempt++) {
            httpMock.expectOne(`${environment.refrenceDataUrl}/stocks`).flush('boom', { status: 500, statusText: 'Server Error' });
        }
    });

    // UI-16l
    it('should propagate a 400 from createTicket so the caller can react', (done) => {
        service.createTicket(ticket).subscribe({
            next: () => done.fail('expected the 400 to propagate'),
            error: (error) => {
                expect(error.status).toEqual(400);
                done();
            }
        });

        httpMock.expectOne(environment.tradesUrl).flush('bad ticker', { status: 400, statusText: 'Bad Request' });
    });

    // UI-16m
    it('should post the ticket payload unchanged, including an out-of-range quantity', () => {
        const hugeTicket: TradeTicket = { ...ticket, quantity: 2147483648 };
        service.createTicket(hugeTicket).subscribe({ next: () => undefined, error: () => undefined });

        const request = httpMock.expectOne(environment.tradesUrl);
        expect(request.request.method).toEqual('POST');
        expect(request.request.body).toEqual(hugeTicket);
        request.flush({});
    });

    // UI-16n
    it('should propagate a connection failure on createTicket', (done) => {
        service.createTicket(ticket).subscribe({
            next: () => done.fail('expected the network error to propagate'),
            error: (error) => {
                expect(error.status).toEqual(0);
                done();
            }
        });

        httpMock.expectOne(environment.tradesUrl).error(new ProgressEvent('network'));
    });
});
