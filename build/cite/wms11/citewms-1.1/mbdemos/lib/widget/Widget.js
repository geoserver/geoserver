/*
License: GPL as per: http://www.gnu.org/copyleft/gpl.html
$Id: Widget.js,v 1.3 2004/10/08 12:11:12 camerons Exp $
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
function Widget(widgetNode, model) {
  var base = new WidgetBase(this, widgetNode, model);

}
