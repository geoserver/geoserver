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
import './leaflet-src.js';  // a lightly modified version of Leaflet for use as browser module
import './proj4-src.js';        // modified version of proj4; could be stripped down for mapml
import './proj4leaflet.js'; // not modified, seems to adapt proj4 for leaflet use. 
import './mapml.js';       // refactored URI usage, replaced with URL standard

export class MapArea extends HTMLAreaElement {
  static get observedAttributes() {
    return ['coords','alt','href','shape','rel','type','target'];
  }
  // see comments below regarding attributeChangedCallback vs. getter/setter 
  // usage.  Effectively, the user of the element must use the property, not
  // the getAttribute/setAttribute/removeAttribute DOM API, because the latter
  // calls don't result in the getter/setter being called (so you have to use
  // the getter/setter directly)
  get alt() {
    return this.hasAttribute('alt') ?  this.getAttribute('alt') : "";
  }
  set alt(value) {
    this.setAttribute('controls', value);
  }
  get coords() {
    return this.hasAttribute('coords') ? this.getAttribute('coords') : "";
  }
  set coords(coordinates) {
    // what to do.  Probably replace the feature with a new one, without changing 
    // anything else...
  }
  get href() {
    return this.hasAttribute('href') ?  this.getAttribute('href') : "";
  }
  set href(url) {
    this.href = url;
  }
  get shape() {
    return this.hasAttribute('shape') ?  this.getAttribute('shape') : "default";
  }
  set shape(shape) {
    shape = shape.toLowerCase();
    var re = /default|circle|rect|poly/;
    if (shape.search(re)) {
      this.shape = shape;
    }
  }
  get rel() {
    return this.hasAttribute('rel') ?  this.getAttribute('rel') : "";
  }
  set rel(rel) {
    this.rel = rel;
  }
  get type() {
    return this.hasAttribute('type') ?  this.getAttribute('type') : "";
  }
  set type(type) {
    this.type = type;
  }
  get target() {
    return this.hasAttribute('target') ?  this.getAttribute('target') : "";
  }
  constructor() {
    // Always call super first in constructor
    super();
  }
  attributeChangedCallback(name, oldValue, newValue) {
    
  }
  connectedCallback() {
    // if the map has been attached, set this layer up wrt Leaflet map
    if (this.parentElement._map) {
        this._attachedToMap();
    }
  }
  _attachedToMap() {
    // need the map to convert container points to LatLngs
    this._map = this.parentElement._map;
    var map = this.parentElement._map;

    // don't go through this if already done
    if (!this._feature) {
      // Scale this.coords if the this._map.poster exists because
      // the img might have been scaled by CSS.
      // compute the style properties to be applied to the feature
      var options = this._styleToPathOptions(window.getComputedStyle(this)),
          points = this.coords ? this._coordsToArray(this.coords): null;
      // scale points if the poster exists because responsive areas
      if (points && this.parentElement.poster) {
        var worig = this.parentElement.poster.width, 
            wresp = this.parentElement.width, 
            wadjstmnt = (worig - wresp)/2,
            horig = this.parentElement.poster.height, 
            hresp = this.parentElement.height, 
            hadjstmnt = (horig - hresp)/2;
        for (var i = 0; i< points.length;i++) {
          points[i][0] = points[i][0] - wadjstmnt;
          points[i][1] = points[i][1] - hadjstmnt;
        }
      }

      if (this.shape === 'circle') {
        var pixelRadius = parseInt(this.coords.split(",")[2]), 
            pointOnCirc = L.point(points[0]).add(L.point(0,pixelRadius)),
            latLngOnCirc = map.containerPointToLatLng(pointOnCirc),
            latLngCenter = map.containerPointToLatLng(points[0]),
            radiusInMeters = map.distance(latLngCenter, latLngOnCirc);
        this._feature = L.circle(latLngCenter, radiusInMeters, options).addTo(map);
      } else if (!this.shape || this.shape === 'rect') {
        var bounds = L.latLngBounds(map.containerPointToLatLng(points[0]), map.containerPointToLatLng(points[1]));
        this._feature = L.rectangle(bounds, options).addTo(map);
      } else if (this.shape === 'poly') {
        this._feature = L.polygon(this._pointsToLatLngs(points),options).addTo(map);
      } else {
        // whole initial area of map is a hyperlink
        this._feature = L.rectangle(map.getBounds(),options).addTo(map);
      }
      if (this.alt) {
        // other Leaflet features are implemented via SVG.  SVG displays tooltips
        // based on the <svg:title> graphics child element.
        var title = L.SVG.create('title'),
            titleText = document.createTextNode(this.alt);
            title.appendChild(titleText);
            this._feature._path.appendChild(title);
      }
      if (this.href) {
        // conditionally act on click on an area link.  If no link it should be an
        // inert area, but Leaflet doesn't quite support this.  For a full
        // implementation, we could actually use an image map replete with area
        // children which would provide the linking / cursor change behaviours
        // that are familiar to HTML authors versed in image maps.
        this._feature.on('click', function() {if (this.href) {window.open(this.href);}}, this);
      }
    }
  }  
  disconnectedCallback() {
    this._map.removeLayer(this._feature);
    delete this._feature;
  }
  _coordsToArray(containerPoints) {
    // returns an array of arrays of coordinate pairs coordsToArray("1,2,3,4") -> [[1,2],[3,4]]
    for (var i=1, points = [], coords = containerPoints.split(",");i<coords.length;i+=2) {
      points.push([parseInt(coords[i-1]),parseInt(coords[i])]);
    }
    return points;
  }
  _pointsToLatLngs(points) {
    // points should be an array of nested container coordinates [[x1,y1],[x2,y2](,[xN,yN])]
    var latLngArray = [];
    if (this._map) {
      for (var i=0,map = this._map;i<points.length;i++) {
        latLngArray.push(map.containerPointToLatLng(points[i]));
      }
    }
    return latLngArray;
  }
  _styleToPathOptions(style) {
    var options = {};
    if(style.stroke !== 'none') {
      options.stroke = true;
      options.color = style.stroke;
      options.opacity = style.strokeOpacity;
      options.weight = parseInt(style.strokeWidth);
      options.dashArray = style.strokeDasharray;
      options.lineCap = style.strokeLinecap;
      options.lineJoin = style.strokeLinejoin;
    } else {
      options.stroke = false;
    }
    if (style.fill !== 'none') {
      options.fill = true;
      options.fillColor = style.fill;
      options.fillOpacity = style.fillOpacity;
      options.fillRule = style.fillRule;
    } else {
      options.fill = false;
    }
    return options;
  }
}
