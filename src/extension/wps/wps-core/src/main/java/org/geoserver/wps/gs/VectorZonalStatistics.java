/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs;

import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

import org.geoserver.wps.WPSException;
import org.geoserver.wps.gs.AggregateProcess.AggregationFunction;
import org.geoserver.wps.gs.AggregateProcess.Results;
import org.geoserver.wps.jts.DescribeParameter;
import org.geoserver.wps.jts.DescribeProcess;
import org.geoserver.wps.jts.DescribeResult;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.collection.DecoratingSimpleFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Geometry;

@DescribeProcess(title = "vectorZonalStatistics", description = "Provides statistics for the distribution "
        + "of a certain quantity in a set of reference areas. "
        + "The data layer must be a point layer, the reference layer must be a polygonal one")
public class VectorZonalStatistics implements GeoServerProcess {

    @DescribeResult(name = "statistics", description = "A feature collection with all the attributes "
            + "of the zoning layer (prefixed by 'z_'), "
            + "and the statistics fields count/min/max/sum/avg/stddev")
    public SimpleFeatureCollection execute(
            @DescribeParameter(name = "data", description = "The point layer containing "
                    + "the data to be used in the statistics") SimpleFeatureCollection data,
            @DescribeParameter(name = "dataAttribute", description = "The attribute to be used for "
                    + "the computation of the statistics") String dataAttribute,
            @DescribeParameter(name = "zones", description = "The various zones in which the statistics will be computed. "
                    + "Must be a polygon layer, each polygon will be used to generate a separate statistic") SimpleFeatureCollection zones) {

        AttributeDescriptor dataDescriptor = data.getSchema().getDescriptor(dataAttribute);
        if (dataDescriptor == null) {
            throw new IllegalArgumentException("Attribute " + dataAttribute + " not found in "
                    + data.getSchema());
        }

        return new ZonalStatisticsCollection(data, dataAttribute, zones);
    }

    /**
     * A feature collection that computes zonal statitics in a streaming fashion
     * @author Andrea Aime - OpenGeo
     */
    static class ZonalStatisticsCollection extends DecoratingSimpleFeatureCollection {
        SimpleFeatureCollection data;

        String dataAttribute;

        SimpleFeatureType targetSchema;

        public ZonalStatisticsCollection(SimpleFeatureCollection data, String dataAttribute,
                SimpleFeatureCollection zones) {
            super(zones);
            this.dataAttribute = dataAttribute;
            this.data = data;

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
            AttributeDescriptor dataDescriptor = data.getSchema().getDescriptor(dataAttribute);
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
            return new ZonalStatisticsIterator(delegate.features(), dataAttribute, data,
                    targetSchema);
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
    static class ZonalStatisticsIterator implements SimpleFeatureIterator {
        Set<AggregationFunction> FUNCTIONS = new HashSet<AggregationFunction>() {
            {
                add(AggregationFunction.Count);
                add(AggregationFunction.Max);
                add(AggregationFunction.Min);
                add(AggregationFunction.Sum);
                add(AggregationFunction.Average);
                add(AggregationFunction.StdDev);
            }
        };

        FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(null);

        SimpleFeatureIterator zones;

        String dataAttribute;

        SimpleFeatureCollection data;

        SimpleFeatureBuilder builder;

        String dataGeomName;

        public ZonalStatisticsIterator(SimpleFeatureIterator zones, String dataAttribute,
                SimpleFeatureCollection data, SimpleFeatureType targetSchema) {
            this.zones = zones;
            this.dataAttribute = dataAttribute;
            this.data = data;
            this.builder = new SimpleFeatureBuilder(targetSchema);
            this.dataGeomName = data.getSchema().getGeometryDescriptor().getLocalName();
        }

        public void close() {
            zones.close();
        }

        public boolean hasNext() {
            return zones.hasNext();
        }

        public SimpleFeature next() throws NoSuchElementException {
            // grab the current zone
            SimpleFeature zone = zones.next();

            try {
                // grab the geometry and eventually reproject it
                Geometry zoneGeom = (Geometry) zone.getDefaultGeometry();
                CoordinateReferenceSystem dataCrs = data.getSchema().getCoordinateReferenceSystem();
                CoordinateReferenceSystem zonesCrs = builder.getFeatureType()
                        .getGeometryDescriptor().getCoordinateReferenceSystem();
                if (!CRS.equalsIgnoreMetadata(zonesCrs, dataCrs)) {
                    zoneGeom = JTS.transform(zoneGeom, CRS.findMathTransform(zonesCrs, dataCrs,
                            true));
                }

                // build the filter and gather the statistics
                Filter areaFilter = ff.within(ff.property(dataGeomName), ff.literal(zoneGeom));
                SimpleFeatureCollection zoneCollection = data.subCollection(areaFilter);
                Results stats = new AggregateProcess().execute(zoneCollection, dataAttribute,
                        FUNCTIONS, true, null);

                // build the resulting feature
                builder.addAll(zone.getAttributes());
                if(stats != null) {
                    builder.add(stats.getCount());
                    builder.add(stats.getMin());
                    builder.add(stats.getMax());
                    builder.add(stats.getSum());
                    builder.add(stats.getAverage());
                    builder.add(stats.getStandardDeviation());
                }
                return builder.buildFeature(zone.getID());
            } catch (Exception e) {
                throw new WPSException("Failed to compute statistics on feature " + zone, e);
            }
        }

    }
}
