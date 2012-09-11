/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
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

    boolean canonicalSchemaLocation;

    @Override
    public boolean isCanonicalSchemaLocation() {
        return canonicalSchemaLocation;
    }

    @Override
    public void setCanonicalSchemaLocation(boolean canonicalSchemaLocation) {
        this.canonicalSchemaLocation = canonicalSchemaLocation;
    }

}