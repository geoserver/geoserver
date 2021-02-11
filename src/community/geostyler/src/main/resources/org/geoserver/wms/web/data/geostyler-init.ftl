(function() {
  // get infos from GeoServer
  var layerNames = '${layer}';
  var layerType = '${layerType}';

  // append some divs to the DOM right above the code editor
  var root = document.createElement('div');
  var title = document.createElement('span');
  var geoStylerDiv = document.createElement('div');
  var checkboxWrapper = document.createElement('div');
  var liveUpdateCheckbox = document.createElement('input');
  var liveUpdateLabel = document.createElement('label');
  checkboxWrapper.id = 'checkboxwrapper';
  title.innerText = 'GeoStyler';
  title.id = 'geoStylerTitle';
  liveUpdateCheckbox.id = 'liveUpdate';
  liveUpdateCheckbox.type = 'checkbox';
  liveUpdateLabel.for = 'liveUpdate';
  liveUpdateLabel.innerHTML = 'Live preview enabled? ' +
    '(automatically saves the style on changes)';
  geoStylerDiv.id = 'geoStylerDiv';
  geoStylerDiv.innerHTML = 'GeoStyler is loading ...';
  root.id = 'geoStyler';

  checkboxWrapper.appendChild(liveUpdateCheckbox);
  checkboxWrapper.appendChild(liveUpdateLabel);

  root.appendChild(title);
  root.appendChild(checkboxWrapper);
  root.appendChild(geoStylerDiv);

  var codeEditor = document.getElementById('style-editor');
  var codeMirror = document.gsEditors.editor;
  document.querySelector('#styleForm').insertBefore(root, codeEditor);

  // handle GeoStyler changes and update the SLD in editor
  var styleParser = new GeoStylerSLDParser.SldStyleParser();
  var geostylerProps;
  var onChange = function(styleObj) {
    styleParser.writeStyle(styleObj).then(function(sld) {
      codeMirror.setValue(sld);
      if (liveUpdateCheckbox.checked) {
        // apply settings immediately
        document.querySelector('.form-button-apply').click();
      }
    });
  };

  // handle code editor changes and apply to GeoStyler
  codeMirror.on('change', function() {
    styleParser.readStyle(codeMirror.getValue())
      .then(function(style) {
        var props = $.extend({}, geostylerProps);
        props.style = style;
        var geostylerStyle = React.createElement(GeoStyler.Style, props);

        GeoStyler.locale.de_DE.GsRule.nameFieldLabel = 'GeoStyler';
        window._GeoStyler = ReactDOM.render(geostylerStyle, geoStylerDiv);
      });
  });

  // parse SLD if available
  var stylePromise = Promise.resolve();
  if (codeMirror.getValue().length > 0) {
    stylePromise = styleParser.readStyle(codeMirror.getValue());
  }

  // fetch a feature when working on a vector layer
  var getFeaturePromise = Promise.resolve();
  if (layerType.toLowerCase() === 'vector') {
    var wfsParser = new GeoStylerWfsParser.WfsDataParser();
    getFeaturePromise = wfsParser.readData({
      url: window.location.origin + '/geoserver/ows',
      version: '2.0.0',
      typeName: layerNames,
      srsName: 'EPSG:4326',
      fetchParams: {
        credentials: 'same-origin'
      },
      maxFeatures: 1
    });
  }

  // finally build the GeoStyler with the parsed style and feature, if available
  Promise.all([stylePromise, getFeaturePromise])
    .then(function(response) {
      var geostylerStyle = React.createElement(
        GeoStyler.Style, geostylerProps = {
          style: response[0],
          data: response[1],
          compact: true,
          enableClassification: true,
          onStyleChange: onChange,
          showAmountColumn: false,
          showDuplicatesColumn: false
        });
      GeoStyler.locale.de_DE.GsRule.nameFieldLabel = 'GeoStyler';
      window._GeoStyler = ReactDOM.render(geostylerStyle, geoStylerDiv);
    });
})();
