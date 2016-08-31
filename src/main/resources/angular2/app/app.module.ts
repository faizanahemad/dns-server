

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
import { AlertComponent } from 'ng2-bootstrap/components/alert';
import { ModalModule } from 'angular2-modal';
import { BootstrapModalModule } from 'angular2-modal/plugins/bootstrap/index';
import { FirstUseModal} from "./components/modals/first-use-modal";
import {ConfigService} from "./services/config.service";

@NgModule({
    imports: [
        BrowserModule,
        FormsModule,
        HttpModule,
        ModalModule.forRoot(),
        BootstrapModalModule,
        routing
    ],
    declarations: [
        AppComponent,
        ConfigComponent,
        ControlsComponent,
        HelpComponent,
        AlertComponent,
        JsonKeyExtractPipe,
        ButtonComponent,
        InlineEditorDirectives,
        FORM_DIRECTIVES
    ],
    providers: [
        appRoutingProviders,ConfigService
    ],
    bootstrap: [ AppComponent ],
    entryComponents: [ FirstUseModal ]
})
export class AppModule { }
