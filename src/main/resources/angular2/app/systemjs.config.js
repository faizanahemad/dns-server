/**
 * System configuration for Angular 2 samples
 * Adjust as necessary for your application needs.
 */
(function(global) {
    // map tells the System loader where to look for things
    var map = {
        'app':                        'angular2/app', // 'dist',
        'rxjs':                       'angular2/node_modules/rxjs',
        '@angular':                   'angular2/node_modules/@angular',
        'ng2-inline-editor':          'angular2/node_modules/ng2-inline-editor/dist',
        'ng2-bootstrap':              'angular2/node_modules/ng2-bootstrap',
        'moment':                     'angular2/node_modules/moment/min',
        'angular2-modal':             'angular2/node_modules/angular2-modal'
    };
    // packages tells the System loader how to load when no filename and/or no extension
    var packages = {
        'app':                        { main: 'main.js',  defaultExtension: 'js' },
        'rxjs':                       { defaultExtension: 'js' },
        'ng2-inline-editor':          { main: 'index.js',  defaultExtension: 'js' },
        'angular2-modal':             { main: 'index.js',  defaultExtension: 'js' },
        'ng2-bootstrap':              { defaultExtension: 'js' },
        'moment':                     { defaultExtension: 'min.js' }
    };
    var ngPackageNames = [
        'common',
        'compiler',
        'core',
        'forms',
        'http',
        'platform-browser',
        'platform-browser-dynamic',
        'router',
        'upgrade'
    ];
    // Bundled (~40 requests):
    function packUmd(pkgName) {
        packages['@angular/'+pkgName] = { main: 'bundles/' + pkgName + '.umd.js', defaultExtension: 'js' };
    }
    // Most environments should use UMD; some (Karma) need the individual index files
    var setPackageConfig = System.packageWithIndex ? packIndex : packUmd;
    // Add package entries for angular packages
    ngPackageNames.forEach(setPackageConfig);
    var config = {
        map: map,
        packages: packages
    };
    System.config(config);
})(this);

System.import('app').catch(function(err){
    console.error(err);
});
