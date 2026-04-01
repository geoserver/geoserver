<!doctype html>
<html lang="en">
  <head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link href="${baseUrl}/img/logo-icon.svg" rel="shortcut icon" type="image/svg+xml">
    <link rel="stylesheet" href="${relBaseUrl}/openlayers10/ol.css" type="text/css">
    <link rel="stylesheet" href="${baseUrl}/css/geoserver.css" type="text/css">
    <link rel="stylesheet" href="${relBaseUrl}/openlayers10/layout.css" type="text/css">
    
    <script src="${baseUrl}/js/geoserver.js" type="text/javascript"></script>
    <script src="${relBaseUrl}/openlayers10/ol.js" type="text/javascript"></script>
    <script src="${relBaseUrl}/webresources/wms/OpenLayers10Map.js" type="text/javascript"></script>
    <title>OpenLayers 10 Map Preview</title>
  </head>
  <body>
  <div id="header" class="gs-header">
    <div class="gs-header-bar">
      <div class="gs-header-left">
        <a class="logo" href="${baseUrl}"></a>
      </div>
      <div class="gs-header-right"></div>
    </div>
  </div>

  <div id="main">
    <div id="sidebar">
      <button id="sidebar-menu" aria-expanded="false" aria-label="Toggle Sidebar">
        <svg class="open" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 30 30" aria-hidden="true">
          <path stroke="currentColor" stroke-linecap="round" stroke-miterlimit="10" stroke-width="2" d="M4 7h22M4 15h22M4 23h22"/>
        </svg>
        <svg class="close" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 16 16" aria-hidden="true">
          <path fill="currentColor" d="M.293.293a1 1 0 0 1 1.414 0L8 6.586 14.293.293a1 1 0 1 1 1.414 1.414L9.414 8l6.293 6.293a1 1 0 0 1-1.414 1.414L8 9.414l-6.293 6.293a1 1 0 0 1-1.414-1.414L6.586 8 .293 1.707a1 1 0 0 1 0-1.414"/>
        </svg>
      </button>

      <div id="sidebar-content">
        <div id="toolbar" class="preview-form">
          <label for="wmsVersionSelector">WMS version:</label>
          <select id="wmsVersionSelector">
            <option value="1.1.1">1.1.1</option>
            <option value="1.3.0">1.3.0</option>
          </select>

          <label for="tilingModeSelector">Tiling:</label>
          <select id="tilingModeSelector">
            <option value="untiled">Single tile</option>
            <option value="tiled">Tiled</option>
          </select>

          <label for="antialiasSelector">Antialias:</label>
          <select id="antialiasSelector">
            <option value="full">Full</option>
            <option value="text">Text only</option>
            <option value="none">Disabled</option>
          </select>

          <label for="imageFormatSelector">Format:</label>
          <select id="imageFormatSelector">
            <option value="image/png">PNG 24bit</option>
            <option value="image/png8">PNG 8bit</option>
            <option value="image/gif">GIF</option>
            <option id="jpeg" value="image/jpeg">JPEG</option>
            <option id="jpeg-png" value="image/vnd.jpeg-png">JPEG-PNG</option>
            <option id="jpeg-png8" value="image/vnd.jpeg-png8">JPEG-PNG8</option>
          </select>

          <label for="styleSelector">Styles:</label>
          <select id="styleSelector">
            <option value="">Default</option>
            <#list styles as style>          
                <option value="${style}">${style}</option>  
            </#list>   
          </select>

          <label for="filter">Filter:</label>
          <div class="filter-container">
            <div class="gs-input-group">
                <select id="filterType">
                    <option value="cql">CQL</option>
                    <option value="ogc">OGC</option>
                    <option value="fid">FeatureID</option>
                </select>
                <button id="updateFilterButton" title="Apply filter">Apply</button>
                <button id="resetFilterButton" title="Reset filter">Reset</button>
            </div>
            <textarea id="filter" placeholder="Enter filter expression..."></textarea>
          </div>

          <label for="customParamsInput">Custom Params:</label>
          <textarea id="customParamsInput" placeholder="e.g. viewparams=year:2026&env=color:red"></textarea>

          <div class="choiceItem debug-toggle-container">
            <input type="checkbox" id="debug-mode-toggle">
            <label for="debug-mode-toggle">Enable Debug Mode</label>
          </div>
        </div>
      </div>
    </div>

    <div id="page">
      <div id="map"></div>
      
      <div id="wrapper">
        <div id="location"></div>
        <div id="scale"></div>
      </div>

      <div id="popup" class="ol-popup">
        <a href="#" id="popup-closer" class="ol-popup-closer">✖</a>
        <div id="popup-content"></div>
      </div>

      <div id="debug-tile-popup" class="ol-debug-popup">
        <div id="debug-layer-type" class="debug-label"></div>
        <button id="debug-copy-btn" class="debug-copy-btn">📋 Copy URL</button>
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
  </div>
  </body>
</html>
