/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config;

import java.io.Serializable;
import java.util.Map;

/**
 * Information about a particular image format.
 *
 * @author Justin Deoliveira, The Open Planning Project
 */
public interface ImageFormatInfo {

    /** Identifier. */
    String getId();

    /** The mime type of the image format. */
    String getMimeType();

    void setMimeType(String mimeType);

    /** @uml.property name="antiAliasing" */
    boolean isAntiAliasing();

    /** @uml.property name="antiAliasing" */
    void setAntiAliasing(boolean antiAliasing);

    /** @uml.property name="nativeAcceleration" */
    boolean isNativeAcceleration();

    /** @uml.property name="nativeAcceleration" */
    void setNativeAcceleration(boolean nativeAcceleration);

    /** @uml.property name="metadata" */
    Map<String, Serializable> getMetadata();

    Map<Object, Object> getClientProperties();
}
