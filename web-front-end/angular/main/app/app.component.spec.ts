import { TestBed } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { AppComponent } from './app.component';
import { ThemeService } from './service/theme.service';

describe('AppComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [AppComponent],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(AppComponent);
    const app = fixture.debugElement.componentInstance;
    expect(app).toBeTruthy();
  });

  it('should expose the theme service to the template', () => {
    const fixture = TestBed.createComponent(AppComponent);
    const app = fixture.componentInstance;
    expect(app.themeService).toBe(TestBed.inject(ThemeService));
  });

  it('should render the header and the router outlet', () => {
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();
    const compiled = fixture.debugElement.nativeElement as HTMLElement;
    expect(compiled.querySelector('app-header')).not.toBeNull();
    expect(compiled.querySelector('router-outlet')).not.toBeNull();
  });

  it('should delegate a header switchTheme event to the theme service', () => {
    const fixture = TestBed.createComponent(AppComponent);
    const themeService = TestBed.inject(ThemeService);
    spyOn(themeService, 'switchTheme');
    fixture.detectChanges();

    const header = fixture.debugElement.nativeElement.querySelector('app-header') as HTMLElement;
    header.dispatchEvent(new CustomEvent('switchTheme'));
    fixture.detectChanges();

    expect(themeService.switchTheme).toHaveBeenCalled();
  });
});
