import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { AgGridModule } from 'ag-grid-angular';
import { of, throwError } from 'rxjs';

import { ComplianceBlotterComponent } from './compliance-blotter.component';
import { AuditService } from 'main/app/service/audit.service';
import { AuditPage, AuditQuery, Decision, OrderDecision } from 'main/app/model/audit.model';
import { accounts as dummyAccounts } from 'main/app/test-utils/mocks.service';

const decision = (id: string, outcome: Decision): OrderDecision => ({
    id,
    correlationId: `corr-${id}`,
    accountId: 11413,
    security: 'AAPL',
    side: 'Buy',
    quantity: 100,
    decision: outcome,
    reasonCode: outcome === Decision.Rejected ? 'ACCOUNT_LIMIT_BREACHED' : 'VALIDATED',
    decisionTimestamp: '2026-01-01T10:00:00Z'
});

class MockAuditService {
    lastQuery?: AuditQuery;
    page: AuditPage = { content: [], page: 0, size: 25, totalElements: 0, totalPages: 0 };

    getDecisions(query: AuditQuery) {
        this.lastQuery = query;
        return of({ ...this.page, page: query.page ?? 0 });
    }
}

describe('ComplianceBlotterComponent', () => {
    let component: ComplianceBlotterComponent;
    let fixture: ComponentFixture<ComplianceBlotterComponent>;
    let auditService: MockAuditService;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [ComplianceBlotterComponent],
            imports: [AgGridModule, FormsModule],
            providers: [{ provide: AuditService, useClass: MockAuditService }]
        }).compileComponents();

        fixture = TestBed.createComponent(ComplianceBlotterComponent);
        component = fixture.componentInstance;
        auditService = TestBed.inject(AuditService) as unknown as MockAuditService;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should show an empty state before any order has been submitted', () => {
        expect(component.isEmpty).toBeTrue();
        expect(fixture.nativeElement.textContent).toContain('No order decisions recorded yet');
    });

    it('should mark rejected rows as distinct and accepted rows as ordinary', () => {
        expect(component.getRowClass({ data: decision('a1', Decision.Rejected) } as any))
            .toEqual('compliance-row-rejected');
        expect(component.getRowClass({ data: decision('a2', Decision.Accepted) } as any)).toEqual('');
    });

    it('should send the active filters to the audit service and reset to the first page', () => {
        component.page = 3;
        component.security = ' AAPL ';
        component.decision = Decision.Rejected;
        component.applyFilters();

        expect(auditService.lastQuery).toEqual(jasmine.objectContaining({
            security: 'AAPL',
            decision: Decision.Rejected,
            page: 0,
            size: 25
        }));
    });

    it('should scope to the selected account only while that filter is on', () => {
        component.account = dummyAccounts[0];
        component.applyFilters();
        expect(auditService.lastQuery?.accountId).toEqual(dummyAccounts[0].id);

        component.limitToAccount = false;
        component.applyFilters();
        expect(auditService.lastQuery?.accountId).toBeUndefined();
    });

    it('should page forward and back within the reported page count', () => {
        auditService.page = { content: [], page: 0, size: 25, totalElements: 60, totalPages: 3 };
        component.applyFilters();

        component.nextPage();
        expect(auditService.lastQuery?.page).toEqual(1);
        component.previousPage();
        expect(auditService.lastQuery?.page).toEqual(0);

        component.previousPage();
        expect(auditService.lastQuery?.page).toEqual(0);
    });

    it('should report the feature being switched off rather than an error', () => {
        spyOn(auditService, 'getDecisions').and.returnValue(throwError(() => ({ status: 503 })));
        component.applyFilters();

        expect(component.unavailable).toBeTrue();
        expect(component.error).toEqual('');
    });

    it('should surface a failure to load without clearing into an empty state', () => {
        spyOn(auditService, 'getDecisions').and.returnValue(throwError(() => ({ status: 500 })));
        component.applyFilters();

        expect(component.unavailable).toBeFalse();
        expect(component.error).toBeTruthy();
        expect(component.isEmpty).toBeFalse();
    });
});
