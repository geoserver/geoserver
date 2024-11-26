package org.geoserver.ogcapi.v1.features;

import com.thoughtworks.xstream.XStream;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterInitializer;

/** Configures XStream for OGC API FeatureService objects that will end up in the metadata maps. */
public class FeatureServiceXStreamPersisterInitializer implements XStreamPersisterInitializer {
    @Override
    public void init(XStreamPersister persister) {
        persister.registerBreifMapComplexType("ogcapiFeatures", FeatureConformanceInfo.class);
        persister.registerBreifMapComplexType("cql2", CQL2Conformance.class);
        persister.registerBreifMapComplexType("ecql", ECQLConformance.class);

        XStream xs = persister.getXStream();
        //        persister.registerBreifMapComplexType("ecql", ECQLConformance.class);
        //        persister.registerBreifMapComplexType("cql2", CQL2Conformance.class);

        xs.allowTypes(new Class[] {FeatureConformanceInfo.class, FeatureConformance.class});
        xs.addDefaultImplementation(FeatureConformance.class, FeatureConformanceInfo.class);

        xs.allowTypes(new Class[] {CQL2Conformance.class});
        xs.allowTypes(new Class[] {ECQLConformance.class});
    }
}
