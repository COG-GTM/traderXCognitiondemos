import React, { useCallback, useEffect, useState } from 'react';
import {
  TextField,
  Button,
  Alert,
  Autocomplete,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
} from '@mui/material';
import { SelectChangeEvent } from '@mui/material';
import { Environment } from '../env';
import { AccountData } from '../AccountsDropdown';

interface User {
  logonId: string;
  fullName: string;
  email: string;
  employeeId: string;
  department: string;
  photoUrl: string;
}

interface AssignUserProps {
  accounts: AccountData[];
  account: AccountData | undefined;
  onUpdate: (account: AccountData) => void;
}

interface UserResponse {
  success: boolean;
  msg: string;
}

export const AssignUser: React.FC<AssignUserProps> = ({ accounts, account, onUpdate }) => {
  const [selectedAccount, setSelectedAccount] = useState<AccountData | undefined>(account);
  const [searchText, setSearchText] = useState('');
  const [users, setUsers] = useState<User[]>([]);
  const [selectedUser, setSelectedUser] = useState<User | null>(null);
  const [addUserResponse, setAddUserResponse] = useState<UserResponse | null>(null);

  useEffect(() => {
    setSelectedAccount(account);
  }, [account]);

  const handleSearch = useCallback(async (query: string) => {
    if (query && query.length > 2) {
      try {
        const response = await fetch(
          `${Environment.people_service_url}/People/GetMatchingPeople?SearchText=${encodeURIComponent(query)}&Take=10`
        );
        if (response.ok) {
          const data = await response.json();
          setUsers(data.people || data || []);
        }
      } catch (err) {
        console.error(err);
        setUsers([]);
      }
    } else {
      setUsers([]);
    }
  }, []);

  const handleAddUser = useCallback(async () => {
    if (!selectedUser || !selectedAccount) {
      return;
    }
    const accountUser = { username: selectedUser.logonId, accountId: selectedAccount.id };
    try {
      const response = await fetch(`${Environment.account_service_url}/accountuser/`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(accountUser),
      });
      if (response.ok) {
        setAddUserResponse({ success: true, msg: 'User added successfully!' });
        onUpdate(selectedAccount);
        setSelectedUser(null);
        setSearchText('');
      } else {
        setAddUserResponse({ success: false, msg: 'There is some error!' });
      }
    } catch (err) {
      console.error(err);
      setAddUserResponse({ success: false, msg: 'There is some error!' });
    }
  }, [selectedUser, selectedAccount, onUpdate]);

  const handleReset = useCallback(() => {
    setSelectedUser(null);
    setSearchText('');
    setSelectedAccount(undefined);
  }, []);

  const handleAccountChange = useCallback(
    (event: SelectChangeEvent<number>) => {
      const acct = accounts.find((a) => a.id === event.target.value);
      setSelectedAccount(acct);
    },
    [accounts]
  );

  useEffect(() => {
    if (addUserResponse) {
      const timer = setTimeout(() => setAddUserResponse(null), 2000);
      return () => clearTimeout(timer);
    }
  }, [addUserResponse]);

  return (
    <div>
      <div style={{ display: 'flex', flexDirection: 'row', alignItems: 'center', gap: '16px' }}>
        <Autocomplete
          freeSolo
          options={users}
          getOptionLabel={(option) =>
            typeof option === 'string' ? option : option.fullName
          }
          inputValue={searchText}
          onInputChange={(_e, value) => {
            setSearchText(value);
            handleSearch(value);
          }}
          onChange={(_e, value) => {
            if (value && typeof value !== 'string') {
              setSelectedUser(value);
            }
          }}
          renderInput={(params) => (
            <TextField
              {...params}
              placeholder="Add User to Account"
              size="small"
              style={{ width: 300 }}
            />
          )}
          style={{ width: 300 }}
        />
        <FormControl size="small" style={{ minWidth: 200 }}>
          <InputLabel>Select an account</InputLabel>
          <Select
            value={selectedAccount?.id || ''}
            label="Select an account"
            onChange={handleAccountChange as (event: SelectChangeEvent<unknown>) => void}
          >
            {accounts.map((acct) => (
              <MenuItem key={acct.id} value={acct.id}>
                {acct.displayName}
              </MenuItem>
            ))}
          </Select>
        </FormControl>
        <Button variant="contained" size="small" onClick={handleAddUser}>
          Add User
        </Button>
        <Button variant="outlined" size="small" onClick={handleReset}>
          Reset
        </Button>
      </div>
      {addUserResponse && (
        <Alert
          severity={addUserResponse.success ? 'success' : 'error'}
          onClose={() => setAddUserResponse(null)}
          style={{ marginTop: 8, maxWidth: 500 }}
        >
          {addUserResponse.msg}
        </Alert>
      )}
    </div>
  );
};
