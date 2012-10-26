package org.geoserver.wfs.xslt.config;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.easymock.EasyMock;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.impl.FeatureTypeInfoImpl;
import org.geoserver.config.GeoServerDataDirectory;
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
            FileUtils.deleteDirectory(dataDir);
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
        original.setTransform("test-tx.xslt");

        repo.putTransformInfo(original);

        assertTrue(new File(testRoot, "test.xml").exists());

        // force the cache to be cleared and reload
        repo.infoCache.clear();
        TransformInfo reloaded = repo.getTransformInfo("test");
        assertEquals(original, reloaded);
    }

    @Test
    public void testLoadInfo() throws IOException {
        String xml = "<transform>\n" //
                + "  <sourceFormat>application/xml</sourceFormat>\n" //
                + "  <outputFormat>text/plain</outputFormat>\n" //
                + "  <fileExtension>txt</fileExtension>\n" //
                + "  <transform>test-tx.xslt</transform>\n"//
                + "</transform>";

        testRoot.mkdirs();
        File file = new File(testRoot, "test.xml");
        FileUtils.writeStringToFile(file, xml);

        TransformInfo info = repo.getTransformInfo("test");
        assertNotNull(info);
        assertEquals("test", info.getName());
        assertEquals("application/xml", info.getSourceFormat());
        assertEquals("text/plain", info.getOutputFormat());
        assertEquals("txt", info.getFileExtension());
        assertEquals("test-tx.xslt", info.getTransform());
    }

    @Test
    public void testRefreshFromFile() throws IOException, InterruptedException {
        // write out the config and make the repo cache it
        String xml1 = "<transform>\n" //
                + "  <sourceFormat>application/xml</sourceFormat>\n" //
                + "  <outputFormat>text/plain</outputFormat>\n" //
                + "  <fileExtension>txt</fileExtension>\n" //
                + "  <transform>test-tx.xslt</transform>\n"//
                + "</transform>";

        testRoot.mkdirs();
        File file = new File(testRoot, "test.xml");
        FileUtils.writeStringToFile(file, xml1);
        
        TransformInfo info1 = repo.getTransformInfo("test");
        assertNotNull(info1);

        // wait enough for the file to be considered stale
        Thread.sleep((long) (CacheItem.MIN_INTERVALS_CHECK * 1.1));
        
        // write another version
        String xml2 = "<transform>\n" //
                + "  <sourceFormat>text/xml; subtype=gml/2.1.2</sourceFormat>\n" //
                + "  <outputFormat>application/json</outputFormat>\n" //
                + "  <fileExtension>json</fileExtension>\n" //
                + "  <transform>json-tx.xslt</transform>\n"//
                + "</transform>";
        FileUtils.writeStringToFile(file, xml2);

        // reload and check
        TransformInfo info2 = repo.getTransformInfo("test");
        assertNotNull(info2);
        assertEquals("test", info2.getName());
        assertEquals("text/xml; subtype=gml/2.1.2", info2.getSourceFormat());
        assertEquals("application/json", info2.getOutputFormat());
        assertEquals("json", info2.getFileExtension());
        assertEquals("json-tx.xslt", info2.getTransform());
    }
    
    @Test
    public void testDeleteOnFilesystem() throws IOException, InterruptedException {
        // write out the config and make the repo cache it
        String xml1 = "<transform>\n" //
                + "  <sourceFormat>application/xml</sourceFormat>\n" //
                + "  <outputFormat>text/plain</outputFormat>\n" //
                + "  <fileExtension>txt</fileExtension>\n" //
                + "  <transform>test-tx.xslt</transform>\n"//
                + "</transform>";

        testRoot.mkdirs();
        File file = new File(testRoot, "test.xml");
        FileUtils.writeStringToFile(file, xml1);
        
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
        original.setTransform("test-tx.xslt");
        original.setFeatureType(ft1);
        
        repo.putTransformInfo(original);
        File configFile = new File(testRoot, "test.xml");
        assertTrue(configFile.exists());

        // force the cache to be cleared and reload
        repo.infoCache.clear();
        TransformInfo reloaded = repo.getTransformInfo("test");
        assertEquals(original, reloaded);
        
        // check the file on disk
        Document doc = XMLUnit.buildTestDocument(FileUtils.readFileToString(configFile));
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
        ti.setTransform("test-tx.xslt");
        ti.setFeatureType(ft);
        String xml = repo.xs.toXML(ti);
        
        FileUtils.writeStringToFile(new File(testRoot, name + ".xml"), xml);
    }
    
}
