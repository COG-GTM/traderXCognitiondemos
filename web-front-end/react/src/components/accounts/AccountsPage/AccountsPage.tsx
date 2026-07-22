import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { AgGridReact } from 'ag-grid-react';
import { ColDef, SelectionChangedEvent } from 'ag-grid-community';
import 'ag-grid-community/styles/ag-grid.css';
import 'ag-grid-community/styles/ag-theme-alpine.css';
import { Account, AccountUser } from '../../../models';
import { accountService } from '../../../services/accountService';
import { ButtonCellRenderer } from '../../shared';
import { EditAccount } from '../EditAccount';
import { AssignUser } from '../AssignUser';

// SECTION 7 — Accounts page container (route: /account).
export const AccountsPage = () => {
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [accountUsers, setAccountUsers] = useState<AccountUser[]>([]);
  const [selectedAccount, setSelectedAccount] = useState<Account | undefined>(undefined);
  const [accountToEdit, setAccountToEdit] = useState<Account | undefined>(undefined);

  const loadData = useCallback(async () => {
    const [accountList, userList] = await Promise.all([
      accountService.getAccounts(),
      accountService.getAccountUsers(),
    ]);
    setAccounts(accountList);
    setAccountUsers(userList);
  }, []);

  useEffect(() => {
    loadData();
  }, [loadData]);

  const handleUpdate = useCallback(
    (account: Account) => {
      setSelectedAccount(account);
      loadData();
    },
    [loadData],
  );

  const columnDefs = useMemo<ColDef<Account>[]>(
    () => [
      { field: 'id', flex: 1 },
      { field: 'displayName', flex: 2 },
      {
        headerName: 'Update',
        cellRenderer: ButtonCellRenderer,
        cellRendererParams: {
          clicked: (account: Account) => setAccountToEdit(account),
        },
        flex: 1,
      },
    ],
    [],
  );

  const columnDefsUser = useMemo<ColDef<AccountUser>[]>(
    () => [
      { field: 'accountId', flex: 1 },
      { field: 'username', flex: 1 },
    ],
    [],
  );

  const onSelectionChanged = useCallback((event: SelectionChangedEvent<Account>) => {
    const selectedRows = event.api.getSelectedRows();
    setSelectedAccount(selectedRows[0]);
  }, []);

  const filteredUsers = useMemo(
    () => accountUsers.filter((user) => user.accountId === selectedAccount?.id),
    [accountUsers, selectedAccount],
  );

  return (
    <div className="p-5 pt-3">
      <div className="d-flex flex-column">
        <EditAccount account={accountToEdit} onUpdate={handleUpdate} />
        <div className="mt-4">
          <h6> Account List </h6>
          <div
            id="accountgrid"
            className="ag-theme-alpine"
            style={{ width: '800px', height: '300px' }}
          >
            <AgGridReact<Account>
              columnDefs={columnDefs}
              rowData={accounts}
              rowSelection="single"
              onSelectionChanged={onSelectionChanged}
            />
          </div>
        </div>
      </div>

      <div className="mt-4">
        <AssignUser account={selectedAccount} accounts={accounts} onUpdate={handleUpdate} />
        <div className="mt-4">
          <h6>Users List {selectedAccount ? `(${selectedAccount.displayName})` : ''}</h6>
          <div
            id="usergrid"
            className="ag-theme-alpine"
            style={{ width: '800px', height: '300px' }}
          >
            <AgGridReact<AccountUser> columnDefs={columnDefsUser} rowData={filteredUsers} />
          </div>
        </div>
      </div>
    </div>
  );
};
