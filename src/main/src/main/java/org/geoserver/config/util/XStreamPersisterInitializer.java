/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config.util;

/**
 * Extension point allowing {@link XStreamPersister} customization, plug-ins can use this to add
 * aliases and custom converters for their own configuration objects
 *
 * @author Andrea Aime - GeoSolutions
 */
public interface XStreamPersisterInitializer {

    /** Allows {@link XStreamPersister} customizations on init */
    void init(XStreamPersister persister);
}
