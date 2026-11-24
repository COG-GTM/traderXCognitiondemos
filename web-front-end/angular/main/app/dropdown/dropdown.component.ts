import { Component, EventEmitter, Input, OnInit, Output, input } from "@angular/core";
import { BsDropdownDirective, BsDropdownToggleDirective, BsDropdownMenuDirective } from "ngx-bootstrap/dropdown";
import { NgClass } from "@angular/common";

let id = 0;

@Component({
    selector: 'app-ngx-dropdown',
    templateUrl: './dropdown.component.html',
    imports: [BsDropdownDirective, BsDropdownToggleDirective, BsDropdownMenuDirective, NgClass]
})
export class DropdownComponent implements OnInit {
    readonly items = input<any[]>();
    readonly itemKey = input<string>('label');
    readonly selectedItem = input<any>();
    @Input() selectionComparator: (src: any, target: any) => boolean;
    @Output() selectedItemChange = new EventEmitter();
    readonly placeholder = input<string>("Please select an item");
    drpId: string;
    drpBtnId: string;

    ngOnInit() {
        this.selectionComparator = this.selectionComparator || this.defaultComparator;
        const uid = id++;
        this.drpId = 'drp' + uid;
        this.drpBtnId = 'drpbtn' + uid;
    }

    defaultComparator = (src: any, target: any) => src === target;

    onItemClick(item: any) {
        if (!this.selectionComparator(this.selectedItem(), item)) {
            this.selectedItemChange.emit(item);
        }
    }
}