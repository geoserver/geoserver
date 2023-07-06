/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.elasticsearch;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.wicket.model.IModel;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.impl.FeatureTypeInfoImpl;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geotools.data.elasticsearch.ElasticLayerConfiguration;
import org.junit.Test;

/** ElasticSearch Resource configuration panel test */
public class ElasticConfigurationPanelTest extends GeoServerWicketTestSupport {
    @SuppressWarnings("unchecked")
    @Test
    public void testOnSaveAssignsNativeName() {
        FeatureTypeInfo featureTypeInfo = new FeatureTypeInfoImpl(null);
        featureTypeInfo.setName("testType");
        featureTypeInfo.setNativeName("defaultType");
        ElasticLayerConfiguration elasticLayerConfiguration =
                new ElasticLayerConfiguration("testDocType");
        featureTypeInfo.getMetadata().put(ElasticLayerConfiguration.KEY, elasticLayerConfiguration);
        IModel<FeatureTypeInfo> model = mock(IModel.class);
        when(model.getObject()).thenReturn(featureTypeInfo);
        ElasticConfigurationPanel elasticConfigurationPanel =
                new ElasticConfigurationPanel("content", model);
        elasticConfigurationPanel.onSave();
        ResourceInfo ri = ((ResourceInfo) featureTypeInfo);
        assertEquals("testType", ri.getNativeName());
    }
}
