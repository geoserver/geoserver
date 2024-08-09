<!DOCTYPE html>
<html>

<head>
    <meta charset="utf-8" />
    <meta name="viewport" content="initial-scale=1,maximum-scale=1,user-scalable=no" />
    <title>i3s client</title>
    <style>
        html,
        body,
        #viewDiv {
            padding: 0;
            margin: 0;
            height: 100%;
            width: 100%;
        }

        #paneDiv {
            padding: 10px;
            max-width: 200px;
            background-color: rgba(255, 255, 255, 0.8);
            font-size: 1.1em;
        }
    </style>
    <link rel="icon" type="image/ico" href="img/favicon.ico" sizes="16x16">

    <link rel="stylesheet" href="https://js.arcgis.com/4.14/esri/themes/light/main.css" />
    <script src="https://js.arcgis.com/4.14/"></script>
    <script src="${baseURL}webresources/ogcapi/3d/i3s.js" type="text/javascript"></script>
</head>

<body>
    <div id="viewDiv"></div>
    <div id="paneDiv" class="esri-widget">
        <h1 style="line-height: 1em;">i3s client</h1>
        <a class="esri-button" href="../" style="text-decoration: none; ">Back</a>
    </div>
</body>

</html>