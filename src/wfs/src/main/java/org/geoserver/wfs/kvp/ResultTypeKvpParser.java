/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.kvp;

import net.opengis.wfs.ResultTypeType;
import org.geoserver.ows.KvpParser;

/**
 * Parses a kvp of the form resultType=<hits|results>.
 *
 * <p>Allowable values are "hits", and "results", which get parsed into the following respectivley.
 *
 * <ul>
 *   <li>{@link net.opengis.wfs.ResultTypeType#HITS_LITERAL}
 *   <li>{@link net.opengis.wfs.ResultTypeType#RESULTS_LITERAL}
 * </ul>
 *
 * @author Justin Deoliveira, The Open Planning Project
 */
public class ResultTypeKvpParser extends KvpParser {
    public ResultTypeKvpParser() {
        super("resultType", ResultTypeType.class);
    }

    public Object parse(String value) throws Exception {
        if ("hits".equalsIgnoreCase(value)) {
            return ResultTypeType.HITS_LITERAL;
        }

        if ("results".equalsIgnoreCase(value)) {
            return ResultTypeType.RESULTS_LITERAL;
        }

        return null;
    }
}
