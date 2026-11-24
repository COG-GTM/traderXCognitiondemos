import { Component, inject } from '@angular/core';
import { ThemeService } from './service/theme.service';
import { HeaderComponent } from './header/header.component';
import { RouterOutlet } from '@angular/router';

@Component({
    selector: 'app-root',
    templateUrl: './app.component.html',
    styleUrls: ['./app.component.scss'],
    imports: [HeaderComponent, RouterOutlet]
})
export class AppComponent {
    themeService = inject(ThemeService);

}
