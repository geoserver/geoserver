/*
Author:       Mike Adair mike.adairATccrs.nrcan.gc.ca
License:      GPL as per: http://www.gnu.org/copyleft/gpl.html

$Id: AoiForm.js,v 1.7 2005/01/26 16:19:40 madair1 Exp $
*/

// Ensure this object's dependancies are loaded.
mapbuilder.loadScript(baseDir+"/widget/WidgetBase.js");
mapbuilder.loadScript(baseDir+"/model/Proj.js");

/**
 * Widget to display the AOI box coordinates
 *
 * @constructor
 * @base WidgetBase
 * @param widgetNode This widget's object node from the configuration document.
 * @param model The model that this widget is a view of.
 */

function AoiForm(widgetNode, model) {
  var base = new WidgetBase(this, widgetNode, model);

  /**
   * Output the AOI coordinates to the associated form input elements.  This
   * method is registered as an AOI listener on the context doc.
   * @param objRef Pointer to this AoiForm object.
   * @param targetNode The node for the enclosing HTML tag for this widget.
   */
  this.displayAoiCoords = function(objRef, targetNode) {
    objRef.aoiForm = document.getElementById(objRef.formName);
    var aoi = objRef.model.getParam("aoi");
    objRef.aoiForm.westCoord.value = aoi[0][0];
    objRef.aoiForm.northCoord.value = aoi[0][1];
    objRef.aoiForm.eastCoord.value = aoi[1][0];
    objRef.aoiForm.southCoord.value = aoi[1][1];
  }
  this.model.addListener('aoi', this.displayAoiCoords, this);

  /**
   * Handles user input from the form element.  This is an onblur handler for 
   * the input elements.
   * @param objRef Pointer to this CurorTrack object.
   * @param targetNode The node for the enclosing HTML tag for this widget.
   */
  this.setAoi = function(event) {
    var aoi = this.model.getParam("aoi");
    if (aoi) {
      var ul = aoi[0];
      var lr = aoi[1];
      switch(this.name) {
        case 'westCoord':
          ul[0] = this.value;
          break;
        case 'northCoord':
          ul[1] = this.value;
          break;
        case 'eastCoord':
          lr[0] = this.value;
          break;
        case 'southCoord':
          lr[1] = this.value;
          break;
      }
      this.model.setParam("aoi",new Array(ul,lr) );
    }
  }

  /**
   * Refreshes the form onblur handlers when this widget is painted.
   * @param objRef Pointer to this AoiForm object.
   */
  this.postPaint = function(objRef) {
    objRef.aoiForm = document.getElementById(objRef.formName);
    objRef.aoiForm.westCoord.onblur = objRef.setAoi;
    objRef.aoiForm.northCoord.onblur = objRef.setAoi;
    objRef.aoiForm.eastCoord.onblur = objRef.setAoi;
    objRef.aoiForm.southCoord.onblur = objRef.setAoi;
    objRef.aoiForm.westCoord.model = objRef.model;
    objRef.aoiForm.northCoord.model = objRef.model;
    objRef.aoiForm.eastCoord.model = objRef.model;
    objRef.aoiForm.southCoord.model = objRef.model;
  }

  //set some properties for the form output
  this.formName = "AoiForm_";// + mbIds.getId();
  this.stylesheet.setParameter("formName", this.formName);
}

