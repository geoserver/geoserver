/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.metadata.data.service;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.metadata.AbstractMetadataTest;
import org.geoserver.metadata.data.model.ComplexMetadataMap;
import org.geoserver.metadata.data.model.MetadataTemplate;
import org.geoserver.metadata.data.model.impl.ComplexMetadataMapImpl;
import org.geoserver.metadata.data.model.impl.MetadataTemplateImpl;
import org.geoserver.metadata.data.service.impl.MetadataConstants;
import org.geoserver.platform.resource.Resource;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Test the template service.
 *
 * @author Timothy De Bock - timothy.debock.github@gmail.com
 */
public class TemplateServiceTest extends AbstractMetadataTest {

    @Autowired private GeoServerDataDirectory dataDirectory;

    @After
    public void after() throws Exception {
        restoreTemplates();
        restoreLayers();
    }

    @Test
    public void testList() throws IOException {
        List<MetadataTemplate> actual = templateService.list();
        Assert.assertEquals(7, actual.size());
        Assert.assertEquals("simple fields", actual.get(0).getName());
        Assert.assertNotNull(actual.get(0).getMetadata());
    }

    @Test
    public void testLoad() throws IOException {
        MetadataTemplate actual = templateService.findByName("allData");

        Assert.assertNotNull(actual.getName());
        Assert.assertEquals("allData", actual.getName());
        Assert.assertNotNull(actual.getMetadata());
    }

    @Test
    public void testSave() throws IOException {
        Resource dir = dataDirectory.get(MetadataConstants.TEMPLATES_DIRECTORY);
        int nof = dir.list().size();

        MetadataTemplateImpl metadataTemplate = new MetadataTemplateImpl();
        metadataTemplate.setId(UUID.randomUUID().toString());
        metadataTemplate.setName("new-record");

        templateService.save(metadataTemplate);

        MetadataTemplate actual = templateService.findByName("new-record");
        Assert.assertEquals("new-record", actual.getName());
        Assert.assertNotNull(actual.getMetadata());

        // assert was stored in dir
        assertEquals(nof + 1, dir.list().size());
    }

    @Test
    public void testSaveErrorFlow() throws IOException {

        MetadataTemplateImpl metadataTemplate = new MetadataTemplateImpl();
        // id required
        try {
            templateService.save(metadataTemplate);
            Assert.fail("Should throw error");
        } catch (IllegalArgumentException ignored) {

        }
        metadataTemplate.setId("newTemplate");

        // name required
        try {
            templateService.save(metadataTemplate);
            Assert.fail("Should throw error");
        } catch (IllegalArgumentException ignored) {

        }
        // no duplicate names
        metadataTemplate.setName("allData");
        try {
            templateService.save(metadataTemplate);
            Assert.fail("Should throw error");
        } catch (IllegalArgumentException ignored) {
        }
    }

    /** Test if: 1) the template data is updated 2) the metadata for linked layers is updated. */
    @Test
    public void testUpdate() throws IOException {
        MetadataTemplate initial = templateService.findByName("simple fields");
        Assert.assertEquals("template-identifier", initial.getMetadata().get("identifier-single"));
        Assert.assertTrue(initial.getLinkedLayers().contains("mylayerFeatureId"));

        initial.getMetadata().put("identifier-single", "updated-value");

        // check if the linked metadata is updated.
        LayerInfo initialMyLayer = geoServer.getCatalog().getLayer("myLayerId");
        Serializable initialCustom = initialMyLayer.getResource().getMetadata().get("custom");
        @SuppressWarnings("unchecked")
        IModel<ComplexMetadataMap> initialMetadataModel =
                new Model<>(
                        new ComplexMetadataMapImpl((HashMap<String, Serializable>) initialCustom));
        Assert.assertEquals(
                1, initialMetadataModel.getObject().size("feature-catalog/feature-attribute/type"));

        templateService.save(initial);
        templateService.update(initial, null);

        MetadataTemplate actual = templateService.findByName("simple fields");
        Assert.assertEquals("updated-value", actual.getMetadata().get("identifier-single"));

        // check if the linked metadata is updated.
        LayerInfo myLayer = geoServer.getCatalog().getLayer("myLayerId");
        Serializable custom = myLayer.getResource().getMetadata().get("custom");
        @SuppressWarnings("unchecked")
        IModel<ComplexMetadataMap> metadataModel =
                new Model<>(new ComplexMetadataMapImpl((HashMap<String, Serializable>) custom));

        Assert.assertEquals(
                "updated-value",
                metadataModel.getObject().get(String.class, "identifier-single").getValue());
        // only linked data from the linked template should change
        Assert.assertEquals(
                1, metadataModel.getObject().size("feature-catalog/feature-attribute/type"));
    }
}
