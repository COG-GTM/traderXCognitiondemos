import { ComponentFixture, TestBed, fakeAsync, flush, tick } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { AgGridAngular, AgGridModule } from 'ag-grid-angular';
import { GetRowIdParams } from 'ag-grid-community';
import { PositionsGridComponent } from './positions-grid.component';
import { positions } from 'main/app/test-utils/mocks.service';

describe('PositionsGridComponent', () => {
    let component: PositionsGridComponent;
    let fixture: ComponentFixture<PositionsGridComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [PositionsGridComponent],
            imports: [
                AgGridModule
            ]
        }).compileComponents();
    });

    beforeEach(() => {
        fixture = TestBed.createComponent(PositionsGridComponent);
        component = fixture.componentInstance;
        component.positions = positions.map((position) => ({ ...position }));
        fixture.detectChanges();
        component.gridApi = fixture.debugElement.query(By.directive(AgGridAngular)).componentInstance.api;
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should show the given positions with a security, quantity and updated column', () => {
        const columns = fixture.nativeElement.querySelectorAll('.ag-header-cell');
        const rows = fixture.nativeElement.querySelectorAll('.ag-center-cols-container .ag-row');
        expect(columns.length).toEqual(3);
        expect(rows.length).toEqual(2);
        expect(rows[0].children[0].innerText).toEqual(component.positions[0].security);
        expect(rows[0].children[1].innerText).toEqual(component.positions[0].quantity.toString());
    });

    it('should update the quantity of a known security from a position update', fakeAsync(() => {
        const expectedQuantity = positions[0].quantity + 100;
        component.ngOnChanges({ positionUpdate: { currentValue: { ...positions[0], quantity: expectedQuantity } } } as any);
        fixture.detectChanges();
        tick(100);

        const rows = fixture.nativeElement.querySelectorAll('.ag-center-cols-container .ag-row');
        expect(rows.length).toEqual(2);
        expect(component.gridApi?.getRowNode(`Position-${positions[0].security}`)?.data?.quantity).toEqual(expectedQuantity);
        flush();
    }));

    it('should add a row for a security that is not in the grid yet', fakeAsync(() => {
        const update = { ...positions[0], security: 'NEWCO', quantity: 12 };
        component.ngOnChanges({ positionUpdate: { currentValue: update } } as any);
        fixture.detectChanges();
        tick(100);

        const rows = fixture.nativeElement.querySelectorAll('.ag-center-cols-container .ag-row');
        expect(rows.length).toEqual(3);
        expect(component.gridApi?.getRowNode('Position-NEWCO')?.data?.quantity).toEqual(12);
        flush();
    }));

    it('should ignore a position update before the grid is ready', () => {
        component.gridApi = undefined;
        expect(() => component.ngOnChanges({ positionUpdate: { currentValue: positions[0] } } as any)).not.toThrow();
    });

    it('getRowId should return the security from position data', () => {
        expect(component.getRowId({ data: positions[0] } as GetRowIdParams)).toEqual(`Position-${positions[0].security}`);
    });
});
