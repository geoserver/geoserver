/*
License: GPL as per: http://www.gnu.org/copyleft/gpl.html
$Id: FeatureList.js,v 1.16 2005/02/06 09:29:50 camerons Exp $
*/

// Ensure this object's dependancies are loaded.
mapbuilder.loadScript(baseDir+"/widget/WidgetBase.js");
mapbuilder.loadScript(baseDir+"/tool/WebServiceAction.js");

/**
 * Functions to render and update a FeatureList from GML.
 * @constructor
 * @base WidgetBase
 * @requires WebServiceAction
 * @author Cameron Shorter
 * @param widgetNode  The widget's XML object node from the configuration document.
 * @param model       The model object that this widget belongs to.
 */
function FeatureList(widgetNode, model) {
  var base = new WidgetBase(this, widgetNode, model);

  /** Tool which processes form key presses. */
  this.webServiceAction=new WebServiceAction();

  /**
   * Call WebServiceAction.processAction() which processes a button press.
   * Note, when EditButton is selected, EditButton set params in the model
   * which are required by WebServiceAction.
   * @param objRef Reference to this object.
   * @param button Button name.
   */
  this.processButton=function(objRef,button){
    objRef.webServiceAction.processAction(
      objRef.webServiceAction,objRef.model,button);
  }
}
