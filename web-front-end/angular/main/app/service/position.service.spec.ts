import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { PositionService } from './position.service';
import { Trade, Position, Side, State } from '../model/trade.model';

describe('PositionService', () => {
  let service: PositionService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [PositionService]
    });
    service = TestBed.inject(PositionService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should fetch trades for an account', () => {
    const mockTrades: Trade[] = [
      { accountid: 1, created: new Date(), id: 'T1', quantity: 100, security: 'AAPL', side: Side.Buy, state: State.Settled, updated: new Date() },
      { accountid: 1, created: new Date(), id: 'T2', quantity: 50, security: 'MSFT', side: Side.Sell, state: State.Pending, updated: new Date() }
    ];

    service.getTrades(1).subscribe(trades => {
      expect(trades.length).toBe(2);
      expect(trades[0].security).toBe('AAPL');
    });

    const req = httpMock.expectOne(req => req.url.includes('/trades/1'));
    expect(req.request.method).toBe('GET');
    req.flush(mockTrades);
  });

  it('should fetch positions for an account', () => {
    const mockPositions: Position[] = [
      { accountid: 1, quantity: 100, security: 'AAPL', updated: new Date() },
      { accountid: 1, quantity: 200, security: 'MSFT', updated: new Date() }
    ];

    service.getPositions(1).subscribe(positions => {
      expect(positions.length).toBe(2);
      expect(positions[0].quantity).toBe(100);
    });

    const req = httpMock.expectOne(req => req.url.includes('/positions/1'));
    expect(req.request.method).toBe('GET');
    req.flush(mockPositions);
  });

  it('should handle error on getTrades', () => {
    service.getTrades(1).subscribe({
      error: (error) => {
        expect(error.status).toBe(404);
      }
    });

    const req = httpMock.expectOne(req => req.url.includes('/trades/1'));
    req.flush('Not Found', { status: 404, statusText: 'Not Found' });
  });

  it('should handle error on getPositions', () => {
    service.getPositions(1).subscribe({
      error: (error) => {
        expect(error.status).toBe(500);
      }
    });

    const req = httpMock.expectOne(req => req.url.includes('/positions/1'));
    req.flush('Server Error', { status: 500, statusText: 'Internal Server Error' });
  });
});
