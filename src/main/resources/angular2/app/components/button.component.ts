import {Component, Input, Injectable} from "@angular/core";
import {Http, Response} from "@angular/http";
import '../rxjs-extensions';
import {Observable} from 'rxjs/Rx';
@Component({
    selector: 'api-button',
    template: `
    <div class="btn-group btn-group-justified" role="group">
        <a type="button" (click)="action()" class="btn btn-{{btnType}}">
            <b>
                <span class="glyphicon glyphicon-{{glyphType}} pull-left"></span>{{btnText}}<i class="fa {{processingGlyph}} pull-right"></i>
            </b>
        </a>
    </div>
  `
})
@Injectable()
export class ButtonComponent {

    constructor(private http: Http){}

    processingGlyph:string="";

    @Input()
    btnType;

    @Input()
    glyphType;

    @Input()
    btnText;

    @Input()
    url;

    action() {
        let promise = this.http.put(this.url,"{}").map((r: Response) => r.ok);
        this.processingGlyph = " fa-spin fa-spinner";
        promise.subscribe((o:boolean)=> {
            if (o) {
                this.processingGlyph = "fa-check";
            } else {
                this.processingGlyph = "fa-times";
            }
        });
        Observable.of("").delay(5000).subscribe((o:string)=> this.processingGlyph = o);

    }
}
