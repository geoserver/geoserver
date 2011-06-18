/*
Author:       Cameron Shorter cameronAtshorter.net
License:      GPL as per: http://www.gnu.org/copyleft/gpl.html

$Id: MapPane.js,v 1.74 2005/01/12 21:36:59 madair1 Exp $
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
function MapPane(widgetNode, model) {
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
    var layer = document.getElementById(layerId);
    if (layer) layer.style.visibility=vis;
  }
  this.model.addListener("hidden",this.hiddenListener,this);

}
