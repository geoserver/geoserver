/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config;

import org.geoserver.catalog.MetadataLinkInfo;

/**
 * Factory used to create geoserver configuration objects.
 *
 * @author Justin Deoliveira, The Open Planning Project
 */
public interface GeoServerFactory {

    /** Creates a new configuration. */
    GeoServerInfo createGlobal();

    /** Creates a new settings. */
    SettingsInfo createSettings();

    /** Creates a new contact. */
    ContactInfo createContact();

    /** Creates a new jai. */
    JAIInfo createJAI();

    /** Creates a new metadata link. */
    MetadataLinkInfo createMetadataLink();

    /** Creates a new Imaging. */
    // ImagingInfo createImaging();

    /** Creates a new image format. */
    // ImageFormatInfo createImageFormat();

    /** Creates a new service. */
    ServiceInfo createService();

    /** Creates a new logging. */
    LoggingInfo createLogging();

    /**
     * Extensible factory method.
     *
     * <p>This method should lookup the appropritae instance of {@link Extension} to create the
     * object. The lookup mechanism is specific to the runtime environement.
     *
     * @param clazz The class of object to create.
     * @return The new object.
     */
    <T extends Object> T create(Class<T> clazz);

    /** Factory extension. */
    interface Extension {

        /**
         * Determines if the extension can create objects of the specified class.
         *
         * @param clazz The class of object to create.
         */
        <T extends Object> boolean canCreate(Class<T> clazz);

        /**
         * Creates an instance of the specified class.
         *
         * <p>This method is only called if {@link #canCreate(Class)} returns <code>true</code>.
         *
         * @param clazz The class of object to create.
         * @return The new object.
         */
        <T extends Object> T create(Class<T> clazz);
    }
}
