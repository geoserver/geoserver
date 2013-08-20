package org.geoserver.map.png;

import org.geotools.styling.AbstractStyleVisitor;
import org.geotools.styling.ColorMap;

/**
 * Check if the style contains a "high change" raster symbolizer, that is, one that generates
 * a continuous set of values for which SUB filtering provides better results
 *
 * @author Andrea Aime - GeoSolutions
 */
class RasterSymbolizerVisitor extends AbstractStyleVisitor {

    boolean highChangeRasterSymbolizer;
    
    public void visit(org.geotools.styling.RasterSymbolizer raster) {
        if(raster.getColorMap() == null) {
            highChangeRasterSymbolizer = true;
            return;
        } 
        
        int cmType = raster.getColorMap().getType();
        if(cmType != ColorMap.TYPE_INTERVALS && cmType != ColorMap.TYPE_VALUES) {
            highChangeRasterSymbolizer = true;
        }
    }
}
