/*
Author:       Cameron Shorter cameronATshorter.net
License:      GPL as per: http://www.gnu.org/copyleft/gpl.html

$Id: GmlRendererTest.js,v 1.3 2005/01/04 05:17:21 madair1 Exp $
*/

// Ensure this object's dependancies are loaded.
mapbuilder.loadScript(baseDir+"/widget/MapContainerBase.js");

/**
 * Render GML into HTML.  this.targetModel references the context model with
 * width/height attributes.
 * @constructor
 * @param widgetNode  The widget's XML object node from the configuration document.
 * @param model       The model object that this widget belongs to.
 */
function GmlRendererTest(widgetNode, model) {
  var base = new MapContainerBase(this,widgetNode,model)

  /**
   * Set up XSL params and convert Gml Coordinate nodes to Gml Coords so that they
   * are easier to process by XSL.
   * @param objRef Pointer to this object.
   */
  this.paint = function(objRef) {
    //var response = postLoad("/mapbuilder/writeXml", objRef.model.doc);
    var features = objRef.model.doc.selectNodes("//gml:featureMember");
    alert("pretending to paint:"+features.length+" features"+objRef.model.doc.xml);
  }
  //this.model.addListener("loadModel",this.paint,this);
  this.override=true;
}
