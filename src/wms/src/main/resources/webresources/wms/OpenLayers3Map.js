
      var pureCoverage = ${pureCoverage?string};
      // if this is just a coverage or a group of them, disable a few items,
      // and default to jpeg format
      var format = 'image/png';
      var bounds = [${request.bbox.minX?c}, ${request.bbox.minY?c},
                    ${request.bbox.maxX?c}, ${request.bbox.maxY?c}];
      if (pureCoverage) {
        document.getElementById('antialiasSelector').disabled = true;
        document.getElementById('jpeg').selected = true;
        format = "image/jpeg";
      }

      var supportsFiltering = ${supportsFiltering?string};
      if (!supportsFiltering) {
        document.getElementById('filterType').disabled = true;
        document.getElementById('filter').disabled = true;
        document.getElementById('updateFilterButton').disabled = true;
        document.getElementById('resetFilterButton').disabled = true;
      }

      var mousePositionControl = new ol.control.MousePosition({
        className: 'custom-mouse-position',
        target: document.getElementById('location'),
        coordinateFormat: ol.coordinate.createStringXY(5),
        undefinedHTML: '&nbsp;'
      });
      var untiled = new ol.layer.Image({
        source: new ol.source.ImageWMS({
          ratio: 1,
          url: '${baseUrl}/${servicePath}',
          params: {'FORMAT': format,
                   'VERSION': '1.1.1',  
             <#list parameters as param>
                "${param.name?js_string}": '${param.value?js_string}',
             </#list>
          }
        })
      });
      var tiled = new ol.layer.Tile({
        visible: false,
        source: new ol.source.TileWMS({
          url: '${baseUrl}/${servicePath}',
          params: {'FORMAT': format, 
                   'VERSION': '1.1.1',
                   tiled: true,
             <#list parameters as param>
                "${param.name?js_string}": '${param.value?js_string}',
             </#list>
             tilesOrigin: ${request.bbox.minX?c} + "," + ${request.bbox.minY?c}
          }
        })
      });
      var projection = new ol.proj.Projection({
          code: '${request.SRS?js_string}',
          units: '${units?js_string}',
          <#if yx == "true">
          axisOrientation: 'neu',
          </#if>
          global: ${global}
      });
      var map = new ol.Map({
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
          {'INFO_FORMAT': 'text/html', 'FEATURE_COUNT': 50});
        if (url) {
          document.getElementById('nodelist').innerHTML = '<iframe seamless src="' + url + '"></iframe>';
        }
      });

      // sets the chosen WMS version
      function setWMSVersion(wmsVersion) {
        map.getLayers().forEach(function(lyr) {
          lyr.getSource().updateParams({'VERSION': wmsVersion});
        });
        if(wmsVersion == "1.3.0") {
            origin = bounds[1] + ',' + bounds[0];
        } else {
            origin = bounds[0] + ',' + bounds[1];
        }
        tiled.getSource().updateParams({'tilesOrigin': origin});
      }

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
          lyr.getSource().updateParams({'FORMAT': mime});
        });
      }

      function setStyle(style){
        map.getLayers().forEach(function(lyr) {
          lyr.getSource().updateParams({'STYLES': style});
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
        if (!supportsFiltering) {
          return;
        }
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
          if (!supportsFiltering) {
            return;
          }
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
