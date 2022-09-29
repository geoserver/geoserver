/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence.server.xstream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.geofence.core.model.Rule;
import org.geoserver.geofence.core.model.enums.GrantType;
import org.geoserver.geofence.server.rest.xml.Batch;
import org.geoserver.geofence.server.rest.xml.BatchOperation;
import org.geoserver.geofence.server.rest.xml.JaxbAdminRule;
import org.geoserver.geofence.server.rest.xml.JaxbRule;
import org.geoserver.geofence.server.rest.xml.JaxbRule.Limits;
import org.geoserver.geofence.server.rest.xml.JaxbRuleList;
import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.io.WKTReader;

/** @author Alessio Fabiani, GeoSolutions S.A.S. */
public class GeofencePersisterSerializationTest {

    private XStreamPersister xmlPersister;
    private XStreamPersister jsonPersister;

    @Before
    public void setup() {
        XStreamPersisterFactory xpf = new XStreamPersisterFactory();
        GeoFenceServerXStreamInitializer initializer = new GeoFenceServerXStreamInitializer();
        xpf.addInitializer(initializer);

        xmlPersister = xpf.createXMLPersister();

        jsonPersister = xpf.createJSONPersister();
    }

    @Test
    public void testDeserialization() throws Exception {
        String xml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<Rule>"
                        + "<access>LIMIT</access>"
                        + "<layer>DE_USNG_UTM18</layer>"
                        + "<limits>"
                        + "     <allowedArea>SRID=4326;MULTIPOLYGON (((-75 -90, -75 90, 75 90, 75 -90, -75 -90)))</allowedArea>"
                        + "     <catalogMode>HIDDEN</catalogMode>"
                        + "</limits>"
                        + "<priority>1</priority>"
                        + "<workspace>geonode</workspace>"
                        + "</Rule>";

        ByteArrayInputStream bais = new ByteArrayInputStream(xml.getBytes(UTF_8));

        JaxbRule rule = xmlPersister.load(bais, JaxbRule.class);

        assertNotNull(rule);

        assertEquals("LIMIT", rule.getAccess());
        assertEquals("DE_USNG_UTM18", rule.getLayer());
        assertEquals("geonode", rule.getWorkspace());
        assertEquals(1, rule.getPriority().intValue());

        assertNotNull(rule.getLimits());

        assertEquals(
                "SRID=4326;MULTIPOLYGON (((-75 -90, -75 90, 75 90, 75 -90, -75 -90)))",
                rule.getLimits().getAllowedArea());

        assertEquals("HIDDEN", rule.getLimits().getCatalogMode());
    }

    @Test
    public void testSerialization() throws Exception {
        JaxbRule rule = new JaxbRule();
        rule.setPriority(1L);
        rule.setUserName("pippo");
        rule.setRoleName("clown");
        rule.setAddressRange("127.0.0.1/32");
        rule.setService("wfs");
        rule.setRequest("getFeature");
        rule.setWorkspace("workspace");
        rule.setLayer("layer");
        rule.setAccess("ALLOW");
        Limits limits = new Limits();
        limits.setCatalogMode("HIDDEN");
        WKTReader reader = new WKTReader();
        limits.setAllowedArea(
                (MultiPolygon)
                        reader.read("MULTIPOLYGON (((-75 -90, -75 90, 75 90, 75 -90, -75 -90)))"));
        rule.setLimits(limits);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        xmlPersister.save(rule, baos);
        baos.flush();

        String xml = new String(baos.toByteArray(), UTF_8);

        // System.err.println(xml);
        assertTrue(xml.contains("pippo"));
        assertTrue(xml.contains("clown"));
        assertTrue(xml.contains("HIDDEN"));
        assertTrue(xml.contains("MULTIPOLYGON (((-75 -90, -75 90, 75 90, 75 -90, -75 -90)))"));

        Rule rule2 = new Rule();
        rule2.setPriority(2L);
        rule2.setUsername("topolino");
        rule2.setRolename("minnie");
        rule2.setService("wfs");
        rule2.setRequest("getFeature");
        rule2.setWorkspace("workspace");
        rule2.setLayer("layer");
        rule2.setAccess(GrantType.ALLOW);

        Rule[] rules = {rule2};
        JaxbRuleList ruleList = new JaxbRuleList(Arrays.asList(rules));

        xmlPersister.save(ruleList, baos);
        baos.flush();

        xml = new String(baos.toByteArray(), UTF_8);

        // System.err.println(xml);
        assertTrue(xml.contains("topolino"));
        assertTrue(xml.contains("minnie"));
    }

    @Test
    public void testJSONBatchDeserialization() throws IOException {
        Batch batch = jsonPersister.load(getClass().getResourceAsStream("batch.json"), Batch.class);
        assertBatch(batch);
    }

    @Test
    public void testBatchXmlDeserialization() throws IOException {
        Batch batch = xmlPersister.load(getClass().getResourceAsStream("batch.xml"), Batch.class);
        assertBatch(batch);
    }

    private void assertBatch(Batch batch) {
        List<BatchOperation> operations = batch.getOperations();
        assertEquals(3, operations.size());
        for (BatchOperation op : operations) {
            switch (op.getType()) {
                case update:
                    assertEquals(BatchOperation.ServiceName.rules, op.getService());
                    assertEquals(3l, op.getId().longValue());
                    JaxbRule rule = (JaxbRule) op.getPayload();
                    assertEquals("ALLOW", rule.getAccess());
                    assertEquals("layer", rule.getLayer());
                    assertEquals(5l, rule.getPriority().longValue());
                    assertEquals("GETMAP", rule.getRequest());
                    assertEquals("WMS", rule.getService());
                    assertEquals("ws", rule.getWorkspace());
                    assertEquals("ROLE_AUTHENTICATED", rule.getRoleName());
                    break;
                case delete:
                    assertEquals(BatchOperation.ServiceName.rules, op.getService());
                    assertEquals(5l, op.getId().longValue());
                    break;
                default:
                    assertEquals(BatchOperation.TypeName.insert, op.getType());
                    assertEquals(BatchOperation.ServiceName.adminrules, op.getService());
                    JaxbAdminRule adminRule = (JaxbAdminRule) op.getPayload();
                    assertEquals("ADMIN", adminRule.getAccess());
                    assertEquals("ROLE_USER", adminRule.getRoleName());
                    assertEquals("ws", adminRule.getWorkspace());
                    assertEquals(2l, adminRule.getPriority().longValue());
                    break;
            }
        }
    }
}
