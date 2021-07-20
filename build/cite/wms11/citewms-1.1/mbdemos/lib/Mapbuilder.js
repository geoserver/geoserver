/*
License: GPL as per: http://www.gnu.org/copyleft/gpl.html
$Id: Mapbuilder.js,v 1.31 2005/02/21 05:11:23 madair1 Exp $
*/

/** get a time stamp at start of the page load */
var mbTimerStart = new Date();

/** the global config object */
var config;

/** URL of Mapbuilder's lib/ directory. */
var baseDir;

// LoadState Constants
var MB_UNLOADED=0;    // Scripts not loaded yet
var MB_LOAD_CORE=1;   // Loading scripts loaded defined in Mapbuilder
var MB_LOAD_CONFIG=2; // Loading scripts loaded defined in Config
var MB_LOADED=3;      // All scripts loaded

/**
 * This Object bootstraps the Mapbuilder libraries by loading the core
 * script files.
 * When Config.js is loaded, the script files for objects described in the
 * Mapbuilder config file are loaded.
 * Objects which have dependencies will trigger the dependancies to load
 * when they are loaded.
 *
 * @constructor
 * @author Cameron Shorter
 * @requires Config
 * @requires Listener
 * @requires ModelBase
 * @requires Sarissa
 * @requires Util
 */
function Mapbuilder() {

  /**
   * Determines which Mapbuilder scripts are loading.
   * TBD: Is it possible to use enumerated types in JS?
   */
  this.loadState=MB_UNLOADED;

  /** Array of objects that are loading.  Don't continue initialisation until
   * all objects have loaded. */
  this.loadingScripts=new Array();

  /** Timer to periodically check if scripts have loaded. */
  this.scriptsTimer=null;

  /**
   * Called periodically and moves onto the next loadState when this round of
   * scripts have loaded.
   * For IE clients, object.readyState is used to check if scripts are loaded.
   * Mozilla works fine without this function - I think it is single threaded.
   */
  this.checkScriptsLoaded=function() {
    if (document.readyState!=null){
      // IE client

      // Scripts are removed from array when they have loaded
      while(this.loadingScripts.length>0
        &&(this.loadingScripts[0].readyState=="loaded"
          ||this.loadingScripts[0].readyState=="complete"
          ||this.loadingScripts[0].readyState==null))
      {
        this.loadingScripts.shift();
      }
      if (this.loadingScripts.length==0){
        this.setLoadState(this.loadState+1);
      }
    }
    else{
      // Mozilla client
      if(this.loadState==MB_LOAD_CORE && config!=null){
        // Config has finished loading
        this.setLoadState(MB_LOAD_CONFIG);
      }
    }
  }

  /**
   * Move onto loading the next set of scripts.
   * @param newState The new loading state.
   */
  this.setLoadState=function(newState){
    this.loadState=newState;
    switch (newState){
      case MB_LOAD_CORE:
        this.loadScript(baseDir+"/util/sarissa/Sarissa.js");
        this.loadScript(baseDir+"/util/Util.js");
        this.loadScript(baseDir+"/util/Listener.js");
        this.loadScript(baseDir+"/model/ModelBase.js");
        this.loadScript(baseDir+"/model/Config.js");
        break;
      case MB_LOAD_CONFIG:
        if(document.readyState){
          // IE
          config=new Config(mbConfigUrl);
          config.loadConfigScripts();
        }else{
          // Mozilla
          this.setLoadState(MB_LOADED);
        }
        break;
      case MB_LOADED:
        clearInterval(this.scriptsTimer);
        break;
    }
  }

  /**
   * Dynamically load a script file if it has not already been loaded.
   * @param url The url of the script.
   */
  this.loadScript=function(url){
    if(!document.getElementById(url)){
      var script = document.createElement('script');
      script.defer = false;   //not sure of effect of this?
      script.type = "text/javascript";
      script.src = url;
      script.id = url;
      document.getElementsByTagName('head')[0].appendChild(script);
      this.loadingScripts.push(script);
    }
  }

  /**
   * Internal function to load scripts for components that don't have <scriptfile>
   * specified in the config file.
   * @param xPath Xpath match of components from the Config file.
   * @param dir The directory the script is located in.
   */
  this.loadScriptsFromXpath=function(nodes,dir) {
    //var nodes = this.doc.selectNodes(xPath);
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
      var index = src.indexOf("/Mapbuilder.js");
      if (index>=0) {
        baseDir = src.substring(0, index);
      }
    }
  }

  // Start loading core scripts.
  this.setLoadState(MB_LOAD_CORE);

  // Start a timer which periodically calls checkScriptsLoaded().
  this.scriptsTimer=setInterval('mapbuilder.checkScriptsLoaded()',100);
}

var mapbuilder=new Mapbuilder();

/**
 * Initialise Mapbuilder if script has been loaded, else wait to be called
 * again.
 */
function mapbuilderInit(){
  if(mapbuilder && mapbuilder.loadState==MB_LOADED){
    clearInterval(mbTimerId);
    config.init(config);
    var mbTimerStop = new Date();
    //alert("load time:"+(mbTimerStop.getTime()-mbTimerStart.getTime()) );
    config.callListeners("loadModel");
    config.callListeners("refresh");
  }
}

/** Timer used when checking if scripts have loaded. */
var mbTimerId;

/**
 * Mapbuilder's main initialisation script.
 * This should be called from the main html file using:
 *   <body onload="mbDoLoad()">
 */
function mbDoLoad() {
  // See if scripts have been loaded every 100msecs, then call config.init().
  mbTimerId=setInterval('mapbuilderInit()',100);
}
