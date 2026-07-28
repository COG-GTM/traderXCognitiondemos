import { ComponentFixture, TestBed } from '@angular/core/testing';

import { BlotterSummaryComponent } from './blotter-summary.component';
import { positions as dummyPositions, trades as dummyTrades } from 'main/app/test-utils/mocks.service';
import { Position, State, Trade } from 'main/app/model/trade.model';

describe('BlotterSummaryComponent', () => {
    let component: BlotterSummaryComponent;
    let fixture: ComponentFixture<BlotterSummaryComponent>;

    const trades: Trade[] = [
        { ...dummyTrades[0], state: State.New },
        { ...dummyTrades[1], state: State.Processing },
        { ...dummyTrades[0], id: 'pending-1', state: State.Pending },
        { ...dummyTrades[0], id: 'settled-1', state: State.Settled },
        { ...dummyTrades[1], id: 'settled-2', state: State.Settled }
    ];

    const positions: Position[] = [
        { ...dummyPositions[0], security: 'AAPL', quantity: 100 },
        { ...dummyPositions[1], security: 'MSFT', quantity: 200 },
        { ...dummyPositions[0], security: 'TSLA', quantity: 0 }
    ];

    const cardValue = (id: string): string =>
        fixture.nativeElement.querySelector(`#${id} .fs-4`).textContent.replace(/\s+/g, ' ').trim();

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [BlotterSummaryComponent]
        }).compileComponents();
    });

    beforeEach(() => {
        fixture = TestBed.createComponent(BlotterSummaryComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should render zeroes when there is no data', () => {
        expect(cardValue('tradesTodayCard')).toEqual('0');
        expect(cardValue('settledCard')).toEqual('0');
        expect(cardValue('securitiesHeldCard')).toEqual('0');
    });

    it('should count trades, working and settled trades from the given trades', () => {
        component.trades = trades;
        component.ngOnChanges();
        fixture.detectChanges();
        expect(cardValue('tradesTodayCard')).toEqual('5');
        expect(cardValue('workingCard')).toEqual('3 Pending');
        expect(cardValue('settledCard')).toEqual('2');
    });

    it('should count distinct securities with a non zero quantity', () => {
        component.positions = positions;
        component.ngOnChanges();
        fixture.detectChanges();
        expect(cardValue('securitiesHeldCard')).toEqual('2');
    });
});
