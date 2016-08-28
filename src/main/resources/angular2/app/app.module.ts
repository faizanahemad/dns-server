

import { AppComponent }  from './app.component';
import {NgModule} from "@angular/core";
import {BrowserModule} from "@angular/platform-browser";
import { HttpModule }     from '@angular/http';
import {FormsModule} from "@angular/forms";
import {ConfigComponent} from "./components/config.component";
import {ControlsComponent} from "./components/controls.component";
import {JsonKeyExtractPipe} from "./scripts/json-key-extract-pipe";
import {ButtonComponent} from "./components/button.component";
import {InlineEditorDirectives} from 'ng2-inline-editor';
import { FORM_DIRECTIVES } from '@angular/common';
import { routing, appRoutingProviders } from './app.routing';
import {HelpComponent} from "./components/help.component";

@NgModule({
    imports: [
        BrowserModule,
        FormsModule,
        HttpModule,
        routing
    ],
    declarations: [
        AppComponent,
        ConfigComponent,
        ControlsComponent,
        HelpComponent,
        JsonKeyExtractPipe,
        ButtonComponent,
        InlineEditorDirectives,
        FORM_DIRECTIVES
    ],
    providers: [
        appRoutingProviders
    ],
    bootstrap: [ AppComponent ]
})
export class AppModule { }
