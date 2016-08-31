import {
    OneButtonPresetBuilder, Modal,
    TwoButtonPresetBuilder
} from "angular2-modal/plugins/bootstrap/index";
export function alert(modal: Modal, title:string, bodyHeader:string, bodyContent:string): OneButtonPresetBuilder {
    return modal.alert()
        .size('lg')
        .showClose(true)
        .title(title)
        .body(`
            <h4>${bodyHeader}</h4>
            <br>
            ${bodyContent}
        `);
}

export function prompt(modal: Modal, title:string, bodyHeader:string, bodyContent:string): OneButtonPresetBuilder {
    return modal.prompt()
        .size('lg')
        .title(title)
        .body(`
            <h4>${bodyHeader}</h4>
            <br>
            ${bodyContent}`);
}

export function confirm(modal: Modal, title:string, bodyHeader:string, bodyContent:string): TwoButtonPresetBuilder {
    return modal.confirm()
        .size('lg')
        .titleHtml(title)
        .body(`
            <h4>${bodyHeader}</h4>
            <br>
            ${bodyContent}`);
}

export function inElement(modal: Modal, title:string, bodyHeader:string, bodyContent:string) {
    return modal.prompt()
        .size('sm')
        .title(title)
        .inElement(true)
        .body(`
            <h4>${bodyHeader}</h4>
            <br>
            ${bodyContent}
         `);
}
