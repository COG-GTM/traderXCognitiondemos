import { Component } from '@angular/core';
import { ThemeService } from './service/theme.service';
import { RouterModule } from '@angular/router';
import { HeaderComponent } from './header/header.component';
import { CommonModule } from '@angular/common';

@Component({
    selector: 'app-root',
    templateUrl: './app.component.html',
    styleUrls: ['./app.component.scss'],
    standalone: true,
    imports: [RouterModule, HeaderComponent, CommonModule]
})
export class AppComponent {

    constructor(public themeService: ThemeService) { }

}
