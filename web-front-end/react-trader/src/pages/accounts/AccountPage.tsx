import { useCallback, useEffect, useMemo, useState } from 'react';
import { AgGridReact } from 'ag-grid-react';
import type { ColDef, SelectionChangedEvent } from 'ag-grid-community';
import 'ag-grid-community/styles/ag-grid.css';
import 'ag-grid-community/styles/ag-theme-alpine.css';
import type { Account } from '../../models/account.model';
import type { AccountUser } from '../../models/user.model';
import { getAccounts, getAccountUsers } from '../../services/account.service';
import { ButtonCellRenderer } from '../../components/accounts/ButtonCellRenderer';
import { EditAccount } from '../../components/accounts/EditAccount';
import { AssignUser } from '../../components/accounts/AssignUser';

export function AccountPage() {
    const [accounts, setAccounts] = useState<Account[]>([]);
    const [users, setUsers] = useState<AccountUser[]>([]);
    const [selectedAccount, setSelectedAccount] = useState<Account | undefined>(undefined);
    const [accountToBeUpdate, setAccountToBeUpdate] = useState<Account | undefined>(undefined);
    const [refreshToken, setRefreshToken] = useState(0);

    const selectedAccountId = selectedAccount?.id ?? 0;

    useEffect(() => {
        let cancelled = false;
        const timer = setTimeout(async () => {
            try {
                const data = await getAccounts();
                if (!cancelled) {
                    setAccounts(data);
                }
            } catch (err) {
                console.error(err);
            }
        }, 200);
        return () => {
            cancelled = true;
            clearTimeout(timer);
        };
    }, [refreshToken]);

    useEffect(() => {
        let cancelled = false;
        const timer = setTimeout(async () => {
            try {
                const data = await getAccountUsers();
                if (!cancelled) {
                    setUsers(data.filter((user) => user.accountId === selectedAccountId));
                }
            } catch (err) {
                console.error(err);
            }
        }, 200);
        return () => {
            cancelled = true;
            clearTimeout(timer);
        };
    }, [selectedAccountId, refreshToken]);

    const columnDefs = useMemo<ColDef[]>(
        () => [
            { field: 'id', flex: 1 },
            { field: 'displayName', flex: 2 },
            {
                headerName: 'Update',
                cellRenderer: ButtonCellRenderer,
                cellRendererParams: {
                    clicked: (account: Account) => setAccountToBeUpdate(account)
                },
                flex: 1
            }
        ],
        []
    );

    const columnDefsUser = useMemo<ColDef[]>(
        () => [
            { field: 'accountId', flex: 1 },
            { field: 'username', flex: 1 }
        ],
        []
    );

    const onUpdate = useCallback((account: Account) => {
        setSelectedAccount(account);
        setRefreshToken((token) => token + 1);
    }, []);

    const onSelectionChanged = useCallback((event: SelectionChangedEvent) => {
        const selectedRows = event.api.getSelectedRows() as Account[];
        setSelectedAccount(selectedRows[0]);
    }, []);

    return (
        <div className="p-5 pt-3">
            <div className="d-flex flex-column">
                <EditAccount onUpdate={onUpdate} account={accountToBeUpdate} />
                <div className="mt-4">
                    <h6> Account List </h6>
                    <div id="accountgrid" className="ag-theme-alpine" style={{ width: 800, height: 300 }}>
                        <AgGridReact
                            columnDefs={columnDefs}
                            rowSelection="single"
                            rowData={accounts}
                            onSelectionChanged={onSelectionChanged}
                        />
                    </div>
                </div>
            </div>

            <div className="mt-4">
                <AssignUser onUpdate={onUpdate} account={selectedAccount} accounts={accounts} />
                <div className="mt-4">
                    <h6>Users List {selectedAccount ? `(${selectedAccount.displayName})` : ''}</h6>
                    <div id="usergrid" className="ag-theme-alpine" style={{ width: 800, height: 300 }}>
                        <AgGridReact columnDefs={columnDefsUser} rowData={users} />
                    </div>
                </div>
            </div>
        </div>
    );
}

export default AccountPage;
