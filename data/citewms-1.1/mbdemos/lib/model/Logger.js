/*
License: GPL as per: http://www.gnu.org/copyleft/gpl.html
$Id: Logger.js,v 1.5 2005/03/04 04:50:37 madair1 Exp $
*/

/**
 * Records a log of events that occur over the course of mapbuilder execution
 * @constructor
 * @base ModelBase
 * @author Mike Adair
 * @param modelNode Pointer to the xml node for this model from the config file.
 * @param parent    The parent model for the object.
 */
function Logger(modelNode, parent) {
  // Inherit the ModelBase functions and parameters
  var modelBase = new ModelBase(this, modelNode, parent);
  this.namespace = "xmlns:mb='http://mapbuilder.sourceforge.net/mapbuilder'";

  //create a new document
  this.doc = Sarissa.getDomDocument("http://mapbuilder.sourceforge.net/mapbuilder","mb:Logger");//!no prefix on the URL
  Sarissa.setXpathNamespaces(this.doc, this.namespace);
  this.doc.async = false;
  this.doc.validateOnParse=false;  //IE6 SP2 parsing bug

  /**
   * appends a new entry in the log file
   * @param evenType    the name of the event that occured
   * @param listenerId  the ID of the listener object
   * @param targetId    the ID of the object passed to the listener function
   * @param paramValue  any parameter info supplied to the listener function
   */
  this.logEvent = function(eventType, listenerId, targetId, paramValue) {
    var eventLog = this.doc.createElement("event");
    eventLog.setAttribute("time", new Date().getTime());
    eventLog.setAttribute("listener", listenerId);
    eventLog.setAttribute("target", targetId);
    if (paramValue) eventLog.setAttribute("param", paramValue);
    eventLog.appendChild(this.doc.createTextNode(eventType));
    this.doc.documentElement.appendChild(eventLog);
  }

  /**
   * clears all entries in the log file
   */
  this.clearLog = function() {
    while (this.doc.documentElement.hasChildNodes() ) {
      this.doc.documentElement.removeChild(this.doc.documentElement.firstChild);
    }
    this.callListeners("refresh");
  }

  /**
   * save the log by http post to the serializeUrl URL provided
   */
  this.saveLog = function() {
    if (config.serializeUrl) {
      var tempDoc = postLoad(config.serializeUrl,logger.doc);
      tempDoc.setProperty("SelectionLanguage", "XPath");
      Sarissa.setXpathNamespaces(tempDoc, "xmlns:xlink='http://www.w3.org/1999/xlink'");
      var onlineResource = tempDoc.selectSingleNode("//OnlineResource");
      var fileUrl = onlineResource.attributes.getNamedItem("xlink:href").nodeValue;
      alert("event log saved as:" + fileUrl);
    } else {
      alert("unable to save event log; provide a serializeUrl property in config");
    }
  }

  /**
   * save the log by http post to the serializeUrl URL provided
   */
  this.refreshLog = function(objRef) {
    objRef.callListeners("refresh");
  }

  if (parent) parent.addListener("refresh",this.refreshLog, this);
  window.onunload = this.saveLog;   //automatically save the log when the page unloads
  window.logger = this;             //global reference to the logger model
}
