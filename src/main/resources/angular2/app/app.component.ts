import {Component, Injectable, OnInit} from "@angular/core";
import './rxjs-extensions';
import {Http, Response} from "@angular/http";
import { Observable } from 'rxjs';
@Component({
    selector: 'my-app',
    templateUrl: 'angular2/app/app.component.html'
})
@Injectable()
export class AppComponent implements OnInit {
    status: Observable<string>;
    totalEntries:string;
    cachedEntries:string;
    private url:string = "admin/status";
    constructor(private http: Http) {}
    getStatus(): Observable<string> {
        return Observable.interval(2000)
            .switchMap(() => this.http.get(this.url))
            .map((r: Response) => r.json().status as string);
    }
    ngOnInit(){
        this.status = this.getStatus();
    }
}
