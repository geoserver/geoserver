/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.metadata.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.geoserver.catalog.Keyword;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.impl.LayerIdentifier;
import org.geoserver.catalog.impl.MetadataLinkInfoImpl;
import org.geoserver.metadata.AbstractMetadataTest;
import org.geoserver.metadata.data.model.ComplexMetadataMap;
import org.geoserver.metadata.data.model.MetadataTemplate;
import org.geoserver.metadata.data.model.impl.ComplexMetadataMapImpl;
import org.geoserver.metadata.data.service.impl.MetadataConstants;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class MetaDataRestServiceTest extends AbstractMetadataTest {

    @Autowired private MetaDataRestService restService;

    @Before
    public void before() throws Exception {
        login();
    }

    @After
    public void after() throws Exception {
        logout();
        restoreLayers();
    }

    @Test
    public void testImportGeonetwork() throws IOException {

        ResourceInfo rInfo =
                geoServer.getCatalog().getResourceByName("topp:mylayer", ResourceInfo.class);

        MetadataTemplate template = templateService.findByName("simple fields");
        template.getLinkedLayers().add(rInfo.getId());
        templateService.save(template);

        restService.importAndLink(
                "geonetwork url",
                "doesn'texist;boo;\ntopp:mylayer; 1a2c6739-3c62-432b-b2a0-aaa589a9e3a1; ");

        rInfo = geoServer.getCatalog().getResourceByName("topp:mylayer", ResourceInfo.class);

        @SuppressWarnings("unchecked")
        Map<String, Serializable> metadataMap =
                (Map<String, Serializable>)
                        rInfo.getMetadata().get(MetadataConstants.CUSTOM_METADATA_KEY);
        // simple single
        assertEquals("1a2c6739-3c62-432b-b2a0-aaa589a9e3a1", metadataMap.get("identifier-single"));
        // simple list
        Serializable actualList = metadataMap.get("refsystem-as-list");
        assertTrue(actualList instanceof List);
        assertEquals(4, ((List<?>) actualList).size());
        assertEquals("Belge_Lambert_1972 (31370)", ((List<?>) actualList).get(0));
        assertEquals("TAW", ((List<?>) actualList).get(1));
        assertEquals("http://www.opengis.net/def/crs/EPSG/0/3043", ((List<?>) actualList).get(2));

        // complex single
        assertEquals("EPSG", metadataMap.get("referencesystem-object/code-space"));
        assertEquals("Belge_Lambert_1972 (31370)", metadataMap.get("referencesystem-object/code"));

        // complex list
        Serializable actualObjectCodeSpaceList =
                metadataMap.get("referencesystem-object-list/code-space");
        assertTrue(actualObjectCodeSpaceList instanceof List);
        assertEquals(6, ((List<?>) actualObjectCodeSpaceList).size());
        assertEquals("EPSG", ((List<?>) actualObjectCodeSpaceList).get(0));
        assertEquals("NGI", ((List<?>) actualObjectCodeSpaceList).get(1));
        assertEquals("EPSG", ((List<?>) actualObjectCodeSpaceList).get(2));

        Serializable actualObjectCodeList = metadataMap.get("referencesystem-object-list/code");
        assertTrue(actualObjectCodeList instanceof List);
        assertEquals(6, ((List<?>) actualObjectCodeList).size());
        assertEquals("Belge_Lambert_1972 (31370)", ((List<?>) actualObjectCodeList).get(0));
        assertEquals("TAW", ((List<?>) actualObjectCodeList).get(1));
        assertEquals(
                "http://www.opengis.net/def/crs/EPSG/0/3043",
                ((List<?>) actualObjectCodeList).get(2));

        // check equal sizes for complex repeatables
        List<?> names = (List<?>) metadataMap.get("contact/name");
        assertEquals(3, names.size());
        List<?> urls = (List<?>) metadataMap.get("contact/url");
        assertEquals(3, urls.size());

        // check multidimensional
        List<?> phones = (List<?>) metadataMap.get("contact/phone");
        assertEquals(3, phones.size());
        assertEquals(2, ((List<?>) phones.get(2)).size());

        // test native mappings
        assertEquals("G3Dv2_01_Q, dikte niet-tabulair Quartair", rInfo.getTitle());
        assertEquals(2, rInfo.getAlias().size());
        assertEquals(
                "Geologisch 3D model Vlaamse Ondergrond versie2- Dikte niet-tabulair Quartair",
                rInfo.getAlias().get(0));
        assertEquals("AndereTitel", rInfo.getAlias().get(1));

        // verify unlinked
        template = templateService.findByName("simple fields");
        assertFalse(template.getLinkedLayers().contains(rInfo.getId()));
    }

    @Test
    public void testLinkTemplates() {
        restService.importAndLink(
                null, "emptyline\ntopp:mylayer; ; allData; template-nested-object");

        ResourceInfo rInfo =
                geoServer.getCatalog().getResourceByName("topp:mylayer", ResourceInfo.class);
        @SuppressWarnings("unchecked")
        ComplexMetadataMap map =
                new ComplexMetadataMapImpl(
                        (Map<String, Serializable>)
                                rInfo.getMetadata().get(MetadataConstants.CUSTOM_METADATA_KEY));

        assertEquals(3, map.size("referencesystem-object-list"));
        ComplexMetadataMap submap01 = map.subMap("referencesystem-object-list", 0);
        ComplexMetadataMap submap02 = map.subMap("referencesystem-object-list", 1);
        assertEquals("list-objectcode01", submap01.get(String.class, "code").getValue());
        assertEquals("list-objectcodeSpace01", submap01.get(String.class, "code-space").getValue());
        assertEquals("list-objectcode02", submap02.get(String.class, "code").getValue());
        assertEquals("list-objectcodeSpace02", submap02.get(String.class, "code-space").getValue());
        // Should be updated list of nested objects
        assertEquals(1, map.size("feature-catalog/feature-attribute"));
        ComplexMetadataMap submapTemplate = map.subMap("feature-catalog/feature-attribute", 0);
        assertEquals("template-identifier", submapTemplate.get(String.class, "name").getValue());
        assertEquals("Geometry", submapTemplate.get(String.class, "type").getValue());
        assertEquals(2, submapTemplate.size("domain"));
        ComplexMetadataMap submapdomain01 = submapTemplate.subMap("domain", 0);
        ComplexMetadataMap submapdomain02 = submapTemplate.subMap("domain", 1);
        assertEquals("template-domain-code01", submapdomain01.get(String.class, "code").getValue());
        assertEquals(
                "template-domain-code01", submapdomain01.get(String.class, "value").getValue());
        assertEquals("template-domain-code02", submapdomain02.get(String.class, "code").getValue());
        assertEquals(
                "template-domain-code02", submapdomain02.get(String.class, "value").getValue());

        // verify linked
        MetadataTemplate template = templateService.findByName("allData");
        assertTrue(template.getLinkedLayers().contains(rInfo.getId()));
        template = templateService.findByName("template-nested-object");
        assertTrue(template.getLinkedLayers().contains(rInfo.getId()));
    }

    @Test
    public void testNativeToCustomPartial() {
        LayerInfo layer = geoServer.getCatalog().getLayerByName("topp:mylayer");

        layer.getResource().getKeywords().add(new Keyword("KEY_foo"));
        layer.getResource().getKeywords().get(0).setVocabulary("VOCABULARY_A");
        layer.getResource().getKeywords().add(new Keyword("KEY_bar"));
        layer.getResource().getKeywords().get(1).setVocabulary("VOCABULARY_A");
        layer.getResource().getKeywords().add(new Keyword("KEY_DOV"));
        layer.getResource().getKeywords().get(2).setVocabulary("VOCABULARY_B");
        layer.getResource().getKeywords().add(new Keyword("KEY_Vlaanderen"));
        layer.getResource().getKeywords().get(3).setVocabulary("VOCABULARY_B");
        layer.getResource().getMetadataLinks().add(new MetadataLinkInfoImpl());
        layer.getResource()
                .getMetadataLinks()
                .get(0)
                .setContent("https://www.dov.vlaanderen.be/geonetwork/?uuid=1234");
        layer.getResource().getMetadataLinks().get(0).setType("text/html");
        layer.getResource().getMetadataLinks().get(0).setMetadataType("ISO191156:2003");
        layer.getResource().getMetadataLinks().add(new MetadataLinkInfoImpl());
        layer.getResource()
                .getMetadataLinks()
                .get(1)
                .setContent(
                        "https://www.dov.vlaanderen.be/geonetwork/srv/nl/csw?Service=CSW&Request=GetRecordById&Version=2.0.2&outputSchema=http://www.isotc211.org/2005/gmd&elementSetName=full&id=1234");
        layer.getResource().getMetadataLinks().get(1).setType("text/xml");
        layer.getResource().getMetadataLinks().get(1).setMetadataType("ISO191156:2003");
        layer.getIdentifiers().add(new LayerIdentifier());
        layer.getIdentifiers().get(0).setIdentifier("abcde");
        layer.getIdentifiers().get(0).setAuthority("DOV-be");
        layer.getIdentifiers().add(new LayerIdentifier());
        layer.getIdentifiers().get(1).setIdentifier("fghi");
        layer.getIdentifiers().get(1).setAuthority("EOV-ce");
        layer.getResource().getMetadata().clear();

        geoServer.getCatalog().save(layer.getResource());
        geoServer.getCatalog().save(layer);

        restService.nativeToCustom("0,2");

        layer = geoServer.getCatalog().getLayerByName("topp:mylayer");
        ResourceInfo rInfo =
                geoServer.getCatalog().getResourceByName("topp:mylayer", ResourceInfo.class);
        @SuppressWarnings("unchecked")
        ComplexMetadataMap map =
                new ComplexMetadataMapImpl(
                        (Map<String, Serializable>)
                                rInfo.getMetadata().get(MetadataConstants.CUSTOM_METADATA_KEY));

        assertEquals(2, map.size("refsystem-as-list"));
        assertEquals("foo", map.get(String.class, "refsystem-as-list", 0).getValue());
        assertEquals("bar", map.get(String.class, "refsystem-as-list", 1).getValue());
        assertEquals(0, map.size("contact"));
        assertEquals("1234", map.get(String.class, "identifier-single").getValue());
        assertEquals(0, map.size("referencesystem-object"));
    }

    @Test
    public void testCustomToNative() throws IOException {
        LayerInfo layer = geoServer.getCatalog().getLayers().get(0);

        @SuppressWarnings("unchecked")
        HashMap<String, Serializable> underlying =
                (HashMap<String, Serializable>)
                        layer.getResource()
                                .getMetadata()
                                .get(MetadataConstants.CUSTOM_METADATA_KEY);
        ComplexMetadataMap map = new ComplexMetadataMapImpl(underlying);

        map.get(String.class, "refsystem-as-list", 0).setValue("foo");
        map.get(String.class, "refsystem-as-list", 1).setValue("bar");
        map.get(String.class, "refsystem-as-list", 2).setValue(null);
        map.subMap("contact", 0).get(String.class, "name").setValue("DOV");
        map.subMap("contact", 1).get(String.class, "name").setValue("Vlaanderen");
        map.get(String.class, "identifier-single").setValue("1234");
        map.subMap("referencesystem-object", 0).get(String.class, "code").setValue("abcde");
        map.subMap("referencesystem-object", 0).get(String.class, "code-space").setValue("DOV-be");
        map.subMap("referencesystem-object", 1).get(String.class, "code").setValue("fghi");
        map.subMap("referencesystem-object", 1).get(String.class, "code-space").setValue("EOV-ce");

        geoServer.getCatalog().save(layer);

        restService.customToNative(layer.getName(), null);

        layer = geoServer.getCatalog().getLayers().get(0);

        assertEquals(4, layer.getResource().getKeywords().size());

        assertEquals("KEY_foo", layer.getResource().getKeywords().get(0).getValue());

        assertEquals("KEY_bar", layer.getResource().getKeywords().get(1).getValue());

        assertEquals("KEY_DOV", layer.getResource().getKeywords().get(2).getValue());

        assertEquals("KEY_Vlaanderen", layer.getResource().getKeywords().get(3).getValue());

        assertEquals("VOCABULARY_A", layer.getResource().getKeywords().get(0).getVocabulary());

        assertEquals("VOCABULARY_A", layer.getResource().getKeywords().get(1).getVocabulary());

        assertEquals("VOCABULARY_B", layer.getResource().getKeywords().get(2).getVocabulary());

        assertEquals("VOCABULARY_B", layer.getResource().getKeywords().get(3).getVocabulary());

        assertEquals(2, layer.getResource().getMetadataLinks().size());

        assertEquals(
                "https://www.dov.vlaanderen.be/geonetwork/?uuid=1234",
                layer.getResource().getMetadataLinks().get(0).getContent());

        assertEquals("text/html", layer.getResource().getMetadataLinks().get(0).getType());

        assertEquals(
                "ISO191156:2003", layer.getResource().getMetadataLinks().get(0).getMetadataType());

        assertEquals(
                "https://www.dov.vlaanderen.be/geonetwork/srv/nl/csw?Service=CSW&Request=GetRecordById&Version=2.0.2&outputSchema=http://www.isotc211.org/2005/gmd&elementSetName=full&id=1234",
                layer.getResource().getMetadataLinks().get(1).getContent());

        assertEquals("text/xml", layer.getResource().getMetadataLinks().get(1).getType());

        assertEquals(
                "ISO191156:2003", layer.getResource().getMetadataLinks().get(1).getMetadataType());

        assertEquals(2, layer.getIdentifiers().size());

        assertEquals("abcde", layer.getIdentifiers().get(0).getIdentifier());

        assertEquals("DOV-be", layer.getIdentifiers().get(0).getAuthority());

        assertEquals("fghi", layer.getIdentifiers().get(1).getIdentifier());

        assertEquals("EOV-ce", layer.getIdentifiers().get(1).getAuthority());
    }
}
