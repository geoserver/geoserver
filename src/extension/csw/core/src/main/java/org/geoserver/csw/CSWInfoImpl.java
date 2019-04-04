/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.csw;

import org.geoserver.config.impl.ServiceInfoImpl;

/**
 * CSW information implementation
 *
 * @author Andrea Aime, GeoSolutions
 */
@SuppressWarnings("unchecked")
public class CSWInfoImpl extends ServiceInfoImpl implements CSWInfo {

    /** */
    private static final long serialVersionUID = -986573241436434750L;

    boolean canonicalSchemaLocation;

    public CSWInfoImpl() {}

    @Override
    public boolean isCanonicalSchemaLocation() {
        return canonicalSchemaLocation;
    }

    @Override
    public void setCanonicalSchemaLocation(boolean canonicalSchemaLocation) {
        this.canonicalSchemaLocation = canonicalSchemaLocation;
    }
}
