package org.geoserver.wms;

import org.geoserver.catalog.CoverageInfo;
import org.geoserver.wms.wms_1_1_1.FilterMosaicGetMapTest;

/**
 * The following will provide data to test the default ImageMosaic cql filter
 * The added mosaic is used WITHOUT dimensions which are instead used as attributes.
 * 
 * @see {@link FilterMosaicGetMapTest}
 * 
 * @author carlo cancellieri
 *
 */
public class WMSFilterMosaicTestSupport extends WMSDimensionsTestSupport {
        
    /**
     * Here we setup the default mosaic filter to the layer 
     * @param metadata
     * @param presentation
     * @param resolution
     */
    protected void setupMosaicFilter(String filter) {
        CoverageInfo info = getCatalog().getCoverageByName(WATTEMP.getLocalPart());
        
        info.getParameters().put("Filter", filter);
        
        getCatalog().save(info);
    }
    
}
