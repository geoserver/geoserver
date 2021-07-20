/*
License: GPL as per: http://www.gnu.org/copyleft/gpl.html
$Id: WpsCapabilities.js,v 1.2 2005/03/08 22:02:27 madair1 Exp $
*/

/**
 * Stores a Web Processing service (WPS) Capabilities document as defined by the 
 * Open Geospatial Consortium (http://opengis.org).
 *
 * @constructor
 * @author Mike Adair
 * @param modelNode   The model's XML object node from the configuration document.
 * @param parentModel The model object that this widget belongs to.
 */
function WpsCapabilities(modelNode, parentModel) {
  // Inherit the ModelBase functions and parameters
  var modelBase = new ModelBase(this, modelNode, parentModel);

  this.namespace = "xmlns:wps='http://www.opengis.net/wps' xmlns:ows='http://www.opengis.net/ows' xmlns:xlink='http://www.w3.org/1999/xlink'";

  /**
   * Returns the serverUrl for the WFS request passed in with the specified
   * HTTP method from the capabilities doc.
   * @param requestName the WFS request to get the URL for
   * @param method http method for the request
   * @param feature ignored for WFS docs
   * @return URL for the specified request with the specified method
   */
  this.getServerUrl = function(requestName, method, feature) {
    var requestParts = requestName.split(':');
    var xpath = "/wps:Capabilities/ows:OperationsMetadata/ows:Operation[@name='"+requestParts[1]+"']";
    if (method.toLowerCase() == "post") {
      xpath += "/ows:DCP/ows:HTTP/ows:Post";
    } else {
      xpath += "/ows:DCP/ows:HTTP/ows:Get";
    }
    return this.doc.selectSingleNode(xpath).getAttribute("xlink:href");
  }

  /**
   * Returns the version for the wps
   * @return the wps version
   */
  this.getVersion = function() {
    var xpath = "/wps:Capabilities";
    return this.doc.selectSingleNode(xpath).getAttribute("version");
  }

  /**
   * Get HTTP method used to retreive this model
   * @return the HTTP method 
   */
  this.getMethod = function() {
    return this.method;
  }

  /**
   * Returns the featureType node with the specified name from the list of nodes
   * selected by the nodeSelectXpath from the capabilities doc.
   * @param featureName name of the featureType to look up
   * @return the featureType node with the specified name.
   */
  this.getFeatureNode = function(featureName) {
    return this.doc.selectSingleNode(this.nodeSelectXpath+"[wps:name='"+featureName+"']");
  }

}

