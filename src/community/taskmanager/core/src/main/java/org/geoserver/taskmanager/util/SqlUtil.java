/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.util;

public final class SqlUtil {

    private SqlUtil() {}

    public static String quote(String tableName) {
        String[] parts = tableName.split("\\.");
        StringBuffer sb = new StringBuffer();
        for (String part : parts) {
            sb.append("\"").append(part).append("\"").append(".");
        }
        sb.setLength(sb.length() - 1);
        return sb.toString();
    }

    public static String notQualified(String tableName) {
        String[] split = tableName.split("\\.", 2);
        if (split.length == 2) {
            return split[1];
        }
        return tableName;
    }

    public static String schema(String tableName) {
        String[] split = tableName.split("\\.", 2);
        if (split.length == 2) {
            return split[0];
        }
        return null;
    }

    public static String qualified(String schema, String tableName) {
        if (schema == null) {
            return tableName;
        } else {
            return schema + "." + tableName;
        }
    }
}
