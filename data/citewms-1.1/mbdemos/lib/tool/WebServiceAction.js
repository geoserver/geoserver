/*
License: GPL as per: http://www.gnu.org/copyleft/gpl.html
$Id: WebServiceAction.js,v 1.3 2005/02/10 19:46:42 camerons Exp $
*/

/**
 * Tool used to initiate a WFS Transaction.
 * @constructor
 * @author Cameron Shorter
 */
function WebServiceAction(){
  /** Url of WFS */
  this.webServiceUrl=null;//widgetNode.selectSingleNode("mb:webServiceUrl").firstChild.nodeValue;

  /** Xsl to convert Feature into a WFS Transaction Insert. */
  this.insertXsl=new XslProcessor(baseDir+"/tool/xsl/wfs_Insert.xsl");

  /**
   * Process an action - usually initiated by a button press.
   * @param objRef Reference to this object.
   * @param model The model which contains the FeatureCollection to send to
   * the WFS as well as other state data.
   * @param action The action to perform, stored as a string, usually a
   * button name.  Button can be:<br/>
   * "Reset" - reset FeatureCollection to the default.<br/>
   * "Insert Feature" - Add a feature to a WFS-T.<br/>
   * "Update Feature" - Update an existing WFS-T feature.<br/>
   */
  this.processAction=function(objRef,model,action){
    httpPayload=new Object();
    param=model.getParam("transactionResponseModel");
    transactionResponseModel=eval("config.objects."+param);
    param=model.getParam("targetContext");
    targetContext=eval("config.objects."+param);

    switch(action){
      case "Reset":
        if(model.url){
          httpPayload.url=model.url;
          httpPayload.method="get";
          model.newRequest(model,httpPayload);
          break;
        }
      case "Insert Feature":
        s=objRef.insertXsl.transformNodeToObject(model.doc);
        httpPayload.postData=s;
        //httpPayload.url=objRef.webServiceUrl;
        httpPayload.url=model.getParam("webServiceUrl");
        httpPayload.method="post";
        transactionResponseModel.newRequest(transactionResponseModel,httpPayload);
        sucess=transactionResponseModel.doc.selectSingleNode("//wfs:TransactionResult/wfs:Status/wfs:SUCCESS");
        if (sucess){
          // Remove FeatureList if feature entry was successful.
          httpPayload.url=null;
          model.newRequest(model,httpPayload);
          // Repaint the WMS layers
          targetContext.callListeners("refresh");
        }
        break;
      case "Update Feature":
        alert("WebServiceAction: Update Feature not implemented");
        break;
      default:
        alert("WebServiceAction: Unknown action: "+action);
    }
  }
}
