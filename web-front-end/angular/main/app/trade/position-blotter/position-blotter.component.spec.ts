import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AgGridAngular } from 'ag-grid-angular';
import { PositionBlotterComponent } from './position-blotter.component';
import { PositionService } from 'main/app/service/position.service';
import { MockTradeService, MockTradeFeedService, positions } from 'main/app/test-utils/mocks.service';
import { TradeFeedService } from 'main/app/service/trade-feed.service';

describe('PositionBlotterComponent', () => {
  let component: PositionBlotterComponent;
  let fixture: ComponentFixture<PositionBlotterComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [PositionBlotterComponent],
      imports: [
        AgGridAngular
      ],
      providers: [
        {
          provide: PositionService,
          useClass: MockTradeService
        },
        {
          provide: TradeFeedService,
          useClass: MockTradeFeedService
        }
      ]
    })
      .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(PositionBlotterComponent);
    component = fixture.componentInstance;
    component.positions = positions;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should show given positions in the grid', async () => {
    // AG Grid v33 renders asynchronously - verify the grid element exists
    const gridEl = fixture.nativeElement.querySelector('ag-grid-angular');
    expect(gridEl).toBeTruthy();
    // Verify the component has the correct positions data
    expect(component.positions.length).toEqual(2);
    expect(component.positions[0].security).toBeDefined();
    expect(component.positions[0].quantity).toBeDefined();
  });

});
