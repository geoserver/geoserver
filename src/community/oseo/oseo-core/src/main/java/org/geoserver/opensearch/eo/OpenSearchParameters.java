/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.geotools.data.Parameter;
import org.geotools.referencing.CRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Envelope;

/**
 * Container/provider for common OpenSearch parameters
 *
 * @author Andrea Aime - GeoSolutions
 */
public class OpenSearchParameters {
    
    public static final CoordinateReferenceSystem OUTPUT_CRS;

    public static final String OS_PREFIX = "os";

    public static final String TIME_PREFIX = "time";

    public static final String GEO_PREFIX = "geo";

    public static final String EO_PREFIX = "eo";

    public static final Parameter<?> SEARCH_TERMS = new ParameterBuilder("searchTerms",
            String.class).prefix(OS_PREFIX).build();

    public static final Parameter<?> START_INDEX = new ParameterBuilder("startIndex", Integer.class)
            .prefix(OS_PREFIX).build();
    
    public static final Parameter<?> GEO_UID = new ParameterBuilder("uid", String.class).prefix(GEO_PREFIX).build();

    public static final String PARAM_PREFIX = "parameterPrefix";

    public static final String MIN_INCLUSIVE = "minInclusive";

    public static final String MAX_INCLUSIVE = "maxInclusive";

    private static final List<Parameter<?>> BASIC_OPENSEARCH;

    private static final List<Parameter<?>> GEO_TIME_OPENSEARCH;

    static {
        BASIC_OPENSEARCH = basicOpenSearchParameters();
        GEO_TIME_OPENSEARCH = geoTimeOpenSearchParameters();
        try {
            OUTPUT_CRS = CRS.decode("urn:ogc:def:crs:EPSG:4326", false);
        } catch (FactoryException e) {
            throw new RuntimeException("Unepected error decoding wgs84 in lat/lon order", e);
        }
    }

    private static List<Parameter<?>> basicOpenSearchParameters() {
        return Arrays.asList( //
                SEARCH_TERMS, START_INDEX);
    }

    private static List<Parameter<?>> geoTimeOpenSearchParameters() {
        return Arrays.asList( //
                GEO_UID,
                new ParameterBuilder("box", Envelope.class).prefix(GEO_PREFIX).build(),
                new ParameterBuilder("name", String.class).prefix(GEO_PREFIX).build(),
                new ParameterBuilder("lat", Double.class).prefix(GEO_PREFIX).minimumInclusive(-90)
                        .maximumInclusive(90).build(),
                new ParameterBuilder("lon", Double.class).prefix(GEO_PREFIX).minimumInclusive(-180)
                        .maximumInclusive(180).build(),
                new ParameterBuilder("radius", Double.class).prefix(GEO_PREFIX).minimumInclusive(0)
                        .build(),
                new ParameterBuilder("start", Date.class).prefix(TIME_PREFIX).build(),
                new ParameterBuilder("end", Date.class).prefix(TIME_PREFIX).build());
    }

    /**
     * Returns the basic OpenSearch search parameters
     * 
     * @return
     */
    public static List<Parameter<?>> getBasicOpensearch(OSEOInfo info) {
        List<Parameter<?>> result = new ArrayList<>(BASIC_OPENSEARCH);

        ParameterBuilder count = new ParameterBuilder("count", Integer.class).prefix(OS_PREFIX);
        count.minimumInclusive(0);
        if (info.getMaximumRecordsPerPage() > 0) {
            count.maximumInclusive(info.getMaximumRecordsPerPage());
        }
        result.add(count.build());

        return result;
    }

    /**
     * Returns the OGC geo/time extension parameters
     * 
     * @return
     */
    public static List<Parameter<?>> getGeoTimeOpensearch() {
        return GEO_TIME_OPENSEARCH;
    }

    /**
     * Returns the qualified name of a parameter, in case the parameter has a PARAM_PREFIX among its metadata, or the simple parameter key other
     * 
     * @param p
     * @return
     */
    public static String getQualifiedParamName(Parameter p) {
        return getQualifiedParamName(p, true);
    }

    /**
     * Returns the qualified name of a parameter, in case the parameter has a PARAM_PREFIX among its metadata, or the simple parameter key other
     * 
     * @param p
     * @return
     */
    public static String getQualifiedParamName(Parameter p, boolean qualifyOpenSearchNative) {
        String prefix = p.metadata == null ? null : (String) p.metadata.get(PARAM_PREFIX);
        if (prefix != null && (!OS_PREFIX.equals(prefix) || qualifyOpenSearchNative)) {
            return prefix + ":" + p.key;
        } else {
            return p.key;
        }
    }
}
