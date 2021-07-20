/*
License: GPL as per: http://www.gnu.org/copyleft/gpl.html
$Id: Reset.js,v 1.1 2004/12/15 04:48:41 madair1 Exp $
*/

// Ensure this object's dependancies are loaded.
mapbuilder.loadScript(baseDir+"/widget/ButtonBase.js");

/**
 * When this button is pressed the map will reload with it's original extent
 * @constructor
 * @base ButtonBase
 * @author Mike Adair mike.adairATccrs.nrcan.gc.ca
 * @param toolNode      The tool node from the Config XML file.
 * @param model  The ButtonBar widget.
 */
function Reset(toolNode, model) {
  var base = new ButtonBase(this, toolNode, model);

  /**
   * Store a copy of the original extent locally.
   * @param objRef Pointer to this object.
   */
  this.initExtent = function(objRef) {
    objRef.originalExtent = new Extent( objRef.targetModel );   
    objRef.originalExtent.init( objRef.originalExtent );
    objRef.originalExtent.setResolution( new Array(objRef.targetModel.getWindowWidth(), objRef.targetModel.getWindowHeight()) );
  }
  this.targetModel.addListener("loadModel",this.initExtent, this);

  /**
   * Calls the reset() method of the context doc to reload at with the original extent
   * @param objRef      Pointer to this AoiMouseHandler object.
   */
  this.doSelect = function(selected,objRef) {
    if (selected){
      var originalExtent = objRef.originalExtent;
      objRef.targetModel.extent.centerAt( originalExtent.getCenter(), originalExtent.res[0] );
    }
  }
}


