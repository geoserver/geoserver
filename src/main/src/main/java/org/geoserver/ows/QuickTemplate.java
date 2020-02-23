/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows;

import java.util.Map;

/**
 * Simpleton but fast template engine, replaces variables following the "${varName}" syntax into a
 * string. No escaping, no extras, but avoids building lots of strings to do its work and the
 * overhead of template instantiation of a true template engine.
 *
 * @author Andrea Aime - GeoSolutions
 */
class QuickTemplate {

    /**
     * Simple replacement of a set of variables in a string with their values. The variable names to
     * expand are case-insensitive.
     */
    static String replaceVariables(CharSequence template, Map<String, String> variables) {
        StringBuilder sb = new StringBuilder(template.toString().toLowerCase());
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            replaceVariable(sb, entry.getKey().toLowerCase(), entry.getValue());
        }

        return sb.toString();
    }

    static void replaceVariable(StringBuilder sb, String key, String value) {
        // infinite loop avoidance
        if (key.equals(value)) {
            return;
        }

        // replace with minimum char movement
        int idx = sb.lastIndexOf(key);
        while (idx >= 0) {
            sb.replace(idx, idx + key.length(), value);
            idx = sb.lastIndexOf(key, idx);
        }
    }
}
