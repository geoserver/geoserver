window.addEventListener('load', function() {
    var urlParams = new URLSearchParams(window.location.search);
    var i3s_url = "";

    require([
        "esri/Map",
        "esri/views/SceneView",
        "esri/layers/SceneLayer",
        "esri/support/actions/ActionButton"
    ], function (Map, SceneView, SceneLayer, ActionButton) {
        // Create Map
        var map = new Map({
            basemap: "dark-gray",
            ground: "world-elevation"
        });

        // Create the SceneView
        var view = new SceneView({
            container: "viewDiv",
            map: map
        });

        // Create SceneLayer and add to the map
        var sceneLayer;
        if (urlParams.has('i3s_resource_url')) {
            i3s_url = urlParams.get('i3s_resource_url');
            sceneLayer = new SceneLayer({
                url: i3s_url,
                popupEnabled: true
            });
            map.add(sceneLayer);
            sceneLayer.when(function () {
                view.goTo(sceneLayer.fullExtent);
            });
        } else {
            alert("Mandatory parameter i3s_resource_url is missing");
        }
        map.add(sceneLayer);
        // Create MeshSymbol3D for symbolizing SceneLayer
        var symbol = {
            type: "mesh-3d", 
            symbolLayers: [{
                type: "fill"
            }]
        };
        // Add the renderer to sceneLayer
        sceneLayer.renderer = {
            type: "simple", // autocasts as new SimpleRenderer()
            symbol: symbol
        };
        view.ui.add("paneDiv", "top-right");
    });
});