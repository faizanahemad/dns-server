

import { AppComponent }  from './app.component';
import {NgModule} from "@angular/core";
import {BrowserModule} from "@angular/platform-browser";
import { HttpModule }     from '@angular/http';
import {FormsModule} from "@angular/forms";
import {ConfigComponent} from "./components/config.component";
import {ListingComponent} from "./components/listing.component";
import {ControlsComponent} from "./components/controls.component";

@NgModule({
    imports: [
        BrowserModule,
        FormsModule,
        HttpModule
    ],
    declarations: [
        AppComponent,
        ConfigComponent,
        ListingComponent,
        ControlsComponent
    ],
    bootstrap: [ AppComponent ]
})
export class AppModule { }
