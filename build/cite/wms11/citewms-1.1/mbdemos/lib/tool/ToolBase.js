/*
License: GPL as per: http://www.gnu.org/copyleft/gpl.html
$Id: ToolBase.js,v 1.24 2005/03/07 03:12:43 madair1 Exp $
*/

/**
 * Base Tool object that all Tools extend.
 * @constructor
 * @author Mike Adair mike.adairATccrs.nrcan.gc.ca
 * @param tool     Pointer to the tool instance being created
 * @param toolNode The tool node from the Config XML file.
 * @param model    The widget object which created this tool.
 */
function ToolBase(tool, toolNode, model) {
  tool.model = model;
  tool.toolNode = toolNode;

  //set the ID for this tool
  var id = toolNode.selectSingleNode("@id");
  if (id) {
    tool.id = id.firstChild.nodeValue;
  } else {
    tool.id = "MbTool_" + mbIds.getId();
  }

  /**
   * Initialize the targetModel property to point to the object.  This happens
   * as an init listener to ensure that the referenced model has been created.
   * @param toolRef Pointer to this object.
   */
  this.initTargetModel = function(toolRef) {
    /** The model this tool will update. */
    var targetModel = toolRef.toolNode.selectSingleNode("mb:targetModel");
    if (targetModel) {
      var targetModelName = targetModel.firstChild.nodeValue;
      toolRef.targetModel = eval("config.objects."+targetModelName);
      if (!toolRef.targetModel) alert("error finding targetModel:"+targetModelName+" for tool:"+toolRef.id);
    } else {
      toolRef.targetModel = toolRef.model;
    }
  }
  tool.initTargetModel = this.initTargetModel;
  tool.model.addListener( "init", tool.initTargetModel, tool );

  /**
   * Initialize the mouseHandler property to point to the object.  This happens
   * as an init listener to ensure that the referenced model has been created.
   * @param toolRef Pointer to this object.
   */
  this.initMouseHandler = function(toolRef) {
    /** Mouse handler which this tool will register listeners with. */
    var mouseHandler = toolRef.toolNode.selectSingleNode("mb:mouseHandler");
    if (mouseHandler) {
      toolRef.mouseHandler = eval("config.objects." + mouseHandler.firstChild.nodeValue);
      if (!toolRef.mouseHandler) {
        alert("error finding mouseHandler:"+mouseHandler.firstChild.nodeValue+" for tool:"+toolRef.id);
      }
    }
  }
  tool.initMouseHandler = this.initMouseHandler;
  tool.model.addListener( "init", tool.initMouseHandler, tool );

  //tools enabled by default; can set to false in config for initial loading
  tool.enabled = true;
  var enabled = toolNode.selectSingleNode("mb:enabled");
  if (enabled) tool.enabled = eval(enabled.firstChild.nodeValue);
}
