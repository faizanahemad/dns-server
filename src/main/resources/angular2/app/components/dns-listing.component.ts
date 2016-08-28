import {Component, Injectable, OnInit} from "@angular/core";
import {Http, Response} from "@angular/http";
import {DnsRecord} from "../models/dnsrecord";
import {Utils} from "../utils";
import {ListingComponent} from "./listing.component";
@Component({
    selector: 'dns-listing-editor',
    templateUrl: 'angular2/app/components/dns-listing.component.html',
    styleUrls: ['angular2/app/styles/listing.component.css']
})

@Injectable()
export class DnsListingComponent extends ListingComponent<DnsRecord> implements OnInit{

    constructor(public http: Http) {
        super(http,"/server/list/dns/count","/server/list/dns",new DnsRecord("","","A","",""))
    }

    records: DnsRecord[];
    newRecord:DnsRecord = new DnsRecord("","","A","","");
    active = true;
    listUrl: string = "/server/list/dns";

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
}
