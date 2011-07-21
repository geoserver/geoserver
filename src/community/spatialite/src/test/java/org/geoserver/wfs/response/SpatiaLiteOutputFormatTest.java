/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wfs.response;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletOutputStream;

import org.geoserver.wfs.WFSTestSupport;
import org.spatialite.libs.MultiLibs;
import org.sqlite.SQLiteConfig;

import com.mockrunner.mock.web.MockHttpServletResponse;
import com.mockrunner.mock.web.MockServletInputStream;

/**
 * Test the SpatiaLiteOutputFormat WFS extension.
 * @author Pablo Velazquez, Geotekne, info@geotekne.com
 * @author Jose Macchi, Geotekne, jmacchi@geotekne.com
 */
public class SpatiaLiteOutputFormatTest extends WFSTestSupport {

    private String TempDataBaseUrl = null;
    private String spatialiteLibraryUrl = null;
    
     protected void oneTimeSetUp( ) throws Exception {
         super.oneTimeSetUp();
         this.spatialiteLibraryUrl = MultiLibs.loadExtension();
         this.TempDataBaseUrl = null;
     }
     protected void oneTimeTearDown( ) throws Exception {
         super.oneTimeTearDown();
         new File(this.spatialiteLibraryUrl).delete();
     }

    /**
     * Creates a connection (SQLITE type) with a temporally
     * dataBase.sqlite 
     * @param responseInput
     * @param tbl_names
     * @param column_names
     * @param geometries
     * @return connection
     */
    private Connection createTempDataBaseConnection(ByteArrayInputStream responseInput) throws Exception{
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.out.println(e);
        }
        File tempDir = File.createTempFile("spatialitemptest", ".sqlite");
        FileOutputStream OStream = new FileOutputStream(tempDir);
        this.TempDataBaseUrl = tempDir.getAbsolutePath();
        
        int longitud = responseInput.available();
        byte[] datos = new byte[longitud];
        responseInput.read(datos);
        OStream.write(datos);
        SQLiteConfig config = new SQLiteConfig();
        config.enableLoadExtension(true);
        responseInput.close();
        OStream.close();
        
    return DriverManager.getConnection("jdbc:sqlite:"+this.TempDataBaseUrl,config.toProperties());
    }
    
    /**
     * Checks that spatialite contains all the tables (once per layer) with correct geometries.
     * Used by many tests to verify SpatiaLite structure.
     * @param responseInput
     * @param tbl_names
     * @param column_names
     * @param geometries
     */
    private void checkGeometries(ByteArrayInputStream responseInput,String[] tbl_names,
            String[] geom_columns,String[] geometries) throws Exception{
        Connection conn = createTempDataBaseConnection(responseInput);
        Statement stmt = conn.createStatement();
        ResultSet rs;
        stmt.execute("SELECT load_extension('"+this.spatialiteLibraryUrl+"')");
        for(int i =0 ;i < tbl_names.length; i++ ) {
            //System.out.println("SELECT GeometryType("+geom_columns[i]+") from "+tbl_names[i]);
            rs = stmt.executeQuery("SELECT GeometryType("+geom_columns[i]+") from "+tbl_names[i]);
            assertEquals(rs.getString(1),geometries[i]);
        }
        conn.close();
        new File(this.TempDataBaseUrl).delete();
    }

    /**
     * Test a request with multiple layers.
     * @throws Exception
     */
    public void testMultiReponse() throws Exception {
        MockHttpServletResponse resp = getAsServletResponse("wfs?request=GetFeature&typeName=Points,MPoints&outputFormat=spatialite");
        ByteArrayInputStream responseInput = testBasicResult(resp, "Points");
        checkGeometries(responseInput,new String[] {"Points","MPoints"},
                new String[] {"pointProperty","multiPointProperty"},new String[]{"POINT","MULTIPOINT"});
    }

    /**
     * Test SPATIALITE Mime format.
     * @throws Exception
     */
    public void testMIMEOutput() throws Exception {
        MockHttpServletResponse resp = getAsServletResponse("wfs?request=GetFeature&typeName=Points&outputFormat=spatialite");
        assertEquals("application/x-sqlite3", resp.getContentType());
    }
    
    /**
     * Test if exist WFS Error, checking for Mime Type.
     * If Mime Type is "application/xml", then an error has occurred
     * @throws Exception
     */
    public void testWFSError() throws Exception {
        MockHttpServletResponse resp = getAsServletResponse("wfs?request=GetFeature&typeName=Points&outputFormat=spatialite");
        assertNotSame("application/xml", resp.getContentType());
    }

    /**
     * Test the content disposition
     * @throws Exception
     */
    public void testContentDisposition() throws Exception {
        MockHttpServletResponse resp = getAsServletResponse("wfs?request=GetFeature&typeName=Points&outputFormat=spatialite");
        String featureName = "Points";
        assertEquals("attachment; filename=" + featureName + ".sqlite", resp
                .getHeader("Content-Disposition"));
    }

    /**
     * Test not null content.
     * @throws Exception
     */
    public void testContentNotNull() throws Exception {
        MockHttpServletResponse resp = getAsServletResponse("wfs?request=GetFeature&typeName=Points&outputFormat=spatialite");
        ByteArrayInputStream sResponse = getBinaryInputStream(resp);
        int dataLengh = sResponse.available();
        boolean contentNull = true;
        byte[] data = new byte[dataLengh];
        sResponse.read(data);
            for (byte aByte : data)
                if (aByte != 0){
                    contentNull = false;
                    System.out.println("Hay contenido");
                    break;
                }
        assertFalse(contentNull);
    }

    /**
     * Test a Point geometry.
     * @throws Exception
     */
    public void testPoints() throws Exception {
        MockHttpServletResponse resp = getAsServletResponse("wfs?request=GetFeature&typeName=Points&outputFormat=spatialite");
        ByteArrayInputStream responseInput = testBasicResult(resp, "Points");
        checkGeometries(responseInput,new String[] {"Points"},
                new String[] {"pointProperty"},new String[]{"POINT"});        
        
    }
    /**
     * Test a MultiPoint geometry.
     * @throws Exception
     */
    public void testMultiPoints() throws Exception {
        MockHttpServletResponse resp = getAsServletResponse("wfs?request=GetFeature&typeName=MPoints&outputFormat=spatialite");
        ByteArrayInputStream responseInput = testBasicResult(resp, "MPoints");
        checkGeometries(responseInput,new String[] {"MPoints"},
                new String[] {"multiPointProperty"},new String[]{"MULTIPOINT"});        
    }

    /**
     * Test a LineString geometry.
     * @throws Exception
     */
    public void testLines() throws Exception {
        MockHttpServletResponse resp = getAsServletResponse("wfs?request=GetFeature&typeName=Lines&outputFormat=spatialite");
        ByteArrayInputStream responseInput = testBasicResult(resp, "Lines");
        checkGeometries(responseInput,new String[] {"Lines"},
                new String[] {"lineStringProperty"},new String[]{"LINESTRING"});
    }

    /**
     * Test a MultiLineString geometry.
     * @throws Exception
     */
   public void testMultiLines() throws Exception {
        MockHttpServletResponse resp = getAsServletResponse("wfs?request=GetFeature&typeName=MLines&outputFormat=spatialite");
        ByteArrayInputStream responseInput = testBasicResult(resp, "MLines");
        checkGeometries(responseInput,new String[] {"MLines"},
                new String[] {"multiLineStringProperty"},new String[]{"MULTILINESTRING"});
    }

    /**
     * Test a Polygon geometry.
     * @throws Exception
     */
    public void testPolygons() throws Exception {
        MockHttpServletResponse resp = getAsServletResponse("wfs?request=GetFeature&typeName=Polygons&outputFormat=spatialite");
        ByteArrayInputStream responseInput = testBasicResult(resp, "Polygons");
        checkGeometries(responseInput,new String[] {"Polygons"},
                new String[] {"polygonProperty"},new String[]{"POLYGON"});
    }

    /**
     * Test a MultiPolygon geometry.
     * @throws Exception
     */
    public void testMultiPolygons() throws Exception {
        MockHttpServletResponse resp = getAsServletResponse("wfs?request=GetFeature&typeName=MPolygons&outputFormat=spatialite");
        ByteArrayInputStream responseInput = testBasicResult(resp, "MPolygons");
        checkGeometries(responseInput,new String[] {"MPolygons"},
                new String[] {"multiPolygonProperty"},new String[]{"MULTIPOLYGON"});
    }
    
    /**
     * Test if the SpatialMetaData has been properly loaded.
     * @throws Exception
     */
    public void testSpatialMetadata() throws Exception {
        MockHttpServletResponse resp = getAsServletResponse("wfs?request=GetFeature&typeName=Points&outputFormat=spatialite");
        ByteArrayInputStream responseInput = getBinaryInputStream(resp);
        Connection conn = createTempDataBaseConnection(responseInput);
        Statement stmt = conn.createStatement();
        boolean SpatialMetaData = true;
        try
        {
            stmt.execute("SELECT count(*) FROM geometry_columns");
        }catch( SQLException e ) {
            SpatialMetaData = false;
        }
        assertTrue(SpatialMetaData);
        conn.close();
        new File(this.TempDataBaseUrl).delete();
    }
    
    /**
     * Test format option FILENAME.
     * @throws Exception
     */
    public void testCustomFileName() throws Exception {
        MockHttpServletResponse resp = getAsServletResponse("wfs?request=GetFeature&format_options=FILENAME:customName&typeName=Points&outputFormat=spatialite");
        ByteArrayInputStream responseInput = testBasicResult(resp, "customName");
        checkGeometries(responseInput,new String[] {"Points"},
                new String[] {"pointProperty"},new String[]{"POINT"});
    }

    /**
     * Test basic extension functionality: mime/type, headers,
     * not empty output generation. 
     * @param resp
     * @param featureName
     * @return sResponse
     * @throws Exception
     */
    public ByteArrayInputStream testBasicResult(MockHttpServletResponse resp, String featureName)
            throws Exception {
        // check mime type
        assertEquals("application/x-sqlite3", resp.getContentType());
        // check the content disposition
        assertEquals("attachment; filename=" + featureName + ".sqlite", resp
                .getHeader("Content-Disposition"));
        ByteArrayInputStream sResponse = getBinaryInputStream(resp); 
        // check for content (without checking in detail)
        assertNotNull(sResponse);
        return sResponse;

    }

    /**
     * Test the ltypes format option.
     * @throws Exception
     */
   /* public void testCustomLineTypes() throws Exception {
        MockHttpServletResponse resp = getAsServletResponse("wfs?request=GetFeature&typeName=Lines&outputFormat=dxf&format_options=ltypes:DASHED!--_*_!0.5");
        String sResponse = testBasicResult(resp, "Lines");
        checkSequence(sResponse,new String[] {"DASHED"});
    }*/
    /**
     * Test the colors format option.
     * @throws Exception
     */
  /*  public void testCustomColors() throws Exception {
        MockHttpServletResponse resp = getAsServletResponse("wfs?request=GetFeature&typeName=Points,MPoints&outputFormat=dxf&format_options=colors:1,2");
        String sResponse = testBasicResult(resp, "Points_MPoints");
        checkSequence(sResponse,new String[] {"LAYER","LAYER","LAYER"," 62\n     1","LAYER"," 62\n     2"});        
    }*/
    
    /**
     * Test custom naming for layers.
     * @throws Exception
     */
  /*  public void testLayerNames() throws Exception {
        MockHttpServletResponse resp = getAsServletResponse("wfs?request=GetFeature&typeName=Points,MPoints&outputFormat=dxf&format_options=layers:MyLayer1,MyLayer2");
        String sResponse = testBasicResult(resp, "Points_MPoints");
        checkSequence(sResponse,new String[] {"LAYER","LAYER","LAYER","MYLAYER1","LAYER","MYLAYER2"});        
    }*/
    
    

}
