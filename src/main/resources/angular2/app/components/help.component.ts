import {Component, Injectable, OnInit} from "@angular/core";
import {Http, Response} from "@angular/http";
import {Utils} from "../utils";
@Component({
    selector: 'help-content',
    templateUrl: 'angular2/app/components/help.component.html',
    styleUrls: ['angular2/app/styles/listing.component.css']
})

@Injectable()
export class HelpComponent{

    constructor(public http: Http) {

    }
}
