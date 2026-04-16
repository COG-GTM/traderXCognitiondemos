import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { AccountService } from 'main/app/service/account.service';
import { AccountComponent } from './account.component';
import { MockAccountService } from 'main/app/test-utils/mocks.service';
import { createAccount } from 'main/app/test-utils/utils';
import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';

describe('Account tests', () => {
  let comp: AccountComponent;
  let fixture: ComponentFixture<AccountComponent>;
  let element: HTMLElement;
  beforeEach(
    waitForAsync(() => {
      TestBed.configureTestingModule({
        declarations: [AccountComponent],
        providers: [
          {
            provide: AccountService,
            useClass: MockAccountService
          }
        ],
        schemas: [CUSTOM_ELEMENTS_SCHEMA]
      }).compileComponents();
    })
  );

  beforeEach(() => {
    fixture = TestBed.createComponent(AccountComponent);
    comp = fixture.componentInstance;
    element = fixture.debugElement.nativeElement;
  });

  it('should create the component', () => {
    expect(comp).toBeTruthy();
  });

  it('should have column definitions for the account grid', () => {
    expect(comp.columnDefs).toBeDefined();
    expect(comp.columnDefs.length).toBeGreaterThan(0);
  });

  it('should set up accounts$ observable on init', () => {
    expect(comp.accounts$).toBeUndefined();
    comp.ngOnInit();
    expect(comp.accounts$).toBeDefined();
  });

  it('should have selectedAccount undefined initially', () => {
    expect(comp.selectedAccount).toBeUndefined();
  });

  it('should set account on update callback', () => {
    expect(comp.selectedAccount).toBeUndefined();
    const account = createAccount();
    comp.onUpdate(account);
    expect(comp.selectedAccount).toBe(account);
  });

  it('should set gridApi when onGridReady is called', () => {
    const mockParams = { api: {} } as any;
    comp.onGridReady(mockParams);
    expect((comp as any).gridApi).toBe(mockParams.api);
  });
});
