import {Component, Injectable, OnInit} from "@angular/core";
import {Http, Response} from "@angular/http";
import {DnsRecord} from "../models/dnsrecord";
import {Utils} from "../utils";
@Component({
    selector: 'listing-editor',
    templateUrl: 'angular2/app/components/listing.component.html',
    styleUrls: ['angular2/app/styles/listing.component.css']
})

@Injectable()
export class ListingComponent implements OnInit {

    constructor(private http: Http) {
    }

    records: DnsRecord[];
    newRecord:DnsRecord = new DnsRecord("","","A","","");
    active = true;
    private listUrl: string = "/list";

    addRecord() {
        let payload: any = {};
        payload[this.newRecord.domain] = this.newRecord;
        this.http.put(this.listUrl,payload)
            .map((r: Response) => r.ok)
            .toPromise()
            .then(response => {
                if (response) {
                    this.clearRecord();
                    this.fetchRecords();
                }
            }).catch(Utils.handleError);
    }

    clearRecord() {
        this.newRecord = new DnsRecord("","","A","","");
        this.active = false;
        setTimeout(() => this.active = true, 0);
    }

    fetchRecords() {
        this.http.get(this.listUrl)
            .toPromise()
            .then(response => this.records = response.json() as DnsRecord[])
            .catch(Utils.handleError);
    }

    removeRecord(record: DnsRecord, index: number) {
        this.http.delete(this.listUrl+"/"+record.domain)
            .map((r: Response) => r.ok)
            .toPromise()
            .then(response => {
                if (response) {
                    this.records.splice(index, 1);
                }
            }).catch(Utils.handleError);

    }

    editRecord(record: DnsRecord, index: number) {
        var payload: any = {};
        payload[record.domain] = record;
        this.http.put(this.listUrl,payload)
            .map((r: Response) => r.ok)
            .toPromise()
            .then(response => {
                if (response) {
                    this.records[index] = record;
                }
            }).catch(Utils.handleError);
    }

    trackByRecords(index: number, record: DnsRecord) { return record.domain; }

    ngOnInit() {
        this.fetchRecords()
    }
}
