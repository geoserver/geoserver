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
import './leaflet-src.js';  // a (very slightly) modified version of Leaflet for use as browser module
import './proj4-src.js';        // modified version of proj4; could be stripped down for mapml
import './proj4leaflet.js'; // not modified, seems to adapt proj4 for leaflet use.
import './mapml.js';       // refactored URI usage, replaced with URL standard
import { MapLayer } from './layer.js';

export class MapViewer extends HTMLElement {
  static get observedAttributes() {
    return ['lat', 'lon', 'zoom', 'projection', 'width', 'height', 'controls'];
  }
  // see comments below regarding attributeChangedCallback vs. getter/setter
  // usage.  Effectively, the user of the element must use the property, not
  // the getAttribute/setAttribute/removeAttribute DOM API, because the latter
  // calls don't result in the getter/setter being called (so you have to use
  // the getter/setter directly)
  get controls() {
    return this.hasAttribute('controls');
  }
  set controls(value) {
    const hasControls = Boolean(value);
    if (hasControls)
      this.setAttribute('controls', '');
    else
      this.removeAttribute('controls');
    this._toggleControls(hasControls);
  }
  get controlslist() {
    return this.hasAttribute('controlslist') ? this.getAttribute("controlslist") : "";
  }
  set controlslist(val) {
    let options = ["nofullscreen", "nozoom", "nolayer", "noreload"],
        lowerVal = val.toLowerCase();
    if (this.controlslist.includes(lowerVal) || !options.includes(lowerVal))return;
    this.setAttribute("controlslist", this.controlslist+` ${lowerVal}`);
  }
  get lat() {
    return this.hasAttribute("lat") ? this.getAttribute("lat") : "0";
  }
  set lat(val) {
    if (val) {
      this.setAttribute("lat", val);
    }
  }
  get lon() {
    return this.hasAttribute("lon") ? this.getAttribute("lon") : "0";
  }
  set lon(val) {
    if (val) {
      this.setAttribute("lon", val);
    }
  }
  get projection() {
    return this.hasAttribute("projection") ? this.getAttribute("projection") : "";
  }
  set projection(val) {
    if(val && M[val]){
      this.setAttribute('projection', val);
      if (this._map && this._map.options.projection !== val){
        this._map.options.crs = M[val];
        this._map.options.projection = val;
        for(let layer of this.querySelectorAll("layer-")){
          layer.removeAttribute("disabled");
          let reAttach = this.removeChild(layer);
          this.appendChild(reAttach);
        }
        if(this._debug) for(let i = 0; i<2;i++) this.toggleDebug();
      } else this.dispatchEvent(new CustomEvent('createmap'));
    } else throw new Error("Undefined Projection");
  }
  get zoom() {
    return this.hasAttribute("zoom") ? this.getAttribute("zoom") : 0;
  }
  set zoom(val) {
      var parsedVal = parseInt(val,10);
      if (!isNaN(parsedVal) && (parsedVal >= 0 && parsedVal <= 25)) {
        this.setAttribute('zoom', parsedVal);
      }
  }
  get layers() {
    return this.getElementsByTagName('layer-');
  }

  get extent(){
    let map = this._map,
      pcrsBounds = M.pixelToPCRSBounds(
        map.getPixelBounds(),
        map.getZoom(),
        map.options.projection);
    let formattedExtent = M.convertAndFormatPCRS(pcrsBounds, map);
    if(map.getMaxZoom() !== Infinity){
      formattedExtent.zoom = {
        minZoom:map.getMinZoom(),
        maxZoom:map.getMaxZoom()
      };
    }
    return (formattedExtent);
  }

  constructor() {
    // Always call super first in constructor
    super();
    this._source = this.outerHTML;
    let tmpl = document.createElement('template');
    tmpl.innerHTML = `<link rel="stylesheet" href="${new URL("mapml.css", import.meta.url).href}">`; // jshint ignore:line
    
    let shadowRoot = this.attachShadow({mode: 'open'});
    this._container = document.createElement('div');

    let output = "<output role='status' aria-live='polite' aria-atomic='true' class='mapml-screen-reader-output'></output>";
    this._container.insertAdjacentHTML("beforeend", output);

    // Set default styles for the map element.
    let mapDefaultCSS = document.createElement('style');
    mapDefaultCSS.innerHTML =
    `:host {` +
    `all: initial;` + // Reset properties inheritable from html/body, as some inherited styles may cause unexpected issues with the map element's components (https://github.com/Maps4HTML/Web-Map-Custom-Element/issues/140).
    `contain: layout size;` + // Contain layout and size calculations within the map element.
    `display: inline-block;` + // This together with dimension properties is required so that Leaflet isn't working with a height=0 box by default.
    `height: 150px;` + // Provide a "default object size" (https://github.com/Maps4HTML/HTML-Map-Element/issues/31).
    `width: 300px;` +
    `border-width: 2px;` + // Set a default border for contrast, similar to UA default for iframes.
    `border-style: inset;` +
    `}` +
    `:host([frameborder="0"]) {` +
    `border-width: 0;` +
    `}` +
    `:host([hidden]) {` +
    `display: none!important;` +
    `}` +
    `:host .leaflet-control-container {` +
    `visibility: hidden!important;` + // Visibility hack to improve percieved performance (mitigate FOUC) – visibility is unset in mapml.css! (https://github.com/Maps4HTML/Web-Map-Custom-Element/issues/154).
    `}`;
    
    // Hide all (light DOM) children of the map element.
    let hideElementsCSS = document.createElement('style');
    hideElementsCSS.innerHTML =
    `mapml-viewer > * {` +
    `display: none!important;` +
    `}`;

    shadowRoot.appendChild(mapDefaultCSS);
    shadowRoot.appendChild(tmpl.content.cloneNode(true));
    shadowRoot.appendChild(this._container);

    this.appendChild(hideElementsCSS);

    this._toggleState = false;
    this.controlsListObserver = new MutationObserver((m) => {
      m.forEach((change)=>{
        if(change.type==="attributes" && change.attributeName === "controlslist")
          this.setControls(false,false,false);
      });
    });
    this.controlsListObserver.observe(this, {attributes:true});
  }
  connectedCallback() {
    if (this.isConnected) {

      // the dimension attributes win, if they're there. A map does not
      // have an intrinsic size, unlike an image or video, and so must
      // have a defined width and height.
      var s = window.getComputedStyle(this),
        wpx = s.width, hpx=s.height,
        w = parseInt(wpx.replace('px','')),
        h = parseInt(hpx.replace('px',''));

      if (wpx === "" || hpx === "") {
         return;
      }

      if (!this.width || this.width !== w) {
        this._container.style.width = wpx;
        this.width = w;
      } else {
        this._container.style.width = this.width+"px";
      }

      if (!this.height || this.height !== h) {
        this._container.style.height = hpx;
        this.height = h;
      } else {
        this._container.style.height = this.height+"px";
      }

      // create an array to track the history of the map and the current index
      if(!this._history){
        this._history = [];
        this._historyIndex = -1;
        this._traversalCall = false;
      }

      // wait for createmap event before creating leaflet map
      // this allows a safeguard for the case where loading a custom TCRS takes longer than loading mapml-viewer.js/web-map.js
      this.addEventListener('createmap', ()=>{
        if (!this._map) {
          this._map = L.map(this._container, {
            center: new L.LatLng(this.lat, this.lon),
            projection: this.projection,
            query: true,
            contextMenu: true,
            announceMovement: M.options.announceMovement,
            featureIndex: true,
            mapEl: this,
            crs: M[this.projection],
            zoom: this.zoom,
            zoomControl: false,
            // because the M.MapMLLayer invokes _tileLayer._onMoveEnd when
            // the mapml response is received the screen tends to flash.  I'm sure
            // there is a better configuration than that, but at this moment
            // I'm not sure how to approach that issue.
            // See https://github.com/Maps4HTML/MapML-Leaflet-Client/issues/24
            fadeAnimation: true
          });
          this._addToHistory();
          // the attribution control is not optional
          this._attributionControl =  this._map.attributionControl.setPrefix('<a href="https://www.w3.org/community/maps4html/" title="W3C Maps for HTML Community Group">Maps4HTML</a> | <a href="https://leafletjs.com" title="A JS library for interactive maps">Leaflet</a>');
          this._attributionControl.getContainer().setAttribute("role","group");
          this._attributionControl.getContainer().setAttribute("aria-label","Map data attribution");

          this.setControls(false,false,true);
          this._crosshair = M.crosshair().addTo(this._map);
          
          // https://github.com/Maps4HTML/Web-Map-Custom-Element/issues/274
          this.setAttribute('role', 'application');
          // Make the Leaflet container element programmatically identifiable
          // (https://github.com/Leaflet/Leaflet/issues/7193).
          this._container.setAttribute('role', 'region');
          this._container.setAttribute('aria-label', 'Interactive map');
    
          this._setUpEvents();
          // this.fire('load', {target: this});
        }
      }, {once:true});
 
      let custom = !(["CBMTILE","APSTILE","OSMTILE","WGS84"].includes(this.projection));
      // if the page doesn't use nav.js or isn't custom then dispatch createmap event	
      if(!custom){	
        this.dispatchEvent(new CustomEvent('createmap'));
      }
    }
  }
  disconnectedCallback() {
    //this._removeEvents();
    delete this._map;
  }
  adoptedCallback() {
//    console.log('Custom map element moved to new page.');
  }

  setControls(isToggle, toggleShow, setup){
    if (this.controls && this._map) {
      let controls = ["_zoomControl", "_reloadButton", "_fullScreenControl", "_layerControl"],
          options = ["nozoom", "noreload", "nofullscreen", 'nolayer'],
          mapSize = this._map.getSize().y,
          totalSize = 0;

      //removes the left hand controls, if not done they will be re-added in the incorrect order
      //better to just reset them
      for(let i = 0 ; i<3;i++){
        if(this[controls[i]]){
          this._map.removeControl(this[controls[i]]);
          delete this[controls[i]];
        }
      }

      if (!this.controlslist.toLowerCase().includes("nolayer") && !this._layerControl && this.layers.length > 0){
        this._layerControl = M.mapMlLayerControl(null,{"collapsed": true, mapEl: this}).addTo(this._map);
        //if this is the initial setup the layers dont need to be readded, causes issues if they are
        if(!setup){
          for (var i=0;i<this.layers.length;i++) {
            if (!this.layers[i].hidden) {
              this._layerControl.addOverlay(this.layers[i]._layer, this.layers[i].label);
              this._map.on('moveend', this.layers[i]._validateDisabled,  this.layers[i]);
              this.layers[i]._layerControl = this._layerControl;
            }
          }
          this._map.fire("validate");
        }
      }
      if (!this.controlslist.toLowerCase().includes("nozoom") && !this._zoomControl && (totalSize + 93) <= mapSize){
        totalSize += 93;
        this._zoomControl = L.control.zoom().addTo(this._map);
      }
      if (!this.controlslist.toLowerCase().includes("noreload") && !this._reloadButton && (totalSize + 49) <= mapSize){
        totalSize += 49;
        this._reloadButton = M.reloadButton().addTo(this._map);
      }
      if (!this.controlslist.toLowerCase().includes("nofullscreen") && !this._fullScreenControl && (totalSize + 49) <= mapSize){
        totalSize += 49;
        this._fullScreenControl = M.fullscreenButton().addTo(this._map);
      }
      //removes any control layers that are not needed, either by the toggling or by the controlslist attribute
      for(let i in options){
        if(this[controls[i]] && (this.controlslist.toLowerCase().includes(options[i]) || (isToggle && !toggleShow ))){
          this._map.removeControl(this[controls[i]]);
          delete this[controls[i]];
        }
      }
    }
  }
  attributeChangedCallback(name, oldValue, newValue) {
//    console.log('Attribute: ' + name + ' changed from: '+ oldValue + ' to: '+newValue);
    // "Best practice": handle side-effects in this callback
    // https://developers.google.com/web/fundamentals/web-components/best-practices
    // https://developers.google.com/web/fundamentals/web-components/best-practices#avoid-reentrancy
    // note that the example is misleading, since the user can't use
    // setAttribute or removeAttribute to set the property, they need to use
    // the property directly in their API usage, which kinda sucks
    /*
  const hasValue = newValue !== null;
  switch (name) {
    case 'checked':
      // Note the attributeChangedCallback is only handling the *side effects*
      // of setting the attribute.
      this.setAttribute('aria-checked', hasValue);
      break;
    ...
  }     */
  }
  _dropHandler(event) {
    event.preventDefault();
    // create a new <layer-> child of this <mapml-viewer> element
      let l = new MapLayer();
      l.src = event.dataTransfer.getData("text");
      l.label = 'Layer';
      l.checked = 'true';
      this.appendChild(l);
      l.addEventListener("error", function () {
        if (l.parentElement) {
          // should invoke lifecyle callbacks automatically by removing it from DOM
          l.parentElement.removeChild(l);
        }
        // garbage collect it
        l = null;
      });
  }
  _dragoverHandler(event) {
    function contains(list, value) {
      for( var i = 0; i < list.length; ++i ) {
        if(list[i] === value) return true;
      }
      return false;
    }
    // check if the thing being dragged is a URL
    var isLink = contains( event.dataTransfer.types, "text/uri-list");
    if (isLink) {
      event.preventDefault();
      event.dataTransfer.dropEffect = "copy";
    }
  }
  _removeEvents() {
    if (this._map) {
      this._map.off('preclick click dblclick mousemove mouseover mouseout mousedown mouseup contextmenu', false, this);
      this._map.off('load movestart move moveend zoomstart zoom zoomend', false, this);
      this.removeEventListener("drop", this._dropHandler, false);
      this.removeEventListener("dragover", this._dragoverHandler, false);
    }
  }
  _setUpEvents() {
    this.addEventListener("drop", this._dropHandler, false);
    this.addEventListener("dragover", this._dragoverHandler, false);
    this.addEventListener("change",
    function(e) {
      if(e.target.tagName === "LAYER-"){
        this.dispatchEvent(new CustomEvent("layerchange", {details:{target: this, originalEvent: e}}));
      }
    }, false);

    this.parentElement.addEventListener('keyup', function (e) {
      if(e.keyCode === 9 && document.activeElement.nodeName === "MAPML-VIEWER"){
        document.activeElement.dispatchEvent(new CustomEvent('mapfocused', {detail:
              {target: this}}));
      }
    });
    this.parentElement.addEventListener('mousedown', function (e) {
      if(document.activeElement.nodeName === "MAPML-VIEWER"){
        document.activeElement.dispatchEvent(new CustomEvent('mapfocused', {detail:
              {target: this}}));
      }
    });
    this._map.on('load',
      function () {
        this.dispatchEvent(new CustomEvent('load', {detail: {target: this}}));
      }, this);
    this._map.on('preclick',
      function (e) {
        this.dispatchEvent(new CustomEvent('preclick', {detail:
          {lat: e.latlng.lat,     lon: e.latlng.lng,
             x: e.containerPoint.x, y: e.containerPoint.y}
         }));
      }, this);
    this._map.on('click',
      function (e) {
        this.dispatchEvent(new CustomEvent('click', {detail:
          {lat: e.latlng.lat,     lon: e.latlng.lng,
             x: e.containerPoint.x, y: e.containerPoint.y}
         }));
      }, this);
    this._map.on('dblclick',
      function (e) {
        this.dispatchEvent(new CustomEvent('dblclick', {detail:
          {lat: e.latlng.lat,     lon: e.latlng.lng,
             x: e.containerPoint.x, y: e.containerPoint.y}
         }));
      }, this);
    this._map.on('mousemove',
      function (e) {
        this.dispatchEvent(new CustomEvent('mousemove', {detail:
          {lat: e.latlng.lat,     lon: e.latlng.lng,
             x: e.containerPoint.x, y: e.containerPoint.y}
         }));
      }, this);
    this._map.on('mouseover',
      function (e) {
        this.dispatchEvent(new CustomEvent('mouseover', {detail:
          {lat: e.latlng.lat,     lon: e.latlng.lng,
             x: e.containerPoint.x, y: e.containerPoint.y}
         }));
      }, this);
    this._map.on('mouseout',
      function (e) {
        this.dispatchEvent(new CustomEvent('mouseout', {detail:
          {lat: e.latlng.lat,     lon: e.latlng.lng,
             x: e.containerPoint.x, y: e.containerPoint.y}
         }));
      }, this);
    this._map.on('mousedown',
      function (e) {
        this.dispatchEvent(new CustomEvent('mousedown', {detail:
          {lat: e.latlng.lat,     lon: e.latlng.lng,
             x: e.containerPoint.x, y: e.containerPoint.y}
         }));
      },this);
    this._map.on('mouseup',
      function (e) {
        this.dispatchEvent(new CustomEvent('mouseup', {detail:
          {lat: e.latlng.lat,     lon: e.latlng.lng,
             x: e.containerPoint.x, y: e.containerPoint.y}
         }));
      }, this);
    this._map.on('contextmenu',
      function (e) {
        this.dispatchEvent(new CustomEvent('contextmenu', {detail:
          {lat: e.latlng.lat,     lon: e.latlng.lng,
             x: e.containerPoint.x, y: e.containerPoint.y}
         }));
      }, this);
    this._map.on('movestart',
      function () {
        this._updateMapCenter();
        this.dispatchEvent(new CustomEvent('movestart', {detail:
          {target: this}}));
      }, this);
    this._map.on('move',
      function () {
        this._updateMapCenter();
        this.dispatchEvent(new CustomEvent('move', {detail:
          {target: this}}));
      }, this);
    this._map.on('moveend',
      function () {
        this._updateMapCenter();
        this._addToHistory();
        this.dispatchEvent(new CustomEvent('moveend', {detail:
          {target: this}}));
      }, this);
    this._map.on('zoomstart',
      function () {
        this._updateMapCenter();
        this.dispatchEvent(new CustomEvent('zoomstart', {detail:
          {target: this}}));
      }, this);
    this._map.on('zoom',
      function () {
        this._updateMapCenter();
        this.dispatchEvent(new CustomEvent('zoom', {detail:
          {target: this}}));
      }, this);
    this._map.on('zoomend',
      function () {
        this._updateMapCenter();
        this.dispatchEvent(new CustomEvent('zoomend', {detail:
          {target: this}}));
      }, this);
  }
  _toggleControls() {
    if (this._map) {
      this.setControls(true, this._toggleState, false);
      this._toggleState = !this._toggleState;
    }
  }

  toggleDebug(){
    if(this._debug){
      this._debug.remove();
      this._debug = undefined;
    } else {
      this._debug = M.debugOverlay().addTo(this._map);
    }
  }
  
  _widthChanged(width) {
    this.style.width = width+"px";
    this._container.style.width = width+"px";
    if (this._map) {
        this._map.invalidateSize(false);
    }
  }
  _heightChanged(height) {
    this.style.height = height+"px";
    this._container.style.height = height+"px";
    if (this._map) {
        this._map.invalidateSize(false);
    }
  }
  zoomTo(lat, lon, zoom) {
    zoom = Number.isInteger(+zoom) ? +zoom : this.zoom;
    let location = new L.LatLng(+lat, +lon);
    this._map.setView(location, zoom);
    this.zoom = zoom;
    this.lat = location.lat;
    this.lon = location.lng;
  }
  _updateMapCenter() {
    // remember to tell Leaflet event handler that 'this' in here refers to
    //  something other than the map in this case the custom polymer element
    this.lat = this._map.getCenter().lat;
    this.lon = this._map.getCenter().lng;
    this.zoom = this._map.getZoom();
  }

  /**
   * Adds to the maps history on moveends
   * @private
   */
  _addToHistory(){
    if(this._traversalCall > 0) { // this._traversalCall tracks how many consecutive moveends to ignore from history
      this._traversalCall--;      // this is useful for ignoring moveends corresponding to back, forward and reload
      return;
    }

    let mapLocation = this._map.getPixelBounds().getCenter();
    let location = {
      zoom: this._map.getZoom(),
      x:mapLocation.x,
      y:mapLocation.y,
    };
    this._historyIndex++;
    this._history.splice(this._historyIndex, 0, location);
  }

  /**
   * Allow user to move back in history
   */
  back(){
    let history = this._history;
    let curr = history[this._historyIndex];

    if(this._historyIndex > 0){
      this._historyIndex--;
      let prev = history[this._historyIndex];

      if(prev.zoom !== curr.zoom){
        this._traversalCall = 2;  // allows the next 2 moveends to be ignored from history

        let currScale = this._map.options.crs.scale(curr.zoom); // gets the scale of the current zoom level
        let prevScale = this._map.options.crs.scale(prev.zoom); // gets the scale of the previous zoom level

        let scale = currScale / prevScale; // used to convert the previous pixel location to be in terms of the current zoom level

        this._map.panBy([((prev.x * scale) - curr.x), ((prev.y * scale) - curr.y)], {animate: false});
        this._map.setZoom(prev.zoom);
      } else {
        this._traversalCall = 1;
        this._map.panBy([(prev.x - curr.x), (prev.y - curr.y)]);
      }
    }
  }

  /**
   * Allows user to move forward in history
   */
  forward(){
    let history = this._history;
    let curr = history[this._historyIndex];
    if(this._historyIndex < history.length - 1){
      this._historyIndex++;
      let next = history[this._historyIndex];

      if(next.zoom !== curr.zoom){
        this._traversalCall = 2; // allows the next 2 moveends to be ignored from history

        let currScale = this._map.options.crs.scale(curr.zoom); // gets the scale of the current zoom level
        let nextScale = this._map.options.crs.scale(next.zoom); // gets the scale of the next zoom level

        let scale = currScale / nextScale; // used to convert the next pixel location to be in terms of the current zoom level

        this._map.panBy([((next.x * scale) - curr.x), ((next.y * scale) - curr.y)], {animate: false});
        this._map.setZoom(next.zoom);
      } else {
        this._traversalCall = 1;
        this._map.panBy([(next.x - curr.x), (next.y - curr.y)]);
      }
    }
  }

  /**
   * Allows the user to reload/reset the map's location to it's initial location
   */
  reload(){
    let initialLocation = this._history.shift();
    let mapLocation = this._map.getPixelBounds().getCenter();
    let curr = {
      zoom: this._map.getZoom(),
      x:mapLocation.x,
      y:mapLocation.y,
    };

    this._history = [initialLocation];
    this._historyIndex = 0;

    if(initialLocation.zoom !== curr.zoom) {
      this._traversalCall = 2; // ignores the next 2 moveend events

      let currScale = this._map.options.crs.scale(curr.zoom); // gets the scale of the current zoom level
      let initScale = this._map.options.crs.scale(initialLocation.zoom); // gets the scale of the initial location's zoom

      let scale = currScale / initScale;

      this._map.panBy([((initialLocation.x * scale) - curr.x), ((initialLocation.y * scale) - curr.y)], {animate: false});
      this._map.setZoom(initialLocation.zoom);
    } else { // if it's on the same zoom level as the initial location, no need to calculate scales
      this._traversalCall = 1;
      this._map.panBy([(initialLocation.x- curr.x), (initialLocation.y - curr.y)]);
    }
  }

  viewSource(){
    let blob = new Blob([this._source],{type:"text/plain"}),
        url = URL.createObjectURL(blob);
    window.open(url);
    URL.revokeObjectURL(url);
  }

  defineCustomProjection(jsonTemplate) {
    let t = JSON.parse(jsonTemplate);
    if (t === undefined || !t.proj4string || !t.projection || !t.resolutions || !t.origin || !t.bounds) throw new Error('Incomplete TCRS Definition');
    if (t.projection.indexOf(":") >= 0) throw new Error('":" is not permitted in projection name');
    if (M[t.projection.toUpperCase()]) return t.projection.toUpperCase();
    let tileSize = [256, 512, 1024, 2048, 4096].includes(t.tilesize)?t.tilesize:256;

    M[t.projection] = new L.Proj.CRS(t.projection, t.proj4string, {
      origin: t.origin,
      resolutions: t.resolutions,
      bounds: L.bounds(t.bounds),
      crs: {
        tcrs: {
          horizontal: {
            name: "x",
            min: 0, 
            max: zoom => (M[t.projection].options.bounds.getSize().x / M[t.projection].options.resolutions[zoom]).toFixed()
          },
          vertical: {
            name: "y",
            min:0, 
            max: zoom => (M[t.projection].options.bounds.getSize().y / M[t.projection].options.resolutions[zoom]).toFixed()
          },
          bounds: zoom => L.bounds([M[t.projection].options.crs.tcrs.horizontal.min,
            M[t.projection].options.crs.tcrs.vertical.min],
            [M[t.projection].options.crs.tcrs.horizontal.max(zoom),
            M[t.projection].options.crs.tcrs.vertical.max(zoom)])
        },
        pcrs: {
          horizontal: {
            name: "easting",
            get min() {return M[t.projection].options.bounds.min.x;},
            get max() {return M[t.projection].options.bounds.max.x;}
          }, 
          vertical: {
            name: "northing", 
            get min() {return M[t.projection].options.bounds.min.y;},
            get max() {return M[t.projection].options.bounds.max.y;}
          },
          get bounds() {return M[t.projection].options.bounds;}
        }, 
        gcrs: {
          horizontal: {
            name: "longitude",
            // set min/max axis values from EPSG registry area of use, retrieved 2019-07-25
            get min() {return M[t.projection].unproject(M.OSMTILE.options.bounds.min).lng;},
            get max() {return M[t.projection].unproject(M.OSMTILE.options.bounds.max).lng;}
          }, 
          vertical: {
            name: "latitude",
            // set min/max axis values from EPSG registry area of use, retrieved 2019-07-25
            get min() {return M[t.projection].unproject(M.OSMTILE.options.bounds.min).lat;},
            get max() {return M[t.projection].unproject(M.OSMTILE.options.bounds.max).lat;}
          },
          get bounds() {return L.latLngBounds(
                [M[t.projection].options.crs.gcrs.vertical.min,M[t.projection].options.crs.gcrs.horizontal.min],
                [M[t.projection].options.crs.gcrs.vertical.max,M[t.projection].options.crs.gcrs.horizontal.max]);}
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
            max: tileSize,
          },
          vertical: {
            name: "j",
            min: 0,
            max: tileSize,
          },
          get bounds() {return L.bounds(
                    [M[t.projection].options.crs.tile.horizontal.min,M[t.projection].options.crs.tile.vertical.min],
                    [M[t.projection].options.crs.tile.horizontal.max,M[t.projection].options.crs.tile.vertical.max]);}
        },
        tilematrix: {
          horizontal: {
            name: "column",
            min: 0,
            max: zoom => (M[t.projection].options.crs.tcrs.horizontal.max(zoom) / M[t.projection].options.crs.tile.bounds.getSize().x).toFixed()
          },
          vertical: {
            name: "row",
            min: 0,
            max: zoom => (M[t.projection].options.crs.tcrs.vertical.max(zoom) / M[t.projection].options.crs.tile.bounds.getSize().y).toFixed()
          },
          bounds: zoom => L.bounds(
                   [M[t.projection].options.crs.tilematrix.horizontal.min,
                   M[t.projection].options.crs.tilematrix.vertical.min],
                   [M[t.projection].options.crs.tilematrix.horizontal.max(zoom),
                   M[t.projection].options.crs.tilematrix.vertical.max(zoom)])
        }
      },
    });      //creates crs using L.Proj
    M[t.projection.toUpperCase()] = M[t.projection]; //adds the projection uppercase to global M
    return t.projection;
  }
}
// need to provide options { extends: ... }  for custom built-in elements
window.customElements.define('mapml-viewer', MapViewer);
window.customElements.define('layer-', MapLayer);
=======
import"./leaflet.js";import"./mapml.js";import DOMTokenList from"./DOMTokenList.js";import{MapLayer}from"./layer.js";import{MapCaption}from"./map-caption.js";import{MapFeature}from"./map-feature.js";import{MapExtent}from"./map-extent.js";class MapViewer extends HTMLElement{static get observedAttributes(){return["lat","lon","zoom","projection","width","height","controls","static","controlslist"]}get controls(){return this.hasAttribute("controls")}set controls(t){Boolean(t)?this.setAttribute("controls",""):this.removeAttribute("controls")}get controlsList(){return this._controlsList}set controlsList(t){this._controlsList.value=t,this.setAttribute("controlslist",t)}get width(){return window.getComputedStyle(this).width.replace("px","")}set width(t){this.setAttribute("width",t)}get height(){return window.getComputedStyle(this).height.replace("px","")}set height(t){this.setAttribute("height",t)}get lat(){return this.hasAttribute("lat")?this.getAttribute("lat"):"0"}set lat(t){t&&this.setAttribute("lat",t)}get lon(){return this.hasAttribute("lon")?this.getAttribute("lon"):"0"}set lon(t){t&&this.setAttribute("lon",t)}get projection(){return this.hasAttribute("projection")?this.getAttribute("projection"):""}set projection(t){if(!t||!M[t])throw new Error("Undefined Projection");if(this.setAttribute("projection",t),this._map&&this._map.options.projection!==t){this._map.options.crs=M[t],this._map.options.projection=t;for(var e of this.querySelectorAll("layer-")){e.removeAttribute("disabled");e=this.removeChild(e);this.appendChild(e)}if(this._debug)for(let t=0;t<2;t++)this.toggleDebug()}else this.dispatchEvent(new CustomEvent("createmap"))}get zoom(){return this.hasAttribute("zoom")?this.getAttribute("zoom"):0}set zoom(t){t=parseInt(t,10);!isNaN(t)&&0<=t&&t<=25&&this.setAttribute("zoom",t)}get layers(){return this.getElementsByTagName("layer-")}get extent(){let t=this._map,e=M.pixelToPCRSBounds(t.getPixelBounds(),t.getZoom(),t.options.projection),o=M._convertAndFormatPCRS(e,t);return t.getMaxZoom()!==1/0&&(o.zoom={minZoom:t.getMinZoom(),maxZoom:t.getMaxZoom()}),o}get static(){return this.hasAttribute("static")}set static(t){Boolean(t)?this.setAttribute("static",""):this.removeAttribute("static")}constructor(){super(),this._source=this.outerHTML,this._history=[],this._historyIndex=-1,this._traversalCall=!1}connectedCallback(){this._initShadowRoot(),this._controlsList=new DOMTokenList(this.getAttribute("controlslist"),this,"controlslist",["noreload","nofullscreen","nozoom","nolayer","noscale","geolocation"]);var t=window.getComputedStyle(this),e=t.width,t=t.height,e=this.hasAttribute("width")?this.getAttribute("width"):parseInt(e.replace("px","")),t=this.hasAttribute("height")?this.getAttribute("height"):parseInt(t.replace("px",""));this._changeWidth(e),this._changeHeight(t),this.addEventListener("createmap",this._createMap),["CBMTILE","APSTILE","OSMTILE","WGS84"].includes(this.projection)&&this.dispatchEvent(new CustomEvent("createmap")),this.setAttribute("role","application"),this._toggleStatic();let o=this.querySelector("map-caption");null!==o&&setTimeout(()=>{this.getAttribute("aria-label")===o.innerHTML&&(this.mapCaptionObserver=new MutationObserver(t=>{this.querySelector("map-caption")!==o&&this.removeAttribute("aria-label")}),this.mapCaptionObserver.observe(this,{childList:!0}))},0)}_initShadowRoot(){this.shadowRoot||this.attachShadow({mode:"open"});let t=document.createElement("template");t.innerHTML=`<link rel="stylesheet" href="${new URL("mapml.css",import.meta.url).href}">`;let e=this.shadowRoot;this._container=document.createElement("div");this._container.insertAdjacentHTML("beforeend","<output role='status' aria-live='polite' aria-atomic='true' class='mapml-screen-reader-output'></output>");let o=document.createElement("style");o.innerHTML=':host {all: initial;contain: layout size;display: inline-block;height: 150px;width: 300px;border-width: 2px;border-style: inset;}:host([frameborder="0"]) {border-width: 0;}:host([hidden]) {display: none!important;}:host .leaflet-control-container {visibility: hidden!important;}';let i=document.createElement("style");i.innerHTML="mapml-viewer > * {display: none!important;}",this.appendChild(i),this._container.setAttribute("role","region"),this._container.setAttribute("aria-label","Interactive map"),e.appendChild(o),e.appendChild(t.content.cloneNode(!0)),e.appendChild(this._container)}_createMap(){this._map||(this._map=L.map(this._container,{center:new L.LatLng(this.lat,this.lon),projection:this.projection,query:!0,contextMenu:!0,announceMovement:M.options.announceMovement,featureIndex:!0,mapEl:this,crs:M[this.projection],zoom:this.zoom,zoomControl:!1,fadeAnimation:!0}),this._addToHistory(),this._createControls(),this._toggleControls(),this._crosshair=M.crosshair().addTo(this._map),M.options.featureIndexOverlayOption&&(this._featureIndexOverlay=M.featureIndexOverlay().addTo(this._map)),this._setUpEvents())}disconnectedCallback(){for(;this.shadowRoot.firstChild;)this.shadowRoot.removeChild(this.shadowRoot.firstChild);delete this._map,this._deleteControls()}adoptedCallback(){}attributeChangedCallback(t,e,o){switch(t){case"controlslist":this._controlsList&&(!1===this._controlsList.valueSet&&(this._controlsList.value=o),this._toggleControls());break;case"controls":null!==e&&null===o?this._hideControls():null===e&&null!==o&&this._showControls();break;case"height":e!==o&&this._changeHeight(o);break;case"width":e!==o&&this._changeWidth(o);break;case"static":this._toggleStatic()}}_createControls(){let t=this._map.getSize().y,e=0;this._layerControl=M.layerControl(null,{collapsed:!0,mapEl:this}).addTo(this._map);let o=M.options.announceScale;"metric"===o&&(o={metric:!0,imperial:!1}),"imperial"===o&&(o={metric:!1,imperial:!0}),this._scaleBar||(this._scaleBar=M.scaleBar(o).addTo(this._map)),!this._zoomControl&&e+93<=t&&(e+=93,this._zoomControl=L.control.zoom().addTo(this._map)),!this._reloadButton&&e+49<=t&&(e+=49,this._reloadButton=M.reloadButton().addTo(this._map)),!this._fullScreenControl&&e+49<=t&&(e+=49,this._fullScreenControl=M.fullscreenButton().addTo(this._map)),this._geolocationButton||(this._geolocationButton=M.geolocationButton().addTo(this._map))}_toggleControls(){!1===this.controls?(this._hideControls(),this._map.contextMenu.toggleContextMenuItem("Controls","disabled")):(this._showControls(),this._map.contextMenu.toggleContextMenuItem("Controls","enabled"))}_hideControls(){this._setControlsVisibility("fullscreen",!0),this._setControlsVisibility("layercontrol",!0),this._setControlsVisibility("reload",!0),this._setControlsVisibility("zoom",!0),this._setControlsVisibility("geolocation",!0),this._setControlsVisibility("scale",!0)}_showControls(){this._setControlsVisibility("fullscreen",!1),this._setControlsVisibility("layercontrol",!1),this._setControlsVisibility("reload",!1),this._setControlsVisibility("zoom",!1),this._setControlsVisibility("geolocation",!0),this._setControlsVisibility("scale",!1),this._controlsList&&this._controlsList.forEach(t=>{switch(t.toLowerCase()){case"nofullscreen":this._setControlsVisibility("fullscreen",!0);break;case"nolayer":this._setControlsVisibility("layercontrol",!0);break;case"noreload":this._setControlsVisibility("reload",!0);break;case"nozoom":this._setControlsVisibility("zoom",!0);break;case"geolocation":this._setControlsVisibility("geolocation",!1);break;case"noscale":this._setControlsVisibility("scale",!0)}}),this._layerControl&&0===this._layerControl._layers.length&&this._layerControl._container.setAttribute("hidden","")}_deleteControls(){delete this._layerControl,delete this._zoomControl,delete this._reloadButton,delete this._fullScreenControl,delete this._geolocationButton,delete this._scaleBar}_setControlsVisibility(t,e){let o;switch(t){case"zoom":this._zoomControl&&(o=this._zoomControl._container);break;case"reload":this._reloadButton&&(o=this._reloadButton._container);break;case"fullscreen":this._fullScreenControl&&(o=this._fullScreenControl._container);break;case"layercontrol":this._layerControl&&(o=this._layerControl._container);break;case"geolocation":this._geolocationButton&&(o=this._geolocationButton._container);break;case"scale":this._scaleBar&&(o=this._scaleBar._container)}o&&(e?([...o.children].forEach(t=>{t.setAttribute("hidden","")}),o.setAttribute("hidden","")):([...o.children].forEach(t=>{t.removeAttribute("hidden")}),o.removeAttribute("hidden")))}_toggleStatic(){var t=this.hasAttribute("static");this._map&&(t?(this._map.dragging.disable(),this._map.touchZoom.disable(),this._map.doubleClickZoom.disable(),this._map.scrollWheelZoom.disable(),this._map.boxZoom.disable(),this._map.keyboard.disable(),this._zoomControl.disable()):(this._map.dragging.enable(),this._map.touchZoom.enable(),this._map.doubleClickZoom.enable(),this._map.scrollWheelZoom.enable(),this._map.boxZoom.enable(),this._map.keyboard.enable(),this._zoomControl.enable()))}_dropHandler(t){t.preventDefault();t=t.dataTransfer.getData("text");M._pasteLayer(this,t)}_dragoverHandler(t){t.preventDefault(),t.dataTransfer.dropEffect="copy"}_removeEvents(){this._map&&(this._map.off(),this.removeEventListener("drop",this._dropHandler,!1),this.removeEventListener("dragover",this._dragoverHandler,!1))}_setUpEvents(){this.addEventListener("drop",this._dropHandler,!1),this.addEventListener("dragover",this._dragoverHandler,!1),this.addEventListener("change",function(t){"LAYER-"===t.target.tagName&&this.dispatchEvent(new CustomEvent("layerchange",{details:{target:this,originalEvent:t}}))},!1),this.parentElement.addEventListener("keyup",function(t){9===t.keyCode&&"MAPML-VIEWER"===document.activeElement.nodeName&&document.activeElement.dispatchEvent(new CustomEvent("mapfocused",{detail:{target:this}}))}),this.addEventListener("keydown",function(t){86===t.keyCode&&t.ctrlKey?navigator.clipboard.readText().then(t=>{M._pasteLayer(this,t)}):32===t.keyCode&&"INPUT"!==this.shadowRoot.activeElement.nodeName&&(t.preventDefault(),this._map.fire("keypress",{originalEvent:t}))}),this.parentElement.addEventListener("mousedown",function(t){"MAPML-VIEWER"===document.activeElement.nodeName&&document.activeElement.dispatchEvent(new CustomEvent("mapfocused",{detail:{target:this}}))}),this._map.on("locationfound",function(t){this.dispatchEvent(new CustomEvent("maplocationfound",{detail:{latlng:t.latlng,accuracy:t.accuracy}}))},this),this._map.on("locationerror",function(t){this.dispatchEvent(new CustomEvent("locationerror",{detail:{error:t.message}}))},this),this._map.on("load",function(){this.dispatchEvent(new CustomEvent("load",{detail:{target:this}}))},this),this._map.on("preclick",function(t){this.dispatchEvent(new CustomEvent("preclick",{detail:{lat:t.latlng.lat,lon:t.latlng.lng,x:t.containerPoint.x,y:t.containerPoint.y}}))},this),this._map.on("click",function(t){this.dispatchEvent(new CustomEvent("click",{detail:{lat:t.latlng.lat,lon:t.latlng.lng,x:t.containerPoint.x,y:t.containerPoint.y}}))},this),this._map.on("dblclick",function(t){this.dispatchEvent(new CustomEvent("dblclick",{detail:{lat:t.latlng.lat,lon:t.latlng.lng,x:t.containerPoint.x,y:t.containerPoint.y}}))},this),this._map.on("mousemove",function(t){this.dispatchEvent(new CustomEvent("mousemove",{detail:{lat:t.latlng.lat,lon:t.latlng.lng,x:t.containerPoint.x,y:t.containerPoint.y}}))},this),this._map.on("mouseover",function(t){this.dispatchEvent(new CustomEvent("mouseover",{detail:{lat:t.latlng.lat,lon:t.latlng.lng,x:t.containerPoint.x,y:t.containerPoint.y}}))},this),this._map.on("mouseout",function(t){this.dispatchEvent(new CustomEvent("mouseout",{detail:{lat:t.latlng.lat,lon:t.latlng.lng,x:t.containerPoint.x,y:t.containerPoint.y}}))},this),this._map.on("mousedown",function(t){this.dispatchEvent(new CustomEvent("mousedown",{detail:{lat:t.latlng.lat,lon:t.latlng.lng,x:t.containerPoint.x,y:t.containerPoint.y}}))},this),this._map.on("mouseup",function(t){this.dispatchEvent(new CustomEvent("mouseup",{detail:{lat:t.latlng.lat,lon:t.latlng.lng,x:t.containerPoint.x,y:t.containerPoint.y}}))},this),this._map.on("contextmenu",function(t){this.dispatchEvent(new CustomEvent("contextmenu",{detail:{lat:t.latlng.lat,lon:t.latlng.lng,x:t.containerPoint.x,y:t.containerPoint.y}}))},this),this._map.on("movestart",function(){this._updateMapCenter(),this.dispatchEvent(new CustomEvent("movestart",{detail:{target:this}}))},this),this._map.on("move",function(){this._updateMapCenter(),this.dispatchEvent(new CustomEvent("move",{detail:{target:this}}))},this),this._map.on("moveend",function(){this._updateMapCenter(),this._addToHistory(),this.dispatchEvent(new CustomEvent("moveend",{detail:{target:this}}))},this),this._map.on("zoomstart",function(){this._updateMapCenter(),this.dispatchEvent(new CustomEvent("zoomstart",{detail:{target:this}}))},this),this._map.on("zoom",function(){this._updateMapCenter(),this.dispatchEvent(new CustomEvent("zoom",{detail:{target:this}}))},this),this._map.on("zoomend",function(){this._updateMapCenter(),this.dispatchEvent(new CustomEvent("zoomend",{detail:{target:this}}))},this),this.addEventListener("fullscreenchange",function(t){null===document.fullscreenElement?this._map.contextMenu.setViewFullScreenInnerHTML("view"):this._map.contextMenu.setViewFullScreenInnerHTML("exit")}),this.addEventListener("keydown",function(t){"MAPML-VIEWER"===document.activeElement.nodeName&&(t.ctrlKey&&82===t.keyCode?(t.preventDefault(),this.reload()):t.altKey&&39===t.keyCode?(t.preventDefault(),this.forward()):t.altKey&&37===t.keyCode&&(t.preventDefault(),this.back()))})}locate(t){this._geolocationButton&&this._geolocationButton.stop(),t?(t.zoomTo&&(t.setView=t.zoomTo,delete t.zoomTo),this._map.locate(t)):this._map.locate({setView:!0,maxZoom:16})}toggleDebug(){this._debug?(this._debug.remove(),this._debug=void 0):this._debug=M.debugOverlay().addTo(this._map)}_changeWidth(t){this._container&&(this._container.style.width=t+"px",this.shadowRoot.styleSheets[0].cssRules[0].style.width=t+"px"),this._map&&this._map.invalidateSize(!1)}_changeHeight(t){this._container&&(this._container.style.height=t+"px",this.shadowRoot.styleSheets[0].cssRules[0].style.height=t+"px"),this._map&&this._map.invalidateSize(!1)}zoomTo(t,e,o){o=Number.isInteger(+o)?+o:this.zoom;e=new L.LatLng(+t,+e);this._map.setView(e,o),this.zoom=o,this.lat=e.lat,this.lon=e.lng}_updateMapCenter(){this.lat=this._map.getCenter().lat,this.lon=this._map.getCenter().lng,this.zoom=this._map.getZoom()}_addToHistory(){var t;0<this._traversalCall?this._traversalCall--:(t=this._map.getPixelBounds().getCenter(),t={zoom:this._map.getZoom(),x:t.x,y:t.y},this._historyIndex++,this._history.splice(this._historyIndex,0,t),this._historyIndex+1!==this._history.length&&(this._history.length=this._historyIndex+1),0===this._historyIndex?(this._map.contextMenu.toggleContextMenuItem("Back","disabled"),this._map.contextMenu.toggleContextMenuItem("Forward","disabled"),this._map.contextMenu.toggleContextMenuItem("Reload","disabled"),this._reloadButton?.disable()):(this._map.contextMenu.toggleContextMenuItem("Back","enabled"),this._map.contextMenu.toggleContextMenuItem("Forward","disabled"),this._map.contextMenu.toggleContextMenuItem("Reload","enabled"),this._reloadButton?.enable()))}back(){var t,e=this._history,o=e[this._historyIndex];0<this._historyIndex&&(this._map.contextMenu.toggleContextMenuItem("Forward","enabled"),this._historyIndex--,t=e[this._historyIndex],0===this._historyIndex&&(this._map.contextMenu.toggleContextMenuItem("Back","disabled"),this._map.contextMenu.toggleContextMenuItem("Reload","disabled"),this._reloadButton?.disable()),t.zoom!==o.zoom?(this._traversalCall=2,e=this._map.options.crs.scale(o.zoom)/this._map.options.crs.scale(t.zoom),this._map.panBy([t.x*e-o.x,t.y*e-o.y],{animate:!1}),this._map.setZoom(t.zoom)):(this._traversalCall=1,this._map.panBy([t.x-o.x,t.y-o.y])))}forward(){var t,e=this._history,o=e[this._historyIndex];this._historyIndex<e.length-1&&(this._map.contextMenu.toggleContextMenuItem("Back","enabled"),this._map.contextMenu.toggleContextMenuItem("Reload","enabled"),this._reloadButton?.enable(),this._historyIndex++,t=e[this._historyIndex],this._historyIndex+1===this._history.length&&this._map.contextMenu.toggleContextMenuItem("Forward","disabled"),t.zoom!==o.zoom?(this._traversalCall=2,e=this._map.options.crs.scale(o.zoom)/this._map.options.crs.scale(t.zoom),this._map.panBy([t.x*e-o.x,t.y*e-o.y],{animate:!1}),this._map.setZoom(t.zoom)):(this._traversalCall=1,this._map.panBy([t.x-o.x,t.y-o.y])))}reload(){var t=this._history.shift(),e=this._map.getPixelBounds().getCenter(),o={zoom:this._map.getZoom(),x:e.x,y:e.y};this._map.contextMenu.toggleContextMenuItem("Back","disabled"),this._map.contextMenu.toggleContextMenuItem("Forward","disabled"),this._map.contextMenu.toggleContextMenuItem("Reload","disabled"),this._reloadButton?.disable(),this._history=[t],this._historyIndex=0,t.zoom!==o.zoom?(this._traversalCall=2,e=this._map.options.crs.scale(o.zoom)/this._map.options.crs.scale(t.zoom),this._map.panBy([t.x*e-o.x,t.y*e-o.y],{animate:!1}),this._map.setZoom(t.zoom)):(this._traversalCall=1,this._map.panBy([t.x-o.x,t.y-o.y]))}_toggleFullScreen(){this._map.toggleFullscreen()}viewSource(){var t=new Blob([this._source],{type:"text/plain"}),t=URL.createObjectURL(t);window.open(t),URL.revokeObjectURL(t)}defineCustomProjection(t){let e=JSON.parse(t);if(!(void 0!==e&&e.proj4string&&e.projection&&e.resolutions&&e.origin&&e.bounds))throw new Error("Incomplete TCRS Definition");if(0<=e.projection.indexOf(":"))throw new Error('":" is not permitted in projection name');if(M[e.projection.toUpperCase()])return e.projection.toUpperCase();t=[256,512,1024,2048,4096].includes(e.tilesize)?e.tilesize:256;return M[e.projection]=new L.Proj.CRS(e.projection,e.proj4string,{origin:e.origin,resolutions:e.resolutions,bounds:L.bounds(e.bounds),crs:{tcrs:{horizontal:{name:"x",min:0,max:t=>Math.round(M[e.projection].options.bounds.getSize().x/M[e.projection].options.resolutions[t])},vertical:{name:"y",min:0,max:t=>Math.round(M[e.projection].options.bounds.getSize().y/M[e.projection].options.resolutions[t])},bounds:t=>L.bounds([M[e.projection].options.crs.tcrs.horizontal.min,M[e.projection].options.crs.tcrs.vertical.min],[M[e.projection].options.crs.tcrs.horizontal.max(t),M[e.projection].options.crs.tcrs.vertical.max(t)])},pcrs:{horizontal:{name:"easting",get min(){return M[e.projection].options.bounds.min.x},get max(){return M[e.projection].options.bounds.max.x}},vertical:{name:"northing",get min(){return M[e.projection].options.bounds.min.y},get max(){return M[e.projection].options.bounds.max.y}},get bounds(){return M[e.projection].options.bounds}},gcrs:{horizontal:{name:"longitude",get min(){return M[e.projection].unproject(M.OSMTILE.options.bounds.min).lng},get max(){return M[e.projection].unproject(M.OSMTILE.options.bounds.max).lng}},vertical:{name:"latitude",get min(){return M[e.projection].unproject(M.OSMTILE.options.bounds.min).lat},get max(){return M[e.projection].unproject(M.OSMTILE.options.bounds.max).lat}},get bounds(){return L.latLngBounds([M[e.projection].options.crs.gcrs.vertical.min,M[e.projection].options.crs.gcrs.horizontal.min],[M[e.projection].options.crs.gcrs.vertical.max,M[e.projection].options.crs.gcrs.horizontal.max])}},map:{horizontal:{name:"i",min:0,max:t=>t.getSize().x},vertical:{name:"j",min:0,max:t=>t.getSize().y},bounds:t=>L.bounds(L.point([0,0]),t.getSize())},tile:{horizontal:{name:"i",min:0,max:t},vertical:{name:"j",min:0,max:t},get bounds(){return L.bounds([M[e.projection].options.crs.tile.horizontal.min,M[e.projection].options.crs.tile.vertical.min],[M[e.projection].options.crs.tile.horizontal.max,M[e.projection].options.crs.tile.vertical.max])}},tilematrix:{horizontal:{name:"column",min:0,max:t=>Math.round(M[e.projection].options.crs.tcrs.horizontal.max(t)/M[e.projection].options.crs.tile.bounds.getSize().x)},vertical:{name:"row",min:0,max:t=>Math.round(M[e.projection].options.crs.tcrs.vertical.max(t)/M[e.projection].options.crs.tile.bounds.getSize().y)},bounds:t=>L.bounds([M[e.projection].options.crs.tilematrix.horizontal.min,M[e.projection].options.crs.tilematrix.vertical.min],[M[e.projection].options.crs.tilematrix.horizontal.max(t),M[e.projection].options.crs.tilematrix.vertical.max(t)])}}}),M[e.projection.toUpperCase()]=M[e.projection],e.projection}geojson2mapml(t,e={}){void 0===e.projection&&(e.projection=this.projection);e=M.geojson2mapml(t,e);return this.appendChild(e),e}}window.customElements.define("mapml-viewer",MapViewer),window.customElements.define("layer-",MapLayer),window.customElements.define("map-caption",MapCaption),window.customElements.define("map-feature",MapFeature),window.customElements.define("map-extent",MapExtent);export{MapViewer};
//# sourceMappingURL=mapml-viewer.js.map
>>>>>>> 4af0ac91de ([GEOS-10940] Update MapML viewer to release 0.11.0)
