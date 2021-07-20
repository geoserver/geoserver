/*
Author:       Mike Adair  mike.adairATccrs.nrcan.gc.ca
License:      GPL as per: http://www.gnu.org/copyleft/gpl.html

$Id: WebServiceRequest.js,v 1.11 2005/03/07 03:12:43 madair1 Exp $
*/

// Ensure this object's dependancies are loaded.
mapbuilder.loadScript(baseDir+"/tool/ToolBase.js");

/**
 * A controller issuing OGC web service requests.  The request is generated
 * by applying a stylesheet to a Layer/FeatureType/Coverage node from a 
 * capabilities document as a listener function.  The listener event name is 
 * a combination of the service type and the request name (e.g. wfs_GetFeature)
 * and the parameter passed to the listener is the featureName (Layer/FeatureType/Coverage).
 * The response from the request is stored in the targetModel.  If the 
 * targetModel is a template model (attribute template="true") the a new model 
 * is created and appended to the parentModel's <models> list.
 * @constructor
 * @param toolNode The tools's XML object node from the configuration document.
 * @param model    The model that this tool belongs to
 */
function WebServiceRequest(toolNode, model) {
  var base = new ToolBase(this, toolNode, model);
  
  //get the request name to add listener to
  var requestName = toolNode.selectSingleNode("mb:requestName");
  if (requestName) {
    this.requestName = requestName.firstChild.nodeValue;
  }

  var styleUrl = baseDir+"/tool/xsl/"+this.requestName.replace(/:/,"_")+".xsl";
  this.stylesheet = new XslProcessor(styleUrl);

  // Set stylesheet parameters for all the child nodes from the config file
  for (var j=0;j<toolNode.childNodes.length;j++) {
    if (toolNode.childNodes[j].firstChild && toolNode.childNodes[j].firstChild.nodeValue) {
      this.stylesheet.setParameter(toolNode.childNodes[j].nodeName,toolNode.childNodes[j].firstChild.nodeValue);
    }
  }

  /**
   * Listener function which will issue the request.
   * @param requestName the name of the web service operation to execute
   * @param featureNodeId the id of the node in the doc to be processed by the stylesheet
   */
  this.doRequest = function(objRef, featureName) {
    var feature = objRef.model.getFeatureNode(featureName);

    // if the targetModel is a template model, then create new model object and
    // assign it an id
    if (objRef.targetModel.template) {
      objRef.targetModel.modelNode.removeAttribute("id");
      objRef.targetModel = objRef.model.createObject(objRef.targetModel.modelNode, objRef.model.models);
    }
    objRef.targetModel.featureName = featureName;

    //confirm inputs
    if (objRef.debug) alert("source:"+feature.xml);
    //if (objRef.debug) alert("stylesheet:"+objRef.stylesheet.xslDom.xml);

    if (objRef.targetModel.containerModel) {

      //this block is to get by a Mapserver WFS bug where the tuple separator 
      //is set to a comma instead of space; and post method doesn't work
      var ts = " ";
/*
      var comment = objRef.model.doc.documentElement.childNodes[1];
      if (comment && comment.nodeType==comment.COMMENT_NODE) {
        //alert(comment.nodeValue);
        if (comment.nodeValue.substring("MapServer version 4.4.")) {
          ts = ",";
          objRef.targetModel.method="get";
        }
      }
*/

      var bBox = objRef.targetModel.containerModel.getBoundingBox();
      objRef.stylesheet.setParameter("bBoxMinX", bBox[0] );
      objRef.stylesheet.setParameter("bBoxMinY", bBox[1] );
      objRef.stylesheet.setParameter("bBoxMaxX", bBox[2] );
      objRef.stylesheet.setParameter("bBoxMaxY", bBox[3] );
      objRef.stylesheet.setParameter("srs", objRef.targetModel.containerModel.getSRS() );
      objRef.stylesheet.setParameter("width", objRef.targetModel.containerModel.getWindowWidth() );
      objRef.stylesheet.setParameter("height", objRef.targetModel.containerModel.getWindowHeight() );
    }
    objRef.stylesheet.setParameter("version", objRef.model.getVersion(feature) );

    //process the doc with the stylesheet
    var httpPayload = new Object();
    httpPayload.method = objRef.targetModel.method;
    objRef.stylesheet.setParameter("httpMethod", httpPayload.method );
    httpPayload.postData = objRef.stylesheet.transformNodeToObject(feature);
    //alert("request data:"+httpPayload.postData.xml);
    //var response = postLoad(config.serializeUrl, httpPayload.postData);

    //allow the tool to have a serverUrl property which overrides the model server URL
    //TBD: this still used?
    if (objRef.serverUrl) {
      httpPayload.url = objRef.serverUrl;
    } else {
      httpPayload.url = objRef.model.getServerUrl(objRef.requestName, httpPayload.method, feature);
    }

    //extract the URL from the transformation result for GET method
    if (httpPayload.method.toLowerCase() == "get") {
      var queryString = httpPayload.postData.selectSingleNode("//QueryString");
      if (httpPayload.url.indexOf("?") < 0) httpPayload.url += "?";
      httpPayload.url += queryString.firstChild.nodeValue;
      httpPayload.postData = null;
    }
    
    objRef.targetModel.newRequest(objRef.targetModel,httpPayload);
  }
  this.model.addListener(this.requestName.replace(/:/,"_"), this.doRequest, this);
}
