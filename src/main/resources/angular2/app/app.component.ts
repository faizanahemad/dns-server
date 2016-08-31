import {Component, Injectable, OnInit, ViewContainerRef, ViewChild} from "@angular/core";
import './rxjs-extensions';
import {Http, Response} from "@angular/http";
import {Observable} from 'rxjs/Rx';
import {Utils} from "./utils";
import {Config} from "./models/config";
import {Overlay} from "angular2-modal";
import {Modal} from "angular2-modal/plugins/bootstrap/index";
import {Router} from "@angular/router";
import {FirstUseModalData, FirstUseModal} from "./components/modals/first-use-modal";
import {ConfigService} from "./services/config.service";
@Component({
    selector: 'my-app',
    templateUrl: 'angular2/app/app.component.html'
})
@Injectable()
export class AppComponent implements OnInit {
    status: string = "STOPPED";
    private defaultStats:string = "STOPPED";
    running:boolean = false;
    alert: any = {};
    config:Config;
    constructor(private router: Router,private overlay: Overlay,public configService:ConfigService,private http: Http,public viewContainerRef:ViewContainerRef,public modal: Modal) {
        this.viewContainerRef = viewContainerRef;
        overlay.defaultViewContainer = viewContainerRef;
    }
    private getStatus() {
        return Observable.interval(10000)
            .switchMap(() => this.http.get(Utils.appConfig.statusUrl)).map((r: Response) => {
                if (r.ok) {
                    let stat = r.json();
                    if (stat.status!="RUNNING") {
                        this.running = false;
                    } else {
                        this.running = true;
                    }
                    return stat;
                }
                else {
                    this.running = false;
                    return this.defaultStats;
                }
            }).subscribe(stat => this.status = stat.status)
    }
    getConfig() {
        this.configService.getConfig().then((conf:Config) => {
                this.config = conf;
                if(conf.firstStart){
                    this.modal.open(FirstUseModal, new FirstUseModalData());
                }
            })
            .catch(Utils.handleError).catch((p)=> {
                this.alert = {msg: "Getting Config Failed, Check if server is running", type: "danger"};
                return Promise.reject(p);
            })
    }
    ngOnInit(){
        this.getStatus();
        this.getConfig()
    }
}
