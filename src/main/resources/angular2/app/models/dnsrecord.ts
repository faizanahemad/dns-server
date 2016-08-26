export class DnsRecord {
    constructor(public domain: string,
                public dns: string,
                public recordType: string,
                public createdAt?: string,
                public updatedAt?: string) {
    }

}
