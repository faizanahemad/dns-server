import {Component, Injectable, OnInit} from "@angular/core";
import {Http, Response} from "@angular/http";
import {DnsRecord} from "../models/dnsrecord";
import {Utils} from "../utils";
import {RedirectRecord} from "../models/redirectrecord";
import {ListingComponent} from "./listing.component";
@Component({
    selector: 'redirect-listing-editor',
    templateUrl: 'angular2/app/components/redirect-listing.component.html',
    styleUrls: ['angular2/app/styles/listing.component.css']
})

@Injectable()
export class RedirectListingComponent extends ListingComponent<RedirectRecord> implements OnInit {

    constructor(public http: Http) {
        super(http,"/server/list/redirect/count","/server/list/redirect",new RedirectRecord("","","",""))
    }

    alert:any = {};
    records: RedirectRecord[];
    newRecord:RedirectRecord = new RedirectRecord("","","","");
    active = true;
    listUrl: string = "/server/list/redirect";

    addRecord() {
        let payload: any = {};
        payload[this.newRecord.requestUrl] = this.newRecord;
        this.http.put(this.listUrl,payload)
            .map((r: Response) => r.ok)
            .toPromise()
            .then(response => {
                if (response) {
                    this.clearRecord();
                    this.fetchRecords();
                }
            }).catch(Utils.handleError).catch((p)=> {
                this.alert = {msg: "Adding record failed, Check if server is running", type: "danger"};
                return Promise.reject(p);
            });;
    }

    removeRecord(record: RedirectRecord, index: number) {
        this.http.delete(this.listUrl+"/"+record.requestUrl)
            .map((r: Response) => r.ok)
            .toPromise()
            .then(response => {
                if (response) {
                    this.records.splice(index, 1);
                }
            }).catch(Utils.handleError).catch((p)=> {
                this.alert = {msg: "Removing Record Failed, Check if server is running", type: "danger"};
                return Promise.reject(p);
            });;

    }

    editRecord(record: RedirectRecord, index: number) {
        var payload: any = {};
        payload[record.requestUrl] = record;
        this.http.put(this.listUrl,payload)
            .map((r: Response) => r.ok)
            .toPromise()
            .then(response => {
                if (response) {
                    this.records[index] = record;
                }
            }).catch(Utils.handleError).catch((p)=> {
                this.alert = {msg: "Editing Record Failed, Check if server is running", type: "danger"};
                return Promise.reject(p);
            });;
    }

    trackByRecords(index: number, record: RedirectRecord) { return record.requestUrl; }
}
