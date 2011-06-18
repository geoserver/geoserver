/*
License: GPL as per: http://www.gnu.org/copyleft/gpl.html
$Id: WidgetBase.js,v 1.67 2005/03/02 19:29:40 camerons Exp $
*/

/**
 * Base Class for widgets.  Associates a node on the page with a stylesheet and
 * model.  All widgets must extend this base class.
 * Defines the default paint() method for all widgets which is where the 
 * stylesheet is applied to the model XML document.
 * The stylesheet URL defaults to "widget/<widgetName>.xsl" if it is not defined
 * in config file.  Set a stylesheet property containing an XSL URL in config
 * to customize the stylesheet used.
 * All stylesheets will have "modelId" and "widgetId" parameters set when called.
 *
 * @constructor
 * @author Mike Adair 
 * @param widget      Pointer to the widget instance being created
 * @param widgetNode  The widget's XML object node from the configuration document.
 * @param model       The model object that this widget belongs to.
 */
function WidgetBase(widget,widgetNode,model) {
  widget.model = model;
  widget.widgetNode = widgetNode;

  /** Method used for painting.
   * xsl2html (default) => output of XSL will be HTML.
   * xsl2js => output of XSL will be javascript to execute.
   */
  widget.paintMethod="xsl2html";

  //allow the widget output to be replaced on each paint call
  var outputNode = widgetNode.selectSingleNode("mb:outputNodeId");
  if ( outputNode ) {
    widget.outputNodeId = outputNode.firstChild.nodeValue;
  } else {
    widget.outputNodeId = "MbWidget_" + mbIds.getId();
  }

  /** Widget's Id defined in the Config (required) */
  if (widgetNode.attributes.getNamedItem("id")) {
    widget.id = widgetNode.attributes.getNamedItem("id").nodeValue;
  } else {
    alert("id required for object:" + widgetNode.nodeName );
  }

  //until htmlTagNode becomes required allow setting of it by widget id
  if (!widget.htmlTagId) {
    var htmlTagNode = widgetNode.selectSingleNode("mb:htmlTagId");
    if (htmlTagNode) {
      widget.htmlTagId = htmlTagNode.firstChild.nodeValue;
    } else {
      widget.htmlTagId = widget.id;
    }
  }

  // Node in main HTML to attach widget to.
  widget.node = document.getElementById(widget.htmlTagId);
  if(!widget.node) {
    //alert("htmlTagId: "+widget.htmlTagId+" for widget "+widgetNode.nodeName+" not found in config");
  }

  //allow the widget output to be replaced on each paint call
  widget.autoRefresh = true;
  var autoRefreshNode = widgetNode.selectSingleNode("mb:autoRefresh");
  if ( autoRefreshNode ) widget.autoRefresh = (autoRefreshNode.firstChild.nodeValue=="false")?false:true;

  //set an empty debug property in config to see inputs and outputs of stylehseet
  if ( widgetNode.selectSingleNode("mb:debug") ) widget.debug=true;

  /** Transient var used to store model XML before and then after XSL transform.
   *  It can be modified by prePaint() .
   */
  widget.resultDoc = null;


  // Set this.stylesheet
  // Defaults to "widget/<widgetName>.xsl" if not defined in config file.
  if ( !widget.stylesheet ) {
    var styleNode = widgetNode.selectSingleNode("mb:stylesheet");
    if (styleNode ) {
      widget.stylesheet = new XslProcessor(styleNode.firstChild.nodeValue);
    } else {
      widget.stylesheet = new XslProcessor(baseDir+"/widget/"+widgetNode.nodeName+".xsl");
    }
  }

  //set the target model
  var targetModel = widgetNode.selectSingleNode("mb:targetModel");
  if (targetModel) {
    widget.targetModel = eval("config.objects."+targetModel.firstChild.nodeValue);
    if ( !widget.targetModel ) {
      alert("error finding targetModel:" + targetModel.firstChild.nodeValue + " for:" + widget.id);
    } 
  } else {
    widget.targetModel = widget.model;
  }
  widget.stylesheet.setParameter("targetModelId", widget.targetModel.id );

  /**
   * Initialize dynamic properties.
   * @param toolRef Pointer to this object.
  this.initTargetModel = function(objRef) {
    //set the target model
    var targetModel = objRef.widgetNode.selectSingleNode("mb:targetModel");
    if (targetModel) {
      objRef.targetModel = eval("config.objects."+targetModel.firstChild.nodeValue);
      if ( !objRef.targetModel ) {
        alert("error finding targetModel:" + targetModel.firstChild.nodeValue + " for:" + objRef.id);
      }
    } else {
      objRef.targetModel = objRef.model;
    }
    objRef.stylesheet.setParameter("targetModelId", objRef.targetModel.id );
  }
   */


  // Set stylesheet parameters for all the child nodes from the config file
  for (var j=0;j<widgetNode.childNodes.length;j++) {
    if (widgetNode.childNodes[j].firstChild
      && widgetNode.childNodes[j].firstChild.nodeValue)
    {
      widget.stylesheet.setParameter(
        widgetNode.childNodes[j].nodeName,
        widgetNode.childNodes[j].firstChild.nodeValue);
    }
  }
  //this is supposed to work too instead of above?
  //widget.stylesheet.setParameter("widgetNode", widgetNode );

  //all stylesheets will have these properties available
  widget.stylesheet.setParameter("modelId", widget.model.id );
  widget.stylesheet.setParameter("modelTitle", widget.model.title );
  widget.stylesheet.setParameter("widgetId", widget.id );
  widget.stylesheet.setParameter("skinDir", config.skinDir );
  widget.stylesheet.setParameter("lang", config.lang );

  /**
   * Move this widget to the absolute (left,top) position in the browser.
   * @param left Absolute left coordinate.
   * @param top Absolute top coordinate.
   */
  this.move = function(left,top) {
    this.node.style.left = left;
    this.node.style.top = top;
  }

  /**
   * Resize this widget.
   * @param width New width.
   * @param height New height.
   */
  this.resize = function(width,height) {
    this.node.style.width = width;
    this.node.style.height = height;
  }

  /**
   * Called change the visibility of this widget's output node
   * @param vis   boolean true or false 
   */
  this.setVisibility = function(vis) {
    var vis="visible";
    if (vis) vis="hidden";
    document.getElementById(this.outputNodeId).style.visibility = vis;
  }

  /**
   * Called before paint(), can be used to set up a widget's paint parameters,
   * or modify model using this.resultDoc().
   * @param objRef Pointer to this object.
   */
  this.prePaint = function(objRef) {
    //no-op by default
  }

  /**
   * Called before paint(), can be used to set up a widget's paint parameters,
   * or modify model using this.resultDoc().
   * @param objRef Pointer to this object.
   */
  this.postPaint = function(objRef) {
    //no-op by default
  }

  /**
   * Render the widget.
   * @param objRef Pointer to widget object.
   */
  this.paint = function(objRef, forceRefresh) {
    if (objRef.model.template) return;

    // Remove widget from display if the model is empty
    if (!objRef.model.doc){
      objRef.clearWidget(objRef);
    }

    if (objRef.model.doc && objRef.node && (objRef.autoRefresh||forceRefresh) && !objRef.override) {

      //if (objRef.debug) alert("source:"+objRef.model.doc.xml);
      objRef.resultDoc = objRef.model.doc; // resultDoc sometimes modified by prePaint()
      objRef.prePaint(objRef);

      //confirm inputs
      if (objRef.debug) alert("prepaint:"+objRef.resultDoc.xml);
      if (objRef.debug) alert("stylesheet:"+objRef.stylesheet.xslDom.xml);

      //set to output to a temporary node
      //hack to get by doc parsing problem in IE
      //the firstChild of tempNode will be the root element output by the stylesheet
      var outputNode = document.getElementById( objRef.outputNodeId );
      var tempNode = document.createElement("DIV");
      switch (objRef.paintMethod) {
        case "xsl2html":
          //process the doc with the stylesheet
          var s = objRef.stylesheet.transformNode(objRef.resultDoc);
          if (objRef.debug) postLoad(config.serializeUrl, s);
          if (objRef.debug) alert("painting:"+objRef.id+":"+s);
          tempNode.innerHTML = s;
          tempNode.firstChild.setAttribute("id", objRef.outputNodeId);

          //look for this widgets output and replace if found,
          //otherwise append it
          if (outputNode) {
            objRef.node.replaceChild(tempNode.firstChild,outputNode);
          } else {
            objRef.node.appendChild(tempNode.firstChild);
          }
          break;
        case "image2html":
          tempNode.style.position="absolute";
          tempNode.style.top=0;
          tempNode.style.left=0;
          tempNode.appendChild(objRef.model.doc);
          tempNode.setAttribute("id", objRef.outputNodeId);

          //look for this widgets output and replace if found,
          //otherwise append it
          if (outputNode) {
            objRef.node.replaceChild(tempNode,outputNode);
          } else {
            objRef.node.appendChild(tempNode);
          }
          break;
        case "xsl2js":
          jsNode = objRef.stylesheet.transformNodeToObject(objRef.resultDoc);
          js=jsNode.selectSingleNode("js").firstChild.nodeValue;
          if (!outputNode) {
            tempNode.innerHTML = "<div/>";
            tempNode.firstChild.setAttribute("id", objRef.outputNodeId);
            objRef.node.appendChild(tempNode.firstChild);
          }
          if (objRef.debug) alert("javascript eval:"+js);
          objRef.model.setParam("modelStatus","rendering");
          eval(js);
          break;
        default:
          alert("WidgetBase: Invalid paintMethod="+objRef.paintMethod);
      }

      objRef.postPaint(objRef);
    }
  }

  /**
   * Remove widget from display.
   * @param objRef Pointer to this object.
   */ 
  this.clearWidget = function(objRef) {
    //with objRef.node remove child
    var outputNode = document.getElementById( objRef.outputNodeId );
    if (outputNode) objRef.node.removeChild(outputNode);
  }

  // If this object is being created because a child is extending this object,
  // then child.properties = this.properties
  for (sProperty in this) {
    widget[sProperty] = this[sProperty];
  }

  // Call paint when model changes
  //config.addListener( "loadModel", widget.initTargetModel, widget );
  //widget.model.addListener("loadModel",widget.paint, widget);
  widget.model.addListener("refresh",widget.paint, widget);
  widget.model.addListener("newModel",widget.clearWidget, widget);
}
