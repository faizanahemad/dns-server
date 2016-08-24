import { Pipe, PipeTransform } from '@angular/core';
@Pipe({name: 'jsonExtract'})
export class JsonKeyExtractPipe implements PipeTransform {
    transform(json: any, key: string): string {
        if (json==null) {
            return ""
        }
        return ""+json[key];
    }
}
