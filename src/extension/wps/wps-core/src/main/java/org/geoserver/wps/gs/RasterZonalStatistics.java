/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs;

import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.image.RenderedImage;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import javax.media.jai.JAI;
import javax.media.jai.ROI;

import org.geoserver.wps.WPSException;
import org.geoserver.wps.jts.DescribeParameter;
import org.geoserver.wps.jts.DescribeProcess;
import org.geoserver.wps.jts.DescribeResult;
import org.geotools.coverage.Category;
import org.geotools.coverage.GridSampleDimension;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.processing.CoverageProcessor;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.collection.DecoratingSimpleFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.image.jai.Registry;
import org.geotools.referencing.CRS;
import org.geotools.referencing.operation.transform.ProjectiveTransform;
import org.geotools.util.NumberRange;
import org.jaitools.imageutils.ROIGeometry;
import org.jaitools.media.jai.zonalstats.ZonalStats;
import org.jaitools.media.jai.zonalstats.ZonalStatsDescriptor;
import org.jaitools.media.jai.zonalstats.ZonalStatsOpImage;
import org.jaitools.media.jai.zonalstats.ZonalStatsRIF;
import org.jaitools.numeric.Range;
import org.jaitools.numeric.Statistic;
import org.opengis.coverage.processing.Operation;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.filter.FilterFactory2;
import org.opengis.metadata.spatial.PixelOrientation;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.util.AffineTransformation;
import com.vividsolutions.jts.simplify.DouglasPeuckerSimplifier;

/**
 * A process computing zonal statistics based on a raster data set and a set of polygonal zones of
 * interest
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 * @author Emanuele Tajariol (GeoSolutions)
 * @author Andrea Aime - GeoSolutions
 */
@DescribeProcess(title = "Raster Zonal Statistics", description = "Provides statistics for the distribution "
        + "of a certain quantity in a set of reference areas. "
        + "The data layer is a raster layer, the reference layer must be a polygonal one")
public class RasterZonalStatistics implements GeoServerProcess {

    private final static CoverageProcessor PROCESSOR = CoverageProcessor.getInstance();

    private final static Operation CROPOPERATION = PROCESSOR.getOperation("CoverageCrop");
    
    static {
        try{
            Registry.registerRIF(JAI.getDefaultInstance(), new ZonalStatsDescriptor(), new ZonalStatsRIF(), Registry.JAI_TOOLS_PRODUCT);
        } catch (Throwable e) {
            // swallow exception in case the op has already been registered.
        }
    }

    @DescribeResult(name = "statistics", description = "A geometryless feature collection with all the attributes "
            + "of the zoning layer (prefixed by 'z_'), "
            + "and the statistics fields count/min/max/sum/avg/stddev")
    public SimpleFeatureCollection execute(
            @DescribeParameter(name = "data", description = "The raster containing "
                    + "the data to be used in the statistics") GridCoverage2D coverage,
            @DescribeParameter(name = "band", description = "The raster band used to compute statistifcs (first band used if not specified)", min = 0) Integer band,
            @DescribeParameter(name = "zones", description = "The various zones in which the statistics will be computed. "
                    + "Must be a polygon layer, each polygon will be used to generate a separate statistic") SimpleFeatureCollection zones,
            @DescribeParameter(name = "classification", description = "An optional coverage whose values will be used as classes" +
            		"for the statistica analysis: each zone will report statistics partitioned by classes according to the values of the grid coverage. " +
            		"It is supposed to be a single band integral data type coverage", min = 0) GridCoverage2D classification) {
        int iband = 0;
        if (band != null) {
            iband = band;
        }

        return new RasterZonalStatisticsCollection(coverage, iband, zones, classification);
    }

    /**
     * A feature collection that computes zonal statitics in a streaming fashion
     * 
     * @author Andrea Aime - OpenGeo
     */
    static class RasterZonalStatisticsCollection extends DecoratingSimpleFeatureCollection {
        GridCoverage2D coverage;

        SimpleFeatureType targetSchema;

        int band;

        GridCoverage2D classification;

        public RasterZonalStatisticsCollection(GridCoverage2D coverage, int band,
                SimpleFeatureCollection zones, GridCoverage2D classification) {
            super(zones);
            this.coverage = coverage;
            this.band = band;
            this.classification = classification;

            SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
            for (AttributeDescriptor att : zones.getSchema().getAttributeDescriptors()) {
                tb.minOccurs(att.getMinOccurs());
                tb.maxOccurs(att.getMaxOccurs());
                tb.restrictions(att.getType().getRestrictions());
                if (att instanceof GeometryDescriptor) {
                    GeometryDescriptor gatt = (GeometryDescriptor) att;
                    tb.crs(gatt.getCoordinateReferenceSystem());
                }
                tb.add("z_" + att.getLocalName(), att.getType().getBinding());
            }
            if(classification != null) {
                tb.add("classification", Integer.class);
            }
            tb.add("count", Long.class);
            tb.add("min", Double.class);
            tb.add("max", Double.class);
            tb.add("sum", Double.class);
            tb.add("avg", Double.class);
            tb.add("stddev", Double.class);
            tb.setName(zones.getSchema().getName());
            targetSchema = tb.buildFeatureType();
        }

        @Override
        public SimpleFeatureType getSchema() {
            return targetSchema;
        }

        @Override
        public SimpleFeatureIterator features() {
            return new RasterZonalStatisticsIterator(delegate.features(), coverage, band,
                    targetSchema, classification);
        }

        @Override
        public Iterator<SimpleFeature> iterator() {
            return new WrappingIterator(features());
        }

        @Override
        public void close(Iterator<SimpleFeature> close) {
            if (close instanceof WrappingIterator) {
                ((WrappingIterator) close).close();
            }
        }
    }

    /**
     * An iterator computing statistics as we go
     */
    static class RasterZonalStatisticsIterator implements SimpleFeatureIterator {
        FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(null);

        SimpleFeatureIterator zones;

        SimpleFeatureBuilder builder;

        GridCoverage2D dataCoverage;

        int band;

        RenderedImage classificationRaster;
        
        List<SimpleFeature> features = new ArrayList<SimpleFeature>();

        public RasterZonalStatisticsIterator(SimpleFeatureIterator zones, GridCoverage2D coverage,
                int band, SimpleFeatureType targetSchema, GridCoverage2D classification) {
            this.zones = zones;
            this.builder = new SimpleFeatureBuilder(targetSchema);
            this.dataCoverage = coverage;
            this.band = band;
            
            // prepare the classification image if necessary
            if(classification != null) {
                // find nodata values
                GridSampleDimension sampleDimension = classification.getSampleDimension(0);
                double[] nodataarr = sampleDimension.getNoDataValues();
                double nodata = nodataarr != null? nodataarr[0] : Double.NaN;
    
                // this will adapt the classification image to the projection and image layout
                // of the data coverage
                classificationRaster = GridCoverage2DRIA.create(classification, dataCoverage, nodata);
            }
        }

        public void close() {
            zones.close();
        }

        public boolean hasNext() {
            return features.size() > 0 || zones.hasNext();
        }

        public SimpleFeature next() throws NoSuchElementException {
            // build the next set of features if necessary
            if(features.size() == 0) {
                // grab the current zone
                SimpleFeature zone = zones.next();
    
                try {
                    // grab the geometry and eventually reproject it to the
                    Geometry zoneGeom = (Geometry) zone.getDefaultGeometry();
                    CoordinateReferenceSystem dataCrs = dataCoverage.getCoordinateReferenceSystem();
                    CoordinateReferenceSystem zonesCrs = builder.getFeatureType()
                            .getGeometryDescriptor().getCoordinateReferenceSystem();
                    if (!CRS.equalsIgnoreMetadata(zonesCrs, dataCrs)) {
                        zoneGeom = JTS.transform(zoneGeom, CRS.findMathTransform(zonesCrs, dataCrs,
                                true));
                    }
    
                    // gather the statistics
                    ZonalStats stats = processStatistics(zoneGeom);
    
                    // build the resulting feature
                    if (stats != null) {
                        if(classificationRaster != null) {
                            // if zonal stats we're going to build
                            for (Integer classZoneId : stats.getZones()) {
                                builder.addAll(zone.getAttributes());
                                builder.add(classZoneId);
                                addStatsToFeature(stats.zone(classZoneId));
                                features.add(builder.buildFeature(zone.getID()));
                            }
                        } else {
                            builder.addAll(zone.getAttributes());
                            addStatsToFeature(stats);
                            features.add(builder.buildFeature(zone.getID()));
                        }
                    } else {
                        builder.addAll(zone.getAttributes());
                        features.add(builder.buildFeature(zone.getID()));
                    }
                } catch (Exception e) {
                    throw new WPSException("Failed to compute statistics on feature " + zone, e);
                }
            } 
            // return the first feature in the current buffer
            SimpleFeature f = features.remove(0);
            return f;
        }

        /**
         * Add the statistics to the feature builder
         * @param stats
         */
        void addStatsToFeature(ZonalStats stats) {
            double sum = stats.statistic(Statistic.SUM).results().get(0).getValue();
            double avg = stats.statistic(Statistic.MEAN).results().get(0).getValue();
            builder.add(Math.round(sum / avg)); // count
            builder.add(stats.statistic(Statistic.MIN).results().get(0).getValue());
            builder.add(stats.statistic(Statistic.MAX).results().get(0).getValue());
            builder.add(sum);
            builder.add(avg);
            builder.add(stats.statistic(Statistic.SDEV).results().get(0).getValue());
        }

        private ZonalStats processStatistics(Geometry geometry) throws TransformException {
            // double checked with the tasmania simple test data, this transformation
            // actually lines up the polygons where they are supposed to be in raster space
            final AffineTransform dataG2WCorrected = new AffineTransform(
                    (AffineTransform) ((GridGeometry2D) dataCoverage.getGridGeometry())
                            .getGridToCRS2D(PixelOrientation.UPPER_LEFT));
            final MathTransform w2gTransform;
            try {
                w2gTransform = ProjectiveTransform.create(dataG2WCorrected.createInverse());
            } catch (NoninvertibleTransformException e) {
                throw new IllegalArgumentException(e.getLocalizedMessage());
            }

            GridCoverage2D cropped = null;
            try {
                // first off, cut the geometry around the coverage bounds if necessary
                ReferencedEnvelope coverageEnvelope = new ReferencedEnvelope(dataCoverage
                        .getEnvelope2D());
                ReferencedEnvelope geometryEnvelope = new ReferencedEnvelope(geometry
                        .getEnvelopeInternal(), dataCoverage.getCoordinateReferenceSystem());
                if (!coverageEnvelope.intersects((Envelope) geometryEnvelope)) {
                    // no intersection, no stats
                    return null;
                } else if (!coverageEnvelope.contains((Envelope) geometryEnvelope)) {
                    // the geometry goes outside of the coverage envelope, that makes
                    // the stats fail for some reason
                    geometry = JTS.toGeometry((Envelope) coverageEnvelope).intersection(geometry);
                    geometryEnvelope = new ReferencedEnvelope(geometry.getEnvelopeInternal(),
                            dataCoverage.getCoordinateReferenceSystem());
                }

                // check if the novalue is != from NaN
                GridSampleDimension sampleDimension = dataCoverage.getSampleDimension(0);
                List<Category> categories = sampleDimension.getCategories();
                List<Range<Double>> novalueRangeList = null;
                if (categories != null) {
                    for (Category category : categories) {
                        String catName = category.getName().toString();
                        if (catName.equalsIgnoreCase("no data")) {
                            NumberRange range = category.getRange();
                            double min = range.getMinimum();
                            double max = category.getRange().getMaximum();
                            if (!Double.isNaN(min) && !Double.isNaN(max)) {
                                // we have to filter those out
                                Range<Double> novalueRange = new Range<Double>(min, true, max, true);
                                novalueRangeList = new ArrayList<Range<Double>>();
                                novalueRangeList.add(novalueRange);
                            }
                            break;
                        }
                    }
                }

                /*
                 * crop on region of interest
                 */
                ParameterValueGroup param = CROPOPERATION.getParameters();
                param.parameter("Source").setValue(dataCoverage);
                param.parameter("Envelope").setValue(new GeneralEnvelope(geometryEnvelope));
                cropped = (GridCoverage2D) PROCESSOR.doOperation(param);
                
                // transform the geometry to raster space so that we can use it as a ROI source
                Geometry rasterSpaceGeometry= JTS.transform(geometry, w2gTransform);
                // System.out.println(rasterSpaceGeometry);
                // System.out.println(rasterSpaceGeometry.getEnvelopeInternal());
                
                // simplify the geometry so that it's as precise as the coverage, excess coordinates
                // just make it slower to determine the point in polygon relationship
                Geometry simplifiedGeometry = DouglasPeuckerSimplifier.simplify(
                        rasterSpaceGeometry, 1);
                //System.out.println(simplifiedGeometry.getEnvelopeInternal());
                
                // compensate for the jaitools range lookup poking the corner of the cells instead 
                // of their center, this makes for odd results if the polygon is just slightly 
                // misaligned with the coverage
                AffineTransformation at = new AffineTransformation();
                
                at.setToTranslation(-0.5, -0.5);
                simplifiedGeometry.apply(at);
                
                // build a shape using a fast point in polygon wrapper
                ROI roi = new ROIGeometry(simplifiedGeometry, false);

                // run the stats via JAI
                Statistic[] reqStatsArr = new Statistic[] { Statistic.MAX, Statistic.MIN,
                        Statistic.RANGE, Statistic.MEAN, Statistic.SDEV, Statistic.SUM };
                final ZonalStatsOpImage zsOp = new ZonalStatsOpImage(
                        cropped.getRenderedImage(),
                        classificationRaster, 
                        null, 
                        null, 
                        reqStatsArr, 
                        new Integer[] { band }, 
                        roi, 
                        null,
                        null,
                        null,
                        false,
                        novalueRangeList);
                return (ZonalStats) zsOp.getProperty(ZonalStatsDescriptor.ZONAL_STATS_PROPERTY);
            } finally {
                // dispose coverages
                if (cropped != null) {
                    cropped.dispose(true);
                }
            }

        }
    }
}
