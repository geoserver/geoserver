/* (c) 2014-2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config;

import java.io.Serializable;
import org.eclipse.imagen.ImageN;
import org.eclipse.imagen.TileCache;

/**
 * Image processing settings, primarily used to configure Eclipse ImageN.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public interface ImageProcessingInfo extends Cloneable, Serializable {

    static enum PngEncoderType {
        JDK,
        PNGJ
    }

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

    /** The jai instance. */
    ImageN getJAI();

    void setJAI(ImageN imagen);

    /** The jai tile cache. */
    TileCache getTileCache();

    void setTileCache(TileCache tileCache);

    public ImageProcessingInfo clone();
}
