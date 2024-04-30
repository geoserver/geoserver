<!doctype html>
<html lang="en">
  <head>
    <meta charset="UTF-8">
    <link rel="stylesheet" href="${resourceLink("openlayers3/ol.css")}" type="text/css" media="screen" />
    <style>
        .ol-zoom {
          top: 52px;
        }
        .ol-toggle-options {
          z-index: 1000;
          background: rgba(255,255,255,0.4);
          border-radius: 4px;
          padding: 2px;
          position: absolute;
          left: 8px;
          top: 8px;
        }
        #updateFilterButton, #resetFilterButton {
          height: 22px;
          width: 22px;
          text-align: center;
          text-decoration: none !important;
          line-height: 22px;
          margin: 1px;
          font-family: 'Lucida Grande',Verdana,Geneva,Lucida,Arial,Helvetica,sans-serif;
          font-weight: bold !important;
          background: rgba(0,60,136,0.5);
          color: white !important;
          padding: 2px;
        }
        .ol-toggle-options a {
          background: rgba(0,60,136,0.5);
          color: white;
          display: block;
          font-family: 'Lucida Grande',Verdana,Geneva,Lucida,Arial,Helvetica,sans-serif;
          font-size: 19px;
          font-weight: bold;
          height: 22px;
          line-height: 11px;
          margin: 1px;
          padding: 0;
          text-align: center;
          text-decoration: none;
          width: 22px;
          border-radius: 2px;
        }
        .ol-toggle-options a:hover {
          color: #fff;
          text-decoration: none;
          background: rgba(0,60,136,0.7);
        }
        body {
            font-family: Verdana, Geneva, Arial, Helvetica, sans-serif;
            font-size: small;
        }
        iframe {
            width: 100%;
            height: 250px;
            border: none;
        }
        /* Toolbar styles */
        #toolbar {
            position: relative;
            padding-bottom: 0.5em;
        }
        #toolbar ul {
            list-style: none;
            padding: 0;
            margin: 0;
        }
        #toolbar ul li {
            float: left;
            padding-right: 1em;
            padding-bottom: 0.5em;
        }
        #toolbar ul li a {
            font-weight: bold;
            font-size: smaller;
            vertical-align: middle;
            color: black;
            text-decoration: none;
        }
        #toolbar ul li a:hover {
            text-decoration: underline;
        }
        #toolbar ul li * {
            vertical-align: middle;
        }
        #map {
            clear: both;
            position: relative;
            width: ${model.width?c}px;
            height: ${model.height?c}px;
            border: 1px solid black;
        }
        #wrapper {
            width: ${model.width?c}px;
        }
        #location {
            float: right;
        }
        /* Styles used by the default GetFeatureInfo output, added to make IE happy */
        table.featureInfo, table.featureInfo td, table.featureInfo th {
            border: 1px solid #ddd;
            border-collapse: collapse;
            margin: 0;
            padding: 0;
            font-size: 90%;
            padding: .2em .1em;
        }
        table.featureInfo th {
            padding: .2em .2em;
            font-weight: bold;
            background: #eee;
        }
        table.featureInfo td {
            background: #fff;
        }
        table.featureInfo tr.odd td {
            background: #eee;
        }
        table.featureInfo caption {
            text-align: left;
            font-size: 100%;
            font-weight: bold;
            padding: .2em .2em;
        }
    </style>
    
    <script src="${resourceLink("openlayers3/ol.js")}" type="text/javascript"></script>
    <script src="${resourceLink("webresources/ogcapi/maps.js")}" type="text/javascript"></script>
    <title>OpenLayers map preview</title>
  </head>
  <body>
    <div id="toolbar" class="d-none">
      <ul>
        <li>
          <a>Tiling:</a>
          <select id="tilingModeSelector">
            <option value="untiled">Single tile</option>
            <option value="tiled">Tiled</option>
          </select>
        </li>
        <li>
          <a>Antialias:</a>
          <select id="antialiasSelector">
            <option value="full">Full</option>
            <option value="text">Text only</option>
            <option value="none">Disabled</option>
          </select>
        </li>
        <li>
          <a>Format:</a>
          <select id="imageFormatSelector">
            <option value="image/png">PNG 24bit</option>
            <option value="image/png8">PNG 8bit</option>
            <option value="image/gif">GIF</option>
            <option id="jpeg" value="image/jpeg">JPEG</option>
            <option id="jpeg-png" value="image/vnd.jpeg-png">JPEG-PNG</option>
            <option id="jpeg-png8" value="image/vnd.jpeg-png8">JPEG-PNG8</option>
          </select>
        </li>
        <li>
          <a>Width/Height:</a>
          <select id="widthSelector">
             <!--
             These values come from a statistics of the viewable area given a certain screen area
             (but have been adapted a litte, simplified numbers, added some resolutions for wide screen)
             You can find them here: http://www.evolt.org/article/Real_World_Browser_Size_Stats_Part_II/20/2297/
             --><option value="auto">Auto</option>
                <option value="600">600</option>
                <option value="750">750</option>
                <option value="950">950</option>
                <option value="1000">1000</option>
                <option value="1200">1200</option>
                <option value="1400">1400</option>
                <option value="1600">1600</option>
                <option value="1900">1900</option>
            </select>
            <select id="heightSelector">
                <option value="auto">Auto</option>
                <option value="300">300</option>
                <option value="400">400</option>
                <option value="500">500</option>
                <option value="600">600</option>
                <option value="700">700</option>
                <option value="800">800</option>
                <option value="900">900</option>
                <option value="1000">1000</option>
            </select>
          </li>
          <li>
              <a>Filter:</a>
              <select id="filterType">
                  <option value="cql">CQL</option>
                  <option value="ogc">OGC</option>
                  <option value="fid">FeatureID</option>
              </select>
              <input type="text" size="80" id="filter"/>
              <a id="updateFilterButton" href="#" title="Apply filter">Apply</a>
              <a id="resetFilterButton" href="#" title="Reset filter">Reset</a>
          </li>
        </ul>
      </div>
    <div id="map">
      <div class="ol-toggle-options ol-unselectable"><a id="options" title="Toggle options toolbar" href="#toggle">...</a></div>
    </div>
    <div id="wrapper">
        <div id="location"></div>
        <div id="scale"></div>
    </div>
    <div id="nodelist">
        <em>Click on the map to get feature info</em>
    </div>
    <input type="hidden" id="minX" value="${model.bbox.minX?c}"/>
    <input type="hidden" id="minY" value="${model.bbox.minY?c}"/>
    <input type="hidden" id="maxX" value="${model.bbox.maxX?c}"/>
    <input type="hidden" id="maxY" value="${model.bbox.maxY?c}"/>
    <input type="hidden" id="SRS" value="${model.SRS}"/>
    <input type="hidden" id="url" value="${url}"/>
    <input type="hidden" id="units" value="${units}"/>
    <#list parameters as param>
    <input type="hidden" class="param" title="${param.name}" value="${param.value}"/>
    </#list>
  </body>
</html>
