/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

/**
 * GeoServer imaging configuration.
 *
 * @author Justin Deoliveira, The Open Planning Project
 */
public interface ImagingInfo {

    /** Identifer. */
    String getId();

    /**
     * @return Returns the imageFormats.
     * @uml.property name="imageFormats"
     * @uml.associationEnd multiplicity="(0 -1)"
     *     inverse="imagingInfo:org.geoserver.config.ImageFormatInfo"
     */
    Collection<ImageFormatInfo> getImageFormats();

    /**
     * Returns the image format corresponding to the specified mime type, or <code>null</code> if it
     * does exist.
     */
    ImageFormatInfo getImageFormatByMimeType(String mimeType);

    /** @uml.property name="allowInterpolation" */
    boolean getAllowInterpolation();

    /** @uml.property name="allowInterpolation" */
    void setAllowInterpolation(boolean allowInterpolation);

    /** @uml.property name="recycling" */
    boolean getRecycling();

    /** @uml.property name="recycling" */
    void setRecycling(boolean recycling);

    /** @uml.property name="tilePriority" */
    int getTilePriority();

    /** @uml.property name="tilePriority" */
    void setTilePriority(int tilePriority);

    /** @uml.property name="tileThreads" */
    int getTileThreads();

    /** @uml.property name="tileThreads" */
    void setTileThreads(int tileThreads);

    String getTileCache();

    void setTileCache(String tileCache);

    /** @uml.property name="memoryCapacity" */
    double getMemoryCapacity();

    /** @uml.property name="memoryCapacity" */
    void setMemoryCapacity(double memoryCapacity);

    /** @uml.property name="memoryThreshold" */
    double getMemoryThreshold();

    /** @uml.property name="memoryThreshold" */
    void setMemoryThreshold(double memoryThreshold);

    /** @uml.property name="metadata" */
    Map<String, Serializable> getMetadata();

    Map<Object, Object> getClientProperties();
}
