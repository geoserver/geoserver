/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster.configuration;

import java.io.IOException;

public interface JMSConfigurationExt {

    /** initialize defaults */
    public void initDefaults(JMSConfiguration config) throws IOException;

    /** returns true if the passed config is different by the default values */
    public boolean override(JMSConfiguration config) throws IOException;
}
