export interface Account {
  id: number;
  displayName: string;
}

export interface AccountUser {
  username: string;
  accountId: number;
}

export interface User {
  logonId: string;
  fullName: string;
  email: string;
  employeeId: string;
  department: string;
  photoUrl: string;
}
