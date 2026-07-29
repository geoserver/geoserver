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
        // the cql2 and ecql types are registered by the ogcapi-core initializer, they are not Features specific
        persister.registerBriefMapComplexType("ogcapiFeatures", FeatureConformance.class);

        XStream xs = persister.getXStream();
        xs.allowTypes(new Class[] {FeatureConformance.class});
    }
}
