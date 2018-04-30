/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs;

import java.util.List;

import org.geoserver.catalog.DimensionInfo;
import org.geoserver.config.ServiceInfo;
import org.geotools.coverage.grid.io.OverviewPolicy;

/**
 * Service configuration object for Web Coverage Service.
 * 
 * @author Justin Deoliveira, The Open Planning Project
 * 
 */
public interface WCSInfo extends ServiceInfo {

    /**
     * Flag determining if gml prefixing is used.
     */
    boolean isGMLPrefixing();

    /**
     * Sets flag determining if gml prefixing is used.
     */
    void setGMLPrefixing(boolean gmlPrefixing);
    
    /**
     * Returns the maximum input size, in kilobytes. The input size is computed as the amount of
     * bytes needed to fully store in memory the input data, {code}width x heigth x pixelsize{code}
     * (whether that memory will actually be fully used or not depends on the data source) 
     * A negative or null value implies there is no input limit.
     */
    long getMaxInputMemory();
    
    /**
     * Sets the maximum input size. See {@link #getMaxInputMemory()}
     */
    void setMaxInputMemory(long size);
    
    /**
     * Returns the maximum output size, in kilobytes. The output size is computed as the amount
     * of bytes needed to store in memory the resulting image {code}width x heigth x pixelsize{code}.
     * Whether that memory will be used or not depends on the data source as well as the output format.
     * A negative or null value implies there is no output limit.
     */
    long getMaxOutputMemory();
    
    /**
     * 
     * @param size
     */
    void setMaxOutputMemory(long size);
    
    /**
     * Returns the overview policy used when returning WCS data
     */
    OverviewPolicy getOverviewPolicy();
    
    /**
     * Sets the overview policyt to be used when processing WCS data
     * @param policy
     */
    void setOverviewPolicy(OverviewPolicy policy);
    
    /**
     * Enables the use of subsampling
     *
     */
    boolean isSubsamplingEnabled();
    
    /**
     * Enableds/disables the use of subsampling during the coverage reads
     */
    public void setSubsamplingEnabled(boolean enabled);
    
    /**
     * Allows users to request data in lat-lon order.
     * 
     * <p>
     * Default to <code>false</code>.
     * 
     * @param latLon <code>true</code> for lat-lon order, <code>false</code> otherwise.
     * 
     */
    public void setLatLon(boolean latLon);
    
    /**
     * Tells me whether we should spit out data in lat-lon or lon-lat order.
     * 
     * @return <code>true</code> for lat-lon order, <code>false</code> otherwise.
     */
    public boolean isLatLon();
    
    /**
     * The srs's that the wcs service supports (not all versions of WCS support this)
     */
    List<String> getSRS();

    /**
     * Returns the maximum number of dimension items that can be requested by a client without
     * getting a service exception. The default is {DimensionInfo#DEFAULT_MAX_REQUESTED_DIMENSION_VALUES}
     * that is, no limit.
     * @return
     */
    default int getMaxRequestedDimensionValues() {
        return DimensionInfo.DEFAULT_MAX_REQUESTED_DIMENSION_VALUES;
    }

    /**
     * Sets the maximum number of dimension items that can be requested by a client without. 
     * Zero or negative will disable the limit.
     * @param maxRequestedDimensionValues Any integer number
     */
    default void setMaxRequestedDimensionValues(int maxRequestedDimensionValues) {
        // if not implemented nothing is done
    }
}
