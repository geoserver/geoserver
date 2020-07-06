/*
License: GPL as per: http://www.gnu.org/copyleft/gpl.html
Dependancies: Context
$Id: GetFeatureInfo.js,v 1.2 2004/12/15 18:16:49 madair1 Exp $
*/

// Ensure this object's dependancies are loaded.
mapbuilder.loadScript(baseDir+"/tool/ButtonBase.js");

/**
 * Implements WMS GetFeatureInfo functionality, popping up a query result
 * window when user clicks on map.
 * @constructor
 * @base ButtonBase
 * @author Nedjo
 * @constructor
 * @param toolNode The XML node in the Config file referencing this object.
 * @param model The widget object which this tool is associated with.
 */
function GetFeatureInfo(toolNode, model) {
  // Extend ButtonBase
  var base = new ButtonBase(this, toolNode, model);

  /** Xsl to build a GetFeatureInfo URL */
  this.xsl=Sarissa.getDomDocument();
  this.xsl.async=false;
  this.xsl.load(baseDir+"/tool/GetFeatureInfo.xsl");

  /** Determine whether Query result is returned as HTML or GML */
  // TBD This should be stored in the Config file.
  this.infoFormat="application/vnd.ogc.gml";
  //this.infoFormat="text/html";

  /**
   * Open window with query info.
   * This function is called when user clicks map with Query tool.
   * @param objRef      Pointer to this GetFeatureInfo object.
   * @param targetNode  The node for the enclosing HTML tag for this widget.
   */

  this.doAction = function(objRef,targetNode) {
    if (objRef.enabled) {
      var queryLayer=objRef.targetModel.getParam("queryLayer");
      if (queryLayer==null) {
        alert("Query layer not selected, select a queryable layer in the Legend.");
      }
      else {
        Sarissa.setXslParameter(
          objRef.xsl,
          "queryLayer", "'"+queryLayer+"'");
        Sarissa.setXslParameter(
          objRef.xsl,
          "xCoord", "'"+targetNode.evpl[0]+"'");
        Sarissa.setXslParameter(
          objRef.xsl,
          "yCoord", "'"+targetNode.evpl[1]+"'");
        Sarissa.setXslParameter(
          objRef.xsl,
          "infoFormat", "'"+objRef.infoFormat+"'");
        Sarissa.setXslParameter(
          objRef.xsl,
          "featureCount", "'1'");

        urlNode=Sarissa.getDomDocument();
        objRef.targetModel.doc.transformNodeToObject(objRef.xsl,urlNode);

        if (objRef.infoFormat=="text/html"){
          alert("url="+url);
          window.open(url,'queryWin','height=200,width=300,scrollbars=yes');
        }else{
          url=objRef.targetModel.getProxyPlusUrl(urlNode.documentElement.firstChild.nodeValue);
          alert("url="+url);
          objRef.targetModel.loadModelDoc(url);
        }
      }
    }
  }

  if (this.mouseHandler) {
    this.mouseHandler.addListener('mouseup',this.doAction,this);
  }
}
