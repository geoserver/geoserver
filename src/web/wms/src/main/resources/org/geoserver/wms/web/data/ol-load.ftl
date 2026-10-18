
var source = new ol.source.ImageWMS({
<#if styleWorkspace??>
  url: "${baseUrl?js_string}/${styleWorkspace?js_string}/wms",
<#else>
  url: "${baseUrl?js_string}/wms",
</#if>
  params: {
<#if previewStyleGroup>
    'SLD': '${styleUrl?js_string}',
<#else>
    'LAYERS': '${layer?js_string}',
    'STYLES': '${style?js_string}',
</#if>
    'FORMAT': 'image/png',
    'FORMAT_OPTIONS': "layout:style-editor-legend;fontAntiAliasing:true",
    'RANDOM': ${cachebuster?c},
    'LEGEND_OPTIONS': 'forceLabels:on;fontAntiAliasing:true',
    'EXCEPTIONS': 'application/vnd.ogc.se_inimage'
  },
  serverType: 'geoserver',
  ratio: 1
});

var extent = [${minx?c}, ${miny?c}, ${maxx?c}, ${maxy?c}];
var scaleControl = document.createElement('div');
scaleControl.className = 'ol-scale ol-control ol-unselectable'

var map = new ol.Map({
  layers: [new ol.layer.Image({source: source})],
  target: '${id?js_string}',
  controls: ol.control.defaults({attribution: false}).extend([
    new ol.control.Control({element: scaleControl})
  ]),
  view: new ol.View({
    zoom: 2,
    projection: "EPSG:4326",
    extent: extent
  })
});

map.getView().on('change:resolution', function(evt) {
  var res = evt.target.getResolution();
  var units = map.getView().getProjection().getUnits();
  var dpi = 25.4 / 0.28;
  var mpu = ol.proj.METERS_PER_UNIT[units];
  var scale = Math.round(res * mpu * 39.37 * dpi);
  scaleControl.innerHTML =  'Scale = 1 : ' + scale.toLocaleString();
});

map.getView().fit(extent, map.getSize());

window.olMap = map;

window.olUpdate = function(id, params) {
  source.updateParams(params);
};

window.resizeStylePage();
