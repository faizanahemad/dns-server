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
    status: Observable<string> = Observable.of("STOPPED");
    storedCount:Observable<string> = Observable.of("0");
    cachedCount:Observable<string> = Observable.of("0");
    private stats: Observable<Response>;
    private url:string = "admin/stats";
    constructor(private http: Http) {}
    private getStats(): Observable<Response> {
        return Observable.interval(5000)
            .switchMap(() => this.http.get(this.url))
    }
    private getStatus(): Observable<string> {
        return this.stats.map((r: Response) => r.json().status as string);
    }
    private getCachedCount(): Observable<string> {
        return this.stats.map((r: Response) => r.json().cache as string);
    }
    private getStoredCount(): Observable<string> {
        return this.stats.map((r: Response) => r.json().store as string);
    }
    ngOnInit(){
        this.stats = this.getStats();
        this.status = this.getStatus();
        this.cachedCount = this.getCachedCount();
        this.storedCount = this.getStoredCount();
    }
}
