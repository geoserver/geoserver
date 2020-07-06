/*
License: GPL as per: http://www.gnu.org/copyleft/gpl.html
$Id: EditButtonBase.js,v 1.6 2005/02/06 09:29:50 camerons Exp $
*/

// Ensure this object's dependancies are loaded.
mapbuilder.loadScript(baseDir+"/widget/ButtonBase.js");

/**
 * Base class for tools which update GML by clicking on the mapPane.
 * @constructor
 * @base ButtonBase
 * @author Cameron Shorter cameronATshorter.net
 * @param button    Pointer to the button instance being created.
 * @param widetNode The node from the Config XML file.
 * @param model  The ButtonBar widget.
 */
function EditButtonBase(button,widetNode, model) {
  // Extend ButtonBase
  var base = new ButtonBase(button, widetNode, model);

  /** Empty GML to load when this tool is selected. */
  this.defaultModelUrl=widetNode.selectSingleNode("mb:defaultModelUrl").firstChild.nodeValue;

  /** Reference to GML node to update when a feature is added. */
  this.featureXpath=widetNode.selectSingleNode("mb:featureXpath").firstChild.nodeValue;

  /**
   * If tool is selected and the Edit Tool has changed (eg, changed from
   * LineEdit to PointEdit) then load new default feature.
   * This function is called when a tool is selected or deselected.
   * The following parameters are copied from this button's config node into
   * the target model:<br/>
   * "transactionResponseModel", "webServiceUrl", "featureXpath",
   * "defaultModelUrl", "targetContext".
   * These values will be used by WebServiceAction when processing a
   * transaction.
   * @param objRef Pointer to this object.
   * @param selected True when selected.
   */
  this.doSelect = function(selected,objRef) {
    if (objRef.enabled && selected && objRef.targetModel.url!=objRef.defaultModelUrl){
      a=new Array("transactionResponseModel","webServiceUrl","featureXpath","defaultModelUrl","targetContext");
      for (i in a){
        param=widetNode.selectSingleNode("mb:"+a[i]);
        if(param){
          objRef.targetModel.setParam(a[i],param.firstChild.nodeValue);
        }
      }

      objRef.targetModel.url=objRef.defaultModelUrl;
      // load default GML
      var httpPayload=new Object();
      httpPayload.url=objRef.defaultModelUrl;
      httpPayload.method="get";
      httpPayload.postData=null;
      objRef.targetModel.newRequest(objRef.targetModel,httpPayload);
    }
  }

  /**
   * Register for mouseup events, called after model loads.
   * @param objRef Pointer to this object.
   */
  this.setMouseListener = function(objRef) {
    if (objRef.mouseHandler) {
      objRef.mouseHandler.model.addListener('mouseup',objRef.doAction,objRef);
    }
  }

  // If this object is being created because a child is extending this object,
  // then child.properties = this.properties
  for (sProperty in this) {
    button[sProperty] = this[sProperty];
  }

  button.targetModel.addListener("loadModel",button.setMouseListener,button);
}
