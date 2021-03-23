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
      this._groups = this._groupTiles(this.options.tileContainer.getElementsByTagName('tile'));
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
          tileElem = document.createElement('tile');
      tileElem.setAttribute("col",coords.x);
      tileElem.setAttribute("row",coords.y);
      tileElem.setAttribute("zoom",coords.z);
      
      for(let i = 0;i<tileGroup.length;i++){
        let tile= document.createElement('img');
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
      let meta = M.metaContentToObject(container.getElementsByTagName('tiles')[0].getAttribute('zoom')),
          zoom = {},tiles = container.getElementsByTagName("tile");
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
        if(e.relatedTarget && e.relatedTarget.parentElement && 
            (e.relatedTarget.className === "mapml-contextmenu mapml-layer-menu" || 
            e.relatedTarget.parentElement.className === "mapml-contextmenu mapml-layer-menu") ||
            (this._map && this._map.contextMenu._layerMenu.style.display === "block"))
         return this;

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
          let anim = L.DomUtil.create("style", "mapml-feature-animation", this._container);
          anim.innerHTML = `@keyframes pathSelect {
          0% {stroke: white;}
          50% {stroke: black;}
        }
        g:focus > path,
        path:focus {
          animation-name: pathSelect;
          animation-duration: 1s;
          stroke-width: 5;
          stroke: black;
        }`;
        }

        this._layers = {};
        if(this.options.query){
          this._mapmlFeatures = mapml;
          this.isVisible = true;
          let native = this._getNativeVariables(mapml);
          this.options.nativeZoom = native.zoom;
          this.options.nativeCS = native.cs;
        }
        if (mapml && !this.options.query) {
          let native = this._getNativeVariables(mapml);
          //needed to check if the feature is static or not, since this method is used by templated also
          if(!mapml.querySelector('extent') && mapml.querySelector('feature') && this.options.static){
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
        if(this.options.query && this._mapmlFeatures.querySelectorAll("feature")[e.i]){
          let feature = this._mapmlFeatures.querySelectorAll("feature")[e.i];
          this.clearLayers();
          this.addData(feature, this.options.nativeCS, this.options.nativeZoom);
          e.popup._navigationBar.querySelector("p").innerText = (e.i + 1) + "/" + this.options._leafletLayer._totalFeatureCount;
          e.popup._content.querySelector("iframe").srcdoc = `<meta http-equiv="content-security-policy" content="script-src 'none';">` + feature.querySelector("properties").innerHTML;
        }
      },

      _getNativeVariables: function(mapml){
        let nativeZoom = mapml.querySelector("meta[name=zoom]") && 
            +M.metaContentToObject(mapml.querySelector("meta[name=zoom]").getAttribute("content")).value || 0;
        let nativeCS = mapml.querySelector("meta[name=cs]") && 
            M.metaContentToObject(mapml.querySelector("meta[name=cs]").getAttribute("content")).content || "GCRS";
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
            projection = container.querySelector('meta[name=projection]') &&
                      M.metaContentToObject(
                        container.querySelector('meta[name=projection]').getAttribute('content'))
                        .content.toUpperCase() || FALLBACK_PROJECTION;
        try{

          let meta = container.querySelector('meta[name=extent]') && 
                      M.metaContentToObject(
                        container.querySelector('meta[name=extent]').getAttribute('content'));

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
        let nMin = 100,nMax=0, features = container.getElementsByTagName('feature'),meta,projection;
        for(let i =0;i<features.length;i++){
          let lZoom = +features[i].getAttribute('zoom');
          if(!features[i].getAttribute('zoom'))lZoom = nativeZoom;
          nMax = Math.max(nMax, lZoom);
          nMin = Math.min(nMin, lZoom);
        }
        try{
          projection = M.metaContentToObject(container.querySelector('meta[name=projection]').getAttribute('content')).content;
          meta = M.metaContentToObject(container.querySelector('meta[name=zoom]').getAttribute('content'));
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
        var features = mapml.nodeType === Node.DOCUMENT_NODE || mapml.nodeName === "LAYER-" ? mapml.getElementsByTagName("feature") : null,
            i, len, feature;

        var linkedStylesheets = mapml.nodeType === Node.DOCUMENT_NODE ? mapml.querySelector("link[rel=stylesheet],style") : null;
        if (linkedStylesheets) {
          var base = mapml.querySelector('base') && mapml.querySelector('base').hasAttribute('href') ? 
              new URL(mapml.querySelector('base').getAttribute('href')).href : 
              mapml.URL;
          M.parseStylesheetAsHTML(mapml,base,this._container);
        }
        if (features) {
         for (i = 0, len = features.length; i < len; i++) {
          // Only add this if geometry is set and not null
          feature = features[i];
          var geometriesExist = feature.getElementsByTagName("geometry").length && feature.getElementsByTagName("coordinates").length;
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
        let zoom = mapml.getAttribute("zoom") || nativeZoom, title = mapml.querySelector("featurecaption");
        title = title ? title.innerHTML : "Feature";

        let layer = this.geometryToLayer(mapml, options.pointToLayer, options, nativeCS, +zoom, title);
        if (layer) {
          layer.properties = mapml.getElementsByTagName('properties')[0];
          
          // if the layer is being used as a query handler output, it will have
          // a color option set.  Otherwise, copy classes from the feature
          if (!layer.options.color && mapml.hasAttribute('class')) {
            layer.options.className = mapml.getAttribute('class');
          }
          layer.defaultOptions = layer.options;
          this.resetStyle(layer);

          if (options.onEachFeature) {
            options.onEachFeature(layer.properties, layer);
            layer.bindTooltip(title, { interactive:true, sticky: true, });
            if(layer._events){
              if(!layer._events.keypress) layer._events.keypress = [];
              layer._events.keypress.push({
                "ctx": layer,
                "fn": this._onSpacePress,
              });
            }
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
          if(toDelete[i].classList.contains("mapml-feature-animation")) continue;
          this._container.removeChild(toDelete[i]);
        }
      },
      _onSpacePress: function(e){
        if(e.originalEvent.keyCode === 32){
          this._openPopup(e);
        }
      },
    geometryToLayer: function (mapml, pointToLayer, vectorOptions, nativeCS, zoom, title) {
      let geometry = mapml.tagName.toUpperCase() === 'FEATURE' ? mapml.getElementsByTagName('geometry')[0] : mapml,
          cs = geometry.getAttribute("cs") || nativeCS, subFeatures = geometry, group = [], multiGroup;

      if(geometry.firstElementChild.tagName === "GEOMETRYCOLLECTION" || geometry.firstElementChild.tagName === "MULTIPOLYGON")
        subFeatures = geometry.firstElementChild;

      for(let geo of subFeatures.children){
        if(group.length > 0) multiGroup = group[group.length - 1].group;
        group.push(M.feature(geo, Object.assign(vectorOptions,
          { nativeCS: cs,
            nativeZoom: zoom,
            projection: this.options.projection,
            featureID: mapml.id,
            multiGroup: multiGroup,
            accessibleTitle: title,
          })));
      }
      return M.featureGroup(group);
    },
  });
  var mapMlFeatures = function (mapml, options) {
  	return new MapMLFeatures(mapml, options);
  };

  var TemplatedTileLayer = L.TileLayer.extend({
      // a TemplateTileLayer is similar to a L.TileLayer except its templates are
      // defined by the <extent><template/></extent>
      // content found in the MapML document.  As such, the client map does not
      // 'revisit' the server for more MapML content, it simply fills the map extent
      // with tiles for which it generates requests on demand (as the user pans/zooms/resizes
      // the map)
      initialize: function(template, options) {
        // _setUpTileTemplateVars needs options.crs, not available unless we set
        // options first...
        let inputData = M.extractInputBounds(template);
        this.zoomBounds = inputData.zoomBounds;
        this.layerBounds=inputData.bounds;
        this.isVisible = true;
        L.extend(options, this.zoomBounds);
        options.tms = template.tms;
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

        if (this.options.opacity < 1) {
          this._updateOpacity();
        }
      },
      _handleMoveEnd : function(e){
        let mapZoom = this._map.getZoom();
        let mapBounds = M.pixelToPCRSBounds(this._map.getPixelBounds(),mapZoom,this._map.options.projection);
        this.isVisible = mapZoom <= this.options.maxZoom && mapZoom >= this.options.minZoom && 
                          this.layerBounds.overlaps(mapBounds);
        if(!(this.isVisible))return;
        this._parentOnMoveEnd();
      },
      createTile: function (coords) {
        let tileGroup = document.createElement("DIV"),
            tileSize = this._map.options.crs.options.crs.tile.bounds.max.x;
        L.DomUtil.addClass(tileGroup, "mapml-tile-group");
        L.DomUtil.addClass(tileGroup, "leaflet-tile");
        
        tileGroup.setAttribute("width", `${tileSize}`);
        tileGroup.setAttribute("height", `${tileSize}`);

        this._template.linkEl.dispatchEvent(new CustomEvent('tileloadstart', {
          detail:{
            x:coords.x,
            y:coords.y,
            zoom:coords.z,
            appendTile: (elem)=>{tileGroup.appendChild(elem);},
          },
        }));

        if (this._template.type.startsWith('image/')) {
          tileGroup.appendChild(L.TileLayer.prototype.createTile.call(this, coords, function(){}));
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
        let stylesheets = markup.querySelector('link[rel=stylesheet],style');
        if (stylesheets) {
          let base = markup.querySelector('base') && markup.querySelector('base').hasAttribute('href') ?
            new URL(markup.querySelector('base').getAttribute('href')).href :
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
              select = (inputs[i].tagName.toLowerCase() === "select"),
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
                if (!isNaN(Number.parseInt(min,10))) {
                  col.min = Number.parseInt(min,10);
                }
                if (!isNaN(Number.parseInt(max,10))) {
                  col.max = Number.parseInt(max,10);
                }
                break;
              case("row"):
                row = {
                  name: name,
                  min: crs.crs.tilematrix.vertical.min,
                  max:  crs.crs.tilematrix.vertical.max(crs.resolutions.length-1)
                };
                if (!isNaN(Number.parseInt(min,10))) {
                  row.min = Number.parseInt(min,10);
                }
                if (!isNaN(Number.parseInt(max,10))) {
                  row.max = Number.parseInt(max,10);
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
          } else if (type.toLowerCase() === "zoom") {
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
        tilematrix2pcrs = function (c,zoom) {
          return transformation.untransform(c.multiplyBy(tileSize),scale(zoom));
        },
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
            
            template.pcrs.bounds = L.bounds(
              tilematrix2pcrs(L.point([col.min,row.min]),zoom.value),
              tilematrix2pcrs(L.point([col.max,row.max]),zoom.value)
            );
            
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
      L.DomUtil.addClass(this._container,'mapml-templatedlayer-container');

      for (var i=0;i<templates.length;i++) {
        if (templates[i].rel === 'tile') {
            this._templates[i].layer = M.templatedTileLayer(templates[i], 
              L.Util.extend(options, {errorTileUrl: "data:image/gif;base64,R0lGODlhAQABAAAAACH5BAEKAAEALAAAAAABAAEAAAICTAEAOw==", zIndex: i, pane: this._container}));
        } else if (templates[i].rel === 'image') {
            this._templates[i].layer = M.templatedImageLayer(templates[i], L.Util.extend(options, {zIndex: i, pane: this._container}));
        } else if (templates[i].rel === 'features') {
            this._templates[i].layer = M.templatedFeaturesLayer(templates[i], L.Util.extend(options, {zIndex: i, pane: this._container}));
        } else if (templates[i].rel === 'query') {
            // add template to array of queryies to be added to map and processed
            // on click/tap events
            this.hasSetBoundsHandler = true;
            if (!this._queries) {
              this._queries = [];
            }
            let inputData = M.extractInputBounds(templates[i]);
            templates[i].layerBounds = inputData.bounds;
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
              select = (inputs[i].tagName.toLowerCase() === "select");
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
    reset: function (templates) {
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
              L.Util.extend(this.options, {errorTileUrl: "data:image/gif;base64,R0lGODlhAQABAAAAACH5BAEKAAEALAAAAAABAAEAAAICTAEAOw==", zIndex: i, pane: this._container}));
        } else if (templates[i].rel === 'image') {
            this._templates[i].layer = M.templatedImageLayer(templates[i], L.Util.extend(this.options, {zIndex: i, pane: this._container}));
        } else if (templates[i].rel === 'features') {
            this._templates[i].layer = M.templatedFeaturesLayer(templates[i], L.Util.extend(this.options, {zIndex: i, pane: this._container}));
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
  //  setZIndex: function (zIndex) {
  //      this.options.zIndex = zIndex;
  //      this._updateZIndex();
  //
  //      return this;
  //  },
  //  _updateZIndex: function () {
  //      if (this._container && this.options.zIndex !== undefined && this.options.zIndex !== null) {
  //          this._container.style.zIndex = this.options.zIndex;
  //      }
  //  },
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
        this.layerBounds=inputData.bounds;
        this.isVisible = true;
        this._template = template;
        this._container = L.DomUtil.create('div', 'leaflet-layer', options.pane);
        L.extend(options, this.zoomBounds);
        L.DomUtil.addClass(this._container, 'mapml-features-container');
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
            opacity = this.options.opacity,
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
            imagePath: this.options.imagePath,
            projection:map.options.projection,
            static: true,
            onEachFeature: function(properties, geometry) {
              // need to parse as HTML to preserve semantics and styles
              var c = document.createElement('div');
              c.insertAdjacentHTML('afterbegin', properties.innerHTML);
              geometry.bindPopup(c, {autoPan:false, closeButton: false});
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
                var base = (new URL(mapml.querySelector('base') ? mapml.querySelector('base').getAttribute('href') : url)).href;
                url = mapml.querySelector('link[rel=next]')? mapml.querySelector('link[rel=next]').getAttribute('href') : null;
                url =  url ? (new URL(url, base)).href: null;
                let nativeZoom = mapml.querySelector("meta[name=zoom]") && 
                  +M.metaContentToObject(mapml.querySelector("meta[name=zoom]").getAttribute("content")).value || 0;
                let nativeCS = mapml.querySelector("meta[name=cs]") && 
                        M.metaContentToObject(mapml.querySelector("meta[name=cs]").getAttribute("content")).content || "GCRS";
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

      _onMoveEnd: function() {
        let mapZoom = this._map.getZoom();
        let mapBounds = M.pixelToPCRSBounds(this._map.getPixelBounds(),mapZoom,this._map.options.projection);
        this.isVisible = mapZoom <= this.zoomBounds.maxZoom && mapZoom >= this.zoomBounds.minZoom && 
                          this.layerBounds.overlaps(mapBounds);
        
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
                var base = (new URL(mapml.querySelector('base') ? mapml.querySelector('base').getAttribute('href') : url)).href;
                url = mapml.querySelector('link[rel=next]')? mapml.querySelector('link[rel=next]').getAttribute('href') : null;
                url =  url ? (new URL(url, base)).href: null;
                // TODO if the xml parser barfed but the response is application/geo+json, use the parent addData method
              let nativeZoom = mapml.querySelector("meta[name=zoom]") && 
                                +M.metaContentToObject(mapml.querySelector("meta[name=zoom]").getAttribute("content")).value || 0;
              let nativeCS = mapml.querySelector("meta[name=cs]") && 
                                M.metaContentToObject(mapml.querySelector("meta[name=cs]").getAttribute("content")).content || "GCRS";
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
          var pxBounds = this._map.getPixelBounds(),
              topLeft = pxBounds.getTopLeft(),
              topRight = pxBounds.getTopRight(),
              bottomRight = pxBounds.getBottomRight(),
              bottomLeft = pxBounds.getBottomLeft(),
              bounds = this._map.getBounds();
              bounds.extend(this._map.unproject(bottomLeft))
                    .extend(this._map.unproject(bottomRight))
                    .extend(this._map.unproject(topLeft))
                    .extend(this._map.unproject(topRight));
          var obj = {};
          // assumes gcrs at this moment
          obj[this.options.feature.zoom.name] = this._map.getZoom();
          obj[this.options.feature.bottom.name] = bounds.getSouth();
          obj[this.options.feature.left.name] = bounds.getWest();
          obj[this.options.feature.top.name] = bounds.getNorth();
          obj[this.options.feature.right.name] = bounds.getEast();
          // hidden and other variables that may be associated
          for (var v in this.options.feature) {
              if (["width","height","left","right","top","bottom","zoom"].indexOf(v) < 0) {
                  obj[v] = this.options.feature[v];
                }
          }
          return L.Util.template(this._template.template, obj);
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
              select = (inputs[i].tagName.toLowerCase() === "select");
          if (type === "width") {
                featuresVarNames.feature.width = {name: name};
          } else if ( type === "height") {
                featuresVarNames.feature.height = {name: name};
          } else if (type === "zoom") {
                featuresVarNames.feature.zoom = {name: name};
          } else if (type === "location" && (units === "pcrs" || units ==="gcrs" || units === "tcrs")) {
            //<input name="..." units="pcrs" type="location" position="top|bottom-left|right" axis="northing|easting">
            switch (axis) {
              case ('x'):
              case ('longitude'):
              case ('easting'):
                if (position) {
                    if (position.match(/.*?-left/i)) {
                      featuresVarNames.feature.left = { name: name, axis: axis};
                    } else if (position.match(/.*?-right/i)) {
                      featuresVarNames.feature.right = { name: name, axis: axis};
                    }
                }
                break;
              case ('y'):
              case ('latitude'):
              case ('northing'):
                if (position) {
                  if (position.match(/top-.*?/i)) {
                    featuresVarNames.feature.top = { name: name, axis: axis};
                  } else if (position.match(/bottom-.*?/i)) {
                    featuresVarNames.feature.bottom = { name: name, axis: axis};
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
          } else if (type === "hidden" || type === "projection") {
              featuresVarNames.feature.hidden.push({name: name, value: value});
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
          this.layerBounds=inputData.bounds;
          this.isVisible = true;
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
                          this.layerBounds.overlaps(mapBounds);
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
              select = (inputs[i].tagName.toLowerCase() === "select");
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
            mapml = content.querySelector('image,feature,tile,extent') ? true : false;
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
          this._initCount = 0;
          this._initExtent(mapml ? content : null);
          
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
      _changeOpacity: function(e) {
        if (e && e.target && e.target.value >=0 && e.target.value <= 1.0) {
          this.changeOpacity(e.target.value);
        }
      },
      changeOpacity: function(opacity) {
          this._container.style.opacity = opacity;
      },
      onAdd: function (map) {
          if(this._extent && !this._validProjection(map)){
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
                imagePath: M.detectImagePath(this._map.getContainer()),
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
                    geometry.bindPopup(c, {autoClose: false, minWidth: 108});
                  }
                }
              });
            }
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
                    imagePath: M.detectImagePath(this._map.getContainer()),
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
                        geometry.bindPopup(c, {autoClose: false, minWidth: 108});
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
            this._mapmlTileContainer.getElementsByTagName("tiles").length > 0)
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
          }

          // if the extent has been initialized and received, update the map,
          if (this._extent) {
              if (this._templateVars) {
                this._templatedLayer = M.templatedLayer(this._templateVars, 
                { pane: this._container,
                  imagePath: M.detectImagePath(this._map.getContainer()),
                  _leafletLayer: this,
                  crs: this.crs
                }).addTo(map);
              }
          } else {
              this.once('extentload', function() {
                  if(!this._validProjection(map)){
                    this.validProjection = false;
                    return;
                  }
                  if (this._templateVars) {
                    this._templatedLayer = M.templatedLayer(this._templateVars, 
                    { pane: this._container,
                      imagePath: M.detectImagePath(this._map.getContainer()),
                      _leafletLayer: this,
                      crs: this.crs
                    }).addTo(map);
                    this._setLayerElExtent();
                  }
                }, this);
          }
          this._setLayerElExtent();
          this.setZIndex(this.options.zIndex);
          this.getPane().appendChild(this._container);
          setTimeout(() => {
            map.fire('checkdisabled');
          }, 0);
          map.on("popupopen", this._attachSkipButtons, this);
      },

      _validProjection : function(map){
        let noLayer = false;
        if(this._templateVars){
          for(let template of this._templateVars)
            if(!template.projectionMatch) noLayer = true;
        }
        if(noLayer || this.getProjection() !== map.options.projection.toUpperCase())
          return false;
        return true;
      },

      //sets the <layer-> elements .bounds property 
      _setLayerElExtent: function(){
        let localBounds, localZoomRanges;
        let layerTypes = ["_staticTileLayer","_imageLayer","_mapmlvectors","_templatedLayer"];
        layerTypes.forEach((type) =>{
          if(this[type]){
            if(type === "_templatedLayer"){
              for(let j =0;j<this[type]._templates.length;j++){
                if(this[type]._templates[j].rel === "query") continue;
                if(this[type]._templates[j].layer.layerBounds){
                  if(!localBounds){
                    localBounds = this[type]._templates[j].layer.layerBounds;
                    localZoomRanges = this[type]._templates[j].layer.zoomBounds;
                  } else {
                    localBounds.extend(this[type]._templates[j].layer.layerBounds.min);
                    localBounds.extend(this[type]._templates[j].layer.layerBounds.max);
                  }
                }
              }
            } else {
              if(this[type].layerBounds){
                if(!localBounds){
                  localBounds = this[type].layerBounds;
                  localZoomRanges = this[type].zoomBounds;
                } else {
                  localBounds.extend(this[type].layerBounds.min);
                  localBounds.extend(this[type].layerBounds.max);
                }
              } 
            }
          }
        });

        if(localBounds){
          //assigns the formatted extent object to .extent and spreads the zoom ranges to .extent also
          this._layerEl.extent = (Object.assign(
                                    M.convertAndFormatPCRS(localBounds,this._map),
                                    {zoom:localZoomRanges}));
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
          if (this._templatedLayer) {
            this._templatedLayer.redraw();
          }
      },
      _onZoomAnim: function(e) {
        var toZoom = e.zoom,
            zoom = this._extent ? this._extent.querySelector("input[type=zoom]") : null,
            min = zoom && zoom.hasAttribute("min") ? parseInt(zoom.getAttribute("min")) : this._map.getMinZoom(),
            max =  zoom && zoom.hasAttribute("max") ? parseInt(zoom.getAttribute("max")) : this._map.getMaxZoom(),
            canZoom = (toZoom < min && this._extent.zoomout) || (toZoom > max && this._extent.zoomin);
        if (!(min <= toZoom && toZoom <= max)){
          if (this._extent.zoomin && toZoom > max) {
            // this._href is the 'original' url from which this layer came
            // since we are following a zoom link we will be getting a new
            // layer almost, resetting child content as appropriate
            this._href = this._extent.zoomin;
            // this.href is the "public" property. When a dynamic layer is
            // accessed, this value changes with every new extent received
            this.href = this._extent.zoomin;
          } else if (this._extent.zoomout && toZoom < min) {
            this._href = this._extent.zoomout;
            this.href = this._extent.zoomout;
          }
        }
        if (this._templatedLayer && canZoom ) {
          // get the new extent
          this._initExtent();
        }
      },
      onRemove: function (map) {
          L.DomUtil.remove(this._container);
          if(this._staticTileLayer){
            map.removeLayer(this._staticTileLayer);
          }
          if(this._mapmlvectors){
            map.removeLayer(this._mapmlvectors);
          }
          map.removeLayer(this._imageLayer);
          if (this._templatedLayer) {
              map.removeLayer(this._templatedLayer);
          }
          map.fire("checkdisabled");
          map.off("popupopen", this._attachSkipButtons);
      },
      getZoomBounds: function () {
          var ext = this._extent;
          var zoom = ext ? ext.querySelector('[type=zoom]') : undefined,
              min = zoom && zoom.hasAttribute('min') ? zoom.getAttribute('min') : this._map.getMinZoom(),
              max = zoom && zoom.hasAttribute('max') ? zoom.getAttribute('max') : this._map.getMaxZoom();
          var bounds = {};
          bounds.min = Math.min(min,max);
          bounds.max = Math.max(min,max);
          return bounds;
      },
      _transformDeprectatedInput: function (i) {
        var type = i.getAttribute("type").toLowerCase();
        if (type === "xmin" || type === "ymin" || type === "xmax" || type === "ymax") {
          i.setAttribute("type", "location");
          i.setAttribute("units","tcrs");
          switch (type) {
            case "xmin":
              i.setAttribute("axis","x");
              i.setAttribute("position","top-left");
              break;
            case "ymin":
              i.setAttribute("axis","y");
              i.setAttribute("position","top-left");
              break;
            case "xmax":
              i.setAttribute("axis","x");
              i.setAttribute("position","bottom-right");
              break;
            case "ymax":
              i.setAttribute("axis","y");
              i.setAttribute("position","bottom-right");
              break;
          }
        } 
      },
      _setUpInputVars: function(inputs) {
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

        var extentVarNames = {extent:{}};
        extentVarNames.extent.hidden = [];
        for (var i=0;i<inputs.length;i++) {
          // this can be removed when the spec removes the deprecated inputs...
          this._transformDeprectatedInput(inputs[i]);
          var type = inputs[i].getAttribute("type"), 
              units = inputs[i].getAttribute("units"), 
              axis = inputs[i].getAttribute("axis"), 
              name = inputs[i].getAttribute("name"), 
              position = inputs[i].getAttribute("position"),
              value = inputs[i].getAttribute("value");
          if (type === "width") {
                extentVarNames.extent.width = {name: name};
          } else if ( type === "height") {
                extentVarNames.extent.height = {name: name};
          } else if (type === "zoom") {
                extentVarNames.extent.zoom = {name: name};
          } else if (type === "location" && (units === "pcrs" || units ==="gcrs" || units === "tcrs")) {
            //<input name="..." units="pcrs" type="location" position="top|bottom-left|right" axis="northing|easting">
            switch (axis) {
              case ('easting'):
                if (position) {
                    if (position.match(/.*?-left/i)) {
                      extentVarNames.extent.left = { name: name, axis: axis};
                    } else if (position.match(/.*?-right/i)) {
                      extentVarNames.extent.right = { name: name, axis: axis};
                    }
                }
                break;
              case ('northing'):
                if (position) {
                  if (position.match(/top-.*?/i)) {
                    extentVarNames.extent.top = { name: name, axis: axis};
                  } else if (position.match(/bottom-.*?/i)) {
                    extentVarNames.extent.bottom = { name: name, axis: axis};
                  }
                }
                break;
              case ('x'):
                if (position) {
                    if (position.match(/.*?-left/i)) {
                      extentVarNames.extent.left = { name: name, axis: axis};
                    } else if (position.match(/.*?-right/i)) {
                      extentVarNames.extent.right = { name: name, axis: axis};
                    }
                }
                break;
              case ('y'):
                if (position) {
                  if (position.match(/top-.*?/i)) {
                    extentVarNames.extent.top = { name: name, axis: axis};
                  } else if (position.match(/bottom-.*?/i)) {
                    extentVarNames.extent.bottom = { name: name, axis: axis};
                  }
                }
                break;
              case ('longitude'):
                if (position) {
                    if (position.match(/.*?-left/i)) {
                      extentVarNames.extent.left = { name: name, axis: axis};
                    } else if (position.match(/.*?-right/i)) {
                      extentVarNames.extent.right = { name: name, axis: axis};
                    }
                }
                break;
              case ('latitude'):
                if (position) {
                  if (position.match(/top-.*?/i)) {
                    extentVarNames.extent.top = { name: name, axis: axis};
                  } else if (position.match(/bottom-.*?/i)) {
                    extentVarNames.extent.bottom = { name: name, axis: axis};
                  }
                }
                break;
            }
            // projection is deprecated, make it hidden
          } else if (type === "hidden" || type === "projection") {
              extentVarNames.extent.hidden.push({name: name, value: value});
          }
        }
        return extentVarNames;
      },
      // retrieve the (projected, scaled) layer extent for the current map zoom level
      getLayerExtentBounds: function(map) {
          
          if (!this._extent) return;
          var zoom = map.getZoom(), projection = map.options.projection,
              ep = this._extent.getAttribute("units"),
              projecting = (projection !== ep),
              p;
          
          var xmin,ymin,xmax,ymax,v1,v2,extentZoomValue;
          
          // todo: create an array of min values, converted to tcrs units
          // take the Math.min of all of them.
          v1 = this._extent.querySelector('[type=xmin]').getAttribute('min');
          v2 = this._extent.querySelector('[type=xmax]').getAttribute('min');
          xmin = Math.min(v1,v2);
          v1 = this._extent.querySelector('[type=xmin]').getAttribute('max');
          v2 = this._extent.querySelector('[type=xmax]').getAttribute('max');
          xmax = Math.max(v1,v2);
          v1 = this._extent.querySelector('[type=ymin]').getAttribute('min');
          v2 = this._extent.querySelector('[type=ymax]').getAttribute('min');
          ymin = Math.min(v1,v2);
          v1 = this._extent.querySelector('[type=ymin]').getAttribute('max');
          v2 = this._extent.querySelector('[type=ymax]').getAttribute('max');
          ymax = Math.max(v1,v2);
          // WGS84 can be converted to Tiled CRS units
          if (projecting) {
              //project and scale to M[projection] from WGS84
              p = M[projection];
              var corners = [
                p.latLngToPoint(L.latLng([ymin,xmin]),zoom),
                p.latLngToPoint(L.latLng([ymax,xmax]),zoom), 
                p.latLngToPoint(L.latLng([ymin,xmin]),zoom), 
                p.latLngToPoint(L.latLng([ymin,xmax]),zoom)
              ];
              return L.bounds(corners);
          } else {
              // if the zoom level of the extent does not match that of the map
              extentZoomValue = parseInt(this._extent.querySelector('[type=zoom]').getAttribute('value'));
              if (extentZoomValue !== zoom) {
                  // convert the extent bounds to corresponding bounds at the current map zoom
                  p = M[projection];
                  return L.bounds(
                      p.latLngToPoint(p.pointToLatLng(L.point(xmin,ymin),extentZoomValue),zoom),
                      p.latLngToPoint(p.pointToLatLng(L.point(xmax,ymax),extentZoomValue),zoom));
              } else {
                  // the extent's zoom value === map.getZoom(), return the bounds
                  return L.bounds(L.point(xmin,ymin), L.point(xmax,ymax));
              }
          }
      },
      getAttribution: function () {
          return this.options.attribution;
      },
      getLayerUserControlsHTML: function () {
        var fieldset = document.createElement('fieldset'),
          input = document.createElement('input'),
          label = document.createElement('label'),
          name = document.createElement('span'),
          details = document.createElement('details'),
          summary = document.createElement('summary'),
          summaryContainer = document.createElement('div'),
          opacity = document.createElement('input'),
          opacityControl = document.createElement('details'),
          opacityControlSummary = document.createElement('summary'),
          opacityControlSummaryLabel = document.createElement('label'),
          mapEl = this._layerEl.parentNode;
          
          summaryContainer.classList.add('mapml-control-summary-container');
          
          let removeButton = document.createElement('a');
          removeButton.href = '#';
          removeButton.role = 'button';
          removeButton.title = 'Remove Layer';
          removeButton.innerHTML = "<span aria-hidden='true'>&#10006;</span>";
          removeButton.classList.add('mapml-layer-remove-button');
          L.DomEvent.disableClickPropagation(removeButton);
          L.DomEvent.on(removeButton, 'click', L.DomEvent.stop);
          L.DomEvent.on(removeButton, 'click', (e)=>{
            mapEl.removeChild(e.target.closest("fieldset").querySelector("span").layer._layerEl);
          }, this);

          input.defaultChecked = this._map ? true: false;
          input.type = 'checkbox';
          input.className = 'leaflet-control-layers-selector';
          name.layer = this;

          if (this._legendUrl) {
            var legendLink = document.createElement('a');
            legendLink.text = ' ' + this._title;
            legendLink.href = this._legendUrl;
            legendLink.target = '_blank';
            legendLink.draggable = false;
            name.appendChild(legendLink);
          } else {
            name.innerHTML = ' ' + this._title;
          }
          label.appendChild(input);
          label.appendChild(name);
          opacityControlSummaryLabel.innerText = 'Opacity';
          opacity.id = "o" + L.stamp(opacity);
          opacityControlSummaryLabel.setAttribute('for', opacity.id);
          opacityControlSummary.appendChild(opacityControlSummaryLabel);
          opacityControl.appendChild(opacityControlSummary);
          opacityControl.appendChild(opacity);
          L.DomUtil.addClass(details, 'mapml-control-layers');
          L.DomUtil.addClass(opacityControl,'mapml-control-layers');
          opacity.setAttribute('type','range');
          opacity.setAttribute('min', '0');
          opacity.setAttribute('max','1.0');
          opacity.setAttribute('value', this._container.style.opacity || '1.0');
          opacity.setAttribute('step','0.1');
          opacity.value = this._container.style.opacity || '1.0';

          fieldset.setAttribute("aria-grabbed", "false");

          fieldset.onmousedown = (downEvent) => {
            if(downEvent.target.tagName.toLowerCase() === "input") return;
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
                  root = mapEl.tagName === "MAPML-VIEWER" ? mapEl.shadowRoot : mapEl.querySelector(".web-map").shadowRoot,
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

          fieldset.appendChild(details);
          details.appendChild(summary);
          summaryContainer.appendChild(label);
          summaryContainer.appendChild(removeButton);
          summary.appendChild(summaryContainer);
          details.appendChild(opacityControl);

          if (this._styles) {
            details.appendChild(this._styles);
          }
          if (this._userInputs) {
            var frag = document.createDocumentFragment();
            var templates = this._templateVars;
            if (templates) {
              for (var i=0;i<templates.length;i++) {
                var template = templates[i];
                for (var j=0;j<template.values.length;j++) {
                  var mapmlInput = template.values[j],
                      id = '#'+mapmlInput.getAttribute('id');
                  // don't add it again if it is referenced > once
                  if (mapmlInput.tagName.toLowerCase() === 'select' && !frag.querySelector(id)) {
                    // generate a <details><summary></summary><input...></details>
                    var userfieldset = document.createElement('fieldset'),
                        selectdetails = document.createElement('details'),
                        selectsummary = document.createElement('summary'),
                        selectSummaryLabel = document.createElement('label');
                        selectSummaryLabel.innerText = mapmlInput.getAttribute('name');
                        selectSummaryLabel.setAttribute('for', mapmlInput.getAttribute('id'));
                        L.DomUtil.addClass(selectdetails, 'mapml-control-layers');
                        selectsummary.appendChild(selectSummaryLabel);
                        selectdetails.appendChild(selectsummary);
                        selectdetails.appendChild(mapmlInput.htmlselect);
                        userfieldset.appendChild(selectdetails);
                    frag.appendChild(userfieldset);
                  }
                }
              }
            }
            details.appendChild(frag);
          }
          return fieldset;
      },
      _initExtent: function(content) {
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
          function _processInitialExtent(content) {
              var mapml = this.responseXML || content;
              if(mapml.querySelector('feature'))layer._content = mapml;
              if(!this.responseXML && this.responseText) mapml = new DOMParser().parseFromString(this.responseText,'text/xml');
              if (this.readyState === this.DONE && mapml.querySelector) {
                  var serverExtent = mapml.querySelector('extent') || mapml.querySelector('meta[name=projection]'), projection;

                  if (serverExtent.tagName.toLowerCase() === "extent" && serverExtent.hasAttribute('units')){
                    projection = serverExtent.getAttribute("units");
                  } else if (serverExtent.tagName.toLowerCase() === "meta" && serverExtent.hasAttribute('content')) {
                    projection = M.metaContentToObject(serverExtent.getAttribute('content')).content;
                  }
                      
                  var projectionMatch = projection && projection === layer.options.mapprojection,
                      metaExtent = mapml.querySelector('meta[name=extent]'),
                      selectedAlternate = !projectionMatch && mapml.querySelector('head link[rel=alternate][projection='+layer.options.mapprojection+']'),
                      
                      base = 
        (new URL(mapml.querySelector('base') ? mapml.querySelector('base').getAttribute('href') : mapml.baseURI || this.responseURL, this.responseURL)).href;
                  
                  if (!serverExtent) {
                      serverExtent = layer._synthesizeExtent(mapml);
                      // the mapml resource does not have a (complete) extent form, save
                      // its content if any so we don't have to revisit the server, ever.
                      if (mapml.querySelector('feature,image,tile')) {
                          layer._content = mapml;
                      }
                  } else if (!projectionMatch && selectedAlternate && selectedAlternate.hasAttribute('href')) {
                       
                      layer.fire('changeprojection', {href:  (new URL(selectedAlternate.getAttribute('href'), base)).href}, false);
                      return;
                  } else if (serverExtent.querySelector('link[rel=tile],link[rel=image],link[rel=features],link[rel=query]') &&
                          serverExtent.hasAttribute("units")) {
                    layer._templateVars = [];
                    // set up the URL template and associated inputs (which yield variable values when processed)
                    var tlist = serverExtent.querySelectorAll('link[rel=tile],link[rel=image],link[rel=features],link[rel=query]'),
                        varNamesRe = (new RegExp('(?:\{)(.*?)(?:\})','g')),
                        zoomInput = serverExtent.querySelector('input[type="zoom" i]'),
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
                        let blankInputs = mapml.querySelectorAll('input');
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
                          var zoomBounds = mapml.querySelector('meta[name=zoom]')?
                                            M.metaContentToObject(mapml.querySelector('meta[name=zoom]').getAttribute('content')):
                                            undefined;
                      while ((v = varNamesRe.exec(template)) !== null) {
                        var varName = v[1],
                            inp = serverExtent.querySelector('input[name='+varName+'],select[name='+varName+']');
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
                            var servers = serverExtent.querySelectorAll('datalist#'+id + ' > option');
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
                          } else if (inp.tagName.toLowerCase() === 'select') {
                            // use a throwaway div to parse the input from MapML into HTML
                            var div =document.createElement("div");
                            div.insertAdjacentHTML("afterbegin",inp.outerHTML);
                            // parse
                            inp.htmlselect = div.querySelector("select");
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
                        layer._templateVars.push({
                          template:decodeURI(new URL(template, base)), 
                          linkEl: t,
                          title:title, 
                          rel: trel, 
                          type: ttype, 
                          values: inputs, 
                          zoomBounds:zoomBounds, 
                          projectionMatch: projectionMatch || selectedAlternate,
                          projection:serverExtent.getAttribute("units") || FALLBACK_PROJECTION,
                          tms:tms,
                        });
                      }
                    }
                  }
                  layer._parseLicenseAndLegend(mapml, layer);
                  layer._extent = serverExtent;
                  
                  
                  var zoomin = mapml.querySelector('link[rel=zoomin]'),
                      zoomout = mapml.querySelector('link[rel=zoomout]');
                  delete layer._extent.zoomin;
                  delete layer._extent.zoomout;
                  if (zoomin) {
                      layer._extent.zoomin = (new URL(zoomin.getAttribute('href'), base)).href;
                  }
                  if (zoomout) {
                      layer._extent.zoomout = (new URL(zoomout.getAttribute('href'), base)).href;
                  }
                  if (layer._templatedLayer) {
                    layer._templatedLayer.reset(layer._templateVars);
                  }
                  if (mapml.querySelector('tile')) {
                    var tiles = document.createElement("tiles"),
                      zoom = mapml.querySelector('meta[name=zoom][content]') || mapml.querySelector('input[type=zoom][value]');
                    tiles.setAttribute("zoom", zoom && zoom.getAttribute('content') || zoom && zoom.getAttribute('value') || "0");
                    var newTiles = mapml.getElementsByTagName('tile');
                    for (var nt=0;nt<newTiles.length;nt++) {
                        tiles.appendChild(document.importNode(newTiles[nt], true));
                    }
                    layer._mapmlTileContainer.appendChild(tiles);
                  }
                  M.parseStylesheetAsHTML(mapml, base, layer._container);
                  var styleLinks = mapml.querySelectorAll('link[rel=style],link[rel="self style"],link[rel="style self"]');
                  if (styleLinks.length > 1) {
                    var stylesControl = document.createElement('details'),
                    stylesControlSummary = document.createElement('summary');
                    stylesControlSummary.innerText = 'Style';
                    stylesControl.appendChild(stylesControlSummary);
                    var changeStyle = function (e) {
                        layer.fire('changestyle', {src: e.target.getAttribute("data-href")}, false);
                    };

                    for (var j=0;j<styleLinks.length;j++) {
                      var styleOption = document.createElement('span'),
                      styleOptionInput = styleOption.appendChild(document.createElement('input'));
                      styleOptionInput.setAttribute("type", "radio");
                      styleOptionInput.setAttribute("id", "rad"+j);
                      styleOptionInput.setAttribute("name", "styles");
                      styleOptionInput.setAttribute("value", styleLinks[j].getAttribute('title'));
                      styleOptionInput.setAttribute("data-href", new URL(styleLinks[j].getAttribute('href'),base).href);
                      var styleOptionLabel = styleOption.appendChild(document.createElement('label'));
                      styleOptionLabel.setAttribute("for", "rad"+j);
                      styleOptionLabel.innerText = styleLinks[j].getAttribute('title');
                      if (styleLinks[j].getAttribute("rel") === "style self" || styleLinks[j].getAttribute("rel") === "self style") {
                        styleOptionInput.checked = true;
                      }
                      stylesControl.appendChild(styleOption);
                      L.DomUtil.addClass(stylesControl,'mapml-control-layers');
                      L.DomEvent.on(styleOptionInput,'click', changeStyle, layer);
                    }
                    layer._styles = stylesControl;
                  }
                  
                  if (mapml.querySelector('title')) {
                    layer._title = mapml.querySelector('title').textContent.trim();
                  } else if (mapml.hasAttribute('label')) {
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
          }
      },
      _createExtent: function () {
      
          var extent = document.createElement('extent'),
              xminInput = document.createElement('input'),
              yminInput = document.createElement('input'),
              xmaxInput = document.createElement('input'),
              ymaxInput = document.createElement('input'),
              zoom = document.createElement('input'),
              projection = document.createElement('input');
      
          zoom.setAttribute('type','zoom');
          zoom.setAttribute('min','0');
          zoom.setAttribute('max','0');
          
          xminInput.setAttribute('type','xmin');
          xminInput.setAttribute('min','');
          xminInput.setAttribute('max','');
          
          yminInput.setAttribute('type','ymin');
          yminInput.setAttribute('min','');
          yminInput.setAttribute('max','');
          
          xmaxInput.setAttribute('type','xmax');
          xmaxInput.setAttribute('min','');
          xmaxInput.setAttribute('max','');

          ymaxInput.setAttribute('type','ymax');
          ymaxInput.setAttribute('min','');
          ymaxInput.setAttribute('max','');
          
          projection.setAttribute('type','projection');
          projection.setAttribute('value','WGS84');
          
          extent.appendChild(xminInput);
          extent.appendChild(yminInput);
          extent.appendChild(xmaxInput);
          extent.appendChild(ymaxInput);
          extent.appendChild(zoom);
          extent.appendChild(projection);

          return extent;
      },
      _validateExtent: function () {
        // TODO: change so that the _extent bounds are set based on inputs
          var serverExtent = this._extent;
          if (!serverExtent || !serverExtent.querySelector || !this._map) {
              return;
          }
          if (serverExtent.querySelector('[type=xmin][min=""], [type=xmin][max=""], [type=xmax][min=""], [type=xmax][max=""], [type=ymin][min=""], [type=ymin][max=""]')) {
              var xmin = serverExtent.querySelector('[type=xmin]'),
                  ymin = serverExtent.querySelector('[type=ymin]'),
                  xmax = serverExtent.querySelector('[type=xmax]'),
                  ymax = serverExtent.querySelector('[type=ymax]'),
                  proj = serverExtent.querySelector('[type=projection][value]'),
                  bounds, projection;
              if (proj) {
                  projection = proj.getAttribute('value');
                  if (projection && projection === 'WGS84') {
                      bounds = this._map.getBounds();
                      xmin.setAttribute('min',bounds.getWest());
                      xmin.setAttribute('max',bounds.getEast());
                      ymin.setAttribute('min',bounds.getSouth());
                      ymin.setAttribute('max',bounds.getNorth());
                      xmax.setAttribute('min',bounds.getWest());
                      xmax.setAttribute('max',bounds.getEast());
                      ymax.setAttribute('min',bounds.getSouth());
                      ymax.setAttribute('max',bounds.getNorth());
                  } else if (projection) {
                      // needs testing.  Also, this will likely be
                      // messing with a server-generated extent.
                      bounds = this._map.getPixelBounds();
                      xmin.setAttribute('min',bounds.getBottomLeft().x);
                      xmin.setAttribute('max',bounds.getTopRight().x);
                      ymin.setAttribute('min',bounds.getTopRight().y);
                      ymin.setAttribute('max',bounds.getBottomLeft().y);
                      xmax.setAttribute('min',bounds.getBottomLeft().x);
                      xmax.setAttribute('max',bounds.getTopRight().x);
                      ymax.setAttribute('min',bounds.getTopRight().y);
                      ymax.setAttribute('max',bounds.getBottomLeft().y);
                  }
              } else {
                  this.error = true;
              }

          }
          if (serverExtent.querySelector('[type=zoom][min=""], [type=zoom][max=""]')) {
              var zoom = serverExtent.querySelector('[type=zoom]');
              zoom.setAttribute('min',this._map.getMinZoom());
              zoom.setAttribute('max',this._map.getMaxZoom());
          }
          var lp = serverExtent.hasAttribute("units") ? serverExtent.getAttribute("units") : null;
          if (lp && M[lp]) {
            this.crs = M[lp];
          } else {
            this.crs = M.OSMTILE;
          }
      },
      _getMapMLExtent: function (bounds, zooms, proj) {
          
          var extent = this._createExtent(),
              zoom = extent.querySelector('input[type=zoom]'),
              xminInput = extent.querySelector('input[type=xmin]'),
              yminInput = extent.querySelector('input[type=ymin]'),
              xmaxInput = extent.querySelector('input[type=xmax]'),
              ymaxInput = extent.querySelector('input[type=ymax]'),
              projection = extent.querySelector('input[type=projection]'),
              zmin = zooms[0] !== undefined && zooms[1] !== undefined ? Math.min(zooms[0],zooms[1]) : '',
              zmax = zooms[0] !== undefined && zooms[1] !== undefined ? Math.max(zooms[0],zooms[1]) : '',
              xmin = bounds ? bounds._southWest ? bounds.getWest() : bounds.getBottomLeft().x : '',
              ymin = bounds ? bounds._southWest ? bounds.getSouth() : bounds.getTopRight().y : '',
              xmax = bounds ? bounds._southWest ? bounds.getEast() : bounds.getTopRight().x : '',
              ymax = bounds ? bounds._southWest ? bounds.getNorth() : bounds.getBottomLeft().y : '';
      
          zoom.setAttribute('min', typeof(zmin) === 'number' && isNaN(zmin)? '' : zmin);
          zoom.setAttribute('max', typeof(zmax) === 'number' && isNaN(zmax)? '' : zmax);
          
          xminInput.setAttribute('min',xmin);
          xminInput.setAttribute('max',xmax);
          
          yminInput.setAttribute('min',ymin);
          yminInput.setAttribute('max',ymax);
          
          xmaxInput.setAttribute('min',xmin);
          xmaxInput.setAttribute('max',xmax);

          ymaxInput.setAttribute('min',ymin);
          ymaxInput.setAttribute('max',ymax);
          
          projection.setAttribute('value',bounds && bounds._southWest && !proj ? 'WGS84' : proj);

          return extent;
      },
      _synthesizeExtent: function (mapml) {
          var metaZoom = mapml.querySelectorAll('meta[name=zoom]')[0],
              metaExtent = mapml.querySelector('meta[name=extent]'),
              metaProjection = mapml.querySelector('meta[name=projection]'),
              proj = metaProjection ? metaProjection.getAttribute('content'): FALLBACK_PROJECTION,
              i,expressions,bounds,zmin,zmax,xmin,ymin,xmax,ymax,expr,lhs,rhs;
          if (metaZoom) {
              expressions = metaZoom.getAttribute('content').split(',');
              for (i=0;i<expressions.length;i++) {
                expr = expressions[i].split('=');
                lhs = expr[0];
                rhs=expr[1];
                if (lhs === 'min') {
                  zmin = parseInt(rhs);
                }
                if (lhs === 'max') {
                  zmax = parseInt(rhs);
                }
              }
          }  
          if (metaExtent) {
              expressions = metaExtent.getAttribute('content').split(',');
              for (i=0;i<expressions.length;i++) {
                expr = expressions[i].split('=');
                lhs = expr[0];
                rhs=expr[1];
                if (lhs === 'xmin') {
                  xmin = parseFloat(rhs);
                }
                if (lhs === 'xmax') {
                  xmax = parseFloat(rhs);
                }
                if (lhs === 'ymin') {
                  ymin = parseFloat(rhs);
                }
                if (lhs === 'ymax') {
                  ymax = parseFloat(rhs);
                }
              }
          }
          if (xmin && ymin && xmax && ymax && proj === 'WGS84') {
              var sw = L.latLng(ymin,xmin), ne = L.latLng(ymax,xmax);
              bounds = L.latLngBounds(sw,ne);
          } else if (xmin && ymin && xmax && ymax) {
              // needs testing
              bounds = L.bounds([[xmin,ymin],[xmax,ymax]]);
          }
          return this._getMapMLExtent(bounds, [zmin,zmax], proj);
      },
      // a layer must share a projection with the map so that all the layers can
      // be overlayed in one coordinate space.  WGS84 is a 'wildcard', sort of.
      getProjection: function () {
        let extent = this._extent;
        if(!extent) return FALLBACK_PROJECTION;
        switch (extent.tagName.toUpperCase()) {
          case "EXTENT":
            if(extent.hasAttribute('units'))
              return extent.getAttribute('units').toUpperCase();
            break;
          case "INPUT":
            if(extent.hasAttribute('value'))
              return extent.getAttribute('value').toUpperCase();
            break;
          case "META":
            if(extent.hasAttribute('content'))
              return M.metaContentToObject(extent.getAttribute('content')).content.toUpperCase(); 
            break;
          default:
            return FALLBACK_PROJECTION; 
        }
        return FALLBACK_PROJECTION;
      },
      _parseLicenseAndLegend: function (xml, layer) {
          var licenseLink =  xml.querySelector('link[rel=license]'), licenseTitle, licenseUrl, attText;
          if (licenseLink) {
              licenseTitle = licenseLink.getAttribute('title');
              licenseUrl = licenseLink.getAttribute('href');
              attText = '<a href="' + licenseUrl + '" title="'+licenseTitle+'">'+licenseTitle+'</a>';
          }
          L.setOptions(layer,{attribution:attText});
          var legendLink = xml.querySelector('link[rel=legend]');
          if (legendLink) {
            layer._legendUrl = legendLink.getAttribute('href');
          }
      },
      // return the LatLngBounds of the map unprojected such that the whole
      // map is covered, not just a band defined by the projected map bounds.
      _getUnprojectedMapLatLngBounds: function(map) {
        
          map = map||this._map; 
          var origin = map.getPixelOrigin(),
            bounds = map.getPixelBounds(),
            nw = map.unproject(origin),
            sw = map.unproject(bounds.getBottomLeft()),
            ne = map.unproject(bounds.getTopRight()),
            se = map.unproject(origin.add(map.getSize()));
          return L.latLngBounds(sw,ne).extend(se).extend(nw);
      },
      // this takes into account that WGS84 is considered a wildcard match.
      _projectionMatches: function(map) {
          map = map||this._map;
          var projection = this.getProjection();
          if (!map.options.projection || projection !== 'WGS84' && map.options.projection !== projection) return false;
          return true;
      },
      getQueryTemplates: function() {
          if (this._templatedLayer && this._templatedLayer._queries) {
            return this._templatedLayer._queries;
          }
      },
      _attachSkipButtons: function(e){
        let popup = e.popup, map = e.target, layer, group,
            content = popup._container.getElementsByClassName("mapml-popup-content")[0];

        content.setAttribute("tabindex", "-1");
        popup._count = 0; // used for feature pagination

        if(popup._source._eventParents){ // check if the popup is for a feature or query
          layer = popup._source._eventParents[Object.keys(popup._source._eventParents)[0]]; // get first parent of feature, there should only be one
          group = popup._source.group;
        } else {
          layer = popup._source._templatedLayer;
        }

        if(popup._container.querySelector('div[class="mapml-focus-buttons"]')){
          L.DomUtil.remove(popup._container.querySelector('div[class="mapml-focus-buttons"]'));
          L.DomUtil.remove(popup._container.querySelector('hr'));
        }
        //add when popopen event happens instead
        let div = L.DomUtil.create("div", "mapml-focus-buttons");

        // creates |< button, focuses map
        let mapFocusButton = L.DomUtil.create('a',"mapml-popup-button", div);
        mapFocusButton.href = '#';
        mapFocusButton.role = "button";
        mapFocusButton.title = "Focus Map";
        mapFocusButton.innerHTML = "<span aria-hidden='true'>|&#10094;</span>";
        L.DomEvent.disableClickPropagation(mapFocusButton);
        L.DomEvent.on(mapFocusButton, 'click', L.DomEvent.stop);
        L.DomEvent.on(mapFocusButton, 'click', (e)=>{
          map.closePopup();
          map._container.focus();
        }, popup);

        // creates < button, focuses previous feature, if none exists focuses the current feature
        let previousButton = L.DomUtil.create('a', "mapml-popup-button", div);
        previousButton.href = '#';
        previousButton.role = "button";
        previousButton.title = "Previous Feature";
        previousButton.innerHTML = "<span aria-hidden='true'>&#10094;</span>";
        L.DomEvent.disableClickPropagation(previousButton);
        L.DomEvent.on(previousButton, 'click', L.DomEvent.stop);
        L.DomEvent.on(previousButton, 'click', layer._previousFeature, popup);

        // static feature counter that 1/1
        let featureCount = L.DomUtil.create("p", "mapml-feature-count", div),
            totalFeatures = this._totalFeatureCount ? this._totalFeatureCount : 1;
        featureCount.innerText = (popup._count + 1)+"/"+totalFeatures;

        // creates > button, focuses next feature, if none exists focuses the current feature
        let nextButton = L.DomUtil.create('a', "mapml-popup-button", div);
        nextButton.href = '#';
        nextButton.role = "button";
        nextButton.title = "Next Feature";
        nextButton.innerHTML = "<span aria-hidden='true'>&#10095;</span>";
        L.DomEvent.disableClickPropagation(nextButton);
        L.DomEvent.on(nextButton, 'click', L.DomEvent.stop);
        L.DomEvent.on(nextButton, 'click', layer._nextFeature, popup);
        
        // creates >| button, focuses map controls
        let controlFocusButton = L.DomUtil.create('a',"mapml-popup-button", div);
        controlFocusButton.href = '#';
        controlFocusButton.role = "button";
        controlFocusButton.title = "Focus Controls";
        controlFocusButton.innerHTML = "<span aria-hidden='true'>&#10095;|</span>";
        L.DomEvent.disableClickPropagation(controlFocusButton);
        L.DomEvent.on(controlFocusButton, 'click', L.DomEvent.stop);
        L.DomEvent.on(controlFocusButton, 'click', (e) => {
          map.closePopup();
          map._controlContainer.querySelector("A").focus();
        }, popup);
    
        let divider = L.DomUtil.create("hr");
        divider.style.borderTop = "1px solid #bbb";

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
          let isTab = focusEvent.originalEvent.keyCode === 9,
              shiftPressed = focusEvent.originalEvent.shiftKey;
          if((focusEvent.originalEvent.path[0].classList.contains("leaflet-popup-close-button") && isTab && !shiftPressed) || focusEvent.originalEvent.keyCode === 27){
            L.DomEvent.stop(focusEvent);
            map.closePopup(popup);
            group.focus();
          } else if ((focusEvent.originalEvent.path[0].title==="Focus Map" || focusEvent.originalEvent.path[0].classList.contains("mapml-popup-content")) && isTab && shiftPressed){
            setTimeout(() => { //timeout needed so focus of the feature is done even after the keypressup event occurs
              L.DomEvent.stop(focusEvent);
              map.closePopup(popup);
              group.focus();
            }, 0);
          }
        }

        function focusMap(focusEvent){
          let isTab = focusEvent.originalEvent.keyCode === 9,
          shiftPressed = focusEvent.originalEvent.shiftKey;

          if((focusEvent.originalEvent.keyCode === 13 && focusEvent.originalEvent.path[0].classList.contains("leaflet-popup-close-button")) || focusEvent.originalEvent.keyCode === 27 ){
            L.DomEvent.stopPropagation(focusEvent);
            map._container.focus();
            map.closePopup(popup);
            if(focusEvent.originalEvent.keyCode !== 27)map._popupClosed = true;
          } else if (isTab && focusEvent.originalEvent.path[0].classList.contains("leaflet-popup-close-button")){
            map.closePopup(popup);
          } else if ((focusEvent.originalEvent.path[0].title==="Focus Map" || focusEvent.originalEvent.path[0].classList.contains("mapml-popup-content")) && isTab && shiftPressed){
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
        this._container = L.DomUtil.create("div", "mapml-debug", map._container);
        this._container.style.width = 150;
        this._container.style.zIndex = 10000;
        this._container.style.position = "absolute";
        this._container.style.top = "auto";
        this._container.style.bottom = "5px";
        this._container.style.left = "5px";
        this._container.style.right = "auto";

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

      this._title = L.DomUtil.create("div", "mapml-debug-banner", this.options.pane);
      this._title.innerHTML = "Debug mode";

      map.debug = {};
      map.debug._infoContainer = this._debugContainer = L.DomUtil.create("div", "mapml-debug-panel", this.options.pane);

      let infoContainer = map.debug._infoContainer;

      map.debug._tileCoord = L.DomUtil.create("div", "mapml-debug-coordinates", infoContainer);
      map.debug._tileMatrixCoord = L.DomUtil.create("div", "mapml-debug-coordinates", infoContainer);
      map.debug._mapCoord = L.DomUtil.create("div", "mapml-debug-coordinates", infoContainer);
      map.debug._tcrsCoord = L.DomUtil.create("div", "mapml-debug-coordinates", infoContainer);
      map.debug._pcrsCoord = L.DomUtil.create("div", "mapml-debug-coordinates", infoContainer);
      map.debug._gcrsCoord = L.DomUtil.create("div", "mapml-debug-coordinates", infoContainer);

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

      this.debug._tileCoord.innerHTML = `tile: i: ${Math.trunc(pointI)}, j: ${Math.trunc(pointJ)}`;
      this.debug._mapCoord.innerHTML = `map: i: ${Math.trunc(e.containerPoint.x)}, j: ${Math.trunc(e.containerPoint.y)}`;
      this.debug._gcrsCoord.innerHTML = `gcrs: lon: ${e.latlng.lng.toFixed(6)}, lat: ${e.latlng.lat.toFixed(6)}`;
      this.debug._tcrsCoord.innerHTML = `tcrs: x:${Math.trunc(point.x)}, y:${Math.trunc(point.y)}`;
      this.debug._tileMatrixCoord.innerHTML = `tilematrix: column:${Math.trunc(point.x / tileSize)}, row:${Math.trunc(point.y / tileSize)}`;
      this.debug._pcrsCoord.innerHTML = `pcrs: easting:${pcrs.x.toFixed(2)}, northing:${pcrs.y.toFixed(2)}`;
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
        if (layers[i].layerBounds) {
          let boundsArray = [
            layers[i].layerBounds.min,
            L.point(layers[i].layerBounds.max.x, layers[i].layerBounds.min.y),
            layers[i].layerBounds.max,
            L.point(layers[i].layerBounds.min.x, layers[i].layerBounds.max.y)
          ];
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
              this._query(event, layer);
          }
      },
      _query(e, layer) {
        var obj = {},
            template = layer.getQueryTemplates()[0],
            zoom = e.target.getZoom(),
            map = this._map,
            crs = layer.crs,
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

        let point = this._map.project(e.latlng),
            scale = this._map.options.crs.scale(this._map.getZoom()),
            pcrsClick = this._map.options.crs.transformation.untransform(point,scale),
            contenttype;

        if(template.layerBounds.contains(pcrsClick)){
          fetch(L.Util.template(template.template, obj), { redirect: 'follow' }).then((response) => {
            contenttype = response.headers.get("Content-Type");
            if (response.status >= 200 && response.status < 300) {
              return response.text();
            } else {
              throw new Error(response.status);
            }
          }).then((mapml) => {
            if (contenttype.startsWith("text/mapml")) {
              return handleMapMLResponse(mapml, e.latlng);
            } else {
              return handleOtherResponse(mapml, layer, e.latlng);
            }
          }).catch((err) => {
            console.log('Looks like there was a problem. Status: ' + err.message);
          });
        }
        function handleMapMLResponse(mapml, loc) {
          let parser = new DOMParser(),
            mapmldoc = parser.parseFromString(mapml, "application/xml");

          for(let feature of mapmldoc.querySelectorAll('feature')){
            if(!feature.querySelector('geometry')){
              let geo = document.createElement('geometry'), point = document.createElement('point'),
                coords = document.createElement('coordinates');
              coords.innerHTML = `${loc.lng} ${loc.lat}`;
              point.appendChild(coords);
              geo.appendChild(point);
              feature.appendChild(geo);
            }
          }

          let f = M.mapMlFeatures(mapmldoc, {
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
              imagePath: M.detectImagePath(map.getContainer()),
              query: true,
              static:true,
          });
          f.addTo(map);

          let div = L.DomUtil.create("div", "mapml-popup-content"),
              c = L.DomUtil.create("iframe");
          c.style = "border: none";
          c.srcdoc = `<meta http-equiv="content-security-policy" content="script-src 'none';">` + mapmldoc.querySelector('feature properties').innerHTML;
          div.appendChild(c);
          // passing a latlng to the popup is necessary for when there is no
          // geometry / null geometry
          layer._totalFeatureCount = mapmldoc.querySelectorAll("feature").length;
          layer.bindPopup(div, popupOptions).openPopup(loc);
          layer.on('popupclose', function() {
              map.removeLayer(f);
          });
          f.showPaginationFeature({i: 0, popup: layer._popup});

        }
        function handleOtherResponse(text, layer, loc) {
          let div = L.DomUtil.create("div", "mapml-popup-content"),
              c = L.DomUtil.create("iframe");
          c.style = "border: none";
          c.srcdoc = `<meta http-equiv="content-security-policy" content="script-src 'none';">` + text;
          div.appendChild(c);
          layer.bindPopup(div, popupOptions).openPopup(loc);
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
          text:"Back (<kbd>B</kbd>)",
          callback:this._goBack,
        },
        {
          text:"Forward (<kbd>F</kbd>)",
          callback:this._goForward,
        },
        {
          text:"Reload (<kbd>R</kbd>)",
          callback:this._reload,
        },
        {
          spacer:"-",
        },
        {
          text:"Toggle Controls (<kbd>T</kbd>)",
          callback:this._toggleControls,
        },
        {
          text:"Copy Coordinates (<kbd>C</kbd>) <span aria-hidden='true'>></span>", 
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
              text:"All",
              callback:this._copyAllCoords,
            }
          ]
        },
        {
          text:"Toggle Debug Mode (<kbd>D</kbd>)",
          callback:this._toggleDebug,
        },
        {
          text:"View Map Source (<kbd>V</kbd>)",
          callback:this._viewSource,
        },
      ];

      this._layerItems = [
        {
          text:"Zoom To Layer (<kbd>Z</kbd>)",
          callback:this._zoomToLayer
        },
        {
          text:"Copy Extent (<kbd>C</kbd>)",
          callback:this._copyLayerExtent
        },
      ];
      this._mapMenuVisible = false;
      this._keyboardEvent = false;

      this._container = L.DomUtil.create("div", "mapml-contextmenu", map._container);
      this._container.style.zIndex = 10001;
      this._container.style.position = "absolute";

      this._container.style.width = "150px";
      
      for (let i = 0; i < 6; i++) {
        this._items[i].el = this._createItem(this._container, this._items[i]);
      }

      this._coordMenu = L.DomUtil.create("div", "mapml-contextmenu mapml-submenu", this._container);
      this._coordMenu.style.zIndex = 10001;
      this._coordMenu.style.position = "absolute";

      this._coordMenu.style.width = "80px";
      this._coordMenu.id = "mapml-copy-submenu";

      this._clickEvent = null;

      for(let i =0;i<this._items[5].submenu.length;i++){
        this._createItem(this._coordMenu,this._items[5].submenu[i],i);
      }

      this._items[6].el = this._createItem(this._container, this._items[6]);
      this._items[7].el = this._createItem(this._container, this._items[7]);

      this._layerMenu = L.DomUtil.create("div", "mapml-contextmenu mapml-layer-menu", map._container);
      this._layerMenu.style.zIndex = 10001;
      this._layerMenu.style.position = "absolute";
      this._layerMenu.style.width = "150px";
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

      let data = `top-left-easting,${tL.horizontal}\ntop-left-northing,${tL.vertical}\n`;
      data += `bottom-right-easting,${bR.horizontal}\nbottom-right-northing,${bR.vertical}`;

      context._copyData(data);
    },

    _zoomToLayer: function (e) {
      let map = e instanceof KeyboardEvent ? this._map : this,
          layerElem = map.contextMenu._layerClicked.layer._layerEl,
          tL = layerElem.extent.topLeft.pcrs,
          bR = layerElem.extent.bottomRight.pcrs,
          layerBounds = L.bounds(L.point(tL.horizontal, tL.vertical), L.point(bR.horizontal, bR.vertical)),
          center = map.options.crs.unproject(layerBounds.getCenter(true)),
          currentZoom = map.getZoom();

      map.setView(center, currentZoom, {animate:false});
      let mapBounds = M.pixelToPCRSBounds(
        map.getPixelBounds(),
        map.getZoom(),
        map.options.projection);
      
      //fits the bounds to the map view
      if(mapBounds.contains(layerBounds)){
        while(mapBounds.contains(layerBounds) && (currentZoom + 1) <= layerElem.extent.zoom.maxZoom){
          currentZoom++;
          map.setView(center, currentZoom, {animate:false});
          mapBounds = M.pixelToPCRSBounds(
            map.getPixelBounds(),
            map.getZoom(),
            map.options.projection);
        }
        if(currentZoom - 1 >= 0) map.flyTo(center, (currentZoom - 1));
      } else {
        while(!(mapBounds.contains(layerBounds)) && (currentZoom - 1) >= layerElem.extent.zoom.minZoom){
          currentZoom--;
          map.setView(center, currentZoom, {animate:false});
          mapBounds = M.pixelToPCRSBounds(
            map.getPixelBounds(),
            map.getZoom(),
            map.options.projection);
        }
      }
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
          el = this._insertElementAt('a', itemCls, container, index),
          callback = this._createEventHandler(el, options.callback, options.context, options.hideOnSelect),
          html = '';

      el.innerHTML = html + options.text;
      el.href = "#";
      el.setAttribute("role","button");
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
      let elem = e.originalEvent.srcElement;
      if(elem.closest("fieldset")){
        elem = elem.closest("fieldset").querySelector("span");
        if(!elem.layer.validProjection) return;
        this._layerClicked = elem;
        this._showAtPoint(e.containerPoint, e, this._layerMenu);
      } else if(elem.classList.contains("leaflet-container")) {
        this._layerClicked = undefined;
        this._showAtPoint(e.containerPoint, e, this._container);
      }
      if(e.originalEvent.button === 0){
        this._keyboardEvent = true;
        this._container.firstChild.focus();
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
              container.style.display = 'block';
                this._mapMenuVisible = true;
            }

            this._map.fire('contextmenu.show', event);
        }
    },

    _hide: function () {
        if (this._mapMenuVisible) {
            this._mapMenuVisible = false;
            this._container.style.display = 'none';
            this._coordMenu.style.display = 'none';
            this._layerMenu.style.display = 'none';
            this._map.fire('contextmenu.hide', {contextmenu: this});
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
        let size = this._size,
            initialDisplay = el.style.display;

        if (!size || this._sizeChanged) {
            size = {};

            el.style.left = '-999999px';
            el.style.right = 'auto';
            el.style.display = 'block';

            size.x = el.offsetWidth;
            size.y = el.offsetHeight;

            el.style.left = 'auto';
            el.style.display = initialDisplay;

            this._sizeChanged = false;
        }

        return size;
    },

     _debounceKeyDown: function(func, wait, immediate) {
      let timeout;
      let context = this, args = arguments;
      clearTimeout(timeout);
      timeout = setTimeout(function() {
        timeout = null;
        if (!immediate) func.apply(context, args);
      }, wait);
      if (immediate && !timeout) func.apply(context, args);
    },

    _onKeyDown: function (e) {
      if(!this._mapMenuVisible) return;
      this._debounceKeyDown(function(){
        let key = e.keyCode;
        if(key !== 16 && key!== 9 && !(!this._layerClicked && key === 67) && e.path[0].innerText !== "Copy Coordinates (C) >")
          this._hide();
        switch(key){
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
          case 68:
            this._toggleDebug(e);
            break;
          case 70:
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
          case 27: //H KEY
            this._hide();
            break;
          case 90: //Z KEY
            if(this._layerClicked)
              this._zoomToLayer(e);
            break;
        }
      },250);
    },

    _showCoordMenu: function(e){
      let mapSize = this._map.getSize(),
          click = this._clickEvent,
          menu = this._coordMenu,
          copyEl = this._items[5].el.el;

      copyEl.setAttribute("aria-expanded","true");
      menu.style.display = "block";

      if (click.containerPoint.x + 150 + 80 > mapSize.x) {
        menu.style.left = 'auto';
        menu.style.right = 150 + 'px';
      } else {
        menu.style.left = 150 + 'px';
        menu.style.right = 'auto';
      }

      if (click.containerPoint.y + 150 > mapSize.y) {
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
          e.srcElement.innerText === "Copy Coordinates (C) >")return;
      let menu = this._coordMenu, copyEl = this._items[5].el.el;
      copyEl.setAttribute("aria-expanded","false");
      menu.style.display = "none";
    },

    _onItemMouseOver: function (e) {
      L.DomUtil.addClass(e.target || e.srcElement, 'over');
      if(e.srcElement.innerText === "Copy Coordinates (C) >") this._showCoordMenu(e);
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
      let bounds = this[projection].options.crs.tilematrix.bounds(0), nMinZoom = 0, nMaxZoom = this[projection].options.resolutions.length - 1;
      if(!template.zoomBounds){
        template.zoomBounds ={};
        template.zoomBounds.min=0;
        template.zoomBounds.max=nMaxZoom;
      }
      for(let i=0;i<inputs.length;i++){
        switch(inputs[i].getAttribute("type")){
          case "zoom":
            nMinZoom = +inputs[i].getAttribute("min");
            nMaxZoom = +inputs[i].getAttribute("max");
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
              break;
              case "y":
              case "latitude":
              case "row":
              case "northing":
                boundsUnit = M.axisToCS(inputs[i].getAttribute("axis").toLowerCase());
                bounds.min.y = min;
                bounds.max.y = max;
              break;
            }
          break;
        }
      }
      let zoomBoundsFormatted = {
        minZoom:+template.zoomBounds.min,
        maxZoom:+template.zoomBounds.max,
        minNativeZoom:nMinZoom,
        maxNativeZoom:nMaxZoom
      };
      return {
        zoomBounds:zoomBoundsFormatted,
        bounds:this.boundsToPCRSBounds(bounds,value,projection,boundsUnit)
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
      if(!pcrsBounds || !zoom && +zoom !== 0 || !cs) return undefined;
      switch (cs.toLowerCase()) {
        case "pcrs":
          return pcrsBounds;
        case "tcrs": 
        case "tilematrix":
          let minPixel = this[projection].transformation.transform(pcrsBounds.min, this[projection].scale(+zoom)),
              maxPixel = this[projection].transformation.transform(pcrsBounds.max, this[projection].scale(+zoom));
          if (cs.toLowerCase() === "tcrs") return L.bounds(minPixel, maxPixel);
          let tileSize = M[projection].options.crs.tile.bounds.max.x;
          return L.bounds(L.point(minPixel.x / tileSize, minPixel.y / tileSize), L.point(maxPixel.x / tileSize,maxPixel.y / tileSize)); 
        case "gcrs":
          let minGCRS = this[projection].unproject(pcrsBounds.min),
              maxGCRS = this[projection].unproject(pcrsBounds.max);
          return L.bounds(L.point(minGCRS.lng, minGCRS.lat), L.point(maxGCRS.lng, maxGCRS.lat)); 
        default:
          return undefined;
      }
    },

    pointToPCRSPoint: function(p, zoom, projection, cs){
      if(!p || !zoom && +zoom !== 0 || !cs || !projection) return undefined;
      let tileSize = M[projection].options.crs.tile.bounds.max.x;
      switch(cs.toUpperCase()){
        case "TILEMATRIX":
          return M.pixelToPCRSPoint(L.point(p.x*tileSize,p.y*tileSize),zoom,projection);
        case "PCRS":
          return p;
        case "TCRS" :
          return M.pixelToPCRSPoint(p,zoom,projection);
        case "GCRS":
          return this[projection].project(L.latLng(p.y,p.x));
        default:
          return undefined;
      }
    },

    pixelToPCRSPoint: function(p, zoom, projection){
      if(!p || !zoom && +zoom !== 0) return undefined;
      return this[projection].transformation.untransform(p,this[projection].scale(zoom));
    },

    boundsToPCRSBounds: function(bounds, zoom, projection, cs){
      if(!bounds || !zoom && +zoom !== 0 || !cs) return undefined;
      return L.bounds(M.pointToPCRSPoint(bounds.min, zoom, projection, cs), M.pointToPCRSPoint(bounds.max, zoom, projection, cs));
    },

    //L.bounds have fixed point positions, where min is always topleft, max is always bottom right, and the values are always sorted by leaflet
    //important to consider when working with pcrs where the origin is not topleft but rather bottomleft, could lead to confusion
    pixelToPCRSBounds : function(bounds, zoom, projection){
      if(!bounds || !bounds.max || !bounds.min ||zoom === undefined || zoom === null || zoom instanceof Object) return undefined;
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
        if (!(container instanceof Element) || !mapml || !mapml.querySelector('link[rel=stylesheet],style')) return;

        if(base instanceof Element) {
          base = base.getAttribute('href')?base.getAttribute('href'):document.URL;
        } else if (!base || base==="" || base instanceof Object) {
          return;
        }

        var ss = [];
        var stylesheets = mapml.querySelectorAll('link[rel=stylesheet],style');
        for (var i=0;i<stylesheets.length;i++) {
          if (stylesheets[i].nodeName.toUpperCase() === "LINK" ) {
            var href = stylesheets[i].hasAttribute('href') ? new URL(stylesheets[i].getAttribute('href'),base).href: null;
            if (href) {
              if (!container.querySelector("link[href='"+href+"']")) {
                var linkElm = document.createElement("link");
                linkElm.setAttribute("href", href);
                linkElm.setAttribute("rel", "stylesheet");
                ss.push(linkElm);
              }
            }  
          } else { // <style>
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
  };

  var ReloadButton = L.Control.extend({
    options: {
      position: 'topleft',
    },

    onAdd: function (map) {
      let container = L.DomUtil.create("div", "mapml-reload-button leaflet-bar");

      let link = L.DomUtil.create("a", "mapml-reload-button", container);
      link.innerHTML = "&#x021BA";
      link.href = "#";
      link.title = "Reload";
      link.setAttribute('role', 'button');
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

  var Crosshair = L.Layer.extend({
    onAdd: function (map) {

      //SVG crosshair design from https://github.com/xguaita/Leaflet.MapCenterCoord/blob/master/src/icons/MapCenterCoordIcon1.svg?short_path=81a5c76
      let svgInnerHTML = `<svg
    xmlns:dc="http://purl.org/dc/elements/1.1/"
    xmlns:cc="http://creativecommons.org/ns#"
    xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
    xmlns:svg="http://www.w3.org/2000/svg"
    xmlns="http://www.w3.org/2000/svg"
    version="1.1"
    x="0px"
    y="0px"
    viewBox="0 0 99.999998 99.999998"
    xml:space="preserve">
    <g><circle
        r="3.9234731"
        cy="50.21946"
        cx="50.027821"
        style="color:#000000;clip-rule:nonzero;display:inline;overflow:visible;isolation:auto;mix-blend-mode:normal;color-interpolation:sRGB;color-interpolation-filters:linearRGB;solid-color:#000000;solid-opacity:1;fill:#000000;fill-opacity:1;fill-rule:nonzero;stroke:#ffffff;stroke-width:2;stroke-linecap:round;stroke-linejoin:round;stroke-miterlimit:4;stroke-dasharray:none;stroke-dashoffset:0;stroke-opacity:1;color-rendering:auto;image-rendering:auto;shape-rendering:auto;text-rendering:auto;enable-background:accumulate" /><path
        d="m 4.9734042,54.423642 31.7671398,0 c 2.322349,0 4.204185,-1.881836 4.204185,-4.204185 0,-2.322349 -1.881836,-4.204184 -4.204185,-4.204184 l -31.7671398,0 c -2.3223489,-2.82e-4 -4.20418433,1.881554 -4.20418433,4.204184 0,2.322631 1.88183543,4.204185 4.20418433,4.204185 z"
        style="color:#000000;clip-rule:nonzero;display:inline;overflow:visible;isolation:auto;mix-blend-mode:normal;color-interpolation:sRGB;color-interpolation-filters:linearRGB;solid-color:#000000;solid-opacity:1;fill:#000000;fill-opacity:1;fill-rule:nonzero;stroke:#ffffff;stroke-width:3;stroke-linecap:round;stroke-linejoin:round;stroke-miterlimit:4;stroke-dasharray:none;stroke-dashoffset:0;stroke-opacity:1;color-rendering:auto;image-rendering:auto;shape-rendering:auto;text-rendering:auto;enable-background:accumulate" /><path
        d="m 54.232003,5.1650429 c 0,-2.3223489 -1.881836,-4.20418433 -4.204184,-4.20418433 -2.322349,0 -4.204185,1.88183543 -4.204185,4.20418433 l 0,31.7671401 c 0,2.322349 1.881836,4.204184 4.204185,4.204184 2.322348,0 4.204184,-1.881835 4.204184,-4.204184 l 0,-31.7671401 z"
        style="fill:#000000;stroke:#ffffff;stroke-width:3;stroke-linecap:round;stroke-linejoin:round;stroke-miterlimit:4;stroke-dasharray:none;stroke-opacity:1;fill-opacity:1" /><path
        d="m 99.287826,50.219457 c 0,-2.322349 -1.881835,-4.204184 -4.204184,-4.204184 l -31.76714,0 c -2.322349,0 -4.204184,1.881835 -4.204184,4.204184 0,2.322349 1.881835,4.204185 4.204184,4.204185 l 31.76714,0 c 2.320658,0 4.204184,-1.881836 4.204184,-4.204185 z"
        style="color:#000000;clip-rule:nonzero;display:inline;overflow:visible;isolation:auto;mix-blend-mode:normal;color-interpolation:sRGB;color-interpolation-filters:linearRGB;solid-color:#000000;solid-opacity:1;fill:#000000;fill-opacity:1;fill-rule:nonzero;stroke:#ffffff;stroke-width:3;stroke-linecap:round;stroke-linejoin:round;stroke-miterlimit:4;stroke-dasharray:none;stroke-dashoffset:0;stroke-opacity:1;color-rendering:auto;image-rendering:auto;shape-rendering:auto;text-rendering:auto;enable-background:accumulate" /><path
        d="m 45.823352,95.27359 c 0,2.322349 1.881836,4.204184 4.204185,4.204184 2.322349,0 4.204184,-1.881835 4.204184,-4.204184 l 0,-31.76714 c 0,-2.322349 -1.881835,-4.204185 -4.204184,-4.204185 -2.322349,0 -4.204185,1.881836 -4.204185,4.204185 l 0,31.76714 z"
        style="color:#000000;clip-rule:nonzero;display:inline;overflow:visible;isolation:auto;mix-blend-mode:normal;color-interpolation:sRGB;color-interpolation-filters:linearRGB;solid-color:#000000;solid-opacity:1;fill:#000000;fill-opacity:1;fill-rule:nonzero;stroke:#ffffff;stroke-width:3;stroke-linecap:round;stroke-linejoin:round;stroke-miterlimit:4;stroke-dasharray:none;stroke-dashoffset:0;stroke-opacity:1;color-rendering:auto;image-rendering:auto;shape-rendering:auto;text-rendering:auto;enable-background:accumulate" /></g></svg>
 `;

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
        this._container.style.visibility = null;
      } else {
        this._container.style.visibility = "hidden";
      }
    },

    _addOrRemoveMapOutline: function (e) {
      let mapContainer = this._map._container;
      if (this._map.isFocused && !this._outline) {
        this._outline = L.DomUtil.create("div", "mapml-outline", mapContainer);
        this._outline.style.width = mapContainer.style.width;
        this._outline.style.height = mapContainer.style.height;
        //mapContainer.style.outlineStyle = "auto";
        //.mapContainer.style.outlineColor = "#44A7CB";
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

    _isMapFocused: function (e) {
      //set this._map.isFocused = true if arrow buttons are used
      if (this._map._container.parentNode.activeElement.classList.contains("leaflet-container") && ["keydown"].includes(e.type) && (e.shiftKey && e.keyCode === 9)) {
        this._map.isFocused = false;
      } else if (this._map._container.parentNode.activeElement.classList.contains("leaflet-container") && ["keyup", "keydown"].includes(e.type)) {
        this._map.isFocused = true;
      } else {
        this._map.isFocused = false;
      }
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
    options: {
      accessibleTitle: "Feature",
    },

    /**
     * Initializes the M.Feature
     * @param {HTMLElement} markup - The markup representation of the feature
     * @param {Object} options - The options of the feature
     */
    initialize: function (markup, options) {
      this.type = markup.tagName.toUpperCase();

      if(this.type === "POINT" || this.type === "MULTIPOINT") options.fillOpacity = 1;
      L.setOptions(this, options);

      this._createGroup();  // creates the <g> element for the feature, or sets the one passed in options as the <g>

      this._parts = [];
      this._markup = markup;
      this.options.zoom = markup.getAttribute('zoom') || this.options.nativeZoom;

      this._convertMarkup();

      if(markup.querySelector('span') || markup.querySelector('a')){
        this._generateOutlinePoints();
      }

      this.isClosed = this._isClosed();
    },

    /**
     * Removes the focus handler, and calls the leaflet L.Path.onRemove
     */
    onRemove: function () {
      L.DomEvent.off(this.group, "keyup keydown mousedown", this._handleFocus, this);
      L.Path.prototype.onRemove.call(this);
    },

    /**
     * Creates the <g> conditionally and also applies event handlers
     * @private
     */
    _createGroup: function(){
      if(this.options.multiGroup){
        this.group = this.options.multiGroup;
      } else {
        this.group = L.SVG.create('g');
        if(this.options.interactive) this.group.setAttribute("aria-expanded", "false");
        this.group.setAttribute('aria-label', this.options.accessibleTitle);
        if(this.options.featureID) this.group.setAttribute("data-fid", this.options.featureID);
        L.DomEvent.on(this.group, "keyup keydown mousedown", this._handleFocus, this);
      }
    },

    /**
     * Handler for focus events
     * @param {L.DOMEvent} e - Event that occured
     * @private
     */
    _handleFocus: function(e) {
      if((e.keyCode === 9 || e.keyCode === 16 || e.keyCode === 13) && e.type === "keyup" && e.target.tagName === "g"){
        this.openTooltip();
      } else {
        this.closeTooltip();
      }
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
     * Converts this._markup to the internal structure of features
     * @private
     */
    _convertMarkup: function () {
      if (!this._markup) return;

      let attr = this._markup.attributes;
      this.featureAttributes = {};
      for(let i = 0; i < attr.length; i++){
        this.featureAttributes[attr[i].name] = attr[i].value;
      }

      let first = true;
      for (let c of this._markup.querySelectorAll('coordinates')) {              //loops through the coordinates of the child
        let ring = [], subrings = [];
        this._coordinateToArrays(c, ring, subrings, this.options.className);              //creates an array of pcrs points for the main ring and the subparts
        if (!first && this.type === "POLYGON") {
          this._parts[0].rings.push(ring[0]);
          if (subrings.length > 0)
            this._parts[0].subrings = this._parts[0].subrings.concat(subrings);
        } else if (this.type === "MULTIPOINT") {
          for (let point of ring[0].points.concat(subrings)) {
            this._parts.push({ rings: [{ points: [point] }], subrings: [], cls: point.cls || this.options.className });
          }
        } else {
          this._parts.push({ rings: ring, subrings: subrings, cls: this.featureAttributes.class || this.options.className });
        }
        first = false;
      }
    },

    /**
     * Generates the feature outline, subtracting the spans to generate those separately
     * @private
     */
    _generateOutlinePoints: function () {
      if (this.type === "MULTIPOINT" || this.type === "POINT" || this.type === "LINESTRING" || this.type === "MULTILINESTRING") return;

      this._outline = [];
      for (let coords of this._markup.querySelectorAll('coordinates')) {
        let nodes = coords.childNodes, cur = 0, tempDiv = document.createElement('div'), nodeLength = nodes.length;
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
     * Converts coordinates element to an object representing the parts and subParts
     * @param {HTMLElement} coords - A single coordinates element
     * @param {Object[]} main - An empty array representing the main parts
     * @param {Object[]} subParts - An empty array representing the sub parts
     * @param {boolean} isFirst - A true | false representing if the current HTML element is the parent coordinates element or not
     * @param {string} cls - The class of the coordinate/span
     * @private
     */
    _coordinateToArrays: function (coords, main, subParts, isFirst = true, cls = undefined) {
      for (let span of coords.children) {
        this._coordinateToArrays(span, main, subParts, false, span.getAttribute("class"));
      }
      let noSpan = coords.textContent.replace(/(<([^>]+)>)/ig, ''),
          pairs = noSpan.match(/(\S+\s+\S+)/gim), local = [];
      for (let p of pairs) {
        let numPair = [];
        p.split(/\s+/gim).forEach(M.parseNumber, numPair);
        let point = M.pointToPCRSPoint(L.point(numPair), this.options.zoom, this.options.projection, this.options.nativeCS);
        local.push(point);
        this._bounds = this._bounds ? this._bounds.extend(point) : L.bounds(point, point);
      }
      if (isFirst) {
        main.push({ points: local });
      } else {
        let attrMap = {}, attr = coords.attributes;
        for(let i = 0; i < attr.length; i++){
          if(attr[i].name === "class") continue;
          attrMap[attr[i].name] = attr[i].value;
        }
        subParts.unshift({ points: local, cls: cls || this.options.className, attr: attrMap});
      }
    },

    /**
     * Returns if the feature is closed or open, useful when styling
     * @returns {boolean}
     * @private
     */
    _isClosed: function () {
      switch (this.type) {
        case 'POLYGON':
        case 'MULTIPOLYGON':
        case 'POINT':
        case 'MULTIPOINT':
          return true;
        case 'LINESTRING':
        case 'MULTILINESTRING':
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
          this._createPath(p, layer.options.className, layer.featureAttributes['aria-label'], true, layer.featureAttributes);
          if(layer.outlinePath) p.path.style.stroke = "none";
        }
        if (p.subrings) {
          for (let r of p.subrings) {
            this._createPath(r, layer.options.className, r.attr['aria-label'], false, r.attr);
            if(r.attr && r.attr.tabindex){
              p.path.setAttribute('tabindex', r.attr.tabindex || '0');
            }
          }
        }
        this._updateStyle(layer);
      }
      if(stampLayer){
        let stamp = L.stamp(layer);
        this._layers[stamp] = layer;
        layer.group.setAttribute('tabindex', '0');
        L.DomUtil.addClass(layer.group, "leaflet-interactive");
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
          if(name === "id") continue;
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

        if(!outlineAdded && layer.pixelOutline) {
          layer.group.appendChild(layer.outlinePath);
          outlineAdded = true;
        }

        for (let subP of p.subrings) {
          if (subP.path)
            layer.group.appendChild(subP.path);
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
     * Adds layer to feature group
     * @param {M.Feature} layer - The layer to be added
     */
    addLayer: function (layer) {
      layer.openTooltip = () => { this.openTooltip(); };         // needed to open tooltip of child features
      layer.closeTooltip = () => { this.closeTooltip(); };       // needed to close tooltip of child features
      L.FeatureGroup.prototype.addLayer.call(this, layer);
    },

    /**
     * Focuses the previous function in the sequence on previous button press
     * @param e
     * @private
     */
    _previousFeature: function(e){
      let group = this._source.group.previousSibling;
      if(!group){
        let currentIndex = this._source.group.closest("div.mapml-layer").style.zIndex;
        let overlays = this._map.getPane("overlayPane").children;
        for(let i = overlays.length - 1; i >= 0; i--){
          let layer = overlays[i];
          if(layer.style.zIndex >= currentIndex) continue;
          group = layer.querySelector("g.leaflet-interactive");
          if(group){
            group = group.parentNode.lastChild;
            break;
          }
        }
        if (!group) group = this._source.group;
      }
      group.focus();
      this._map.closePopup();
    },

    /**
     * Focuses next feature in sequence
     * @param e
     * @private
     */
    _nextFeature: function(e){
      let group = this._source.group.nextSibling;
      if(!group){
        let currentIndex = this._source.group.closest("div.mapml-layer").style.zIndex;

        for(let layer of this._map.getPane("overlayPane").children){
          if(layer.style.zIndex <= currentIndex) continue;
          group = layer.querySelectorAll("g.leaflet-interactive");
          if(group.length > 0)break;
        }
        group = group && group.length > 0 ? group[0] : this._source.group;
      }
      group.focus();
      this._map.closePopup();
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

  M.QueryHandler = QueryHandler;
  M.ContextMenu = ContextMenu;

  // see https://leafletjs.com/examples/extending/extending-3-controls.html#handlers
  L.Map.addInitHook('addHandler', 'query', M.QueryHandler);
  L.Map.addInitHook('addHandler', 'contextMenu', M.ContextMenu);

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
