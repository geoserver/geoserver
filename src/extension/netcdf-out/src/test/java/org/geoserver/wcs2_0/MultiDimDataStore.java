/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
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
import org.geotools.data.CollectionFeatureReader;
import org.geotools.data.FeatureReader;
import org.geotools.data.Query;
import org.geotools.data.QueryCapabilities;
import org.geotools.data.store.ContentDataStore;
import org.geotools.data.store.ContentEntry;
import org.geotools.data.store.ContentFeatureSource;
import org.geotools.feature.NameImpl;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.filter.visitor.DefaultFilterVisitor;
import org.geotools.geometry.Envelope2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.geometry.jts.spatialschema.geometry.JTSUtils;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.Name;
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

/**
 * A simple datastore containing 2 granules
 *
 * @author Jeroen Dries jeroen.dries@vito.be
 */
public class MultiDimDataStore extends ContentDataStore {

    private static final Logger LOGGER =
            org.geotools.util.logging.Logging.getLogger("org.geoserver.wcs2_0");

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

    private static final List<String> BAND_LIST =
            Arrays.asList(
                    "NDVI", "BLUE/TOC", "SWIR/VAA", "NIR/TOC", "RED/TOC", "SWIR/TOC", "VNIR/VAA");

    /** The 'dim_' prefix is mandatory as per the WMS spec, and also required by geoserver */
    private static final String BAND_DIMENSION = "BANDS";

    private static final SimpleDateFormat PROBA_DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy");

    static {
        PROBA_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    private static final Date THE_DATE = new Date();
    private static final ReferencedEnvelope TOTAL_BOUNDS =
            new ReferencedEnvelope(10.000, 55, 40, 70, EPSG_4326);
    private static final String FILE_LOCATION_ATTRIBUTE = "fileLocation";
    private static final String LABEL_ATTRIBUTE = "label";
    private static final String GEOMETRY_ATTRIBUTE = "geometry";
    private static final String TIME_ATTRIBUTE = "timestamp";
    private static final String TYPENAME = "FlexysCoverage";
    private static final SimpleFeatureType FEATURE_TYPE = createSchema();

    private SimpleFeature feature1;

    private SimpleFeature feature2;

    MultiDimDataStore(String parentLocation) {
        super();
        GeometryFactory geometryBuilder = new GeometryFactory();
        SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(FEATURE_TYPE);
        featureBuilder.add(geometryBuilder.toGeometry(new Envelope(10, 30, 40, 70)));

        featureBuilder.add(parentLocation + "/2DLatLonCoverage.nc");
        featureBuilder.add(THE_DATE);
        featureBuilder.set(BAND_DIMENSION, "MyBand");
        featureBuilder.set(LABEL_ATTRIBUTE, "X" + 0 + "Y" + 0);
        feature1 = featureBuilder.buildFeature("feature1");

        featureBuilder = new SimpleFeatureBuilder(FEATURE_TYPE);

        featureBuilder.add(geometryBuilder.toGeometry(new Envelope(35, 55, 40, 70)));

        featureBuilder.add(parentLocation + "/2DLatLonCoverage2.nc");
        featureBuilder.add(THE_DATE);
        featureBuilder.set(BAND_DIMENSION, "MyBand");
        featureBuilder.set(LABEL_ATTRIBUTE, "X" + 0 + "Y" + 0);

        feature2 = featureBuilder.buildFeature("feature2");
    }

    @Override
    public List<Name> createTypeNames() throws IOException {
        return Collections.singletonList(new NameImpl(TYPENAME));
    }

    @Override
    public SimpleFeatureType getSchema(Name typeName) throws IOException {
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
        builder.add(LABEL_ATTRIBUTE, String.class);
        return builder.buildFeatureType();
    }

    @Override
    public ContentFeatureSource createFeatureSource(ContentEntry entry) throws IOException {
        return new ContentFeatureSource(entry, Query.ALL) {

            {
                queryCapabilities =
                        new QueryCapabilities() {
                            @Override
                            public boolean supportsSorting(SortBy[] sortAttributes) {
                                if (sortAttributes != null && sortAttributes.length == 1) {
                                    if (sortAttributes[0]
                                            .getPropertyName()
                                            .getPropertyName()
                                            .equals("timestamp")) {
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
            public ReferencedEnvelope getBoundsInternal(Query query) throws IOException {
                return TOTAL_BOUNDS;
            }

            @Override
            protected int getCountInternal(Query query) throws IOException {
                if (query.getFilter() == Filter.INCLUDE) { // filtering not implemented
                    int count = 0;
                    try (FeatureReader<SimpleFeatureType, SimpleFeature> featureReader =
                            getReaderInternal(query)) {
                        while (featureReader.hasNext()) {
                            featureReader.next();
                            count++;
                        }
                    }
                    return count;
                }
                return -1; // feature by feature scan required to count records
            }

            @Override
            protected FeatureReader<SimpleFeatureType, SimpleFeature> getReaderInternal(Query query)
                    throws IOException {

                if (query.getPropertyNames() != null && query.getPropertyNames().length == 1) {
                    if (query.getPropertyNames()[0].equals(TIME_ATTRIBUTE)) {
                        // geoserver is determining the time domain
                        List<Date> availableTimes = Collections.singletonList(THE_DATE);
                        List<SimpleFeature> features = new ArrayList<>(availableTimes.size());

                        int idCounter = 0;
                        for (Date date : availableTimes) {
                            SimpleFeatureBuilder featureBuilder =
                                    new SimpleFeatureBuilder(FEATURE_TYPE);
                            featureBuilder.set(TIME_ATTRIBUTE, date);
                            features.add(featureBuilder.buildFeature("dummyID" + idCounter++));
                        }
                        return wrapAndCache(features);
                    }
                    if (query.getPropertyNames()[0].equals(BAND_DIMENSION)) {
                        List<SimpleFeature> features = new ArrayList<>(BAND_LIST.size());

                        int idCounter = 0;
                        for (String band : BAND_LIST) {
                            SimpleFeatureBuilder featureBuilder =
                                    new SimpleFeatureBuilder(FEATURE_TYPE);
                            featureBuilder.set(BAND_DIMENSION, band);
                            features.add(featureBuilder.buildFeature("dummyID" + idCounter++));
                        }
                        return wrapAndCache(features);
                    }
                }
                try {
                    final Date date = THE_DATE;
                    date.setTime(0);

                    final Date beginDate = THE_DATE;
                    beginDate.setTime(0);
                    final Date endDate = THE_DATE;
                    endDate.setTime(0);
                    final List<String> band = new ArrayList<>();

                    DefaultFilterVisitor filterVisitor =
                            new DefaultFilterVisitor() {

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

                                    Geometry polygon =
                                            ((Geometry)
                                                    ((Literal) filter.getExpression2()).getValue());
                                    org.opengis.geometry.Geometry polygon2 =
                                            JTSUtils.jtsToGo1(
                                                    polygon,
                                                    envelope.getCoordinateReferenceSystem());
                                    envelope.setBounds(new Envelope2D(polygon2.getEnvelope()));
                                    return super.visit(filter, data);
                                }

                                /** Used by WCS 2.0 to select a time range */
                                @Override
                                public Object visit(
                                        PropertyIsGreaterThanOrEqualTo filter, Object data) {
                                    PropertyName prop;
                                    Literal lit;
                                    if (filter.getExpression1() instanceof PropertyName
                                            && filter.getExpression2() instanceof Literal) {
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
                                        if (lit.getValue() != null) {
                                            beginDate.setTime(((Date) lit.getValue()).getTime());
                                        }
                                    }
                                    return super.visit(filter, data);
                                }

                                /** Used by WCS 2.0 to select a time range */
                                @Override
                                public Object visit(
                                        PropertyIsLessThanOrEqualTo filter, Object data) {
                                    PropertyName prop;
                                    Literal lit;
                                    if (filter.getExpression1() instanceof PropertyName
                                            && filter.getExpression2() instanceof Literal) {
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
                                        if (lit.getValue() != null) {
                                            endDate.setTime(((Date) lit.getValue()).getTime());
                                        }
                                    }
                                    return super.visit(filter, data);
                                }

                                @Override
                                public Object visit(PropertyIsEqualTo filter, Object data) {
                                    PropertyName prop;
                                    Literal lit;
                                    if (filter.getExpression1() instanceof PropertyName
                                            && filter.getExpression2() instanceof Literal) {
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
                                        beginDate.setTime(
                                                ((Date)
                                                                ((Literal)
                                                                                filter
                                                                                        .getLowerBoundary())
                                                                        .getValue())
                                                        .getTime());
                                        endDate.setTime(
                                                ((Date)
                                                                ((Literal)
                                                                                filter
                                                                                        .getUpperBoundary())
                                                                        .getValue())
                                                        .getTime());
                                    }
                                    if (prop.getPropertyName().equals(BAND_DIMENSION)) {
                                        String bands =
                                                (String)
                                                        ((Literal) filter.getLowerBoundary())
                                                                .getValue();
                                        String[] singleBands = bands.split(",");
                                        band.addAll(Arrays.asList(singleBands));
                                    }
                                    return super.visit(filter, data);
                                }
                            };
                    Envelope2D bbox =
                            new Envelope2D(DefaultGeographicCRS.WGS84, -180, -90, 360, 180);

                    query.getFilter().accept(filterVisitor, bbox);
                    LOGGER.fine("Mosaic query on bbox: " + bbox);

                    // very rudimentary filtering, for unit test only!
                    if (bbox.getMaxX() <= 35.) {
                        return wrapAndCache(Collections.singletonList(feature1));
                    } else {
                        return wrapAndCache(Arrays.asList(feature1, feature2));
                    }

                } catch (Exception e) {

                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
            }

            @Override
            protected SimpleFeatureType buildFeatureType() throws IOException {
                return FEATURE_TYPE;
            }

            public ContentDataStore getDataStore() {
                return MultiDimDataStore.this;
            }

            public String toString() {
                return "AbstractDataStore.AbstractFeatureSource(" + TYPENAME + ")";
            }
        };
    }

    private FeatureReader<SimpleFeatureType, SimpleFeature> wrapAndCache(
            List<SimpleFeature> features) {

        return new CollectionFeatureReader(features, FEATURE_TYPE);
    }
}
