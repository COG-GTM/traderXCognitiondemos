import { ComponentFixture, TestBed } from '@angular/core/testing';
import { DropdownComponent } from './dropdown.component';
import { DropdownModule } from './dropdown.module';

describe('DropdownComponent', () => {
  let component: DropdownComponent;
  let fixture: ComponentFixture<DropdownComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DropdownModule]
    }).compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(DropdownComponent);
    component = fixture.componentInstance;
    component.items = [{ label: 'Item 1' }, { label: 'Item 2' }];
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should set default comparator on init', () => {
    expect(component.selectionComparator).toBeDefined();
  });

  it('should generate unique drpId and drpBtnId', () => {
    expect(component.drpId).toBeDefined();
    expect(component.drpBtnId).toBeDefined();
    expect(component.drpId).toContain('drp');
    expect(component.drpBtnId).toContain('drpbtn');
  });

  it('should emit selectedItemChange on item click when item differs', () => {
    spyOn(component.selectedItemChange, 'emit');
    component.selectedItem = { label: 'Item 1' };
    component.onItemClick({ label: 'Item 2' });
    expect(component.selectedItemChange.emit).toHaveBeenCalledWith({ label: 'Item 2' });
  });

  it('should not emit selectedItemChange when same item is clicked', () => {
    spyOn(component.selectedItemChange, 'emit');
    const item = { label: 'Item 1' };
    component.selectedItem = item;
    component.onItemClick(item);
    expect(component.selectedItemChange.emit).not.toHaveBeenCalled();
  });

  it('should use custom comparator if provided', () => {
    const customComparator = (a: any, b: any) => a.label === b.label;
    component.selectionComparator = customComparator;
    component.ngOnInit();
    expect(component.selectionComparator).toBe(customComparator);
  });
});
