// Account List grid (with a per-row "Update" button) + Users List grid.
// Ported from Angular accounts/account.component.ts/.html and
// accounts/button-renderer.component.ts.

import { useCallback, useEffect, useState } from 'react';
import { AgGridReact } from 'ag-grid-react';
import { Button } from '@mui/material';
import { ColDef, ICellRendererParams, SelectionChangedEvent } from 'ag-grid-community';

import 'ag-grid-community/styles/ag-grid.css';
import 'ag-grid-community/styles/ag-theme-alpine.css';

import { getAccounts, getAccountUsers } from './api';
import { Account, AccountUser } from './types';

// React cell renderer for the "Update" button (mirrors ButtonCellRendererComponent).
type ButtonCellParams = ICellRendererParams<Account> & { clicked: (account: Account) => void };

const UpdateButtonRenderer = (params: ButtonCellParams) => {
	const onClick = () => {
		if (params.data) {
			params.clicked(params.data);
		}
	};
	return (
		<Button variant="contained" color="info" size="small" onClick={onClick}>
			Update
		</Button>
	);
};

export interface AccountListProps {
	// Currently selected account (drives the Users List filter + header).
	selectedAccount?: Account;
	// Fired when the row-level "Update" button is clicked (prefills edit form).
	onAccountToUpdate: (account: Account) => void;
	// Fired when a grid row is selected.
	onSelectAccount: (account: Account) => void;
	// Bump to force a reload of the accounts + users grids (e.g. after add/update).
	refreshTrigger?: number;
}

export const AccountList = ({
	selectedAccount,
	onAccountToUpdate,
	onSelectAccount,
	refreshTrigger = 0,
}: AccountListProps) => {
	const [accounts, setAccounts] = useState<Account[]>([]);
	const [users, setUsers] = useState<AccountUser[]>([]);

	useEffect(() => {
		let cancelled = false;
		getAccounts()
			.then((data) => {
				if (!cancelled) {
					setAccounts(data);
				}
			})
			.catch((err) => console.error(err));
		return () => {
			cancelled = true;
		};
	}, [refreshTrigger]);

	useEffect(() => {
		let cancelled = false;
		const accountId = selectedAccount?.id;
		getAccountUsers()
			.then((data) => {
				if (!cancelled) {
					setUsers(accountId === undefined ? [] : data.filter((u) => u.accountId === accountId));
				}
			})
			.catch((err) => console.error(err));
		return () => {
			cancelled = true;
		};
	}, [refreshTrigger, selectedAccount]);

	const columnDefs: ColDef<Account>[] = [
		{ field: 'id', flex: 1 },
		{ field: 'displayName', flex: 2 },
		{
			headerName: 'Update',
			cellRenderer: UpdateButtonRenderer,
			cellRendererParams: {
				clicked: (account: Account) => onAccountToUpdate(account),
			},
			flex: 1,
		},
	];

	const columnDefsUser: ColDef<AccountUser>[] = [
		{ field: 'accountId', flex: 1 },
		{ field: 'username', flex: 1 },
	];

	const onSelectionChanged = useCallback(
		(event: SelectionChangedEvent<Account>) => {
			const selected = event.api.getSelectedRows();
			if (selected.length > 0) {
				onSelectAccount(selected[0]);
			}
		},
		[onSelectAccount],
	);

	return (
		<>
			<div className="mt-4">
				<h6>Account List</h6>
				<div
					className="ag-theme-alpine"
					id="accountgrid"
					style={{ width: 800, height: 300 }}
				>
					<AgGridReact<Account>
						rowData={accounts}
						columnDefs={columnDefs}
						rowSelection="single"
						onSelectionChanged={onSelectionChanged}
					/>
				</div>
			</div>

			<div className="mt-4">
				<h6>Users List {selectedAccount ? `(${selectedAccount.displayName})` : ''}</h6>
				<div
					className="ag-theme-alpine"
					id="usergrid"
					style={{ width: 800, height: 300 }}
				>
					<AgGridReact<AccountUser> rowData={users} columnDefs={columnDefsUser} />
				</div>
			</div>
		</>
	);
};
