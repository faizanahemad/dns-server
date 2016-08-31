import { Injectable } from '@angular/core';
import {Config} from "../models/config";
import {Response, Http} from "@angular/http";
import {Utils} from "../utils";

@Injectable()
export class ConfigService {
    constructor(private http:Http){}
    getConfig():Promise<Config> {
        return this._getConfig(Utils.appConfig.configUrl)
    }

    getDefaultConfig():Promise<Config> {
        return this._getConfig(Utils.appConfig.defaultConfigUrl);
    }

    setConfig(config:Config):Promise<Response> {
        return this.http.post(Utils.appConfig.configUrl, config)
            .toPromise()
            .catch(Utils.handleError)
    }

    private _getConfig(url: string):Promise<Config> {
        return this.http.get(url).map((r: Response)=> r.json() as Config)
            .toPromise()
            .catch(Utils.handleError)
    }
}
