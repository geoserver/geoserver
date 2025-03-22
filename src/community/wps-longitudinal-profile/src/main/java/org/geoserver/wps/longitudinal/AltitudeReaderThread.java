/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.longitudinal;

import static java.math.RoundingMode.HALF_UP;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.geotools.api.data.FeatureSource;
import org.geotools.api.data.Query;
import org.geotools.api.feature.Feature;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.FilterFactory;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.api.util.ProgressListener;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureIterator;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.jaitools.jts.CoordinateSequence2D;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

class AltitudeReaderThread implements Callable<List<ProfileVertice>> {
    static final Logger LOGGER = Logging.getLogger(AltitudeReaderThread.class);

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();
    private static final FilterFactory FF = CommonFactoryFinder.getFilterFactory();
    private final ProgressListener monitor;
    private final CoverageSampler sampler;
    private final DistanceSlopeCalculator calculator;
    private MathTransform tx;
    private List<ProfileVertice> pvs;
    FeatureSource adjustmentFeatureSource;
    String altitudeName;

    public AltitudeReaderThread(
            List<ProfileVertice> pvs,
            int altitudeIndex,
            FeatureSource adjustmentFeatureSource,
            String altitudeName,
            GridCoverage2D gridCoverage2D,
            DistanceSlopeCalculator calculator,
            ProgressListener monitor)
            throws FactoryException {
        this.pvs = pvs;
        this.adjustmentFeatureSource = adjustmentFeatureSource;
        this.altitudeName = altitudeName;
        this.monitor = monitor;
        this.calculator = calculator;
        this.sampler = new CoverageSampler(gridCoverage2D, altitudeIndex);

        // do we need to reproject the points?
        CoordinateReferenceSystem targetProjection = calculator.getProjection();
        CoordinateReferenceSystem sourceProjection = gridCoverage2D.getCoordinateReferenceSystem2D();
        if (CRS.isTransformationRequired(sourceProjection, targetProjection))
            this.tx = CRS.findMathTransform(sourceProjection, targetProjection, true);
    }

    @Override
    public List<ProfileVertice> call() throws Exception {
        Coordinate[] coords = pvs.stream()
                .map(ProfileVertice::getCoordinate)
                .collect(Collectors.toList())
                .toArray(new Coordinate[pvs.size()]);
        Geometry geometry = GEOMETRY_FACTORY.createLineString(coords);

        Map<Geometry, Double> adjGeomValues = new HashMap<>();
        if (adjustmentFeatureSource != null) {
            FeatureType schema = adjustmentFeatureSource.getSchema();
            Filter filter =
                    FF.intersects(FF.property(schema.getGeometryDescriptor().getName()), FF.literal(geometry));
            Query query = new Query(schema.getName().getLocalPart(), filter);
            try (FeatureIterator<?> featureIterator =
                    adjustmentFeatureSource.getFeatures(query).features()) {
                while (featureIterator.hasNext()) {
                    Feature f = featureIterator.next();
                    Geometry g = (Geometry) f.getDefaultGeometryProperty().getValue();
                    Double altitude = (Double) f.getProperty(altitudeName).getValue();
                    adjGeomValues.put(g, altitude);
                    if (monitor.isCanceled()) return Collections.emptyList();
                }
            }
        }

        boolean first = true;
        for (ProfileVertice pv : pvs) {
            LOGGER.fine("processing position:" + pv.getCoordinate());
            Point point =
                    new Point(new CoordinateSequence2D(pv.getCoordinate().x, pv.getCoordinate().y), GEOMETRY_FACTORY);
            Double altCorrection = getAltitudeCorrection(adjGeomValues, point);

            double altitude = sampler.sample(pv.getCoordinate());
            double roundedAltitude =
                    BigDecimal.valueOf(altitude).setScale(2, HALF_UP).doubleValue();
            pv.setAltitude(roundedAltitude - altCorrection);

            // reproject if necessary
            if (this.tx != null) {
                point = (Point) JTS.transform(point, tx);
                pv.setCoordinate(point.getCoordinate());
            }

            // compute distance and slope
            calculator.next(point, pv.getAltitude());
            if (first) {
                first = false;
            } else {
                pv.setDistancePrevious(calculator.getDistance());
                pv.setSlope(calculator.getSlope());
            }

            if (monitor.isCanceled()) return null;
        }
        return pvs;
    }

    private static Double getAltitudeCorrection(Map<Geometry, Double> adjGeomValues, Point point) {
        Double altCorrection = 0.0;
        for (Map.Entry<Geometry, Double> entry : adjGeomValues.entrySet()) {
            if (entry.getKey().intersects(point)) {
                altCorrection = entry.getValue();
                break;
            }
        }
        return altCorrection;
    }
}
