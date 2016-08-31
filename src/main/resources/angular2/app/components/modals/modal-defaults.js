"use strict";
function alert(modal, title, bodyHeader, bodyContent) {
    return modal.alert()
        .size('lg')
        .showClose(true)
        .title(title)
        .body("\n            <h4>" + bodyHeader + "</h4>\n            <br>\n            " + bodyContent + "\n        ");
}
exports.alert = alert;
function prompt(modal, title, bodyHeader, bodyContent) {
    return modal.prompt()
        .size('lg')
        .title(title)
        .body("\n            <h4>" + bodyHeader + "</h4>\n            <br>\n            " + bodyContent);
}
exports.prompt = prompt;
function confirm(modal, title, bodyHeader, bodyContent) {
    return modal.confirm()
        .size('lg')
        .titleHtml(title)
        .body("\n            <h4>" + bodyHeader + "</h4>\n            <br>\n            " + bodyContent);
}
exports.confirm = confirm;
function inElement(modal, title, bodyHeader, bodyContent) {
    return modal.prompt()
        .size('sm')
        .title(title)
        .inElement(true)
        .body("\n            <h4>" + bodyHeader + "</h4>\n            <br>\n            " + bodyContent + "\n         ");
}
exports.inElement = inElement;
//# sourceMappingURL=modal-defaults.js.map