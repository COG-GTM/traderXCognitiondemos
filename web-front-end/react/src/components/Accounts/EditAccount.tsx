// Add / edit account form. Ported from Angular
// accounts/edit/edit.component.ts + edit.component.html.

import { useEffect, useRef, useState } from 'react';
import { Alert, Box, Button, TextField } from '@mui/material';
import { addAccount } from './api';
import { Account, AccountResponse } from './types';

export interface EditAccountProps {
	// The account chosen for editing (via Update button). When undefined the
	// form is in "Add" mode.
	account?: Account;
	// Emitted after a successful add/update with the saved account.
	onUpdate: (account: Account) => void;
}

export const EditAccount = ({ account, onUpdate }: EditAccountProps) => {
	const [displayName, setDisplayName] = useState<string>('');
	const [accountResponse, setAccountResponse] = useState<AccountResponse | undefined>(undefined);
	const timeoutRef = useRef<ReturnType<typeof setTimeout> | undefined>(undefined);

	// Mirror Angular's @Input setter: when an account is selected for editing,
	// pre-fill its display name.
	useEffect(() => {
		if (account?.displayName) {
			setDisplayName(account.displayName);
		}
	}, [account]);

	useEffect(() => {
		return () => {
			if (timeoutRef.current) {
				clearTimeout(timeoutRef.current);
			}
		};
	}, []);

	const showAlert = (response: AccountResponse) => {
		setAccountResponse(response);
		if (timeoutRef.current) {
			clearTimeout(timeoutRef.current);
		}
		timeoutRef.current = setTimeout(() => setAccountResponse(undefined), 2000);
	};

	const reset = () => {
		setDisplayName('');
	};

	const add = async () => {
		if (!displayName.trim()) {
			return;
		}
		const payload: Partial<Account> = { ...(account ?? {}), displayName };
		try {
			await addAccount(payload);
			showAlert({ success: true, msg: `Account ${payload.id ? 'updated' : 'added'} successfully!` });
			onUpdate(payload as Account);
			reset();
		} catch (err) {
			console.error(err);
			showAlert({ success: false, msg: 'There is some error!' });
		}
	};

	return (
		<Box className="edit-account">
			<Box sx={{ display: 'flex', flexDirection: 'row', alignItems: 'center', gap: 2 }}>
				<TextField
					id="new-account"
					name="accountname"
					placeholder="Account name"
					size="small"
					required
					value={displayName}
					onChange={(e) => setDisplayName(e.target.value)}
				/>
				<Button variant="contained" size="small" onClick={add}>
					{account ? 'Update' : 'Add'} Account
				</Button>
				<Button variant="outlined" color="secondary" size="small" onClick={reset}>
					Reset
				</Button>
			</Box>
			{accountResponse && (
				<Alert
					sx={{ mt: 2 }}
					severity={accountResponse.success ? 'success' : 'error'}
					onClose={() => setAccountResponse(undefined)}
				>
					{accountResponse.msg}
				</Alert>
			)}
		</Box>
	);
};
