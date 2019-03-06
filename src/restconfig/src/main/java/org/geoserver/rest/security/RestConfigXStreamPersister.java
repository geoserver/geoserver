/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.security;

import java.util.logging.Logger;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterInitializer;
import org.geoserver.rest.security.xml.JaxbUser;
import org.geotools.util.logging.Logging;

/**
 * XStreamPersisterInitializer implementation for gs-rest-config
 *
 * @author ImranR
 */
public class RestConfigXStreamPersister implements XStreamPersisterInitializer {

    /** logging instance */
    static Logger LOGGER = Logging.getLogger(RestConfigXStreamPersister.class);

    @Override
    public void init(XStreamPersister persister) {

        persister
                .getXStream()
                .allowTypesByWildcard(new String[] {"org.geoserver.rest.security.xml.*"});
        persister.getXStream().alias("user", JaxbUser.class);
    }
}
