/*
License: GPL as per: http://www.gnu.org/copyleft/gpl.html
$Id: WmsCapabilities.js,v 1.6 2005/03/06 04:14:51 madair1 Exp $
*/

/**
 * Stores a Web Map Server (WMS) Capabilities document as defined by the 
 * Open Geospatial Consortium (http://opengis.org and extensions the the WMC.  
 *
 * @constructor
 * @author Mike Adair
 * @param modelNode   The model's XML object node from the configuration document.
 * @param parentModel The model object that this widget belongs to.
 */
function WmsCapabilities(modelNode, parentModel) {
  // Inherit the ModelBase functions and parameters
  var modelBase = new ModelBase(this, modelNode, parentModel);

  this.namespace = "xmlns:wms='http://www.opengis.net/wms' xmlns:xlink='http://www.w3.org/1999/xlink'";

  /**
   * Returns the serverUrl for the WMS request passed in with the specified
   * HTTP method from the capabilities doc.
   * @param requestName the WMS request to get the URL for
   * @param method http method for the request
   * @param feature ignored for WMS docs
   * @return URL for the specified request with the specified method
   */
  this.getServerUrl = function(requestName, method, feature) {
    var xpath = "/WMT_MS_Capabilities/Capability/Request/"+requestName;
    if (method.toLowerCase() == "post") {
      xpath += "/DCPType/HTTP/Post/OnlineResource";
    } else {
      xpath += "/DCPType/HTTP/Get/OnlineResource";
    }
    return this.doc.selectSingleNode(xpath).getAttribute("xlink:href");
  }

  /**
   * Returns the version for the WMS
   * @return the WMS version
   */
  this.getVersion = function() {
    var xpath = "/WMT_MS_Capabilities";
    return this.doc.selectSingleNode(xpath).getAttribute("version");
  }

  /**
   * @return the title of the WMS server
   */
  this.getServerTitle = function() {
    var xpath = "/WMT_MS_Capabilities/Service/Title";
    var node = this.doc.selectSingleNode(xpath);
    return node.firstChild.nodeValue;
  }

  /**
   * @return the name of the WMS server
   */
  this.getServiceName = function() {
    var xpath = "/WMT_MS_Capabilities/Service/Name";
    var node = this.doc.selectSingleNode(xpath);
    return node.firstChild.nodeValue;
  }

  /**
   * Returns the Layer node with the specified name from the list of nodes
   * selected by the nodeSelectXpath from the capabilities doc.
   * @param featureName name of the featureType to look up
   * @return the Layer node with the specified name.
   */
  this.getFeatureNode = function(featureName) {
    return this.doc.selectSingleNode(this.nodeSelectXpath+"[Name='"+featureName+"']");
  }

  /**
   * Looks up a Layer node from the capabilities document based on the name
   * passed in and sets that as a model param for any objects regsitered for
   * the "AddNodeToContext" listener.
   * @param featureName name of the featureType to look up
   */
  this.addToContext = function(featureName) {
    var feature = this.getFeatureNode(featureName);
    this.setParam("AddNodeToContext",feature);
  }

}

