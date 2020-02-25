/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.catalog;

import static org.custommonkey.xmlunit.XMLAssert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.rest.RestBaseController;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

public class FeatureTypeControllerWFSTest extends CatalogRESTTestSupport {

    private static String BASEPATH = RestBaseController.ROOT_PATH;

    @Before
    public void removePropertyStores() {
        removeStore("gs", "pds");
        removeStore("gs", "ngpds");
    }

    @Before
    public void addPrimitiveGeoFeature() throws IOException {
        revertLayer(SystemTestData.PRIMITIVEGEOFEATURE);
    }

    @Test
    public void testGetAllByWorkspace() throws Exception {
        Document dom = getAsDOM(BASEPATH + "/workspaces/sf/featuretypes.xml");
        assertEquals(
                catalog.getFeatureTypesByNamespace(catalog.getNamespaceByPrefix("sf")).size(),
                dom.getElementsByTagName("featureType").getLength());
    }

    void addPropertyDataStore(boolean configureFeatureType) throws Exception {
        ByteArrayOutputStream zbytes = new ByteArrayOutputStream();
        ZipOutputStream zout = new ZipOutputStream(zbytes);

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(bytes));
        writer.write("_=name:String,pointProperty:Point\n");
        writer.write("pdsa.0='zero'|POINT(0 0)\n");
        writer.write("pdsa.1='one'|POINT(1 1)\n");
        writer.flush();

        zout.putNextEntry(new ZipEntry("pdsa.properties"));
        zout.write(bytes.toByteArray());
        bytes.reset();

        writer.write("_=name:String,pointProperty:Point\n");
        writer.write("pdsb.0='two'|POINT(2 2)\n");
        writer.write("pdsb.1='trhee'|POINT(3 3)\n");
        writer.flush();
        zout.putNextEntry(new ZipEntry("pdsb.properties"));
        zout.write(bytes.toByteArray());

        zout.flush();
        zout.close();

        String q = "configure=" + (configureFeatureType ? "all" : "none");
        put(
                BASEPATH + "/workspaces/gs/datastores/pds/file.properties?" + q,
                zbytes.toByteArray(),
                "application/zip");
    }

    void addGeomlessPropertyDataStore(boolean configureFeatureType) throws Exception {
        ByteArrayOutputStream zbytes = new ByteArrayOutputStream();
        ZipOutputStream zout = new ZipOutputStream(zbytes);

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(bytes));
        writer.write("_=name:String,intProperty:Integer\n");
        writer.write("ngpdsa.0='zero'|0\n");
        writer.write("ngpdsa.1='one'|1\n");
        writer.flush();

        zout.putNextEntry(new ZipEntry("ngpdsa.properties"));
        zout.write(bytes.toByteArray());
        bytes.reset();

        writer.write("_=name:String,intProperty:Integer\n");
        writer.write("ngpdsb.0='two'|2\n");
        writer.write("ngpdsb.1='trhee'|3\n");
        writer.flush();
        zout.putNextEntry(new ZipEntry("ngpdsb.properties"));
        zout.write(bytes.toByteArray());

        zout.flush();
        zout.close();

        String q = "configure=" + (configureFeatureType ? "all" : "none");
        put(
                BASEPATH + "/workspaces/gs/datastores/ngpds/file.properties?" + q,
                zbytes.toByteArray(),
                "application/zip");
    }

    /** Add a property data store with multiple feature types, but only configure the first. */
    void addPropertyDataStoreOnlyConfigureFirst() throws Exception {
        ByteArrayOutputStream zbytes = new ByteArrayOutputStream();
        ZipOutputStream zout = new ZipOutputStream(zbytes);

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(bytes));
        writer.write("_=name:String,pointProperty:Point\n");
        writer.write("pdsa.0='zero'|POINT(0 0)\n");
        writer.write("pdsa.1='one'|POINT(1 1)\n");
        writer.flush();

        zout.putNextEntry(new ZipEntry("pdsa.properties"));
        zout.write(bytes.toByteArray());
        bytes.reset();

        writer.write("_=name:String,pointProperty:Point\n");
        writer.write("pdsb.0='two'|POINT(2 2)\n");
        writer.write("pdsb.1='trhee'|POINT(3 3)\n");
        writer.flush();
        zout.putNextEntry(new ZipEntry("pdsb.properties"));
        zout.write(bytes.toByteArray());

        zout.flush();
        zout.close();

        String q = "configure=first";
        put(
                BASEPATH + "/workspaces/gs/datastores/pds/file.properties?" + q,
                zbytes.toByteArray(),
                "application/zip");
    }

    @Test
    public void testPostAsXML() throws Exception {
        Document dom = getAsDOM("wfs?request=getfeature&typename=sf:pdsa");
        assertEquals("ows:ExceptionReport", dom.getDocumentElement().getNodeName());

        addPropertyDataStore(false);
        String xml =
                "<featureType>"
                        + "<name>pdsa</name>"
                        + "<nativeName>pdsa</nativeName>"
                        + "<srs>EPSG:4326</srs>"
                        + "<nativeCRS>EPSG:4326</nativeCRS>"
                        + "<nativeBoundingBox>"
                        + "<minx>0.0</minx>"
                        + "<maxx>1.0</maxx>"
                        + "<miny>0.0</miny>"
                        + "<maxy>1.0</maxy>"
                        + "<crs>EPSG:4326</crs>"
                        + "</nativeBoundingBox>"
                        + "<store>pds</store>"
                        + "</featureType>";
        MockHttpServletResponse response =
                postAsServletResponse(
                        BASEPATH + "/workspaces/gs/datastores/pds/featuretypes/", xml, "text/xml");

        assertEquals(201, response.getStatus());
        assertNotNull(response.getHeader("Location"));
        assertTrue(
                response.getHeader("Location")
                        .endsWith("/workspaces/gs/datastores/pds/featuretypes/pdsa"));

        dom = getAsDOM("wfs?request=getfeature&typename=gs:pdsa");
        assertEquals("wfs:FeatureCollection", dom.getDocumentElement().getNodeName());
        assertEquals(2, dom.getElementsByTagName("gs:pdsa").getLength());
    }

    @Test
    public void testPostAsXMLInlineStore() throws Exception {
        Document dom = getAsDOM("wfs?request=getfeature&typename=sf:pdsa");
        assertEquals("ows:ExceptionReport", dom.getDocumentElement().getNodeName());

        addPropertyDataStore(false);
        String xml =
                "<featureType>"
                        + "<name>pdsa</name>"
                        + "<nativeName>pdsa</nativeName>"
                        + "<srs>EPSG:4326</srs>"
                        + "<nativeCRS>EPSG:4326</nativeCRS>"
                        + "<nativeBoundingBox>"
                        + "<minx>0.0</minx>"
                        + "<maxx>1.0</maxx>"
                        + "<miny>0.0</miny>"
                        + "<maxy>1.0</maxy>"
                        + "<crs>EPSG:4326</crs>"
                        + "</nativeBoundingBox>"
                        + "<store>pds</store>"
                        + "</featureType>";
        MockHttpServletResponse response =
                postAsServletResponse(BASEPATH + "/workspaces/gs/featuretypes/", xml, "text/xml");

        assertEquals(201, response.getStatus());
        assertNotNull(response.getHeader("Location"));
        assertTrue(response.getHeader("Location").endsWith("/workspaces/gs/featuretypes/pdsa"));

        dom = getAsDOM("wfs?request=getfeature&typename=gs:pdsa");
        assertEquals("wfs:FeatureCollection", dom.getDocumentElement().getNodeName());
        assertEquals(2, dom.getElementsByTagName("gs:pdsa").getLength());
    }

    @Test
    public void testPostAsJSON() throws Exception {
        Document dom = getAsDOM("wfs?request=getfeature&typename=sf:pdsa");
        assertEquals("ows:ExceptionReport", dom.getDocumentElement().getNodeName());

        addPropertyDataStore(false);
        String json =
                "{"
                        + "'featureType':{"
                        + "'name':'pdsa',"
                        + "'nativeName':'pdsa',"
                        + "'srs':'EPSG:4326',"
                        + "'nativeBoundingBox':{"
                        + "'minx':0.0,"
                        + "'maxx':1.0,"
                        + "'miny':0.0,"
                        + "'maxy':1.0,"
                        + "'crs':'EPSG:4326'"
                        + "},"
                        + "'nativeCRS':'EPSG:4326',"
                        + "'store':'pds'"
                        + "}"
                        + "}";
        MockHttpServletResponse response =
                postAsServletResponse(
                        BASEPATH + "/workspaces/gs/datastores/pds/featuretypes/",
                        json,
                        "text/json");

        assertEquals(201, response.getStatus());
        assertNotNull(response.getHeader("Location"));
        assertTrue(
                response.getHeader("Location")
                        .endsWith("/workspaces/gs/datastores/pds/featuretypes/pdsa"));

        dom = getAsDOM("wfs?request=getfeature&typename=gs:pdsa");
        assertEquals("wfs:FeatureCollection", dom.getDocumentElement().getNodeName());
        assertEquals(2, dom.getElementsByTagName("gs:pdsa").getLength());
    }
}
