import { enableProdMode } from '@angular/core';
import { platformBrowserDynamic } from '@angular/platform-browser-dynamic';
import { ModuleRegistry } from 'ag-grid-community';
import { ClientSideRowModelModule } from 'ag-grid-community';

import { AppModule } from './app/app.module';
import { environment } from './environments/environment';

if (environment.production) {
    enableProdMode();
}

ModuleRegistry.registerModules([ClientSideRowModelModule]);

platformBrowserDynamic()
    .bootstrapModule(AppModule)
    .catch((err) => console.error(err));
