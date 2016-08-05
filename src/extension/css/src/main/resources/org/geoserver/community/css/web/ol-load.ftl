OpenLayers.DOTS_PER_INCH = 25.4 / 0.28;
OpenLayers.ImgPath = "${baseUrl}/www/openlayers/img/";

var cfg = {
  maxExtent: new OpenLayers.Bounds(${minx?c}, ${miny?c}, ${maxx?c}, ${maxy?c}),
  maxResolution: ${resolution?c},
  controls: [
    new OpenLayers.Control.PanZoomBar(),
    new OpenLayers.Control.Scale(),
    new OpenLayers.Control.Navigation()
  ]
};

var map = new OpenLayers.Map("${id}", cfg);
<#if styleWorkspace??>
map.addLayer(new OpenLayers.Layer.WMS("GeoServer WMS", "${baseUrl}/${styleWorkspace}/wms",
<#else>
map.addLayer(new OpenLayers.Layer.WMS("GeoServer WMS", "${baseUrl}/wms",
</#if>
    {
      layers: "${layer}",
      styles: "${style}",
      format: "image/png",
      format_options: "layout:css-legend;fontAntiAliasing:true",
      random: ${cachebuster?c}
    }, {
      singleTile: true,
      ratio: 1
    }
  )
);

map.zoomToMaxExtent();
window.olMaps = window.olMaps || {};
window.olMaps["${id}"] = map;
if (!window.olUpdate) {
    window.olUpdate = function(id, params) {
        var map = window.olMaps[id];
        for (var i = 0; i < map.layers.length; i++) {
            var layer = map.layers[i];
            if (layer.mergeNewParams) layer.mergeNewParams(params);
        }
    };
}
