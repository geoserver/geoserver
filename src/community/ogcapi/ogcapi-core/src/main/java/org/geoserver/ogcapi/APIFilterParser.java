/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi;

import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.FilterFactoryImpl;
import org.geotools.filter.spatial.DefaultCRSFilterVisitor;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.cqljson.CQLJsonCompiler;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/** Centralizes filter/filter-lang handling. */
public class APIFilterParser {

    private static final FilterFactory2 FF = CommonFactoryFinder.getFilterFactory2();
    public static String CQL_TEXT = "cql-text";
    public static String CQL_JSON = "cql-json";

    /**
     * Parses the filter over the supported filter languages (right now, only {@link #CQL_TEXT} and
     * {@link #CQL_OBJECT}) and defaults the geometry literals in spatial filters to CRS84
     */
    public Filter parse(String filter, String filterLang) {
        return parse(filter, filterLang, null);
    }

    /**
     * Parses the filter over the supported filter languages (right now, only {@link #CQL_TEXT} and
     * {@link #CQL_OBJECT}) and defaults the geometry literals in spatial filters to filter crs.
     */
    public Filter parse(String filter, String filterLang, String filterCRS) {
        if (filter == null) {
            return null;
        }

        // by OGC-API filter spec
        CoordinateReferenceSystem queryCRS = DefaultGeographicCRS.WGS84;
        if (filterCRS != null) {
            try {
                queryCRS = CRS.decode(filterCRS);
            } catch (FactoryException e) {
                throw new InvalidParameterValueException(e.getMessage(), e);
            }
        }

        // right now there is a spec only for cql-text and cql-object, will be extended when more
        // languages are
        // recognized (could have its own extension point too, if we want to allow easy extension
        // with new custom languages)
        if (filterLang != null && (!filterLang.equals(CQL_TEXT) && !filterLang.equals(CQL_JSON))) {
            throw new InvalidParameterValueException(
                    "Only supported filter-lang options at the moment are "
                            + CQL_TEXT
                            + " and "
                            + CQL_JSON
                            + " but '"
                            + filterLang
                            + "' was found instead");
        }

        try {
            Filter parsedFilter = null;
            if (filterLang == null || filterLang.equals(CQL_TEXT)) {
                parsedFilter = ECQL.toFilter(filter);
            } else if (filterLang.equals(CQL_JSON)) {
                CQLJsonCompiler cqlJsonCompiler =
                        new CQLJsonCompiler(filter, new FilterFactoryImpl());
                cqlJsonCompiler.compileFilter();
                parsedFilter = cqlJsonCompiler.getFilter();
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
