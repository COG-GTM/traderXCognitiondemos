import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';
import { AppComponent } from './app.component';

import { provideHttpClient, withInterceptorsFromDi, withJsonpSupport } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { AgGridModule } from 'ag-grid-angular';
import { HeaderComponent } from './header/header.component';

import { RouterModule } from '@angular/router';
import { routes } from './routing';
import { PageNotFoundComponent } from './page-not-found.component';

@NgModule({ declarations: [AppComponent], bootstrap: [AppComponent], imports: [BrowserModule,
    CommonModule,
    AgGridModule,
    RouterModule.forRoot(routes), HeaderComponent, PageNotFoundComponent], providers: [provideHttpClient(withInterceptorsFromDi(), withJsonpSupport())] })
export class AppModule { }
