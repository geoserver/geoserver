/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 * Copyright © 2019 World Wide Web Consortium, (Massachusetts Institute of Technology, 
 * European Research Consortium for Informatics and Mathematics, Keio    
 * University, Beihang). All Rights Reserved. This work is distributed under the 
 * W3C® Software License [1] in the hope that it will be useful, but WITHOUT ANY 
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR 
 * A PARTICULAR PURPOSE.
 * [1] http://www.w3.org/Consortium/Legal/copyright-software
 * 
 */
(function () {
  'use strict';

  var MapMLStaticTileLayer = L.GridLayer.extend({

    initialize: function (options) {
      this.zoomBounds = this._getZoomBounds(options.tileContainer,options.maxZoomBound);
      L.extend(options, this.zoomBounds);
      L.setOptions(this, options);
      this._groups = this._groupTiles(this.options.tileContainer.getElementsByTagName('map-tile'));
    },

    onAdd: function(){
      this._bounds = this._getLayerBounds(this._groups,this._map.options.projection); //stores meter values of bounds
      this.layerBounds = this._bounds[Object.keys(this._bounds)[0]];
      for(let key of Object.keys(this._bounds)){
        this.layerBounds.extend(this._bounds[key].min);
        this.layerBounds.extend(this._bounds[key].max);
      }
      L.GridLayer.prototype.onAdd.call(this,this._map);
      this._handleMoveEnd();
    },
    
    getEvents: function(){
      let events = L.GridLayer.prototype.getEvents.call(this,this._map);
      this._parentOnMoveEnd = events.moveend;
      events.moveend = this._handleMoveEnd;
      events.move = ()=>{}; //needed to prevent moveend from running
      return events;
    },


    //sets the bounds flag of the layer and calls default moveEnd if within bounds
    //its the zoom level is between the nativeZoom and zoom then it uses the nativeZoom value to get the bound its checking
    _handleMoveEnd : function(e){
      let mapZoom = this._map.getZoom();
      let zoomLevel = mapZoom;
      zoomLevel = zoomLevel > this.options.maxNativeZoom? this.options.maxNativeZoom: zoomLevel;
      zoomLevel = zoomLevel < this.options.minNativeZoom? this.options.minNativeZoom: zoomLevel;
      this.isVisible = mapZoom <= this.zoomBounds.maxZoom && mapZoom >= this.zoomBounds.minZoom && 
                        this._bounds[zoomLevel] && this._bounds[zoomLevel]
                        .overlaps(M.pixelToPCRSBounds(
                          this._map.getPixelBounds(),
                          this._map.getZoom(),
                          this._map.options.projection));
      if(!(this.isVisible))return; //onMoveEnd still gets fired even when layer is out of bounds??, most likely need to overrride _onMoveEnd
      this._parentOnMoveEnd();
    },

    _isValidTile(coords) {
      return this._groups[this._tileCoordsToKey(coords)];
    },

    createTile: function (coords) {
      let tileGroup = this._groups[this._tileCoordsToKey(coords)] || [],
          tileElem = document.createElement('map-tile'), tileSize = this.getTileSize();
      tileElem.setAttribute("col",coords.x);
      tileElem.setAttribute("row",coords.y);
      tileElem.setAttribute("zoom",coords.z);
      
      for(let i = 0;i<tileGroup.length;i++){
        let tile= document.createElement('img');
        tile.width = tileSize.x;
        tile.height = tileSize.y;
        tile.alt = '';
        tile.setAttribute("role","presentation");
        tile.src = tileGroup[i].src;
        tileElem.appendChild(tile);
      }
      return tileElem;
    },

    _getLayerBounds: function(tileGroups, projection){
      let layerBounds = {}, tileSize = M[projection].options.crs.tile.bounds.max.x;
      for(let tile in tileGroups){
        let sCoords = tile.split(":"), pixelCoords = {};
        pixelCoords.x = +sCoords[0] * tileSize;
        pixelCoords.y = +sCoords[1] * tileSize;
        pixelCoords.z = +sCoords[2]; //+String same as parseInt(String)
        if(sCoords[2] in layerBounds){

          layerBounds[sCoords[2]].extend(L.point(pixelCoords.x ,pixelCoords.y ));
          layerBounds[sCoords[2]].extend(L.point(((pixelCoords.x+tileSize) ),((pixelCoords.y+tileSize) )));
        } else {
          layerBounds[sCoords[2]] = L.bounds(
                                      L.point(pixelCoords.x ,pixelCoords.y ),
                                      L.point(((pixelCoords.x+tileSize) ),((pixelCoords.y+tileSize) )));
        }
      }
      // TODO: optimize by removing 2nd loop, add util function to convert point in pixels to point in pcrs, use that instead then this loop
      // won't be needed
      for(let pixelBounds in layerBounds){
        let zoom = +pixelBounds;
        layerBounds[pixelBounds] = M.pixelToPCRSBounds(layerBounds[pixelBounds],zoom,projection);
      }

      return layerBounds;
    },

    _getZoomBounds: function(container, maxZoomBound){
      if(!container) return null;
      let meta = M.metaContentToObject(container.getElementsByTagName('map-tiles')[0].getAttribute('zoom')),
          zoom = {},tiles = container.getElementsByTagName("map-tile");
      zoom.nativeZoom = +meta.value || 0;
      zoom.maxNativeZoom = 0;
      zoom.minNativeZoom = maxZoomBound;
      for (let i=0;i<tiles.length;i++) {
        let lZoom = +tiles[i].getAttribute('zoom');
        if(!tiles[i].getAttribute('zoom')) lZoom = zoom.nativeZoom;
        zoom.minNativeZoom = Math.min(zoom.minNativeZoom, lZoom);
        zoom.maxNativeZoom = Math.max(zoom.maxNativeZoom, lZoom);
      }

      //hard coded to only natively zoom out 2 levels, any more and too many tiles are going to be loaded in at one time
      //lagging the users computer
      zoom.minZoom = zoom.minNativeZoom - 2 <= 0? 0: zoom.minNativeZoom - 2;
      zoom.maxZoom = maxZoomBound;
      if(meta.min)zoom.minZoom = +meta.min < (zoom.minNativeZoom - 2)?(zoom.minNativeZoom - 2):+meta.min;
      if(meta.max)zoom.maxZoom = +meta.max;
      return zoom;
    },

    _groupTiles: function (tiles) {
      let tileMap = {};
      for (let i=0;i<tiles.length;i++) {
        let tile = {};
        tile.row = +tiles[i].getAttribute('row');
        tile.col = +tiles[i].getAttribute('col');
        tile.zoom = +tiles[i].getAttribute('zoom') || this.options.nativeZoom;
        tile.src = tiles[i].getAttribute('src');
        let tileCode = tile.col+":"+tile.row+":"+tile.zoom;
        if(tileCode in tileMap){
          tileMap[tileCode].push(tile);
        } else {
          tileMap[tileCode]=[tile];
        }
      }
      return tileMap;
    },
  });

  var mapMLStaticTileLayer = function(options) {
    return new MapMLStaticTileLayer(options);
  };

  var MapMLLayerControl = L.Control.Layers.extend({
      /* removes 'base' layers as a concept */
      options: {
        autoZIndex: false,
        sortLayers: true,
        sortFunction: function (layerA, layerB) {
          return layerA.options.zIndex < layerB.options.zIndex ? -1 : (layerA.options.zIndex > layerB.options.zIndex ? 1 : 0);
        }
      },
      initialize: function (overlays, options) {
          L.setOptions(this, options);
          
          // the _layers array contains objects like {layer: layer, name: "name", overlay: true}
          // the array index is the id of the layer returned by L.stamp(layer) which I guess is a unique hash
          this._layerControlInputs = [];
          this._layers = [];
          this._lastZIndex = 0;
          this._handlingClick = false;

          for (var i in overlays) {
              this._addLayer(overlays[i], i, true);
          }
      },
      onAdd: function () {
          this._initLayout();
          this._map.on('validate', this._validateInput, this);
          L.DomEvent.on(this.options.mapEl, "layerchange", this._validateInput, this);
          L.DomEvent.on(this._container, 'keydown', this._focusFirstLayer, this._container);
          this._update();
          //this._validateExtents();
          if(this._layers.length < 1 && !this._map._showControls){
            this._container.setAttribute("hidden","");
          } else {
            this._map._showControls = true;
          }
          return this._container;
      },
      onRemove: function (map) {
          map.off('validate', this._validateInput, this);
          L.DomEvent.off(this._container, 'keydown', this._focusFirstLayer, this._container);
          // remove layer-registerd event handlers so that if the control is not
          // on the map it does not generate layer events
          for (var i = 0; i < this._layers.length; i++) {
            this._layers[i].layer.off('add remove', this._onLayerChange, this);
            this._layers[i].layer.off('extentload', this._validateInput, this);
          }
      },
      addOrUpdateOverlay: function (layer, name) {
        var alreadyThere = false;
        for (var i=0;i<this._layers.length;i++) {
          if (this._layers[i].layer === layer) {
            alreadyThere = true;
            this._layers[i].name = name;
            // replace the controls with updated controls if necessary.
            break;
          }
        }
        if (!alreadyThere) {
          this.addOverlay(layer, name);
        }
        if(this._layers.length > 0){
          this._container.removeAttribute("hidden");
          this._map._showControls = true;
        }
        return (this._map) ? this._update() : this;
      },
      removeLayer: function (layer) {
        L.Control.Layers.prototype.removeLayer.call(this, layer);
        if(this._layers.length === 0){
          this._container.setAttribute("hidden", "");
        }
      },
      _validateInput: function (e) {
        for (let i = 0; i < this._layers.length; i++) {
          if(!this._layers[i].input.labels[0])continue;
          let label = this._layers[i].input.labels[0].getElementsByTagName("span"),
              input = this._layers[i].input.labels[0].getElementsByTagName("input");
          input[0].checked = this._layers[i].layer._layerEl.checked;
          if(this._layers[i].layer._layerEl.disabled && this._layers[i].layer._layerEl.checked){
            input[0].closest("fieldset").disabled = true;
            label[0].style.fontStyle = "italic";
          } else {
            input[0].closest("fieldset").disabled = false;
            label[0].style.fontStyle = "normal";
          }
          // check if an extent is disabled and disable it
          if(this._layers[i].layer._extent && this._layers[i].layer._extent._mapExtents){
            for(let j = 0; j < this._layers[i].layer._extent._mapExtents.length; j++){
              let input = this._layers[i].layer._extent._mapExtents[j].extentAnatomy,
                  label = input.getElementsByClassName("mapml-layer-item-name")[0];
              if(this._layers[i].layer._extent._mapExtents[j].disabled && this._layers[i].layer._extent._mapExtents[j].checked){
                label.style.fontStyle = "italic";
                input.disabled = true;
              } else {
                label.style.fontStyle = "normal";
                input.disabled = false;
              }
            }
          }
        }

      },

      // focus the first layer in the layer control when enter is pressed
      _focusFirstLayer: function(e){
        if(e.key === 'Enter' && this.className != 'leaflet-control-layers leaflet-control leaflet-control-layers-expanded'){
          var elem = this.children[1].children[2].children[0].children[0].children[0].children[0];
          if(elem) setTimeout(() => elem.focus(), 0);
          }
      },
      
      _withinZoomBounds: function(zoom, range) {
          return range.min <= zoom && zoom <= range.max;
      },
      _addItem: function (obj) {
        var layercontrols  =  obj.layer.getLayerUserControlsHTML();
        // the input is required by Leaflet...
        obj.input = layercontrols.querySelector('input');

        this._layerControlInputs.push(obj.input);
      		obj.input.layerId = L.stamp(obj.layer);

        L.DomEvent.on(obj.input, 'click', this._onInputClick, this);
        // this is necessary because when there are several layers in the
        // layer control, the response to the last one can be a long time
        // after the info is first displayed, so we have to go back and
        // verify the layer element is not disabled and can have an enabled input.
        obj.layer.on('extentload', this._validateInput, this);
        this._overlaysList.appendChild(layercontrols);
        return layercontrols;
      },

      //overrides collapse and conditionally collapses the panel
      collapse: function(e){
        if (
          e.target.tagName === "SELECT" ||
          e.relatedTarget &&
          e.relatedTarget.parentElement &&
          (
            e.relatedTarget.className === "mapml-contextmenu mapml-layer-menu" ||
            e.relatedTarget.parentElement.className === "mapml-contextmenu mapml-layer-menu"
          ) ||
          (this._map && this._map.contextMenu._layerMenu.style.display === "block")
        ) return this;

        L.DomUtil.removeClass(this._container, 'leaflet-control-layers-expanded');
  		  return this;
      }
  });
  var mapMlLayerControl = function (layers, options) {
  	return new MapMLLayerControl(layers, options);
  };

  const FALLBACK_PROJECTION = "OSMTILE";
  const FALLBACK_CS = "TILEMATRIX";
  const BLANK_TT_TREF = "mapmltemplatedtileplaceholder";

  var MapMLFeatures = L.FeatureGroup.extend({
    /*
     * M.MapML turns any MapML feature data into a Leaflet layer. Based on L.GeoJSON.
     */
      initialize: function (mapml, options) {

        L.setOptions(this, options);
        if(this.options.static) {
          this._container = L.DomUtil.create('div', 'leaflet-layer', this.options.pane);
          // must have leaflet-pane class because of new/changed rule in leaflet.css
          // info: https://github.com/Leaflet/Leaflet/pull/4597
          L.DomUtil.addClass(this._container, 'leaflet-pane mapml-vector-container');
          L.setOptions(this.options.renderer, {pane: this._container});
        }

        this._layers = {};
        if(this.options.query){
          this._mapmlFeatures = mapml.features ? mapml.features: mapml;
          this.isVisible = true;
          let native = this._getNativeVariables(mapml);
          this.options.nativeZoom = native.zoom;
          this.options.nativeCS = native.cs;
        }
        if (mapml && !this.options.query) {
          let native = this._getNativeVariables(mapml);
          //needed to check if the feature is static or not, since this method is used by templated also
          if(!mapml.querySelector('map-extent') && mapml.querySelector('map-feature') && this.options.static){
            this._features = {};
            this._staticFeature = true;
            this.isVisible = true; //placeholder for when this actually gets updated in the future
            this.zoomBounds = this._getZoomBounds(mapml, native.zoom);
            this.layerBounds = this._getLayerBounds(mapml);
            L.extend(this.options, this.zoomBounds);
          }
          this.addData(mapml, native.cs, native.zoom);
          if(this._staticFeature){
            this._resetFeatures(this._clampZoom(this.options._leafletLayer._map.getZoom()));

            this.options._leafletLayer._map._addZoomLimit(this);
          }
        }
      },

      onAdd: function(map){
        L.FeatureGroup.prototype.onAdd.call(this, map);
        if(this._mapmlFeatures)map.on("featurepagination", this.showPaginationFeature, this);
      },

      onRemove: function(map){
        if(this._mapmlFeatures){
          map.off("featurepagination", this.showPaginationFeature, this);
          delete this._mapmlFeatures;
          L.DomUtil.remove(this._container);
        }
        L.FeatureGroup.prototype.onRemove.call(this, map);
        this._map.featureIndex.cleanIndex();
      },

      getEvents: function(){
        if(this._staticFeature){
          return {
            'moveend':this._handleMoveEnd,
            'zoomend' : this._handleZoomEnd,
          };
        }
        return {
          'moveend':this._removeCSS
        };
      },


      showPaginationFeature: function(e){
        if(this.options.query && this._mapmlFeatures[e.i]){
          let feature = this._mapmlFeatures[e.i];
          this.clearLayers();
          this.addData(feature, this.options.nativeCS, this.options.nativeZoom);
          e.popup._navigationBar.querySelector("p").innerText = (e.i + 1) + "/" + this.options._leafletLayer._totalFeatureCount;
          e.popup._content.querySelector("iframe").setAttribute("sandbox", "allow-same-origin allow-forms");
          e.popup._content.querySelector("iframe").srcdoc = feature.querySelector("map-properties").innerHTML;
        }
      },

      _getNativeVariables: function(mapml){
        let nativeZoom = (mapml.querySelector && mapml.querySelector("map-meta[name=zoom]") &&
            +M.metaContentToObject(mapml.querySelector("map-meta[name=zoom]").getAttribute("content")).value) || 0;
        let nativeCS = (mapml.querySelector && mapml.querySelector("map-meta[name=cs]") &&
            M.metaContentToObject(mapml.querySelector("map-meta[name=cs]").getAttribute("content")).content) || "PCRS";
        return {zoom:nativeZoom, cs: nativeCS};
      },

      _handleMoveEnd : function(){
        let mapZoom = this._map.getZoom(),
            withinZoom = mapZoom <= this.zoomBounds.maxZoom && mapZoom >= this.zoomBounds.minZoom;   
        this.isVisible = withinZoom && this._layers && this.layerBounds && 
                          this.layerBounds.overlaps(
                            M.pixelToPCRSBounds(
                              this._map.getPixelBounds(),
                              mapZoom,this._map.options.projection));
        this._removeCSS();
      },

      _handleZoomEnd: function(e){
        let mapZoom = this._map.getZoom();
        if(mapZoom > this.zoomBounds.maxZoom || mapZoom < this.zoomBounds.minZoom){
          this.clearLayers();
          return;
        }
        let clampZoom = this._clampZoom(mapZoom);
        this._resetFeatures(clampZoom);
      },

      //sets default if any are missing, better to only replace ones that are missing
      _getLayerBounds : function(container) {
        if (!container) return null;
        let cs = FALLBACK_CS,
            projection = container.querySelector('map-meta[name=projection]') &&
                      M.metaContentToObject(
                        container.querySelector('map-meta[name=projection]').getAttribute('content'))
                        .content.toUpperCase() || FALLBACK_PROJECTION;
        try{

          let meta = container.querySelector('map-meta[name=extent]') &&
                      M.metaContentToObject(
                        container.querySelector('map-meta[name=extent]').getAttribute('content'));

          let zoom = meta.zoom || 0;
          
          let metaKeys = Object.keys(meta);
          for(let i =0;i<metaKeys.length;i++){
            if(!metaKeys[i].includes("zoom")){
              cs = M.axisToCS(metaKeys[i].split("-")[2]);
              break;
            }
          }
          let axes = M.csToAxes(cs);
          return M.boundsToPCRSBounds(
                  L.bounds(L.point(+meta[`top-left-${axes[0]}`],+meta[`top-left-${axes[1]}`]),
                  L.point(+meta[`bottom-right-${axes[0]}`],+meta[`bottom-right-${axes[1]}`])),
                  zoom,projection,cs);
        } catch (error){
          //if error then by default set the layer to osm and bounds to the entire map view
          return M.boundsToPCRSBounds(M[projection].options.crs.tilematrix.bounds(0),0,projection, cs);
        }
      },

      _resetFeatures : function (zoom){
        this.clearLayers();
        // since features are removed and re-added by zoom level, need to clean the feature index before re-adding
        if(this._map) this._map.featureIndex.cleanIndex();
        if(this._features && this._features[zoom]){
          for(let k =0;k < this._features[zoom].length;k++){
            this.addLayer(this._features[zoom][k]);
          }
        }
      },

      _clampZoom : function(zoom){
        if(zoom > this.zoomBounds.maxZoom || zoom < this.zoomBounds.minZoom) return zoom;
        if (undefined !== this.zoomBounds.minNativeZoom && zoom < this.zoomBounds.minNativeZoom) {
          return this.zoomBounds.minNativeZoom;
        }
        if (undefined !== this.zoomBounds.maxNativeZoom && this.zoomBounds.maxNativeZoom < zoom) {
          return this.zoomBounds.maxNativeZoom;
        }

        return zoom;
      },

      _setZoomTransform: function(center, clampZoom){
        var scale = this._map.getZoomScale(this._map.getZoom(),clampZoom),
  		    translate = center.multiplyBy(scale)
  		        .subtract(this._map._getNewPixelOrigin(center, this._map.getZoom())).round();

        if (any3d) {
          L.setTransform(this._layers[clampZoom], translate, scale);
        } else {
          L.setPosition(this._layers[clampZoom], translate);
        }
      },

      _getZoomBounds: function(container, nativeZoom){
        if (!container) return null;
        let nMin = 100,nMax=0, features = container.getElementsByTagName('map-feature'),meta,projection;
        for(let i =0;i<features.length;i++){
          let lZoom = +features[i].getAttribute('zoom');
          if(!features[i].getAttribute('zoom'))lZoom = nativeZoom;
          nMax = Math.max(nMax, lZoom);
          nMin = Math.min(nMin, lZoom);
        }
        try{
          projection = M.metaContentToObject(container.querySelector('map-meta[name=projection]').getAttribute('content')).content;
          meta = M.metaContentToObject(container.querySelector('map-meta[name=zoom]').getAttribute('content'));
        } catch(error){
          return {
            minZoom:0,
            maxZoom: M[projection || FALLBACK_PROJECTION].options.resolutions.length - 1,
            minNativeZoom:nMin,
            maxNativeZoom:nMax
          };
        }
        return {
          minZoom:+meta.min ,
          maxZoom:+meta.max ,
          minNativeZoom:nMin,
          maxNativeZoom:nMax
        };
      },

      addData: function (mapml, nativeCS, nativeZoom) {
        var features = mapml.nodeType === Node.DOCUMENT_NODE || mapml.nodeName === "LAYER-" ? mapml.getElementsByTagName("map-feature") : null,
            i, len, feature;

        var linkedStylesheets = mapml.nodeType === Node.DOCUMENT_NODE ? mapml.querySelector("map-link[rel=stylesheet],map-style") : null;
        if (linkedStylesheets) {
          var base = mapml.querySelector('map-base') && mapml.querySelector('map-base').hasAttribute('href') ? 
              new URL(mapml.querySelector('map-base').getAttribute('href')).href : 
              mapml.URL;
          M.parseStylesheetAsHTML(mapml,base,this._container);
        }
        if (features) {
         for (i = 0, len = features.length; i < len; i++) {
          // Only add this if geometry is set and not null
          feature = features[i];
          var geometriesExist = feature.getElementsByTagName("map-geometry").length && feature.getElementsByTagName("map-coordinates").length;
          if (geometriesExist) {
           this.addData(feature, nativeCS, nativeZoom);
          }
         }
         return this; //if templated this runs
        }

        //if its a mapml with no more links this runs
        var options = this.options;

        if (options.filter && !options.filter(mapml)) { return; }
        
        if (mapml.classList.length) {
          options.className = mapml.classList.value;
        }
        let zoom = mapml.getAttribute("zoom") || nativeZoom, title = mapml.querySelector("map-featurecaption");
        title = title ? title.innerHTML : "Feature";

        if(mapml.querySelector("map-properties")) {
          options.properties = document.createElement('div');
          options.properties.classList.add("mapml-popup-content");
          options.properties.insertAdjacentHTML('afterbegin', mapml.querySelector("map-properties").innerHTML);
        }

        let layer = this.geometryToLayer(mapml, options, nativeCS, +zoom, title);
        if (layer) {
          // if the layer is being used as a query handler output, it will have
          // a color option set.  Otherwise, copy classes from the feature
          if (!layer.options.color && mapml.hasAttribute('class')) {
            layer.options.className = mapml.getAttribute('class');
          }
          layer.defaultOptions = layer.options;
          this.resetStyle(layer);

          if (options.onEachFeature) {
            layer.bindTooltip(title, { interactive:true, sticky: true, });
          }
          if(this._staticFeature){
            let featureZoom = mapml.getAttribute('zoom') || nativeZoom;
            if(featureZoom in this._features){
              this._features[featureZoom].push(layer);
            } else {
              this._features[featureZoom]=[layer];
            }
            return;
          } else {
            return this.addLayer(layer);
          }
        }
      },
          
      resetStyle: function (layer) {
        var style = this.options.style;
        if (style) {
         // reset any custom styles
         L.Util.extend(layer.options, layer.defaultOptions);
         this._setLayerStyle(layer, style);
        }
      },

      setStyle: function (style) {
        this.eachLayer(function (layer) {
          this._setLayerStyle(layer, style);
        }, this);
      },

      _setLayerStyle: function (layer, style) {
        if (typeof style === 'function') {
          style = style(layer.feature);
        }
        if (layer.setStyle) {
          layer.setStyle(style);
        }
      },
      _removeCSS: function(){
        let toDelete = this._container.querySelectorAll("link[rel=stylesheet],style");
        for(let i = 0; i < toDelete.length;i++){
          this._container.removeChild(toDelete[i]);
        }
      },
    geometryToLayer: function (mapml, vectorOptions, nativeCS, zoom, title) {
      let geometry = mapml.tagName.toUpperCase() === 'MAP-FEATURE' ? mapml.getElementsByTagName('map-geometry')[0] : mapml,
          cs = geometry?.getAttribute("cs") || nativeCS, group = [], svgGroup = L.SVG.create('g'), copyOptions = Object.assign({}, vectorOptions);
      if (geometry) {
        for(let geo of geometry.querySelectorAll('map-polygon, map-linestring, map-multilinestring, map-point, map-multipoint')){
          group.push(M.feature(geo, Object.assign(copyOptions,
            { nativeCS: cs,
              nativeZoom: zoom,
              projection: this.options.projection,
              featureID: mapml.id,
              group: svgGroup,
              wrappers: this._getGeometryParents(geo.parentElement),
              featureLayer: this,
              _leafletLayer: this.options._leafletLayer,
            })));
        }
        let groupOptions = {group:svgGroup, featureID: mapml.id, accessibleTitle: title, onEachFeature: vectorOptions.onEachFeature, properties: vectorOptions.properties, _leafletLayer: this.options._leafletLayer,},
          collections = geometry.querySelector('map-multipolygon') || geometry.querySelector('map-geometrycollection');
          if(collections) groupOptions.wrappers = this._getGeometryParents(collections.parentElement);

        return M.featureGroup(group, groupOptions);
      }
    },

    _getGeometryParents: function(subType, elems = []){
      if(subType && subType.tagName.toUpperCase() !== "MAP-GEOMETRY"){
        if(subType.tagName.toUpperCase() === "MAP-MULTIPOLYGON" || subType.tagName.toUpperCase() === "MAP-GEOMETRYCOLLECTION")
          return this._getGeometryParents(subType.parentElement, elems);
        return this._getGeometryParents(subType.parentElement, elems.concat([subType]));
      } else {
        return elems;
      }
    },
  });
  var mapMlFeatures = function (mapml, options) {
  	return new MapMLFeatures(mapml, options);
  };

  var TemplatedTileLayer = L.TileLayer.extend({
      // a TemplateTileLayer is similar to a L.TileLayer except its templates are
      // defined by the <map-extent><template/></map-extent>
      // content found in the MapML document.  As such, the client map does not
      // 'revisit' the server for more MapML content, it simply fills the map extent
      // with tiles for which it generates requests on demand (as the user pans/zooms/resizes
      // the map)
      initialize: function(template, options) {
        // _setUpTileTemplateVars needs options.crs, not available unless we set
        // options first...
        let inputData = M.extractInputBounds(template);
        this.zoomBounds = inputData.zoomBounds;
        this.extentBounds=inputData.bounds;
        this.isVisible = true;
        L.extend(options, this.zoomBounds);
        options.tms = template.tms;
        delete options.opacity;
        L.setOptions(this, options);
        this._setUpTileTemplateVars(template);

        if (template.tile.subdomains) {
          L.setOptions(this, L.extend(this.options, {subdomains: template.tile.subdomains}));
        }
        this._template = template;
        this._initContainer();
        // call the parent constructor with the template tref value, per the 
        // Leaflet tutorial: http://leafletjs.com/examples/extending/extending-1-classes.html#methods-of-the-parent-class
        L.TileLayer.prototype.initialize.call(this, template.template, L.extend(options, {pane: this._container}));
      },
      onAdd : function(){
        this._map._addZoomLimit(this);
        L.TileLayer.prototype.onAdd.call(this,this._map);
        this._handleMoveEnd();
      },

      getEvents: function(){
        let events = L.TileLayer.prototype.getEvents.call(this,this._map);
        this._parentOnMoveEnd = events.moveend;
        events.moveend = this._handleMoveEnd;
        return events;
      },

      _initContainer: function () {
        if (this._container) { return; }

        this._container = L.DomUtil.create('div', 'leaflet-layer', this.options.pane);
        L.DomUtil.addClass(this._container,'mapml-templated-tile-container');
        this._updateZIndex();
      },
      _handleMoveEnd : function(e){
        let mapZoom = this._map.getZoom();
        let mapBounds = M.pixelToPCRSBounds(this._map.getPixelBounds(),mapZoom,this._map.options.projection);
        this.isVisible = mapZoom <= this.options.maxZoom && mapZoom >= this.options.minZoom && 
                          this.extentBounds.overlaps(mapBounds);
          if(!(this.isVisible))return;
        this._parentOnMoveEnd();
      },
      createTile: function (coords) {
        let tileGroup = document.createElement("DIV"),
            tileSize = this.getTileSize();
        L.DomUtil.addClass(tileGroup, "mapml-tile-group");
        L.DomUtil.addClass(tileGroup, "leaflet-tile");

        this._template.linkEl.dispatchEvent(new CustomEvent('tileloadstart', {
          detail:{
            x:coords.x,
            y:coords.y,
            zoom:coords.z,
            appendTile: (elem)=>{tileGroup.appendChild(elem);},
          },
        }));

        if (this._template.type.startsWith('image/')) {
          let tile = L.TileLayer.prototype.createTile.call(this, coords, function(){});
          tile.width = tileSize.x;
          tile.height = tileSize.y;
          tileGroup.appendChild(tile);
        } else if(!this._url.includes(BLANK_TT_TREF)) {
          // tiles of type="text/mapml" will have to fetch content while creating
          // the tile here, unless there can be a callback associated to the element
          // that will render the content in the alread-placed tile
          // var tile = L.DomUtil.create('canvas', 'leaflet-tile');
          this._fetchTile(coords, tileGroup);
        }
        return tileGroup;
      },
      _mapmlTileReady: function(tile) {
          L.DomUtil.addClass(tile,'leaflet-tile-loaded');
      },
      // instead of being child of a pane, the TemplatedTileLayers are 'owned' by the group,
      // and so are DOM children of the group, not the pane element (the MapMLLayer is
      // a child of the overlay pane and always has a set of sub-layers)
      getPane: function() {
        return this.options.pane;
      },
      _fetchTile:  function (coords, tile) {
         fetch(this.getTileUrl(coords),{redirect: 'follow'}).then(
            function(response) {
              if (response.status >= 200 && response.status < 300) {
                return Promise.resolve(response);
              } else {
                console.log('Looks like there was a problem. Status Code: ' + response.status);
                return Promise.reject(response);
              }
            }).then(function(response) {
              return response.text();
            }).then(text => {
              var parser = new DOMParser();
                  return parser.parseFromString(text, "application/xml");
            }).then(mapml => {
              this._createFeatures(mapml, coords, tile);
              this._mapmlTileReady(tile);
            }).catch(err => {console.log("Error Creating Tile");});
      },

      _createFeatures: function(markup, coords, tile){
        let stylesheets = markup.querySelector('map-link[rel=stylesheet],map-style');
        if (stylesheets) {
          let base = markup.querySelector('map-base') && markup.querySelector('map-base').hasAttribute('href') ?
            new URL(markup.querySelector('map-base').getAttribute('href')).href :
            markup.URL;
          M.parseStylesheetAsHTML(markup,base,tile);
        }

        let svg = L.SVG.create('svg'), g = L.SVG.create('g'), tileSize = this._map.options.crs.options.crs.tile.bounds.max.x,
            xOffset = coords.x * tileSize, yOffset = coords.y * tileSize;

        let tileFeatures = M.mapMlFeatures(markup, {
          projection: this._map.options.projection,
          static: false,
          interactive: false,
        });

        for(let groupID in tileFeatures._layers){
          for(let featureID in tileFeatures._layers[groupID]._layers){
            let layer = tileFeatures._layers[groupID]._layers[featureID];
            M.FeatureRenderer.prototype._initPath(layer, false);
            layer._project(this._map, L.point([xOffset, yOffset]), coords.z);
            M.FeatureRenderer.prototype._addPath(layer, g, false);
            M.FeatureRenderer.prototype._updateFeature(layer);
          }
        }
        svg.setAttribute('width', tileSize.toString());
        svg.setAttribute('height', tileSize.toString());
        svg.appendChild(g);
        tile.appendChild(svg);
      },

      getTileUrl: function (coords) {
          if (coords.z >= this._template.tilematrix.bounds.length || 
                  !this._template.tilematrix.bounds[coords.z].contains(coords)) {
            return '';
          }
          var obj = {};
          obj[this._template.tilematrix.col.name] = coords.x;
          obj[this._template.tilematrix.row.name] = coords.y;
          obj[this._template.zoom.name] = this._getZoomForUrl();
          obj[this._template.pcrs.easting.left] = this._tileMatrixToPCRSPosition(coords, 'top-left').x;
          obj[this._template.pcrs.easting.right] = this._tileMatrixToPCRSPosition(coords, 'top-right').x;
          obj[this._template.pcrs.northing.top] = this._tileMatrixToPCRSPosition(coords, 'top-left').y;
          obj[this._template.pcrs.northing.bottom] = this._tileMatrixToPCRSPosition(coords, 'bottom-left').y;
          obj[this._template.tile.server] = this._getSubdomain(coords);
          for (var v in this._template.tile) {
              if (["row","col","zoom","left","right","top","bottom"].indexOf(v) < 0) {
                  obj[v] = this._template.tile[v];
              }
          }
          if (this._map && !this._map.options.crs.infinite) {
            let invertedY = this._globalTileRange.max.y - coords.y;
            if (this.options.tms) {
              obj[this._template.tilematrix.row.name] = invertedY;
            }
            //obj[`-${this._template.tilematrix.row.name}`] = invertedY; //leaflet has this but I dont see a use in storing row and -row as it doesnt follow that pattern
          }
          obj.r = this.options.detectRetina && L.Browser.retina && this.options.maxZoom > 0 ? '@2x' : '';
          return L.Util.template(this._url, obj);
      },
      _tileMatrixToPCRSPosition: function (coords, pos) {
  // this is a tile:
  // 
  //   top-left         top-center           top-right
  //      +------------------+------------------+
  //      |                  |                  |
  //      |                  |                  |
  //      |                  |                  |
  //      |                  |                  |
  //      |                  |                  |
  //      |                  |                  |
  //      + center-left    center               + center-right
  //      |                  |                  |
  //      |                  |                  |
  //      |                  |                  |
  //      |                  |                  |
  //      |                  |                  |
  //      |                  |                  |
  //      |                  |                  |
  //      +------------------+------------------+
  //   bottom-left     bottom-center      bottom-right

    var map = this._map,
        crs = map.options.crs,
        tileSize = this.getTileSize(),

        nwPoint = coords.scaleBy(tileSize),
        sePoint = nwPoint.add(tileSize),
        centrePoint = nwPoint.add(Math.floor(tileSize / 2)),

        nw = crs.transformation.untransform(nwPoint,crs.scale(coords.z)),
        se = crs.transformation.untransform(sePoint,crs.scale(coords.z)),
        cen = crs.transformation.untransform(centrePoint, crs.scale(coords.z)),
        result = null;

        switch (pos) {
          case('top-left'):
            result = nw;
            break;
          case('bottom-left'):
            result = new L.Point(nw.x,se.y);
            break;
          case('center-left'):
            result = new L.Point(nw.x,cen.y);
            break;
          case('top-right'):
            result = new L.Point(se.x,nw.y);
            break;
          case('bottom-right'):
            result = se;
            break;
          case('center-right'):
            result = new L.Point(se.x,cen.y);
            break;
          case('top-center'):
            result = new L.Point(cen.x,nw.y);
            break;
          case('bottom-center'):
            result = new L.Point(cen.x,se.y);
            break;
          case('center'):
            result = cen;
            break;
        }
        return result;
      },
      _setUpTileTemplateVars: function(template) {
        // process the inputs associated to template and create an object named
        // tile with member properties as follows:
        // {row: 'rowvarname', 
        //  col: 'colvarname', 
        //  left: 'leftvarname', 
        //  right: 'rightvarname', 
        //  top: 'topvarname', 
        //  bottom: 'bottomvarname'}
        template.tile = {};
        var inputs = template.values,
            crs = this.options.crs.options,
            zoom, east, north, row, col;
        
        for (var i=0;i<template.values.length;i++) {
          var type = inputs[i].getAttribute("type"), 
              units = inputs[i].getAttribute("units"), 
              axis = inputs[i].getAttribute("axis"), 
              name = inputs[i].getAttribute("name"), 
              position = inputs[i].getAttribute("position"),
              shard = (type === "hidden" && inputs[i].hasAttribute("shard")),
              select = (inputs[i].tagName.toLowerCase() === "map-select"),
              value = inputs[i].getAttribute("value"),
              min = inputs[i].getAttribute("min"),
              max = inputs[i].getAttribute("max");
          if (type === "location" && units === "tilematrix") {
            switch (axis) {
              case("column"):
                col = {
                  name: name,
                  min: crs.crs.tilematrix.horizontal.min,
                  max: crs.crs.tilematrix.horizontal.max(crs.resolutions.length-1)
                };
                if (!isNaN(Number.parseFloat(min))) {
                  col.min = Number.parseFloat(min);
                }
                if (!isNaN(Number.parseFloat(max))) {
                  col.max = Number.parseFloat(max);
                }
                break;
              case("row"):
                row = {
                  name: name,
                  min: crs.crs.tilematrix.vertical.min,
                  max:  crs.crs.tilematrix.vertical.max(crs.resolutions.length-1)
                };
                if (!isNaN(Number.parseFloat(min))) {
                  row.min = Number.parseFloat(min);
                }
                if (!isNaN(Number.parseFloat(max))) {
                  row.max = Number.parseFloat(max);
                }
                break;
              case('longitude'):
              case("easting"):
                if (!east) {
                  east = {
                    min: crs.crs.pcrs.horizontal.min,
                    max: crs.crs.pcrs.horizontal.max
                  };
                }
                if (!isNaN(Number.parseFloat(min))) {
                  east.min = Number.parseFloat(min);
                }
                if (!isNaN(Number.parseFloat(max))) {
                  east.max = Number.parseFloat(max);
                }
                if (position) {
                  if (position.match(/.*?-left/i)) {
                    east.left = name;
                  } else if (position.match(/.*?-right/i)) {
                    east.right = name;
                  }
                }
                break;
              case('latitude'):
              case("northing"):
                if (!north) {
                  north = {
                    min: crs.crs.pcrs.vertical.min,
                    max: crs.crs.pcrs.vertical.max
                  };
                }
                if (!isNaN(Number.parseFloat(min))) {
                  north.min = Number.parseFloat(min);
                }
                if (!isNaN(Number.parseFloat(max))) {
                  north.max = Number.parseFloat(max);
                }
                if (position) {
                  if (position.match(/top-.*?/i)) {
                    north.top = name;
                  } else if (position.match(/bottom-.*?/i)) {
                    north.bottom = name;
                  }
                } 
                break;
                // unsuportted axis value
            }
          } else if (type && type.toLowerCase() === "zoom") {
            //<input name="..." type="zoom" value="0" min="0" max="17">
             zoom = {
               name: name,
               min: 0, 
               max: crs.resolutions.length,
               value: crs.resolutions.length 
             };
             if (!isNaN(Number.parseInt(value,10)) && 
                     Number.parseInt(value,10) >= zoom.min && 
                     Number.parseInt(value,10) <= zoom.max) {
               zoom.value = Number.parseInt(value,10);
             } else {
               zoom.value = zoom.max;
             }
             if (!isNaN(Number.parseInt(min,10)) && 
                     Number.parseInt(min,10) >= zoom.min && 
                     Number.parseInt(min,10) <= zoom.max) {
               zoom.min = Number.parseInt(min,10);
             }
             if (!isNaN(Number.parseInt(max,10)) && 
                     Number.parseInt(max,10) >= zoom.min && 
                     Number.parseInt(max,10) <= zoom.max) {
               zoom.max = Number.parseInt(max,10);
             }
             template.zoom = zoom;
          } else if (shard) {
            template.tile.server = name;
            template.tile.subdomains = inputs[i].servers.slice();
          } else if (select) {
              /*jshint -W104 */
            const parsedselect = inputs[i].htmlselect;
            template.tile[name] = function() {
                return parsedselect.value;
            };
          } else {
             // needs to be a const otherwise it gets overwritten
            /*jshint -W104 */
            const input = inputs[i];
            template.tile[name] = function () {
                return input.getAttribute("value");
            };
          }
        }
        var transformation = this.options.crs.transformation,
            tileSize = this.options.crs.options.crs.tile.bounds.max.x,
            scale = L.bind(this.options.crs.scale, this.options.crs),
        pcrs2tilematrix = function(c,zoom) {
          return transformation.transform(c, scale(zoom)).divideBy(tileSize).floor();
        };
        if (east && north) {
          
          template.pcrs = {};
          template.pcrs.bounds = L.bounds([east.min,north.min],[east.max,north.max]);
          template.pcrs.easting = east;
          template.pcrs.northing = north;
          
        } else if ( col && row && !isNaN(zoom.value)) {
            
            // convert the tile bounds at this zoom to a pcrs bounds, then 
            // go through the zoom min/max and create a tile-based bounds
            // at each zoom that applies to the col/row values that constrain what tiles
            // will be requested so that we don't generate too many 404s
            if (!template.pcrs) {
              template.pcrs = {};
              template.pcrs.easting = '';
              template.pcrs.northing = '';
            }
            
            template.pcrs.bounds =  M.boundsToPCRSBounds(
                        L.bounds(L.point([col.min,row.min]),
                        L.point([col.max,row.max])),
                        zoom.value, this.options.crs, M.axisToCS("column"));
            
            template.tilematrix = {};
            template.tilematrix.col = col;
            template.tilematrix.row = row;

        } else {
          console.log('Unable to determine bounds for tile template: ' + template.template);
        }
        
        if (!template.tilematrix) {
          template.tilematrix = {};
          template.tilematrix.col = {};
          template.tilematrix.row = {};
        }
        template.tilematrix.bounds = [];
        var pcrsBounds = template.pcrs.bounds;
        // the template should _always_ have a zoom, because we force it to
        // by first processing the extent to determine the zoom and if none, adding
        // one and second by copying that zoom into the set of template variable inputs
        // even if it is not referenced by one of the template's variable references
        var zmin = template.zoom?template.zoom.min:0,
            zmax = template.zoom?template.zoom.max:crs.resolutions.length;
        for (var z=0; z <= zmax; z++) {
          template.tilematrix.bounds[z] = (z >= zmin ?
              L.bounds(pcrs2tilematrix(pcrsBounds.min,z),
                pcrs2tilematrix(pcrsBounds.max,z)) :
                        L.bounds(L.point([-1,-1]),L.point([-1,-1])));
        }
      }
  });
  var templatedTileLayer = function(template, options) {
    return new TemplatedTileLayer(template, options);
  };

  var TemplatedLayer = L.Layer.extend({
    initialize: function(templates, options) {
      this._templates =  templates;
      L.setOptions(this, options);
      this._container = L.DomUtil.create('div', 'leaflet-layer', options.pane);
      this._container.style.opacity = this.options.opacity;
      L.DomUtil.addClass(this._container,'mapml-templatedlayer-container');

      for (var i=0;i<templates.length;i++) {
        if (templates[i].rel === 'tile') {
            this.setZIndex(options.extentZIndex);
            this._templates[i].layer = M.templatedTileLayer(templates[i], 
              L.Util.extend(options, {errorTileUrl: "data:image/gif;base64,R0lGODlhAQABAAAAACH5BAEKAAEALAAAAAABAAEAAAICTAEAOw==", zIndex: options.extentZIndex, pane: this._container}));
        } else if (templates[i].rel === 'image') {
            this.setZIndex(options.extentZIndex);
            this._templates[i].layer = M.templatedImageLayer(templates[i], L.Util.extend(options, {zIndex: options.extentZIndex, pane: this._container}));
        } else if (templates[i].rel === 'features') {
            this.setZIndex(options.extentZIndex);
            this._templates[i].layer = M.templatedFeaturesLayer(templates[i], L.Util.extend(options, {zIndex: options.extentZIndex, pane: this._container}));
        } else if (templates[i].rel === 'query') {
            // add template to array of queryies to be added to map and processed
            // on click/tap events
            this.hasSetBoundsHandler = true;
            if (!this._queries) {
              this._queries = [];
            }
            let inputData = M.extractInputBounds(templates[i]);
            templates[i].extentBounds = inputData.bounds;
            templates[i].zoomBounds = inputData.zoomBounds;
            this._queries.push(L.extend(templates[i], this._setupQueryVars(templates[i])));
        }
      }
    },
    getEvents: function() {
        return {
            zoomstart: this._onZoomStart
        };
    },
    redraw: function() {
      this.closePopup();
      for (var i=0;i<this._templates.length;i++) {
        if (this._templates[i].rel === 'tile' || this._templates[i].rel === 'image' || this._templates[i].rel === 'features') {
            this._templates[i].layer.redraw();
        }
      }
    },
    _onZoomStart: function() {
        this.closePopup();
    },

    _setupQueryVars: function(template) {
        // process the inputs associated to template and create an object named
        // query with member properties as follows:
        // {width: 'widthvarname', 
        //  height: 'heightvarname', 
        //  left: 'leftvarname', 
        //  right: 'rightvarname', 
        //  top: 'topvarname', 
        //  bottom: 'bottomvarname'
        //  i: 'ivarname'
        //  j: 'jvarname'}
        //  x: 'xvarname' x being the tcrs x axis
        //  y: 'yvarname' y being the tcrs y axis
        //  z: 'zvarname' zoom
        //  title: link title

        var queryVarNames = {query:{}},
            inputs = template.values;
        
        for (var i=0;i<template.values.length;i++) {
          var type = inputs[i].getAttribute("type"), 
              units = inputs[i].getAttribute("units"), 
              axis = inputs[i].getAttribute("axis"), 
              name = inputs[i].getAttribute("name"), 
              position = inputs[i].getAttribute("position"),
              rel = inputs[i].getAttribute("rel"),
              select = (inputs[i].tagName.toLowerCase() === "map-select");
          if (type === "width") {
                queryVarNames.query.width = name;
          } else if ( type === "height") {
                queryVarNames.query.height = name;
          } else if (type === "location") { 
            switch (axis) {
              case('x'):
              case('y'):
              case("column"): 
              case("row"):
                queryVarNames.query[axis] = name;
                break;
              case ('longitude'):
              case ('easting'):
                if (position) {
                    if (position.match(/.*?-left/i)) {
                      if (rel === "pixel") {
                        queryVarNames.query.pixelleft = name;
                      } else if (rel === "tile") {
                        queryVarNames.query.tileleft = name;
                      } else {
                        queryVarNames.query.mapleft = name;
                      }
                    } else if (position.match(/.*?-right/i)) {
                      if (rel === "pixel") {
                        queryVarNames.query.pixelright = name;
                      } else if (rel === "tile") {
                        queryVarNames.query.tileright = name;
                      } else {
                        queryVarNames.query.mapright = name;
                      }
                    }
                } else {
                    queryVarNames.query[axis] = name;
                }
                break;
              case ('latitude'):
              case ('northing'):
                if (position) {
                    if (position.match(/top-.*?/i)) {
                      if (rel === "pixel") {
                        queryVarNames.query.pixeltop = name;
                      } else if (rel === "tile") {
                        queryVarNames.query.tiletop = name;
                      } else {
                        queryVarNames.query.maptop = name;
                      }
                    } else if (position.match(/bottom-.*?/i)) {
                      if (rel === "pixel") {
                        queryVarNames.query.pixelbottom = name;
                      } else if (rel === "tile") {
                        queryVarNames.query.tilebottom = name;
                      } else {
                        queryVarNames.query.mapbottom = name;
                      }
                    }
                } else {
                  queryVarNames.query[axis] = name;
                }
                break;
              case('i'):
                if (units === "tile") {
                  queryVarNames.query.tilei = name;
                } else {
                  queryVarNames.query.mapi = name;
                }
                break;
              case('j'):
                if (units === "tile") {
                  queryVarNames.query.tilej = name;
                } else {
                  queryVarNames.query.mapj = name;
                }
                break;
                // unsuportted axis value
            }
          } else if (type === "zoom") {
            //<input name="..." type="zoom" value="0" min="0" max="17">
             queryVarNames.query.zoom = name;
          } else if (select) {
              /*jshint -W104 */
            const parsedselect = inputs[i].htmlselect;
            queryVarNames.query[name] = function() {
                return parsedselect.value;
            };
          } else {
              /*jshint -W104 */
              const input = inputs[i];
             queryVarNames.query[name] = function () {
                return input.getAttribute("value");
             };
          }
        }
        queryVarNames.query.title = template.title;
        return queryVarNames;
    },
    reset: function (templates, extentZIndex) {
      if (!templates) {return;}
      if (!this._map) {return;}
      var addToMap = this._map && this._map.hasLayer(this),
          old_templates = this._templates;
      delete this._queries;
      this._map.off('click', null, this);

      this._templates = templates;
      for (var i=0;i<templates.length;i++) {
        if (templates[i].rel === 'tile') {
            this._templates[i].layer = M.templatedTileLayer(templates[i],
              L.Util.extend(this.options, {errorTileUrl: "data:image/gif;base64,R0lGODlhAQABAAAAACH5BAEKAAEALAAAAAABAAEAAAICTAEAOw==", zIndex: extentZIndex, pane: this._container}));
        } else if (templates[i].rel === 'image') {
            this._templates[i].layer = M.templatedImageLayer(templates[i], L.Util.extend(this.options, {zIndex: extentZIndex, pane: this._container}));
        } else if (templates[i].rel === 'features') {
            this._templates[i].layer = M.templatedFeaturesLayer(templates[i], L.Util.extend(this.options, {zIndex: extentZIndex, pane: this._container}));
        } else if (templates[i].rel === 'query') {
            if (!this._queries) {
              this._queries = [];
            }
            this._queries.push(L.extend(templates[i], this._setupQueryVars(templates[i])));
        }
        if (addToMap) {
          this.onAdd(this._map);
        }
      }
      for (i=0;i<old_templates.length;i++) {
        if (this._map.hasLayer(old_templates[i].layer)) {
          this._map.removeLayer(old_templates[i].layer);
        }
      }
    },
    onAdd: function (map) {
      for (var i=0;i<this._templates.length;i++) {
        if (this._templates[i].rel !== 'query') {
          map.addLayer(this._templates[i].layer);
        }
      }
    },
    //addTo: function(map) {
      //for(let i = 0; i < this._templates.length; i++){
    //    this._templates[0].layer.addTo(map);
      //}
    //},
    setZIndex: function (zIndex) {
        this.options.zIndex = zIndex;
        this._updateZIndex();

        return this;
    },
    _updateZIndex: function () {
        if (this._container && this.options.zIndex !== undefined && this.options.zIndex !== null) {
            this._container.style.zIndex = this.options.zIndex;
        }
    },
    onRemove: function (map) {
      L.DomUtil.remove(this._container);
      for (var i=0;i<this._templates.length;i++) {
        if (this._templates[i].rel !== 'query') {
          map.removeLayer(this._templates[i].layer);
        }
      }
    },

    _previousFeature: function(e){
      if(this._count + -1 >= 0){
        this._count--;
        this._map.fire("featurepagination", {i: this._count, popup: this});
      }
    },
    
    _nextFeature: function(e){
      if(this._count + 1 < this._source._totalFeatureCount){
        this._count++;
        this._map.fire("featurepagination", {i: this._count, popup: this});
      }
    },

    changeOpacity: function(opacity){
      this._container.style.opacity = opacity;
    },

  });
  var templatedLayer = function(templates, options) {
    // templates is an array of template objects
    // a template object contains the template, plus associated <input> elements
    // which need to be processed just prior to creating a url from the template 
    // with the values of the inputs
    return new TemplatedLayer(templates, options);
  };

  var TemplatedFeaturesLayer =  L.Layer.extend({
    // this and M.ImageLayer could be merged or inherit from a common parent
      initialize: function(template, options) {
        let inputData = M.extractInputBounds(template);
        this.zoomBounds = inputData.zoomBounds;
        this.extentBounds=inputData.bounds;
        this.isVisible = true;
        this._template = template;
        this._container = L.DomUtil.create('div', 'leaflet-layer', options.pane);
        L.extend(options, this.zoomBounds);
        L.DomUtil.addClass(this._container, 'mapml-features-container');
        delete options.opacity;
        L.setOptions(this, L.extend(options,this._setUpFeaturesTemplateVars(template)));
      },
      getEvents: function () {
          var events = {
              moveend: this._onMoveEnd
          };
          return events;
      },
      onAdd: function () {
        this._map._addZoomLimit(this);
        var mapml, headers = new Headers({'Accept': 'text/mapml'});
            var parser = new DOMParser(),
            opacity = this.options.opacity || 1,
            container = this._container,
            map = this._map;
        if (!this._features) {
          this._features = M.mapMlFeatures( null, {
            // pass the vector layer a renderer of its own, otherwise leaflet
            // puts everything into the overlayPane
            renderer: M.featureRenderer(),
            // pass the vector layer the container for the parent into which
            // it will append its own container for rendering into
            pane: container,
            opacity: opacity,
            projection:map.options.projection,
            static: true,
            onEachFeature: function(properties, geometry) {
              // need to parse as HTML to preserve semantics and styles
              var c = document.createElement('div');
              c.classList.add("mapml-popup-content");
              c.insertAdjacentHTML('afterbegin', properties.innerHTML);
              geometry.bindPopup(c, {autoClose: false, minWidth: 108});
            }
          });
        }
        // this was tricky...recursion alwasy breaks my brain
        var features = this._features,
            _pullFeatureFeed = function (url, limit) {
              return (fetch (url,{redirect: 'follow',headers: headers})
                      .then( function (response) {return response.text();})
                      .then( function (text) {
                mapml = parser.parseFromString(text,"application/xml");
                var base = (new URL(mapml.querySelector('map-base') ? mapml.querySelector('map-base').getAttribute('href') : url)).href;
                url = mapml.querySelector('map-link[rel=next]')? mapml.querySelector('map-link[rel=next]').getAttribute('href') : null;
                url =  url ? (new URL(url, base)).href: null;
                let nativeZoom = mapml.querySelector("map-meta[name=zoom]") &&
                  +M.metaContentToObject(mapml.querySelector("map-meta[name=zoom]").getAttribute("content")).value || 0;
                let nativeCS = mapml.querySelector("map-meta[name=cs]") &&
                        M.metaContentToObject(mapml.querySelector("map-meta[name=cs]").getAttribute("content")).content || "GCRS";
                features.addData(mapml, nativeCS, nativeZoom);
                if (url && --limit) {
                  return _pullFeatureFeed(url, limit);
                }
              }));
            };
        _pullFeatureFeed(this._getfeaturesUrl(), 10)
          .then(function() { 
                map.addLayer(features);
                map.fire('moveend');  // TODO: replace with moveend handler for layer and not entire map
          })
          .catch(function (error) { console.log(error);});
      },
      redraw: function() {
          this._onMoveEnd();
      },

      _onMoveEnd: function() {
        let mapZoom = this._map.getZoom();
        let mapBounds = M.pixelToPCRSBounds(this._map.getPixelBounds(),mapZoom,this._map.options.projection);
        this.isVisible = mapZoom <= this.zoomBounds.maxZoom && mapZoom >= this.zoomBounds.minZoom && 
                          this.extentBounds.overlaps(mapBounds);
        
        this._features.clearLayers();
        if(!(this.isVisible)){
          return;
        }

        // TODO add preference with a bit less weight than that for text/mapml; 0.8 for application/geo+json; 0.6
        var mapml, headers = new Headers({'Accept': 'text/mapml;q=0.9,application/geo+json;q=0.8'}),
            parser = new DOMParser(),
            features = this._features,
            map = this._map,
            context = this,
            MAX_PAGES = 10,
          _pullFeatureFeed = function (url, limit) {
            return (fetch (url,{redirect: 'follow',headers: headers})
                    .then( function (response) {return response.text();})
                    .then( function (text) {
                      //TODO wrap this puppy in a try/catch/finally to parse application/geo+json if necessary
                mapml = parser.parseFromString(text,"application/xml");
                var base = (new URL(mapml.querySelector('map-base') ? mapml.querySelector('map-base').getAttribute('href') : url)).href;
                url = mapml.querySelector('map-link[rel=next]')? mapml.querySelector('map-link[rel=next]').getAttribute('href') : null;
                url =  url ? (new URL(url, base)).href: null;
                // TODO if the xml parser barfed but the response is application/geo+json, use the parent addData method
              let nativeZoom = mapml.querySelector("map-meta[name=zoom]") &&
                                +M.metaContentToObject(mapml.querySelector("map-meta[name=zoom]").getAttribute("content")).value || 0;
              let nativeCS = mapml.querySelector("map-meta[name=cs]") &&
                                M.metaContentToObject(mapml.querySelector("map-meta[name=cs]").getAttribute("content")).content || "GCRS";
              features.addData(mapml, nativeCS, nativeZoom);
              if (url && --limit) {
                return _pullFeatureFeed(url, limit);
              }
            }));
          };
        _pullFeatureFeed(this._getfeaturesUrl(), MAX_PAGES)
          .then(function() { 
            map.addLayer(features);
            M.TemplatedFeaturesLayer.prototype._updateTabIndex(context);
          })
          .catch(function (error) { console.log(error);});
      },
      setZIndex: function (zIndex) {
          this.options.zIndex = zIndex;
          this._updateZIndex();
          return this;
      },
      _updateTabIndex: function(context){
        let c = context || this;
        for(let layerNum in c._features._layers){
          let layer = c._features._layers[layerNum];
          if(layer._path){
            if(layer._path.getAttribute("d") !== "M0 0"){
              layer._path.setAttribute("tabindex", 0);
            } else {
              layer._path.removeAttribute("tabindex");
            }
            if(layer._path.childElementCount === 0) {
              let title = document.createElement("title");
              title.innerText = "Feature";
              layer._path.appendChild(title);
            }
          }
        }
      },
      _updateZIndex: function () {
          if (this._container && this.options.zIndex !== undefined && this.options.zIndex !== null) {
              this._container.style.zIndex = this.options.zIndex;
          }
      },
      onRemove: function () {
        this._map.removeLayer(this._features);
      },
      _getfeaturesUrl: function() {
          var obj = {};
          if (this.options.feature.zoom) {
            obj[this.options.feature.zoom] = this._map.getZoom();
          }
          if (this.options.feature.width) {
            obj[this.options.feature.width] = this._map.getSize().x;
          }
          if (this.options.feature.height) {
            obj[this.options.feature.height] = this._map.getSize().y;
          }
          if (this.options.feature.bottom) {
            obj[this.options.feature.bottom] = this._TCRSToPCRS(this._map.getPixelBounds().max,this._map.getZoom()).y;
          }
          if (this.options.feature.left) {
            obj[this.options.feature.left] = this._TCRSToPCRS(this._map.getPixelBounds().min, this._map.getZoom()).x;
          }
          if (this.options.feature.top) {
            obj[this.options.feature.top] = this._TCRSToPCRS(this._map.getPixelBounds().min, this._map.getZoom()).y;
          }
          if (this.options.feature.right) {
            obj[this.options.feature.right] = this._TCRSToPCRS(this._map.getPixelBounds().max,this._map.getZoom()).x;
          }
          // hidden and other variables that may be associated
          for (var v in this.options.feature) {
              if (["width","height","left","right","top","bottom","zoom"].indexOf(v) < 0) {
                  obj[v] = this.options.feature[v];
                }
          }
          return L.Util.template(this._template.template, obj);
      },
      _TCRSToPCRS: function(coords, zoom) {
        // TCRS pixel point to Projected CRS point (in meters, presumably)
        var map = this._map,
            crs = map.options.crs,
            loc = crs.transformation.untransform(coords,crs.scale(zoom));
            return loc;
      },
      _setUpFeaturesTemplateVars: function(template) {
        // process the inputs and create an object named "extent"
        // with member properties as follows:
        // {width: {name: 'widthvarname'}, // value supplied by map if necessary
        //  height: {name: 'heightvarname'}, // value supplied by map if necessary
        //  left: {name: 'leftvarname', axis: 'leftaxisname'}, // axis name drives (coordinate system of) the value supplied by the map
        //  right: {name: 'rightvarname', axis: 'rightaxisname'}, // axis name (coordinate system of) drives the value supplied by the map
        //  top: {name: 'topvarname', axis: 'topaxisname'}, // axis name drives (coordinate system of) the value supplied by the map
        //  bottom: {name: 'bottomvarname', axis: 'bottomaxisname'} // axis name drives (coordinate system of) the value supplied by the map
        //  zoom: {name: 'zoomvarname'}
        //  hidden: [{name: name, value: value}]}

        var featuresVarNames = {feature:{}},
            inputs = template.values;
        featuresVarNames.feature.hidden = [];
        for (var i=0;i<inputs.length;i++) {
          // this can be removed when the spec removes the deprecated inputs...
          var type = inputs[i].getAttribute("type"), 
              units = inputs[i].getAttribute("units"), 
              axis = inputs[i].getAttribute("axis"), 
              name = inputs[i].getAttribute("name"), 
              position = inputs[i].getAttribute("position"),
              value = inputs[i].getAttribute("value"),
              select = (inputs[i].tagName.toLowerCase() === "map-select");
          if (type === "width") {
                featuresVarNames.feature.width = name;
          } else if ( type === "height") {
                featuresVarNames.feature.height = name;
          } else if (type === "zoom") {
                featuresVarNames.feature.zoom = name;
          } else if (type === "location" && (units === "pcrs" || units ==="gcrs") ) {
            //<input name="..." units="pcrs" type="location" position="top|bottom-left|right" axis="northing|easting">
            switch (axis) {
              case ('x'):
              case ('longitude'):
              case ('easting'):
                if (position) {
                    if (position.match(/.*?-left/i)) {
                      featuresVarNames.feature.left = name;
                    } else if (position.match(/.*?-right/i)) {
                      featuresVarNames.feature.right = name;
                    }
                }
                break;
              case ('y'):
              case ('latitude'):
              case ('northing'):
                if (position) {
                  if (position.match(/top-.*?/i)) {
                    featuresVarNames.feature.top = name;
                  } else if (position.match(/bottom-.*?/i)) {
                    featuresVarNames.feature.bottom = name;
                  }
                }
                break;
            }
           } else if (select) {
              /*jshint -W104 */
            const parsedselect = inputs[i].htmlselect;
            featuresVarNames.feature[name] = function() {
                return parsedselect.value;
            };
           // projection is deprecated, make it hidden
          } else {
              /*jshint -W104 */
              const input = inputs[i];
              featuresVarNames.feature[name] = function() {
                  return input.getAttribute("value");
              };
          }
        }
        return featuresVarNames;
      }
  });
  var templatedFeaturesLayer = function(template, options) {
      return new TemplatedFeaturesLayer(template, options);
  };

  var TemplatedImageLayer =  L.Layer.extend({
      initialize: function(template, options) {
          this._template = template;
          this._container = L.DomUtil.create('div', 'leaflet-layer', options.pane);
          L.DomUtil.addClass(this._container, 'mapml-image-container');
          let inputData = M.extractInputBounds(template);
          this.zoomBounds = inputData.zoomBounds;
          this.extentBounds=inputData.bounds;
          this.isVisible = true;
          delete options.opacity;
          L.extend(options, this.zoomBounds);
          L.setOptions(this, L.extend(options,this._setUpExtentTemplateVars(template)));
      },
      getEvents: function () {
          var events = {
              moveend: this._onMoveEnd
          };
          return events;
      },
      onAdd: function () {
          this._map._addZoomLimit(this);  //used to set the zoom limit of the map
          this.setZIndex(this.options.zIndex);
          this._onMoveEnd();
      },
      redraw: function() {
          this._onMoveEnd();
      },

      _clearLayer: function(){
        let containerImages = this._container.querySelectorAll('img');
        for(let i = 0; i< containerImages.length;i++){
          this._container.removeChild(containerImages[i]);
        }
      },

      _onMoveEnd: function() {
        let mapZoom = this._map.getZoom();
        let mapBounds = M.pixelToPCRSBounds(this._map.getPixelBounds(),mapZoom,this._map.options.projection);
        this.isVisible = mapZoom <= this.zoomBounds.maxZoom && mapZoom >= this.zoomBounds.minZoom && 
                          this.extentBounds.overlaps(mapBounds);
        if(!(this.isVisible)){
          this._clearLayer();
          return;
        }
        var map = this._map,
          loc = map.getPixelBounds().min.subtract(map.getPixelOrigin()),
          size = map.getSize(),
          src = this.getImageUrl(),
          overlayToRemove = this._imageOverlay;
          this._imageOverlay = M.imageOverlay(src,loc,size,0,this._container);
            
        this._imageOverlay.addTo(map);
        if (overlayToRemove) {
          this._imageOverlay.on('load error', function () {map.removeLayer(overlayToRemove);});
        }
      },
      setZIndex: function (zIndex) {
          this.options.zIndex = zIndex;
          this._updateZIndex();

          return this;
      },
      _updateZIndex: function () {
          if (this._container && this.options.zIndex !== undefined && this.options.zIndex !== null) {
              this._container.style.zIndex = this.options.zIndex;
          }
      },
      onRemove: function (map) {
        this._clearLayer();
        map._removeZoomLimit(this);
        this._container = null;
      },
      getImageUrl: function() {
          var obj = {};
          obj[this.options.extent.width] = this._map.getSize().x;
          obj[this.options.extent.height] = this._map.getSize().y;
          obj[this.options.extent.bottom] = this._TCRSToPCRS(this._map.getPixelBounds().max,this._map.getZoom()).y;
          obj[this.options.extent.left] = this._TCRSToPCRS(this._map.getPixelBounds().min, this._map.getZoom()).x;
          obj[this.options.extent.top] = this._TCRSToPCRS(this._map.getPixelBounds().min, this._map.getZoom()).y;
          obj[this.options.extent.right] = this._TCRSToPCRS(this._map.getPixelBounds().max,this._map.getZoom()).x;
          // hidden and other variables that may be associated
          for (var v in this.options.extent) {
              if (["width","height","left","right","top","bottom"].indexOf(v) < 0) {
                  obj[v] = this.options.extent[v];
                }
          }
          return L.Util.template(this._template.template, obj);
      },
      _TCRSToPCRS: function(coords, zoom) {
        // TCRS pixel point to Projected CRS point (in meters, presumably)
        var map = this._map,
            crs = map.options.crs,
            loc = crs.transformation.untransform(coords,crs.scale(zoom));
            return loc;
      },
      _setUpExtentTemplateVars: function(template) {
        // process the inputs associated to template and create an object named
        // extent with member properties as follows:
        // {width: 'widthvarname', 
        //  height: 'heightvarname', 
        //  left: 'leftvarname', 
        //  right: 'rightvarname', 
        //  top: 'topvarname', 
        //  bottom: 'bottomvarname'}

        var extentVarNames = {extent:{}},
            inputs = template.values;
        
        for (var i=0;i<template.values.length;i++) {
          var type = inputs[i].getAttribute("type"), 
              units = inputs[i].getAttribute("units"), 
              axis = inputs[i].getAttribute("axis"), 
              name = inputs[i].getAttribute("name"), 
              position = inputs[i].getAttribute("position"),
              select = (inputs[i].tagName.toLowerCase() === "map-select");
          if (type === "width") {
                extentVarNames.extent.width = name;
          } else if ( type === "height") {
                extentVarNames.extent.height = name;
          } else if (type === "location" && (units === "pcrs" || units ==="gcrs") ) {
            //<input name="..." units="pcrs" type="location" position="top|bottom-left|right" axis="northing|easting|latitude|longitude">
            switch (axis) {
              case ('longitude'):
              case ('easting'):
                if (position) {
                    if (position.match(/.*?-left/i)) {
                      extentVarNames.extent.left = name;
                    } else if (position.match(/.*?-right/i)) {
                      extentVarNames.extent.right = name;
                    }
                }
                break;
              case ('latitude'):
              case ('northing'):
                if (position) {
                  if (position.match(/top-.*?/i)) {
                    extentVarNames.extent.top = name;
                  } else if (position.match(/bottom-.*?/i)) {
                    extentVarNames.extent.bottom = name;
                  }
                }
                break;
            }
          } else if (select) {
              /*jshint -W104 */
            const parsedselect = inputs[i].htmlselect;
            extentVarNames.extent[name] = function() {
                return parsedselect.value;
            };
          } else {
              /*jshint -W104 */
              const input = inputs[i];
              extentVarNames.extent[name] = function() {
                  return input.getAttribute("value");
              };
          }
        }
        return extentVarNames;
      },
  });
  var templatedImageLayer = function(template, options) {
      return new TemplatedImageLayer(template, options);
  };

  var ImageOverlay = L.ImageOverlay.extend({
  	initialize: function (url, location, size, angle, container, options) { // (String, Point, Point, Number, Element, Object)
                  this._container = container;
  		this._url = url;
                  // instead of calculating where the image goes, put it at 0,0
  		//this._location = L.point(location);
                  // the location for WMS requests will be the upper left hand
                  // corner of the map.  When the map is initialized, that is 0,0,
                  // but as the user pans, of course the
  		this._location = location;
                  this._size = L.point(size);
                  this._angle = angle;

  		L.setOptions(this, options);
  	},
          getEvents: function() {
  		var events = {
  			viewreset: this._reset
  		};

  		if (this._zoomAnimated) {
  			events.zoomanim = this._animateZoom;
  		}

  		return events;
          },
  	onAdd: function () {
                  this.on({ 
                    load: this._onImageLoad
                    });

  		if (!this._image) {
                      this._initImage();
  		}

  		if (this.options.interactive) {
  			L.DomUtil.addClass(this._image, 'leaflet-interactive');
  			this.addInteractiveTarget(this._image);
  		}

  		this._container.appendChild(this._image);
  		this._reset();
  	},
  	onRemove: function () {
  		L.DomUtil.remove(this._image);
  		if (this.options.interactive) {
  			this.removeInteractiveTarget(this._image);
  		}
  	},
          _onImageLoad: function () {
              if (!this._image) { return; }
              this._image.loaded =  +new Date();
              this._updateOpacity();
          },
  	_animateZoom: function (e) {
  		var scale = this._map.getZoomScale(e.zoom),
  		    translate = this._map.getPixelOrigin().add(this._location).multiplyBy(scale)
  		        .subtract(this._map._getNewPixelOrigin(e.center, e.zoom)).round();

  		if (L.Browser.any3d) {
  			L.DomUtil.setTransform(this._image, translate, scale);
  		} else {
  			L.DomUtil.setPosition(this._image, translate);
  		}
  	},
          _reset: function () {
  		var image = this._image,
  		    location = this._location,
                      size = this._size;

                  // TBD use the angle to establish the image rotation in CSS

  		L.DomUtil.setPosition(image, location);

  		image.style.width  = size.x + 'px';
  		image.style.height = size.y + 'px';
          },
  	_updateOpacity: function () {
  		if (!this._map) { return; }

  		//L.DomUtil.setOpacity(this._image, this.options.opacity);

  		var now = +new Date(),
  		    nextFrame = false;

                  var image = this._image;

                  var fade = Math.min(1, (now - image.loaded) / 200);

                  L.DomUtil.setOpacity(image, fade);
                  if (fade < 1) {
                          nextFrame = true;
                  } 
  		if (nextFrame) {
  			L.Util.cancelAnimFrame(this._fadeFrame);
  			this._fadeFrame = L.Util.requestAnimFrame(this._updateOpacity, this);
  		}
                  L.DomUtil.addClass(image, 'leaflet-image-loaded');
  	}
          
  });
  var imageOverlay = function (url, location, size, angle, container, options) {
          return new ImageOverlay(url, location, size, angle, container, options);
  };

  var MapMLLayer = L.Layer.extend({
      // zIndex has to be set, for the case where the layer is added to the
      // map before the layercontrol is used to control it (where autoZindex is used)
      // e.g. in the raw MapML-Leaflet-Client index.html page.
      options: {
          maxNext: 10,
          zIndex: 0,
          maxZoom: 25
      },
      // initialize is executed before the layer is added to a map
      initialize: function (href, content, options) {
          // in the custom element, the attribute is actually 'src'
          // the _href version is the URL received from layer-@src
          var mapml;
          if (href) {
              this._href = href;
          }
          if (content) {
            this._layerEl = content;
            mapml = content.querySelector('map-feature,map-tile,map-extent') ? true : false;
            if (!href && mapml) {
                this._content = content;
            }
          }
          L.setOptions(this, options);
          this._container = L.DomUtil.create('div', 'leaflet-layer');
          L.DomUtil.addClass(this._container,'mapml-layer');
          this._imageContainer = L.DomUtil.create('div', 'leaflet-layer', this._container);
          L.DomUtil.addClass(this._imageContainer,'mapml-image-container');
          
          // this layer 'owns' a mapmlTileLayer, which is a subclass of L.GridLayer
          // it 'passes' what tiles to load via the content of this._mapmlTileContainer
          this._mapmlTileContainer = L.DomUtil.create('div', 'mapml-tile-container', this._container);
          // hit the service to determine what its extent might be
          // OR use the extent of the content provided

          if (!mapml && content && content.hasAttribute('label')) this._title = content.getAttribute('label');
          this._initialize(mapml ? content : null);
          
          // a default extent can't be correctly set without the map to provide
          // its bounds , projection, zoom range etc, so if that stuff's not
          // established by metadata in the content, we should use map properties
          // to set the extent, but the map won't be available until the <layer>
          // element is attached to the <map> element, wait for that to happen.
          this.on('attached', this._validateExtent, this );
          // weirdness.  options is actually undefined here, despite the hardcoded
          // options above. If you use this.options, you see the options defined
          // above.  Not going to change this, but failing to understand ATM.
          // may revisit some time.
          this.validProjection = true; 
          
          // _mapmlLayerItem is set to the root element representing this layer
          // in the layer control, iff the layer is not 'hidden' 
          this._mapmlLayerItem = {};
      },
      setZIndex: function (zIndex) {
          this.options.zIndex = zIndex;
          this._updateZIndex();

          return this;
      },
      _updateZIndex: function () {
          if (this._container && this.options.zIndex !== undefined && this.options.zIndex !== null) {
              this._container.style.zIndex = this.options.zIndex;
          }
      },
      // remove all the extents before removing the layer from the map
      _removeExtents: function(map){
        if(this._extent._mapExtents){
          for(let i = 0; i < this._extent._mapExtents.length; i++){
            if(this._extent._mapExtents[i].templatedLayer){
              map.removeLayer(this._extent._mapExtents[i].templatedLayer);
            }
          }
        }
        if (this._extent._queries) {
          delete this._extent._queries;
        }
      },
      _changeOpacity: function(e) {
        if (e && e.target && e.target.value >=0 && e.target.value <= 1.0) {
          this.changeOpacity(e.target.value);
        }
      },
      changeOpacity: function(opacity) {
          this._container.style.opacity = opacity;
          if(this.opacityEl) this.opacityEl.value = opacity;
      },
      _changeExtentOpacity: function(e){
        if(e && e.target && e.target.value >=0 && e.target.value <= 1.0){
          this.templatedLayer.changeOpacity(e.target.value);
          this._templateVars.opacity = e.target.value;
        }
      },
      _changeExtent: function(e, extentEl) {
          if(e.target.checked){
            extentEl.checked = true;
            if(this._layerEl.checked){
                extentEl.templatedLayer = M.templatedLayer(extentEl._templateVars, 
                  { pane: this._container,
                    opacity: extentEl._templateVars.opacity,
                    _leafletLayer: this,
                    crs: extentEl.crs,
                    extentZIndex: extentEl.extentZIndex
                  }).addTo(this._map);
                  extentEl.templatedLayer.setZIndex();
                  this._setLayerElExtent();  
              }       
          } else {
              L.DomEvent.stopPropagation(e);
              extentEl.checked = false;
              if(this._layerEl.checked) this._map.removeLayer(extentEl.templatedLayer);
              this._setLayerElExtent();
          }
      },

      onAdd: function (map) {
          if((this._extent || this._extent_mapExtents) && !this._validProjection(map)){
            this.validProjection = false;
            return;
          }
          this._map = map;
          if(this._content){
            if (!this._mapmlvectors) {
              this._mapmlvectors = M.mapMlFeatures(this._content, {
                // pass the vector layer a renderer of its own, otherwise leaflet
                // puts everything into the overlayPane
                renderer: M.featureRenderer(),
                // pass the vector layer the container for the parent into which
                // it will append its own container for rendering into
                pane: this._container,
                opacity: this.options.opacity,
                projection:map.options.projection,
                // each owned child layer gets a reference to the root layer
                _leafletLayer: this,
                static: true,
                onEachFeature: function(properties, geometry) {
                  // need to parse as HTML to preserve semantics and styles
                  if (properties) {
                    var c = document.createElement('div');
                    c.classList.add("mapml-popup-content");
                    c.insertAdjacentHTML('afterbegin', properties.innerHTML);
                    geometry.bindPopup(c, {autoClose: false, minWidth: 165});
                  }
                }
              });
            }
            this._setLayerElExtent();
            map.addLayer(this._mapmlvectors);
          } else {
            this.once('extentload', function() {
              if(!this._validProjection(map)){
                this.validProjection = false;
                return;
              }
              if (!this._mapmlvectors) {
                this._mapmlvectors = M.mapMlFeatures(this._content, {
                    // pass the vector layer a renderer of its own, otherwise leaflet
                    // puts everything into the overlayPane
                    renderer: M.featureRenderer(),
                    // pass the vector layer the container for the parent into which
                    // it will append its own container for rendering into
                    pane: this._container,
                    opacity: this.options.opacity,
                    projection:map.options.projection,
                    // each owned child layer gets a reference to the root layer
                    _leafletLayer: this,
                    static: true,
                    onEachFeature: function(properties, geometry) {
                      // need to parse as HTML to preserve semantics and styles
                      if (properties) {
                        var c = document.createElement('div');
                        c.classList.add("mapml-popup-content");
                        c.insertAdjacentHTML('afterbegin', properties.innerHTML);
                        geometry.bindPopup(c, {autoClose: false, minWidth: 165});
                      }
                    }
                  }).addTo(map);
              }
              this._setLayerElExtent();
            },this);
          }
          
          
          
          if (!this._imageLayer) {
              this._imageLayer = L.layerGroup();
          }
          map.addLayer(this._imageLayer);
          // the layer._imageContainer property contains an element in which
          // content will be maintained
          
          //only add the layer if there are tiles to be rendered
          if((!this._staticTileLayer || this._staticTileLayer._container === null) && 
            this._mapmlTileContainer.getElementsByTagName("map-tiles").length > 0)
          {
            this._staticTileLayer = M.mapMLStaticTileLayer({
              pane:this._container,
              _leafletLayer: this,
              className:"mapml-static-tile-layer",
              tileContainer:this._mapmlTileContainer,
              maxZoomBound:map.options.crs.options.resolutions.length - 1,
              tileSize: map.options.crs.options.crs.tile.bounds.max.x,
            });
            map.addLayer(this._staticTileLayer);
            this._setLayerElExtent();
          }
          
          const createAndAdd = createAndAddTemplatedLayers.bind(this);
          // if the extent has been initialized and received, update the map,
          if (this._extent && this._extent._mapExtents && this._extent._mapExtents[0]._templateVars) {
            createAndAdd();
          } else { // wait for extent to be loaded
            this.once('extentload', function() {
                if(!this._validProjection(map)){
                  this.validProjection = false;
                  return;
                }
                createAndAdd();
              }, this);
          }
          this.setZIndex(this.options.zIndex);
          this.getPane().appendChild(this._container);
          setTimeout(() => {
            map.fire('checkdisabled');
          }, 0);
          map.on("popupopen", this._attachSkipButtons, this);
          
          function createAndAddTemplatedLayers() {
            if(this._extent && this._extent._mapExtents){
              for(let i = 0; i < this._extent._mapExtents.length; i++){
                if (this._extent._mapExtents[i]._templateVars && this._extent._mapExtents[i].checked) {
                  if(!this._extent._mapExtents[i].extentZIndex) this._extent._mapExtents[i].extentZIndex = i;
                  this._templatedLayer = M.templatedLayer(this._extent._mapExtents[i]._templateVars, 
                    { pane: this._container,
                      opacity: this._extent._mapExtents[i]._templateVars.opacity,
                      _leafletLayer: this,
                      crs: this._extent.crs,
                      extentZIndex: this._extent._mapExtents[i].extentZIndex,
                      }).addTo(map);   
                      this._extent._mapExtents[i].templatedLayer = this._templatedLayer;
                      if(this._templatedLayer._queries){
                        if(!this._extent._queries) this._extent._queries = [];
                        this._extent._queries = this._extent._queries.concat(this._templatedLayer._queries);
                      }
                }
               }
              this._setLayerElExtent();
            }
          }
      },


      _validProjection : function(map){
        let noLayer = false;
        if(this._extent && this._extent._mapExtents){
          for(let i = 0; i < this._extent._mapExtents.length; i++){
            if(this._extent._mapExtents[i]._templateVars){
              for(let template of this._extent._mapExtents[i]._templateVars)
                if(!template.projectionMatch && template.projection !== map.options.projection) {
                  noLayer = true; // if there's a single template where projections don't match, set noLayer to true
                  break;
                } 
              }
        }
      }
        return !(noLayer || this.getProjection() !== map.options.projection.toUpperCase());
      },

      //sets the <layer-> elements .bounds property 
      _setLayerElExtent: function(){
        let bounds, zoomMax, zoomMin, maxNativeZoom, minNativeZoom,
            zoomBounds = {minZoom: 0, maxZoom: 0, maxNativeZoom: 0, minNativeZoom: 0};
        let layerTypes = ["_staticTileLayer","_imageLayer","_mapmlvectors","_templatedLayer"];
        layerTypes.forEach((type) =>{
          if(this[type]){
            if(type === "_templatedLayer"){
              for(let i = 0; i < this._extent._mapExtents.length; i++){
                for(let j = 0; j < this._extent._mapExtents[i]._templateVars.length; j++){
                  let inputData = M.extractInputBounds(this._extent._mapExtents[i]._templateVars[j]);
                  this._extent._mapExtents[i]._templateVars[j].tempExtentBounds = inputData.bounds;
                  this._extent._mapExtents[i]._templateVars[j].extentZoomBounds = inputData.zoomBounds;
                }
              }
              for(let i = 0; i < this._extent._mapExtents.length; i++){
                if(this._extent._mapExtents[i].checked){
                  for(let j = 0; j < this._extent._mapExtents[i]._templateVars.length; j++){
                    if(!bounds){
                      bounds = this._extent._mapExtents[i]._templateVars[j].tempExtentBounds;
                      zoomMax = this._extent._mapExtents[i]._templateVars[j].extentZoomBounds.maxZoom;
                      zoomMin = this._extent._mapExtents[i]._templateVars[j].extentZoomBounds.minZoom;
                      maxNativeZoom = this._extent._mapExtents[i]._templateVars[j].extentZoomBounds.maxNativeZoom;
                      minNativeZoom = this._extent._mapExtents[i]._templateVars[j].extentZoomBounds.minNativeZoom;
                    } else {
                      bounds.extend(this._extent._mapExtents[i]._templateVars[j].tempExtentBounds.min);
                      bounds.extend(this._extent._mapExtents[i]._templateVars[j].tempExtentBounds.max);
                      zoomMax = Math.max(zoomMax, this._extent._mapExtents[i]._templateVars[j].extentZoomBounds.maxZoom);
                      zoomMin = Math.min(zoomMin, this._extent._mapExtents[i]._templateVars[j].extentZoomBounds.minZoom);
                      maxNativeZoom = Math.max(maxNativeZoom, this._extent._mapExtents[i]._templateVars[j].extentZoomBounds.maxNativeZoom);
                      minNativeZoom = Math.min(minNativeZoom, this._extent._mapExtents[i]._templateVars[j].extentZoomBounds.minNativeZoom);
                    }
                  }
                }
              }
              zoomBounds.minZoom = zoomMin;
              zoomBounds.maxZoom = zoomMax;
              zoomBounds.minNativeZoom = minNativeZoom;
              zoomBounds.maxNativeZoom = maxNativeZoom;
              this._extent.zoomBounds = zoomBounds;
              this._extent.layerBounds = bounds;
              // assign each template the layer and zoom bounds
              for(let i = 0; i < this._extent._mapExtents.length; i++){
                this._extent._mapExtents[i].templatedLayer.layerBounds = bounds;
                this._extent._mapExtents[i].templatedLayer.zoomBounds = zoomBounds;
              }
            } else {
              if(this[type].layerBounds){
                if(!bounds){
                  bounds = this[type].layerBounds;
                  zoomBounds = this[type].zoomBounds;
                } else {
                  bounds.extend(this[type].layerBounds.min);
                  bounds.extend(this[type].layerBounds.max);
                }
              } 
            }
          }
        });
        if(bounds){
          //assigns the formatted extent object to .extent and spreads the zoom ranges to .extent also
          this._layerEl.extent = (Object.assign(
                                    M.convertAndFormatPCRS(bounds,this._map),
                                    {zoom:zoomBounds}));
        }
      },

      addTo: function (map) {
          map.addLayer(this);
          return this;
      },
      getEvents: function () {
          return {zoomanim: this._onZoomAnim};
      },
      redraw: function() {
        // for now, only redraw templated layers.
          if (this._extent._mapExtents) {
            for(let i = 0; i < this._extent._mapExtents.length; i++){
              if(this._extent._mapExtents[i].templatedLayer){
                this._extent._mapExtents[i].templatedLayer.redraw();
              }
            }
          }
      },
      _onZoomAnim: function(e) {
        // get the min and max zooms from all extents
        var toZoom = e.zoom,
            zoom = (this._extent && this._extent._mapExtents) ? this._extent._mapExtents[0].querySelector("map-input[type=zoom]") : null,
            min = zoom && zoom.hasAttribute("min") ? parseInt(zoom.getAttribute("min")) : this._map.getMinZoom(),
            max =  zoom && zoom.hasAttribute("max") ? parseInt(zoom.getAttribute("max")) : this._map.getMaxZoom();
        if(zoom){
          for(let i = 1; i < this._extent._mapExtents.length; i++){
            zoom = this._extent._mapExtents[i].querySelector("map-input[type=zoom]");
            if(zoom && zoom.hasAttribute("min")) { min = Math.min(parseInt(zoom.getAttribute("min")), min); }
            if(zoom && zoom.hasAttribute("max")){ max = Math.max(parseInt(zoom.getAttribute("max")), max); }
          }
        }
        var canZoom = (toZoom < min && this._extent.zoomout) || (toZoom > max && this._extent.zoomin);
        if (!(min <= toZoom && toZoom <= max)){
          if (this._extent.zoomin && toZoom > max) {
            // this._href is the 'original' url from which this layer came
            // since we are following a zoom link we will be getting a new
            // layer almost, resetting child content as appropriate
            this._href = this._extent.zoomin;
            this._layerEl.src = this._extent.zoomin;
            // this.href is the "public" property. When a dynamic layer is
            // accessed, this value changes with every new extent received
            this.href = this._extent.zoomin;
            this._layerEl.src = this._extent.zoomin;
          } else if (this._extent.zoomout && toZoom < min) {
            this._href = this._extent.zoomout;
            this.href = this._extent.zoomout;
            this._layerEl.src = this._extent.zoomout;
          }
        }
        if (this._templatedLayer && canZoom ) ;
      },
      onRemove: function (map) {
          L.DomUtil.remove(this._container);
          if(this._staticTileLayer) map.removeLayer(this._staticTileLayer);
          if(this._mapmlvectors) map.removeLayer(this._mapmlvectors);
          if(this._imageLayer) map.removeLayer(this._imageLayer);
          if (this._extent && this._extent._mapExtents) this._removeExtents(map);

          map.fire("checkdisabled");
          map.off("popupopen", this._attachSkipButtons);
      },
      getAttribution: function () {
          return this.options.attribution;
      },

      getLayerExtentHTML: function (labelName, i) {
        var extent = L.DomUtil.create('fieldset', 'mapml-layer-extent'),
          extentProperties = L.DomUtil.create('div', 'mapml-layer-item-properties', extent),
          extentSettings = L.DomUtil.create('div', 'mapml-layer-item-settings', extent),
          extentLabel = L.DomUtil.create('label', 'mapml-layer-item-toggle', extentProperties),
          input = L.DomUtil.create('input'),
          svgExtentControlIcon = L.SVG.create('svg'),
          extentControlPath1 = L.SVG.create('path'),
          extentControlPath2 = L.SVG.create('path'),
          extentNameIcon = L.DomUtil.create('span'),
          extentItemControls = L.DomUtil.create('div', 'mapml-layer-item-controls', extentProperties),
          opacityControl = L.DomUtil.create('details', 'mapml-layer-item-opacity', extentSettings),
          extentOpacitySummary = L.DomUtil.create('summary', '', opacityControl),
          mapEl = this._layerEl.parentNode,
          layerEl = this._layerEl,
          opacity = L.DomUtil.create('input', '', opacityControl);
          extentSettings.hidden = true;
          extent.setAttribute("aria-grabbed", "false");
          if(!labelName){ // if a label attribute is not present, set it to hidden in layer control
            extent.setAttribute("hidden", "");
            this._extent._mapExtents[i].hidden = true;
          }

          // append the svg paths
          svgExtentControlIcon.setAttribute('viewBox', '0 0 24 24');
          svgExtentControlIcon.setAttribute('height', '22');
          svgExtentControlIcon.setAttribute('width', '22');
          extentControlPath1.setAttribute('d', 'M0 0h24v24H0z');
          extentControlPath1.setAttribute('fill', 'none');
          extentControlPath2.setAttribute('d', 'M12 8c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm0 2c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm0 6c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z');
          svgExtentControlIcon.appendChild(extentControlPath1);
          svgExtentControlIcon.appendChild(extentControlPath2);

          let removeExtentButton = L.DomUtil.create('button', 'mapml-layer-item-remove-control', extentItemControls);
          removeExtentButton.type = 'button';
          removeExtentButton.title = 'Remove Sub Layer';
          removeExtentButton.innerHTML = "<span aria-hidden='true'>&#10005;</span>";
          removeExtentButton.classList.add('mapml-button');
          L.DomEvent.on(removeExtentButton, 'click', L.DomEvent.stop);
          L.DomEvent.on(removeExtentButton, 'click', (e)=>{
            let allRemoved = true;
            e.target.checked = false;
            this._extent._mapExtents[i].removed = true;
            this._extent._mapExtents[i].checked = false;
            if(this._layerEl.checked) this._changeExtent(e, this._extent._mapExtents[i]);
            this._extent._mapExtents[i].extentAnatomy.parentNode.removeChild(this._extent._mapExtents[i].extentAnatomy);
            for(let j = 0; j < this._extent._mapExtents.length; j++){
              if(!this._extent._mapExtents[j].removed) allRemoved = false;
            }
            if(allRemoved) this._layerItemSettingsHTML.removeChild(this._extentGroupAnatomy); 
          }, this);

          let extentsettingsButton = L.DomUtil.create('button', 'mapml-layer-item-settings-control', extentItemControls);
          extentsettingsButton.type = 'button';
          extentsettingsButton.title = 'Extent Settings';
          extentsettingsButton.setAttribute('aria-expanded', false);
          extentsettingsButton.classList.add('mapml-button');
          L.DomEvent.on(extentsettingsButton, 'click', (e)=>{
            if(extentSettings.hidden === true){
              extentsettingsButton.setAttribute('aria-expanded', true);
              extentSettings.hidden = false;
            } else {
              extentsettingsButton.setAttribute('aria-expanded', false);
              extentSettings.hidden = true;
            }
          }, this);

          extentNameIcon.setAttribute('aria-hidden', true);
          extentLabel.appendChild(input);
          extentsettingsButton.appendChild(extentNameIcon);
          extentNameIcon.appendChild(svgExtentControlIcon);
          extentOpacitySummary.innerText = 'Opacity';
          extentOpacitySummary.id = 'mapml-layer-item-opacity-' + L.stamp(extentOpacitySummary);
          opacity.setAttribute('type','range');
          opacity.setAttribute('min', '0');
          opacity.setAttribute('max','1.0');
          opacity.setAttribute('value', this._extent._mapExtents[i]._templateVars.opacity || '1.0');
          opacity.setAttribute('step','0.1');
          opacity.setAttribute('aria-labelledby', 'mapml-layer-item-opacity-' + L.stamp(extentOpacitySummary));
          this._extent._mapExtents[i]._templateVars.opacity = this._extent._mapExtents[i]._templateVars.opacity || '1.0';
          L.DomEvent.on(opacity, 'change', this._changeExtentOpacity, this._extent._mapExtents[i]);

          var extentItemNameSpan = L.DomUtil.create('span', 'mapml-layer-item-name', extentLabel);
          input.defaultChecked = this._extent._mapExtents[i] ? true: false;
          this._extent._mapExtents[i].checked = input.defaultChecked;
          input.type = 'checkbox';
          extentItemNameSpan.innerHTML = labelName;
          L.DomEvent.on(input, 'change', (e)=>{
            this._changeExtent(e, this._extent._mapExtents[i]);
          });
          extentItemNameSpan.id = 'mapml-extent-item-name-{' + L.stamp(extentItemNameSpan) + '}';
          extent.setAttribute('aria-labelledby', extentItemNameSpan.id);
          extentItemNameSpan.extent = this._extent._mapExtents[i];

          extent.onmousedown = (downEvent) => {
            if(downEvent.target.tagName.toLowerCase() === "input" || downEvent.target.tagName.toLowerCase() === "select") return;
            downEvent.preventDefault();
            downEvent.stopPropagation();

            let control = extent,
                controls = extent.parentNode,
                moving = false, yPos = downEvent.clientY;

                document.body.onmousemove = (moveEvent) => {
                  moveEvent.preventDefault();
      
                  // Fixes flickering by only moving element when there is enough space
                  let offset = moveEvent.clientY - yPos;
                  moving = Math.abs(offset) > 5 || moving;
                  if( (controls && !moving) || (controls && controls.childElementCount <= 1) || 
                      controls.getBoundingClientRect().top > control.getBoundingClientRect().bottom || 
                      controls.getBoundingClientRect().bottom < control.getBoundingClientRect().top){
                        return;
                      }
                  
                  controls.classList.add("mapml-draggable");
                  control.style.transform = "translateY("+ offset +"px)";
                  control.style.pointerEvents = "none";
      
                  let x = moveEvent.clientX, y = moveEvent.clientY,
                      root = mapEl.tagName === "MAPML-VIEWER" ? mapEl.shadowRoot : mapEl.querySelector(".mapml-web-map").shadowRoot,
                      elementAt = root.elementFromPoint(x, y),
                      swapControl = !elementAt || !elementAt.closest("fieldset") ? control : elementAt.closest("fieldset");
            
                  swapControl =  Math.abs(offset) <= swapControl.offsetHeight ? control : swapControl;
                  
                  control.setAttribute("aria-grabbed", 'true');
                  control.setAttribute("aria-dropeffect", "move");
                  if(swapControl && controls === swapControl.parentNode){
                    swapControl = swapControl !== control.nextSibling? swapControl : swapControl.nextSibling;
                    if(control !== swapControl){ 
                      yPos = moveEvent.clientY;
                      control.style.transform = null;
                    }
                    controls.insertBefore(control, swapControl);
                  }
                };

                document.body.onmouseup = () => {
                  control.setAttribute("aria-grabbed", "false");
                  control.removeAttribute("aria-dropeffect");
                  control.style.pointerEvents = null;
                  control.style.transform = null;
                  let controlsElems = controls.children,
                      zIndex = 0;
                  for(let c of controlsElems){
                    let extentEl = c.querySelector("span").extent;
                    
                    extentEl.setAttribute("data-moving","");
                    layerEl.insertAdjacentElement("beforeend", extentEl);
                    extentEl.removeAttribute("data-moving");
      
                    extentEl.extentZIndex = zIndex;
                    extentEl.templatedLayer.setZIndex(zIndex);
                    zIndex++;
                  }
                  controls.classList.remove("mapml-draggable");
                  document.body.onmousemove = document.body.onmouseup = null;
                };

                
          };

          return extent;
      },

      getLayerUserControlsHTML: function () {
        var fieldset = L.DomUtil.create('fieldset', 'mapml-layer-item'),
          input = L.DomUtil.create('input'),
          layerItemName = L.DomUtil.create('span', 'mapml-layer-item-name'),
          settingsButtonNameIcon = L.DomUtil.create('span'),
          layerItemProperty = L.DomUtil.create('div', 'mapml-layer-item-properties', fieldset),
          layerItemSettings = L.DomUtil.create('div', 'mapml-layer-item-settings', fieldset),
          itemToggleLabel = L.DomUtil.create('label', 'mapml-layer-item-toggle', layerItemProperty),
          layerItemControls = L.DomUtil.create('div', 'mapml-layer-item-controls', layerItemProperty),
          opacityControl = L.DomUtil.create('details', 'mapml-layer-item-opacity mapml-control-layers', layerItemSettings),
          opacity = L.DomUtil.create('input'),
          opacityControlSummary = L.DomUtil.create('summary'),
          svgSettingsControlIcon = L.SVG.create('svg'),
          settingsControlPath1 = L.SVG.create('path'),
          settingsControlPath2 = L.SVG.create('path'),
          extentsFieldset = L.DomUtil.create('fieldset', 'mapml-layer-grouped-extents'),
          mapEl = this._layerEl.parentNode;
          this.opacityEl = opacity;
          this._mapmlLayerItem = fieldset;

          // append the paths in svg for the remove layer and toggle icons
          svgSettingsControlIcon.setAttribute('viewBox', '0 0 24 24');
          svgSettingsControlIcon.setAttribute('height', '22');
          svgSettingsControlIcon.setAttribute('width', '22');
          settingsControlPath1.setAttribute('d', 'M0 0h24v24H0z');
          settingsControlPath1.setAttribute('fill', 'none');
          settingsControlPath2.setAttribute('d', 'M12 8c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm0 2c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm0 6c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z');
          svgSettingsControlIcon.appendChild(settingsControlPath1);
          svgSettingsControlIcon.appendChild(settingsControlPath2);
          
          layerItemSettings.hidden = true;
          settingsButtonNameIcon.setAttribute('aria-hidden', true);
          
          let removeControlButton = L.DomUtil.create('button', 'mapml-layer-item-remove-control', layerItemControls);
          removeControlButton.type = 'button';
          removeControlButton.title = 'Remove Layer';
          removeControlButton.innerHTML = "<span aria-hidden='true'>&#10005;</span>";
          removeControlButton.classList.add('mapml-button');
          //L.DomEvent.disableClickPropagation(removeControlButton);
          L.DomEvent.on(removeControlButton, 'click', L.DomEvent.stop);
          L.DomEvent.on(removeControlButton, 'click', (e)=>{
            let fieldset = 0, elem, root;
            root = mapEl.tagName === "MAPML-VIEWER" ? mapEl.shadowRoot : mapEl.querySelector(".mapml-web-map").shadowRoot;
            if(e.target.closest("fieldset").nextElementSibling && !e.target.closest("fieldset").nextElementSibling.disbaled){
              elem = e.target.closest("fieldset").previousElementSibling;
              while(elem){
                fieldset += 2; // find the next layer menu item
                elem = elem.previousElementSibling;
              }
            } else {
              // focus on the link
              elem = "link";
            }
            mapEl.removeChild(e.target.closest("fieldset").querySelector("span").layer._layerEl);
            elem = elem ? root.querySelector(".leaflet-control-attribution").firstElementChild: elem = root.querySelectorAll('input')[fieldset];
            elem.focus();
          }, this);

          let itemSettingControlButton = L.DomUtil.create('button', 'mapml-layer-item-settings-control', layerItemControls);
          itemSettingControlButton.type = 'button';
          itemSettingControlButton.title = 'Layer Settings';
          itemSettingControlButton.setAttribute('aria-expanded', false);
          itemSettingControlButton.classList.add('mapml-button');
          L.DomEvent.on(itemSettingControlButton, 'click', (e)=>{
            if(layerItemSettings.hidden === true){
              itemSettingControlButton.setAttribute('aria-expanded', true);
              layerItemSettings.hidden = false;
            } else {
              itemSettingControlButton.setAttribute('aria-expanded', false);
              layerItemSettings.hidden = true;
            }
          }, this);

          input.defaultChecked = this._map ? true: false;
          input.type = 'checkbox';
          layerItemName.layer = this;

          if (this._legendUrl) {
            var legendLink = document.createElement('a');
            legendLink.text = ' ' + this._title;
            legendLink.href = this._legendUrl;
            legendLink.target = '_blank';
            legendLink.draggable = false;
            layerItemName.appendChild(legendLink);
          } else {
            layerItemName.innerHTML = this._title;
          }
          layerItemName.id = 'mapml-layer-item-name-{' + L.stamp(layerItemName) + '}';
          opacityControlSummary.innerText = 'Opacity';
          opacityControlSummary.id = 'mapml-layer-item-opacity-' + L.stamp(opacityControlSummary);
          opacityControl.appendChild(opacityControlSummary);
          opacityControl.appendChild(opacity);
          opacity.setAttribute('type','range');
          opacity.setAttribute('min', '0');
          opacity.setAttribute('max','1.0');
          opacity.setAttribute('value', this._container.style.opacity || '1.0');
          opacity.setAttribute('step','0.1');
          opacity.setAttribute('aria-labelledby', opacityControlSummary.id);
          opacity.value = this._container.style.opacity || '1.0';

          fieldset.setAttribute("aria-grabbed", "false");
          fieldset.setAttribute('aria-labelledby', layerItemName.id);

          fieldset.onmousedown = (downEvent) => {
            if(downEvent.target.tagName.toLowerCase() === "input" || downEvent.target.tagName.toLowerCase() === "select") return;
            downEvent.preventDefault();
            let control = fieldset,
                controls = fieldset.parentNode,
                moving = false, yPos = downEvent.clientY;

            document.body.onmousemove = (moveEvent) => {
              moveEvent.preventDefault();

              // Fixes flickering by only moving element when there is enough space
              let offset = moveEvent.clientY - yPos;
              moving = Math.abs(offset) > 5 || moving;
              if( (controls && !moving) || (controls && controls.childElementCount <= 1) || 
                  controls.getBoundingClientRect().top > control.getBoundingClientRect().bottom || 
                  controls.getBoundingClientRect().bottom < control.getBoundingClientRect().top){
                    return;
                  }
              
              controls.classList.add("mapml-draggable");
              control.style.transform = "translateY("+ offset +"px)";
              control.style.pointerEvents = "none";

              let x = moveEvent.clientX, y = moveEvent.clientY,
                  root = mapEl.tagName === "MAPML-VIEWER" ? mapEl.shadowRoot : mapEl.querySelector(".mapml-web-map").shadowRoot,
                  elementAt = root.elementFromPoint(x, y),
                  swapControl = !elementAt || !elementAt.closest("fieldset") ? control : elementAt.closest("fieldset");
        
              swapControl =  Math.abs(offset) <= swapControl.offsetHeight ? control : swapControl;
              
              control.setAttribute("aria-grabbed", 'true');
              control.setAttribute("aria-dropeffect", "move");
              if(swapControl && controls === swapControl.parentNode){
                swapControl = swapControl !== control.nextSibling? swapControl : swapControl.nextSibling;
                if(control !== swapControl){ 
                  yPos = moveEvent.clientY;
                  control.style.transform = null;
                }
                controls.insertBefore(control, swapControl);
              }
            };

            document.body.onmouseup = () => {
              control.setAttribute("aria-grabbed", "false");
              control.removeAttribute("aria-dropeffect");
              control.style.pointerEvents = null;
              control.style.transform = null;
              let controlsElems = controls.children,
                  zIndex = 1;
              for(let c of controlsElems){
                let layerEl = c.querySelector("span").layer._layerEl;
                
                layerEl.setAttribute("data-moving","");
                mapEl.insertAdjacentElement("beforeend", layerEl);
                layerEl.removeAttribute("data-moving");

                
                layerEl._layer.setZIndex(zIndex);
                zIndex++;
              }
              controls.classList.remove("mapml-draggable");
              document.body.onmousemove = document.body.onmouseup = null;
            };
          };

          L.DomEvent.on(opacity,'change', this._changeOpacity, this);

          itemToggleLabel.appendChild(input);
          itemToggleLabel.appendChild(layerItemName);
          itemSettingControlButton.appendChild(settingsButtonNameIcon);
          settingsButtonNameIcon.appendChild(svgSettingsControlIcon);

          if (this._styles) {
            layerItemSettings.appendChild(this._styles);
          }

          if (this._userInputs) {
            var frag = document.createDocumentFragment();
            var templates = this._extent._templateVars;
            if (templates) {
              for (var i=0;i<templates.length;i++) {
                var template = templates[i];
                for (var j=0;j<template.values.length;j++) {
                  var mapmlInput = template.values[j],
                      id = '#'+mapmlInput.getAttribute('id');
                  // don't add it again if it is referenced > once
                  if (mapmlInput.tagName.toLowerCase() === 'map-select' && !frag.querySelector(id)) {
                    // generate a <details><summary></summary><select...></details>
                    var selectdetails = L.DomUtil.create('details', 'mapml-layer-item-time mapml-control-layers', frag),
                        selectsummary = L.DomUtil.create('summary'),
                        selectSummaryLabel = L.DomUtil.create('label');
                        selectSummaryLabel.innerText = mapmlInput.getAttribute('name');
                        selectSummaryLabel.setAttribute('for', mapmlInput.getAttribute('id'));
                        selectsummary.appendChild(selectSummaryLabel);
                        selectdetails.appendChild(selectsummary);
                        selectdetails.appendChild(mapmlInput.htmlselect);
                  }
                }
              }
            }
            layerItemSettings.appendChild(frag);
          }

          // if there are extents, add them to the layer control
          if(this._extent && this._extent._mapExtents) {
            var allHidden = true;
            this._layerItemSettingsHTML = layerItemSettings;
            this._extentGroupAnatomy = extentsFieldset;
            extentsFieldset.setAttribute('aria-label', 'Sublayers');
            for(let j=0; j < this._extent._mapExtents.length; j++) {
              extentsFieldset.appendChild(this._extent._mapExtents[j].extentAnatomy);
              if(!this._extent._mapExtents[j].hidden) allHidden = false;
            }
            if(!allHidden) layerItemSettings.appendChild(extentsFieldset);
          }

          return this._mapmlLayerItem;
      },
      _initialize: function(content) {
          if (!this._href && !content) {return;}
          var layer = this;
          // the this._href (comes from layer@src) should take precedence over 
          // content of the <layer> element, but if no this._href / src is provided
          // but there *is* child content of the <layer> element (which is copied/
          // referred to by this._content), we should use that content.
          if (this._href) {
              var xhr = new XMLHttpRequest();
  //            xhr.withCredentials = true;
              _get(this._href, _processInitialExtent);
          } else if (content) {
              // may not set this._extent if it can't be done from the content
              // (eg a single point) and there's no map to provide a default yet
              _processInitialExtent.call(this, content);
          }
          function _get(url, fCallback  ) {
              xhr.onreadystatechange = function () { 
                if(this.readyState === this.DONE) {
                  if (this.status === 400 || 
                      this.status === 404 || 
                      this.status === 500 || 
                      this.status === 406) {
                      layer.error = true;
                      layer.fire('extentload', layer, true);
                      xhr.abort();
                  }
                }};
              xhr.onload = fCallback;
              xhr.onerror = function () { 
                layer.error = true;
                layer.fire('extentload', layer, true);
              };
              xhr.open("GET", url);
              xhr.setRequestHeader("Accept",M.mime);
              xhr.overrideMimeType("text/xml");
              xhr.send();
          }
          function transcribe(element) {
              var select = document.createElement("select");
              var elementAttrNames = element.getAttributeNames();

              for(let i = 0; i < elementAttrNames.length; i++){
                  select.setAttribute(elementAttrNames[i], element.getAttribute(elementAttrNames[i]));
              }

              var options = element.children;

              for(let i = 0; i < options.length; i++){
                  var option = document.createElement("option");
                  var optionAttrNames = options[i].getAttributeNames();

                  for (let j = 0; j < optionAttrNames.length; j++){
                      option.setAttribute(optionAttrNames[j], options[i].getAttribute(optionAttrNames[j]));
                  }

                  option.innerHTML = options[i].innerHTML;
                  select.appendChild(option);
              }
              return select;
          }

          function _initTemplateVars(serverExtent, metaExtent, projection, mapml, base, projectionMatch){
            var templateVars = [];
            // set up the URL template and associated inputs (which yield variable values when processed)
            var tlist = serverExtent.querySelectorAll('map-link[rel=tile],map-link[rel=image],map-link[rel=features],map-link[rel=query]'),
                varNamesRe = (new RegExp('(?:\{)(.*?)(?:\})','g')),
                zoomInput = serverExtent.querySelector('map-input[type="zoom" i]'),
                includesZoom = false, extentFallback = {};

            extentFallback.zoom = 0;
            if (metaExtent){
              let content = M.metaContentToObject(metaExtent.getAttribute("content")), cs;
              
              extentFallback.zoom = content.zoom || extentFallback.zoom;

              let metaKeys = Object.keys(content);
              for(let i =0;i<metaKeys.length;i++){
                if(!metaKeys[i].includes("zoom")){
                  cs = M.axisToCS(metaKeys[i].split("-")[2]);
                  break;
                }
              }
              let axes = M.csToAxes(cs);
              extentFallback.bounds = M.boundsToPCRSBounds(
                L.bounds(L.point(+content[`top-left-${axes[0]}`],+content[`top-left-${axes[1]}`]),
                L.point(+content[`bottom-right-${axes[0]}`],+content[`bottom-right-${axes[1]}`])),
                extentFallback.zoom, projection, cs);
              
            } else {
              extentFallback.bounds = M[projection].options.crs.pcrs.bounds;
            }
              
            for (var i=0;i< tlist.length;i++) {
              var t = tlist[i], template = t.getAttribute('tref'); 
              if(!template){
                template = BLANK_TT_TREF;
                let blankInputs = mapml.querySelectorAll('map-input');
                for (let i of blankInputs){
                  template += `{${i.getAttribute("name")}}`;
                }
              }
              
              var v,
                  title = t.hasAttribute('title') ? t.getAttribute('title') : 'Query this layer',
                  vcount=template.match(varNamesRe),
                  trel = (!t.hasAttribute('rel') || t.getAttribute('rel').toLowerCase() === 'tile') ? 'tile' : t.getAttribute('rel').toLowerCase(),
                  ttype = (!t.hasAttribute('type')? 'image/*':t.getAttribute('type').toLowerCase()),
                  inputs = [],
                  tms = t && t.hasAttribute("tms");
                  var zoomBounds = mapml.querySelector('map-meta[name=zoom]')?
                                    M.metaContentToObject(mapml.querySelector('map-meta[name=zoom]').getAttribute('content')):
                                    undefined;
              while ((v = varNamesRe.exec(template)) !== null) {
                var varName = v[1],
                    inp = serverExtent.querySelector('map-input[name='+varName+'],map-select[name='+varName+']');
                if (inp) {

                  if ((inp.hasAttribute("type") && inp.getAttribute("type")==="location") && 
                      (!inp.hasAttribute("min" )) && 
                      (inp.hasAttribute("axis") && !["i","j"].includes(inp.getAttribute("axis").toLowerCase()))){
                    zoomInput.setAttribute("value", extentFallback.zoom);
                    
                    let axis = inp.getAttribute("axis"), 
                        axisBounds = M.convertPCRSBounds(extentFallback.bounds, extentFallback.zoom, projection, M.axisToCS(axis));
                    inp.setAttribute("min", axisBounds.min[M.axisToXY(axis)]);
                    inp.setAttribute("max", axisBounds.max[M.axisToXY(axis)]);
                  }

                  inputs.push(inp);
                  includesZoom = includesZoom || inp.hasAttribute("type") && inp.getAttribute("type").toLowerCase() === "zoom";
                  if (inp.hasAttribute('shard')) {
                    var id = inp.getAttribute('list');
                    inp.servers = [];
                    var servers = serverExtent.querySelectorAll('map-datalist#'+id + ' > map-option');
                    if (servers.length === 0 && inp.hasAttribute('value')) {
                      servers = inp.getAttribute('value').split('');
                    }
                    for (var s=0;s < servers.length;s++) {
                      if (servers[s].getAttribute) {
                        inp.servers.push(servers[s].getAttribute('value'));
                      } else {
                        inp.servers.push(servers[s]);
                      }
                    }
                  } else if (inp.tagName.toLowerCase() === 'map-select') {
                    // use a throwaway div to parse the input from MapML into HTML
                    var div =document.createElement("div");
                    div.insertAdjacentHTML("afterbegin",inp.outerHTML);
                    // parse
                    inp.htmlselect = div.querySelector("map-select");
                    inp.htmlselect = transcribe(inp.htmlselect);

                    // this goes into the layer control, so add a listener
                    L.DomEvent.on(inp.htmlselect, 'change', layer.redraw, layer);
                    if (!layer._userInputs) {
                      layer._userInputs = [];
                    }
                    layer._userInputs.push(inp.htmlselect);
                  }
                  // TODO: if this is an input@type=location 
                  // get the TCRS min,max attribute values at the identified zoom level 
                  // save this information as properties of the serverExtent,
                  // perhaps as a bounds object so that it can be easily used
                  // later by the layer control to determine when to enable
                  // disable the layer for drawing.
                } else {
                  console.log('input with name='+varName+' not found for template variable of same name');
                  // no match found, template won't be used
                  break;
                }
              }
              if (template && vcount.length === inputs.length || template === BLANK_TT_TREF) {
                if (trel === 'query') {
                  layer.queryable = true;
                }
                if(!includesZoom && zoomInput) {
                  inputs.push(zoomInput);
                }
                // template has a matching input for every variable reference {varref}
                templateVars.push({
                  template:decodeURI(new URL(template, base)), 
                  linkEl: t,
                  title:title, 
                  rel: trel, 
                  type: ttype, 
                  values: inputs, 
                  zoomBounds:zoomBounds,
                  extentPCRSFallback: {bounds: extentFallback.bounds}, 
                  projectionMatch: projectionMatch,
                  projection:serverExtent.getAttribute("units") || FALLBACK_PROJECTION,
                  tms:tms,
                });
              }
            }
            return templateVars;
          }

          function _processInitialExtent(content) {
            //TODO: include inline extents
              var mapml = this.responseXML || content;
              if(mapml.querySelector && mapml.querySelector('map-feature'))layer._content = mapml;
              if(!this.responseXML && this.responseText) mapml = new DOMParser().parseFromString(this.responseText,'text/xml');
              if (this.readyState === this.DONE && mapml.querySelector && !mapml.querySelector("parsererror")) {
                  var serverExtent = mapml.querySelectorAll('map-extent'), projection, projectionMatch, serverMeta;
                      
                  
                  if(!serverExtent.length){
                    serverMeta = mapml.querySelector('map-meta[name=projection]');
                  }

                  // check whether all map-extent elements have the same units
                  if(serverExtent.length >= 1){
                    for(let i = 0; i < serverExtent.length; i++){
                      if (serverExtent[i].tagName.toLowerCase() === "map-extent" && serverExtent[i].hasAttribute('units')){
                        projection = serverExtent[i].getAttribute("units");
                      }
                      projectionMatch = projection && projection === layer.options.mapprojection;
                      if(!projectionMatch){
                        break;
                      }
                    }
                  } else if(serverMeta){
                    if (serverMeta.tagName.toLowerCase() === "map-meta" && serverMeta.hasAttribute('content')) {
                      projection = M.metaContentToObject(serverMeta.getAttribute('content')).content;
                      projectionMatch = projection && projection === layer.options.mapprojection;
                    }
                  }  
                  
                  var metaExtent = mapml.querySelector('map-meta[name=extent]'),
                      selectedAlternate = !projectionMatch && mapml.querySelector('map-head map-link[rel=alternate][projection='+layer.options.mapprojection+']'),
                      
                      base = 
        (new URL(mapml.querySelector('map-base') ? mapml.querySelector('map-base').getAttribute('href') : mapml.baseURI || this.responseURL, this.responseURL)).href;
                  
                  if (!projectionMatch && selectedAlternate && selectedAlternate.hasAttribute('href')) {
                       
                      layer.fire('changeprojection', {href:  (new URL(selectedAlternate.getAttribute('href'), base)).href}, false);
                      return;
                  } else if (!projectionMatch && layer._map && layer._map.options.mapEl.querySelectorAll("layer-").length === 1){
                    layer._map.options.mapEl.projection = projection;
                    return;
                  } else if (!serverMeta){
                    layer._extent = {};
                    if(projectionMatch){
                      layer._extent.crs = M[projection];
                    }
                    layer._extent._mapExtents = []; // stores all the map-extent elements in the layer
                    layer._extent._templateVars = []; // stores all template variables coming from all extents
                    for(let j = 0; j < serverExtent.length; j++){
                      if (serverExtent[j].querySelector('map-link[rel=tile],map-link[rel=image],map-link[rel=features],map-link[rel=query]') &&
                          serverExtent[j].hasAttribute("units")) {
                            layer._extent._mapExtents.push(serverExtent[j]);
                            projectionMatch = projectionMatch || selectedAlternate;
                            let templateVars = _initTemplateVars.call(layer, serverExtent[j], metaExtent, projection, mapml, base, projectionMatch);
                            layer._extent._mapExtents[j]._templateVars = templateVars;
                            layer._extent._templateVars = layer._extent._templateVars.concat(templateVars);
                          } 
                        }     
                  } else {
                    layer._extent = serverMeta;
                  }  
                  layer._parseLicenseAndLegend(mapml, layer, projection);

                  var zoomin = mapml.querySelector('map-link[rel=zoomin]'),
                      zoomout = mapml.querySelector('map-link[rel=zoomout]');
                  delete layer._extent.zoomin;
                  delete layer._extent.zoomout;
                  if (zoomin) {
                    layer._extent.zoomin = (new URL(zoomin.getAttribute('href'), base)).href;
                  }
                  if (zoomout) {
                    layer._extent.zoomout = (new URL(zoomout.getAttribute('href'), base)).href;
                  }
                  if (layer._extent._mapExtents) {
                    for(let i = 0; i < layer._extent._mapExtents.length; i++){
                      if(layer._extent._mapExtents[i].templatedLayer){
                        layer._extent._mapExtents[i].templatedLayer.reset(layer._extent._mapExtents[i]._templateVars, layer._extent._mapExtents[i].extentZIndex);
                      }
                    }
                    
                  }
                  if (mapml.querySelector('map-tile')) {
                    var tiles = document.createElement("map-tiles"),
                      zoom = mapml.querySelector('map-meta[name=zoom][content]') || mapml.querySelector('map-input[type=zoom][value]');
                    tiles.setAttribute("zoom", zoom && zoom.getAttribute('content') || zoom && zoom.getAttribute('value') || "0");
                    var newTiles = mapml.getElementsByTagName('map-tile');
                    for (var nt=0;nt<newTiles.length;nt++) {
                        tiles.appendChild(document.importNode(newTiles[nt], true));
                    }
                    layer._mapmlTileContainer.appendChild(tiles);
                  }
                  M.parseStylesheetAsHTML(mapml, base, layer._container);

                  // add multiple extents
                  if(layer._extent._mapExtents){
                    for(let j = 0; j < layer._extent._mapExtents.length; j++){
                      var labelName = layer._extent._mapExtents[j].getAttribute('label');
                      var extentElement = layer.getLayerExtentHTML(labelName, j);
                      layer._extent._mapExtents[j].extentAnatomy = extentElement;
                    }
                  }

                  var styleLinks = mapml.querySelectorAll('map-link[rel=style],map-link[rel="self style"],map-link[rel="style self"]');
                  if (styleLinks.length > 1) {
                    var stylesControl = document.createElement('details'),
                    stylesControlSummary = document.createElement('summary');
                    stylesControlSummary.innerText = 'Style';
                    stylesControl.appendChild(stylesControlSummary);
                    var changeStyle = function (e) {
                        layer.fire('changestyle', {src: e.target.getAttribute("data-href")}, false);
                    };

                    for (var j=0;j<styleLinks.length;j++) {
                      var styleOption = document.createElement('div'),
                      styleOptionInput = styleOption.appendChild(document.createElement('input'));
                      styleOptionInput.setAttribute("type", "radio");
                      styleOptionInput.setAttribute("id", "rad-"+L.stamp(styleOptionInput));
                      styleOptionInput.setAttribute("name", "styles-"+this._title);
                      styleOptionInput.setAttribute("value", styleLinks[j].getAttribute('title'));
                      styleOptionInput.setAttribute("data-href", new URL(styleLinks[j].getAttribute('href'),base).href);
                      var styleOptionLabel = styleOption.appendChild(document.createElement('label'));
                      styleOptionLabel.setAttribute("for", "rad-"+L.stamp(styleOptionInput));
                      styleOptionLabel.innerText = styleLinks[j].getAttribute('title');
                      if (styleLinks[j].getAttribute("rel") === "style self" || styleLinks[j].getAttribute("rel") === "self style") {
                        styleOptionInput.checked = true;
                      }
                      stylesControl.appendChild(styleOption);
                      L.DomUtil.addClass(stylesControl,'mapml-layer-item-style mapml-control-layers');
                      L.DomEvent.on(styleOptionInput,'click', changeStyle, layer);
                    }
                    layer._styles = stylesControl;
                  }
                  
                  if (mapml.querySelector('map-title')) {
                    layer._title = mapml.querySelector('map-title').textContent.trim();
                  } else if (mapml instanceof Element && mapml.hasAttribute('label')) {
                    layer._title = mapml.getAttribute('label').trim();
                  }
                  if (layer._map) {
                      layer._validateExtent();
                      // if the layer is checked in the layer control, force the addition
                      // of the attribution just received
                      if (layer._map.hasLayer(layer)) {
                          layer._map.attributionControl.addAttribution(layer.getAttribution());
                      }
                      //layer._map.fire('moveend', layer);
                  }
              } else {
                  layer.error = true;
              }
              layer.fire('extentload', layer, false);
              layer._layerEl.dispatchEvent(new CustomEvent('extentload', {detail: layer,}));
          }
      },
      _validateExtent: function () {
        // TODO: change so that the _extent bounds are set based on inputs
        if(!this._extent || !this._map){
          return;
        }
        var serverExtent = this._extent._mapExtents ? this._extent._mapExtents : [this._extent], lp;
          
          // loop through the map-extent elements and assign each one its crs
          for(let i = 0; i < serverExtent.length; i++){
            if (!serverExtent[i].querySelector) {
              return;
            }
          if (serverExtent[i].querySelector('[type=zoom][min=""], [type=zoom][max=""]')) {
              var zoom = serverExtent[i].querySelector('[type=zoom]');
              zoom.setAttribute('min',this._map.getMinZoom());
              zoom.setAttribute('max',this._map.getMaxZoom());
          }
            lp = serverExtent[i].hasAttribute("units") ? serverExtent[i].getAttribute("units") : null;
            if (lp && M[lp]) {
              if(this._extent._mapExtents) this._extent._mapExtents[i].crs = M[lp];
              else this._extent.crs = M[lp];
            } else {
              if(this._extent._mapExtents) this._extent._mapExtents[i].crs = M.OSMTILE;
              else this._extent.crs = M.OSMTILE;
            }
          }
      },
      // a layer must share a projection with the map so that all the layers can
      // be overlayed in one coordinate space.  WGS84 is a 'wildcard', sort of.
      getProjection: function () {
        if(!this._extent) { return; }
        let extent = this._extent._mapExtents ? this._extent._mapExtents[0] : this._extent; // the projections for each extent eould be the same (as) validated in _validProjection, so can use mapExtents[0]
        if(!extent) return FALLBACK_PROJECTION;
        switch (extent.tagName.toUpperCase()) {
          case "MAP-EXTENT":
            if(extent.hasAttribute('units'))
              return extent.getAttribute('units').toUpperCase();
            break;
          case "MAP-INPUT":
            if(extent.hasAttribute('value'))
              return extent.getAttribute('value').toUpperCase();
            break;
          case "MAP-META":
            if(extent.hasAttribute('content'))
              return M.metaContentToObject(extent.getAttribute('content')).content.toUpperCase(); 
            break;
          default:
            return FALLBACK_PROJECTION; 
        }
        return FALLBACK_PROJECTION;
      },
      _parseLicenseAndLegend: function (xml, layer) {
          var licenseLink =  xml.querySelector('map-link[rel=license]'), licenseTitle, licenseUrl, attText;
          if (licenseLink) {
              licenseTitle = licenseLink.getAttribute('title');
              licenseUrl = licenseLink.getAttribute('href');
              attText = '<a href="' + licenseUrl + '" title="'+licenseTitle+'">'+licenseTitle+'</a>';
          }
          L.setOptions(layer,{attribution:attText});
          var legendLink = xml.querySelector('map-link[rel=legend]');
          if (legendLink) {
            layer._legendUrl = legendLink.getAttribute('href');
          }
      },
      getQueryTemplates: function(pcrsClick) {
          if (this._extent && this._extent._queries) {
            var templates = [];
            // only return queries that are in bounds
            if (this._layerEl.checked && !this._layerEl.hidden && this._mapmlLayerItem) {
              let layerAndExtents = this._mapmlLayerItem.querySelectorAll(".mapml-layer-item-name");
              for(let i = 0; i < layerAndExtents.length; i++){
                if (layerAndExtents[i].extent || this._extent._mapExtents.length === 1) { // the layer won't have an .extent property, this is kind of a hack
                  let extent = layerAndExtents[i].extent || this._extent._mapExtents[0];
                  for (let j = 0; j < extent._templateVars.length; j++) {
                    if (extent.checked) {
                      let template = extent._templateVars[j];
                      // for each template in the extent, see if it corresponds to one in the this._extent._queries array
                      for (let k = 0; k < this._extent._queries.length; k++) {
                        let queryTemplate = this._extent._queries[k];
                        if (template === queryTemplate && queryTemplate.extentBounds.contains(pcrsClick)) {
                          templates.push(queryTemplate);
                        }
                      }
                    }
                  }
                }
              }
              return templates;
            }
          }
      },
      _attachSkipButtons: function(e){
        let popup = e.popup, map = e.target, layer, group,
            content = popup._container.getElementsByClassName("mapml-popup-content")[0];

        popup._container.setAttribute("role", "dialog");
        content.setAttribute("tabindex", "-1");
        // https://github.com/Maps4HTML/Web-Map-Custom-Element/pull/467#issuecomment-844307818
        content.setAttribute("role", "document");
        popup._count = 0; // used for feature pagination

        if(popup._source._eventParents){ // check if the popup is for a feature or query
          layer = popup._source._eventParents[Object.keys(popup._source._eventParents)[0]]; // get first parent of feature, there should only be one
          group = popup._source.group;
        } else {
          layer = popup._source._templatedLayer;
        }

        if(popup._container.querySelector('nav[class="mapml-focus-buttons"]')){
          L.DomUtil.remove(popup._container.querySelector('nav[class="mapml-focus-buttons"]'));
          L.DomUtil.remove(popup._container.querySelector('hr'));
        }
        //add when popopen event happens instead
        let div = L.DomUtil.create("nav", "mapml-focus-buttons");

        // creates |< button, focuses map
        let mapFocusButton = L.DomUtil.create("button", "mapml-popup-button", div);
        mapFocusButton.type = "button";
        mapFocusButton.title = "Focus Map";
        mapFocusButton.innerHTML = "<span aria-hidden='true'>|&#10094;</span>";
        L.DomEvent.on(mapFocusButton, 'click', (e)=>{
          L.DomEvent.stop(e);
          map.featureIndex._sortIndex();
          map.closePopup();
          map._container.focus();
        }, popup);

        // creates < button, focuses previous feature, if none exists focuses the current feature
        let previousButton = L.DomUtil.create("button", "mapml-popup-button", div);
        previousButton.type = "button";
        previousButton.title = "Previous Feature";
        previousButton.innerHTML = "<span aria-hidden='true'>&#10094;</span>";
        L.DomEvent.on(previousButton, 'click', layer._previousFeature, popup);

        // static feature counter that 1/1
        let featureCount = L.DomUtil.create("p", "mapml-feature-count", div),
            totalFeatures = this._totalFeatureCount ? this._totalFeatureCount : 1;
        featureCount.innerText = (popup._count + 1)+"/"+totalFeatures;

        // creates > button, focuses next feature, if none exists focuses the current feature
        let nextButton = L.DomUtil.create("button", "mapml-popup-button", div);
        nextButton.type = "button";
        nextButton.title = "Next Feature";
        nextButton.innerHTML = "<span aria-hidden='true'>&#10095;</span>";
        L.DomEvent.on(nextButton, 'click', layer._nextFeature, popup);
        
        // creates >| button, focuses map controls
        let controlFocusButton = L.DomUtil.create("button", "mapml-popup-button", div);
        controlFocusButton.type = "button";
        controlFocusButton.title = "Focus Controls";
        controlFocusButton.innerHTML = "<span aria-hidden='true'>&#10095;|</span>";
        L.DomEvent.on(controlFocusButton, 'click', (e) => {
          map.featureIndex._sortIndex();
          map.featureIndex.currentIndex = map.featureIndex.inBoundFeatures.length - 1;
          map.featureIndex.inBoundFeatures[0].path.setAttribute("tabindex", -1);
          map.featureIndex.inBoundFeatures[map.featureIndex.currentIndex].path.setAttribute("tabindex", 0);
          L.DomEvent.stop(e);
          map.closePopup();
          map._controlContainer.querySelector("A").focus();
        }, popup);
    
        let divider = L.DomUtil.create("hr");

        popup._navigationBar = div;
        popup._content.appendChild(divider);
        popup._content.appendChild(div);
        
        content.focus();

        if(group) {
          // e.target = this._map
          // Looks for keydown, more specifically tab and shift tab
          group.setAttribute("aria-expanded", "true");
          map.on("keydown", focusFeature);
        } else {
          map.on("keydown", focusMap);
        }
        // When popup is open, what gets focused with tab needs to be done using JS as the DOM order is not in an accessibility friendly manner
        function focusFeature(focusEvent){
          let path = focusEvent.originalEvent.path || focusEvent.originalEvent.composedPath();
          let isTab = focusEvent.originalEvent.keyCode === 9,
              shiftPressed = focusEvent.originalEvent.shiftKey;
          if((path[0].classList.contains("leaflet-popup-close-button") && isTab && !shiftPressed) || focusEvent.originalEvent.keyCode === 27){
            setTimeout(() => {
              L.DomEvent.stop(focusEvent);
              map.closePopup(popup);
              group.focus();
            }, 0);
          } else if ((path[0].title==="Focus Map" || path[0].classList.contains("mapml-popup-content")) && isTab && shiftPressed){
            setTimeout(() => { //timeout needed so focus of the feature is done even after the keypressup event occurs
              L.DomEvent.stop(focusEvent);
              map.closePopup(popup);
              group.focus();
            }, 0);
          }
        }

        function focusMap(focusEvent){
          let path = focusEvent.originalEvent.path || focusEvent.originalEvent.composedPath();
          let isTab = focusEvent.originalEvent.keyCode === 9,
          shiftPressed = focusEvent.originalEvent.shiftKey;

          if((focusEvent.originalEvent.keyCode === 13 && path[0].classList.contains("leaflet-popup-close-button")) || focusEvent.originalEvent.keyCode === 27 ){
            L.DomEvent.stopPropagation(focusEvent);
            map._container.focus();
            map.closePopup(popup);
            if(focusEvent.originalEvent.keyCode !== 27)map._popupClosed = true;
          } else if (isTab && path[0].classList.contains("leaflet-popup-close-button")){
            map.closePopup(popup);
          } else if ((path[0].title==="Focus Map" || path[0].classList.contains("mapml-popup-content")) && isTab && shiftPressed){
            setTimeout(() => { //timeout needed so focus of the feature is done even after the keypressup event occurs
              L.DomEvent.stop(focusEvent);
              map.closePopup(popup);
              map._container.focus();
            }, 0);
          }
        }

        // if popup closes then the focusFeature handler can be removed
        map.on("popupclose", removeHandlers);
        function removeHandlers(removeEvent){
          if (removeEvent.popup === popup){
            map.off("keydown", focusFeature);
            map.off("keydown", focusMap);
            map.off('popupclose', removeHandlers);
            if(group) group.setAttribute("aria-expanded", "false");
          }
        }
      },
  });
  var mapMLLayer = function (url, node, options) {
    if (!url && !node) return null;
  	return new MapMLLayer(url, node, options);
  };

  var DebugOverlay = L.Layer.extend({

    onAdd: function (map) {

      let mapSize = map.getSize();

      //conditionally show container for debug panel/banner only when the map has enough space for it
      if (mapSize.x > 400 || mapSize.y > 300) {
        this._container = L.DomUtil.create("table", "mapml-debug", map._container);

        this._panel = debugPanel({
          className: "mapml-debug-panel",
          pane: this._container,
        });
        map.addLayer(this._panel);

      }

      this._grid = debugGrid({
        className: "mapml-debug-grid",
        pane: map._panes.mapPane,
        zIndex: 400,
        tileSize: map.options.crs.options.crs.tile.bounds.max.x,
      });
      map.addLayer(this._grid);

      this._vectors = debugVectors({
        className: "mapml-debug-vectors",
        pane: map._panes.mapPane,
        toolPane: this._container,
      });
      map.addLayer(this._vectors);
    },

    onRemove: function (map) {
      map.removeLayer(this._grid);
      map.removeLayer(this._vectors);
      if (this._panel) {  //conditionally remove the panel, as it's not always added
        map.removeLayer(this._panel);
        L.DomUtil.remove(this._container);
      }
    },

  });

  var debugOverlay = function () {
    return new DebugOverlay();
  };

  var DebugPanel = L.Layer.extend({

    initialize: function (options) {
      L.setOptions(this, options);
    },

    onAdd: function (map) {

      this._title = L.DomUtil.create("caption", "mapml-debug-banner", this.options.pane);
      this._title.innerHTML = "Debug mode";

      map.debug = {};
      map.debug._infoContainer = this._debugContainer = L.DomUtil.create("tbody", "mapml-debug-panel", this.options.pane);

      let infoContainer = map.debug._infoContainer;

      map.debug._tileCoord = L.DomUtil.create("tr", "mapml-debug-coordinates", infoContainer);
      map.debug._tileMatrixCoord = L.DomUtil.create("tr", "mapml-debug-coordinates", infoContainer);
      map.debug._mapCoord = L.DomUtil.create("tr", "mapml-debug-coordinates", infoContainer);
      map.debug._tcrsCoord = L.DomUtil.create("tr", "mapml-debug-coordinates", infoContainer);
      map.debug._pcrsCoord = L.DomUtil.create("tr", "mapml-debug-coordinates", infoContainer);
      map.debug._gcrsCoord = L.DomUtil.create("tr", "mapml-debug-coordinates", infoContainer);

      this._map.on("mousemove", this._updateCoords);

    },
    onRemove: function () {
      L.DomUtil.remove(this._title);
      if (this._debugContainer) {
        L.DomUtil.remove(this._debugContainer);
        this._map.off("mousemove", this._updateCoords);
      }
    },
    _updateCoords: function (e) {
      if (this.contextMenu._visible) return;
      let mapEl = this.options.mapEl,
        point = mapEl._map.project(e.latlng),
        scale = mapEl._map.options.crs.scale(+mapEl.zoom),
        pcrs = mapEl._map.options.crs.transformation.untransform(point, scale),
        tileSize = mapEl._map.options.crs.options.crs.tile.bounds.max.x,
        pointI = point.x % tileSize, pointJ = point.y % tileSize;

      if (pointI < 0) pointI += tileSize;
      if (pointJ < 0) pointJ += tileSize;

      this.debug._tileCoord.innerHTML = `
      <th scope="row">tile: </th>
      <td>i: ${Math.trunc(pointI)}, </td>
      <td>j: ${Math.trunc(pointJ)}</td>
      `;
      this.debug._mapCoord.innerHTML = `
      <th scope="row">map: </th>
      <td>i: ${Math.trunc(e.containerPoint.x)}, </td>
      <td>j: ${Math.trunc(e.containerPoint.y)}</td>
      `;
      this.debug._gcrsCoord.innerHTML = `
      <th scope="row">gcrs: </th>
      <td>lon: ${e.latlng.lng.toFixed(6)}, </td>
      <td>lat: ${e.latlng.lat.toFixed(6)}</td>
      `;
      this.debug._tcrsCoord.innerHTML = `
      <th scope="row">tcrs: </th>
      <td>x: ${Math.trunc(point.x)}, </td>
      <td>y: ${Math.trunc(point.y)}</td>
      `;
      this.debug._tileMatrixCoord.innerHTML = `
      <th scope="row">tilematrix: </th>
      <td>column: ${Math.trunc(point.x / tileSize)}, </td>
      <td>row: ${Math.trunc(point.y / tileSize)}</td>
      `;
      this.debug._pcrsCoord.innerHTML = `
      <th scope="row">pcrs: </th>
      <td>easting: ${pcrs.x.toFixed(2)}, </td>
      <td>northing: ${pcrs.y.toFixed(2)}</td>
      `;
    },

  });

  var debugPanel = function (options) {
    return new DebugPanel(options);
  };

  var DebugGrid = L.GridLayer.extend({

    initialize: function (options) {
      L.setOptions(this, options);
      L.GridLayer.prototype.initialize.call(this, this._map);
    },

    createTile: function (coords) {
      let tile = L.DomUtil.create("div", "mapml-debug-tile");
      tile.setAttribute("col", coords.x);
      tile.setAttribute("row", coords.y);
      tile.setAttribute("zoom", coords.z);
      tile.innerHTML = [`col: ${coords.x}`, `row: ${coords.y}`, `zoom: ${coords.z}`].join(', ');

      tile.style.outline = '1px dashed red';
      return tile;
    },
  });

  var debugGrid = function (options) {
    return new DebugGrid(options);
  };

  var DebugVectors = L.LayerGroup.extend({
    initialize: function (options) {
      L.setOptions(this, options);
      L.LayerGroup.prototype.initialize.call(this, this._map, options);
    },
    onAdd: function (map) {
      map.on('overlayremove', this._mapLayerUpdate, this);
      map.on('overlayadd', this._mapLayerUpdate, this);
      let center = map.options.crs.transformation.transform(L.point(0, 0), map.options.crs.scale(0));
      this._centerVector = L.circle(map.options.crs.pointToLatLng(center, 0), { radius: 250 });
      this._centerVector.bindTooltip("Projection Center");

      this._addBounds(map);
    },
    onRemove: function (map) {
      this.clearLayers();
    },

    _addBounds: function (map) {
      let id = Object.keys(map._layers),
        layers = map._layers,
        colors = ["#FF5733", "#8DFF33", "#3397FF", "#E433FF", "#F3FF33"],
        j = 0;

      this.addLayer(this._centerVector);

      for (let i of id) {
        if (layers[i].layerBounds || layers[i].extentBounds) {
          let boundsArray;
          if(layers[i].layerBounds){
            boundsArray = [
              layers[i].layerBounds.min,
              L.point(layers[i].layerBounds.max.x, layers[i].layerBounds.min.y),
              layers[i].layerBounds.max,
              L.point(layers[i].layerBounds.min.x, layers[i].layerBounds.max.y)
            ];
          } else {
            boundsArray = [
              layers[i].extentBounds.min,
              L.point(layers[i].extentBounds.max.x, layers[i].extentBounds.min.y),
              layers[i].extentBounds.max,
              L.point(layers[i].extentBounds.min.x, layers[i].extentBounds.max.y)
            ];
          }        
          let boundsRect = projectedExtent(boundsArray, {
            color: colors[j % colors.length],
            weight: 2,
            opacity: 1,
            fillOpacity: 0.01,
            fill: true,
          });
          if (layers[i].options._leafletLayer)
            boundsRect.bindTooltip(layers[i].options._leafletLayer._title, { sticky: true });
          this.addLayer(boundsRect);
          j++;
        }
      }

      if(map.totalLayerBounds){
        let totalBoundsArray = [
          map.totalLayerBounds.min,
          L.point(map.totalLayerBounds.max.x, map.totalLayerBounds.min.y),
          map.totalLayerBounds.max,
          L.point(map.totalLayerBounds.min.x, map.totalLayerBounds.max.y)
        ];

        let totalBounds = projectedExtent(
            totalBoundsArray,
            {color: "#808080", weight: 5, opacity: 0.5, fill: false});
        this.addLayer(totalBounds);
      }
    },

    _mapLayerUpdate: function (e) {
      this.clearLayers();
      this._addBounds(e.target);
    },
  });

  var debugVectors = function (options) {
    return new DebugVectors(options);
  };


  var ProjectedExtent = L.Path.extend({
    options: {
      className: "mapml-debug-extent",
    },
    initialize: function (locations, options) {
      //locations passed in as pcrs coordinates
      this._locations = locations;
      L.setOptions(this, options);
    },

    _project: function () {
      this._rings = [];
      let scale = this._map.options.crs.scale(this._map.getZoom()),
        map = this._map;
      for (let i = 0; i < this._locations.length; i++) {
        let point = map.options.crs.transformation.transform(this._locations[i], scale);
        //substract the pixel origin from the pixel coordinates to get the location relative to map viewport
        this._rings.push(L.point(point.x, point.y)._subtract(map.getPixelOrigin()));
      }
      //leaflet SVG renderer looks for and array of arrays to build polygons,
      //in this case it only deals with a rectangle so one closed array or points
      this._parts = [this._rings];
    },

    _update: function () {
      if (!this._map) return;
      this._renderer._updatePoly(this, true); //passing true creates a closed path i.e. a rectangle
    },

  });

  var projectedExtent = function (locations, options) {
    return new ProjectedExtent(locations, options);
  };

  var QueryHandler = L.Handler.extend({
      addHooks: function() {
          // get a reference to the actual <map> element, so we can 
          // use its layers property to iterate the layers from top down
          // evaluating if they are 'on the map' (enabled)
          L.setOptions(this, {mapEl: this._map.options.mapEl});
          L.DomEvent.on(this._map, 'click', this._queryTopLayer, this);
          L.DomEvent.on(this._map, 'keypress', this._queryTopLayerAtMapCenter, this);
      },
      removeHooks: function() {
          L.DomEvent.off(this._map, 'click', this._queryTopLayer, this);
          L.DomEvent.on(this._map, 'keypress', this._queryTopLayerAtMapCenter, this);
      },
      _getTopQueryableLayer: function() {
          var layers = this.options.mapEl.layers;
          // work backwards in document order (top down)
          for (var l=layers.length-1;l>=0;l--) {
            var mapmlLayer = layers[l]._layer;
            if (layers[l].checked && mapmlLayer.queryable) {
                return mapmlLayer;
            }
          }
      },
      _queryTopLayerAtMapCenter: function (event) {
        setTimeout(() => {
          if (this._map.isFocused && !this._map._popupClosed && (event.originalEvent.key === " " || +event.originalEvent.keyCode === 13)) {
            this._map.fire('click', { 
                latlng: this._map.getCenter(),
                layerPoint: this._map.latLngToLayerPoint(this._map.getCenter()),
                containerPoint: this._map.latLngToContainerPoint(this._map.getCenter())
            });
          } else {
            delete this._map._popupClosed;
          }
        }, 0);
      },
      _queryTopLayer: function(event) {
          var layer = this._getTopQueryableLayer();
          if (layer) {
              if(layer._mapmlFeatures) delete layer._mapmlFeatures;
              this._query(event, layer);
          }
      },
      _query(e, layer) {
        var zoom = e.target.getZoom(),
            map = this._map,
            crs = layer._extent.crs, // the crs for each extent would be the same
            tileSize = map.options.crs.options.crs.tile.bounds.max.x,
            container = layer._container,
            popupOptions = {autoClose: false, autoPan: true, maxHeight: (map.getSize().y * 0.5) - 50},
            tcrs2pcrs = function (c) {
              return crs.transformation.untransform(c,crs.scale(zoom));
            },
            tcrs2gcrs = function (c) {
              return crs.unproject(crs.transformation.untransform(c,crs.scale(zoom)),zoom);
            };
        var tcrsClickLoc = crs.latLngToPoint(e.latlng, zoom),
            tileMatrixClickLoc = tcrsClickLoc.divideBy(tileSize).floor(),
            tileBounds = new L.Bounds(tcrsClickLoc.divideBy(tileSize).floor().multiplyBy(tileSize), 
            tcrsClickLoc.divideBy(tileSize).ceil().multiplyBy(tileSize));

        let point = this._map.project(e.latlng),
            scale = this._map.options.crs.scale(this._map.getZoom()),
            pcrsClick = this._map.options.crs.transformation.untransform(point,scale);
        let templates = layer.getQueryTemplates(pcrsClick);

        var fetchFeatures = function(template, obj, lastOne) {
          const parser = new DOMParser();
          fetch(L.Util.template(template.template, obj), { redirect: 'follow' })
            .then((response) => {
              if (response.status >= 200 && response.status < 300) {
                return response.text().then( text => {
                  return {
                    contenttype: response.headers.get("Content-Type"),
                    text: text
                  };
                });
              } else {
                throw new Error(response.status);
              }
        }).then((response) => {
          if(!layer._mapmlFeatures) layer._mapmlFeatures = [];
          if (response.contenttype.startsWith("text/mapml")) {
            // the mapmldoc could have <map-meta> elements that are important, perhaps
            // also, the mapmldoc can have many features
            let mapmldoc = parser.parseFromString(response.text, "application/xml"),
                features = Array.prototype.slice.call(mapmldoc.querySelectorAll("map-feature"));
            if(features.length) layer._mapmlFeatures = layer._mapmlFeatures.concat(features);
          } else {
            // synthesize a single feature from text or html content
            let geom = "<map-geometry cs='gcrs'>"+e.latlng.lng+" "+e.latlng.lat+"</map-geometry>",
                feature = parser.parseFromString("<map-feature><map-properties>"+
                  response.text+"</map-properties>"+geom+"</map-feature>", "text/html").querySelector("map-feature");
            layer._mapmlFeatures.push(feature);
          }
          if(lastOne) return displayFeaturesPopup(layer._mapmlFeatures, e.latlng);
        }).catch((err) => {
          console.log('Looks like there was a problem. Status: ' + err.message);
        });
      };

      for(let i = 0; i < templates.length; i++){

        var obj = {},
            template = templates[i];
   
        // all of the following are locations that might be used in a query, I think.
        obj[template.query.tilei] = tcrsClickLoc.x.toFixed() - (tileMatrixClickLoc.x * tileSize);
        obj[template.query.tilej] = tcrsClickLoc.y.toFixed() - (tileMatrixClickLoc.y * tileSize);
        
        // this forces the click to the centre of the map extent in the layer crs
        obj[template.query.mapi] = (map.getSize().divideBy(2)).x.toFixed();
        obj[template.query.mapj] = (map.getSize().divideBy(2)).y.toFixed();
        
        obj[template.query.pixelleft] = crs.pointToLatLng(tcrsClickLoc, zoom).lng;
        obj[template.query.pixeltop] = crs.pointToLatLng(tcrsClickLoc, zoom).lat;
        obj[template.query.pixelright] = crs.pointToLatLng(tcrsClickLoc.add([1,1]), zoom).lng;
        obj[template.query.pixelbottom] = crs.pointToLatLng(tcrsClickLoc.add([1,1]), zoom).lat;
        
        obj[template.query.column] = tileMatrixClickLoc.x;
        obj[template.query.row] = tileMatrixClickLoc.y;
        obj[template.query.x] = tcrsClickLoc.x.toFixed();
        obj[template.query.y] = tcrsClickLoc.y.toFixed();
        
        // whereas the layerPoint is calculated relative to the origin plus / minus any
        // pan movements so is equal to containerPoint at first before any pans, but
        // changes as the map pans. 
        obj[template.query.easting] =  tcrs2pcrs(tcrsClickLoc).x;
        obj[template.query.northing] = tcrs2pcrs(tcrsClickLoc).y;
        obj[template.query.longitude] =  tcrs2gcrs(tcrsClickLoc).lng;
        obj[template.query.latitude] = tcrs2gcrs(tcrsClickLoc).lat;
        obj[template.query.zoom] = zoom;
        obj[template.query.width] = map.getSize().x;
        obj[template.query.height] = map.getSize().y;
        // assumes the click is at the centre of the map, per template.query.mapi, mapj above
        obj[template.query.mapbottom] = tcrs2pcrs(tcrsClickLoc.add(map.getSize().divideBy(2))).y;
        obj[template.query.mapleft] = tcrs2pcrs(tcrsClickLoc.subtract(map.getSize().divideBy(2))).x;
        obj[template.query.maptop] = tcrs2pcrs(tcrsClickLoc.subtract(map.getSize().divideBy(2))).y;
        obj[template.query.mapright] = tcrs2pcrs(tcrsClickLoc.add(map.getSize().divideBy(2))).x;
        
        obj[template.query.tilebottom] = tcrs2pcrs(tileBounds.max).y;
        obj[template.query.tileleft] = tcrs2pcrs(tileBounds.min).x;
        obj[template.query.tiletop] = tcrs2pcrs(tileBounds.min).y;
        obj[template.query.tileright] = tcrs2pcrs(tileBounds.max).x;
        // add hidden or other variables that may be present into the values to
        // be processed by L.Util.template below.
        for (var v in template.query) {
            if (["mapi","mapj","tilei","tilej","row","col","x","y","easting","northing","longitude","latitude","width","height","zoom","mapleft","mapright",",maptop","mapbottom","tileleft","tileright","tiletop","tilebottom","pixeltop","pixelbottom","pixelleft","pixelright"].indexOf(v) < 0) {
                obj[v] = template.query[v];
            }
        }

        if(template.extentBounds.contains(pcrsClick)){
          let lastOne = (i === (templates.length - 1)) ? true: false;
          fetchFeatures(template, obj, lastOne);
        }
      }
        function displayFeaturesPopup(features, loc) {

          let f = M.mapMlFeatures(features, {
              // pass the vector layer a renderer of its own, otherwise leaflet
              // puts everything into the overlayPane
              renderer: M.featureRenderer(),
              // pass the vector layer the container for the parent into which
              // it will append its own container for rendering into
              pane: container,
              //color: 'yellow',
              // instead of unprojecting and then projecting and scaling,
              // a much smarter approach would be to scale at the current
              // zoom
              projection: map.options.projection,
              _leafletLayer: layer,
              query: true,
              static:true,
          });
          f.addTo(map);

          let div = L.DomUtil.create("div", "mapml-popup-content"),
              c = L.DomUtil.create("iframe");
          c.style = "border: none";
          c.srcdoc = features[0].querySelector('map-feature map-properties').innerHTML;
          c.setAttribute("sandbox","allow-same-origin allow-forms");
          div.appendChild(c);
          // passing a latlng to the popup is necessary for when there is no
          // geometry / null geometry
          layer._totalFeatureCount = features.length;
          layer.bindPopup(div, popupOptions).openPopup(loc);
          layer.on('popupclose', function() {
              map.removeLayer(f);
          });
          f.showPaginationFeature({i: 0, popup: layer._popup});

        }
      }
  });

  /*
  MIT License related to portions of M.ContextMenu 
  Copyright (c) 2017 adam.ratcliffe@gmail.com
  Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the “Software”), 
  to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, 
  and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
  The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
  */
  var ContextMenu = L.Handler.extend({
    _touchstart: L.Browser.msPointer ? 'MSPointerDown' : L.Browser.pointer ? 'pointerdown' : 'touchstart',

    initialize: function (map) {
      L.Handler.prototype.initialize.call(this, map);

      //setting the items in the context menu and their callback functions
      this._items = [
        {
          text: M.options.locale.cmBack + " (<kbd>B</kbd>)",
          callback:this._goBack,
        },
        {
          text: M.options.locale.cmForward + " (<kbd>F</kbd>)",
          callback:this._goForward,
        },
        {
          text: M.options.locale.cmReload + " (<kbd>R</kbd>)",
          callback:this._reload,
        },
        {
          spacer:"-",
        },
        {
          text: M.options.locale.cmToggleControls + " (<kbd>T</kbd>)",
          callback:this._toggleControls,
        },
        {
          text: M.options.locale.cmCopyCoords + " (<kbd>C</kbd>)<span></span>",
          callback:this._copyCoords,
          hideOnSelect:false,
          popup:true,
          submenu:[
            {
              text:"tile",
              callback:this._copyTile,
            },
            {
              text:"tilematrix",
              callback:this._copyTileMatrix,
            },
            {
              spacer:"-",
            },
            {
              text:"map",
              callback:this._copyMap,
            },
            {
              spacer:"-",
            },
            {
              text:"tcrs",
              callback:this._copyTCRS,
            },
            {
              text:"pcrs",
              callback:this._copyPCRS,
            },
            {
              text:"gcrs",
              callback:this._copyGCRS,
            },
            {
              spacer:"-",
            },
            {
              text: M.options.locale.cmCopyAll,
              callback:this._copyAllCoords,
            }
          ]
        },
        {
          text: M.options.locale.cmToggleDebug + " (<kbd>D</kbd>)",
          callback:this._toggleDebug,
        },
        {
          text: M.options.locale.cmCopyMapML + " (<kbd>M</kbd>)",
          callback:this._copyMapML,
        },
        {
          text: M.options.locale.cmViewSource + " (<kbd>V</kbd>)",
          callback:this._viewSource,
        },
      ];

      this._layerItems = [
        {
          text: M.options.locale.lmZoomToLayer + " (<kbd>Z</kbd>)",
          callback:this._zoomToLayer
        },
        {
          text: M.options.locale.lmCopyExtent + " (<kbd>C</kbd>)",
          callback:this._copyLayerExtent
        },
      ];
      this._mapMenuVisible = false;
      this._keyboardEvent = false;

      this._container = L.DomUtil.create("div", "mapml-contextmenu", map._container);
      this._container.setAttribute('hidden', '');
      
      for (let i = 0; i < 6; i++) {
        this._items[i].el = this._createItem(this._container, this._items[i]);
      }

      this._coordMenu = L.DomUtil.create("div", "mapml-contextmenu mapml-submenu", this._container);
      this._coordMenu.id = "mapml-copy-submenu";
      this._coordMenu.setAttribute('hidden', '');

      this._clickEvent = null;

      for(let i =0;i<this._items[5].submenu.length;i++){
        this._createItem(this._coordMenu,this._items[5].submenu[i],i);
      }

      this._items[6].el = this._createItem(this._container, this._items[6]);
      this._items[7].el = this._createItem(this._container, this._items[7]);
      this._items[8].el = this._createItem(this._container, this._items[8]);

      this._layerMenu = L.DomUtil.create("div", "mapml-contextmenu mapml-layer-menu", map._container);
      this._layerMenu.setAttribute('hidden', '');
      for (let i = 0; i < this._layerItems.length; i++) {
        this._createItem(this._layerMenu, this._layerItems[i]);
      }

      L.DomEvent
        .on(this._container, 'click', L.DomEvent.stop)
        .on(this._container, 'mousedown', L.DomEvent.stop)
        .on(this._container, 'dblclick', L.DomEvent.stop)
        .on(this._container, 'contextmenu', L.DomEvent.stop)
        .on(this._layerMenu, 'click', L.DomEvent.stop)
        .on(this._layerMenu, 'mousedown', L.DomEvent.stop)
        .on(this._layerMenu, 'dblclick', L.DomEvent.stop)
        .on(this._layerMenu, 'contextmenu', L.DomEvent.stop);
    },

    addHooks: function () {
      var container = this._map.getContainer();

      L.DomEvent
        .on(container, 'mouseleave', this._hide, this)
        .on(document, 'keydown', this._onKeyDown, this);

      if (L.Browser.touch) {
        L.DomEvent.on(document, this._touchstart, this._hide, this);
      }

      this._map.on({
        contextmenu: this._show,
        mousedown: this._hide,
        zoomstart: this._hide
      }, this);
    },

    removeHooks: function () {
      var container = this._map.getContainer();

      L.DomEvent
        .off(container, 'mouseleave', this._hide, this)
        .off(document, 'keydown', this._onKeyDown, this);

      if (L.Browser.touch) {
        L.DomEvent.off(document, this._touchstart, this._hide, this);
      }

      this._map.off({
        contextmenu: this._show,
        mousedown: this._hide,
        zoomstart: this._hide
      }, this);
    },

    _copyLayerExtent: function (e) {
      let context = e instanceof KeyboardEvent ? this._map.contextMenu : this.contextMenu,
          layerElem = context._layerClicked.layer._layerEl,
          tL = layerElem.extent.topLeft.pcrs,
          bR = layerElem.extent.bottomRight.pcrs;

      let data = `<map-meta name="extent" content="top-left-easting=${tL.horizontal}, top-left-northing=${tL.vertical}, bottom-right-easting=${bR.horizontal}, bottom-right-northing=${bR.vertical}"></map-meta>`;
      context._copyData(data);
    },

    _zoomToLayer: function (e) {
      let context = e instanceof KeyboardEvent ? this._map.contextMenu : this.contextMenu;
      context._layerClicked.layer._layerEl.focus();
    },

    _goForward: function(e){
      let mapEl = e instanceof KeyboardEvent?this._map.options.mapEl:this.options.mapEl;
      mapEl.forward();
    },

    _goBack: function(e){
      let mapEl = e instanceof KeyboardEvent?this._map.options.mapEl:this.options.mapEl;
      mapEl.back();
    },

    _reload: function(e){
      let mapEl = e instanceof KeyboardEvent?this._map.options.mapEl:this.options.mapEl;
      mapEl.reload();
    },

    _toggleControls: function(e){
      let mapEl = e instanceof KeyboardEvent?this._map.options.mapEl:this.options.mapEl;
      mapEl._toggleControls();
    },

    _copyMapML: function(e){
      let context = e instanceof KeyboardEvent ? this._map.contextMenu : this.contextMenu,
        mapEl = e instanceof KeyboardEvent?this._map.options.mapEl:this.options.mapEl;
      context._copyData(mapEl.outerHTML.replace(/<div class="mapml-web-map">.*?<\/div>|<style>\[is="web-map"].*?<\/style>|<style>mapml-viewer.*?<\/style>/gm, ""));
    },

    _viewSource: function(e){
      let mapEl = e instanceof KeyboardEvent?this._map.options.mapEl:this.options.mapEl;
      mapEl.viewSource();
    },

    _toggleDebug: function(e){
      let mapEl = e instanceof KeyboardEvent?this._map.options.mapEl:this.options.mapEl;
      mapEl.toggleDebug();
    },

    _copyCoords: function(e){
      let directory = this.contextMenu?this.contextMenu:this;
      directory._showCoordMenu(e);
    },

    _copyData: function(data){
      const el = document.createElement('textarea');
      el.value = data;
      document.body.appendChild(el);
      el.select();
      document.execCommand('copy');
      document.body.removeChild(el);
    },

    _copyGCRS: function(e){
      let mapEl = this.options.mapEl,
          click = this.contextMenu._clickEvent;
      this.contextMenu._copyData(`z:${mapEl.zoom}, lon :${click.latlng.lng.toFixed(6)}, lat:${click.latlng.lat.toFixed(6)}`);
    },

    _copyTCRS: function(e){
      let mapEl = this.options.mapEl,
          click = this.contextMenu._clickEvent,
          point = mapEl._map.project(click.latlng);
      this.contextMenu._copyData(`z:${mapEl.zoom}, x:${point.x}, y:${point.y}`);
    },

    _copyTileMatrix: function(e){
      let mapEl = this.options.mapEl,
          click = this.contextMenu._clickEvent,
          point = mapEl._map.project(click.latlng),
          tileSize = mapEl._map.options.crs.options.crs.tile.bounds.max.x;
      this.contextMenu._copyData(`z:${mapEl.zoom}, column:${Math.trunc(point.x/tileSize)}, row:${Math.trunc(point.y/tileSize)}`);
    },

    _copyPCRS: function(e){
      let mapEl = this.options.mapEl,
          click = this.contextMenu._clickEvent,
          point = mapEl._map.project(click.latlng),
          scale = mapEl._map.options.crs.scale(+mapEl.zoom),
          pcrs = mapEl._map.options.crs.transformation.untransform(point,scale);
      this.contextMenu._copyData(`z:${mapEl.zoom}, easting:${pcrs.x.toFixed(2)}, northing:${pcrs.y.toFixed(2)}`);
    },

    _copyTile: function(e){
      let mapEl = this.options.mapEl,
          click = this.contextMenu._clickEvent,
          point = mapEl._map.options.crs.project(click.latlng),
          tileSize = mapEl._map.options.crs.options.crs.tile.bounds.max.x,
          pointX = point.x % tileSize, pointY = point.y % tileSize;
      if(pointX < 0) pointX+= tileSize;
      if(pointY < 0) pointY+= tileSize;

      this.contextMenu._copyData(`z:${mapEl.zoom}, i:${Math.trunc(pointX)}, j:${Math.trunc(pointY)}`);
    },

    _copyMap: function(e){
      let mapEl = this.options.mapEl,
          click = this.contextMenu._clickEvent;
      this.contextMenu._copyData(`z:${mapEl.zoom}, i:${Math.trunc(click.containerPoint.x)}, j:${Math.trunc(click.containerPoint.y)}`);
    },

    _copyAllCoords: function(e){
      let mapEl = this.options.mapEl,
      click = this.contextMenu._clickEvent,
      point = mapEl._map.project(click.latlng),
      tileSize = mapEl._map.options.crs.options.crs.tile.bounds.max.x,
      pointX = point.x % tileSize, pointY = point.y % tileSize,
      scale = mapEl._map.options.crs.scale(+mapEl.zoom),
      pcrs = mapEl._map.options.crs.transformation.untransform(point,scale);
      let allData = `z:${mapEl.zoom}\n`;
      allData += `tile: i:${Math.trunc(pointX)}, j:${Math.trunc(pointY)}\n`;
      allData += `tilematrix: column:${Math.trunc(point.x/tileSize)}, row:${Math.trunc(point.y/tileSize)}\n`;
      allData += `map: i:${Math.trunc(click.containerPoint.x)}, j:${Math.trunc(click.containerPoint.y)}\n`;
      allData += `tcrs: x:${Math.trunc(point.x)}, y:${Math.trunc(point.y)}\n`;
      allData += `pcrs: easting:${pcrs.x.toFixed(2)}, northing:${pcrs.y.toFixed(2)}\n`;
      allData += `gcrs: lon :${click.latlng.lng.toFixed(6)}, lat:${click.latlng.lat.toFixed(6)}`;
      this.contextMenu._copyData(allData);
    },

    _createItem: function (container, options, index) {
      if (options.spacer) {
        return this._createSeparator(container, index);
      }

      var itemCls = 'mapml-contextmenu-item',
          el = this._insertElementAt('button', itemCls, container, index),
          callback = this._createEventHandler(el, options.callback, options.context, options.hideOnSelect),
          html = '';

      el.innerHTML = html + options.text;
      el.setAttribute("type", "button");
      el.classList.add("mapml-button");
      if(options.popup){
        el.setAttribute("aria-haspopup", "true");
        el.setAttribute("aria-expanded", "false");
        el.setAttribute("aria-controls", "mapml-copy-submenu");
      }

      L.DomEvent
        .on(el, 'mouseover', this._onItemMouseOver, this)
        .on(el, 'mouseout', this._onItemMouseOut, this)
        .on(el, 'mousedown', L.DomEvent.stopPropagation)
        .on(el, 'click', callback);

      if (L.Browser.touch) {
        L.DomEvent.on(el, this._touchstart, L.DomEvent.stopPropagation);
      }

      // Devices without a mouse fire "mouseover" on tap, but never “mouseout"
      if (!L.Browser.pointer) {
        L.DomEvent.on(el, 'click', this._onItemMouseOut, this);
      }

      return {
        id: L.Util.stamp(el),
        el: el,
        callback: callback
      };
    },

    _createSeparator: function (container, index) {
      let el = this._insertElementAt('div', 'mapml-contextmenu-separator', container, index);

      return {
        id: L.Util.stamp(el),
        el: el
      };
    },

    _createEventHandler: function (el, func, context, hideOnSelect) {
      let parent = this;

      hideOnSelect = (hideOnSelect !== undefined) ? hideOnSelect : true;

      return function (e) {
        let map = parent._map,
          containerPoint = parent._showLocation.containerPoint,
          layerPoint = map.containerPointToLayerPoint(containerPoint),
          latlng = map.layerPointToLatLng(layerPoint),
          relatedTarget = parent._showLocation.relatedTarget,
          data = {
            containerPoint: containerPoint,
            layerPoint: layerPoint,
            latlng: latlng,
            relatedTarget: relatedTarget
          };

        if (hideOnSelect) {
          parent._hide();
        }

        if (func) {
          func.call(context || map, data);
        }

        parent._map.fire('contextmenu.select', {
          contextmenu: parent,
          el: el
        });
      };
    },

    _insertElementAt: function (tagName, className, container, index) {
        let refEl,
            el = document.createElement(tagName);

        el.className = className;

        if (index !== undefined) {
            refEl = container.children[index];
        }

        if (refEl) {
            container.insertBefore(el, refEl);
        } else {
            container.appendChild(el);
        }

        return el;
    },

    _show: function (e) {
      if(this._mapMenuVisible) this._hide();
      this._clickEvent = e;
      let elem = e.originalEvent.target;
      if(elem.closest("fieldset")){
        elem = elem.closest("fieldset");
        elem = (elem.className === "mapml-layer-extent") ? elem.closest("fieldset").parentNode.parentNode.parentNode.querySelector("span") : elem.querySelector("span");
        if(!elem.layer.validProjection) return;
        this._layerClicked = elem;
        this._showAtPoint(e.containerPoint, e, this._layerMenu);
      } else if(elem.classList.contains("leaflet-container") || elem.classList.contains("mapml-debug-extent") ||
        elem.tagName === "path") {
        this._layerClicked = undefined;
        this._showAtPoint(e.containerPoint, e, this._container);
      }
      if(e.originalEvent.button === 0 || e.originalEvent.button === -1){
        this._keyboardEvent = true;
        if(this._layerClicked){
          let activeEl = document.activeElement;
          this._elementInFocus = activeEl.shadowRoot.activeElement;
          this._layerMenuTabs = 1;
          this._layerMenu.firstChild.focus();
        } else {
          this._container.firstChild.focus();
        }

      }
    },

    _showAtPoint: function (pt, data, container) {
        if (this._items.length) {
            let event = L.extend(data || {}, {contextmenu: this});

            this._showLocation = {
                containerPoint: pt
            };

            if (data && data.relatedTarget){
                this._showLocation.relatedTarget = data.relatedTarget;
            }

            this._setPosition(pt,container);

            if (!this._mapMenuVisible) {
              container.removeAttribute('hidden');
                this._mapMenuVisible = true;
            }

            this._map.fire('contextmenu.show', event);
        }
    },

    _hide: function () {
        if (this._mapMenuVisible) {
            this._mapMenuVisible = false;
            this._container.setAttribute('hidden', '');
            this._coordMenu.setAttribute('hidden', '');
            this._layerMenu.setAttribute('hidden', '');
            this._map.fire('contextmenu.hide', {contextmenu: this});
            setTimeout(() => this._map._container.focus(), 0);
        }
    },

    _setPosition: function (pt, container) {
        let mapSize = this._map.getSize(),
            containerSize = this._getElementSize(container),
            anchor;

        if (this._map.options.contextmenuAnchor) {
            anchor = L.point(this._map.options.contextmenuAnchor);
            pt = pt.add(anchor);
        }

        container._leaflet_pos = pt;

        if (pt.x + containerSize.x > mapSize.x) {
            container.style.left = 'auto';
            container.style.right = Math.min(Math.max(mapSize.x - pt.x, 0), mapSize.x - containerSize.x - 1) + 'px';
        } else {
            container.style.left = Math.max(pt.x, 0) + 'px';
            container.style.right = 'auto';
        }

        if (pt.y + containerSize.y > mapSize.y) {
            container.style.top = 'auto';
            container.style.bottom = Math.min(Math.max(mapSize.y - pt.y, 0), mapSize.y - containerSize.y - 1) + 'px';
        } else {
            container.style.top = Math.max(pt.y, 0) + 'px';
            container.style.bottom = 'auto';
        }
    },

    _getElementSize: function (el) {
        let size = this._size;

        if (!size || this._sizeChanged) {
            size = {};

            el.style.left = '-999999px';
            el.style.right = 'auto';

            size.x = el.offsetWidth;
            size.y = el.offsetHeight;

            el.style.left = 'auto';

            this._sizeChanged = false;
        }

        return size;
    },

     // once tab is clicked on the layer menu, change the focus back to the layer control
     _focusOnLayerControl: function(){
      this._mapMenuVisible = false;
      delete this._layerMenuTabs;
      this._layerMenu.setAttribute('hidden', '');
      if(this._elementInFocus){
        this._elementInFocus.focus();
      } else {
        this._layerClicked.parentElement.firstChild.focus();
      }
      delete this._elementInFocus;
    },

    _onKeyDown: function (e) {
      if(!this._mapMenuVisible) return;

      let key = e.keyCode;
      let path = e.path || e.composedPath();

      if(key === 13)
        e.preventDefault();
      // keep track of where the focus is on the layer menu and when the layer menu is tabbed out of, focus on layer control
      if(this._layerMenuTabs && (key === 9 || key === 27)){
        if(e.shiftKey){
          this._layerMenuTabs -= 1;
        } else {
          this._layerMenuTabs += 1;
        }
        if(this._layerMenuTabs === 0 || this._layerMenuTabs === 3 || key === 27){
          L.DomEvent.stop(e);
          this._focusOnLayerControl();
        } 
      } else if(key !== 16 && key!== 9 && !(!this._layerClicked && key === 67) && path[0].innerText !== (M.options.locale.cmCopyCoords + " (C)")){
        this._hide();
      }
      switch(key){
        case 13:  //ENTER KEY
        case 32:  //SPACE KEY
          if(this._map._container.parentNode.activeElement.parentNode.classList.contains("mapml-contextmenu"))
            this._map._container.parentNode.activeElement.click();
          break;
        case 66: //B KEY
          this._goBack(e);
          break;
        case 67: //C KEY
          if(this._layerClicked){
            this._copyLayerExtent(e);
          } else {
            this._copyCoords({
              latlng:this._map.getCenter()
            });
          }
          break;
        case 68: //D KEY
          this._toggleDebug(e);
          break;
        case 77: //M KEY
          this._copyMapML(e);
          break;
        case 70: //F KEY
          this._goForward(e);
          break;
        case 82: //R KEY
          this._reload(e);
          break;
        case 84: //T KEY
          this._toggleControls(e);
          break;
        case 86: //V KEY
          this._viewSource(e);
          break;
        case 90: //Z KEY
          if(this._layerClicked)
            this._zoomToLayer(e);
          break;
      }
    },

    _showCoordMenu: function(e){
      let mapSize = this._map.getSize(),
          click = this._clickEvent,
          menu = this._coordMenu,
          copyEl = this._items[5].el.el;

      copyEl.setAttribute("aria-expanded","true");
      menu.removeAttribute('hidden');

      if (click.containerPoint.x + 160 + 80 > mapSize.x) {
        menu.style.left = 'auto';
        menu.style.right = 160 + 'px';
      } else {
        menu.style.left = 160 + 'px';
        menu.style.right = 'auto';
      }

      if (click.containerPoint.y + 160 > mapSize.y) {
        menu.style.top = 'auto';
        menu.style.bottom = 20 + 'px';
      } else {
        menu.style.top = 100 + 'px';
        menu.style.bottom = 'auto';
      }
      if(this._keyboardEvent)menu.firstChild.focus();
    },

    _hideCoordMenu: function(e){
      if(e.srcElement.parentElement.classList.contains("mapml-submenu") ||
          e.srcElement.innerText === (M.options.locale.cmCopyCoords + " (C)"))return;
      let menu = this._coordMenu, copyEl = this._items[5].el.el;
      copyEl.setAttribute("aria-expanded","false");
      menu.setAttribute('hidden', '');
    },

    _onItemMouseOver: function (e) {
      L.DomUtil.addClass(e.target || e.srcElement, 'over');
      if(e.srcElement.innerText === (M.options.locale.cmCopyCoords + " (C)")) this._showCoordMenu(e);
    },

    _onItemMouseOut: function (e) {
      L.DomUtil.removeClass(e.target || e.srcElement, 'over');
      this._hideCoordMenu(e);
    }
  });

  var Util = {
    convertAndFormatPCRS : function(pcrsBounds, map){
      if(!pcrsBounds || !map) return {};

      let tcrsTopLeft = [], tcrsBottomRight = [],
          tileMatrixTopLeft = [], tileMatrixBottomRight = [],
          tileSize = map.options.crs.options.crs.tile.bounds.max.y;

      for(let i = 0;i<map.options.crs.options.resolutions.length;i++){
        let scale = map.options.crs.scale(i),
            minConverted = map.options.crs.transformation.transform(pcrsBounds.min,scale),
            maxConverted = map.options.crs.transformation.transform(pcrsBounds.max,scale);
            
        tcrsTopLeft.push({
          horizontal: minConverted.x,
          vertical:maxConverted.y,
        });
        tcrsBottomRight.push({
          horizontal: maxConverted.x,
          vertical: minConverted.y,
        });

        //converts the tcrs values from earlier to tilematrix
        tileMatrixTopLeft.push({
          horizontal: tcrsTopLeft[i].horizontal / tileSize,
          vertical:tcrsTopLeft[i].vertical / tileSize,
        });
        tileMatrixBottomRight.push({
          horizontal: tcrsBottomRight[i].horizontal / tileSize,
          vertical: tcrsBottomRight[i].vertical / tileSize,
        });
      }
      
      //converts the gcrs, I believe it can take any number values from -inf to +inf
      let unprojectedMin = map.options.crs.unproject(pcrsBounds.min),
          unprojectedMax = map.options.crs.unproject(pcrsBounds.max);

      let gcrs = {
        topLeft:{
          horizontal: unprojectedMin.lng,
          vertical:unprojectedMax.lat,
        },
        bottomRight:{
          horizontal: unprojectedMax.lng,
          vertical: unprojectedMin.lat,
        },
      };

      //formats known pcrs bounds to correct format
      let pcrs = {
        topLeft:{
          horizontal:pcrsBounds.min.x,
          vertical:pcrsBounds.max.y,
        },
        bottomRight:{
          horizontal:pcrsBounds.max.x,
          vertical:pcrsBounds.min.y,
        },
      };

      //formats all extent data
      return {
        topLeft:{
          tcrs:tcrsTopLeft,
          tilematrix:tileMatrixTopLeft,
          gcrs:gcrs.topLeft,
          pcrs:pcrs.topLeft,
        },
        bottomRight:{
          tcrs:tcrsBottomRight,
          tilematrix:tileMatrixBottomRight,
          gcrs:gcrs.bottomRight,
          pcrs:pcrs.bottomRight,
        },
        projection:map.options.projection
      };
    },
    extractInputBounds: function(template){
      if(!template) return undefined;

      //sets variables with their respective fallback values incase content is missing from the template
      let inputs = template.values, projection = template.projection || FALLBACK_PROJECTION, value = 0, boundsUnit = FALLBACK_CS;
      let bounds = this[projection].options.crs.tilematrix.bounds(0), 
          defaultMinZoom = 0, defaultMaxZoom = this[projection].options.resolutions.length - 1,
          nativeMinZoom = defaultMinZoom, nativeMaxZoom = defaultMaxZoom;
      let locInputs = false, numberOfAxes = 0;
      for(let i=0;i<inputs.length;i++){
        switch(inputs[i].getAttribute("type")){
          case "zoom":
            nativeMinZoom = +(inputs[i].hasAttribute("min") && !isNaN(+inputs[i].getAttribute("min")) ? inputs[i].getAttribute("min") : defaultMinZoom);
            nativeMaxZoom = +(inputs[i].hasAttribute("max") && !isNaN(+inputs[i].getAttribute("max")) ? inputs[i].getAttribute("max") : defaultMaxZoom);
            value = +inputs[i].getAttribute("value");
          break;
          case "location":
            if(!inputs[i].getAttribute("max") || !inputs[i].getAttribute("min")) continue;
            let max = +inputs[i].getAttribute("max"),min = +inputs[i].getAttribute("min");
            switch(inputs[i].getAttribute("axis").toLowerCase()){
              case "x":
              case "longitude":
              case "column":
              case "easting":
                boundsUnit = M.axisToCS(inputs[i].getAttribute("axis").toLowerCase());
                bounds.min.x = min;
                bounds.max.x = max;
                numberOfAxes++;
              break;
              case "y":
              case "latitude":
              case "row":
              case "northing":
                boundsUnit = M.axisToCS(inputs[i].getAttribute("axis").toLowerCase());
                bounds.min.y = min;
                bounds.max.y = max;
                numberOfAxes++;
              break;
            }
          break;
        }
      }
      if (numberOfAxes >= 2) {
        locInputs = true;
      }
      // min/maxZoom are copied from <meta name=zoom content="min=n,max=m>, are *display* range for content
      // min/maxNativeZoom are received from <input type=zoom min=... max=...>, describe *server* content availability
      let zoomBounds = {
        minZoom: template.zoomBounds?.min && !isNaN(+template.zoomBounds.min) ? +template.zoomBounds.min : defaultMinZoom,
        maxZoom: template.zoomBounds?.max && !isNaN(+template.zoomBounds.max) ? +template.zoomBounds.max : defaultMaxZoom,
        minNativeZoom: nativeMinZoom,
        maxNativeZoom: nativeMaxZoom
      };
      if(!locInputs && template.extentPCRSFallback && template.extentPCRSFallback.bounds) {
        bounds = template.extentPCRSFallback.bounds;
      } else if (locInputs) {
        bounds = this.boundsToPCRSBounds(bounds,value,projection,boundsUnit);
      } else {
        bounds = this[projection].options.crs.pcrs.bounds;
      }
      return {
        zoomBounds: zoomBounds,
        bounds: bounds
      };
    },

    axisToCS : function(axis){
      try{
        switch(axis.toLowerCase()){
          case "row":
          case "column":
            return "TILEMATRIX";
          case "i":
          case "j":
            return ["MAP","TILE"];
          case "x":
          case "y":
            return "TCRS";
          case "latitude":
          case "longitude":
            return "GCRS";
          case "northing":
          case "easting":
            return "PCRS";
          default:
            return FALLBACK_CS;
        }
      } catch (e) {return undefined;}
    },

    //takes a given cs and retuns the axes, first horizontal then vertical
    csToAxes: function(cs){
      try{
        switch(cs.toLowerCase()){
          case "tilematrix":
            return ["column", "row"];
          case "map":
          case "tile":
            return ["i", "j"];
          case "tcrs":
            return ["x", "y"];
          case "gcrs":
            return ["longitude", "latitude"];
          case "pcrs":
            return ["easting", "northing"];
        }
      } catch (e) {return undefined;}
    },

    axisToXY: function(axis){
      try{
        switch(axis.toLowerCase()){
          case "i":
          case "column":
          case "longitude":
          case "x":
          case "easting":
            return "x";
          case "row":
          case "j":
          case "latitude":
          case "y":
          case "northing":
            return "y";

          default:
            return undefined;
        }
      } catch (e) {return undefined;}
    },

    convertPCRSBounds: function(pcrsBounds, zoom, projection, cs){
      if(!pcrsBounds || (!zoom && zoom !== 0) || !Number.isFinite(+zoom) || !projection || !cs) return undefined;
      projection = (typeof projection === "string") ? M[projection] : projection;
      switch (cs.toUpperCase()) {
        case "PCRS":
          return pcrsBounds;
        case "TCRS":
        case "TILEMATRIX":
          let minPixel = projection.transformation.transform(pcrsBounds.min, projection.scale(+zoom)),
              maxPixel = projection.transformation.transform(pcrsBounds.max, projection.scale(+zoom));
          if (cs.toUpperCase() === "TCRS") return L.bounds(minPixel, maxPixel);
          let tileSize = projection.options.crs.tile.bounds.max.x;
          return L.bounds(L.point(minPixel.x / tileSize, minPixel.y / tileSize), L.point(maxPixel.x / tileSize,maxPixel.y / tileSize)); 
        case "GCRS":
          let minGCRS = projection.unproject(pcrsBounds.min),
              maxGCRS = projection.unproject(pcrsBounds.max);
          return L.bounds(L.point(minGCRS.lng, minGCRS.lat), L.point(maxGCRS.lng, maxGCRS.lat)); 
        default:
          return undefined;
      }
    },

    pointToPCRSPoint: function(point, zoom, projection, cs){
      if(!point || (!zoom && zoom !== 0) || !Number.isFinite(+zoom) || !cs || !projection) return undefined;
      projection = (typeof projection === "string") ? M[projection] : projection;
      let tileSize = projection.options.crs.tile.bounds.max.x;
      switch(cs.toUpperCase()){
        case "TILEMATRIX":
          return M.pixelToPCRSPoint(L.point(point.x*tileSize,point.y*tileSize),zoom,projection);
        case "PCRS":
          return point;
        case "TCRS" :
          return M.pixelToPCRSPoint(point,zoom,projection);
        case "GCRS":
          return projection.project(L.latLng(point.y,point.x));
        default:
          return undefined;
      }
    },

    pixelToPCRSPoint: function(point, zoom, projection){
      if(!point || (!zoom && zoom !== 0) || !Number.isFinite(+zoom) || !projection) return undefined;
      projection = (typeof projection === "string") ? M[projection] : projection;
      return projection.transformation.untransform(point,projection.scale(zoom));
    },

    boundsToPCRSBounds: function(bounds, zoom, projection, cs){
      if(!bounds || !bounds.max || !bounds.min || (!zoom && zoom !== 0) || !Number.isFinite(+zoom) || !projection || !cs) return undefined;
      projection = (typeof projection === "string") ? M[projection] : projection;
      return L.bounds(M.pointToPCRSPoint(bounds.min, zoom, projection, cs), M.pointToPCRSPoint(bounds.max, zoom, projection, cs));
    },

    //L.bounds have fixed point positions, where min is always topleft, max is always bottom right, and the values are always sorted by leaflet
    //important to consider when working with pcrs where the origin is not topleft but rather bottomleft, could lead to confusion
    pixelToPCRSBounds : function(bounds, zoom, projection){
      if(!bounds || !bounds.max || !bounds.min || (!zoom && zoom !== 0) || !Number.isFinite(+zoom) || !projection) return undefined;
      projection = (typeof projection === "string") ? M[projection] : projection;
      return L.bounds(M.pixelToPCRSPoint(bounds.min, zoom, projection), M.pixelToPCRSPoint(bounds.max, zoom, projection));
    },
    //meta content is the content attribute of meta
    // input "max=5,min=4" => [[max,5][min,5]]
    metaContentToObject: function(input){
      if(!input || input instanceof Object)return {};
      let content = input.split(/\s+/).join("");
      let contentArray = {};
      let stringSplit = content.split(',');

      for(let i=0;i<stringSplit.length;i++){
        let prop = stringSplit[i].split("=");
        if(prop.length === 2) contentArray[prop[0]]=prop[1];
      }
      if(contentArray !== "" && stringSplit[0].split("=").length ===1)contentArray.content = stringSplit[0];
      return contentArray;
    },
    coordsToArray: function(containerPoints) {
      // returns an array of arrays of coordinate pairs coordsToArray("1,2,3,4") -> [[1,2],[3,4]]
      for (var i=1, pairs = [], coords = containerPoints.split(",");i<coords.length;i+=2) {
        pairs.push([parseInt(coords[i-1]),parseInt(coords[i])]);
      }
      return pairs;
    },
    parseStylesheetAsHTML: function(mapml, base, container) {
        if (!(container instanceof Element) || !mapml || !mapml.querySelector('map-link[rel=stylesheet],map-style')) return;

        if(base instanceof Element) {
          base = base.getAttribute('href')?base.getAttribute('href'):document.URL;
        } else if (!base || base==="" || base instanceof Object) {
          return;
        }

        var ss = [];
        var stylesheets = mapml.querySelectorAll('map-link[rel=stylesheet],map-style');
        for (var i=0;i<stylesheets.length;i++) {
          if (stylesheets[i].nodeName.toUpperCase() === "MAP-LINK" ) {
            var href = stylesheets[i].hasAttribute('href') ? new URL(stylesheets[i].getAttribute('href'),base).href: null;
            if (href) {
              if (!container.querySelector("link[href='"+href+"']")) {
                var linkElm = document.createElement("link");
                linkElm.setAttribute("href", href);
                linkElm.setAttribute("rel", "stylesheet");
                ss.push(linkElm);
              }
            }  
          } else { // <map-style>
              var styleElm = document.createElement('style');
              styleElm.textContent = stylesheets[i].textContent;
              ss.push(styleElm);
          }
        }
        // insert <link> or <style> elements after the begining  of the container
        // element, in document order as copied from original mapml document
        // note the code below assumes hrefs have been resolved and elements
        // re-parsed from xml and serialized as html elements ready for insertion
        for (var s=ss.length-1;s >= 0;s--) {
          container.insertAdjacentElement('afterbegin',ss[s]);
        }
    },

    splitCoordinate: function(element, index, array) {
      var a = [];
      element.split(/\s+/gim).forEach(M.parseNumber,a);
      this.push(a);
    },

    parseNumber : function(element, index, array){
      this.push(parseFloat(element));
    },

    handleLink: function (link, leafletLayer) {
      let zoomTo, justPan = false, layer, map = leafletLayer._map, opacity;
      if(link.type === "text/html" && link.target !== "_blank"){  // all other target values other than blank behave as _top
        link.target = "_top";
      } else if (link.type !== "text/html" && link.url.includes("#")){
        let hash = link.url.split("#"), loc = hash[1].split(",");
        zoomTo = {z: loc[0] || 0, lng: loc[1] || 0, lat: loc[2] || 0};
        justPan = !hash[0]; // if the first half of the array is an empty string then the link is just for panning
        if(["/", ".","#"].includes(link.url[0])) link.target = "_self";
      }
      if(!justPan) {
        let newLayer = false;
        layer = document.createElement('layer-');
        layer.setAttribute('src', link.url);
        layer.setAttribute('checked', '');
        switch (link.target) {
          case "_blank":
            if (link.type === "text/html") {
              window.open(link.url);
            } else {
              map.options.mapEl.appendChild(layer);
              newLayer = true;
            }
            break;
          case "_parent":
            for (let l of map.options.mapEl.querySelectorAll("layer-"))
              if (l._layer !== leafletLayer) map.options.mapEl.removeChild(l);
            map.options.mapEl.appendChild(layer);
            map.options.mapEl.removeChild(leafletLayer._layerEl);
            newLayer = true;
            break;
          case "_top":
            window.location.href = link.url;
            break;
          default:
            opacity = leafletLayer._layerEl.opacity;
            leafletLayer._layerEl.insertAdjacentElement('beforebegin', layer);
            map.options.mapEl.removeChild(leafletLayer._layerEl);
            newLayer = true;
        }
        if(!link.inPlace && newLayer) L.DomEvent.on(layer,'extentload', function focusOnLoad(e) {
          if(newLayer && ["_parent", "_self"].includes(link.target) && layer.parentElement.querySelectorAll("layer-").length === 1)
            layer.parentElement.projection = layer._layer.getProjection();
          if(layer.extent){
            if(zoomTo) layer.parentElement.zoomTo(+zoomTo.lat, +zoomTo.lng, +zoomTo.z);
            else layer.focus();
            L.DomEvent.off(layer, 'extentload', focusOnLoad);
          }

          if(opacity) layer.opacity = opacity;
          map.getContainer().focus();
        });
      } else if (zoomTo && !link.inPlace && justPan){
        leafletLayer._map.options.mapEl.zoomTo(+zoomTo.lat, +zoomTo.lng, +zoomTo.z);
        if(opacity) layer.opacity = opacity;
      }
    },

    gcrsToTileMatrix: function (mapEl) {
      let point = mapEl._map.project(mapEl._map.getCenter());
      let tileSize = mapEl._map.options.crs.options.crs.tile.bounds.max.y;
      let column = Math.trunc(point.x / tileSize);
      let row = Math.trunc(point.y / tileSize);
      return [column, row];
    }
  };

  var ReloadButton = L.Control.extend({
    options: {
      position: 'topleft',
    },

    onAdd: function (map) {
      let container = L.DomUtil.create("div", "mapml-reload-button leaflet-bar");

      let link = L.DomUtil.create("button", "mapml-reload-button", container);
      link.innerHTML = "<span aria-hidden='true'>&#x021BA</span>";
      link.title = M.options.locale.cmReload;
      link.setAttribute("type", "button");
      link.classList.add("mapml-button");
      link.setAttribute('aria-label', "Reload");

      L.DomEvent.disableClickPropagation(link);
      L.DomEvent.on(link, 'click', L.DomEvent.stop);
      L.DomEvent.on(link, 'click', this._goReload, this);

      this._reloadButton = link;

      this._updateDisabled();
      map.on('moveend', this._updateDisabled, this);

      return container;
    },

    onRemove: function (map) {
      map.off('moveend', this._updateDisabled, this);
    },

    disable: function () {
      this._disabled = true;
      this._updateDisabled();
      return this;
    },

    enable: function () {
      this._disabled = false;
      this._updateDisabled();
      return this;
    },

    _goReload: function (e) {
      if (!this._disabled && this._map.options.mapEl._history.length > 1) {
        this._map.options.mapEl.reload();
      }
    },

    _updateDisabled: function () {
      setTimeout(() => {
        L.DomUtil.removeClass(this._reloadButton, "leaflet-disabled");
        this._reloadButton.setAttribute("aria-disabled", "false");

        if (this._map && (this._disabled || this._map.options.mapEl._history.length <= 1)) {
          L.DomUtil.addClass(this._reloadButton, "leaflet-disabled");
          this._reloadButton.setAttribute("aria-disabled", "true");
        }
      }, 0);
    }
  });

  var reloadButton = function (options) {
    return new ReloadButton(options);
  };

  var FullscreenButton = L.Control.extend({
          options: {
              position: 'topleft',
              title: {
                  'false': 'View fullscreen',
                  'true': 'Exit fullscreen'
              }
          },

          onAdd: function (map) {
              var container = L.DomUtil.create('div', 'leaflet-control-fullscreen leaflet-bar leaflet-control');

              this.link = L.DomUtil.create('a', 'leaflet-control-fullscreen-button leaflet-bar-part', container);
              this.link.href = '#';
              this.link.setAttribute('role', 'button');

              this._map = map;
              this._map.on('fullscreenchange', this._toggleTitle, this);
              this._toggleTitle();

              L.DomEvent.on(this.link, 'click', this._click, this);

              return container;
          },

          onRemove: function (map) {
              map.off('fullscreenchange', this._toggleTitle, this);
          },

          _click: function (e) {
              L.DomEvent.stopPropagation(e);
              L.DomEvent.preventDefault(e);
              this._map.toggleFullscreen(this.options);
          },

          _toggleTitle: function() {
              this.link.title = this.options.title[this._map.isFullscreen()];
          }
      });

      L.Map.include({
          isFullscreen: function () {
              return this._isFullscreen || false;
          },

          toggleFullscreen: function (options) {
              // the <map> element can't contain a shadow root, so we used a child <div>
              // <mapml-viewer> can contain a shadow root, so return it directly
              var mapEl = this.getContainer().getRootNode().host,
                  container = mapEl.nodeName === "DIV" ? mapEl.parentElement : mapEl;
              if (this.isFullscreen()) {
                  if (options && options.pseudoFullscreen) {
                      this._disablePseudoFullscreen(container);
                  } else if (document.exitFullscreen) {
                      document.exitFullscreen();
                  } else if (document.mozCancelFullScreen) {
                      document.mozCancelFullScreen();
                  } else if (document.webkitCancelFullScreen) {
                      document.webkitCancelFullScreen();
                  } else if (document.msExitFullscreen) {
                      document.msExitFullscreen();
                  } else {
                      this._disablePseudoFullscreen(container);
                  }
              } else {
                  if (options && options.pseudoFullscreen) {
                      this._enablePseudoFullscreen(container);
                  } else if (container.requestFullscreen) {
                      container.requestFullscreen();
                  } else if (container.mozRequestFullScreen) {
                      container.mozRequestFullScreen();
                  } else if (container.webkitRequestFullscreen) {
                      container.webkitRequestFullscreen(Element.ALLOW_KEYBOARD_INPUT);
                  } else if (container.msRequestFullscreen) {
                      container.msRequestFullscreen();
                  } else {
                      this._enablePseudoFullscreen(container);
                  }
              }

          },

          _enablePseudoFullscreen: function (container) {
              L.DomUtil.addClass(container, 'leaflet-pseudo-fullscreen');
              this._setFullscreen(true);
              this.fire('fullscreenchange');
          },

          _disablePseudoFullscreen: function (container) {
              L.DomUtil.removeClass(container, 'leaflet-pseudo-fullscreen');
              this._setFullscreen(false);
              this.fire('fullscreenchange');
          },

          _setFullscreen: function(fullscreen) {
              this._isFullscreen = fullscreen;
              var container = this.getContainer().getRootNode().host;
              if (fullscreen) {
                  L.DomUtil.addClass(container, 'mapml-fullscreen-on');
              } else {
                  L.DomUtil.removeClass(container, 'mapml-fullscreen-on');
              }
              this.invalidateSize();
          },

          _onFullscreenChange: function (e) {
              var fullscreenElement =
                  document.fullscreenElement ||
                  document.mozFullScreenElement ||
                  document.webkitFullscreenElement ||
                  document.msFullscreenElement,
                  mapEl = this.getContainer().getRootNode().host,
                  container = mapEl.nodeName === "DIV" ? mapEl.parentElement : mapEl;
              

              if (fullscreenElement === container && !this._isFullscreen) {
                  this._setFullscreen(true);
                  this.fire('fullscreenchange');
              } else if (fullscreenElement !== container && this._isFullscreen) {
                  this._setFullscreen(false);
                  this.fire('fullscreenchange');
              }
          }
      });

      L.Map.mergeOptions({
          fullscreenControl: false
      });

      L.Map.addInitHook(function () {
          if (this.options.fullscreenControl) {
              this.fullscreenControl = new FullscreenButton(this.options.fullscreenControl);
              this.addControl(this.fullscreenControl);
          }

          var fullscreenchange;

          if ('onfullscreenchange' in document) {
              fullscreenchange = 'fullscreenchange';
          } else if ('onmozfullscreenchange' in document) {
              fullscreenchange = 'mozfullscreenchange';
          } else if ('onwebkitfullscreenchange' in document) {
              fullscreenchange = 'webkitfullscreenchange';
          } else if ('onmsfullscreenchange' in document) {
              fullscreenchange = 'MSFullscreenChange';
          }

          if (fullscreenchange) {
              var onFullscreenChange = L.bind(this._onFullscreenChange, this);

              this.whenReady(function () {
                  L.DomEvent.on(document, fullscreenchange, onFullscreenChange);
              });

              this.on('unload', function () {
                  L.DomEvent.off(document, fullscreenchange, onFullscreenChange);
              });
          }
      });

      var fullscreenButton = function (options) {
          return new FullscreenButton(options);
      };

  var Crosshair = L.Layer.extend({
    onAdd: function (map) {

      // SVG crosshair design from https://github.com/xguaita/Leaflet.MapCenterCoord/blob/master/src/icons/MapCenterCoordIcon1.svg?short_path=81a5c76
      // Optimized with SVGOMG: https://jakearchibald.github.io/svgomg/
      let svgInnerHTML = `<svg xmlns="http://www.w3.org/2000/svg" xml:space="preserve" viewBox="0 0 100 100"><g stroke="#fff" stroke-linecap="round" stroke-linejoin="round"><circle cx="50.028" cy="50.219" r="3.923" stroke-width="2" color="currentColor" overflow="visible"/><path stroke-width="3" d="M4.973 54.424h31.768a4.204 4.204 0 1 0 0-8.409H4.973A4.203 4.203 0 0 0 .77 50.22a4.203 4.203 0 0 0 4.204 4.205z" color="currentColor" overflow="visible"/><path stroke-width="3" d="M54.232 5.165a4.204 4.204 0 1 0-8.408 0v31.767a4.204 4.204 0 1 0 8.408 0V5.165z"/><path stroke-width="3" d="M99.288 50.22a4.204 4.204 0 0 0-4.204-4.205H63.317a4.204 4.204 0 1 0 0 8.409h31.767a4.205 4.205 0 0 0 4.204-4.205zM45.823 95.274a4.204 4.204 0 1 0 8.409 0V63.506a4.204 4.204 0 1 0-8.409 0v31.768z" color="currentColor" overflow="visible"/></g></svg>`;

      this._container = L.DomUtil.create("div", "mapml-crosshair", map._container);
      this._container.innerHTML = svgInnerHTML;
      map.isFocused = false;
      this._isQueryable = false;

      map.on("layerchange layeradd layerremove overlayremove", this._toggleEvents, this);
      map.on("popupopen", this._isMapFocused, this);
      L.DomEvent.on(map._container, "keydown keyup mousedown", this._isMapFocused, this);

      this._addOrRemoveCrosshair();
    },

    onRemove: function (map) {
      map.off("layerchange layeradd layerremove overlayremove", this._toggleEvents);
      map.off("popupopen", this._isMapFocused);
      L.DomEvent.off(map._container, "keydown keyup mousedown", this._isMapFocused);
    },

    _toggleEvents: function () {
      if (this._hasQueryableLayer()) {
        this._map.on("viewreset move moveend", this._addOrRemoveCrosshair, this);
      } else {
        this._map.off("viewreset move moveend", this._addOrRemoveCrosshair, this);
      }
      this._addOrRemoveCrosshair();
    },

    _addOrRemoveCrosshair: function (e) {
      if (this._hasQueryableLayer()) {
        this._container.removeAttribute("hidden");
      } else {
        this._container.setAttribute("hidden", "");
      }
    },

    _addOrRemoveMapOutline: function (e) {
      let mapContainer = this._map._container;
      if (this._map.isFocused && !this._outline) {
        this._outline = L.DomUtil.create("div", "mapml-outline", mapContainer);
      } else if (!this._map.isFocused && this._outline) {
        L.DomUtil.remove(this._outline);
        delete this._outline;
      }
    },

    _hasQueryableLayer: function () {
      let layers = this._map.options.mapEl.layers;
      if (this._map.isFocused) {
        for (let layer of layers) {
          if (layer.checked && layer._layer.queryable) {
            return true;
          }
        }
      }
      return false;
    },

    // TODO: should be merged with the 'mapfocused' event emitted by mapml-viewer and map, not trivial
    _isMapFocused: function (e) {
      //set this._map.isFocused = true if arrow buttons are used
      if(!this._map._container.parentNode.activeElement){
        this._map.isFocused = false;
        return;
      }
      let isLeafletContainer = this._map._container.parentNode.activeElement.classList.contains("leaflet-container");
      if (isLeafletContainer && ["keydown"].includes(e.type) && (e.shiftKey && e.keyCode === 9)) {
        this._map.isFocused = false;
      } else this._map.isFocused = isLeafletContainer && ["keyup", "keydown"].includes(e.type);

      if(this._map.isFocused) this._map.fire("mapkeyboardfocused");
      this._addOrRemoveMapOutline();
      this._addOrRemoveCrosshair();
    },

  });


  var crosshair = function (options) {
    return new Crosshair(options);
  };

  /**
   * M.Feature is a extension of L.Path that understands mapml feature markup
   * It converts the markup to the following structure (abstract enough to encompass all feature types) for example:
   *  this._outlinePath = HTMLElement;
   *  this._parts = [
   *    {
   *      path: HTMLElement,
   *      rings:[
   *        {points:[{x:1,y:1}, ...]},
   *        ...
   *      ],
   *      subrings:[
   *        {points:[{x:2, y:2}, ...], cls:"Span Class Name", path: HTMLElement,},
   *        ...
   *      ],
   *      cls:"className",
   *    },
   *    ...
   *  ];
   */
  var Feature = L.Path.extend({

    /**
     * Initializes the M.Feature
     * @param {HTMLElement} markup - The markup representation of the feature
     * @param {Object} options - The options of the feature
     */
    initialize: function (markup, options) {
      this.type = markup.tagName.toUpperCase();

      if(this.type === "MAP-POINT" || this.type === "MAP-MULTIPOINT") options.fillOpacity = 1;

      if(options.wrappers.length > 0)
        options = Object.assign(this._convertWrappers(options.wrappers), options);
      L.setOptions(this, options);

      this.group = this.options.group;
      this.options.interactive = this.options.link || (this.options.properties && this.options.onEachFeature);

      this._parts = [];
      this._markup = markup;
      this.options.zoom = markup.getAttribute('zoom') || this.options.nativeZoom;

      this._convertMarkup();

      if(markup.querySelector('map-span') || markup.querySelector('map-a')){
        this._generateOutlinePoints();
      }

      this.isClosed = this._isClosed();
    },

    /**
     * Attaches link handler to the sub parts' paths
     * @param {SVGElement} elem - The element to add listeners to, either path or g elements
     * @param {Object} link - The link object that contains the url, type and target data
     * @param leafletLayer
     */
    attachLinkHandler: function (elem, link, leafletLayer) {
      let dragStart, container = document.createElement('div'), p = document.createElement('p'), hovered = false;
      container.classList.add('mapml-link-preview');
      container.appendChild(p);
      elem.classList.add('map-a');
      if (link.visited) elem.classList.add("map-a-visited");
      L.DomEvent.on(elem, 'mousedown', e => dragStart = {x:e.clientX, y:e.clientY}, this);
      L.DomEvent.on(elem, "mouseup", (e) => {
        if (e.button !== 0) return; // don't trigger when button isn't left click
        let onTop = true, nextLayer = this.options._leafletLayer._layerEl.nextElementSibling;
        while(nextLayer && onTop){
          if(nextLayer.tagName && nextLayer.tagName.toUpperCase() === "LAYER-")
            onTop = !(nextLayer.checked && nextLayer._layer.queryable);
          nextLayer = nextLayer.nextElementSibling;
        }
        if(onTop && dragStart) {
          L.DomEvent.stop(e);
          let dist = Math.sqrt(Math.pow(dragStart.x - e.clientX, 2) + Math.pow(dragStart.y - e.clientY, 2));
          if (dist <= 5){
            link.visited = true;
            elem.setAttribute("stroke", "#6c00a2");
            elem.classList.add("map-a-visited");
            M.handleLink(link, leafletLayer);
          }
        }
      }, this);
      L.DomEvent.on(elem, "keypress", (e) => {
        L.DomEvent.stop(e);
        if(e.keyCode === 13 || e.keyCode === 32) {
          link.visited = true;
          elem.setAttribute("stroke", "#6c00a2");
          elem.classList.add("map-a-visited");
          M.handleLink(link, leafletLayer);
        }
      }, this);
      L.DomEvent.on(elem, 'mouseenter keyup', (e) => {
        if(e.target !== e.currentTarget) return;
        hovered = true;
        let resolver = document.createElement('a'), mapWidth = this._map.getContainer().clientWidth;
        resolver.href = link.url;
        p.innerHTML = resolver.href;

        this._map.getContainer().appendChild(container);

        while(p.clientWidth > mapWidth/2){
          p.innerHTML = p.innerHTML.substring(0, p.innerHTML.length - 5) + "...";
        }
        setTimeout(()=>{
          if(hovered) p.innerHTML = resolver.href;
        }, 1000);
      }, this);
      L.DomEvent.on(elem, 'mouseout keydown mousedown', (e) => {
        if(e.target !== e.currentTarget || !container.parentElement) return;
        hovered = false;
        this._map.getContainer().removeChild(container);
      }, this);
      L.DomEvent.on(leafletLayer._map.getContainer(),'mouseout mouseenter click', (e) => { //adds a lot of event handlers
        if(!container.parentElement) return;
        hovered = false;
        this._map.getContainer().removeChild(container);
      }, this);
    },

    /**
     * Updates internal structure of the feature to the new map state, the structure can be found in this._parts
     * @param {L.Map} addedMap - The map that the feature is part of, can be left blank in the case of static features
     * @param {L.Point} tileOrigin - The tile origin for the feature, if blank then it takes the maps pixel origin in the function
     * @param {int} zoomingTo - The zoom the map is animating to, if left blank then it takes the map zoom, its provided because in templated tiles zoom is delayed
     * @private
     */
    _project: function (addedMap, tileOrigin = undefined, zoomingTo = undefined) {
      let map = addedMap || this._map, origin = tileOrigin || map.getPixelOrigin(), zoom = zoomingTo === undefined ? map.getZoom() : zoomingTo;
      for (let p of this._parts) {
        p.pixelRings = this._convertRing(p.rings, map, origin, zoom);
        for (let subP of p.subrings) {
          subP.pixelSubrings = this._convertRing([subP], map, origin, zoom);
        }
      }
      if (!this._outline) return;
      this.pixelOutline = [];
      for (let o of this._outline) {
        this.pixelOutline = this.pixelOutline.concat(this._convertRing(o, map, origin, zoom));
      }
    },

    /**
     * Converts the PCRS points to pixel points that can be used to create the SVG
     * @param {L.Point[][]} r - Is the rings of a feature, either the mainParts, subParts or outline
     * @param {L.Map} map - The map that the feature is part of
     * @param {L.Point} origin - The origin used to calculate the pixel points
     * @param {int} zoom - The current zoom level of the map
     * @returns {L.Point[][]}
     * @private
     */
    _convertRing: function (r, map, origin, zoom) {
      // TODO: Implement Ramer-Douglas-Peucer Algo for simplifying points
      let scale = map.options.crs.scale(zoom), parts = [];
      for (let sub of r) {
        let interm = [];
        for (let p of sub.points) {
          let conv = map.options.crs.transformation.transform(p, scale);
          interm.push(L.point(conv.x, conv.y)._subtract(origin).round());
        }
        parts.push(interm);
      }
      return parts;
    },

    /**
     * Updates the features
     * @private
     */
    _update: function () {
      if (!this._map) return;
      this._renderer._updateFeature(this);
    },

    /**
     * Converts the map-spans, a and divs around a geometry subtype into options for the feature
     * @param {HTMLElement[]} elems - The current zoom level of the map
     * @private
     */
    _convertWrappers: function (elems) {
      if(!elems || elems.length === 0) return;
      let classList = '', output = {};
      for(let elem of elems){
        if(elem.tagName.toUpperCase() !== "MAP-A" && elem.className){
          classList +=`${elem.className} `;
        } else if(!output.link && elem.getAttribute("href")) {
          let link = {};
          link.url = elem.getAttribute("href");
          if(elem.hasAttribute("target")) link.target = elem.getAttribute("target");
          if(elem.hasAttribute("type")) link.type = elem.getAttribute("type");
          if(elem.hasAttribute("inplace")) link.inPlace = true;
          output.link = link;
        }
      }
      output.className = `${classList} ${this.options.className}`.trim();
      return output;
    },

    /**
     * Converts this._markup to the internal structure of features
     * @private
     */
    _convertMarkup: function () {
      if (!this._markup) return;

      let attr = this._markup.attributes;
      this.featureAttributes = {};
      if(this.options.link && this._markup.parentElement.tagName.toUpperCase() === "MAP-A" && this._markup.parentElement.parentElement.tagName.toUpperCase() !== "MAP-GEOMETRY")
        this.featureAttributes.tabindex = "0";
      for(let i = 0; i < attr.length; i++){
        this.featureAttributes[attr[i].name] = attr[i].value;
      }

      let first = true;
      for (let c of this._markup.querySelectorAll('map-coordinates')) {              //loops through the coordinates of the child
        let ring = [], subRings = [];
        this._coordinateToArrays(c, ring, subRings, this.options.className);              //creates an array of pcrs points for the main ring and the subparts
        if (!first && this.type === "MAP-POLYGON") {
          this._parts[0].rings.push(ring[0]);
          if (subRings.length > 0)
            this._parts[0].subrings = this._parts[0].subrings.concat(subRings);
        } else if (this.type === "MAP-MULTIPOINT") {
          for (let point of ring[0].points.concat(subRings)) {
            this._parts.push({ rings: [{ points: [point] }], subrings: [], cls:`${point.cls || ""} ${this.options.className || ""}`.trim() });
          }
        } else {
          this._parts.push({ rings: ring, subrings: subRings, cls: `${this.featureAttributes.class || ""} ${this.options.className || ""}`.trim() });
        }
        first = false;
      }
    },

    /**
     * Generates the feature outline, subtracting the map-spans to generate those separately
     * @private
     */
    _generateOutlinePoints: function () {
      if (this.type === "MAP-MULTIPOINT" || this.type === "MAP-POINT" || this.type === "MAP-LINESTRING" || this.type === "MAP-MULTILINESTRING") return;

      this._outline = [];
      for (let coords of this._markup.querySelectorAll('map-coordinates')) {
        let nodes = coords.childNodes, cur = 0, tempDiv = document.createElement('div'), nodeLength = nodes.length;
        for(let i = 0; i < nodes.length; i++){
          if(nodes[i].textContent.trim().length === 0){
            nodes[i].remove();
          }
        }
        for (let n of nodes) {
          let line = [];
          if (!n.tagName) {  //no tagName means it's text content
            let c = '', ind = (((cur - 1)%nodes.length) + nodes.length) % nodes.length; // this equation turns Javascript's % to how it behaves in C for example
            if (nodes[ind].tagName) {
              let prev = nodes[ind].textContent.trim().split(/\s+/);
              c += `${prev[prev.length - 2]} ${prev[prev.length - 1]} `;
            }
            c += n.textContent;
            ind = (((cur + 1)%nodes.length) + nodes.length) % nodes.length; // this is equivalent to C/C++'s (cur + 1) % nodes.length
            if (nodes[ind].tagName) {
              let next = nodes[ind].textContent.trim().split(/\s+/);
              c += `${next[0]} ${next[1]} `;
            }
            tempDiv.innerHTML = c;
            this._coordinateToArrays(tempDiv, line, [], true, this.featureAttributes.class || this.options.className);
            this._outline.push(line);
          }
          cur++;
        }
      }
    },

    /**
     * Converts map-coordinates element to an object representing the parts and subParts
     * @param {HTMLElement} coords - A single map-coordinates element
     * @param {Object[]} main - An empty array representing the main parts
     * @param {Object[]} subParts - An empty array representing the sub parts
     * @param {boolean} isFirst - A true | false representing if the current HTML element is the parent map-coordinates element or not
     * @param {string} cls - The class of the coordinate/span
     * @param parents
     * @private
     */
    _coordinateToArrays: function (coords, main, subParts, isFirst = true, cls = undefined, parents = []) {
      for (let span of coords.children) {
        this._coordinateToArrays(span, main, subParts, false, span.getAttribute("class"), parents.concat([span]));
      }
      let noSpan = coords.textContent.replace(/(<([^>]+)>)/ig, ''),
          pairs = noSpan.match(/(\S+\s+\S+)/gim), local = [], bounds;
      for (let p of pairs) {
        let numPair = [];
        p.split(/\s+/gim).forEach(M.parseNumber, numPair);
        let point = M.pointToPCRSPoint(L.point(numPair), this.options.zoom, this.options.projection, this.options.nativeCS);
        local.push(point);
        bounds = bounds ? bounds.extend(point) : L.bounds(point, point);
      }
      if (this._bounds) {
        this._bounds.extend(bounds.min);
        this._bounds.extend(bounds.max);
      } else {
        this._bounds = bounds;
      }
      if (isFirst) {
        main.push({ points: local });
      } else {
        let attrMap = {}, attr = coords.attributes, wrapperAttr = this._convertWrappers(parents);
        if(wrapperAttr.link) attrMap.tabindex = "0";
        for(let i = 0; i < attr.length; i++){
          if(attr[i].name === "class") continue;
          attrMap[attr[i].name] = attr[i].value;
        }
        subParts.unshift({
          points: local,
          center: bounds.getCenter(),
          cls: `${cls || ""} ${wrapperAttr.className || ""}`.trim(),
          attr: attrMap,
          link: wrapperAttr.link,
          linkTarget: wrapperAttr.linkTarget,
          linkType: wrapperAttr.linkType});
      }
    },

    /**
     * Returns if the feature is closed or open, useful when styling
     * @returns {boolean}
     * @private
     */
    _isClosed: function () {
      switch (this.type) {
        case 'MAP-POLYGON':
        case 'MAP-MULTIPOLYGON':
        case 'MAP-POINT':
        case 'MAP-MULTIPOINT':
          return true;
        case 'MAP-LINESTRING':
        case 'MAP-MULTILINESTRING':
          return false;
        default:
          return false;
      }
    },

    /**
     * Returns the center of the entire feature
     * @returns {L.Point}
     */
    getCenter: function () {
      if (!this._bounds) return null;
      return this._map.options.crs.unproject(this._bounds.getCenter());
    },

    getPCRSCenter: function () {
      return this._bounds.getCenter();
    },
  });

  /**
   *
   * @param {HTMLElement} markup - The markup of the feature
   * @param {Object} options - Options of the feature
   * @returns {M.Feature}
   */
  var feature = function (markup, options) {
    return new Feature(markup, options);
  };

  /**
   * Returns a new Feature Renderer
   * @param {Object} options - Options for the renderer
   * @returns {*}
   */
  var FeatureRenderer = L.SVG.extend({
    /**
     * Override method of same name from L.SVG, use the this._container property
     * to set up the role="none presentation" on featureGroupu container,
     * per this recommendation: 
     * https://github.com/Maps4HTML/Web-Map-Custom-Element/pull/471#issuecomment-845192246
     * @private overrides ancestor method so that we have a _container to work with
     */
    _initContainer: function () {
      // call the method we're overriding, per https://leafletjs.com/examples/extending/extending-1-classes.html#methods-of-the-parent-class
      // note you have to pass 'this' as the first arg
      L.SVG.prototype._initContainer.call(this);
      // knowing that the previous method call creates the this._container, we
      // access it and set the role="none presetation" which suppresses the 
      // announcement of "Graphic" on each feature focus.
      this._container.setAttribute('role', 'none presentation');
    },
    
    /**
     * Creates all the appropriate path elements for a M.Feature
     * @param {M.Feature} layer - The M.Feature that needs paths generated
     * @param {boolean} stampLayer - Whether or not a layer should be stamped and stored in the renderer layers
     * @private
     */
    _initPath: function (layer, stampLayer = true) {

      if(layer._outline) {
        let outlinePath = L.SVG.create('path');
        if (layer.options.className) L.DomUtil.addClass(outlinePath, layer.featureAttributes.class || layer.options.className);
        L.DomUtil.addClass(outlinePath, 'mapml-feature-outline');
        outlinePath.style.fill = "none";
        layer.outlinePath = outlinePath;
      }

      //creates the main parts and sub parts paths
      for (let p of layer._parts) {
        if (p.rings){
          this._createPath(p, layer.options.className, layer.featureAttributes['aria-label'], layer.options.interactive, layer.featureAttributes);
          if(layer.outlinePath) p.path.style.stroke = "none";
        }
        if (p.subrings) {
          for (let r of p.subrings) {
            this._createPath(r, layer.options.className, r.attr['aria-label'], (r.link !== undefined), r.attr);
          }
        }
        this._updateStyle(layer);
      }
      if(stampLayer){
        let stamp = L.stamp(layer);
        this._layers[stamp] = layer;
      }
    },

    /**
     * Creates paths for either mainParts, subParts or outline of a feature
     * @param {Object} ring - The ring the current path is being generated for
     * @param {string} title - The accessible aria-label of a path
     * @param {string} cls - The class of the path
     * @param {boolean} interactive - The boolean representing whether a feature is interactive or not
     * @param {Object} attr - Attributes map
     * @private
     */
    _createPath: function (ring, cls, title, interactive = false, attr = undefined) {
      let p = L.SVG.create('path');
      ring.path = p;
      if(!attr) {
        if (title) p.setAttribute('aria-label', title);
      } else {
        for(let [name, value] of Object.entries(attr)){
          if(name === "id" || name === "tabindex") continue;
          p.setAttribute(name, value);
        }
      }
      if (ring.cls || cls) {
        L.DomUtil.addClass(p, ring.cls || cls);
      }
      if (interactive) {
        L.DomUtil.addClass(p, 'leaflet-interactive');
      }
    },

    /**
     * Adds all the paths needed for a feature
     * @param {M.Feature} layer - The feature that needs it's paths added
     * @param {HTMLElement} container - The location the paths need to be added to
     * @param {boolean} interactive - Whether a feature is interactive or not
     * @private
     */
    _addPath: function (layer, container = undefined, interactive = true) {
      if (!this._rootGroup && !container) { this._initContainer(); }
      let c = container || this._rootGroup, outlineAdded = false;
      if(interactive) {
        layer.addInteractiveTarget(layer.group);
      }
      for (let p of layer._parts) {
        if (p.path)
          layer.group.appendChild(p.path);
        if (interactive){
          if(layer.options.link) layer.attachLinkHandler(p.path, layer.options.link, layer.options._leafletLayer);
          layer.addInteractiveTarget(p.path);
        }

        if(!outlineAdded && layer.pixelOutline) {
          layer.group.appendChild(layer.outlinePath);
          outlineAdded = true;
        }

        for (let subP of p.subrings) {
          if (subP.path) {
            if (subP.link){
              layer.attachLinkHandler(subP.path, subP.link, layer.options._leafletLayer);
              layer.addInteractiveTarget(subP.path);
            }
            layer.group.appendChild(subP.path);
          }
        }
      }
      c.appendChild(layer.group);
    },

    /**
     * Removes all the paths related to a feature
     * @param {M.Feature} layer - The feature who's paths need to be removed
     * @private
     */
    _removePath: function (layer) {
      for (let p of layer._parts) {
        if (p.path) {
          layer.removeInteractiveTarget(p.path);
          L.DomUtil.remove(p.path);
        }
        for (let subP of p.subrings) {
          if (subP.path)
            L.DomUtil.remove(subP.path);
        }
      }
      if(layer.outlinePath) L.DomUtil.remove(layer.outlinePath);
      layer.removeInteractiveTarget(layer.group);
      L.DomUtil.remove(layer.group);
      delete this._layers[L.stamp(layer)];
    },

    /**
     * Updates the d attribute of all paths of a feature
     * @param {M.Feature} layer - The Feature that needs updating
     * @private
     */
    _updateFeature: function (layer) {
      if (layer.pixelOutline) this._setPath(layer.outlinePath, this.geometryToPath(layer.pixelOutline, false));
      for (let p of layer._parts) {
        this._setPath(p.path, this.geometryToPath(p.pixelRings, layer.isClosed));
        for (let subP of p.subrings) {
          this._setPath(subP.path, this.geometryToPath(subP.pixelSubrings, false));
        }
      }
    },

    /**
     * Generates the marker d attribute for a given point
     * @param {L.Point} p - The point of the marker
     * @returns {string}
     * @private
     */
    _pointToMarker: function (p) {
      return `M${p.x} ${p.y} L${p.x - 12.5} ${p.y - 30} C${p.x - 12.5} ${p.y - 50}, ${p.x + 12.5} ${p.y - 50}, ${p.x + 12.5} ${p.y - 30} L${p.x} ${p.y}z`;
    },

    /**
     * Updates the styles of all paths of a feature
     * @param {M.Feature} layer - The feature that needs styles updated
     * @private
     */
    _updateStyle: function (layer) {
      this._updatePathStyle(layer.outlinePath, layer, false, true);
      for (let p of layer._parts) {
        if (p.path) {
          this._updatePathStyle(p.path, layer, true);
        }
        for (let subP of p.subrings) {
          if (subP.path)
            this._updatePathStyle(subP.path, layer);
        }
      }
    },

    /**
     * Updates the style of a single path
     * @param {HTMLElement} path - The path that needs updating
     * @param {M.Feature} layer - The feature layer
     * @param {boolean} isMain - Whether it's the main parts or not
     * @param {boolean} isOutline - Whether a path is an outline or not
     * @private
     */
    _updatePathStyle: function (path, layer, isMain = false, isOutline = false) {
      if (!path || !layer) { return; }
      let options = layer.options, isClosed = layer.isClosed;
      if ((options.stroke && (!isClosed || isOutline)) || (isMain && !layer.outlinePath)) {
        path.setAttribute('stroke', options.color);
        path.setAttribute('stroke-opacity', options.opacity);
        path.setAttribute('stroke-width', options.weight);
        path.setAttribute('stroke-linecap', options.lineCap);
        path.setAttribute('stroke-linejoin', options.lineJoin);

        if (options.dashArray) {
          path.setAttribute('stroke-dasharray', options.dashArray);
        } else {
          path.removeAttribute('stroke-dasharray');
        }

        if (options.dashOffset) {
          path.setAttribute('stroke-dashoffset', options.dashOffset);
        } else {
          path.removeAttribute('stroke-dashoffset');
        }

        if (options.link){
          path.setAttribute("stroke", options.link.visited?"#6c00a2":"#0000EE");
          path.setAttribute("stroke-opacity", "1");
          path.setAttribute("stroke-width", "1px");
          path.setAttribute("stroke-dasharray", "none");
        }
      } else {
        path.setAttribute('stroke', 'none');
      }

      if(isClosed && !isOutline) {
        if (!options.fill) {
          path.setAttribute('fill', options.fillColor || options.color);
          path.setAttribute('fill-opacity', options.fillOpacity);
          path.setAttribute('fill-rule', options.fillRule || 'evenodd');
        } else {
          path.setAttribute('fill', options.color);
        }
      } else {
        path.setAttribute('fill', 'none');
      }
    },

    /**
     * Sets the d attribute of a path
     * @param {HTMLElement} path - The path that is being updated
     * @param {string} def - The new d attribute of the path
     * @private
     */
    _setPath: function (path, def) {
      path.setAttribute('d', def);
    },

    /**
     * Generates the d string of a feature part
     * @param {L.Point[]} rings - The points making up a given part of a feature
     * @param {boolean} closed - Whether a feature is closed or not
     * @returns {string}
     */
    geometryToPath: function (rings, closed) {
      let str = '', i, j, len, len2, points, p;

      for (i = 0, len = rings.length; i < len; i++) {
        points = rings[i];
        if (points.length === 1) {
          return this._pointToMarker(points[0]);
        }
        for (j = 0, len2 = points.length; j < len2; j++) {
          p = points[j];
          str += (j ? 'L' : 'M') + p.x + ' ' + p.y;
        }
        str += closed ? 'z' : '';
      }
      return str || 'M0 0';
    },
  });

  /**
   * Returns new M.FeatureRenderer
   * @param {Object} options - Options for the renderer
   * @returns {M.FeatureRenderer}
   */
  var featureRenderer = function (options) {
    return new FeatureRenderer(options);
  };

  var FeatureGroup = L.FeatureGroup.extend({

    /**
     * Initialize the feature group
     * @param {M.Feature[]} layers
     * @param {Object} options
     */
    initialize: function (layers, options) {
      if(options.wrappers && options.wrappers.length > 0)
        options = Object.assign(M.Feature.prototype._convertWrappers(options.wrappers), options);

      L.LayerGroup.prototype.initialize.call(this, layers, options);

      if((this.options.onEachFeature && this.options.properties) || this.options.link) {
        L.DomUtil.addClass(this.options.group, "leaflet-interactive");
        L.DomEvent.on(this.options.group, "keyup keydown mousedown", this._handleFocus, this);
        let firstLayer = layers[Object.keys(layers)[0]];
        if(layers.length === 1 && firstLayer.options.link) this.options.link = firstLayer.options.link;
        if(this.options.link){
          M.Feature.prototype.attachLinkHandler.call(this, this.options.group, this.options.link, this.options._leafletLayer);
          this.options.group.setAttribute('role', 'link');
        } else {
          this.options.group.setAttribute("aria-expanded", "false");
          this.options.group.setAttribute('role', 'button');
          this.options.onEachFeature(this.options.properties, this);
          this.off("click", this._openPopup);
        }
      }

      this.options.group.setAttribute('aria-label', this.options.accessibleTitle);
      if(this.options.featureID) this.options.group.setAttribute("data-fid", this.options.featureID);
    },

    onAdd: function (map) {
      L.LayerGroup.prototype.onAdd.call(this, map);
      this.updateInteraction();
    },

    updateInteraction: function () {
      let map = this._map || this.options._leafletLayer._map;
      if((this.options.onEachFeature && this.options.properties) || this.options.link)
        map.featureIndex.addToIndex(this, this.getPCRSCenter(), this.options.group);

      for (let layerID in this._layers) {
        let layer = this._layers[layerID];
        for(let part of layer._parts){
          if(layer.featureAttributes && layer.featureAttributes.tabindex)
            map.featureIndex.addToIndex(layer, layer.getPCRSCenter(), part.path);
          for(let subPart of part.subrings) {
            if(subPart.attr && subPart.attr.tabindex) map.featureIndex.addToIndex(layer, subPart.center, subPart.path);
          }
        }
      }
    },

    /**
     * Handler for focus events
     * @param {L.DOMEvent} e - Event that occurred
     * @private
     */
    _handleFocus: function(e) {
      if((e.keyCode === 9 || e.keyCode === 16) && e.type === "keydown"){
        let index = this._map.featureIndex.currentIndex;
        if(e.keyCode === 9 && e.shiftKey) {
          if(index === this._map.featureIndex.inBoundFeatures.length - 1)
            this._map.featureIndex.inBoundFeatures[index].path.setAttribute("tabindex", -1);
          if(index !== 0){
            L.DomEvent.stop(e);
            this._map.featureIndex.inBoundFeatures[index - 1].path.focus();
            this._map.featureIndex.currentIndex--;
          }
        } else if (e.keyCode === 9) {
          if(index !== this._map.featureIndex.inBoundFeatures.length - 1) {
            L.DomEvent.stop(e);
            this._map.featureIndex.inBoundFeatures[index + 1].path.focus();
            this._map.featureIndex.currentIndex++;
          } else {
            this._map.featureIndex.inBoundFeatures[0].path.setAttribute("tabindex", -1);
            this._map.featureIndex.inBoundFeatures[index].path.setAttribute("tabindex", 0);
          }
        }
      } else if (!([9, 16, 13, 27].includes(e.keyCode))){
        this._map.featureIndex.currentIndex = 0;
        this._map.featureIndex.inBoundFeatures[0].path.focus();
      }

      if(e.target.tagName.toUpperCase() !== "G") return;
      if((e.keyCode === 9 || e.keyCode === 16 || e.keyCode === 13) && e.type === "keyup") {
        this.openTooltip();
      } else if (e.keyCode === 13 || e.keyCode === 32){
        this.closeTooltip();
        if(!this.options.link && this.options.onEachFeature){
          L.DomEvent.stop(e);
          this.openPopup();
        }
      } else {
        this.closeTooltip();
      }
    },

    /**
     * Add a M.Feature to the M.FeatureGroup
     * @param layer
     */
    addLayer: function (layer) {
      if(!layer.options.link && layer.options.interactive) {
        this.options.onEachFeature(this.options.properties, layer);
      }
      L.FeatureGroup.prototype.addLayer.call(this, layer);
    },

    /**
     * Focuses the previous function in the sequence on previous button press
     * @param e
     * @private
     */
    _previousFeature: function(e){
      L.DomEvent.stop(e);
      this._map.featureIndex.currentIndex = Math.max(this._map.featureIndex.currentIndex - 1, 0);
      let prevFocus = this._map.featureIndex.inBoundFeatures[this._map.featureIndex.currentIndex];
      prevFocus.path.focus();
      this._map.closePopup();
    },

    /**
     * Focuses next feature in sequence
     * @param e
     * @private
     */
    _nextFeature: function(e){
      L.DomEvent.stop(e);
      this._map.featureIndex.currentIndex = Math.min(this._map.featureIndex.currentIndex + 1, this._map.featureIndex.inBoundFeatures.length - 1);
      let nextFocus = this._map.featureIndex.inBoundFeatures[this._map.featureIndex.currentIndex];
      nextFocus.path.focus();
      this._map.closePopup();
    },

    getPCRSCenter: function () {
      let bounds;
      for(let l in this._layers){
        let layer = this._layers[l];
        if (!bounds) {
          bounds = L.bounds(layer.getPCRSCenter(), layer.getPCRSCenter());
        } else {
          bounds.extend(layer.getPCRSCenter());
        }
      }
      return bounds.getCenter();
    },
  });

  /**
   * Returns new M.FeatureGroup
   * @param {M.Feature[]} layers - Layers belonging to feature group
   * @param {Object} options - Options for the feature group
   * @returns {M.FeatureGroup}
   */
  var featureGroup = function (layers, options) {
    return new FeatureGroup(layers, options);
  };

  var AnnounceMovement = L.Handler.extend({
      addHooks: function () {
          this._map.on({
              layeradd: this.totalBounds,
              layerremove: this.totalBounds,
          });

          this._map.options.mapEl.addEventListener('moveend', this.announceBounds);
          this._map.dragging._draggable.addEventListener('dragstart', this.dragged);
          this._map.options.mapEl.addEventListener('mapfocused', this.focusAnnouncement);
      },
      removeHooks: function () {
          this._map.off({
              layeradd: this.totalBounds,
              layerremove: this.totalBounds,
          });

          this._map.options.mapEl.removeEventListener('moveend', this.announceBounds);
          this._map.dragging._draggable.removeEventListener('dragstart', this.dragged);
          this._map.options.mapEl.removeEventListener('mapfocused', this.focusAnnouncement);
      },

       focusAnnouncement: function () {
           let mapEl = this;
           setTimeout(function (){
               let el = mapEl.querySelector(".mapml-web-map") ? mapEl.querySelector(".mapml-web-map").shadowRoot.querySelector(".leaflet-container") :
                   mapEl.shadowRoot.querySelector(".leaflet-container");

               let mapZoom = mapEl._map.getZoom();
               let location = M.gcrsToTileMatrix(mapEl);
               let standard = M.options.locale.amZoom + " " + mapZoom + " " + M.options.locale.amColumn + " " + location[0] + " " + M.options.locale.amRow + " " + location[1];

               if(mapZoom === mapEl._map._layersMaxZoom){
                   standard = M.options.locale.amMaxZoom + " " + standard;
               }
               else if(mapZoom === mapEl._map._layersMinZoom){
                   standard = M.options.locale.amMinZoom + " " + standard;
               }

               el.setAttribute("aria-roledescription", "region " + standard);
               setTimeout(function () {
                   el.removeAttribute("aria-roledescription");
               }, 2000);
           }, 0);
       },

      announceBounds: function () {
          if(this._traversalCall > 0){
              return;
          }
          let mapZoom = this._map.getZoom();
          let mapBounds = M.pixelToPCRSBounds(this._map.getPixelBounds(),mapZoom,this._map.options.projection);

          let visible = true;
          if(this._map.totalLayerBounds){
              visible = mapZoom <= this._map._layersMaxZoom && mapZoom >= this._map._layersMinZoom &&
                  this._map.totalLayerBounds.overlaps(mapBounds);
          }

          let output = this.querySelector(".mapml-web-map") ? this.querySelector(".mapml-web-map").shadowRoot.querySelector(".mapml-screen-reader-output") :
              this.shadowRoot.querySelector(".mapml-screen-reader-output");

          //GCRS to TileMatrix
          let location = M.gcrsToTileMatrix(this);
          let standard = M.options.locale.amZoom + " " + mapZoom + " " + M.options.locale.amColumn + " " + location[0] + " " + M.options.locale.amRow + " " + location[1];

          if(!visible){
              let outOfBoundsPos = this._history[this._historyIndex];
              let inBoundsPos = this._history[this._historyIndex - 1];
              this.back();
              this._history.pop();

              if(outOfBoundsPos.zoom !== inBoundsPos.zoom){
                  output.innerText = M.options.locale.amZoomedOut;
              }
              else if(this._map.dragging._draggable.wasDragged){
                  output.innerText = M.options.locale.amDraggedOut;
              }
              else if(outOfBoundsPos.x > inBoundsPos.x){
                  output.innerText = M.options.locale.amEastBound;
              }
              else if(outOfBoundsPos.x < inBoundsPos.x){
                  output.innerText = M.options.locale.amWestBound;
              }
              else if(outOfBoundsPos.y < inBoundsPos.y){
                  output.innerText = M.options.locale.amNorthBound;
              }
              else if(outOfBoundsPos.y > inBoundsPos.y){
                  output.innerText = M.options.locale.amSouthBound;
              }

          }
          else {
              let prevZoom = this._history[this._historyIndex - 1] ? this._history[this._historyIndex - 1].zoom : this._history[this._historyIndex].zoom;
              if(mapZoom === this._map._layersMaxZoom && mapZoom !== prevZoom){
                  output.innerText = M.options.locale.amMaxZoom + " " + standard;
              }
              else if(mapZoom === this._map._layersMinZoom && mapZoom !== prevZoom){
                  output.innerText = M.options.locale.amMinZoom + " " + standard;
              }
              else {
                  output.innerText = standard;
              }
          }
          this._map.dragging._draggable.wasDragged = false;
      },

      totalBounds: function () {
          let layers = Object.keys(this._layers);
          let bounds = L.bounds();

          layers.forEach(i => {
              if(this._layers[i].layerBounds){
                  if(!bounds){
                      let point = this._layers[i].layerBounds.getCenter();
                      bounds = L.bounds(point, point);
                  }
                  bounds.extend(this._layers[i].layerBounds.min);
                  bounds.extend(this._layers[i].layerBounds.max);
              }
          });

          this.totalLayerBounds = bounds;
      },

      dragged: function () {
          this.wasDragged = true;
      }

  });

  var FeatureIndex = L.Handler.extend({
    initialize: function (map) {
      L.Handler.prototype.initialize.call(this, map);
      this.inBoundFeatures = [];
      this.outBoundFeatures = [];
      this.currentIndex = 0;
      this._mapPCRSBounds = M.pixelToPCRSBounds(
        map.getPixelBounds(),
        map.getZoom(),
        map.options.projection);
    },

    addHooks: function () {
      this._map.on("mapkeyboardfocused", this._updateMapBounds, this);
      this._map.on('mapkeyboardfocused', this._sortIndex, this);
    },

    removeHooks: function () {
      this._map.off("mapkeyboardfocused", this._updateMapBounds);
      this._map.off('mapkeyboardfocused', this._sortIndex);
    },

    /**
     * Adds a svg element to the index of tabbable features, it also keeps track of the layer it's associated + center
     * @param layer - the layer object the feature is associated with
     * @param lc - the layer center
     * @param path - the svg element that needs to be focused, can be a path or g
     */
    addToIndex: function (layer, lc, path) {
      let mc = this._mapPCRSBounds.getCenter();
      let dist = Math.sqrt(Math.pow(lc.x - mc.x, 2) + Math.pow(lc.y - mc.y, 2));
      let index = this._mapPCRSBounds.contains(lc) ? this.inBoundFeatures : this.outBoundFeatures;

      let elem = {path: path, layer: layer, center: lc, dist: dist};
      path.setAttribute("tabindex", -1);

      index.push(elem);

      // TODO: this insertion loop has potential to be improved slightly
      for (let i = index.length - 1; i > 0 && index[i].dist < index[i-1].dist; i--) {
        let tmp = index[i];
        index[i] = index[i-1];
        index[i-1] = tmp;
      }

      if (this._mapPCRSBounds.contains(lc))
        this.inBoundFeatures = index;
      else
        this.outBoundFeatures = index;
    },

    /**
     * Removes features that are no longer on the map, also moves features to the respective array depending
     * on whether the feature is in the maps viewport or not
     */
    cleanIndex: function() {
      this.currentIndex = 0;
      this.inBoundFeatures = this.inBoundFeatures.filter((elem) => {
        let inbound = this._mapPCRSBounds.contains(elem.center);
        elem.path.setAttribute("tabindex", -1);
        if (elem.layer._map && !inbound) {
          this.outBoundFeatures.push(elem);
        }
        return elem.layer._map && inbound;
      });
      this.outBoundFeatures = this.outBoundFeatures.filter((elem) => {
        let inbound = this._mapPCRSBounds.contains(elem.center);
        elem.path.setAttribute("tabindex", -1);
        if (elem.layer._map && inbound) {
          this.inBoundFeatures.push(elem);
        }
        return elem.layer._map && !inbound;
      });
    },

    /**
     * Sorts the index of features in the map's viewport based on distance from center
     * @private
     */
    _sortIndex: function() {
      this.cleanIndex();
      if(this.inBoundFeatures.length === 0) return;

      let mc = this._mapPCRSBounds.getCenter();

      this.inBoundFeatures.sort(function(a, b) {
        let ac = a.center;
        let bc = b.center;
        a.dist = Math.sqrt(Math.pow(ac.x - mc.x, 2) + Math.pow(ac.y - mc.y, 2));
        b.dist = Math.sqrt(Math.pow(bc.x - mc.x, 2) + Math.pow(bc.y - mc.y, 2));
        return a.dist - b.dist;
      });

      this.inBoundFeatures[0].path.setAttribute("tabindex", 0);
    },

    /**
     * Event handler for 'mapfocused' event to update the map's bounds in terms of PCRS
     * @param e - the event object
     * @private
     */
    _updateMapBounds: function (e) {
      // TODO: map's PCRS bounds is used in other parts of the viewer, can be moved out to the map object directly
      this._mapPCRSBounds = M.pixelToPCRSBounds(
        this._map.getPixelBounds(),
        this._map.getZoom(),
        this._map.options.projection);
    },
  });

  var Options = {
    announceMovement: false,
    locale: {
      cmBack: "Back",
      cmForward: "Forward",
      cmReload: "Reload",
      cmToggleControls: "Toggle Controls",
      cmCopyCoords: "Copy Coordinates",
      cmToggleDebug: "Toggle Debug Mode",
      cmCopyMapML: "Copy MapML",
      cmViewSource: "View Map Source",
      cmCopyAll: "All",
      lmZoomToLayer: "Zoom To Layer",
      lmCopyExtent: "Copy Extent",
      lcOpacity: "Opacity",
      btnZoomIn: "Zoom in",
      btnZoomOut: "Zoom out",
      btnFullScreen: "View fullscreen",
      amZoom: "zoom level",
      amColumn: "column",
      amRow: "row",
      amMaxZoom: "At maximum zoom level, zoom in disabled",
      amMinZoom: "At minimum zoom level, zoom out disabled",
      amZoomedOut: "Zoomed out of bounds, returning to",
      amDraggedOut: "Dragged out of bounds, returning to",
      amEastBound: "Reached east bound, panning east disabled",
      amWestBound: "Reached west bound, panning west disabled",
      amNorthBound: "Reached north bound, panning north disabled",
      amSouthBound: "Reached south bound, panning south disabled"
    }
  };

  L.Map.Keyboard.include({
      _onKeyDown: function (e) {

          if (e.altKey || e.metaKey) { return; }

          let zoomIn = {
              187: 187,
              107: 107,
              61: 61,
              171: 171
          };

          let zoomOut = {
              189: 189,
              109: 109,
              54: 54,
              173: 173
          };

          var key = e.keyCode,
              map = this._map,
              offset;

          if (key in this._panKeys) {
              if (!map._panAnim || !map._panAnim._inProgress) {
                  offset = this._panKeys[key];
                  if (e.shiftKey) {
                      offset = L.point(offset).multiplyBy(3);
                  }
                  if (e.ctrlKey) {
                      offset = L.point(offset).divideBy(5);
                  }

                  map.panBy(offset);

                  if (map.options.maxBounds) {
                      map.panInsideBounds(map.options.maxBounds);
                  }
              }
          } else if (key in this._zoomKeys) {
              if((key in zoomIn && map._layersMaxZoom !== map.getZoom()) || (key in zoomOut && map._layersMinZoom !== map.getZoom()))
                  map.setZoom(map.getZoom() + (e.shiftKey ? 3 : 1) * this._zoomKeys[key]);

          } else if (key === 27 && map._popup && map._popup.options.closeOnEscapeKey) {
              map.closePopup();

          } else {
              return;
          }

          L.DomEvent.stop(e);
      }
  });

  /* 
   * Copyright 2015-2016 Canada Centre for Mapping and Earth Observation, 
   * Earth Sciences Sector, Natural Resources Canada.
   * 
   * License
   * 
   * By obtaining and/or copying this work, you (the licensee) agree that you have 
   * read, understood, and will comply with the following terms and conditions.
   * 
   * Permission to copy, modify, and distribute this work, with or without 
   * modification, for any purpose and without fee or royalty is hereby granted, 
   * provided that you include the following on ALL copies of the work or portions 
   * thereof, including modifications:
   * 
   * The full text of this NOTICE in a location viewable to users of the 
   * redistributed or derivative work.
   * 
   * Any pre-existing intellectual property disclaimers, notices, or terms and 
   * conditions. If none exist, the W3C Software and Document Short Notice should 
   * be included.
   * 
   * Notice of any changes or modifications, through a copyright statement on the 
   * new code or document such as "This software or document includes material 
   * copied from or derived from [title and URI of the W3C document]. 
   * Copyright © [YEAR] W3C® (MIT, ERCIM, Keio, Beihang)."
   * 
   * Disclaimers
   * 
   * THIS WORK IS PROVIDED "AS IS," AND COPYRIGHT HOLDERS MAKE NO REPRESENTATIONS 
   * OR WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO, WARRANTIES OF 
   * MERCHANTABILITY OR FITNESS FOR ANY PARTICULAR PURPOSE OR THAT THE USE OF THE 
   * SOFTWARE OR DOCUMENT WILL NOT INFRINGE ANY THIRD PARTY PATENTS, COPYRIGHTS, 
   * TRADEMARKS OR OTHER RIGHTS.
   * COPYRIGHT HOLDERS WILL NOT BE LIABLE FOR ANY DIRECT, INDIRECT, SPECIAL OR 
   * CONSEQUENTIAL DAMAGES ARISING OUT OF ANY USE OF THE SOFTWARE OR DOCUMENT.
   * 
   * The name and trademarks of copyright holders may NOT be used in advertising or 
   * publicity pertaining to the work without specific, written prior permission. 
   * Title to copyright in this work will at all times remain with copyright holders.
   */

  /* global L, Node */
  (function (window, document, undefined$1) {
    
  var M = {};
  window.M = M;

  (function () {
    M.detectImagePath = function (container) {
      // this relies on the CSS style leaflet-default-icon-path containing a 
      // relative url() that leads to a valid icon file.  Since that depends on
      // how all of this stuff is deployed (i.e. custom element or as leaflet-plugin)
      // also, because we're using 'shady DOM' api, the container must be 
      // a shady dom container, because the custom element tags it with added
      // style-scope ... and related classes.
     var el = L.DomUtil.create('div',  'leaflet-default-icon-path', container);
     var path = L.DomUtil.getStyle(el, 'background-image') ||
                L.DomUtil.getStyle(el, 'backgroundImage');	// IE8

     container.removeChild(el);

     if (path === null || path.indexOf('url') !== 0) {
      path = '';
     } else {
      path = path.replace(/^url\(["']?/, '').replace(/marker-icon\.png["']?\)$/, '');
     }

     return path;
    };
    M.mime = "text/mapml";

    let mapOptions = window.document.head.querySelector("map-options");
    M.options = Options;
    if (mapOptions) M.options = Object.assign(M.options, JSON.parse(mapOptions.innerHTML));

    // see https://leafletjs.com/reference-1.5.0.html#crs-l-crs-base
    // "new classes can't inherit from (L.CRS), and methods can't be added 
    // to (L.CRS.anything) with the include function
    // so we'll use the options property as a way to integrate needed 
    // properties and methods...
    M.WGS84 = new L.Proj.CRS('EPSG:4326','+proj=longlat +ellps=WGS84 +datum=WGS84 +no_defs ', {
      origin: [-180,+90],
      bounds: L.bounds([[-180,-90],[180,90]]),
      resolutions: [
        0.703125,
        0.3515625,
        0.17578125,
        0.087890625,
        0.0439453125,
        0.02197265625,
        0.010986328125,
        0.0054931640625,
        0.00274658203125,
        0.001373291015625,
        0.0006866455078125,
        0.0003433227539062,
        0.0001716613769531,
        0.0000858306884766,
        0.0000429153442383,
        0.0000214576721191,
        0.0000107288360596,
        0.0000053644180298,
        0.0000026822090149,
        0.0000013411045074,
        0.0000006705522537,
        0.0000003352761269
      ],
      crs: {
        tcrs: {
          horizontal: {
            name: "x",
            min: 0, 
            max: zoom => (M.WGS84.options.bounds.getSize().x / M.WGS84.options.resolutions[zoom]).toFixed()
          },
          vertical: {
            name: "y",
            min:0, 
            max: zoom => (M.WGS84.options.bounds.getSize().y / M.WGS84.options.resolutions[zoom]).toFixed()
          },
          bounds: zoom => L.bounds([M.WGS84.options.crs.tcrs.horizontal.min,
                            M.WGS84.options.crs.tcrs.vertical.min],
                           [M.WGS84.options.crs.tcrs.horizontal.max(zoom),
                            M.WGS84.options.crs.tcrs.vertical.max(zoom)])
        },
        pcrs: {
          horizontal: {
            name: "longitude",
            get min() {return M.WGS84.options.crs.gcrs.horizontal.min;},
            get max() {return M.WGS84.options.crs.gcrs.horizontal.max;}
          }, 
          vertical: {
            name: "latitude", 
            get min() {return M.WGS84.options.crs.gcrs.vertical.min;},
            get max() {return M.WGS84.options.crs.gcrs.vertical.max;}
          },
          get bounds() {return M.WGS84.options.bounds;}
        }, 
        gcrs: {
          horizontal: {
            name: "longitude",
            // set min/max axis values from EPSG registry area of use, retrieved 2019-07-25
            min: -180.0,
            max: 180.0
          }, 
          vertical: {
            name: "latitude",
            // set min/max axis values from EPSG registry area of use, retrieved 2019-07-25
            min: -90.0,
            max: 90.0
          },
          get bounds() {return L.latLngBounds(
                [M.WGS84.options.crs.gcrs.vertical.min,M.WGS84.options.crs.gcrs.horizontal.min],
                [M.WGS84.options.crs.gcrs.vertical.max,M.WGS84.options.crs.gcrs.horizontal.max]);}
        },
        map: {
          horizontal: {
            name: "i",
            min: 0,
            max: map => map.getSize().x
          },
          vertical: {
            name: "j",
            min: 0,
            max: map => map.getSize().y
          },
          bounds: map => L.bounds(L.point([0,0]),map.getSize())
        },
        tile: {
          horizontal: {
            name: "i",
            min: 0,
            max: 256
          },
          vertical: {
            name: "j",
            min: 0,
            max: 256
          },
          get bounds() {return L.bounds(
                    [M.WGS84.options.crs.tile.horizontal.min,M.WGS84.options.crs.tile.vertical.min],
                    [M.WGS84.options.crs.tile.horizontal.max,M.WGS84.options.crs.tile.vertical.max]);}
        },
        tilematrix: {
          horizontal: {
            name: "column",
            min: 0,
            max: zoom => (M.WGS84.options.crs.tcrs.horizontal.max(zoom) / M.WGS84.options.crs.tile.bounds.getSize().x).toFixed()
          },
          vertical: {
            name: "row",
            min: 0,
            max: zoom => (M.WGS84.options.crs.tcrs.vertical.max(zoom) / M.WGS84.options.crs.tile.bounds.getSize().y).toFixed()
          },
          bounds: zoom => L.bounds(
                   [M.WGS84.options.crs.tilematrix.horizontal.min,
                    M.WGS84.options.crs.tilematrix.vertical.min],
                   [M.WGS84.options.crs.tilematrix.horizontal.max(zoom),
                    M.WGS84.options.crs.tilematrix.vertical.max(zoom)])
        }
      }
    });
    M.CBMTILE = new L.Proj.CRS('EPSG:3978',
    '+proj=lcc +lat_1=49 +lat_2=77 +lat_0=49 +lon_0=-95 +x_0=0 +y_0=0 +ellps=GRS80 +datum=NAD83 +units=m +no_defs', {
      origin: [-34655800, 39310000],
      bounds: L.bounds([[-34655800,-39000000],[10000000,39310000]]),
      resolutions: [
        38364.660062653464, 
        22489.62831258996, 
        13229.193125052918, 
        7937.5158750317505, 
        4630.2175937685215, 
        2645.8386250105837,
        1587.5031750063501,
        926.0435187537042, 
        529.1677250021168, 
        317.50063500127004, 
        185.20870375074085, 
        111.12522225044451, 
        66.1459656252646, 
        38.36466006265346, 
        22.48962831258996,
        13.229193125052918,
        7.9375158750317505, 
        4.6302175937685215,
        2.6458386250105836,
        1.5875031750063502,
        0.92604351875370428,
        0.52916772500211673,
        0.31750063500127002,
        0.18520870375074083,
        0.11112522225044451,
        0.066145965625264591
      ],
      crs: {
        tcrs: {
          horizontal: {
            name: "x",
            min: 0, 
            max: zoom => (M.CBMTILE.options.bounds.getSize().x / M.CBMTILE.options.resolutions[zoom]).toFixed()
          },
          vertical: {
            name: "y",
            min:0, 
            max: zoom => (M.CBMTILE.options.bounds.getSize().y / M.CBMTILE.options.resolutions[zoom]).toFixed()
          },
          bounds: zoom => L.bounds([M.CBMTILE.options.crs.tcrs.horizontal.min,
                            M.CBMTILE.options.crs.tcrs.vertical.min],
                           [M.CBMTILE.options.crs.tcrs.horizontal.max(zoom),
                            M.CBMTILE.options.crs.tcrs.vertical.max(zoom)])
        },
        pcrs: {
          horizontal: {
            name: "easting",
            get min() {return M.CBMTILE.options.bounds.min.x;},
            get max() {return M.CBMTILE.options.bounds.max.x;}
          }, 
          vertical: {
            name: "northing", 
            get min() {return M.CBMTILE.options.bounds.min.y;},
            get max() {return M.CBMTILE.options.bounds.max.y;}
          },
          get bounds() {return M.CBMTILE.options.bounds;}
        }, 
        gcrs: {
          horizontal: {
            name: "longitude",
            // set min/max axis values from EPSG registry area of use, retrieved 2019-07-25
            min: -141.01,
            max: -47.74
          }, 
          vertical: {
            name: "latitude",
            // set min/max axis values from EPSG registry area of use, retrieved 2019-07-25
            min: 40.04,
            max: 86.46
          },
          get bounds() {return L.latLngBounds(
                [M.CBMTILE.options.crs.gcrs.vertical.min,M.CBMTILE.options.crs.gcrs.horizontal.min],
                [M.CBMTILE.options.crs.gcrs.vertical.max,M.CBMTILE.options.crs.gcrs.horizontal.max]);}
        },
        map: {
          horizontal: {
            name: "i",
            min: 0,
            max: map => map.getSize().x
          },
          vertical: {
            name: "j",
            min: 0,
            max: map => map.getSize().y
          },
          bounds: map => L.bounds(L.point([0,0]),map.getSize())
        },
        tile: {
          horizontal: {
            name: "i",
            min: 0,
            max: 256
          },
          vertical: {
            name: "j",
            min: 0,
            max: 256
          },
          get bounds() {return L.bounds(
                    [M.CBMTILE.options.crs.tile.horizontal.min,M.CBMTILE.options.crs.tile.vertical.min],
                    [M.CBMTILE.options.crs.tile.horizontal.max,M.CBMTILE.options.crs.tile.vertical.max]);}
        },
        tilematrix: {
          horizontal: {
            name: "column",
            min: 0,
            max: zoom => (M.CBMTILE.options.crs.tcrs.horizontal.max(zoom) / M.CBMTILE.options.crs.tile.bounds.getSize().x).toFixed()
          },
          vertical: {
            name: "row",
            min: 0,
            max: zoom => (M.CBMTILE.options.crs.tcrs.vertical.max(zoom) / M.CBMTILE.options.crs.tile.bounds.getSize().y).toFixed()
          },
          bounds: zoom => L.bounds([0,0],
                   [M.CBMTILE.options.crs.tilematrix.horizontal.max(zoom),
                    M.CBMTILE.options.crs.tilematrix.vertical.max(zoom)])
        }
      }
    });
    M.APSTILE = new L.Proj.CRS('EPSG:5936',
    '+proj=stere +lat_0=90 +lat_ts=50 +lon_0=-150 +k=0.994 +x_0=2000000 +y_0=2000000 +datum=WGS84 +units=m +no_defs', {
      origin: [-2.8567784109255E7, 3.2567784109255E7],
      bounds: L.bounds([[-28567784.109254867,-28567784.109254755],[32567784.109255023,32567784.10925506]]),
      resolutions: [
        238810.813354,
        119405.406677,
        59702.7033384999,
        29851.3516692501,
        14925.675834625,
        7462.83791731252,
        3731.41895865639,
        1865.70947932806,
        932.854739664032,
        466.427369832148,
        233.213684916074,
        116.606842458037,
        58.3034212288862,
        29.1517106145754,
        14.5758553072877,
        7.28792765351156,
        3.64396382688807,
        1.82198191331174,
        0.910990956788164,
        0.45549547826179
      ],
      crs: {
        tcrs: {
          horizontal: {
            name: "x",
            min: 0, 
            max: zoom => (M.APSTILE.options.bounds.getSize().x / M.APSTILE.options.resolutions[zoom]).toFixed()
          },
          vertical: {
            name: "y",
            min:0, 
            max: zoom => (M.APSTILE.options.bounds.getSize().y / M.APSTILE.options.resolutions[zoom]).toFixed()
          },
          bounds: zoom => L.bounds([M.APSTILE.options.crs.tcrs.horizontal.min,
                            M.APSTILE.options.crs.tcrs.vertical.min],
                           [M.APSTILE.options.crs.tcrs.horizontal.max(zoom),
                            M.APSTILE.options.crs.tcrs.vertical.max(zoom)])
        },
        pcrs: {
          horizontal: {
            name: "easting",
            get min() {return M.APSTILE.options.bounds.min.x;},
            get max() {return M.APSTILE.options.bounds.max.x;}
          }, 
          vertical: {
            name: "northing", 
            get min() {return M.APSTILE.options.bounds.min.y;},
            get max() {return M.APSTILE.options.bounds.max.y;}
          },
          get bounds() {return M.APSTILE.options.bounds;}
        }, 
        gcrs: {
          horizontal: {
            name: "longitude",
            // set min/max axis values from EPSG registry area of use, retrieved 2019-07-25
            min: -180.0,
            max: 180.0
          }, 
          vertical: {
            name: "latitude",
            // set min/max axis values from EPSG registry area of use, retrieved 2019-07-25
            min: 60.0,
            max: 90.0
          },
          get bounds() {return L.latLngBounds(
                    [M.APSTILE.options.crs.gcrs.vertical.min,M.APSTILE.options.crs.gcrs.horizontal.min],
                    [M.APSTILE.options.crs.gcrs.vertical.max,M.APSTILE.options.crs.gcrs.horizontal.max]);}
        },
        map: {
          horizontal: {
            name: "i",
            min: 0,
            max: map => map.getSize().x
          },
          vertical: {
            name: "j",
            min: 0,
            max: map => map.getSize().y
          },
          bounds: map => L.bounds(L.point([0,0]),map.getSize())
        },
        tile: {
          horizontal: {
            name: "i",
            min: 0,
            max: 256
          },
          vertical: {
            name: "j",
            min: 0,
            max: 256
          },
          get bounds() {return L.bounds(
                    [M.APSTILE.options.crs.tile.horizontal.min,M.APSTILE.options.crs.tile.vertical.min],
                    [M.APSTILE.options.crs.tile.horizontal.max,M.APSTILE.options.crs.tile.vertical.max]);}
        },
        tilematrix: {
          horizontal: {
            name: "column",
            min: 0,
            max: zoom => (M.APSTILE.options.crs.tcrs.horizontal.max(zoom) / M.APSTILE.options.crs.tile.bounds.getSize().x).toFixed()
          },
          vertical: {
            name: "row",
            min: 0,
            max: zoom => (M.APSTILE.options.crs.tcrs.vertical.max(zoom) / M.APSTILE.options.crs.tile.bounds.getSize().y).toFixed()
          },
          bounds: zoom => L.bounds([0,0],
                   [M.APSTILE.options.crs.tilematrix.horizontal.max(zoom),
                    M.APSTILE.options.crs.tilematrix.vertical.max(zoom)])
        }
      }
    });
    M.OSMTILE = L.CRS.EPSG3857;
    L.setOptions(M.OSMTILE, {
      origin: [-20037508.342787, 20037508.342787],
      bounds: L.bounds([[-20037508.342787, -20037508.342787],[20037508.342787, 20037508.342787]]),
      resolutions: [
        156543.0339,
        78271.51695,
        39135.758475,
        19567.8792375,
        9783.93961875,
        4891.969809375,
        2445.9849046875,
        1222.9924523438,
        611.49622617188,
        305.74811308594,
        152.87405654297,
        76.437028271484,
        38.218514135742,
        19.109257067871,
        9.5546285339355,
        4.7773142669678,
        2.3886571334839,
        1.1943285667419,
        0.59716428337097,
        0.29858214168549,
        0.14929107084274,
        0.074645535421371,
        0.03732276771068573,
        0.018661383855342865,
        0.009330691927671432495
      ],
      crs: {
        tcrs: {
          horizontal: {
            name: "x",
            min: 0, 
            max: zoom => (M.OSMTILE.options.bounds.getSize().x / M.OSMTILE.options.resolutions[zoom]).toFixed()
          },
          vertical: {
            name: "y",
            min:0, 
            max: zoom => (M.OSMTILE.options.bounds.getSize().y / M.OSMTILE.options.resolutions[zoom]).toFixed()
          },
          bounds: zoom => L.bounds([M.OSMTILE.options.crs.tcrs.horizontal.min,
                            M.OSMTILE.options.crs.tcrs.vertical.min],
                           [M.OSMTILE.options.crs.tcrs.horizontal.max(zoom),
                            M.OSMTILE.options.crs.tcrs.vertical.max(zoom)])
        },
        pcrs: {
          horizontal: {
            name: "easting",
            get min() {return M.OSMTILE.options.bounds.min.x;},
            get max() {return M.OSMTILE.options.bounds.max.x;}
          }, 
          vertical: {
            name: "northing", 
            get min() {return M.OSMTILE.options.bounds.min.y;},
            get max() {return M.OSMTILE.options.bounds.max.y;}
          },
          get bounds() {return M.OSMTILE.options.bounds;}
        }, 
        gcrs: {
          horizontal: {
            name: "longitude",
            get min() {return M.OSMTILE.unproject(M.OSMTILE.options.bounds.min).lng;},
            get max() {return M.OSMTILE.unproject(M.OSMTILE.options.bounds.max).lng;}
          }, 
          vertical: {
            name: "latitude",
            get min() {return M.OSMTILE.unproject(M.OSMTILE.options.bounds.min).lat;},
            get max() {return M.OSMTILE.unproject(M.OSMTILE.options.bounds.max).lat;}
          },
          get bounds() {return L.latLngBounds(
                [M.OSMTILE.options.crs.gcrs.vertical.min,M.OSMTILE.options.crs.gcrs.horizontal.min],
                [M.OSMTILE.options.crs.gcrs.vertical.max,M.OSMTILE.options.crs.gcrs.horizontal.max]);}
        },
        map: {
          horizontal: {
            name: "i",
            min: 0,
            max: map => map.getSize().x
          },
          vertical: {
            name: "j",
            min: 0,
            max: map => map.getSize().y
          },
          bounds: map => L.bounds(L.point([0,0]),map.getSize())
        },
        tile: {
          horizontal: {
            name: "i",
            min: 0,
            max: 256
          },
          vertical: {
            name: "j",
            min: 0,
            max: 256
          },
          get bounds() {return L.bounds(
                    [M.OSMTILE.options.crs.tile.horizontal.min,M.OSMTILE.options.crs.tile.vertical.min],
                    [M.OSMTILE.options.crs.tile.horizontal.max,M.OSMTILE.options.crs.tile.vertical.max]);}
        },
        tilematrix: {
          horizontal: {
            name: "column",
            min: 0,
            max: zoom => (M.OSMTILE.options.crs.tcrs.horizontal.max(zoom) / M.OSMTILE.options.crs.tile.bounds.getSize().x).toFixed()
          },
          vertical: {
            name: "row",
            min: 0,
            max: zoom => (M.OSMTILE.options.crs.tcrs.vertical.max(zoom) / M.OSMTILE.options.crs.tile.bounds.getSize().y).toFixed()
          },
          bounds: zoom => L.bounds([0,0],
                   [M.OSMTILE.options.crs.tilematrix.horizontal.max(zoom),
                    M.OSMTILE.options.crs.tilematrix.vertical.max(zoom)])
        }
      }
    });
  }());

  M.handleLink = Util.handleLink;
  M.convertPCRSBounds = Util.convertPCRSBounds;
  M.axisToXY = Util.axisToXY;
  M.csToAxes = Util.csToAxes;
  M.convertAndFormatPCRS = Util.convertAndFormatPCRS;
  M.axisToCS = Util.axisToCS;
  M.parseNumber = Util.parseNumber;
  M.extractInputBounds = Util.extractInputBounds;
  M.splitCoordinate = Util.splitCoordinate;
  M.boundsToPCRSBounds = Util.boundsToPCRSBounds;
  M.pixelToPCRSBounds = Util.pixelToPCRSBounds;
  M.metaContentToObject = Util.metaContentToObject;
  M.coordsToArray = Util.coordsToArray;
  M.parseStylesheetAsHTML = Util.parseStylesheetAsHTML;
  M.pointToPCRSPoint = Util.pointToPCRSPoint;
  M.pixelToPCRSPoint = Util.pixelToPCRSPoint;
  M.gcrsToTileMatrix = Util.gcrsToTileMatrix;

  M.QueryHandler = QueryHandler;
  M.ContextMenu = ContextMenu;
  M.AnnounceMovement = AnnounceMovement;
  M.FeatureIndex = FeatureIndex;

  // see https://leafletjs.com/examples/extending/extending-3-controls.html#handlers
  L.Map.addInitHook('addHandler', 'query', M.QueryHandler);
  L.Map.addInitHook('addHandler', 'contextMenu', M.ContextMenu);
  L.Map.addInitHook('addHandler', 'announceMovement', M.AnnounceMovement);
  L.Map.addInitHook('addHandler', 'featureIndex', M.FeatureIndex);

  M.MapMLLayer = MapMLLayer;
  M.mapMLLayer = mapMLLayer;

  M.ImageOverlay = ImageOverlay;
  M.imageOverlay = imageOverlay;

  M.TemplatedImageLayer = TemplatedImageLayer;
  M.templatedImageLayer = templatedImageLayer;

  M.TemplatedFeaturesLayer = TemplatedFeaturesLayer;
  M.templatedFeaturesLayer = templatedFeaturesLayer;

  M.TemplatedLayer = TemplatedLayer;
  M.templatedLayer = templatedLayer;

  M.TemplatedTileLayer = TemplatedTileLayer;
  M.templatedTileLayer = templatedTileLayer;

  M.MapMLFeatures = MapMLFeatures;
  M.mapMlFeatures = mapMlFeatures;

  M.MapMLLayerControl = MapMLLayerControl;
  M.mapMlLayerControl = mapMlLayerControl;

  M.ReloadButton = ReloadButton;
  M.reloadButton = reloadButton;

  M.FullscreenButton = FullscreenButton;
  M.fullscreenButton = fullscreenButton;

  M.MapMLStaticTileLayer = MapMLStaticTileLayer;
  M.mapMLStaticTileLayer = mapMLStaticTileLayer;

  M.DebugOverlay = DebugOverlay;
  M.debugOverlay = debugOverlay;

  M.Crosshair = Crosshair;
  M.crosshair = crosshair;

  M.Feature = Feature;
  M.feature = feature;

  M.FeatureRenderer = FeatureRenderer;
  M.featureRenderer = featureRenderer;

  M.FeatureGroup = FeatureGroup;
  M.featureGroup = featureGroup;

  }(window));

}());
