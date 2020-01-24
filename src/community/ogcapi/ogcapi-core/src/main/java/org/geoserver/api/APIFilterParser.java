/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api;

import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.opengis.filter.Filter;

/** Centralizes filter/filter-lang handling. */
public class APIFilterParser {

    public static String CQL_TEXT = "cql-text";

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
            return ECQL.toFilter(filter);
        } catch (CQLException e) {
            throw new InvalidParameterValueException(e.getMessage(), e);
        }
    }
}
