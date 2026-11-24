import { Routes } from '@angular/router';
import { PageNotFoundComponent } from './page-not-found.component';

export const routes: Routes = [
    { path: 'trade', loadComponent: () => import('./trade/trade.component').then((m) => m.TradeComponent) },
    { path: 'account', loadComponent: () => import('./accounts/account.component').then((m) => m.AccountComponent) },
    { path: '', redirectTo: '/trade', pathMatch: 'full' }, // redirect to `first-component`
    { path: '**', component: PageNotFoundComponent }
];
