/*
Author:       Mike Adair mike.adairATccrs.nrcan.gc.ca
License:      GPL as per: http://www.gnu.org/copyleft/gpl.html
$Id: Proj.js,v 1.7 2005/03/06 04:14:51 madair1 Exp $
*/

/**
 * Provides latitude/longitude to map projection (and vice versa) transformation methods. 
 * Initialized with EPSG codes.  Has properties for units and title strings.
 * All coordinates are handled as points which is a 2 element array where x is 
 * the first element and y is the second. 
 * For the Forward() method pass in lat/lon and it returns map XY.
 * For the Inverse() method pass in map XY and it returns lat/long.
 *
 * TBD: retrieve initialization params (and conversion code?) from a web service 
 *
 * @constructor
 *
 * @param srs         The SRS (EPSG code) of the projection
 */

function Proj(srs) {
  this.srs = srs.toUpperCase();
  switch(this.srs) {
    case "EPSG:4326":       //lat/lon projection WGS_84
    case "EPSG:4269":       //lat/lon projection WGS_84
    case "CRS:84":          //lat/lon projection WGS_84
      this.Forward = identity;
      this.Inverse = identity;
      this.units = "degrees";
      this.title = "Lat/Long";
      break;
    case "EPSG:42101":        //North American LCC WGS_84
      this.Init = lccinit;
      this.Forward = ll2lcc;
      this.Inverse = lcc2ll;
      this.Init(new Array(6378137.0,6356752.314245,49.0,77.0,-95.0, 0.0, 0.0, -8000000));
      this.units = "meters";
      this.title = "Lambert Conformal Conic";
      break;
    case "EPSG:42304":        //North American LCC NAD_83
      this.Init = lccinit;
      this.Forward = ll2lcc;
      this.Inverse = lcc2ll;
      this.Init(new Array(6378137.0,6356752.314,49.0,77.0,-95.0, 49.0, 0.0, 0));
      this.units = "meters";
      this.title = "Lambert Conformal Conic";
      break;
    case "EPSG:26986":        //NAD83 / Massachusetts Mainland
      this.Init = lccinit;
      this.Forward = ll2lcc;
      this.Inverse = lcc2ll;
      this.Init(new Array(6378137.0,6356752.314,42.68333333333333,41.71666666666667,-71.5, 41.0, 200000, 750000));
      this.units = "meters";
      this.title = "Massachusetts Mainland (LCC)";
      break;
    case "EPSG:32761":        //Polar Stereographic
    case "EPSG:32661":
      this.Init = psinit;
      this.Forward = ll2ps;
      this.Inverse = ps2ll;
      this.Init(new Array(6378137.0, 6356752.314245, 0.0, -90.0, 2000000, 2000000));
      this.units = "meters";
      this.title = "Polar Stereographic";
      break;
    case "SCENE":             //this is really a pixel projection with bilinear interpolation of the corners to get ll
      this.Init = sceneInit;
      this.Forward = ll2scene;
      this.Inverse = scene2ll;
      this.GetXYCoords = identity;  //override to get line 0 at top left
      this.GetPLCoords = identity; //
      break;
    case "PIXEL":
      this.Forward = ll2pixel;
      this.Inverse = pixel2ll;
      this.units = "pixels";
      this.GetXYCoords = identity;  //override to get line 0 at top left
      this.GetPLCoords = identity; //
      break;
    default:
      //or retrieve parameters from web service based on SRS lookup
      alert("unsupported map projection: "+this.srs);
  }

  this.matchSrs = function(otherSrs) {
    if (this.srs == otherSrs.toUpperCase() ) return true;
    return false;
  }

}

function identity(coords) {
  return coords;
}

/**
 * Scene projection forward transformation.
 * Forward trasnformation need reverse bilinear interpolation or orbit modelling 
 * (to be implemented)
 * @param coords  Lat/Long coords passed in
 * @return map coordinates
 */
function ll2scene(coords) {
  alert("ll2scene not defined");
  //return new Array(124, 15+256);  //for testing only, 
  return null;
}

/**
 * Scene projection Inverse transformation.
 * This is really a pixel representation with bi-linear interpolation of the corner coords.
 * @param coords  map coordinates passed in
 * @return Lat/Long coords 
 */
function scene2ll(coords) {
  var xpct = (coords[0]-this.ul[0])/(this.lr[0]-this.ul[0]);
  var ypct = (coords[1]-this.ul[1])/(this.lr[1]-this.ul[1]);
//  alert("pct:"+xpct+":"+ypct);
  var lon = bilinterp(xpct, ypct, this.cul[0], this.cur[0], this.cll[0], this.clr[0])
  var lat = bilinterp(xpct, ypct, this.cul[1], this.cur[1], this.cll[1], this.clr[1])
  return new Array(lon, lat);
}

/**
 * Scene projection initialization function
 * @param param  array of the corner coordinates (which are in turn 2D point arrays)
 * in the order upper-left, upper-right, lower-left, lower-right
 */
function sceneInit(param) {
  this.cul = param[0];
  this.cur = param[1];
  this.cll = param[2];
  this.clr = param[3];
}

/**
 * Bilinear interpolation function to return an interpolated value for either
 * x or y for some point located within a box where the XY of the corners are known.
 * This should be applied to the x and y coordinates separately, 
 * ie. first interpolate for the x value, then the y.
 * the a,b,c,d params are thus the x or y values at the corners.
 * @param x distance from the left side of the box as a percentage
 * @param y distance from the top of the box as a percentage
 * @param a either x or y value at the UL corner of the box
 * @param b either x or y value at the UR corner of the box
 * @param c either x or y value at the LL corner of the box
 * @param d either x or y value at the LR corner of the box
 */
function bilinterp(x, y, a, b, c, d) {
  var top = x*(b-a) + a;
  var bot = x*(d-c) + c;
//alert("top:"+top+"  bot:"+bot);
  return y*(bot-top) + top;
}

// pixel projection object definition
// a pixel representation 
// forward transformation
function ll2pixel(coords) {
  alert("ll2pixel not defined");
  return null;
}

// inverse transformation
function pixel2ll(coords) {
  alert("pixel2ll not defined");
//  return new Array(lon, lat);
  return null;
}


/****************************************************************************
The following code is a direct port of PROJ4 coordinate transformation code
from C to Javascript.  For more information go to http://proj.maptools.org/
Currently suppported projections include: Lambert Conformal Conic (LCC), 
Lat/Long, Polar Stereographic.
Porting C to Javascript is fairly straightforward so other support for more 
projections is easy to add.
*/

var PI = Math.PI;
var HALF_PI = PI*0.5;
var TWO_PI = PI*2.0;
var EPSLN = 1.0e-10;
var R2D = 57.2957795131;
var D2R =0.0174532925199;
var R = 6370997.0;        // Radius of the earth (sphere)


// Initialize the Lambert Conformal conic projection
// -----------------------------------------------------------------
function lccinit(param) {
// array of:  r_maj,r_min,lat1,lat2,c_lon,c_lat,false_east,false_north
//double c_lat;                   /* center latitude                      */
//double c_lon;                   /* center longitude                     */
//double lat1;                    /* first standard parallel              */
//double lat2;                    /* second standard parallel             */
//double r_maj;                   /* major axis                           */
//double r_min;                   /* minor axis                           */
//double false_east;              /* x offset in meters                   */
//double false_north;             /* y offset in meters                   */

  this.r_major = param[0];
  this.r_minor = param[1];
  var lat1 = param[2] * D2R;
  var lat2 = param[3] * D2R;
  this.center_lon = param[4] * D2R;
  this.center_lat = param[5] * D2R;
  this.false_easting = param[6];
  this.false_northing = param[7];

// Standard Parallels cannot be equal and on opposite sides of the equator
  if (Math.abs(lat1+lat2) < EPSLN) {
    alert("Equal Latitiudes for St. Parallels on opposite sides of equator - lccinit");
    return;
  }

  var temp = this.r_minor / this.r_major;
  this.e = Math.sqrt(1.0 - temp*temp);

  var sin1 = Math.sin(lat1);
  var cos1 = Math.cos(lat1);
  var ms1 = msfnz(this.e, sin1, cos1);
  var ts1 = tsfnz(this.e, lat1, sin1);
  
  var sin2 = Math.sin(lat2);
  var cos2 = Math.cos(lat2);
  var ms2 = msfnz(this.e, sin2, cos2);
  var ts2 = tsfnz(this.e, lat2, sin2);
  
  var ts0 = tsfnz(this.e, this.center_lat, Math.sin(this.center_lat));

  if (Math.abs(lat1 - lat2) > EPSLN) {
    this.ns = Math.log(ms1/ms2)/Math.log(ts1/ts2);
  } else {
    this.ns = sin1;
  }
  this.f0 = ms1 / (this.ns * Math.pow(ts1, this.ns));
  this.rh = this.r_major * this.f0 * Math.pow(ts0, this.ns);
}


// Lambert Conformal conic forward equations--mapping lat,long to x,y
// -----------------------------------------------------------------
function ll2lcc(coords) {

  var lon = coords[0];
  var lat = coords[1];

// convert to radians
  if ( lat <= 90.0 && lat >= -90.0 && lon <= 180.0 && lon >= -180.0) {
    lat *= D2R;
    lon *= D2R;
  } else {
    alert("*** Input out of range ***: lon: "+lon+" - lat: "+lat);
    return null;
  }

  var con  = Math.abs( Math.abs(lat) - HALF_PI);
  var ts;
  if (con > EPSLN) {
    ts = tsfnz(this.e, lat, Math.sin(lat) );
    rh1 = this.r_major * this.f0 * Math.pow(ts, this.ns);
  } else {
    con = lat * this.ns;
    if (con <= 0) {
      alert("Point can not be projected - ll2lcc");
      return null;
    }
    rh1 = 0;
  }
  var theta = this.ns * adjust_lon(lon - this.center_lon);
  var x = rh1 * Math.sin(theta) + this.false_easting;
  var y = this.rh - rh1 * Math.cos(theta) + this.false_northing;

  return new Array(x,y);
}

// Lambert Conformal Conic inverse equations--mapping x,y to lat/long
// -----------------------------------------------------------------
function lcc2ll(coords) {

  var rh1, con, ts;
  var lat, lon;
  x = coords[0] - this.false_easting;
  y = this.rh - coords[1] + this.false_northing;
  if (this.ns > 0) {
    rh1 = Math.sqrt (x * x + y * y);
    con = 1.0;
  } else {
    rh1 = -Math.sqrt (x * x + y * y);
    con = -1.0;
  }
  var theta = 0.0;
  if (rh1 != 0) {
    theta = Math.atan2((con * x),(con * y));
  }
  if ((rh1 != 0) || (this.ns > 0.0)) {
    con = 1.0/this.ns;
    ts = Math.pow((rh1/(this.r_major * this.f0)), con);
    lat = phi2z(this.e, ts);
    if (lat == -9999) return null;
  } else {
    lat = -HALF_PI;
  }
  lon = adjust_lon(theta/this.ns + this.center_lon);
  return new Array(R2D*lon, R2D*lat);
}

// Function to compute the constant small m which is the radius of
//   a parallel of latitude, phi, divided by the semimajor axis.
// -----------------------------------------------------------------
function msfnz(eccent, sinphi, cosphi) {
      var con = eccent * sinphi;
      return cosphi/(Math.sqrt(1.0 - con * con));
}

// Function to compute the constant small t for use in the forward
//   computations in the Lambert Conformal Conic and the Polar
//   Stereographic projections.
// -----------------------------------------------------------------
function tsfnz(eccent, phi, sinphi) {
  var con = eccent * sinphi;
  var com = .5 * eccent; 
  con = Math.pow(((1.0 - con) / (1.0 + con)), com);
  return (Math.tan(.5 * (HALF_PI - phi))/con);
}


// Function to compute the latitude angle, phi2, for the inverse of the
//   Lambert Conformal Conic and Polar Stereographic projections.
// ----------------------------------------------------------------
function phi2z(eccent, ts) {
  var eccnth = .5 * eccent;
  var con, dphi;
  var phi = HALF_PI - 2 * Math.atan(ts);
  for (i = 0; i <= 15; i++) {
    con = eccent * Math.sin(phi);
    dphi = HALF_PI - 2 * Math.atan(ts *(Math.pow(((1.0 - con)/(1.0 + con)),eccnth))) - phi;
    phi += dphi; 
    if (Math.abs(dphi) <= .0000000001) return phi;
  }
  alert("Convergence error - phi2z");
  return -9999;
}

// Function to return the sign of an argument
function sign(x) { if (x < 0.0) return(-1); else return(1);}

// Function to adjust longitude to -180 to 180; input in radians
function adjust_lon(x) {x=(Math.abs(x)<PI)?x:(x-(sign(x)*TWO_PI));return(x);}


