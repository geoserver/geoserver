/*
Author:       Cameron Shorter cameronAtshorter.net
License:      GPL as per: http://www.gnu.org/copyleft/gpl.html

$Id: TimeSeries.js,v 1.3 2005/02/21 05:10:57 madair1 Exp $
*/

// Ensure this object's dependancies are loaded.
mapbuilder.loadScript(baseDir+"/widget/MapContainerBase.js");

/**
 * Widget to render a map from an OGC context document.
 * @constructor
 * @base MapContainerBase
 * @param widgetNode  The widget's XML object node from the configuration document.
 * @param model       The model object that this widget belongs to.
 */
function TimeSeries(widgetNode, model) {
  var base = new MapContainerBase(this,widgetNode,model);

  /**
   * Called when the context's hidden attribute changes.
   * @param layerName The Name of the LayerList/Layer from the Context which
   * has changed.
   * @param objRef This object.
   * @param layerName  The name of the layer that was toggled.
   */
  this.hiddenListener=function(objRef, layerName){
    var vis="visible";
    if(objRef.model.getHidden(layerName)=="1") {
      vis="hidden";
    }
    var layerId = objRef.model.id + "_" + objRef.id + "_" + layerName;
    var timestamp = objRef.model.getCurrentTimestamp(layerName);
    if (timestamp) layerId += "_" + timestamp;
    var layer = document.getElementById(layerId);
    if (layer) {
      layer.style.visibility=vis;
    } else {
      alert("error finding layerId:"+layerId);
    }
  }
  this.model.addListener("hidden",this.hiddenListener,this);

  /**
   * Called when the map timestamp is changed
   * @param layerName The Name of the LayerList/Layer from the Context which
   * has changed.
   * @param objRef This object.
   * @param layerName  The name of the layer that was toggled.
   */
  this.timestampListener=function(objRef, timestampIndex){
    var layerName = objRef.model.timestampList.getAttribute("layerName");
    var timestamp = objRef.model.timestampList.childNodes[timestampIndex];
    var vis = (timestamp.getAttribute("current")=="1") ? "visible":"hidden";
    var layerId = objRef.model.id + "_" + objRef.id + "_" + layerName + "_" + timestamp.firstChild.nodeValue;
    var layer = document.getElementById(layerId);
    if (layer) {
      layer.style.visibility=vis;
    } else {
      alert("error finding layerId:"+layerId);
    }
  }
  this.model.addListener("timestamp",this.timestampListener,this);

  this.prePaint=function(objRef) {
    var timelist = "";
    var timestampList = objRef.model.timestampList;
    if (timestampList) {
      for (var i=timestampList.firstFrame; i<=timestampList.lastFrame; ++i) {
        timelist += timestampList.childNodes[i].firstChild.nodeValue + ",";
      }
      objRef.stylesheet.setParameter("timeList", timelist.substring(0,timelist.length-1));  //remove trailing comma
    }
  }
}
