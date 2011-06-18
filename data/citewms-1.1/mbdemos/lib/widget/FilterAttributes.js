/*
License: GPL as per: http://www.gnu.org/copyleft/gpl.html
$Id: FilterAttributes.js,v 1.1 2004/11/01 03:24:25 madair1 Exp $
*/

// Ensure this object's dependancies are loaded.
mapbuilder.loadScript(baseDir+"/widget/WidgetBase.js");

/**
 * This generic widget requires the config file to specify a <stylesheet>.
 * @constructor
 * @base WidgetBase
 * @author Cameron Shorter cameronATshorter.net
 * @param widgetNode  The widget's XML object node from the configuration document.
 * @param model       The model object that this widget belongs to.
 */
function xxFilterAttributes(widgetNode, model) {
  var base = new WidgetBase(this, widgetNode, model);


}
