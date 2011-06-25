/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2002-2008, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geoserver.spatialite;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Map;

import org.geotools.data.jdbc.FilterToSQL;
import org.geotools.geometry.jts.Geometries;
import org.geotools.jdbc.BasicSQLDialect;
import org.geotools.jdbc.JDBCDataStore;
import org.geotools.referencing.CRS;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import com.vividsolutions.jts.io.WKTWriter;

/**
 * Dialect for SpatiaLite embedded database.
 * 
 * @author Justin Deoliveira, OpenGeo
 *
 *
 *
 * @source $URL: http://svn.osgeo.org/geotools/trunk/modules/plugin/jdbc/jdbc-spatialite/src/main/java/org/geotools/data/spatialite/SpatiaLiteDialect.java $
 */
public class SpatiaLiteDialect extends BasicSQLDialect {

    public static String SPATIALITE_SPATIAL_INDEX = "org.geotools.data.spatialite.spatialIndex";
    
    public SpatiaLiteDialect(JDBCDataStore dataStore) {
        super(dataStore);
    }

    @Override
    public void initializeConnection(Connection cx) throws SQLException {
        Statement st = cx.createStatement();
        try {
            st = cx.createStatement();
            
            //determine if the spatial metadata tables need to be created
            boolean initSpatialMetaData = false;
            try {
                st.execute( "SELECT count(*) from geometry_columns");
            }catch( SQLException e ) {
                initSpatialMetaData = true;
            }
            
            if ( initSpatialMetaData ) {
                st.execute( "SELECT InitSpatialMetaData()");
                st.close();
                st = cx.createStatement();
            }
            
            //determine if the spatial ref sys table needs to be loaded
            boolean loadSpatialRefSys = false;
            ResultSet rs = st.executeQuery( "SELECT * FROM spatial_ref_sys");
            try {
                loadSpatialRefSys = !rs.next();
            }
            finally {
                dataStore.closeSafe( rs );
            }
            
            if ( loadSpatialRefSys ) {
                try {
                    BufferedReader in = new BufferedReader( new InputStreamReader( 
                        getClass().getResourceAsStream( "init_spatialite-2.3.sql") ) );
                    String line = null;
                    while( (line = in.readLine() ) != null ) {
                        st.execute( line );
                    }
                    
                    in.close();
                }
                catch( IOException e ) {
                    throw new RuntimeException( "Error reading spatial ref sys file", e );
                }
            }
        }
        finally {
            dataStore.closeSafe( st );
        }
    }
    
    @Override
    public Class<?> getMapping(ResultSet columnMetaData, Connection cx) throws SQLException {
        //the sqlite jdbc driver maps geometry type to varchar, so do a lookup
        // in the geometry_columns table
        String tbl = columnMetaData.getString( "TABLE_NAME");
        String col = columnMetaData.getString( "COLUMN_NAME");
        
        String sql = "SELECT type FROM geometry_columns " + 
            "WHERE f_table_name = '" + tbl + "' " + 
            "AND f_geometry_column = '" + col + "'";
        LOGGER.fine( sql );
        
        Statement st = cx.createStatement();
        try {
            ResultSet rs = st.executeQuery( sql );
            try {
                if ( rs.next() ) {
                    String type = rs.getString( "type" );
                    return Geometries.getForName( type ).getBinding();
                }
            }
            finally {
                dataStore.closeSafe( rs ); 
            }
        }
        finally {
            dataStore.closeSafe( st );
        }
        
        return null;
    }
    
    @Override
    public void registerClassToSqlMappings(Map<Class<?>, Integer> mappings) {
        super.registerClassToSqlMappings(mappings);
        mappings.put( Geometries.POINT.getBinding(), Geometries.POINT.getSQLType() );
        mappings.put( Geometries.LINESTRING.getBinding(), Geometries.LINESTRING.getSQLType() );
        mappings.put( Geometries.POLYGON.getBinding(), Geometries.POLYGON.getSQLType() );
        mappings.put( Geometries.MULTIPOINT.getBinding(), Geometries.MULTIPOINT.getSQLType() );
        mappings.put( Geometries.MULTILINESTRING.getBinding(), Geometries.MULTILINESTRING.getSQLType() );
        mappings.put( Geometries.MULTIPOLYGON.getBinding(), Geometries.MULTIPOLYGON.getSQLType() );
        mappings.put( Geometries.GEOMETRY.getBinding(), Geometries.GEOMETRY.getSQLType() );
        mappings.put( Geometries.GEOMETRYCOLLECTION.getBinding(), Geometries.GEOMETRYCOLLECTION.getSQLType() );
        
        //override some internal defaults
        mappings.put(Long.class, Types.INTEGER);
        mappings.put(Double.class, Types.REAL);
    }
    
    @Override
    public String getGeometryTypeName(Integer type) {
        return Geometries.getForSQLType( type ).getName();
    }
    
    @Override
    public Integer getGeometrySRID(String schemaName, String tableName, String columnName,
            Connection cx) throws SQLException {
        String sql = "SELECT srid FROM geometry_columns " + 
            "WHERE f_table_name = '" + tableName + "' " + 
            "AND f_geometry_column = '" + columnName + "'";
        Statement st = cx.createStatement();
        try {
            LOGGER.fine( sql );
            ResultSet rs = st.executeQuery( sql );
            try {
                if ( rs.next() ) {
                    return Integer.valueOf( rs.getInt( 1 ) );
                }
            }
            finally {
                dataStore.closeSafe( rs );
            }
        }
        finally {
            dataStore.closeSafe( st );
        }
        
        return super.getGeometrySRID(schemaName, tableName, columnName, cx);
    }
    
    @Override
    public void encodeGeometryColumn(GeometryDescriptor gatt, int srid, StringBuffer sql) {
        sql.append( "AsText(");
        encodeColumnName( gatt.getLocalName(), sql);
        sql.append( ")||';").append(srid).append("'");
    }
    
    @Override
    public Geometry decodeGeometryValue(GeometryDescriptor descriptor, ResultSet rs, int column,
            GeometryFactory factory, Connection cx) throws IOException, SQLException {
        String string = rs.getString( column );
        if ( string == null || "".equals( string.trim() ) ) {
            return null;
        }
        
        String[] split = string.split( ";" );
        String wkt = split[0];
        try {
            return new WKTReader(factory).read( wkt );
        }
        catch( ParseException e ) {
            throw (IOException) new IOException().initCause( e );
        }
        
    }
    
    @Override
    public void encodeGeometryValue(Geometry value, int srid, StringBuffer sql) throws IOException {
        sql.append("GeomFromText('") .append( new WKTWriter().write( value ) ).append( "',")
            .append(srid).append(")");
    }

    @Override
    public Geometry decodeGeometryValue(GeometryDescriptor descriptor, ResultSet rs, String column,
            GeometryFactory factory, Connection cx) throws IOException, SQLException {
        return null;
    }
    
    @Override
    public void encodeGeometryEnvelope(String tableName, String geometryColumn, StringBuffer sql) {
        sql.append("asText(envelope(");
        encodeColumnName(geometryColumn, sql);
        sql.append( "))");
    }
    
    @Override
    public Envelope decodeGeometryEnvelope(ResultSet rs, int column, Connection cx)
            throws SQLException, IOException {
        String wkt = rs.getString( column );
        if ( wkt != null ) {
            try {
                return new WKTReader().read( wkt ).getEnvelopeInternal();
            } 
            catch (ParseException e) {
                throw (IOException) new IOException("Error decoding envelope bounds").initCause( e );
            }
        }
        
        return null;
    }
    
    @Override
    public void postCreateTable(String schemaName, SimpleFeatureType featureType, Connection cx)
            throws SQLException, IOException {
        
        //create any geometry columns entries after the fact
        for ( AttributeDescriptor ad : featureType.getAttributeDescriptors() ) {
            if ( ad instanceof GeometryDescriptor ) {
                GeometryDescriptor gd = (GeometryDescriptor) ad;
                StringBuffer sql = new StringBuffer( "INSERT INTO geometry_columns VALUES (");
                
                //table name
                sql.append( "'").append( featureType.getTypeName() ).append( "'," );
                
                //geometry name
                sql.append( "'").append( gd.getLocalName() ).append( "',");
                
                //type
                String gType = Geometries.getForBinding((Class<? extends Geometry>) gd.getType().getBinding() ).getName();
                if ( gType == null ) {
                    throw new IOException( "Unknown geometry type: " + gd.getType().getBinding() );
                }
                sql.append( "'").append( gType ).append( "',");
                
                //coord dimension
                sql.append( 2 ).append( ",");
                
                //srid 
                Integer epsgCode = null;
                if ( gd.getCoordinateReferenceSystem() != null ) {
                    CoordinateReferenceSystem crs = gd.getCoordinateReferenceSystem();
                    try {
                        epsgCode = CRS.lookupEpsgCode( crs , true );
                    } 
                    catch (Exception e) {}
                }
                if ( epsgCode == null ) {
                    throw new IOException( "Unable to find epsg code code.");
                }
                sql.append( epsgCode ).append( ",");
                
                //spatial index enabled
                sql.append( 0 ).append( ")");
                
                LOGGER.fine( sql.toString() );
                Statement st = cx.createStatement();
                try {
                    st.executeUpdate( sql.toString() );
                }
                finally {
                    dataStore.closeSafe( st );
                }
            }
        }
    }
    
    @Override
    public void postCreateFeatureType(SimpleFeatureType featureType, DatabaseMetaData metadata,
            String schemaName, Connection cx) throws SQLException {
        //figure out if the table has a spatial index and mark the feature type as so
        for (AttributeDescriptor ad : featureType.getAttributeDescriptors()) {
            if (!(ad instanceof GeometryDescriptor)) {
                continue;
            }
            
            GeometryDescriptor gd = (GeometryDescriptor) ad;
            String idxTableName = "idx_" + featureType.getTypeName() + "_" + gd.getLocalName();
            
            ResultSet rs = metadata.getTables(null, schemaName, idxTableName, new String[]{"TABLE"});
            try {
                if (rs.next()) {
                    gd.getUserData().put(SPATIALITE_SPATIAL_INDEX, idxTableName);
                }
            }
            finally {
                dataStore.closeSafe(rs);
            }
        }
    }
    
    @Override
    public boolean lookupGeneratedValuesPostInsert() {
        return true;
    }
    
    @Override
    public Object getLastAutoGeneratedValue(String schemaName, String tableName, String columnName,
            Connection cx) throws SQLException {
        Statement st = cx.createStatement();
        try {
            ResultSet rs = st.executeQuery( "SELECT last_insert_rowid();");
            try {
                if (rs.next()) {
                    return rs.getInt( 1 );
                }
            }
            finally {
                dataStore.closeSafe(rs);
            }
        }
        finally {
            dataStore.closeSafe(st);
        }
        
        return null;
    }
    
    @Override
    public boolean isLimitOffsetSupported() {
        //TODO: figure out why aggregate functions don't work with limit offset applied 
        return false;
    }
    
    @Override
    public void applyLimitOffset(StringBuffer sql, int limit, int offset) {
        if(limit > 0 && limit < Integer.MAX_VALUE) {
            sql.append(" LIMIT " + limit);
            if(offset > 0) {
                sql.append(" OFFSET " + offset);
            }
        } else if(offset > 0) {
            sql.append(" OFFSET " + offset);
        }
    }
    
    @Override
    public FilterToSQL createFilterToSQL() {
        return new SpatiaLiteFilterToSQL();
    }
}
