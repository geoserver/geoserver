/*
License: GPL as per: http://www.gnu.org/copyleft/gpl.html
$Id: SetAoi.js,v 1.2 2004/12/15 18:16:49 madair1 Exp $
*/

// Ensure this object's dependancies are loaded.
mapbuilder.loadScript(baseDir+"/widget/ButtonBase.js");

/**
 * When this button is selected, the AOI box stays visible and no zoom happens. 
 * @constructor
 * @base ButtonBase
 * @author Mike Adair mike.adairATccrs.nrcan.gc.ca
 * @param toolNode      The tool node from the Config XML file.
 * @param model  The ButtonBar widget.
 */
function SetAoi(toolNode, model) {
  // Extend ButtonBase
  var base = new ButtonBase(this, toolNode, model);

  /**
   * The action to do on click
   * @param objRef      Pointer to this object.
   * @param targetNode  The node for the enclosing HTML tag for this widget.
   */
  this.doAction = function(objRef,targetNode) {
    //does nothing for the moment
  }

  if (this.mouseHandler) {
    this.mouseHandler.model.addListener('mouseup',this.doAction,this);
  }

}

