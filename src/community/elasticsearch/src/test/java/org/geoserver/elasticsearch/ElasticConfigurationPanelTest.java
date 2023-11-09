/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.elasticsearch;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.List;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.impl.FeatureTypeInfoImpl;
import org.geoserver.web.FormTestPage;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geotools.api.feature.type.Name;
import org.geotools.data.elasticsearch.ElasticAttribute;
import org.geotools.data.elasticsearch.ElasticDataStore;
import org.geotools.data.elasticsearch.ElasticLayerConfiguration;
import org.geotools.data.util.NullProgressListener;
import org.junit.Test;

/** ElasticSearch Resource configuration panel test */
public class ElasticConfigurationPanelTest extends GeoServerWicketTestSupport {
    @SuppressWarnings("unchecked")
    @Test
    public void testOnSaveAssignsNativeName() throws Exception {
        ElasticConfigurationPanel elasticConfigurationPanel =
                getPanel(getModel(getFeatureTypeInfo()));
        elasticConfigurationPanel.onSave();
        FeatureTypeInfo featureTypeInfo = getFeatureTypeInfo();
        ResourceInfo ri = ((ResourceInfo) featureTypeInfo);
        assertEquals("testType", ri.getNativeName());
    }

    private ElasticConfigurationPanel getPanel(IModel<FeatureTypeInfo> model) {
        return new ElasticConfigurationPanel("panel", model);
    }

    private FeatureTypeInfo getFeatureTypeInfo() throws Exception {
        FeatureTypeInfo featureTypeInfo = spy(new FeatureTypeInfoImpl(null));
        featureTypeInfo.setName("testType");
        featureTypeInfo.setNativeName("testType");
        ElasticLayerConfiguration elasticLayerConfiguration =
                new ElasticLayerConfiguration("testDocType");
        elasticLayerConfiguration.getAttributes().add(new ElasticAttribute("testAttr"));
        featureTypeInfo.getMetadata().put(ElasticLayerConfiguration.KEY, elasticLayerConfiguration);
        DataStoreInfo dataStoreInfo = mock(DataStoreInfo.class);
        ElasticDataStore elasticDataStore = mock(ElasticDataStore.class);
        ElasticAttribute elasticAttribute = new ElasticAttribute("testAttr");
        when(elasticDataStore.getElasticAttributes(isA(Name.class)))
                .thenReturn(List.of(elasticAttribute));
        doReturn(elasticDataStore)
                .when(dataStoreInfo)
                .getDataStore(isA(NullProgressListener.class));
        when(featureTypeInfo.getStore()).thenReturn(dataStoreInfo);
        return featureTypeInfo;
    }

    private FeatureTypeInfo getFeatureTypeInfo2() throws Exception {
        FeatureTypeInfo featureTypeInfo = spy(new FeatureTypeInfoImpl(null));
        featureTypeInfo.setName("testType2");
        featureTypeInfo.setNativeName("defaultType2");
        ElasticLayerConfiguration elasticLayerConfiguration =
                new ElasticLayerConfiguration("testDocType2");
        elasticLayerConfiguration.getAttributes().add(new ElasticAttribute("testAttr"));
        elasticLayerConfiguration.getAttributes().add(new ElasticAttribute("testAttr2"));
        featureTypeInfo.getMetadata().put(ElasticLayerConfiguration.KEY, elasticLayerConfiguration);
        DataStoreInfo dataStoreInfo = mock(DataStoreInfo.class);
        ElasticDataStore elasticDataStore = mock(ElasticDataStore.class);
        ElasticAttribute elasticAttribute = new ElasticAttribute("testAttr");
        ElasticAttribute elasticAttribute2 = new ElasticAttribute("testAttr2");
        when(elasticDataStore.getElasticAttributes(isA(Name.class)))
                .thenReturn(List.of(elasticAttribute, elasticAttribute2));
        doReturn(elasticDataStore)
                .when(dataStoreInfo)
                .getDataStore(isA(NullProgressListener.class));
        when(featureTypeInfo.getStore()).thenReturn(dataStoreInfo);
        return featureTypeInfo;
    }

    @SuppressWarnings("unchecked")
    private IModel<FeatureTypeInfo> getModel(FeatureTypeInfo featureTypeInfo) {
        IModel<FeatureTypeInfo> model = mock(IModel.class);
        when(model.getObject()).thenReturn(featureTypeInfo);
        return model;
    }

    @Test
    public void testModalRefresh() throws Exception {
        FeatureTypeInfo featureTypeInfo = getFeatureTypeInfo();
        IModel<FeatureTypeInfo> model = getModel(featureTypeInfo);
        ElasticConfigurationPanel elasticConfigurationPanel = getPanel(model);

        FormTestPage page = new FormTestPage(id -> elasticConfigurationPanel);
        tester.startPage(page);
        AjaxLink launchModalLink =
                (AjaxLink) tester.getLastRenderedPage().get("form:panel:esPanel:edit");
        tester.executeAjaxEvent(launchModalLink, "click");
        Form esForm = (Form) tester.getLastRenderedPage().get("form:panel:modal:content:es_form");
        assertEquals(
                1, ((WebMarkupContainer) esForm.get("esAttributes:listContainer:items")).size());
        FeatureTypeInfo featureTypeInfo2 = getFeatureTypeInfo2();
        IModel panelModel = elasticConfigurationPanel.getDefaultModel();
        when(panelModel.getObject()).thenReturn(featureTypeInfo2);
        AjaxButton refreshButton =
                (AjaxButton)
                        tester.getLastRenderedPage()
                                .get("form:panel:modal:content:es_form:es_refresh");
        tester.executeAjaxEvent(refreshButton, "click");
        Form esForm2 = (Form) tester.getLastRenderedPage().get("form:panel:modal:content:es_form");
        assertEquals(
                2, ((WebMarkupContainer) esForm2.get("esAttributes:listContainer:items")).size());
    }
}
