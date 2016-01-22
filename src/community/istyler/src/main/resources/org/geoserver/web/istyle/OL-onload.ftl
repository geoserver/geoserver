OpenLayers.DOTS_PER_INCH= 25.4 / 0.28;

var cfg = {
  maxExtent: new OpenLayers.Bounds(${minX}, ${minY}, ${maxX}, ${maxY}),
  maxResolution: ${res}
}

var map = new OpenLayers.Map("${markupId}", cfg);
map.addLayer(new OpenLayers.Layer.WMS("GeoServer WMS", "${geoserver}/wms", { 
     layers: "${layers}",
     styles: "${styles}",
     format: "image/png",
     srs: "${srs}", 
     random: "${ran}" 
  }, 
  {singleTile: true, ratio: 1}));

map.zoomToMaxExtent();
window.olMaps = window.olMaps || {};
window.olMaps["${markupId}"] = map;