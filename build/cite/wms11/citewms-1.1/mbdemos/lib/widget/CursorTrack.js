/*
Author:       Mike Adair mike.adairATccrs.nrcan.gc.ca
License:      GPL as per: http://www.gnu.org/copyleft/gpl.html

$Id: CursorTrack.js,v 1.16 2005/01/12 21:36:53 madair1 Exp $
*/

// Ensure this object's dependancies are loaded.
mapbuilder.loadScript(baseDir+"/widget/WidgetBase.js");
mapbuilder.loadScript(baseDir+"/model/Proj.js");

/**
 * Widget to display the mouse coordinates when it is over a sibling mappane
 * widget.
 *
 * @constructor
 * @base WidgetBase
 * @param widgetNode This widget's object node from the configuration document.
 * @param model The model that this widget is a view of.
 */

function CursorTrack(widgetNode, model) {
  var base = new WidgetBase(this, widgetNode, model);

  /**
   * Start cursor tracking when the mouse is over the mappane.
   * set an interval to output the coords so that it doesn't execute on every
   * move event.  The setInterval method in IE doesn't allow passing of an
   * argument to the function called so set a global reference to glass pane
   * here;  mouse can only be over one glass pane at time so this should be safe.
   * @param objRef Pointer to this CurorTrack object.
   * @param targetNode The node for the enclosing HTML tag for this widget.
   */
  this.mouseOverHandler = function(objRef, targetNode) {
    objRef.coordForm = document.getElementById(objRef.formName);
    window.cursorTrackObject = objRef;
    window.cursorTrackNode = targetNode;
    objRef.mouseOver = true;
    objRef.mouseTrackTimer = setInterval( ReportCoords, 100, objRef);
  }

  /**
   * Stop cursor tracking when the mouse is over the mappane.
   * @param objRef Pointer to this CurorTrack object.
   * @param targetNode The node for the enclosing HTML tag for this widget.
   */
  this.mouseOutHandler = function(objRef, targetNode) {
    if (objRef.mouseTrackTimer) clearInterval(objRef.mouseTrackTimer);
    objRef.mouseOver = false;
    objRef.coordForm.longitude.value = "";
    objRef.coordForm.latitude.value = "";
  }

  //initialize dynamic properties
  this.init = function(toolRef) {
    //associate the cursor track with a mappane widget
    var mouseHandler = widgetNode.selectSingleNode("mb:mouseHandler");
    if (mouseHandler) {
      toolRef.mouseHandler = eval("config.objects."+mouseHandler.firstChild.nodeValue);
      toolRef.mouseHandler.addListener('mouseover', toolRef.mouseOverHandler, toolRef);
      toolRef.mouseHandler.addListener('mouseout', toolRef.mouseOutHandler, toolRef);
    } else {
      alert('CursorTrack requires a mouseHandler property');
    }
    toolRef.proj = new Proj( toolRef.model.getSRS() );
  }
  this.model.addListener("loadModel", this.init, this );


  //set some properties for the form output
  this.formName = "CursorTrackForm_" + mbIds.getId();
  this.stylesheet.setParameter("formName", this.formName);

}

/** Update the lat/long coordinates in coordForm. */
function ReportCoords() {
  var objRef = window.cursorTrackObject;
  if (objRef.mouseOver) {
    var evxy = objRef.model.extent.getXY( window.cursorTrackNode.evpl );
    var evll = objRef.proj.Inverse( evxy );
    objRef.coordForm.longitude.value = Math.round(evll[0]*100000)/100000;
    objRef.coordForm.latitude.value = Math.round(evll[1]*100000)/100000;
  }
}
