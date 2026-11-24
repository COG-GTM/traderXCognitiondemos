import { Component, OnInit, output } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';

@Component({
    selector: 'app-header',
    templateUrl: './header.component.html',
    styleUrls: ['./header.component.scss'],
    imports: [RouterLink, RouterLinkActive]
})
export class HeaderComponent implements OnInit {

  readonly switchTheme = output();

  ngOnInit(): void {
  }

}
