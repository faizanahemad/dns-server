import {Component, Injectable, OnInit} from "@angular/core";
import './rxjs-extensions';
import {Http, Response} from "@angular/http";
import {Observable} from 'rxjs/Rx';
@Component({
    selector: 'my-app',
    templateUrl: 'angular2/app/app.component.html'
})
@Injectable()
export class AppComponent implements OnInit {
    stats: Observable<Response>;
    private url:string = "admin/stats";
    constructor(private http: Http) {}
    private getStats(): Observable<Response> {
        return Observable.interval(5000)
            .switchMap(() => this.http.get(this.url)).map((r: Response) => r.json());
    }
    ngOnInit(){
        this.stats = this.getStats();
    }
}
