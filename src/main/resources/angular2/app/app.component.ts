import {Component, Injectable, OnInit} from "@angular/core";
import './rxjs-extensions';
import {Http, Response} from "@angular/http";
import {Observable} from 'rxjs/Rx';
import {Stats} from "./models/stats";
import {stat} from "fs";
@Component({
    selector: 'my-app',
    templateUrl: 'angular2/app/app.component.html'
})
@Injectable()
export class AppComponent implements OnInit {
    stats: Stats = new Stats("STOPPED","NA","NA");
    private url:string = "admin/stats";
    private defaultStats:Stats = new Stats("STOPPED","NA","NA");
    running:boolean = false;
    constructor(private http: Http) {}
    private getStats() {
        return Observable.interval(5000)
            .switchMap(() => this.http.get(this.url)).map((r: Response) => {
                if (r.ok) {
                    let stat = r.json() as Stats;
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
            }).subscribe(stat => this.stats = stat)
    }
    ngOnInit(){
        this.getStats();
    }
}
