/*
Author:       Cameron Shorter cameronAtshorter.net
License:      GPL as per: http://www.gnu.org/copyleft/gpl.html

$Id: MapImage.js,v 1.1 2005/01/13 05:24:52 madair1 Exp $
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
function MapImage(widgetNode, model) {
  var base = new MapContainerBase(this,widgetNode,model);
  this.paintMethod = "image2html";

  this.prePaint = function(objRef) {
    objRef.model.doc.width = objRef.containerModel.getWindowWidth();
    objRef.model.doc.height = objRef.containerModel.getWindowHeight();
  }

}
