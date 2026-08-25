import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TradeTicketComponent } from './trade-ticket.component';
import { By } from '@angular/platform-browser';
import { FormsModule } from '@angular/forms';
import { stocks as dummyStocks, accounts as dummyAccounts } from 'main/app/test-utils/mocks.service';
import { TypeaheadModule } from 'ngx-bootstrap/typeahead';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { setInputValue } from 'main/app/test-utils/utils';

describe('TradeTicketComponent', () => {
  let component: TradeTicketComponent;
  let fixture: ComponentFixture<TradeTicketComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [TradeTicketComponent],
      imports: [
        FormsModule,
        NoopAnimationsModule,
        TypeaheadModule.forRoot()
      ]
    })
      .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(TradeTicketComponent);
    component = fixture.componentInstance;
    component.account = dummyAccounts[0];
    component.stocks = dummyStocks;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should show ticket with initial values', async () => {
    await fixture.whenStable();
    const quantityField = fixture.debugElement.query(By.css('#quantityField'));
    expect(quantityField.nativeElement.value).toEqual('0');
    const buyButton = fixture.debugElement.query(By.css('#buyButton'));
    expect(buyButton.nativeElement.checked).toBeTrue();
    const accountLabel = fixture.debugElement.query(By.css('#accountLabel'));
    expect(accountLabel.nativeElement.value).toEqual(component.account?.displayName);
  });

  it('should update ticket object with given values on create click and emit create event', async () => {
    setInputValue(fixture, '#quantityField', '10');
    const sellButton = fixture.debugElement.query(By.css('#sellButton'));
    sellButton.nativeElement.click();
    fixture.detectChanges();
    component.ticket.security = dummyStocks[0].ticker;

    spyOn(component.create, 'emit');
    const createButton = fixture.debugElement.query(By.css('#createButton'));
    createButton.nativeElement.click();
    fixture.detectChanges();

    expect(component.create.emit).toHaveBeenCalledWith(
      {
        quantity: 10, accountId: component.account?.id as any, side: 'Sell', security: component.ticket.security
      });
  });

  it('should emit cancel on cancel click', async () => {
    spyOn(component.cancel, 'emit');
    const cancelButton = fixture.debugElement.query(By.css('#cancelButton'));
    cancelButton.nativeElement.click();
    fixture.detectChanges();
    expect(component.cancel.emit).toHaveBeenCalled();
  });

  it('should seed the typeahead list from the stocks input on init', () => {
    expect(component.filteredStocks).toEqual(dummyStocks);
  });

  // ---------------------------------------------------------------------
  // UI-01..UI-09 edge / corner cases
  // ---------------------------------------------------------------------

  describe('quantity edge cases', () => {

    // UI-01
    it('should silently swallow a create with quantity 0 and show no validation feedback', () => {
      spyOn(component.create, 'emit');
      component.ticket.security = dummyStocks[0].ticker;
      setInputValue(fixture, '#quantityField', '0');
      expect(component.ticket.quantity).toEqual(0);

      fixture.debugElement.query(By.css('#createButton')).nativeElement.click();
      fixture.detectChanges();

      expect(component.create.emit).not.toHaveBeenCalled();
      // observed behaviour: nothing at all is rendered to tell the user why
      expect(fixture.nativeElement.querySelector('.alert')).toBeNull();
      expect(fixture.nativeElement.querySelector('.invalid-feedback')).toBeNull();
      expect(fixture.nativeElement.querySelector('.is-invalid')).toBeNull();
      expect(fixture.nativeElement.textContent).not.toMatch(/required|invalid|must be/i);
    });

    // UI-02a
    it('should emit a negative quantity unvalidated', () => {
      spyOn(component.create, 'emit');
      component.ticket.security = dummyStocks[0].ticker;
      setInputValue(fixture, '#quantityField', '-5');

      component.onCreate();

      expect(component.ticket.quantity).toEqual(-5);
      expect(component.create.emit).toHaveBeenCalledWith(jasmine.objectContaining({ quantity: -5 }));
    });

    // UI-02b
    it('should emit a fractional quantity unvalidated', () => {
      spyOn(component.create, 'emit');
      component.ticket.security = dummyStocks[0].ticker;
      setInputValue(fixture, '#quantityField', '1.5');

      component.onCreate();

      expect(component.ticket.quantity).toEqual(1.5);
      expect(component.create.emit).toHaveBeenCalledWith(jasmine.objectContaining({ quantity: 1.5 }));
    });

    // UI-02c
    it('should coerce a numeric string typed into the number field to a number', () => {
      spyOn(component.create, 'emit');
      component.ticket.security = dummyStocks[0].ticker;
      setInputValue(fixture, '#quantityField', '42');

      component.onCreate();

      expect(typeof component.ticket.quantity).toEqual('number');
      expect(component.create.emit).toHaveBeenCalledWith(jasmine.objectContaining({ quantity: 42 }));
    });

    // UI-02d
    it('should treat non-numeric text in the number field as an empty (null) quantity and not emit', () => {
      spyOn(component.create, 'emit');
      component.ticket.security = dummyStocks[0].ticker;
      setInputValue(fixture, '#quantityField', 'abc');

      component.onCreate();

      // a type=number input reports '' for junk input, so ngModel writes null
      expect(component.ticket.quantity).toBeNull();
      expect(component.create.emit).not.toHaveBeenCalled();
    });

    // UI-03
    it('should emit a quantity beyond the java int range without any client side guard', () => {
      spyOn(component.create, 'emit');
      component.ticket.security = dummyStocks[0].ticker;
      setInputValue(fixture, '#quantityField', '2147483648');

      component.onCreate();

      expect(component.ticket.quantity).toEqual(2147483648);
      expect(component.create.emit).toHaveBeenCalledWith(jasmine.objectContaining({ quantity: 2147483648 }));
    });
  });

  describe('security edge cases', () => {

    // UI-04a
    it('should not emit when the security is empty', () => {
      spyOn(component.create, 'emit');
      component.ticket.quantity = 10;
      component.ticket.security = '';

      component.onCreate();

      expect(component.create.emit).not.toHaveBeenCalled();
    });

    // UI-04b
    it('should emit a whitespace-only security because it is never trimmed', () => {
      spyOn(component.create, 'emit');
      component.ticket.quantity = 10;
      component.ticket.security = '   ';

      component.onCreate();

      expect(component.create.emit).toHaveBeenCalledWith(jasmine.objectContaining({ security: '   ' }));
    });

    // UI-04c
    it('should keep an unknown free-typed ticker out of the ticket and silently refuse to create', () => {
      spyOn(component.create, 'emit');
      component.ticket.quantity = 10;
      const stockInput = fixture.debugElement.query(By.css('#stock-input'));
      stockInput.nativeElement.value = 'NOTATICKER';
      stockInput.nativeElement.dispatchEvent(new Event('input'));
      fixture.detectChanges();

      // the text the user typed is bound to selectedCompany, never to ticket.security
      expect(component.selectedCompany).toEqual('NOTATICKER');
      component.onBlur();
      // onBlur bails out early because selectedCompany is truthy, so security stays ''
      expect(component.ticket.security).toEqual('');

      component.onCreate();
      expect(component.create.emit).not.toHaveBeenCalled();
    });

    // UI-04d
    it('should clear the security on blur when nothing was ever typed or selected', () => {
      component.ticket.security = 'AAPL';
      component.selectedCompany = undefined;

      component.onBlur();

      expect(component.ticket.security).toEqual('');
    });

    // UI-04e
    it('should take the ticker (not the company name) from a typeahead selection', () => {
      component.onSelect({ value: dummyStocks[1].companyName, item: dummyStocks[1] } as any);
      expect(component.ticket.security).toEqual(dummyStocks[1].ticker);
    });
  });

  describe('account and stocks inputs', () => {

    // UI-05
    it('should default accountId to 0 when no account is supplied and still allow a submit', () => {
      const localFixture = TestBed.createComponent(TradeTicketComponent);
      const localComponent = localFixture.componentInstance;
      localComponent.account = undefined;
      localComponent.stocks = dummyStocks;
      localFixture.detectChanges();

      expect(localComponent.ticket.accountId).toEqual(0);

      spyOn(localComponent.create, 'emit');
      localComponent.ticket.security = dummyStocks[0].ticker;
      localComponent.ticket.quantity = 3;
      localComponent.onCreate();

      expect(localComponent.create.emit).toHaveBeenCalledWith(jasmine.objectContaining({ accountId: 0 }));
    });

    // UI-06a
    it('should render with an empty stocks list', () => {
      const localFixture = TestBed.createComponent(TradeTicketComponent);
      const localComponent = localFixture.componentInstance;
      localComponent.account = dummyAccounts[0];
      localComponent.stocks = [];
      expect(() => localFixture.detectChanges()).not.toThrow();
      expect(localComponent.filteredStocks).toEqual([]);
    });

    // UI-06b
    it('should leave filteredStocks undefined when the stocks input is undefined', () => {
      const localFixture = TestBed.createComponent(TradeTicketComponent);
      const localComponent = localFixture.componentInstance;
      localComponent.account = dummyAccounts[0];
      localComponent.stocks = undefined as any;
      expect(() => localFixture.detectChanges()).not.toThrow();
      // observed behaviour: the [] default declared on the field is overwritten with undefined
      expect(localComponent.filteredStocks).toBeUndefined();
    });
  });

  describe('side toggle, resubmission and cancel', () => {

    // UI-07a
    it('should round trip the side between Buy and Sell', () => {
      expect(component.ticket.side).toEqual('Buy');

      fixture.debugElement.query(By.css('#sellButton')).nativeElement.click();
      fixture.detectChanges();
      expect(component.ticket.side).toEqual('Sell');

      fixture.debugElement.query(By.css('#buyButton')).nativeElement.click();
      fixture.detectChanges();
      expect(component.ticket.side).toEqual('Buy');
    });

    // UI-07b
    it('should NOT reset the ticket after a successful create', () => {
      component.ticket.security = dummyStocks[0].ticker;
      component.ticket.quantity = 7;
      component.selectedCompany = dummyStocks[0].companyName;

      component.onCreate();

      // observed behaviour: the form keeps the previous values
      expect(component.ticket.quantity).toEqual(7);
      expect(component.ticket.security).toEqual(dummyStocks[0].ticker);
      expect(component.selectedCompany).toEqual(dummyStocks[0].companyName);
    });

    // UI-08
    it('should emit two identical tickets on a rapid double click of create', () => {
      const emitted: any[] = [];
      component.create.subscribe((ticket) => emitted.push({ ...ticket }));
      component.ticket.security = dummyStocks[0].ticker;
      component.ticket.quantity = 11;

      const createButton = fixture.debugElement.query(By.css('#createButton')).nativeElement;
      createButton.click();
      createButton.click();
      fixture.detectChanges();

      expect(emitted.length).toEqual(2);
      expect(emitted[0]).toEqual(emitted[1]);
      // observed behaviour: nothing disables the button between the two clicks
      expect(createButton.disabled).toBeFalse();
    });

    // UI-09
    it('should emit cancel without touching the ticket state', () => {
      component.ticket.security = dummyStocks[0].ticker;
      component.ticket.quantity = 9;
      component.selectedCompany = dummyStocks[0].companyName;
      spyOn(component.cancel, 'emit');

      component.onCancel();

      expect(component.cancel.emit).toHaveBeenCalled();
      expect(component.ticket).toEqual({
        quantity: 9,
        accountId: dummyAccounts[0].id,
        side: 'Buy',
        security: dummyStocks[0].ticker
      });
      expect(component.selectedCompany).toEqual(dummyStocks[0].companyName);
    });
  });
});
