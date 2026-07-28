import { Component, EventEmitter, Output } from '@angular/core';
import { State, StateFilter } from '../../model/trade.model';

interface StateOption {
    id: string;
    label: string;
    value: StateFilter;
}

@Component({
    selector: 'app-blotter-filter',
    templateUrl: './blotter-filter.component.html',
    styleUrls: ['./blotter-filter.component.scss']
})
export class BlotterFilterComponent {
    @Output() stateChange = new EventEmitter<StateFilter>();
    @Output() securityChange = new EventEmitter<string>();

    selectedState: StateFilter = 'All';
    security = '';

    readonly stateOptions: StateOption[] = [
        { id: 'stateFilterAll', label: 'All', value: 'All' },
        { id: 'stateFilterNew', label: 'New', value: State.New },
        { id: 'stateFilterProcessing', label: 'Processing', value: State.Processing },
        { id: 'stateFilterPending', label: 'Pending', value: State.Pending },
        { id: 'stateFilterSettled', label: 'Settled', value: State.Settled }
    ];

    onStateSelect(state: StateFilter) {
        if (state === this.selectedState) { return; }
        this.selectedState = state;
        this.stateChange.emit(state);
    }

    onSecurityInput(security: string) {
        this.security = security;
        this.securityChange.emit(security);
    }
}
