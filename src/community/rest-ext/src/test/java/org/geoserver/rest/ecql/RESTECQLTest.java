/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.ecql;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.io.IOUtils;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.catalog.rest.CatalogRESTTestSupport;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.impl.SettingsInfoImpl;
import org.geoserver.data.test.MockData;
import org.geoserver.rest.util.RESTUtils;
import org.geotools.data.DataUtilities;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.expression.Expression;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/** Unit test for evaluating the ECQL REST PathMapper. */
public class RESTECQLTest extends CatalogRESTTestSupport {

    private static SimpleFeatureType type;

    private static List<String> fileNames;

    static {
        try {
            // Feature type associated to the path variable inside the cql expressions
            type = DataUtilities.createType("type", "path:string,name:string");
        } catch (SchemaException e) {
            LOGGER.log(Level.WARNING, e.getMessage(), e);
        }
        // Populating the filename list
        fileNames = new ArrayList<String>();
        fileNames.add("NCOM_wattemp_000_20081031T0000000_12.tiff");
        fileNames.add("NCOM_wattemp_000_20081101T0000000_12.tiff");
        fileNames.add("NCOM_wattemp_100_20081031T0000000_12.tiff");
        fileNames.add("NCOM_wattemp_100_20081101T0000000_12.tiff");
    }

    @Test
    public void testRegExp() throws Exception {
        // RegExp expression
        String expression =
                "stringTemplate(path, '(\\w{4})_(\\w{7})_(\\d{3})_(\\d{4})(\\d{2})(\\d{2})T(\\d{7})_(\\d{2})\\.(\\w{4})', "
                        + "'/${1}/${4}/${5}/${6}/${0}')";
        // Testing of the defined exception
        testExpression("test", "mosaic_test", expression, fileNames);
    }

    @Test
    public void testSubString() throws Exception {
        // SubString expression
        String expression =
                "if_then_else(strEndsWith(name,'.tiff'),Concatenate(strSubstring(path, 0, 4),'/',name),'')";
        // Testing of the defined exception
        testExpression("test2", "mosaic_test2", expression, fileNames);
    }

    /**
     * Initial settings for the root key, mapper and expression used
     *
     */
    private void initialSetup(String expression) {
        // Selection of the root directory
        String root = getTestData().getDataDirectoryRoot().getAbsolutePath();

        // Setting of the global configuration
        GeoServerInfo global = getGeoServer().getGlobal();
        SettingsInfoImpl info = (SettingsInfoImpl) ModificationProxy.unwrap(global.getSettings());
        // Setting of the metadata map if not present
        if (info.getMetadata() == null) {
            info.setMetadata(new MetadataMap());
        }
        // Selection of the metadata map
        MetadataMap map = info.getMetadata();
        // Insertion of the Root directory and the ecql expression
        map.put(RESTUtils.ROOT_KEY, root);
        map.put(RESTUploadECQLPathMapper.EXPRESSION_KEY, expression);
        // Save the global settings
        getGeoServer().save(global);
    }

    /**
     * Private method for adding the selected coverage inside the defined workspace via REST and
     * then checking if the coverage has been placed inside the defined directory
     *
     */
    private void testExpression(
            String workspace, String coverageStore, String expression, List<String> fileNames)
            throws IOException, Exception, ParserConfigurationException, SAXException {
        // Initial Settings
        initialSetup(expression);
        // Selection of a zip file
        URL zip = MockData.class.getResource("watertemp.zip");

        // byte[] bytes = FileUtils.readFileToByteArray(URLs.urlToFile(zip));

        byte[] bytes;
        try (InputStream is = zip.openStream()) {
            bytes = IOUtils.toByteArray(is);
        }

        // creation of the workspace if not already present
        createWorkSpace(workspace);

        // Uploading the file via rest
        MockHttpServletResponse response =
                putAsServletResponse(
                        "/rest/workspaces/"
                                + workspace
                                + "/coveragestores/"
                                + coverageStore
                                + "/file.imagemosaic",
                        bytes,
                        "application/zip");
        assertEquals(201, response.getStatus());
        // Check if the coverage is present
        String content = response.getContentAsString();
        Document d = dom(new ByteArrayInputStream(content.getBytes()));
        assertEquals("coverageStore", d.getDocumentElement().getNodeName());

        // Final checks
        CoverageStoreInfo cs = getCatalog().getCoverageStoreByName(workspace, coverageStore);
        assertNotNull(cs);
        CoverageInfo ci = getCatalog().getCoverageByName(workspace, coverageStore);
        assertNotNull(ci);
        // Check if the final path is correct for each input file
        for (String fileName : fileNames) {
            // File related to the filenamee
            File finalFile = extractFile(expression, cs, fileName, fileName);
            // Check if the file really exists
            assertTrue(finalFile.exists());
        }
    }

    /**
     * Private method for creating a new file object associated to the input path.
     *
     */
    private File extractFile(
            String expression, CoverageStoreInfo cs, String itemPath, String filename)
            throws CQLException {
        // Url to the final element
        String url = cs.getURL();
        // Convert the String expression into a CQL expression
        Expression exp = ECQL.toExpression(expression);
        // Feature associated to the input path
        SimpleFeature feature =
                SimpleFeatureBuilder.build(type, new Object[] {itemPath, filename}, null);
        // Perform Regular Expression match
        String newPath = exp.evaluate(feature, String.class);
        // Final FILE creation
        return new File(url, newPath);
    }

    /**
     * Creation of a new workspace defined by the input "workspace" name
     *
     */
    private void createWorkSpace(String workspace) throws Exception {
        // Check if the workspace is already present
        if (getCatalog().getWorkspaceByName(workspace) != null) {
            return;
        }

        // Creation of a new Workspace called "test"
        String xml = "<workspace>" + "<name>" + workspace + "</name>" + "</workspace>";

        MockHttpServletResponse responseBefore =
                postAsServletResponse("/rest/workspaces", xml, "text/xml");
        assertEquals(201, responseBefore.getStatus());
        assertNotNull(responseBefore.getHeader("Location"));
        assertTrue(responseBefore.getHeader("Location").endsWith("/workspaces/" + workspace));

        // Setting of the workspace configuration
        WorkspaceInfo ws = getCatalog().getWorkspaceByName(workspace);
        assertNotNull(ws);
    }
}
