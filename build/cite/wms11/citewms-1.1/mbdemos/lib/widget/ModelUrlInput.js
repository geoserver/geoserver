/*
Author:       Mike Adair mike.adairATccrs.nrcan.gc.ca
License:      GPL as per: http://www.gnu.org/copyleft/gpl.html

$Id: ModelUrlInput.js,v 1.8 2005/02/05 19:22:54 madair1 Exp $
*/

// Ensure this object's dependancies are loaded.
mapbuilder.loadScript(baseDir+"/widget/WidgetBase.js");

/**
 * Widget to display the a form to input any model's URL and load the new URL 
 * as the model's document
 *
 * @constructor
 * @base WidgetBase
 * @param widgetNode  This widget's object node from the configuration document.
 * @param model       The model that this widget is a view of.
 */

function ModelUrlInput(widgetNode, model) {
  var base = new WidgetBase(this, widgetNode, model);

  /**
   * Handles submission of the form (via javascript in an <a> tag)
   */
  this.submitForm = function() {
    var httpPayload = new Object();
    httpPayload.url = this.urlInputForm.modelUrl.value;
    httpPayload.method = this.targetModel.method;
/*
    for (var i=0; i<httpMethod.length; ++i) {   //loop through radio buttons
      if (httpMethod[i].checked) {
        httpPayload.method = httpMethod[i].value;
        if (httpPayload.method.toLowerCase() == "post") {
          httpPayload.postData = null; //TBD get this from somewhere? or not allow post?
          if (this.debug) alert("postData:"+httpPayload.postData.xml);
        } else {
          httpPayload.postData = null;
        }
        break;
      }
    }
*/    
    this.targetModel.newRequest(this.targetModel,httpPayload);
  }

  /**
   * handles keypress events to filter out everything except "enter".  
   * Pressing the "enter" key will trigger a form submit
   * @param event  the event object passed in for Mozilla; IE uses window.event
   */
  this.handleKeyPress = function(event) {
    var keycode;
    var target;
    if (event){
      //Mozilla
      keycode=event.which;
      target=event.currentTarget;
    }else{
      //IE
      keycode=window.event.keyCode;
      target=window.event.srcElement.form;
    }

    if (keycode == 13) {    //enter key
      target.parentWidget.submitForm();
    return false;   //prevent the form from actually being submitted
    }
  }

  /**
   * initializes stylesheet parameters for the form
   * @param objRef Pointer to this widget object.
   */
  this.prePaint = function(objRef) {
    objRef.stylesheet.setParameter("modelUrl", objRef.targetModel.url);
    objRef.stylesheet.setParameter("modelTitle", objRef.targetModel.title);
  }

  /**
   * Refreshes the form and event handlers when this widget is painted.
   * @param objRef Pointer to this CurorTrack object.
   */
  this.postPaint = function(objRef) {
    objRef.urlInputForm = document.getElementById(objRef.formName);
    objRef.urlInputForm.parentWidget = objRef;
    objRef.urlInputForm.onkeypress = objRef.handleKeyPress;
    //objRef.WebServiceForm.onsubmit = objRef.submitForm;
    //objRef.WebServiceForm.mapsheet.onblur = objRef.setMapsheet;
  }

  //set some properties for the form output
  this.formName = "urlInputForm_";// + mbIds.getId();
  this.stylesheet.setParameter("formName", this.formName);
}

