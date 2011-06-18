/*
Author:       Mike Adair mike.adairATccrs.nrcan.gc.ca
License:      GPL as per: http://www.gnu.org/copyleft/gpl.html

$Id: ModelStatus.js,v 1.1 2005/01/10 03:07:46 madair1 Exp $
*/

// Ensure this object's dependancies are loaded.
mapbuilder.loadScript(baseDir+"/widget/WidgetBase.js");

/**
 * Widget to display the a form to input any model's URL and load the new URL 
 * as the model's document
 *
 * @constructor
 * @base WidgetBase
 * @param widgetNode  This widget's object node from the configuration document.
 * @param model       The model that this widget is a view of.
 */

function ModelStatus(widgetNode, model) {
  var base = new WidgetBase(this, widgetNode, model);

  /**
   * initializes stylesheet parameters for the widget
   * @param objRef Pointer to this widget object.
   */
  this.prePaint = function(objRef) {
    objRef.stylesheet.setParameter("statusMessage", objRef.targetModel.getParam("modelStatus"));
  }

  /**
   * Refreshes the form and event handlers when this widget is painted.
   * @param objRef Pointer to this CurorTrack object.
   */
  this.showStatus = function(objRef) {
    objRef.paint(objRef);
  }
  this.model.addListener("modelStatus",this.showStatus,this);
}

