/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.generatedgeometries.web.longitudelatitude;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.geoserver.catalog.AttributeTypeInfo;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.ResourcePool;
import org.geoserver.catalog.impl.AttributeTypeInfoImpl;
import org.geoserver.generatedgeometries.core.longitudelatitude.LongLatGeometryGenerationStrategy;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opengis.feature.type.FeatureType;
import org.vfny.geoserver.global.ConfigurationException;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LongLatGeometryConfigurationPanelTest extends GeoServerWicketTestSupport {

    private GeoServerApplication application;
    private IModel model;

    private List<AttributeTypeInfo> attributeTypeInfoList;
    private AttributeTypeInfoImpl attributeTypeInfo1;
    private AttributeTypeInfoImpl attributeTypeInfo2;

    @Before
    public void before() throws IOException {
        System.setProperty("quietTests", "false");

        application = mock(GeoServerApplication.class);
        Catalog catalog = mock(Catalog.class);
        ResourcePool resourcePool = mock(ResourcePool.class);
        FeatureTypeInfo info = mock(FeatureTypeInfo.class);
        FeatureType featureType = mock(FeatureType.class);
        model = mock(IModel.class);
        attributeTypeInfo1 = new AttributeTypeInfoImpl();
        attributeTypeInfo1.setName("attr1");
        attributeTypeInfo2 = new AttributeTypeInfoImpl();
        attributeTypeInfo2.setName("attr2");
        attributeTypeInfoList = Arrays.asList(attributeTypeInfo1, attributeTypeInfo2);

        when(application.getCatalog()).thenReturn(catalog);
        when(catalog.getResourcePool()).thenReturn(resourcePool);
        when(resourcePool.getFeatureType(info)).thenReturn(featureType);
        when(resourcePool.loadAttributes(info)).thenReturn(attributeTypeInfoList);
        when(model.getObject()).thenReturn(info);
    }

    @Test
    public void testThatConfigurationPanelHasLoadedControlsAndNotCreateConfigWhenNothingIsSelected() {
        // given
        LongLatGeometryConfigurationPanel panel =
                new LongLatGeometryConfigurationPanel("id", model, () -> application);

        // when
        tester.startComponentInPage(panel);

        // then
        Component lonDDLComponent =
                tester.getComponentFromLastRenderedPage("id:lonAttributesDropDown");
        assertThat(lonDDLComponent, CoreMatchers.instanceOf(DropDownChoice.class));
        DropDownChoice<AttributeTypeInfo> lonAttributesDropDown =
                (DropDownChoice<AttributeTypeInfo>) lonDDLComponent;
        Assert.assertArrayEquals(
                lonAttributesDropDown.getChoices().toArray(), attributeTypeInfoList.toArray());

        Component latDDLComponent =
                tester.getComponentFromLastRenderedPage("id:latAttributesDropDown");
        assertThat(latDDLComponent, CoreMatchers.instanceOf(DropDownChoice.class));
        DropDownChoice<AttributeTypeInfo> latAttributesDropDown =
                (DropDownChoice<AttributeTypeInfo>) latDDLComponent;
        Assert.assertArrayEquals(
                latAttributesDropDown.getChoices().toArray(), attributeTypeInfoList.toArray());

        assertThat(
                tester.getComponentFromLastRenderedPage("id:geometryAttributeName"),
                CoreMatchers.instanceOf(TextField.class));

        try {
            panel.getLongLatConfiguration();
            fail("Exception should be triggered for empty configuration");
        } catch (ConfigurationException e) {
            assertTrue(true);
        }
    }

    @Test
    public void testThatConfigurationPanelCreateConfigWhenAllAttributesAreSelected() {

        // given
        String geometryAttrName = "the_geometry";
        LongLatGeometryGenerationStrategy.LongLatConfiguration longLatConfiguration = null;
        LongLatGeometryConfigurationPanel panel =
                new LongLatGeometryConfigurationPanel("id", model, () -> application);

        // when
        tester.startComponentInPage(panel);
        DropDownChoice<AttributeTypeInfo> lonAttributesDropDown =
                (DropDownChoice<AttributeTypeInfo>)
                        tester.getComponentFromLastRenderedPage("id:lonAttributesDropDown");
        DropDownChoice<AttributeTypeInfo> latAttributesDropDown =
                (DropDownChoice<AttributeTypeInfo>)
                        tester.getComponentFromLastRenderedPage("id:latAttributesDropDown");
        TextField<String> geometryAttrField =
                (TextField<String>)
                        tester.getComponentFromLastRenderedPage("id:geometryAttributeName");

        // set values
        lonAttributesDropDown.setModelObject(attributeTypeInfo1);
        latAttributesDropDown.setModelObject(attributeTypeInfo2);
        geometryAttrField.setModelObject(geometryAttrName);

        // then
        try {
            longLatConfiguration = panel.getLongLatConfiguration();
        } catch (ConfigurationException e) {
            fail(e.getMessage());
        }
        assertNotNull(longLatConfiguration);
        assertSame(longLatConfiguration.latAttributeName, attributeTypeInfo2.getName());
        assertSame(longLatConfiguration.longAttributeName, attributeTypeInfo1.getName());
        assertSame(longLatConfiguration.geomAttributeName, geometryAttrName);
    }

    @Test
    public void testThatConfigurationPanelDoNotCreateConfigWhenEvenOneAttributeIsMissing() {

        // given
        LongLatGeometryGenerationStrategy.LongLatConfiguration longLatConfiguration = null;
        LongLatGeometryConfigurationPanel panel =
                new LongLatGeometryConfigurationPanel("id", model, () -> application);

        // when
        tester.startComponentInPage(panel);
        DropDownChoice<AttributeTypeInfo> lonAttributesDropDown =
                (DropDownChoice<AttributeTypeInfo>)
                        tester.getComponentFromLastRenderedPage("id:lonAttributesDropDown");
        DropDownChoice<AttributeTypeInfo> latAttributesDropDown =
                (DropDownChoice<AttributeTypeInfo>)
                        tester.getComponentFromLastRenderedPage("id:latAttributesDropDown");

        // set values
        lonAttributesDropDown.setModelObject(attributeTypeInfo1);
        latAttributesDropDown.setModelObject(attributeTypeInfo2);

        // then
        try {
            longLatConfiguration = panel.getLongLatConfiguration();
        } catch (ConfigurationException e) {
            assertTrue(e instanceof ConfigurationException);
        }

        assertNull(longLatConfiguration);
    }
}
