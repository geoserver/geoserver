window.addEventListener('load', function() {
  var urlParams = new URLSearchParams(window.location.search);
  var url_3dtiles_json = "";
  var configuration =  {
        baseLayerPicker: false,
        vrButton: false,
        geocoder: false,
        navigationHelpButton: false,
        selectionIndicator: false,
        shadows: true,
        timeline: true,
        sceneModePicker: false,
        imageryProvider: new Cesium.OpenStreetMapImageryProvider({
            url: 'https://a.tile.openstreetmap.org/'
  })};
  // check if we can enable the terrain provider
  var cesiumAccessToken = document.getElementById('cesiumAccessToken').value;
  if (typeof cesiumAccessToken === "string" && cesiumAccessToken.length > 0) {
    Cesium.Ion.defaultAccessToken = cesiumAccessToken;
    configuration.terrainProvider = Cesium.createWorldTerrain();
  }
  // create the viewer
  var viewer = new Cesium.Viewer('cesiumContainer', configuration);
  viewer.scene.postProcessStages.fxaa.enabled = false;
  viewer.scene.globe.maximumScreenSpaceError = 1.5;

  if (urlParams.has('url_3dtiles_json')) {
      url_3dtiles_json = urlParams.get('url_3dtiles_json');
      var tileset = viewer.scene.primitives.add(new Cesium.Cesium3DTileset({
          url: url_3dtiles_json
      }))
      Cesium.when(tileset.readyPromise).then(function (tileset) {
          viewer.flyTo(tileset)
      })
  } else {
    alert("Mandatory parameter url_3dtiles_json is missing");
  }
});