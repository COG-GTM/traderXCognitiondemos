import { enableProdMode, provideZoneChangeDetection } from '@angular/core';
import { provideHttpClient, withJsonpSupport } from '@angular/common/http';
import { bootstrapApplication } from '@angular/platform-browser';
import { provideRouter } from '@angular/router';

import { environment } from './environments/environment';
import { routes } from './app/routing';
import { AppComponent } from './app/app.component';

if (environment.production) {
    enableProdMode();
}

bootstrapApplication(AppComponent, {
    providers: [
        provideZoneChangeDetection(),
        provideHttpClient(withJsonpSupport()),
        provideRouter(routes)
    ]
})
    .catch((err) => console.error(err));
