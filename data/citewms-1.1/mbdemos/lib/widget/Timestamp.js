/*
License: GPL as per: http://www.gnu.org/copyleft/gpl.html
$Id: Timestamp.js,v 1.2 2005/02/21 05:10:57 madair1 Exp $
*/

// Ensure this object's dependancies are loaded.
mapbuilder.loadScript(baseDir+"/widget/WidgetBase.js");

/**
 * Adds a timstamp listener to refresh the output
 * @constructor
 * @author Mike Adair mike.adairATccrs.nrcan.gc.ca
 * @param toolNode      The tool node from the Config XML file.
 * @param model  The ButtonBar widget.
 */
function Timestamp(toolNode, model) {
  var base = new WidgetBase(this, toolNode, model);

  this.updateTimestamp = function (objRef, timestamp) {
    var inputEl = document.getElementById("timestampValue");
    inputEl.value = objRef.model.timestampList.childNodes[timestamp].firstChild.nodeValue;
  }

  this.model.addListener("timestamp",this.updateTimestamp, this);
}


