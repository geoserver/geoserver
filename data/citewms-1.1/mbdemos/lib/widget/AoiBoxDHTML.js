/*
Author:       Mike Adair mike.adairATccrs.nrcan.gc.ca
License:      GPL as per: http://www.gnu.org/copyleft/gpl.html

$Id: AoiBoxDHTML.js,v 1.1 2005/03/09 06:35:39 madair1 Exp $
*/

// Ensure this object's dependancies are loaded.
mapbuilder.loadScript(baseDir+"/tool/WidgetBase.js");

/**
 * Widget to draw an Area Of Interest box of a model.  The box can be drawn with
 * the paint() method and is registered as a listener of the context AOI property.
 * This object works entirely in pixel/line coordinate space and knows nothing
 * about geography.
 * @constructor
 * @base ToolBase
 * @param configNode The node for this object from the Config file.
 * @param model The model that contains this object.
 */
function AoiBoxDHTML(configNode, model) {
  var base = new WidgetBase(this, configNode, model);

  this.lineWidth = configNode.selectSingleNode("mb:lineWidth").firstChild.nodeValue; // Zoombox line width; pass in as param?
  this.lineColor = configNode.selectSingleNode("mb:lineColor").firstChild.nodeValue; // color of zoombox lines; pass in as param?
  this.crossSize = configNode.selectSingleNode("mb:crossSize").firstChild.nodeValue;

  /** Hide or show the box.
    * @param vis    boolean true for visible; false for hidden
    * @return       none
    */
  this.setVis = function(vis) {
    var visibility = "hidden";
    if (vis) visibility = "visible";
    this.Top.style.visibility = visibility;
    this.Left.style.visibility = visibility;
    this.Right.style.visibility = visibility;
    this.Bottom.style.visibility = visibility;
  }

  /** Listener to turn the box off
    * @param objRef  reference to this object
    * @return       none
    */
  this.clear = function(objRef) {
    objRef.setVis(false);
  }
  this.model.addListener("loadModel",this.clear, this);

  /** draw out the box.
    * if the box width or height is less than the cross size property, then the
    * drawCross method is called, otherwise call drawBox.
    */
  this.paint = function(thisTool) {
    var aoiBox = thisTool.model.getParam("aoi");widgetNode, model) {
    if (aoiBox) {
      var ul = thisTool.model.extent.getPL(aoiBox[0]);
      var lr = thisTool.model.extent.getPL(aoiBox[1]);
      //check if ul=lr, then draw cross, else drawbox
      if ( (Math.abs( ul[0]-lr[0] ) < thisTool.crossSize) && 
          (Math.abs( ul[1]-lr[1] ) < thisTool.crossSize) ) {
        thisTool.drawCross( new Array( (ul[0]+lr[0])/2, (ul[1]+lr[1])/2) );
      } else {
        thisTool.drawBox(ul, lr);
      }
    }
  }
  this.model.addListener("aoi",this.paint, this);
  this.model.addListener("refresh",this.paint, this);

  /** Draw a box.
    * @param ul Upper Left position as an (x,y) array in screen coords.
    * @param lr Lower Right position as an (x,y) array in screen coords.
    */
  this.drawBox = function(ul, lr) {
    this.Top.style.left = ul[0];
    this.Top.style.top = ul[1];
    this.Top.style.width = lr[0]-ul[0]
    this.Top.style.height = this.lineWidth;

    this.Left.style.left = ul[0];
    this.Left.style.top = ul[1];
    this.Left.style.width = this.lineWidth;
    this.Left.style.height = lr[1]-ul[1];

    this.Right.style.left = lr[0]-this.lineWidth;
    this.Right.style.top = ul[1];
    this.Right.style.width = this.lineWidth;
    this.Right.style.height = lr[1]-ul[1];

    this.Bottom.style.left = ul[0];
    this.Bottom.style.top = lr[1]-this.lineWidth;
    this.Bottom.style.width = lr[0]-ul[0];
    this.Bottom.style.height = this.lineWidth;

    this.setVis(true);
  }
    
  /** Draw a cross.
    * @param center The center of the cross as an (x,y) array in screen coordinates.
    */
  this.drawCross = function(center) {
    this.Top.style.left = Math.floor( center[0] - this.crossSize/2 );
    this.Top.style.top = Math.floor( center[1] - this.lineWidth/2 );
    this.Top.style.width = this.crossSize;
    this.Top.style.height = this.lineWidth;
    this.Top.style.visibility = "visible";

    this.Left.style.left = Math.floor( center[0] - this.lineWidth/2 );
    this.Left.style.top = Math.floor( center[1] - this.crossSize/2 );
    this.Left.style.width = this.lineWidth;
    this.Left.style.height = this.crossSize;
    this.Left.style.visibility = "visible";

    this.Right.style.visibility = "hidden";
    this.Bottom.style.visibility = "hidden";
  }
    
  /** Insert a <div> element into the parentNode html to hold the lines.
    * @return The new <div> node.
    */
  this.getImageDiv = function( parentNode ) {
    var newDiv = document.createElement("DIV");
    newDiv.innerHTML = "<IMG SRC='"+config.skinDir+"/images/Spacer.gif' WIDTH='1' HEIGHT='1'/>";
    newDiv.style.position = "absolute";
    newDiv.style.backgroundColor = this.lineColor;
    newDiv.style.visibility = "hidden";
    newDiv.style.zIndex = 300;
    parentNode.appendChild( newDiv );
    return newDiv;
  }

  /**
   * Called when the parent widget is painted to create the aoi box 
   * @param thisTool This object.
   */
  this.loadAoiBox = function(thisTool) {
    var containerNode = thisTool.parentWidget.node;//document.getElementById( thisTool.parentWidget.mbWidgetId );//
    thisTool.Top = thisTool.getImageDiv( containerNode );
    thisTool.Bottom = thisTool.getImageDiv( containerNode );
    thisTool.Left = thisTool.getImageDiv( containerNode );
    thisTool.Right = thisTool.getImageDiv( containerNode );
    thisTool.paint(thisTool);
  }
  this.loadAoiBox(this);

}
