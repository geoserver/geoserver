/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import org.geoserver.opensearch.eo.store.OpenSearchAccess;
import org.geotools.data.Parameter;
import org.geotools.feature.NameImpl;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.PropertyName;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Container/provider for common OpenSearch parameters. Parameter keys are used as the Kvp keys in
 * URLs, an optional prefix associates them to a namespace, an optional name can be used to build
 * the fully qualified name of the parameter, otherwise the key will be used (the difference between
 * the two is used to resolve some conflicts like "relation" used by both time and geo namespaces):
 *
 * @author Andrea Aime - GeoSolutions
 */
public class OpenSearchParameters {

    public static final CoordinateReferenceSystem OUTPUT_CRS;

    public static enum GeometryRelation {
        intersects,
        disjoint,
        contains
    }

    /**
     * Possible relationships between data time validity and query one
     *
     * @author Andrea Aime - GeoSolutions
     */
    public static enum DateRelation {
        intersects,
        contains,
        during,
        disjoint,
        equals
    };

    public static final String OS_PREFIX = "os";

    public static final String TIME_PREFIX = "time";

    public static final Parameter<?> TIME_END =
            new ParameterBuilder("timeEnd", Date.class).prefix(TIME_PREFIX).name("end").build();

    public static final Parameter<?> TIME_START =
            new ParameterBuilder("timeStart", Date.class).prefix(TIME_PREFIX).name("start").build();

    public static final Parameter<?> TIME_RELATION =
            new ParameterBuilder("timeRelation", DateRelation.class)
                    .prefix(TIME_PREFIX)
                    .name("relation")
                    .build();

    public static final String GEO_PREFIX = "geo";

    public static final Parameter<?> GEO_RADIUS =
            new ParameterBuilder("radius", Double.class)
                    .prefix(GEO_PREFIX)
                    .minimumInclusive(0)
                    .build();

    public static final Parameter<?> GEO_RELATION =
            new ParameterBuilder("geoRelation", DateRelation.class)
                    .prefix(GEO_PREFIX)
                    .name("relation")
                    .build();

    public static final Parameter<?> GEO_GEOMETRY =
            new ParameterBuilder("geometry", Geometry.class).prefix(GEO_PREFIX).build();

    public static final Parameter<?> GEO_LON =
            new ParameterBuilder("lon", Double.class)
                    .prefix(GEO_PREFIX)
                    .minimumInclusive(-180)
                    .maximumInclusive(180)
                    .build();

    public static final Parameter<?> GEO_LAT =
            new ParameterBuilder("lat", Double.class)
                    .prefix(GEO_PREFIX)
                    .minimumInclusive(-90)
                    .maximumInclusive(90)
                    .build();

    public static final Parameter<?> GEO_NAME =
            new ParameterBuilder("name", String.class).prefix(GEO_PREFIX).build();

    public static final String EO_PREFIX = "eo";

    public static final Parameter<?> SEARCH_TERMS =
            new ParameterBuilder("searchTerms", String.class).prefix(OS_PREFIX).build();

    public static final Parameter<?> START_INDEX =
            new ParameterBuilder("startIndex", Integer.class).prefix(OS_PREFIX).build();

    public static final Parameter<?> GEO_UID =
            new ParameterBuilder("uid", String.class).prefix(GEO_PREFIX).build();

    public static final Parameter<?> GEO_BOX =
            new ParameterBuilder("box", Envelope.class).prefix(GEO_PREFIX).build();

    public static final String PARAM_PREFIX = "parameterPrefix";

    /** Name of the parameter in the URLs, if missing it's the same as key */
    public static final String PARAM_NAME = "parameterName";

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
                GEO_BOX,
                GEO_NAME,
                GEO_LAT,
                GEO_LON,
                GEO_RADIUS,
                GEO_GEOMETRY,
                GEO_RELATION,
                TIME_START,
                TIME_END,
                TIME_RELATION);
    }

    /** Returns the basic OpenSearch search parameters */
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

    /** Returns the OGC geo/time extension parameters */
    public static List<Parameter<?>> getGeoTimeOpensearch() {
        return GEO_TIME_OPENSEARCH;
    }

    /**
     * Returns the qualified name of a parameter, in case the parameter has a PARAM_PREFIX among its
     * metadata, or the simple parameter key other
     *
     * @param oseo Reference to the service configuration
     * @param p the parameter
     */
    public static String getQualifiedParamName(OSEOInfo oseo, Parameter p) {
        return getQualifiedParamName(oseo, p, true);
    }

    /**
     * Returns the qualified name of a parameter, in case the parameter has a PARAM_PREFIX among its
     * metadata, or the simple parameter key other
     *
     * @param oseo Reference to the service configuration
     */
    public static String getQualifiedParamName(
            OSEOInfo oseo, Parameter p, boolean qualifyOpenSearchNative) {
        String name = getParameterName(p);

        String prefix = getParameterPrefix(p);
        if (prefix != null) {
            if ((OS_PREFIX.equals(prefix))) {
                if (qualifyOpenSearchNative) {
                    return prefix + ":" + name;
                } else {
                    return name;
                }
            } else if (ProductClass.isProductClass(oseo, prefix)) {
                // all the EO parameters should be put in the EO namespace
                return "eo:" + name;
            }
            return prefix + ":" + name;
        } else {
            return name;
        }
    }

    /** Returns the PARAM_PREFIX entry found in the parameter metadata, if any */
    public static String getParameterPrefix(Parameter p) {
        String prefix = p.metadata == null ? null : (String) p.metadata.get(PARAM_PREFIX);
        return prefix;
    }

    /**
     * Returns the PARAM_NAME entry found in the parameter metadata, if any, or the key otherwise
     */
    public static String getParameterName(Parameter p) {
        String name = p.metadata == null ? null : (String) p.metadata.get(PARAM_NAME);
        if (name == null) {
            name = p.key;
        }
        return name;
    }

    /**
     * Builds the {@link PropertyName} for the given OpenSearch parameter
     *
     * @param oseo Reference to the service configuration
     * @param ff The filter factory used to build the filters
     */
    public static PropertyName getFilterPropertyFor(
            OSEOInfo oseo, FilterFactory2 ff, Parameter<?> parameter) {
        String prefix = getParameterPrefix(parameter);
        String namespace = null;

        if (EO_PREFIX.equals(prefix)) {
            namespace = OpenSearchAccess.EO_NAMESPACE;
        } else {
            // product parameter maybe?
            ProductClass pc = ProductClass.getProductClassFromPrefix(oseo, prefix);
            namespace = pc.getNamespace();
        }

        // the name
        String name = getParameterName(parameter);
        return ff.property(new NameImpl(namespace, name));
    }
}
