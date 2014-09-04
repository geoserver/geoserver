/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.data.AbstractDataStore;
import org.geotools.data.AbstractFeatureSource;
import org.geotools.data.CollectionFeatureReader;
import org.geotools.data.DataSourceException;
import org.geotools.data.DataStore;
import org.geotools.data.DataUtilities;
import org.geotools.data.EmptyFeatureReader;
import org.geotools.data.FeatureListener;
import org.geotools.data.FeatureReader;
import org.geotools.data.MaxFeatureReader;
import org.geotools.data.Query;
import org.geotools.data.QueryCapabilities;
import org.geotools.data.ReTypeFeatureReader;
import org.geotools.data.Transaction;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.filter.visitor.DefaultFilterVisitor;
import org.geotools.geometry.Envelope2D;
import org.geotools.geometry.jts.JTSUtils;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.PropertyIsBetween;
import org.opengis.filter.PropertyIsEqualTo;
import org.opengis.filter.PropertyIsGreaterThanOrEqualTo;
import org.opengis.filter.PropertyIsLessThanOrEqualTo;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.spatial.BBOX;
import org.opengis.filter.spatial.Intersects;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

/**
 * 
 * A simple datastore containing 2 granules
 * 
 * @author Jeroen Dries jeroen.dries@vito.be
 *
 */
public class MultiDimDataStore extends AbstractDataStore {



    protected static final Logger LOGGER = org.geotools.util.logging.Logging.getLogger("org.geoserver.wcs2_0");

    private static final CoordinateReferenceSystem EPSG_4326;
    static {
	CoordinateReferenceSystem crs = DefaultGeographicCRS.WGS84;
	try {
	    crs = CRS.decode("EPSG:4326");
	} catch (FactoryException e) {
	    LOGGER.log(Level.WARNING, e.getMessage(), e);
	}
	EPSG_4326 = crs;
    }


    private static final List<String> BAND_LIST = Arrays.asList("NDVI", "BLUE/TOC", "SWIR/VAA", "NIR/TOC", "RED/TOC",
	    "SWIR/TOC", "VNIR/VAA");

    /**
     * The 'dim_' prefix is mandatory as per the WMS spec, and also required by
     * geoserver
     */
    private static final String BAND_DIMENSION = "BANDS";

    private static final SimpleDateFormat PROBA_DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy");
    static{
	PROBA_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }
    
    private static final ReferencedEnvelope TOTAL_BOUNDS = new ReferencedEnvelope(10.000, 55, 40, 70,
	    EPSG_4326);
    public static final String FILE_LOCATION_ATTRIBUTE = "fileLocation";
    private static final String LABEL_ATTRIBUTE = "label";
    public static final String GEOMETRY_ATTRIBUTE = "geometry";
    static final String TIME_ATTRIBUTE = "timestamp";
    public static final String TYPENAME = "FlexysCoverage";
    private static final SimpleFeatureType FEATURE_TYPE = createSchema();

    private SimpleFeature feature1;

    private SimpleFeature feature2;
   

    public MultiDimDataStore(String parentLocation) {
	super(false);
	GeometryFactory geometryBuilder = new GeometryFactory();
	SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(FEATURE_TYPE);
	    featureBuilder.add(geometryBuilder.toGeometry(new Envelope(10, 30, 40, 70)));
	    
	    featureBuilder.add(parentLocation + "/2DLatLonCoverage.nc");
	    Date theDate = new Date();
	    featureBuilder.add(theDate);
	    featureBuilder.set(BAND_DIMENSION, "MyBand");
	    featureBuilder.set(LABEL_ATTRIBUTE, "X" + 0 + "Y" + 0);
	    feature1 = featureBuilder.buildFeature("feature1");
	    
	    featureBuilder = new SimpleFeatureBuilder(FEATURE_TYPE);
	    
	    featureBuilder.add(geometryBuilder.toGeometry(new Envelope(35, 55, 40, 70)));
	    
	    featureBuilder.add(parentLocation + "/2DLatLonCoverage2.nc");
	    featureBuilder.add(theDate);
	    featureBuilder.set(BAND_DIMENSION, "MyBand");
	    featureBuilder.set(LABEL_ATTRIBUTE, "X" + 0 + "Y" + 0);
	    
	    feature2 = featureBuilder.buildFeature("feature2");
    }

    @Override
    public String[] getTypeNames() throws IOException {
	return new String[] { TYPENAME };
    }

    @Override
    public SimpleFeatureType getSchema(String typeName) throws IOException {
	return FEATURE_TYPE;
    }

    private static SimpleFeatureType createSchema() {

	SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
	builder.setName(TYPENAME);

	builder.setCRS(EPSG_4326);

	builder.setDefaultGeometry(GEOMETRY_ATTRIBUTE);
	builder.setNamespaceURI("VITOEOData");

	builder.add(GEOMETRY_ATTRIBUTE, Geometry.class);
	builder.add(FILE_LOCATION_ATTRIBUTE, String.class);
	builder.add(TIME_ATTRIBUTE, Date.class);
	builder.add(BAND_DIMENSION, String.class);
	builder.add(LABEL_ATTRIBUTE,String.class);
	return builder.buildFeatureType();
    }

    @Override
    protected FeatureReader<SimpleFeatureType, SimpleFeature> getFeatureReader(String typeName, Query query)
	    throws IOException {
	

	if (query.getPropertyNames() != null && query.getPropertyNames().length == 1) {
	    if (query.getPropertyNames()[0].equals(TIME_ATTRIBUTE)) {
		// geoserver is determining the time domain		
		List<Date> availableTimes = Arrays.asList(new Date());
		List<SimpleFeature> features = new ArrayList<SimpleFeature>(availableTimes.size());

		int idCounter = 0;
		for (Date date : availableTimes) {
		    SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(FEATURE_TYPE);
		    featureBuilder.set(TIME_ATTRIBUTE, date);
		    features.add(featureBuilder.buildFeature("dummyID" + idCounter++));
		}
		return wrapAndCache(features);
	    }
	    if (query.getPropertyNames()[0].equals(BAND_DIMENSION)) {
		List<SimpleFeature> features = new ArrayList<SimpleFeature>(BAND_LIST.size());

		int idCounter = 0;
		for (String band : BAND_LIST) {
		    SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(FEATURE_TYPE);
		    featureBuilder.set(BAND_DIMENSION, band);
		    features.add(featureBuilder.buildFeature("dummyID" + idCounter++));
		}
		return wrapAndCache(features);
	    }
	}
	try {
	    final Date date = new Date();
	    date.setTime(0);

	    final Date beginDate = new Date();
	    beginDate.setTime(0);
	    final Date endDate = new Date();
	    endDate.setTime(0);
	    final List<String> band = new ArrayList<String>();

	    DefaultFilterVisitor filterVisitor = new DefaultFilterVisitor() {

		@Override
		public Object visit(BBOX filter, Object data) {
		    Envelope2D envelope = (Envelope2D) data;
		    filter.getBounds();
		    envelope.setBounds(filter.getBounds());
		    return super.visit(filter, data);
		}
		
		

		@Override
		public Object visit(Intersects filter, Object data) {
		    Envelope2D envelope = (Envelope2D) data;
		    
		    Geometry polygon= ((Geometry)((Literal)filter.getExpression2()).getValue());
		    org.opengis.geometry.Geometry polygon2 = JTSUtils.jtsToGo1(polygon, envelope.getCoordinateReferenceSystem());
		    envelope.setBounds(new Envelope2D(polygon2.getEnvelope()));
		    return super.visit(filter, data);
		}



		/**
		 * Used by WCS 2.0 to select a time range
		 */
		@Override
		public Object visit(PropertyIsGreaterThanOrEqualTo filter, Object data) {
		    PropertyName prop;
		    Literal lit;
		    if (filter.getExpression1() instanceof PropertyName && filter.getExpression2() instanceof Literal) {
			prop = (PropertyName) filter.getExpression1();
			lit = (Literal) filter.getExpression2();
		    } else if (filter.getExpression2() instanceof PropertyName
			    && filter.getExpression1() instanceof Literal) {
			prop = (PropertyName) filter.getExpression1();
			lit = (Literal) filter.getExpression2();
		    } else {
			return super.visit(filter, data);
		    }
		    if (prop.getPropertyName().equals(TIME_ATTRIBUTE)) {			
			if(lit.getValue()!=null){
			    beginDate.setTime(((Date) lit.getValue()).getTime());
			}
		    }
		    return super.visit(filter, data);
		}

		/**
		 * Used by WCS 2.0 to select a time range
		 */
		@Override
		public Object visit(PropertyIsLessThanOrEqualTo filter, Object data) {
		    PropertyName prop;
		    Literal lit;
		    if (filter.getExpression1() instanceof PropertyName && filter.getExpression2() instanceof Literal) {
			prop = (PropertyName) filter.getExpression1();
			lit = (Literal) filter.getExpression2();
		    } else if (filter.getExpression2() instanceof PropertyName
			    && filter.getExpression1() instanceof Literal) {
			prop = (PropertyName) filter.getExpression1();
			lit = (Literal) filter.getExpression2();
		    } else {
			return super.visit(filter, data);
		    }
		    if (prop.getPropertyName().equals(TIME_ATTRIBUTE)) {
			if(lit.getValue()!=null){
			    endDate.setTime(((Date) lit.getValue()).getTime());			    
			}
		    }
		    return super.visit(filter, data);
		}

		@Override
		public Object visit(PropertyIsEqualTo filter, Object data) {
		    PropertyName prop;
		    Literal lit;
		    if (filter.getExpression1() instanceof PropertyName && filter.getExpression2() instanceof Literal) {
			prop = (PropertyName) filter.getExpression1();
			lit = (Literal) filter.getExpression2();
		    } else if (filter.getExpression2() instanceof PropertyName
			    && filter.getExpression1() instanceof Literal) {
			prop = (PropertyName) filter.getExpression1();
			lit = (Literal) filter.getExpression2();
		    } else {
			return super.visit(filter, data);
		    }
		    if (prop.getPropertyName().equals(TIME_ATTRIBUTE)) {
			date.setTime(((Date) lit.getValue()).getTime());
		    }
		    if (prop.getPropertyName().equals(BAND_DIMENSION)) {
			band.add((String) lit.getValue());

		    }
		    return super.visit(filter, data);
		}

		@Override
		public Object visit(PropertyIsBetween filter, Object data) {
		    PropertyName prop = (PropertyName) filter.getExpression();		    
		    
		    if (prop.getPropertyName().equals(TIME_ATTRIBUTE)) {
			beginDate.setTime(((Date) ((Literal)filter.getLowerBoundary()).getValue()).getTime());
			endDate.setTime(((Date) ((Literal)filter.getUpperBoundary()).getValue()).getTime());
		    } if (prop.getPropertyName().equals(BAND_DIMENSION)) {
			String bands = (String) ((Literal)filter.getLowerBoundary()).getValue();
			String[] singleBands = bands.split(",");
			band.addAll(Arrays.asList(singleBands));

		    }
		    return super.visit(filter, data);
		}

	    };
	    Envelope2D bbox = new Envelope2D(DefaultGeographicCRS.WGS84,-180,-90,360,180);
	    
	    
	    query.getFilter().accept(filterVisitor, bbox);
	    LOGGER.fine("Mosaic query on bbox: " + bbox);
	    	   	   
	    //very rudimentary filtering, for unit test only!
	    if(bbox.getMaxX()<=35.){
		return wrapAndCache(Arrays.asList(feature1));
	    }else{
		return wrapAndCache(Arrays.asList(feature1,feature2));
	    }
	    
	    

	} catch (Exception e) {

	    e.printStackTrace();
	    throw new RuntimeException(e);
	}
    } 

   

    @Override
    protected FeatureReader<SimpleFeatureType, SimpleFeature> getFeatureReader(String typeName) throws IOException {
	return new CollectionFeatureReader(Collections.EMPTY_LIST, FEATURE_TYPE);
    }
    
    public  FeatureReader<SimpleFeatureType, SimpleFeature> getFeatureReader(Query query,Transaction transaction) throws IOException {
        Filter filter = query.getFilter();
        String typeName = query.getTypeName();
        String propertyNames[] = query.getPropertyNames();

        if (filter == null) {
            throw new NullPointerException("getFeatureReader requires Filter: "
                + "did you mean Filter.INCLUDE?");
        }
        if( typeName == null ){
            throw new NullPointerException(
                "getFeatureReader requires typeName: "
                + "use getTypeNames() for a list of available types");
        }
        if (transaction == null) {
            throw new NullPointerException(
                "getFeatureReader requires Transaction: "
                + "did you mean to use Transaction.AUTO_COMMIT?");
        }
        SimpleFeatureType featureType = getSchema( query.getTypeName() );

        if( propertyNames != null || query.getCoordinateSystem()!=null ){
            try {
                featureType = DataUtilities.createSubType( featureType, propertyNames, query.getCoordinateSystem() );
            } catch (SchemaException e) {
                LOGGER.log( Level.FINEST, e.getMessage(), e);
                throw new DataSourceException( "Could not create Feature Type for query", e );

            }
        }
        if ( filter == Filter.EXCLUDE || filter.equals( Filter.EXCLUDE )) {
            return new EmptyFeatureReader<SimpleFeatureType, SimpleFeature>(featureType);
        }
        //GR: allow subclases to implement as much filtering as they can,
        //by returning just it's unsupperted filter
        filter = getUnsupportedFilter(typeName, filter);
        if(filter == null){
            throw new NullPointerException("getUnsupportedFilter shouldn't return null. Do you mean Filter.INCLUDE?");
        }

       
        
        // This calls our subclass "simple" implementation
        // All other functionality will be built as a reader around
        // this class
        //
         FeatureReader<SimpleFeatureType, SimpleFeature> reader = getFeatureReader(typeName, query);       

        if (!featureType.equals(reader.getFeatureType())) {
            LOGGER.fine("Recasting feature type to subtype by using a ReTypeFeatureReader");
            reader = new ReTypeFeatureReader(reader, featureType, false);
        }

        if (query.getMaxFeatures() != Query.DEFAULT_MAX) {
			    reader = new MaxFeatureReader<SimpleFeatureType, SimpleFeature>(reader, query.getMaxFeatures());
        }

        return reader;
    }

    @Override
    public SimpleFeatureSource getFeatureSource(final String typeName) throws IOException {
	return new AbstractFeatureSource(getSupportedHints()) {

	    {
		queryCapabilities = new QueryCapabilities() {
		    @Override
		    public boolean supportsSorting(SortBy[] sortAttributes) {
			if (sortAttributes != null && sortAttributes.length == 1) {
			    if (sortAttributes[0].getPropertyName().getPropertyName().equals("timestamp")) {
				// sort by timestamp happens to be what we do by
				// default
				// TODO does the PDF support a sort order?
				return true;
			    }
			}
			return super.supportsSorting(sortAttributes);
		    }

		};
	    }

	    // the image mosaic reader does not want this bounds to be null
	    @Override
	    public ReferencedEnvelope getBounds() throws IOException {
		return TOTAL_BOUNDS;
	    }

	    public DataStore getDataStore() {
		return MultiDimDataStore.this;
	    }

	    public String toString() {
		return "AbstractDataStore.AbstractFeatureSource(" + TYPENAME + ")";
	    }

	    public void addFeatureListener(FeatureListener listener) {
		listenerManager.addFeatureListener(this, listener);
	    }

	    public void removeFeatureListener(FeatureListener listener) {
		listenerManager.removeFeatureListener(this, listener);
	    }

	    public SimpleFeatureType getSchema() {
		return FEATURE_TYPE;
	    }
	};
    }  
    
    private FeatureReader<SimpleFeatureType, SimpleFeature> wrapAndCache(List<SimpleFeature> features) {
	CollectionFeatureReader result = new CollectionFeatureReader(features, FEATURE_TYPE);
	
	return result;
    }


}
