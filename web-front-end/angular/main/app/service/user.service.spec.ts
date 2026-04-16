import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { UserService } from './user.service';
import { User } from '../model/user.model';

describe('UserService', () => {
  let service: UserService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [UserService]
    });
    service = TestBed.inject(UserService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should fetch users matching search text', () => {
    const mockUsers: User[] = [
      { logonId: 'jdoe', fullName: 'John Doe', email: 'john@test.com', employeeId: 'E001', department: 'IT', photoUrl: '' }
    ];

    service.getUsers('john').subscribe(users => {
      expect(users.length).toBe(1);
      expect(users[0].fullName).toBe('John Doe');
    });

    const req = httpMock.expectOne(req => req.url.includes('/People/GetMatchingPeople'));
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('SearchText')).toBe('john');
    expect(req.request.params.get('Take')).toBe('10');
    req.flush({ people: mockUsers });
  });

  it('should return empty array when people is null', () => {
    service.getUsers('test').subscribe(users => {
      expect(users).toEqual([]);
    });

    const req = httpMock.expectOne(req => req.url.includes('/People/GetMatchingPeople'));
    req.flush({ people: null });
  });

  it('should handle error on getUsers', () => {
    service.getUsers('test').subscribe({
      error: (error) => {
        expect(error.status).toBe(500);
      }
    });

    const req = httpMock.expectOne(req => req.url.includes('/People/GetMatchingPeople'));
    req.flush('Server Error', { status: 500, statusText: 'Internal Server Error' });
  });
});
