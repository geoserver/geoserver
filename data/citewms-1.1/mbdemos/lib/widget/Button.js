/*
License: GPL as per: http://www.gnu.org/copyleft/gpl.html
$Id: Button.js,v 1.1 2005/02/14 05:15:35 madair1 Exp $
*/

// Ensure this object's dependancies are loaded.
mapbuilder.loadScript(baseDir+"/widget/ButtonBase.js");

/**
 * Generic Button object.  Set the <action> property in config for the controller
 * method to be called when selected
 * @base ButtonBase
 * @author Mike Adair mike.adairATccrs.nrcan.gc.ca
 * @param toolNode      The tool node from the Config XML file.
 * @param model  The ButtonBar widget.
 */
function Button(toolNode, model) {
  var base = new ButtonBase(this, toolNode, model);

}


