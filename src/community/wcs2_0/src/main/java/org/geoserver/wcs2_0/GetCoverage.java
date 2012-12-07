package org.geoserver.wcs2_0;

import java.io.IOException;

import net.opengis.wcs20.GetCoverageType;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.wcs.CoverageCleanerCallback;
import org.geoserver.wcs.WCSInfo;
import org.geoserver.wcs2_0.exception.WCS20Exception;
import org.geoserver.wcs2_0.util.NCNameResourceCodec;
import org.opengis.coverage.grid.GridCoverage;
import org.opengis.coverage.grid.GridCoverageReader;
import org.vfny.geoserver.wcs.WcsException;

/**
 * Implementation of the WCS 2.0 GetCoverage request
 * 
 * @author Andrea Aime - GeoSolutions
 */
public class GetCoverage {

    private WCSInfo wcs;
    private Catalog catalog;

    public GetCoverage(WCSInfo serviceInfo, Catalog catalog) {
        this.wcs = serviceInfo;
        this.catalog = catalog;
    }

    public GridCoverage run(GetCoverageType request) {
        // get the coverage 
        LayerInfo linfo = NCNameResourceCodec.getCoverage(catalog, request.getCoverageId());
        if(linfo == null) {
            throw new WCS20Exception("Could not locate coverage " + request.getCoverageId(), 
                    WCS20Exception.WCSExceptionCode.NoSuchCoverage, "coverageId");
        } 
        
        // TODO: handle trimming and slicing
        
        CoverageInfo cinfo = (CoverageInfo) linfo.getResource();
        GridCoverage coverage = null;
        try {
            GridCoverageReader reader = cinfo.getGridCoverageReader(null, null);
            
            // use this to check if we can use overviews or not
            boolean subsample = wcs.isSubsamplingEnabled();
            
            // TODO: setup the params to force the usage of imageread and to make it use
            // the right overview and so on
            coverage = reader.read(null);
            
            // TODO: handle crop, scale, reproject and so on
        } catch(IOException e) {
            throw new WcsException("Failed to read the coverage " + request.getCoverageId(), e);
        } finally {
            // make sure the coverage will get cleaned at the end of the processing
            if(coverage != null) {
                CoverageCleanerCallback.addCoverages(coverage);
            }
        }
        
        return coverage;
    }
    
    

}
