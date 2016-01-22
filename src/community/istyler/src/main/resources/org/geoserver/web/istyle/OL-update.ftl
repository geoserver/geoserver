var map = window.olMaps["${markupId}"];

if (${layerChanged?string}) {
    map.removeLayer(map.layers[0]);
    map.maxExtent = new OpenLayers.Bounds(${minX?c},${minY?c},${maxX?c},${maxY?c});
    map.maxResolution = ${res?c}
    map.projection = "${srs}"
    map.addLayer(new OpenLayers.Layer.WMS("GeoServer WMS", "${geoserver}/wms", { 
       layers: "${layers}",
       styles: "${styles}",
       srs: "${srs}", 
       random: "${ran}" 
    }, 
    {singleTile: true, ratio: 1}));
    map.zoomToMaxExtent();
}
else {
    var params = { 
        layers: "${layers}",
        styles: "${styles}",
        random: "${ran}"
    };
    map.layers[0].mergeNewParams(params);
}
