/*
License: GPL as per: http://www.gnu.org/copyleft/gpl.html
$Id: ContextCollection.js,v 1.9 2005/03/01 04:49:25 madair1 Exp $
*/

/**
 * Stores a Web Map Context (WMC) Collection document as defined by the Open 
 * GIS Consortium http://opengis.org and extensions the the WMC.  
 * @constructor
 * @base ModelBase
 * @author Mike Adair
 * @param modelNode Pointer to the xml node for this model from the config file.
 * @param parent    The parent model for the object.
 */
function ContextCollection(modelNode, parent) {
  // Inherit the ModelBase functions and parameters
  var modelBase = new ModelBase(this, modelNode, parent);

  /** Insert a new context.
    * @param context An XML node which describes the context.
    * @param zindex The position to insert this context in the contextList, if set
    * to null this context will be inserted at the end.
    * @return The identifier string used to reference this context.
    */
  this.insertContext=function(context,zindex){
    //TBD Fill this in.
  }

  /** Delete this context.
   * @param id The context identifier.
   */
  this.deleteContext=function(id){
    //TBD Fill this in.
  }

  /** Move this context to a new position in the contextList.
    * @param context The context id to move.
    * @param zindex The position to move this context to in the contextList, if set
    * to null this context will be inserted at the end.
    */
  this.reorderContext=function(context,zindex){
    //TBD Fill this in.
  }

  /** Select this context for further operations 
    * @param context The context id to select.
    * @param selected Set to true/false.
    */
  this.selectContext=function(context,selected){
    for(var i=0;i<this.listeners["select"].length;i++) {
      this.listeners["select"][i][0](context,this.listeners["select"][i][1]);
    }
  }

}
