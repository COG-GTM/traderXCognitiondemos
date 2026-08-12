import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { AuditPage, AuditQuery } from '../model/audit.model';
import { environment } from 'main/environments/environment';

/**
 * Read side of the best-execution audit trail, served by position-service. There is no write
 * method here and there should not be one: the record is evidence, not application state.
 */
@Injectable({
    providedIn: 'root'
})
export class AuditService {
    private decisionsUrl = `${environment.positionsUrl}/audit/decisions`;

    constructor(private http: HttpClient) { }

    getDecisions(query: AuditQuery): Observable<AuditPage> {
        let params = new HttpParams();
        Object.entries(query).forEach(([key, value]) => {
            if (value !== undefined && value !== null && value !== '') {
                params = params.set(key, String(value));
            }
        });
        return this.http.get<AuditPage>(this.decisionsUrl, { params }).pipe(
            catchError(this.handleError)
        );
    }

    private handleError(error: HttpErrorResponse) {
        console.error(error);
        return throwError(() => error);
    }
}
