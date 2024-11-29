/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import org.geootols.filter.text.cql_2.CQL2;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.FilterFactory;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.FilterFactoryImpl;
import org.geotools.filter.spatial.DefaultCRSFilterVisitor;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.cqljson.CQLJsonCompiler;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;

/** Centralizes filter/filter-lang handling. */
public class APIFilterParser {

    private static final FilterFactory FF = CommonFactoryFinder.getFilterFactory();
    public static String CQL_TEXT =
            "cql-text"; // for compatibility, but should not really be advertised
    public static String ECQL_TEXT = "ecql-text"; // GeoServer own CQL
    public static String CQL2_TEXT = "cql2-text"; // OGC CQL2
    public static String CQL2_JSON = "cql2-json"; // OGC CQL2-JSON, see requirement 38
    // https://portal.ogc.org/files/96288#cql-json

    /** The list of encodings that should go in API documents */
    public static Set<String> SUPPORTED_ENCODINGS =
            new LinkedHashSet<>(Arrays.asList(ECQL_TEXT, CQL2_TEXT, CQL2_JSON));

    /**
     * Parses the filter over the supported filter languages (right now, only {@link #CQL_TEXT},
     * {@link #CQL2_JSON} and {@link #CQL_OBJECT}) and defaults the geometry literals in spatial
     * filters to CRS84
     */
    public Filter parse(String filter, String filterLang) {
        return parse(filter, filterLang, null);
    }

    /**
     * Parses the filter over the supported filter languages (right now, only {@link #CQL_TEXT},
     * {@link #CQL2_JSON} and {@link #CQL_OBJECT}) and defaults the geometry literals in spatial
     * filters to filter crs.
     */
    public Filter parse(String filter, String filterLang, String filterCRS) {
        if (filter == null) {
            return null;
        }

        // for backwards compatibility
        if (CQL_TEXT.equals(filterLang)) filterLang = ECQL_TEXT;

        // by OGC-API filter spec
        if (filterLang == null) filterLang = CQL2_TEXT;

        // by OGC-API filter spec
        CoordinateReferenceSystem queryCRS = DefaultGeographicCRS.WGS84;
        if (filterCRS != null) {
            try {
                queryCRS = CRS.decode(filterCRS);
            } catch (FactoryException e) {
                throw new InvalidParameterValueException(e.getMessage(), e);
            }
        }

        // right now there is a spec only for cql-text, cql-json and cql-object, will be extended
        // when more
        // languages are recognized (could have its own extension point too,
        // if we want to allow easy extension with new custom languages)
        if (filterLang != null && (!SUPPORTED_ENCODINGS.contains(filterLang))) {
            throw new InvalidParameterValueException(
                    "Only supported filter-lang options at the moment are "
                            + SUPPORTED_ENCODINGS
                            + " but '"
                            + filterLang
                            + "' was found instead");
        }

        try {
            Filter parsedFilter = null;
            if (ECQL_TEXT.equals(filterLang)) {
                parsedFilter = ECQL.toFilter(filter);
            } else if (CQL2_JSON.equals(filterLang)) {
                CQLJsonCompiler cqlJsonCompiler =
                        new CQLJsonCompiler(filter, new FilterFactoryImpl());
                cqlJsonCompiler.compileFilter();
                parsedFilter = cqlJsonCompiler.getFilter();
            } else {
                parsedFilter = CQL2.toFilter(filter);
            }

            // in OGC APIs assume CRS84 as the default, but the underlying machinery may default to
            // the native CRS in EPSG axis order instead, best making the CRS explicit instead
            if (parsedFilter != null) {
                DefaultCRSFilterVisitor crsDefaulter = new DefaultCRSFilterVisitor(FF, queryCRS);
                parsedFilter = (Filter) parsedFilter.accept(crsDefaulter, null);
                return parsedFilter;
            } else {
                return null;
            }
        } catch (CQLException e) {
            throw new InvalidParameterValueException(e.getMessage(), e);
        }
    }
}
