/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.dimension;

import com.thoughtworks.xstream.XStream;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterInitializer;

/**
 * Initializes the XStream persister to treat the {@link DefaultValueConfigurations} values in the
 * ResourceInfo metadata map
 *
 * @author Andrea Aime - GeoSolutions
 */
public class DynamicDefaultXStreamInitializer implements XStreamPersisterInitializer {

    @Override
    public void init(XStreamPersister persister) {
        persister.registerBreifMapComplexType(
                "DynamicDefaultValues", DefaultValueConfigurations.class);
        XStream xs = persister.getXStream();
        xs.alias("configuration", DefaultValueConfiguration.class);
        xs.allowTypeHierarchy(org.geoserver.wms.dimension.DefaultValueConfiguration.class);
        xs.allowTypeHierarchy(org.geoserver.wms.dimension.DefaultValueConfigurations.class);
    }
}
