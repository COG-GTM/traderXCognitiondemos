import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { AccountService } from './account.service';
import { Account } from '../model/account.model';
import { AccountUser } from '../model/user.model';

describe('AccountService', () => {
  let service: AccountService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [AccountService]
    });
    service = TestBed.inject(AccountService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should fetch accounts via GET', () => {
    const mockAccounts: Account[] = [
      { id: 1, displayName: 'Test Account 1' },
      { id: 2, displayName: 'Test Account 2' }
    ];

    service.getAccounts().subscribe(accounts => {
      expect(accounts.length).toBe(2);
      expect(accounts).toEqual(mockAccounts);
    });

    const req = httpMock.expectOne(req => req.url.includes('/account/'));
    expect(req.request.method).toBe('GET');
    req.flush(mockAccounts);
  });

  it('should add an account via POST', () => {
    const newAccount: Partial<Account> = { displayName: 'New Account' };
    const mockResponse: Account = { id: 3, displayName: 'New Account' };

    service.addAccount(newAccount).subscribe(response => {
      expect(response).toBeTruthy();
    });

    const req = httpMock.expectOne(req => req.url.includes('/account/'));
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(newAccount);
    req.flush(mockResponse);
  });

  it('should add an account user via POST', () => {
    const accountUser: AccountUser = { username: 'testuser', accountId: 1 };

    service.addAccountUser(accountUser).subscribe(response => {
      expect(response).toBeTruthy();
    });

    const req = httpMock.expectOne(req => req.url.includes('/accountuser/'));
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(accountUser);
    req.flush(accountUser);
  });

  it('should fetch account users via GET', () => {
    const mockUsers: AccountUser[] = [
      { username: 'user1', accountId: 1 },
      { username: 'user2', accountId: 2 }
    ];

    service.getAccountUsers().subscribe(users => {
      expect(users.length).toBe(2);
      expect(users).toEqual(mockUsers);
    });

    const req = httpMock.expectOne(req => req.url.includes('/accountuser/'));
    expect(req.request.method).toBe('GET');
    req.flush(mockUsers);
  });

  it('should handle error on getAccounts', () => {
    service.getAccounts().subscribe({
      error: (error) => {
        expect(error.status).toBe(500);
      }
    });

    const req = httpMock.expectOne(req => req.url.includes('/account/'));
    req.flush('Server Error', { status: 500, statusText: 'Internal Server Error' });
    // Retry 1
    const req2 = httpMock.expectOne(req => req.url.includes('/account/'));
    req2.flush('Server Error', { status: 500, statusText: 'Internal Server Error' });
    // Retry 2
    const req3 = httpMock.expectOne(req => req.url.includes('/account/'));
    req3.flush('Server Error', { status: 500, statusText: 'Internal Server Error' });
  });
});
