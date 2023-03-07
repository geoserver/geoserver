/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi;

import com.thoughtworks.xstream.XStream;
import java.util.List;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterInitializer;
import org.geoserver.ogcapi.impl.LinkInfoImpl;
import org.springframework.stereotype.Component;

/** Configures XStream for OGC API configuration objects that will end up in the metadata maps. */
@Component
public class OGCAPIXStreamPersisterInitializer implements XStreamPersisterInitializer {
    @Override
    public void init(XStreamPersister persister) {
        XStream xs = persister.getXStream();
        xs.alias("link", LinkInfo.class);
        xs.addDefaultImplementation(LinkInfoImpl.class, LinkInfo.class);
        persister.registerBreifMapComplexType("list", List.class);
        xs.allowTypes(new Class[] {LinkInfo.class});
    }
}
