/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import org.apache.commons.lang3.SystemUtils;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geoserver.test.TestSetupFrequency;

/**
 * Test support class providing additional accessors for security related beans.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class GeoServerSecurityTestSupport extends GeoServerSystemTestSupport {

    /** Accessor for the geoserver master password. */
    protected String getMasterPassword() {
        return new String(getSecurityManager().getMasterPassword());
    }

    @Override
    protected TestSetupFrequency lookupTestSetupPolicy() {
        // if it's windows the file system locks are causing random failures,
        // switch to a full re-setup of the data directory instead
        if (SystemUtils.IS_OS_WINDOWS) {
            return TestSetupFrequency.REPEAT;
        }
        return super.lookupTestSetupPolicy();
    }
}
