// Accounts management page. Composes the edit form + account/users grids.
// Ported from Angular accounts/account.component.html.
//
// NOTE: the Angular page also renders <app-assign-user> between the edit form
// and the Users List. That is piece 10 (AssignUser) and is intentionally NOT
// imported here to keep this piece self-contained. A placeholder slot is left
// below; integration should mount the AssignUser component there and pass it
// `account={selectedAccount}` plus the accounts list.

import { ReactNode, useState } from 'react';
import { Box } from '@mui/material';
import { EditAccount } from './EditAccount';
import { AccountList } from './AccountList';
import { Account } from './types';

export interface AccountsPageProps {
	// Optional slot where the AssignUser piece should be mounted during integration.
	assignUserSlot?: ReactNode;
}

export const AccountsPage = ({ assignUserSlot }: AccountsPageProps) => {
	// Account chosen for editing (prefills the edit form) — Angular's accountToBeUpdate.
	const [accountToBeUpdate, setAccountToBeUpdate] = useState<Account | undefined>(undefined);
	// Currently selected account — Angular's selectedAccount.
	const [selectedAccount, setSelectedAccount] = useState<Account | undefined>(undefined);
	// Refresh counter — mirrors Angular's accountBehaviorSubject driving reloads.
	const [refreshTrigger, setRefreshTrigger] = useState<number>(0);

	// Fired after a successful add/update in the edit form.
	const handleUpdate = (account: Account) => {
		setSelectedAccount(account);
		setRefreshTrigger((n) => n + 1);
	};

	// Fired when a grid row is selected.
	const handleSelect = (account: Account) => {
		setSelectedAccount(account);
		setRefreshTrigger((n) => n + 1);
	};

	return (
		<Box className="accounts-page" sx={{ p: 5, pt: 3 }}>
			<Box sx={{ display: 'flex', flexDirection: 'column' }}>
				<EditAccount account={accountToBeUpdate} onUpdate={handleUpdate} />
				<AccountList
					selectedAccount={selectedAccount}
					onAccountToUpdate={setAccountToBeUpdate}
					onSelectAccount={handleSelect}
					refreshTrigger={refreshTrigger}
				/>
			</Box>

			{/* Integration slot for the AssignUser piece (10/10). */}
			{assignUserSlot}
		</Box>
	);
};
