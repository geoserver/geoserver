/* Copyright (c) 2001 - 2008 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config;

import javax.media.jai.JAI;

import com.sun.media.jai.util.SunTileCache;

/**
 * Java Advanced Imaging configuration.
 * 
 * @author Justin Deoliveira, OpenGeo
 *
 */
public interface JAIInfo {

    /**
     * Flag controlling image interpolation.
     */
    boolean getAllowInterpolation();
    void setAllowInterpolation(boolean allowInterpolation);

    /**
     * Flag controlling the recycling of image tiles during jai operations.
     */
    boolean isRecycling();
    void setRecycling(boolean recycling);

    /**
     * The priority of the jai processing thread.
     */
    int getTilePriority();
    void setTilePriority(int tilePriority);

    /**
     * The number of threads allocated for jai image processing. 
     */
    int getTileThreads();
    void setTileThreads(int tileThreads);

    /**
     * The maximum percentage of memory allocated to jai for image processing. 
     */
    double getMemoryCapacity();
    void setMemoryCapacity(double memoryCapacity);

    /**
     * @uml.property name="memoryThreshold"
     */
    double getMemoryThreshold();
    void setMemoryThreshold(double memoryThreshold);

    /**
     * Flag controlling native PNG image processing.
     */
    boolean isPngAcceleration();
    void setPngAcceleration(boolean pngAcceleration);

    /**
     * Flag controlling native JPEG image processing.
     */
    boolean isJpegAcceleration();
    void setJpegAcceleration(boolean jpegAcceleration);

    /**
     * Flag controlling native mosaicing operations.
     */
    boolean isAllowNativeMosaic();
    void setAllowNativeMosaic(boolean allowNativeMosaic);
    
    /**
     * Flag controlling the image io cache.
     * @deprecated Replaced by {@link CoverageAccessInfo#getImageIOCacheThreshold()}
     */
    void setImageIOCache(boolean imageIOCache);
    /**
     * @deprecated
     */
    boolean isImageIOCache();

    /**
     * The jai instance.
     */
    JAI getJAI();
    void setJAI(JAI jai);

    /**
     * The jai tile cache.
     */
    SunTileCache getTileCache();
    void setTileCache(SunTileCache tileCache);
}
