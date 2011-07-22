/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.spatialite;
 
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SimpleTimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipOutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.xml.namespace.QName;

import net.opengis.wfs.FeatureCollectionType;
import net.opengis.wfs.GetFeatureType;
import net.opengis.wfs.QueryType;

import org.apache.commons.io.FileUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.config.GeoServer;
import org.geoserver.data.util.IOUtils;
import org.geoserver.feature.RetypingFeatureCollection;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.template.GeoServerTemplateLoader;
import org.geoserver.wfs.WFSException;
import org.geoserver.wfs.WFSGetFeatureOutputFormat;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wfs.response.ShapeZipOutputFormat.FileNameSource;
import org.geotools.data.DataStore;
import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureWriter;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.Geometries;
import org.geotools.jdbc.JDBCDataStore;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.geotools.wfs.v1_1.WFS;
import org.geotools.wfs.v1_1.WFSConfiguration;
import org.geotools.xml.Encoder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.AttributeType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.feature.type.GeometryType;
import org.opengis.feature.type.Name;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.ReferenceIdentifier;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.InternationalString;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

import freemarker.template.Configuration;
import freemarker.template.Template;

import org.apache.commons.dbcp.BasicDataSource;
import org.geotools.jdbc.JDBCDataStore;
import org.geotools.jdbc.JDBCDataStoreFactory;
import org.geotools.jdbc.SQLDialect;
import org.sqlite.SQLiteConfig;
import org.spatialite.libs.MultiLibs;
/**
 *
 * WFS output format for a GetFeature operation in which the outputFormat is "spatialite".
 * The reference documentation for this format can be found in this link:
 * @link:http://www.gaia-gis.it/spatialite/docs.html.
 * 
 * Based on CSVOutputFormat.java and ShapeZipOutputFormat.java from geoserver 2.2.x
 *
 * @author ported to gs 2.2.x by Pablo Velazquez, Geotekne, pvelazquez@geotekne.com
 * @author ported to gs 2.2.x by Jose Macchi, Geotekne, jmacchi@geotekne.com
 *
 */

public class SpatiaLiteOutputFormat extends WFSGetFeatureOutputFormat {
    
    //A Hashtable to convert the native data types to data types supported by SQLite
    private Hashtable<String, String> dataTypes = new Hashtable();
    private Hashtable<Class<? extends Geometry>, String> geomTypes = new Hashtable();
    private String driverClassName = "org.sqlite.JDBC";
    
    public SpatiaLiteOutputFormat(GeoServer gs) {
        super(gs,"SpatiaLite");
        
        //Initializing the dataTypes Hashtable
        dataTypes.put("STRING","TEXT");
        dataTypes.put("INT","INTEGER");
        dataTypes.put("LONG","INTEGER");
        dataTypes.put("BOOL","INTEGER");
        dataTypes.put("DOUBLE","REAL");
        dataTypes.put("FLOAT","REAL");
        
        //Initializing the geomTypes Hashtable
        geomTypes.put(Geometries.POINT.getBinding(),"POINT");
        geomTypes.put(Geometries.LINESTRING.getBinding(),"LINESTRING");
        geomTypes.put(Geometries.POLYGON.getBinding(),"POLYGON");
        geomTypes.put(Geometries.MULTIPOINT.getBinding(),"MULTIPOINT");
        geomTypes.put(Geometries.MULTILINESTRING.getBinding(),"MULTILINESTRING");
        geomTypes.put(Geometries.MULTIPOLYGON.getBinding(),"MULTIPOLYGON");
        geomTypes.put(Geometries.GEOMETRY.getBinding(), "GEOMETRY");
        geomTypes.put(Geometries.GEOMETRYCOLLECTION.getBinding(), "GEOMETRYCOLLECTION");


    }

    /**
     * @return "application/x-sqlite3";
     */
    @Override
    public String getMimeType(Object value, Operation operation)
            throws ServiceException {
        return "application/x-sqlite3";
    }

    @Override
    protected boolean canHandleInternal(Operation operation) {
        return super.canHandleInternal(operation);
    }
    
    @Override
    protected void write(FeatureCollectionType featureCollection,
    OutputStream output, Operation getFeature) throws IOException,
    ServiceException {
    List<SimpleFeatureCollection> collections = new ArrayList<SimpleFeatureCollection>();
    collections.addAll(featureCollection.getFeature());
    Charset charset = Charset.forName("UTF-8");
    write(collections, charset, output, (GetFeatureType) getFeature.getParameters()[0]);
    }
    
    protected void write(List<SimpleFeatureCollection> collections, Charset charset, OutputStream output, GetFeatureType request) 
    throws IOException, ServiceException {
        
        Connection conn = null;
        
        /**
         * Get the necessary JDBC object.
         */
        try {
            Class.forName(this.driverClassName);
        } catch (ClassNotFoundException e) {
            System.out.println(e);
        }
        /**
         * base location to temporally store spatialite database files
         */
        File tempDir = File.createTempFile("spatialitemp", ".sqlite");

        /**
         * enables load extension
         */
        SQLiteConfig config = new SQLiteConfig();
        config.enableLoadExtension(true);
        
        /**
         * the Url for the temporally sqlite file
         */
        String JDBCFileUrl = tempDir.getAbsolutePath();
        
        try {
            //create a connection to database
            conn = DriverManager.getConnection("jdbc:sqlite:"+JDBCFileUrl,config.toProperties());
            Statement stmt = conn.createStatement();
            stmt.setQueryTimeout(30);
            
            /**
             * A string to store the statements to run to create the Spatialite DataBase 
             */
            String sql = null;
            conn.setAutoCommit(false);
            String spatialiteLibraryUrl = MultiLibs.loadExtension();
            sql = "SELECT load_extension('"+spatialiteLibraryUrl+"');";
            stmt.execute(sql);
            sql = "SELECT InitSpatialMetaData();";
            stmt.execute(sql);
            conn.commit();
            
            /**
             * A string to store the names of the columns that will be used to populate the table 
             */
            String column_names = null;
            
            //We might get multiple feature collections in our response so we need to
            //write out multiple tables, one for each query response.
            for (SimpleFeatureCollection fc : collections) {
                
                //get the current feature
                SimpleFeatureType ft = fc.getSchema();
                
                //To check if the current feature has a geometry.
                String the_geom = null;
                if(ft.getGeometryDescriptor() != null) {
                    the_geom = ft.getGeometryDescriptor().getLocalName();
                } 
                
                //Get the table name for the current feature
                String tbl_name = ft.getName().getLocalPart();
                
                
                /**
                 * Create the table for the current feature as follows:
                 * - first get the statement for create the table
                 * - execute the statement
                 * - second get the statement for add the geometry (if has one) 
                 * - execute the statement
                 */
                
                //Initialize the "create table" query.
                column_names = "";
                int column_cnt = 0;
                sql = "CREATE TABLE "+tbl_name;
                sql += " ( PK_UID INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT";
                
                //Get the columns names for the table tbl_name
                for ( int i = 0; i < ft.getAttributeCount(); i++ ) {
                    AttributeDescriptor ad = ft.getDescriptor( i );
                    if (ad.getLocalName() != the_geom){
                        sql += ", "+prepareColumnHeader( ad );
                        column_names += ad.getLocalName();
                        column_cnt++;
                        if ( i < ft.getAttributeCount()-1 ){
                            column_names += ", ";
                        }
                           
                    }
                }
                sql += ");";
                // Finish creating the table
                
                System.out.println(sql);
                stmt.execute(sql);
                conn.commit();
                
                int srid = 0;
                //If the table : "tbl_name" has a geometry, then i write the sql to add the geometry
                if (the_geom != null){
                    sql = "SELECT AddGeometryColumn('"+tbl_name+"', ";
                    //get the geometry type
                    sql += "'"+the_geom+"', ";
                    //get the SRID.
                    srid = getSpatialSRID(ft.getCoordinateReferenceSystem());
                    sql += srid+", ";
                    //get the Geometry type.
                    String geom_type = getSpatialGeometryType(ft);
                    if (geom_type == null){
                        throw new WFSException("Error while adding the geometry column in table " 
                        + tbl_name+ ", unrecognized geometry type");
                    }
                    sql += "'"+geom_type+"', ";
                    //get Dimensions, we only works whit 2 dimensions. 
                    String dimension = "XY";
                    sql += "'" + dimension + "'";
                    sql += " );";
                }
                //finish creating the geometry column.
                System.out.println(sql);
                stmt.execute(sql);
                conn.commit();
                
                
                /**
                 * Populates the table for the current feature as follows:
                 * For each row
                 *      - first: configure the statement with the appropriates fields.
                 *      - second: add to the statement the field the_geom if has a geometry.
                 *      - third: configure the statement with the appropriates values.
                 *      (if has a geometry i add that value) 
                 *      - execute the statement
                 * Finally commit.
                 */
                //Start populating the table: tbl_name.
                SimpleFeatureIterator i = fc.features();
                try
                {
                    while( i.hasNext() ) {
                        SimpleFeature row = i.next();
                        sql = "INSERT INTO "+tbl_name+" ("+column_names;
                        //if has a geometry, i add the field the_geom.
                        if (the_geom != null)
                            if (column_cnt > 0 )
                            {
                                sql += ", "+the_geom+" ) ";
                            }
                            else
                            {
                                sql += the_geom+") ";
                            }
    
                        else{
                            sql += ") ";
                        }
    
                        //I store the default geometry value, so i can omit it and add at the end.
                        Object geom_data = row.getDefaultGeometry();
                        sql += "VALUES (";
                        for ( int j = 0; j < row.getAttributeCount(); j++ ) {
                            Object rowAtt = row.getAttribute( j );
                            if (!rowAtt.equals(geom_data)){
                                if ( rowAtt != null ){
                                    //We just transform all content to String.
                                    sql += "'"+rowAtt.toString()+"'";
                                }
                                if ( j < row.getAttributeCount()-1 ) {
                                    sql += ", ";    
                                }
                            }
                        }
                        
                        //Finally if has geometry, insert the geometry data.
                        if (the_geom != null){
                            if (column_cnt > 0 ){
                                sql +=  ", ";
                            }
                            sql += "GeomFromText('"+prepareGeom(geom_data.toString())+"', "+srid+")";
                        }
                        sql += ");";
                        System.out.println(sql);
                        stmt.executeUpdate(sql);
                    }
                    conn.commit();
                }
                finally 
                {
                    fc.close( i );
                }
            }
        } catch (SQLException e) {
            System.out.println(e);
        }
        
        /**
         * A FileInputStream to read the tempDir in a byte array
         * so i can write this in the OutputStream output and flush it.
         */
        FileInputStream JDBCIn = new FileInputStream(tempDir);
        int longitud = JDBCIn.available();
        byte[] datos = new byte[longitud];
        JDBCIn.read(datos);
        output.write(datos);
    }
    
    public String getCapabilitiesElementName() {
        return "SPATIALITE";
    }
    
    @Override
    public String[][] getHeaders(Object value, Operation operation) throws ServiceException {
        SimpleFeatureCollection fc = (SimpleFeatureCollection) ((FeatureCollectionType) value).getFeature().get(0);
        GetFeatureType request = (GetFeatureType) OwsUtils.parameter(operation.getParameters(),
                GetFeatureType.class);
        String outputFileName = null;
        if (request != null) {
            Map<String, ?> formatOptions = request.getFormatOptions();
            outputFileName = (String) formatOptions.get("FILENAME");
        }
        if (outputFileName == null) {
            outputFileName = ((QName) ((QueryType) request.getQuery().get(0)).getTypeName().get(0))
                            .getLocalPart();
        }
        return (String[][]) new String[][] {
                { "Content-Disposition", "attachment; filename=" + outputFileName + ".sqlite" }
            };
    }
    
    
    //Returns the geometry type of a feature.
    //If the feature doesn't have a recognized geometry type this return a null value.
    
    private String getSpatialGeometryType(SimpleFeatureType featureType){
        Class<?> geomType = featureType.getGeometryDescriptor().getType().getBinding();
        return (String)geomTypes.get(geomType);
        
    }
    
    //Get the Current SRID of a Feature.
    //If the feature doesn't have a SRID, this return SRID = "-1" (default SpatiaLite SRID)
    private int getSpatialSRID(CoordinateReferenceSystem crs){
        try {
            return CRS.lookupEpsgCode(crs, true);
        } catch (FactoryException e) {
            System.out.println(e.getMessage());
            LOGGER.log(Level.FINER, e.getMessage(), e);
            return -1;
        }
    }
    
    //Prepares the column headers whit the format:
    //         "COLUMN_NAME" + "COLUMN_TYPE"
    private String prepareColumnHeader( AttributeDescriptor ad ){
        //I split the binding, and get the last split, that represents the data type
        String[] split = ad.getType().getBinding().getName().split("\\.");
        String column_type = (String)dataTypes.get(split[split.length-1].toUpperCase());
    return ad.getLocalName().toUpperCase()+" "+column_type;
    }
    
    /**
     * This method return a prepared MULTIPOINT geometry if is MULTIPOINT (We need do this
     * because MULTIPOINT Feature format is: MULTIPOINT ((x y),(x y),(x y)) and
     * MULTIPOINT Spatialite format is: MULTIPOINT (x y, x y, x y))
     * @param theGeom;
     * @return value;
     */
    
    //Need to solve the problem whit GEOMETRY COLLECTIONS! (if a GEOMETRY COLLECTION have MULTIPOINTS, all of them need to be Fixed)
    private String prepareGeom (String theGeom){
        String value = theGeom;
        if ((boolean)theGeom.contains("MULTIPOINT"))
        {
            value = value.replaceAll("\\(\\(", "*");
            value = value.replaceAll("\\)\\)", "#");
            value = value.replaceAll("\\(", "");
            value = value.replaceAll("\\)", "");
            value = value.replaceAll("\\*", "(");
            value = value.replaceAll("\\#", ")");
        }
        return value;
    }

}
