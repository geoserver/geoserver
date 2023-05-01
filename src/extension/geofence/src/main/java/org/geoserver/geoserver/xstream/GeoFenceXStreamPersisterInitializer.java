/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2015 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geoserver.xstream;

import com.thoughtworks.xstream.XStream;
import org.geoserver.config.util.CollectionConverter;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterInitializer;
import org.geoserver.geofence.rest.xml.Batch;
import org.geoserver.geofence.rest.xml.BatchOperation;
import org.geoserver.geofence.rest.xml.JaxbAdminRule;
import org.geoserver.geofence.rest.xml.JaxbAdminRuleList;
import org.geoserver.geofence.rest.xml.JaxbRule;
import org.geoserver.geofence.rest.xml.JaxbRuleList;
import org.geoserver.geofence.rest.xml.MultiPolygonAdapter;
import org.geoserver.geoserver.authentication.auth.GeoFenceAuthenticationProviderConfig;

/** @author ETj (etj at geo-solutions.it) */
public class GeoFenceXStreamPersisterInitializer implements XStreamPersisterInitializer {

    @Override
    public void init(XStreamPersister persister) {

        XStream xs = persister.getXStream();
        xs.alias(
                "geoFenceAuthenticationProviderConfig", GeoFenceAuthenticationProviderConfig.class);
        xs.alias("Rule", JaxbRule.class);
        xs.alias("AdminRule", JaxbAdminRule.class);
        xs.alias("Rules", JaxbRuleList.class);
        xs.alias("AdminRules", JaxbAdminRuleList.class);
        xs.alias("Batch", Batch.class);
        xs.alias("BatchOperation", BatchOperation.class);
        xs.registerLocalConverter(
                Batch.class, "operations", new CollectionConverter(xs.getMapper()));
        xs.addImplicitCollection(Batch.class, "operations", BatchOperation.class);
        xs.registerConverter(
                new BatchOpXtreamConverter(xs.getMapper(), xs.getReflectionProvider()));
        xs.allowTypes(
                new Class[] {
                    GeoFenceAuthenticationProviderConfig.class,
                    JaxbRule.class,
                    JaxbAdminRule.class,
                    JaxbRuleList.class,
                    JaxbAdminRuleList.class,
                    MultiPolygonAdapter.class,
                    JaxbRule.Limits.class,
                    JaxbRule.LayerDetails.class,
                    JaxbRule.LayerAttribute.class,
                    Batch.class,
                    BatchOperation.class
                });
    }
}
