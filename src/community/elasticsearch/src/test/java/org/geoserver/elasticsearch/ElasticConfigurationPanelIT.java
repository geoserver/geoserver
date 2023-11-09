/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.elasticsearch;

import static org.junit.Assert.assertEquals;

import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AbstractAjaxBehavior;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.list.ListView;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.impl.FeatureTypeInfoImpl;
import org.geoserver.web.data.resource.ResourceConfigurationPage;
import org.geotools.data.elasticsearch.ElasticLayerConfiguration;
import org.geotools.feature.NameImpl;
import org.junit.Test;

/** ElasticSearch Resource configuration panel integration test */
public class ElasticConfigurationPanelIT extends ElasticTestSupport {
    @Test
    public void testRefreshFeatureResourceConfigurationPanel() throws Exception {
        init();
        Catalog catalog = getCatalog();
        CatalogBuilder builder = new CatalogBuilder(catalog);
        builder.setStore(elasticDs);
        FeatureTypeInfoImpl featureTypeInfo =
                (FeatureTypeInfoImpl) builder.buildFeatureType(new NameImpl(indexName));
        // if the layer configuration is not set in the metadata, no attributes will display
        featureTypeInfo.getMetadata().put(ElasticLayerConfiguration.KEY, config);
        catalog.add(featureTypeInfo);
        LayerInfo layerInfo = builder.buildLayer(featureTypeInfo);
        login();
        tester.startPage(new ResourceConfigurationPage(layerInfo, false));
        ListView attributeList =
                (ListView)
                        tester.getLastRenderedPage()
                                .get(
                                        "publishedinfo:tabs:panel:theList:1:content:attributePanel:attributesTable:attributes");
        assertEquals(25, attributeList.size());

        AjaxLink launchModalLink =
                (AjaxLink)
                        tester.getLastRenderedPage()
                                .get("publishedinfo:tabs:panel:theList:2:content:esPanel:edit");
        // launch the modal window, in manual testing this will open when the page is launched
        tester.executeAjaxEvent(launchModalLink, "click");
        // use REST client to add an attribute
        client.addTextAttribute(indexName, "testText");

        ModalWindow modal =
                (ModalWindow)
                        tester.getLastRenderedPage()
                                .get("publishedinfo:tabs:panel:theList:2:content:modal");
        // closing the modal window in testing does not trigger the WindowClosedBehavior
        for (Behavior behavior : modal.getBehaviors()) {
            if (behavior instanceof AbstractDefaultAjaxBehavior) {
                String name = behavior.getClass().getSimpleName();
                if (name.startsWith("WindowClosedBehavior")) {
                    tester.executeBehavior((AbstractAjaxBehavior) behavior);
                }
            }
        }

        ListView attributeList2 =
                (ListView)
                        tester.getLastRenderedPage()
                                .get(
                                        "publishedinfo:tabs:panel:theList:1:content:attributePanel:attributesTable:attributes");
        // new attribute should now be visible in the list
        assertEquals(26, attributeList2.size());
    }
}
