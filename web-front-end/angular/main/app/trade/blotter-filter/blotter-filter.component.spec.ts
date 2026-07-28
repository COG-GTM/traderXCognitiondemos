import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';

import { BlotterFilterComponent } from './blotter-filter.component';
import { State } from 'main/app/model/trade.model';

describe('BlotterFilterComponent', () => {
    let component: BlotterFilterComponent;
    let fixture: ComponentFixture<BlotterFilterComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [BlotterFilterComponent],
            imports: [FormsModule]
        }).compileComponents();
    });

    beforeEach(() => {
        fixture = TestBed.createComponent(BlotterFilterComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should render a button per state with All selected by default', () => {
        const buttons = fixture.nativeElement.querySelectorAll('.btn-group .btn');
        expect(buttons.length).toEqual(5);
        expect(fixture.nativeElement.querySelector('#stateFilterAll').classList).toContain('btn-primary');
        expect(fixture.nativeElement.querySelector('#stateFilterSettled').classList).toContain('btn-secondary');
    });

    it('should emit the state and select the clicked button', () => {
        spyOn(component.stateChange, 'emit');
        fixture.nativeElement.querySelector('#stateFilterSettled').click();
        fixture.detectChanges();
        expect(component.stateChange.emit).toHaveBeenCalledWith(State.Settled);
        expect(component.selectedState).toEqual(State.Settled);
        expect(fixture.nativeElement.querySelector('#stateFilterSettled').classList).toContain('btn-primary');
        expect(fixture.nativeElement.querySelector('#stateFilterAll').classList).toContain('btn-secondary');
    });

    it('should not emit when the selected state is clicked again', () => {
        spyOn(component.stateChange, 'emit');
        fixture.nativeElement.querySelector('#stateFilterAll').click();
        expect(component.stateChange.emit).not.toHaveBeenCalled();
    });

    it('should emit the typed security', () => {
        spyOn(component.securityChange, 'emit');
        const input = fixture.nativeElement.querySelector('#securityFilterInput');
        input.value = 'aapl';
        input.dispatchEvent(new Event('input'));
        fixture.detectChanges();
        expect(component.securityChange.emit).toHaveBeenCalledWith('aapl');
    });
});
