/*
License: GPL as per: http://www.gnu.org/copyleft/gpl.html
$Id: ZoomIn.js,v 1.3 2005/01/04 05:17:22 madair1 Exp $
*/

// Ensure this object's dependancies are loaded.
mapbuilder.loadScript(baseDir+"/widget/ButtonBase.js");

/**
 * When this button is selected, clicks on the MapPane trigger a zoomIn to the 
 * currently set AOI.
 * @constructor
 * @base ButtonBase
 * @author Mike Adair mike.adairATccrs.nrcan.gc.ca
 * @param toolNode      The tool node from the Config XML file.
 * @param model  The ButtonBar widget.
 */
function ZoomIn(toolNode, model) {
  // Extend ButtonBase
  var base = new ButtonBase(this, toolNode, model);

  this.zoomBy = 4;//TBD: get this from config

  /**
   * Calls the model's ceter at method to zoom in.  If the AOI is a single point,
   * it zooms in by the zoomBy factor.
   * @param objRef      Pointer to this object.
   * @param targetNode  The node for the enclosing HTML tag for this widget.
   */
  this.doAction = function(objRef,targetNode) {
    if (objRef.enabled) {
      var bbox = objRef.targetModel.getParam("aoi");
      if ( bbox!=null) {
        var extent = objRef.targetModel.extent;
        var ul = bbox[0];
        var lr = bbox[1];
        if ( ( ul[0]==lr[0] ) && ( ul[1]==lr[1] ) ) {
          extent.centerAt( ul, extent.res[0]/objRef.zoomBy );
        } else {
          extent.zoomToBox( ul, lr );
        }
      }
    }
  }

  /**
   * Register for mouseUp events.
   */
  this.setMouseListener = function(toolRef) {
    if (toolRef.mouseHandler) {
      toolRef.mouseHandler.model.addListener('mouseup',toolRef.doAction,toolRef);
    }
  }
  config.addListener( "loadModel", this.setMouseListener, this );

}

