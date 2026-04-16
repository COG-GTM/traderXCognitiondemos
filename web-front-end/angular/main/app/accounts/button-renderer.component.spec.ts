import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ButtonCellRendererComponent } from './button-renderer.component';

describe('ButtonCellRendererComponent', () => {
  let component: ButtonCellRendererComponent;
  let fixture: ComponentFixture<ButtonCellRendererComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ButtonCellRendererComponent]
    }).compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ButtonCellRendererComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize with agInit', () => {
    const mockParams = {
      data: { id: 1, displayName: 'Test Account' },
      clicked: jasmine.createSpy('clicked')
    } as any;
    component.agInit(mockParams);
    expect(component).toBeTruthy();
  });

  it('should call clicked callback on clickHandler', () => {
    const clickedSpy = jasmine.createSpy('clicked');
    const mockParams = {
      data: { id: 1, displayName: 'Test Account' },
      clicked: clickedSpy
    } as any;
    component.agInit(mockParams);
    component.clickHandler();
    expect(clickedSpy).toHaveBeenCalledWith({ id: 1, displayName: 'Test Account' });
  });

  it('should return false from refresh', () => {
    expect(component.refresh({} as any)).toBe(false);
  });

  it('should render the Update button', () => {
    const mockParams = {
      data: { id: 1 },
      clicked: jasmine.createSpy('clicked')
    } as any;
    component.agInit(mockParams);
    fixture.detectChanges();
    const button = fixture.nativeElement.querySelector('button');
    expect(button).toBeTruthy();
    expect(button.textContent.trim()).toBe('Update');
  });
});
