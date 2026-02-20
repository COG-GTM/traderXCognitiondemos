import React, { useCallback, useEffect, useState } from 'react';
import { AgGridReact } from 'ag-grid-react';
import { ColDef, GridReadyEvent, GridApi } from 'ag-grid-community';
import 'ag-grid-community/styles/ag-grid.css';
import 'ag-grid-community/styles/ag-theme-alpine.css';
import { Button } from '@mui/material';
import { Environment } from '../env';
import { Account, AccountUser } from './types';
import { EditAccount } from './EditAccount';
import { AssignUser } from './AssignUser';
import './AccountManagement.css';

export const AccountManagement: React.FC = () => {
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [accountUsers, setAccountUsers] = useState<AccountUser[]>([]);
  const [selectedAccount, setSelectedAccount] = useState<Account | null>(null);
  const [accountToEdit, setAccountToEdit] = useState<Account | null>(null);
  const [gridApi, setGridApi] = useState<GridApi | null>(null);

  const fetchAccounts = useCallback(async () => {
    try {
      const res = await fetch(`${Environment.account_service_url}/account/`);
      if (res.ok) {
        const data = await res.json();
        setAccounts(data);
      }
    } catch (err) {
      console.error(err);
    }
  }, []);

  const fetchAccountUsers = useCallback(async (accountId: number) => {
    try {
      const res = await fetch(`${Environment.account_service_url}/accountuser/`);
      if (res.ok) {
        const allUsers: AccountUser[] = await res.json();
        setAccountUsers(allUsers.filter((u) => u.accountId === accountId));
      }
    } catch (err) {
      console.error(err);
    }
  }, []);

  useEffect(() => {
    fetchAccounts();
  }, [fetchAccounts]);

  useEffect(() => {
    if (selectedAccount) {
      fetchAccountUsers(selectedAccount.id);
    } else {
      setAccountUsers([]);
    }
  }, [selectedAccount, fetchAccountUsers]);

  const handleUpdate = useCallback((account: Account) => {
    fetchAccounts();
    if (selectedAccount) {
      fetchAccountUsers(selectedAccount.id);
    }
  }, [fetchAccounts, fetchAccountUsers, selectedAccount]);

  const onSelectionChanged = useCallback(() => {
    if (gridApi) {
      const selectedRows = gridApi.getSelectedRows() as Account[];
      if (selectedRows.length > 0) {
        setSelectedAccount(selectedRows[0]);
      }
    }
  }, [gridApi]);

  const onGridReady = useCallback((params: GridReadyEvent) => {
    setGridApi(params.api);
  }, []);

  const UpdateButtonRenderer: React.FC<{ data: Account }> = ({ data }) => (
    <Button
      variant="contained"
      size="small"
      color="info"
      onClick={() => setAccountToEdit(data)}
    >
      Update
    </Button>
  );

  const accountColumnDefs: ColDef[] = [
    { field: 'id', flex: 1 },
    { field: 'displayName', flex: 2 },
    {
      headerName: 'Update',
      cellRenderer: UpdateButtonRenderer,
      flex: 1,
    },
  ];

  const userColumnDefs: ColDef[] = [
    { field: 'accountId', flex: 1 },
    { field: 'username', flex: 1 },
  ];

  return (
    <div className="account-management">
      <div className="account-section">
        <EditAccount accountToEdit={accountToEdit} onUpdate={handleUpdate} />
        <div className="account-grid-section">
          <h6>Account List</h6>
          <div className="ag-theme-alpine" style={{ width: 800, height: 300 }}>
            <AgGridReact
              columnDefs={accountColumnDefs}
              rowData={accounts}
              rowSelection="single"
              onSelectionChanged={onSelectionChanged}
              onGridReady={onGridReady}
            />
          </div>
        </div>
      </div>
      <div className="user-section">
        <AssignUser
          accounts={accounts}
          selectedAccount={selectedAccount}
          onUpdate={handleUpdate}
        />
        <div className="user-grid-section">
          <h6>
            Users List{selectedAccount ? ` (${selectedAccount.displayName})` : ''}
          </h6>
          <div className="ag-theme-alpine" style={{ width: 800, height: 300 }}>
            <AgGridReact columnDefs={userColumnDefs} rowData={accountUsers} />
          </div>
        </div>
      </div>
    </div>
  );
};
