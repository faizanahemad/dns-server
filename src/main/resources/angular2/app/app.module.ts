

import { AppComponent }  from './app.component';
import {NgModule} from "@angular/core";
import {BrowserModule} from "@angular/platform-browser";
import { HttpModule }     from '@angular/http';
import {FormsModule} from "@angular/forms";
import {ConfigComponent} from "./components/config.component";
import {ListingComponent} from "./components/listing.component";
import {ControlsComponent} from "./components/controls.component";
import {JsonKeyExtractPipe} from "./scripts/json-key-extract-pipe";
import {ButtonComponent} from "./components/button.component";

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
        ControlsComponent,
        JsonKeyExtractPipe,
        ButtonComponent
    ],
    bootstrap: [ AppComponent ]
})
export class AppModule { }
