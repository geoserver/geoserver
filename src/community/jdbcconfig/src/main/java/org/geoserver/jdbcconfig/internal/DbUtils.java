/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jdbcconfig.internal;

import com.google.common.collect.Maps;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geotools.util.logging.Logging;

public class DbUtils {

    static final Logger LOGGER = Logging.getLogger(DbUtils.class.getPackage().getName());

    public static Map<String, ?> params(Object... kv) {
        Map<String, Object> params = Maps.newHashMap();
        String paramName;
        Object paramValue;
        for (int i = 0; i < kv.length; i += 2) {
            paramName = (String) kv[i];
            paramValue = kv[i + 1];
            params.put(paramName, paramValue);
        }
        return params;
    }

    public static void logStatement(CharSequence sql, Map<String, ?> namedParameters) {
        if (LOGGER.isLoggable(Level.FINER)) {
            StringBuilder sb = new StringBuilder(sql);
            for (Entry<String, ?> e : namedParameters.entrySet()) {
                Object value = e.getValue();
                String sval;
                if (value instanceof Collection) {
                    Collection<?> c = (Collection<?>) value;
                    StringBuilder cv = new StringBuilder();
                    for (Iterator<?> it = c.iterator(); it.hasNext(); ) {
                        Object v = it.next();
                        if (v == null) {
                            cv.append("null");
                        } else if (v instanceof Number) {
                            cv.append(v);
                        } else {
                            cv.append("'").append(String.valueOf(v)).append("'");
                        }
                        if (it.hasNext()) {
                            cv.append(", ");
                        }
                    }
                    sval = cv.toString();
                } else {
                    sval =
                            value == null
                                    ? "null"
                                    : (value instanceof Number
                                            ? String.valueOf(value)
                                            : "'" + String.valueOf(value) + "'");
                }
                String paramName = ":" + e.getKey();
                int idx;
                while ((idx = sb.indexOf(paramName)) > -1) {
                    sb.replace(idx, idx + paramName.length(), sval);
                }
            }
            LOGGER.finer(sb.toString());
        } else if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("querying " + sql + "\n with values: " + namedParameters);
        }
    }
}
