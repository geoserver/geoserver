<!doctype html>
<html lang="en">
  <head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link rel="stylesheet" href="${resourceLink("openlayers10/ol.css")}" type="text/css">
    <link rel="stylesheet" href="${resourceLink("css/geoserver.css")}" type="text/css">
    <link rel="stylesheet" href="${resourceLink("openlayers10/layout.css")}" type="text/css">

    <script src="${resourceLink("openlayers10/ol.js")}" type="text/javascript"></script>
    <script src="${resourceLink("webresources/ogcapi/maps.js")}" type="text/javascript"></script>
    <title>OpenLayers map preview</title>
  </head>
  <body>
  <div id="header" class="gs-header">
    <div class="gs-header-bar">
      <div class="gs-header-left">
        <a class="logo" href="${serviceLink("")}"></a>
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
            <option value="image/jpeg">JPEG</option>
            <option value="image/vnd.jpeg-png">JPEG-PNG</option>
            <option value="image/vnd.jpeg-png8">JPEG-PNG8</option>
          </select>

          <label for="filter">Filter:</label>
          <div class="filter-container">
            <div class="gs-input-group">
              <select id="filterType">
                <option value="cql2-text">CQL2 Text</option>
                <option value="cql2-json">CQL2 JSON</option>
                <option value="ecql-text">ECQL Text</option>
              </select>
              <button id="updateFilterButton" title="Apply filter">Apply</button>
              <button id="resetFilterButton" title="Reset filter">Reset</button>
            </div>
            <textarea id="filter" placeholder="Enter filter expression..."></textarea>
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
        <a href="#" id="popup-closer" class="ol-popup-closer">&#10006;</a>
        <div id="popup-content"></div>
      </div>

      <input type="hidden" id="minX" value="${model.bbox.minX?c}"/>
      <input type="hidden" id="minY" value="${model.bbox.minY?c}"/>
      <input type="hidden" id="maxX" value="${model.bbox.maxX?c}"/>
      <input type="hidden" id="maxY" value="${model.bbox.maxY?c}"/>
      <input type="hidden" id="SRS" value="${model.SRS}"/>
      <input type="hidden" id="crsCurie" value="${crsCurie}"/>
      <input type="hidden" id="axisOrder" value="${axisOrder}"/>
      <input type="hidden" id="url" value="${url}"/>
      <input type="hidden" id="units" value="${units}"/>
      <#list parameters as param>
      <input type="hidden" class="param" title="${param.name}" value="${param.value}"/>
      </#list>
    </div>
  </div>
  </body>
</html>
