import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AccountComponent } from './account.component';
import { AgGridModule } from 'ag-grid-angular';
import { EditAccountComponent } from './edit/edit.component';
import { FormsModule } from '@angular/forms';
import { ButtonCellRendererComponent } from './button-renderer.component';
import { AssignUserToAccountComponent } from './user/assign-user.component';
import { TypeaheadModule } from 'ngx-bootstrap/typeahead';
import { AlertModule } from 'ngx-bootstrap/alert';
import { DropdownComponent } from '../dropdown/dropdown.component';

@NgModule({
  declarations: [ButtonCellRendererComponent],
  imports: [
    CommonModule,
    FormsModule,
    TypeaheadModule.forRoot(),
    DropdownComponent,
    AlertModule.forRoot(),
    AgGridModule
  ],
  exports: [ButtonCellRendererComponent]
})
export class AccountsModule { }
