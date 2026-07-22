import { useEffect, useState } from 'react';
import type { Account } from '../../models/account.model';
import { addAccount } from '../../services/account.service';
import { TimedAlert } from './TimedAlert';

interface EditAccountProps {
    account?: Account;
    onUpdate: (account: Account) => void;
}

interface AccountResponse {
    success: boolean;
    msg: string;
}

export function EditAccount({ account, onUpdate }: EditAccountProps) {
    const [currentAccount, setCurrentAccount] = useState<Account | undefined>(account);
    const [displayName, setDisplayName] = useState('');
    const [accountResponse, setAccountResponse] = useState<AccountResponse | undefined>(undefined);

    useEffect(() => {
        setCurrentAccount(account);
        if (account?.displayName) {
            setDisplayName(account.displayName);
        }
    }, [account]);

    const add = async () => {
        if (!displayName.trim()) {
            return;
        }
        const payload = Object.assign(currentAccount ? { ...currentAccount } : {}, { displayName }) as Account;
        try {
            await addAccount(payload);
            setAccountResponse({ success: true, msg: `Account ${payload.id ? 'updated' : 'added'} successfully!` });
            onUpdate(payload);
            reset();
        } catch (err) {
            console.error(err);
            setAccountResponse({ success: false, msg: 'There is some error!' });
        }
    };

    const reset = () => {
        setCurrentAccount(undefined);
        setDisplayName('');
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
                />
                <button type="button" className="btn btn-sm btn-primary me-3 account-btn" onClick={add}>
                    {currentAccount ? 'Update' : 'Add'} Account
                </button>
                <button type="button" className="btn btn-sm btn-secondary me-4" onClick={reset}>
                    Reset
                </button>
            </form>
            {accountResponse && (
                <TimedAlert
                    type={accountResponse.success ? 'success' : 'danger'}
                    onClosed={() => setAccountResponse(undefined)}
                >
                    {accountResponse.msg}
                </TimedAlert>
            )}
        </>
    );
}
