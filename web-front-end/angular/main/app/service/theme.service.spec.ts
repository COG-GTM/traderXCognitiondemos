import { TestBed } from '@angular/core/testing';
import { ThemeService } from './theme.service';

describe('ThemeService', () => {
  let service: ThemeService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(ThemeService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should have default theme as professional-dark', () => {
    expect(service.currentTheme).toBe('professional-dark');
  });

  it('should switch theme from dark to light', () => {
    const mockThemeTag = document.createElement('link');
    mockThemeTag.id = 'theme-tag';
    document.head.appendChild(mockThemeTag);

    service.switchTheme();
    expect(service.currentTheme).toBe('professional-light');
    expect(document.documentElement.className).toBe('professional-light');

    document.head.removeChild(mockThemeTag);
  });

  it('should switch theme from light back to dark', () => {
    const mockThemeTag = document.createElement('link');
    mockThemeTag.id = 'theme-tag';
    document.head.appendChild(mockThemeTag);

    service.switchTheme(); // dark -> light
    service.switchTheme(); // light -> dark
    expect(service.currentTheme).toBe('professional-dark');
    expect(document.documentElement.className).toBe('professional-dark');

    document.head.removeChild(mockThemeTag);
  });
});
