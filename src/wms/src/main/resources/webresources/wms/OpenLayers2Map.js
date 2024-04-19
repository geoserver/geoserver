
var map;
var untiled;
var tiled;
var pureCoverage;
var supportsFiltering;
// pink tile avoidance
OpenLayers.IMAGE_RELOAD_ATTEMPTS = 5;
// make OL compute scale according to WMS spec
OpenLayers.DOTS_PER_INCH = 25.4 / 0.28;

window.onload = function() {
    // if this is just a coverage or a group of them, disable a few items,
    // and default to jpeg format
    format = 'image/png';
    pureCoverage = document.getElementById('pureCoverage').value;
    if(pureCoverage) {
        document.getElementById('antialiasSelector').disabled = true;
        document.getElementById('jpeg').selected = true;
        format = "image/jpeg";
    }

    supportsFiltering = document.getElementById('supportsFiltering').value;
    if (!supportsFiltering) {
        document.getElementById('filterType').disabled = true;
        document.getElementById('filter').disabled = true;
        document.getElementById('updateFilterButton').disabled = true;
        document.getElementById('resetFilterButton').disabled = true;
    }
    
    var bounds = new OpenLayers.Bounds(
        parseFloat(document.getElementById('minX').value),
        parseFloat(document.getElementById('minY').value),
        parseFloat(document.getElementById('maxX').value),
        parseFloat(document.getElementById('maxY').value)
    );
    var options = {
        controls: [],
        maxExtent: bounds,
        maxResolution: parseFloat(document.getElementById('maxResolution').value),
        projection: document.getElementById('SRS').value,
        units: document.getElementById('units').value
    };
    map = new OpenLayers.Map('map', options);
    
    // setup tiled layer
    var tiledParams = {};
    var paramInputs = document.getElementsByTagName('input');
    for (var i = 0; i < paramInputs.length; i++) {
        if (paramInputs[i].getAttribute('class') == 'param') {
            tiledParams[paramInputs[i].title] = paramInputs[i].value;
        }
    }
    tiledParams.format = format;
    tiledParams.tilesOrigin = map.maxExtent.left + ',' + map.maxExtent.bottom;
    tiledParams.tiled = true;
    var yx = {};
    yx[document.getElementById('SRS').value] = document.getElementById('yx').value == 'true';
    tiled = new OpenLayers.Layer.WMS(
        document.getElementById('layerName').value  + " - Tiled",
        document.getElementById('baseUrl').value + '/' + document.getElementById('servicePath').value,
        tiledParams,
        {
            buffer: 0,
            displayOutsideMaxExtent: true,
            isBaseLayer: true,
            yx : yx
        } 
    );
    
    // setup single tiled layer
    var untiledParams = {};
    for (i = 0; i < paramInputs.length; i++) {
        if (paramInputs[i].getAttribute('class') == 'param') {
            untiledParams[paramInputs[i].title] = paramInputs[i].value;
        }
    }
    untiledParams.format = format;
    untiled = new OpenLayers.Layer.WMS(
        document.getElementById('layerName').value  + " - Untiled",
        document.getElementById('baseUrl').value + '/' + document.getElementById('servicePath').value,
        untiledParams,
        {
           singleTile: true, 
           ratio: 1, 
           isBaseLayer: true,
           yx : yx
        } 
    );
    
    map.addLayers([untiled, tiled]);

    // build up all controls
    map.addControl(new OpenLayers.Control.PanZoomBar({
        position: new OpenLayers.Pixel(2, 15)
    }));
    map.addControl(new OpenLayers.Control.Navigation());
    map.addControl(new OpenLayers.Control.Scale($('scale')));
    map.addControl(new OpenLayers.Control.MousePosition({element: $('location')}));
    map.zoomToExtent(bounds);
    
    // wire up the option button
    var options = document.getElementById("options");
    options.onclick = toggleControlPanel;
    
    // support GetFeatureInfo
    map.events.register('click', map, function (e) {
        document.getElementById('nodelist').innerHTML = "Loading... please wait...";
        var params = {};
        for (i = 0; i < paramInputs.length; i++) {
            if (paramInputs[i].getAttribute('class') == 'param') {
                params[paramInputs[i].title] = paramInputs[i].value;
            }
        }
        params.REQUEST = 'GetFeatureInfo';
        params.EXCEPTIONS = 'application/vnd.ogc.se_xml';
        params.BBOX = map.getExtent().toBBOX();
        params.SERVICE = 'WMS';
        params.INFO_FORMAT = 'text/html';
        params.QUERY_LAYERS = map.layers[0].params.LAYERS;
        params.FEATURE_COUNT = 50;
        params.WIDTH = map.size.w;
        params.HEIGHT = map.size.h;
        params.FORMAT = format;
        params.STYLES = map.layers[0].params.STYLES;
        params.SRS = map.layers[0].params.SRS;
        
        // handle the wms 1.3 vs wms 1.1 madness
        if(map.layers[0].params.VERSION == "1.3.0") {
            params.version = "1.3.0";
            params.j = parseInt(e.xy.x);
            params.i = parseInt(e.xy.y);
        } else {
            params.version = "1.1.1";
            params.x = parseInt(e.xy.x);
            params.y = parseInt(e.xy.y);
        }
        
        // merge filters
        if(map.layers[0].params.CQL_FILTER != null) {
            params.cql_filter = map.layers[0].params.CQL_FILTER;
        } 
        if(map.layers[0].params.FILTER != null) {
            params.filter = map.layers[0].params.FILTER;
        }
        if(map.layers[0].params.FEATUREID) {
            params.featureid = map.layers[0].params.FEATUREID;
        }
        var url = document.getElementById('baseUrl').value + '/';
        url = url + document.getElementById('servicePath').value + '?';
        for (var param in params) {
            url = url + param + '=' + params[param] + '&';
        }
        url = url.substring(0, url.length - 1);
        document.getElementById('nodelist').innerHTML =
            '<iframe seamless src="' + url.replace(/"/g, "&quot;") + '"></iframe>';
    });
    
    // shows/hide the control panel
    function toggleControlPanel(event){
        var toolbar = document.getElementById("toolbar");
        if (toolbar.style.display == "none") {
            toolbar.style.display = "block";
        }
        else {
            toolbar.style.display = "none";
        }
        event.stopPropagation();
        map.updateSize()
    }
    
    // Tiling mode, can be 'tiled' or 'untiled'
    function setTileMode(tilingMode){
        if (tilingMode == 'tiled') {
            untiled.setVisibility(false);
            tiled.setVisibility(true);
            map.setBaseLayer(tiled);
        }
        else {
            untiled.setVisibility(true);
            tiled.setVisibility(false);
            map.setBaseLayer(untiled);
        }
    }
    
    // Transition effect, can be null or 'resize'
    function setTransitionMode(transitionEffect){
        if (transitionEffect === 'resize') {
            tiled.transitionEffect = transitionEffect;
            untiled.transitionEffect = transitionEffect;
        }
        else {
            tiled.transitionEffect = null;
            untiled.transitionEffect = null;
        }
    }
    
    // changes the current tile format
    function setImageFormat(mime){
        // we may be switching format on setup
        if(tiled == null)
          return;
        
        tiled.mergeNewParams({
            format: mime
        });
        untiled.mergeNewParams({
            format: mime
        });
        /*
        var paletteSelector = document.getElementById('paletteSelector')
        if (mime == 'image/jpeg') {
            paletteSelector.selectedIndex = 0;
            setPalette('');
            paletteSelector.disabled = true;
        }
        else {
            paletteSelector.disabled = false;
        }
        */
    }
    
    // sets the chosen style
    function setStyle(style){
        // we may be switching style on setup
        if(tiled == null)
          return;
        
        tiled.mergeNewParams({
            styles: style
        });
        untiled.mergeNewParams({
            styles: style
        });
    }
    
    // sets the chosen WMS version
    function setWMSVersion(wmsVersion){
        // we may be switching style on setup
        if(wmsVersion == null)
          return;
        
        if(wmsVersion == "1.3.0") {
           origin = map.maxExtent.bottom + ',' + map.maxExtent.left;
        } else {
           origin = map.maxExtent.left + ',' + map.maxExtent.bottom;
        }
          
        tiled.mergeNewParams({
            version: wmsVersion,
            tilesOrigin : origin
        });
        untiled.mergeNewParams({
            version: wmsVersion
        });
    }
    
    function setAntialiasMode(mode){
        tiled.mergeNewParams({
            format_options: 'antialias:' + mode
        });
        untiled.mergeNewParams({
            format_options: 'antialias:' + mode
        });
    }
    
    function setPalette(mode){
        if (mode == '') {
            tiled.mergeNewParams({
                palette: null
            });
            untiled.mergeNewParams({
                palette: null
            });
        }
        else {
            tiled.mergeNewParams({
                palette: mode
            });
            untiled.mergeNewParams({
                palette: mode
            });
        }
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
        if(!supportsFiltering)
          return;
        
        var filterType = document.getElementById('filterType').value;
        var filter = document.getElementById('filter').value;
        
        // by default, reset all filters
        var filterParams = {
            filter: null,
            cql_filter: null,
            featureId: null
        };
        if (OpenLayers.String.trim(filter) != "") {
            if (filterType == "cql") 
                filterParams["cql_filter"] = filter;
            if (filterType == "ogc") 
                filterParams["filter"] = filter;
            if (filterType == "fid") 
                filterParams["featureId"] = filter;
        }
        // merge the new filter definitions
        mergeNewParams(filterParams);
    }
    
    function resetFilter() {
        if(!supportsFiltering)
          return;
    
        document.getElementById('filter').value = "";
        updateFilter();
    }
    
    function mergeNewParams(params){
        tiled.mergeNewParams(params);
        untiled.mergeNewParams(params);
    }

    // set event handlers
    document.getElementById('tilingModeSelector').onchange = function(event) { setTileMode(event.target.value); };
    document.getElementById('transitionEffectSelector').onchange = function(event) { setTransitionMode(event.target.value); };
    document.getElementById('imageFormatSelector').onchange = function(event) { setImageFormat(event.target.value); };
    document.getElementById('styleSelector').onchange = function(event) { setStyle(event.target.value); };
    document.getElementById('wmsVersionSelector').onchange = function(event) { setWMSVersion(event.target.value); };
    document.getElementById('antialiasSelector').onchange = function(event) { setAntialiasMode(event.target.value); };
    document.getElementById('widthSelector').onchange = function(event) { setWidth(event.target.value); };
    document.getElementById('heightSelector').onchange = function(event) { setHeight(event.target.value); };
    document.getElementById('updateFilterButton').onclick = updateFilter;
    document.getElementById('resetFilterButton').onclick = resetFilter;

};
