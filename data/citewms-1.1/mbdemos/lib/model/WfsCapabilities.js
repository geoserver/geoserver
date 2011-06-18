/*
License: GPL as per: http://www.gnu.org/copyleft/gpl.html
$Id: WfsCapabilities.js,v 1.7 2005/03/06 04:14:51 madair1 Exp $
*/

/**
 * Stores a Web Feature (WFS) Capabilities document as defined by the 
 * Open Geospatial Consortium (http://opengis.org).
 *
 * @constructor
 * @author Mike Adair
 * @param modelNode   The model's XML object node from the configuration document.
 * @param parentModel The model object that this widget belongs to.
 */
function WfsCapabilities(modelNode, parentModel) {
  // Inherit the ModelBase functions and parameters
  var modelBase = new ModelBase(this, modelNode, parentModel);

  this.namespace = "xmlns:wfs='http://www.opengis.net/wfs'";

  /**
   * Returns the serverUrl for the WFS request passed in with the specified
   * HTTP method from the capabilities doc.
   * @param requestName the WFS request to get the URL for
   * @param method http method for the request
   * @param feature ignored for WFS docs
   * @return URL for the specified request with the specified method
   */
  this.getServerUrl = function(requestName, method, feature) {
    var xpath = "/wfs:WFS_Capabilities/wfs:Capability/wfs:Request/"+requestName;
    if (method.toLowerCase() == "post") {
      xpath += "/wfs:DCPType/wfs:HTTP/wfs:Post";
    } else {
      xpath += "/wfs:DCPType/wfs:HTTP/wfs:Get";
    }
    return this.doc.selectSingleNode(xpath).getAttribute("onlineResource");
  }

  /**
   * Returns the version for the WFS
   * @return the WFS version
   */
  this.getVersion = function() {
    var xpath = "/wfs:WFS_Capabilities";
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
    return this.doc.selectSingleNode(this.nodeSelectXpath+"[wfs:Name='"+featureName+"']");
  }

}

