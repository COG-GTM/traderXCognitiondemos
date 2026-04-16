import { TestBed } from '@angular/core/testing';
import { TradeFeedService } from './trade-feed.service';

describe('TradeFeedService', () => {
  let service: TradeFeedService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(TradeFeedService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should have subscribe method', () => {
    expect(typeof service.subscribe).toBe('function');
  });

  it('should have unSubscribe method', () => {
    expect(typeof service.unSubscribe).toBe('function');
  });
});
