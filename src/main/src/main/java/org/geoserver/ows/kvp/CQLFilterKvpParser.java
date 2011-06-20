/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.ows.kvp;

import java.util.List;

import org.geoserver.ows.KvpParser;
import org.geoserver.platform.ServiceException;
import org.geoserver.util.XCQL;
import org.geotools.filter.text.cql2.CQLException;


/**
 * Parses the CQL_FILTER parameter into a list of filters
 * @author Andrea Aime - TOPP
 *
 */
public class CQLFilterKvpParser extends KvpParser {
    public CQLFilterKvpParser() {
        super("cql_filter", List.class);
    }

    public Object parse(String value) throws Exception {
        try {
            return XCQL.toFilterList(value);
        } catch (CQLException pe) {
            throw new ServiceException("Could not parse CQL filter list.", pe);
        }
    }
}
