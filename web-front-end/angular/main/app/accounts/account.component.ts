import { ColDef, GridApi, GridReadyEvent, Module } from 'ag-grid-community';
import { Component, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { AgGridModule, AgGridAngular } from 'ag-grid-angular';
import { BehaviorSubject } from 'rxjs';
import { AccountService } from '../service/account.service';
import { Account } from '../model/account.model';
import { debounceTime, map, switchMap } from 'rxjs/operators';
import { ButtonCellRendererComponent } from './button-renderer.component';
import { UserService } from '../service/user.service';
import { EditAccountComponent } from './edit/edit.component';
import { AssignUserToAccountComponent } from './user/assign-user.component';

@Component({
    selector: 'app-account',
    templateUrl: 'account.component.html',
    styleUrls: ['account.component.scss'],
    imports: [EditAccountComponent, AgGridAngular, AssignUserToAccountComponent]
})
export class AccountComponent {
    private accountService = inject(AccountService);

    private gridApi!: GridApi;

    accountBehaviorSubject = new BehaviorSubject(0);
    accountAddAction$ = this.accountBehaviorSubject.asObservable();
    readonly accounts = toSignal(this.accountAddAction$.pipe(
        debounceTime(200),
        switchMap(() => this.accountService.getAccounts())
    ), { initialValue: null });
    readonly users = toSignal(this.accountAddAction$.pipe(
        debounceTime(200),
        switchMap((accountId) => this.accountService.getAccountUsers().pipe(
            map((users) => users.filter((user) => user.accountId === accountId))
        ))
    ), { initialValue: null });
    readonly selectedAccount = signal<Account | undefined>(undefined);
    readonly accountToBeUpdate = signal<Account | undefined>(undefined);
    columnDefs: ColDef[] = [
        {
            field: 'id',
            flex: 1
        },
        {
            field: 'displayName',
            flex: 2
        },
        {
            headerName: 'Update',
            cellRenderer: 'btnCellRenderer',
            cellRendererParams: {
                clicked: (account: Account) => this.accountToBeUpdate.set(account)
            },
            flex: 1
        }
    ];

    columnDefsUser: ColDef[] = [
        {
            field: 'accountId',
            flex: 1
        },
        {
            field: 'username',
            flex: 1
        }
    ];

    frameworkComponents = {
        btnCellRenderer: ButtonCellRendererComponent
    };

    onUpdate(account: Account) {
        this.accountBehaviorSubject.next(account?.id);
        this.selectedAccount.set(account);
    }

    onSelectionChanged() {
        const selectedRows = this.gridApi.getSelectedRows() as Account[];
        this.selectedAccount.set(selectedRows[0]);
        this.accountBehaviorSubject.next(selectedRows[0].id);
    }

    onGridReady(params: GridReadyEvent) {
        this.gridApi = params.api;
    }
}
