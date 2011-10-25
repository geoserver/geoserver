var map = window.olMaps["${id}"];
for (var i = 0; i < map.layers.length; i++) {
  var layer = map.layers[i];
  if (layer.mergeNewParams) layer.mergeNewParams({random: ${cachebuster}});
}
