import React, { useCallback, useEffect, useState } from 'react';
import { AgGridReact } from 'ag-grid-react';
import { ColDef, GridReadyEvent, GridApi } from 'ag-grid-community';
import { Button } from '@mui/material';
import { Environment } from '../env';
import { AccountData } from '../AccountsDropdown';
import { EditAccount } from './EditAccount';
import { AssignUser } from './AssignUser';
import './AccountManagement.css';

import 'ag-grid-community/styles/ag-grid.css';
import 'ag-grid-community/styles/ag-theme-alpine.css';

interface AccountUser {
  accountId: number;
  username: string;
}

const UpdateButtonRenderer: React.FC<{
  data: AccountData;
  onClick: (account: AccountData) => void;
}> = ({ data, onClick }) => (
  <Button size="small" variant="contained" color="info" onClick={() => onClick(data)}>
    Update
  </Button>
);

export const AccountManagement: React.FC = () => {
  const [accounts, setAccounts] = useState<AccountData[]>([]);
  const [accountUsers, setAccountUsers] = useState<AccountUser[]>([]);
  const [filteredUsers, setFilteredUsers] = useState<AccountUser[]>([]);
  const [selectedAccount, setSelectedAccount] = useState<AccountData | undefined>(undefined);
  const [accountToBeUpdate, setAccountToBeUpdate] = useState<AccountData | undefined>(undefined);
  const [gridApi, setGridApi] = useState<GridApi | null>(null);

  const accountColumnDefs: ColDef[] = [
    { field: 'id', flex: 1 },
    { field: 'displayName', flex: 2 },
    {
      headerName: 'Update',
      flex: 1,
      cellRenderer: (params: { data: AccountData }) => (
        <UpdateButtonRenderer data={params.data} onClick={setAccountToBeUpdate} />
      ),
    },
  ];

  const userColumnDefs: ColDef[] = [
    { field: 'accountId', flex: 1 },
    { field: 'username', flex: 1 },
  ];

  const fetchAccounts = useCallback(async () => {
    try {
      const response = await fetch(`${Environment.account_service_url}/account/`);
      if (response.ok) {
        const data = await response.json();
        setAccounts(data);
      }
    } catch (err) {
      console.error('Error fetching accounts:', err);
    }
  }, []);

  const fetchAccountUsers = useCallback(async () => {
    try {
      const response = await fetch(`${Environment.account_service_url}/accountuser/`);
      if (response.ok) {
        const data = await response.json();
        setAccountUsers(data);
      }
    } catch (err) {
      console.error('Error fetching account users:', err);
    }
  }, []);

  useEffect(() => {
    fetchAccounts();
    fetchAccountUsers();
  }, [fetchAccounts, fetchAccountUsers]);

  useEffect(() => {
    if (selectedAccount) {
      setFilteredUsers(accountUsers.filter((u) => u.accountId === selectedAccount.id));
    } else {
      setFilteredUsers([]);
    }
  }, [selectedAccount, accountUsers]);

  const handleUpdate = useCallback(
    (account: AccountData) => {
      fetchAccounts();
      fetchAccountUsers();
      if (account?.id) {
        setSelectedAccount(account);
      }
    },
    [fetchAccounts, fetchAccountUsers]
  );

  const onSelectionChanged = useCallback(() => {
    if (gridApi) {
      const selectedRows = gridApi.getSelectedRows() as AccountData[];
      if (selectedRows.length > 0) {
        setSelectedAccount(selectedRows[0]);
      }
    }
  }, [gridApi]);

  const onGridReady = useCallback((params: GridReadyEvent) => {
    setGridApi(params.api);
  }, []);

  return (
    <div className="account-management">
      <div className="account-section">
        <EditAccount account={accountToBeUpdate} onUpdate={handleUpdate} />
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
          account={selectedAccount}
          onUpdate={handleUpdate}
        />
        <div className="user-grid-section">
          <h6>
            Users List{' '}
            {selectedAccount ? `(${selectedAccount.displayName})` : ''}
          </h6>
          <div className="ag-theme-alpine" style={{ width: 800, height: 300 }}>
            <AgGridReact columnDefs={userColumnDefs} rowData={filteredUsers} />
          </div>
        </div>
      </div>
    </div>
  );
};
