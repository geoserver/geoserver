/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.threadlocals;

import java.util.Map;

/**
 * Implementors of this interface allow to transfer thread locals from the current thread to another
 * one. They are used to make sure important thread local values are transferred into thread
 * pools/scheduled tasks/delayed actions executing portions of the GeoServer work
 *
 * @author Andrea Aime - GeoSolutions
 */
public interface ThreadLocalTransfer {

    /**
     * Collects the current thread locals values into the storage. The implementation must try hard
     * to use a unique name for each key (a good choice is
     * fullyQualifiedClassName#thredLocalFieldName), the caller utility will make sure there are no
     * conflicts (in case of conflict an exception will be thrown)
     */
    void collect(Map<String, Object> storage);

    /** Set the thread local values in the current thread */
    void apply(Map<String, Object> storage);

    /** Clean up the thread locals in the current thread */
    void cleanup();
}
