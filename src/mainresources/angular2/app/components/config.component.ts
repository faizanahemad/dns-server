import {Component, OnInit, Injectable, OnChanges} from "@angular/core";
import {Http, Response} from "@angular/http";
import {Config, ApplicationConfig, DBConfig, DNSConfig} from "../models/config";
import "../rxjs-extensions";
import {Observable} from "rxjs/Rx";
import {Utils} from "../utils";
import {ConfigService} from "../services/config.service";
@Component({
    selector: 'config-editor',
    templateUrl: 'angular2/app/components/config.component.html'
})
@Injectable()
export class ConfigComponent implements OnInit,OnChanges {
    ngOnInit(): void {
        this.getConfig()
    }

    ngOnChanges(): void {
        this.getConfig()
    }

    constructor(private configService:ConfigService) {
    }

    processingGlyph: string = "";
    config: Config = new Config(new  ApplicationConfig("","","JSON"),new DBConfig("","","",""),new DNSConfig("","",0,0),true);
    alert: any = {};

    private configCallHandler(configPromise:Promise<Config>) {
        configPromise.then(conf => this.config = conf)
            .catch(Utils.handleError).catch((p)=> {
            this.alert = {msg: "Getting Config Failed, Check if server is running", type: "danger"};
            return Promise.reject(p);
        })
    }

    getConfig() {
        this.configCallHandler(this.configService.getConfig())
    }

    saveConfig() {
        this.processingGlyph = " fa-spin fa-spinner";
        this.config.firstStart = false;
        this.configService.setConfig(this.config).then((r:Response)=>Promise.resolve(r.ok))
            .then((o: boolean)=> {
                if (o) {
                    this.processingGlyph = "fa-check";
                } else {
                    this.processingGlyph = "fa-times";
                }
            }).catch(Utils.handleError).catch((p)=> {
                this.alert = {msg: "Saving Config Failed, Check if server is running", type: "danger"};
                return Promise.reject(p);
            });
        Observable.of("").delay(5000).subscribe((o: string)=> this.processingGlyph = o);
    }

    resetConfig() {
        this.configCallHandler(this.configService.getConfig())
    }

    defaultConfig() {
        this.configService.getDefaultConfig();
        this.config.firstStart = false;
        this.saveConfig()
    }


}
