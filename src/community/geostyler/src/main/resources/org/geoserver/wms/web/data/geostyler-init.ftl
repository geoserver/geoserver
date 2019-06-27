(function() {

var layerNames = '${layer}';
var styleName = '${style}';
var root = document.getElementById('geoStylerDiv');
var codeEditor = document.getElementById('style-editor');
var codeMirror = document.gsEditors.editor;

if (!root) {
    console.warn('Render target not found!');
    return;
}

var styleParser = new GeoStylerSLDParser.SldStyleParser();
var wfsParser = new GeoStylerWfsParser.WfsDataParser();
var styleUrl = window.location.origin + '/geoserver' + '${styleUrl}';
var stylePromise = fetch(styleUrl).then(r => r.text()).then(t => styleParser.readStyle(t));

var wfsParser = new GeoStylerWfsParser.WfsDataParser();
var wfsDataPromise = wfsParser.readData({
    url: window.location.origin + '/geoserver/ows',
    version: '2.0.0',
    typeName: layerNames,
    srsName: 'EPSG:4326',
    fetchParams: {
        credentials: 'same-origin'
    },
    maxFeatures: 1
});

var onChange = function(styleObj) {
    styleParser.writeStyle(styleObj).then(sld => {
        // update SLD
        // TODO: Check if editor is a codeEditor instance
        document.gsEditors.editor.setValue(sld)
    });
}

var geostylerProps;

Promise.all([stylePromise, wfsDataPromise])
    .then(function(response) {
        var geostylerStyle = React.createElement(GeoStyler.Style, geostylerProps = {
            style: response[0],
            data: response[1],
            compact: true,
            enableClassification: false,
            onStyleChange: onChange,
            showAmountColumn: false,
            showDuplicatesColumn: false
        });

        GeoStyler.locale.de_DE.GsRule.nameFieldLabel = 'GeoStyler'
        window._GeoStyler = ReactDOM.render(geostylerStyle, root);
    });

codeMirror.on('change', function() {
    console.log('onchange');
    console.log(codeMirror.getValue());
    styleParser.readStyle(codeMirror.getValue())
        .then(style => {
            var props = $.extend({}, geostylerProps);
            props.style = style;
            var geostylerStyle = React.createElement(GeoStyler.Style, props);

            GeoStyler.locale.de_DE.GsRule.nameFieldLabel = 'GeoStyler'
            window._GeoStyler = ReactDOM.render(geostylerStyle, root);
        });
});

})();
