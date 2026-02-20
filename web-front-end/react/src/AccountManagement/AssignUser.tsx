import React, { useCallback, useEffect, useState } from 'react';
import {
  Button,
  TextField,
  Alert,
  Autocomplete,
  MenuItem,
  Select,
  InputLabel,
  FormControl,
} from '@mui/material';
import { SelectChangeEvent } from '@mui/material';
import { Environment } from '../env';
import { Account, User } from './types';

interface AssignUserProps {
  accounts: Account[];
  selectedAccount: Account | null;
  onUpdate: (account: Account) => void;
}

export const AssignUser: React.FC<AssignUserProps> = ({ accounts, selectedAccount, onUpdate }) => {
  const [search, setSearch] = useState('');
  const [users, setUsers] = useState<User[]>([]);
  const [selectedUser, setSelectedUser] = useState<User | null>(null);
  const [accountId, setAccountId] = useState<number>(selectedAccount?.id ?? 0);
  const [response, setResponse] = useState<{ success: boolean; msg: string } | null>(null);

  useEffect(() => {
    if (selectedAccount) {
      setAccountId(selectedAccount.id);
    }
  }, [selectedAccount]);

  const searchUsers = useCallback(async (query: string) => {
    if (query.length > 2) {
      try {
        const res = await fetch(
          `${Environment.people_service_url}/People/GetMatchingPeople?SearchText=${encodeURIComponent(query)}&Take=10`
        );
        if (res.ok) {
          const data = await res.json();
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

  useEffect(() => {
    const timer = setTimeout(() => {
      searchUsers(search);
    }, 300);
    return () => clearTimeout(timer);
  }, [search, searchUsers]);

  const handleAdd = async () => {
    if (!selectedUser || !accountId) {
      return;
    }
    const accountUser = { username: selectedUser.logonId, accountId };
    try {
      const res = await fetch(`${Environment.account_service_url}/accountuser/`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(accountUser),
      });
      if (res.ok) {
        setResponse({ success: true, msg: 'User added successfully!' });
        const account = accounts.find((a) => a.id === accountId);
        if (account) {
          onUpdate(account);
        }
        setSelectedUser(null);
        setSearch('');
      } else {
        setResponse({ success: false, msg: 'There is some error!' });
      }
    } catch (err) {
      console.error(err);
      setResponse({ success: false, msg: 'There is some error!' });
    }
  };

  const handleReset = () => {
    setSelectedUser(null);
    setSearch('');
    setAccountId(0);
  };

  const handleAccountChange = (event: SelectChangeEvent<number>) => {
    setAccountId(event.target.value as number);
  };

  useEffect(() => {
    if (response) {
      const timer = setTimeout(() => setResponse(null), 2000);
      return () => clearTimeout(timer);
    }
  }, [response]);

  return (
    <div>
      <div style={{ display: 'flex', flexDirection: 'row', alignItems: 'center', gap: '16px' }}>
        <Autocomplete
          freeSolo
          options={users}
          getOptionLabel={(option) => (typeof option === 'string' ? option : option.fullName)}
          inputValue={search}
          onInputChange={(_e, value) => setSearch(value)}
          onChange={(_e, value) => {
            if (value && typeof value !== 'string') {
              setSelectedUser(value);
            }
          }}
          renderInput={(params) => (
            <TextField {...params} label="Add User to Account" size="small" sx={{ width: 300 }} />
          )}
          sx={{ width: 300 }}
        />
        <FormControl size="small" sx={{ minWidth: 200 }}>
          <InputLabel>Select an account</InputLabel>
          <Select
            value={accountId || ''}
            label="Select an account"
            onChange={handleAccountChange}
          >
            {accounts.map((account) => (
              <MenuItem key={account.id} value={account.id}>
                {account.displayName}
              </MenuItem>
            ))}
          </Select>
        </FormControl>
        <Button variant="contained" size="small" onClick={handleAdd}>
          Add User
        </Button>
        <Button variant="outlined" size="small" onClick={handleReset}>
          Reset
        </Button>
      </div>
      {response && (
        <Alert
          severity={response.success ? 'success' : 'error'}
          onClose={() => setResponse(null)}
          sx={{ mt: 1, maxWidth: 400 }}
        >
          {response.msg}
        </Alert>
      )}
    </div>
  );
};
