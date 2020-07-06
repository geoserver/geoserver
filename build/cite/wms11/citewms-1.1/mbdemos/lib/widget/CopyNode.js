/*
Author:       Mike Adair  mike.adairATccrs.nrcan.gc.ca
License:      GPL as per: http://www.gnu.org/copyleft/gpl.html

$Id: CopyNode.js,v 1.2 2005/01/13 05:24:52 madair1 Exp $
*/

// Ensure this object's dependancies are loaded.
//mapbuilder.loadScript(baseDir+"/widget/WidgetBase.js");

/**
 * Widget which generates a WFS query from it's parent document
 * @constructor
 * @base WidgetBase
 * @param widgetNode  The widget's XML object node from the configuration document.
 * @param model       The model object that this widget belongs to.
 */
function CopyNode(widgetNode, model) {
  var base = new WidgetBase(this, widgetNode, model);

  /**
   * Render the widget.  Equivalent function to paint.
   * @param objRef Pointer to this object.
   */
  this.copyNode = function(objRef, feature) {
    objRef.stylesheet.setParameter("version", objRef.model.getVersion() );
    objRef.stylesheet.setParameter("serverUrl", objRef.model.getServerUrl("GetMap","get") );
    objRef.stylesheet.setParameter("serverTitle", objRef.model.getServerTitle() );
    objRef.stylesheet.setParameter("serviceName", "wms");//objRef.model.getServiceName() );
    var newNode = objRef.stylesheet.transformNodeToObject(feature);
    if (objRef.debug) alert(newNode.xml);
    var parentNode = objRef.targetModel.doc.selectSingleNode("/wmc:ViewContext/wmc:LayerList");
    parentNode.appendChild(objRef.targetModel.doc.importNode(newNode.documentElement,true));
    objRef.targetModel.callListeners("refresh");
  }
  this.model.addListener("AddNodeToContext", this.copyNode, this);

}
