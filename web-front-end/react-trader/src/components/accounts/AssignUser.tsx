import { useEffect, useState } from 'react';
import type { Account } from '../../models/account.model';
import type { User } from '../../models/user.model';
import { addAccountUser } from '../../services/account.service';
import { getUsers } from '../../services/user.service';
import { TimedAlert } from './TimedAlert';

interface AssignUserProps {
    accounts: Account[];
    account?: Account;
    onUpdate: (account: Account) => void;
}

interface AddUserResponse {
    success?: boolean;
    error?: boolean;
    msg: string;
}

export function AssignUser({ accounts, account, onUpdate }: AssignUserProps) {
    const [selectedAccount, setSelectedAccount] = useState<Account | undefined>(account);
    const [user, setUser] = useState<User | undefined>(undefined);
    const [search, setSearch] = useState('');
    const [suggestions, setSuggestions] = useState<User[]>([]);
    const [showSuggestions, setShowSuggestions] = useState(false);
    const [addUserResponse, setAddUserResponse] = useState<AddUserResponse | undefined>(undefined);

    useEffect(() => {
        setSelectedAccount(account);
    }, [account]);

    useEffect(() => {
        if (!search || search.length <= 2) {
            setSuggestions([]);
            return;
        }
        let cancelled = false;
        const timer = setTimeout(async () => {
            try {
                const users = await getUsers(search);
                if (!cancelled) {
                    setSuggestions(users || []);
                    setShowSuggestions(true);
                }
            } catch (err) {
                console.log((err as Error)?.message || 'Something goes wrong');
                if (!cancelled) {
                    setSuggestions([]);
                }
            }
        }, 100);
        return () => {
            cancelled = true;
            clearTimeout(timer);
        };
    }, [search]);

    const add = async () => {
        if (!user || !selectedAccount) {
            return;
        }
        const accountUser = { username: user.logonId, accountId: selectedAccount.id };
        try {
            await addAccountUser(accountUser);
            setAddUserResponse({ success: true, msg: 'User added successfully!' });
            onUpdate(selectedAccount);
            reset(true);
        } catch (err) {
            setAddUserResponse({ error: true, msg: 'There is some error!' });
            console.error(err);
        }
    };

    const onSelect = (selected: User) => {
        setUser(selected);
        setSearch(selected.fullName);
        setShowSuggestions(false);
    };

    const reset = (fromAdd = false) => {
        setUser(undefined);
        setSearch('');
        setSuggestions([]);
        setShowSuggestions(false);
        // keep account sticky if we are just adding a user
        if (!fromAdd) {
            setSelectedAccount(undefined);
        }
    };

    return (
        <>
            <form className="d-flex flex-row">
                <div className="position-relative me-4" style={{ width: 300 }}>
                    <input
                        id="account-user"
                        name="account-user"
                        value={search}
                        onChange={(e) => {
                            setSearch(e.target.value);
                            setUser(undefined);
                        }}
                        aria-label="Add User to Account"
                        placeholder="Add User to Account"
                        className="form-control"
                        autoComplete="off"
                    />
                    {showSuggestions && suggestions.length > 0 && (
                        <ul className="dropdown-menu show w-100" role="listbox">
                            {suggestions.map((suggestion) => (
                                <li key={suggestion.logonId} role="option" aria-selected={false}>
                                    <button
                                        type="button"
                                        className="dropdown-item"
                                        onClick={() => onSelect(suggestion)}
                                    >
                                        {suggestion.fullName}
                                    </button>
                                </li>
                            ))}
                        </ul>
                    )}
                </div>
                <select
                    id="account-name"
                    name="account"
                    className="form-select d-inline-flex me-4 w-auto"
                    style={{ height: 38 }}
                    aria-label="Select an account"
                    value={selectedAccount?.id ?? ''}
                    onChange={(e) => {
                        const id = Number(e.target.value);
                        setSelectedAccount(accounts.find((a) => a.id === id));
                    }}
                >
                    <option value="" disabled>
                        Select an account
                    </option>
                    {accounts.map((a) => (
                        <option key={a.id} value={a.id}>
                            {a.displayName}
                        </option>
                    ))}
                </select>
                <button type="button" className="btn btn-sm btn-primary me-4" onClick={add}>
                    Add User
                </button>
                <button type="button" className="btn btn-sm btn-secondary" onClick={() => reset()}>
                    Reset
                </button>
            </form>
            {addUserResponse && (
                <TimedAlert
                    type={addUserResponse.success ? 'success' : 'danger'}
                    onClosed={() => setAddUserResponse(undefined)}
                >
                    {addUserResponse.msg}
                </TimedAlert>
            )}
        </>
    );
}
