"use strict";
var __extends = (this && this.__extends) || function (d, b) {
    for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p];
    function __() { this.constructor = d; }
    d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
};
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};
var core_1 = require('@angular/core');
var angular2_modal_1 = require('angular2-modal');
var index_1 = require('angular2-modal/plugins/bootstrap/index');
var config_service_1 = require("../../services/config.service");
var utils_1 = require("../../utils");
var FirstUseModalData = (function (_super) {
    __extends(FirstUseModalData, _super);
    function FirstUseModalData() {
        _super.call(this);
        this.context = new angular2_modal_1.OverlayContext;
        this.context.inElement = false;
        this.context.isBlocking = true;
    }
    return FirstUseModalData;
}(index_1.BSModalContext));
exports.FirstUseModalData = FirstUseModalData;
var FirstUseModal = (function () {
    function FirstUseModal(dialog, configService) {
        this.dialog = dialog;
        this.configService = configService;
        this.clicked = new core_1.EventEmitter();
        this.alert = {};
        this.context = dialog.context;
    }
    FirstUseModal.prototype.reject = function () {
        this.clicked.emit(false);
        console.log("First start modal dismissed");
        this.dialog.dismiss();
    };
    FirstUseModal.prototype.accept = function () {
        var _this = this;
        this.clicked.emit(true);
        this.dialog.close();
        console.log("First start modal closed");
        this.configService.getConfig().then(function (config) {
            config.firstStart = false;
            _this.configService.setConfig(config);
        }).catch(utils_1.Utils.handleError).catch(function (p) {
            _this.alert = { msg: "Saving Config Failed, Check if server is running", type: "danger" };
            return Promise.reject(p);
        });
    };
    __decorate([
        core_1.Output(), 
        __metadata('design:type', core_1.EventEmitter)
    ], FirstUseModal.prototype, "clicked", void 0);
    FirstUseModal = __decorate([
        core_1.Component({
            selector: 'first-use-modal',
            styles: ["\n        bs-modal-container > div {\n            width: 600px;\n            margin: 30px auto;\n        }\n        .modal-content {\n            display: block;\n            width: 600px;\n            margin: 30px auto;\n        }\n    "],
            template: "\n        \n        \n            <div class=\"modal-header\">\n            \n                    <button type=\"button\" class=\"close\" (click)=\"reject()\"><span>&times;</span></button>\n                \n                    <h4 class=\"modal-title\">Using for the first time? Read below...</h4>\n                \n            </div>\n            <div class=\"modal-body\">\n                <ul>\n                \n                    <li>Is MySql installed then configure it for higher performance. Go to <a routerLink=\"/settings\" (click)=\"accept()\"><i class=\"fa fa-cog\"></i> Settings</a> for that.</li>\n                    <li>For Guide on usage and How Tos go to <a routerLink=\"/help\" (click)=\"accept()\"><i class=\"fa fa-question-circle-o\"></i> Help</a>.</li>\n                    <li>For other configuration options look at <a routerLink=\"/settings\" (click)=\"accept()\"><i class=\"fa fa-cog\"></i> Settings</a> as well.</li>\n                </ul>\n            </div>\n            <div class=\"modal-footer\">\n                \n                    <button class=\"btn btn-success\" (click)=\"accept()\" routerLink=\"/settings\"><i class=\"fa fa-cog\"></i> Configure</button>\n                    <button class=\"btn btn-primary\" (click)=\"accept()\" routerLink=\"/help\"><i class=\"fa fa-question-circle-o\"></i> Help</button>\n                    <button class=\"btn btn-default\" (click)=\"reject()\">Close</button>\n                \n            </div>\n        \n        "
        }), 
        __metadata('design:paramtypes', [angular2_modal_1.DialogRef, config_service_1.ConfigService])
    ], FirstUseModal);
    return FirstUseModal;
}());
exports.FirstUseModal = FirstUseModal;
//# sourceMappingURL=first-use-modal.js.map