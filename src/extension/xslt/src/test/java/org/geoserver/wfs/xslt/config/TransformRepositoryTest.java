/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.xslt.config;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.xml.transform.Transformer;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.stream.StreamSource;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.easymock.EasyMock;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.impl.FeatureTypeInfoImpl;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.wfs.xslt.XSLTTestSupport;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

public class TransformRepositoryTest {

    File testRoot;

    private TransformRepository repo;

    private FeatureTypeInfoImpl ft1;

    private FeatureTypeInfoImpl ft2;

    @Before
    public void setup() throws IOException {
        // make sure the test root exists and it's empty
        File dataDir = new File("./target/repository-test");
        if (dataDir.exists()) {
            XSLTTestSupport.deleteDirectory(dataDir);
        }
        assertTrue(dataDir.mkdirs());

        // setup a mock catalog
        Catalog catalog = EasyMock.createNiceMock(Catalog.class);
        ft1 = new FeatureTypeInfoImpl(catalog);
        ft1.setId("ft1-id");
        ft2 = new FeatureTypeInfoImpl(catalog);
        ft2.setId("ft2-id");
        expect(catalog.getFeatureType("ft1-id")).andReturn(ft1).anyTimes();
        expect(catalog.getFeatureType("ft2-id")).andReturn(ft2).anyTimes();
        replay(catalog);

        repo = new TransformRepository(new GeoServerDataDirectory(dataDir), catalog);
        testRoot = new File(new File(dataDir, "wfs"), "transform");
    }

    @Test
    public void testSaveNoName() throws IOException {
        TransformInfo original = new TransformInfo();
        try {
            repo.putTransformInfo(original);
            fail("Shoudl have thrown an exception, the tx name is not set");
        } catch (IllegalArgumentException e) {
            // fine
        }
    }

    @Test
    public void testSaveReloadInfo() throws IOException {
        TransformInfo original = new TransformInfo();
        original.setName("test");
        original.setSourceFormat("application/xml");
        original.setOutputFormat("text/plain");
        original.setFileExtension("txt");
        original.setXslt("test-tx.xslt");

        repo.putTransformInfo(original);

        assertTrue(new File(testRoot, "test.xml").exists());

        // force the cache to be cleared and reload
        repo.infoCache.clear();
        TransformInfo reloaded = repo.getTransformInfo("test");
        assertEquals(original, reloaded);
    }

    @Test
    public void testLoadInfo() throws IOException {
        String xml =
                "<transform>\n" //
                        + "  <sourceFormat>application/xml</sourceFormat>\n" //
                        + "  <outputFormat>text/plain</outputFormat>\n" //
                        + "  <fileExtension>txt</fileExtension>\n" //
                        + "  <xslt>test-tx.xslt</xslt>\n" //
                        + "</transform>";

        testRoot.mkdirs();
        File file = new File(testRoot, "test.xml");
        FileUtils.writeStringToFile(file, xml, "UTF-8");

        TransformInfo info = repo.getTransformInfo("test");
        assertNotNull(info);
        assertEquals("test", info.getName());
        assertEquals("application/xml", info.getSourceFormat());
        assertEquals("text/plain", info.getOutputFormat());
        assertEquals("txt", info.getFileExtension());
        assertEquals("test-tx.xslt", info.getXslt());
    }

    @Test
    public void testRefreshFromFile() throws IOException, InterruptedException {
        // write out the config and make the repo cache it
        String xml1 =
                "<transform>\n" //
                        + "  <sourceFormat>application/xml</sourceFormat>\n" //
                        + "  <outputFormat>text/plain</outputFormat>\n" //
                        + "  <fileExtension>txt</fileExtension>\n" //
                        + "  <xslt>test-tx.xslt</xslt>\n" //
                        + "</transform>";

        testRoot.mkdirs();
        File file = new File(testRoot, "test.xml");
        FileUtils.writeStringToFile(file, xml1, "UTF-8");

        TransformInfo info1 = repo.getTransformInfo("test");
        assertNotNull(info1);

        // wait enough for the file to be considered stale
        Thread.sleep((long) (CacheItem.MIN_INTERVALS_CHECK * 1.1));

        // write another version
        String xml2 =
                "<transform>\n" //
                        + "  <sourceFormat>text/xml; subtype=gml/2.1.2</sourceFormat>\n" //
                        + "  <outputFormat>application/json</outputFormat>\n" //
                        + "  <fileExtension>json</fileExtension>\n" //
                        + "  <xslt>json-tx.xslt</xslt>\n" //
                        + "</transform>";
        FileUtils.writeStringToFile(file, xml2, "UTF-8");

        // reload and check
        TransformInfo info2 = repo.getTransformInfo("test");
        assertNotNull(info2);
        assertEquals("test", info2.getName());
        assertEquals("text/xml; subtype=gml/2.1.2", info2.getSourceFormat());
        assertEquals("application/json", info2.getOutputFormat());
        assertEquals("json", info2.getFileExtension());
        assertEquals("json-tx.xslt", info2.getXslt());
    }

    @Test
    public void testDeleteOnFilesystem() throws IOException, InterruptedException {
        // write out the config and make the repo cache it
        String xml1 =
                "<transform>\n" //
                        + "  <sourceFormat>application/xml</sourceFormat>\n" //
                        + "  <outputFormat>text/plain</outputFormat>\n" //
                        + "  <fileExtension>txt</fileExtension>\n" //
                        + "  <xslt>test-tx.xslt</xslt>\n" //
                        + "</transform>";

        testRoot.mkdirs();
        File file = new File(testRoot, "test.xml");
        FileUtils.writeStringToFile(file, xml1, "UTF-8");

        TransformInfo info1 = repo.getTransformInfo("test");
        assertNotNull(info1);

        // wait enough for the file to be considered stale
        Thread.sleep((long) (CacheItem.MIN_INTERVALS_CHECK * 1.1));

        // delete the resource from disk
        file.delete();

        // reload and check we are not getting a stale object
        TransformInfo info2 = repo.getTransformInfo("test");
        assertNull(info2);
    }

    @Test
    public void testFeatureTypeReference() throws Exception {
        TransformInfo original = new TransformInfo();
        original.setName("test");
        original.setSourceFormat("application/xml");
        original.setOutputFormat("text/plain");
        original.setFileExtension("txt");
        original.setXslt("test-tx.xslt");
        original.setFeatureType(ft1);

        repo.putTransformInfo(original);
        File configFile = new File(testRoot, "test.xml");
        assertTrue(configFile.exists());

        // force the cache to be cleared and reload
        repo.infoCache.clear();
        TransformInfo reloaded = repo.getTransformInfo("test");
        assertEquals(original, reloaded);

        // check the file on disk
        Document doc = XMLUnit.buildTestDocument(FileUtils.readFileToString(configFile, "UTF-8"));
        XMLAssert.assertXpathEvaluatesTo("ft1-id", "/transform/featureType/id", doc);
    }

    @Test
    public void testListMethods() throws Exception {
        // prepare a set of configurations
        writeConfiguration("c1", null);
        writeConfiguration("c2", ft1);
        writeConfiguration("c3", ft2);

        // check all transforms
        List<TransformInfo> configs = repo.getAllTransforms();
        assertEquals(3, configs.size());
        Set<String> names = getConfigurationNames(configs);
        assertTrue(names.contains("c1"));
        assertTrue(names.contains("c2"));
        assertTrue(names.contains("c3"));

        // check global
        configs = repo.getAllTransforms();
        assertEquals(3, configs.size());
        names = getConfigurationNames(configs);
        assertTrue(names.contains("c1"));

        // check associated to ft1
        configs = repo.getTypeTransforms(ft1);
        assertEquals(1, configs.size());
        names = getConfigurationNames(configs);
        assertTrue(names.contains("c2"));

        // check associated to ft2
        configs = repo.getTypeTransforms(ft2);
        assertEquals(1, configs.size());
        names = getConfigurationNames(configs);
        assertTrue(names.contains("c3"));
    }

    @Test
    public void testWriteXSLT() throws Exception {
        TransformInfo original = new TransformInfo();
        original.setName("test");
        original.setSourceFormat("application/xml");
        original.setOutputFormat("text/plain");
        original.setFileExtension("txt");
        original.setXslt("test-tx.xslt");

        repo.putTransformInfo(original);

        File info = new File(testRoot, "test.xml");
        assertTrue(info.exists());

        repo.putTransformSheet(original, getClass().getResourceAsStream("test.xslt"));

        File xslt = new File(testRoot, "test-tx.xslt");
        assertTrue(xslt.exists());

        String expected = IOUtils.toString(getClass().getResourceAsStream("test.xslt"), "UTF-8");
        String actual = FileUtils.readFileToString(xslt, "UTF-8");
        assertEquals(expected, actual);

        repo.removeTransformInfo(original);
        assertFalse(info.exists());
        assertFalse(xslt.exists());
    }

    @Test
    public void testWriteXSLTShared() throws Exception {
        TransformInfo info1 = new TransformInfo();
        info1.setName("test1");
        info1.setSourceFormat("application/xml");
        info1.setOutputFormat("text/plain");
        info1.setFileExtension("txt");
        info1.setXslt("test-tx.xslt");

        repo.putTransformInfo(info1);

        File infoFile1 = new File(testRoot, "test1.xml");
        assertTrue(infoFile1.exists());

        TransformInfo info2 = new TransformInfo();
        info2.setName("test2");
        info2.setSourceFormat("application/xml");
        info2.setOutputFormat("text/plain");
        info2.setFileExtension("txt");
        info2.setXslt("test-tx.xslt");

        repo.putTransformInfo(info2);

        File infoFile2 = new File(testRoot, "test2.xml");
        assertTrue(infoFile2.exists());

        repo.putTransformSheet(info1, getClass().getResourceAsStream("test.xslt"));

        File xslt = new File(testRoot, "test-tx.xslt");
        assertTrue(xslt.exists());

        repo.removeTransformInfo(info1);
        assertFalse(infoFile1.exists());
        // shared, not deleted
        assertTrue(xslt.exists());
        assertTrue(infoFile2.exists());

        // remote the other too
        repo.removeTransformInfo(info2);
        assertFalse(xslt.exists());
        assertFalse(infoFile2.exists());
    }

    @Test
    public void testTransform() throws Exception {
        TransformInfo info = new TransformInfo();
        info.setName("test");
        info.setSourceFormat("application/xml");
        info.setOutputFormat("text/plain");
        info.setFileExtension("txt");
        info.setXslt("test-tx.xslt");

        repo.putTransformInfo(info);
        repo.putTransformSheet(info, getClass().getResourceAsStream("test.xslt"));

        Transformer transformer = repo.getTransformer(info);
        InputStream is = getClass().getResourceAsStream("sample.xml");
        StreamSource source = new StreamSource(is);
        DOMResult result = new DOMResult();
        transformer.transform(source, result);
        Document dom = (Document) result.getNode();
        XMLAssert.assertXpathEvaluatesTo("12", "count(/html/body/table/tr/td)", dom);
        XMLAssert.assertXpathEvaluatesTo("1", "count(/html/body/table/tr[td='museum'])", dom);
        XMLAssert.assertXpathEvaluatesTo(
                "1", "count(/html/body/table/tr[td='-74.0104611,40.70758763'])", dom);
    }

    private Set<String> getConfigurationNames(List<TransformInfo> configs) {
        Set<String> result = new HashSet<String>();
        for (TransformInfo ti : configs) {
            result.add(ti.getName());
        }

        return result;
    }

    private void writeConfiguration(String name, FeatureTypeInfo ft) throws IOException {
        TransformInfo ti = new TransformInfo();
        ti.setName(name);
        ti.setSourceFormat("application/xml");
        ti.setOutputFormat("text/plain");
        ti.setFileExtension("txt");
        ti.setXslt("test-tx.xslt");
        ti.setFeatureType(ft);
        String xml = repo.xs.toXML(ti);

        FileUtils.writeStringToFile(new File(testRoot, name + ".xml"), xml, "UTF-8");
    }
}
