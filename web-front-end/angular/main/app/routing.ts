import { Routes } from '@angular/router';
import { AccountComponent } from './accounts/account.component';
import { PageNotFoundComponent } from './page-not-found.component';
import { TradeComponent } from './trade/trade.component';
import { TechnicalAnalysisComponent } from './technical-analysis/technical-analysis.component';

export const routes: Routes = [
    { path: 'trade', component: TradeComponent },
    { path: 'account', component: AccountComponent },
    { path: 'technical-analysis', component: TechnicalAnalysisComponent },
    { path: '', redirectTo: '/trade', pathMatch: 'full' }, // redirect to `first-component`
    { path: '**', component: PageNotFoundComponent }
];
