import { Account } from '../model/account.model';
import { User } from '../model/user.model';
import { faker } from '@faker-js/faker';
import { Stock } from '../model/symbol.model';
import { Trade, State, Side, Position } from '../model/trade.model';
import { ComponentFixture } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

export function sleep(delay = 0) {
  return new Promise((re) => setTimeout(re, delay));
}

/** Types a value into an input bound with ngModel and flushes change detection. */
export function setInputValue<T>(fixture: ComponentFixture<T>, selector: string, value: string) {
  const input = fixture.debugElement.query(By.css(selector)).nativeElement as HTMLInputElement;
  input.value = value;
  input.dispatchEvent(new Event('input'));
  fixture.detectChanges();
  return input;
}

/**
 * Runs change detection until ag-grid has finished its asynchronous
 * initialisation / rendering work, so the DOM and the grid api are usable.
 */
export async function settle<T>(fixture: ComponentFixture<T>, rounds = 3) {
  for (let round = 0; round < rounds; round++) {
    fixture.detectChanges();
    await fixture.whenStable();
    await new Promise((resolve) => setTimeout(resolve));
  }
  fixture.detectChanges();
}

/** Text of every rendered ag-grid body row, cell by cell. */
export function gridRowTexts<T>(fixture: ComponentFixture<T>): string[][] {
  const rows = Array.from(
    fixture.nativeElement.querySelectorAll('.ag-center-cols-container .ag-row')
  ) as HTMLElement[];
  return rows.map((row) => Array.from(row.querySelectorAll('.ag-cell')).map((cell) => (cell as HTMLElement).textContent ?? ''));
}

export function createUser(): User {
  return {
    fullName: faker.name.firstName(),
    email: faker.internet.email(),
    department: faker.commerce.department(),
    logonId: faker.random.alphaNumeric(5),
    employeeId: faker.random.alpha(5),
    photoUrl: 'testurl'
  };
}

export function createAccount(): Account {
  return {
    displayName: faker.company.name(),
    id: faker.datatype.number()
  };
}

export function createStock(): Stock {
  return {
    companyName: faker.company.name(),
    ticker: faker.random.alpha(4)
  };
}

export function createTrade(): Trade {
  return {
    created: faker.date.recent(),
    id: faker.random.alpha(5),
    state: State.Pending,
    side: Side.Buy,
    ...createPosition()
  };
}

export function createPosition(): Position {
  return {
    accountid: faker.datatype.number(),
    quantity: faker.datatype.number(100),
    security: faker.random.alpha(4),
    updated: faker.date.recent()
  };
}
