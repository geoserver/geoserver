package org.geoserver.cite;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFactorySpi;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureStore;
import org.geotools.data.FeatureWriter;
import org.geotools.data.Transaction;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.CRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.io.WKTReader;

public class Loader {

    static Logger LOGGER = Logger.getLogger( "org.geoserver.cite" );
    
    public static void main(String[] args ) throws Exception {
        String datasource = null;
        String service = null;
        for ( int i = 0; i < args.length; i++ ) {
            if ( "-d".equals( args[i]) && i < args.length-1) {
                datasource = args[++i];
            }
            if ( "-s".equals( args[i] ) && i < args.length-1) {
                service = args[++i];
            }
        }
        
        if ( datasource == null || service == null ) {
            usage();
            System.exit(1);
        }
        
        if ( !new File( datasource + ".properties").exists() ) {
            System.out.print( "No such file: " + datasource + ".properties");
        }
        Properties p = new Properties();
        p.load( new FileInputStream( datasource+".properties") );
        
        Loader l = new Loader();
        l.loadDataStore( p );
        
        if ( new File( datasource + ".aliases" ).exists() ) {
            p = new Properties();
            p.load( new FileInputStream( datasource+".aliases") );
            l.loadAliases( p );
        }
        
        if ( service.toLowerCase().startsWith("wfs-1.0")) {
            l.loadWFS10();
        }
        if ( service.toLowerCase().startsWith( "wfs-1.1")) {
            l.loadWFS11();
        }
        
        //l.loadAliases( a );
        //l.loadWFS10();
        //l.loadWFS11();
        l.dispose();
    }
    
    static void usage() {
        System.out.println( "java " + Loader.class.getName() + " -d <datasource> -s <service>");
    }
    
    /**
     * data store parameters
     */
    HashMap params;
    
    /**
     * type name aliases
     */
    HashMap aliases;
    
    /**
     * the datastore 
     */
    DataStore dataStore;
    
    public void loadDataStore( Properties properties ) throws IOException {
        params = new HashMap();
        for ( Object o : properties.keySet() ) {
            params.put( o.toString(), properties.get( o ) );
        }
        
        DataStoreFactorySpi factory = null;
        for ( Iterator<DataStoreFactorySpi> i =  DataStoreFinder.getAvailableDataStores(); i.hasNext(); ) {
            DataStoreFactorySpi f = i.next();
            if ( f.canProcess( params ) ) {
                factory = f;
                break;
            }
        }
        
        if ( factory == null ) {
            throw new IllegalStateException( "Could not aquire datastore.");
        }
        
        dataStore = factory.createDataStore(params);
        if ( dataStore == null ) {
            throw new IllegalStateException( "Could not create datastore from specified parameters." );
        }
    }
    
    public void dispose() {
        dataStore.dispose();
    }
    
    public void loadAliases( Properties properties ) {
        aliases = new HashMap();
        aliases.putAll( properties );
    }
    
    SimpleFeatureType buildFeatureType( String name, String namespaceURI, int srid, List<String> names, Class... types ) {
        return buildFeatureType(name, namespaceURI, srid, names, Collections.EMPTY_LIST, types);
    }
    
    SimpleFeatureType buildFeatureType( String name, String namespaceURI, int srid, List<String> names, List<String> mandatory, Class... types ) {
        if ( aliases != null && aliases.containsKey( name ) ) {
            name = (String) aliases.get( name );
        }
        
        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.setName( name );
        b.setNamespaceURI( namespaceURI );
        try {
            b.setCRS( CRS.decode( "EPSG:" + srid) );
        } 
        catch( Exception e ) {
            throw new RuntimeException( e );
        }
        
        for ( int i = 0; i < names.size(); i++ ) {
            String attName = names.get( i );
            if ( mandatory.contains( attName ) ) {
                b.minOccurs( 1 );
                b.nillable(false);
            }
            if ( "description".equalsIgnoreCase( attName) ) {
                b.length( 5000 );
            }
            
            b.add(  attName, types[ i ] );
        }
        
        return b.buildFeatureType();
    }
    
    static String cdf ="http://www.opengis.net/cite/data" ;
    static String cgf ="http://www.opengis.net/cite/geometry"; 
    
    public void loadWFS10() throws Exception {
        loadDeletes(dataStore);
        loadFifteen(dataStore);
        loadLocks(dataStore);
        loadInserts(dataStore);
        loadNulls(dataStore);
        loadOther(dataStore);
        loadSeven(dataStore);
        loadUpdates(dataStore);
        loadLines(dataStore);
        loadMLines(dataStore);
        loadMPoints(dataStore);
        loadMPolygons(dataStore);
        loadPoints(dataStore);
        loadPolygons(dataStore);
    }
    
    void loadDeletes(DataStore dataStore) throws Exception {
        SimpleFeatureType Deletes = buildFeatureType( "Deletes", cdf, 32615, 
                Arrays.asList( "id", "pointProperty"),  String.class, Point.class );    

        FeatureWriter w = buildFeatureStore( Deletes, dataStore );
        feature( w, "td0001", geometry( "POINT(500050 500050)" , 32615 ) );
        feature( w, "td0002", geometry( "POINT(500050 500050)" , 32615 ) );
        feature( w, "td0003", geometry( "POINT(500050 500050)" , 32615 ) );
        w.close();
    }
    
    void loadFifteen(DataStore dataStore) throws Exception {
        SimpleFeatureType Fifteen = buildFeatureType( "Fifteen", cdf, 32615, 
                Arrays.asList( "pointProperty" ), Point.class );
        
        FeatureWriter w = buildFeatureStore( Fifteen, dataStore );
        feature( w, geometry( "POINT(500050 500050)" , 32615 ) );
        feature( w, geometry( "POINT(500050 500050)" , 32615 ) );
        feature( w, geometry( "POINT(500050 500050)" , 32615 ) );
        feature( w, geometry( "POINT(500050 500050)" , 32615 ) );
        feature( w, geometry( "POINT(500050 500050)" , 32615 ) );
        feature( w, geometry( "POINT(500050 500050)" , 32615 ) );
        feature( w, geometry( "POINT(500050 500050)" , 32615 ) );
        feature( w, geometry( "POINT(500050 500050)" , 32615 ) );
        feature( w, geometry( "POINT(500050 500050)" , 32615 ) );
        feature( w, geometry( "POINT(500050 500050)" , 32615 ) );
        feature( w, geometry( "POINT(500050 500050)" , 32615 ) );
        feature( w, geometry( "POINT(500050 500050)" , 32615 ) );
        feature( w, geometry( "POINT(500050 500050)" , 32615 ) );
        feature( w, geometry( "POINT(500050 500050)" , 32615 ) );
        feature( w, geometry( "POINT(500050 500050)" , 32615 ) );
        w.close();
    }
    
    void loadInserts(DataStore dataStore) throws Exception {
        SimpleFeatureType Inserts = buildFeatureType( "Inserts", cdf, 32615, 
                Arrays.asList( "id", "pointProperty"),  String.class, Point.class );
        
        FeatureWriter w = buildFeatureStore( Inserts, dataStore );
        w.close();
    }
    
    void loadLocks(DataStore dataStore) throws Exception {
        
        SimpleFeatureType Locks = buildFeatureType( "Locks", cdf, 32615,
            Arrays.asList( "id", "pointProperty"),  String.class, Point.class );
        
        FeatureWriter w = buildFeatureStore(Locks, dataStore);
        feature( w, "lfla0001", geometry("POINT(500050 500050)", 32615 ) );
        feature( w, "lfla0002", geometry("POINT(500050 500050)", 32615 ) );
        feature( w, "lfla0003", geometry("POINT(500050 500050)", 32615 ) );
        feature( w, "lfla0004", geometry("POINT(500050 500050)", 32615 ) );
        feature( w, "gfwlla0001", geometry("POINT(500050 500050)", 32615 ) );
        feature( w, "gfwlla0002", geometry("POINT(500050 500050)", 32615 ) );
        feature( w, "gfwlla0003", geometry("POINT(500050 500050)", 32615 ) );
        feature( w, "gfwlla0004", geometry("POINT(500050 500050)", 32615 ) );
        feature( w, "lfbt0001", geometry("POINT(500050 500050)", 32615 ) );
        feature( w, "lfbt0002", geometry("POINT(500050 500050)", 32615 ) );
        feature( w, "lfbt0003", geometry("POINT(500050 500050)", 32615 ) );
        feature( w, "lfbt0004", geometry("POINT(500050 500050)", 32615 ) );
        feature( w, "lfbt0005", geometry("POINT(500050 500050)", 32615 ) );
        feature( w, "lfbt0006", geometry("POINT(500050 500050)", 32615 ) );
        feature( w, "gfwlbt0001", geometry("POINT(500050 500050)", 32615 ) );
        feature( w, "gfwlbt0002", geometry("POINT(500050 500050)", 32615 ) );
        feature( w,  "gfwlbt0003", geometry("POINT(500050 500050)", 32615 ) );
        feature( w, "gfwlbt0004", geometry("POINT(500050 500050)", 32615 ) );
        feature( w, "gfwlbt0005", geometry("POINT(500050 500050)", 32615 ) );
        feature( w, "gfwlbt0006", geometry("POINT(500050 500050)", 32615 ) );
        feature( w, "lfe0001", geometry("POINT(500050 500050)", 32615 ) );
        feature( w, "lfe0002",geometry("POINT(500050 500050)", 32615 ) );
        feature( w, "lfe0003",geometry("POINT(500050 500050)", 32615 ) );
        feature( w, "lfe0004",geometry("POINT(500050 500050)", 32615 ) );
        feature( w, "gfwle0001",geometry("POINT(500050 500050)", 32615 ) );
        feature( w, "gfwle0002",geometry("POINT(500050 500050)", 32615 ) );
        feature( w, "gfwle0003",geometry("POINT(500050 500050)", 32615 ) );
        feature( w, "gfwle0004",geometry("POINT(500050 500050)", 32615 ) );
        feature( w, "lfra0001",geometry("POINT(500050 500050)", 32615 ) );
        feature( w, "lfra0002",geometry("POINT(500050 500050)", 32615 ) );
        feature( w, "lfra0003",geometry("POINT(500050 500050)", 32615 ) );
        feature( w, "lfra0004",geometry("POINT(500050 500050)", 32615 ) );
        feature( w, "lfra0005",geometry("POINT(500050 500050)", 32615 ) );
        feature( w, "lfra0006",geometry("POINT(500050 500050)", 32615 ) );
        feature( w, "lfra0007",geometry("POINT(500050 500050)", 32615 ) );
        feature( w, "lfra0008",geometry("POINT(500050 500050)", 32615 ) );
        feature( w, "lfra0009",geometry("POINT(500050 500050)", 32615 ) );
        feature( w, "lfra0010",geometry("POINT(500050 500050)", 32615 ) );
        feature( w, "gfwlra0001",geometry("POINT(500050 500050)", 32615 ) );
        feature( w, "gfwlra0002",geometry("POINT(500050 500050)", 32615 ) );
        feature( w, "gfwlra0003",geometry("POINT(500050 500050)", 32615 ) );
        feature( w, "gfwlra0004",geometry("POINT(500050 500050)", 32615 ) );
        feature( w, "gfwlra0005",geometry("POINT(500050 500050)", 32615 ) );
        feature( w, "gfwlra0006",geometry("POINT(500050 500050)", 32615 ) );
        feature( w, "gfwlra0007",geometry("POINT(500050 500050)", 32615 ) );
        feature( w, "gfwlra0008",geometry("POINT(500050 500050)", 32615 ) );
        feature( w, "gfwlra0009",geometry("POINT(500050 500050)", 32615 ) );
        feature( w, "gfwlra0010",geometry("POINT(500050 500050)", 32615 ) );
        feature( w, "lfrs0001",geometry("POINT(500050 500050)", 32615 ) );
        feature( w, "lfrs0002",geometry("POINT(500050 500050)", 32615 ) );
        feature( w, "lfrs0003",geometry("POINT(500050 500050)", 32615 ) );
        feature( w, "lfrs0004",geometry("POINT(500050 500050)", 32615 ) );
        feature( w, "lfrs0005",geometry("POINT(500050 500050)", 32615 ) );
        feature( w, "lfrs0006",geometry("POINT(500050 500050)", 32615 ) );
        feature( w, "lfrs0007",geometry("POINT(500050 500050)", 32615 ) );
        feature( w, "lfrs0008",geometry("POINT(500050 500050)", 32615 ) );
        feature( w, "lfrs0009",geometry("POINT(500050 500050)", 32615 ) );
        feature( w, "lfrs0010",geometry("POINT(500050 500050)", 32615 ) );
        feature( w, "gfwlrs0001",geometry("POINT(500050 500050)", 32615 ) );
        feature( w, "gfwlrs0002",geometry("POINT(500050 500050)", 32615 ) );
        feature( w, "gfwlrs0003",geometry("POINT(500050 500050)", 32615 ) );
        feature( w, "gfwlrs0004",geometry("POINT(500050 500050)", 32615 ) );
        feature( w, "gfwlrs0005",geometry("POINT(500050 500050)", 32615 ) );
        feature( w, "gfwlrs0006",geometry("POINT(500050 500050)", 32615 ) );
        feature( w, "gfwlrs0007",geometry("POINT(500050 500050)", 32615 ) );
        feature( w, "gfwlrs0008",geometry("POINT(500050 500050)", 32615 ) );
        feature( w, "gfwlrs0009",geometry("POINT(500050 500050)", 32615 ) );
        feature( w, "gfwlrs0010",geometry("POINT(500050 500050)", 32615 ) );
        w.close();
    }
    
    void loadNulls(DataStore dataStore) throws Exception {
        SimpleFeatureType Nulls = buildFeatureType( "Nulls", cdf,  32615,
                Arrays.asList( "description", "name", "integers", "dates", "pointProperty"),
                String.class, String.class, Integer.class, Date.class, Point.class );
        
        FeatureWriter w = buildFeatureStore(Nulls, dataStore);
        feature( w, "nullFeature",null,null,null,null );
        w.close();
    }
    
    void loadOther(DataStore dataStore) throws Exception {
        SimpleFeatureType Other = buildFeatureType( "Other", cdf, 32615,
                Arrays.asList( "description", "name", "pointProperty", "string1", "string2", "integers", "dates"),
                Arrays.asList( "string1" ), 
                String.class, String.class, Point.class, String.class, String.class, Integer.class, Date.class );    
    
        FeatureWriter w = buildFeatureStore( Other, dataStore );
        
        feature( w, "A Single Feature used to test returning of properties", "singleFeature", 
                geometry("POINT(500050 500050)",32615 ), "always", "sometimes",7, date( "2002-12-02" ) );

    }
    
    void loadSeven(DataStore dataStore) throws Exception {
        SimpleFeatureType Seven = buildFeatureType( "Seven", cdf, 32615,
                Arrays.asList( "pointProperty"), Point.class );
        
        FeatureWriter w = buildFeatureStore(Seven, dataStore);
        feature( w, geometry( "POINT(500050 500050)" , 32615 ) );
        feature( w, geometry( "POINT(500050 500050)" , 32615 ) );
        feature( w, geometry( "POINT(500050 500050)" , 32615 ) );
        feature( w, geometry( "POINT(500050 500050)" , 32615 ) );
        feature( w, geometry( "POINT(500050 500050)" , 32615 ) );
        feature( w, geometry( "POINT(500050 500050)" , 32615 ) );
        feature( w, geometry( "POINT(500050 500050)" , 32615 ) );
        w.close();
    }
    
    void loadUpdates(DataStore dataStore) throws Exception {
        SimpleFeatureType Updates = buildFeatureType( "Updates", cdf, 32615,
                Arrays.asList( "id", "pointProperty"), String.class, Point.class );
        
        FeatureWriter w = buildFeatureStore( Updates, dataStore );
        feature( w, "tu0001", geometry("POINT(500050 500050)", 32615 ) );
        feature( w, "tu0002", geometry("POINT(500050 500050)", 32615 ) );
        feature( w, "tu0003", geometry("POINT(500050 500050)", 32615 ) );
        feature( w, "tu0004", geometry("POINT(500050 500050)", 32615 ) );
        w.close();
    }
    
    void loadLines(DataStore dataStore) throws Exception {
        SimpleFeatureType Lines = buildFeatureType( "Lines", cgf, 32615,
                Arrays.asList( "id", "lineStringProperty"), String.class, LineString.class );
            
        FeatureWriter w = buildFeatureStore(Lines, dataStore);
        feature(w, "t0001", geometry("LINESTRING(500125 500025,500175 500075)", 32615 ));
        w.close();
    }
    
    void loadMLines(DataStore dataStore) throws Exception {
        SimpleFeatureType MLines = buildFeatureType( "MLines", cgf,  32615,
                Arrays.asList( "id", "multiLineStringProperty"), String.class, MultiLineString.class );
        
        FeatureWriter w = buildFeatureStore(MLines, dataStore);
        feature(w, "t0004", geometry("MULTILINESTRING((500425 500025,500475 500075),(500425 500075,500475 500025))",32615));
        w.close();
    }
    
    void loadMPoints(DataStore dataStore) throws Exception {
        SimpleFeatureType MPoints = buildFeatureType( "MPoints", cgf, 32615,
                Arrays.asList( "id", "multiPointProperty"), String.class, MultiPoint.class );
        
        FeatureWriter w = buildFeatureStore( MPoints, dataStore );
        feature( w, "t0003", geometry("MULTIPOINT(500325 500025,500375 500075)",32615));
        w.close();
    }
    
    void loadMPolygons(DataStore dataStore) throws Exception {
        SimpleFeatureType MPolygons = buildFeatureType( "MPolygons", cgf,  32615,
                Arrays.asList( "id", "multiPolygonProperty"), String.class, MultiPolygon.class );
            
        FeatureWriter w = buildFeatureStore( MPolygons, dataStore );
        feature( w, "t0005", geometry("MULTIPOLYGON(((500525 500025,500550 500050,500575 500025," +
            "500525 500025)),((500525 500050,500525 500075,500550 500075,500550 500050,500525 500050)))",32615));
        w.close();
    }
        
    void loadPoints(DataStore dataStore) throws Exception {
        SimpleFeatureType Points = buildFeatureType( "Points", cgf, 32615,
            Arrays.asList( "id", "pointProperty"), String.class, Point.class );    
    
        FeatureWriter w = buildFeatureStore(Points,dataStore);
        feature( w, "t0000", geometry("POINT(500050 500050)",32615));
        w.close();
    }
    
    void loadPolygons(DataStore dataStore) throws Exception {
        SimpleFeatureType Polygons = buildFeatureType( "Polygons", cgf, 32615,
                Arrays.asList( "id", "polygonProperty"), String.class, Polygon.class );
    
        FeatureWriter w = buildFeatureStore(Polygons,dataStore);
        feature( w, "t0002", geometry("POLYGON((500225 500025,500225 500075,500275 500050,500275 500025,500225 500025))",32615));
        w.close();
    }

    String sf = "http://cite.opengeospatial.org/gmlsf";
    
    public void loadWFS11() throws Exception {
        loadPrimitiveGeoFeature(dataStore);
        loadAggregateGeoFeature(dataStore);
        //loadEntitŽGŽnŽrique(dataStore);
        loadEntiteGenerique(dataStore);
    }
    
    void loadPrimitiveGeoFeature(DataStore dataStore) throws Exception {
        SimpleFeatureType PrimitiveGeoFeature = buildFeatureType( "PrimitiveGeoFeature", sf, 4326,
            Arrays.asList( "description", "name", "surfaceProperty", "pointProperty", "curveProperty",
                "intProperty", "uriProperty", "measurand", "dateTimeProperty", "dateProperty", "decimalProperty"), 
            Arrays.asList( "intProperty", "measurand", "decimalProperty" ), 
            String.class, String.class, Polygon.class, Point.class, LineString.class, Integer.class, 
            String.class, Double.class, Date.class, Date.class, Double.class );    
        
        FeatureWriter w = buildFeatureStore(PrimitiveGeoFeature,dataStore);
        feature( w, "description-f001", "name-f001", null, geometry("POINT(2.00342 39.73245)", 4326), 
            null, 155, "http://www.opengeospatial.org/", 12765.0, null, date( "2006-10-25" ), 5.03 );
        feature( w, "description-f002", "name-f002", null, geometry("POINT(0.22601 59.41276)", 4326), 
                null, 154, "http://www.opengeospatial.org/", 12769.0, null, date( "2006-10-23" ), 4.02 );
        feature( w, "description-f003", "name-f003", null, null, geometry("LINESTRING(9.799 46.074,10.466 46.652,11.021 47.114)", 4326), 
                180, null, 672.1, null, date( "2006-09-01" ), 12.92 );
        feature( w, "description-f008", "name-f008", geometry("POLYGON((30.899 45.174,30.466 45.652,30.466 45.891,30.899 45.174))", 4326),null,
                null, 300, null, 783.5, datetime("2006-06-28 07:08:00 +0200"), date( "2006-12-12" ), 18.92 );
        feature( w, null, "name-f015", null, geometry("POINT(-10.52 34.94)", 4326), 
                null, -900, null, 2.4, null, null, 7.9 );
        w.close();
    }
    void loadAggregateGeoFeature(DataStore dataStore) throws Exception {
        SimpleFeatureType AggregateGeoFeature = buildFeatureType( "AggregateGeoFeature", sf, 4326,
                Arrays.asList( "description", "name", "multiPointProperty", "multiCurveProperty", 
                    "multiSurfaceProperty", "doubleProperty", "intRangeProperty", "strProperty", "featureCode"), 
                Arrays.asList( "doubleProperty", "strProperty", "featureCode" ), 
                String.class, String.class, MultiPoint.class, MultiLineString.class, MultiPolygon.class, 
                Double.class, String.class, String.class, String.class );    
        
        FeatureWriter w = buildFeatureStore(AggregateGeoFeature,dataStore);
        feature( w, "description-f005","name-f005", geometry("MULTIPOINT(29.86 70.83,31.08 68.87,32.19 71.96)",4326), 
            null,null,2012.78,null,"Ma quande lingues coalesce, li grammatica del resultant lingue es plu simplic e " +
            "regulari quam ti del coalescent lingues. Li nov lingua franca va esser plu simplic e regulari quam li " +
            "existent Europan lingues.","BK030");
        feature( w, "description-f009","name-f009",null, geometry("MULTILINESTRING((-5.899 55.174," +
            "-5.466 55.652,-5.899 55.891,-5.899 58.174,-5.466 58.652,-5.899 58.891),(-5.188 53.265," +
            "-4.775 54.354,-4.288 52.702,-4.107 53.611,-4.010 55.823))",4326),null,20.01,null,
            "Ma quande lingues coalesce, li grammatica del resultant.","GB007");
        feature( w, "description-f010","name-f010",null,null,geometry("MULTIPOLYGON(((20 50,19 54,20 55," +
            "30 60,28 52,27 51,29 49,27 47,20 50),(25 55,25.2 56,25.1 56,25 55)),((20.0 35.5,24.0 35.0," +
            "28.0 35.0,27.5 39.0,22.0 37.0,20.0 35.5),(26.0 36.0,25.0 37.0,27.0 36.8,26.0 36.0)))",4326),
            24510,null,"Ma quande lingues coalesce, li grammatica del resultant lingue es plu simplic e " +
            "regulari quam ti del coalescent lingues. Li nov lingua franca va esser plu simplic e regulari " +
            "quam li existent Europan lingues.","AK020");
        feature( w, null,"name-f016",null,null,geometry("MULTIPOLYGON(((6.0 57.5, 8.0 57.5, 8.0 60.0, 9.0 62.5, 5.0 62.5,6.0 60.0,6.0 57.5)," +
            "(6.5 58.0,6.5 59.0,7.0 59.0,6.5 58.0)))",4326),-182.9,null,"In rhoncus nisl sit amet sem.","EE010");
        w.close();
    }
    
    void loadEntiteGenerique(DataStore dataStore) throws Exception {
    //void loadEntitŽGŽnŽrique(DataStore dataStore) throws Exception {
        SimpleFeatureType /*EntitŽGŽnŽrique*/EntiteGenerique = buildFeatureType( "EntitŽGŽnŽrique", sf, 4326,
            Arrays.asList( "description", "name", "attribut.GŽomŽtrie", "boolProperty", "str4Property", "featureRef"),
            Arrays.asList( "attribut.GŽomŽtrie", "boolProperty", "str4Property"), 
            String.class, String.class, Geometry.class, Boolean.class, String.class, String.class );
        FeatureWriter w = buildFeatureStore(EntiteGenerique, dataStore);
        feature( w, "description-f004","name-f004", geometry("POLYGON((0 60.5,0 64,6.25 64,6.25 60.5,0 60.5)," +
            "(2 61.5,2 62.5,4 62,2 61.5))",4326),true,"abc3","name-f003"); 
        feature( w, "description-f007","name-f007", geometry("POLYGON((15 35,16 40,20 39,22.5 37,18 36,15 35)," +
            "(17.5 37.1,17.6 37.2,17.7 37.3,17.8 37.4,17.9 37.5,17.9 37,17.5 37.1))",4326),false,"def4",null);
        feature( w, "description-f017", "name-f017",geometry("LINESTRING(4.899 50.174,5.466 52.652,6.899 53.891," + 
            "7.780 54.382,8.879 54.982)",4326),false,"qrst","name-f015");
        w.close();
    }

 
    
    FeatureWriter buildFeatureStore( SimpleFeatureType schema, DataStore dataStore ) throws Exception {
        LOGGER.info( "Loading " + schema.getTypeName() );
        try {
            dataStore.createSchema(schema);
        }
        catch( Exception e ) {
            //could already exist, do a quick query to check
            try {
                if ( dataStore.getSchema( schema.getName() ) == null ) {
                    throw e;
                }    
            }
            catch( Exception e2 ) {
                throw e;
            }
            
            //delete all features
            FeatureStore store = (FeatureStore) dataStore.getFeatureSource( schema.getName() );
            store.removeFeatures(Filter.INCLUDE);
        }
        
        return dataStore.getFeatureWriterAppend( schema.getTypeName(), Transaction.AUTO_COMMIT );
    }
    
    void feature( FeatureWriter w, Object... values ) throws Exception {
        w.hasNext();
        SimpleFeature f = (SimpleFeature) w.next();
        for ( int i = 0; i < values.length; i++ ) {
            f.setAttribute(i, values[i] );
        }
        w.write();
    }
    
    Geometry geometry( String wkt, int srid ) throws Exception {
        Geometry g = new WKTReader().read( wkt );
        g.setUserData( CRS.decode( "EPSG:" + srid ) );
        return g;
    }
    
    Date date( String d ) throws ParseException {
        return new SimpleDateFormat( "yyyy-MM-dd Z").parse(d + " +0000" );
    }
    
    Date datetime( String d ) throws ParseException {
        return new SimpleDateFormat( "yyyy-MM-dd hh:mm:ss Z").parse(d);
    }
    

}