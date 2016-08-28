import {Http, Response} from "@angular/http";
import '../rxjs-extensions';
import {Observable, Subscription} from 'rxjs/Rx';
import {Utils} from "../utils";

export abstract class ListingComponent<T> {

    constructor(public http: Http, public countUrl:string,public listUrl:string, public newInstance:T) {
    }

    records: T[];
    newRecord:T = Object.assign({},this.newInstance);
    active = true;
    countSubscriber:Subscription;

    public count:string;

    getCount() {
        return Observable.interval(10000)
            .switchMap(() => this.http.get(this.countUrl)).map((r: Response) => {
                if (r.ok) {
                    let cnt = r.json()["count"];
                    return cnt;
                }
                else {
                    return "NA"
                }
            }).subscribe(count => this.count = count)
    }

    clearRecord() {
        this.newRecord = this.newInstance;
        this.active = false;
        setTimeout(() => this.active = true, 0);
    }

    fetchRecords() {
        this.http.get(this.listUrl)
            .toPromise()
            .then(response => this.records = response.json() as T[])
            .catch(Utils.handleError);
    }

    ngOnInit() {
        this.fetchRecords();
        this.countSubscriber = this.getCount();
    }

    ngOnDestroy() {
        this.countSubscriber.unsubscribe();
    }

    abstract addRecord()
    abstract removeRecord(record: T, index: number)
    abstract editRecord(record: T, index: number)
    abstract trackByRecords(index: number, record: T):string
}
