/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.elasticsearch;

import com.thoughtworks.xstream.XStream;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterInitializer;
import org.geotools.data.elasticsearch.ElasticAttribute;
import org.geotools.data.elasticsearch.ElasticLayerConfiguration;

/**
 * Implementation of XStreamPersisterInitializer extension point to serialize
 * ElasticLayerConfiguration
 */
class ElasticXStreamInitializer implements XStreamPersisterInitializer {

    @Override
    public void init(XStreamPersister persister) {
        persister.registerBreifMapComplexType(
                "elasticLayerConfiguration", ElasticLayerConfiguration.class);
        XStream xs = persister.getXStream();
        xs.alias("esAttribute", ElasticAttribute.class);
    }
}
