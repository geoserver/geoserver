/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jdbcconfig.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.geotools.util.logging.Logging;

public class DbUtils {

    static final Logger LOGGER = Logging.getLogger(DbUtils.class.getPackage().getName());

    public static Map<String, ?> params(Object... kv) {
        if (kv.length == 0) {
            return Collections.emptyMap();
        }
        Map<String, Object> params = new LinkedHashMap<>();
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
            LOGGER.finer("Querying: " + getLogStatement(sql, namedParameters));
        } else if (LOGGER.isLoggable(Level.FINE)) {
            if (namedParameters == null || namedParameters.isEmpty()) {
                LOGGER.fine("Querying: " + sql);
            } else {
                LOGGER.fine("Querying: " + sql + "\n with values: " + namedParameters);
            }
        }
    }

    public static String getLogStatement(CharSequence sql, Map<String, ?> namedParameters) {
        if (namedParameters == null || namedParameters.isEmpty()) {
            return sql.toString();
        }
        StringBuilder sb = new StringBuilder(sql);
        if (namedParameters.size() >= 10) {
            // replace the parameters in reverse order to correctly handle complex queries
            // with 10+ of a certain type of parameter
            List<Entry<String, ?>> list = new ArrayList<>(namedParameters.entrySet());
            Collections.reverse(list);
            list.forEach(parameter -> replaceParameter(sb, parameter));
        } else {
            namedParameters.entrySet().forEach(parameter -> replaceParameter(sb, parameter));
        }
        return sb.toString();
    }

    private static void replaceParameter(StringBuilder sb, Entry<String, ?> parameter) {
        Object value = parameter.getValue();
        String paramValue;
        if (value instanceof Collection) {
            Collection<?> c = (Collection<?>) value;
            paramValue = c.stream().map(DbUtils::toString).collect(Collectors.joining(", "));
        } else {
            paramValue = toString(value);
        }
        String paramName = ":" + parameter.getKey();
        for (int index; (index = sb.indexOf(paramName)) > -1; ) {
            sb.replace(index, index + paramName.length(), paramValue);
        }
    }

    private static String toString(Object value) {
        if (value == null) {
            return "null";
        } else if (value instanceof Number) {
            return value.toString();
        }
        return "'" + value.toString().replace("'", "''") + "'";
    }
}
