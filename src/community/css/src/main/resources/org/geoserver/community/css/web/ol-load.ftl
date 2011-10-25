OpenLayers.DOTS_PER_INCH= 25.4 / 0.28;
OpenLayers.ImgPath = "../www/openlayers/img/";

var cfg = {
  maxExtent: new OpenLayers.Bounds(${minx}, ${miny}, ${maxx}, ${maxy}),
  maxResolution: ${resolution},
  controls: [
    new OpenLayers.Control.PanZoomBar(),
    new OpenLayers.Control.Scale(),
    new OpenLayers.Control.Navigation()
  ]
};

var map = new OpenLayers.Map("${id}", cfg);
map.addLayer(new OpenLayers.Layer.WMS("GeoServer WMS", "../wms",
    {
      layers: "${layer}",
      styles: "${style}",
      format: "image/png",
      random: ${cachebuster}
    }, {
      singleTile: true
    }
  )
);

map.zoomToMaxExtent();
window.olMaps = window.olMaps || {};
window.olMaps["${id}"] = map;
