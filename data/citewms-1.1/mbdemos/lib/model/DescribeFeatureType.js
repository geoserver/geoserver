/*
License: GPL as per: http://www.gnu.org/copyleft/gpl.html
$Id: DescribeFeatureType.js,v 1.6 2005/03/01 04:49:25 madair1 Exp $
*/

/**
 * Stores a WFS DescribeFeatureType request reponse as defined by the Open GIS Consortium
 * http://opengis.org (WFS).  
 *
 * @constructor
 * @base ModelBase
 * @author Mike Adair
 * @param modelNode Pointer to the xml node for this model from the config file.
 * @param parent    The parent model for the object.
 */
function DescribeFeatureType(modelNode, parent) {
  // Inherit the ModelBase functions and parameters
  var modelBase = new ModelBase(this, modelNode, parent);
}

