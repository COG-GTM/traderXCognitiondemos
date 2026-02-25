import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { environment } from 'main/environments/environment';

export interface PnlEntry {
    accountId: number;
    security: string;
    netQuantity: number;
    avgCostBasis: number;
    realizedPnL: number;
    currentPrice: number;
    marketValue: number;
    unrealizedPnL: number;
    totalPnL: number;
    lastUpdated: string;
}

@Injectable({
    providedIn: 'root'
})
export class PnlService {
    private pnlUrl = `${environment.pnlServiceUrl}/pnl/`;
    constructor(private http: HttpClient) { }

    getPnl(accountId: number): Observable<PnlEntry[]> {
        return this.http.get<PnlEntry[]>(this.pnlUrl + accountId).pipe(
            catchError(this.handleError)
        );
    }

    private handleError(error: HttpErrorResponse) {
        console.error(error);
        return throwError(() => error);
    }
}
