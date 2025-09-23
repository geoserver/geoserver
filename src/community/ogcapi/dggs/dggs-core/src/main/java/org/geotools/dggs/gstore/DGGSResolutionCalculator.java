/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2020, Open Source Geospatial Foundation (OSGeo)
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
package org.geotools.dggs.gstore;

import static java.lang.Integer.MAX_VALUE;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static org.geotools.dggs.gstore.DGGSStore.VP_RESOLUTION;
import static org.geotools.dggs.gstore.DGGSStore.VP_RESOLUTION_DELTA;

import java.util.Map;
import java.util.Optional;
import org.geotools.api.data.Query;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.dggs.DGGSInstance;
import org.geotools.dggs.Zone;
import org.geotools.filter.function.EnvFunction;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.renderer.lite.RendererUtilities;
import org.geotools.util.ConverterFactory;
import org.geotools.util.Converters;
import org.geotools.util.NumberRange;
import org.geotools.util.factory.Hints;
import org.locationtech.jts.algorithm.MinimumBoundingCircle;
import org.locationtech.jts.geom.Polygon;

/** Helper class extracting the target resolution at which to render a DGGS */
public class DGGSResolutionCalculator {

    /* The GetMap scale denominator, as an {@link Double}, duplicated here to avoid a dependency
     * onto gs-wms */
    public static final String WMS_SCALE_DENOMINATOR = "WMS_SCALE_DENOMINATOR";

    private static final double DISTANCE_SCALE_FACTOR = 0.0254 / (25.4 / 0.28);

    /** The key used to store the resolution offset in the layer metadata */
    public static final String CONFIGURED_OFFSET_KEY = "dggs.resOffset";

    static final Hints.ConfigurationMetadataKey OFFSET_HINTS_KEY =
            Hints.ConfigurationMetadataKey.get(CONFIGURED_OFFSET_KEY);

    /** The key used to store the minimum resolution in the layer metadata */
    public static final String CONFIGURED_MINRES_KEY = "dggs.minResolution";

    static final Hints.ConfigurationMetadataKey MINRES_HINTS_KEY =
            Hints.ConfigurationMetadataKey.get(CONFIGURED_MINRES_KEY);

    /** The key used to store the maximum resolution in the layer metadata */
    public static final String CONFIGURED_MAXRES_KEY = "dggs.maxResolution";

    static final Hints.ConfigurationMetadataKey MAXRES_HINTS_KEY =
            Hints.ConfigurationMetadataKey.get(CONFIGURED_MAXRES_KEY);

    double[] levelThresholds;

    public DGGSResolutionCalculator(DGGSInstance dggs) {
        // compute threshold switch levels (arbitrary heuristic)
        levelThresholds = new double[dggs.getResolutions().length];
        for (int i = 0; i < levelThresholds.length; i++) {
            Zone zone = dggs.getZone(0, 0, i);
            Polygon polygon = zone.getBoundary();
            double radius = new MinimumBoundingCircle(polygon).getRadius();
            levelThresholds[i] = radius / 100;
        }
    }

    public int getTargetResolution(Query query, int defaultResolution) {
        Hints hints = query.getHints();

        Optional<Map> viewParams = Optional.ofNullable(hints.get(Hints.VIRTUAL_TABLE_PARAMETERS))
                .filter(Map.class::isInstance)
                .map(Map.class::cast);

        // did the user ask for a specific resolution?
        Optional<Integer> requestedResolution =
                viewParams.map(m -> m.get(VP_RESOLUTION)).map(n -> safeConvert(n, Integer.class));
        if (requestedResolution.isPresent()) {
            return validateResolution(requestedResolution.get().intValue());
        }

        // the simplificaiton distance varies too much as we pan around on projections
        // with significant deformations (e.g., polar, web mercator) leading to resolution
        // switches and excess zone generation. Try something more stable and predictable first,
        // like the OGC scale denominator.
        Optional<Double> distance;
        Double sd = safeConvert(EnvFunction.getLocalValues().get(WMS_SCALE_DENOMINATOR), Double.class);
        if (sd != null) {
            distance = Optional.of(scaleToDistance(DefaultGeographicCRS.WGS84, sd));
        } else {
            distance = getDoubleHint(hints, Hints.GEOMETRY_DISTANCE);
        }

        // do we have a resoution delta?
        Optional<Integer> resolutionDelta =
                viewParams.map(m -> m.get(VP_RESOLUTION_DELTA)).map(n -> safeConvert(n, Integer.class));
        // if not available through the request, try the values coming from the configuration
        if (resolutionDelta.isEmpty()) {
            resolutionDelta = getIntegerHint(hints, OFFSET_HINTS_KEY);
        }
        int resOffset = resolutionDelta.orElse(0);

        // compute resolution and eventually apply delta
        int resolution = distance.map(n -> getResolutionFromThresholds(n.doubleValue()) + resOffset)
                .orElse(defaultResolution);

        // see if there is a min/max resolution set, if so, use it (don't limit to 0 on purpose,
        // if a resolution has been forced to an invalid value, it should not return zones)
        resolution = max(getIntegerHint(hints, MINRES_HINTS_KEY).orElse(-MAX_VALUE), resolution);
        resolution = min(getIntegerHint(hints, MAXRES_HINTS_KEY).orElse(MAX_VALUE), resolution);

        return resolution;
    }

    /** Given the hints and a key, returns the integer value associated with the key, as an {@link Optional}. */
    private Optional<Integer> getIntegerHint(Hints hints, Object key) {
        return Optional.ofNullable(hints.get(key)).map(n -> safeConvert(n, Integer.class));
    }

    /** Given the hints and a key, returns the double value associated with the key, as an {@link Optional}. */
    private Optional<Double> getDoubleHint(Hints hints, Object key) {
        return Optional.ofNullable(hints.get(key)).map(n -> safeConvert(n, Double.class));
    }

    /**
     * Converts a scale denominator to a generalization distance using the OGC SLD scale denominator computation rules
     *
     * @param crs The CRS of the data
     * @param scaleDenominator The target scale denominator
     * @return The generalization distance
     */
    double scaleToDistance(CoordinateReferenceSystem crs, double scaleDenominator) {
        return scaleDenominator * DISTANCE_SCALE_FACTOR / RendererUtilities.toMeters(1, crs);
    }

    private int getResolutionFromThresholds(double distance) {
        if (distance == 0) return 0;

        for (int i = 0; i < levelThresholds.length; i++) {
            if (levelThresholds[i] < distance) return i;
        }
        return levelThresholds.length - 1;
    }

    private int validateResolution(int resolution) {
        if (resolution < 0 || resolution >= levelThresholds.length) {
            throw new IllegalArgumentException("Requested resolution "
                    + resolution
                    + " is not valid, please provide a value between 0 and "
                    + (levelThresholds.length - 1));
        }

        return resolution;
    }

    private <T> T safeConvert(Object n, Class<T> target) {
        return Converters.convert(n, target, new Hints(ConverterFactory.SAFE_CONVERSION, true));
    }

    /** Returns true if the given resolution is valid for DGGS at hand, false otherwise. */
    public boolean isValid(int targetResolution) {
        return targetResolution >= 0 && targetResolution < levelThresholds.length;
    }

    public NumberRange<Integer> getValidResolutions() {
        return new NumberRange<>(Integer.class, 0, levelThresholds.length - 1);
    }
}
