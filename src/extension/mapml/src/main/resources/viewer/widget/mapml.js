<<<<<<< HEAD
/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
=======
/*! @maps4html/web-map-custom-element 28-04-2023 */
/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
>>>>>>> 4af0ac91de ([GEOS-10940] Update MapML viewer to release 0.11.0)
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 * Copyright (c) 2023 Canada Centrre for Mapping and Earth Observation, Natural
 * Resources Canada
 * Copyright © 2023 World Wide Web Consortium, (Massachusetts Institute of Technology, 
 * European Research Consortium for Informatics and Mathematics, Keio    
 * University, Beihang). All Rights Reserved. This work is distributed under the 
 * W3C® Software License [1] in the hope that it will be useful, but WITHOUT ANY 
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR 
 * A PARTICULAR PURPOSE.
 * [1] http://www.w3.org/Consortium/Legal/copyright-software
 * 
 */
<<<<<<< HEAD
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
=======
!function(){"use strict";function t(t){return new o(t)}function e(t,e){return new i(t,e)}var o=L.GridLayer.extend({initialize:function(t){this.zoomBounds=this._getZoomBounds(t.tileContainer,t.maxZoomBound),L.extend(t,this.zoomBounds),L.setOptions(this,t),this._groups=this._groupTiles(this.options.tileContainer.getElementsByTagName("map-tile"))},onAdd:function(){this._bounds=this._getLayerBounds(this._groups,this._map.options.projection),this.layerBounds=this._bounds[Object.keys(this._bounds)[0]];for(var t of Object.keys(this._bounds))this.layerBounds.extend(this._bounds[t].min),this.layerBounds.extend(this._bounds[t].max);L.GridLayer.prototype.onAdd.call(this,this._map),this._handleMoveEnd()},getEvents:function(){let t=L.GridLayer.prototype.getEvents.call(this,this._map);return this._parentOnMoveEnd=t.moveend,t.moveend=this._handleMoveEnd,t.move=()=>{},t},_handleMoveEnd:function(t){var e=this._map.getZoom();let o=e;o=o>this.options.maxNativeZoom?this.options.maxNativeZoom:o,o=o<this.options.minNativeZoom?this.options.minNativeZoom:o,this.isVisible=e<=this.zoomBounds.maxZoom&&e>=this.zoomBounds.minZoom&&this._bounds[o]&&this._bounds[o].overlaps(M.pixelToPCRSBounds(this._map.getPixelBounds(),this._map.getZoom(),this._map.options.projection)),this.isVisible&&this._parentOnMoveEnd()},_isValidTile(t){return this._groups[this._tileCoordsToKey(t)]},createTile:function(t){let o=this._groups[this._tileCoordsToKey(t)]||[],i=document.createElement("map-tile"),n=this.getTileSize();i.setAttribute("col",t.x),i.setAttribute("row",t.y),i.setAttribute("zoom",t.z);for(let e=0;e<o.length;e++){let t=document.createElement("img");t.width=n.x,t.height=n.y,t.alt="",t.setAttribute("role","presentation"),t.src=o[e].src,i.appendChild(t)}return i},_getLayerBounds:function(t,e){let o={},i=M[e].options.crs.tile.bounds.max.x;for(var n in t){let t=n.split(":"),e={};e.x=+t[0]*i,e.y=+t[1]*i,e.z=+t[2],t[2]in o?(o[t[2]].extend(L.point(e.x,e.y)),o[t[2]].extend(L.point(e.x+i,e.y+i))):o[t[2]]=L.bounds(L.point(e.x,e.y),L.point(e.x+i,e.y+i))}for(var a in o){var r=+a;o[a]=M.pixelToPCRSBounds(o[a],r,e)}return o},_getZoomBounds:function(t,e){if(!t)return null;let o=M._metaContentToObject(t.getElementsByTagName("map-tiles")[0].getAttribute("zoom")),i={},n=t.getElementsByTagName("map-tile");i.nativeZoom=+o.value||0,i.maxNativeZoom=0,i.minNativeZoom=e;for(let e=0;e<n.length;e++){let t=+n[e].getAttribute("zoom");n[e].getAttribute("zoom")||(t=i.nativeZoom),i.minNativeZoom=Math.min(i.minNativeZoom,t),i.maxNativeZoom=Math.max(i.maxNativeZoom,t)}return i.minZoom=i.minNativeZoom-2<=0?0:i.minNativeZoom-2,i.maxZoom=e,o.min&&(i.minZoom=+o.min<i.minNativeZoom-2?i.minNativeZoom-2:+o.min),o.max&&(i.maxZoom=+o.max),i},_groupTiles:function(o){let i={};for(let e=0;e<o.length;e++){let t={};t.row=+o[e].getAttribute("row"),t.col=+o[e].getAttribute("col"),t.zoom=+o[e].getAttribute("zoom")||this.options.nativeZoom,t.src=o[e].getAttribute("src");var n=t.col+":"+t.row+":"+t.zoom;n in i?i[n].push(t):i[n]=[t]}return i}}),i=L.Control.Layers.extend({options:{autoZIndex:!1,sortLayers:!0,sortFunction:function(t,e){return t.options.zIndex<e.options.zIndex?-1:t.options.zIndex>e.options.zIndex?1:0}},initialize:function(t,e){for(var o in L.setOptions(this,e),this._layerControlInputs=[],this._layers=[],this._lastZIndex=0,this._handlingClick=!1,t)this._addLayer(t[o],o,!0)},onAdd:function(){return this._initLayout(),this._map.on("validate",this._validateInput,this),L.DomEvent.on(this.options.mapEl,"layerchange",this._validateInput,this),L.DomEvent.on(this._container.getElementsByTagName("a")[0],"keydown",this._focusFirstLayer,this._container),L.DomEvent.on(this._container,"contextmenu",this._preventDefaultContextMenu,this),this._update(),this._layers.length<1&&!this._map._showControls?this._container.setAttribute("hidden",""):this._map._showControls=!0,this._container},onRemove:function(t){t.off("validate",this._validateInput,this),L.DomEvent.off(this._container.getElementsByTagName("a")[0],"keydown",this._focusFirstLayer,this._container);for(var e=0;e<this._layers.length;e++)this._layers[e].layer.off("add remove",this._onLayerChange,this),this._layers[e].layer.off("extentload",this._validateInput,this)},addOrUpdateOverlay:function(t,e){for(var o=!1,i=0;i<this._layers.length;i++)if(this._layers[i].layer===t){o=!0,this._layers[i].name=e;break}return o||this.addOverlay(t,e),0<this._layers.length&&(this._container.removeAttribute("hidden"),this._map._showControls=!0),this._map?this._update():this},removeLayer:function(t){L.Control.Layers.prototype.removeLayer.call(this,t),0===this._layers.length&&this._container.setAttribute("hidden","")},_validateInput:function(t){for(let i=0;i<this._layers.length;i++)if(this._layers[i].input.labels[0]){let t=this._layers[i].input.labels[0].getElementsByTagName("span"),e=this._layers[i].input.labels[0].getElementsByTagName("input");if(e[0].checked=this._layers[i].layer._layerEl.checked,this._layers[i].layer._layerEl.disabled&&this._layers[i].layer._layerEl.checked?(e[0].closest("fieldset").disabled=!0,t[0].style.fontStyle="italic"):(e[0].closest("fieldset").disabled=!1,t[0].style.fontStyle="normal"),this._layers[i].layer._extent&&this._layers[i].layer._extent._mapExtents)for(let o=0;o<this._layers[i].layer._extent._mapExtents.length;o++){let t=this._layers[i].layer._extent._mapExtents[o].extentAnatomy,e=t.getElementsByClassName("mapml-layer-item-name")[0];this._layers[i].layer._extent._mapExtents[o].disabled&&this._layers[i].layer._extent._mapExtents[o].checked?(e.style.fontStyle="italic",t.disabled=!0):(e.style.fontStyle="normal",t.disabled=!1)}}},_focusFirstLayer:function(t){var e;"Enter"!==t.key||"leaflet-control-layers leaflet-control leaflet-control-layers-expanded"!==this.className||(e=this.children[1].children[2].children[0].children[0].children[0].children[0])&&setTimeout(()=>e.focus(),0)},_withinZoomBounds:function(t,e){return e.min<=t&&t<=e.max},_addItem:function(t){var e=t.layer.getLayerUserControlsHTML();return t.input=e.querySelector("input.leaflet-control-layers-selector"),this._layerControlInputs.push(t.input),t.input.layerId=L.stamp(t.layer),L.DomEvent.on(t.input,"click",this._onInputClick,this),t.layer.on("extentload",this._validateInput,this),this._overlaysList.appendChild(e),e},collapse:function(t){return"SELECT"===t.target.tagName||t.relatedTarget&&t.relatedTarget.parentElement&&("mapml-contextmenu mapml-layer-menu"===t.relatedTarget.className||"mapml-contextmenu mapml-layer-menu"===t.relatedTarget.parentElement.className)||this._map&&"block"===this._map.contextMenu._layerMenu.style.display||(L.DomUtil.removeClass(this._container,"leaflet-control-layers-expanded"),"touch"===t.originalEvent?.pointerType&&(this._container._isExpanded=!1)),this},_preventDefaultContextMenu:function(t){var e=this._map.mouseEventToLatLng(t),o=this._map.mouseEventToContainerPoint(t);t.preventDefault(),this._container._isExpanded||"touch"!==t.pointerType?this._map.fire("contextmenu",{originalEvent:t,containerPoint:o,latlng:e}):this._container._isExpanded=!0}});const I="OSMTILE",d="TILEMATRIX",k="mapmltemplatedtileplaceholder";function n(t,e){return new p(t,e)}function a(t,e){return new h(t,e)}function r(t,e){return new _(t,e)}function s(t,e){return new y(t,e)}function l(t,e){return new f(t,e)}function m(t,e,o,i,n,a){return new g(t,e,o,i,n,a)}function u(t,e,o){return t||e?new x(t,e,o):null}function c(){return new b}var p=L.FeatureGroup.extend({initialize:function(t,e){var o;L.setOptions(this,e),this.options.static&&(this._container=L.DomUtil.create("div","leaflet-layer",this.options.pane),L.DomUtil.addClass(this._container,"leaflet-pane mapml-vector-container"),L.setOptions(this.options.renderer,{pane:this._container})),this._layers={},this.options.query&&(this._mapmlFeatures=t.features||t,this.isVisible=!0,o=this._getNativeVariables(t),this.options.nativeZoom=o.zoom,this.options.nativeCS=o.cs),t&&!this.options.query&&(o=this._getNativeVariables(t),!t.querySelector("map-extent")&&t.querySelector("map-feature")&&this.options.static&&(this._features={},this._staticFeature=!0,this.isVisible=!0,this.zoomBounds=this._getZoomBounds(t,o.zoom),this.layerBounds=this._getLayerBounds(t),L.extend(this.options,this.zoomBounds)),this.addData(t,o.cs,o.zoom),this._staticFeature&&(this._resetFeatures(),this.options._leafletLayer._map._addZoomLimit(this)))},onAdd:function(t){L.FeatureGroup.prototype.onAdd.call(this,t),this._mapmlFeatures&&t.on("featurepagination",this.showPaginationFeature,this)},onRemove:function(t){this._mapmlFeatures&&(t.off("featurepagination",this.showPaginationFeature,this),delete this._mapmlFeatures,L.DomUtil.remove(this._container)),L.FeatureGroup.prototype.onRemove.call(this,t),this._map.featureIndex.cleanIndex()},getEvents:function(){return this._staticFeature?{moveend:this._handleMoveEnd,zoomend:this._handleZoomEnd}:{}},showPaginationFeature:function(e){if(this.options.query&&this._mapmlFeatures[e.i]){let t=this._mapmlFeatures[e.i];t._extentEl.shadowRoot.firstChild?.remove(),this.clearLayers(),t._featureGroup=this.addData(t,this.options.nativeCS,this.options.nativeZoom),t._extentEl.shadowRoot.appendChild(t),e.popup._navigationBar.querySelector("p").innerText=e.i+1+"/"+this.options._leafletLayer._totalFeatureCount,e.popup._content.querySelector("iframe").setAttribute("sandbox","allow-same-origin allow-forms"),e.popup._content.querySelector("iframe").srcdoc=t.querySelector("map-properties").innerHTML,this._map.fire("attachZoomLink",{i:e.i,currFeature:t}),this._map.once("popupclose",function(t){this.shadowRoot.innerHTML=""},t._extentEl)}},_getNativeVariables:function(t){return{zoom:t.querySelector&&t.querySelector("map-meta[name=zoom]")&&+M._metaContentToObject(t.querySelector("map-meta[name=zoom]").getAttribute("content")).value||0,cs:t.querySelector&&t.querySelector("map-meta[name=cs]")&&M._metaContentToObject(t.querySelector("map-meta[name=cs]").getAttribute("content")).content||"PCRS"}},_handleMoveEnd:function(){var t=this._map.getZoom(),e=t<=this.zoomBounds.maxZoom&&t>=this.zoomBounds.minZoom;this.isVisible=e&&this._layers&&this.layerBounds&&this.layerBounds.overlaps(M.pixelToPCRSBounds(this._map.getPixelBounds(),t,this._map.options.projection)),this._removeCSS()},_handleZoomEnd:function(t){var e=this._map.getZoom();e>this.zoomBounds.maxZoom||e<this.zoomBounds.minZoom?this.clearLayers():this._resetFeatures()},_getLayerBounds:function(t){if(!t)return null;let o=d,i=t.querySelector("map-meta[name=projection]")&&M._metaContentToObject(t.querySelector("map-meta[name=projection]").getAttribute("content")).content.toUpperCase()||I;try{var n=t.querySelector("map-meta[name=extent]")&&M._metaContentToObject(t.querySelector("map-meta[name=extent]").getAttribute("content")),a=n.zoom||0;let e=Object.keys(n);for(let t=0;t<e.length;t++)if(!e[t].includes("zoom")){o=M.axisToCS(e[t].split("-")[2]);break}var r=M.csToAxes(o);return M.boundsToPCRSBounds(L.bounds(L.point(+n["top-left-"+r[0]],+n["top-left-"+r[1]]),L.point(+n["bottom-right-"+r[0]],+n["bottom-right-"+r[1]])),a,i,o)}catch(t){return M.boundsToPCRSBounds(M[i].options.crs.tilematrix.bounds(0),0,i,o)}},_resetFeatures:function(){this.clearLayers(),this._map&&this._map.featureIndex.cleanIndex();let i=this._map||this.options._leafletLayer._map;if(this._features)for(var n in this._features)for(let o=0;o<this._features[n].length;o++){let t=this._features[n][o],e=t._checkRender(i.getZoom(),this.zoomBounds.minZoom,this.zoomBounds.maxZoom);e&&this.addLayer(t)}},_setZoomTransform:function(t,e){var o=this._map.getZoomScale(this._map.getZoom(),e),t=t.multiplyBy(o).subtract(this._map._getNewPixelOrigin(t,this._map.getZoom())).round();any3d?L.setTransform(this._layers[e],t,o):L.setPosition(this._layers[e],t)},_getZoomBounds:function(t,o){if(!t)return null;let i=100,n=0,a=t.querySelectorAll("map-feature"),e,r;for(let e=0;e<a.length;e++){let t=+a[e].getAttribute("zoom");a[e].getAttribute("zoom")||(t=o),n=Math.max(n,t),i=Math.min(i,t)}try{r=M._metaContentToObject(t.querySelector("map-meta[name=projection]").getAttribute("content")).content,e=M._metaContentToObject(t.querySelector("map-meta[name=zoom]").getAttribute("content"))}catch(t){return{minZoom:0,maxZoom:M[r||I].options.resolutions.length-1,minNativeZoom:i,maxNativeZoom:n}}return{minZoom:+e.min,maxZoom:+e.max,minNativeZoom:i,maxNativeZoom:n}},addData:function(i,n,a){var t,e,o,r=i.nodeType===Node.DOCUMENT_NODE||"LAYER-"===i.nodeName?i.getElementsByTagName("map-feature"):null;if((i.nodeType===Node.DOCUMENT_NODE?i.querySelector("map-link[rel=stylesheet],map-style"):null)&&(s=i.querySelector("map-base")&&i.querySelector("map-base").hasAttribute("href")?new URL(i.querySelector("map-base").getAttribute("href")).href:i.URL,M._parseStylesheetAsHTML(i,s,this._container)),r){for(t=0,e=r.length;t<e;t++)(o=r[t]).getElementsByTagName("map-geometry").length&&o.getElementsByTagName("map-coordinates").length&&(i.nodeType===Node.DOCUMENT_NODE?(o._DOMnode||(o._DOMnode=o.cloneNode(!0)),o._DOMnode._featureGroup=this.addData(o._DOMnode,n,a)):o._featureGroup=this.addData(o,n,a));return this}var s=this.options;if(!s.filter||s.filter(i)){i.classList.length&&(s.className=i.classList.value);let t=i.getAttribute("zoom")||a,e=i.querySelector("map-featurecaption");e=e?e.innerHTML:"Feature",i.querySelector("map-properties")&&(s.properties=document.createElement("div"),s.properties.classList.add("mapml-popup-content"),s.properties.insertAdjacentHTML("afterbegin",i.querySelector("map-properties").innerHTML));let o=this.geometryToLayer(i,s,n,+t,e);if(o)return!o.options.color&&i.hasAttribute("class")&&(o.options.className=i.getAttribute("class")),o.defaultOptions=o.options,this.resetStyle(o),s.onEachFeature&&o.bindTooltip(e,{interactive:!0,sticky:!0}),this._staticFeature?(s=i.getAttribute("zoom")||a)in this._features?this._features[s].push(o):this._features[s]=[o]:this.addLayer(o),"MAP-FEATURE"===i.tagName.toUpperCase()&&(i._groupEl=o.options.group),o}},resetStyle:function(t){var e=this.options.style;e&&(L.Util.extend(t.options,t.defaultOptions),this._setLayerStyle(t,e))},setStyle:function(e){this.eachLayer(function(t){this._setLayerStyle(t,e)},this)},_setLayerStyle:function(t,e){"function"==typeof e&&(e=e(t.feature)),t.setStyle&&t.setStyle(e)},_removeCSS:function(){var e=this._container.querySelectorAll("link[rel=stylesheet],style");for(let t=0;t<e.length;t++)this._container.removeChild(e[t])},geometryToLayer:function(o,i,t,n,a){let r="MAP-FEATURE"===o.tagName.toUpperCase()?o.getElementsByTagName("map-geometry")[0]:o,s=r?.getAttribute("cs")||t,l=[],m=L.SVG.create("g"),u=Object.assign({},i);if(r){for(var c of r.querySelectorAll("map-polygon, map-linestring, map-multilinestring, map-point, map-multipoint"))l.push(M.feature(c,Object.assign(u,{nativeCS:s,nativeZoom:n,projection:this.options.projection,featureID:o.id,group:m,wrappers:this._getGeometryParents(c.parentElement),featureLayer:this,_leafletLayer:this.options._leafletLayer})));let t={group:m,mapmlFeature:o,featureID:o.id,accessibleTitle:a,onEachFeature:i.onEachFeature,properties:i.properties,_leafletLayer:this.options._leafletLayer},e=r.querySelector("map-multipolygon")||r.querySelector("map-geometrycollection");return e&&(t.wrappers=this._getGeometryParents(e.parentElement)),M.featureGroup(l,t)}},_getGeometryParents:function(t,e=[]){return t&&"MAP-GEOMETRY"!==t.tagName.toUpperCase()?"MAP-MULTIPOLYGON"===t.tagName.toUpperCase()||"MAP-GEOMETRYCOLLECTION"===t.tagName.toUpperCase()?this._getGeometryParents(t.parentElement,e):this._getGeometryParents(t.parentElement,e.concat([t])):e}}),h=L.TileLayer.extend({initialize:function(t,e){var o=M._extractInputBounds(t);this.zoomBounds=o.zoomBounds,this.extentBounds=o.bounds,this.isVisible=!0,L.extend(e,this.zoomBounds),e.tms=t.tms,delete e.opacity,L.setOptions(this,e),this._setUpTileTemplateVars(t),this._template=t,this._initContainer(),L.TileLayer.prototype.initialize.call(this,t.template,L.extend(e,{pane:this._container}))},onAdd:function(){this._map._addZoomLimit(this),L.TileLayer.prototype.onAdd.call(this,this._map),this._handleMoveEnd()},getEvents:function(){let t=L.TileLayer.prototype.getEvents.call(this,this._map);return this._parentOnMoveEnd=t.moveend,t.moveend=this._handleMoveEnd,t},_initContainer:function(){this._container||(this._container=L.DomUtil.create("div","leaflet-layer",this.options.pane),L.DomUtil.addClass(this._container,"mapml-templated-tile-container"),this._updateZIndex())},_handleMoveEnd:function(t){var e=this._map.getZoom(),o=M.pixelToPCRSBounds(this._map.getPixelBounds(),e,this._map.options.projection);this.isVisible=e<=this.options.maxZoom&&e>=this.options.minZoom&&this.extentBounds.overlaps(o),this.isVisible&&this._parentOnMoveEnd()},createTile:function(e){let o=document.createElement("DIV"),i=this.getTileSize();if(L.DomUtil.addClass(o,"mapml-tile-group"),L.DomUtil.addClass(o,"leaflet-tile"),this._template.linkEl.dispatchEvent(new CustomEvent("tileloadstart",{detail:{x:e.x,y:e.y,zoom:e.z,appendTile:t=>{o.appendChild(t)}}})),this._template.type.startsWith("image/")){let t=L.TileLayer.prototype.createTile.call(this,e,function(){});t.width=i.x,t.height=i.y,o.appendChild(t)}else this._url.includes(k)||this._fetchTile(e,o);return o},_mapmlTileReady:function(t){L.DomUtil.addClass(t,"leaflet-tile-loaded")},getPane:function(){return this.options.pane},_fetchTile:function(e,o){fetch(this.getTileUrl(e),{redirect:"follow"}).then(function(t){return 200<=t.status&&t.status<300?Promise.resolve(t):(console.log("Looks like there was a problem. Status Code: "+t.status),Promise.reject(t))}).then(function(t){return t.text()}).then(t=>{return(new DOMParser).parseFromString(t,"application/xml")}).then(t=>{this._createFeatures(t,e,o),this._mapmlTileReady(o)}).catch(t=>{console.log("Error Creating Tile")})},_createFeatures:function(t,e,o){var i;t.querySelector("map-link[rel=stylesheet],map-style")&&(i=t.querySelector("map-base")&&t.querySelector("map-base").hasAttribute("href")?new URL(t.querySelector("map-base").getAttribute("href")).href:t.URL,M._parseStylesheetAsHTML(t,i,o));let n=L.SVG.create("svg"),a=L.SVG.create("g"),r=this._map.options.crs.options.crs.tile.bounds.max.x,s=e.x*r,l=e.y*r;var m,u=M.featureLayer(t,{projection:this._map.options.projection,static:!1,interactive:!1});for(m in u._layers)for(var c in u._layers[m]._layers){let t=u._layers[m]._layers[c];M.FeatureRenderer.prototype._initPath(t,!1),t._project(this._map,L.point([s,l]),e.z),M.FeatureRenderer.prototype._addPath(t,a,!1),M.FeatureRenderer.prototype._updateFeature(t)}n.setAttribute("width",r.toString()),n.setAttribute("height",r.toString()),n.appendChild(a),o.appendChild(n)},getTileUrl:function(t){if(t.z>=this._template.tilematrix.bounds.length||!this._template.tilematrix.bounds[t.z].contains(t))return"";var e,o={},i=this._template.linkEl,n=i.zoomInput;for(e in o[this._template.tilematrix.col.name]=t.x,o[this._template.tilematrix.row.name]=t.y,n&&i.hasAttribute("tref")&&i.getAttribute("tref").includes(`{${n.getAttribute("name")}}`)&&(o[this._template.zoom.name]=this._getZoomForUrl()),o[this._template.pcrs.easting.left]=this._tileMatrixToPCRSPosition(t,"top-left").x,o[this._template.pcrs.easting.right]=this._tileMatrixToPCRSPosition(t,"top-right").x,o[this._template.pcrs.northing.top]=this._tileMatrixToPCRSPosition(t,"top-left").y,o[this._template.pcrs.northing.bottom]=this._tileMatrixToPCRSPosition(t,"bottom-left").y,this._template.tile)["row","col","zoom","left","right","top","bottom"].indexOf(e)<0&&(o[e]=this._template.tile[e]);return this._map&&!this._map.options.crs.infinite&&(t=this._globalTileRange.max.y-t.y,this.options.tms&&(o[this._template.tilematrix.row.name]=t)),o.r=this.options.detectRetina&&L.Browser.retina&&0<this.options.maxZoom?"@2x":"",L.Util.template(this._url,o)},_tileMatrixToPCRSPosition:function(t,e){var o=this._map.options.crs,i=this.getTileSize(),n=t.scaleBy(i),a=n.add(i),i=n.add(Math.floor(i/2)),r=o.transformation.untransform(n,o.scale(t.z)),s=o.transformation.untransform(a,o.scale(t.z)),l=o.transformation.untransform(i,o.scale(t.z)),m=null;switch(e){case"top-left":m=r;break;case"bottom-left":m=new L.Point(r.x,s.y);break;case"center-left":m=new L.Point(r.x,l.y);break;case"top-right":m=new L.Point(s.x,r.y);break;case"bottom-right":m=s;break;case"center-right":m=new L.Point(s.x,l.y);break;case"top-center":m=new L.Point(l.x,r.y);break;case"bottom-center":m=new L.Point(l.x,s.y);break;case"center":m=l}return m},_setUpTileTemplateVars:function(t){t.tile={};for(var e,o,i,n,a,r=t.values,s=this.options.crs.options,l=0;l<t.values.length;l++){var m=r[l].getAttribute("type"),u=r[l].getAttribute("units"),c=r[l].getAttribute("axis"),p=r[l].getAttribute("name"),h=r[l].getAttribute("position"),d="map-select"===r[l].tagName.toLowerCase(),_=r[l].getAttribute("value"),y=r[l].getAttribute("min"),f=r[l].getAttribute("max");if("location"===m&&"tilematrix"===u)switch(c){case"column":a={name:p,min:s.crs.tilematrix.horizontal.min,max:s.crs.tilematrix.horizontal.max(s.resolutions.length-1)},isNaN(Number.parseFloat(y))||(a.min=Number.parseFloat(y)),isNaN(Number.parseFloat(f))||(a.max=Number.parseFloat(f));break;case"row":n={name:p,min:s.crs.tilematrix.vertical.min,max:s.crs.tilematrix.vertical.max(s.resolutions.length-1)},isNaN(Number.parseFloat(y))||(n.min=Number.parseFloat(y)),isNaN(Number.parseFloat(f))||(n.max=Number.parseFloat(f));break;case"longitude":case"easting":o=o||{min:s.crs.pcrs.horizontal.min,max:s.crs.pcrs.horizontal.max},isNaN(Number.parseFloat(y))||(o.min=Number.parseFloat(y)),isNaN(Number.parseFloat(f))||(o.max=Number.parseFloat(f)),h&&(h.match(/.*?-left/i)?o.left=p:h.match(/.*?-right/i)&&(o.right=p));break;case"latitude":case"northing":i=i||{min:s.crs.pcrs.vertical.min,max:s.crs.pcrs.vertical.max},isNaN(Number.parseFloat(y))||(i.min=Number.parseFloat(y)),isNaN(Number.parseFloat(f))||(i.max=Number.parseFloat(f)),h&&(h.match(/top-.*?/i)?i.top=p:h.match(/bottom-.*?/i)&&(i.bottom=p))}else if(m&&"zoom"===m.toLowerCase())e={name:p,min:0,max:s.resolutions.length,value:s.resolutions.length},!isNaN(Number.parseInt(_,10))&&Number.parseInt(_,10)>=e.min&&Number.parseInt(_,10)<=e.max?e.value=Number.parseInt(_,10):e.value=e.max,!isNaN(Number.parseInt(y,10))&&Number.parseInt(y,10)>=e.min&&Number.parseInt(y,10)<=e.max&&(e.min=Number.parseInt(y,10)),!isNaN(Number.parseInt(f,10))&&Number.parseInt(f,10)>=e.min&&Number.parseInt(f,10)<=e.max&&(e.max=Number.parseInt(f,10)),t.zoom=e;else if(d){const S=r[l].htmlselect;t.tile[p]=function(){return S.value}}else{const I=r[l];t.tile[p]=function(){return I.getAttribute("value")}}}function g(t,e){return x.transform(t,v(e)).divideBy(b).floor()}var x=this.options.crs.transformation,b=this.options.crs.options.crs.tile.bounds.max.x,v=L.bind(this.options.crs.scale,this.options.crs);o&&i?(t.pcrs={},t.pcrs.bounds=L.bounds([o.min,i.min],[o.max,i.max]),t.pcrs.easting=o,t.pcrs.northing=i):a&&n&&!isNaN(e.value)?(t.pcrs||(t.pcrs={},t.pcrs.easting="",t.pcrs.northing=""),t.pcrs.bounds=M.boundsToPCRSBounds(L.bounds(L.point([a.min,n.min]),L.point([a.max,n.max])),e.value,this.options.crs,M.axisToCS("column")),t.tilematrix={},t.tilematrix.col=a,t.tilematrix.row=n):console.log("Unable to determine bounds for tile template: "+t.template),t.tilematrix||(t.tilematrix={},t.tilematrix.col={},t.tilematrix.row={}),t.tilematrix.bounds=[];for(var E=t.pcrs.bounds,C=t.zoom?t.zoom.min:0,A=t.zoom?t.zoom.max:s.resolutions.length,T=0;T<=A;T++)t.tilematrix.bounds[T]=C<=T?L.bounds(g(E.min,T),g(E.max,T)):L.bounds(L.point([-1,-1]),L.point([-1,-1]))},_clampZoom:function(t){var e=L.GridLayer.prototype._clampZoom.call(this,t);return this._template.step>this.zoomBounds.maxNativeZoom&&(this._template.step=this.zoomBounds.maxNativeZoom),t!==e?t=e:t%this._template.step!=0&&(t=Math.floor(t/this._template.step)*this._template.step),t}}),_=L.Layer.extend({initialize:function(t,e){this._templates=t,L.setOptions(this,e),this._container=L.DomUtil.create("div","leaflet-layer",e.pane),this._container.style.opacity=this.options.opacity,L.DomUtil.addClass(this._container,"mapml-templatedlayer-container");for(var o,i=0;i<t.length;i++)"tile"===t[i].rel?(this.setZIndex(e.extentZIndex),this._templates[i].layer=M.templatedTileLayer(t[i],L.Util.extend(e,{errorTileUrl:"data:image/gif;base64,R0lGODlhAQABAAAAACH5BAEKAAEALAAAAAABAAEAAAICTAEAOw==",zIndex:e.extentZIndex,pane:this._container}))):"image"===t[i].rel?(this.setZIndex(e.extentZIndex),this._templates[i].layer=M.templatedImageLayer(t[i],L.Util.extend(e,{zIndex:e.extentZIndex,pane:this._container}))):"features"===t[i].rel?(this.setZIndex(e.extentZIndex),this._templates[i].layer=M.templatedFeaturesLayer(t[i],L.Util.extend(e,{zIndex:e.extentZIndex,pane:this._container}))):"query"===t[i].rel&&(this.hasSetBoundsHandler=!0,this._queries||(this._queries=[]),o=M._extractInputBounds(t[i]),t[i].extentBounds=o.bounds,t[i].zoomBounds=o.zoomBounds,t[i]._extentEl=this.options.extentEl,this._queries.push(L.extend(t[i],this._setupQueryVars(t[i]))))},getEvents:function(){return{zoomstart:this._onZoomStart}},redraw:function(){this.closePopup();for(var t=0;t<this._templates.length;t++)"tile"!==this._templates[t].rel&&"image"!==this._templates[t].rel&&"features"!==this._templates[t].rel||this._templates[t].layer.redraw()},_onZoomStart:function(){this.closePopup()},_setupQueryVars:function(t){for(var e={query:{}},o=t.values,i=0;i<t.values.length;i++){var n=o[i].getAttribute("type"),a=o[i].getAttribute("units"),r=o[i].getAttribute("axis"),s=o[i].getAttribute("name"),l=o[i].getAttribute("position"),m=o[i].getAttribute("rel"),u="map-select"===o[i].tagName.toLowerCase();if("width"===n)e.query.width=s;else if("height"===n)e.query.height=s;else if("location"===n)switch(r){case"x":case"y":case"column":case"row":e.query[r]=s;break;case"longitude":case"easting":l?l.match(/.*?-left/i)?"pixel"===m?e.query.pixelleft=s:"tile"===m?e.query.tileleft=s:e.query.mapleft=s:l.match(/.*?-right/i)&&("pixel"===m?e.query.pixelright=s:"tile"===m?e.query.tileright=s:e.query.mapright=s):e.query[r]=s;break;case"latitude":case"northing":l?l.match(/top-.*?/i)?"pixel"===m?e.query.pixeltop=s:"tile"===m?e.query.tiletop=s:e.query.maptop=s:l.match(/bottom-.*?/i)&&("pixel"===m?e.query.pixelbottom=s:"tile"===m?e.query.tilebottom=s:e.query.mapbottom=s):e.query[r]=s;break;case"i":"tile"===a?e.query.tilei=s:e.query.mapi=s;break;case"j":"tile"===a?e.query.tilej=s:e.query.mapj=s}else if("zoom"===n)e.query.zoom=s;else if(u){const c=o[i].htmlselect;e.query[s]=function(){return c.value}}else{const p=o[i];e.query[s]=function(){return p.getAttribute("value")}}}return e.query.title=t.title,e},reset:function(t,e){if(t&&this._map){var o=this._map&&this._map.hasLayer(this),i=this._templates;delete this._queries,this._map.off("click",null,this),this._templates=t;for(var n=0;n<t.length;n++)"tile"===t[n].rel?this._templates[n].layer=M.templatedTileLayer(t[n],L.Util.extend(this.options,{errorTileUrl:"data:image/gif;base64,R0lGODlhAQABAAAAACH5BAEKAAEALAAAAAABAAEAAAICTAEAOw==",zIndex:e,pane:this._container})):"image"===t[n].rel?this._templates[n].layer=M.templatedImageLayer(t[n],L.Util.extend(this.options,{zIndex:e,pane:this._container})):"features"===t[n].rel?this._templates[n].layer=M.templatedFeaturesLayer(t[n],L.Util.extend(this.options,{zIndex:e,pane:this._container})):"query"===t[n].rel&&(this._queries||(this._queries=[]),this._queries.push(L.extend(t[n],this._setupQueryVars(t[n])))),o&&this.onAdd(this._map);for(n=0;n<i.length;n++)this._map.hasLayer(i[n].layer)&&this._map.removeLayer(i[n].layer)}},onAdd:function(t){for(var e=0;e<this._templates.length;e++)"query"!==this._templates[e].rel&&t.addLayer(this._templates[e].layer)},setZIndex:function(t){return this.options.zIndex=t,this._updateZIndex(),this},_updateZIndex:function(){this._container&&void 0!==this.options.zIndex&&null!==this.options.zIndex&&(this._container.style.zIndex=this.options.zIndex)},onRemove:function(t){L.DomUtil.remove(this._container);for(var e=0;e<this._templates.length;e++)"query"!==this._templates[e].rel&&t.removeLayer(this._templates[e].layer)},_previousFeature:function(t){0<=this._count+-1&&(this._count--,this._map.fire("featurepagination",{i:this._count,popup:this}))},_nextFeature:function(t){this._count+1<this._source._totalFeatureCount&&(this._count++,this._map.fire("featurepagination",{i:this._count,popup:this}))},changeOpacity:function(t){this._container.style.opacity=t}}),y=L.Layer.extend({initialize:function(t,e){var o=M._extractInputBounds(t);this.zoomBounds=o.zoomBounds,this.extentBounds=o.bounds,this.isVisible=!0,this._template=t,this._extentEl=e.extentEl,this._container=L.DomUtil.create("div","leaflet-layer",e.pane),L.extend(e,this.zoomBounds),L.DomUtil.addClass(this._container,"mapml-features-container"),delete e.opacity,L.setOptions(this,L.extend(e,this._setUpFeaturesTemplateVars(t)))},getEvents:function(){return{moveend:this._onMoveEnd}},onAdd:function(){this._map._addZoomLimit(this);var t=this.options.opacity||1,e=this._container,o=this._map;this._features||(this._features=M.featureLayer(null,{renderer:M.featureRenderer(),pane:e,opacity:t,projection:o.options.projection,static:!0,onEachFeature:function(t,e){var o=document.createElement("div");o.classList.add("mapml-popup-content"),o.insertAdjacentHTML("afterbegin",t.innerHTML),e.bindPopup(o,{autoClose:!1,minWidth:108})}})),o.fire("moveend")},redraw:function(){this._onMoveEnd()},_removeCSS:function(){var o=this._container.querySelectorAll("link[rel=stylesheet],style");for(let e=0;e<o.length;e++){let t=o[e].parentNode;t.removeChild(o[e])}},_onMoveEnd:function(){var t=this._map.options.mapEl._history,e=t[t.length-1],o=t[t.length-2]??e,i=this._template.step,t=this._map.getZoom();let n=t;if("1"!==i&&(t+1)%i==0&&e.zoom===o.zoom-1||e.zoom===o.zoom||Math.floor(t/i)*i!=Math.floor(o.zoom/i)*i)n=Math.floor(t/i)*i;else if(t%this._template.step!=0)return;var a,r,s,l,m,u,c,p,o=this._map.getPixelBounds(this._map.getCenter(),n),i=this._getfeaturesUrl(n,o);i!==this._url&&(o=M.pixelToPCRSBounds(this._map.getPixelBounds(),t,this._map.options.projection),this.isVisible=t<=this.zoomBounds.maxZoom&&t>=this.zoomBounds.minZoom&&this.extentBounds.overlaps(o),this._features.clearLayers(),this._extentEl.shadowRoot&&(this._extentEl.shadowRoot.innerHTML=""),this._removeCSS(),this.isVisible||n!==t?(r=new Headers({Accept:"text/mapml;q=0.9,application/geo+json;q=0.8"}),s=new DOMParser,l=this._features,m=this._extentEl,u=this._map,p=function(i,n){return fetch(i,{redirect:"follow",headers:r}).then(function(t){return t.text()}).then(function(t){a=s.parseFromString(t,"application/xml");var e=new URL(a.querySelector("map-base")?a.querySelector("map-base").getAttribute("href"):i).href;i=(i=a.querySelector("map-link[rel=next]")?a.querySelector("map-link[rel=next]").getAttribute("href"):null)?new URL(i,e).href:null;var o,t=m._nativeZoom=a.querySelector("map-meta[name=zoom]")&&+M._metaContentToObject(a.querySelector("map-meta[name=zoom]").getAttribute("content")).value||0,e=m._nativeCS=a.querySelector("map-meta[name=cs]")&&M._metaContentToObject(a.querySelector("map-meta[name=cs]").getAttribute("content")).content||"PCRS";l.addData(a,e,t);for(o of a.querySelector("map-body").children)m.shadowRoot.append(o._DOMnode),o._DOMnode._extentEl=m;if(i&&--n)return p(i,n)})},(c=this)._url=i,p(i,10).then(function(){u.addLayer(l),u.fire("templatedfeatureslayeradd"),M.TemplatedFeaturesLayer.prototype._updateTabIndex(c)}).catch(function(t){console.log(t)})):this._url="")},setZIndex:function(t){return this.options.zIndex=t,this._updateZIndex(),this},_updateTabIndex:function(t){var o,i=t||this;for(o in i._features._layers){let e=i._features._layers[o];if(e._path&&("M0 0"!==e._path.getAttribute("d")?e._path.setAttribute("tabindex",0):e._path.removeAttribute("tabindex"),0===e._path.childElementCount)){let t=document.createElement("title");t.innerText="Feature",e._path.appendChild(t)}}},_updateZIndex:function(){this._container&&void 0!==this.options.zIndex&&null!==this.options.zIndex&&(this._container.style.zIndex=this.options.zIndex)},onRemove:function(){this._map.removeLayer(this._features)},_getfeaturesUrl:function(t,e){void 0===t&&(t=this._map.getZoom()),void 0===e&&(e=this._map.getPixelBounds());var o,i={};for(o in this.options.feature.zoom&&(i[this.options.feature.zoom]=t),this.options.feature.width&&(i[this.options.feature.width]=this._map.getSize().x),this.options.feature.height&&(i[this.options.feature.height]=this._map.getSize().y),this.options.feature.bottom&&(i[this.options.feature.bottom]=this._TCRSToPCRS(e.max,t).y),this.options.feature.left&&(i[this.options.feature.left]=this._TCRSToPCRS(e.min,t).x),this.options.feature.top&&(i[this.options.feature.top]=this._TCRSToPCRS(e.min,t).y),this.options.feature.right&&(i[this.options.feature.right]=this._TCRSToPCRS(e.max,t).x),this.options.feature)["width","height","left","right","top","bottom","zoom"].indexOf(o)<0&&(i[o]=this.options.feature[o]);return L.Util.template(this._template.template,i)},_TCRSToPCRS:function(t,e){var o=this._map.options.crs;return o.transformation.untransform(t,o.scale(e))},_setUpFeaturesTemplateVars:function(t){var e={feature:{}},o=t.values;e.feature.hidden=[];for(var i=0;i<o.length;i++){var n=o[i].getAttribute("type"),a=o[i].getAttribute("units"),r=o[i].getAttribute("axis"),s=o[i].getAttribute("name"),l=o[i].getAttribute("position"),m=(o[i].getAttribute("value"),"map-select"===o[i].tagName.toLowerCase());if("width"===n)e.feature.width=s;else if("height"===n)e.feature.height=s;else if("zoom"===n)e.feature.zoom=s;else if("location"!==n||"pcrs"!==a&&"gcrs"!==a)if(m){const u=o[i].htmlselect;e.feature[s]=function(){return u.value}}else{const c=o[i];e.feature[s]=function(){return c.getAttribute("value")}}else switch(r){case"x":case"longitude":case"easting":l&&(l.match(/.*?-left/i)?e.feature.left=s:l.match(/.*?-right/i)&&(e.feature.right=s));break;case"y":case"latitude":case"northing":l&&(l.match(/top-.*?/i)?e.feature.top=s:l.match(/bottom-.*?/i)&&(e.feature.bottom=s))}}return e}}),f=L.Layer.extend({initialize:function(t,e){this._template=t,this._container=L.DomUtil.create("div","leaflet-layer",e.pane),L.DomUtil.addClass(this._container,"mapml-image-container");var o=M._extractInputBounds(t);this.zoomBounds=o.zoomBounds,this.extentBounds=o.bounds,this.isVisible=!0,delete e.opacity,L.extend(e,this.zoomBounds),L.setOptions(this,L.extend(e,this._setUpExtentTemplateVars(t)))},getEvents:function(){return{moveend:this._onMoveEnd,zoomstart:this._clearLayer}},onAdd:function(){this._map._addZoomLimit(this),this.setZIndex(this.options.zIndex),this._onAdd()},redraw:function(){this._onMoveEnd()},_clearLayer:function(){var e=this._container.querySelectorAll("img");for(let t=0;t<e.length;t++)this._container.removeChild(e[t])},_addImage:function(t,e,o){let i=this._map,n=this._imageLayer;t=this.getImageUrl(t,e),e=i.getSize();this._imageOverlay=M.imageLayer(t,o,e,0,this._container),this._imageOverlay._step=this._template.step,this._imageOverlay.addTo(i),n&&(this._imageOverlay._overlayToRemove=n._url,this._imageOverlay.on("load error",function(){i.removeLayer(n)}))},_scaleImage:function(o,i){let n=this;setTimeout(function(){var t=n._template.step,e=Math.floor(i/t)*t,t=n._map.getZoomScale(i,e),e=o.min.multiplyBy(t).subtract(n._map._getNewPixelOrigin(n._map.getCenter(),i)).round();L.DomUtil.setTransform(n._imageOverlay._image,e,t)})},_onAdd:function(){var t=this._map.getZoom();let e=t;var o=this._template.step;t%o!=0&&(e=Math.floor(t/o)*o);var i=this._map.getPixelBounds(this._map.getCenter(),e);this._pixelOrigins={},this._pixelOrigins[e]=i.min;o=this._map.getPixelBounds().min.subtract(this._map.getPixelOrigin());this._addImage(i,e,o),t!==e&&this._scaleImage(i,t)},_onMoveEnd:function(t){var e=this._map.getZoom(),o=this._map.options.mapEl._history,i=o[o.length-1];let n=o[o.length-2];n=n||i;var a,r=this._template.step,o=Math.floor(e/r)*r;let s=this._map.getPixelBounds(this._map.getCenter(),o);"1"!==r&&(e+1)%r==0&&i.zoom===n.zoom-1?(this._addImage(s,o,L.point(0,0)),this._scaleImage(s,e)):t&&e%r!=0?(this._imageOverlay._overlayToRemove=this._imageOverlay._url,i.zoom!==n.zoom?(this._addImage(s,o,L.point(0,0)),this._pixelOrigins[o]=s.min,this._scaleImage(s,e)):(a=this._pixelOrigins[o],a=s.min.subtract(a),this.getImageUrl(s,o)!==this._imageOverlay._url&&(this._addImage(s,o,a),this._scaleImage(s,e)))):(o=M.pixelToPCRSBounds(this._map.getPixelBounds(),e,this._map.options.projection),this.isVisible=e<=this.zoomBounds.maxZoom&&e>=this.zoomBounds.minZoom&&this.extentBounds.overlaps(o),this.isVisible?(o=(a=this._map).getPixelBounds().min.subtract(a.getPixelOrigin()),this._addImage(a.getPixelBounds(),e,o),this._pixelOrigins[e]=a.getPixelOrigin()):this._clearLayer())},setZIndex:function(t){return this.options.zIndex=t,this._updateZIndex(),this},_updateZIndex:function(){this._container&&void 0!==this.options.zIndex&&null!==this.options.zIndex&&(this._container.style.zIndex=this.options.zIndex)},onRemove:function(t){this._clearLayer(),t._removeZoomLimit(this),this._container=null},getImageUrl:function(t,e){var o,i={};for(o in i[this.options.extent.width]=this._map.getSize().x,i[this.options.extent.height]=this._map.getSize().y,i[this.options.extent.bottom]=this._TCRSToPCRS(t.max,e).y,i[this.options.extent.left]=this._TCRSToPCRS(t.min,e).x,i[this.options.extent.top]=this._TCRSToPCRS(t.min,e).y,i[this.options.extent.right]=this._TCRSToPCRS(t.max,e).x,this.options.extent)["width","height","left","right","top","bottom"].indexOf(o)<0&&(i[o]=this.options.extent[o]);return L.Util.template(this._template.template,i)},_TCRSToPCRS:function(t,e){var o=this._map.options.crs;return o.transformation.untransform(t,o.scale(e))},_setUpExtentTemplateVars:function(t){for(var e={extent:{}},o=t.values,i=0;i<t.values.length;i++){var n=o[i].getAttribute("type"),a=o[i].getAttribute("units"),r=o[i].getAttribute("axis"),s=o[i].getAttribute("name"),l=o[i].getAttribute("position"),m="map-select"===o[i].tagName.toLowerCase();if("width"===n)e.extent.width=s;else if("height"===n)e.extent.height=s;else if("location"!==n||"pcrs"!==a&&"gcrs"!==a)if(m){const u=o[i].htmlselect;e.extent[s]=function(){return u.value}}else{const c=o[i];e.extent[s]=function(){return c.getAttribute("value")}}else switch(r){case"longitude":case"easting":l&&(l.match(/.*?-left/i)?e.extent.left=s:l.match(/.*?-right/i)&&(e.extent.right=s));break;case"latitude":case"northing":l&&(l.match(/top-.*?/i)?e.extent.top=s:l.match(/bottom-.*?/i)&&(e.extent.bottom=s))}}return e}}),g=L.ImageOverlay.extend({initialize:function(t,e,o,i,n,a){this._container=n,this._url=t,this._location=e,this._size=L.point(o),this._angle=i,L.setOptions(this,a)},getEvents:function(){var t={viewreset:this._reset};return this._zoomAnimated&&this._step<=1&&(t.zoomanim=this._animateZoom),t},onAdd:function(){this.on({load:this._onImageLoad}),this._image||this._initImage(),this.options.interactive&&(L.DomUtil.addClass(this._image,"leaflet-interactive"),this.addInteractiveTarget(this._image)),this._container.appendChild(this._image),this._reset()},onRemove:function(){L.DomUtil.remove(this._image),this.options.interactive&&this.removeInteractiveTarget(this._image)},_onImageLoad:function(){this._image&&(this._image.loaded=+new Date,this._updateOpacity())},_animateZoom:function(t){var e=this._map.getZoomScale(t.zoom),t=this._map.getPixelOrigin().add(this._location).multiplyBy(e).subtract(this._map._getNewPixelOrigin(t.center,t.zoom)).round();L.Browser.any3d?L.DomUtil.setTransform(this._image,t,e):L.DomUtil.setPosition(this._image,t)},_reset:function(t){var e=this._image,o=this._location,i=this._size;t&&1<this._step&&(void 0===this._overlayToRemove||this._url===this._overlayToRemove)||(L.DomUtil.setPosition(e,o),e.style.width=i.x+"px",e.style.height=i.y+"px")},_updateOpacity:function(){var t,e,o;this._map&&(o=+new Date,t=!1,e=this._image,o=Math.min(1,(o-e.loaded)/200),L.DomUtil.setOpacity(e,o),(t=o<1?!0:t)&&(L.Util.cancelAnimFrame(this._fadeFrame),this._fadeFrame=L.Util.requestAnimFrame(this._updateOpacity,this)),L.DomUtil.addClass(e,"leaflet-image-loaded"))}}),x=L.Layer.extend({options:{maxNext:10,zIndex:0,maxZoom:25,opacity:"1.0"},initialize:function(t,e,o){var i;t&&(this._href=t),e&&(i=!!(this._layerEl=e).querySelector("map-feature,map-tile,map-extent"),!t&&i&&(this._content=e)),L.setOptions(this,o),this._container=L.DomUtil.create("div","leaflet-layer"),this.changeOpacity(this.options.opacity),L.DomUtil.addClass(this._container,"mapml-layer"),this._imageContainer=L.DomUtil.create("div","leaflet-layer",this._container),L.DomUtil.addClass(this._imageContainer,"mapml-image-container"),this._mapmlTileContainer=L.DomUtil.create("div","mapml-tile-container",this._container),!i&&e&&e.hasAttribute("label")&&(this._title=e.getAttribute("label")),this._initialize(i?e:null),this.on("attached",this._validateExtent,this),this.validProjection=!0,this._mapmlLayerItem={}},setZIndex:function(t){return this.options.zIndex=t,this._updateZIndex(),this},getHref:function(){return this._href??""},_updateZIndex:function(){this._container&&void 0!==this.options.zIndex&&null!==this.options.zIndex&&(this._container.style.zIndex=this.options.zIndex)},_removeExtents:function(e){if(this._extent._mapExtents)for(let t=0;t<this._extent._mapExtents.length;t++)this._extent._mapExtents[t].templatedLayer&&e.removeLayer(this._extent._mapExtents[t].templatedLayer);this._extent._queries&&delete this._extent._queries},_changeOpacity:function(t){t&&t.target&&0<=t.target.value&&t.target.value<=1&&this.changeOpacity(t.target.value)},changeOpacity:function(t){this._container.style.opacity=t,this.opacityEl&&(this.opacityEl.value=t)},_changeExtentOpacity:function(t){t&&t.target&&0<=t.target.value&&t.target.value<=1&&(this.templatedLayer.changeOpacity(t.target.value),this._templateVars.opacity=t.target.value)},_changeExtent:function(t,e){t.target.checked?(e.checked=!0,this._layerEl.checked&&(e.templatedLayer=M.templatedLayer(e._templateVars,{pane:this._container,opacity:e._templateVars.opacity,_leafletLayer:this,crs:e.crs,extentZIndex:e.extentZIndex,extentEl:e._DOMnode||e}).addTo(this._map),e.templatedLayer.setZIndex(),this._setLayerElExtent())):(L.DomEvent.stopPropagation(t),e.checked=!1,this._layerEl.checked&&this._map.removeLayer(e.templatedLayer),this._setLayerElExtent())},onAdd:function(o){if(!this._extent||this._validProjection(o)){this._map=o,this._content?(this._mapmlvectors||(this._mapmlvectors=M.featureLayer(this._content,{renderer:M.featureRenderer(),pane:this._container,opacity:this.options.opacity,projection:o.options.projection,_leafletLayer:this,static:!0,onEachFeature:function(t,e){var o;t&&((o=document.createElement("div")).classList.add("mapml-popup-content"),o.insertAdjacentHTML("afterbegin",t.innerHTML),e.bindPopup(o,{autoClose:!1,minWidth:165}))}})),this._setLayerElExtent(),o.addLayer(this._mapmlvectors)):this.once("extentload",function(){this._validProjection(o)?(this._mapmlvectors||(this._mapmlvectors=M.featureLayer(this._content,{renderer:M.featureRenderer(),pane:this._container,opacity:this.options.opacity,projection:o.options.projection,_leafletLayer:this,static:!0,onEachFeature:function(t,e){var o;t&&((o=document.createElement("div")).classList.add("mapml-popup-content"),o.insertAdjacentHTML("afterbegin",t.innerHTML),e.bindPopup(o,{autoClose:!1,minWidth:165}))}}).addTo(o)),this._setLayerElExtent()):this.validProjection=!1},this),this._imageLayer||(this._imageLayer=L.layerGroup()),o.addLayer(this._imageLayer),(!this._staticTileLayer||null===this._staticTileLayer._container)&&0<this._mapmlTileContainer.getElementsByTagName("map-tiles").length&&(this._staticTileLayer=M.staticTileLayer({pane:this._container,_leafletLayer:this,className:"mapml-static-tile-layer",tileContainer:this._mapmlTileContainer,maxZoomBound:o.options.crs.options.resolutions.length-1,tileSize:o.options.crs.options.crs.tile.bounds.max.x}),o.addLayer(this._staticTileLayer),this._setLayerElExtent());const t=function(){if(this._extent&&this._extent._mapExtents){for(let t=0;t<this._extent._mapExtents.length;t++){var e;this._extent._mapExtents[t]._templateVars&&this._extent._mapExtents[t].checked&&(this._extent._mapExtents[t].extentZIndex||(this._extent._mapExtents[t].extentZIndex=t),this._templatedLayer=M.templatedLayer(this._extent._mapExtents[t]._templateVars,{pane:this._container,opacity:this._extent._mapExtents[t]._templateVars.opacity,_leafletLayer:this,crs:this._extent.crs,extentZIndex:this._extent._mapExtents[t].extentZIndex,extentEl:this._extent._mapExtents[t]._DOMnode||this._extent._mapExtents[t]}).addTo(o),this._extent._mapExtents[t].templatedLayer=this._templatedLayer,this._templatedLayer._queries&&(this._extent._queries||(this._extent._queries=[]),this._extent._queries=this._extent._queries.concat(this._templatedLayer._queries))),this._extent._mapExtents[t].hasAttribute("opacity")&&(e=this._extent._mapExtents[t].getAttribute("opacity"),this._extent._mapExtents[t].templatedLayer.changeOpacity(e))}this._setLayerElExtent()}}.bind(this);this._extent&&this._extent._mapExtents&&this._extent._mapExtents[0]._templateVars?t():this.once("extentload",function(){this._validProjection(o)?t():this.validProjection=!1},this),this.setZIndex(this.options.zIndex),this.getPane().appendChild(this._container),setTimeout(()=>{o.fire("checkdisabled")},0),o.on("popupopen",this._attachSkipButtons,this)}else this.validProjection=!1},_validProjection:function(e){let o=!1;if(this._extent&&this._extent._mapExtents)for(let t=0;t<this._extent._mapExtents.length;t++)if(this._extent._mapExtents[t]._templateVars)for(var i of this._extent._mapExtents[t]._templateVars)if(!i.projectionMatch&&i.projection!==e.options.projection){o=!0;break}return!(o||this.getProjection()!==e.options.projection.toUpperCase())},_setLayerElExtent:function(){let i,n,a,r,s,e={minZoom:0,maxZoom:0,maxNativeZoom:0,minNativeZoom:0};["_staticTileLayer","_imageLayer","_mapmlvectors","_templatedLayer"].forEach(t=>{if(this[t])if("_templatedLayer"===t){for(let e=0;e<this._extent._mapExtents.length;e++)for(let t=0;t<this._extent._mapExtents[e]._templateVars.length;t++){var o=M._extractInputBounds(this._extent._mapExtents[e]._templateVars[t]);this._extent._mapExtents[e]._templateVars[t].tempExtentBounds=o.bounds,this._extent._mapExtents[e]._templateVars[t].extentZoomBounds=o.zoomBounds}for(let e=0;e<this._extent._mapExtents.length;e++)if(this._extent._mapExtents[e].checked)for(let t=0;t<this._extent._mapExtents[e]._templateVars.length;t++)s=i?(i.extend(this._extent._mapExtents[e]._templateVars[t].tempExtentBounds.min),i.extend(this._extent._mapExtents[e]._templateVars[t].tempExtentBounds.max),n=Math.max(n,this._extent._mapExtents[e]._templateVars[t].extentZoomBounds.maxZoom),a=Math.min(a,this._extent._mapExtents[e]._templateVars[t].extentZoomBounds.minZoom),r=Math.max(r,this._extent._mapExtents[e]._templateVars[t].extentZoomBounds.maxNativeZoom),Math.min(s,this._extent._mapExtents[e]._templateVars[t].extentZoomBounds.minNativeZoom)):(i=this._extent._mapExtents[e]._templateVars[t].tempExtentBounds,n=this._extent._mapExtents[e]._templateVars[t].extentZoomBounds.maxZoom,a=this._extent._mapExtents[e]._templateVars[t].extentZoomBounds.minZoom,r=this._extent._mapExtents[e]._templateVars[t].extentZoomBounds.maxNativeZoom,this._extent._mapExtents[e]._templateVars[t].extentZoomBounds.minNativeZoom);e.minZoom=a,e.maxZoom=n,e.minNativeZoom=s,e.maxNativeZoom=r,this._extent.zoomBounds=e,this._extent.layerBounds=i;for(let t=0;t<this._extent._mapExtents.length;t++)this._extent._mapExtents[t].templatedLayer.layerBounds=i,this._extent._mapExtents[t].templatedLayer.zoomBounds=e}else this[t].layerBounds&&(i?(i.extend(this[t].layerBounds.min),i.extend(this[t].layerBounds.max)):(i=this[t].layerBounds,e=this[t].zoomBounds))}),i&&(this._layerEl.extent=Object.assign(M._convertAndFormatPCRS(i,this._map),{zoom:e}))},addTo:function(t){return t.addLayer(this),this},getEvents:function(){return{zoomanim:this._onZoomAnim}},redraw:function(){if(this._extent._mapExtents)for(let t=0;t<this._extent._mapExtents.length;t++)this._extent._mapExtents[t].templatedLayer&&this._extent._mapExtents[t].templatedLayer.redraw()},_onZoomAnim:function(t){if(this._map){var t=t.zoom,e=this._extent&&this._extent._mapExtents?this._extent._mapExtents[0].querySelector("map-input[type=zoom]"):null,o=e&&e.hasAttribute("min")?parseInt(e.getAttribute("min")):this._map.getMinZoom(),i=e&&e.hasAttribute("max")?parseInt(e.getAttribute("max")):this._map.getMaxZoom();if(e)for(let t=1;t<this._extent._mapExtents.length;t++)(e=this._extent._mapExtents[t].querySelector("map-input[type=zoom]"))&&e.hasAttribute("min")&&(o=Math.min(parseInt(e.getAttribute("min")),o)),e&&e.hasAttribute("max")&&(i=Math.max(parseInt(e.getAttribute("max")),i));t<o&&this._extent.zoomout||i<t&&this._extent.zoomin;o<=t&&t<=i||(this._extent.zoomin&&i<t?(this._href=this._extent.zoomin,this._layerEl.src=this._extent.zoomin,this.href=this._extent.zoomin,this._layerEl.src=this._extent.zoomin):this._extent.zoomout&&t<o&&(this._href=this._extent.zoomout,this.href=this._extent.zoomout,this._layerEl.src=this._extent.zoomout)),this._templatedLayer}},onRemove:function(t){L.DomUtil.remove(this._container),this._staticTileLayer&&t.removeLayer(this._staticTileLayer),this._mapmlvectors&&t.removeLayer(this._mapmlvectors),this._imageLayer&&t.removeLayer(this._imageLayer),this._extent&&this._extent._mapExtents&&this._removeExtents(t),t.fire("checkdisabled"),t.off("popupopen",this._attachSkipButtons)},getAttribution:function(){return this.options.attribution},getLayerExtentHTML:function(t,o){var i=L.DomUtil.create("fieldset","mapml-layer-extent"),e=L.DomUtil.create("div","mapml-layer-item-properties",i),n=L.DomUtil.create("div","mapml-layer-item-settings",i),a=L.DomUtil.create("label","mapml-layer-item-toggle",e),r=L.DomUtil.create("input"),s=L.SVG.create("svg"),l=L.SVG.create("path"),m=L.SVG.create("path"),u=L.DomUtil.create("span"),c=L.DomUtil.create("div","mapml-layer-item-controls",e),p=L.DomUtil.create("details","mapml-layer-item-opacity",n),e=L.DomUtil.create("summary","",p),h=this._layerEl.parentNode,d=this._layerEl,p=L.DomUtil.create("input","",p);n.hidden=!0,i.setAttribute("aria-grabbed","false"),t||(i.setAttribute("hidden",""),this._extent._mapExtents[o].hidden=!0),s.setAttribute("viewBox","0 0 24 24"),s.setAttribute("height","22"),s.setAttribute("width","22"),l.setAttribute("d","M0 0h24v24H0z"),l.setAttribute("fill","none"),m.setAttribute("d","M12 8c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm0 2c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm0 6c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z"),s.appendChild(l),s.appendChild(m);let _=L.DomUtil.create("button","mapml-layer-item-remove-control",c);_.type="button",_.title="Remove Sub Layer",_.innerHTML="<span aria-hidden='true'>&#10005;</span>",_.classList.add("mapml-button"),L.DomEvent.on(_,"click",L.DomEvent.stop),L.DomEvent.on(_,"click",t=>{let e=!0;t.target.checked=!1,this._extent._mapExtents[o].removed=!0,this._extent._mapExtents[o].checked=!1,this._layerEl.checked&&this._changeExtent(t,this._extent._mapExtents[o]),this._extent._mapExtents[o].extentAnatomy.parentNode.removeChild(this._extent._mapExtents[o].extentAnatomy);for(let t=0;t<this._extent._mapExtents.length;t++)this._extent._mapExtents[t].removed||(e=!1);e&&this._layerItemSettingsHTML.removeChild(this._extentGroupAnatomy)},this);let y=L.DomUtil.create("button","mapml-layer-item-settings-control",c);y.type="button",y.title="Extent Settings",y.setAttribute("aria-expanded",!1),y.classList.add("mapml-button"),L.DomEvent.on(y,"click",t=>{!0===n.hidden?(y.setAttribute("aria-expanded",!0),n.hidden=!1):(y.setAttribute("aria-expanded",!1),n.hidden=!0)},this),u.setAttribute("aria-hidden",!0),a.appendChild(r),y.appendChild(u),u.appendChild(s),e.innerText="Opacity",e.id="mapml-layer-item-opacity-"+L.stamp(e),p.setAttribute("type","range"),p.setAttribute("min","0"),p.setAttribute("max","1.0"),p.setAttribute("step","0.1"),p.setAttribute("aria-labelledby","mapml-layer-item-opacity-"+L.stamp(e));e=this._extent._mapExtents[o].hasAttribute("opacity")?this._extent._mapExtents[o].getAttribute("opacity"):"1.0";this._extent._mapExtents[o]._templateVars.opacity=e,p.setAttribute("value",e),p.value=e,L.DomEvent.on(p,"change",this._changeExtentOpacity,this._extent._mapExtents[o]);a=L.DomUtil.create("span","mapml-layer-item-name",a);return r.defaultChecked=!!this._extent._mapExtents[o],this._extent._mapExtents[o].checked=r.defaultChecked,r.type="checkbox",a.innerHTML=t,L.DomEvent.on(r,"change",t=>{this._changeExtent(t,this._extent._mapExtents[o])}),a.id="mapml-extent-item-name-{"+L.stamp(a)+"}",i.setAttribute("aria-labelledby",a.id),a.extent=this._extent._mapExtents[o],i.ontouchstart=i.onmousedown=e=>{if("label"===e.target.parentElement.tagName.toLowerCase()&&"input"!==e.target.tagName.toLowerCase()||"label"===e.target.tagName.toLowerCase()){e.stopPropagation(),e=e instanceof TouchEvent?e.touches[0]:e;let s=i,l=i.parentNode,t=!1,m=e.clientY;document.body.ontouchmove=document.body.onmousemove=a=>{a.preventDefault();var r=(a=a instanceof TouchEvent?a.touches[0]:a).clientY-m;if(t=5<Math.abs(r)||t,!(l&&!t||l&&l.childElementCount<=1||l.getBoundingClientRect().top>s.getBoundingClientRect().bottom||l.getBoundingClientRect().bottom<s.getBoundingClientRect().top)){l.classList.add("mapml-draggable"),s.style.transform="translateY("+r+"px)",s.style.pointerEvents="none";let t=a.clientX,e=a.clientY,o=("MAPML-VIEWER"===h.tagName?h:h.querySelector(".mapml-web-map")).shadowRoot,i=o.elementFromPoint(t,e),n=i&&i.closest("fieldset")?i.closest("fieldset"):s;n=Math.abs(r)<=n.offsetHeight?s:n,s.setAttribute("aria-grabbed","true"),s.setAttribute("aria-dropeffect","move"),n&&l===n.parentNode&&(n=n!==s.nextSibling?n:n.nextSibling,s!==n&&(m=a.clientY,s.style.transform=null),l.insertBefore(s,n))}},document.body.ontouchend=document.body.onmouseup=()=>{s.setAttribute("aria-grabbed","false"),s.removeAttribute("aria-dropeffect"),s.style.pointerEvents=null,s.style.transform=null;let t=l.children,e=0;for(var o of t){let t=o.querySelector("span").extent;t.setAttribute("data-moving",""),d.insertAdjacentElement("beforeend",t),t.removeAttribute("data-moving"),t.extentZIndex=e,t.templatedLayer.setZIndex(e),e++}l.classList.remove("mapml-draggable"),document.body.ontouchmove=document.body.onmousemove=document.body.ontouchend=document.body.onmouseup=null}}},i},getLayerUserControlsHTML:function(){var o=L.DomUtil.create("fieldset","mapml-layer-item"),t=L.DomUtil.create("input"),e=L.DomUtil.create("span","mapml-layer-item-name"),i=L.DomUtil.create("span"),n=L.DomUtil.create("div","mapml-layer-item-properties",o),a=L.DomUtil.create("div","mapml-layer-item-settings",o),r=L.DomUtil.create("label","mapml-layer-item-toggle",n),s=L.DomUtil.create("div","mapml-layer-item-controls",n),l=L.DomUtil.create("details","mapml-layer-item-opacity mapml-control-layers",a),m=L.DomUtil.create("input"),u=L.DomUtil.create("summary"),c=L.SVG.create("svg"),p=L.SVG.create("path"),n=L.SVG.create("path"),h=L.DomUtil.create("fieldset","mapml-layer-grouped-extents"),d=this._layerEl.parentNode;this.opacityEl=m,this._mapmlLayerItem=o,c.setAttribute("viewBox","0 0 24 24"),c.setAttribute("height","22"),c.setAttribute("width","22"),c.setAttribute("fill","currentColor"),p.setAttribute("d","M0 0h24v24H0z"),p.setAttribute("fill","none"),n.setAttribute("d","M12 8c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm0 2c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm0 6c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z"),c.appendChild(p),c.appendChild(n),a.hidden=!0,i.setAttribute("aria-hidden",!0);let _=L.DomUtil.create("button","mapml-layer-item-remove-control",s);_.type="button",_.title="Remove Layer",_.innerHTML="<span aria-hidden='true'>&#10005;</span>",_.classList.add("mapml-button"),L.DomEvent.on(_,"click",L.DomEvent.stop),L.DomEvent.on(_,"click",t=>{let e=0,o,i;if(i=("MAPML-VIEWER"===d.tagName?d:d.querySelector(".mapml-web-map")).shadowRoot,t.target.closest("fieldset").nextElementSibling&&!t.target.closest("fieldset").nextElementSibling.disbaled)for(o=t.target.closest("fieldset").previousElementSibling;o;)e+=2,o=o.previousElementSibling;else o="link";d.removeChild(t.target.closest("fieldset").querySelector("span").layer._layerEl),o=o?i.querySelector(".leaflet-control-attribution").firstElementChild:o=i.querySelectorAll("input")[e],o.focus()},this);let y=L.DomUtil.create("button","mapml-layer-item-settings-control",s);if(y.type="button",y.title="Layer Settings",y.setAttribute("aria-expanded",!1),y.classList.add("mapml-button"),L.DomEvent.on(y,"click",t=>{let e=this._layerEl._layerControl._container;e._isExpanded||"touch"!==t.pointerType?!0===a.hidden?(y.setAttribute("aria-expanded",!0),a.hidden=!1):(y.setAttribute("aria-expanded",!1),a.hidden=!0):e._isExpanded=!0},this),t.defaultChecked=!!this._map,t.type="checkbox",t.setAttribute("class","leaflet-control-layers-selector"),(e.layer=this)._legendUrl?((s=document.createElement("a")).text=" "+this._title,s.href=this._legendUrl,s.target="_blank",s.draggable=!1,e.appendChild(s)):e.innerHTML=this._title,e.id="mapml-layer-item-name-{"+L.stamp(e)+"}",u.innerText="Opacity",u.id="mapml-layer-item-opacity-"+L.stamp(u),l.appendChild(u),l.appendChild(m),m.setAttribute("type","range"),m.setAttribute("min","0"),m.setAttribute("max","1.0"),m.setAttribute("value",this._container.style.opacity||"1.0"),m.setAttribute("step","0.1"),m.setAttribute("aria-labelledby",u.id),m.value=this._container.style.opacity||"1.0",o.setAttribute("aria-grabbed","false"),o.setAttribute("aria-labelledby",e.id),o.ontouchstart=o.onmousedown=e=>{if("label"===e.target.parentElement.tagName.toLowerCase()&&"input"!==e.target.tagName.toLowerCase()||"label"===e.target.tagName.toLowerCase()){e=e instanceof TouchEvent?e.touches[0]:e;let s=o,l=o.parentNode,t=!1,m=e.clientY;document.body.ontouchmove=document.body.onmousemove=a=>{a.preventDefault();var r=(a=a instanceof TouchEvent?a.touches[0]:a).clientY-m;if(t=5<Math.abs(r)||t,!(l&&!t||l&&l.childElementCount<=1||l.getBoundingClientRect().top>s.getBoundingClientRect().bottom||l.getBoundingClientRect().bottom<s.getBoundingClientRect().top)){l.classList.add("mapml-draggable"),s.style.transform="translateY("+r+"px)",s.style.pointerEvents="none";let t=a.clientX,e=a.clientY,o=("MAPML-VIEWER"===d.tagName?d:d.querySelector(".mapml-web-map")).shadowRoot,i=o.elementFromPoint(t,e),n=i&&i.closest("fieldset")?i.closest("fieldset"):s;n=Math.abs(r)<=n.offsetHeight?s:n,s.setAttribute("aria-grabbed","true"),s.setAttribute("aria-dropeffect","move"),n&&l===n.parentNode&&(n=n!==s.nextSibling?n:n.nextSibling,s!==n&&(m=a.clientY,s.style.transform=null),l.insertBefore(s,n))}},document.body.ontouchend=document.body.onmouseup=()=>{s.setAttribute("aria-grabbed","false"),s.removeAttribute("aria-dropeffect"),s.style.pointerEvents=null,s.style.transform=null;let t=l.children,e=1;for(var o of t){let t=o.querySelector("span").layer._layerEl;t.setAttribute("data-moving",""),d.insertAdjacentElement("beforeend",t),t.removeAttribute("data-moving"),t._layer.setZIndex(e),e++}l.classList.remove("mapml-draggable"),document.body.ontouchmove=document.body.onmousemove=document.body.onmouseup=null}}},L.DomEvent.on(m,"change",this._changeOpacity,this),r.appendChild(t),r.appendChild(e),y.appendChild(i),i.appendChild(c),this._styles&&a.appendChild(this._styles),this._userInputs){var f=document.createDocumentFragment(),g=this._extent._templateVars;if(g)for(var x=0;x<g.length;x++)for(var b=g[x],v=0;v<b.values.length;v++){var M,E,C=b.values[v],A="#"+C.getAttribute("id");"map-select"!==C.tagName.toLowerCase()||f.querySelector(A)||(M=L.DomUtil.create("details","mapml-layer-item-time mapml-control-layers",f),E=L.DomUtil.create("summary"),(A=L.DomUtil.create("label")).innerText=C.getAttribute("name"),A.setAttribute("for",C.getAttribute("id")),E.appendChild(A),M.appendChild(E),M.appendChild(C.htmlselect))}a.appendChild(f)}if(this._extent&&this._extent._mapExtents){var T=!0;this._layerItemSettingsHTML=a,(this._extentGroupAnatomy=h).setAttribute("aria-label","Sublayers");for(let t=0;t<this._extent._mapExtents.length;t++)h.appendChild(this._extent._mapExtents[t].extentAnatomy),this._extent._mapExtents[t].hidden||(T=!1);T||a.appendChild(h)}return this._mapmlLayerItem},_initialize:function(t){var S,E,e,o;function C(e,i,n,t,o,a){var r=[],s=e.querySelectorAll("map-link[rel=tile],map-link[rel=image],map-link[rel=features],map-link[rel=query]"),l=new RegExp("(?:{)(.*?)(?:})","g"),m=e.querySelector('map-input[type="zoom" i]'),u=!1,c={zoom:0};if(i){let t=M._metaContentToObject(i.getAttribute("content")),e;c.zoom=t.zoom||c.zoom;let o=Object.keys(t);for(let t=0;t<o.length;t++)if(!o[t].includes("zoom")){e=M.axisToCS(o[t].split("-")[2]);break}i=M.csToAxes(e);c.bounds=M.boundsToPCRSBounds(L.bounds(L.point(+t["top-left-"+i[0]],+t["top-left-"+i[1]]),L.point(+t["bottom-right-"+i[0]],+t["bottom-right-"+i[1]])),c.zoom,n,e)}else c.bounds=M[n].options.crs.pcrs.bounds;for(var p=0;p<s.length;p++){var h=s[p],d=h.getAttribute("tref");if(h.zoomInput=m,!d){var _,d=k;for(_ of t.querySelectorAll("map-input"))d+=`{${_.getAttribute("name")}}`}for(var y=h.hasAttribute("title")?h.getAttribute("title"):"Query this layer",f=d.match(l),g=h.hasAttribute("rel")&&"tile"!==h.getAttribute("rel").toLowerCase()?h.getAttribute("rel").toLowerCase():"tile",x=h.hasAttribute("type")?h.getAttribute("type").toLowerCase():"image/*",b=[],v=h&&h.hasAttribute("tms"),E=t.querySelector("map-meta[name=zoom]")?M._metaContentToObject(t.querySelector("map-meta[name=zoom]").getAttribute("content")):void 0;null!==(T=l.exec(d));){var C,A=T[1],T=e.querySelector("map-input[name="+A+"],map-select[name="+A+"]");if(!T){console.log("input with name="+A+" not found for template variable of same name");break}!T.hasAttribute("type")||"location"!==T.getAttribute("type")||T.hasAttribute("min")&&T.hasAttribute("max")||!T.hasAttribute("axis")||["i","j"].includes(T.getAttribute("axis").toLowerCase())||(m&&d.includes(`{${m.getAttribute("name")}}`)&&m.setAttribute("value",c.zoom),C=T.getAttribute("axis"),A=M.convertPCRSBounds(c.bounds,c.zoom,n,M.axisToCS(C)),T.setAttribute("min",A.min[M.axisToXY(C)]),T.setAttribute("max",A.max[M.axisToXY(C)])),b.push(T),u=u||T.hasAttribute("type")&&"zoom"===T.getAttribute("type").toLowerCase(),"map-select"===T.tagName.toLowerCase()&&((C=document.createElement("div")).insertAdjacentHTML("afterbegin",T.outerHTML),T.htmlselect=C.querySelector("map-select"),T.htmlselect=function(e){var o=document.createElement("select"),i=e.getAttributeNames();for(let t=0;t<i.length;t++)o.setAttribute(i[t],e.getAttribute(i[t]));var n=e.children;for(let e=0;e<n.length;e++){var a=document.createElement("option"),r=n[e].getAttributeNames();for(let t=0;t<r.length;t++)a.setAttribute(r[t],n[e].getAttribute(r[t]));a.innerHTML=n[e].innerHTML,o.appendChild(a)}return o}(T.htmlselect),L.DomEvent.on(T.htmlselect,"change",S.redraw,S),S._userInputs||(S._userInputs=[]),S._userInputs.push(T.htmlselect))}if(d&&f.length===b.length||d===k){"query"===g&&(S.queryable=!0),!u&&m&&b.push(m);let t=m?m.getAttribute("step"):1;t&&"0"!==t&&!isNaN(t)||(t=1),r.push({template:decodeURI(new URL(d,o)),linkEl:h,title:y,rel:g,type:x,values:b,zoomBounds:E,extentPCRSFallback:{bounds:c.bounds},projectionMatch:a,projection:e.getAttribute("units")||I,tms:v,step:t})}}return r}function i(t){var e=this.responseXML||t;if(e.querySelector&&e.querySelector("map-feature")&&(S._content=e),!this.responseXML&&this.responseText&&(e=(new DOMParser).parseFromString(this.responseText,"text/xml")),this.readyState===this.DONE&&e.querySelector&&!e.querySelector("parsererror")){var o,i=e.querySelectorAll("map-extent");if(i.length||(m=e.querySelector("map-meta[name=projection]")),1<=i.length)for(let t=0;t<i.length&&(n=(o="map-extent"===i[t].tagName.toLowerCase()&&i[t].hasAttribute("units")?i[t].getAttribute("units"):o)&&o===S.options.mapprojection);t++);else m&&"map-meta"===m.tagName.toLowerCase()&&m.hasAttribute("content")&&(n=(o=M._metaContentToObject(m.getAttribute("content")).content)&&o===S.options.mapprojection);var n,a,r=e.querySelector("map-meta[name=extent]"),s=!n&&e.querySelector("map-head map-link[rel=alternate][projection="+S.options.mapprojection+"]"),l=new URL(e.querySelector("map-base")?e.querySelector("map-base").getAttribute("href"):e.baseURI||this.responseURL,this.responseURL).href;if(!n&&s&&s.hasAttribute("href"))return void S.fire("changeprojection",{href:new URL(s.getAttribute("href"),l).href},!1);if(!n&&S._map&&1===S._map.options.mapEl.querySelectorAll("layer-").length)return void(S._map.options.mapEl.projection=o);if(m)S._extent=m;else{S._extent={},n&&(S._extent.crs=M[o]),S._extent._mapExtents=[],S._extent._templateVars=[];for(let t=0;t<i.length;t++)i[t].querySelector("map-link[rel=tile],map-link[rel=image],map-link[rel=features],map-link[rel=query]")&&i[t].hasAttribute("units")&&(S._extent._mapExtents.push(i[t]),n=n||s,a=C.call(S,i[t],r,o,e,l,n),S._extent._mapExtents[t]._templateVars=a,S._extent._templateVars=S._extent._templateVars.concat(a))}S._parseLicenseAndLegend(e,S,o);var t=e.querySelector("map-link[rel=zoomin]"),m=e.querySelector("map-link[rel=zoomout]");if(delete S._extent.zoomin,delete S._extent.zoomout,t&&(S._extent.zoomin=new URL(t.getAttribute("href"),l).href),m&&(S._extent.zoomout=new URL(m.getAttribute("href"),l).href),S._extent._mapExtents)for(let t=0;t<S._extent._mapExtents.length;t++)S._extent._mapExtents[t].templatedLayer&&S._extent._mapExtents[t].templatedLayer.reset(S._extent._mapExtents[t]._templateVars,S._extent._mapExtents[t].extentZIndex);if(e.querySelector("map-tile")){var u=document.createElement("map-tiles"),c=e.querySelector("map-meta[name=zoom][content]")||e.querySelector("map-input[type=zoom][value]");u.setAttribute("zoom",c&&c.getAttribute("content")||c&&c.getAttribute("value")||"0");for(var p=e.getElementsByTagName("map-tile"),h=0;h<p.length;h++)u.appendChild(document.importNode(p[h],!0));S._mapmlTileContainer.appendChild(u)}if(M._parseStylesheetAsHTML(e,l,S._container),S._extent._mapExtents)for(let t=0;t<S._extent._mapExtents.length;t++){var d=S._extent._mapExtents[t].getAttribute("label"),d=S.getLayerExtentHTML(d,t);S._extent._mapExtents[t].extentAnatomy=d}var _=e.querySelectorAll('map-link[rel=style],map-link[rel="self style"],map-link[rel="style self"]');if(1<_.length){var y=document.createElement("details"),c=document.createElement("summary");c.innerText="Style",y.appendChild(c);for(var f=function(t){S.fire("changestyle",{src:t.target.getAttribute("data-href")},!1)},g=0;g<_.length;g++){var x=document.createElement("div"),b=x.appendChild(document.createElement("input"));b.setAttribute("type","radio"),b.setAttribute("id","rad-"+L.stamp(b)),b.setAttribute("name","styles-"+this._title),b.setAttribute("value",_[g].getAttribute("title")),b.setAttribute("data-href",new URL(_[g].getAttribute("href"),l).href);var v=x.appendChild(document.createElement("label"));v.setAttribute("for","rad-"+L.stamp(b)),v.innerText=_[g].getAttribute("title"),"style self"!==_[g].getAttribute("rel")&&"self style"!==_[g].getAttribute("rel")||(b.checked=!0),y.appendChild(x),L.DomUtil.addClass(y,"mapml-layer-item-style mapml-control-layers"),L.DomEvent.on(b,"click",f,S)}S._styles=y}e.querySelector("map-title")?S._title=e.querySelector("map-title").textContent.trim():e instanceof Element&&e.hasAttribute("label")&&(S._title=e.getAttribute("label").trim()),S._map&&(S._validateExtent(),S._map.hasLayer(S)&&S._map.attributionControl.addAttribution(S.getAttribution()))}else S.error=!0;this.responseXML&&!function(){let t=E.responseXML,o=this._layerEl.shadowRoot;if(t){var e=t.children[0].children[1].children;if(e){var i,n,a=t.children[0].children[0].querySelector("map-base")?.getAttribute("href");for(i of e){let e=i.cloneNode(!0);if(i._DOMnode=e,"map-link"===e.nodeName){let t=e.getAttribute("href")||e.getAttribute("tref");!t||0!==t.indexOf("http://")&&0!==t.indexOf("https://")||(n=a+t,e.hasAttribute("href")?e.setAttribute("href",n):e.setAttribute("tref",n))}o.appendChild(e)}}}}.call(S),S.fire("extentload",S,!1),S._layerEl.parentElement&&S._layerEl.parentElement._toggleControls(),S._layerEl.dispatchEvent(new CustomEvent("extentload",{detail:S}))}(this._href||t)&&((S=this)._href?(E=new XMLHttpRequest,e=this._href,o=i,E.onreadystatechange=function(){this.readyState===this.DONE&&(400!==this.status&&404!==this.status&&500!==this.status&&406!==this.status||(S.error=!0,S.fire("extentload",S,!0),E.abort()))},E.onload=o,E.onerror=function(){S.error=!0,S.fire("extentload",S,!0)},E.open("GET",e),E.setRequestHeader("Accept",M.mime),E.overrideMimeType("text/xml"),E.send()):t&&i.call(this,t))},_validateExtent:function(){if(this._extent&&this._map){var e,o=this._extent._mapExtents||[this._extent];for(let t=0;t<o.length;t++){if(!o[t].querySelector)return;o[t].querySelector('[type=zoom][min=""], [type=zoom][max=""]')&&((e=o[t].querySelector("[type=zoom]")).setAttribute("min",this._map.getMinZoom()),e.setAttribute("max",this._map.getMaxZoom())),(e=o[t].hasAttribute("units")?o[t].getAttribute("units"):null)&&M[e]?this._extent._mapExtents?this._extent._mapExtents[t].crs=M[e]:this._extent.crs=M[e]:this._extent._mapExtents?this._extent._mapExtents[t].crs=M.OSMTILE:this._extent.crs=M.OSMTILE}}},getProjection:function(){if(this._extent){let t=this._extent._mapExtents?this._extent._mapExtents[0]:this._extent;if(!t)return I;switch(t.tagName.toUpperCase()){case"MAP-EXTENT":if(t.hasAttribute("units"))return t.getAttribute("units").toUpperCase();break;case"MAP-INPUT":if(t.hasAttribute("value"))return t.getAttribute("value").toUpperCase();break;case"MAP-META":if(t.hasAttribute("content"))return M._metaContentToObject(t.getAttribute("content")).content.toUpperCase();break;default:return I}return I}},_parseLicenseAndLegend:function(t,e){var o,i=t.querySelector("map-link[rel=license]");i&&(o=i.getAttribute("title"),o='<a href="'+i.getAttribute("href")+'" title="'+o+'">'+o+"</a>"),L.setOptions(e,{attribution:o});t=t.querySelector("map-link[rel=legend]");t&&(e._legendUrl=t.getAttribute("href"))},getQueryTemplates:function(o){if(this._extent&&this._extent._queries){var i=[];if(this._layerEl.checked&&!this._layerEl.hidden&&this._mapmlLayerItem){var e=this._mapmlLayerItem.querySelectorAll(".mapml-layer-item-name");for(let t=0;t<e.length;t++)if(e[t].extent||1===this._extent._mapExtents.length){var n=e[t].extent||this._extent._mapExtents[0];for(let t=0;t<n._templateVars.length;t++)if(n.checked){var a=n._templateVars[t];for(let e=0;e<this._extent._queries.length;e++){let t=this._extent._queries[e];a===t&&t.extentBounds.contains(o)&&i.push(t)}}}return i}}},_attachSkipButtons:function(t){let n=t.popup,a=t.target,e,r,o=n._container.getElementsByClassName("mapml-popup-content")[0];n._container.setAttribute("role","dialog"),o.setAttribute("tabindex","-1"),o.setAttribute("role","document"),n._count=0,n._source._eventParents?(e=n._source._eventParents[Object.keys(n._source._eventParents)[0]],r=n._source.group,_.call(n)):(e=n._source._templatedLayer,a.on("attachZoomLink",_,n)),n._container.querySelector('nav[class="mapml-focus-buttons"]')&&(L.DomUtil.remove(n._container.querySelector('nav[class="mapml-focus-buttons"]')),L.DomUtil.remove(n._container.querySelector("hr")));var i=L.DomUtil.create("nav","mapml-focus-buttons");let s=L.DomUtil.create("button","mapml-popup-button",i);s.type="button",s.title="Focus Map",s.innerHTML="<span aria-hidden='true'>|&#10094;</span>",L.DomEvent.on(s,"click",t=>{L.DomEvent.stop(t),a.featureIndex._sortIndex(),a.closePopup(),a._container.focus()},n);let l=L.DomUtil.create("button","mapml-popup-button",i);l.type="button",l.title="Previous Feature",l.innerHTML="<span aria-hidden='true'>&#10094;</span>",L.DomEvent.on(l,"click",e._previousFeature,n);let m=L.DomUtil.create("p","mapml-feature-count",i),u=this._totalFeatureCount||1;m.innerText=n._count+1+"/"+u;let c=L.DomUtil.create("button","mapml-popup-button",i);c.type="button",c.title="Next Feature",c.innerHTML="<span aria-hidden='true'>&#10095;</span>",L.DomEvent.on(c,"click",e._nextFeature,n);let p=L.DomUtil.create("button","mapml-popup-button",i);p.type="button",p.title="Focus Controls",p.innerHTML="<span aria-hidden='true'>&#10095;|</span>",L.DomEvent.on(p,"click",t=>{a.featureIndex._sortIndex(),a.featureIndex.currentIndex=a.featureIndex.inBoundFeatures.length-1,a.featureIndex.inBoundFeatures[0].path.setAttribute("tabindex",-1),a.featureIndex.inBoundFeatures[a.featureIndex.currentIndex].path.setAttribute("tabindex",0),L.DomEvent.stop(t),a.closePopup(),a._controlContainer.querySelector("A:not([hidden])").focus()},n);t=L.DomUtil.create("hr","mapml-popup-divider");function h(t){let e=t.originalEvent.path||t.originalEvent.composedPath();var o=9===t.originalEvent.keyCode,i=t.originalEvent.shiftKey;(e[0].classList.contains("leaflet-popup-close-button")&&o&&!i||27===t.originalEvent.keyCode||e[0].classList.contains("leaflet-popup-close-button")&&13===t.originalEvent.keyCode||e[0].classList.contains("mapml-popup-content")&&o&&i||e[0]===n._content.querySelector("a")&&o&&i)&&setTimeout(()=>{a.closePopup(n),r.focus(),L.DomEvent.stop(t)},0)}function d(t){let e=t.originalEvent.path||t.originalEvent.composedPath();var o=9===t.originalEvent.keyCode,i=t.originalEvent.shiftKey;13===t.originalEvent.keyCode&&e[0].classList.contains("leaflet-popup-close-button")||27===t.originalEvent.keyCode?(L.DomEvent.stopPropagation(t),a.closePopup(n),a._container.focus(),27!==t.originalEvent.keyCode&&(a._popupClosed=!0)):o&&e[0].classList.contains("leaflet-popup-close-button")?a.closePopup(n):(e[0].classList.contains("mapml-popup-content")&&o&&i||e[0]===n._content.querySelector("a")&&o&&i)&&(a.closePopup(n),setTimeout(()=>{L.DomEvent.stop(t),a._container.focus()},0))}function _(e){let o=this._content,i=e?e.currFeature:this._source._groupLayer._featureEl;if(o.querySelector("a.mapml-zoom-link")&&o.querySelector("a.mapml-zoom-link").remove(),i.querySelector("map-geometry")){var n=i.extent.topLeft.gcrs,e=i.extent.bottomRight.gcrs,e=L.latLngBounds(L.latLng(n.horizontal,n.vertical),L.latLng(e.horizontal,e.vertical)).getCenter(!0);let t=document.createElement("a");t.href=`#${i.getMaxZoom()},${e.lng},`+e.lat,t.innerHTML=""+M.options.locale.popupZoom,t.className="mapml-zoom-link",t.onclick=t.onkeydown=function(t){(t instanceof MouseEvent||13===t.keyCode)&&(t.preventDefault(),i.zoomTo())},o.insertBefore(t,o.querySelector("hr.mapml-popup-divider"))}}n._navigationBar=i,n._content.appendChild(t),n._content.appendChild(i),o.focus(),r&&!M.options.featureIndexOverlayOption?(r.setAttribute("aria-expanded","true"),a.on("keydown",h)):a.on("keydown",d),a.on("popupclose",function t(e){e.popup===n&&(a.off("keydown",h),a.off("keydown",d),a.off("popupopen",_),a.off("popupclose",t),r&&r.setAttribute("aria-expanded","false"))})}}),b=L.Layer.extend({onAdd:function(t){var e=t.getSize();(400<e.x||300<e.y)&&(this._container=L.DomUtil.create("table","mapml-debug",t._container),this._panel=E({className:"mapml-debug-panel",pane:this._container}),t.addLayer(this._panel)),this._grid=A({className:"mapml-debug-grid",pane:t._panes.mapPane,zIndex:400,tileSize:t.options.crs.options.crs.tile.bounds.max.x}),t.addLayer(this._grid),this._vectors=S({className:"mapml-debug-vectors",pane:t._panes.mapPane,toolPane:this._container}),t.addLayer(this._vectors)},onRemove:function(t){t.removeLayer(this._grid),t.removeLayer(this._vectors),this._panel&&(t.removeLayer(this._panel),L.DomUtil.remove(this._container))}}),v=L.Layer.extend({initialize:function(t){L.setOptions(this,t)},onAdd:function(t){this._title=L.DomUtil.create("caption","mapml-debug-banner",this.options.pane),this._title.innerHTML="Debug mode",t.debug={},t.debug._infoContainer=this._debugContainer=L.DomUtil.create("tbody","mapml-debug-panel",this.options.pane);var e=t.debug._infoContainer;t.debug._tileCoord=L.DomUtil.create("tr","mapml-debug-coordinates",e),t.debug._tileMatrixCoord=L.DomUtil.create("tr","mapml-debug-coordinates",e),t.debug._mapCoord=L.DomUtil.create("tr","mapml-debug-coordinates",e),t.debug._tcrsCoord=L.DomUtil.create("tr","mapml-debug-coordinates",e),t.debug._pcrsCoord=L.DomUtil.create("tr","mapml-debug-coordinates",e),t.debug._gcrsCoord=L.DomUtil.create("tr","mapml-debug-coordinates",e),this._map.on("mousemove",this._updateCoords)},onRemove:function(){L.DomUtil.remove(this._title),this._debugContainer&&(L.DomUtil.remove(this._debugContainer),this._map.off("mousemove",this._updateCoords))},_updateCoords:function(s){if(!this.contextMenu._visible){let t=this.options.mapEl,e=t._map.project(s.latlng),o=t._map.options.crs.scale(+t.zoom),i=t._map.options.crs.transformation.untransform(e,o),n=t._map.options.crs.options.crs.tile.bounds.max.x,a=e.x%n,r=e.y%n;a<0&&(a+=n),r<0&&(r+=n),this.debug._tileCoord.innerHTML=`
      <th scope="row">tile: </th>
      <td>i: ${Math.trunc(a)}, </td>
      <td>j: ${Math.trunc(r)}</td>
      `,this.debug._mapCoord.innerHTML=`
      <th scope="row">map: </th>
      <td>i: ${Math.trunc(s.containerPoint.x)}, </td>
      <td>j: ${Math.trunc(s.containerPoint.y)}</td>
      `,this.debug._gcrsCoord.innerHTML=`
      <th scope="row">gcrs: </th>
      <td>lon: ${s.latlng.lng.toFixed(6)}, </td>
      <td>lat: ${s.latlng.lat.toFixed(6)}</td>
      `,this.debug._tcrsCoord.innerHTML=`
>>>>>>> 4af0ac91de ([GEOS-10940] Update MapML viewer to release 0.11.0)
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
<<<<<<< HEAD
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
=======
      <td>easting: ${i.x.toFixed(2)}, </td>
      <td>northing: ${i.y.toFixed(2)}</td>
      `}}}),E=function(t){return new v(t)},C=L.GridLayer.extend({initialize:function(t){L.setOptions(this,t),L.GridLayer.prototype.initialize.call(this,this._map)},createTile:function(t){let e=L.DomUtil.create("div","mapml-debug-tile");return e.setAttribute("col",t.x),e.setAttribute("row",t.y),e.setAttribute("zoom",t.z),e.innerHTML=["col: "+t.x,"row: "+t.y,"zoom: "+t.z].join(", "),e.style.outline="1px dashed red",e}}),A=function(t){return new C(t)},T=L.LayerGroup.extend({initialize:function(t){L.setOptions(this,t),L.LayerGroup.prototype.initialize.call(this,this._map,t)},onAdd:function(t){t.on("overlayremove",this._mapLayerUpdate,this),t.on("overlayadd",this._mapLayerUpdate,this);var e=t.options.crs.transformation.transform(L.point(0,0),t.options.crs.scale(0));this._centerVector=L.circle(t.options.crs.pointToLatLng(e,0),{radius:250}),this._centerVector.bindTooltip("Projection Center"),this._addBounds(t)},onRemove:function(t){this.clearLayers()},_addBounds:function(t){let e=Object.keys(t._layers),o=t._layers,i=["#FF5733","#8DFF33","#3397FF","#E433FF","#F3FF33"],n=0;this.addLayer(this._centerVector);for(var a of e)if(o[a].layerBounds||o[a].extentBounds){let t;t=o[a].layerBounds?[o[a].layerBounds.min,L.point(o[a].layerBounds.max.x,o[a].layerBounds.min.y),o[a].layerBounds.max,L.point(o[a].layerBounds.min.x,o[a].layerBounds.max.y)]:[o[a].extentBounds.min,L.point(o[a].extentBounds.max.x,o[a].extentBounds.min.y),o[a].extentBounds.max,L.point(o[a].extentBounds.min.x,o[a].extentBounds.max.y)];let e=z(t,{color:i[n%i.length],weight:2,opacity:1,fillOpacity:.01,fill:!0});o[a].options._leafletLayer&&e.bindTooltip(o[a].options._leafletLayer._title,{sticky:!0}),this.addLayer(e),n++}t.totalLayerBounds&&(t=[t.totalLayerBounds.min,L.point(t.totalLayerBounds.max.x,t.totalLayerBounds.min.y),t.totalLayerBounds.max,L.point(t.totalLayerBounds.min.x,t.totalLayerBounds.max.y)],t=z(t,{color:"#808080",weight:5,opacity:.5,fill:!1}),this.addLayer(t))},_mapLayerUpdate:function(t){this.clearLayers(),this._addBounds(t.target)}}),S=function(t){return new T(t)},w=L.Path.extend({getCenter:function(t){let e=this._map.options.crs;return e.unproject(L.bounds(this._locations).getCenter())},options:{className:"mapml-debug-extent"},initialize:function(t,e){this._locations=t,L.setOptions(this,e)},_project:function(){this._rings=[];let e=this._map.options.crs.scale(this._map.getZoom()),o=this._map;for(let t=0;t<this._locations.length;t++){var i=o.options.crs.transformation.transform(this._locations[t],e);this._rings.push(L.point(i.x,i.y)._subtract(o.getPixelOrigin()))}this._parts=[this._rings]},_update:function(){this._map&&this._renderer._updatePoly(this,!0)}}),z=function(t,e){return new w(t,e)},P=L.Handler.extend({addHooks:function(){L.setOptions(this,{mapEl:this._map.options.mapEl}),L.DomEvent.on(this._map,"click",this._queryTopLayer,this),L.DomEvent.on(this._map,"keypress",this._queryTopLayerAtMapCenter,this)},removeHooks:function(){L.DomEvent.off(this._map,"click",this._queryTopLayer,this),L.DomEvent.on(this._map,"keypress",this._queryTopLayerAtMapCenter,this)},_getTopQueryableLayer:function(){for(var t=this.options.mapEl.layers,e=t.length-1;0<=e;e--){var o=t[e]._layer;if(t[e].checked&&o.queryable)return o}},_queryTopLayerAtMapCenter:function(t){setTimeout(()=>{!this._map.isFocused||this._map._popupClosed||" "!==t.originalEvent.key&&13!=+t.originalEvent.keyCode?delete this._map._popupClosed:this._map.fire("click",{latlng:this._map.getCenter(),layerPoint:this._map.latLngToLayerPoint(this._map.getCenter()),containerPoint:this._map.latLngToContainerPoint(this._map.getCenter())})},0)},_queryTopLayer:function(t){var e=this._getTopQueryableLayer();e&&(e._mapmlFeatures&&delete e._mapmlFeatures,this._query(t,e))},_query(r,s){function e(t){return a.transformation.untransform(t,a.scale(n))}function o(t){return a.unproject(a.transformation.untransform(t,a.scale(n)),n)}function i(i,t,n){const a=new DOMParser;fetch(L.Util.template(i.template,t),{redirect:"follow"}).then(e=>{if(200<=e.status&&e.status<300)return e.text().then(t=>({contenttype:e.headers.get("Content-Type"),text:t}));throw new Error(e.status)}).then(o=>{if(s._mapmlFeatures||(s._mapmlFeatures=[]),o.contenttype.startsWith("text/mapml")){let t=a.parseFromString(o.text,"application/xml"),e=Array.prototype.slice.call(t.querySelectorAll("map-feature"));e.length&&(s._mapmlFeatures=s._mapmlFeatures.concat(e))}else{var t="<map-geometry cs='gcrs'><map-point><map-coordinates>"+r.latlng.lng+" "+r.latlng.lat+"</map-coordinates></map-point></map-geometry>",t=a.parseFromString("<map-feature><map-properties>"+o.text+"</map-properties>"+t+"</map-feature>","text/html").querySelector("map-feature");s._mapmlFeatures.push(t)}if(n){for(var e of s._mapmlFeatures)e._extentEl=i._extentEl;!function(t,e){let o=M.featureLayer(t,{renderer:M.featureRenderer(),pane:u,projection:l.options.projection,_leafletLayer:s,query:!0,static:!0});o.addTo(l);let i=L.DomUtil.create("div","mapml-popup-content"),n=L.DomUtil.create("iframe");n.style="border: none",n.srcdoc=t[0].querySelector("map-feature map-properties").innerHTML,n.setAttribute("sandbox","allow-same-origin allow-forms"),i.appendChild(n),s._totalFeatureCount=t.length,s.bindPopup(i,c).openPopup(e),s.on("popupclose",function(){l.removeLayer(o)}),o.showPaginationFeature({i:0,popup:s._popup})}(s._mapmlFeatures,r.latlng)}}).catch(t=>{console.log("Looks like there was a problem. Status: "+t.message)})}var n=r.target.getZoom(),l=this._map,a=s._extent.crs,m=l.options.crs.options.crs.tile.bounds.max.x,u=s._container,c={autoClose:!1,autoPan:!0,maxHeight:.5*l.getSize().y-50},p=a.latLngToPoint(r.latlng,n),h=p.divideBy(m).floor(),d=new L.Bounds(p.divideBy(m).floor().multiplyBy(m),p.divideBy(m).ceil().multiplyBy(m)),t=this._map.project(r.latlng),_=this._map.options.crs.scale(this._map.getZoom()),y=this._map.options.crs.transformation.untransform(t,_),f=s.getQueryTemplates(y);for(let t=0;t<f.length;t++){var g,x={},b=f[t];for(g in x[b.query.tilei]=p.x.toFixed()-h.x*m,x[b.query.tilej]=p.y.toFixed()-h.y*m,x[b.query.mapi]=l.getSize().divideBy(2).x.toFixed(),x[b.query.mapj]=l.getSize().divideBy(2).y.toFixed(),x[b.query.pixelleft]=a.pointToLatLng(p,n).lng,x[b.query.pixeltop]=a.pointToLatLng(p,n).lat,x[b.query.pixelright]=a.pointToLatLng(p.add([1,1]),n).lng,x[b.query.pixelbottom]=a.pointToLatLng(p.add([1,1]),n).lat,x[b.query.column]=h.x,x[b.query.row]=h.y,x[b.query.x]=p.x.toFixed(),x[b.query.y]=p.y.toFixed(),x[b.query.easting]=e(p).x,x[b.query.northing]=e(p).y,x[b.query.longitude]=o(p).lng,x[b.query.latitude]=o(p).lat,x[b.query.zoom]=n,x[b.query.width]=l.getSize().x,x[b.query.height]=l.getSize().y,x[b.query.mapbottom]=e(p.add(l.getSize().divideBy(2))).y,x[b.query.mapleft]=e(p.subtract(l.getSize().divideBy(2))).x,x[b.query.maptop]=e(p.subtract(l.getSize().divideBy(2))).y,x[b.query.mapright]=e(p.add(l.getSize().divideBy(2))).x,x[b.query.tilebottom]=e(d.max).y,x[b.query.tileleft]=e(d.min).x,x[b.query.tiletop]=e(d.min).y,x[b.query.tileright]=e(d.max).x,b.query)["mapi","mapj","tilei","tilej","row","col","x","y","easting","northing","longitude","latitude","width","height","zoom","mapleft","mapright",",maptop","mapbottom","tileleft","tileright","tiletop","tilebottom","pixeltop","pixelbottom","pixelleft","pixelright"].indexOf(g)<0&&(x[g]=b.query[g]);b.extentBounds.contains(y)&&i(b,x,t===f.length-1)}}}),B=L.Handler.extend({_touchstart:L.Browser.msPointer?"MSPointerDown":L.Browser.pointer?"pointerdown":"touchstart",initialize:function(t){L.Handler.prototype.initialize.call(this,t),this.activeIndex=0,this.excludedIndices=[4,7],this.isRunned=!1,this._items=[{text:M.options.locale.cmBack+" (<kbd>Alt+Left Arrow</kbd>)",callback:this._goBack},{text:M.options.locale.cmForward+" (<kbd>Alt+Right Arrow</kbd>)",callback:this._goForward},{text:M.options.locale.cmReload+" (<kbd>Ctrl+R</kbd>)",callback:this._reload},{text:M.options.locale.btnFullScreen+" (<kbd>F</kbd>)",callback:this._toggleFullScreen},{spacer:"-"},{text:M.options.locale.cmCopyCoords+" (<kbd>C</kbd>)<span></span>",callback:this._copyCoords,hideOnSelect:!1,popup:!0,submenu:[{text:M.options.locale.cmCopyMapML,callback:this._copyMapML},{text:M.options.locale.cmCopyExtent,callback:this._copyExtent},{text:M.options.locale.cmCopyLocation,callback:this._copyLocation}]},{text:M.options.locale.cmPasteLayer+" (<kbd>P</kbd>)",callback:this._paste},{spacer:"-"},{text:M.options.locale.cmToggleControls+" (<kbd>T</kbd>)",callback:this._toggleControls},{text:M.options.locale.cmToggleDebug+" (<kbd>D</kbd>)",callback:this._toggleDebug},{text:M.options.locale.cmViewSource+" (<kbd>V</kbd>)",callback:this._viewSource}],this.defExtCS=M.options.defaultExtCoor,this.defLocCS=M.options.defaultLocCoor,this._layerItems=[{text:M.options.locale.lmZoomToLayer+" (<kbd>Z</kbd>)",callback:this._zoomToLayer},{text:M.options.locale.lmCopyLayer+" (<kbd>L</kbd>)",callback:this._copyLayer}],this._mapMenuVisible=!1,this._keyboardEvent=!1,this._container=L.DomUtil.create("div","mapml-contextmenu",t._container),this._container.setAttribute("hidden","");for(let t=0;t<6;t++)this._items[t].el=this._createItem(this._container,this._items[t]);this._coordMenu=L.DomUtil.create("div","mapml-contextmenu mapml-submenu",this._container),this._coordMenu.id="mapml-copy-submenu",this._coordMenu.setAttribute("hidden",""),this._clickEvent=null;for(let t=0;t<this._items[5].submenu.length;t++)this._createItem(this._coordMenu,this._items[5].submenu[t],t);this._items[6].el=this._createItem(this._container,this._items[6]),this._items[7].el=this._createItem(this._container,this._items[7]),this._items[8].el=this._createItem(this._container,this._items[8]),this._items[9].el=this._createItem(this._container,this._items[9]),this._items[10].el=this._createItem(this._container,this._items[10]),this._layerMenu=L.DomUtil.create("div","mapml-contextmenu mapml-layer-menu",t._container),this._layerMenu.setAttribute("hidden","");for(let t=0;t<this._layerItems.length;t++)this._createItem(this._layerMenu,this._layerItems[t]);L.DomEvent.on(this._container,"click",L.DomEvent.stop).on(this._container,"mousedown",L.DomEvent.stop).on(this._container,"dblclick",L.DomEvent.stop).on(this._container,"contextmenu",L.DomEvent.stop).on(this._layerMenu,"click",L.DomEvent.stop).on(this._layerMenu,"mousedown",L.DomEvent.stop).on(this._layerMenu,"dblclick",L.DomEvent.stop).on(this._layerMenu,"contextmenu",L.DomEvent.stop),this.t=document.createElement("template"),this.t.innerHTML=`<map-feature zoom="">
        <map-featurecaption></map-featurecaption>
        <map-properties>
            <h2></h2>
            <div style="text-align:center"></div>
        </map-properties>
        <map-geometry cs="">
          <map-point>
            <map-coordinates></map-coordinates>
          </map-point>
        </map-geometry>
      </map-feature>`},addHooks:function(){var t=this._map.getContainer();L.DomEvent.on(t,"mouseleave",this._hide,this).on(document,"keydown",this._onKeyDown,this),L.Browser.touch&&L.DomEvent.on(document,this._touchstart,this._hide,this),this._map.on({contextmenu:this._show,mousedown:this._hide,zoomstart:this._hide},this)},removeHooks:function(){var t=this._map.getContainer();L.DomEvent.off(t,"mouseleave",this._hide,this).off(document,"keydown",this._onKeyDown,this),L.Browser.touch&&L.DomEvent.off(document,this._touchstart,this._hide,this),this._map.off({contextmenu:this._show,mousedown:this._hide,zoomstart:this._hide},this)},_updateCS:function(){this.defExtCS===M.options.defaultExtCoor&&this.defLocCS===M.options.defaultLocCoor||(this.defExtCS=M.options.defaultExtCoor,this.defLocCS=M.options.defaultLocCoor)},_copyExtent:function(t){let e=(t instanceof KeyboardEvent?this._map:this).contextMenu,o=e.defExtCS?e.defExtCS.toLowerCase():"pcrs",i=(t instanceof KeyboardEvent?this:this.options.mapEl).extent.topLeft[o],n=(t instanceof KeyboardEvent?this:this.options.mapEl).extent.bottomRight[o],a="";"pcrs"===o?a=`<map-meta name="extent" content="top-left-easting=${Math.round(i.horizontal)}, top-left-northing=${Math.round(i.vertical)}, bottom-right-easting=${Math.round(n.horizontal)}, bottom-right-northing=${Math.round(n.vertical)}"></map-meta>`:"gcrs"===o?a=`<map-meta name="extent" content="top-left-longitude=${i.horizontal}, top-left-latitude=${i.vertical}, bottom-right-longitude=${n.horizontal}, bottom-right-latitude=${n.vertical}"></map-meta>`:"tcrs"===o?a=`<map-meta name="extent" content="top-left-x=${i[0].horizontal}, top-left-y=${i[0].vertical}, bottom-right-x=${n[n.length-1].horizontal}, bottom-right-y=${n[n.length-1].vertical}"></map-meta>`:"tilematrix"===o?a=`<map-meta name="extent" content="top-left-column=${i[0].horizontal}, top-left-row=${i[0].vertical}, bottom-right-column=${n[n.length-1].horizontal}, bottom-right-row=${n[n.length-1].vertical}"></map-meta>`:console.log("not support"),e._copyData(a)},_zoomToLayer:function(t){let e=(t instanceof KeyboardEvent?this._map:this).contextMenu;e._layerClicked.layer._layerEl.zoomTo()},_copyLayer:function(t){let e=(t instanceof KeyboardEvent?this._map:this).contextMenu,o=e._layerClicked.layer._layerEl;e._copyData(o.getOuterHTML())},_goForward:function(t){let e=(t instanceof KeyboardEvent?this._map:this).options.mapEl;e.forward()},_goBack:function(t){let e=(t instanceof KeyboardEvent?this._map:this).options.mapEl;e.back()},_reload:function(t){let e=(t instanceof KeyboardEvent?this._map:this).options.mapEl;e.reload()},_toggleFullScreen:function(t){let e=(t instanceof KeyboardEvent?this._map:this).options.mapEl;e._toggleFullScreen()},_toggleControls:function(t){let e=(t instanceof KeyboardEvent?this._map:this).options.mapEl;e.controls?e.controls=!1:e.controls=!0},_copyMapML:function(t){let e=(t instanceof KeyboardEvent?this._map:this).contextMenu,o=(t instanceof KeyboardEvent?this._map:this).options.mapEl;e._copyData(o.outerHTML.replace(/<div class="mapml-web-map">.*?<\/div>|<style>\[is="web-map"].*?<\/style>|<style>mapml-viewer.*?<\/style>/gm,""))},_paste:function(t){(t instanceof KeyboardEvent?this._map:this).contextMenu;let e=(t instanceof KeyboardEvent?this._map:this).options.mapEl;navigator.clipboard.readText().then(t=>{M._pasteLayer(e,t)})},_viewSource:function(t){let e=(t instanceof KeyboardEvent?this._map:this).options.mapEl;e.viewSource()},_toggleDebug:function(t){let e=(t instanceof KeyboardEvent?this._map:this).options.mapEl;e.toggleDebug()},_copyCoords:function(t){let e=this.contextMenu||this;e._showCoordMenu(t)},_copyData:function(t){const e=document.createElement("textarea");e.value=t,document.body.appendChild(e),e.select(),document.execCommand("copy"),document.body.removeChild(e)},_copyLocation:function(t){const e=this.contextMenu;switch(e.defLocCS.toLowerCase()){case"tile":e._copyTile.call(this,t);break;case"tilematrix":e._copyTileMatrix.call(this,t);break;case"map":e._copyMap.call(this,t);break;case"tcrs":e._copyTCRS.call(this,t);break;case"pcrs":e._copyPCRS.call(this,t);break;default:e._copyGCRS.call(this,t)}},_copyGCRS:function(t){let e=this.options.mapEl,o=this.contextMenu._clickEvent,i=e.projection,n=this.contextMenu.t.content.firstElementChild.cloneNode(!0),a=n.querySelector("map-featurecaption"),r=n.querySelector("h2"),s=n.querySelector("div"),l=n.querySelector("map-geometry"),m=n.querySelector("map-coordinates");n.setAttribute("zoom",e.zoom),l.setAttribute("cs","gcrs"),a.textContent=`Copied ${i} gcrs location`,r.textContent=`Copied ${i} gcrs location`,s.textContent=o.latlng.lng.toFixed(6)+" "+o.latlng.lat.toFixed(6),m.textContent=o.latlng.lng.toFixed(6)+" "+o.latlng.lat.toFixed(6),this.contextMenu._copyData(n.outerHTML)},_copyTCRS:function(t){let e=this.options.mapEl,o=this.contextMenu._clickEvent,i=e._map.project(o.latlng),n=i.x.toFixed(),a=i.y.toFixed(),r=e.projection,s=this.contextMenu.t.content.firstElementChild.cloneNode(!0),l=s.querySelector("map-featurecaption"),m=s.querySelector("h2"),u=s.querySelector("div"),c=s.querySelector("map-geometry"),p=s.querySelector("map-coordinates");s.setAttribute("zoom",e.zoom),c.setAttribute("cs","tcrs"),l.textContent=`Copied ${r} tcrs location`,m.textContent=`Copied ${r} tcrs location`,u.textContent=n+" "+a,p.textContent=n+" "+a,this.contextMenu._copyData(s.outerHTML)},_copyTileMatrix:function(t){let e=this.options.mapEl,o=this.contextMenu._clickEvent,i=e._map.project(o.latlng),n=e._map.options.crs.options.crs.tile.bounds.max.x,a=e.projection,r=this.contextMenu.t.content.firstElementChild.cloneNode(!0),s=r.querySelector("map-featurecaption"),l=r.querySelector("h2"),m=r.querySelector("div"),u=r.querySelector("map-geometry"),c=r.querySelector("map-coordinates");r.setAttribute("zoom",e.zoom),u.setAttribute("cs","gcrs"),s.textContent=`Copied ${a} tilematrix location (not implemented yet)`,l.textContent=`Copied ${a} tilematrix location (not implemented yet)`,m.textContent=Math.trunc(i.x/n)+" "+Math.trunc(i.y/n),c.textContent=o.latlng.lng.toFixed(6)+" "+o.latlng.lat.toFixed(6),this.contextMenu._copyData(r.outerHTML)},_copyPCRS:function(t){let e=this.options.mapEl,o=this.contextMenu._clickEvent,i=e._map.project(o.latlng),n=e._map.options.crs.scale(+e.zoom),a=e._map.options.crs.transformation.untransform(i,n).round(),r=e.projection,s=this.contextMenu.t.content.firstElementChild.cloneNode(!0),l=s.querySelector("map-featurecaption"),m=s.querySelector("h2"),u=s.querySelector("div"),c=s.querySelector("map-geometry"),p=s.querySelector("map-coordinates");s.setAttribute("zoom",e.zoom),c.setAttribute("cs","pcrs"),l.textContent=`Copied ${r} pcrs location`,m.textContent=`Copied ${r} pcrs location`,u.textContent=a.x+" "+a.y,p.textContent=a.x+" "+a.y,this.contextMenu._copyData(s.outerHTML)},_copyTile:function(t){let e=this.options.mapEl,o=this.contextMenu._clickEvent,i=e._map.project(o.latlng),n=e._map.options.crs.options.crs.tile.bounds.max.x,a=i.x%n,r=i.y%n,s=L.point(a,r).trunc(),l=e.projection,m=this.contextMenu.t.content.firstElementChild.cloneNode(!0),u=m.querySelector("map-featurecaption"),c=m.querySelector("h2"),p=m.querySelector("div"),h=m.querySelector("map-geometry"),d=m.querySelector("map-coordinates");s.x<0&&(s.x+=n),s.y<0&&(s.y+=n),m.setAttribute("zoom",e.zoom),h.setAttribute("cs","gcrs"),u.textContent=`Copied ${l} tile location (not implemented yet)`,c.textContent=`Copied ${l} tile location (not implemented yet)`,p.textContent=s.x+" "+s.y,d.textContent=o.latlng.lng.toFixed(6)+" "+o.latlng.lat.toFixed(6),this.contextMenu._copyData(m.outerHTML)},_copyMap:function(t){let e=this.options.mapEl,o=this.contextMenu._clickEvent,i=o.containerPoint.trunc(),n=e.projection,a=this.contextMenu.t.content.firstElementChild.cloneNode(!0),r=a.querySelector("map-featurecaption"),s=a.querySelector("h2"),l=a.querySelector("div"),m=a.querySelector("map-geometry"),u=a.querySelector("map-coordinates");a.setAttribute("zoom",e.zoom),m.setAttribute("cs","gcrs"),r.textContent=`Copied ${n} map location (not implemented yet)`,s.textContent=`Copied ${n} map location (not implemented yet)`,l.textContent=i.x+" "+i.y,u.textContent=o.latlng.lng.toFixed(6)+" "+o.latlng.lat.toFixed(6),this.contextMenu._copyData(a.outerHTML)},_copyAllCoords:function(t){let e=this.options.mapEl,o=this.contextMenu._clickEvent,i=e._map.project(o.latlng),n=e._map.options.crs.options.crs.tile.bounds.max.x,a=i.x%n,r=i.y%n,s=e._map.options.crs.scale(+e.zoom),l=e._map.options.crs.transformation.untransform(i,s);var m=`z:${e.zoom}
`;m+=`tile: i:${Math.trunc(a)}, j:${Math.trunc(r)}
`,m+=`tilematrix: column:${Math.trunc(i.x/n)}, row:${Math.trunc(i.y/n)}
`,m+=`map: i:${Math.trunc(o.containerPoint.x)}, j:${Math.trunc(o.containerPoint.y)}
`,m+=`tcrs: x:${Math.trunc(i.x)}, y:${Math.trunc(i.y)}
`,m+=`pcrs: easting:${l.x.toFixed(2)}, northing:${l.y.toFixed(2)}
`,m+=`gcrs: lon :${o.latlng.lng.toFixed(6)}, lat:`+o.latlng.lat.toFixed(6),this.contextMenu._copyData(m)},_createItem:function(t,e,o){if(e.spacer)return this._createSeparator(t,o);t=this._insertElementAt("button","mapml-contextmenu-item",t,o),o=this._createEventHandler(t,e.callback,e.context,e.hideOnSelect);return t.innerHTML=""+e.text,t.setAttribute("type","button"),t.classList.add("mapml-button"),e.popup&&(t.setAttribute("aria-haspopup","true"),t.setAttribute("aria-expanded","false"),t.setAttribute("aria-controls","mapml-copy-submenu")),L.DomEvent.on(t,"mouseover",this._onItemMouseOver,this).on(t,"mouseout",this._onItemMouseOut,this).on(t,"mousedown",L.DomEvent.stopPropagation).on(t,"click",o),L.Browser.touch&&L.DomEvent.on(t,this._touchstart,L.DomEvent.stopPropagation),L.Browser.pointer||L.DomEvent.on(t,"click",this._onItemMouseOut,this),{id:L.Util.stamp(t),el:t,callback:o}},_createSeparator:function(t,e){e=this._insertElementAt("div","mapml-contextmenu-separator",t,e);return{id:L.Util.stamp(e),el:e}},_createEventHandler:function(s,l,m,u){let c=this;return u=void 0===u||u,function(t){let e=c._map,o=c._showLocation.containerPoint,i=e.containerPointToLayerPoint(o),n=e.layerPointToLatLng(i),a=c._showLocation.relatedTarget,r={containerPoint:o,layerPoint:i,latlng:n,relatedTarget:a};u&&c._hide(),l&&l.call(m||e,r),c._map.fire("contextmenu.select",{contextmenu:c,el:s})}},_insertElementAt:function(t,e,o,i){let n,a=document.createElement(t);return a.className=e,void 0!==i&&(n=o.children[i]),n?o.insertBefore(a,n):o.appendChild(a),a},_show:function(t){this._mapMenuVisible&&this._hide();let e=(this._clickEvent=t).originalEvent.target;if(e.closest("fieldset")){if(e=e.closest("fieldset"),e=("mapml-layer-extent"===e.className?e.closest("fieldset").parentNode.parentNode.parentNode:e).querySelector("span"),!e.layer.validProjection)return;this._layerClicked=e,this._layerMenu.removeAttribute("hidden"),this._showAtPoint(t.containerPoint,t,this._layerMenu)}else{var o;(e.classList.contains("leaflet-container")||e.classList.contains("mapml-debug-extent")||"path"===e.tagName)&&(o=this._map.options.mapEl.layers,this._layerClicked=Array.from(o).find(t=>t.checked),this._container.removeAttribute("hidden"),this._showAtPoint(t.containerPoint,t,this._container),this._updateCS())}0!==t.originalEvent.button&&-1!==t.originalEvent.button||(this._keyboardEvent=!0,this._layerClicked.className.includes("mapml-layer-item")?(t=document.activeElement,this._elementInFocus=t.shadowRoot.activeElement,this._layerMenuTabs=1,this._layerMenu.firstChild.focus()):this._container.querySelectorAll("button:not([disabled])")[0].focus())},_showAtPoint:function(t,e,o){var i;this._items.length&&(i=L.extend(e||{},{contextmenu:this}),this._showLocation={containerPoint:t},e&&e.relatedTarget&&(this._showLocation.relatedTarget=e.relatedTarget),this._setPosition(t,o),this._mapMenuVisible||(o.removeAttribute("hidden"),this._mapMenuVisible=!0),this._map.fire("contextmenu.show",i))},_hide:function(){this._mapMenuVisible&&(this._mapMenuVisible=!1,this._container.setAttribute("hidden",""),this._coordMenu.setAttribute("hidden",""),this._layerMenu.setAttribute("hidden",""),this._map.fire("contextmenu.hide",{contextmenu:this}),setTimeout(()=>this._map._container.focus(),0),this.activeIndex=0,this.isRunned=!1)},_setPosition:function(t,e){var o,i=this._map.getSize(),n=this._getElementSize(e);this._map.options.contextmenuAnchor&&(o=L.point(this._map.options.contextmenuAnchor),t=t.add(o)),(e._leaflet_pos=t).x+n.x>i.x?(e.style.left="auto",e.style.right=Math.min(Math.max(i.x-t.x,0),i.x-n.x-1)+"px"):(e.style.left=Math.max(t.x,0)+"px",e.style.right="auto"),t.y+n.y>i.y?(e.style.top="auto",e.style.bottom=Math.min(Math.max(i.y-t.y,0),i.y-n.y-1)+"px"):(e.style.top=Math.max(t.y,0)+"px",e.style.bottom="auto")},_getElementSize:function(t){let e=this._size;return e&&!this._sizeChanged||(e={},t.style.left="-999999px",t.style.right="auto",e.x=t.offsetWidth,e.y=t.offsetHeight,t.style.left="auto",this._sizeChanged=!1),e},_focusOnLayerControl:function(){this._mapMenuVisible=!1,delete this._layerMenuTabs,this._layerMenu.setAttribute("hidden",""),(this._elementInFocus||this._layerClicked.parentElement.firstChild).focus(),delete this._elementInFocus},_setActiveItem:function(o){if(null===document.activeElement.shadowRoot&&!0===this.noActiveEl&&(this.noActiveEl=!1,this._items[9].el.el.focus()),document.activeElement.shadowRoot.activeElement.innerHTML===this._items[o].el.el.innerHTML){let t=o+1;for(;this._items[t].el.el.disabled;)t++,t>=this._items.length&&(t=0);this._setActiveItem(t)}else if(this.excludedIndices.includes(o)){let t=o+1,e=o-1;for(;this.excludedIndices.includes(t)||this._items[t].el.el.disabled;)t++,t>=this._items.length&&(t=0);for(;this.excludedIndices.includes(e)||this._items[e].el.el.disabled;)e--,e<0&&(e=this._items.length-1);this.activeIndex<o?this._setActiveItem(t):this._setActiveItem(e)}else this._items[o].el.el.focus(),this.activeIndex=o},_onKeyDown:function(t){if(this._mapMenuVisible){var e=t.keyCode,o=t.path||t.composedPath();if(13===e&&t.preventDefault(),!this._layerMenuTabs||9!==e&&27!==e)if(38===e)if(this._coordMenu.hasAttribute("hidden")||null!==document.activeElement.shadowRoot&&document.activeElement.shadowRoot.activeElement.innerHTML!==this._coordMenu.children[0].innerHTML)if(this._coordMenu.hasAttribute("hidden")||document.activeElement.shadowRoot.activeElement.innerHTML!==this._coordMenu.children[1].innerHTML)if(this._coordMenu.hasAttribute("hidden")||document.activeElement.shadowRoot.activeElement.innerHTML!==this._coordMenu.children[2].innerHTML)if(this._layerMenu.hasAttribute("hidden")||document.activeElement.shadowRoot.activeElement.innerHTML!==this._layerMenu.children[0].innerHTML)if(this._layerMenu.hasAttribute("hidden"))if(0<this.activeIndex){let t=this.activeIndex-1;for(;this._items[t].el.el.disabled;)t--,t<0&&(t=this._items.length-1);this._setActiveItem(t)}else this._setActiveItem(this._items.length-1);else this._layerMenu.children[0].focus();else this._layerMenu.children[1].focus();else this._coordMenu.children[1].focus();else this._coordMenu.children[0].focus();else this._coordMenu.children[2].focus();else if(40===e)if(this._coordMenu.hasAttribute("hidden")||null!==document.activeElement.shadowRoot&&document.activeElement.shadowRoot.activeElement.innerHTML!==this._coordMenu.children[2].innerHTML)if(this._coordMenu.hasAttribute("hidden")||document.activeElement.shadowRoot.activeElement.innerHTML!==this._coordMenu.children[1].innerHTML)if(this._coordMenu.hasAttribute("hidden")||document.activeElement.shadowRoot.activeElement.innerHTML!==this._coordMenu.children[0].innerHTML)if(this._layerMenu.hasAttribute("hidden")||document.activeElement.shadowRoot.activeElement.innerHTML!==this._layerMenu.children[0].innerHTML)if(this._layerMenu.hasAttribute("hidden"))if(this.activeIndex<this._items.length-1)if(this.isRunned||0!==this.activeIndex||this._items[this.activeIndex].el.el.disabled){let t=this.activeIndex+1;for(;this._items[t].el.el.disabled;)t++,t>=this._items.length&&(t=0);this._setActiveItem(t)}else this._setActiveItem(0),this.isRunned=!0;else{let t=0;for(;this._items[t].el.el.disabled;)t++,t>=this._items.length&&(t=0);this._setActiveItem(t)}else this._layerMenu.children[0].focus();else this._layerMenu.children[1].focus();else this._coordMenu.children[1].focus();else this._coordMenu.children[2].focus();else this._coordMenu.children[0].focus();else 39===e?null!==document.activeElement.shadowRoot&&document.activeElement.shadowRoot.activeElement.innerHTML===this._items[5].el.el.innerHTML&&this._coordMenu.hasAttribute("hidden")?(this._showCoordMenu(),this._coordMenu.children[0].focus()):document.activeElement.shadowRoot.activeElement.innerHTML!==this._items[5].el.el.innerHTML||this._coordMenu.hasAttribute("hidden")||this._coordMenu.children[0].focus():37===e?this._coordMenu.hasAttribute("hidden")||null===document.activeElement.shadowRoot||document.activeElement.shadowRoot.activeElement.innerHTML!==this._coordMenu.children[0].innerHTML&&document.activeElement.shadowRoot.activeElement.innerHTML!==this._coordMenu.children[1].innerHTML&&document.activeElement.shadowRoot.activeElement.innerHTML!==this._coordMenu.children[2].innerHTML||(this._coordMenu.setAttribute("hidden",""),this._setActiveItem(5)):27===e?null===document.activeElement.shadowRoot||this._coordMenu.hasAttribute("hidden")?this._hide():document.activeElement.shadowRoot.activeElement.innerHTML!==this._coordMenu.children[0].innerHTML&&document.activeElement.shadowRoot.activeElement.innerHTML!==this._coordMenu.children[1].innerHTML&&document.activeElement.shadowRoot.activeElement.innerHTML!==this._coordMenu.children[2].innerHTML||(this._coordMenu.setAttribute("hidden",""),this._setActiveItem(5)):16===e||9===e||!this._layerClicked.className.includes("mapml-layer-item")&&67===e||o[0].innerText===M.options.locale.cmCopyCoords+" (C)"||this._hide();else t.shiftKey?--this._layerMenuTabs:this._layerMenuTabs+=1,0!==this._layerMenuTabs&&3!==this._layerMenuTabs&&27!==e||(L.DomEvent.stop(t),this._focusOnLayerControl());switch(e){case 13:document.activeElement.shadowRoot.activeElement.innerHTML===this._items[5].el.el.innerHTML?(this._copyCoords({latlng:this._map.getCenter()}),this._coordMenu.firstChild.focus()):this._map._container.parentNode.activeElement.parentNode.classList.contains("mapml-contextmenu")&&this._map._container.parentNode.activeElement.click();break;case 32:this._map._container.parentNode.activeElement.parentNode.classList.contains("mapml-contextmenu")&&this._map._container.parentNode.activeElement.click();break;case 67:this._copyCoords({latlng:this._map.getCenter()}),this._coordMenu.firstChild.focus();break;case 68:this._toggleDebug(t);break;case 77:this._copyMapML(t);break;case 76:this._layerClicked.className.includes("mapml-layer-item")&&this._copyLayer(t);break;case 70:this._toggleFullScreen(t);break;case 80:this._paste(t);break;case 84:this._toggleControls(t);break;case 86:this._viewSource(t);break;case 90:this._layerClicked.className.includes("mapml-layer-item")&&this._zoomToLayer(t)}}},_showCoordMenu:function(t){let e=this._map.getSize(),o=this._clickEvent,i=this._coordMenu,n=this._items[5].el.el;n.setAttribute("aria-expanded","true"),i.removeAttribute("hidden");var a=this._container.offsetWidth,r=(this._container.offsetHeight,i.offsetWidth);o.containerPoint.x+a+r>e.x?(i.style.left="auto",i.style.right=a+"px"):(i.style.left=a+"px",i.style.right="auto"),i.style.top="78px",i.style.bottom="auto"},_hideCoordMenu:function(t){if(t.relatedTarget&&t.relatedTarget.parentElement&&!t.relatedTarget.parentElement.classList.contains("mapml-submenu")&&!t.relatedTarget.classList.contains("mapml-submenu")){let t=this._coordMenu,e=this._items[4].el.el;e.setAttribute("aria-expanded","false"),t.setAttribute("hidden",""),this.noActiveEl=!0}},_onItemMouseOver:function(t){L.DomUtil.addClass(t.target||t.srcElement,"over"),t.srcElement.innerText===M.options.locale.cmCopyCoords+" (C)"&&this._showCoordMenu(t)},_onItemMouseOut:function(t){L.DomUtil.removeClass(t.target||t.srcElement,"over"),this._hideCoordMenu(t)},toggleContextMenuItem:function(t,e){t=t.toUpperCase(),"disabled"===e?"CONTROLS"===t?this._items[8].el.el.disabled=!0:"BACK"===t?this._items[0].el.el.disabled=!0:"FORWARD"===t?this._items[1].el.el.disabled=!0:"RELOAD"===t&&(this._items[2].el.el.disabled=!0):"enabled"===e&&("CONTROLS"===t?this._items[8].el.el.disabled=!1:"BACK"===t?this._items[0].el.el.disabled=!1:"FORWARD"===t?this._items[1].el.el.disabled=!1:"RELOAD"===t&&(this._items[2].el.el.disabled=!1))},setViewFullScreenInnerHTML:function(t){"view"===t?this._map.contextMenu._items[3].el.el.innerHTML=M.options.locale.btnFullScreen+" (<kbd>F</kbd>)":"exit"===t&&(this._map.contextMenu._items[3].el.el.innerHTML=M.options.locale.btnExitFullScreen+" (<kbd>F</kbd>)")}}),N=function(e,o){if(!e||!o)return{};let i=[],n=[],a=[],r=[],s=o.options.crs.options.crs.tile.bounds.max.y;for(let t=0;t<o.options.crs.options.resolutions.length;t++){var l=o.options.crs.scale(t),m=o.options.crs.transformation.transform(e.min,l),l=o.options.crs.transformation.transform(e.max,l);i.push({horizontal:m.x,vertical:l.y}),n.push({horizontal:l.x,vertical:m.y}),a.push({horizontal:i[t].horizontal/s,vertical:i[t].vertical/s}),r.push({horizontal:n[t].horizontal/s,vertical:n[t].vertical/s})}var t=o.options.crs.unproject(e.min),u=o.options.crs.unproject(e.max);let c={horizontal:t.lng,vertical:u.lat},p={horizontal:u.lng,vertical:t.lat},h={horizontal:e.min.x,vertical:e.max.y},d={horizontal:e.max.x,vertical:e.min.y};return{topLeft:{tcrs:i,tilematrix:a,gcrs:c,pcrs:h},bottomRight:{tcrs:n,tilematrix:r,gcrs:p,pcrs:d},projection:o.options.projection}},F=function(u){if(u){let e=u.values,t=u.projection||I,o=0,i=d,n=this[t].options.crs.tilematrix.bounds(0),a=this[t].options.resolutions.length-1,r=0,s=a,l=!1,m=0;for(let t=0;t<e.length;t++)switch(e[t].getAttribute("type")){case"zoom":r=+(e[t].hasAttribute("min")&&!isNaN(+e[t].getAttribute("min"))?e[t].getAttribute("min"):0),s=+(e[t].hasAttribute("max")&&!isNaN(+e[t].getAttribute("max"))?e[t].getAttribute("max"):a),o=+e[t].getAttribute("value");break;case"location":if(!e[t].getAttribute("max")||!e[t].getAttribute("min"))continue;var c=+e[t].getAttribute("max"),p=+e[t].getAttribute("min");switch(e[t].getAttribute("axis").toLowerCase()){case"x":case"longitude":case"column":case"easting":i=M.axisToCS(e[t].getAttribute("axis").toLowerCase()),n.min.x=p,n.max.x=c,m++;break;case"y":case"latitude":case"row":case"northing":i=M.axisToCS(e[t].getAttribute("axis").toLowerCase()),n.min.y=p,n.max.y=c,m++}}2<=m&&(l=!0);var h={minZoom:u.zoomBounds?.min&&!isNaN(+u.zoomBounds.min)?+u.zoomBounds.min:0,maxZoom:u.zoomBounds?.max&&!isNaN(+u.zoomBounds.max)?+u.zoomBounds.max:a,minNativeZoom:r,maxNativeZoom:s};return n=!l&&u.extentPCRSFallback&&u.extentPCRSFallback.bounds?u.extentPCRSFallback.bounds:l?this.boundsToPCRSBounds(n,o,t,i):this[t].options.crs.pcrs.bounds,{zoomBounds:h,bounds:n}}},q=function(t){try{switch(t.toLowerCase()){case"row":case"column":return"TILEMATRIX";case"i":case"j":return["MAP","TILE"];case"x":case"y":return"TCRS";case"latitude":case"longitude":return"GCRS";case"northing":case"easting":return"PCRS";default:return d}}catch(t){return}},O=function(t){try{switch(t.toLowerCase()){case"tilematrix":return["column","row"];case"map":case"tile":return["i","j"];case"tcrs":return["x","y"];case"gcrs":return["longitude","latitude"];case"pcrs":return["easting","northing"]}}catch(t){return}},D=function(t){try{switch(t.toLowerCase()){case"i":case"column":case"longitude":case"x":case"easting":return"x";case"row":case"j":case"latitude":case"y":case"northing":return"y";default:return}}catch(t){return}},R=function(t,e,o,i){if(t&&(e||0===e)&&Number.isFinite(+e)&&o&&i)switch(o="string"==typeof o?M[o]:o,i.toUpperCase()){case"PCRS":return t;case"TCRS":case"TILEMATRIX":var n=o.transformation.transform(t.min,o.scale(+e)),a=o.transformation.transform(t.max,o.scale(+e));if("TCRS"===i.toUpperCase())return L.bounds(n,a);var r=o.options.crs.tile.bounds.max.x;return L.bounds(L.point(n.x/r,n.y/r),L.point(a.x/r,a.y/r));case"GCRS":a=o.unproject(t.min),r=o.unproject(t.max);return L.bounds(L.point(a.lng,a.lat),L.point(r.lng,r.lat));default:return}},U=function(t,e,o,i){if(t&&(e||0===e)&&Number.isFinite(+e)&&i&&o){var n=(o="string"==typeof o?M[o]:o).options.crs.tile.bounds.max.x;switch(i.toUpperCase()){case"TILEMATRIX":return M.pixelToPCRSPoint(L.point(t.x*n,t.y*n),e,o);case"PCRS":return t;case"TCRS":return M.pixelToPCRSPoint(t,e,o);case"GCRS":return o.project(L.latLng(t.y,t.x));default:return}}},Z=function(t,e,o){if(t&&(e||0===e)&&Number.isFinite(+e)&&o)return(o="string"==typeof o?M[o]:o).transformation.untransform(t,o.scale(e))},j=function(t,e,o,i){if(t&&t.max&&t.min&&(e||0===e)&&Number.isFinite(+e)&&o&&i)return o="string"==typeof o?M[o]:o,L.bounds(M.pointToPCRSPoint(t.min,e,o,i),M.pointToPCRSPoint(t.max,e,o,i))},H=function(t,e,o){if(t&&t.max&&t.min&&(e||0===e)&&Number.isFinite(+e)&&o)return o="string"==typeof o?M[o]:o,L.bounds(M.pixelToPCRSPoint(t.min,e,o),M.pixelToPCRSPoint(t.max,e,o))},G=function(t){if(!t||t instanceof Object)return{};let e=t.split(/\s+/).join(""),o={},i=e.split(",");for(let t=0;t<i.length;t++){var n=i[t].split("=");2===n.length&&(o[n[0]]=n[1])}return""!==o&&1===i[0].split("=").length&&(o.content=i[0]),o},V=function(t){for(var e=1,o=[],i=t.split(",");e<i.length;e+=2)o.push([parseInt(i[e-1]),parseInt(i[e])]);return o},$=function(t,e,o){if(o instanceof Element&&t&&t.querySelector("map-link[rel=stylesheet],map-style")){if(e instanceof Element)e=e.getAttribute("href")?e.getAttribute("href"):document.URL;else if(!e||""===e||e instanceof Object)return;for(var i,n,a=[],r=t.querySelectorAll("map-link[rel=stylesheet],map-style"),s=0;s<r.length;s++)"MAP-LINK"===r[s].nodeName.toUpperCase()?(i=r[s].hasAttribute("href")?new URL(r[s].getAttribute("href"),e).href:null)&&(o.querySelector("link[href='"+i+"']")||((n=document.createElement("link")).setAttribute("href",i),n.setAttribute("rel","stylesheet"),a.push(n))):((n=document.createElement("style")).textContent=r[s].textContent,a.push(n));for(var l=a.length-1;0<=l;l--)o.insertAdjacentElement("afterbegin",a[l])}},W=function(t,e,o){var i=[];t.split(/\s+/gim).forEach(M._parseNumber,i),this.push(i)},K=function(t,e,o){this.push(parseFloat(t))},Y=function(i,t){let n,o=!1,a,r=t._map,s;if("text/html"===i.type&&"_blank"!==i.target)i.target="_top";else if("text/html"!==i.type&&i.url.includes("#")){let t=i.url.split("#"),e=t[1].split(",");n={z:e[0]||0,lng:e[1]||0,lat:e[2]||0},o=!t[0],["/",".","#"].includes(i.url[0])&&(i.target="_self")}if(o)n&&!i.inPlace&&o&&(t._map.options.mapEl.zoomTo(+n.lat,+n.lng,+n.z),s&&(a.opacity=s));else{let o=!1;switch(a=document.createElement("layer-"),a.setAttribute("src",i.url),a.setAttribute("checked",""),i.target){case"_blank":"text/html"===i.type?window.open(i.url):(r.options.mapEl.appendChild(a),o=!0);break;case"_parent":for(var e of r.options.mapEl.querySelectorAll("layer-"))e._layer!==t&&r.options.mapEl.removeChild(e);r.options.mapEl.appendChild(a),r.options.mapEl.removeChild(t._layerEl),o=!0;break;case"_top":window.location.href=i.url;break;default:s=t._layerEl.opacity,t._layerEl.insertAdjacentElement("beforebegin",a),r.options.mapEl.removeChild(t._layerEl),o=!0}!i.inPlace&&o&&L.DomEvent.on(a,"extentload",function t(e){o&&["_parent","_self"].includes(i.target)&&1===a.parentElement.querySelectorAll("layer-").length&&(a.parentElement.projection=a._layer.getProjection()),a.extent&&(n?a.parentElement.zoomTo(+n.lat,+n.lng,+n.z):a.zoomTo(),L.DomEvent.off(a,"extentload",t)),s&&(a.opacity=s),r.getContainer().focus()})}},X=function(t){var e=t._map.project(t._map.getCenter()),t=t._map.options.crs.options.crs.tile.bounds.max.y;return[Math.trunc(e.x/t),Math.trunc(e.y/t)]},Q=function(e,o){try{new URL(o);let t='<layer- src="'+o+'" label="'+M.options.locale.dfLayer+'" checked=""></layer->';e.insertAdjacentHTML("beforeend",t),e.lastChild.addEventListener("error",function(){e&&e.removeChild(e.lastChild),t=null})}catch(t){if("<layer-"===(o=o.replace(/(<!--.*?-->)|(<!--[\S\s]+?-->)|(<!--[\S\s]*?$)/g,"").trim()).slice(0,7)&&"</layer->"===o.slice(-9))e.insertAdjacentHTML("beforeend",o);else if("<map-feature"===o.slice(0,12)&&"</map-feature>"===o.slice(-14)){var i=`<layer- label="${M.options.locale.dfPastedLayer}" checked>
                       <map-meta name='projection' content='${e.projection}'></map-meta>`+o+"</layer->";e.insertAdjacentHTML("beforeend",i)}else try{e.geojson2mapml(JSON.parse(o))}catch{console.log("Invalid Input!")}}},J=function(i){let t=document.createElement("table"),e=t.createTHead(),o=e.insertRow(),n=document.createElement("th"),a=document.createElement("th");n.appendChild(document.createTextNode("Property name")),a.appendChild(document.createTextNode("Property value")),n.setAttribute("role","columnheader"),a.setAttribute("role","columnheader"),n.setAttribute("scope","col"),a.setAttribute("scope","col"),o.appendChild(n),o.appendChild(a);let r=t.createTBody();for(var s in i)if(i.hasOwnProperty(s)){let t=r.insertRow(),e=document.createElement("th"),o=document.createElement("td");e.appendChild(document.createTextNode(s)),o.appendChild(document.createTextNode(i[s])),e.setAttribute("scope","row"),o.setAttribute("itemprop",s),t.appendChild(e),t.appendChild(o)}return t},tt=function(t,e,o){return t==={}||(t[0]=Math.min(e,t[0]),t[1]=Math.min(o,t[1]),t[2]=Math.max(e,t[2]),t[3]=Math.max(o,t[3])),t},et=function(m,u={},c=null,p={}){u=Object.assign({},{label:null,projection:"OSMTILE",caption:null,properties:null,geometryFunction:null},u);var h=(m="string"==typeof m?JSON.parse(m):m).type.toUpperCase();let d="",t=!1,e=new DOMParser;null===c&&(m.bbox||(t=!0),a="<layer- label='' checked><map-meta name='projection' content='"+u.projection+"'></map-meta><map-meta name='cs' content='gcrs'></map-meta></layer->",c=e.parseFromString(a,"text/html"),null!==u.label?c.querySelector("layer-").setAttribute("label",u.label):m.name?c.querySelector("layer-").setAttribute("label",m.name):m.title?c.querySelector("layer-").setAttribute("label",m.title):c.querySelector("layer-").setAttribute("label",M.options.locale.dfLayer));let _="<map-point></map-point>";_=e.parseFromString("<map-point></map-point>","text/html");let y="<map-multipoint><map-coordinates></map-coordinates></map-multipoint>";y=e.parseFromString("<map-multipoint><map-coordinates></map-coordinates></map-multipoint>","text/html");let f="<map-linestring><map-coordinates></map-coordinates></map-linestring>";f=e.parseFromString("<map-linestring><map-coordinates></map-coordinates></map-linestring>","text/html");let g="<map-multilinestring></map-multilinestring>";g=e.parseFromString("<map-multilinestring></map-multilinestring>","text/html");let x="<map-polygon></map-polygon>";x=e.parseFromString("<map-polygon></map-polygon>","text/html");let b="<map-multipolygon></map-multipolygon>";b=e.parseFromString("<map-multipolygon></map-multipolygon>","text/html");let L="<map-geometrycollection></map-geometrycollection>";L=e.parseFromString("<map-geometrycollection></map-geometrycollection>","text/html");let n="<map-feature><map-featurecaption></map-featurecaption><map-geometry></map-geometry><map-properties></map-properties></map-feature>";n=e.parseFromString("<map-feature><map-featurecaption></map-featurecaption><map-geometry></map-geometry><map-properties></map-properties></map-feature>","text/html");let v="<map-coordinates></map-coordinates>";if(v=e.parseFromString("<map-coordinates></map-coordinates>","text/html"),"FEATURECOLLECTION"===h){m.bbox?c.querySelector("layer-").insertAdjacentHTML("afterbegin","<map-meta name='extent' content='top-left-longitude="+m.bbox[0]+", top-left-latitude="+m.bbox[1]+", bottom-right-longitude="+m.bbox[2]+",bottom-right-latitude="+m.bbox[3]+"'></map-meta>"):p=[1/0,1/0,Number.NEGATIVE_INFINITY,Number.NEGATIVE_INFINITY];var o=m.features;for(let t=0;t<o.length;t++)M.geojson2mapml(o[t],u,c,p)}else if("FEATURE"===h){let t=n.cloneNode(!0),e=t.querySelector("map-feature");m.bbox?c.querySelector("layer-").insertAdjacentHTML("afterbegin","<map-meta name='extent' content='top-left-longitude="+m.bbox[0]+", top-left-latitude="+m.bbox[1]+", bottom-right-longitude="+m.bbox[2]+",bottom-right-latitude="+m.bbox[3]+"'></map-meta>"):"object"==typeof p&&void 0===p.length&&(p=[1/0,1/0,Number.NEGATIVE_INFINITY,Number.NEGATIVE_INFINITY]);let o=c.querySelector("layer-").getAttribute("label");"function"==typeof u.caption?o=u.caption(m):"string"==typeof u.caption?(o=m.properties[u.caption],void 0===o&&(o=u.caption)):m.id&&(o=m.id),e.querySelector("map-featurecaption").innerHTML=o;let i;"function"==typeof u.properties?(i=u.properties(m),i instanceof Element||(i=!1,console.error("options.properties function returns a string instead of an HTMLElement."))):i="string"==typeof u.properties?(e.querySelector("map-properties").insertAdjacentHTML("beforeend",u.properties),!1):u.properties instanceof HTMLElement?u.properties:M._properties2Table(m.properties),i&&e.querySelector("map-properties").appendChild(i);var a=M.geojson2mapml(m.geometry,u,c,p);"function"==typeof u.geometryFunction?e.querySelector("map-geometry").appendChild(u.geometryFunction(a,m)):e.querySelector("map-geometry").appendChild(a),c.querySelector("layer-").appendChild(e)}else if(["POINT","LINESTRING","POLYGON","MULTIPOINT","MULTILINESTRING","MULTIPOLYGON","GEOMETRYCOLLECTION"].includes(h))switch(h){case"POINT":p=M._updateExtent(p,m.coordinates[0],m.coordinates[1]),d=m.coordinates[0]+" "+m.coordinates[1];let t=_.cloneNode(!0);t=t.querySelector("map-point");let e=v.cloneNode(!0);return e=e.querySelector("map-coordinates"),e.innerHTML=d,t.appendChild(e),t;case"LINESTRING":let o=f.cloneNode(!0),i=o.querySelector("map-coordinates");d="";for(let t=0;t<m.coordinates.length;t++)p=M._updateExtent(p,m.coordinates[t][0],m.coordinates[t][1]),d=d+m.coordinates[t][0]+" "+m.coordinates[t][1]+" ";return i.innerHTML=d,o.querySelector("map-linestring");case"POLYGON":let n=x.cloneNode(!0);n=n.querySelector("map-polygon");for(let o=0;o<m.coordinates.length;o++){let e="",t=v.cloneNode(!0);t=t.querySelector("map-coordinates");for(let t=0;t<m.coordinates[o].length;t++)p=M._updateExtent(p,m.coordinates[o][t][0],m.coordinates[o][t][1]),e=e+m.coordinates[o][t][0]+" "+m.coordinates[o][t][1]+" ";t.innerHTML=e,n.appendChild(t)}return n;case"MULTIPOINT":d="";let a=y.cloneNode(!0);a=a.querySelector("map-multipoint");for(let t=0;t<m.coordinates.length;t++)p=M._updateExtent(p,m.coordinates[t][0],m.coordinates[t][1]),d=d+m.coordinates[t][0]+" "+m.coordinates[t][1]+" ";return a.querySelector("map-coordinates").innerHTML=d,a;case"MULTILINESTRING":let r=g.cloneNode(!0);r=r.querySelector("map-multilinestring");for(let o=0;o<m.coordinates.length;o++){let e="",t=v.cloneNode(!0);t=t.querySelector("map-coordinates");for(let t=0;t<m.coordinates[o].length;t++)p=M._updateExtent(p,m.coordinates[o][t][0],m.coordinates[o][t][1]),e=e+m.coordinates[o][t][0]+" "+m.coordinates[o][t][1]+" ";t.innerHTML=e,r.appendChild(t)}return r;case"MULTIPOLYGON":let s=b.cloneNode(!0);s=s.querySelector("map-multiPolygon");for(let n=0;n<m.coordinates.length;n++){let i=x.cloneNode(!0);i=i.querySelector("map-polygon");for(let o=0;o<m.coordinates[n].length;o++){let e="",t=v.cloneNode(!0);t=t.querySelector("map-coordinates");for(let t=0;t<m.coordinates[n][o].length;t++)p=M._updateExtent(p,m.coordinates[n][o][t][0],m.coordinates[n][o][t][1]),e=e+m.coordinates[n][o][t][0]+" "+m.coordinates[n][o][t][1]+" ";t.innerHTML=e,i.appendChild(t)}s.appendChild(i)}return s;case"GEOMETRYCOLLECTION":let l=L.cloneNode(!0);l=l.querySelector("map-geometrycollection");for(let t=0;t<m.geometries.length;t++){var E=M.geojson2mapml(m.geometries[t],u,c,p);l.appendChild(E)}return l}return t&&c.querySelector("layer-").insertAdjacentHTML("afterbegin","<map-meta name='extent' content='top-left-longitude="+p[0]+", top-left-latitude="+p[1]+", bottom-right-longitude="+p[2]+",bottom-right-latitude="+p[3]+"'></map-meta>"),c.querySelector("layer-")},ot=function(e){let o=[];e=e.filter(t=>!/[^\d.-]/g.test(t)).filter(t=>t);for(let t=0;t<e.length;t+=2)o.push(e.slice(t,t+2).map(Number));return o},it=function(t){null!==t.querySelector("thead")&&t.querySelector("thead").remove();let e={};return t.querySelectorAll("tr").forEach(t=>{t=t.querySelectorAll("th, td");e[t[0].innerHTML]=t[1].innerHTML}),e},nt=function(t,n,a,r){for(;"MAP-SPAN"===t.nodeName.toUpperCase()||"MAP-A"===t.nodeName.toUpperCase();)t=t.firstElementChild;let s=t.nodeName,l={},m;switch(s.toUpperCase()){case"MAP-POINT":var u;l.type="Point",r?(u=proj4.transform(n,a,t.querySelector("map-coordinates").innerHTML.split(/[<>\ ]/g).map(Number)),l.coordinates=[u.x,u.y]):l.coordinates=t.querySelector("map-coordinates").innerHTML.split(/[<>\ ]/g).map(Number);break;case"MAP-LINESTRING":l.type="LineString",m=t.querySelector("map-coordinates").innerHTML.split(/[<>\ ]/g),m=M._breakArray(m),r&&(m=M._pcrsToGcrs(m,n,a)),l.coordinates=m;break;case"MAP-POLYGON":l.type="Polygon",l.coordinates=[];let e=0;t.querySelectorAll("map-coordinates").forEach(t=>{t=t.innerHTML.split(/[<>\ ]/g),t=M._breakArray(t),r&&(t=M._pcrsToGcrs(t,n,a)),l.coordinates[e]=t,e++});break;case"MAP-MULTIPOINT":l.type="MultiPoint",m=M._breakArray(t.querySelector("map-coordinates").innerHTML.split(/[<>\ ]/g)),r&&(m=M._pcrsToGcrs(m,n,a)),l.coordinates=m;break;case"MAP-MULTILINESTRING":l.type="MultiLineString",l.coordinates=[];let o=0;t.querySelectorAll("map-coordinates").forEach(t=>{t=t.innerHTML.split(/[<>\ ]/g),t=M._breakArray(t),r&&(t=M._pcrsToGcrs(t,n,a)),l.coordinates[o]=t,o++});break;case"MAP-MULTIPOLYGON":l.type="MultiPolygon",l.coordinates=[];let i=0;t.querySelectorAll("map-polygon").forEach(t=>{let e=0;l.coordinates.push([]),t.querySelectorAll("map-coordinates").forEach(t=>{t=t.innerHTML.split(/[<>\ ]/g),t=M._breakArray(t),r&&(t=M._pcrsToGcrs(t,n,a)),l.coordinates[i].push([]),l.coordinates[i][e]=t,e++}),i++})}return l},at=function(e,o,i){let n=[];for(let t=0;t<e.length;t++){var a=[(a=proj4.transform(o,i,e[t])).x,a.y];n.push(a)}return n},rt=function(t,i={}){i=Object.assign({},{propertyFunction:null,transform:!0},i);let n={type:"FeatureCollection"};n.title=t.getAttribute("label"),n.features=[];let a=null,r=null;i.transform&&(a=new proj4.Proj(t.parentElement._map.options.crs.code),r=new proj4.Proj("EPSG:4326"),"EPSG:3857"!==t.parentElement._map.options.crs.code&&"EPSG:4326"!==t.parentElement._map.options.crs.code||(i.transform=!1));let e=t.querySelectorAll("map-meta");e.forEach(e=>{if("extent"===e.getAttribute("name")){let t=e.getAttribute("content"),o=t.split(","),i={};for(let e=0;e<o.length;e++){let t=o[e].split("=");t[0]=t[0].trim(),t[1]=parseFloat(t[1]),i[t[0]]=t[1]}n.bbox=[i["top-left-longitude"],i["top-left-latitude"],i["bottom-right-longitude"],i["bottom-right-latitude"]]}});let o=t.querySelectorAll("map-feature"),s=0;return o.forEach(t=>{var e;n.features[s]={type:"Feature"},n.features[s].geometry={},n.features[s].properties={},t.querySelector("map-properties")?"function"==typeof i.propertyFunction?(e=i.propertyFunction(t.querySelector("map-properties")),n.features[s].properties=e):null!==t.querySelector("map-properties").querySelector("table")?(e=t.querySelector("map-properties").querySelector("table").cloneNode(!0),e=M._table2properties(e),n.features[s].properties=e):n.features[s].properties={prop0:t.querySelector("map-properties").innerHTML.replace(/(<([^>]+)>)/gi,"")}:n.features[s].properties=null;let o=t.querySelector("map-geometry").firstElementChild;for(;"MAP-SPAN"===o.nodeName.toUpperCase()||"MAP-A"===o.nodeName.toUpperCase();)o=o.firstElementChild;"MAP-GEOMETRYCOLLECTION"!==o.nodeName.toUpperCase()?n.features[s].geometry=M._geometry2geojson(o,a,r,i.transform):(n.features[s].geometry.type="GeometryCollection",n.features[s].geometry.geometries=[],t=o.children,Array.from(t).forEach(t=>{var e=t.nodeName.toUpperCase();"MAP-SPAN"===e||"MAP-A"===e?([...(t=t.cloneNode(!0)).querySelectorAll("map-a, map-span")].forEach(t=>t.replaceWith(...t.children)),Array.from(t.children).forEach(t=>{t=M._geometry2geojson(t,a,r,i.transform),n.features[s].geometry.geometries.push(t)})):(t=M._geometry2geojson(t,a,r,i.transform),n.features[s].geometry.geometries.push(t))})),s++}),n},st=function(a,r,s,l){if(a){let t=r.getZoom(),e=r.options.crs.scale(t),o=r.options.crs.transformation.transform(a.getCenter(!0),e);var m=r.getSize().divideBy(2),u=o.subtract(m).round(),c=o.add(m).round(),p=M.pixelToPCRSPoint(u,t,r.options.projection),h=M.pixelToPCRSPoint(c,t,r.options.projection);let i=L.bounds(p,h),n=i.contains(a)?1:-1;for(;-1==n&&!i.contains(a)&&t-1>=s||1==n&&i.contains(a)&&t+1<=l;)t+=n,e=r.options.crs.scale(t),o=r.options.crs.transformation.transform(a.getCenter(!0),e),u=o.subtract(m).round(),c=o.add(m).round(),p=M.pixelToPCRSPoint(u,t,r.options.projection),h=M.pixelToPCRSPoint(c,t,r.options.projection),i=L.bounds(p,h);return 1==n&&0<=t-1&&(t!==l||!i.contains(a))&&t--,t}},lt=L.Control.Attribution.extend({options:{prefix:'<a href="https://www.w3.org/community/maps4html/">Maps for HTML Community Group</a> | <img src="data:image/svg+xml;base64,PHN2ZyBhcmlhLWhpZGRlbj0idHJ1ZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIiB3aWR0aD0iMTIiIGhlaWdodD0iOCI+PHBhdGggZmlsbD0iIzRDN0JFMSIgZD0iTTAgMGgxMnY0SDB6Ii8+PHBhdGggZmlsbD0iI0ZGRDUwMCIgZD0iTTAgNGgxMnYzSDB6Ii8+PHBhdGggZmlsbD0iI0UwQkMwMCIgZD0iTTAgN2gxMnYxSDB6Ii8+PC9zdmc+" style="padding-right: 0.3em;" alt="Slava Ukraini"> <a href="https://leafletjs.com" title="A JS library for interactive maps">Leaflet</a> '},onAdd:function(t){for(var e in(t.attributionControl=this)._container=L.DomUtil.create("details","leaflet-control-attribution"),L.DomEvent.disableClickPropagation(this._container),t._layers)t._layers[e].getAttribution&&this.addAttribution(t._layers[e].getAttribution());this._update(),t.on("layeradd",this._addAttribution,this);let o=document.createElement("dialog");return o.setAttribute("class","shortcuts-dialog"),o.setAttribute("autofocus",""),o.onclick=function(t){t.stopPropagation()},o.innerHTML=`<b>${M.options.locale.kbdShortcuts} </b><button aria-label="Close" onclick='this.parentElement.close()'>X</button>`+`<ul><b>${M.options.locale.kbdMovement}</b><li><kbd>&#8593</kbd> ${M.options.locale.kbdPanUp}</li><li><kbd>&#8595</kbd> ${M.options.locale.kbdPanDown}</li><li><kbd>&#8592</kbd> ${M.options.locale.kbdPanLeft}</li><li><kbd>&#8594</kbd> ${M.options.locale.kbdPanRight}</li><li><kbd>+</kbd> ${M.options.locale.btnZoomIn}</li><li><kbd>-</kbd> ${M.options.locale.btnZoomOut}</li><li><kbd>shift</kbd> + <kbd>&#8592/&#8593/&#8594/&#8595</kbd> 3x ${M.options.locale.kbdPanIncrement}</li><li><kbd>ctrl</kbd> + <kbd>&#8592/&#8593/&#8594/&#8595</kbd> 0.2x ${M.options.locale.kbdPanIncrement}</li><li><kbd>shift</kbd> + <kbd>+/-</kbd> ${M.options.locale.kbdZoom}</li></ul>`+`<ul><b>${M.options.locale.kbdFeature}</b><li><kbd>&#8592/&#8593</kbd> ${M.options.locale.kbdPrevFeature}</li><li><kbd>&#8594/&#8595</kbd> ${M.options.locale.kbdNextFeature}</li></ul>`,t._container.appendChild(o),this._container},_update:function(){if(this._map){var t,e=[];for(t in this._attributions)this._attributions[t]&&e.push(t);var o=[];this.options.prefix&&o.push(this.options.prefix),e.length&&o.push(e.join(", ")),this._container.innerHTML='<summary><svg aria-hidden="true" xmlns="http://www.w3.org/2000/svg" height="30px" viewBox="0 0 24 24" width="30px" fill="currentColor"><path d="M0 0h24v24H0V0z" fill="none"></path><path d="M11 7h2v2h-2zm0 4h2v6h-2zm1-9C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8z"></path></svg></summary><div class="mapml-attribution-container">'+`<button onclick="this.closest('.leaflet-container').querySelector('.shortcuts-dialog').showModal()" class="shortcuts-button mapml-button">${M.options.locale.kbdShortcuts}</button> | `+o.join(' <span aria-hidden="true">|</span> ')+"</div>",this._container.setAttribute("role","group"),this._container.setAttribute("aria-label",""+M.options.locale.btnAttribution)}}});L.Map.mergeOptions({attributionControl:!1,toggleableAttributionControl:!0}),L.Map.addInitHook(function(){this.options.toggleableAttributionControl&&(new lt).addTo(this)});function mt(t){return new lt(t)}function ut(t){return new pt(t)}function ct(t){return new ht(t)}var pt=L.Control.extend({options:{position:"topleft"},onAdd:function(t){var e=L.DomUtil.create("div","mapml-reload-button leaflet-bar");let o=L.DomUtil.create("button","mapml-reload-button",e);return o.innerHTML="<span aria-hidden='true'>&#x021BA</span>",o.title=M.options.locale.cmReload,o.setAttribute("type","button"),o.classList.add("mapml-button"),o.setAttribute("aria-label","Reload"),L.DomEvent.disableClickPropagation(o),L.DomEvent.on(o,"click",L.DomEvent.stop),L.DomEvent.on(o,"click",this._goReload,this),this._reloadButton=o,this._updateDisabled(),t.on("moveend",this._updateDisabled,this),e},onRemove:function(t){t.off("moveend",this._updateDisabled,this)},disable:function(){return this._disabled=!0,this._updateDisabled(),this},enable:function(){return this._disabled=!1,this._updateDisabled(),this},_goReload:function(t){!this._disabled&&1<this._map.options.mapEl._history.length&&this._map.options.mapEl.reload()},_updateDisabled:function(){setTimeout(()=>{L.DomUtil.removeClass(this._reloadButton,"leaflet-disabled"),this._reloadButton.setAttribute("aria-disabled","false"),this._map&&(this._disabled||this._map.options.mapEl._history.length<=1)&&(L.DomUtil.addClass(this._reloadButton,"leaflet-disabled"),this._reloadButton.setAttribute("aria-disabled","true"))},0)}}),ht=L.Control.Scale.extend({options:{maxWidth:100,updateWhenIdle:!0,position:"bottomleft"},onAdd:function(t){t._container.insertAdjacentHTML("beforeend","<output role='status' aria-live='polite' aria-atomic='true' class='mapml-screen-reader-output-scale'></output>"),this._container=L.DomUtil.create("div","mapml-control-scale");var e=L.Control.Scale.prototype.onAdd.call(this,t);return this._container.appendChild(e),this._container.setAttribute("tabindex",0),this._scaleControl=this,setTimeout(()=>{this._updateOutput(),this._focusOutput()},0),t.on("zoomend moveend",this._updateOutput,this),this._map._container.addEventListener("focus",()=>this._focusOutput()),this._container},onRemove:function(t){t.off("zoomend moveend",this._updateOutput,this)},getContainer:function(){return this._container},_pixelsToDistance:function(t,e){var o=96*window.devicePixelRatio;return"metric"===e?t/o*2.54:t/o},_scaleLength:function(t){let e=t.getAttribute("style");return parseInt(e.match(/width:\s*(\d+)px/)[1])},_focusOutput:function(){setTimeout(()=>{let t=this._map._container.querySelector(".mapml-screen-reader-output-scale");t.textContent="",setTimeout(()=>{t.textContent=this._container.getAttribute("aria-label")},100)},0)},_updateOutput:function(){let t="",e=this._scaleControl.getContainer().getElementsByClassName("leaflet-control-scale-line")[0];var o;t=this.options.metric?(o=parseFloat(this._pixelsToDistance(this._scaleLength(e),"metric").toFixed(1)),t=o+" centimeters to "+e.textContent.trim(),t=t.replace(/(\d+)\s*m\b/g,"$1 meters"),t.replace(/ km/g," kilometers")):(o=parseFloat(this._pixelsToDistance(this._scaleLength(e),"imperial").toFixed(1)),t=o+" inches to "+e.textContent.trim(),t=t.replace(/ft/g,"feet"),t.replace(/mi/g,"miles")),this._container.setAttribute("aria-label",t),this._map._container.querySelector(".mapml-screen-reader-output-scale").textContent=t}}),dt=L.Control.extend({options:{position:"topleft",title:{false:"View Fullscreen",true:"Exit Fullscreen"}},onAdd:function(t){var e=L.DomUtil.create("div","leaflet-control-fullscreen leaflet-bar leaflet-control");return this.link=L.DomUtil.create("a","leaflet-control-fullscreen-button leaflet-bar-part",e),this.link.href="#",this.link.setAttribute("role","button"),this._map=t,this._map.on("fullscreenchange",this._toggleTitle,this),this._toggleTitle(),L.DomEvent.on(this.link,"click",this._click,this),e},onRemove:function(t){t.off("fullscreenchange",this._toggleTitle,this)},_click:function(t){L.DomEvent.stopPropagation(t),L.DomEvent.preventDefault(t),this._map.toggleFullscreen(this.options)},_toggleTitle:function(){this.link.title=this.options.title[this._map.isFullscreen()]}});L.Map.include({isFullscreen:function(){return this._isFullscreen||!1},toggleFullscreen:function(t){var e=this.getContainer().getRootNode().host,e="DIV"===e.nodeName?e.parentElement:e;this.isFullscreen()?t&&t.pseudoFullscreen?this._disablePseudoFullscreen(e):document.exitFullscreen?document.exitFullscreen():document.mozCancelFullScreen?document.mozCancelFullScreen():document.webkitCancelFullScreen?document.webkitCancelFullScreen():document.msExitFullscreen?document.msExitFullscreen():this._disablePseudoFullscreen(e):t&&t.pseudoFullscreen?this._enablePseudoFullscreen(e):e.requestFullscreen?e.requestFullscreen():e.mozRequestFullScreen?e.mozRequestFullScreen():e.webkitRequestFullscreen?e.webkitRequestFullscreen(Element.ALLOW_KEYBOARD_INPUT):e.msRequestFullscreen?e.msRequestFullscreen():this._enablePseudoFullscreen(e)},_enablePseudoFullscreen:function(t){L.DomUtil.addClass(t,"leaflet-pseudo-fullscreen"),this._setFullscreen(!0),this.fire("fullscreenchange")},_disablePseudoFullscreen:function(t){L.DomUtil.removeClass(t,"leaflet-pseudo-fullscreen"),this._setFullscreen(!1),this.fire("fullscreenchange")},_setFullscreen:function(t){this._isFullscreen=t;var e=this.getContainer().getRootNode().host;t?L.DomUtil.addClass(e,"mapml-fullscreen-on"):L.DomUtil.removeClass(e,"mapml-fullscreen-on"),this.invalidateSize()},_onFullscreenChange:function(t){var e=document.fullscreenElement||document.mozFullScreenElement||document.webkitFullscreenElement||document.msFullscreenElement,o=this.getContainer().getRootNode().host,o="DIV"===o.nodeName?o.parentElement:o;e!==o||this._isFullscreen?e!==o&&this._isFullscreen&&(this._setFullscreen(!1),this.fire("fullscreenchange")):(this._setFullscreen(!0),this.fire("fullscreenchange"))}}),L.Map.mergeOptions({fullscreenControl:!1}),L.Map.addInitHook(function(){var t,e;this.options.fullscreenControl&&(this.fullscreenControl=new dt(this.options.fullscreenControl),this.addControl(this.fullscreenControl)),"onfullscreenchange"in document?t="fullscreenchange":"onmozfullscreenchange"in document?t="mozfullscreenchange":"onwebkitfullscreenchange"in document?t="webkitfullscreenchange":"onmsfullscreenchange"in document&&(t="MSFullscreenChange"),t&&(e=L.bind(this._onFullscreenChange,this),this.whenReady(function(){L.DomEvent.on(document,t,e)}),this.on("unload",function(){L.DomEvent.off(document,t,e)}))});function _t(t){return new dt(t)}function yt(t){return new Lt(t)}function ft(t){return new vt(t)}function gt(t,e){return new Mt(t,e)}function xt(t){return new Et(t)}function bt(t,e){return new Ct(t,e)}var Lt=L.Control.extend({options:{position:"bottomright"},onAdd:function(t){this.locateControl=L.control.locate({showPopup:!1,strings:{title:M.options.locale.btnLocTrackOff},position:this.options.position,locateOptions:{maxZoom:16}}).addTo(t);var e=this.locateControl._container,o=this.locateControl;return new MutationObserver(function(t){e.classList.contains("active")&&e.classList.contains("following")?(e.firstChild.title=M.options.locale.btnLocTrackOn,o._marker.bindTooltip(M.options.locale.btnMyLocTrackOn,{permanent:!0})):e.classList.contains("active")?(e.firstChild.title=M.options.locale.btnLocTrackLastKnown,o._marker.bindTooltip(M.options.locale.btnMyLastKnownLocTrackOn)):e.firstChild.title=M.options.locale.btnLocTrackOff}).observe(e,{attributes:!0,attributeFilter:["class"]}),e},stop:function(){return this.locateControl.stop()}}),vt=L.Layer.extend({onAdd:function(t){this._container=L.DomUtil.create("div","mapml-crosshair",t._container),this._container.innerHTML='<svg xmlns="http://www.w3.org/2000/svg" xml:space="preserve" viewBox="0 0 100 100"><g stroke="#fff" stroke-linecap="round" stroke-linejoin="round"><circle cx="50.028" cy="50.219" r="3.923" stroke-width="2" color="currentColor" overflow="visible"/><path stroke-width="3" d="M4.973 54.424h31.768a4.204 4.204 0 1 0 0-8.409H4.973A4.203 4.203 0 0 0 .77 50.22a4.203 4.203 0 0 0 4.204 4.205z" color="currentColor" overflow="visible"/><path stroke-width="3" d="M54.232 5.165a4.204 4.204 0 1 0-8.408 0v31.767a4.204 4.204 0 1 0 8.408 0V5.165z"/><path stroke-width="3" d="M99.288 50.22a4.204 4.204 0 0 0-4.204-4.205H63.317a4.204 4.204 0 1 0 0 8.409h31.767a4.205 4.205 0 0 0 4.204-4.205zM45.823 95.274a4.204 4.204 0 1 0 8.409 0V63.506a4.204 4.204 0 1 0-8.409 0v31.768z" color="currentColor" overflow="visible"/></g></svg>',t.isFocused=!1,this._isQueryable=!1,t.on("layerchange layeradd layerremove overlayremove",this._toggleEvents,this),t.on("popupopen",this._isMapFocused,this),L.DomEvent.on(t._container,"keydown keyup mousedown",this._isMapFocused,this),this._addOrRemoveCrosshair()},onRemove:function(t){t.off("layerchange layeradd layerremove overlayremove",this._toggleEvents),t.off("popupopen",this._isMapFocused),L.DomEvent.off(t._container,"keydown keyup mousedown",this._isMapFocused)},_toggleEvents:function(){this._hasQueryableLayer()?this._map.on("viewreset move moveend",this._addOrRemoveCrosshair,this):this._map.off("viewreset move moveend",this._addOrRemoveCrosshair,this),this._addOrRemoveCrosshair()},_addOrRemoveCrosshair:function(t){this._hasQueryableLayer()?this._container.removeAttribute("hidden"):this._container.setAttribute("hidden","")},_addOrRemoveMapOutline:function(t){var e=this._map._container;this._map.isFocused&&!this._outline?this._outline=L.DomUtil.create("div","mapml-outline",e):!this._map.isFocused&&this._outline&&(L.DomUtil.remove(this._outline),delete this._outline)},_hasQueryableLayer:function(){var t=this._map.options.mapEl.layers;if(this._map.isFocused)for(var e of t)if(e.checked&&e._layer.queryable)return!0;return!1},_isMapFocused:function(t){var e;this._map._container.parentNode.activeElement?((e=this._map._container.parentNode.activeElement.classList.contains("leaflet-container"))&&["keydown"].includes(t.type)&&t.shiftKey&&9===t.keyCode?this._map.isFocused=!1:this._map.isFocused=e&&["keyup","keydown"].includes(t.type),this._map.isFocused&&this._map.fire("mapkeyboardfocused"),this._addOrRemoveMapOutline(),this._addOrRemoveCrosshair()):this._map.isFocused=!1}}),Mt=L.Path.extend({initialize:function(t,e){this.type=t.tagName.toUpperCase(),"MAP-POINT"!==this.type&&"MAP-MULTIPOINT"!==this.type||(e.fillOpacity=1),0<e.wrappers.length&&(e=Object.assign(this._convertWrappers(e.wrappers),e)),L.setOptions(this,e),this.group=this.options.group,this.options.interactive=this.options.link||this.options.properties&&this.options.onEachFeature,this._parts=[],this._markup=t,this.options.zoom=t.getAttribute("zoom")||this.options.nativeZoom,this._convertMarkup(),(t.querySelector("map-span")||t.querySelector("map-a"))&&this._generateOutlinePoints(),this.isClosed=this._isClosed()},attachLinkHandler:function(i,n,a){let r,o=document.createElement("div"),s=document.createElement("p"),l=!1;o.classList.add("mapml-link-preview"),o.appendChild(s),i.classList.add("map-a"),n.visited&&i.classList.add("map-a-visited"),i.mousedown=t=>r={x:t.clientX,y:t.clientY},L.DomEvent.on(i,"mousedown",i.mousedown,this),i.mouseup=o=>{if(0===o.button){let t=!0,e=this.options._leafletLayer._layerEl.nextElementSibling;for(;e&&t;)e.tagName&&"LAYER-"===e.tagName.toUpperCase()&&(t=!(e.checked&&e._layer.queryable)),e=e.nextElementSibling;t&&r&&o.eventPhase!==Event.BUBBLING_PHASE&&Math.sqrt(Math.pow(r.x-o.clientX,2)+Math.pow(r.y-o.clientY,2))<=5&&(n.visited=!0,i.setAttribute("stroke","#6c00a2"),i.classList.add("map-a-visited"),M._handleLink(n,a))}},L.DomEvent.on(i,"mouseup",i.mouseup,this),L.DomEvent.on(i,"keypress",t=>{L.DomEvent.stop(t),13!==t.keyCode&&32!==t.keyCode||(n.visited=!0,i.setAttribute("stroke","#6c00a2"),i.classList.add("map-a-visited"),M._handleLink(n,a))},this),L.DomEvent.on(i,"mouseenter keyup",t=>{if(t.target===t.currentTarget){l=!0;let t=document.createElement("a"),e=this._map.getContainer().clientWidth;for(t.href=n.url,s.innerHTML=t.href,this._map.getContainer().appendChild(o);s.clientWidth>e/2;)s.innerHTML=s.innerHTML.substring(0,s.innerHTML.length-5)+"...";setTimeout(()=>{l&&(s.innerHTML=t.href)},1e3)}},this),L.DomEvent.on(i,"mouseout keydown mousedown",t=>{t.target===t.currentTarget&&o.parentElement&&(l=!1,this._map.getContainer().removeChild(o))},this),L.DomEvent.on(a._map.getContainer(),"mouseout mouseenter click",t=>{o.parentElement&&(l=!1,this._map.getContainer().removeChild(o))},this)},_project:function(t,e=void 0,o=void 0){let i=t||this._map,n=e||i.getPixelOrigin(),a=void 0===o?i.getZoom():o;for(var r of this._parts){r.pixelRings=this._convertRing(r.rings,i,n,a);for(var s of r.subrings)s.pixelSubrings=this._convertRing([s],i,n,a)}if(this._outline){this.pixelOutline=[];for(var l of this._outline)this.pixelOutline=this.pixelOutline.concat(this._convertRing(l,i,n,a))}},_convertRing:function(t,e,o,i){let n=e.options.crs.scale(i),a=[];for(var r of t){let t=[];for(var s of r.points){s=e.options.crs.transformation.transform(s,n);t.push(L.point(s.x,s.y)._subtract(o).round())}a.push(t)}return a},_update:function(){this._map&&this._renderer._updateFeature(this)},_convertWrappers:function(o){if(o&&0!==o.length){let t="",e={};for(var i of o)if("MAP-A"!==i.tagName.toUpperCase()&&i.className)t+=i.className+" ";else if(!e.link&&i.getAttribute("href")){let t={};t.url=i.getAttribute("href"),i.hasAttribute("target")&&(t.target=i.getAttribute("target")),i.hasAttribute("type")&&(t.type=i.getAttribute("type")),i.hasAttribute("inplace")&&(t.inPlace=!0),e.link=t}return e.className=(t+" "+this.options.className).trim(),e}},_convertMarkup:function(){if(this._markup){var i,e=this._markup.attributes;this.featureAttributes={},this.options.link&&"MAP-A"===this._markup.parentElement.tagName.toUpperCase()&&"MAP-GEOMETRY"!==this._markup.parentElement.parentElement.tagName.toUpperCase()&&(this.featureAttributes.tabindex="0");for(let t=0;t<e.length;t++)this.featureAttributes[e[t].name]=e[t].value;let o=!0;for(i of this._markup.querySelectorAll("map-coordinates")){let t=[],e=[];if(this._coordinateToArrays(i,t,e,this.options.className),o||"MAP-POLYGON"!==this.type)if("MAP-MULTIPOINT"===this.type)for(var n of t[0].points.concat(e))this._parts.push({rings:[{points:[n]}],subrings:[],cls:(`${n.cls||""} `+(this.options.className||"")).trim()});else this._parts.push({rings:t,subrings:e,cls:(`${this.featureAttributes.class||""} `+(this.options.className||"")).trim()});else this._parts[0].rings.push(t[0]),0<e.length&&(this._parts[0].subrings=this._parts[0].subrings.concat(e));o=!1}}},_generateOutlinePoints:function(){if("MAP-MULTIPOINT"!==this.type&&"MAP-POINT"!==this.type&&"MAP-LINESTRING"!==this.type&&"MAP-MULTILINESTRING"!==this.type){this._outline=[];for(var t of this._markup.querySelectorAll("map-coordinates")){let o=t.childNodes,i=0,n=document.createElement("div");o.length;for(let t=0;t<o.length;t++)0===o[t].textContent.trim().length&&o[t].remove();for(var a of o){var r,s=[];if(!a.tagName){let t="",e=((i-1)%o.length+o.length)%o.length;o[e].tagName&&(r=o[e].textContent.trim().split(/\s+/),t+=`${r[r.length-2]} ${r[r.length-1]} `),t+=a.textContent,e=((i+1)%o.length+o.length)%o.length,o[e].tagName&&(a=o[e].textContent.trim().split(/\s+/),t+=`${a[0]} ${a[1]} `),n.innerHTML=t,this._coordinateToArrays(n,s,[],!0,this.featureAttributes.class||this.options.className),this._outline.push(s)}i++}}}},_coordinateToArrays:function(i,t,n,e=!0,a=void 0,r=[]){for(var o of i.children)this._coordinateToArrays(o,t,n,!1,o.getAttribute("class"),r.concat([o]));let s=i.textContent.replace(/(<([^>]+)>)/gi,""),l=s.match(/(\S+\s+\S+)/gim),m=[],u;for(var c of l){var p=[];c.split(/\s+/gim).forEach(M._parseNumber,p);p=M.pointToPCRSPoint(L.point(p),this.options.zoom,this.options.projection,this.options.nativeCS);m.push(p),u=u?u.extend(p):L.bounds(p,p)}if(this._bounds?(this._bounds.extend(u.min),this._bounds.extend(u.max)):this._bounds=u,e)t.push({points:m});else{let e={},o=i.attributes,t=this._convertWrappers(r);t.link&&(e.tabindex="0");for(let t=0;t<o.length;t++)"class"!==o[t].name&&(e[o[t].name]=o[t].value);n.unshift({points:m,center:u.getCenter(),cls:(`${a||""} `+(t.className||"")).trim(),attr:e,link:t.link,linkTarget:t.linkTarget,linkType:t.linkType})}},_isClosed:function(){switch(this.type){case"MAP-POLYGON":case"MAP-MULTIPOLYGON":case"MAP-POINT":case"MAP-MULTIPOINT":return!0;default:return!1}},getCenter:function(){return this._bounds?this._map.options.crs.unproject(this._bounds.getCenter()):null},getPCRSCenter:function(){return this._bounds.getCenter()}}),Et=L.SVG.extend({_initContainer:function(){L.SVG.prototype._initContainer.call(this),this._container.setAttribute("role","none presentation")},_initPath:function(e,t=!0){if(e._outline){let t=L.SVG.create("path");e.options.className&&L.DomUtil.addClass(t,e.featureAttributes.class||e.options.className),L.DomUtil.addClass(t,"mapml-feature-outline"),t.style.fill="none",e.outlinePath=t}for(var o of e._parts){if(o.rings&&(this._createPath(o,e.options.className,e.featureAttributes["aria-label"],e.options.interactive,e.featureAttributes),e.outlinePath&&(o.path.style.stroke="none")),o.subrings)for(var i of o.subrings)this._createPath(i,e.options.className,i.attr["aria-label"],void 0!==i.link,i.attr);this._updateStyle(e)}t&&(t=L.stamp(e),this._layers[t]=e)},_createPath:function(t,e,o,i=!1,n=void 0){let a=L.SVG.create("path");if(t.path=a,n)for(var[r,s]of Object.entries(n))"id"!==r&&"tabindex"!==r&&a.setAttribute(r,s);else o&&a.setAttribute("aria-label",o);(t.cls||e)&&L.DomUtil.addClass(a,t.cls||e),i&&L.DomUtil.addClass(a,"leaflet-interactive")},_addPath:function(t,e=void 0,o=!0){this._rootGroup||e||this._initContainer();let i=e||this._rootGroup,n=!1;o&&t.addInteractiveTarget(t.group);for(var a of t._parts){a.path&&t.group.appendChild(a.path),o&&(t.options.link&&t.attachLinkHandler(a.path,t.options.link,t.options._leafletLayer),t.addInteractiveTarget(a.path)),!n&&t.pixelOutline&&(t.group.appendChild(t.outlinePath),n=!0);for(var r of a.subrings)r.path&&(r.link&&(t.attachLinkHandler(r.path,r.link,t.options._leafletLayer),t.addInteractiveTarget(r.path)),t.group.appendChild(r.path))}i.appendChild(t.group)},_removePath:function(t){for(var e of t._parts){e.path&&(t.removeInteractiveTarget(e.path),L.DomUtil.remove(e.path));for(var o of e.subrings)o.path&&L.DomUtil.remove(o.path)}t.outlinePath&&L.DomUtil.remove(t.outlinePath),t.removeInteractiveTarget(t.group),L.DomUtil.remove(t.group),delete this._layers[L.stamp(t)]},_updateFeature:function(t){t.pixelOutline&&this._setPath(t.outlinePath,this.geometryToPath(t.pixelOutline,!1));for(var e of t._parts){this._setPath(e.path,this.geometryToPath(e.pixelRings,t.isClosed));for(var o of e.subrings)this._setPath(o.path,this.geometryToPath(o.pixelSubrings,!1))}},_pointToMarker:function(t){return`M${t.x} ${t.y} L${t.x-12.5} ${t.y-30} C${t.x-12.5} ${t.y-50}, ${t.x+12.5} ${t.y-50}, ${t.x+12.5} ${t.y-30} L${t.x} ${t.y}z`},_updateStyle:function(t){this._updatePathStyle(t.outlinePath,t,!1,!0);for(var e of t._parts){e.path&&this._updatePathStyle(e.path,t,!0);for(var o of e.subrings)o.path&&this._updatePathStyle(o.path,t)}},_updatePathStyle:function(t,e,o=!1,i=!1){var n,a;t&&e&&(n=e.options,a=e.isClosed,n.stroke&&(!a||i)||o&&!e.outlinePath?(t.setAttribute("stroke",n.color),t.setAttribute("stroke-opacity",n.opacity),t.setAttribute("stroke-width",n.weight),t.setAttribute("stroke-linecap",n.lineCap),t.setAttribute("stroke-linejoin",n.lineJoin),n.dashArray?t.setAttribute("stroke-dasharray",n.dashArray):t.removeAttribute("stroke-dasharray"),n.dashOffset?t.setAttribute("stroke-dashoffset",n.dashOffset):t.removeAttribute("stroke-dashoffset"),n.link&&(t.setAttribute("stroke",n.link.visited?"#6c00a2":"#0000EE"),t.setAttribute("stroke-opacity","1"),t.setAttribute("stroke-width","1px"),t.setAttribute("stroke-dasharray","none"))):t.setAttribute("stroke","none"),a&&!i?n.fill?t.setAttribute("fill",n.color):(t.setAttribute("fill",n.fillColor||n.color),t.setAttribute("fill-opacity",n.fillOpacity),t.setAttribute("fill-rule",n.fillRule||"evenodd")):t.setAttribute("fill","none"))},_setPath:function(t,e){t.setAttribute("d",e)},geometryToPath:function(t,e){let o="",i,n,a,r,s,l;for(i=0,a=t.length;i<a;i++){if(1===(s=t[i]).length)return this._pointToMarker(s[0]);for(n=0,r=s.length;n<r;n++)l=s[n],o+=(n?"L":"M")+l.x+" "+l.y;o+=e?"z":""}return o||"M0 0"}}),Ct=L.FeatureGroup.extend({initialize:function(t,e){var o;e.wrappers&&0<e.wrappers.length&&(e=Object.assign(M.Feature.prototype._convertWrappers(e.wrappers),e)),L.LayerGroup.prototype.initialize.call(this,t,e),this._featureEl=this.options.mapmlFeature,(this.options.onEachFeature&&this.options.properties||this.options.link)&&(L.DomUtil.addClass(this.options.group,"leaflet-interactive"),e=t[Object.keys(t)[0]],1===t.length&&e.options.link&&(this.options.link=e.options.link),this.options.link?(M.Feature.prototype.attachLinkHandler.call(this,this.options.group,this.options.link,this.options._leafletLayer),this.options.group.setAttribute("role","link")):(this.options.group.setAttribute("aria-expanded","false"),this.options.group.setAttribute("role","button"),this.options.onEachFeature(this.options.properties,this),this.off("click",this._openPopup))),L.DomEvent.on(this.options.group,"keyup keydown",this._handleFocus,this),this.options.group.setAttribute("aria-label",this.options.accessibleTitle),this.options.featureID&&this.options.group.setAttribute("data-fid",this.options.featureID);for(o of t)o._groupLayer=this},onAdd:function(t){L.LayerGroup.prototype.onAdd.call(this,t),this.updateInteraction()},updateInteraction:function(){let e=this._map||this.options._leafletLayer._map;for(var o in(this.options.onEachFeature||this.options.link)&&e.featureIndex.addToIndex(this,this.getPCRSCenter(),this.options.group),this._layers){let t=this._layers[o];for(var i of t._parts){t.featureAttributes&&t.featureAttributes.tabindex&&e.featureIndex.addToIndex(t,t.getPCRSCenter(),i.path);for(var n of i.subrings)n.attr&&n.attr.tabindex&&e.featureIndex.addToIndex(t,n.center,n.path)}}},_checkRender:function(t,e,o){var i=this._featureEl.getAttribute("min"),n=this._featureEl.getAttribute("max");return!(o<t||t<e)&&(null===i&&null===n||!(null!==i&&t<+i||null!==n&&+n<t))},_handleFocus:function(t){if([9,16,27,37,38,39,40].includes(t.keyCode)&&"keydown"===t.type){var e=this._map.featureIndex.currentIndex;if(37===t.keyCode||38===t.keyCode)L.DomEvent.stop(t),this._map.featureIndex.inBoundFeatures[e].path.setAttribute("tabindex",-1),0===e?(this._map.featureIndex.inBoundFeatures[this._map.featureIndex.inBoundFeatures.length-1].path.focus(),this._map.featureIndex.currentIndex=this._map.featureIndex.inBoundFeatures.length-1):(this._map.featureIndex.inBoundFeatures[e-1].path.focus(),this._map.featureIndex.currentIndex--);else if(39===t.keyCode||40===t.keyCode)L.DomEvent.stop(t),this._map.featureIndex.inBoundFeatures[e].path.setAttribute("tabindex",-1),e===this._map.featureIndex.inBoundFeatures.length-1?(this._map.featureIndex.inBoundFeatures[0].path.focus(),this._map.featureIndex.currentIndex=0):(this._map.featureIndex.inBoundFeatures[e+1].path.focus(),this._map.featureIndex.currentIndex++);else if(27===t.keyCode){if("g"!==(this._map.options.mapEl.shadowRoot||this._map.options.mapEl.querySelector(".mapml-web-map").shadowRoot).activeElement.nodeName)return;this._map._container.focus()}else if(9===t.keyCode){let t=this;setTimeout(function(){t._map.featureIndex.inBoundFeatures[0].path.setAttribute("tabindex",0)},0)}}else[9,16,13,27,37,38,39,40,49,50,51,52,53,54,55].includes(t.keyCode)||(this._map.featureIndex.currentIndex=0,this._map.featureIndex.inBoundFeatures[0].path.focus());"G"===t.target.tagName.toUpperCase()&&([9,13,16,37,38,39,40,49,50,51,52,53,54,55,27].includes(t.keyCode)&&"keyup"===t.type?this.openTooltip():13===t.keyCode||32===t.keyCode?(this.closeTooltip(),!this.options.link&&this.options.onEachFeature&&(L.DomEvent.stop(t),this.openPopup())):this.closeTooltip())},addLayer:function(t){!t.options.link&&t.options.interactive&&this.options.onEachFeature(this.options.properties,t),L.FeatureGroup.prototype.addLayer.call(this,t)},_previousFeature:function(t){L.DomEvent.stop(t),this._map.featureIndex.currentIndex=Math.max(this._map.featureIndex.currentIndex-1,0);let e=this._map.featureIndex.inBoundFeatures[this._map.featureIndex.currentIndex];e.path.focus(),this._map.closePopup()},_nextFeature:function(t){L.DomEvent.stop(t),this._map.featureIndex.currentIndex=Math.min(this._map.featureIndex.currentIndex+1,this._map.featureIndex.inBoundFeatures.length-1);let e=this._map.featureIndex.inBoundFeatures[this._map.featureIndex.currentIndex];e.path.focus(),this._map.closePopup()},getPCRSCenter:function(){let e;for(var o in this._layers){let t=this._layers[o];e?e.extend(t.getPCRSCenter()):e=L.bounds(t.getPCRSCenter(),t.getPCRSCenter())}return e.getCenter()}}),At=L.Handler.extend({addHooks:function(){this._map.on({layeradd:this.totalBounds,layerremove:this.totalBounds}),this._map.options.mapEl.addEventListener("moveend",this.announceBounds),this._map.dragging._draggable.addEventListener("dragstart",this.dragged),this._map.options.mapEl.addEventListener("mapfocused",this.focusAnnouncement)},removeHooks:function(){this._map.off({layeradd:this.totalBounds,layerremove:this.totalBounds}),this._map.options.mapEl.removeEventListener("moveend",this.announceBounds),this._map.dragging._draggable.removeEventListener("dragstart",this.dragged),this._map.options.mapEl.removeEventListener("mapfocused",this.focusAnnouncement)},focusAnnouncement:function(){let i=this;setTimeout(function(){let t=(i.querySelector(".mapml-web-map")?i.querySelector(".mapml-web-map"):i).shadowRoot.querySelector(".leaflet-container");var e=i._map.getZoom();M._gcrsToTileMatrix(i);let o=M.options.locale.amZoom+" "+e;e===i._map._layersMaxZoom?o=M.options.locale.amMaxZoom+" "+o:e===i._map._layersMinZoom&&(o=M.options.locale.amMinZoom+" "+o),t.setAttribute("aria-roledescription","region "+o),setTimeout(function(){t.removeAttribute("aria-roledescription")},2e3)},0)},announceBounds:function(){if(!(0<this._traversalCall)){var o=this._map.getZoom(),i=M.pixelToPCRSBounds(this._map.getPixelBounds(),o,this._map.options.projection);let t=!0;this._map.totalLayerBounds&&(t=o<=this._map._layersMaxZoom&&o>=this._map._layersMinZoom&&this._map.totalLayerBounds.overlaps(i));let e=(this.querySelector(".mapml-web-map")?this.querySelector(".mapml-web-map"):this).shadowRoot.querySelector(".mapml-screen-reader-output");M._gcrsToTileMatrix(this);var n,i=M.options.locale.amZoom+" "+o;t?(n=(this._history[this._historyIndex-1]?this._history[this._historyIndex-1]:this._history[this._historyIndex]).zoom,o===this._map._layersMaxZoom&&o!==n?e.innerText=M.options.locale.amMaxZoom+" "+i:o===this._map._layersMinZoom&&o!==n?e.innerText=M.options.locale.amMinZoom+" "+i:e.innerText=i):(n=this._history[this._historyIndex],i=this._history[this._historyIndex-1],this.back(),this._history.pop(),n.zoom!==i.zoom?e.innerText=M.options.locale.amZoomedOut:this._map.dragging._draggable.wasDragged?e.innerText=M.options.locale.amDraggedOut:n.x>i.x?e.innerText=M.options.locale.amEastBound:n.x<i.x?e.innerText=M.options.locale.amWestBound:n.y<i.y?e.innerText=M.options.locale.amNorthBound:n.y>i.y&&(e.innerText=M.options.locale.amSouthBound)),this._map.dragging._draggable.wasDragged=!1}},totalBounds:function(){let t=Object.keys(this._layers),o=L.bounds();t.forEach(t=>{var e;this._layers[t].layerBounds&&(o||(e=this._layers[t].layerBounds.getCenter(),o=L.bounds(e,e)),o.extend(this._layers[t].layerBounds.min),o.extend(this._layers[t].layerBounds.max))}),this.totalLayerBounds=o},dragged:function(){this.wasDragged=!0}}),Tt=L.Handler.extend({initialize:function(t){L.Handler.prototype.initialize.call(this,t),this.inBoundFeatures=[],this.outBoundFeatures=[],this.currentIndex=0,this._mapPCRSBounds=M.pixelToPCRSBounds(t.getPixelBounds(),t.getZoom(),t.options.projection)},addHooks:function(){this._map.on("mapkeyboardfocused",this._updateMapBounds,this),this._map.on("mapkeyboardfocused",this._sortIndex,this)},removeHooks:function(){this._map.off("mapkeyboardfocused",this._updateMapBounds),this._map.off("mapkeyboardfocused",this._sortIndex)},addToIndex:function(t,e,o){var i=this._mapPCRSBounds.getCenter(),i=Math.sqrt(Math.pow(e.x-i.x,2)+Math.pow(e.y-i.y,2));let n=this._mapPCRSBounds.contains(e)?this.inBoundFeatures:this.outBoundFeatures;i={path:o,layer:t,center:e,dist:i};o.setAttribute("tabindex",-1),n.push(i);for(let t=n.length-1;0<t&&n[t].dist<n[t-1].dist;t--){var a=n[t];n[t]=n[t-1],n[t-1]=a}this._mapPCRSBounds.contains(e)?this.inBoundFeatures=n:this.outBoundFeatures=n},cleanIndex:function(){this.currentIndex=0,this.inBoundFeatures=this.inBoundFeatures.filter(t=>{var e=this._mapPCRSBounds.contains(t.center);return t.path.setAttribute("tabindex",-1),t.layer._map&&!e&&this.outBoundFeatures.push(t),t.layer._map&&e}),this.outBoundFeatures=this.outBoundFeatures.filter(t=>{var e=this._mapPCRSBounds.contains(t.center);return t.path.setAttribute("tabindex",-1),t.layer._map&&e&&this.inBoundFeatures.push(t),t.layer._map&&!e})},_sortIndex:function(){if(this.cleanIndex(),0!==this.inBoundFeatures.length){let n=this._mapPCRSBounds.getCenter();this.inBoundFeatures.sort(function(t,e){var o=t.center,i=e.center;return t.dist=Math.sqrt(Math.pow(o.x-n.x,2)+Math.pow(o.y-n.y,2)),e.dist=Math.sqrt(Math.pow(i.x-n.x,2)+Math.pow(i.y-n.y,2)),t.dist-e.dist}),this.inBoundFeatures[0].path.setAttribute("tabindex",0)}},_updateMapBounds:function(t){this._mapPCRSBounds=M.pixelToPCRSBounds(this._map.getPixelBounds(),this._map.getZoom(),this._map.options.projection)}}),St={featureIndexOverlayOption:!1,announceMovement:!1,announceScale:{metric:!0,imperial:!1},defaultExtCoor:"pcrs",defaultLocCoor:"gcrs",locale:{cmBack:"Back",cmForward:"Forward",cmReload:"Reload",cmToggleControls:"Toggle Controls",cmCopyCoords:"Copy",cmCopyMapML:"Map",cmCopyExtent:"Extent",cmCopyLocation:"Location",cmToggleDebug:"Toggle Debug Mode",cmPasteLayer:"Paste",cmViewSource:"View Map Source",lmZoomToLayer:"Zoom To Layer",lmCopyLayer:"Copy Layer",lcOpacity:"Opacity",btnAttribution:"Map data attribution",btnZoomIn:"Zoom in",btnZoomOut:"Zoom out",btnFullScreen:"View Fullscreen",btnExitFullScreen:"Exit Fullscreen",btnLocTrackOn:"Show my location - location tracking on",btnMyLocTrackOn:"My current location, shown on map",btnLocTrackOff:"Show my location - location tracking off",btnLocTrackLastKnown:"Show my location - last known location shown",btnMyLastKnownLocTrackOn:"My last known location, shown on map",amZoom:"zoom level",amColumn:"column",amRow:"row",amMaxZoom:"At maximum zoom level, zoom in disabled",amMinZoom:"At minimum zoom level, zoom out disabled",amZoomedOut:"Zoomed out of bounds, returning to",amDraggedOut:"Dragged out of bounds, returning to",amEastBound:"Reached east bound, panning east disabled",amWestBound:"Reached west bound, panning west disabled",amNorthBound:"Reached north bound, panning north disabled",amSouthBound:"Reached south bound, panning south disabled",kbdShortcuts:"Keyboard shortcuts",kbdMovement:"Movement keys",kbdFeature:"Feature navigation keys",kbdPanUp:"Pan up",kbdPanDown:"Pan down",kbdPanLeft:"Pan left",kbdPanRight:"Pan right",kbdPanIncrement:"pan increment",kbdZoom:"Zoom in/out 3 levels",kbdPrevFeature:"Previous feature",kbdNextFeature:"Next feature",dfLayer:"Layer",popupZoom:"Zoom to here",dfPastedLayer:"Pasted layer"}};L.Map.Keyboard.include({_onKeyDown:function(t){if(!t.altKey&&!t.metaKey){var e,o=t.keyCode,i=this._map;if(o in this._panKeys)i._panAnim&&i._panAnim._inProgress||(e=this._panKeys[o],t.shiftKey&&(e=L.point(e).multiplyBy(3)),t.ctrlKey&&(e=L.point(e).divideBy(5)),i.panBy(e),i.options.maxBounds&&i.panInsideBounds(i.options.maxBounds));else if(o in this._zoomKeys)(o in{187:187,107:107,61:61,171:171}&&i._layersMaxZoom!==i.getZoom()||o in{189:189,109:109,54:54,173:173}&&i._layersMinZoom!==i.getZoom())&&i.setZoom(i.getZoom()+(t.shiftKey?3:1)*this._zoomKeys[o]);else{if(27!==o||!i._popup||!i._popup.options.closeOnEscapeKey)return;i.closePopup()}L.DomEvent.stop(t)}}});function It(t){return new zt(t)}var kt,wt,zt=L.Layer.extend({onAdd:function(t){this._container=L.DomUtil.create("div","mapml-feature-index-box",t._container),this._container.innerHTML='<svg xmlns="http://www.w3.org/2000/svg" xml:space="preserve" viewBox="0 0 100 100"><path fill="none" stroke="#000" stroke-linecap="round" stroke-linejoin="round" stroke-width="3" d="M0 0h100v100H0z" color="#000" overflow="visible"/></svg>',this._output=L.DomUtil.create("output","mapml-feature-index",t._container),this._output.setAttribute("role","status"),this._output.setAttribute("aria-live","polite"),this._output.setAttribute("aria-atomic","true"),this._body=L.DomUtil.create("span","mapml-feature-index-content",this._output),this._body.index=0,this._output.initialFocus=!1,t.on("layerchange layeradd layerremove overlayremove",this._toggleEvents,this),t.on("moveend focus templatedfeatureslayeradd",this._checkOverlap,this),t.on("keydown",this._onKeyDown,this),this._addOrRemoveFeatureIndex()},_calculateReticleBounds:function(){let t=this._map.getPixelBounds();var e=t.getCenter(),o=Math.abs(t.min.x-t.max.x)/this._map.options.mapEl.width,i=Math.abs(t.min.y-t.max.y)/this._map.options.mapEl.height;let n=getComputedStyle(this._container).width.replace(/[^\d.]/g,"");"%"===getComputedStyle(this._container).width.slice(-1)&&(n=n*this._map.options.mapEl.width/100);var a=o*n/2,o=i*n/2,i=L.point(e.x-a,e.y+o),o=L.point(e.x+a,e.y-o),o=L.bounds(i,o);return M.pixelToPCRSBounds(o,this._map.getZoom(),this._map.options.projection)},_checkOverlap:function(t){if("focus"===t.type&&(this._output.initialFocus=!0),this._output.initialFocus)if(this._output.popupClosed)this._output.popupClosed=!1;else{this._map.fire("mapkeyboardfocused");let n=this._calculateReticleBounds(),a=this._map.featureIndex.inBoundFeatures,r=1,t=Object.keys(a),s=this._body;s.innerHTML="",s.index=0,s.allFeatures=[],t.forEach(t=>{let e=a[t].layer;var o=a[t].layer._layers;let i=L.bounds();if(o){let t=Object.keys(o);t.forEach(t=>{i=i||L.bounds(e._layers[t]._bounds.min,e._layers[t]._bounds.max),i.extend(e._layers[t]._bounds.min),i.extend(e._layers[t]._bounds.max)})}else e._bounds&&(i=L.bounds(e._bounds.min,e._bounds.max));n.overlaps(i)&&(t=a[t].path.getAttribute("aria-label"),r<8&&s.appendChild(this._updateOutput(t,r,r)),r%7!=0&&1!==r||s.allFeatures.push([]),s.allFeatures[Math.floor((r-1)/7)].push({label:t,index:r,layer:e}),s.allFeatures[1]&&1===s.allFeatures[1].length&&s.appendChild(this._updateOutput("More results",0,9)),r+=1)}),this._addToggleKeys()}},_updateOutput:function(t,e,o){let i=document.createElement("span");return i.setAttribute("data-index",e),i.innerHTML=`<kbd>${o}</kbd>`+" "+t+"<span>, </span>",i},_addToggleKeys:function(){let e=this._body.allFeatures;for(let t=0;t<e.length;t++){if(0===e[t].length)return;e[t-1]&&e[t].push({label:"Previous results"}),e[t+1]&&0<e[t+1].length&&e[t].push({label:"More results"})}},_onKeyDown:function(e){var t=this._body,o=e.originalEvent.keyCode;if(49<=o&&o<=55){if(t.allFeatures[t.index]){e=t.allFeatures[t.index][o-49];if(e){let t=e.layer;t&&(this._map.featureIndex.currentIndex=e.index-1,t._popup?(this._map.closePopup(),t.openPopup()):t.options.group.focus())}}}else 56===o?this._newContent(t,-1):57===o&&this._newContent(t,1)},_newContent:function(o,t){var e=o.firstChild.getAttribute("data-index"),i=o.allFeatures[Math.floor((e-1)/7+t)];if(i&&0<i.length){o.innerHTML="",o.index+=t;for(let e=0;e<i.length;e++){var n=i[e],a=n.index||0;let t=e+1;"More results"===n.label&&(t=9),"Previous results"===n.label&&(t=8),o.appendChild(this._updateOutput(n.label,a,t))}}},_toggleEvents:function(){this._map.on("viewreset move moveend focus blur popupclose",this._addOrRemoveFeatureIndex,this)},_addOrRemoveFeatureIndex:function(t){var e=this._body.allFeatures?this._body.allFeatures.length:0;if(this._output.initialFocus){if(this._output.hasAttribute("aria-hidden")){let t=this;setTimeout(function(){t._output.removeAttribute("aria-hidden")},100)}}else this._output.setAttribute("aria-hidden","true");t&&"popupclose"===t.type?(this._output.setAttribute("aria-hidden","true"),this._output.popupClosed=!0):t&&"focus"===t.type?(this._container.removeAttribute("hidden"),0!==e&&this._output.classList.remove("mapml-screen-reader-output")):t&&t.originalEvent&&"pointermove"===t.originalEvent.type?(this._container.setAttribute("hidden",""),this._output.classList.add("mapml-screen-reader-output")):t&&t.target._popup?this._container.setAttribute("hidden",""):t&&"blur"===t.type?(this._container.setAttribute("hidden",""),this._output.classList.add("mapml-screen-reader-output"),this._output.initialFocus=!1,this._addOrRemoveFeatureIndex()):this._map.isFocused&&t?(this._container.removeAttribute("hidden"),0!==e?this._output.classList.remove("mapml-screen-reader-output"):this._output.classList.add("mapml-screen-reader-output")):(this._container.setAttribute("hidden",""),this._output.classList.add("mapml-screen-reader-output"))}});kt=window,wt={},kt.M=wt,function(){wt.detectImagePath=function(t){var e=L.DomUtil.create("div","leaflet-default-icon-path",t),o=L.DomUtil.getStyle(e,"background-image")||L.DomUtil.getStyle(e,"backgroundImage");return t.removeChild(e),o=null===o||0!==o.indexOf("url")?"":o.replace(/^url\(["']?/,"").replace(/marker-icon\.png["']?\)$/,"")},wt.mime="text/mapml";var t=kt.document.head.querySelector("map-options");wt.options=St,t&&(wt.options=Object.assign(wt.options,JSON.parse(t.innerHTML))),wt.WGS84=new L.Proj.CRS("EPSG:4326","+proj=longlat +ellps=WGS84 +datum=WGS84 +no_defs ",{origin:[-180,90],bounds:L.bounds([[-180,-90],[180,90]]),resolutions:[.703125,.3515625,.17578125,.087890625,.0439453125,.02197265625,.010986328125,.0054931640625,.00274658203125,.001373291015625,.0006866455078125,.0003433227539062,.0001716613769531,858306884766e-16,429153442383e-16,214576721191e-16,107288360596e-16,53644180298e-16,26822090149e-16,13411045074e-16,6.705522537e-7,3.352761269e-7],crs:{tcrs:{horizontal:{name:"x",min:0,max:t=>Math.round(wt.WGS84.options.bounds.getSize().x/wt.WGS84.options.resolutions[t])},vertical:{name:"y",min:0,max:t=>Math.round(wt.WGS84.options.bounds.getSize().y/wt.WGS84.options.resolutions[t])},bounds:t=>L.bounds([wt.WGS84.options.crs.tcrs.horizontal.min,wt.WGS84.options.crs.tcrs.vertical.min],[wt.WGS84.options.crs.tcrs.horizontal.max(t),wt.WGS84.options.crs.tcrs.vertical.max(t)])},pcrs:{horizontal:{name:"longitude",get min(){return wt.WGS84.options.crs.gcrs.horizontal.min},get max(){return wt.WGS84.options.crs.gcrs.horizontal.max}},vertical:{name:"latitude",get min(){return wt.WGS84.options.crs.gcrs.vertical.min},get max(){return wt.WGS84.options.crs.gcrs.vertical.max}},get bounds(){return wt.WGS84.options.bounds}},gcrs:{horizontal:{name:"longitude",min:-180,max:180},vertical:{name:"latitude",min:-90,max:90},get bounds(){return L.latLngBounds([wt.WGS84.options.crs.gcrs.vertical.min,wt.WGS84.options.crs.gcrs.horizontal.min],[wt.WGS84.options.crs.gcrs.vertical.max,wt.WGS84.options.crs.gcrs.horizontal.max])}},map:{horizontal:{name:"i",min:0,max:t=>t.getSize().x},vertical:{name:"j",min:0,max:t=>t.getSize().y},bounds:t=>L.bounds(L.point([0,0]),t.getSize())},tile:{horizontal:{name:"i",min:0,max:256},vertical:{name:"j",min:0,max:256},get bounds(){return L.bounds([wt.WGS84.options.crs.tile.horizontal.min,wt.WGS84.options.crs.tile.vertical.min],[wt.WGS84.options.crs.tile.horizontal.max,wt.WGS84.options.crs.tile.vertical.max])}},tilematrix:{horizontal:{name:"column",min:0,max:t=>Math.round(wt.WGS84.options.crs.tcrs.horizontal.max(t)/wt.WGS84.options.crs.tile.bounds.getSize().x)},vertical:{name:"row",min:0,max:t=>Math.round(wt.WGS84.options.crs.tcrs.vertical.max(t)/wt.WGS84.options.crs.tile.bounds.getSize().y)},bounds:t=>L.bounds([wt.WGS84.options.crs.tilematrix.horizontal.min,wt.WGS84.options.crs.tilematrix.vertical.min],[wt.WGS84.options.crs.tilematrix.horizontal.max(t),wt.WGS84.options.crs.tilematrix.vertical.max(t)])}}}),wt.CBMTILE=new L.Proj.CRS("EPSG:3978","+proj=lcc +lat_1=49 +lat_2=77 +lat_0=49 +lon_0=-95 +x_0=0 +y_0=0 +ellps=GRS80 +datum=NAD83 +units=m +no_defs",{origin:[-34655800,3931e4],bounds:L.bounds([[-34655800,-39e6],[1e7,3931e4]]),resolutions:[38364.660062653464,22489.62831258996,13229.193125052918,7937.5158750317505,4630.2175937685215,2645.8386250105837,1587.5031750063501,926.0435187537042,529.1677250021168,317.50063500127004,185.20870375074085,111.12522225044451,66.1459656252646,38.36466006265346,22.48962831258996,13.229193125052918,7.9375158750317505,4.6302175937685215,2.6458386250105836,1.5875031750063502,.9260435187537043,.5291677250021167,.31750063500127,.18520870375074083,.11112522225044451,.06614596562526459],crs:{tcrs:{horizontal:{name:"x",min:0,max:t=>Math.round(wt.CBMTILE.options.bounds.getSize().x/wt.CBMTILE.options.resolutions[t])},vertical:{name:"y",min:0,max:t=>Math.round(wt.CBMTILE.options.bounds.getSize().y/wt.CBMTILE.options.resolutions[t])},bounds:t=>L.bounds([wt.CBMTILE.options.crs.tcrs.horizontal.min,wt.CBMTILE.options.crs.tcrs.vertical.min],[wt.CBMTILE.options.crs.tcrs.horizontal.max(t),wt.CBMTILE.options.crs.tcrs.vertical.max(t)])},pcrs:{horizontal:{name:"easting",get min(){return wt.CBMTILE.options.bounds.min.x},get max(){return wt.CBMTILE.options.bounds.max.x}},vertical:{name:"northing",get min(){return wt.CBMTILE.options.bounds.min.y},get max(){return wt.CBMTILE.options.bounds.max.y}},get bounds(){return wt.CBMTILE.options.bounds}},gcrs:{horizontal:{name:"longitude",min:-141.01,max:-47.74},vertical:{name:"latitude",min:40.04,max:86.46},get bounds(){return L.latLngBounds([wt.CBMTILE.options.crs.gcrs.vertical.min,wt.CBMTILE.options.crs.gcrs.horizontal.min],[wt.CBMTILE.options.crs.gcrs.vertical.max,wt.CBMTILE.options.crs.gcrs.horizontal.max])}},map:{horizontal:{name:"i",min:0,max:t=>t.getSize().x},vertical:{name:"j",min:0,max:t=>t.getSize().y},bounds:t=>L.bounds(L.point([0,0]),t.getSize())},tile:{horizontal:{name:"i",min:0,max:256},vertical:{name:"j",min:0,max:256},get bounds(){return L.bounds([wt.CBMTILE.options.crs.tile.horizontal.min,wt.CBMTILE.options.crs.tile.vertical.min],[wt.CBMTILE.options.crs.tile.horizontal.max,wt.CBMTILE.options.crs.tile.vertical.max])}},tilematrix:{horizontal:{name:"column",min:0,max:t=>Math.round(wt.CBMTILE.options.crs.tcrs.horizontal.max(t)/wt.CBMTILE.options.crs.tile.bounds.getSize().x)},vertical:{name:"row",min:0,max:t=>Math.round(wt.CBMTILE.options.crs.tcrs.vertical.max(t)/wt.CBMTILE.options.crs.tile.bounds.getSize().y)},bounds:t=>L.bounds([0,0],[wt.CBMTILE.options.crs.tilematrix.horizontal.max(t),wt.CBMTILE.options.crs.tilematrix.vertical.max(t)])}}}),wt.APSTILE=new L.Proj.CRS("EPSG:5936","+proj=stere +lat_0=90 +lat_ts=50 +lon_0=-150 +k=0.994 +x_0=2000000 +y_0=2000000 +datum=WGS84 +units=m +no_defs",{origin:[-28567784.109255,32567784.109255],bounds:L.bounds([[-28567784.109254867,-28567784.109254755],[32567784.109255023,32567784.10925506]]),resolutions:[238810.813354,119405.406677,59702.7033384999,29851.3516692501,14925.675834625,7462.83791731252,3731.41895865639,1865.70947932806,932.854739664032,466.427369832148,233.213684916074,116.606842458037,58.3034212288862,29.1517106145754,14.5758553072877,7.28792765351156,3.64396382688807,1.82198191331174,.910990956788164,.45549547826179],crs:{tcrs:{horizontal:{name:"x",min:0,max:t=>Math.round(wt.APSTILE.options.bounds.getSize().x/wt.APSTILE.options.resolutions[t])},vertical:{name:"y",min:0,max:t=>Math.round(wt.APSTILE.options.bounds.getSize().y/wt.APSTILE.options.resolutions[t])},bounds:t=>L.bounds([wt.APSTILE.options.crs.tcrs.horizontal.min,wt.APSTILE.options.crs.tcrs.vertical.min],[wt.APSTILE.options.crs.tcrs.horizontal.max(t),wt.APSTILE.options.crs.tcrs.vertical.max(t)])},pcrs:{horizontal:{name:"easting",get min(){return wt.APSTILE.options.bounds.min.x},get max(){return wt.APSTILE.options.bounds.max.x}},vertical:{name:"northing",get min(){return wt.APSTILE.options.bounds.min.y},get max(){return wt.APSTILE.options.bounds.max.y}},get bounds(){return wt.APSTILE.options.bounds}},gcrs:{horizontal:{name:"longitude",min:-180,max:180},vertical:{name:"latitude",min:60,max:90},get bounds(){return L.latLngBounds([wt.APSTILE.options.crs.gcrs.vertical.min,wt.APSTILE.options.crs.gcrs.horizontal.min],[wt.APSTILE.options.crs.gcrs.vertical.max,wt.APSTILE.options.crs.gcrs.horizontal.max])}},map:{horizontal:{name:"i",min:0,max:t=>t.getSize().x},vertical:{name:"j",min:0,max:t=>t.getSize().y},bounds:t=>L.bounds(L.point([0,0]),t.getSize())},tile:{horizontal:{name:"i",min:0,max:256},vertical:{name:"j",min:0,max:256},get bounds(){return L.bounds([wt.APSTILE.options.crs.tile.horizontal.min,wt.APSTILE.options.crs.tile.vertical.min],[wt.APSTILE.options.crs.tile.horizontal.max,wt.APSTILE.options.crs.tile.vertical.max])}},tilematrix:{horizontal:{name:"column",min:0,max:t=>Math.round(wt.APSTILE.options.crs.tcrs.horizontal.max(t)/wt.APSTILE.options.crs.tile.bounds.getSize().x)},vertical:{name:"row",min:0,max:t=>Math.round(wt.APSTILE.options.crs.tcrs.vertical.max(t)/wt.APSTILE.options.crs.tile.bounds.getSize().y)},bounds:t=>L.bounds([0,0],[wt.APSTILE.options.crs.tilematrix.horizontal.max(t),wt.APSTILE.options.crs.tilematrix.vertical.max(t)])}}}),wt.OSMTILE=L.CRS.EPSG3857,L.setOptions(wt.OSMTILE,{origin:[-20037508.342787,20037508.342787],bounds:L.bounds([[-20037508.342787,-20037508.342787],[20037508.342787,20037508.342787]]),resolutions:[156543.0339,78271.51695,39135.758475,19567.8792375,9783.93961875,4891.969809375,2445.9849046875,1222.9924523438,611.49622617188,305.74811308594,152.87405654297,76.437028271484,38.218514135742,19.109257067871,9.5546285339355,4.7773142669678,2.3886571334839,1.1943285667419,.59716428337097,.29858214168549,.14929107084274,.074645535421371,.03732276771068573,.018661383855342866,.009330691927671433],crs:{tcrs:{horizontal:{name:"x",min:0,max:t=>Math.round(wt.OSMTILE.options.bounds.getSize().x/wt.OSMTILE.options.resolutions[t])},vertical:{name:"y",min:0,max:t=>Math.round(wt.OSMTILE.options.bounds.getSize().y/wt.OSMTILE.options.resolutions[t])},bounds:t=>L.bounds([wt.OSMTILE.options.crs.tcrs.horizontal.min,wt.OSMTILE.options.crs.tcrs.vertical.min],[wt.OSMTILE.options.crs.tcrs.horizontal.max(t),wt.OSMTILE.options.crs.tcrs.vertical.max(t)])},pcrs:{horizontal:{name:"easting",get min(){return wt.OSMTILE.options.bounds.min.x},get max(){return wt.OSMTILE.options.bounds.max.x}},vertical:{name:"northing",get min(){return wt.OSMTILE.options.bounds.min.y},get max(){return wt.OSMTILE.options.bounds.max.y}},get bounds(){return wt.OSMTILE.options.bounds}},gcrs:{horizontal:{name:"longitude",get min(){return wt.OSMTILE.unproject(wt.OSMTILE.options.bounds.min).lng},get max(){return wt.OSMTILE.unproject(wt.OSMTILE.options.bounds.max).lng}},vertical:{name:"latitude",get min(){return wt.OSMTILE.unproject(wt.OSMTILE.options.bounds.min).lat},get max(){return wt.OSMTILE.unproject(wt.OSMTILE.options.bounds.max).lat}},get bounds(){return L.latLngBounds([wt.OSMTILE.options.crs.gcrs.vertical.min,wt.OSMTILE.options.crs.gcrs.horizontal.min],[wt.OSMTILE.options.crs.gcrs.vertical.max,wt.OSMTILE.options.crs.gcrs.horizontal.max])}},map:{horizontal:{name:"i",min:0,max:t=>t.getSize().x},vertical:{name:"j",min:0,max:t=>t.getSize().y},bounds:t=>L.bounds(L.point([0,0]),t.getSize())},tile:{horizontal:{name:"i",min:0,max:256},vertical:{name:"j",min:0,max:256},get bounds(){return L.bounds([wt.OSMTILE.options.crs.tile.horizontal.min,wt.OSMTILE.options.crs.tile.vertical.min],[wt.OSMTILE.options.crs.tile.horizontal.max,wt.OSMTILE.options.crs.tile.vertical.max])}},tilematrix:{horizontal:{name:"column",min:0,max:t=>Math.round(wt.OSMTILE.options.crs.tcrs.horizontal.max(t)/wt.OSMTILE.options.crs.tile.bounds.getSize().x)},vertical:{name:"row",min:0,max:t=>Math.round(wt.OSMTILE.options.crs.tcrs.vertical.max(t)/wt.OSMTILE.options.crs.tile.bounds.getSize().y)},bounds:t=>L.bounds([0,0],[wt.OSMTILE.options.crs.tilematrix.horizontal.max(t),wt.OSMTILE.options.crs.tilematrix.vertical.max(t)])}}})}(),wt._handleLink=Y,wt.convertPCRSBounds=R,wt.axisToXY=D,wt.csToAxes=O,wt._convertAndFormatPCRS=N,wt.axisToCS=q,wt._parseNumber=K,wt._extractInputBounds=F,wt._splitCoordinate=W,wt.boundsToPCRSBounds=j,wt.pixelToPCRSBounds=H,wt._metaContentToObject=G,wt._coordsToArray=V,wt._parseStylesheetAsHTML=$,wt.pointToPCRSPoint=U,wt.pixelToPCRSPoint=Z,wt._gcrsToTileMatrix=X,wt._pasteLayer=Q,wt._properties2Table=J,wt._updateExtent=tt,wt.geojson2mapml=et,wt._breakArray=ot,wt._table2properties=it,wt._geometry2geojson=nt,wt._pcrsToGcrs=at,wt.mapml2geojson=rt,wt.getMaxZoom=st,wt.QueryHandler=P,wt.ContextMenu=B,wt.AnnounceMovement=At,wt.FeatureIndex=Tt,L.Map.addInitHook("addHandler","query",wt.QueryHandler),L.Map.addInitHook("addHandler","contextMenu",wt.ContextMenu),L.Map.addInitHook("addHandler","announceMovement",wt.AnnounceMovement),L.Map.addInitHook("addHandler","featureIndex",wt.FeatureIndex),wt.MapMLLayer=x,wt.mapMLLayer=u,wt.ImageLayer=g,wt.imageLayer=m,wt.TemplatedImageLayer=f,wt.templatedImageLayer=l,wt.TemplatedFeaturesLayer=y,wt.templatedFeaturesLayer=s,wt.TemplatedLayer=_,wt.templatedLayer=r,wt.TemplatedTileLayer=h,wt.templatedTileLayer=a,wt.FeatureLayer=p,wt.featureLayer=n,wt.LayerControl=i,wt.layerControl=e,wt.AttributionButton=lt,wt.attributionButton=mt,wt.ReloadButton=pt,wt.reloadButton=ut,wt.ScaleBar=ht,wt.scaleBar=ct,wt.FullscreenButton=dt,wt.fullscreenButton=_t,wt.geolocationButton=yt,wt.StaticTileLayer=o,wt.staticTileLayer=t,wt.DebugOverlay=b,wt.debugOverlay=c,wt.Crosshair=vt,wt.crosshair=ft,wt.FeatureIndexOverlay=zt,wt.featureIndexOverlay=It,wt.Feature=Mt,wt.feature=gt,wt.FeatureRenderer=Et,wt.featureRenderer=xt,wt.FeatureGroup=Ct,wt.featureGroup=bt}();
//# sourceMappingURL=mapml.js.map
>>>>>>> 4af0ac91de ([GEOS-10940] Update MapML viewer to release 0.11.0)
