import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TechnicalAnalysisComponent } from './technical-analysis.component';
import { FormsModule } from '@angular/forms';
import { DropdownModule } from '../dropdown/dropdown.module';

@NgModule({
  declarations: [TechnicalAnalysisComponent],
  imports: [
    CommonModule,
    FormsModule,
    DropdownModule
  ],
  exports: [TechnicalAnalysisComponent]
})
export class TechnicalAnalysisModule { }
