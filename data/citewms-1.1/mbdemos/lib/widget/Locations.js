/*
Author:       Tom Kralidis
License:      GPL as per: http://www.gnu.org/copyleft/gpl.html

$Id: Locations.js,v 1.16 2005/01/04 05:17:22 madair1 Exp $
*/

// Ensure this object's dependancies are loaded.
mapbuilder.loadScript(baseDir+"/widget/WidgetBase.js");

/**
 * Widget to display predefined locations.
 * @constructor
 * @base WidgetBase
 * @param widgetNode This widget's object node from the configuration document.
 * @param model The model that this widget is a view of.
 */


function Locations(widgetNode, model) {
  var base = new WidgetBase(this, widgetNode, model);

  //TBD: set this from the setAoi function?
  this.model.getSRS = function(){return "EPSG:4326";}

  /**
   * Change the AOI coordinates from select box choice of prefined locations
   * @param bbox the bbox value of the location keyword chosen
   */

  this.setAoi = function(bbox, targetModel) {
    var bboxArray = new Array();
    bboxArray     = bbox.split(",");
    var ul = new Array(parseFloat(bboxArray[0]),parseFloat(bboxArray[3]));
    var lr = new Array(parseFloat(bboxArray[2]),parseFloat(bboxArray[1]));
    this.model.setParam("aoi",new Array(ul,lr));

    //convert this.model XY to latlong
    //convert latlong to targetmodel XY
    //extent.setAoi takes XY as input
    this.targetModel.setParam("aoi", new Array(ul,lr));
    this.targetModel.setParam("mouseup",this);
  }
}
