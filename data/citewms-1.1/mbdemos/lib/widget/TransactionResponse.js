/*
Author:       Cameron Shorter
License:      GPL as per: http://www.gnu.org/copyleft/gpl.html

$Id: TransactionResponse.js,v 1.3 2005/02/05 03:12:26 camerons Exp $
*/

// Ensure this object's dependancies are loaded.
mapbuilder.loadScript(baseDir+"/widget/WidgetBase.js");

/**
 * Render the response from a WFS request.
 * @constructor
 * @base WidgetBase
 * @author Cameron Shorter
 * @param widgetNode  The widget's XML object node from the configuration document.
 * @param model       The model object that this widget belongs to.
 */
function TransactionResponse(widgetNode, model) {
  var base = new WidgetBase(this,widgetNode,model);
}
