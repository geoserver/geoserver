/*
Author:       Cameron Shorter cameronATshorter.net
License:      GPL as per: http://www.gnu.org/copyleft/gpl.html

$Id: AoiBoxWZ.js,v 1.11 2005/01/04 05:17:21 madair1 Exp $
*/
// Ensure this object's dependancies are loaded.
mapbuilder.loadScript(baseDir+"/widget/MapContainerBase.js");
mapbuilder.loadScript(baseDir+"/util/wz_jsgraphics/wz_jsgraphics.js");

/**
 * Render an Area Of Interest (AOI) Box over a map.
 * This widget extends GmlRenderer and uses GmlRenderer.xsl to build the HTML box.
 * @constructor
 * @base GmlRenderer
 * @param widgetNode  The widget's XML object node from the configuration document.
 * @param model       The model object that this widget belongs to.
 */
function AoiBoxWZ(widgetNode, model) {
  // Inherit the MapContainerBase functions and parameters
  var base = new MapContainerBase(this,widgetNode, model);
  for (sProperty in base) { 
    this[sProperty] = base[sProperty]; 
  } 

  this.lineWidth = widgetNode.selectSingleNode("mb:lineWidth").firstChild.nodeValue;
  this.lineColor = widgetNode.selectSingleNode("mb:lineColor").firstChild.nodeValue;
  this.crossSize = widgetNode.selectSingleNode("mb:crossSize").firstChild.nodeValue;

  /**
   * Render the widget.
   * If the box width or height is less than the cross size, then draw a cross,
   * otherwise draw a box.
   * @param objRef Pointer to this object.
   */
  this.paint = function(objRef) {

    //create the output node the first time this is called
    var outputNode = document.getElementById( objRef.outputNodeId );
    if (!outputNode) {
      outputNode = document.createElement("DIV");
      outputNode.setAttribute("id",objRef.outputNodeId);
      outputNode.style.position="relative";
      objRef.node.appendChild(outputNode);
    }

    if (! objRef.jg) {
      // WZ Graphics object and rendering functions.
      objRef.jg=new jsGraphics(objRef.outputNodeId);
      objRef.jg.setColor(objRef.lineColor);

      //TBD: The following causes lines to be drawn incorrectly in Mozilla 1.71
      objRef.jg.setStroke(parseInt(objRef.lineWidth));
    }

    var aoiBox = objRef.model.getParam("aoi");
    if (aoiBox) {
      var ul = objRef.model.extent.getPL(aoiBox[0]);
      var lr = objRef.model.extent.getPL(aoiBox[1]);
      var width= lr[0]-ul[0];
      var height= lr[1]-ul[1];

      objRef.jg.clear();

      //check if ul=lr, then draw cross, else drawbox
      if ((width < objRef.crossSize) && (height < objRef.crossSize) ) {
        // draw cross
        var x=(lr[0]+ul[0])/2;
        var y=(lr[1]+ul[1])/2;
        var c=objRef.crossSize/2;
        objRef.jg.drawLine(x+c,y,x-c,y);
        objRef.jg.drawLine(x,y+c,x,y-c);
      } else {
        // draw box
        objRef.jg.drawRect(ul[0],ul[1],width,height);
      }
      objRef.jg.paint();
    }
  }
  model.addListener("aoi",this.paint, this);

  /**
   * Reset internal variables after container is redrawn due to refreshing
   * of the model.
   */
  this.refresh = function(objRef) {
    objRef.jg=null;
  }
  model.addListener("refresh",this.refresh, this);
}
