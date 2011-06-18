/*
License: GPL as per: http://www.gnu.org/copyleft/gpl.html
$Id: Listener.js,v 1.13 2005/02/05 20:11:42 camerons Exp $
*/

/**
 * Base Listener class that is instanciated by all objects which provide
 * event listeners.
 * @constructor
 * @author Cameron Shorter
 */
function Listener(objRef) {
  /** An array [params] of (listener, target). */
  this.listeners=new Array();

  /**
   * An array [params] of values. These values might not be used if the set/get
   * functions are overwritten.
   */
  this.values=new Array();

  /**
   * Add a listener function which will be called when param is updated;  The
   * listener function should usually be: paint(target).
   * @param listener The function to call when the parameter changes.
   * @param target The object which owns the listener function.
   * @param param Parameter name; if this parameter changes then an event is
   * sent to all interested listeners.
   */
  this.addListener=function(param, listener, target) {
    if (window.logger) logger.logEvent("addListener: "+param,this.id,target.id);
    if(!this.listeners[param]){
      this.listeners[param]=new Array();
    }
    this.removeListener(param,listener,target);
    //for(var i=0;i<this.listeners[param].length;i++){
    //  if(this.listeners[param][i][0]==listener && this.listeners[param][i][1]==target){
    //    return;
    //    alert("Listener.addListener again: target="+target.id);
    //  }
    //}
    this.listeners[param].push(new Array(listener,target));
  }

  /**
   * Add a listener function at the start of the list.  
   * @param listener The function to call when the parameter changes.
   * @param target The object which owns the listener function.
   * @param param Parameter name; if this parameter changes then an event is
   * sent to all interested listeners.
   */
  this.addFirstListener=function(param, listener, target) {
    if (window.logger) logger.logEvent("addFirstListener: "+param,this.id,target.id);
    if(!this.listeners[param]){
      this.listeners[param]=new Array();
    }
    this.removeListener(param,listener,target);
    this.listeners[param].unshift(new Array(listener,target));
  }

  /**
   * Remove a listener so that it is not called anymore when a param changes.
   * @param listener The function to call when the parameter changes.
   * @param target The object which owns the listener function.
   * @param param Parameter name; if this parameter changes then an event is
   * sent to all interested listeners.
   */
  this.removeListener=function(param,listener,target){
    if(this.listeners[param]){
      for(var i=0;i<this.listeners[param].length;i++){
        if(this.listeners[param][i][0]==listener && this.listeners[param][i][1]==target){
          for(var j=i;i<this.listeners[param].length-1;i++){
            this.listeners[param][j]=this.listeners[param][j+1];
          }
          this.listeners[param].pop();
          return;
        }
      }
    }
  }

  /**
   * Call all the listeners that have registered interest in this parameter
   * using addListener.
   * @param param The parameter that has changed.
   * @param value The new parameter value.
   */
  this.callListeners=function(param,value) {
    if (this.listeners[param]){
      var count = this.listeners[param].length;
      for(var i=0;i<count;i++){
        if (window.logger) logger.logEvent(param,this.id,this.listeners[param][i][1].id,value);
        //this is listenerFunction(target,value)
        this.listeners[param][i][0](this.listeners[param][i][1],value);
      }
    }
  }

  /**
   * Update parameter and call all interested listeners.  This function may
   * be overloaded to store the parameter in a elsewhere (eg in an XML
   * document).
   * @param param The parameter to change.
   * @parma value The new value of the param.
   */
  this.setParam=function(param,value) {
    this.values[param] = value;

    // Call all the interested listeners
    this.callListeners(param,value);
  }

  /**
   * Return the param value, or return null if it does not exist.  This
   * function may be overloaded to store the param elsewhere (eg in
   * an XML document).
   * @param param The parameter to get.
   * @return The value of the param.
   */
  this.getParam=function(param) {
    return this.values[param];
  }

  for (sProperty in this) { 
    objRef[sProperty] = this[sProperty]; 
  } 
}
