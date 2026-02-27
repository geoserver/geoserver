<!doctype html>
<html lang="en">
  <head>
    <meta charset="UTF-8">
    <link rel="stylesheet" href="${relBaseUrl}/openlayers3/ol.css" type="text/css">
    <link rel="stylesheet" href="${relBaseUrl}/openlayers3/layout.css" type="text/css">
    <script src="${relBaseUrl}/openlayers3/ol.js" type="text/javascript"></script>
    <script src="${relBaseUrl}/webresources/wms/OpenLayers3Map.js" type="text/javascript"></script>
    <title>OpenLayers map preview</title>
  </head>
  <body>
  <div id="main">
    <div id="sidebar">
      <div id="brand">
        <a wicket:id="home" id="logo" href="#"></a>
        <button id="navigation-menu">
          <svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 30 30'>
            <path stroke='currentColor' stroke-linecap='round' stroke-miterlimit='10' stroke-width='2' d='M4 7h22M4 15h22M4 23h22' />
          </svg>
        </button>
      </div>
      <div id="sidebar-content">
        <div id="toolbar" class="preview-form">
          <label>WMS version:</label>
          <select id="wmsVersionSelector">
            <option value="1.1.1">1.1.1</option>
            <option value="1.3.0">1.3.0</option>
          </select>
          <label>Tiling:</label>
          <select id="tilingModeSelector">
            <option value="untiled">Single tile</option>
            <option value="tiled">Tiled</option>
          </select>
          <label>Antialias:</label>
          <select id="antialiasSelector">
            <option value="full">Full</option>
            <option value="text">Text only</option>
            <option value="none">Disabled</option>
          </select>
          <label>Format:</label>
          <select id="imageFormatSelector">
            <option value="image/png">PNG 24bit</option>
            <option value="image/png8">PNG 8bit</option>
            <option value="image/gif">GIF</option>
            <option id="jpeg" value="image/jpeg">JPEG</option>
            <option id="jpeg-png" value="image/vnd.jpeg-png">JPEG-PNG</option>
            <option id="jpeg-png8" value="image/vnd.jpeg-png8">JPEG-PNG8</option>
          </select>
          <label>Styles:</label>
          <select id="styleSelector">
            <option value="">Default</option>
            <#list styles as style>          
                <option value="${style}">${style}</option>  
            </#list>   
          </select>
          <label>Width/Height:</label>
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
            <label>Filter:</label>
            <select id="filterType">
                <option value="cql">CQL</option>
                <option value="ogc">OGC</option>
                <option value="fid">FeatureID</option>
            </select>
            <input type="text" size="80" id="filter"/>
            <a id="updateFilterButton" class="button" href="#" title="Apply filter">Apply</a>
            <a id="resetFilterButton" class="button outline-primary" href="#" title="Reset filter">Reset</a>
        </div>
      </div>
    </div>
    <div id="page">
      <div id="map"></div>
      <div id="wrapper">
          <div id="location"></div>
          <div id="scale"></div>
      </div>
      <div id="nodelist">
          <em>Click on the map to get feature info</em>
      </div>
      <input type="hidden" id="pureCoverage" value="${pureCoverage}"/>
      <input type="hidden" id="supportsFiltering" value="${supportsFiltering}"/>
      <input type="hidden" id="minX" value="${request.bbox.minX?c}"/>
      <input type="hidden" id="minY" value="${request.bbox.minY?c}"/>
      <input type="hidden" id="maxX" value="${request.bbox.maxX?c}"/>
      <input type="hidden" id="maxY" value="${request.bbox.maxY?c}"/>
      <input type="hidden" id="SRS" value="${request.SRS}"/>
      <input type="hidden" id="yx" value="${yx}"/>
      <input type="hidden" id="global" value="${global}"/>
      <input type="hidden" id="baseUrl" value="${baseUrl}"/>
      <input type="hidden" id="servicePath" value="${servicePath}"/>
      <input type="hidden" id="units" value="${units}"/>
      <#list parameters as param>
      <input type="hidden" class="param" title="${param.name}" value="${param.value}"/>
      </#list>
    </div>
  </body>
</html>