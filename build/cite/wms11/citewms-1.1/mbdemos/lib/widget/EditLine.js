/*
License: GPL as per: http://www.gnu.org/copyleft/gpl.html
$Id: EditLine.js,v 1.5 2004/12/22 23:06:30 camerons Exp $
*/

// Ensure this object's dependancies are loaded.
mapbuilder.loadScript(baseDir+"/widget/EditButtonBase.js");

/**
 * When this button is selected, clicks on the MapPane will add a
 * new point to a line.
 * @constructor
 * @base EditButtonBase
 * @author Cameron Shorter cameronATshorter.net
 * @param widgetNode The from the Config XML file.
 * @param model  The ButtonBar widget.
 */
function EditLine(widgetNode, model) {
  // Extend EditButtonBase
  var base = new EditButtonBase(this, widgetNode, model);

  /**
   * Append a point to a line.
   * @param objRef      Pointer to this object.
   * @param targetNode  The node for the enclosing HTML tag for this widget.
   */
  this.doAction = function(objRef,targetNode) {
    if (objRef.enabled) {
      point=objRef.mouseHandler.model.extent.getXY(targetNode.evpl);
      old=objRef.targetModel.getXpathValue(
        objRef.targetModel,
        objRef.featureXpath);
      if(!old){old=""};
      sucess=objRef.targetModel.setXpathValue(
        objRef.targetModel,
        objRef.featureXpath,
        old+" "+point[0]+","+point[1]);
      if(!sucess){
        alert("EditLine: invalid featureXpath in config: "+objRef.featureXpath);
      }
    }
  }
}
