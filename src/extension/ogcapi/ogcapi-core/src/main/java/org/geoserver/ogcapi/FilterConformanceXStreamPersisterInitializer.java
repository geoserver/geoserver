/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi;

import com.thoughtworks.xstream.XStream;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterInitializer;

/**
 * Configures XStream for the filter conformance objects stored in service metadata maps. They are shared by every OGC
 * API service supporting the filter parameters, hence configured in a centralized place.
 */
public class FilterConformanceXStreamPersisterInitializer implements XStreamPersisterInitializer {
    @Override
    public void init(XStreamPersister persister) {
        // the type ids are part of the persisted configuration, they cannot change
        persister.registerBriefMapComplexType("cql2", CQL2Conformance.class);
        persister.registerBriefMapComplexType("ecql", ECQLConformance.class);

        XStream xs = persister.getXStream();
        xs.allowTypes(new Class[] {CQL2Conformance.class});
        xs.allowTypes(new Class[] {ECQLConformance.class});
    }
}
