/* (c) 2014-2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config;

import java.io.Serializable;
import javax.media.jai.JAI;
import javax.media.jai.TileCache;

/**
 * Java Advanced Imaging configuration.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public interface JAIInfo extends Cloneable, Serializable {

    static enum PngEncoderType {
        JDK,
        NATIVE,
        PNGJ
    };

    /** Flag controlling image interpolation. */
    boolean getAllowInterpolation();

    void setAllowInterpolation(boolean allowInterpolation);

    /** Flag controlling the recycling of image tiles during jai operations. */
    boolean isRecycling();

    void setRecycling(boolean recycling);

    /** The priority of the jai processing thread. */
    int getTilePriority();

    void setTilePriority(int tilePriority);

    /** The number of threads allocated for jai image processing. */
    int getTileThreads();

    void setTileThreads(int tileThreads);

    /** The maximum percentage of memory allocated to jai for image processing. */
    double getMemoryCapacity();

    void setMemoryCapacity(double memoryCapacity);

    /** @uml.property name="memoryThreshold" */
    double getMemoryThreshold();

    void setMemoryThreshold(double memoryThreshold);

    PngEncoderType getPngEncoderType();

    void setPngEncoderType(PngEncoderType type);

    /** Flag controlling native JPEG image processing. */
    boolean isJpegAcceleration();

    void setJpegAcceleration(boolean jpegAcceleration);

    /** Flag controlling native mosaicing operations. */
    boolean isAllowNativeMosaic();

    void setAllowNativeMosaic(boolean allowNativeMosaic);

    /** Flag controlling native warping operations. */
    boolean isAllowNativeWarp();

    void setAllowNativeWarp(boolean allowNativeWarp);

    /** The jai instance. */
    JAI getJAI();

    void setJAI(JAI jai);

    /** The jai tile cache. */
    TileCache getTileCache();

    void setTileCache(TileCache tileCache);

    /** JAI-EXT section */
    JAIEXTInfo getJAIEXTInfo();

    void setJAIEXTInfo(JAIEXTInfo jaiext);

    public JAIInfo clone();
}
