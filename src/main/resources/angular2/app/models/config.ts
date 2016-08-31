export class Config {
    constructor(public application: ApplicationConfig,
                public dbConf: DBConfig,
                public dnsConf: DNSConfig,
                public firstStart:boolean) {
    }

}

export class DBConfig {
    constructor(public user: string,
                public password: string,
                public url: string,
                public dBName: string) {
    }

}

export class DNSConfig {
    constructor(public dnsResolver: string,
                public dnsResolverSecondLevel: string,
                public maxEntries: number,
                public entryExpiryTime: number) {
    }

}

export class ApplicationConfig {
    constructor(public dnsJsonFile: string,
                public urlShortnerJsonFile: string,
                public storageMedium: string) {
    }

}
