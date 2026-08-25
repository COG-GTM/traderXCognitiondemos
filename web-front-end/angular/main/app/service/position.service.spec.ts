import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { PositionService } from './position.service';
import { environment } from 'main/environments/environment';

describe('PositionService HTTP edge cases', () => {
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

    afterEach(() => httpMock.verify());

    // UI-16e
    it('should surface a 404 from getTrades as an error notification', (done) => {
        service.getTrades(42).subscribe({
            next: () => done.fail('expected the 404 to propagate'),
            error: (error) => {
                expect(error.status).toEqual(404);
                done();
            }
        });

        httpMock.expectOne(`${environment.positionsUrl}/trades/42`).flush('nope', { status: 404, statusText: 'Not Found' });
    });

    // UI-16f
    it('should surface a 500 from getPositions as an error notification', (done) => {
        service.getPositions(42).subscribe({
            next: () => done.fail('expected the 500 to propagate'),
            error: (error) => {
                expect(error.status).toEqual(500);
                done();
            }
        });

        httpMock.expectOne(`${environment.positionsUrl}/positions/42`).flush('boom', { status: 500, statusText: 'Server Error' });
    });

    // UI-16g
    it('should surface a network timeout / connection failure as an error notification', (done) => {
        service.getTrades(42).subscribe({
            next: () => done.fail('expected the network error to propagate'),
            error: (error) => {
                expect(error.status).toEqual(0);
                done();
            }
        });

        httpMock.expectOne(`${environment.positionsUrl}/trades/42`).error(new ProgressEvent('timeout'));
    });

    // UI-16h
    it('should not retry a failed trades request', () => {
        service.getTrades(42).subscribe({ next: () => undefined, error: () => undefined });

        const requests = httpMock.match(`${environment.positionsUrl}/trades/42`);
        expect(requests.length).toEqual(1);
        requests[0].flush('boom', { status: 500, statusText: 'Server Error' });
    });

    // UI-16i
    it('should build a url that keeps a negative or zero account id verbatim', () => {
        service.getPositions(0).subscribe({ next: () => undefined, error: () => undefined });
        httpMock.expectOne(`${environment.positionsUrl}/positions/0`).flush([]);

        service.getPositions(-1).subscribe({ next: () => undefined, error: () => undefined });
        httpMock.expectOne(`${environment.positionsUrl}/positions/-1`).flush([]);
    });

    // UI-16j
    it('should pass an empty trade list straight through', (done) => {
        service.getTrades(42).subscribe((trades) => {
            expect(trades).toEqual([]);
            done();
        });
        httpMock.expectOne(`${environment.positionsUrl}/trades/42`).flush([]);
    });
});
