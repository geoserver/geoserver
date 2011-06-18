/*
Author:       Mike Adair mike.adairATccrs.nrcan.gc.ca
License: GPL as per: http://www.gnu.org/copyleft/gpl.html
$Id: Extent.js,v 1.12 2005/03/07 03:12:43 madair1 Exp $
*/


var Rearth = 6378137.0;                 // Radius of the earth (sphere); different from Proj value?
var degToMeter = Rearth*2*Math.PI/360;
var mbScaleFactor = 72 * 39.3701;   //PixelsPerInch*InchesPerMapUnit; magic numbers 
                                    //need to determine magic number for lat/lon

/**
 * A tool designed to handle geography calculations for widgets which render
 * the model in 2D.
 * Use of this tool requires that it's model implements get/setWindowHeight/Width
 * methods.
 * Encapsulates all geography and image size aspects of a geographic object 
 * displayed in a rectangular area on the screen.
 * All coordinates are handled as points which is a 2 element array, where x is 
 * the first element and y is the second. Coordinates are either pixel and lixel
 * (pl) relative to the top left of the extent or projection XY values (xy). 
 *
 * @constructor
 * @param model       the model document that this extent represents
 * @param initialRes  (optional) if supplied the extent resolution will be set to this value
 */
function Extent( model, initialRes ) {
  this.model = model;
  this.size = new Array();
  this.res = new Array();
  this.zoomBy = 4;
  this.id = model.id + "_MbExtent" + mbIds.getId();

  /**
   * Returns the XY center of this extent
   * @return  array of XY for th center of the extent
   */
  this.getCenter = function() {
    return new Array((this.ul[0]+this.lr[0])/2, (this.ul[1]+this.lr[1])/2);
  }

  /**
   * Returns XY coordinates for given pixel line coords w.r.t. top left corner
   * @param pl   pixel line in extent to calculate
   * @return     point array of XY coordinates
   */
  this.getXY = function(pl) {
    var x = this.ul[0]+pl[0]*this.res[0];
    var y = this.ul[1]- pl[1]*this.res[1];
    return new Array(x,y);
  }

  /**
   * Returns pixel/line coordinates for given XY projection coords
   * @param xy   projection XY coordinate to calculate
   * @return     point array of pxiel/line coordinates w.r.t. top left corner
   */
  this.getPL = function(xy) {
    var p = Math.floor( (xy[0]-this.ul[0])/this.res[0] );
    var l = Math.floor( (this.ul[1]-xy[1])/this.res[1] );
    return new Array(p,l);
  }

  /**
   * Adjust the extent so that it is centered at given XY coordinate with given
   * resolution.  Extent width and height remain fixed.  Optionally check to 
   * ensure that it doesn't go beyond available extent.
   *
   * @param center      projection XY coordinate to center at
   * @param newres      resolution to display at
   * @param limitExtent ensure that the extent doesn't go beyond available bbox (TBD: not complete/tested)
   * @return            none
   */
  this.centerAt = function(center, newres, limitExtent) {
    var half = new Array(this.size[0]/2, this.size[1]/2);
    this.lr = new Array(center[0]+half[0]*newres, center[1]-half[1]*newres);
    this.ul = new Array(center[0]-half[0]*newres, center[1]+half[1]*newres);

    //make sure the request doesn't extend beyond the available model
    //TBD this block not tested
    if ( limitExtent ) {
      var xShift = 0;
      if ( this.lr[0] > ContextExtent.lr[0] ) xShift = ContextExtent.lr[0] - this.lr[0];
      if ( this.ul[0] < ContextExtent.ul[0] ) xShift = ContextExtent.ul[0] - this.ul[0];
      this.lr[0] += xShift;
      this.ul[0] += xShift;

      var yShift = 0;
      if ( this.lr[1] < ContextExtent.lr[1] ) yShift = ContextExtent.lr[1] - this.lr[1];
      if ( this.ul[1] > ContextExtent.ul[1] ) yShift = ContextExtent.ul[1] - this.ul[1];
      this.lr[1] += yShift;
      this.ul[1] += yShift;
    }

    this.model.setBoundingBox( new Array(this.ul[0], this.lr[1], this.lr[0], this.ul[1]) );
    //this.setResolution(size);
    this.setSize(newres);
  }

  /**
   * Adjust the extent to the given bbox.  Resolution is recalculated. 
   * Extent width and height remain fixed.  
   * @param ul      upper left coordinate of bbox in XY projection coords
   * @param lr      lower right coordinate of bbox in XY projection coords
   */
  this.zoomToBox = function(ul, lr) {    //pass in xy
    var center = new Array((ul[0]+lr[0])/2, (ul[1]+lr[1])/2);
    newres = Math.max((lr[0] - ul[0])/this.size[0], (ul[1] - lr[1])/this.size[1]);
    this.centerAt( center, newres );
  } 

/**
   * Adjust the width and height to that bbox is displayed at specified resolution
   * @param res   the resolution to be set
   */
  //TBD update the model doc
  this.setSize = function(res) {     //pass in a resolution and width, height are recalculated
    this.res[0] = this.res[1] = res;
    this.size[0] = (this.lr[0] - this.ul[0])/this.res[0];
    this.size[1] = (this.ul[1] - this.lr[1])/this.res[1];
    this.width = Math.ceil(this.size[0]);
    this.height = Math.ceil(this.size[1]);
  }

  /**
   * Adjust the resolution so the bbox fits in the specified width and height
   * @param size   width, height array passed in
   */
  //TBD update the model doc
  this.setResolution = function(size) {    //pass in a width, height and res is recalculated
    this.size[0] = size[0];
    this.size[1] = size[1];
    this.res[0] = (this.lr[0] - this.ul[0])/this.size[0];
    this.res[1] = (this.ul[1] - this.lr[1])/this.size[1];
    this.width = Math.ceil(this.size[0]);
    this.height = Math.ceil(this.size[1]);
  }

  /**
   * Returns the map scale denominator for the current extent resolution
   * @return map scale denominator
   */
  this.getScale = function() {
    var pixRes = null;
    switch(this.model.getSRS()) {
      case "EPSG:4326":				//all projection codes in degrees
      case "EPSG:4269":				
        pixRes = this.res[0]*degToMeter;
        break;
      default:                //all projection codes in meters
        pixRes = this.res[0];
        break;
    }
    return mbScaleFactor*pixRes;
  }

  /**
   * Sets the model's resolution from mapScale input value.  The map center 
   * remains fixed.
   * @param scale   map scale denominator value
   */
  this.setScale = function(scale) {
    var newRes = null;
    switch(this.model.getSRS()) {
      case "EPSG:4326":				//all projection codes in degrees
      case "EPSG:4269":				
        //convert to resolution in degrees
        newRes = scale/(mbScaleFactor*degToMeter);
        break;
      default:                //all projection codes in meters
        newRes = scale/mbScaleFactor;
        break;
    }
    this.centerAt(this.getCenter(), newRes );
  }

  /**
   * Initialization of the Extent tool, called as a loadModel event listener.
   * @param extent      the object being initialized
   * @param initialRes  (optional) if supplied the extent resolution will be set to this value
   */
  this.init = function(extent, initialRes) {
    var bbox = extent.model.getBoundingBox();
    extent.ul = new Array(bbox[0],bbox[3]);
    extent.lr = new Array(bbox[2],bbox[1]);
    if ( initialRes ) {
      extent.setSize( initialRes );
    } else {
      extent.setResolution( new Array(extent.model.getWindowWidth(), extent.model.getWindowHeight() ) );
    }
  }
  if ( initialRes ) this.init(this, initialRes);
}

  
