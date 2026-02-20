import React, { useEffect, useState } from 'react';
import { Button, TextField, Alert } from '@mui/material';
import { Environment } from '../env';
import { Account } from './types';

interface EditAccountProps {
  accountToEdit: Account | null;
  onUpdate: (account: Account) => void;
}

export const EditAccount: React.FC<EditAccountProps> = ({ accountToEdit, onUpdate }) => {
  const [displayName, setDisplayName] = useState('');
  const [response, setResponse] = useState<{ success: boolean; msg: string } | null>(null);

  useEffect(() => {
    if (accountToEdit) {
      setDisplayName(accountToEdit.displayName);
    }
  }, [accountToEdit]);

  const handleAdd = async () => {
    if (!displayName.trim()) {
      return;
    }
    const account: Partial<Account> = accountToEdit
      ? { ...accountToEdit, displayName }
      : { displayName };

    try {
      const res = await fetch(`${Environment.account_service_url}/account/`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(account),
      });
      if (res.ok) {
        const savedAccount = await res.json();
        setResponse({ success: true, msg: `Account ${accountToEdit ? 'updated' : 'added'} successfully!` });
        onUpdate(savedAccount);
        handleReset();
      } else {
        setResponse({ success: false, msg: 'There is some error!' });
      }
    } catch (err) {
      console.error(err);
      setResponse({ success: false, msg: 'There is some error!' });
    }
  };

  const handleReset = () => {
    setDisplayName('');
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
        <TextField
          id="new-account"
          label="Account name"
          placeholder="Account name"
          value={displayName}
          onChange={(e) => setDisplayName(e.target.value)}
          size="small"
          required
        />
        <Button variant="contained" size="small" onClick={handleAdd}>
          {accountToEdit ? 'Update' : 'Add'} Account
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
