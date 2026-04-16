import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { SymbolService } from './symbols.service';
import { Stock } from '../model/symbol.model';
import { TradeTicket } from '../model/trade.model';

describe('SymbolService', () => {
  let service: SymbolService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [SymbolService]
    });
    service = TestBed.inject(SymbolService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should fetch stocks via GET', () => {
    const mockStocks: Stock[] = [
      { ticker: 'AAPL', companyName: 'Apple Inc.' },
      { ticker: 'MSFT', companyName: 'Microsoft Corp.' }
    ];

    service.getStocks().subscribe(stocks => {
      expect(stocks.length).toBe(2);
      expect(stocks[0].ticker).toBe('AAPL');
    });

    const req = httpMock.expectOne(req => req.url.includes('/stocks'));
    expect(req.request.method).toBe('GET');
    req.flush(mockStocks);
  });

  it('should create a trade ticket via POST', () => {
    const ticket: TradeTicket = { side: 'Buy', quantity: 100, security: 'AAPL', accountId: 1 };

    service.createTicket(ticket).subscribe(response => {
      expect(response).toBeTruthy();
    });

    const req = httpMock.expectOne(req => req.url.includes('/trade/'));
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(ticket);
    req.flush({ success: true });
  });

  it('should handle error on getStocks', () => {
    service.getStocks().subscribe({
      error: (error) => {
        expect(error.status).toBe(500);
      }
    });

    // retry(2) means 3 total requests
    const req1 = httpMock.expectOne(req => req.url.includes('/stocks'));
    req1.flush('Error', { status: 500, statusText: 'Server Error' });
    const req2 = httpMock.expectOne(req => req.url.includes('/stocks'));
    req2.flush('Error', { status: 500, statusText: 'Server Error' });
    const req3 = httpMock.expectOne(req => req.url.includes('/stocks'));
    req3.flush('Error', { status: 500, statusText: 'Server Error' });
  });

  it('should handle error on createTicket', () => {
    const ticket: TradeTicket = { side: 'Buy', quantity: 100, security: 'AAPL', accountId: 1 };

    service.createTicket(ticket).subscribe({
      error: (error) => {
        expect(error.status).toBe(400);
      }
    });

    const req = httpMock.expectOne(req => req.url.includes('/trade/'));
    req.flush('Bad Request', { status: 400, statusText: 'Bad Request' });
  });
});
