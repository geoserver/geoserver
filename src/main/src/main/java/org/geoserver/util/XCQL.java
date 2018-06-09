/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.util;

import java.util.List;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.opengis.filter.Filter;

/**
 * Utility class for dealing with ECQL/CQL.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class XCQL {

    /**
     * Parses a CQL/ECQL filter list string.
     *
     * <p>This method first attempts to parse as ECQL, and on an error falls back to CQL.
     *
     * @param filter The ecql/cql string.
     * @see ECQL#toFilterList(String)
     */
    public static List<Filter> toFilterList(String filter) throws CQLException {
        try {
            return ECQL.toFilterList(filter);
        } catch (CQLException e) {
            // failed to parse as ecql, attempt to fall back on to CQL
            try {
                return CQL.toFilterList(filter);
            } catch (CQLException e1) {
                // throw back original exception
            }
            throw e;
        }
    }

    /**
     * Parses a CQL/ECQL filter string.
     *
     * <p>This method first attempts to parse as ECQL, and on an error falls back to CQL.
     *
     * @param filter The ecql/cql string.
     * @see ECQL#toFilter(String)
     */
    public static Filter toFilter(String filter) throws CQLException {
        try {
            return ECQL.toFilter(filter);
        } catch (CQLException e) {
            // failed to parse as ecql, attempt to fall back on to CQL
            try {
                return CQL.toFilter(filter);
            } catch (CQLException e1) {
                // throw back original exception
            }
            throw e;
        }
    }
}
