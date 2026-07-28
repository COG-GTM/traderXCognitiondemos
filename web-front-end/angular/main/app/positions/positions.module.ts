import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AgGridModule } from 'ag-grid-angular';
import { PositionsComponent } from './positions.component';
import { PositionsGridComponent } from './positions-grid/positions-grid.component';
import { PositionsSummaryComponent } from './positions-summary/positions-summary.component';
import { DropdownModule } from '../dropdown/dropdown.module';

@NgModule({
  declarations: [PositionsComponent, PositionsGridComponent, PositionsSummaryComponent],
  imports: [
    CommonModule,
    AgGridModule,
    DropdownModule
  ],
  exports: [PositionsComponent]
})
export class PositionsModule { }
