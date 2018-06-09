/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wfs;

import com.thoughtworks.xstream.XStream;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterInitializer;
import org.geotools.data.wfs.internal.v2_0.storedquery.ParameterMappingBlockValue;
import org.geotools.data.wfs.internal.v2_0.storedquery.ParameterMappingDefaultValue;
import org.geotools.data.wfs.internal.v2_0.storedquery.ParameterMappingExpressionValue;
import org.geotools.data.wfs.internal.v2_0.storedquery.StoredQueryConfiguration;

/**
 * Configure XStreamPersisters for WFS
 *
 * @author Sampo Savolainen (Spatineo)
 */
public class WFSXStreamPersisterInitializer implements XStreamPersisterInitializer {

    @Override
    public void init(XStreamPersister persister) {
        XStream xs = persister.getXStream();
        xs.alias("storedQueryConfiguration", StoredQueryConfiguration.class);
        xs.alias(
                "storedQueryParameterMappingExpressionValue",
                ParameterMappingExpressionValue.class);
        xs.alias("storedQueryParameterMappingDefaultValue", ParameterMappingDefaultValue.class);
        xs.alias("storedQueryParameterMappingBlockValue", ParameterMappingBlockValue.class);
        xs.allowTypes(
                new Class[] {
                    StoredQueryConfiguration.class,
                    ParameterMappingExpressionValue.class,
                    ParameterMappingDefaultValue.class,
                    ParameterMappingBlockValue.class
                });

        persister.registerBreifMapComplexType(
                "storedQueryConfiguration", StoredQueryConfiguration.class);
    }
}
