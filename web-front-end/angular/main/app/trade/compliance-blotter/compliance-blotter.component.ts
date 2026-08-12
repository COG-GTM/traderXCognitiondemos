import { ColDef, GridApi, GridReadyEvent, RowClassParams } from 'ag-grid-community';
import { Component, Input, OnChanges, OnInit, SimpleChanges } from '@angular/core';
import { Account } from 'main/app/model/account.model';
import { AuditQuery, Decision, OrderDecision } from 'main/app/model/audit.model';
import { AuditService } from 'main/app/service/audit.service';

/**
 * Compliance view over the retained order decisions (MiFID II Art. 16(6), RTS 27/28).
 *
 * Read-only by construction: no row is editable, and there is no action on this view that can
 * change or remove a record.
 */
@Component({
    selector: 'app-compliance-blotter',
    templateUrl: './compliance-blotter.component.html',
    styleUrls: ['./compliance-blotter.component.scss']
})
export class ComplianceBlotterComponent implements OnInit, OnChanges {
    @Input() account?: Account;

    decisions: OrderDecision[] = [];
    gridApi: GridApi;
    loading = false;
    unavailable = false;
    error = '';

    limitToAccount = true;
    security = '';
    decision: Decision | '' = '';
    from = '';
    to = '';

    page = 0;
    pageSize = 25;
    totalElements = 0;
    totalPages = 0;

    /** Whether the last query narrowed the trail, so an empty result can be explained as such. */
    filtered = false;

    readonly decisionOptions = [
        { value: '', label: 'All decisions' },
        { value: Decision.Rejected, label: 'Rejected only' },
        { value: Decision.Accepted, label: 'Accepted only' }
    ];

    columnDefs: ColDef[] = [
        {
            headerName: 'DECISION',
            field: 'decision',
            width: 120,
            cellClass: (params) => params.value === Decision.Rejected ? 'compliance-cell-rejected' : ''
        },
        { headerName: 'REASON', field: 'reasonCode', width: 200 },
        { headerName: 'TIMESTAMP (UTC)', field: 'decisionTimestamp', width: 200 },
        { headerName: 'ACCOUNT', field: 'accountId', width: 110 },
        { headerName: 'SECURITY', field: 'security', width: 110 },
        { headerName: 'SIDE', field: 'side', width: 90 },
        { headerName: 'QUANTITY', field: 'quantity', width: 110 },
        { headerName: 'NOTIONAL', field: 'notional', width: 130 },
        { headerName: 'LIMIT APPLIED', field: 'limitValue', width: 140 },
        { headerName: 'SUBMITTED BY', field: 'submittedBy', width: 140 },
        { headerName: 'CORRELATION ID', field: 'correlationId', width: 300 }
    ];

    defaultColDef: ColDef = { sortable: false, resizable: true };

    constructor(private auditService: AuditService) { }

    /**
     * Nothing is loaded until the account arrives if the view claims to be scoped to it. An
     * unscoped first query would briefly show other accounts' decisions under a ticked
     * "Selected account only", which on a compliance screen is not a cosmetic problem.
     */
    ngOnInit() {
        if (!this.limitToAccount || this.account) {
            this.load();
        }
    }

    ngOnChanges(change: SimpleChanges) {
        if (change.account?.currentValue && change.account.currentValue !== change.account.previousValue
            && this.limitToAccount) {
            this.page = 0;
            this.load();
        }
    }

    applyFilters() {
        this.page = 0;
        this.load();
    }

    nextPage() {
        if (this.page + 1 < this.totalPages) {
            this.page += 1;
            this.load();
        }
    }

    previousPage() {
        if (this.page > 0) {
            this.page -= 1;
            this.load();
        }
    }

    onGridReady(params: GridReadyEvent) {
        this.gridApi = params.api;
    }

    /**
     * Rejections are the records a regulator asks about first, so they are the ones the eye
     * should land on.
     */
    getRowClass(params: RowClassParams): string {
        return params.data?.decision === Decision.Rejected ? 'compliance-row-rejected' : '';
    }

    get isEmpty(): boolean {
        return !this.loading && !this.unavailable && !this.error && this.decisions.length === 0;
    }

    private load() {
        this.filtered = Boolean(this.security?.trim() || this.decision || this.from || this.to);
        const query: AuditQuery = {
            accountId: this.limitToAccount ? this.account?.id : undefined,
            security: this.security?.trim() || undefined,
            decision: this.decision || undefined,
            from: this.toInstant(this.from),
            to: this.toInstant(this.to),
            page: this.page,
            size: this.pageSize
        };

        this.loading = true;
        this.error = '';
        this.auditService.getDecisions(query).subscribe({
            next: (result) => {
                this.decisions = result.content;
                this.page = result.page;
                this.totalElements = result.totalElements;
                this.totalPages = result.totalPages;
                this.unavailable = false;
                this.loading = false;
            },
            error: (response) => {
                this.decisions = [];
                this.totalElements = 0;
                this.totalPages = 0;
                this.unavailable = response?.status === 503;
                this.error = this.unavailable ? '' : 'Could not load the audit trail.';
                this.loading = false;
            }
        });
    }

    /**
     * The datetime-local inputs are wall clock with no zone. They are read as UTC, matching the
     * column the reviewer is reading the timestamps from: parsing them in the browser's zone
     * would shift a quarter-boundary query by the viewer's offset and quietly drop the records
     * at each end of the window they asked for.
     */
    private toInstant(value: string): string | undefined {
        return value ? new Date(`${value}Z`).toISOString() : undefined;
    }
}
