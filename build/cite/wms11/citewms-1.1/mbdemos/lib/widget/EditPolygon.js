/*
License: GPL as per: http://www.gnu.org/copyleft/gpl.html
$Id: EditPolygon.js,v 1.2 2004/12/22 23:06:30 camerons Exp $
*/

// Ensure this object's dependancies are loaded.
mapbuilder.loadScript(baseDir+"/widget/EditButtonBase.js");

/**
 * When this button is selected, clicks on the MapPane will add a
 * new point to a polygon.
 * @constructor
 * @base EditButtonBase
 * @author Cameron Shorter cameronATshorter.net
 * @param widetNode The node from the Config XML file.
 * @param model  The ButtonBar widget.
 */
function EditPolygon(widetNode, model) {
  // Extend EditButtonBase
  var base = new EditButtonBase(this, widetNode, model);

  /**
   * Append a point to a polygon.
   * @param objRef      Pointer to this object.
   * @param targetNode  The node for the enclosing HTML tag for this widget.
   */
  this.doAction = function(objRef,targetNode) {
    if (objRef.enabled) {
      point=objRef.mouseHandler.model.extent.getXY(targetNode.evpl);
      old=objRef.targetModel.getXpathValue(
        objRef.targetModel,
        objRef.featureXpath);
      if(!old){old=""}
      sucess=objRef.targetModel.setXpathValue(
        objRef.targetModel,
        objRef.featureXpath,
        old+" "+point[0]+","+point[1]);
      if(!sucess){
        alert("EditPolygon: invalid featureXpath in config: "+objRef.featureXpath);
      }
    }
  }
}
