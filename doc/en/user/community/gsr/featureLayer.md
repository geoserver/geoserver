# Feature Layer Examples

## Fema example

```html
<html>
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="initial-scale=1,maximum-scale=1,user-scalable=no">
    <title>FeatureLayer - 4.5</title>

    <link rel="stylesheet" href="https://js.arcgis.com/4.11/esri/css/main.css">
    <script src="https://js.arcgis.com/4.11/"></script>

    <style>
        html,
        body,
        #viewDiv {
            padding: 0;
            margin: 0;
            height: 100%;
            width: 100%;
        }
    </style>

    <script>
        require([
                "esri/Map",
                "esri/views/MapView",
                "esri/layers/FeatureLayer",
                "dojo/domReady!layers-featurelayer"
            ],
            function (Map, MapView,
                      FeatureLayer) {

                var map = new Map({});

                var view = new MapView({
                    container: "viewDiv",
                    map: map,
                    extent: { // autocasts as new Extent()
                        xmin: -107.97,
                        ymin: 25.46,
                        xmax: -91.28,
                        ymax: 37.74,
                        spatialReference: 4326
                    }
                });

                /********************
                 * Add feature layer
                 ********************/
                var featureLayer2 = new FeatureLayer({
                    url: "http://localhost:8080/geoserver/gsr/services/cite/FeatureServer/0",
                    outFields: ["*"]
                });

                map.add(featureLayer2);

            });

        require(["esri/config"], function (esriConfig) {
            esriConfig.request.corsEnabledServers.push("localhost:8080");
        });
    </script>
</head>

<body>
<div id="viewDiv"></div>
</body>

</html>

<!-- Esri®, ArcGIS® and ArcGIS Online®  are trademarks, registered trademarks, or service marks of Esri in the United States, the European Community, or certain other jurisdictions. Other companies and products mentioned may be trademarks of their respective owners.-->
```

## Point example

```html
<html>
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="initial-scale=1,maximum-scale=1,user-scalable=no">
    <title>FeatureLayer - 4.5</title>

    <link rel="stylesheet" href="https://js.arcgis.com/4.5/esri/css/main.css">
    <script src="https://js.arcgis.com/4.5/"></script>

    <style>
        html,
        body,
        #viewDiv {
            padding: 0;
            margin: 0;
            height: 100%;
            width: 100%;
        }
    </style>

    <script>
        require([
                "esri/Map",
                "esri/views/MapView",
                "esri/layers/FeatureLayer",
                "dojo/domReady!layers-featurelayer"
            ],
            function (Map, MapView,
                      FeatureLayer) {

                var map = new Map({});

                var view = new MapView({
                    container: "viewDiv",
                    map: map,

                    extent: { // autocasts as new Extent()
                        xmin: -103.8725637911543,
                        ymin: 44.37740330855979,
                        xmax: -103.63794182141925,
                        ymax: 44.48804280772808,
                        spatialReference: 4326
                    }
                });

                /********************
                 * Add feature layer
                 ********************/

                    // Carbon storage of trees in Warren Wilson College.
                var featureLayer = new FeatureLayer({
                        url: "http://localhost:8080/geoserver/gsr/services/sf/FeatureServer/0"
                    });

                map.add(featureLayer);

            });

        require(["esri/config"], function (esriConfig) {
            esriConfig.request.corsEnabledServers.push("localhost:8080");
        });
    </script>
</head>

<body>
<div id="viewDiv"></div>
</body>

</html>

<!-- Esri®, ArcGIS® and ArcGIS Online®  are trademarks, registered trademarks, or service marks of Esri in the United States, the European Community, or certain other jurisdictions. Other companies and products mentioned may be trademarks of their respective owners.-->
```

## Polygon example

```html
<html>
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="initial-scale=1,maximum-scale=1,user-scalable=no">
    <title>FeatureLayer - 4.5</title>

    <link rel="stylesheet" href="https://js.arcgis.com/4.5/esri/css/main.css">
    <script src="https://js.arcgis.com/4.5/"></script>

    <style>
        html,
        body,
        #viewDiv {
            padding: 0;
            margin: 0;
            height: 100%;
            width: 100%;
        }
    </style>

    <script>
        require([
                "esri/Map",
                "esri/views/MapView",
                "esri/layers/FeatureLayer",
                "dojo/domReady!layers-featurelayer"
            ],
            function (Map, MapView,
                      FeatureLayer) {

                var map = new Map({});

                var view = new MapView({
                    container: "viewDiv",
                    map: map,
                    extent: { // autocasts as new Extent()
                        xmin: -74.255591362951,
                        ymin: 40.4961153956091,
                        xmax: -73.7000090634675,
                        ymax: 40.915532775817,
                        spatialReference: 4326
                    }
                });

                /********************
                 * Add feature layer
                 ********************/
                var featureLayer2 = new FeatureLayer({
                    url: "http://localhost:8080/geoserver/gsr/services/cite/FeatureServer/0",
                    outFields: ["*"]
                });

                map.add(featureLayer2);

            });

        require(["esri/config"], function (esriConfig) {
            esriConfig.request.corsEnabledServers.push("localhost:8080");
        });
    </script>
</head>

<body>
<div id="viewDiv"></div>
</body>

</html>

<!-- Esri®, ArcGIS® and ArcGIS Online®  are trademarks, registered trademarks, or service marks of Esri in the United States, the European Community, or certain other jurisdictions. Other companies and products mentioned may be trademarks of their respective owners.-->
```

## Polygon non cors example

```html
<html>
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="initial-scale=1,maximum-scale=1,user-scalable=no">
    <title>FeatureLayer - 4.5</title>

    <link rel="stylesheet" href="https://js.arcgis.com/4.5/esri/css/main.css">
    <script src="https://js.arcgis.com/4.5/"></script>

    <style>
        html,
        body,
        #viewDiv {
            padding: 0;
            margin: 0;
            height: 100%;
            width: 100%;
        }
    </style>

    <script>
        require([
                "esri/Map",
                "esri/views/MapView",
                "esri/layers/FeatureLayer",
                "dojo/domReady!layers-featurelayer"
            ],
            function (Map, MapView,
                      FeatureLayer) {

                var map = new Map({});

                var view = new MapView({
                    container: "viewDiv",
                    map: map,
                    extent: { // autocasts as new Extent()
                        xmin: -74.255591362951,
                        ymin: 40.4961153956091,
                        xmax: -73.7000090634675,
                        ymax: 40.915532775817,
                        spatialReference: 4326
                    }
                });

                /********************
                 * Add feature layer
                 ********************/

                var featureLayer2 = new FeatureLayer({
                    url: "http://localhost:9191/geoserver/gsr/services/cite/FeatureServer/0",
                    outFields: ["*"]
                });

                map.add(featureLayer2);

            });
    </script>
</head>

<body>
<div id="viewDiv"></div>
</body>

</html>

<!-- Esri®, ArcGIS® and ArcGIS Online®  are trademarks, registered trademarks, or service marks of Esri in the United States, the European Community, or certain other jurisdictions. Other companies and products mentioned may be trademarks of their respective owners.-->
```
