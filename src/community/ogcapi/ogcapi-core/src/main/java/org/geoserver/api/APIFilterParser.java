/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api;

import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.spatial.DefaultCRSFilterVisitor;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;

/** Centralizes filter/filter-lang handling. */
public class APIFilterParser {

    private static final FilterFactory2 FF = CommonFactoryFinder.getFilterFactory2();
    public static String CQL_TEXT = "cql-text";

    /**
     * Parses the filter over the supported filter languages (right now, only {@link #CQL_TEXT}) and
     * defaults the geometry liters in spatial filters to CRS84
     */
    public Filter parse(String filter, String filterLang) {
        if (filter == null) {
            return null;
        }

        // right now there is a spec only for cql-text, will be extended when more languages are
        // recognized (could have its own extension point too, if we want to allow easy extension
        // with new custom languages)
        if (filterLang != null && !filterLang.equals(CQL_TEXT)) {
            throw new InvalidParameterValueException(
                    "Only supported filter-lang at the moment is "
                            + CQL_TEXT
                            + " but '"
                            + filterLang
                            + "' was found instead");
        }

        try {
            Filter parsedFilter = ECQL.toFilter(filter);
            // in OGC APIs assume CRS84 as the default, but the underlying machinery may default to
            // the native CRS in EPSG axis order instead, best making the CRS explicit instead
            DefaultCRSFilterVisitor crsDefaulter =
                    new DefaultCRSFilterVisitor(FF, DefaultGeographicCRS.WGS84);
            return (Filter) parsedFilter.accept(crsDefaulter, null);
        } catch (CQLException e) {
            throw new InvalidParameterValueException(e.getMessage(), e);
        }
    }
}
