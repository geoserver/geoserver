/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.metadata.data.service;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.impl.FeatureTypeInfoImpl;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.metadata.AbstractMetadataTest;
import org.geoserver.metadata.data.model.impl.ComplexMetadataMapImpl;
import org.geoserver.platform.resource.Resource;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * Test Import geonetwork. Test if the imported xml is mapped on the model in the correct way.
 *
 * @author Timothy De Bock - timothy.debock.github@gmail.com
 */
public class GeonetworkXmlParserTest extends AbstractMetadataTest {

    @Autowired GeonetworkXmlParser xmlParser;

    @Autowired private GeoServerDataDirectory dataDirectory;

    @After
    public void after() throws Exception {
        restoreLayers();
    }

    @Test
    public void testMapping() throws IOException {
        MetadataMap metadataMap = new MetadataMap();
        ComplexMetadataMapImpl complexMetadataMap = new ComplexMetadataMapImpl(metadataMap);

        complexMetadataMap.get(String.class, "refsystem-as-list", 0).setValue("old-value");

        ResourceInfo rInfo = new FeatureTypeInfoImpl(null);

        Document fileAsResource =
                getDocument("geonetwork-1a2c6739-3c62-432b-b2a0-aaa589a9e3a1.xml");

        xmlParser.parseMetadata(fileAsResource, rInfo, complexMetadataMap);

        // simple single
        Assert.assertEquals(
                "1a2c6739-3c62-432b-b2a0-aaa589a9e3a1", metadataMap.get("identifier-single"));
        // simple list
        Serializable actualList = metadataMap.get("refsystem-as-list");
        Assert.assertTrue(actualList instanceof List);
        Assert.assertEquals(4, ((List<?>) actualList).size());
        Assert.assertEquals("Belge_Lambert_1972 (31370)", ((List<?>) actualList).get(0));
        Assert.assertEquals("TAW", ((List<?>) actualList).get(1));
        Assert.assertEquals(
                "http://www.opengis.net/def/crs/EPSG/0/3043", ((List<?>) actualList).get(2));
        assertEquals("G3Dv2_01_Q, dikte niet-tabulair Quartair", ((List<?>) actualList).get(3));

        // complex single
        Assert.assertEquals("EPSG", metadataMap.get("referencesystem-object/code-space"));
        Assert.assertEquals(
                "Belge_Lambert_1972 (31370)", metadataMap.get("referencesystem-object/code"));

        // complex list
        Serializable actualObjectCodeSpaceList =
                metadataMap.get("referencesystem-object-list/code-space");
        Assert.assertTrue(actualObjectCodeSpaceList instanceof List);
        Assert.assertEquals(6, ((List<?>) actualObjectCodeSpaceList).size());
        Assert.assertEquals("EPSG", ((List<?>) actualObjectCodeSpaceList).get(0));
        Assert.assertEquals("NGI", ((List<?>) actualObjectCodeSpaceList).get(1));
        Assert.assertEquals("EPSG", ((List<?>) actualObjectCodeSpaceList).get(2));

        Serializable actualObjectCodeList = metadataMap.get("referencesystem-object-list/code");
        Assert.assertTrue(actualObjectCodeList instanceof List);
        Assert.assertEquals(6, ((List<?>) actualObjectCodeList).size());
        Assert.assertEquals("Belge_Lambert_1972 (31370)", ((List<?>) actualObjectCodeList).get(0));
        Assert.assertEquals("TAW", ((List<?>) actualObjectCodeList).get(1));
        Assert.assertEquals(
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
    }

    private Document getDocument(String fileName) throws IOException {
        for (Resource resource : dataDirectory.get("metadata").list()) {
            if (resource.name().equals(fileName)) {
                try {
                    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                    dbf.setNamespaceAware(true);
                    InputStream stream = resource.in();
                    DocumentBuilder db = dbf.newDocumentBuilder();
                    Document doc = db.parse(stream);
                    doc.getDocumentElement().normalize();
                    return doc;
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (ParserConfigurationException e) {
                    e.printStackTrace();
                } catch (SAXException e) {
                    e.printStackTrace();
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                }
            }
        }
        throw new IOException("Resource not found: " + fileName);
    }
}
