import { Component,OnInit,Output, EventEmitter } from '@angular/core';
@Component({
    selector: 'config-editor',
    templateUrl: 'angular2/app/components/config.component.html'
})

export class ConfigComponent implements OnInit{
    @Output() cssUpdated = new EventEmitter<string>();
    @Output() error = new EventEmitter<string>();

    ngOnInit(){
        }

    onInlineClick(){
    }
}
