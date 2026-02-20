import { useCallback, useEffect, useState } from "react";
import { Environment } from '../env';

export interface AccountUser {
  username: string;
  accountId: number;
}

export const GetAccountUsers = (accountId: number) => {
  const [accountUsers, setAccountUsers] = useState<AccountUser[]>([]);

  const fetchAccountUsers = useCallback(async () => {
    try {
      const response = await fetch(`${Environment.account_service_url}/accountuser/`);
      if (response.ok) {
        const allUsers: AccountUser[] = await response.json();
        if (accountId > 0) {
          setAccountUsers(allUsers.filter((u) => u.accountId === accountId));
        } else {
          setAccountUsers([]);
        }
      }
    } catch (error) {
      console.error(error);
    }
  }, [accountId]);

  useEffect(() => {
    fetchAccountUsers();
  }, [fetchAccountUsers]);

  return { accountUsers, refetchAccountUsers: fetchAccountUsers };
};
