/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.generatedgeometries.web.longitudelatitude;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.geoserver.catalog.AttributeTypeInfo;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.ResourcePool;
import org.geoserver.catalog.impl.AttributeTypeInfoImpl;
import org.geoserver.generatedgeometries.core.GeneratedGeometryConfigurationException;
import org.geoserver.generatedgeometries.core.longitudelatitude.LongLatGeometryGenerationStrategy;
import org.geoserver.web.FormTestPage;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.web.wicket.CRSPanel;
import org.geotools.referencing.CRS;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.opengis.feature.type.FeatureType;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

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
    public void
            testThatConfigurationPanelHasLoadedControlsAndNotCreateConfigWhenNothingIsSelected() {
        // given
        FormTestPage testPage =
                new FormTestPage(
                        id -> new LongLatGeometryConfigurationPanel(id, model, () -> application));

        // when
        FormTestPage page = tester.startPage(testPage);
        print(page, false, true, true);

        // then
        Component lonDDLComponent =
                tester.getComponentFromLastRenderedPage(
                        page.getFormPagePath("lonAttributesDropDown"));
        assertThat(lonDDLComponent, CoreMatchers.instanceOf(DropDownChoice.class));
        DropDownChoice<AttributeTypeInfo> lonAttributesDropDown =
                (DropDownChoice<AttributeTypeInfo>) lonDDLComponent;
        Assert.assertArrayEquals(
                lonAttributesDropDown.getChoices().toArray(), attributeTypeInfoList.toArray());

        Component latDDLComponent =
                tester.getComponentFromLastRenderedPage(
                        page.getFormPagePath("latAttributesDropDown"));
        assertThat(latDDLComponent, CoreMatchers.instanceOf(DropDownChoice.class));
        DropDownChoice<AttributeTypeInfo> latAttributesDropDown =
                (DropDownChoice<AttributeTypeInfo>) latDDLComponent;
        Assert.assertArrayEquals(
                latAttributesDropDown.getChoices().toArray(), attributeTypeInfoList.toArray());

        assertThat(
                tester.getComponentFromLastRenderedPage(
                        page.getFormPagePath("geometryAttributeName")),
                CoreMatchers.instanceOf(TextField.class));

        try {
            LongLatGeometryConfigurationPanel panel =
                    (LongLatGeometryConfigurationPanel)
                            tester.getComponentFromLastRenderedPage(page.getFormPagePath(""));
            panel.getLongLatConfiguration();
            fail("Exception should be triggered for empty configuration");
        } catch (GeneratedGeometryConfigurationException e) {
            assertTrue(true);
        }
    }

    @Test
    @Ignore
    public void testThatConfigurationPanelCreateConfigWhenAllAttributesAreSelected()
            throws FactoryException {
        // given
        String geometryAttrName = "the_geometry";
        LongLatGeometryGenerationStrategy.LongLatConfiguration longLatConfiguration = null;
        FormTestPage testPage =
                new FormTestPage(
                        id -> new LongLatGeometryConfigurationPanel(id, model, () -> application));

        // when
        FormTestPage page = tester.startPage(testPage);
        print(page, false, true, true);

        DropDownChoice<AttributeTypeInfo> lonAttributesDropDown =
                (DropDownChoice<AttributeTypeInfo>)
                        tester.getComponentFromLastRenderedPage(
                                page.getFormPagePath("lonAttributesDropDown"));
        DropDownChoice<AttributeTypeInfo> latAttributesDropDown =
                (DropDownChoice<AttributeTypeInfo>)
                        tester.getComponentFromLastRenderedPage(
                                page.getFormPagePath("latAttributesDropDown"));
        TextField<String> geometryAttrField =
                (TextField<String>)
                        tester.getComponentFromLastRenderedPage(
                                page.getFormPagePath("geometryAttributeName"));
        CRSPanel srsPicker =
                (CRSPanel)
                        tester.getComponentFromLastRenderedPage(page.getFormPagePath("srsPicker"));

        // set values
        lonAttributesDropDown.setModelObject(attributeTypeInfo1);
        latAttributesDropDown.setModelObject(attributeTypeInfo2);
        geometryAttrField.setModelObject(geometryAttrName);
        CoordinateReferenceSystem crs = CRS.decode("EPSG:4326");
        srsPicker.setModelObject(crs);

        // then
        try {
            LongLatGeometryConfigurationPanel panel =
                    (LongLatGeometryConfigurationPanel)
                            tester.getComponentFromLastRenderedPage(page.getFormPagePath(""));
            longLatConfiguration = panel.getLongLatConfiguration();
        } catch (GeneratedGeometryConfigurationException e) {
            fail(e.getMessage());
        }
        assertNotNull(longLatConfiguration);
        assertSame(longLatConfiguration.latAttributeName, attributeTypeInfo2.getName());
        assertSame(longLatConfiguration.longAttributeName, attributeTypeInfo1.getName());
        assertSame(longLatConfiguration.geomAttributeName, geometryAttrName);
        assertSame(longLatConfiguration.crs, crs);
    }

    @Test
    public void testThatConfigurationPanelDoNotCreateConfigWhenSomeAttributesAreMissing() {
        // given
        LongLatGeometryGenerationStrategy.LongLatConfiguration longLatConfiguration = null;
        FormTestPage testPage =
                new FormTestPage(
                        id -> new LongLatGeometryConfigurationPanel(id, model, () -> application));

        // when
        FormTestPage page = tester.startPage(testPage);
        print(page, false, true, true);

        DropDownChoice<AttributeTypeInfo> lonAttributesDropDown =
                (DropDownChoice<AttributeTypeInfo>)
                        tester.getComponentFromLastRenderedPage(
                                page.getFormPagePath("lonAttributesDropDown"));
        DropDownChoice<AttributeTypeInfo> latAttributesDropDown =
                (DropDownChoice<AttributeTypeInfo>)
                        tester.getComponentFromLastRenderedPage(
                                page.getFormPagePath("latAttributesDropDown"));
        TextField<String> geometryAttrField =
                (TextField<String>)
                        tester.getComponentFromLastRenderedPage(
                                page.getFormPagePath("geometryAttributeName"));

        // set values
        lonAttributesDropDown.setModelObject(attributeTypeInfo1);
        latAttributesDropDown.setModelObject(attributeTypeInfo2);

        // then
        try {
            LongLatGeometryConfigurationPanel panel =
                    (LongLatGeometryConfigurationPanel)
                            tester.getComponentFromLastRenderedPage(page.getFormPagePath(""));
            longLatConfiguration = panel.getLongLatConfiguration();
        } catch (GeneratedGeometryConfigurationException e) {
            // ok
        }

        assertNull(longLatConfiguration);
    }
}
