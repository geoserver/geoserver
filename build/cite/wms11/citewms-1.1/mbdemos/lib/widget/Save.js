/*
License: GPL as per: http://www.gnu.org/copyleft/gpl.html
$Id: Save.js,v 1.1 2004/12/15 04:48:41 madair1 Exp $
*/

// Ensure this object's dependancies are loaded.
mapbuilder.loadScript(baseDir+"/widget/ButtonBase.js");

/**
 * When this button is pressed the targetModel is posted to the serializeUrl.
 * Also defines a listener function for the "modelSaved" event which opens 
 * the serialized document in a new browser window.  This listener is only
 * registered if the a popupWindowName is defined for the button in config.
 *
 * @constructor
 * @base ButtonBase
 * @author Mike Adair mike.adairATccrs.nrcan.gc.ca
 * @param toolNode      The tool node from the Config XML file.
 * @param model  The ButtonBar widget.
 */
function Save(toolNode, model) {
  var base = new ButtonBase(this, toolNode, model);

  /**
   * Calls the reset() method of the context doc to reload at with the original extent
   * @param objRef      Pointer to this AoiMouseHandler object.
   */
  this.doSelect = function(selected,objRef) {
    if (selected) {
      objRef.targetModel.saveModel(objRef.targetModel);
    }
  }

  /**
   * opens a saved model in a new window
   * @param objRef Pointer to this SaveModel object.
   */
  this.savedModelPopup = function(objRef, fileUrl) {
    window.open(fileUrl, this.popupWindowName);
  }
  var popupWindowName = toolNode.selectSingleNode("mb:popupWindowName");
  if (popupWindowName) {
    this.popupWindowName = popupWindowName.firstChild.nodeValue;
    this.targetModel.addListener("modelSaved", this.savedModelPopup, this);
  }


}


