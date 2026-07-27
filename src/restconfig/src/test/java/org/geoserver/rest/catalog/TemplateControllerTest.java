/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.catalog;

import static org.geoserver.rest.RestBaseController.ROOT_PATH;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.geoserver.data.test.SystemTestData;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

public class TemplateControllerTest extends CatalogRESTTestSupport {

    @Override
    public void setUpTestData(SystemTestData testData) throws Exception {
        super.setUpTestData(testData);

        testData.setUpDefaultRasterLayers();
    }

    private String getIndexAsString(String childPath, String format) throws Exception {
        String indexUrl = childPath.substring(0, childPath.lastIndexOf("/"));
        if (format != null) {
            indexUrl += "." + format;
        }
        return getAsString(indexUrl);
    }

    private static String getName(String path) {
        return path.substring(path.lastIndexOf('/') + 1, path.length());
    }

    private void testGetPutGetDeleteGet(String path, String content) throws Exception {
        String name = getName(path);

        String htmlIndexToken = "geoserver" + path + "\">" + name + "</a></li>";
        String xmlIndexToken = "<name>" + name + "</name>";
        String jsonIndexToken = "{\"name\":\"" + name + "\"";

        // GET
        assertNotFound(path);
        assertFalse(getIndexAsString(path, null).contains(htmlIndexToken));
        assertFalse(getIndexAsString(path, "html").contains(htmlIndexToken));
        assertFalse(getIndexAsString(path, "xml").contains(xmlIndexToken));
        assertFalse(getIndexAsString(path, "json").contains(jsonIndexToken));

        // PUT
        put(path, content).close();
        String list = getIndexAsString(path, null);
        if (!list.contains(htmlIndexToken)) {
            assertTrue("list " + path, list.contains(htmlIndexToken));
        }
        assertTrue("list " + path, getIndexAsString(path, "html").contains(htmlIndexToken));
        assertTrue("list " + path, getIndexAsString(path, "xml").contains(xmlIndexToken));
        assertTrue("list " + path, getIndexAsString(path, "json").contains(jsonIndexToken));

        // GET
        assertEquals(content, getAsString(path).trim());

        // DELETE
        assertEquals(200, deleteAsServletResponse(path).getStatus());

        // GET
        assertNotFound(path);
        assertFalse(getIndexAsString(path, null).contains(htmlIndexToken));
        assertFalse(getIndexAsString(path, "html").contains(htmlIndexToken));
        assertFalse(getIndexAsString(path, "xml").contains(xmlIndexToken));
        assertFalse(getIndexAsString(path, "json").contains(jsonIndexToken));
    }

    @Test
    public void testGetPutGetDeleteGet() throws Exception {
        String path = ROOT_PATH + "/templates/my_template.ftl";
        testGetPutGetDeleteGet(path, "hello world");
    }

    private List<String> getAllPaths() {
        List<String> paths = new ArrayList<>();

        // global templates
        paths.add(ROOT_PATH + "/templates/aTemplate.ftl");
        paths.add(ROOT_PATH + "/templates/anotherTemplate.ftl");

        // workspace templates (cite workspace exists from default test data)
        paths.add(ROOT_PATH + "/workspaces/cite/templates/aTemplate.ftl");
        paths.add(ROOT_PATH + "/workspaces/cite/templates/anotherTemplate.ftl");

        // datastore templates (cite datastore exists in cite workspace)
        paths.add(ROOT_PATH + "/workspaces/cite/datastores/cite/templates/aTemplate.ftl");
        paths.add(ROOT_PATH + "/workspaces/cite/datastores/cite/templates/anotherTemplate.ftl");

        // feature type templates (Buildings exists in cite datastore)
        paths.add(ROOT_PATH + "/workspaces/cite/datastores/cite/featuretypes/Buildings/templates/aTemplate.ftl");
        paths.add(ROOT_PATH + "/workspaces/cite/datastores/cite/featuretypes/Buildings/templates/anotherTemplate.ftl");

        // coveragestore templates (DEM exists in wcs workspace from setUpDefaultRasterLayers)
        paths.add(ROOT_PATH + "/workspaces/wcs/coveragestores/DEM/templates/aTemplate.ftl");
        paths.add(ROOT_PATH + "/workspaces/wcs/coveragestores/DEM/templates/anotherTemplate.ftl");

        // coverage templates
        paths.add(ROOT_PATH + "/workspaces/wcs/coveragestores/DEM/coverages/DEM/templates/aTemplate.ftl");
        paths.add(ROOT_PATH + "/workspaces/wcs/coveragestores/DEM/coverages/DEM/templates/anotherTemplate.ftl");

        return paths;
    }

    @Test
    public void testAllPathsSequentially() throws Exception {
        Random random = new Random();
        for (String path : getAllPaths()) {
            testGetPutGetDeleteGet(path, "hello test " + random.nextInt(1000));
        }
    }

    void assertNotFound(String path) throws Exception {
        MockHttpServletResponse response = getAsServletResponse(path + "?quietOnNotFound=true");
        assertEquals("404 expected for '" + path + "'", 404, response.getStatus());
    }

    @Test
    public void testAllPaths() throws Exception {
        String contentHeader = "hello path ";
        List<String> paths = getAllPaths();

        for (String path : paths) { // GET - confirm template not there
            assertNotFound(path);
        }

        for (String path : paths) { // PUT
            put(path, contentHeader + path).close();
        }

        for (String path : paths) { // GET
            assertEquals(contentHeader + path, getAsString(path).trim());
        }

        for (String path : paths) { // DELETE
            MockHttpServletResponse response = deleteAsServletResponse(path);
            assertEquals(200, response.getStatus());
        }

        for (String path : paths) { // GET - confirm template removed
            assertNotFound(path);
        }
    }

    @Test
    public void testUpdate() throws Exception {
        String fooTemplate = ROOT_PATH + "/templates/foo.ftl";
        String barTemplate = ROOT_PATH + "/templates/bar.ftl";

        String fooContent = "hello foo - longer than bar";
        String barContent = "hello bar";

        // PUT
        put(fooTemplate, fooContent).close();
        put(barTemplate, barContent).close();

        // GET
        assertEquals(fooContent, getAsString(fooTemplate).trim());
        assertEquals(barContent, getAsString(barTemplate).trim());

        fooContent = "goodbye foo";

        // PUT
        put(fooTemplate, fooContent).close();

        // GET
        assertEquals(fooContent, getAsString(fooTemplate).trim());
        assertEquals(barContent, getAsString(barTemplate).trim());
    }

    // -- Non-existent workspace --

    @Test
    public void testPutNonExistentWorkspace() throws Exception {
        String path = ROOT_PATH + "/workspaces/nonexistent/templates/test.ftl";
        assertEquals(404, putAsServletResponse(path, "content", "text/plain").getStatus());
    }

    @Test
    public void testGetNonExistentWorkspace() throws Exception {
        assertNotFound(ROOT_PATH + "/workspaces/nonexistent/templates/test.ftl");
    }

    @Test
    public void testDeleteNonExistentWorkspace() throws Exception {
        assertEquals(
                404,
                deleteAsServletResponse(ROOT_PATH + "/workspaces/nonexistent/templates/test.ftl")
                        .getStatus());
    }

    @Test
    public void testListNonExistentWorkspace() throws Exception {
        assertNotFound(ROOT_PATH + "/workspaces/nonexistent/templates");
    }

    // -- Non-existent datastore --

    @Test
    public void testPutNonExistentDatastore() throws Exception {
        String path = ROOT_PATH + "/workspaces/cite/datastores/nonexistent/templates/test.ftl";
        assertEquals(404, putAsServletResponse(path, "content", "text/plain").getStatus());
    }

    @Test
    public void testGetNonExistentDatastore() throws Exception {
        assertNotFound(ROOT_PATH + "/workspaces/cite/datastores/nonexistent/templates/test.ftl");
    }

    @Test
    public void testDeleteNonExistentDatastore() throws Exception {
        assertEquals(
                404,
                deleteAsServletResponse(ROOT_PATH + "/workspaces/cite/datastores/nonexistent/templates/test.ftl")
                        .getStatus());
    }

    @Test
    public void testListNonExistentDatastore() throws Exception {
        assertNotFound(ROOT_PATH + "/workspaces/cite/datastores/nonexistent/templates");
    }

    // -- Non-existent coveragestore --

    @Test
    public void testPutNonExistentCoveragestore() throws Exception {
        String path = ROOT_PATH + "/workspaces/wcs/coveragestores/nonexistent/templates/test.ftl";
        assertEquals(404, putAsServletResponse(path, "content", "text/plain").getStatus());
    }

    @Test
    public void testListNonExistentCoveragestore() throws Exception {
        assertNotFound(ROOT_PATH + "/workspaces/wcs/coveragestores/nonexistent/templates");
    }

    // -- Non-existent feature type --

    @Test
    public void testPutNonExistentFeatureType() throws Exception {
        String path = ROOT_PATH + "/workspaces/cite/datastores/cite/featuretypes/nonexistent/templates/test.ftl";
        assertEquals(404, putAsServletResponse(path, "content", "text/plain").getStatus());
    }

    @Test
    public void testListNonExistentFeatureType() throws Exception {
        assertNotFound(ROOT_PATH + "/workspaces/cite/datastores/cite/featuretypes/nonexistent/templates");
    }

    // -- Non-existent coverage --

    @Test
    public void testPutNonExistentCoverage() throws Exception {
        String path = ROOT_PATH + "/workspaces/wcs/coveragestores/DEM/coverages/nonexistent/templates/test.ftl";
        assertEquals(404, putAsServletResponse(path, "content", "text/plain").getStatus());
    }

    @Test
    public void testListNonExistentCoverage() throws Exception {
        assertNotFound(ROOT_PATH + "/workspaces/wcs/coveragestores/DEM/coverages/nonexistent/templates");
    }

    @Test
    public void testAllPathsSequentiallyForJson() throws Exception {
        Random random = new Random();
        for (String path : getAllJsonTemplatePaths()) {
            testGetPutGetDeleteGet(path, "{key: a json template} " + random.nextInt(1000));
        }
    }

    private List<String> getAllJsonTemplatePaths() {
        List<String> paths = new ArrayList<>();

        paths.add(ROOT_PATH + "/templates/aTemplate_json.ftl");
        paths.add(ROOT_PATH + "/templates/anotherTemplate_json.ftl");

        paths.add(ROOT_PATH + "/workspaces/cite/templates/aTemplate_json.ftl");
        paths.add(ROOT_PATH + "/workspaces/cite/templates/anotherTemplate_json.ftl");

        paths.add(ROOT_PATH + "/workspaces/cite/datastores/cite/templates/aTemplate_json.ftl");
        paths.add(ROOT_PATH + "/workspaces/cite/datastores/cite/templates/anotherTemplate_json.ftl");

        paths.add(ROOT_PATH + "/workspaces/cite/datastores/cite/featuretypes/Buildings/templates/aTemplate_json.ftl");
        paths.add(ROOT_PATH
                + "/workspaces/cite/datastores/cite/featuretypes/Buildings/templates/anotherTemplate_json.ftl");

        paths.add(ROOT_PATH + "/workspaces/wcs/coveragestores/DEM/templates/aTemplate_json.ftl");
        paths.add(ROOT_PATH + "/workspaces/wcs/coveragestores/DEM/templates/anotherTemplate_json.ftl");

        paths.add(ROOT_PATH + "/workspaces/wcs/coveragestores/DEM/coverages/DEM/templates/aTemplate_json.ftl");
        paths.add(ROOT_PATH + "/workspaces/wcs/coveragestores/DEM/coverages/DEM/templates/anotherTemplate_json.ftl");

        return paths;
    }
}
