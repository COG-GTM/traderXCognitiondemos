import React, { useEffect, useRef, useState } from 'react';
import { Alert, Button } from 'react-bootstrap';
import { Account } from '../../../models';
import { accountService } from '../../../services/accountService';

// SECTION 8 — Add / edit account form.
export interface EditAccountProps {
  account?: Account;
  onUpdate: (account: Account) => void;
}

interface AccountResponse {
  success: boolean;
  msg: string;
}

export const EditAccount = ({ account, onUpdate }: EditAccountProps) => {
  const [editedAccount, setEditedAccount] = useState<Account | undefined>(account);
  const [displayName, setDisplayName] = useState<string>('');
  const [accountResponse, setAccountResponse] = useState<AccountResponse | undefined>(undefined);
  const timeoutRef = useRef<ReturnType<typeof setTimeout>>();

  useEffect(() => {
    setEditedAccount(account);
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
    setEditedAccount(undefined);
    setDisplayName('');
  };

  const add = () => {
    if (!displayName.trim()) {
      return;
    }
    const theAccount = { ...(editedAccount ?? {}), displayName } as Account;
    accountService.addAccount(theAccount).then(
      () => {
        showAlert({
          success: true,
          msg: `Account ${theAccount.id ? 'updated' : 'added'} successfully!`,
        });
        onUpdate(theAccount);
        reset();
      },
      (err) => {
        console.error(err);
        showAlert({ success: false, msg: 'There is some error!' });
      }
    );
  };

  return (
    <>
      <form className="d-flex flex-row">
        <input
          id="new-account"
          name="accountname"
          placeholder="Account name"
          value={displayName}
          onChange={(e) => setDisplayName(e.target.value)}
          className="form-control me-4"
          type="text"
          required
          style={{ width: '300px' }}
        />
        <Button
          type="button"
          size="sm"
          variant="primary"
          className="me-3 account-btn"
          onClick={add}
        >
          {editedAccount ? 'Update' : 'Add'} Account
        </Button>
        <Button type="button" size="sm" variant="secondary" className="me-4" onClick={reset}>
          Reset
        </Button>
      </form>
      {accountResponse && (
        <Alert
          variant={accountResponse.success ? 'success' : 'danger'}
          dismissible
          onClose={() => setAccountResponse(undefined)}
        >
          {accountResponse.msg}
        </Alert>
      )}
    </>
  );
};
