/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package mil.nga.giat.elasticsearch;

import mil.nga.giat.data.elasticsearch.ElasticAttribute;
import mil.nga.giat.data.elasticsearch.ElasticLayerConfiguration;

import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterInitializer;

import com.thoughtworks.xstream.XStream;

/**
 * 
 * Implementation of XStreamPersisterInitializer extension point to serialize ElasticLayerConfiguration
 *
 */
public class ElasticXStreamInitializer implements XStreamPersisterInitializer {

    @Override
    public void init(XStreamPersister persister) {
        persister.registerBreifMapComplexType("elasticLayerConfiguration",ElasticLayerConfiguration.class);
        XStream xs = persister.getXStream();
        xs.alias("esAttribute", ElasticAttribute.class);
    }
}
