/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

/* Copyright (c) 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gsr.controller.relationship;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.gsr.controller.ControllerTest;
import org.geoserver.gsr.model.relationship.MessageDirection;
import org.geoserver.gsr.model.relationship.RelationshipClass;
import org.geoserver.gsr.model.relationship.RelationshipType;
import org.geoserver.gsr.translate.relationship.RelationshipDAO;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

public class RelationshipControllerTest extends ControllerTest {

    private String query(String workspaceName, String params) {
        return "/gsr/relationships/" + workspaceName + params;
    }

    /**
     * Executes an ows request using the DELETE method, with xml as body content.
     *
     * @param path The porition of the request after the context ( no query string ), example:
     *     'wms'.
     * @param contentType
     * @return the servlet response
     */
    protected MockHttpServletResponse deleteAsServletResponse(
            String path, String body, String contentType) throws Exception {
        MockHttpServletRequest request = createRequest(path);
        request.setMethod("DELETE");
        request.setContentType(contentType);
        request.setContent(body.getBytes("UTF-8"));

        return dispatch(request);
    }

    /**
     * Executes an ows request using the PUT method, with xml as body content.
     *
     * @param path The porition of the request after the context ( no query string ), example:
     *     'wms'.
     * @param contentType
     * @return the servlet response
     */
    @Override
    protected MockHttpServletResponse putAsServletResponse(
            String path, String body, String contentType) throws Exception {
        MockHttpServletRequest request = createRequest(path);
        request.setMethod("PUT");
        request.setContentType(contentType);
        request.setContent(body.getBytes("UTF-8"));

        return dispatch(request);
    }

    RelationshipDAO relationshipDAO;

    @Before
    public void revert() throws IOException {
        relationshipDAO = new RelationshipDAO(getGeoServer());
        // Use 'cgf' workspace - 0 Lines, 1 MLines, 2 MPoints, 3 MPolygons, 4 Points, 5 Polygons,
        relationshipDAO.deleteRelationshipClass("cgf", "Lines", "Points", "id", "id");
        revertLayer(MockData.POINTS);
        revertLayer(MockData.MPOINTS);
        revertLayer(MockData.MLINES);
        revertLayer(MockData.MPOLYGONS);
        revertLayer(MockData.LINES);
        revertLayer(MockData.POLYGONS);
    }

    private void createTestRelationship() {
        RelationshipClass relationshipClassCheck = new RelationshipClass();
        relationshipClassCheck.setRelationshipId(1L);
        relationshipClassCheck.setWorkspaceName("cgf");
        relationshipClassCheck.setOriginTable("Lines");
        relationshipClassCheck.setDestinationTable("Points");
        relationshipClassCheck.setOriginPrimaryKey("id");
        relationshipClassCheck.setOriginForeignKey("id");
        relationshipClassCheck.setDestinationPrimaryKey("r");
        relationshipClassCheck.setDestinationForeignKey("r");
        relationshipClassCheck.setRelationshipType(RelationshipType.SIMPLE);
        relationshipClassCheck.setMessageDirection(MessageDirection.NONE);
        relationshipDAO.upsertRelationshipClass("cgf", relationshipClassCheck);
    }

    private RelationshipClass getRelationshipClass() {
        return relationshipDAO.getRelationshipClass("cgf", "Lines", "Points", "id", "id");
    }

    /**
     * Tests creating a new RelationshipClass into the cfg workspace
     *
     * @throws Exception
     */
    @Test
    public void createRelationship() throws Exception {
        String q = query("cgf", "");
        String addsBody =
                "{"
                        + "   \"workspaceName\":\"cgf\","
                        + "   \"originTable\":\"Lines\","
                        + "   \"destinationTable\":\"Points\","
                        + "   \"relationshipType\":\"SIMPLE\","
                        + "   \"forwardLabel\":\"r\","
                        + "   \"backwardLabel\":\"r\","
                        + "   \"messageDirection\":\"NONE\","
                        + "   \"cardinality\":\"ONE_TO_ONE\","
                        + "   \"attributed\":false,"
                        + "   \"originPrimaryKey\":\"id\","
                        + "   \"originForeignKey\":\"id\","
                        + "   \"destinationPrimaryKey\":\"r\","
                        + "   \"destinationForeignKey\":\"r\""
                        + "}";
        JSON result = postAsJSON(q, addsBody, "application/json");
        assertTrue(String.valueOf(result) + " is a JSON object", result instanceof JSONObject);

        Catalog catalog = getCatalog();
        WorkspaceInfo workspaceInfo = catalog.getWorkspaceByName("cgf");
        MetadataMap metadataMap = workspaceInfo.getMetadata();
        HashSet relationshipClassSet =
                (HashSet) metadataMap.get(RelationshipDAO.RELATIONSHIP_CLASS_KEY);
        RelationshipClass relationshipClassCheck = new RelationshipClass();
        relationshipClassCheck.setWorkspaceName("cgf");
        relationshipClassCheck.setOriginTable("Lines");
        relationshipClassCheck.setDestinationTable("Points");
        relationshipClassCheck.setOriginPrimaryKey("id");
        relationshipClassCheck.setOriginForeignKey("id");
        assertTrue(relationshipClassSet.contains(relationshipClassCheck));
    }

    /**
     * Tests retrieving RelationshipClasses by the relationshipID, the full composite
     * id(OriginTable,DestinationTable,OriginPrimaryKey,OriginForeignKey), a list by originTable,
     * and a list by DestinationTable
     *
     * @throws Exception
     */
    @Test
    public void getRelationship() throws Exception {
        createTestRelationship();
        Gson gson = new Gson();
        String q = query("cgf", "/1");
        JSON result = getAsJSON(q);
        String strJson = String.valueOf(result);
        assertTrue(strJson + " is a JSON object", result instanceof JSONObject);
        RelationshipClass bean3 = gson.fromJson(strJson, RelationshipClass.class);
        assertTrue(bean3.getOriginPrimaryKey().equals("id"));

        q =
                query(
                        "cgf",
                        "/originTable/Lines/destinationTable/Points/originPrimaryKey/id/originForeignKey/id");
        JSON result3 = getAsJSON(q);
        String strJson3 = String.valueOf(result);
        assertTrue(strJson3 + " is a JSON object", result3 instanceof JSONObject);
        RelationshipClass bean = gson.fromJson(strJson3, RelationshipClass.class);

        assertTrue(bean.getOriginPrimaryKey().equals("id"));

        q = query("cgf", "/originTable/Lines");
        JSON result2 = getAsJSON(q);
        assertTrue(String.valueOf(result) + " is a JSON array", result2 instanceof JSONArray);
        JSONArray jsonarray1 = (JSONArray) result2;
        RelationshipClass bean2 =
                gson.fromJson(jsonarray1.get(0).toString(), RelationshipClass.class);
        assertTrue(bean2.getOriginPrimaryKey().equals("id"));

        q = query("cgf", "/destinationTable/Points");
        deleteAsServletResponse(q);
    }

    /**
     * Tests deleting a RelationshipClass in the cgf workspace using the full composite
     * id(OriginTable,DestinationTable,OriginPrimaryKey,OriginForeignKey) and the
     * RelationshipClassId
     *
     * @throws Exception
     */
    @Test
    public void deleteRelationship() throws Exception {
        createTestRelationship();
        String q = query("cgf", "");
        String body =
                "{"
                        + "   \"workspaceName\":\"cgf\","
                        + "   \"originTable\":\"Lines\","
                        + "   \"destinationTable\":\"Points\","
                        + "   \"relationshipType\":\"SIMPLE\","
                        + "   \"forwardLabel\":\"r\","
                        + "   \"backwardLabel\":\"r\","
                        + "   \"messageDirection\":\"NONE\","
                        + "   \"cardinality\":\"ONE_TO_ONE\","
                        + "   \"attributed\":false,"
                        + "   \"originPrimaryKey\":\"id\","
                        + "   \"originForeignKey\":\"id\","
                        + "   \"destinationPrimaryKey\":\"r\","
                        + "   \"destinationForeignKey\":\"r\""
                        + "}";
        MockHttpServletResponse response = deleteAsServletResponse(q, body, "application/json");
        assertTrue(response.getContentAsString().equals("true"));
        RelationshipClass checkRelationshipClass = getRelationshipClass();
        assertNull(checkRelationshipClass);

        createTestRelationship();
        q = query("cgf", "/delete/1");
        MockHttpServletResponse response2 = getAsServletResponse(q);
        assertTrue(response.getContentAsString().equals("true"));
        RelationshipClass checkRelationshipClass2 = getRelationshipClass();
        assertNull(checkRelationshipClass2);
    }

    /**
     * Tests updating a RelationshipClass using a RelationshipClass object that has the id fields
     * populated(OriginTable,DestinationTable,OriginPrimaryKey,OriginForeignKey)
     *
     * @throws Exception
     */
    @Test
    public void updateRelationship() throws Exception {
        createTestRelationship();
        RelationshipClass checkRelationshipClass = getRelationshipClass();
        assertTrue(checkRelationshipClass.getDestinationPrimaryKey().equals("r"));

        String q = query("cgf", "");
        String body =
                "{"
                        + "   \"workspaceName\":\"cgf\","
                        + "   \"originTable\":\"Lines\","
                        + "   \"destinationTable\":\"Points\","
                        + "   \"relationshipType\":\"SIMPLE\","
                        + "   \"forwardLabel\":\"r\","
                        + "   \"backwardLabel\":\"r\","
                        + "   \"messageDirection\":\"NONE\","
                        + "   \"cardinality\":\"ONE_TO_ONE\","
                        + "   \"attributed\":false,"
                        + "   \"originPrimaryKey\":\"id\","
                        + "   \"originForeignKey\":\"id\","
                        + "   \"destinationPrimaryKey\":\"s\","
                        + "   \"destinationForeignKey\":\"r\""
                        + "}";
        MockHttpServletResponse response = putAsServletResponse(q, body, "application/json");
        List<RelationshipClass> relationshipClassList =
                relationshipDAO.getAllRelationshipClassesByWorkspace("cgf");
        assertTrue(relationshipClassList.get(0).getDestinationPrimaryKey().equals("s"));
    }
}
