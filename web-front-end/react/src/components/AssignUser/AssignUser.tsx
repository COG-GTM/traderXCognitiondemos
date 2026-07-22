import { useEffect, useMemo, useRef, useState } from 'react';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Alert from '@mui/material/Alert';
import Autocomplete from '@mui/material/Autocomplete';
import TextField from '@mui/material/TextField';
import CircularProgress from '@mui/material/CircularProgress';
import { Dropdown } from '../Dropdown';
import { addAccountUser, getMatchingPeople } from './api';
import { Account, AddUserResponse, AssignUserProps, User } from './types';

const SEARCH_DEBOUNCE_MS = 300;
const MIN_SEARCH_LENGTH = 3;

/**
 * React port of the Angular assign-user-to-account form
 * (`assign-user.component.ts` / `.html`).
 *
 * A people typeahead: typing > 2 chars queries the people service
 * (debounced); selecting a user and clicking "Add User" POSTs
 * `{ username, accountId }` to the account service, then shows a
 * success/error alert that auto-dismisses.
 */
export function AssignUser({ account, accounts, onUpdate }: AssignUserProps) {
	const [search, setSearch] = useState('');
	const [options, setOptions] = useState<User[]>([]);
	const [loading, setLoading] = useState(false);
	const [user, setUser] = useState<User | null>(null);
	const [selectedAccount, setSelectedAccount] = useState<Account | undefined>(account);
	const [response, setResponse] = useState<AddUserResponse | undefined>(undefined);

	const dismissTimer = useRef<ReturnType<typeof setTimeout>>();

	// keep local account selection in sync when parent changes it
	useEffect(() => {
		setSelectedAccount(account);
	}, [account]);

	// debounced people typeahead (Angular used switchMap on the search stream)
	useEffect(() => {
		const query = search.trim();
		if (query.length <= MIN_SEARCH_LENGTH - 1) {
			setOptions([]);
			setLoading(false);
			return;
		}
		setLoading(true);
		const handle = setTimeout(async () => {
			const people = await getMatchingPeople(query);
			setOptions(people);
			setLoading(false);
		}, SEARCH_DEBOUNCE_MS);
		return () => clearTimeout(handle);
	}, [search]);

	// auto-dismiss the alert after 2s, mirroring [dismissOnTimeout]="2000"
	useEffect(() => {
		if (!response) {
			return;
		}
		dismissTimer.current = setTimeout(() => setResponse(undefined), 2000);
		return () => clearTimeout(dismissTimer.current);
	}, [response]);

	const comparator = useMemo(
		() => (src: Account | undefined, target: Account) => src?.id === target.id,
		[]
	);

	const add = async () => {
		if (!user || !selectedAccount) {
			return;
		}
		try {
			await addAccountUser({ username: user.logonId, accountId: selectedAccount.id });
			setResponse({ success: true, msg: 'User added successfully!' });
			onUpdate?.(selectedAccount);
			reset(true);
		} catch (err) {
			setResponse({ error: true, msg: 'There is some error!' });
			console.error(err);
		}
	};

	const reset = (fromAdd = false) => {
		setUser(null);
		setSearch('');
		setOptions([]);
		// keep account sticky if we are just adding a user
		if (!fromAdd) {
			setSelectedAccount(undefined);
		}
	};

	return (
		<Box>
			<Box sx={{ display: 'flex', flexDirection: 'row', alignItems: 'center', gap: 2 }}>
				<Autocomplete<User>
					sx={{ width: 300 }}
					options={options}
					loading={loading}
					filterOptions={(x) => x}
					getOptionLabel={(option) => option.fullName}
					isOptionEqualToValue={(option, value) => option.logonId === value.logonId}
					value={user}
					onChange={(_event, value) => setUser(value)}
					onInputChange={(_event, value) => setSearch(value)}
					inputValue={search}
					renderInput={(params) => (
						<TextField
							{...params}
							size="small"
							label="Add User to Account"
							placeholder="Add User to Account"
							aria-label="Add User to Account"
							InputProps={{
								...params.InputProps,
								endAdornment: (
									<>
										{loading ? <CircularProgress color="inherit" size={16} /> : null}
										{params.InputProps.endAdornment}
									</>
								),
							}}
						/>
					)}
				/>
				<Dropdown<Account>
					items={accounts}
					itemKey="displayName"
					selectedItem={selectedAccount}
					placeholder="Select an account"
					selectionComparator={comparator}
					onSelectedItemChange={setSelectedAccount}
				/>
				<Button variant="contained" size="small" onClick={add}>
					Add User
				</Button>
				<Button variant="outlined" size="small" color="secondary" onClick={() => reset()}>
					Reset
				</Button>
			</Box>
			{response && (
				<Alert
					sx={{ mt: 2 }}
					severity={response.success ? 'success' : 'error'}
					onClose={() => setResponse(undefined)}
				>
					{response.msg}
				</Alert>
			)}
		</Box>
	);
}
