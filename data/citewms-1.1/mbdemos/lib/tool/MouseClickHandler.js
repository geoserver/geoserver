/*
Author:       Cameron Shorter cameronATshorter.net
License:      GPL as per: http://www.gnu.org/copyleft/gpl.html

$Id: MouseClickHandler.js,v 1.6 2005/03/07 03:12:43 madair1 Exp $
*/

// Ensure this object's dependancies are loaded.
mapbuilder.loadScript(baseDir+"/tool/ToolBase.js");

/**
 * Tool which processes mouse clicks on a widget.
 * The tool must be enabled before use by calling tool.enable(true);
 * This tool registers mouse event listeners on the parent widget.
 * This tool works entirely in pixel/line coordinate space and knows nothing
 * about geography.
 * @constructor
 * @base ToolBase
 * @param toolNode  The node for this tool from the configuration document.
 * @param model     The model object that contains this tool
 */
function MouseClickHandler(toolNode, model) {
  var base = new ToolBase(this, toolNode, model);

  /**
   * Process a mouse click action.
   * @param objRef      Pointer to this MouseClickHandler object.
   * @param targetNode  The node for the enclosing HTML tag for this widget.
   */
  this.clickHandler = function(objRef,targetNode) {
    //TBD: or convert to map XY here?
    objRef.model.setParam("clickPoint", targetNode.evpl);
    //alert("MouseClickHandler click point="+targetNode.evpl);
  }

  model.addListener('mouseup',this.clickHandler,this);
}
