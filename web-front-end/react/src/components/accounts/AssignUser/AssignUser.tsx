import React, { useEffect, useState } from 'react';
import { Alert, Button } from 'react-bootstrap';
import { Account, User } from '../../../models';
import { accountService } from '../../../services/accountService';
import { userService } from '../../../services/userService';
import { Dropdown } from '../../Dropdown';

// SECTION 9 — Assign a user to an account.
export interface AssignUserProps {
  account?: Account;
  accounts: Account[];
  onUpdate: (account: Account) => void;
}

interface AddUserResponse {
  type: 'success' | 'danger';
  msg: string;
}

const SEARCH_DEBOUNCE_MS = 300;
const MIN_QUERY_LENGTH = 2;

export const AssignUser = ({ account, accounts, onUpdate }: AssignUserProps) => {
  const [search, setSearch] = useState('');
  const [user, setUser] = useState<User | undefined>(undefined);
  const [selectedAccount, setSelectedAccount] = useState<Account | undefined>(account);
  const [matches, setMatches] = useState<User[]>([]);
  const [showMatches, setShowMatches] = useState(false);
  const [response, setResponse] = useState<AddUserResponse | undefined>(undefined);

  useEffect(() => {
    setSelectedAccount(account);
  }, [account]);

  useEffect(() => {
    const query = search.trim();
    if (query.length <= MIN_QUERY_LENGTH) {
      setMatches([]);
      return;
    }
    // The user just picked this exact person; don't re-query for it.
    if (user && user.fullName === query) {
      return;
    }
    let cancelled = false;
    const handle = setTimeout(() => {
      userService
        .getUsers(query)
        .then((data) => {
          if (cancelled) {
            return;
          }
          setMatches(data || []);
          setShowMatches(true);
        })
        .catch((err) => {
          if (cancelled) {
            return;
          }
          console.log((err && err.message) || 'Something goes wrong');
          setMatches([]);
        });
    }, SEARCH_DEBOUNCE_MS);
    return () => {
      cancelled = true;
      clearTimeout(handle);
    };
  }, [search, user]);

  const onSelectUser = (match: User) => {
    setUser(match);
    setSearch(match.fullName);
    setShowMatches(false);
  };

  const add = () => {
    if (!user || !selectedAccount) {
      return;
    }
    const account = selectedAccount;
    accountService
      .addAccountUser({ username: user.logonId, accountId: account.id })
      .then(() => {
        setResponse({ type: 'success', msg: 'User added successfully!' });
        onUpdate(account);
        // keep the account sticky, clear the picked user/search
        setUser(undefined);
        setSearch('');
        setMatches([]);
        setShowMatches(false);
      })
      .catch((err) => {
        setResponse({ type: 'danger', msg: 'There is some error!' });
        console.error(err);
      });
  };

  const reset = () => {
    setUser(undefined);
    setSearch('');
    setMatches([]);
    setShowMatches(false);
    setSelectedAccount(undefined);
  };

  return (
    <>
      <form className="d-flex flex-row" onSubmit={(e) => e.preventDefault()}>
        <div className="me-4 position-relative" style={{ width: 300 }}>
          <input
            id="account-user"
            name="account-user"
            className="form-control"
            autoComplete="off"
            aria-label="Add User to Account"
            placeholder="Add User to Account"
            value={search}
            onChange={(e) => {
              setSearch(e.target.value);
              setUser(undefined);
            }}
            onFocus={() => {
              if (matches.length > 0) {
                setShowMatches(true);
              }
            }}
          />
          {showMatches && matches.length > 0 && (
            <ul
              className="dropdown-menu show w-100"
              style={{ maxHeight: 240, overflowY: 'auto' }}
              role="listbox"
            >
              {matches.map((match) => (
                <li key={match.logonId}>
                  <button
                    type="button"
                    role="option"
                    aria-selected={user?.logonId === match.logonId}
                    className="dropdown-item"
                    onClick={() => onSelectUser(match)}
                  >
                    {match.fullName}
                  </button>
                </li>
              ))}
            </ul>
          )}
        </div>
        <Dropdown<Account>
          items={accounts}
          itemKey="displayName"
          placeholder="Select an account"
          selectedItem={selectedAccount}
          selectionComparator={(a, b) => a?.id === b?.id}
          onSelect={setSelectedAccount}
        />
        <Button
          type="button"
          variant="primary"
          size="sm"
          className="ms-4 me-4"
          onClick={add}
        >
          Add User
        </Button>
        <Button type="button" variant="secondary" size="sm" onClick={reset}>
          Reset
        </Button>
      </form>
      {response && (
        <Alert
          className="mt-3"
          variant={response.type}
          dismissible
          onClose={() => setResponse(undefined)}
        >
          {response.msg}
        </Alert>
      )}
    </>
  );
};
