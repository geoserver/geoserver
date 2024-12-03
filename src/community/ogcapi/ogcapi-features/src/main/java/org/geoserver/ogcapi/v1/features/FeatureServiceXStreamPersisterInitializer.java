/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.features;

import com.thoughtworks.xstream.XStream;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterInitializer;

/** Configures XStream for OGC API FeatureService objects that will end up in the metadata maps. */
public class FeatureServiceXStreamPersisterInitializer implements XStreamPersisterInitializer {
    @Override
    public void init(XStreamPersister persister) {
        persister.registerBreifMapComplexType("ogcapiFeatures", FeatureConformance.class);
        persister.registerBreifMapComplexType("cql2", CQL2Conformance.class);
        persister.registerBreifMapComplexType("ecql", ECQLConformance.class);

        XStream xs = persister.getXStream();
        xs.allowTypes(new Class[] {FeatureConformance.class});
        xs.allowTypes(new Class[] {CQL2Conformance.class});
        xs.allowTypes(new Class[] {ECQLConformance.class});
    }
}
