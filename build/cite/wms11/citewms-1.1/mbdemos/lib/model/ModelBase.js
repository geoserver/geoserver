/*
License: GPL as per: http://www.gnu.org/copyleft/gpl.html
$Id: ModelBase.js,v 1.58 2005/03/04 04:50:37 madair1 Exp $
*/

/**
 * Base Model class to be inherited by all Model objects and provdes methods
 * and properties common to all models.
 * Stores the XML document as the .doc property of the model.
 * Inherits from the Listener class so all models are also listener objects that
 * can call registered listeners.
 * @constructor
 * @base Listener
 * @author Cameron Shorter
 * @param model       Pointer to the model instance being created
 * @param modelNode   The model's XML object node from the configuration document.
 * @param parentModel The model object that this model belongs to.
 */
function ModelBase(model, modelNode, parentModel) {
  // Inherit the Listener functions and parameters
  var listener = new Listener(model);

  model.modelNode = modelNode;
  var idAttr = modelNode.attributes.getNamedItem("id");
  if (idAttr) {
    model.id = idAttr.nodeValue;
  } else {
    //auto generated unique ID assigned to this model
    model.id = "MbModel_" + mbIds.getId();
  }

  //get the human readable title for the model
  var titleNode = modelNode.selectSingleNode("mb:title");
  if (titleNode) {
    model.title = titleNode.firstChild.nodeValue;
  } else {
    model.title = model.id;
  }

  //load the Model object from the initial URL in config or from a URL param.
  //the URL can also be passed in as a URL parameter by using the model ID
  //as the parameter name (this method takes precendence over the config file
  if (window.cgiArgs[model.id]) {  
    model.url = window.cgiArgs[model.id];
  } else if (window[model.id]) {  
    model.url = window[model.id];
  } else if (modelNode.url) {  
    model.url = modelNode.url;
  } else {
    var defaultModel = modelNode.selectSingleNode("mb:defaultModelUrl");
    if (defaultModel) model.url = defaultModel.firstChild.nodeValue;
  }

  //set the method property
  var method = modelNode.selectSingleNode("mb:method");
  if (method) {
    model.method = method.firstChild.nodeValue;
  } else {
    model.method = "get";
  }

  //set the namespace property
  var namespace = modelNode.selectSingleNode("mb:namespace");
  if (namespace) {
    model.namespace = namespace.firstChild.nodeValue;
  }

  var templateAttr = modelNode.attributes.getNamedItem("template");
  if (templateAttr) {
    model.template = (templateAttr.nodeValue=="true")?true:false;
    model.modelNode.removeAttribute("template");
  }

  //get the xpath to select nodes from the parent doc
  var nodeSelectXpath = modelNode.selectSingleNode("mb:nodeSelectXpath");
  if (nodeSelectXpath) {
    model.nodeSelectXpath = nodeSelectXpath.firstChild.nodeValue;
  }

  /**
   * Get the value of a node as selected by an XPath expression.1
   * @param objRef Reference to this node.
   * @param xpath XPath of the node to update.
   * @return value of the node or null if XPath does not find a node.
   */
  this.getXpathValue=function(objRef,xpath){
    node=objRef.doc.selectSingleNode(xpath);
    if(node && node.firstChild){
      return node.firstChild.nodeValue;
    }else{
      return null;
    }
  }
  model.getXpathValue=this.getXpathValue;

  /**
   * Update the value of a node within this model's XML document.
   * Triggers a refresh event from the model.
   * @param objRef Reference to this node.
   * @param xpath Xpath of the node to update.
   * @param value Node's new value.
   * @return Returns false if Xpath does not find a node.
   */
  this.setXpathValue=function(objRef,xpath,value){
    node=objRef.doc.selectSingleNode(xpath);
    if(node){
      if(node.firstChild){
        node.firstChild.nodeValue=value;
      }else{
        dom=Sarissa.getDomDocument();
        v=dom.createTextNode(value);
        node.appendChild(v);
      }
      objRef.setParam("refresh");
      return true;
    }else{
      return false;
    }
  }
  model.setXpathValue=this.setXpathValue;

  /**
   * Load a Model's document.  This will only occur if the model.url property is
   * set. Calling this method triggers several events:
   * modelStatus - to indicate that the model state is changing
   * newModel - to give widgetrs a chance to clear themselves before the doc is loaded
   * loadModel - to indicate that the document is loaded successfully
   * refresh - to indicate that widgets should be refreshed
   *
   * @param modelRef Pointer to the model object being loaded.
   */
  this.loadModelDoc = function(modelRef){
    modelRef.setParam("modelStatus","loading");

    modelRef.callListeners( "newModel" );
    if (modelRef.url) {

      if (modelRef.contentType == "image") {
        //image models are set as a DOM image object
        modelRef.doc = new Image();
        modelRef.doc.src = modelRef.url;
      } else {
        //XML content type
        if (modelRef.postData) {
          //http POST
          modelRef.doc = postLoad(modelRef.url,modelRef.postData);
        } else {
          //http GET
          modelRef.doc = Sarissa.getDomDocument();
          modelRef.doc.async = false;
          modelRef.doc.validateOnParse=false;  //IE6 SP2 parsing bug
          var url=getProxyPlusUrl(modelRef.url);
          modelRef.doc.load(url);
        }

        if (modelRef.doc.parseError < 0){
          var message = "error loading document: " + modelRef.url;
          if (modelRef.doc.documentElement) message += " - " +Sarissa.getParseErrorText(modelRef.doc);
          alert(message);
          return;
        }

        // the following two lines are needed for IE; set the namespace for selection
        modelRef.doc.setProperty("SelectionLanguage", "XPath");
        if (modelRef.namespace) Sarissa.setXpathNamespaces(modelRef.doc, modelRef.namespace);
      }

      //call the loadModel event
      modelRef.callListeners("loadModel");
      modelRef.callListeners("refresh");

    } else {
      //no URL means this is a template model
      //alert("url parameter required for loadModelDoc");
    }
  }
  model.loadModelDoc = this.loadModelDoc;

  /**
   * Load XML for a model from an httpPayload object
   * To update model data, use:<br/>
   * httpPayload=new Object();<br/>
   * httpPayload.url="url" or null. If set to null, all dependant widgets
   *   will be removed from the display.<br/>
   * httpPayload.httpMethod="post" or "get"<br/>
   * httpPayload.postData=XML or null<br/>
   * @param modelRef    Pointer to the model object being loaded.
   * @param httpPayload an object tho fully specify the request to be made
   */
  this.newRequest = function(modelRef, httpPayload){
    modelRef.url = httpPayload.url;
    if (!modelRef.url){
      modelRef.doc=null;
    }
    modelRef.method = httpPayload.method;
    modelRef.postData = httpPayload.postData;
    modelRef.loadModelDoc(modelRef);
  }
  model.newRequest = this.newRequest;

 /**
   * save the model by posting it to the serializeUrl, which is defined as a 
   * property of config.
   * @param objRef Pointer to this object.
   */
  this.saveModel = function(objRef) {
    if (config.serializeUrl) {
      var response = postLoad(config.serializeUrl, objRef.doc);
      response.setProperty("SelectionLanguage", "XPath");
      Sarissa.setXpathNamespaces(response, "xmlns:xlink='http://www.w3.org/1999/xlink'");
      var onlineResource = response.selectSingleNode("//OnlineResource");
      var fileUrl = onlineResource.attributes.getNamedItem("xlink:href").nodeValue;
      objRef.setParam("modelSaved", fileUrl);
    } else {
      alert("serializeUrl must be specified in config to save a model");
    }
  }
  model.saveModel = this.saveModel;

  /**
   * Creates all mapbuilder JavaScript objects based on the Object nodes defined
   * in the configuration file.
   * A reference to the created model is stored as a property of the config.objects
   * property using the model's ID; you can always get a reference to a mapbuilder
   * object as: "config.objects.objectId"
   * @param configNode The node from config for the model to be created
   */
  this.createObject = function(configNode) {
    var objectType = configNode.nodeName;
    var evalStr = "new " + objectType + "(configNode,this);";
    var newObject = eval( evalStr );
    if ( newObject ) {
      config.objects[newObject.id] = newObject;
      return newObject;
    } else { 
      alert("error creating object:" + objType);
    }
  }
  model.createObject = this.createObject;

  /**
   * Creates all the mapbuilder objects from the config file as selected by the
   * XPath value passed in.
   * @param objectXpath The XPath for the set of nodes being created
   */
  this.loadObjects = function(objectXpath) {
    //loop through all nodes selected from config
    var configObjects = this.modelNode.selectNodes( objectXpath );
    for (var i=0; i<configObjects.length; i++ ) {
      this.createObject( configObjects[i]);
    }
  }
  model.loadObjects = this.loadObjects;

  /**
   * Initialization of all javascript model, widget and tool objects for this model. 
   * Calling this method triggers an init event for this model.
   * @param modelRef Pointer to this object.
   */
  model.init = function(modelRef) {
    modelRef.loadObjects("mb:models/*");
    modelRef.loadObjects("mb:widgets/*");
    modelRef.loadObjects("mb:tools/*");
    modelRef.callListeners("init");
  }

  //don't load in models and widgets if this is the config doc, 
  //defer that to an explcit config.init() call in mapbuilder.js
  if (parentModel && !model.template) {
    parentModel.addListener("loadModel",model.loadModelDoc, model);
    parentModel.addListener("init",model.init, model);
  }

}
