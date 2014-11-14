/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.solr;

import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterInitializer;
import org.geotools.data.solr.SolrAttribute;
import org.geotools.data.solr.SolrLayerConfiguration;

import com.thoughtworks.xstream.XStream;

/**
 * 
 * Implementation of XStreamPersisterInitializer extension point to serialize SolrLayerConfiguration
 *
 */
public class SolrXStreamInitializer implements XStreamPersisterInitializer {

    @Override
    public void init(XStreamPersister persister) {
        persister.registerBreifMapComplexType("solrLayerConfiguration",SolrLayerConfiguration.class);
        XStream xs = persister.getXStream();
        xs.alias("solrAttribute", SolrAttribute.class);
    }
}
