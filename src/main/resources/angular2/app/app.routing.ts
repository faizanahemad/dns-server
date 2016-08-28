import { ModuleWithProviders } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';
import {RedirectListingComponent} from "./components/redirect-listing.component";
import {DnsListingComponent} from "./components/dns-listing.component";
import {HelpComponent} from "./components/help.component";
import {ConfigComponent} from "./components/config.component";

const appRoutes: Routes = [
    { path: 'redirect', component: RedirectListingComponent },
    { path: 'dns', component: DnsListingComponent },
    { path: 'ui', component: RedirectListingComponent },
    { path: 'help', component: HelpComponent },
    { path: 'settings', component: ConfigComponent },
    {
        path: '',
        redirectTo: 'redirect',
        pathMatch: 'full'
    }
];

export const appRoutingProviders: any[] = [

];

export const routing: ModuleWithProviders = RouterModule.forRoot(appRoutes);
