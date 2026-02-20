import React, { useCallback, useEffect, useState } from 'react';
import { TextField, Button, Alert } from '@mui/material';
import { Environment } from '../env';
import { AccountData } from '../AccountsDropdown';

interface EditAccountProps {
  account: AccountData | undefined;
  onUpdate: (account: AccountData) => void;
}

interface AccountResponse {
  success: boolean;
  msg: string;
}

export const EditAccount: React.FC<EditAccountProps> = ({ account, onUpdate }) => {
  const [displayName, setDisplayName] = useState('');
  const [accountResponse, setAccountResponse] = useState<AccountResponse | null>(null);

  useEffect(() => {
    if (account?.displayName) {
      setDisplayName(account.displayName);
    }
  }, [account]);

  const handleReset = useCallback(() => {
    setDisplayName('');
  }, []);

  const handleAdd = useCallback(async () => {
    if (!displayName.trim()) {
      return;
    }
    const payload = account
      ? { ...account, displayName }
      : { displayName };

    try {
      const response = await fetch(`${Environment.account_service_url}/account/`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
      });
      if (response.ok) {
        const savedAccount = await response.json();
        setAccountResponse({
          success: true,
          msg: `Account ${account ? 'updated' : 'added'} successfully!`,
        });
        onUpdate(savedAccount);
        handleReset();
      } else {
        setAccountResponse({ success: false, msg: 'There is some error!' });
      }
    } catch (err) {
      console.error(err);
      setAccountResponse({ success: false, msg: 'There is some error!' });
    }
  }, [displayName, account, onUpdate, handleReset]);

  useEffect(() => {
    if (accountResponse) {
      const timer = setTimeout(() => setAccountResponse(null), 2000);
      return () => clearTimeout(timer);
    }
  }, [accountResponse]);

  return (
    <div>
      <div style={{ display: 'flex', flexDirection: 'row', alignItems: 'center', gap: '16px' }}>
        <TextField
          id="new-account"
          placeholder="Account name"
          value={displayName}
          onChange={(e) => setDisplayName(e.target.value)}
          size="small"
          style={{ width: 300 }}
          required
        />
        <Button variant="contained" size="small" onClick={handleAdd}>
          {account ? 'Update' : 'Add'} Account
        </Button>
        <Button variant="outlined" size="small" onClick={handleReset}>
          Reset
        </Button>
      </div>
      {accountResponse && (
        <Alert
          severity={accountResponse.success ? 'success' : 'error'}
          onClose={() => setAccountResponse(null)}
          style={{ marginTop: 8, maxWidth: 500 }}
        >
          {accountResponse.msg}
        </Alert>
      )}
    </div>
  );
};
