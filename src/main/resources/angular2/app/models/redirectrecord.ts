export class RedirectRecord {
    constructor(public requestUrl: string,
                public redirectUrl: string,
                public createdAt?: string,
                public updatedAt?: string) {
    }

}
