import {Component, Output, EventEmitter} from '@angular/core';

import {DialogRef, ModalComponent, OverlayContext} from 'angular2-modal';
import { BSModalContext } from 'angular2-modal/plugins/bootstrap/index';
import {ConfigService} from "../../services/config.service";
import {Config} from "../../models/config";
import {Utils} from "../../utils";

export class FirstUseModalData extends BSModalContext {
    context:OverlayContext;
    constructor() {
        super();
        this.context = new OverlayContext;
        this.context.inElement = false;
        this.context.isBlocking = true;
    }
}
@Component({
    selector: 'first-use-modal',
    styles: [`
        bs-modal-container > div {
            width: 600px;
            margin: 30px auto;
        }
        .modal-content {
            display: block;
            width: 600px;
            margin: 30px auto;
        }
    `],
     template: `
        
        
            <div class="modal-header">
            
                    <button type="button" class="close" (click)="reject()"><span>&times;</span></button>
                
                    <h4 class="modal-title">Using for the first time? Read below...</h4>
                
            </div>
            <div class="modal-body">
                <ul>
                
                    <li>Is MySql installed then configure it for higher performance. Go to <a routerLink="/settings" (click)="accept()"><i class="fa fa-cog"></i> Settings</a> for that.</li>
                    <li>For Guide on usage and How Tos go to <a routerLink="/help" (click)="accept()"><i class="fa fa-question-circle-o"></i> Help</a>.</li>
                    <li>For other configuration options look at <a routerLink="/settings" (click)="accept()"><i class="fa fa-cog"></i> Settings</a> as well.</li>
                </ul>
            </div>
            <div class="modal-footer">
                
                    <button class="btn btn-success" (click)="accept()" routerLink="/settings"><i class="fa fa-cog"></i> Configure</button>
                    <button class="btn btn-primary" (click)="accept()" routerLink="/help"><i class="fa fa-question-circle-o"></i> Help</button>
                    <button class="btn btn-default" (click)="reject()">Close</button>
                
            </div>
        
        `
})
export class FirstUseModal implements ModalComponent<any> {
    context: any;


    @Output() clicked: EventEmitter<boolean> = new EventEmitter<boolean>();
    alert: any = {};


    constructor(public dialog: DialogRef<FirstUseModalData>, public configService:ConfigService) {
        this.context = dialog.context;
    }

    reject() {
        this.clicked.emit(false);
        console.log("First start modal dismissed");
        this.dialog.dismiss();
    }

    accept() {
        this.clicked.emit(true);
        this.dialog.close();
        console.log("First start modal closed");
        this.configService.getConfig().then((config:Config)=>{
            config.firstStart = false;
            this.configService.setConfig(config);
        }).catch(Utils.handleError).catch((p)=> {
            this.alert = {msg: "Saving Config Failed, Check if server is running", type: "danger"};
            return Promise.reject(p);
        });
    }
}
