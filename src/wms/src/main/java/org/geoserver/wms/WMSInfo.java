/* Copyright (c) 2010 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wms;

import java.util.List;

import org.geoserver.config.ServiceInfo;

/**
 * Configuration object for Web Map Service.
 * 
 * @author Justin Deoliveira, The Open Planning Project
 * 
 */
public interface WMSInfo extends ServiceInfo {

    enum WMSInterpolation {
        Nearest, Bilinear, Bicubic
    }

    /**
     * The watermarking configuration.
     */
    WatermarkInfo getWatermark();

    /**
     * Sets the watermarking configuration.
     */
    void setWatermark(WatermarkInfo watermark);

    WMSInterpolation getInterpolation();

    void setInterpolation(WMSInterpolation interpolation);

    /**
     * The srs's that the wms service supports.
     */
    List<String> getSRS();

    /**
     * The maximum search radius for GetFeatureInfo
     */
    int getMaxBuffer();

    /**
     * Sets the maximum search radius for GetFeatureInfo (if 0 or negative no maximum is enforced)
     */
    void setMaxBuffer(int buffer);

    /**
     * Returns the max amount of memory, in kilobytes, that each WMS request can allocate (each
     * output format will make a best effort attempt to respect it, but there are no guarantees)
     * 
     * @return the limit, or 0 if no limit
     */
    int getMaxRequestMemory();

    /**
     * Sets the max amount of memory, in kilobytes, that each WMS request can allocate. Set it to 0
     * if no limit is desired.
     */
    void setMaxRequestMemory(int max);

    /**
     * The max time, in seconds, a WMS request is allowed to spend rendering the map. Various output
     * formats will do a best effort to respect it (raster formats, for example, will account just
     * rendering time, but not image encoding time)
     */
    int getMaxRenderingTime();

    /**
     * Sets the max allowed rendering time, in seconds
     * 
     * @param maxRenderingTime
     */
    void setMaxRenderingTime(int maxRenderingTime);

    /**
     * The max number of rendering errors that will be tolerated before stating the rendering
     * operation failed by throwing a service exception back to the client
     */
    int getMaxRenderingErrors();

    /**
     * Sets the max number of rendering errors tolerated
     * 
     * @param maxRenderingTime
     */
    void setMaxRenderingErrors(int maxRenderingTime);
}
