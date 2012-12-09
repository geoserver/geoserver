package org.geoserver.wcs2_0.kvp;

import java.util.List;
import java.util.Map;

import net.opengis.wcs20.DimensionSubsetType;
import net.opengis.wcs20.GetCoverageType;
import net.opengis.wcs20.Wcs20Factory;

import org.geoserver.ows.kvp.EMFKvpRequestReader;

/**
 * KVP reader for WCS 2.0 GetCoverage request
 * 
 * @author Andrea Aime - GeoSolutions
 */
public class WCS20GetCoverageRequestReader extends EMFKvpRequestReader {

    public WCS20GetCoverageRequestReader() {
        super(GetCoverageType.class, Wcs20Factory.eINSTANCE);
    }
    
    @Override
    public Object read(Object request, Map kvp, Map rawKvp) throws Exception {
        // TODO add support for all the extensions, which will all require us to setup
        // custom parsing code since there is no explicit fields to fill 
        GetCoverageType gc = (GetCoverageType) super.read(request, kvp, rawKvp);
        Object subsets = kvp.get("subset");
        if(subsets instanceof DimensionSubsetType) {
            gc.getDimensionSubset().add((DimensionSubsetType) subsets);
        } else if(subsets instanceof List) {
            for (Object subset : (List) subsets) {
                gc.getDimensionSubset().add((DimensionSubsetType) subset);
            }
        }
        
        
        return gc;
    }

    
}
