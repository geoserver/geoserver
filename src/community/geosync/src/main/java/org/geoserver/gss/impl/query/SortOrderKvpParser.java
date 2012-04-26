/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.gss.impl.query;

import org.geoserver.ows.KvpParser;
import org.geoserver.platform.ServiceException;
import org.opengis.filter.sort.SortOrder;

/**
 * {@link SortOrder} KVP parser
 * 
 */
public class SortOrderKvpParser extends KvpParser {

    public SortOrderKvpParser(final String key) {
        super(key, SortOrder.class);
    }

    /**
     * @param value
     *            one of {@code ASCENDING} or {@code DESCENDING}
     * @see org.geoserver.ows.KvpParser#parse(java.lang.String)
     */
    @Override
    public Object parse(String value) throws Exception {
        if (SortOrder.ASCENDING.name().equals(value)) {
            return SortOrder.ASCENDING;
        }
        if (SortOrder.DESCENDING.name().equals(value)) {
            return SortOrder.DESCENDING;
        }
        throw new ServiceException("Unknown SortOrder: '" + value
                + "'. Expected one of ASCENDING|DESCENDING", "InvalidParameterValue", getKey());
    }
}
