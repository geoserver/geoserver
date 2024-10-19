
var map;
var untiled;
var tiled;

window.addEventListener('load', function() {

  var format = 'image/png';
  var bounds = [parseFloat(document.getElementById('minX').value),
                parseFloat(document.getElementById('minY').value),
                parseFloat(document.getElementById('maxX').value),
                parseFloat(document.getElementById('maxY').value)];

  var mousePositionControl = new ol.control.MousePosition({
    className: 'custom-mouse-position',
    target: document.getElementById('location'),
    coordinateFormat: ol.coordinate.createStringXY(5),
    undefinedHTML: '&nbsp;'
  });
  var cleaningFunction = function(params) {
    return params.split("&")
      .map(function(kv) {
        var kva = kv.split("=");
        return kva[0].toLowerCase() + "=" + kva[1];
      })
      .filter(function(kv) {
        var kva = kv.split("=");
        var skip = ["service", "version", "request", "format"]
        return !skip.includes(kva[0]);
      })
    .join("&");
  }
  var imageLoadFunction = function(image, src) {
   var url = src.split('?');
   var newParams = cleaningFunction(url[1]);
   (image.getImage()).src = url[0] + "?" + newParams;
  }
  var untiledParams = {};
  var tiledParams = {};
  var paramInputs = document.getElementsByTagName('input');
  for (var i = 0; i < paramInputs.length; i++) {
    if (paramInputs[i].getAttribute('class') == 'param') {
      untiledParams[paramInputs[i].title] = paramInputs[i].value;
      tiledParams[paramInputs[i].title] = paramInputs[i].value;
    }
  }
  untiledParams.f = format;
  tiledParams.f = format;
  tiledParams.tiled = true;
  tiledParams.tilesOrigin = document.getElementById('minX').value + ',' + document.getElementById('minY').value;
  untiled = new ol.layer.Image({
    source: new ol.source.ImageWMS({
      ratio: 1,
      url: document.getElementById('url').value,
      imageLoadFunction: imageLoadFunction,
      params: untiledParams
    })
  });
  tiled = new ol.layer.Tile({
    visible: false,
    source: new ol.source.TileWMS({
      url: document.getElementById('url').value,
      tileLoadFunction: imageLoadFunction,
      params: tiledParams
    })
  });
  var projection = new ol.proj.Projection({
    code: document.getElementById('SRS').value,
    units: document.getElementById('units').value,
    global: false
  });
  map = new ol.Map({
    controls: ol.control.defaults({
      attribution: false
    }).extend([mousePositionControl]),
    target: 'map',
    layers: [
      untiled,
      tiled
    ],
    view: new ol.View({
       projection: projection
    })
  });
  map.getView().on('change:resolution', function(evt) {
    var resolution = evt.target.get('resolution');
    var units = map.getView().getProjection().getUnits();
    var dpi = 25.4 / 0.28;
    var mpu = ol.proj.METERS_PER_UNIT[units];
    var scale = resolution * mpu * 39.37 * dpi;
    if (scale >= 9500 && scale <= 950000) {
      scale = Math.round(scale / 1000) + "K";
    } else if (scale >= 950000) {
      scale = Math.round(scale / 1000000) + "M";
    } else {
      scale = Math.round(scale);
    }
    document.getElementById('scale').innerHTML = "Scale = 1 : " + scale;
  });
  map.getView().fit(bounds, map.getSize());
  
  map.on('singleclick', function(evt) {
    document.getElementById('nodelist').innerHTML = "Loading... please wait...";
    var view = map.getView();
    var viewResolution = view.getResolution();
    var source = untiled.get('visible') ? untiled.getSource() : tiled.getSource();
    var url = source.getGetFeatureInfoUrl(
      evt.coordinate, viewResolution, view.getProjection(),
      {'f': 'text/html', 'FEATURE_COUNT': 50});
    if (url) {
      var urlParts = url.split('?');
      var newParams = cleaningFunction(urlParts[1]);
      url = urlParts[0] + "/info" + "?" + newParams;
      document.getElementById('nodelist').innerHTML =
        '<iframe seamless src="' + url.replace(/"/g, "&quot;") + '"></iframe>';
    }
  });

  // Tiling mode, can be 'tiled' or 'untiled'
  function setTileMode(tilingMode) {
    if (tilingMode == 'tiled') {
      untiled.set('visible', false);
      tiled.set('visible', true);
    } else {
      tiled.set('visible', false);
      untiled.set('visible', true);
    }
  }

  function setAntialiasMode(mode) {
    map.getLayers().forEach(function(lyr) {
      lyr.getSource().updateParams({'FORMAT_OPTIONS': 'antialias:' + mode});
    });
  }

  // changes the current tile format
  function setImageFormat(mime) {
    map.getLayers().forEach(function(lyr) {
      lyr.getSource().updateParams({'f': mime});
    });
  }

  function setWidth(size){
    var mapDiv = document.getElementById('map');
    var wrapper = document.getElementById('wrapper');

    if (size == "auto") {
      // reset back to the default value
      mapDiv.style.width = null;
      wrapper.style.width = null;
    }
    else {
      mapDiv.style.width = size + "px";
      wrapper.style.width = size + "px";
    }
    // notify OL that we changed the size of the map div
    map.updateSize();
  }

  function setHeight(size){
    var mapDiv = document.getElementById('map');
    if (size == "auto") {
      // reset back to the default value
      mapDiv.style.height = null;
    }
    else {
      mapDiv.style.height = size + "px";
    }
    // notify OL that we changed the size of the map div
    map.updateSize();
  }

  function updateFilter(){
    var filterType = document.getElementById('filterType').value;
    var filter = document.getElementById('filter').value;
    // by default, reset all filters
    var filterParams = {
      'FILTER': null,
      'CQL_FILTER': null,
      'FEATUREID': null
    };
    if (filter.replace(/^\s\s*/, '').replace(/\s\s*$/, '') != "") {
      if (filterType == "cql") {
        filterParams["CQL_FILTER"] = filter;
      }
      if (filterType == "ogc") {
        filterParams["FILTER"] = filter;
      }
      if (filterType == "fid")
        filterParams["FEATUREID"] = filter;
    }
    // merge the new filter definitions
    map.getLayers().forEach(function(lyr) {
      lyr.getSource().updateParams(filterParams);
    });
  }

  function resetFilter() {
    document.getElementById('filter').value = "";
    updateFilter();
  }

  // shows/hide the control panel
  function toggleControlPanel(){
    var toolbar = document.getElementById("toolbar");
    if (toolbar.style.display == "none") {
      toolbar.style.display = "block";
    }
    else {
      toolbar.style.display = "none";
    }
    map.updateSize()
  }

  // set event handlers
  document.getElementById('tilingModeSelector').onchange = function(event) { setTileMode(event.target.value); };
  document.getElementById('antialiasSelector').onchange = function(event) { setAntialiasMode(event.target.value); };
  document.getElementById('imageFormatSelector').onchange = function(event) { setImageFormat(event.target.value); };
  document.getElementById('widthSelector').onchange = function(event) { setWidth(event.target.value); };
  document.getElementById('heightSelector').onchange = function(event) { setHeight(event.target.value); };
  document.getElementById('updateFilterButton').onclick = updateFilter;
  document.getElementById('resetFilterButton').onclick = resetFilter;
  document.getElementById('options').onclick = toggleControlPanel;

});
