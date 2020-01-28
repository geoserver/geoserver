/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.metadata.data.service;

import static org.junit.Assert.assertEquals;

import com.google.common.collect.Lists;
import java.io.Serializable;
import java.util.HashMap;
import org.geoserver.catalog.Keyword;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.impl.LayerIdentifier;
import org.geoserver.catalog.impl.MetadataLinkInfoImpl;
import org.geoserver.metadata.AbstractMetadataTest;
import org.geoserver.metadata.data.model.ComplexMetadataMap;
import org.geoserver.metadata.data.model.impl.ComplexMetadataMapImpl;
import org.geoserver.metadata.data.service.impl.MetadataConstants;
import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class CustomNativeMappingServiceTest extends AbstractMetadataTest {

    @Autowired CustomNativeMappingService cnmService;

    @After
    public void after() throws Exception {
        restoreLayers();
    }

    @Test
    public void testCustomToNative() {
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

        cnmService.mapCustomToNative(layer);

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

    @Test
    public void testNativeToCustom() {
        LayerInfo layer = geoServer.getCatalog().getLayers().get(0);

        @SuppressWarnings("unchecked")
        HashMap<String, Serializable> underlying =
                (HashMap<String, Serializable>)
                        layer.getResource()
                                .getMetadata()
                                .get(MetadataConstants.CUSTOM_METADATA_KEY);
        underlying.clear();
        ComplexMetadataMap map = new ComplexMetadataMapImpl(underlying);

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

        cnmService.mapNativeToCustom(layer);

        assertEquals(2, map.size("refsystem-as-list"));
        assertEquals("foo", map.get(String.class, "refsystem-as-list", 0).getValue());
        assertEquals("bar", map.get(String.class, "refsystem-as-list", 1).getValue());
        assertEquals(2, map.size("contact"));
        assertEquals("DOV", map.subMap("contact", 0).get(String.class, "name").getValue());
        assertEquals("Vlaanderen", map.subMap("contact", 1).get(String.class, "name").getValue());
        assertEquals("1234", map.get(String.class, "identifier-single").getValue());
        assertEquals(2, map.size("referencesystem-object"));
        assertEquals(
                "abcde",
                map.subMap("referencesystem-object", 0).get(String.class, "code").getValue());
        assertEquals(
                "DOV-be",
                map.subMap("referencesystem-object", 0).get(String.class, "code-space").getValue());
        assertEquals(
                "fghi",
                map.subMap("referencesystem-object", 1).get(String.class, "code").getValue());
        assertEquals(
                "EOV-ce",
                map.subMap("referencesystem-object", 1).get(String.class, "code-space").getValue());
    }

    @Test
    public void testNativeToCustomPartial() {
        LayerInfo layer = geoServer.getCatalog().getLayers().get(0);

        @SuppressWarnings("unchecked")
        HashMap<String, Serializable> underlying =
                (HashMap<String, Serializable>)
                        layer.getResource()
                                .getMetadata()
                                .get(MetadataConstants.CUSTOM_METADATA_KEY);
        underlying.clear();
        ComplexMetadataMap map = new ComplexMetadataMapImpl(underlying);

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

        cnmService.mapNativeToCustom(layer, Lists.newArrayList(0, 2));

        assertEquals(2, map.size("refsystem-as-list"));
        assertEquals("foo", map.get(String.class, "refsystem-as-list", 0).getValue());
        assertEquals("bar", map.get(String.class, "refsystem-as-list", 1).getValue());
        assertEquals(0, map.size("contact"));
        assertEquals("1234", map.get(String.class, "identifier-single").getValue());
        assertEquals(0, map.size("referencesystem-object"));
    }
}
