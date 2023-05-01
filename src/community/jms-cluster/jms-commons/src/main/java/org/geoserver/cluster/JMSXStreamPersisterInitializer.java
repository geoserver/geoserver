/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster;

import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterInitializer;

/** Extension point to enable JSM packages name in the SecureXStream. */
public class JMSXStreamPersisterInitializer implements XStreamPersisterInitializer {

    @Override
    public void init(XStreamPersister persister) {
        persister
                .getXStream()
                .allowTypesByWildcard(new String[] {"org.geoserver.cluster.impl.events.**"});
    }
}
