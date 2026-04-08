/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo.security;

import java.io.Serializable;
import java.util.List;

/** Base class for EO access limit configuration, with elements shared amongst Collections and Products. */
public interface EOAccessLimitInfo extends Serializable, Cloneable {

    /** The CQL filter matching the collections/products the user is allowed to see */
    String getCQLFilter();

    /** Sets the CQL filter matching the collections/products the user is allowed to see */
    void setCQLFilter(String cqlFilter);

    /**
     * The roles allowed to access the collections/products matched by the CQL filter. This is a live collection, it can
     * be modified directly.
     */
    List<String> getRoles();
}
