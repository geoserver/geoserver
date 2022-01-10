/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.solr;

import com.thoughtworks.xstream.XStream;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterInitializer;
import org.geotools.data.solr.SolrAttribute;
import org.geotools.data.solr.SolrLayerConfiguration;

/**
 * Implementation of XStreamPersisterInitializer extension point to serialize SolrLayerConfiguration
 */
public class SolrXStreamInitializer implements XStreamPersisterInitializer {

    @Override
    public void init(XStreamPersister persister) {
        persister.registerBreifMapComplexType(
                "solrLayerConfiguration", SolrLayerConfiguration.class);
        XStream xs = persister.getXStream();
        xs.alias("solrAttribute", SolrAttribute.class);
        xs.allowTypes(new Class[] {SolrAttribute.class, SolrLayerConfiguration.class});
    }
}
