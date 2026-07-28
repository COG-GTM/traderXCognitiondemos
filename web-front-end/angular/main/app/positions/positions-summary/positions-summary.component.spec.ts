import { ComponentFixture, TestBed } from '@angular/core/testing';
import { PositionsSummaryComponent } from './positions-summary.component';
import { positions } from 'main/app/test-utils/mocks.service';

describe('PositionsSummaryComponent', () => {
    let component: PositionsSummaryComponent;
    let fixture: ComponentFixture<PositionsSummaryComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [PositionsSummaryComponent]
        }).compileComponents();
    });

    beforeEach(() => {
        fixture = TestBed.createComponent(PositionsSummaryComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should show zeros when there are no positions', () => {
        expect(fixture.nativeElement.querySelector('#securityCountValue').innerText).toEqual('0');
        expect(fixture.nativeElement.querySelector('#totalQuantityValue').innerText).toEqual('0');
    });

    it('should count distinct securities and total absolute quantity', () => {
        component.positions = [
            { ...positions[0], security: 'AAPL', quantity: 10 },
            { ...positions[1], security: 'MSFT', quantity: -5 },
            { ...positions[0], security: 'AAPL', quantity: 10 }
        ];
        fixture.detectChanges();
        expect(component.securityCount).toEqual(2);
        expect(component.totalQuantity).toEqual(25);
        expect(fixture.nativeElement.querySelector('#securityCountValue').innerText).toEqual('2');
        expect(fixture.nativeElement.querySelector('#totalQuantityValue').innerText).toEqual('25');
    });
});
