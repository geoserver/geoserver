/*
License: GPL as per: http://www.gnu.org/copyleft/gpl.html
$Id: WmsCapabilitiesImport.js,v 1.6 2004/10/08 12:11:12 camerons Exp $
*/

// Ensure this object's dependancies are loaded.
mapbuilder.loadScript(baseDir+"/widget/WidgetBase.js");

/**
 * Build a WMC document from a WMS GetCapabilities response.
 * @constructor
 * @base WidgetBase
 * @author Cameron Shorter cameronATshorter.net
 * @param widgetNode  The widget's XML object node from the configuration document.
 * @param model       The model object that this widget belongs to.
 */

function WmsCapabilitiesImport(widgetNode, model) {
  var base = new WidgetBase(this, widgetNode, model);

  /**
   * Load Capabilities document when ENTER is pressed.
   * param e The window event.
   */
  this.onKeyPress=function(e) {
    var url;
    var keycode;
    if (e.which){
      //Mozilla
      keycode=e.which;
      url=e.currentTarget.value;
    }else{
      //IE
      keycode=window.event.keyCode;
      url=window.event.srcElement.value;
    }

    if (keycode == 13) {
      capabilities = Sarissa.getDomDocument();
      capabilities.async = false;
      capabilities.load(url);
      alert("capabilities="+capabilities.xml);

      xsl = Sarissa.getDomDocument();
      xsl.async = false;
      xsl.load(baseDir+"/widget/wms/WMSCapabilities2Context.xsl");
      alert("xsl="+xsl.xml);

      context=Sarissa.getDomDocument();
      capabilities.transformNodeToObject(xsl,context);
      alert("context="+context.xml);

      // Load the new Context Document
      this.model.loadModelNode(context);
    }
  }
}
