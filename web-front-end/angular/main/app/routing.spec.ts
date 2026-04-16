import { TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { Router } from '@angular/router';
import { routes } from './routing';

describe('App Routing', () => {
  let router: Router;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [RouterTestingModule.withRoutes(routes)]
    });
    router = TestBed.inject(Router);
  });

  it('should have routes defined', () => {
    expect(routes.length).toBeGreaterThan(0);
  });

  it('should have a route for trade', () => {
    const tradeRoute = routes.find(r => r.path === 'trade');
    expect(tradeRoute).toBeDefined();
  });

  it('should have a route for account', () => {
    const accountRoute = routes.find(r => r.path === 'account');
    expect(accountRoute).toBeDefined();
  });

  it('should have a default redirect to /trade', () => {
    const defaultRoute = routes.find(r => r.path === '');
    expect(defaultRoute).toBeDefined();
    expect(defaultRoute?.redirectTo).toBe('/trade');
  });

  it('should have a wildcard route for 404', () => {
    const wildcardRoute = routes.find(r => r.path === '**');
    expect(wildcardRoute).toBeDefined();
  });
});
