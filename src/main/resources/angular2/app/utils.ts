export class Utils {
    public static handleError(error: any): Promise<any> {
        console.error('An error occurred', error);
        return Promise.reject(error.message || error);
    }
    static appConfig:any = {
        configUrl:"/server/config/",
        defaultConfigUrl:"/server/config/default/",
        statusUrl: "admin/status"
    }
}
