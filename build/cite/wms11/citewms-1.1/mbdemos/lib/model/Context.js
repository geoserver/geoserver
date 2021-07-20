/*
License: GPL as per: http://www.gnu.org/copyleft/gpl.html
$Id: Context.js,v 1.76 2005/03/06 04:14:50 madair1 Exp $
*/

/**
 * Stores a Web Map Context (WMC) document as defined by the Open GIS Consortium
 * http://opengis.org and extensions the the WMC.  
 *
 * Listeners supported by this model:
 * "refresh" called when window parameters (width/height, bbox) are changed
 * "hidden" called when visibilty of a layer is changed
 *
 * @constructor
 * @base ModelBase
 * @author Cameron Shorter
 * @param modelNode Pointer to the xml node for this model from the config file.
 * @param parent    The parent model for the object.
  * 
 */
function Context(modelNode, parent) {
  // Inherit the ModelBase functions and parameters
  var modelBase = new ModelBase(this, modelNode, parent);

  this.namespace = "xmlns:mb='http://mapbuilder.sourceforge.net/mapbuilder' xmlns:wmc='http://www.opengis.net/context' xmlns:xsl='http://www.w3.org/1999/XSL/Transform'";

  /**
   * Change a Layer's visibility.
   * @param layerName  The name of the layer that is to be changed
   * @param hidden     String with the value to be set; 1=hidden, 0=visible.
   */
  this.setHidden=function(layerName, hidden){
    // Set the hidden attribute in the Context
    var hiddenValue = "0";
    if (hidden) hiddenValue = "1";
      
    var layer=this.doc.selectSingleNode("/wmc:ViewContext/wmc:LayerList/wmc:Layer[wmc:Name='"+layerName+"']");
    if (layer) layer.setAttribute("hidden", hiddenValue);
    // Call the listeners
    this.callListeners("hidden", layerName);
  }

  /**
   * Get the layer's visiblity attribute value.
   * @param layerName  The name of the layer that is to be changed
   * @return hidden  String with the value; 1=hidden, 0=visible.
   */
  this.getHidden=function(layerName){
    var hidden=1;
    var layer=this.doc.selectSingleNode("/wmc:ViewContext/wmc:LayerList/wmc:Layer[wmc:Name='"+layerName+"']");
    if (layer) hidden = layer.getAttribute("hidden");
    return hidden;
  }

  /**
   * Get the BoundingBox value from the Context document.
   * @return BoundingBox array with the sequence (xmin,ymin,xmax,ymax).
   */
  this.getBoundingBox=function() {
    var boundingBox=this.doc.selectSingleNode("/wmc:ViewContext/wmc:General/wmc:BoundingBox");
    bbox = new Array();
    bbox[0]=parseFloat(boundingBox.getAttribute("minx"));
    bbox[1]=parseFloat(boundingBox.getAttribute("miny"));
    bbox[2]=parseFloat(boundingBox.getAttribute("maxx"));
    bbox[3]=parseFloat(boundingBox.getAttribute("maxy"));
    return bbox;
  }

  /**
   * Set the BoundingBox element and call the refresh listeners
   * @param boundingBox array in the sequence (xmin, ymin, xmax, ymax).
   */
  this.setBoundingBox=function(boundingBox) {
    // Set BoundingBox in context
    //bbox=this.doc.documentElement.getElementsByTagName("BoundingBox").item(0);
    var bbox=this.doc.selectSingleNode("/wmc:ViewContext/wmc:General/wmc:BoundingBox");
    bbox.setAttribute("minx", boundingBox[0]);
    bbox.setAttribute("miny", boundingBox[1]);
    bbox.setAttribute("maxx", boundingBox[2]);
    bbox.setAttribute("maxy", boundingBox[3]);
    // Call the listeners
    this.callListeners("refresh");
  }

  /**
   * Set the Spacial Reference System for the context document.
   * @param srs The Spatial Reference System.
   */
  this.setSRS=function(srs) {
    var bbox=this.doc.selectSingleNode("/wmc:ViewContext/wmc:General/wmc:BoundingBox");
    bbox.setAttribute("SRS",srs);
    this.callListeners("refresh");
  }

  /**
   * Get the Spacial Reference System from the context document.
   * @return srs The Spatial Reference System.
   */
  this.getSRS=function() {
    var bbox=this.doc.selectSingleNode("/wmc:ViewContext/wmc:General/wmc:BoundingBox");
    srs=bbox.getAttribute("SRS");
    return srs;
  }

  /**
   * Get the Window width.
   * @return width The width of map window from the context document
   */
  this.getWindowWidth=function() {
    var win=this.doc.selectSingleNode("/wmc:ViewContext/wmc:General/wmc:Window");
    return win.getAttribute("width");
  }

  /**
   * Set the Window widt    alert("loading " + url);
   alert("loading " + url);
   h.
   * @param width The width of map window (therefore of map layer images).
   */
  this.setWindowWidth=function(width) {
    var win=this.doc.selectSingleNode("/wmc:ViewContext/wmc:General/wmc:Window");
    win.setAttribute("width", width);
    this.callListeners("refresh");
  }

  /**
   * Get the Window height.
   * @return height The height of map window from the context document.
   */
  this.getWindowHeight=function() {
    var win=this.doc.selectSingleNode("/wmc:ViewContext/wmc:General/wmc:Window");
    return win.getAttribute("height");
  }

  /**
   * Set the Window height.
   * @param height The height of map window to set in the context document
   */
  this.setWindowHeight=function(height) {
    var win=this.doc.selectSingleNode("/wmc:ViewContext/wmc:General/wmc:Window");
    win.setAttribute("height", height);
    this.callListeners("refresh");
  }

  /**
   * Returns the serverUrl for the layer passed in as the feature argument.
   * @param requestName ignored for context docs (only GetMap supported)
   * @param method ignored for context docs (only GET supported)
   * @param feature the Layer node from the context doc
   * @return URL for the GetMap request 
   */
  this.getServerUrl = function(requestName, method, feature) {
    return feature.selectSingleNode("wmc:Server/wmc:OnlineResource").getAttribute("xlink:href");
  }

  /**
   * Returns the WMS version for the layer passed in as the feature argument
   * @param feature the Layer node from the context doc
   * @return the WMS GetMap version for the Layer.
   */
  this.getVersion = function(feature) {  
    return feature.selectSingleNode("wmc:Server").getAttribute("version");
  }

  /**
   * Get HTTP method for the specified feature
   * @param feature the Layer node from the context doc
   * @return the HTTP method to get the feature with
   */
  this.getMethod = function(feature) {
    return feature.selectSingleNode("wmc:Server/wmc:OnlineResource").getAttribute("wmc:method");
  }

  /**
   * Adds a node to the Context document extension element.  The extension element
   * will be created if it doesn't already exist.  
   * @param extensionNode the node to be appended in the extension element.
   * @return the ndoe added to the extension element
   */
  this.setExtension = function(extensionNode) {
    var extension = this.doc.selectSingleNode("/wmc:ViewContext/wmc:General/wmc:Extension");
    if (!extension) {
      var general = this.doc.selectSingleNode("/wmc:ViewContext/wmc:General");
      extension = general.appendChild(this.doc.createElementNS('http://www.opengis.net/context',"Extension"));
    }
    return extension.appendChild(extensionNode);
  }

  /**
   * Returns the contents of the extension element
   * @return the contents of the extension element
   */
  this.getExtension = function() {
    return this.doc.selectSingleNode("/wmc:ViewContext/wmc:General/wmc:Extension");
  }

  /**
   * Parses a Dimension element from the Context document as a loadModel listener.
   * This results in an XML structure with one element for each GetMap time value 
   * parameter and added to the Context extrension element.
   * @param objRef a pointer to this object 
   */
  this.initTimeExtent = function( objRef ) {
    var mbNS = "http://mapbuilder.sourceforge.net/mapbuilder";
    //only the first one selected is used as the timestamp source
    //var extentNode = objRef.doc.selectSingleNode("//wmc:Layer/wmc:Dimension[@name='time']");
    //TBD: how to deal with multiple time dimensions in one context doc, or caps doc?
    var timeNodes = objRef.doc.selectNodes("//wmc:Dimension[@name='time']");
    for (var i=0; i<timeNodes.length; ++i) {
      var extentNode = timeNodes[i];
      objRef.timestampList = objRef.doc.createElementNS(mbNS,"TimestampList");  //set mb as namespace instead
      var layerName = extentNode.parentNode.parentNode.selectSingleNode("wmc:Name").firstChild.nodeValue;
      objRef.timestampList.setAttribute("layerName", layerName);
      //alert("found time dimension, extent:"+extentNode.firstChild.nodeValue);
      var times = extentNode.firstChild.nodeValue.split(",");   //comma separated list of arguments
      for (var j=0; j<times.length; ++j) {
        var params = times[j].split("/");     // parses start/end/period
        if (params.length==3) {
          var start = setISODate(params[0]);
          var stop = setISODate(params[1]);
          var period = params[2];
          var parts = period.match(/^P((\d*)Y)?((\d*)M)?((\d*)D)?T?((\d*)H)?((\d*)M)?((.*)S)?/);
          for (var i=1; i<parts.length; ++i) {
            if (!parts[i]) parts[i]=0;
          }
          //alert("start time:"+start.toString());
          do {
            var timestamp = objRef.doc.createElementNS(mbNS,"Timestamp");
            timestamp.appendChild(objRef.doc.createTextNode(getISODate(start)));
            objRef.timestampList.appendChild(timestamp);

            start.setFullYear(start.getFullYear()+parseInt(parts[2],10));
            start.setMonth(start.getMonth()+parseInt(parts[4],10));
            start.setDate(start.getDate()+parseInt(parts[6],10));
            start.setHours(start.getHours()+parseInt(parts[8],10));
            start.setMinutes(start.getMinutes()+parseInt(parts[10],10));
            start.setSeconds(start.getSeconds()+parseFloat(parts[12]));
            //alert("time:"+start.toString());
          } while(start.getTime() <= stop.getTime());

        } else {
          //output single date value
          var timestamp = objRef.doc.createElementNS(mbNS,"Timestamp");
          timestamp.appendChild(objRef.doc.createTextNode(times[j]));
          objRef.timestampList.appendChild(timestamp);
        }
      }
     objRef.setExtension(objRef.timestampList);  
    }
  }
  this.addFirstListener( "loadModel", this.initTimeExtent, this );

  /**
   * Returns the current timestamp value.
   * @param layerName the name of the Layer from which the timestamp list was generated
   * @return the current timestamp value.
   */
  this.getCurrentTimestamp = function( layerName ) {
    var extension = this.getExtension();
    var timestamp = extension.selectSingleNode("mb:TimestampList[@layerName='"+layerName+"']/mb:Timestamp[@current='1']");
    return timestamp;
  }
}

