/*
License: GPL as per: http://www.gnu.org/copyleft/gpl.html
$Id: MapbuilderServerLoad.js,v 1.1 2005/02/05 19:24:40 madair1 Exp $
*/

/** URL of Mapbuilder's lib/ directory. */
var baseDir;

function Mapbuilder() {
  /**
   * Dynamically load a script file if it has not already been loaded.
   * @param url The url of the script.
   */
  this.loadScript=function(url){
    //no-op so that dynamic verison doesn't break
  }

  /**
   * Internal function to load scripts for components that don't have <scriptfile>
   * specified in the config file.
   * @param xPath Xpath match of components from the Config file.
   * @param dir The directory the script is located in.
   */
  this.loadScriptsFromXpath=function(xPath,dir) {
    var nodes = this.doc.selectNodes(xPath);
    for (var i=0; i<nodes.length; i++) {
      if (nodes[i].selectSingleNode("mb:scriptFile")==null){
        scriptFile = baseDir+"/"+dir+nodes[i].nodeName+".js";
        this.loadScript(scriptFile);
      }
    }
  }

  //derive the baseDir value by looking for the script tag that loaded this file
  var head = document.getElementsByTagName('head')[0];
  var nodes = head.childNodes;
  for (var i=0; i<nodes.length; ++i ){
    var src = nodes.item(i).src;
    if (src) {
      var index = src.indexOf("/MapbuilderServerLoad.js");
      if (index>=0) {
        baseDir = src.substring(0, index);
      }
    }
  }
}

var mapbuilder=new Mapbuilder();

/**
 * Mapbuilder's main initialisation script.
 * This should be called from the main html file using:
 *   <body onload="mbDoLoad()">
 */
function mbDoLoad() {
  config.init(config);
  var mbTimerStop = new Date();
  //alert("load time:"+(mbTimerStop.getTime()-mbTimerStart.getTime()) );
  config.callListeners("loadModel");
}
