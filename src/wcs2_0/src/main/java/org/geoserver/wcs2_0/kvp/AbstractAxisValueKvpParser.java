/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0.kvp;

import java.util.ArrayList;
import java.util.List;
import org.geoserver.ows.KvpParser;

/**
 * Base class for parsing axis(value)[,axis(value)]* syntax
 *
 * @author Andrea Aime - GeoSolutions
 */
public abstract class AbstractAxisValueKvpParser<T> extends KvpParser {

    public AbstractAxisValueKvpParser(String key, Class binding) {
        super(key, binding);
    }

    protected List<T> parseItem(String spec) throws Exception {
        // clean up extra space
        spec = spec.trim();

        List<T> results = new ArrayList<T>();
        int base = 0;
        for (; ; ) {
            // search the open parenthesis
            int idxOpen = spec.indexOf("(", base);
            if (idxOpen == -1) {
                throwInvalidSyntaxException(null);
            }
            int idxNextOpen = spec.indexOf("(", idxOpen + 1);

            // search the closed parens
            int idxClosed = spec.indexOf(")", idxOpen);
            if (idxClosed == -1 || (idxNextOpen > 0 && idxClosed > idxNextOpen)) {
                throwInvalidSyntaxException(null);
            }
            int idxNextClosed = spec.indexOf(")", idxClosed + 1);

            // extract the two components
            String axisName = spec.substring(base, idxOpen);
            String value = spec.substring(idxOpen + 1, idxClosed);

            T result = buildItem(axisName, value);
            results.add(result);

            // we should also have a comma after the closed parens
            int idxSeparator = spec.indexOf(",", idxClosed);
            if (idxSeparator == -1) {
                if (idxClosed == spec.length() - 1) {
                    return results;
                } else {
                    throwInvalidSyntaxException(null);
                }
            } else {
                if (idxSeparator > idxNextClosed) {
                    throwInvalidSyntaxException(null);
                }
                base = idxSeparator + 1;
            }
        }
    }

    protected abstract T buildItem(String axisName, String value);

    protected abstract void throwInvalidSyntaxException(Exception e);
}
